## Context

Veja `proposal.md` para a motivação e `specs/realized-result/spec.md` para o contrato. A baseline persiste somente Operações e deriva a posição pelo fluxo `CarteiraResource → PosicaoService → CalculadoraPosicao`. `CalculadoraPosicao` já é a interpretação financeira única de COMPRA, VENDA parcial, zeramento, novo ciclo, cronologia e precisão; `OperacaoService` a reutiliza para validar a candidata e `PosicaoService` a reutiliza para consolidar o histórico.

`OperacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc` já carrega todo o histórico da Carteira com `@EntityGraph(attributePaths = "acao")`. Dentro de cada grupo Carteira+Ação, `dataOperacao` e `ordemNoDia` determinam a ordem financeira; o ID apenas estabiliza a leitura global. O endpoint de posição omite zerados, portanto não pode representar resultado realizado de ciclos encerrados.

O preço médio interno da calculadora usa escala 24 e `HALF_EVEN`; a projeção contábil usa escala 12. Como o resultado de uma VENDA pode depender de preço médio periódico, a política aprovada exige calcular com o estado interno, acumular antes da normalização e arredondar somente o total final.

## Goals / Non-Goals

**Goals:**

- estender o replay único para produzir resultado realizado acumulado sem modificar a semântica da posição;
- consultar o acumulado por Carteira+Ação inclusive para posições encerradas e múltiplos ciclos;
- manter precisão interna, separação por moeda, snapshot transacional consistente e ausência de efeitos colaterais;
- preservar integralmente os contratos de cadastro de VENDA, posição consolidada e atualização de cotação.

**Non-Goals:**

- detalhar cada VENDA ou criar endpoint por Ação;
- persistir resultado, posição, evento calculado ou snapshot;
- calcular total de Carteira, resultado total, patrimônio, rentabilidade realizada/histórica, TWR ou XIRR;
- modelar taxas, impostos, dividendos, juros, câmbio ou cotação histórica;
- alterar entidades, schema, repositories, providers, dependências ou configurações.

## Decisions

### 1. Criar endpoint histórico próprio no `CarteiraResource`

`CarteiraResource` acrescentará somente:

```text
GET /carteiras/{carteiraId}/resultados-realizados
```

O resource delegará a `ResultadoRealizadoService.listarPorCarteira(carteiraId)` e devolverá `ResponseEntity.ok(lista)`. O contrato não será colocado em `OperacaoResource`, pois a resposta não é uma listagem de fatos individuais, nem em `GET /posicoes`, que representa somente posições abertas.

Alternativas rejeitadas: campo em `PosicaoResponse`, que perde ciclos encerrados; endpoint por VENDA, que amplia o escopo; e endpoint de nível superior sem Carteira, que elimina o contexto financeiro obrigatório.

### 2. Usar um DTO acumulado mínimo por Ação

Será criado o record:

```text
ResultadoRealizadoResponse(
  acaoId,
  ticker,
  nomeEmpresa,
  mercado,
  moeda,
  resultadoRealizado
)
```

`ResultadoRealizadoMapper` receberá a Ação e o acumulado já calculado e apenas os projetará. Não haverá cálculo financeiro no mapper. Somente grupos cujo replay indique ao menos uma VENDA serão mapeados; isso diferencia corretamente ausência de realização de um acumulado realizado igual a zero.

Alternativa rejeitada: inferir existência de VENDA por `resultadoRealizado != 0`, pois lucro e prejuízo podem se anular.

### 3. Evoluir o mesmo `ResultadoReplay`

`CalculadoraPosicao.reproduzir` continuará executando um único fold e passará a produzir, além de `PosicaoCalculada` e eventual `FalhaReplay`:

- `resultadoRealizado` acumulado e normalizado para saída;
- `possuiVenda`, booleano interno.

O estado mutável local do fold ganhará `resultadoRealizadoInterno=0` e `possuiVenda=false`. `validarQuantidade`, usado no POST, continuará no modo quantitativo: não precisa calcular preço médio ou resultado, mas devolverá valores neutros nos novos campos. Assim, seu contrato público de erro `POSICAO_INSUFICIENTE` e seu custo atual permanecem preservados.

Alternativas rejeitadas: segunda calculadora ou segundo replay, que poderiam divergir; e produzir uma lista de eventos detalhados, desnecessária para o DTO aprovado e mais custosa em memória.

### 4. Calcular a VENDA antes de alterar o estado contábil

Para toda VENDA válida no modo financeiro, a ordem dentro do fold será:

```text
precoMedioAntesDaVenda = precoMedioInterno
resultadoVenda =
    (precoUnitarioVenda - precoMedioAntesDaVenda)
    × quantidadeVendida
resultadoRealizadoInterno += resultadoVenda
possuiVenda = true

novaQuantidade = quantidade - quantidadeVendida
se novaQuantidade = 0:
    quantidade = 0
    custo = 0
    precoMedio = 0
senão:
    custo = custo × novaQuantidade / quantidadeAnterior
    quantidade = novaQuantidade
    precoMedio permanece vigente
```

Calcular antes do zeramento é indispensável para não perder a base da VENDA total. O preço recebido pela VENDA participa somente do resultado realizado; nunca redefine custo ou preço médio remanescente.

### 5. Acumular continuamente entre ciclos

O zeramento afetará quantidade, custo e preço médio, mas não `resultadoRealizadoInterno` nem `possuiVenda`. Nova COMPRA começa o estado contábil a partir de zero e futuras VENDAS continuam somando ao acumulado histórico do mesmo grupo.

Alternativas rejeitadas: zerar o resultado junto com a posição, que apaga fatos realizados; e acumular por ciclo no contrato, que exigiria identidade e DTO de ciclo ainda não definidos.

### 6. Aplicar a política numérica somente ao acumulado final

O cálculo usará:

- `BigDecimal` em todas as etapas;
- preço médio interno vigente em escala 24, já produzido pelo replay;
- subtração preço de venda menos preço médio sem nova normalização;
- multiplicação exata pela quantidade em escala até 6;
- soma exata dos resultados internos, sem arredondamento por VENDA;
- normalização final do acumulado para escala 12 com `RoundingMode.HALF_EVEN`;
- precisão total máxima 38 após a normalização;
- zero final `0.000000000000`.

Uma falha de normalização ou precisão produzirá `FalhaReplay` do tipo `CALCULO_FORA_DA_PRECISAO`; o service a traduzirá para `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`. A calculadora não usará o preço médio de escala 12 exposto no DTO.

Alternativas rejeitadas: arredondar cada VENDA, que altera a soma; `UNNECESSARY`, incompatível com médias periódicas; e `MathContext` global, que poderia descartar dígitos inteiros antes da validação final.

### 7. Criar `ResultadoRealizadoService` read-only

O service será `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)` e executará:

1. validar Carteira com `CarteiraRepository.findById`;
2. carregar uma vez o histórico com o método cronológico existente de `OperacaoRepository`;
3. agrupar em memória por `acao.id`, mantendo a ordem relativa recebida;
4. chamar `CalculadoraPosicao.reproduzir` uma vez por grupo;
5. falhar integralmente se qualquer grupo for inválido;
6. selecionar grupos com `possuiVenda=true`;
7. mapear e ordenar por mercado, ticker e `acaoId`.

Não haverá lock pessimista, save, flush, Clock, provider ou chamada externa. Uma Operação concorrente ficará integralmente dentro ou fora do snapshot repetível.

### 8. Reutilizar o fetch plan e não criar repository

O método `findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc` continuará sendo a única leitura do histórico e seu `@EntityGraph("acao")` evitará N+1 na projeção. Não será criada consulta por VENDA, por Ação ou agregação SQL: SQL agregado não conhece o preço médio vigente antes de cada VENDA nem os reinícios de ciclo.

O processamento será `O(n)` em tempo para as Operações carregadas e `O(n)` em memória nesta primeira versão. A escala futura deverá ser guiada por medição antes de cache ou materialização.

### 9. Preservar cronologia financeira e isolamento

A consulta global está ordenada por data, ordem no dia e ID. Ao agrupar sem reordenar, cada Carteira+Ação conserva `dataOperacao ASC, ordemNoDia ASC`; a constraint vigente impede duplicidade dessas duas chaves dentro do grupo. O ID permanece somente estabilizador entre grupos independentes.

Cada service consulta uma Carteira por vez e cada grupo usa uma Ação. Nenhuma soma ocorre entre Ações ou moedas. BRASIL permanece BRL e EUA permanece USD, inclusive com quantidade fracionária.

### 10. Traduzir falhas conforme o contexto

`ResultadoRealizadoService` mapeará `HISTORICO_INCONSISTENTE` para `409 / HISTORICO_OPERACOES_INCONSISTENTE`, com detalhes do grupo e da Operação quando disponíveis, e `CALCULO_FORA_DA_PRECISAO` para `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`. Nenhum grupo válido será devolvido se outro falhar.

`OperacaoService` continuará convertendo falha quantitativa da candidata em `409 / POSICAO_INSUFICIENTE`. A extensão interna da calculadora não substituirá esse código nem mudará lock, transação ou persistência do POST.

### 11. Preservar posição aberta e cotação

`PosicaoService` poderá continuar consumindo `posicao()` do replay e ignorar os novos campos internos. Seus cálculos posteriores de valor atual, resultado não realizado e rentabilidade permanecem na mesma ordem e fórmula. Nenhum campo será acrescentado a `PosicaoResponse`.

`Acao.cotacaoAtual`, `dataHoraCotacao`, BRAPI, Alpha Vantage e `CotacaoProvider` não participam do resultado realizado. O fluxo `AcaoResource → AcaoService → CotacaoProvider → AcaoCotacaoPersistenceService → AcaoRepository` permanece independente.

### 12. Não alterar persistência ou schema

`ResultadoRealizadoResponse`, mapper, service e os campos internos do replay não são entidades. `Operacao`, `Acao`, `Carteira`, repositories, changeSets 001–004, changelog master, dependências, configurações e `ddl-auto=validate` permanecerão inalterados.

## Risks / Trade-offs

- [Preço médio periódico exige aproximação] → usar o estado interno de escala 24 e arredondar somente o acumulado final com `HALF_EVEN`.
- [Alterar `ResultadoReplay` pode regressar posição e POST] → manter os acessores existentes, valores neutros no replay quantitativo e executar regressões completas de ambos os consumidores.
- [Resultado zero não revela se houve VENDA] → manter `possuiVenda` explícito no resultado interno, sem inferência pelo valor.
- [Uma Carteira grande exige replay completo] → reutilizar índice/fetch atuais e processamento linear; medir antes de materialização ou cache.
- [Falha em um grupo impede todos os resultados] → comportamento intencional para não apresentar uma consolidação histórica parcial como completa.
- [A lista não detalha a origem de cada valor] → preservar histórico nos endpoints de Operações e deixar detalhamento por VENDA para capability própria.
- [Mudança futura na política fiscal pode divergir do custo médio contábil] → esta capability segue estritamente o PRD atual e não antecipa preço médio fiscal, impostos ou compensações.

## Migration Plan

1. Estender o resultado interno e os testes puros da calculadora, preservando os consumidores existentes.
2. Adicionar DTO, mapper, service e rota de consulta sem alterar persistência.
3. Executar testes unitários, HTTP, repository/fetch, concorrência e regressões de Operações, posição e cotação.
4. Validar H2/PostgreSQL quando disponível, Liquibase/Hibernate, suíte completa, OpenSpec strict e Graphify.

Rollback consiste em remover a rota, DTO, mapper, service e os campos internos acrescentados ao replay. Não existe rollback de banco porque nenhum dado ou schema será criado.
