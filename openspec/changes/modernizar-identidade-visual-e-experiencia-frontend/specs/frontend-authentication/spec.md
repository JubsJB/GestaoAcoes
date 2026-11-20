## MODIFIED Requirements

### Requirement: Telas e ações conforme perfil
O Angular SHALL oferecer login, redefinição e gestão ADMIN com a identidade visual global vigente, proteger rotas e ocultar/desabilitar escritas para USER, inclusive cotação, snapshot, custódia e estorno. A modernização da apresentação SHALL preservar integralmente ações, validações, fluxos de autenticação e senha, permissões e sessões; mudanças visuais no dashboard e na navegação SHALL seguir os requisitos globais de experiência visual sem alterar comportamento de segurança.

#### Scenario: USER abre formulário por URL
- **WHEN** USER tenta navegar diretamente a uma rota de escrita ou administração
- **THEN** guard nega acesso e mantém destino consultável, além de backend negar request forçado

#### Scenario: ADMIN gerencia conta
- **WHEN** ADMIN abre gestão de usuários
- **THEN** pode cadastrar, consultar, mudar perfil/situação e copiar código autorizado, sem campo para escolher senha do titular

#### Scenario: Sessão em verificação na nova apresentação
- **WHEN** a sessão precisa ser confirmada antes de liberar conteúdo ou após retomar uma aba
- **THEN** a nova apresentação conserva o bloqueio e mascaramento do conteúdo protegido e overlays, sem expor dados ou permitir ações antes da confirmação
