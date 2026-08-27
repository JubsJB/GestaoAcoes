## ADDED Requirements

### Requirement: Calcular rentabilidade percentual atual por moeda
Para cada moeda presente no resumo, o sistema SHALL calcular `rentabilidadePercentual = (resultadoNaoRealizadoTotal / custoTotalPosicoes) × 100` usando exclusivamente os acumulados oficiais já produzidos para a mesma Carteira e moeda. A identidade `((patrimonioAtual / custoTotalPosicoes) - 1) × 100` SHALL permanecer somente uma equivalência matemática e MUST NOT constituir segunda fonte de cálculo.

#### Scenario: Ganho potencial agregado
- **WHEN** uma moeda possui `resultadoNaoRealizadoTotal` positivo e `custoTotalPosicoes` positivo
- **THEN** `rentabilidadePercentual` é positiva e representa o ganho potencial agregado das posições abertas dessa moeda

#### Scenario: Perda potencial agregada
- **WHEN** uma moeda possui `resultadoNaoRealizadoTotal` negativo e `custoTotalPosicoes` positivo
- **THEN** `rentabilidadePercentual` é negativa e representa a perda potencial agregada sem ser limitada artificialmente a zero ou a `-100`

#### Scenario: Resultado agregado nulo
- **WHEN** `resultadoNaoRealizadoTotal` é zero e `custoTotalPosicoes` é positivo
- **THEN** `rentabilidadePercentual` é `0.000000` e nunca `null`

#### Scenario: Ganho superior a cem por cento
- **WHEN** `resultadoNaoRealizadoTotal` é superior a `custoTotalPosicoes`
- **THEN** o sistema retorna o percentual calculado acima de `100.000000` sem aplicar teto artificial

### Requirement: Derivar rentabilidade dos totais consolidados
A rentabilidade da Carteira SHALL ser calculada somente depois da consolidação por moeda e MUST NOT reprocessar Operações, reagrupar posições, recalcular custo, recalcular patrimônio ou obter média simples das rentabilidades individuais.

#### Scenario: Múltiplas posições da mesma moeda
- **WHEN** duas posições abertas possuem custos `1000` e `3000` e resultados não realizados `100` e `300`
- **THEN** o resumo usa `400 / 4000 × 100` e retorna `10.000000`
- **AND** não calcula média simples dos percentuais individuais

#### Scenario: Divisão periódica
- **WHEN** os totais consolidados são `resultadoNaoRealizadoTotal=400` e `custoTotalPosicoes=3000`
- **THEN** `rentabilidadePercentual` é `13.333333` conforme a política numérica aprovada

### Requirement: Exigir custo total positivo para moeda presente
Todo item por moeda devolvido pelo resumo SHALL possuir `custoTotalPosicoes > 0`, pois ele representa ao menos uma posição aberta válida. O sistema MUST NOT retornar `null`, infinito, zero artificial, denominador fabricado ou resposta parcial quando essa invariável for violada.

#### Scenario: Custo total igual a zero em moeda presente
- **WHEN** uma moeda com posição aberta chega à etapa percentual com `custoTotalPosicoes=0`
- **THEN** toda a consulta falha com `409 Conflict` e código `HISTORICO_OPERACOES_INCONSISTENTE`

#### Scenario: Custo total negativo em moeda presente
- **WHEN** uma moeda com posição aberta chega à etapa percentual com `custoTotalPosicoes<0`
- **THEN** toda a consulta falha com `409 Conflict` e código `HISTORICO_OPERACOES_INCONSISTENTE`

#### Scenario: Carteira sem posições abertas
- **WHEN** a Carteira existe, mas não possui posições abertas
- **THEN** o sistema responde `200 OK` com `carteiraId` e `resumos=[]`
- **AND** não cria item BRL ou USD com rentabilidade artificial

### Requirement: Restringir rentabilidade aos ciclos atualmente abertos
A rentabilidade por moeda SHALL usar somente custo e resultado não realizado das posições atualmente abertas. Resultado realizado, caixa e valores de ciclos encerrados MUST NOT participar da fórmula nem manter artificialmente uma moeda no resumo.

#### Scenario: Venda parcial
- **WHEN** uma VENDA parcial deixa posição remanescente positiva
- **THEN** a rentabilidade usa somente `custoTotalPosicoes` e `resultadoNaoRealizadoTotal` remanescentes
- **AND** ignora o resultado realizado da parcela vendida

#### Scenario: Venda total remove a última posição da moeda
- **WHEN** uma VENDA total encerra a última posição aberta de uma moeda
- **THEN** o item dessa moeda desaparece do resumo
- **AND** resultado realizado histórico não recria o item

#### Scenario: Novo ciclo após zeramento
- **WHEN** uma nova COMPRA inicia ciclo após encerramento total
- **THEN** a rentabilidade usa somente custo e resultado não realizado do novo ciclo

### Requirement: Manter rentabilidade atual separada de métricas históricas
O percentual desta capability SHALL representar exclusivamente o resultado não realizado atual em relação ao custo contábil das posições abertas. O sistema MUST NOT tratá-lo como rentabilidade histórica, realizada, total desde a criação, ponderada por tempo ou por fluxos financeiros.

#### Scenario: Resultado realizado existente
- **WHEN** a Carteira possui resultado realizado histórico e posições abertas
- **THEN** o resultado realizado não participa do numerador, do denominador nem do percentual atual

#### Scenario: Métricas fora do escopo
- **WHEN** o resumo é consultado
- **THEN** aportes, resgates, caixa, dividendos, impostos, TWR, XIRR, conversão cambial, patrimônio histórico e evolução patrimonial não participam da rentabilidade

### Requirement: Aplicar política numérica percentual compartilhada
O cálculo SHALL usar exclusivamente `BigDecimal`, dividir em escala intermediária 24 com `RoundingMode.HALF_EVEN`, multiplicar o quociente por `100`, normalizar o percentual final para escala 6 com `RoundingMode.HALF_EVEN` e validar precisão máxima 38. Essa política SHALL ser matematicamente idêntica à política da rentabilidade percentual da posição e MUST NOT possuir implementação concorrente. O sistema MUST NOT usar `float`, `double`, `MathContext`, truncamento ou arredondamento implícito.

#### Scenario: Percentual representável
- **WHEN** o percentual pode ser representado dentro dos limites aprovados
- **THEN** o sistema o devolve com escala final 6

#### Scenario: Percentual fora da precisão
- **WHEN** o percentual não pode ser representado com precisão máxima 38 e escala 6
- **THEN** toda a consulta falha com `422 Unprocessable Entity` e código `CALCULO_POSICAO_FORA_DA_PRECISAO`
- **AND** nenhum resumo parcial é devolvido

### Requirement: Preservar consulta única e ausência de efeitos colaterais
A inclusão do percentual SHALL reutilizar o mesmo estado consolidado e consistente do resumo atual, com custo de cálculo O(1) por moeda após a agregação. A consulta MUST NOT provocar segunda consolidação, nova query, query por moeda ou indicador, novo replay, segunda chamada à fonte de posições, lock pessimista, escrita, `Clock`, persistência ou chamada externa.

#### Scenario: Uma única fonte consolidada
- **WHEN** o resumo com rentabilidade é consultado
- **THEN** custo, patrimônio, resultado não realizado e percentual são derivados do mesmo conjunto agregado por moeda
- **AND** o fetch plan vigente permanece sem N+1

#### Scenario: Providers indisponíveis
- **WHEN** BRAPI, Alpha Vantage ou `CotacaoProvider` está indisponível
- **THEN** `GET /resumo` não chama provider nem `PATCH /acoes/{id}/cotacao`

#### Scenario: Ausência de persistência
- **WHEN** o percentual é produzido
- **THEN** nenhuma entidade, tabela, migration, repository, snapshot, scheduler ou cache de rentabilidade é criado

## MODIFIED Requirements

### Requirement: Representar resumo consolidado por moeda
O `ResumoCarteiraResponse` SHALL conter exclusivamente `carteiraId` e `resumos`, e cada `ResumoMoedaResponse` SHALL conter exclusivamente `moeda`, `custoTotalPosicoes`, `patrimonioAtual`, `resultadoNaoRealizadoTotal` e `rentabilidadePercentual`.

#### Scenario: Carteira com posições BRL e USD
- **WHEN** a Carteira possui posições abertas em BRL e USD
- **THEN** a resposta contém um item independente para BRL e outro para USD
- **AND** os itens são ordenados por `moeda ASC`
- **AND** cada item contém sua própria rentabilidade percentual
- **AND** nenhum total monetário ou percentual combina moedas diferentes

#### Scenario: Carteira com apenas uma moeda
- **WHEN** a Carteira possui posições abertas somente em uma moeda
- **THEN** a resposta contém somente o item dessa moeda
- **AND** não cria item artificial de valor zero para moeda ausente

### Requirement: Aplicar política numérica do resumo
O sistema SHALL continuar somando `custoTotalPosicoes`, `patrimonioAtual` e `resultadoNaoRealizadoTotal` exclusivamente com `BigDecimal.add`, sem `MathContext` e sem arredondamento intermediário, normalizando somente esses acumulados finais para escala 12 com `RoundingMode.UNNECESSARY` e precisão máxima 38. Depois da consolidação, `rentabilidadePercentual` SHALL seguir exclusivamente sua política percentual de divisão em escala 24 com `HALF_EVEN`, multiplicação por 100, escala final 6 com `HALF_EVEN` e precisão máxima 38.

#### Scenario: Acumulados representáveis
- **WHEN** os acumulados exatos e o percentual de uma moeda podem ser representados nas políticas aprovadas
- **THEN** custo, patrimônio e resultado não realizado são retornados em escala 12
- **AND** rentabilidade é retornada em escala 6
- **AND** nenhum valor sofre truncamento ou arredondamento silencioso

#### Scenario: Qualquer acumulado fora da precisão
- **WHEN** qualquer acumulado ou percentual de qualquer moeda excede a precisão máxima ou não pode ser normalizado conforme sua política
- **THEN** a consulta inteira falha com `422 Unprocessable Entity`
- **AND** reutiliza `CALCULO_POSICAO_FORA_DA_PRECISAO`
- **AND** não retorna resumo parcial

#### Scenario: Histórico inconsistente
- **WHEN** o replay oficial detecta posição aberta inválida, histórico impossível ou custo total não positivo em moeda presente
- **THEN** a consulta inteira falha com `409 Conflict` e `HISTORICO_OPERACOES_INCONSISTENTE`
