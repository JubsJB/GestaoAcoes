## Purpose

Definir a apresentação consistente, responsiva e acessível da administração de usuários, mantendo os comandos existentes e a separação entre experiência visual e regras de autorização do backend.

## ADDED Requirements

### Requirement: Resumo administrativo da listagem completa
A listagem SHALL apresentar total de usuários, usuários ativos e usuários bloqueados, calculados exclusivamente sobre a resposta completa já retornada pela consulta existente. MUST NOT adicionar requisições, endpoints ou controles de filtro. O resumo SHALL ser omitido durante carregamento e erro; uma consulta vazia bem-sucedida SHALL apresentar contagens zero. Se o contrato passar a retornar subconjunto ou paginação, o resumo MUST NOT ser apresentado como total global sem nova análise.

#### Scenario: Contas carregadas com sucesso
- **WHEN** a consulta completa retorna duas contas ativas e uma bloqueada
- **THEN** o resumo apresenta total 3, ativos 2 e bloqueados 1, sem outra requisição

#### Scenario: Consulta indisponível
- **WHEN** a consulta está pendente ou falha
- **THEN** o resumo fica ausente e o estado existente de carregamento ou erro permanece identificável

#### Scenario: Consulta completa vazia
- **WHEN** a consulta completa retorna uma coleção vazia com sucesso
- **THEN** as três contagens são zero e o estado vazio existente permanece disponível

### Requirement: Coleção administrativa consistente
A listagem de usuários SHALL apresentar username, perfil, situação da conta, situação da senha e ações existentes com hierarquia clara, cabeçalhos associados e padrão visual das demais coleções. Em largura compacta SHALL preservar todos os campos, nomes acessíveis e ações, com uma única representação ativa para teclado e tecnologia assistiva. MUST NOT introduzir busca, filtros, ordenação, paginação ou chamadas adicionais ausentes no fluxo atual.

#### Scenario: Consulta administrativa
- **WHEN** ADMIN carrega a listagem de usuários
- **THEN** identifica conta, perfil, situação e ação de consulta sem depender exclusivamente de cor e sem novos controles funcionais

#### Scenario: Lista compacta e estados
- **WHEN** a coleção é apresentada em largura compacta, vazia, carregando ou com erro
- **THEN** todos os campos permanecem consultáveis quando existentes e estados/recuperação mantêm a semântica e o comportamento atuais

### Requirement: Formulários e comandos administrativos padronizados
Criação e detalhe do usuário SHALL usar padrões globais de cabeçalho, formulário, dados de detalhe, ações e feedback. Comandos existentes de perfil, situação e emissão de código SHALL manter confirmações e restrições atuais. A interface MUST NOT oferecer edição de campos não suportados nem escolha da senha do titular pelo administrador. Acesso e visibilidade SHALL continuar subordinados às permissões existentes.

#### Scenario: Criar conta
- **WHEN** ADMIN abre o cadastro
- **THEN** encontra somente os campos e comandos existentes com labels, erros e ação principal consistentes com os demais formulários

#### Scenario: Comando com confirmação
- **WHEN** ADMIN executa alteração que exige confirmação
- **THEN** o diálogo conserva contexto, confirmação explícita, bloqueios e tratamento de resposta, com contenção e restauração de foco

#### Scenario: Perfil sem autorização
- **WHEN** USER acessa a navegação ou tenta uma rota administrativa
- **THEN** a modernização não revela ações administrativas nem modifica a negação de acesso existente

### Requirement: Código de uso único protegido na apresentação
O diálogo de código autorizado SHALL preservar exibição, cópia, avisos, fechamento e descarte de dados sensíveis definidos pelo fluxo atual. A modernização MUST NOT persistir código, senha ou credenciais para previews, capturas, logs ou conveniência visual, nem reemitir ou reutilizar código ao reorganizar o layout.

#### Scenario: Revisão visual de código
- **WHEN** o diálogo de código é revisado visualmente
- **THEN** as evidências usam fixture fictícia ou redação do valor, preservando o comportamento real de uso único e descarte

#### Scenario: Alteração de largura
- **WHEN** o diálogo muda de dimensão
- **THEN** código e ações continuam alcançáveis sem nova emissão, cópia automática ou alteração de validade
