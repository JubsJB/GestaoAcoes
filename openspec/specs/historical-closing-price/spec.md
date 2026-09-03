# historical-closing-price Specification

## Purpose
Fornecer o fechamento bruto diário de uma Ação na data exata de pregão, por meio do provider correspondente ao mercado, com erros distinguíveis sempre que a resposta externa permitir uma classificação honesta.

## Requirements

### Requirement: Consulta de fechamento histórico por mercado e data exata
O sistema SHALL consultar o provider correspondente a `BRASIL` ou `EUA` usando ticker normalizado e `LocalDate`, e SHALL retornar ticker, `dataPregao` e `close` bruto positivo e exatamente representável nos limites monetários da Operação. O resultado MUST corresponder exatamente à data solicitada e MUST NOT ser substituído por cotação atual, `adjustedClose`, candle de outro pregão ou preço manual.

#### Scenario: Fechamento brasileiro exato
- **WHEN** a BRAPI retorna um candle diário do ticker solicitado cuja data é exatamente a data consultada
- **THEN** o sistema retorna exclusivamente o `close` bruto desse candle

#### Scenario: Fechamento americano exato
- **WHEN** a Alpha Vantage retorna em `Time Series (Daily)` a chave `YYYY-MM-DD` exatamente igual à data consultada
- **THEN** o sistema retorna exclusivamente o valor de `4. close` desse candle

#### Scenario: Apenas candle de outro dia
- **WHEN** a resposta não contém candle cuja data seja exatamente a solicitada
- **THEN** o sistema não seleciona pregão anterior ou posterior

#### Scenario: Fechamento inválido
- **WHEN** o candle exato contém `close` ausente, não numérico, não positivo ou fora da precisão aceita
- **THEN** o sistema responde `502 Bad Gateway` com código `RESPOSTA_EXTERNA_INVALIDA`

### Requirement: Consulta histórica oficial da BRAPI
Para `BRASIL`, o sistema SHALL consultar o histórico diário oficial com ticker, `startDate` e `endDate` iguais à data solicitada e intervalo diário. A autenticação e os erros externos SHALL seguir o contrato vigente da BRAPI, e campos de fechamento ajustado MUST ser ignorados.

#### Scenario: Parâmetros de uma única data
- **WHEN** um fechamento brasileiro é solicitado
- **THEN** a consulta usa `symbols=<ticker>`, `startDate=YYYY-MM-DD`, `endDate=YYYY-MM-DD` e `interval=1d`

#### Scenario: Close e adjustedClose divergentes
- **WHEN** o candle exato contém `close` e `adjustedClose` diferentes
- **THEN** o sistema retorna `close` e ignora `adjustedClose`

#### Scenario: Resultado correspondente
- **WHEN** a resposta contém o resultado do ticker esperado e um candle válido exatamente na data solicitada
- **THEN** o sistema retorna o `close` bruto desse candle

#### Scenario: Ticker divergente ou ausente
- **WHEN** o resultado não informa ticker ou informa ticker diferente do solicitado
- **THEN** o sistema responde `502 Bad Gateway` com `RESPOSTA_EXTERNA_INVALIDA`

#### Scenario: Resultados múltiplos incompatíveis
- **WHEN** a resposta contém múltiplos resultados e não permite selecionar inequivocamente um único resultado do ticker solicitado
- **THEN** o sistema responde `502 Bad Gateway` com `RESPOSTA_EXTERNA_INVALIDA`

#### Scenario: Array histórico ausente, nulo ou vazio
- **WHEN** `historicalDataPrice` está ausente, nulo ou vazio sem outro sinal inequívoco do provider
- **THEN** o sistema responde `502 Bad Gateway` com `RESPOSTA_EXTERNA_INVALIDA`

#### Scenario: Candle BRAPI malformado
- **WHEN** um candle necessário à resposta possui data ausente ou inválida, `close` ausente, não numérico, zero ou negativo
- **THEN** o sistema responde `502 Bad Gateway` com `RESPOSTA_EXTERNA_INVALIDA`

#### Scenario: Somente candle de outra data
- **WHEN** a resposta válida contém candle de data diferente e não contém a data exata solicitada
- **THEN** o sistema não usa outro pregão nem fallback e aplica somente a classificação sustentada pela resposta

### Requirement: Consulta histórica compacta da Alpha Vantage
Para `EUA`, o sistema SHALL consultar exclusivamente `TIME_SERIES_DAILY` com `symbol=<ticker>`, `outputsize=compact` e a configuração de API key vigente, e SHALL interpretar `Time Series (Daily)` sem usar `TIME_SERIES_DAILY_ADJUSTED` ou `GLOBAL_QUOTE`. Para analisar a janela, um candle válido SHALL possuir chave parseável como `LocalDate`, data distinta, `4. close` presente, numérico e maior que zero. Estrutura necessária malformada SHALL produzir `502 RESPOSTA_EXTERNA_INVALIDA`.

#### Scenario: Série diária compacta
- **WHEN** um fechamento americano é solicitado
- **THEN** a consulta usa `TIME_SERIES_DAILY`, o ticker solicitado e `outputsize=compact`

#### Scenario: Data exata presente
- **WHEN** a chave exata da data solicitada possui candle válido
- **THEN** o sistema retorna exclusivamente seu `4. close` bruto

#### Scenario: Data anterior à janela retornada
- **WHEN** a data solicitada é anterior à menor data válida e a série contém pelo menos 100 candles diários válidos com datas distintas
- **THEN** o sistema responde `422 Unprocessable Content` com código `HISTORICO_COTACAO_FORA_DO_ALCANCE`

#### Scenario: Data ausente dentro da janela retornada
- **WHEN** a data solicitada está entre a menor e a maior data válidas retornadas, mas não existe candle nessa data
- **THEN** o sistema responde `422 Unprocessable Content` com código `COTACAO_HISTORICA_INDISPONIVEL`

#### Scenario: Data anterior em série curta
- **WHEN** a data solicitada é anterior à menor data válida e a série contém menos de 100 candles válidos com datas distintas
- **THEN** o sistema responde `502 Bad Gateway` com `RESPOSTA_EXTERNA_INVALIDA`, sem concluir se o ativo é recente, a série é parcial ou existe outra limitação

#### Scenario: Candle inválido na série
- **WHEN** a estrutura necessária da série contém chave de data não parseável, data duplicada ou `4. close` ausente, não numérico, zero ou negativo
- **THEN** o sistema responde `502 Bad Gateway` com `RESPOSTA_EXTERNA_INVALIDA`

### Requirement: Classificação dos payloads Alpha Vantage
O sistema SHALL inspecionar `Note`, `Information` e `Error Message` antes de interpretar `Time Series (Daily)`, aplicando somente classificações inequívocas. `Note` ou `Information` inequivocamente de rate limit SHALL produzir `429 LIMITE_REQUISICOES_EXCEDIDO`. `Error Message` inequivocamente indicando ticker ou símbolo inválido SHALL produzir `404 TICKER_INEXISTENTE`. `Information` ou `Error Message` sem classificação inequívoca, série ausente ou vazia sem outro sinal inequívoco e payload malformado SHALL produzir `502 RESPOSTA_EXTERNA_INVALIDA`.

#### Scenario: Note de limite
- **WHEN** `Note` informa inequivocamente limite de requisições
- **THEN** o sistema responde `429` com `LIMITE_REQUISICOES_EXCEDIDO`

#### Scenario: Information de limite
- **WHEN** `Information` informa inequivocamente limite de requisições
- **THEN** o sistema responde `429` com `LIMITE_REQUISICOES_EXCEDIDO`

#### Scenario: Símbolo inválido inequívoco
- **WHEN** `Error Message` informa inequivocamente ticker ou símbolo inválido
- **THEN** o sistema responde `404` com `TICKER_INEXISTENTE`

#### Scenario: Mensagem não classificável
- **WHEN** `Information` ou `Error Message` existe mas não sustenta inequivocamente limite ou ticker inexistente
- **THEN** o sistema responde `502` com `RESPOSTA_EXTERNA_INVALIDA`

#### Scenario: Série ausente ou vazia
- **WHEN** `Time Series (Daily)` está ausente ou vazia sem outro sinal inequívoco
- **THEN** o sistema responde `502` com `RESPOSTA_EXTERNA_INVALIDA`

#### Scenario: Payload Alpha Vantage malformado
- **WHEN** o payload não possui a estrutura necessária para interpretar a série
- **THEN** o sistema responde `502` com `RESPOSTA_EXTERNA_INVALIDA`

### Requirement: Erros históricos e externos padronizados
O sistema SHALL responder `422 COTACAO_HISTORICA_INDISPONIVEL` quando a data estiver dentro do intervalo consultável e não houver fechamento exato, e `422 HISTORICO_COTACAO_FORA_DO_ALCANCE` quando for possível determinar que a data precede a janela disponível. SHALL preservar `404 TICKER_INEXISTENTE`, `429 LIMITE_REQUISICOES_EXCEDIDO`, `502 RESPOSTA_EXTERNA_INVALIDA`, `503 SERVICO_EXTERNO_INDISPONIVEL` e `504 SERVICO_EXTERNO_TIMEOUT` conforme a causa observável.

#### Scenario: Fim de semana ou feriado demonstrável
- **WHEN** a data está dentro da janela consultável e não existe candle exato
- **THEN** o sistema responde `422` com `COTACAO_HISTORICA_INDISPONIVEL`

#### Scenario: Limite do provider
- **WHEN** o provider informa limite de requisições excedido
- **THEN** o sistema responde `429` com `LIMITE_REQUISICOES_EXCEDIDO`

#### Scenario: Timeout
- **WHEN** a consulta excede o timeout configurado
- **THEN** o sistema responde `504` com `SERVICO_EXTERNO_TIMEOUT`

#### Scenario: Indisponibilidade externa
- **WHEN** o provider está indisponível ou sua integração não está configurada
- **THEN** o sistema responde `503` com `SERVICO_EXTERNO_INDISPONIVEL`

#### Scenario: Ticker inexistente distinguível
- **WHEN** o provider informa de modo inequívoco que o ticker não existe
- **THEN** o sistema responde `404` com `TICKER_INEXISTENTE`

### Requirement: Capability separada da cotação corrente
A consulta de fechamento histórico SHALL permanecer conceitualmente separada da cotação corrente, MUST NOT alterar `Acao.cotacaoAtual` ou seu histórico de observações e MUST NOT exigir persistência de candles OHLC.

#### Scenario: Consulta sem efeito colateral
- **WHEN** um fechamento histórico é consultado com sucesso ou erro
- **THEN** nenhuma cotação corrente, observação histórica ou entidade de domínio é criada ou modificada por essa consulta
