# role-access-control Specification

## Purpose

Aplicar menor privilégio às APIs financeiras compartilhadas e à administração, preservando integralmente as regras de negócio existentes.

## Requirements

### Requirement: Carteiras compartilhadas e consultas autenticadas
O sistema SHALL permitir a USER e ADMIN consultar carteiras, ações, corretoras, operações, distribuição, custódia, dashboards e relatórios existentes, sem proprietário por carteira. Toda consulta de negócio SHALL exigir sessão válida. MUST NOT criar endpoints de relatórios ou CRUD ainda inexistentes apenas para cumprir a matriz.

#### Scenario: Dois usuários consultam a mesma carteira
- **WHEN** duas identidades autorizadas consultam uma carteira existente
- **THEN** ambas veem os mesmos dados compartilhados, sem filtro por proprietário

#### Scenario: Consulta anônima
- **WHEN** cliente sem sessão válida consulta qualquer API financeira
- **THEN** recebe 401 sem dados financeiros

### Requirement: Escrita financeira exclusiva de ADMIN
O sistema SHALL aplicar no backend a matriz completa do design D6, restringindo todas as escritas financeiras a ADMIN antes de executar efeitos externos ou persistência. Isso inclui cadastro de carteiras/ações/corretoras/operações, alteração/exclusão de carteira, atualização individual/em lote de cotações, snapshots, movimentações e estornos de custódia. Consultas auxiliares SHALL permanecer consultas sem conceder privilégio financeiro.

#### Scenario: USER força escrita direta
- **WHEN** USER envia request com sessão e CSRF válidos a qualquer escrita inventariada, inclusive cotação, snapshot ou estorno
- **THEN** recebe 403 sem alteração financeira nem chamada de provider de escrita

#### Scenario: ADMIN registra movimentação ou operação
- **WHEN** ADMIN com sessão válida e CSRF válido executa escrita
- **THEN** a operação segue validação financeira vigente, incluindo locks, precisão e histórico, com mesmos resultados e erros de domínio

### Requirement: Administração e fechamento por padrão
O sistema SHALL reservar todas as APIs de usuários a ADMIN, inclusive leitura, e negar superfícies não classificadas. Exceções de autenticação SHALL limitar-se a GET /auth/csrf, login, confirmação de redefinição, documentação/assets e preflight técnico; POSTs públicos SHALL continuar protegidos por CSRF; /auth/me, atividade e logout exigem autenticação. Métodos de escrita SHALL ter justificativa de sua proteção em nível de método conforme design D6.

#### Scenario: USER consulta administração
- **WHEN** USER acessa GET /usuarios ou uma subrota administrativa
- **THEN** recebe 403 sem dados de contas

#### Scenario: Nova rota não classificada
- **WHEN** uma rota não está na allowlist de acesso
- **THEN** sua presença não a torna pública nem uma consulta de negócio automaticamente permitida

#### Scenario: Documentação pública
- **WHEN** visitante acessa Swagger/OpenAPI
- **THEN** pode ler contratos, mas executar operação de negócio exige cookie JWT válido e respeita o perfil; mutações também exigem CSRF

#### Scenario: Sem atalho de segurança para ferramentas
- **WHEN** Postman ou Swagger tenta escrita sem CSRF válido, mesmo com cookie ADMIN
- **THEN** recebe 403 CSRF_INVALIDO sem executar efeito e sem exceção por tipo de cliente
