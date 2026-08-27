## Context

`PosicaoService.listarPorCarteira` já valida a Carteira, carrega o histórico cronológico com `Operacao.acao`, executa o replay uma vez por Ação e entrega `PosicaoResponse` contendo `custoPosicao`, `valorAtualPosicao` e `resultadoNaoRealizado`. `PatrimonioService` chama esse fluxo uma vez e atualmente implementa internamente a soma e a normalização de `valorAtualPosicao` por moeda.

O novo resumo precisa somar três valores da mesma lista e manter `patrimonioAtual` idêntico ao endpoint existente. Copiar a soma para outro service criaria duas implementações da agregação patrimonial; chamar `PatrimonioService` e `PosicaoService` no mesmo fluxo provocaria duas consolidações.

## Goals / Non-Goals

**Goals:**

- Obter as posições consolidadas exatamente uma vez por requisição de resumo.
- Consolidar custo, patrimônio e resultado não realizado por moeda com a política numérica aprovada.
- Garantir uma única implementação da agregação patrimonial usada por `GET /patrimonio` e `GET /resumo`.
- Manter mappers sem regras financeiras e services sem providers, escrita ou queries adicionais.

**Non-Goals:**

- Alterar replay, `PosicaoResponse`, `PatrimonioResponse` ou contratos existentes.
- Incluir resultado realizado, rentabilidade consolidada, caixa, câmbio, histórico ou persistência.
- Criar agregação SQL, repository ou endpoint por moeda.

## Decisions

### 1. Contrato REST e DTOs

Adicionar `GET /carteiras/{carteiraId}/resumo` em `CarteiraResource`. `ResumoCarteiraResponse` conterá `carteiraId` e `List<ResumoMoedaResponse> resumos`; cada item conterá `moeda`, `custoTotalPosicoes`, `patrimonioAtual` e `resultadoNaoRealizadoTotal`. A ordenação será `moeda ASC`.

Alternativas rejeitadas: estender `PatrimonioResponse`, porque mudaria um contrato focado; criar endpoint por moeda, porque seria redundante e desnecessário nesta versão.

### 2. Fontes oficiais dos acumulados

- `custoTotalPosicoes`: soma de `PosicaoResponse.custoPosicao`.
- `patrimonioAtual`: soma de `PosicaoResponse.valorAtualPosicao`.
- `resultadoNaoRealizadoTotal`: soma de `PosicaoResponse.resultadoNaoRealizado`.

`patrimonioAtual - custoTotalPosicoes` será somente identidade para testes de consistência. Não haverá segunda implementação de resultado não realizado. O resultado realizado continuará fora do resumo porque o modelo não possui caixa.

### 3. Agregador puro compartilhado por moeda

Extrair de `PatrimonioService` um componente puro, provisoriamente chamado `AgregadorPosicoesPorMoeda`, que receba uma coleção de `PosicaoResponse` e devolva totais internos por moeda para custo, patrimônio e resultado não realizado. O componente será usado por `PatrimonioService` e pelo novo `ResumoCarteiraService`.

O agregador centralizará o agrupamento, `BigDecimal.add`, normalização final e validação de precisão. Ele não consultará banco, repository, service ou provider; não usará `Clock`; não abrirá transação; não persistirá; não executará replay; e não recalculará qualquer valor individual da posição. Falha de representação será sinalizada por exceção interna com o contexto do acumulado, e cada service fará a tradução para `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`, preservando a separação entre cálculo puro e HTTP.

`PatrimonioService` continuará chamando `PosicaoService.listarPorCarteira` exatamente uma vez, passará a lista ao agregador, consumirá somente `patrimonioAtual` e preservará seu contrato. `ResumoCarteiraService` chamará `PosicaoService.listarPorCarteira` exatamente uma vez, passará a mesma lista ao agregador e consumirá os três totais. `ResumoCarteiraService` não chamará `PatrimonioService`.

Alternativas rejeitadas:

- Duplicar as somas no novo service: permite divergência entre patrimônio e resumo.
- Fazer `ResumoCarteiraService` chamar `PatrimonioService` além de `PosicaoService`: executa duas consolidações e pode observar estados distintos.
- Alterar `PosicaoService` para retornar DTO agregado: mistura consolidação por Ação com apresentação por moeda.

Esta extração altera somente a organização interna de `PatrimonioService` e preserva integralmente seu comportamento. A estratégia de agregação está aprovada e definitiva.

### 4. Política numérica

Cada campo será acumulado independentemente por moeda com `BigDecimal.add`, sem `MathContext` ou normalização intermediária. Ao final, cada acumulado será normalizado para escala 12 com `RoundingMode.UNNECESSARY` e validado com precisão máxima 38.

Falha em qualquer acumulado produzirá `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` e abortará toda a resposta. Resultado não realizado negativo e zero são válidos; zero será representado em escala 12. Histórico inconsistente continuará falhando antes da agregação pelo mecanismo vigente de `PosicaoService`.

### 5. Transação e consistência

`ResumoCarteiraService` usará `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`. A chamada única a `PosicaoService.listarPorCarteira` participará da mesma transação Spring por propagação padrão. Não haverá lock pessimista, escrita ou consulta adicional.

### 6. Mapeamento

`ResumoCarteiraMapper` projetará somente totais já calculados em `ResumoMoedaResponse` e a lista imutável em `ResumoCarteiraResponse`. Nenhuma soma ou normalização ocorrerá no mapper.

## Risks / Trade-offs

- [Extração compartilhada alterar inadvertidamente `GET /patrimonio`] → preservar contrato e mensagens de erro existentes, manter testes de regressão do patrimônio e comparar ambos os endpoints para a mesma Carteira.
- [Overflow em apenas um dos três acumulados] → normalizar todos antes de mapear e falhar integralmente, sem resposta parcial.
- [Transação externa e chamada service-to-service] → usar propagação padrão e confirmar por teste que há uma única chamada ao `PosicaoService` e uma visão `REPEATABLE_READ`.
- [Map não determinístico] → acumular por `Moeda` e ordenar explicitamente pelo nome do enum antes do mapeamento.

## Migration Plan

Não há migração de dados ou schema. A implementação será aditiva no contrato REST; a extração interna será protegida pela regressão completa de `GET /patrimonio` e `GET /posicoes`. O rollback remove o endpoint e os novos componentes e devolve a agregação compartilhada ao `PatrimonioService`, sem transformação de dados.
