## 1. Implementação isolada
- [x] 1.1 Observar RED para a fronteira e seleção de transporte.
- [x] 1.2 Implementar ingresso, troca e verificação de integridade.
- [x] 1.3 Executar testes isolados de autenticação, papéis e regressão HTTPS.

## 2. Ensaio mediante autorização
- [ ] 2.1 Conferir publicação, cookies, login/logout, CSRF, papéis e dashboard no notebook.
- [ ] 2.2 Conferir persistência e retorno a HTTPS no mesmo PostgreSQL.

## Evidências isoladas

- RED observado no Node para seleção, publicação e recibo HTTP ausentes e no Java para a fronteira de transporte ainda inexistente.
- GREEN com 60 testes Maven direcionados. O novo teste usa Tomcat local e H2 em memória, incluindo cookies, CSRF, login/logout, revogação e acesso ADMIN/USER.
- GREEN com 26 testes Node do roteiro e 1 teste de cookies no Chrome 157.0.8103.79 sobre localhost:8081 com perfil e servidor descartáveis.
- Docker Compose v5.5.1 renderizou as quatro combinações com ambiente fictício. PostgreSQL, bootstrap, redes e volume são idênticos entre transportes para cada modo de integração.
- A publicação HTTP foi confirmada no Docker limpo. A retomada após parada e o retorno a HTTPS com o mesmo PostgreSQL permanecem pendentes.

## Ensaio autenticado de 20/11/2026

No Docker limpo, em `sem-integracoes/http-local`, o ensaio funcional terminou com **7 de 7 etapas aprovadas**.

| Etapa | Evidência |
| --- | --- |
| 1. Login ADMIN | Dashboard abriu com o papel ADMIN. |
| 2. Carteiras e históricos | As posições e os históricos das duas carteiras conferiram em conteúdo e ordem. |
| 3. Valores financeiros | Os valores exibidos nas duas carteiras conferiram com a auditoria. |
| 4. Navegação | **Voltar**, no detalhe da operação, devolveu o usuário à lista de Operações com a carteira selecionada ou ao histórico da carteira, conforme a origem. |
| 5. Login USER | Dashboard abriu com o papel USER. |
| 6. Permissões do USER | As duas carteiras ficaram disponíveis para consulta. Os controles administrativos verificados estavam ausentes e `GET /api/usuarios` retornou HTTP 403. |
| 7. Logout | As duas sessões foram encerradas. `GET /api/carteiras` retornou HTTP 401 após cada saída. Após o logout do USER, reabrir um detalhe protegido redirecionou para o login. |

O `check` confirmou o modo offline e o transporte HTTP local. O endpoint de CSRF respondeu HTTP 204 ao final. O ensaio preservou dados financeiros, usuários, permissões e cotações. O item 2.1 permanece aberto para a conferência explícita dos atributos dos cookies no perfil do notebook. Há evidência desses atributos em teste isolado. Uma sondagem a partir de outra máquina também fica em aberto, assim como o retorno a HTTPS do item 2.2. Consulte a [versão usada no ensaio](../../../docs/APRESENTACAO-DOCKER.md#versão-aprovada-e-evidências-de-20112026).
