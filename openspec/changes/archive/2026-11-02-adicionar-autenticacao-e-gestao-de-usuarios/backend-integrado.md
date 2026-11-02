# Relatório da implementação integrada do backend

Estado: backend integrado implementado e validado nos recortes autorizados, com aceite final executado em 31/10/2026 após retomada da interrupção por limite de uso. Aguardar revisão do solicitante. Não autoriza administração de usuários, redefinição funcional, Angular, deploy, stage/commit/push/merge ou arquivamento. Tarefas mistas e a pendência processual de 2.2 permanecem desmarcadas, conforme abaixo.

## Baseline e escopo

Repositório: `C:/Users/julia/Projetos/GestaoAcoes/gestaoacoes`. Snapshot SHA-256 dos 1130 arquivos preexistentes e Git status inicial registrados em `target/auth-integrado/baseline.json` e `git-status-inicial.txt`, antes de modificar código. README, teste Angular, ZIP, persistência e artefatos já estavam com alterações locais; não são atribuídos integralmente a este ciclo.

Java 17, Boot 4.1.0 e BOM Security 7.1.0 reconfirmados. Dependências de Security e Resource Server geridas pelo BOM, usando o nome atual `spring-boot-starter-security-oauth2-resource-server`, mais `spring-security-test`; sem biblioteca JWT paralela. API `csrf.spa()` compilada na versão efetiva. Usar Nimbus e JwtAuthenticationProvider sem acionar a DSL Resource Server que exclui CSRF implicitamente.

## Recorte A — preparação

- Configuração de chave Base64 runtime de pelo menos 32 bytes; duração positiva limitada a quatro horas; erros sem valor rejeitado. HTTP local depende de profile explicitamente local e não pode ser habilitado em conjunto com profile de produção.
- BCrypt custo 10, username canônico e política de senha de oito caracteres/máximo 72 bytes UTF-8, sem trim ou truncamento. Verificação equivalente com hash dummy para contas inelegíveis; mesma resposta para senha incorreta, usuário inexistente, bloqueado ou sem credencial.
- Bootstrap local one-shot pelo profile `bootstrap-admin`, sem servidor HTTP; usuário/senha somente em variáveis runtime `AUTH_BOOTSTRAP_USERNAME`/`AUTH_BOOTSTRAP_PASSWORD`, sem defaults. Lock singleton, recusa de banco inicializado, auditoria mínima e fechamento do contexto. Remover profile/segredos após a execução. Nenhum endpoint ou usuário automático no modo normal.
- Fixtures aleatórias e H2 descartável; nenhuma credencial de teste na aplicação publicada.

Evidências: `a-config-red.log` (compilação: classe ainda inexistente), `a-config-green.log` (1 teste); `a-bootstrap-red.log` (compilação: serviços inexistentes após resolver permissão do cache Maven), `a-bootstrap-green.log` (2 testes); `a-command-red.log` (comando inexistente), `a-final.log` (**63 testes, zero falhas/erros**), incluindo persistência existente. A ampliação de configuração em `b-matriz-config.log` passou com testes de falha real do contexto. A primeira tentativa Maven sem acesso ao cache `.m2` falhou por permissão e não é RED funcional.

## Recorte B — autenticação e autorização

Implementados e verificados: login/me/logout, JWT somente por cookie, CSRF separado e rotacionado no login/logout, consulta persistida por request e revogação por SID/versão. Matriz D6 explícita/default deny com proteção adicional dos métodos de escrita nos Resources. Sem segurança dentro da transação financeira. Configuração HTTPS e CORS de mesma origem; headers X-Forwarded-* não são confiados diretamente.

O inventário HTTP reconcilia todos os 32 mappings financeiros reais. Testes financeiros existentes recebem ADMIN por fixture isolada, cookie real e token reconhecido pelo repositório CSRF real, com filtros ativos. Rotas não classificadas passam de 404/405 para 403; a ausência dos aliases/métodos continua assegurada pelo inventário de mappings e pelo contrato OpenAPI. O orçamento de duas consultas financeiras foi preservado com StatementInspector apenas de teste, separando as tabelas de segurança sem excluir consultas financeiras ou alterar cobertura de produção.

REDs HTTP observados: handshake 204/401; login 200/403; leitura USER 200/403; logout 204/403; limite de login 429/401; HTTPS 400/204. Respectivos GREENs estão nos logs `b-*.log` de cada ciclo. Extensões de cenários foram adicionadas como verificações após o tracer inicial; não se alega RED individual retroativo para todos os cenários de credenciais/claims.

Primeiras regressões: **45 testes, zero falhas/erros** em `b-logout-financeiro-green.log` e `b-tls-regressao-green.log`. `b-seguranca-completa.log`: **37 testes, zero falhas/erros**. `b-aceite.log` identificou adaptações necessárias de contrato, matcher do YAML e medição SQL, corrigidas sem liberar rotas financeiras. `b-aceite-final.log` teve interferência de recompilação por execução Maven sobreposta (21 erros de classpath), falha de execução e não de domínio. Na repetição sequencial `b-aceite-sequencial.log` houve **210 testes, uma falha e zero erros**: um helper financeiro 404 havia sido alterado indevidamente junto à adaptação de rota inexistente. A asserção financeira original foi restaurada; `b-operacao-preservada.log` confirmou **13 testes de operações, zero falhas/erros**. Não houve relaxamento de regras ou liberação temporária de APIs.

`b-reinicio.log`: **1 teste, zero falhas/erros**, fechando/reabrindo três contextos reais do backend sobre o mesmo H2 em memória com keeper e Clock controlado; preserva sessão válida, revogação e corte ocioso. Servidores somente em porta efêmera de loopback; nenhum banco persistente ou provider real usado.

## Recorte C — sessão e inatividade

A implementação do endpoint e o teste inicial já existiam antes da retomada: `c-atividade-red.log` registra 200 esperado/403 observado; `c-atividade-green.log` registra **36 testes, zero falhas/erros**, incluindo autenticação e OpenAPI. Os testes adicionais de borda/concorrência foram gravados, mas a tentativa de executá-los foi recusada pelo limite de uso antes de iniciar o Maven. Não existia `c-bordas-concorrencia.log` ao retomar.

Diagnóstico da retomada: documentos e diff/status lidos antes de editar; nenhum processo Java/Maven ativo encontrado por `Get-Process` (consulta CIM indisponível no sandbox). Snapshot adicional de **1152 arquivos** em `baseline-retomada.json`. Código e alterações existentes foram preservados; não se reiniciou a implementação.

Na retomada, `c-bordas-concorrencia.log`: **9 testes, zero falhas/erros**, sem alteração em produção:

- Atividade imediatamente antes da borda renova somente o prazo ocioso da própria sessão; SID, usuário e timestamp enviados no corpo não determinam o alvo ou a hora.
- Exatamente dez minutos negam /me e atividade; GETs, polling e CSRF não renovam; atividade tardia não ressuscita SID e 401 não remove cookie por resposta atrasada.
- Atividade contínua permite uso somente antes das quatro horas; exp JWT, banco e cookie permanecem coerentes, sem refresh/reemissão.
- Exemplo 08h00/08h30/08h40/12h00, SID compartilhado entre clientes/abas, navegador independente e logout efetivo sem sinal visual.
- Limite de login é liberado ao fim da janela, sem bloqueio permanente da conta.
- Requests aguardam lock real no H2 (confirmado por `information_schema.sessions.blocker_id`); tempo e versão são reavaliados após espera/commit. Concorrência de logout e atividade nunca desfaz revogação.

Foi acrescentado somente um teste de aceitação ausente à classe existente `ReinicioAutenticacaoTest`: aumentar a duração configurada de uma para quatro horas após reinício não prolonga sessão já emitida, mesmo com atividade recente. `c-reinicio-configuracao.log`: **2 testes, zero falhas/erros**, incluindo o reinício anteriormente verificado, em H2 em memória e loopback. Trata-se de extensão de verificação de comportamento existente, sem alegação de RED retroativo.

Rastreabilidade: 6.1 e 6.3 têm contratos completos de servidor verificados; 6.2, 6.4 e 6.6 continuam parciais por dependerem também de cliente oficial e/ou comandos administrativos/reset ainda fora do ciclo.

## Recorte D — aceite

Executado sequencialmente, sem Maven concorrente, somente com bancos H2 de teste descartáveis e infraestrutura externa real desabilitada.

| Evidência da retomada em target/auth-integrado | Resultado | Interpretação |
| --- | --- | --- |
| c-bordas-concorrencia.log | 9 testes; 0 falhas; 0 erros | Contratos de atividade, bordas, abas, revogação, locks e janela de login |
| c-reinicio-configuracao.log | 2 testes; 0 falhas; 0 erros | Reinício real e aumento de configuração sem prolongar sessão existente |
| d-verify.log | 2482 considerados; 7 falhas; 0 erros; 102 ignorados | Teste temporal ainda contava chamadas ao Clock global, agora também lido pela autenticação; não foi falha de regra financeira |
| d-temporal-fronteira.log | 17 testes; 0 falhas; 0 erros | Medição movida para o Clock de aquisição já existente; preservadas asserções de valores, precisão, datas, rollback e nenhuma aquisição em inválidos/leituras |
| d-verify-final.log | **2482 considerados; 2380 executados com sucesso; 0 falhas; 0 erros; 102 ignorados; BUILD SUCCESS** | Suíte completa, regressão financeira, autenticação/autorização, cookies, CSRF, sessões e artefato do backend |
| d-graphify-final.log | Atualização AST concluída | 13778 nós, 29192 arestas, 918 comunidades; artefatos locais ignorados pelo Git |

Comando final, na raiz do backend:

```powershell
.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" verify
```

Os 102 testes ignorados são condicionais preexistentes; o baseline também tinha 102 ignorados. Não foram adicionados skips para acomodar segurança. O total aumentou de 2431 para 2482, com 51 testes novos. PostgreSQL real e providers reais não foram usados como ambientes de validação. Nenhum teste/build Angular foi executado, pois o frontend permanece fora do ciclo.

### JaCoCo e preservação de cobertura

Relatório novo em `target/site/jacoco/index.html` e `jacoco.xml`, gerado por verify com dados novos (`jacoco.append=false`), incluindo todas as classes de produção do módulo. **259 classes analisadas; ambos os portões globais de 80% aprovados**, sem reduzir limites nem adicionar exclusões.

| Métrica global | Cobertos / total | Cobertura |
| --- | --- | --- |
| Linhas | 3913 / 4068 | **96,19%** |
| Ramos | 2022 / 2385 | **84,78%** |

Baseline anterior: 3627/3770 linhas (96,21%) e 1879/2203 ramos (85,29%). A razão global variou com a inclusão da autenticação, mas os portões foram preservados. A comparação por classe confirmou **nenhuma redução de elementos cobertos nas 230 classes preexistentes comparáveis fora do pacote de autenticação**, incluindo o domínio financeiro. Dados em `cobertura-final.json`; cópia do XML anterior em `jacoco-pre-integrado.xml`. Não confundir percentuais antigos com os desta entrega.

### Rastreabilidade e estado das tarefas

| IDs | Evidência / comportamento verificado | Estado final |
| --- | --- | --- |
| 2.1 | AuthRuntimeConfigTest: chave, duração, falha de contexto sem expor valores e profiles locais; BOM/API efetivos compilados | Concluída |
| 2.2 | CredenciaisBootstrapTest e AutenticacaoHttpTest: credenciais válidas/incorretas/inexistentes/bloqueadas/não inicializadas, BCrypt e limite UTF-8 | **Parcial processual**: implementação e testes completos, mas não há RED individual anterior à implementação para todos os casos negativos; não marcar como ciclo TDD integral nem fabricar evidência retroativa |
| 2.3, 2.7, 2.8 | AutenticacaoHttpTest: JWT real, algoritmo/assinatura/claims, cookie e reload, ausência de Bearer/URL alternativo, CSRF e rotação, no-store e ausência de segredos no JSON | Concluídas no contrato de backend; reset funcional e cliente Angular continuam em suas próprias tarefas |
| 2.4 | AutenticacaoHttpTest, SegurancaIndisponivelHttpTest e ReinicioAutenticacaoTest: estado por request, versão, logout, banco indisponível/503 e reinício | Concluída; eventos administrativos não foram implementados |
| 2.5 | CredenciaisBootstrapTest: bootstrap único/concorrente, recusa de reexecução e segredo inválido, comando one-shot; inventário/OpenAPI sem endpoint de bootstrap | Concluída |
| 2.6 | AutenticacaoHttpTest e SessaoInatividadeHttpTest: 5 falhas por identidade+IP/50 por IP em 15min, Retry-After, fim da janela, cookies, HTTPS, CORS e headers não confiados | Concluída no servidor; configuração operacional de proxy confiável permanece em 9.4 |
| 3.1–3.2 | Inventário de 32 mappings reais, cookie/CSRF reais, anônimo/USER/ADMIN e guards de escrita; regressão financeira integral | Concluídas |
| 3.3 | Consultas auxiliares persistidas e prévias revisadas; regressão temporal, de preços, patrimônio e resumo preserva dados e orçamento SQL financeiro | Concluída |
| 3.4 | AutenticacaoHttpTest e SegurancaIndisponivelHttpTest: StandardError, 401/403 CSRF/perfil, precedência e 503; erros financeiros mantidos | Concluída |
| 3.5 | Paths diretos testados, proxy existente /api/ → backend / inspecionado em frontend/nginx.conf; docs JSON/YAML públicas e nenhum bypass por Authorization/ferramentas | Concluída no backend; execução real de Swagger/Postman e TLS/proxy publicado permanece em 9.2/9.4 |
| 6.1 | SessaoInatividadeHttpTest: interação válida, corte exato e aferição após lock | Concluída |
| 6.2 | Servidor não renova por GET/polling/me/csrf/timers, ignora hora cliente e nega atividade tardia | Parcial; deduplicação/eventos confiáveis do cliente dependem de 7.5 |
| 6.3 | SessaoInatividadeHttpTest e ReinicioAutenticacaoTest: 4h, exp/banco/Max-Age, jornada e mudança de configuração | Concluída |
| 6.4 | Locks, versão por fixture externa e logout/atividade concorrentes verificados | Parcial; comandos de bloqueio/reset e respectivas corridas dependem de 4.x/5.x; não se alega cancelamento de request financeiro já autorizado |
| 6.6 | SID comum entre clientes e sessões independentes, logout mesmo sem sinal entre clientes | Parcial; timer/estado visual dependem de Angular e invalidação por comandos administrativos depende de 4.x |
| 10.1 | Fixtures isoladas com filtros reais e regressão financeira/segurança/sessões no backend aprovadas | Parcial conforme reorganização; cenários integrados com cliente/etapas futuras ainda pendentes |
| 10.2 | verify final e JaCoCo global aprovados | Concluída |
| 10.5 | Grafo AST atualizado, cenário → testes/evidências revisados, OpenSpec strict e portões conferidos, sem alegação retroativa de TDD | Concluída como conferência; lacuna identificada em 2.2 permanece explícita |
| 10.6 | Relatório, limitações, inventário e Git apresentados para revisão; nenhuma operação proibida | Concluída |

Marcação final: **22/56 tarefas concluídas e 34 desmarcadas**. Dentre as desmarcadas, 2.2, 6.2, 6.4, 6.6 e 10.1 têm progresso parcial documentado; as demais continuam posteriores. Nenhum ID foi renumerado ou duplicado. A pendência de 2.2 é de aceite do processo/evidências, não falta de validação funcional; não se remove código correto para produzir RED artificial.

### Endpoints e contrato efetivo

- `GET /auth/csrf`: público, 204, materializa XSRF-TOKEN legível por JS, sem autenticar/registrar atividade e sem JSESSIONID.
- `POST /auth/login`: público com CSRF obrigatório; username/senha, BCrypt, nova sessão/SID e JWT HS256 somente no cookie GESTAOACOES_AUTH; resposta de identidade e prazos. Credenciais inválidas indistinguíveis, bloqueadas/não inicializadas negadas; abuso retorna 429.
- `GET /auth/me`: exige cookie/sessão atuais, restaura identidade/prazos sem atividade nem reemissão de JWT.
- `POST /auth/logout`: exige cookie e CSRF, revoga SID compartilhado e remove cookie no sucesso; invalida CSRF anterior no navegador.
- `POST /auth/atividade`: exige cookie e CSRF, revalida estado e tempo após locks e atualiza apenas a atividade da própria sessão. Corpo não define usuário/SID/hora do servidor. Não prova interação humana; esse protocolo será usado pelo futuro cliente oficial.

Cookies: auth HttpOnly; ambos Path=/, SameSite=Lax, host-only e Secure fora do modo HTTP local explicitamente permitido. JWT não aparece em resposta JSON, JavaScript, Web Storage ou header alternativo. SID/usuário/versão e prazos são conferidos no banco por request; falha de banco nega com 503. Inatividade expira autoritativamente no servidor na igualdade de dez minutos, sem job, tolerância ou dependência de limpeza física da linha. JWT/expiração absoluta limitados a quatro horas; sem refresh ou renovação automática. A apresentação visual/limpeza no cliente permanece posterior.

### Arquivos e preservação das alterações locais

**Nesta retomada**, além dos artefatos ignorados em target/graphify-out, foram modificados somente:

1. `src/test/java/com/projeto/autenticacao/ReinicioAutenticacaoTest.java`: cenário de mudança de configuração após reinício.
2. `src/test/java/com/projeto/resources/AcaoCadastroTemporalIntegrationTest.java`: fronteira de medição do Clock financeiro.
3. `openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/tasks.md`: autorização vigente e marcações verificadas, preservando tarefas parciais.
4. Este `backend-integrado.md`: diagnóstico, evidências e aceite.

Inventário do **ciclo integrado inteiro**, distinguindo o que já existia ao retomar:

- Produção criada: `AutenticacaoResource.java`; security/`AuthConfig`, `AuthRuntimeConfig`, `BootstrapAdminCommand`, `BootstrapAdminService`, `CookieAuthenticationFilter`, `CredenciaisConfig`, `CredenciaisService`, `JwtService`, `SecurityErrors`, `SecurityWebConfig`, `SessaoService`, `TransportSecurityFilter`; `application-bootstrap-admin.properties`.
- Produção estendida: `Usuario`, `SessaoAutenticacao`, `ContadorAbuso` e respectivos repositories; `pom.xml`; guards em `AcaoResource`, `AtualizacaoCotacoesCarteiraResource`, `CarteiraResource`, `CorretoraResource`, `MovimentacaoCustodiaResource`, `OperacaoResource`. Nenhum cálculo financeiro foi alterado.
- Testes criados: `AuthRuntimeConfigTest`, `CredenciaisBootstrapTest`, `AutenticacaoHttpTest`, `FinanceiroAutenticadoFixture`, `ReinicioAutenticacaoTest`, `SegurancaIndisponivelHttpTest`, `SessaoInatividadeHttpTest`.
- Testes/configuração adaptados: `application-test.properties` e resources/`AcaoCadastroTemporalIntegrationTest`, `AcaoCotacaoHttpIntegrationTest`, `AcaoResourceTest`, `AtualizacaoCotacoesCarteiraHttpIntegrationTest`, `AtualizacaoCotacoesCarteiraResourceTest`, `CarteiraResourceTest`, `CorretoraResourceTest`, `EvolucaoPatrimonialResourceTest`, `OpenApiDocumentationTest`, `OperacaoResourceTest`, `PatrimonioResourceTest`, `PosicaoResourceTest`, `PrecoOperacaoResourceTest`, `ReferenciaDataJpaDiagnosticoTest`, `ResultadoRealizadoResourceTest`, `ResumoCarteiraResourceTest`.
- Documentação: relatório novo neste ciclo e tasks.md atualizado; proposta/design/specs/reorganização e relatório histórico da etapa 1 preservados nesta retomada.

Comparação SHA-256 com o baseline do ciclo confirma preservação dos **70 arquivos protegidos**, incluindo README, teste Angular, ZIP, todas as migrations e testes/suportes de custódia. Comparação com o snapshot da retomada distingue os quatro arquivos acima; nenhum arquivo removido. Nenhuma alteração de produção nesta retomada. Detalhes em `conferencia-escopo.json`.

Verificação de segredos literais na produção nova/alterada (26 arquivos) não encontrou JWT, hash BCrypt, chave privada ou segredo auth fixo; revisão confirmou credenciais somente runtime e fixtures efêmeras de teste. Não se imprime conteúdo potencialmente sensível na conferência. Git diff/status conferidos; `git diff --check` sem erros, apenas avisos de normalização LF/CRLF em arquivos já modificados. Nenhum arquivo no stage. A pasta da change e a persistência já eram não rastreadas antes deste ciclo; o Git status não deve ser interpretado como atribuição de todas essas mudanças à retomada.

OpenSpec `validate adicionar-autenticacao-e-gestao-de-usuarios --strict`: válido. Graphify atualizado apenas por AST; os avisos conhecidos de JSONs sem nós e ausência de tree_sitter_sql não são prova de validação SQL, nem substituem a homologação PostgreSQL. Não foi instalada dependência adicional para esse diagnóstico.

Nenhum git add, commit, push, merge, arquivamento ou deploy executado. Interromper aqui e aguardar revisão antes de qualquer etapa posterior.

## Pendências preservadas

Homologação PostgreSQL da migration 009 permanece em **10.4**, não executada; migrations não são alteradas neste ciclo. Administração 4.x, reset 5.x, cliente Angular 7.x/8.x e integração pública/operacional completa 9.x continuam posteriores. Fixtures de ativo/versão não constituem comandos administrativos implementados.

Publicado exige TLS e mesma origem; o contêiner deve reconhecer HTTPS de forma confiável. Integração operacional de proxy confiável ainda depende de 9.4; não interpretar headers arbitrários para contornar a exigência de TLS ou limites por IP. Atrás de proxy, usar seu endereço de conexão agrega conservadoramente os limites por IP até a configuração operacional apropriada.

## Recorte posterior aprovado — etapa 4

Após a aprovação técnica do backend descrito neste relatório histórico, foram autorizadas e implementadas 4.1, 4.3, 4.4 e 4.5. Os contratos, testes e evidências da administração estão em etapa-4-administracao.md. A indicação anterior de administração como futura descreve o encerramento do ciclo integrado original. Cadastro 4.2 continua pendente do código inicial da etapa 5; redefinição e Angular não foram antecipados. Os testes administrativos reais ampliam as partes de servidor de 6.4/6.6, sem concluir suas responsabilidades de reset/cliente. Homologação 009 e concorrência PostgreSQL permanecem pendentes em 10.4.

## Recorte posterior — etapa 5 e cadastro 4.2

Após aprovação da etapa 4 e dos contratos de preparação, foram implementadas 5.1–5.5 e 4.2 nos recortes A–E. Evidências, contratos de emissão/consumo, tentativas, limites, concorrência, rollback e reinício estão em etapa-5-redefinicao.md. As referências anteriores a reset/cadastro como futuros descrevem os ciclos históricos. Agora a parte backend de reset em 6.4/6.6 está verificada; cliente/abas continuam pendentes. Mantidas a lacuna processual de 2.2, as tarefas mistas de Angular/integração e 10.4 PostgreSQL. Nenhuma regra ou lock financeiro foi alterado.
