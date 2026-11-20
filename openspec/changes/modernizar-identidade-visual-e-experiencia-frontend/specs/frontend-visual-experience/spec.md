## MODIFIED Requirements

### Requirement: Identidade visual e tokens semânticos
A aplicação SHALL aplicar identidade clara, moderna, minimalista e orientada a dados, com azul como cor principal de marca, navegação e ações, fundos brancos/cinza muito claro, cartões arredondados, bordas discretas e sombras suaves. Verde e vermelho SHALL ser reservados principalmente aos resultados financeiros e estados semânticos. A aplicação MUST usar papéis distintos para marca, `success`, `financial-positive`, `negative`, `error` e `warning`; cores coincidentes MUST NOT tornar esses conceitos semanticamente equivalentes. A semântica SHALL abranger info, ação destrutiva, bordas, superfícies, texto, hover e foco. Valores visuais equivalentes compartilhados SHALL ser centralizados sem renomear tokens apenas por cosmética. A paleta inicial SHALL ser validada por contraste e aprovação visual antes de propagação.

#### Scenario: Identidade oliva aplicada
- **WHEN** shell, autenticação, administração ou página financeira são apresentados
- **THEN** a identidade anteriormente oliva é substituída por azul para marca, navegação e ações principais sobre superfícies claras, com ornamentação subordinada aos dados

#### Scenario: Marca separada de resultado financeiro
- **WHEN** marca, lucro, prejuízo, sucesso, warning ou erro são apresentados
- **THEN** cada conceito usa semântica visual própria e informação adicional à cor

#### Scenario: Contraste das combinações
- **WHEN** texto ou ícone é exibido sobre uma cor da paleta
- **THEN** a combinação atende WCAG 2.2 AA aplicável e superfícies de apoio claras não recebem texto branco quando o contraste for insuficiente

#### Scenario: Ação destrutiva distinguível
- **WHEN** uma ação Excluir é apresentada
- **THEN** seu tratamento comunica destruição por texto e semântica visual própria, distinguindo-a da ação primária de marca sem mudar confirmação ou efeito

#### Scenario: Refinamento de tonalidades
- **WHEN** as tonalidades iniciais são ajustadas após revisão de contraste ou aprovação visual
- **THEN** azul permanece como identidade principal e as combinações são verificadas sem obrigação de conservar hexadecimais exatos

## ADDED Requirements

### Requirement: Cobertura visual integral dos fluxos existentes
A aplicação SHALL apresentar linguagem visual consistente no shell, login, primeiro acesso, redefinição de senha, administração de usuários, dashboard, carteiras, ativos, corretoras, operações e custódia, incluindo formulários em página e diálogo, detalhes, confirmações, carregamento, vazio, ausência de busca, erro, retry e sucesso. A modernização MUST preservar ações, campos, rotas, query params, validações, dados e comportamentos existentes, inclusive diferenças entre ADMIN e USER. MUST NOT acrescentar controles funcionais, métricas não aprovadas ou requisições para preencher o layout. O resumo administrativo explicitamente aprovado SHALL seguir frontend-user-administration, sem ampliar métricas financeiras.

#### Scenario: Fluxo percorrido entre áreas
- **WHEN** o usuário transita entre lista, formulário, detalhe e confirmação em áreas diferentes
- **THEN** encontra hierarquia, espaçamento, superfícies, ações e feedback consistentes sem perder contexto ou funções autorizadas

#### Scenario: Estado não ideal
- **WHEN** uma área apresenta carregamento, ausência, erro ou retry
- **THEN** usa a mesma identidade e conserva a classificação, mensagem e recuperação previstas no fluxo aprovado

#### Scenario: Conteúdo financeiro extenso
- **WHEN** nomes longos, decimais exatos, datas ou frações auditáveis de custódia são apresentados
- **THEN** o conteúdo permanece integralmente consultável e operável a 320 CSS px, sem truncamento de informação necessária, arredondamento adicional ou rolagem horizontal obrigatória

### Requirement: Composição visual de autenticação
As telas de login, definição inicial e redefinição de senha SHALL compartilhar identidade com painel visual de degradês suaves e formas abstratas e formulário em layout dividido no desktop, adaptado a uma coluna no mobile. Decoração SHALL ser dispensável à compreensão e não competir com foco, campos, mensagens ou ações. MUST preservar o fluxo por username/senha e código recebido do administrador, estados de espera, erro, sucesso, primeiro acesso e continuidade de sessão. MUST NOT oferecer cadastro público, login social ou recuperação por e-mail.

#### Scenario: Acesso em desktop e mobile
- **WHEN** o viewport muda entre desktop e mobile
- **THEN** a composição muda de duas áreas para uma coluna sem duplicar campos, controles de foco, anúncios ou requisições

#### Scenario: Confirmação de senha
- **WHEN** o usuário define ou redefine a senha com código autorizado
- **THEN** permanecem os campos, políticas, mensagens, descarte de dados sensíveis e confirmação sem login automático do fluxo existente

### Requirement: Dashboard modular com protagonismo financeiro
O dashboard SHALL organizar indicadores e gráficos existentes em composição modular inspirada em bento grid, com contexto da carteira e ações existentes identificáveis. Os grupos BRL e USD SHALL permanecer separados; evolução, composição, desempenho, posições e resultados realizados SHALL conservar dados, ordem lógica de leitura, alternativas textuais e controles aprovados. MUST NOT acrescentar métricas, conversão cambial, soma entre moedas ou gráficos com dados fictícios.

#### Scenario: Duas moedas
- **WHEN** a carteira possui dados em BRL e USD
- **THEN** cada moeda possui resumo claramente identificado sem consolidado artificial entre elas

#### Scenario: Reflow dos módulos
- **WHEN** a largura disponível diminui ou o texto é ampliado a 200%
- **THEN** os módulos se reorganizam sem altura fixa que corte conteúdo, alteração de significado ou perda de dados e ações

### Requirement: Aprovação visual antes da propagação
A evolução visual SHALL ser submetida a revisão explícita do usuário em amostras desktop/mobile antes de propagação para os grupos seguintes. A aprovação da direção inicial ou da documentação MUST NOT ser tratada como aprovação automática da paleta final, das amostras ou da implementação. Critérios existentes de acessibilidade, equivalência HTTP e bundle SHALL permanecer condições independentes de aceite.

#### Scenario: Fundação proposta
- **WHEN** a primeira amostra de identidade e componentes estiver disponível
- **THEN** são apresentados paleta, tipografia, superfícies, formulário e coleção em desktop/mobile para aprovação antes de aplicação às demais telas

#### Scenario: Aprovação parcial
- **WHEN** uma amostra é aprovada e outra exige ajuste
- **THEN** somente o grupo aprovado pode avançar dentro do escopo autorizado e o grupo pendente não é propagado
