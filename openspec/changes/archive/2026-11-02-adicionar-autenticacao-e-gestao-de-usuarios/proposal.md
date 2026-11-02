## Why

Estado atual em 01/11/2026: tarefa 10.4 aprovada tecnicamente pelo solicitante; 9.4 implementada e homologada localmente em recursos descartáveis, conforme operacional-https.md, aguardando revisão. Todas as 56 tarefas verificadas, sem arquivamento. A homologação HTTPS local não declara prontidão nem autoriza implantação de produção. Os estados e pendências descritos abaixo preservam o histórico anterior.

Antes desta change, o sistema financeiro não identificava usuários nem restringia escritas por perfil. Esta evolução incorpora os conceitos das aulas 13 e 14 e as regras aprovadas pelo solicitante, preservando carteiras compartilhadas e o domínio financeiro.

**Estado posterior às aprovações de implementação:** persistência, backend, administração, definição/redefinição, Angular A–H e fechamento 2.2/6.5/9.1–9.3 aprovados pelo solicitante. 10.4 foi posteriormente autorizada e executada em PostgreSQL 17.11 descartável, com instalação/upgrade, integridade, concorrência, atomicidade, reinícios e descarte comprovados em homologacao-postgresql.md, aguardando revisão. Permanece pendente somente 9.4 (produção HTTPS/runtime). Swagger mantém a limitação Try it out documentada e Postman validado sem bypass; 2.2 mantém a exceção histórica aceita e 6.5 não implementa job. O sistema não está declarado pronto para produção. Os parágrafos de sequência original abaixo registram planejamento histórico e não revogam autorizações posteriores.

A próxima entrega planejada reúne 2.1–2.8, 3.1–3.5 e as partes indispensáveis de servidor da etapa 6, com regressão financeira e fixtures isoladas autenticadas por cookie/CSRF. ADMIN deve manter os contratos financeiros e USER somente consultar, conforme D6; não haverá entrega com APIs públicas ou bloqueio indiscriminado. Administração, redefinição de senha e Angular continuam posteriores. Sequência, pontos de controle e rastreabilidade parcial constam em `tasks.md` e `reorganizacao-da-implementacao.md`; IDs e requisitos permanecem preservados.

## What Changes

- **BREAKING**: todas as APIs de negócio passarão a exigir autenticação; somente ADMIN poderá alterar dados financeiros, inclusive cotações, snapshots, compras, vendas, custódia e estornos.
- Login por username/senha, JWT HS256 e BCrypt; criação segura do primeiro ADMIN, sem cadastro público.
- JWT em cookie HttpOnly, Secure em produção, SameSite=Lax, Path=/ e sem Domain; nenhuma credencial exposta ao JavaScript ou armazenada em Web Storage. Reload restaura identidade/permissões por /auth/me sem registrar atividade.
- Proteção CSRF independente do JWT, integrada entre Spring Security e Angular, incluindo login, confirmação de senha, atividade, logout e demais escritas; Postman/Swagger seguem a mesma proteção.
- Sessão compartilhada entre abas, persistida no PostgreSQL, com dez minutos de inatividade e limite absoluto configurável de quatro horas contado desde login, consistente com exp do JWT e sem renovação automática.
- Logout revoga a sessão compartilhada; eventos administrativos invalidam globalmente as sessões da conta, sem mecanismos redundantes. Sincronização visual entre abas e servidor autoritativo.
- Administração de usuários, proteção do último ADMIN ativo e redefinição por código de uso único de quinze minutos; administrador nunca define ou conhece a senha escolhida pelo usuário.
- Angular com login, redefinição, administração, guards, interceptor, controle de ações por perfil e retorno seguro à rota anterior; limpeza de dados entre sessões.
- TDD e manutenção dos portões globais de 80% de linhas e ramos em ambos os projetos.
- Fora do escopo: propriedade individual de carteiras, cadastro público, MFA, e-mail de recuperação, refresh token e modernização geral. `modernizar-layout-e-dashboards` será uma change separada, não criada aqui.

## Capabilities

### New Capabilities

- `user-authentication`: credenciais, cookie JWT, CSRF, bootstrap e contratos públicos seguros.
- `session-lifecycle`: atividade explícita controlada, sessão entre abas, limites de dez minutos/quatro horas e revogação.
- `role-access-control`: matriz de acesso sobre APIs financeiras compartilhadas.
- `user-administration`: cadastro, consulta, perfil, bloqueio e último administrador.
- `password-reset`: autorização administrativa e consumo seguro de código temporário.
- `frontend-authentication`: restauração após reload, CSRF, sincronização entre abas, navegação protegida e isolamento de estado.

### Modified Capabilities

- `api-documentation`: autenticação por cookie e CSRF, endpoints, clientes e matriz 401/403.
- `api-error-handling`: erros de autenticação e autorização também na cadeia de filtros.
- `frontend-application-shell`: contexto autenticado, preferência limitada à sessão e vazio conforme perfil, substituindo persistência global entre sessões.
- `frontend-portfolio-management`: seleção do detalhe preservada somente na sessão autenticada.
- `portfolio-creation`: Carteiras compartilhadas sem proprietário individual; criação restrita a ADMIN autenticado com CSRF, vedada a USER.

As especificações financeiras permanecem vigentes; a nova política transversal de acesso é definida em `role-access-control`, sem alterar cálculos ou criar operações financeiras novas.

## Impact

Backend Java 17/Spring Boot 4.1.0 em `com.projeto`, cujo BOM local gerencia Security 7.1.0: persistência de usuários/sessões entregue na etapa 1; services, resources e configuração Spring Security com bibliotecas Resource Server para ler JWT do cookie serão implementados no bloco integrado, sem biblioteca JWT redundante ou fallback Bearer que contorne CSRF, conforme D3.1. A migration incremental 009 foi validada em H2 descartável; sua homologação PostgreSQL permanece pendente em 10.4. Os testes HTTP financeiros deverão ser preservados e adaptados para cookies/CSRF no bloco integrado, sem credenciais fixas na aplicação. Angular 22, OpenAPI, Postman e integração operacional completa continuam em suas etapas posteriores, preservando decimais lossless e contratos financeiros.

O PRD e AGENTS ainda tratam autenticação como evolução futura; a solicitação atual autoriza seu planejamento, mas não a associação de carteiras a proprietários. Sua atualização documental será tarefa da implementação aprovada. Referências, inventário e decisões pendentes constam em `design.md` e `references.md`.
