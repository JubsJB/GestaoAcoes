## MODIFIED Requirements

### Requirement: Venda e cronologia permanecem autoritativas no backend
O frontend MUST NOT calcular saldo autoritativo, preço médio, lucro, elegibilidade de venda ou replay. SHALL preservar formulário e contexto diante de rejeição consolidada, saldo da corretora/grupo nulo, agravamento histórico ou custódia ativa invalidada. SHALL apresentar mensagens específicas com evento/grupo/data e detalhes normalizados, sem sucesso, mudança automática de corretora/data ou retry de escrita. Tipos financeiros SHALL continuar COMPRA/VENDA e consultas de preço permanecer independentes.

#### Scenario: Posição insuficiente
- **WHEN** o backend responde `409 POSICAO_INSUFICIENTE`
- **THEN** a interface explica a posição insuficiente, preserva a entrada e não apresenta sucesso

#### Scenario: Corretora insuficiente
- **WHEN** POST retorna 409 SALDO_CORRETORA_INSUFICIENTE
- **THEN** explica saldo do grupo informado ou sem corretora, preserva entrada e permite correção manual

#### Scenario: Conflito histórico ou custódia invalidada
- **WHEN** POST retorna CONFLITO_HISTORICO_QUANTITATIVO ou MOVIMENTACAO_CUSTODIA_INVALIDADA
- **THEN** mostra evento/data afetados sem propor saneamento automático

#### Scenario: Falha transitória
- **WHEN** POST retorna 409 CONFLITO_TRANSACIONAL_TRANSITORIO
- **THEN** informa conflito temporário, não repete escrita e mantém formulário
