## MODIFIED Requirements

### Requirement: Manter histórico desacoplado de Operações e consultas atuais
`HistoricoCotacao` SHALL continuar representando observações da cotação corrente efetivamente persistidas, com sua granularidade e semântica temporal vigentes. Ele MUST NOT ser reutilizado como armazenamento de candles OHLC, como fonte do fechamento diário de COMPRA ou como cache obrigatório da consulta histórica externa. O registro de uma nova COMPRA MUST NOT criar, alterar ou inferir registros nessa tabela.

#### Scenario: Compra com fechamento externo
- **WHEN** uma COMPRA obtém o fechamento bruto diário do provider
- **THEN** esse valor é persistido somente como preço da Operação e não cria observação em `HistoricoCotacao`

#### Scenario: Operação com preço diferente
- **WHEN** o preço persistido na Operação difere da cotação corrente ou de observações existentes
- **THEN** o histórico de cotação corrente permanece inalterado

#### Scenario: Consulta da posição atual
- **WHEN** posição, patrimônio ou snapshots usam cotações correntes conforme seus contratos
- **THEN** a nova consulta histórica de fechamento não substitui essa fonte nem altera os cálculos existentes

### Requirement: Não expor consulta pública nesta primeira fatia
A capability de fechamento histórico SHALL ser usada internamente pelo registro de COMPRA e MUST NOT criar endpoint público de histórico ou persistência pública de candles nesta change. APIs existentes de Ação e histórico de cotação corrente SHALL manter seus contratos.

#### Scenario: Consulta das APIs atuais
- **WHEN** clientes usam endpoints existentes fora de `POST /operacoes`
- **THEN** não recebem novo endpoint, campo OHLC ou mudança de semântica do histórico corrente

#### Scenario: Ausência de histórico externo retroativo
- **WHEN** o backend consulta fechamento para uma COMPRA
- **THEN** não preenche retroativamente `historico_cotacao`
