## MODIFIED Requirements

### Requirement: Destinos estruturais carregados sob demanda
Cada área principal SHALL constituir um limite independente de carregamento sob demanda. Enquanto uma área não possuir capability funcional aprovada, ela SHALL apresentar somente um placeholder estrutural. Uma capability posterior explicitamente aprovada MAY substituir o placeholder de sua própria área por comportamento funcional, preservando o shell e o limite lazy sem tornar funcionais as demais áreas.

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
- **WHEN** uma capability aprovada introduz comportamento funcional para uma área principal
- **THEN** somente o placeholder dessa área é substituído, mantendo o limite lazy, o shell e os placeholders das áreas ainda não implementadas
