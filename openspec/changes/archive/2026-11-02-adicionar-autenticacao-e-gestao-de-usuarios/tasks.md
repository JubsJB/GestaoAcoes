Estado atual em 01/11/2026: 56/56 tarefas concluídas e verificadas. 10.4 aprovada tecnicamente; 9.4 homologada localmente em recursos descartáveis, aguardando revisão conforme operacional-https.md. Sem deploy público ou declaração de prontidão de produção. Preservados abaixo os registros históricos de sequência e pendências anteriores.

Etapa 1, backend integrado, administração, definição/redefinição de senha e Angular A–H aprovados tecnicamente; evidências históricas nos relatórios próprios. Fechamento de 2.2, 6.5 e 9.1–9.3 aprovado pelo solicitante. 10.4 executada e verificada em PostgreSQL 17.11 exclusivamente descartável, conforme homologacao-postgresql.md, aguardando revisão. Marcação atual: 55/56 tarefas concluídas; pendente somente 9.4. 2.2 mantém exceção processual aceita, sem RED histórico fabricado; 6.5 permanece plano/medição, sem job. Produção HTTPS/runtime continua pendente, sem declaração de prontidão de produção. As sequências e os parágrafos de progresso anteriores abaixo preservam o histórico; esta marcação expressa o estado atual, sem renumerar IDs.

Para cada comportamento na execução futura: consultar cenário aprovado, escrever um teste na fronteira pública aprovada, registrar RED pelo motivo esperado, implementar mínimo, confirmar GREEN e revisar/refatorar com testes verdes. Não fazer todos os testes primeiro nem inventar RED de documentação. Rastrear cenário → teste → evidência de implementação.

## Sequência integrada e pontos de controle

Os números abaixo continuam identificando grupos de responsabilidades, não versões independentes publicáveis. Nenhum ID é renumerado ou duplicado; esta sequência referencia os checkboxes existentes.

1. **Preparação:** reconfirmar Git/baseline, BOM/APIs e fronteiras TDD; trabalhar 2.1, 2.2 e 2.5 com fixtures isoladas. Usuários e segredos de teste não serão credenciais fixas nem contas automáticas da aplicação.
2. **Integração de autenticação e acesso:** desenvolver 2.3–2.4, 2.6–2.8 em conjunto com 3.1–3.5 e as fixtures de 10.1. Handlers de 3.4 acompanham a cadeia desde sua integração. Preservar testes HTTP financeiros e adaptar autenticação por cookie e CSRF, sem desligar filtros ou reduzir suas verificações. Ponto de controle: anônimo negado; USER consulta e não escreve; ADMIN mantém operações e erros financeiros conforme D6, sem efeitos nos caminhos negados. Nenhuma API financeira pública ou bloqueada indiscriminadamente será aceita como solução definitiva.
3. **Sessão e inatividade efetivas no servidor:** completar 6.1 e 6.3 e os cenários de servidor de 6.2, 6.4 e 6.6. Ponto de controle: POST /auth/atividade válido persiste somente atividade da própria sessão; dez minutos exatos de inatividade e quatro horas absolutas negam acesso; GET/polling/me/csrf não renovam; atividade tardia não ressuscita SID; revogação e concorrência permanecem efetivas. Não aceitar sessão limitada a dez minutos desde login por ausência do protocolo de atividade.
4. **Aceite integrado:** executar as verificações de backend de 10.1–10.2 e 10.5–10.6, incluindo regressão financeira, cobertura existente preservada e portões globais, rastreabilidade, Graphify após código, OpenSpec strict e diff/status. Nenhuma falha financeira será aceita como consequência normal da segurança. Apresentar relatório e aguardar revisão, sem deploy, commit, push, merge ou arquivamento.

Para tarefas que também abrangem etapas posteriores, registrar no relatório futuro, por ID, os cenários entregues, testes/evidências e cenários ainda pendentes. Manter o checkbox desmarcado enquanto a redação integral não for cumprida. A matriz de partes de 6.2/6.4/6.6 e dos controles transversais está em reorganizacao-da-implementacao.md. Esta reorganização não conclui tarefas nem dispensa as dependências 4.x/5.x/7.x/8.x/9.x e a homologação PostgreSQL 009 em 10.4.

## 1. Aprovação, estrutura de usuários e persistência

- [x] 1.1 Registrar autorização explícita do pedido de 30/10/2026 para o recorte de persistência da etapa 1 e suas fronteiras públicas de teste, mantendo etapas 2–10 sem autorização de execução.
- [x] 1.2 Reconfirmar estado do repositório, Boot 4.1.0/BOM Security 7.1.0 e última migration; preservar mudanças locais e registrar baseline isolado/contratos financeiros.
- [x] 1.3 Criar e validar Usuario/Papel, UsuarioRepository e UsuarioResponse conforme as convenções do backend; verificar normalização e unicidade do username e serialização do DTO sem senha/hash, por testes de modelo, persistência em H2 descartável e contrato do DTO. A responsabilidade de serviço funcional permanece nas tarefas 4.1–4.5, concluídas nos recortes próprios; não é atribuída à persistência desta tarefa.

- [x] 1.4 Preparar migration YAML incremental de usuários, singleton e auditoria, com constraints/índices; validar em H2 descartável sem executar banco persistente ou alterar migrations 001–008.
- [x] 1.5 Criar modelo/migration incremental de sessões, autorizações e contadores de abuso, com índices e UTC, sem atualizar entidades financeiras ou vincular carteiras a usuários.

## 2. Autenticação JWT e sessão básica

- [x] 2.1 Adicionar dependências geridas pelo BOM e configuração validada de segredos runtime; testar falha de configuração insegura sem logar valores.
- [x] 2.2 Implementar BCrypt e login em ciclos separados: credencial válida, incorreta, inexistente, bloqueada, não inicializada e senha acima de 72 bytes UTF-8; usar resultados independentes.

Conclusão de 2.2 por exceção processual explicitamente aceita pelo solicitante em 31/10/2026: comportamento completo verificado por testes independentes de serviço/HTTP, sem evidência histórica de RED individual anterior à implementação para todos os negativos. Redação original preservada acima; não se afirma cumprimento de todos os ciclos RED/GREEN. Aceitação, justificativa, comportamentos e testes em fechamento-documentacao.md; evidências históricas em backend-integrado.md. Não inventar ciclo retroativo nem remover proteção correta para fabricar RED.

- [x] 2.3 Implementar emissão/validação JWT exclusivamente em cookie HttpOnly e /auth/me com testes HTTP de cookie real, assinatura/algoritmo/issuer/audience/exp e sid/usuário/versão; não devolver JWT em JSON ou aceitar Bearer alternativo.
- [x] 2.4 Implementar sessão persistida compartilhada entre abas, verificação por request e logout com revogação individual; testar reinício, 503 e invalidação global por versão sem varredura redundante de sessões.
- [x] 2.5 Implementar bootstrap one-shot por segredo runtime com exclusão concorrente; verificar execução única, reexecução inofensiva e nenhum endpoint de bootstrap.
- [x] 2.6 Implementar limites de login, mensagens genéricas/no-store, cookie Secure em produção, SameSite=Lax, Path=/ e sem Domain; validar TLS e mesma origem/CORS restrito.

- [x] 2.7 Implementar handshake GET /auth/csrf e contrato de backend Spring Security 7.1.0 SPA/Angular XSRF, sem JSESSIONID; usar filtro cookie/JwtAuthenticationProvider sem exclusão implícita da DSL Resource Server, token separado e CSRF obrigatório nas mutações, incluindo login/atividade/logout; testar rotação e cookie ADMIN + Bearer sem CSRF ainda negado. Preservar a obrigatoriedade para reset, cujo endpoint e teste funcional pertencem a 5.2; não criar fluxo vazio nem alegar cobertura de reset nesta tarefa. A integração do cliente Angular permanece em 7.1–7.2.
- [x] 2.8 Testar reload com cookie/sessão válidos e /me sem atividade; cookie ausente/inválido/expirado/revogado, Max-Age versus exp e ausência de JWT em JavaScript/Web Storage/body.

## 3. Autorização transversal e erros

- [x] 3.1 Criar matriz HTTP sobre todos os mappings reais com cookie e CSRF válidos conforme caso; cobrir 200/401/403 sem efeitos, inclusive lote/PATCH de cotação, snapshot e estorno.
- [x] 3.2 Aplicar allowlist, default deny, @EnableMethodSecurity e @PreAuthorize conforme D6, com justificativa de fronteira por método; manter segurança fora do protocolo transacional da carteira.
- [x] 3.3 Garantir que GET auxiliar liberado não persiste cotação financeira; manter leitura compartilhada sem proprietário e sem novos endpoints fictícios.
- [x] 3.4 Implementar EntryPoint/DeniedHandler/CSRF com StandardError, 401 de autenticação, 403 ACESSO_NEGADO e 403 CSRF_INVALIDO; testar precedência CSRF quando ambos faltam e erros financeiros preservados.
- [x] 3.5 Verificar paths diretos e proxy /api, allowlist de /auth/csrf e documentação; Postman/Swagger e header alternativo não contornam autenticação ou CSRF.

## 4. Gestão administrativa de usuários

A responsabilidade de serviço funcional antes agrupada em 1.3 é entregue separadamente nas tarefas abaixo, por TDD nas fronteiras públicas aprovadas e integração HTTP. Reutilizadas entidades, repositories e DTO seguro da etapa 1, sem recriar persistência. Nenhum serviço funcional é atribuído à conclusão de 1.3. Consultas e PATCHs foram entregues na etapa 4; 4.2 foi concluída com o código inicial na etapa 5. O DTO expõe versao, mantendo versaoSeguranca interna. PATCHs exigem versaoEsperada, validada antes do no-op, e retornam 200 com DTO atualizado. No-op não incrementa versões nem invalida sessões ou gera auditoria. Decisões e evidências nos relatórios das etapas 4/5 e design D4/D7.

- [x] 4.1 Implementar por TDD o serviço funcional de consulta/listagem de usuários e integrá-lo a GET /usuarios e /usuarios/{id} para ADMIN, reutilizando o DTO seguro; verificar ausência de segredos nas respostas e acesso de USER negado.
- [x] 4.2 Implementar por TDD o cadastro na fronteira pública do serviço funcional de usuários, sem senha administrativa, com conta não inicializada e contrato de código inicial integrado à etapa 5; reutilizar a persistência da etapa 1 e verificar normalização e unicidade, inclusive concorrente, pelo serviço.
- [x] 4.3 Implementar mudança de perfil, bloqueio/habilitação e versão esperada com 409; invalidar globalmente por versaoSeguranca, revogar códigos no bloqueio e manter auditoria mínima sem subsistema avançado.
- [x] 4.4 Implementar proteção do último ADMIN operacional, inclusive autoalteração e dois administradores concorrentes; usar estado real e ordem de locks D7.
- [x] 4.5 Testar bloqueio seguido de habilitação, códigos pendentes invalidados e tokens antigos negados; não oferecer DELETE ou atualização de senha por ADMIN.

## 5. Redefinição e primeira definição de senha

- [x] 5.1 Implementar emissão administrativa de código CSPRNG, HMAC com chave própria e validade 15min; testar exibição única, ausência em listagens/logs e substituição de código pendente.
- [x] 5.2 Implementar confirmação pública com CSRF, senha/confirmacao, BCrypt e consumo+incremento de versão atômicos; testar invalidação em todas as abas/navegadores, sem emitir cookie autenticado antes de login separado.
- [x] 5.3 Implementar cenários de senha divergente, código incorreto, cinco tentativas, expiração exata, reuso e conta bloqueada; garantir persistência das tentativas no caminho de erro.
- [x] 5.4 Implementar rate limiting persistido por username+IP/IP e respostas uniformes contra enumeração; cobrir identidade inexistente e 429 com Retry-After.
- [x] 5.5 Testar consumo duplo, emissão versus consumo, reset versus login/bloqueio e rollback integral sob falha de persistência, sem recuperar hash/código por endpoint.

Contratos aprovados e evidências A–E em etapa-5-redefinicao.md: POST /usuarios retorna 201/Location e envelope usuario/autorizacao; POST /usuarios/{id}/redefinicoes exige versaoEsperada e retorna 200/envelope; POST público /auth/redefinicoes/confirmar retorna 204 sem JWT, exige CSRF e aceita cookie antigo. Emissão incrementa versão otimista; consumo incrementa versão de segurança. Limites 10/50 incluem payloads inválidos na camada apropriada. Esta conclusão não altera pendências de cliente ou homologação PostgreSQL.

## 6. Inatividade e consistência de sessões

- [x] 6.1 Implementar atividade explícita com CSRF, Clock e lock/update condicional; aferir hora após espera e testar imediatamente antes da borda/exatamente em dez minutos, sem graça temporal.
- [x] 6.2 Testar que polling/me/csrf/timers não registram atividade, timestamp do cliente é ignorado, eventos significativos são deduplicados e atividade tardia não revive sid.
- [x] 6.3 Testar quatro horas absolutas configuráveis desde login com exp JWT/registro/Max-Age consistentes; cobrir exemplo 08h00/08h40/12h00, alteração de configuração sem prolongar sessão emitida, sem refresh/renovação.
- [x] 6.4 Testar concorrência atividade/expiração/logout/versão, login versus bloqueio/reset e request previamente autorizado; logout A deve negar cookie antigo de B mesmo se perder BroadcastChannel.
- [x] 6.5 Planejar limpeza de expirados sem depender dela para negar acesso e medir carga de eventos significativos/uma chamada em voo por aba, sem throttle temporal ou perda da última interação pendente.

Conclusão documental de 6.5: plano-limpeza-autenticacao.md registra política explicitamente aprovada em 31/10/2026 (sete dias sessões/autorizações; 24 horas contadores após janela; auditoria/usuários/singleton preservados), lotes/frequência/falhas/locks e negação independente da exclusão. Medição controlada de cem interações/duas chamadas sequenciais já verificada no Angular. Nenhum job, DELETE ou acesso PostgreSQL implementado/executado; execução futura requer recorte próprio autorizado.

- [x] 6.6 Testar atividade válida de B mantendo sessão compartilhada de A, timer local sem logout indevido, sessões independentes em outros navegadores e invalidação global administrativa.

Progresso administrativo em 6.4/6.6: comandos reais de perfil/bloqueio invalidam sessões independentes; login esperando bloqueio é negado após commit; ator é revalidado após espera por locks. Testes em AdministracaoUsuariosHttpTest e evidências em etapa-4-administracao.md. Permanecem desmarcadas por incluírem reset e/ou integração de abas/cliente ainda não implementados. Pendências anteriores de 2.2, 6.2 e 10.1 preservadas; TDD desta etapa não apaga lacunas históricas nem conclui Angular.

Progresso posterior de reset em 6.4/6.6: consumo real invalida sessões independentes, persiste após reinício e concorre com login/bloqueio, conforme RedefinicaoConcorrenciaTest e ReinicioAutenticacaoTest; evidências em etapa-5-redefinicao.md. O reset backend citado como futuro no parágrafo histórico acima foi entregue; as tarefas mistas continuam desmarcadas pelas responsabilidades de cliente/abas não concluídas. A parte backend da regressão 10.1 foi renovada sem concluir sua integração Angular.

## 7. Angular: autenticação e retorno

Conclusão de cliente de 6.2/6.4/6.6: componentes, Router, HttpTestingController e relógio/navegador controlados verificam eventos, sinais perdidos, abas suspensas, prazos globais, troca de identidade e respostas tardias. Combinam-se aos testes transacionais/HTTP de servidor já aprovados nas etapas anteriores; não representam concorrência PostgreSQL ou E2E completo com dois navegadores reais. Evidências e limites em frontend-integrado.md. Em 6.5, a medição de cem interações com uma chamada em voo e apenas uma pendente foi verificada; o planejamento operacional de limpeza/retenção foi posteriormente aprovado e concluído em plano-limpeza-autenticacao.md, sem condicionar expiração a job.

- [x] 7.1 Restaurar identidade/perfil por /me antes de abrir shell no reload/foco, usando cookie automático e Angular XSRF na mesma origem; distinguir 401, 403 CSRF/perfil, 429 e 503 sem expor JWT ou alterar parser lossless.
- [x] 7.2 Criar login/reset fora do shell com obtenção de CSRF e atualização após login/logout; mensagens genéricas, sem consulta pública de existência e sem retry automático de comandos.
- [x] 7.3 Adicionar guards de sessão e ADMIN e identidade/sair no layout, sem reformular dashboards ou navegação geral.
- [x] 7.4 Implementar intenção de retorno por identidade com allowlist de parâmetros/fragmento, TTL e consumo único; testar carteiraId/origem, página proibida, carteira excluída, URL externa e troca de usuário. Conforme aprovação Angular, não serializar filtros locais de custódia na URL nem restaurar campos não salvos.
- [x] 7.5 Implementar pointerdown, keydown sem repeat, input/change, scrollend e navegação humana com deduplicação sem debounce; uma atividade em voo por aba e marcador pendente, sem heartbeat, movimento de ponteiro ou foco como atividade; usar timers simulados.
- [x] 7.6 Limpar contexto/preferência global/caches/forms, cancelar requests e descartar respostas por geração; sincronizar login/logout/atividade por sinais sem segredos e revalidar /me, preservando retorno só da mesma identidade.

- [x] 7.7 Implementar fallback /me a cada 30s nas abas visíveis e revalidação ao foco, mascarando aba oculta; testar sinal perdido, bloqueio/perfil/reset externos, 503 e sessão ativa em outra aba sem registrar atividade por leitura.
- [x] 7.8 Testar navegação após reload válido, cookie inválido/revogado, retorno autorizado/negado, troca A → B e resposta 401 antiga que não deve apagar sessão nova; não restaurar campos não salvos.

## 8. Angular: administração e restrições financeiras

- [x] 8.1 Criar listagem/cadastro de usuários para ADMIN, edição de perfil/situação e autorização/cópia do código uma única vez, com no-store e sem campo de senha administrativa.
- [x] 8.2 Adaptar cada ação financeira da matriz D6 para USER consultar sem gravar: carteiras, ações, corretoras, operações, cotação em lote/individual, snapshots, movimentações e estornos.
- [x] 8.3 Testar componentes+Router+HttpTestingController: USER sem ações/rotas administrativas, ADMIN autorizado, mudança de perfil externa percebida em 401 e ausência de retry automático de escrita.
- [x] 8.4 Verificar foco/teclado, erro por campo e confirmação das ações administrativas usando os padrões visuais atuais.

## 9. Documentação e integração de execução

- [x] 9.1 Atualizar OpenAPI real JSON/YAML com cookieAuth apiKey in:cookie, CSRF em todas as mutações, /auth/csrf, atributos/limites e 401/403/409/429/503; sem esquema Bearer alternativo e preservando contratos financeiros.
- [x] 9.2 Adaptar Postman com cookie jar e CSRF antes/depois de login/logout; validar Swagger same-origin com springdoc CSRF real ou documentar uso de Postman se Try it out limitado, sem bypass; demonstrar 200/401/403.
- [x] 9.3 Documentar PRD/README, matriz de acesso, horizontalidade compartilhada e justificativas @PreAuthorize, onboarding, bootstrap, códigos, prazos e limites de revogação.
- [x] 9.4 Preparar configuração futura runtime/Compose/.env.example sem segredos reais, TLS/mesma origem, cookie/CSRF, limite 4h e rollback seguro; remover segredo bootstrap; não executar deploy/migration persistente sem autorização.

Aceite 9.1–9.3: 28 testes OpenAPI, documentos JSON/YAML reais, quatro testes estáticos da coleção e fluxo Newman de 28 itens/41 requests/44 asserções em H2 isolado aprovados; suíte completa de 2.564 testes (2.462 aprovados, 102 condicionais ignorados), sem falhas/erros; JaCoCo 96,13% linhas/84,79% ramos. Swagger same-origin comprovou handshake 204 e consulta anônima 401, mas a validação de header obrigatório antecede o interceptor no Try it out: limitação documentada com Postman como alternativa prevista, sem bypass. Evidências, TDD e limites em fechamento-documentacao.md e docs/AUTENTICACAO.md.

## 10. Regressão, cobertura e validação final

- [x] 10.1 Adaptar fixtures para cookies e CSRF sem desligar segurança, executar regressão financeira e cenários de reload/abas/expiração/revogação/CSRF com relógio controlado; preservar locks/auditoria/precisão.
- [x] 10.2 Executar na raiz `.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" verify`; exigir >=80% de linhas e ramos globais JaCoCo, sem reduzir limites/exclusões.
- [x] 10.3 No frontend, definir VITEST_MAX_WORKERS=1 e NG_BUILD_MAX_WORKERS=2 no ambiente isolado, executar `npm run test:coverage` e `npm run build`; exigir >=80% linhas/ramos globais V8 incluindo produção não importada, preservando exclusões vigentes.
- [x] 10.4 Validar constraints/concorrência PostgreSQL, incluindo a homologação pendente da migration 009, apenas se houver autorização futura para ambiente descartável dedicado; registrar limitações se não executado, sem apontar para banco persistente ou providers reais.

Aceite de 10.4: autorização explícita do solicitante para suporte/execução descartável; PostgreSQL 17.11 linux/amd64 fixado por digest e contexto local. Rodada dedicada de 137 testes aprovada sem ignorados (135 casos PostgreSQL e dois de preflight), seguida de 40 testes da migration com prova reforçada de rollback preservando dados/auditoria. Instalação/upgrade, integridade, locks reais, atomicidade, revogação, códigos, abuso e reinícios comprovados. Regressão Maven: 2.671 testes, 2.464 aprovados e 207 condicionais ignorados, sem falhas/erros; JaCoCo 96,13% linhas/84,79% ramos. Quatorze execuções exclusivas e 42 recursos descartados/ausentes por inspeção final. Nenhuma alteração de produção ou migration; 9.4 permanece pendente. Evidências e limites em homologacao-postgresql.md.
- [x] 10.5 Atualizar Graphify após alteração de código e executar `openspec validate adicionar-autenticacao-e-gestao-de-usuarios --strict`; conferir cenário → teste e evidências RED/GREEN/revisão/portões, sem alegar TDD retroativo.
- [x] 10.6 Apresentar evidências, riscos remanescentes e arquivos alterados para revisão; não fazer commit/push/merge/archive automaticamente nem marcar tarefa parcialmente entregue como concluída.

Aceite 9.4: Nginx HTTPS em gestaoacoes.localhost:8443, Tomcat nativo com proxy exato /32, cookies/CSRF/origem, segredos externos, roles separadas, bootstrap one-shot, IP real/rate limiting, revogação, reinício e recuperação criptografada em outro banco comprovados. Quatorze grupos de verificações na rodada final passaram; recursos Docker exclusivos descartados. Maven 2674 testes (2467 aprovados/207 condicionais), JaCoCo 96,16% linhas/84,69% ramos; Angular 1120 testes, 98,35% linhas/91,60% ramos. Detalhes, falhas intermediárias, evidências e limites em operacional-https.md e docs/OPERACIONAL-HTTPS.md. Não houve alteração de migration, regra financeira, implantação pública ou arquivamento.
