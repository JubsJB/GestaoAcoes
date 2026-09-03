## MODIFIED Requirements

### Requirement: Destinos estruturais carregados sob demanda
Cada área principal SHALL constituir um limite independente de carregamento sob demanda. Enquanto uma área não possuir capability funcional aprovada, ela SHALL apresentar somente um placeholder estrutural. Capabilities posteriores explicitamente aprovadas MAY substituir os placeholders de suas próprias áreas por comportamento funcional, preservando o shell e cada limite lazy sem tornar funcionais as demais áreas. As capabilities `frontend-broker-management`, `frontend-stock-management`, `frontend-portfolio-management` e `frontend-operation-management` SHALL fornecer respectivamente o comportamento funcional de Corretoras, Ações, Carteiras e Operações, sem transferir essas responsabilidades de domínio ao shell.

#### Scenario: Resolução por limite lazy
- **WHEN** o usuário navega para uma das cinco áreas principais
- **THEN** a rota é resolvida pelo limite lazy configurado para a área e seu conteúdo é exibido dentro do shell

#### Scenario: Placeholder identificável
- **WHEN** uma área ainda não possui capability funcional aprovada
- **THEN** ela apresenta um título principal que identifica a área sem simular funcionalidade de negócio

#### Scenario: Placeholder sem integração
- **WHEN** um destino permanece apenas como placeholder
- **THEN** nenhuma requisição HTTP é realizada e nenhum contrato ou service de domínio é necessário

#### Scenario: Evolução funcional aprovada
- **WHEN** as capabilities aprovadas de Corretoras, Ações, Carteiras e Operações introduzem comportamento funcional em suas áreas
- **THEN** somente esses placeholders são substituídos, mantendo os limites lazy, o shell e o placeholder de Dashboard

