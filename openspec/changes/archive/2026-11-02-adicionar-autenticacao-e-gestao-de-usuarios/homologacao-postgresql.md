# Homologação PostgreSQL — tarefa 10.4

Execução autorizada pelo solicitante após a análise preparatória. Escopo exclusivamente de testes PostgreSQL descartáveis; 9.4 permanece pendente. Nenhuma alteração da migration 009, regra financeira, configuração de produção ou operação Git de escrita é autorizada.

## Preflight e isolamento

Contexto fixado desktop-linux, endpoint local npipe:////./pipe/dockerDesktopLinuxEngine, Docker Engine Linux/amd64 sob WSL2. Imagem local docker.io/library/postgres:17.11-alpine3.24, PostgreSQL 17.11, digest sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb. Sem pull ou substituição de contexto/imagem. A primeira inspeção sem permissão de acesso ao pipe foi repetida com execução autorizada fora da restrição do processo; o daemon foi comprovado antes de qualquer provisionamento.

O suporte reutiliza CustodiaPostgresOperacional/Manifesto/DockerCli/Jdbc/DataSource: UUID, nomes e rótulos exclusivos, nova rede/volume/container, porta aleatória somente 127.0.0.1 e diferente de 5432, roles de provisionamento/aplicação distintas, credenciais e marcador CSPRNG. Não utiliza Compose, .env ou recursos existentes. O transporte existente fixa o mesmo pipe conferido no preflight. Antes de cada conexão física entregue ao pool, são conferidos recurso/imagem/plataforma, URL e credenciais exatas, banco/usuário/versão e marcador SQL protegido. Configurações externas de datasource/Liquibase/JPA/perfis são recusadas pelo guard existente. O contexto Spring recebe somente o datasource explicitamente registrado; não há fallback para dev/H2. Providers são direcionados a loopback indisponível e somente fronteiras externas permanecem simuladas nos testes herdados.

Manifestos sem segredos são preservados em target/auth-postgresql/<uuid>/manifesto.json. Eventos de asserções/casos e descarte são anexados a evidencias.jsonl antes do descarte; logs Maven guardam contagens e relações de bloqueio sem consulta/valores SQL. Nenhuma evidência exporta senhas, hashes, código, JWT ou marcador. Pools/contextos fecham antes do descarte; o adaptador fecha conexões rastreadas e verifica todos os recibos antes de remover cada recurso exato. Divergência preserva o recurso para diagnóstico, sem prune ou remoção por prefixo.

## TDD e adaptação dos testes

Fronteiras aprovadas: preflight/executor, Liquibase/JDBC, HTTP/serviços reais e Clock controlado. 01-preflight-red.log comprova recusa inicialmente ausente de contexto divergente; 01-preflight-green.log comprova implementação da guarda. As regras existentes foram caracterizadas em PostgreSQL por herança das mesmas asserções aprovadas, sem inventar RED de funcionalidades já corretas. Testes H2 preservam seus caminhos; somente hooks de observação de locks, injeção/remoção de falhas e nomes físicos de metadados foram extraídos para adaptação do motor.

Diferenças explícitas: locks PostgreSQL observados por pg_stat_activity/pg_blocking_pids, sem usar information_schema.sessions do H2. Falhas reais usam trigger PL/pgSQL restrito às tabelas de autenticação do banco descartável. Cada SQL negativo de integridade usa savepoint, confere SQLSTATE de integridade/tamanho e restaura a transação, não aceitando 25P02 como prova. Timestamps são comparados por Instant e a precisão física é conferida em microssegundos.

Incidentes do harness: importação de DirtiesContext corrigida antes de executar banco; overrides de ciclo de vida JUnit precisaram das próprias anotações; fixture JWT passou a 64 bytes para construir o HS512 propositalmente inválido do teste de algoritmo. Nenhum desses ajustes alterou produção ou migration. A recusa com opt-in incorreto foi executada e falhou claramente sem criar recursos, em 05-optin-negativo.log.

## Evidências e aceite

02-sessao-pg-corrigido.log: nove testes de sessão aprovados e descarte comprovado. 04-migration-http.log: quarenta testes físicos da migration e dezesseis testes HTTP/JWT aprovados, total 56, sem falhas/erros/ignorados. A primeira rodada ampliada também comprovou administração, atomicidade, reset/abuso e upgrade com preservação financeira/reinício, mas não representa aceite final pelas falhas do harness acima.

A seleção sem opt-in também foi executada: 05-optin-ausente.log falhou explicitamente, sem ignorar o teste e sem criar recursos. A rodada 06 revelou um teste herdado que usava Instant.now() para avançar um Clock já controlado; passou a usar clock.instant(), mantendo a asserção 401. A guarda de reinício também recusou operação enquanto havia conexões ainda visíveis no servidor: agora exige todos os pools fechados e observa zero conexões por prazo limitado antes de reiniciar. Não é sleep como prova, tolerância de autenticação ou liberação de acesso. 07-administracao-reinicio.log comprovou 27 testes administrativos e três contextos sucessivos da aplicação com reinício real do banco, total 28 aprovados.

08-homologacao-aceite.log: **137 testes aprovados, zero falhas/erros/ignorados**, em 2min03s:

| Classe/cenário | Casos aprovados |
| --- | ---: |
| Administração, último ADMIN, versões, revalidação e bootstrap concorrente | 27 |
| Atomicidade e falhas PostgreSQL de usuário/autorização/auditoria/contador | 6 |
| Cadastro e definição inicial | 6 |
| HTTP/JWT/cookies/CSRF, matriz financeira, login e revogação | 16 |
| Migration 009, integridade física, tipos, timestamps, idempotência e rollback | 40 |
| Concorrência de emissão/consumo, bloqueio/reset, login/reset e abuso | 9 |
| Reinício real de banco e três contextos da aplicação | 1 |
| Definição/redefinição, expiração, tentativas, política e confidencialidade | 20 |
| Atividade/inatividade, limite absoluto, sessões e espera por locks | 9 |
| Upgrade 001–008 → 009, preservação financeira e reinício | 1 |
| Preflight sem acesso ao banco | 2 |

São 135 casos contra PostgreSQL e dois de preflight. As esperas reais foram registradas com PID bloqueado, bloqueadores obtidos por pg_blocking_pids e wait_event=transactionid; não há consulta de conteúdo/segredos nos logs de locks. Os cenários preservam as asserções aprovadas de contratos e efeitos, inclusive 401/403/409/429/503.

15-singleton-ausente.log renovou os 27 casos administrativos após incluir no cenário de bootstrap a ausência da linha singleton: recusa por IllegalStateException, nenhuma outra conta criada e nenhum controle recriado silenciosamente. A linha é restaurada somente pela fixture de teste. Isso completa a prova negativa planejada sem alterar o código publicado; não se contabiliza essa repetição como novos casos distintos.

10-rollback-preservacao.log renovou os 40 testes físicos após reforçar o caso de rollback: fixtures confirmadas em todas as seis tabelas de autenticação, recusa especificamente por StopChangeException, comparação integral dos registros em outra conexão, histórico 009 preservado e lock Liquibase liberado. Nenhum DROP de tabelas de autenticação foi executado. O registro sintético é removido apenas como limpeza da fixture nesse banco descartável. Não somar essa repetição aos 137 casos distintos.

Instalação aplica os dez changeSets e não semeia credenciais. Upgrade aplica os nove changeSets reais de 001–008 antes da 009, reutiliza a fixture financeira sintética aprovada e compara registros/estruturas das nove tabelas financeiras, incluindo referências, decimais e datas; a comparação permanece igual após idempotência e reinício. Validação Hibernate do contexto completo inclui entidades de autenticação, sem a exclusão histórica do validador de custódia. Catálogo PostgreSQL comprova seis PKs, duas UNIQUE, cinco FKs RESTRICT, dezenove CHECKs, quatro índices explícitos de autenticação, defaults/nulabilidade, UUID/BIGINT e precisão temporal. Cada SQL negativo recupera seu savepoint, não contaminando o próximo caso com transação abortada.

11-verify.log: **BUILD SUCCESS**, 2min20s, 2.671 testes: **2.464 aprovados, zero falhas/erros, 207 condicionais ignorados**. Os ignorados são os 102 condicionais anteriores mais 105 métodos/templates das novas classes PostgreSQL, não selecionadas no comando padrão. Com opt-in, os templates são expandidos nos 135 casos PostgreSQL comprovados acima; não interpretar os skips da suíte padrão como execução PostgreSQL ou falta de ambiente silenciosa. A regressão inclui os testes financeiros/autenticação/administração/reset/sessões já existentes, com asserções preservadas. XMLs de target/surefire-reports refletem a última suíte padrão; as evidências específicas reais foram preservadas nos logs nomeados, manifestos e evidencias.jsonl, sem reconstruir/fabricar resultados.

JaCoCo global, incluindo produção não exercitada: linhas **4.269/4.441 = 96,1270%**; ramos **2.297/2.709 = 84,7914%**. Ambos os portões de 80% aprovados; cobertura idêntica ao fechamento anterior. Nenhum código de produção foi alterado/excluído. HTML/XML em target/site/jacoco.

16-verify-final.log renovou a suíte completa no último estado de código de teste, após a asserção de singleton ausente: BUILD SUCCESS em 2min38s, novamente 2.671 testes/2.464 aprovados/207 condicionais ignorados, zero falhas/erros e as mesmas contagens JaCoCo acima, com ambos os portões aprovados. Não houve alteração de código após essa execução; somente consolidação documental.

Comandos executados sequencialmente na raiz do backend:

```powershell
.\mvnw.cmd -B "-Dtest=AutenticacaoPostgres*Test" "-Dauth.pg.real=EXECUTAR_009" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" test
.\mvnw.cmd -B "-Dtest=AutenticacaoPostgresMigrationRealTest" "-Dauth.pg.real=EXECUTAR_009" test
.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" verify
```

## Descarte e limites

Quatorze execuções independentes foram criadas entre ciclos de desenvolvimento, correções e aceite: quatorze containers, quatorze redes e quatorze volumes exclusivos. Todos encerrados pelo adaptador com recibos; inspeção final independente de cada ID/nome confirmou **42 ausências**, sem efetuar remoções adicionais. Evidência em target/auth-postgresql/descarte-verificado.json. A imagem local preexistente e todos os recursos anteriores foram preservados. As tentativas de compilação/opt-in recusadas não criaram recursos.

Houve avisos transitórios do executor de abertura de conexões Hikari em algumas rodadas. Pools foram encerrados, o rastreador JDBC foi fechado sem conexões remanescentes, e a prova de reinício exigiu zero conexões observadas no PostgreSQL. A inspeção final comprovou ausência dos containers; não se omite o aviso nem se declara um recurso pendente sem evidência.

Limites: um único pin PostgreSQL 17.11 em Linux/amd64 e Docker Desktop local Windows; não comprova outros majors/plataformas, alta disponibilidade, carga, failover ou recuperação após queda abrupta. Reinícios são controlados, com fechamento da aplicação. HTTP usa MockMvc com a cadeia real de segurança/cookie/JWT/CSRF e banco real; não é aceite de TLS/proxy/navegador de produção. Providers externos não foram consultados. Outras suítes PostgreSQL financeiras condicionais não foram habilitadas indiscriminadamente nem redirecionadas para este cluster; regressão financeira completa padrão foi executada em H2 isolado e preservação de dados na migration foi comprovada em PostgreSQL.

10.4 é concluída após os aceites acima; 9.4 permanece pendente. Não há autorização implícita para deploy, banco persistente, rotação de credenciais reais ou execução de limpeza automática. A revisão do solicitante antecede qualquer próximo recorte.

## Arquivos, Git e conferências finais

Baseline SHA-256 anterior às edições: 1.219 arquivos rastreados/não rastreados visíveis ao Git; 82 modificados rastreados, 129 não rastreados e zero staged. Comparação final: dez arquivos existentes alterados dentro do recorte, dezesseis criados, zero removidos; os outros 1.209 arquivos permaneceram idênticos. Evidências em target/auth-postgresql/baseline.json e inventario-final.json. Nenhum arquivo src/main, migration, pom.xml, configuração de infraestrutura ou frontend foi modificado. README preserva as alterações locais anteriores, inclusive o título preexistente.

Existentes alterados:

- src/test/java/com/projeto/autenticacao/AdministracaoUsuariosHttpTest.java — hooks de falha e locks; avanço pelo Clock injetado, preservando asserção 401.
- src/test/java/com/projeto/autenticacao/AutenticacaoMigrationTest.java — acesso da subclasse aos recursos de teste e normalização dos nomes físicos de metadados; mesmas asserções H2.
- src/test/java/com/projeto/autenticacao/RedefinicaoAtomicidadeTest.java — hook de remoção de trigger, preservando asserções de atomicidade.
- src/test/java/com/projeto/autenticacao/SessaoInatividadeHttpTest.java — hook de observação de lock, preservando todos os prazos/asserções.
- proposal.md, design.md e tasks.md desta change — estado atual/rastreabilidade, sem renumerar ou arquivar.
- README.md, docs/PRD.md e docs/AUTENTICACAO.md — atualização pontual do estado PostgreSQL, comando de opt-in, limites e pendência 9.4.

Criados em src/test/java/com/projeto/autenticacao:

- AutenticacaoPostgresPreflight.java e AutenticacaoPostgresPreflightTest.java.
- AutenticacaoPostgresExecucao.java e AutenticacaoPostgresReal.java — suporte/opt-in/isolamento/evidências/ciclo de vida.
- AutenticacaoPostgresAdministracaoRealTest.java, AutenticacaoPostgresAtomicidadeRealTest.java e AutenticacaoPostgresCadastroRealTest.java.
- AutenticacaoPostgresHttpRealTest.java e AutenticacaoPostgresMigrationRealTest.java.
- AutenticacaoPostgresRedefinicaoRealTest.java e AutenticacaoPostgresReinicioRealTest.java.
- AutenticacaoPostgresSenhaHttpRealTest.java, AutenticacaoPostgresSessaoRealTest.java e AutenticacaoPostgresUpgradeRealTest.java.

Criados adicionalmente: src/test/java/com/projeto/custodia/postgresql/AutenticacaoPostgresFinanceiro.java (ponte para fixture/retrato financeiro já aprovados) e este homologacao-postgresql.md. Nenhuma dependência foi adicionada e nenhum usuário/segredo foi semeado na aplicação publicada.

OpenSpec --strict: change válida (13-openspec.log). git diff --check: exit 0, sem erro de whitespace (14-diff-check.log). A revisão de evidências não encontrou JWT ou hash BCrypt exportado; os testes herdados de confidencialidade também verificaram que senha/código não entram nos logs. Não se afirma varredura de todo o histórico Git ou rotação da credencial histórica já documentada no README. Novos artefatos temporários ficam sob target/auth-postgresql, ignorado pelo Git; nenhum arquivo anterior foi apagado/restaurado/movido/preparado.

Git final: **82 rastreados modificados, 145 não rastreados e zero staged**, snapshot em target/auth-postgresql/status-final.txt. Não foram executados git add, commit, push, merge ou arquivamento. Tarefas: **55/56**, somente 9.4 pendente. Interromper ao fim deste recorte e aguardar revisão.

Graphify atualizado após o último código de teste por AST local, sem LLM/API: **zero tokens de API**, outputs ignorados, com detalhes em 17-graphify-final.log. Mantidos avisos de JSON/configurações sem nós e parser SQL opcional ausente, sem instalação adicional ou rotulagem paga. O grafo não substitui as provas diretas de código/banco/testes.

## Recursos registrados

Todos em 127.0.0.1; o volume de cada linha tem nome exato custodia-pg17-data-<UUID>. Todos os IDs abaixo foram inspecionados como ausentes após descarte. Nenhum recurso preexistente foi removido.

| UUID da execução | Container ID | Rede ID | Porta | Estado final |
| --- | --- | --- | ---: | --- |
| 1396eca6-a5da-405f-8509-46613b8314f5 | 37a037eb0b97024ce8b6da11a2999629b187f13a4fc2f987eb92039786148390 | 49a2adb0e1410489ebad228d768a125043549a980d128fdddd6622e5af71017b | 53678 | Descartados os três recursos |
| 266c2d27-510d-49c3-9c57-bb1af4deb0c7 | dbb62e142dda2d7550c33ec4ceda3e7217e2a3d995e6c5a2e7ca29feaebb2ab9 | 464813821c800f2848768695e1c8d5f184d9d4007f3ffcf14e79b08e0238860a | 59023 | Descartados os três recursos |
| 549702ba-6e99-469c-a72d-514978a4e189 | 9f860ab7c637c03894fe324cace532fef4c02fdb654742c382f98af5fbecb8ab | 798b049f72358fdda00dbaa56a54991bffc780a8431afa11057879e220d810ab | 54791 | Descartados os três recursos |
| 610f94c1-e727-490a-97cf-3b38f2d80002 | 266dc3c2d090d2faa64a52029dae024237150f8e1fc93f30e875166cce0b5653 | 875829612c272d6d34eaee669abaa8eaed186780eaee36369bbdf80666bdb307 | 57428 | Descartados os três recursos |
| 66befef5-f8cd-4e26-9670-0e9606c9db80 | f26845d948854a643af4c1831d573d7555ae32af8586a2811c397488702fb654 | 52f1122614a89e374f4749efa5d2a5b762e78e1cc0d30669b49e4b38e9aaf752 | 58863 | Descartados os três recursos |
| 83dee34d-fae0-45c4-a8d9-6bae11d37d7e | 9b74367a59a6c22fb455d0c212868a7da632ffc01d2b98127c8a89ca67faa298 | f90ecd0c646c19a5b1d81509c2c3b41c448f1b91589474593189342378ae59d9 | 56608 | Descartados os três recursos |
| 8d78f713-374d-4fab-a8bf-318db0622821 | 80bdbe5e6251ca1a8dffd23e5318e3c4ebc96c0866cdff707cdf09dc2f1995e8 | 69fc6c1f92adc3d2a914f5599ca12fd9b2a5dbcead4ca829258c784fe1860a02 | 62412 | Descartados os três recursos |
| 916d7796-6b2b-41e5-8881-c55fc737b579 | 6ec8b01e0bab7cb2f4f4c3bd6596385513a1c4744b88cf97bcd77f6c7b378fe1 | 20159148aa0ad1bab646b4873971d8c332855f17cbb7aed639e5032d85b73e0a | 58750 | Descartados os três recursos |
| 9622406c-b845-4f5d-ac74-59145ec4b6ef | 76809d0613d861ba1e4c425e1e713f9526815daa71fa3ca510f3daa84f40be34 | 911adbc1d0e9e2d3c5ed6b1af43e96a63e502ce2e4a042ccdf3a30a39879343b | 61093 | Descartados os três recursos |
| 9c8297b6-06f4-4b3f-bf3a-9ef189bc8d48 | 1109b8a2550d63e34dea0d5639e233fbb15d91fcb0d31efa7c20775ad1e736ff | c3acb45033494f8441f25629bb766d12926d174f18e9bff9483e7757826196a3 | 61210 | Descartados os três recursos |
| b59efd2b-6614-4c74-9424-9c76b3a5c857 | 0e2ad17cfb2c150266b78724447717b2bad46f1b9e801cbc2304e7eb9ddfccf2 | 0ae9cbe085cd1653c93983a16260d27a268afce41bb1bddf96eee1b30567391f | 59788 | Descartados os três recursos |
| e81deda4-f909-4cf1-bee4-4a83129b82db | aa06428dbc2e8ca4c019774412e600b73e8b817162f8f876eed419b1e1332a0d | c015c5e011a3b6f2ddbd8e7356912cf7c649641692dbbeed50ba93b412859a22 | 50980 | Descartados os três recursos |
| f6923816-7f6f-46b6-ade0-5d584100f222 | b0f018138f3faf8655a6018a2cfc959bf00c4a322cb15ce1739fcc3d81a34c3a | b8bfbcb838df9333f41a682f5539f9c9a5261be1dfce40dbda1836cce38c1026 | 54164 | Descartados os três recursos |
| fb8f27a3-514d-41b2-9e09-bfbb52a9a65d | e845b859af42564c6054418f2265be2a16fe8e85fd095a3a97078901779a1777 | b49ba259a4817a5ffd440f76e53b3ae54874092e0ace949fe3526718ecb255bf | 58843 | Descartados os três recursos |
