## RENAMED Requirements

- FROM: `### Requirement: Não expor consulta pública nesta primeira fatia`
- TO: `### Requirement: Separação entre prévia pública e histórico de cotações`

## MODIFIED Requirements

### Requirement: Manter histórico desacoplado de Operações e consultas atuais
`HistoricoCotacao` SHALL continuar representando observações da cotação corrente efetivamente persistidas, com sua granularidade e semântica temporal vigentes. Ele MUST NOT ser reutilizado como armazenamento de candles OHLC, como fonte do fechamento diário retornado pela prévia ou como cache obrigatório da consulta histórica externa. O registro de COMPRA ou VENDA e a consulta de prévia MUST NOT criar, alterar ou inferir registros nessa tabela.

#### Scenario: Compra com fechamento externo
- **WHEN** o cliente consulta a prévia histórica e depois registra COMPRA com preço final informado
- **THEN** a prévia não persiste dados e a Operação persiste somente o preço informado e validado, sem criar observação em `HistoricoCotacao`

#### Scenario: Operação com preço diferente
- **WHEN** o preço persistido na Operação difere da cotação corrente ou de observações existentes
- **THEN** o histórico de cotação corrente permanece inalterado

#### Scenario: Consulta da posição atual
- **WHEN** posição, patrimônio ou snapshots usam cotações correntes conforme seus contratos
- **THEN** a nova consulta histórica de fechamento não substitui essa fonte nem altera os cálculos existentes

### Requirement: Separação entre prévia pública e histórico de cotações
O fechamento histórico SHALL permanecer disponível pelo endpoint independente de prévia já existente, sem consulta pelo POST de Operação. Esta reconciliação MUST NOT criar endpoint genérico de candles, persistência pública de OHLC ou backfill. APIs existentes de Ação e histórico de cotação corrente SHALL manter seus contratos.

#### Scenario: Consulta das APIs atuais
- **WHEN** clientes usam endpoints existentes fora de `POST /operacoes`
- **THEN** preservam a prévia independente existente, sem novo endpoint genérico de candles, campo OHLC ou mudança de semântica do histórico corrente

#### Scenario: Ausência de histórico externo retroativo
- **WHEN** o backend consulta fechamento pelo fluxo independente de prévia
- **THEN** não preenche retroativamente `historico_cotacao`
