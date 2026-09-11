## MODIFIED Requirements

### Requirement: Dashboard funcional e contextual por carteira
A aplicação SHALL substituir o placeholder de `/dashboard` por uma página funcional carregada no shell e SHALL usar uma carteira selecionada como contexto exclusivo dos indicadores. A página SHALL consumir o contexto global do shell, sem seletor local próprio nem listagem duplicada de Carteiras, aplicando a precedência e o fallback especificados em frontend-application-shell.

#### Scenario: Nenhuma carteira disponível
- **WHEN** a listagem de Carteiras retorna vazia
- **THEN** o Dashboard apresenta estado vazio e ação para cadastrar uma Carteira sem solicitar indicadores financeiros

#### Scenario: Única carteira disponível
- **WHEN** a listagem retorna exatamente uma Carteira e a URL não contém seleção explícita
- **THEN** essa Carteira é selecionada automaticamente por não haver ambiguidade e seus dados financeiros são carregados

#### Scenario: Múltiplas carteiras sem seleção
- **WHEN** a listagem retorna duas ou mais Carteiras e a URL não contém seleção explícita
- **THEN** o Dashboard usa a seleção global válida ou o fallback por id ASC e carrega somente seus indicadores

#### Scenario: Seleção explícita
- **WHEN** o usuário seleciona uma Carteira disponível no shell
- **THEN** somente os indicadores dessa Carteira são carregados e apresentados

#### Scenario: Troca de carteira
- **WHEN** o usuário troca a Carteira selecionada no shell
- **THEN** os dados anteriores deixam de ser apresentados como atuais e o Dashboard carrega o novo contexto

### Requirement: Seleção persistida na URL
O Dashboard SHALL representar a seleção atual pelo query parameter `carteiraId`, SHALL restaurar uma seleção válida após acesso direto ou reload e SHALL tratar valores inválidos ou inexistentes sem quebrar a página nem consultar um identificador não validado contra a coleção disponível.

#### Scenario: Query parameter válido
- **WHEN** `/dashboard?carteiraId={id}` referencia uma Carteira retornada pela listagem
- **THEN** essa Carteira é selecionada e seus dados financeiros são carregados

#### Scenario: Query parameter ausente
- **WHEN** o Dashboard é acessado sem `carteiraId`
- **THEN** aplica-se a precedência do contexto global e a seleção válida resolvida é representada em carteiraId sem criar ciclo de navegação

#### Scenario: Query parameter malformado
- **WHEN** `carteiraId` não representa um identificador positivo válido
- **THEN** o Dashboard apresenta estado tratável de seleção inválida sem falha de runtime nem consulta financeira para esse valor

#### Scenario: Carteira indicada não existe mais
- **WHEN** `carteiraId` não corresponde a qualquer Carteira retornada
- **THEN** o Dashboard informa que a seleção não está disponível e permite escolher outra Carteira

### Requirement: Experiência acessível do Dashboard
O Dashboard SHALL possuir título principal, seções hierárquicas, identificação clara da Carteira ativa e label acessível para seleção no shell, nomes acessíveis para ações e estados dinâmicos anunciados sem deslocar foco indevidamente. Resultado positivo e negativo MUST NOT ser distinguido somente por cor.

#### Scenario: Uso por tecnologia assistiva
- **WHEN** a página é percorrida por tecnologia assistiva
- **THEN** seleção, cards, posições, resultados, loading e erros possuem estrutura e nomes compreensíveis

#### Scenario: Resultado com semântica não cromática
- **WHEN** um resultado positivo ou negativo é apresentado
- **THEN** sinal e texto comunicam seu significado independentemente da cor

#### Scenario: Interação por teclado
- **WHEN** o usuário opera a página apenas por teclado
- **THEN** seletor global, retry, reload e navegação possuem foco visível e ordem coerente
