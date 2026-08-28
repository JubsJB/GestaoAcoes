## ADDED Requirements

### Requirement: Consulta singular de Ação persistida por ticker e mercado
O sistema SHALL expor exclusivamente `GET /acoes/por-ticker?ticker={ticker}&mercado={mercado}` para consultar uma única Ação persistida usando conjuntamente o ticker e o mercado. Ambos os query parameters SHALL ser obrigatórios no contrato. O ticker SHALL aceitar a representação normalizável vigente, o mercado SHALL ser limitado a `BRASIL` ou `EUA`, e a consulta MUST NOT escolher implicitamente um mercado quando o mesmo ticker existir em ambos. O sistema SHALL NOT expor aliases nem transformar `GET /acoes` em consulta singular.

#### Scenario: Ação brasileira encontrada
- **WHEN** o cliente solicita `GET /acoes/por-ticker` com ticker válido e `mercado=BRASIL` correspondentes a uma Ação persistida
- **THEN** o sistema responde `200 OK` com a Ação brasileira correspondente

#### Scenario: Ação americana encontrada
- **WHEN** o cliente solicita `GET /acoes/por-ticker` com ticker válido e `mercado=EUA` correspondentes a uma Ação persistida
- **THEN** o sistema responde `200 OK` com a Ação americana correspondente

#### Scenario: Mesmo ticker em mercados distintos
- **WHEN** existem Ações com o mesmo ticker normalizado em `BRASIL` e `EUA`
- **THEN** o mercado informado seleciona somente a Ação da combinação correspondente, sem preferência implícita ou retorno arbitrário

#### Scenario: Ticker normalizável
- **WHEN** o ticker possui espaços nas extremidades ou letras minúsculas, mas é válido segundo a política vigente
- **THEN** o sistema aplica `trim`, converte para maiúsculas de forma independente de locale e consulta o valor normalizado preservando os caracteres internos

#### Scenario: Ticker inválido
- **WHEN** o ticker é nulo, vazio, contém somente espaços ou excede o tamanho máximo vigente após normalização
- **THEN** o sistema responde `400 Bad Request` com código `TICKER_INVALIDO` sem consultar dados persistidos

#### Scenario: Mercado ausente
- **WHEN** o query parameter `mercado` está ausente
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e não consulta dados persistidos

#### Scenario: Mercado inválido
- **WHEN** o query parameter `mercado` não corresponde a `BRASIL` nem `EUA`
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` pelo tratamento centralizado vigente e não consulta dados persistidos

#### Scenario: Combinação inexistente
- **WHEN** ticker e mercado são válidos, mas nenhuma Ação possui a combinação normalizada
- **THEN** o sistema responde `404 Not Found` no formato centralizado vigente de Ação não encontrada

#### Scenario: Ausência de aliases
- **WHEN** o cliente tenta usar `/acoes/ticker/{ticker}`, `/acoes` com ticker e mercado como lookup singular ou `/acoes/por-ticker/{ticker}/mercados/{mercado}`
- **THEN** o sistema não trata essas formas como contratos da consulta singular por ticker e mercado

### Requirement: Resposta equivalente à consulta por ID
A consulta por ticker e mercado SHALL reutilizar integralmente o contrato completo de resposta usado por `GET /acoes/{id}` e MUST NOT criar representação reduzida nem expor a entidade de persistência.

#### Scenario: Representação da Ação encontrada
- **WHEN** uma Ação é encontrada pelo ticker normalizado e mercado
- **THEN** a resposta contém id, ticker, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação conforme persistidos

### Requirement: Consulta local e read-only
A consulta por ticker e mercado SHALL usar exclusivamente dados persistidos, SHALL NOT modificar a Ação e MUST NOT consultar BRAPI, Alpha Vantage ou qualquer outro provider. A consulta MUST NOT revalidar ticker externamente nem atualizar nome, moeda, cotação ou data/hora da cotação.

#### Scenario: Consulta encontrada sem integração externa
- **WHEN** a combinação corresponde a uma Ação persistida
- **THEN** a resposta é produzida somente pelo banco de dados, sem chamada externa e sem escrita

#### Scenario: Consulta inexistente sem integração externa
- **WHEN** a combinação não corresponde a uma Ação persistida
- **THEN** o `404 Not Found` é determinado somente pelo banco de dados, sem chamada externa e sem escrita

### Requirement: Compatibilidade dos contratos existentes
A consulta por ticker e mercado SHALL preservar sem alteração comportamental `POST /acoes`, `GET /acoes`, `GET /acoes/{id}` e `PATCH /acoes/{id}/cotacao` e SHALL NOT introduzir filtros genéricos ou paginação na listagem.

#### Scenario: APIs existentes após a ampliação
- **WHEN** a nova consulta singular estiver disponível
- **THEN** cadastro, listagem, consulta por ID e atualização de cotação mantêm seus contratos, status e representações vigentes
