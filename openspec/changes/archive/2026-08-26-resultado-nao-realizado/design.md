## Context

Veja `proposal.md` para a motivação e `specs/portfolio-position/spec.md` para o contrato. A baseline já consolida posições sob demanda no fluxo `CarteiraResource → PosicaoService → CalculadoraPosicao → PosicaoMapper → PosicaoResponse`, calcula `valorAtualPosicao` com os dados persistidos da Ação e carrega `Operacao.acao` no mesmo fetch plan para evitar N+1. A consulta é `readOnly` com `Isolation.REPEATABLE_READ`; a atualização externa permanece isolada em `PATCH /acoes/{id}/cotacao`.

O PRD define resultado não realizado como `valorAtual - custoPosicao` e exige que ele considere somente a posição mantida. O replay existente já trata compra, venda parcial, zeramento e novo ciclo, logo o novo cálculo deve consumir o estado final pronto sem reinterpretar Operações.

## Goals / Non-Goals

**Goals:**

- Acrescentar um indicador monetário exato ao DTO da posição aberta.
- Centralizar a subtração e sua validação numérica na `CalculadoraPosicao`.
- Preservar a separação entre estado contábil derivado de Operações e avaliação de mercado derivada da cotação persistida.
- Manter consulta sem escrita, chamadas externas, Clock, lock pessimista ou novas queries.

**Non-Goals:**

- Calcular ou expor resultado realizado, rentabilidade, patrimônio ou agregações.
- Persistir posição ou resultado, criar schema, migration, cache ou snapshot.
- Alterar atualização de cotação, providers, regras de Operação ou replay financeiro.

## Decisions

### 1. Usar `valorAtualPosicao - custoPosicao` como fórmula oficial

A `CalculadoraPosicao` calculará o resultado a partir dos dois valores finais já consolidados e normalizados. Essa escolha segue literalmente o PRD, reutiliza as políticas aprovadas dos operandos e evita recalcular uma expressão equivalente a partir de preço médio potencialmente oriundo de divisão periódica.

A alternativa `(cotacaoAtual - precoMedio) × quantidadeAtual` é matematicamente reconhecida, mas não será uma segunda implementação: ela pode amplificar diferenças de representação do preço médio apresentado e criar duas fontes de verdade.

### 2. Calcular na `CalculadoraPosicao`, fora do mapper

Será acrescentada uma operação pura equivalente a `calcularResultadoNaoRealizado(valorAtualPosicao, custoPosicao)`. Ela não acessará repository, provider, Clock ou estado mutável. O `PosicaoService` coordenará o cálculo depois do replay e do valor atual; o `PosicaoMapper` receberá o valor pronto e apenas o projetará no novo campo de `PosicaoResponse`.

A alternativa de subtrair no mapper foi rejeitada porque colocaria regra financeira em código de projeção. Um novo service exclusivo também foi rejeitado por não adicionar uma fronteira arquitetural útil.

### 3. Aplicar escala 12, precisão máxima 38 e exatidão obrigatória

Os operandos `valorAtualPosicao` e `custoPosicao` já saem em escala 12. A calculadora fará a subtração com `BigDecimal`, normalizará a saída para escala 12 usando `RoundingMode.UNNECESSARY` e verificará precisão total máxima 38. Valores positivos, negativos e zero são válidos; zero será representado em escala 12, nunca como `null`.

Embora a subtração de dois operandos não negativos dentro do mesmo limite tenda a permanecer representável, a validação explícita protege chamadas unitárias, futuras evoluções e estados inconsistentes sem introduzir arredondamento silencioso. Qualquer falha aritmética seguirá o mapeamento existente para `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`, sem resposta parcial e sem novo código de erro.

### 4. Consumir somente o estado final do ciclo aberto

O cálculo ocorrerá apenas depois de o replay concluir e depois do filtro que omite quantidade zero. Assim, venda parcial usa custo e quantidade remanescentes; venda total não gera DTO; compra após zeramento usa apenas o novo ciclo. Preço de venda e eventual resultado realizado não serão lidos pelo novo cálculo.

### 5. Preservar consulta transacional e fetch plan atuais

`PosicaoService.listarPorCarteira` continuará `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`, sem lock. O método existente de `OperacaoRepository` com `@EntityGraph(attributePaths = "acao")` continuará trazendo as Ações junto ao histórico. A subtração opera em memória e não requer query adicional.

### 6. Manter a atualização de cotação como fluxo independente

`GET /carteiras/{carteiraId}/posicoes` continuará lendo `Acao.cotacaoAtual` e `Acao.dataHoraCotacao` persistidas. Não chamará `CotacaoProvider`, BRAPI, Alpha Vantage, `Clock` nem `PATCH` internamente. O fluxo dedicado `AcaoResource → AcaoService → CotacaoProvider → AcaoCotacaoPersistenceService → AcaoRepository` permanece responsável por obter e persistir nova cotação.

### 7. Não alterar persistência nem contratos financeiros adjacentes

O novo valor existirá somente em `PosicaoResponse`. Nenhuma entidade, repository, migration ou dependência será criada. `Operacao.precoUnitario`, `precoMedio`, `custoPosicao`, replay, validação de VENDA e proteção de DELETE de Carteira permanecem inalterados.

## Risks / Trade-offs

- [Risco] Uma implementação alternativa baseada em preço médio apresentado pode divergir da fórmula oficial em casos periódicos → testes exigirão que a única fonte seja `valorAtualPosicao - custoPosicao`.
- [Risco] Acrescentar o campo ao record altera construtores e fixtures existentes → atualizar somente os pontos de projeção e testes afetados, mantendo ordem e semântica dos campos anteriores.
- [Risco] Falha numérica de uma posição poderia produzir lista parcial → manter o comportamento fail-fast existente e mapear a falha antes de devolver a resposta.
- [Trade-off] O resultado reflete a última cotação persistida, que pode estar desatualizada → manter `dataHoraCotacao` no contrato e deixar atualização sob responsabilidade explícita do PATCH, sem sacrificar disponibilidade do GET.

## Migration Plan

1. Introduzir cálculo puro e testes unitários.
2. Estender DTO, orquestração e mapper, ajustando testes HTTP e de serviço.
3. Executar regressões de posição, Operações, atualização de cotação e persistência H2/PostgreSQL quando disponível.
4. Publicar como acréscimo compatível do campo JSON; rollback consiste em reverter apenas código e testes desta change, pois não há estado persistido nem migration.
