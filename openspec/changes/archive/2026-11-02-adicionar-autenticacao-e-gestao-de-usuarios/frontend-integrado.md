# Integração Angular — recortes A–H

Execução autorizada em 31/10/2026 após aprovação da arquitetura e dos oito recortes. Produção continua expressamente pendente: **“Manter produção pendente; validar Angular localmente”**. Não houve alteração de topologia TLS, confiança em headers encaminhados, ampliação de CORS, regra financeira, migration ou código Java.

## Resultado

Angular standalone 22.1.4, Signals/RxJS e Material existentes. Rotas públicas /login, /definir-senha e /redefinir-senha; gestão ADMIN em /usuarios, /usuarios/novo e /usuarios/:id. Shell bloqueado até validar /auth/me; ADMIN mantém consultas/comandos financeiros e USER somente consultas. Não houve modernização geral dos dashboards.

JWT exclusivamente no cookie HttpOnly do backend, sem Bearer/refresh token. Identidade/perfil/prazos ficam em memória; senhas, códigos, JWT e dados financeiros não são persistidos em Web Storage. Somente intenção de navegação permitida, vínculo de identidade e expiração são guardados em sessionStorage por até 60 minutos. Preferência global legada de carteira removida; seleção somente em memória da sessão.

Cookie automático e XSRF nativo Angular em /api da mesma origem. Handshake antes/depois de login e após logout. Reset público obtém CSRF sem consultar existência de conta nem depender do JWT anterior. Erro CSRF recupera token para submissão manual posterior, sem repetir comando. Login/reset tratam validação, 401, 403, 429/Retry-After e indisponibilidade sem enumerar contas. Os parsers financeiros/lossless existentes permanecem preservados.

Interceptor cancela leituras e descarta respostas/erros de gerações antigas. Encerramento/troca limpa carteira, caches, mutações locais, formulários, diálogos e notificações; shell anterior é destruído antes de liberar nova identidade. Retorno tem consumo único, TTL, vínculo quando identidade anterior é conhecida, revalidação de perfil/carteira e allowlist de rota, fragmento main-content e parâmetros carteiraId/origem. URLs externas, IDs e parâmetros conhecidos malformados são recusados. Filtros locais de custódia não são serializados nem restaurados, conforme aprovação.

Atividade somente por evento confiável em aba visível: pointerdown, keydown sem repeat, input/change, scrollend e navegação humana. Deduplicação por gesto, uma chamada em voo e uma pendente, sem debounce temporal. Movimento de ponteiro, foco, polling e timers não renovam atividade. /me a cada 30s visíveis e ao foco só verifica sessão. Aba oculta é mascarada/inert, incluindo overlays; ao retomar precisa revalidar. Timer consulta estado global antes de decidir por expiração local: B pode manter A ativa. Prazos são ancorados em performance.now() e horário do servidor, sem ampliar dez minutos/quatro horas ou renovar JWT. BroadcastChannel carrega somente tipo, nunca identidade/prazo/credencial; /me permanece autoritativo.

Administração projeta somente os oito campos públicos do DTO, incluindo versao, sem versaoSeguranca. PATCH/reemissão enviam versão esperada e nunca ator no corpo. Mudanças exigem confirmação; conflito exige atualização manual, sem repetir comando; ULTIMO_ADMIN_ATIVO tem orientação própria. Sucesso atualiza DTO/versão. Autoalteração efetiva encerra identidade local; no-op e emissão não encerram. Cadastro não tem senha. Código é mostrado apenas no diálogo de emissão; cópia explícita tem fallback manual. Fechar/navegar/encerrar remove o código; não há recuperação persistida.

## Evidências por recorte

Logs locais ignorados em target/auth-angular; testes permanentes em frontend/src e frontend/test-support. Contagens intermediárias não são somadas nem substituem o aceite final.

| Recorte | Entrega verificada | Evidência |
| --- | --- | --- |
| A | Handshake, restauração deduplicada, geração, erros, corpo financeiro preservado | a-01–a-11; a-checkpoint-coverage: 985 testes/84 arquivos, 98,69% linhas e 93,27% ramos naquele checkpoint |
| B | Login público, senha limpa, shell protegido, identidade/logout | b-01–b-04; b-checkpoint e b-04-green: 21 testes em cada subconjunto |
| C | Diretiva/guards ADMIN em todos os comandos financeiros da matriz | c-01–c-05; c-regressao-completa: 1.000 testes/88 arquivos; URL direta adicional em c-05-green |
| D | Retorno/vínculo/TTL, cancelamento, caches, destruição de shell e diálogos | d-01–d-16; d-checkpoint-final: 1.035 testes/90 arquivos |
| E | Eventos, pendência única, prazos, máscara, sinais/fallback | e-01–e-19; e-checkpoint-completo: 1.067 testes/92 arquivos |
| F | Definição/reset públicos, política 8 caracteres/72 bytes, CSRF, erros, sem login automático | f-01–f-07; f-checkpoint-completo: 1.085 testes/93 arquivos |
| G | DTO/lista, cadastro, versão, perfil/situação, código, conflitos/no-op/autoalteração | g-01–g-04, g-06-routes, g-07/g-08; g-checkpoint-completo: 1.107 testes/97 arquivos; ampliações incluídas no aceite |
| H | Destinos inválidos, deduplicação após suspensão/cancelamento, regressão, cobertura/build, HTTP/browser locais | h-01: nove falhas esperadas corrigidas, 51 testes aprovados; h-aceite-coverage, h-build e smoke-http |

g-code-copy-keyboard.log: dois casos de clipboard disponível/indisponível e fechamento por Escape aprovados.

### Evidências TDD e incidentes de harness

REDs por símbolo inexistente documentam ausência da fronteira pública prevista; erros incidentais de compilação não comprovam comportamento. E-12/e-14 iniciais falharam pelo harness (providers HTTP e detecção de mudanças), não pelo requisito; foram corrigidos/verificados sem retirar código correto para fabricar RED. E-11 e e-15/e-16 têm evidências próprias. G-05 exigiu sincronizar fechamento Material e representar StandardError completo na fixture; não foi correção do contrato para satisfazer teste. Perfil, reemissão e cópia também receberam verificações de regressão após a implementação inicial: não alegar RED individual prévio de todos esses ramos. A lacuna histórica de 2.2 permanece aberta.

## Aceite automatizado

Comandos sequenciais na pasta frontend, com VITEST_MAX_WORKERS=1 e NG_BUILD_MAX_WORKERS=2:

~~~powershell
npm test -- --watch=false
npm run test:coverage
npm run build
~~~

Aceite definitivo em **h-aceite-coverage.log: 1.120 testes aprovados, 98 arquivos, zero falhas e zero ignorados**.

| Cobertura V8 global | Resultado |
| --- | --- |
| Linhas | **3.058/3.109 — 98,35%** |
| Ramos | **3.067/3.348 — 91,60%** |
| Statements | 5.613/5.919 — 94,83% |
| Funções | 985/1.047 — 94,07% |

Portões globais 80% linhas/ramos preservados, incluindo produção não importada; nenhuma exclusão, limite ou asserção financeira reduzida. Relatórios HTML, LCOV e JSON em frontend/coverage/frontend. Testes financeiros existentes adaptados com fixtures isoladas de sessão/preferência em memória, sem remover asserções de negócio.

Build de produção aprovado (h-build.log). Bundle inicial **628,38 kB**: aviso de 500 kB, abaixo do erro de 1 MB. Avisos de estilos existentes e inalterados: portfolio-positions **4,15 kB** e portfolio-evolution **5,68 kB**. Budgets não alterados para ocultar avisos.

Não houve nova suíte Maven/JaCoCo completa nesta etapa Angular. Backend preservado; evidência aprovada da etapa 5: **2.555 testes, 2.453 aprovados, 102 opcionais ignorados, 96,04% linhas/84,36% ramos**. São números históricos, não nova execução. Smoke local usa classes já compiladas desse backend.

## Validação real local

Harness descartável target/auth-angular/AngularSmoke.java, profile test, backend somente 127.0.0.1:19187 e H2 único em memória. Fixture ADMIN apenas nesse contexto. Angular loopback 14231, origem exclusiva angular-smoke.localhost e proxy para esse backend. JWT/HMAC de teste independentes/efêmeros do profile test; senha de fixture não é configuração/credencial fixa da aplicação publicada.

No navegador: login ADMIN, restauração após reload, listagem, cadastro com código único sem senha administrativa, fechamento sem recuperar código, bloqueio confirmado com versão atualizada, foco inicial em Cancelar, logout e página pública com erros por campo sem shell. Capturas só de fixtures em administracao-local.jpg e definicao-local.jpg. Código gerado não foi impresso nem incluído nas capturas.

**smoke-http.log: SMOKE_HTTP_OK**, contra frontend/proxy/backend reais:

- Cookie HttpOnly/SameSite=Lax/Path=/ sem Domain; HTTP excepcional somente test/loopback.
- CSRF ausente/inválido/válido, 401/403/409, 201/Location/no-store.
- Definição pública sem login automático; USER consulta e não grava; no-op sem incremento.
- Mudança efetiva invalida sessão anterior; logout nega cookie antigo.
- Leituras financeiras/me/csrf não alteram última atividade.

Cookies/códigos/senhas do smoke ficam apenas em memória/jar do harness; valores não são registrados. O runtime de navegador não expôs document.cookie à inspeção: não alegar leitura direta do cookie jar no teste UI; atributos foram verificados no HTTP real e reload no navegador.

Incidentes ambientais: plugin Maven dependency indisponível offline; reutilizado classpath do Surefire aprovado, sem instalar dependência. Java source-file conflitou com devtools; repetido com restart desabilitado. Sandbox negou loopback do cliente Java; execução isolada foi autorizada pelo revisor automático e repetida com sucesso. Não são falhas funcionais da aplicação. Três processos dos servidores temporários encerrados após conferir PIDs/argumentos; aba temporária fechada. Nenhum provider externo consultado. Liquibase somente no H2 descartável, nunca banco persistente.

## Rastreabilidade

| IDs | Resultado |
| --- | --- |
| 7.1–7.8 | Entregues/verificados: AuthApi/SessionService/interceptor, guards, retorno, Boundary/Coordinator e testes componentes/Router/HTTP |
| 8.1–8.4 | Entregues/verificados: administração, diretiva ADMIN, rotas, regressão financeira e acessibilidade/Material |
| 6.2/6.4/6.6 | Partes de cliente concluídas, combinadas aos serviços/transações/HTTP aprovados nas etapas 4/5; abas/tempo/sinais usam navegador/Clock controlados |
| 6.5 | Parcial: cem eventos com primeira atividade em voo resultam em duas chamadas sequenciais, nenhuma concorrente e sem debounce; planejamento operacional de limpeza/retenção pendente |
| 10.1/10.3 | Integração/regressão Angular, portões de cobertura e build concluídos; backend aprovado/preservado, sem alegar nova suíte Maven |
| 10.5/10.6 | Atualização de grafo, validação estrita e inventário registrados na conferência final |

Cenário → teste: reload/guards em auth.guards/shell-auth.integration; cookies/CSRF em auth-api/session.interceptor e smoke HTTP; intenção/vínculo/TTL em return-navigation/session-isolation; máscara/perfil/identidade em browser-session.integration; prazos/atividade/sinal perdido em session-coordinator; login/Retry-After em login-page; política/reset em password-page; DTO/versionamento/administração em usuarios-api e usuario-create/detail/list; copiar/Escape em one-time-code-dialog; restrições em admin-only e suítes financeiras adaptadas. Regras financeiras verificadas pelas mesmas asserções.

## Pendências e riscos

- **9.4:** produção HTTPS/mesma origem/runtime e aceite Secure pendentes por decisão expressa. Compose/Nginx HTTP versionado não demonstra TLS nem fornece as duas chaves runtime. Sem confiar em X-Forwarded-Proto arbitrário/ampliar CORS. Único ajuste versionado de proxy: changeOrigin=false, preservando Host/Origin locais.
- **10.4:** homologação PostgreSQL da migration 009, constraints/locks/concorrência e testes opcionais pendentes. Nenhum PostgreSQL executado.
- **2.2:** lacuna histórica de RED individual continua pendente, apesar do comportamento backend validado.
- **6.5:** planejamento de retenção/limpeza em revisão própria; negação de expiração/revogação independe de job/apagar linhas.
- **9.1–9.3:** OpenAPI/Postman/README operacional e demonstração externa permanecem pendentes; não declarar change inteira concluída.
- Não houve teste de carga real nem E2E de longa duração com múltiplos navegadores reais. Há testes controlados e smoke HTTP/browser locais, sem alegar concorrência PostgreSQL.
- Avisos de bundle/estilos mantidos visíveis; sem modernização geral dos dashboards.

## Preservação e controle de alterações

Baseline antes da primeira edição: **1.169 arquivos SHA-256**, baseline.json e status-inicial.txt. Backend, migrations, pom, README, ZIP e teste offset-date-time preexistente preservados byte a byte. Inventário contra esse baseline distingue alterações anteriores das desta execução, sem remover arquivos. Staging inicial/final vazios; nenhum git add, commit, push, merge, arquivamento, deploy ou banco persistente.

Conferência final e inventário completo seguem abaixo. Aguardar revisão do solicitante antes de qualquer etapa adicional.

## Conferência final e inventário de arquivos

Graphify atualizado após código/testes: exit 0, 14.277 nós, 30.601 arestas e 931 comunidades. Permanecem avisos de 12 JSONs sem nós, tree_sitter_sql ausente e rótulos de comunidades alterados. Não instalar dependência nem executar extração/rotulagem LLM; atualização de código não equivale a homologação SQL.

OpenSpec --strict: válido. git diff --check: exit 0; apenas avisos de normalização LF/CRLF preexistentes. Revisão adicional dos arquivos novos/modificados: zero espaços finais; 42 arquivos TypeScript de produção revisados, sem JWT/hash BCrypt/chave privada literal ou credencial da fixture de smoke. A busca automatizada complementa a revisão, sem substituir uma auditoria completa de segredos.

Baseline de 1.169 arquivos: 1.118 preservados byte a byte, 51 modificados no escopo e 46 criados; zero removidos e zero fora do escopo. Os 51 incluem 46 arquivos frontend e cinco artefatos OpenSpec; o relatório é um dos 46 novos. Git final: 78 modificados rastreados e 125 não rastreados, incluindo as alterações preexistentes; zero staged. Inventários/status locais em inventario-final.json, resumo-final.json e status-final.txt. Nenhuma operação Git proibida executada.

Tarefas: 49/56 marcadas. Concluídas neste recorte: 6.2, 6.4, 6.6, 7.1–7.8, 8.1–8.4, 10.1 e 10.3. Mantidas pendentes: 2.2, 6.5, 9.1–9.4 e 10.4. A conclusão de 10.5/10.6 já registrada anteriormente é renovada pela conferência atual; não arquivar a change.

### Criados nesta execução

- `frontend/src/app/core/auth/admin-only.directive.spec.ts`
- `frontend/src/app/core/auth/admin-only.directive.ts`
- `frontend/src/app/core/auth/auth-api.service.spec.ts`
- `frontend/src/app/core/auth/auth-api.service.ts`
- `frontend/src/app/core/auth/auth-browser.ts`
- `frontend/src/app/core/auth/auth.guards.spec.ts`
- `frontend/src/app/core/auth/auth.guards.ts`
- `frontend/src/app/core/auth/browser-session.integration.spec.ts`
- `frontend/src/app/core/auth/return-navigation.service.spec.ts`
- `frontend/src/app/core/auth/return-navigation.service.ts`
- `frontend/src/app/core/auth/route-access.ts`
- `frontend/src/app/core/auth/session-boundary.service.ts`
- `frontend/src/app/core/auth/session-coordinator.service.spec.ts`
- `frontend/src/app/core/auth/session-coordinator.service.ts`
- `frontend/src/app/core/auth/session-isolation.integration.spec.ts`
- `frontend/src/app/core/auth/session.interceptor.spec.ts`
- `frontend/src/app/core/auth/session.interceptor.ts`
- `frontend/src/app/core/auth/session.service.spec.ts`
- `frontend/src/app/core/auth/session.service.ts`
- `frontend/src/app/core/auth/shell-auth.integration.spec.ts`
- `frontend/src/app/core/http/retry-after-state.ts`
- `frontend/src/app/features/auth/auth-page.scss`
- `frontend/src/app/features/auth/login-page.component.spec.ts`
- `frontend/src/app/features/auth/login-page.component.ts`
- `frontend/src/app/features/auth/password-page.component.spec.ts`
- `frontend/src/app/features/auth/password-page.component.ts`
- `frontend/src/app/features/auth/password-validators.ts`
- `frontend/src/app/features/usuarios/admin-confirm-dialog.component.ts`
- `frontend/src/app/features/usuarios/one-time-code-dialog.component.spec.ts`
- `frontend/src/app/features/usuarios/one-time-code-dialog.component.ts`
- `frontend/src/app/features/usuarios/usuario-create-page.component.spec.ts`
- `frontend/src/app/features/usuarios/usuario-create-page.component.ts`
- `frontend/src/app/features/usuarios/usuario-detail-page.component.spec.ts`
- `frontend/src/app/features/usuarios/usuario-detail-page.component.ts`
- `frontend/src/app/features/usuarios/usuario.model.ts`
- `frontend/src/app/features/usuarios/usuarios-api.service.spec.ts`
- `frontend/src/app/features/usuarios/usuarios-api.service.ts`
- `frontend/src/app/features/usuarios/usuarios-list-page.component.spec.ts`
- `frontend/src/app/features/usuarios/usuarios-list-page.component.ts`
- `frontend/src/app/features/usuarios/usuarios-page.scss`
- `frontend/src/app/features/usuarios/usuarios.routes.ts`
- `frontend/test-support/auth-browser.ts`
- `frontend/test-support/authenticated-module.ts`
- `frontend/test-support/session-fixture.ts`
- `frontend/test-support/usuario-fixture.ts`
- `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/frontend-integrado.md`

### Modificados em relação ao baseline desta execução

- `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/design.md`
- `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/proposal.md`
- `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/reorganizacao-da-implementacao.md`
- `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/specs/frontend-authentication/spec.md`
- `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/tasks.md`
- `frontend/proxy.conf.json`
- `frontend/src/app/app.component.ts`
- `frontend/src/app/app.config.ts`
- `frontend/src/app/app.routes.spec.ts`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/core/carteira/carteira-context.service.spec.ts`
- `frontend/src/app/core/carteira/carteira-context.service.ts`
- `frontend/src/app/core/carteira/carteira-navigation.service.spec.ts`
- `frontend/src/app/core/carteira/carteira-navigation.service.ts`
- `frontend/src/app/features/acoes/acoes.routes.spec.ts`
- `frontend/src/app/features/acoes/acoes.routes.ts`
- `frontend/src/app/features/acoes/pages/acao-detail-page.component.spec.ts`
- `frontend/src/app/features/acoes/pages/acao-detail-page.component.ts`
- `frontend/src/app/features/acoes/pages/acoes-list-dialog.integration.spec.ts`
- `frontend/src/app/features/acoes/pages/acoes-list-page.component.spec.ts`
- `frontend/src/app/features/acoes/pages/acoes-list-page.component.ts`
- `frontend/src/app/features/carteiras/carteira-context-mutations.spec.ts`
- `frontend/src/app/features/carteiras/carteiras.routes.ts`
- `frontend/src/app/features/carteiras/custodia-historico.component.html`
- `frontend/src/app/features/carteiras/custodia-historico.component.spec.ts`
- `frontend/src/app/features/carteiras/custodia-historico.component.ts`
- `frontend/src/app/features/carteiras/custodia-recarga.integration.spec.ts`
- `frontend/src/app/features/carteiras/pages/carteira-detail-page.component.spec.ts`
- `frontend/src/app/features/carteiras/pages/carteira-detail-page.component.ts`
- `frontend/src/app/features/carteiras/pages/carteiras-dialog.integration.spec.ts`
- `frontend/src/app/features/carteiras/pages/carteiras-list-page.component.spec.ts`
- `frontend/src/app/features/carteiras/pages/carteiras-list-page.component.ts`
- `frontend/src/app/features/corretoras/corretoras.routes.ts`
- `frontend/src/app/features/corretoras/pages/corretoras-list-dialog.integration.spec.ts`
- `frontend/src/app/features/corretoras/pages/corretoras-list-page.component.spec.ts`
- `frontend/src/app/features/corretoras/pages/corretoras-list-page.component.ts`
- `frontend/src/app/features/dashboard/dashboard-page.component.html`
- `frontend/src/app/features/dashboard/dashboard-page.component.spec.ts`
- `frontend/src/app/features/dashboard/dashboard-page.component.ts`
- `frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.html`
- `frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.spec.ts`
- `frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.ts`
- `frontend/src/app/features/operacoes/operacoes.routes.ts`
- `frontend/src/app/features/operacoes/pages/operacoes-list-page.component.spec.ts`
- `frontend/src/app/features/operacoes/pages/operacoes-list-page.component.ts`
- `frontend/src/app/layout/carteira-selector/carteira-selector.component.spec.ts`
- `frontend/src/app/layout/carteira-selector/carteira-selector.component.ts`
- `frontend/src/app/layout/main-layout/main-layout.component.html`
- `frontend/src/app/layout/main-layout/main-layout.component.spec.ts`
- `frontend/src/app/layout/main-layout/main-layout.component.ts`
- `frontend/src/styles.scss`
