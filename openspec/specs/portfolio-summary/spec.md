# portfolio-summary Specification

## Purpose

Definir a consulta do resumo financeiro atual de uma Carteira, consolidando por moeda o custo, o patrimônio, o resultado não realizado e a rentabilidade percentual das posições abertas.

## Requirements

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

### Requirement: Calcular rentabilidade percentual atual por moeda
Para cada moeda presente no resumo, o sistema SHALL calcular `rentabilidadePercentual = (resultadoNaoRealizadoTotal / custoTotalPosicoes) × 100` usando exclusivamente os acumulados oficiais já produzidos para a mesma Carteira e moeda. A identidade `((patrimonioAtual / custoTotalPosicoes) - 1) × 100` SHALL permanecer somente uma equivalência matemática e MUST NOT constituir segunda fonte de cálculo. O valor SHALL ser representado como percentual, de modo que `12.500000` significa `12,5%`, e MUST NOT ser retornado como razão `0.125000`.

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

### Requirement: Considerar somente posições atualmente abertas
O resumo SHALL considerar exclusivamente posições abertas produzidas pelo replay oficial e MUST NOT recriar posições zeradas ou incorporar valores financeiros de ciclos encerrados.

#### Scenario: Venda parcial
- **WHEN** uma VENDA parcial deixa quantidade remanescente positiva
- **THEN** custo, patrimônio e resultado não realizado do resumo refletem somente a posição remanescente
- **AND** a rentabilidade usa somente o custo e o resultado não realizado remanescentes
- **AND** o resultado realizado da parcela vendida não participa

#### Scenario: Venda total
- **WHEN** uma VENDA total encerra uma posição
- **THEN** essa posição deixa de participar integralmente do resumo atual
- **AND** seu resultado realizado histórico permanece somente na consulta dedicada
- **AND** o item da moeda desaparece quando não resta outra posição aberta nessa moeda

#### Scenario: Novo ciclo após zeramento
- **WHEN** uma nova COMPRA inicia ciclo após o encerramento total
- **THEN** o novo ciclo volta a participar do resumo com seus próprios valores
- **AND** não herda custo, patrimônio, resultado não realizado ou rentabilidade do ciclo encerrado

### Requirement: Manter conceitos financeiros independentes
O resumo atual SHALL representar somente custo, patrimônio, resultado não realizado e rentabilidade percentual atual das posições abertas e MUST NOT somar resultado realizado ou resultado não realizado novamente ao patrimônio.

#### Scenario: Resultado realizado histórico
- **WHEN** a Carteira possui resultado realizado histórico
- **THEN** ele não é tratado como saldo de caixa nem integra o resumo atual
- **AND** não participa do numerador, do denominador ou da rentabilidade percentual

#### Scenario: Ausência de dupla contagem
- **WHEN** uma posição possui `resultadoNaoRealizado`
- **THEN** o patrimônio utiliza somente `valorAtualPosicao`
- **AND** não soma novamente o resultado não realizado

#### Scenario: Rentabilidade individual
- **WHEN** uma posição possui `rentabilidadePercentual`
- **THEN** o resumo não soma, agrega nem promedia rentabilidades individuais
- **AND** deriva a rentabilidade da moeda exclusivamente dos totais consolidados

### Requirement: Aplicar política numérica do resumo
O sistema SHALL continuar somando `custoTotalPosicoes`, `patrimonioAtual` e `resultadoNaoRealizadoTotal` exclusivamente com `BigDecimal.add`, sem `MathContext` e sem arredondamento intermediário, normalizando somente esses acumulados finais para escala 12 com `RoundingMode.UNNECESSARY` e precisão máxima 38. Depois da consolidação, `rentabilidadePercentual` SHALL usar exclusivamente `BigDecimal`, dividir em escala intermediária 24 com `RoundingMode.HALF_EVEN`, multiplicar o quociente por `100`, normalizar para escala final 6 com `RoundingMode.HALF_EVEN` e validar precisão máxima 38. Essa política SHALL ser exatamente a mesma utilizada pela rentabilidade percentual da posição e MUST NOT possuir implementação concorrente em calculadora de posição, serviço, agregador ou mapper. O sistema MUST NOT usar `float`, `double`, `MathContext`, truncamento ou arredondamento implícito no percentual.

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

### Requirement: Preservar separação por moeda
O sistema MUST acumular BRL e USD independentemente e MUST NOT converter moedas, consultar câmbio ou produzir total monetário único multimoeda.

#### Scenario: Resumo multimoeda
- **WHEN** a Carteira contém posições abertas em BRL e USD
- **THEN** custo, patrimônio, resultado não realizado e rentabilidade de cada item usam somente posições da própria moeda
- **AND** o sistema não soma nem calcula média dos percentuais entre moedas

### Requirement: Consultar sem efeitos colaterais ou integrações externas
A consulta SHALL operar em modo read-only sob `Isolation.REPEATABLE_READ`, reutilizar uma única consolidação oficial de posições e calcular o percentual em O(1) uma vez por moeda após uma única agregação monetária. Ela MUST NOT persistir resumo, adquirir lock pessimista, usar `Clock`, chamar provider externo, criar consulta adicional por indicador, executar segundo replay ou chamar novamente a fonte de posições.

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

### Requirement: Compartilhar a agregação oficial por moeda
As consultas de posições, patrimônio e resumo SHALL preservar os fluxos `CarteiraResource → PosicaoService → CalculadoraPosicao → CalculadoraRentabilidade → PosicaoMapper → PosicaoResponse`, `CarteiraResource → PatrimonioService → PosicaoService → AgregadorPosicoesPorMoeda → PatrimonioMapper → PatrimonioResponse` e `CarteiraResource → ResumoCarteiraService → PosicaoService → AgregadorPosicoesPorMoeda → CalculadoraRentabilidade → ResumoCarteiraMapper → ResumoCarteiraResponse`. Patrimônio e resumo SHALL reutilizar o mesmo `AgregadorPosicoesPorMoeda` puro, e posição e resumo SHALL reutilizar a mesma implementação percentual pura.

#### Scenario: Consulta de patrimônio
- **WHEN** o cliente consulta `GET /patrimonio`
- **THEN** `PatrimonioService` chama `PosicaoService.listarPorCarteira` exatamente uma vez
- **AND** usa o agregador compartilhado sem segundo replay ou query financeira adicional
- **AND** não depende de `CalculadoraRentabilidade`

#### Scenario: Consulta de resumo
- **WHEN** o cliente consulta `GET /resumo`
- **THEN** `ResumoCarteiraService` chama `PosicaoService.listarPorCarteira` exatamente uma vez
- **AND** não chama `PatrimonioService`
- **AND** usa o agregador compartilhado sem segundo replay ou query financeira adicional
- **AND** calcula a rentabilidade exatamente uma vez por moeda com a implementação percentual compartilhada

#### Scenario: Pureza do agregador
- **WHEN** os totais por moeda são calculados
- **THEN** o `AgregadorPosicoesPorMoeda` opera sem banco, repository, service, provider, `Clock`, transação, persistência ou replay financeiro
- **AND** o fetch plan cronológico vigente permanece sem N+1

#### Scenario: Pureza da calculadora percentual
- **WHEN** a rentabilidade da posição ou do resumo é calculada
- **THEN** `CalculadoraRentabilidade` opera sem banco, repository, service, provider, `Clock`, transação, persistência, replay ou estado mutável de domínio
- **AND** `CalculadoraPosicao` não mantém algoritmo percentual concorrente

### Requirement: Preservar o escopo financeiro do resumo atual
A capability SHALL permanecer limitada ao resumo atual por moeda das posições abertas e MUST NOT introduzir resultado realizado, rentabilidade realizada, histórica ou total, patrimônio único multimoeda, conversão cambial, caixa, aportes, resgates, patrimônio histórico, evolução patrimonial, snapshots, TWR, XIRR, dividendos, impostos, scheduler ou cache.

#### Scenario: Consulta dentro do escopo
- **WHEN** o resumo atual é consultado
- **THEN** somente custo, patrimônio, resultado não realizado e rentabilidade percentual atual das posições abertas são consolidados por moeda
- **AND** nenhuma funcionalidade excluída é calculada, persistida ou retornada


