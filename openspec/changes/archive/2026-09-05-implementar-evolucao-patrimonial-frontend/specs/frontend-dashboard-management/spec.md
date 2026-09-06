## MODIFIED Requirements

### Requirement: Contratos financeiros autoritativos
Para a Carteira selecionada, o Dashboard SHALL consumir `GET /carteiras/{id}/resumo`, `GET /carteiras/{id}/posicoes`, `GET /carteiras/{id}/resultados-realizados` e, por meio de `frontend-portfolio-evolution`, `GET /carteiras/{id}/evolucao-patrimonial`. O frontend MUST apresentar os valores devolvidos e MUST NOT recalcular preço médio, custo, valor atual, patrimônio, resultado realizado, resultado não realizado, rentabilidade ou evolução. O Dashboard MUST NOT consumir `/patrimonio`; criação de snapshot SHALL ocorrer exclusivamente por ação manual conforme `frontend-portfolio-evolution`.

#### Scenario: Carregamento financeiro
- **WHEN** uma Carteira válida é selecionada
- **THEN** resumo, posições, resultados realizados e evolução dessa Carteira são solicitados sem endpoint financeiro não aprovado

#### Scenario: Valor financeiro autoritativo
- **WHEN** qualquer resposta financeira é apresentada
- **THEN** o valor corresponde ao campo devolvido pelo backend e não a um recálculo do frontend

#### Scenario: Resultado realizado por ação
- **WHEN** resultados realizados são retornados
- **THEN** cada resultado é apresentado por Ação e o frontend não cria um total calculado a partir da coleção

### Requirement: Reload e consistência observável
O Dashboard SHALL oferecer atualização explícita que refaça resumo, posições, resultados realizados e evolução patrimonial para a Carteira selecionada. As respostas MAY ser solicitadas em paralelo; o frontend MUST NOT reconciliar diferenças temporais entre elas por cálculo nem executar retry automático. Atualizar dados MUST NOT criar snapshot; somente a ação explícita “Registrar snapshot” MAY executar o POST correspondente.

#### Scenario: Atualização solicitada
- **WHEN** o usuário aciona “Atualizar dados” com uma Carteira selecionada
- **THEN** as consultas financeiras, incluindo a evolução, são novamente realizadas para o mesmo contexto sem criar snapshot

#### Scenario: Respostas de contextos diferentes
- **WHEN** o usuário troca de Carteira enquanto consultas anteriores estão pendentes
- **THEN** respostas do contexto anterior não substituem nem contaminam o Dashboard atual

