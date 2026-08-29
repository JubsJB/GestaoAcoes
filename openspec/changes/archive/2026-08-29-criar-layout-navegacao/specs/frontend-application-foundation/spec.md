## MODIFIED Requirements

### Requirement: Navegação inicial sem feature de negócio
A foundation SHALL possuir configuração central de rotas suficiente para inicializar e validar o roteamento, mas MUST NOT antecipar rotas ou funcionalidades de negócio de dashboard, corretoras, ações, carteiras, operações, indicadores, snapshots, evolução patrimonial ou gráficos. Essa restrição MUST NOT impedir que capabilities posteriores explicitamente aprovadas introduzam rotas e destinos estruturais para essas áreas, desde que tais estruturas não implementem comportamento de negócio e permaneçam fora da responsabilidade da foundation.

#### Scenario: Rota inicial
- **WHEN** somente a foundation é inicializada, antes de uma capability visual ou funcional posterior
- **THEN** o router resolve uma rota técnica mínima sem carregar rota ou funcionalidade de negócio antecipada

#### Scenario: Evolução estrutural aprovada
- **WHEN** uma capability posterior aprovada introduz rotas estruturais para áreas da aplicação
- **THEN** essas rotas podem compor a aplicação sem significar que CRUD, integração HTTP de domínio, DTOs, services, forms, tabelas, indicadores ou cálculos de negócio foram implementados pela foundation

#### Scenario: Rota desconhecida
- **WHEN** o navegador acessa uma rota não reconhecida
- **THEN** a aplicação aplica o comportamento técnico de fallback definido pela capability vigente sem introduzir funcionalidade de negócio
