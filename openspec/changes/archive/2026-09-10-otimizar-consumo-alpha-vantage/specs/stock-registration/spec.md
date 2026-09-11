## MODIFIED Requirements

### Requirement: Seleção da Ação e do provider pelo estado persistido
O sistema SHALL localizar a Ação pelo ID antes de qualquer chamada externa e SHALL usar exclusivamente o ticker e o mercado persistidos para solicitar a nova cotação. O sistema SHALL consultar BRAPI quando o mercado persistido for `BRASIL` e Alpha Vantage quando for `EUA`, respeitando reutilizacao e cooldown de cotacao EUA definidos em alpha-vantage-consumption, sem permitir seleção do provider pelo cliente.

#### Scenario: Ação brasileira persistida
- **WHEN** a Ação encontrada possui `mercado=BRASIL`
- **THEN** o sistema consulta somente a BRAPI usando o ticker persistido

#### Scenario: Ação americana persistida
- **WHEN** a Ação encontrada possui `mercado=EUA`
- **THEN** o sistema reutiliza a cotacao persistida recente ou respeita cooldown ativo; quando uma consulta e necessaria, consulta somente a Alpha Vantage usando o ticker persistido e a identidade ja validada

#### Scenario: Ação inexistente
- **WHEN** o ID informado não corresponde a uma Ação persistida
- **THEN** o sistema responde `404 Not Found` no formato padronizado atual e não consulta provider
