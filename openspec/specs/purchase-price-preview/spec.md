# purchase-price-preview Specification

## Purpose
Permitir que clientes consultem, sem efeitos colaterais, o fechamento histórico exato que informa uma COMPRA, mantendo o backend e seus providers como fonte autoritativa.

## Requirements

### Requirement: Consulta REST da prévia de preço de COMPRA
O sistema SHALL expor `GET /operacoes/previa-compra` com os query parameters obrigatórios `ticker`, `mercado` e `dataOperacao` no formato `YYYY-MM-DD`. O sistema SHALL normalizar o ticker, aceitar somente `BRASIL` e `EUA`, exigir uma Ação cadastrada para a combinação canônica e responder `200 OK` com `PreviaPrecoCompraResponse` contendo `ticker`, `mercado`, `moeda`, `dataCotacao` e `precoUnitario`. Valores monetários SHALL usar representação decimal exata e datas SHALL representar data civil sem hora ou timezone.

#### Scenario: Prévia brasileira encontrada
- **WHEN** o cliente consulta uma Ação cadastrada em `BRASIL` e existe fechamento exato na data informada
- **THEN** o sistema responde `200 OK` com moeda `BRL`, a data exata e o fechamento bruto da BRAPI

#### Scenario: Prévia americana encontrada
- **WHEN** o cliente consulta uma Ação cadastrada em `EUA` e existe fechamento exato na data informada
- **THEN** o sistema responde `200 OK` com moeda `USD`, a data exata e o `4. close` bruto da Alpha Vantage

#### Scenario: Parâmetros inválidos
- **WHEN** ticker, mercado ou data está ausente, nulo, malformado, desconhecido ou a data é futura segundo a regra vigente do mercado
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` no formato `StandardError`

#### Scenario: Ação não cadastrada
- **WHEN** ticker e mercado normalizados não identificam uma Ação persistida
- **THEN** o sistema responde `404 Not Found`, não consulta provider e não cadastra Ação

### Requirement: Mesma fonte histórica da criação de COMPRA
A prévia SHALL reutilizar a capacidade vigente de fechamento histórico: BRAPI para `BRASIL`, Alpha Vantage para `EUA`, fechamento bruto não ajustado e correspondência exata com `dataOperacao`. A consulta MUST NOT usar cotação atual, `adjustedClose`, preço manual ou candle de pregão anterior ou posterior. O sistema MUST manter uma única regra de validação do resultado histórico compartilhada entre prévia e criação, sem duplicar parser, classificação de erros ou limite de candles.

#### Scenario: Close bruto prevalece
- **WHEN** o provider apresenta fechamento bruto e fechamento ajustado diferentes
- **THEN** a prévia devolve exclusivamente o fechamento bruto usado pela regra de COMPRA

#### Scenario: Data sem pregão
- **WHEN** não existe candle exato em data classificável dentro da janela disponível
- **THEN** o sistema responde `422 Unprocessable Content` com `COTACAO_HISTORICA_INDISPONIVEL` e não substitui a data

#### Scenario: Data fora do alcance
- **WHEN** a resposta permite determinar que a data antecede o histórico disponível
- **THEN** o sistema responde `422 Unprocessable Content` com `HISTORICO_COTACAO_FORA_DO_ALCANCE`

#### Scenario: Erros do provider
- **WHEN** o provider sinaliza ticker inexistente, limite excedido, resposta inválida, indisponibilidade ou timeout
- **THEN** o sistema preserva respectivamente `404 TICKER_INEXISTENTE`, `429 LIMITE_REQUISICOES_EXCEDIDO`, `502 RESPOSTA_EXTERNA_INVALIDA`, `503 SERVICO_EXTERNO_INDISPONIVEL` ou `504 SERVICO_EXTERNO_TIMEOUT`

### Requirement: Prévia informativa e sem efeitos colaterais
A prévia SHALL ser somente informativa, MUST NOT persistir cotação, Operação ou qualquer outro estado e MUST NOT alterar o contrato de `POST /operacoes`. Uma COMPRA criada posteriormente MUST continuar sem aceitar `precoUnitario` e MUST consultar e validar novamente o fechamento histórico como autoridade final, independentemente do valor anteriormente exibido.

#### Scenario: Consulta não reserva preço
- **WHEN** uma prévia retorna um preço e depois o cliente envia a COMPRA correspondente
- **THEN** o POST consulta novamente o fechamento e usa somente o resultado validado no momento da criação

#### Scenario: Cliente tenta impor preço da COMPRA
- **WHEN** o cliente envia `precoUnitario`, inclusive nulo, em uma COMPRA após obter uma prévia
- **THEN** o POST continua respondendo `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

#### Scenario: Prévia sem mutação
- **WHEN** a consulta termina com sucesso ou erro
- **THEN** nenhuma Ação, cotação, Operação, posição ou snapshot é criado ou modificado
