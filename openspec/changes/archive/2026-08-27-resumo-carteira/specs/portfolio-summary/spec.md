## Purpose

Definir a consulta do resumo financeiro atual de uma Carteira, consolidando por moeda o custo, o patrimônio e o resultado não realizado das posições abertas.

## ADDED Requirements

### Requirement: Consultar resumo financeiro atual da Carteira
O sistema SHALL expor `GET /carteiras/{carteiraId}/resumo`, sem body, filtros ou paginação e sem retornar `Location`.

#### Scenario: Carteira existente com posições abertas
- **WHEN** o cliente consulta o resumo de uma Carteira existente com posições abertas
- **THEN** o sistema responde `200 OK` com um `ResumoCarteiraResponse`
- **AND** a resposta contém `carteiraId` e a coleção `resumos` consolidada por moeda

#### Scenario: Carteira existente sem posições abertas
- **WHEN** o cliente consulta o resumo de uma Carteira existente sem posições abertas
- **THEN** o sistema responde `200 OK`
- **AND** retorna o `carteiraId` consultado e `resumos` igual a `[]`

#### Scenario: Carteira inexistente
- **WHEN** o cliente consulta o resumo de uma Carteira inexistente
- **THEN** o sistema responde `404 Not Found` pelo tratamento centralizado vigente

### Requirement: Representar resumo consolidado por moeda
O `ResumoCarteiraResponse` SHALL conter exclusivamente `carteiraId` e `resumos`, e cada `ResumoMoedaResponse` SHALL conter exclusivamente `moeda`, `custoTotalPosicoes`, `patrimonioAtual` e `resultadoNaoRealizadoTotal`.

#### Scenario: Carteira com posições BRL e USD
- **WHEN** a Carteira possui posições abertas em BRL e USD
- **THEN** a resposta contém um item independente para BRL e outro para USD
- **AND** os itens são ordenados por `moeda ASC`
- **AND** nenhum total monetário combina moedas diferentes

#### Scenario: Carteira com apenas uma moeda
- **WHEN** a Carteira possui posições abertas somente em uma moeda
- **THEN** a resposta contém somente o item dessa moeda
- **AND** não cria item artificial de valor zero para moeda ausente

### Requirement: Consolidar o custo das posições abertas
Para cada moeda, o sistema SHALL calcular `custoTotalPosicoes = soma(custoPosicao)` das posições abertas dessa moeda, utilizando exclusivamente o `custoPosicao` já produzido pela consolidação oficial de posições.

#### Scenario: Múltiplas posições da mesma moeda
- **WHEN** a Carteira possui múltiplas posições abertas na mesma moeda
- **THEN** `custoTotalPosicoes` é a soma exata dos respectivos `custoPosicao`
- **AND** o sistema não recalcula custo diretamente a partir das Operações

### Requirement: Preservar o patrimônio oficial por moeda
Para cada moeda, o sistema SHALL calcular `patrimonioAtual = soma(valorAtualPosicao)` das posições abertas dessa moeda, utilizando exclusivamente o `valorAtualPosicao` já produzido pela consolidação oficial de posições.

#### Scenario: Equivalência com a consulta de patrimônio
- **WHEN** `GET /resumo` e `GET /patrimonio` observam a mesma Carteira sob o mesmo conjunto consistente de posições
- **THEN** `patrimonioAtual` de cada moeda é matematicamente idêntico nos dois contratos
- **AND** o resumo não recalcula `quantidadeAtual × cotacaoAtual`

### Requirement: Consolidar o resultado não realizado das posições abertas
Para cada moeda, o sistema SHALL calcular `resultadoNaoRealizadoTotal = soma(resultadoNaoRealizado)` das posições abertas dessa moeda. A identidade `resultadoNaoRealizadoTotal = patrimonioAtual - custoTotalPosicoes` SHALL servir somente como verificação matemática e MUST NOT constituir uma segunda fonte de cálculo.

#### Scenario: Resultado total positivo
- **WHEN** a soma dos resultados não realizados das posições de uma moeda é positiva
- **THEN** `resultadoNaoRealizadoTotal` representa o ganho potencial agregado nessa moeda

#### Scenario: Resultado total negativo
- **WHEN** a soma dos resultados não realizados das posições de uma moeda é negativa
- **THEN** `resultadoNaoRealizadoTotal` preserva o valor negativo como perda potencial agregada

#### Scenario: Resultado total zero
- **WHEN** os resultados não realizados das posições de uma moeda somam exatamente zero
- **THEN** `resultadoNaoRealizadoTotal` é `0.000000000000`
- **AND** nunca é `null`

### Requirement: Considerar somente posições atualmente abertas
O resumo SHALL considerar exclusivamente posições abertas produzidas pelo replay oficial e MUST NOT recriar posições zeradas ou incorporar valores financeiros de ciclos encerrados.

#### Scenario: Venda parcial
- **WHEN** uma VENDA parcial deixa quantidade remanescente positiva
- **THEN** custo, patrimônio e resultado não realizado do resumo refletem somente a posição remanescente
- **AND** o resultado realizado da parcela vendida não participa

#### Scenario: Venda total
- **WHEN** uma VENDA total encerra uma posição
- **THEN** essa posição deixa de participar integralmente do resumo atual
- **AND** seu resultado realizado histórico permanece somente na consulta dedicada

#### Scenario: Novo ciclo após zeramento
- **WHEN** uma nova COMPRA inicia ciclo após o encerramento total
- **THEN** o novo ciclo volta a participar do resumo com seus próprios valores
- **AND** não herda custo, patrimônio ou resultado não realizado do ciclo encerrado

### Requirement: Manter conceitos financeiros independentes
O resumo atual SHALL representar somente custo, patrimônio e resultado não realizado das posições abertas e MUST NOT somar resultado realizado, resultado não realizado novamente ao patrimônio ou rentabilidade percentual.

#### Scenario: Resultado realizado histórico
- **WHEN** a Carteira possui resultado realizado histórico
- **THEN** ele não é tratado como saldo de caixa nem integra o resumo atual

#### Scenario: Ausência de dupla contagem
- **WHEN** uma posição possui `resultadoNaoRealizado`
- **THEN** o patrimônio utiliza somente `valorAtualPosicao`
- **AND** não soma novamente o resultado não realizado

#### Scenario: Rentabilidade individual
- **WHEN** uma posição possui `rentabilidadePercentual`
- **THEN** o resumo não soma, agrega, promedia ou deriva rentabilidade consolidada

### Requirement: Aplicar política numérica do resumo
O sistema SHALL utilizar exclusivamente `BigDecimal`, somar cada indicador com `BigDecimal.add` sem `MathContext` e sem arredondamento intermediário e normalizar somente cada acumulado final para escala 12 com `RoundingMode.UNNECESSARY` e precisão máxima 38.

#### Scenario: Acumulados representáveis
- **WHEN** os acumulados exatos de uma moeda podem ser representados na política aprovada
- **THEN** custo, patrimônio e resultado não realizado são retornados em escala 12
- **AND** nenhum valor sofre truncamento ou arredondamento silencioso

#### Scenario: Qualquer acumulado fora da precisão
- **WHEN** custo, patrimônio ou resultado não realizado de qualquer moeda excede a precisão máxima 38 ou não pode ser normalizado sem perda
- **THEN** a consulta inteira falha com `422 Unprocessable Entity`
- **AND** reutiliza `CALCULO_POSICAO_FORA_DA_PRECISAO`
- **AND** não retorna resumo parcial

#### Scenario: Histórico inconsistente
- **WHEN** o replay oficial detecta posição aberta inválida ou histórico impossível
- **THEN** a consulta inteira falha com `409 Conflict` e `HISTORICO_OPERACOES_INCONSISTENTE`

### Requirement: Preservar separação por moeda
O sistema MUST acumular BRL e USD independentemente e MUST NOT converter moedas, consultar câmbio ou produzir total monetário único multimoeda.

#### Scenario: Resumo multimoeda
- **WHEN** a Carteira contém posições abertas em BRL e USD
- **THEN** custo, patrimônio e resultado não realizado de cada item usam somente posições da própria moeda

### Requirement: Consultar sem efeitos colaterais ou integrações externas
A consulta SHALL operar em modo read-only sob `Isolation.REPEATABLE_READ`, reutilizar uma única consolidação oficial de posições e MUST NOT persistir resumo, adquirir lock pessimista, usar `Clock`, chamar provider externo ou criar consulta adicional por indicador.

#### Scenario: Uma única consolidação de posições
- **WHEN** o resumo é consultado
- **THEN** a posição consolidada é obtida uma única vez
- **AND** custo, patrimônio e resultado não realizado são derivados em memória da mesma lista
- **AND** não ocorre segundo replay financeiro nem query por moeda, posição, Ação ou indicador

#### Scenario: Providers indisponíveis
- **WHEN** BRAPI, Alpha Vantage ou outro provider está indisponível
- **THEN** o resumo continua utilizando somente as cotações já persistidas refletidas nas posições
- **AND** não chama `CotacaoProvider` nem `PATCH /acoes/{id}/cotacao`

#### Scenario: Ausência de persistência e timestamp agregado
- **WHEN** o resumo é produzido
- **THEN** nenhuma entidade, tabela, repository, migration ou snapshot de resumo é criado
- **AND** nenhum timestamp agregado é fabricado ou retornado
