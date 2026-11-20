# Tarefa 9.4 — HTTPS/runtime e recuperação local

Execução autorizada e realizada em 01/11/2026, alternativa B: terminação HTTPS em Nginx e encaminhamento interno nativo reconhecido exclusivamente do proxy identificado. Nenhuma hospedagem pública, banco/volume de desenvolvimento, credencial real ou alteração das migrations/regras financeiras.

## Implementação

- compose.operacional.yaml autônomo, separado do Compose de desenvolvimento. Apenas 127.0.0.1:8443 publicado; redes ingresso/API/banco distintas, duas últimas internas. Backend e PostgreSQL sem portas publicadas.
- Perfil operacional fail-closed: origem https://gestaoacoes.localhost:8443, auth.allow-local-http=false, forwarding native com proxy IPv4 exato /32 e quatro headers previstos. Recusa perfis locais misturados, datasource alternativo, origem/proxy/headers incompatíveis e segredos inválidos antes de instanciar infraestrutura.
- Configtree obrigatório com arquivos externos privados; JWT/HMAC independentes de 48 bytes, estáveis em reinícios. Backend UID 10001, diretório de segredos privado e mounts somente leitura. Role aplicação distinta do provisionador; bootstrap one-shot removido após execução, sem credenciais no backend normal.
- Nginx HTTPS exige Host/porta públicos, mantém Origin e sobrescreve forwarding. Location adaptado ao prefixo /api; OpenAPI mesma origem. Segurança existente, BCrypt, JWT-cookie, CSRF, ADMIN/USER e regras financeiras preservados.
- Executor Node com opt-in, preflight/manifesto, CA exclusiva sem instalação global, builds de artefatos atuais, isolamento/verificação de propriedade e descarte por IDs. Backup sintético criptografado externo, restauração em outro banco novo, comparação integral e invalidação antes de reabrir.
- Runbook completo: docs/OPERACIONAL-HTTPS.md; README, PRD e AUTENTICACAO atualizados sem sobrescrever outras mudanças locais. Nenhum job de backup/limpeza ou infraestrutura de produção.

Bases verificadas linux/amd64, contexto desktop-linux, engine local 29.7.2:

| Base | Digest |
|---|---|
| PostgreSQL 17.11-alpine3.24 | sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb |
| Nginx 1.30.6-alpine3.24 | sha256:0e52bd054126cdc040b1276c0af347aeb02a41b9cfe305d6094767ac30e97949 |
| Eclipse Temurin 17-jre-jammy | sha256:c35794d1348777e6bc310b9b7f420098ef8f37e988199549b545fded79a895a2 |

## Evidência de aceite real

Rodada aprovada: **4cd21ffa-415c-46d0-9328-7dde641dc3c3**. Manifesto sanitizado em target/auth-https/UUID/manifesto.json; saída em target/auth-https/topologia-quinta.log. Quatorze grupos de verificações passaram:

| Critério | Evidência |
|---|---|
| DNS, CA/SAN e HTTPS | Nome resolve para loopback; OpenSSL verify e clientes Node/curl com CA explícita. CA desconhecida e SAN incorreto recusados, sem TLS bypass. |
| Isolamento antes de migrations | Labels/IDs/contexto/imagens/redes/membros, banco/role/marcador e portas conferidos; PostgreSQL 17.11, aplicação não superusuária. |
| Bootstrap | Comando sem servidor HTTP concluiu e container foi removido; serviço normal sem senha/profile de bootstrap. |
| Cookie/CSRF/reload | Secure, HttpOnly, Lax, Path=/, sem Domain, Max-Age positivo <=14400; JWT ausente do JSON; login e CSRF reais; reload não altera última atividade/expiração. CSRF inválido/ausente negado. |
| Matriz financeira | Anônimo 401; ADMIN cria carteira sintética; USER consulta e recebe 403 ao escrever/administrar. Location preserva origem/porta/prefixo. |
| Origem/forwarding | Host incompatível e porta ausente 421; Origin externo ou sem porta pública 403. Headers de cliente sobrescritos; OpenAPI /api e Swagger GET acessíveis. |
| Segredos | Backend lê arquivos somente leitura; UID alheio não consegue ler; CA privada não montada. |
| Backend direto | Sem publicação de porta. Peer interno fora do IP confiável, com HTTPS/XFF forjados, recebe 400 HTTPS_OBRIGATORIO. |
| IP/rate limiting | Dois containers clientes com IPs reais diferentes: primeiro atinge cinco falhas/429+Retry-After, segundo ainda recebe 401. XFF alternado não evade limite. Hashes persistidos correspondem aos IPs reais, sem publicar seus valores de segurança. |
| Revogação | Bloqueio nega cookie antigo; habilitação não restaura sessão; logout nega cópia antiga do cookie. |
| Reinício | Aplicação parada, PostgreSQL reiniciado e aplicação retomada: sessão, identidade, última atividade, expiração e autorização pendente preservadas; arquivos JWT/HMAC idênticos. |
| Backup/restore | pg_dump public aberto somente em memória e AES-256-GCM no disco externo; restore transacional em banco restauracao novo, distinto de homologacao. Comparação integral de todas as tabelas public, incluindo registros financeiros, auditoria e Liquibase, antes da reabertura. |
| Recuperação | Incremento de versões/revogação/invalidação sob manutenção, chaves novas, JPA validate/Liquibase compatíveis; cookie/código restaurados negados, novo login e leitura financeira funcionam. |
| Descarte | Todos os recursos exclusivos removidos e inventário Docker final sem label da execução. |

Não se tratou de ensaio de navegador interativo nem de espera manual de dez minutos/quatro horas. Transporte real foi testado com clientes HTTPS de confiança isolada; limites temporais/abas continuam sustentados pelos testes de Clock/Angular e PostgreSQL anteriores. Não houve importação de CA global ou contato com providers reais.

## TDD, falhas intermediárias e regressão

Fronteiras utilizadas: validação pública do runtime, preflight/propriedade do executor e contratos HTTP/TLS na topologia real.

- runtime-red.log: falha esperada por aceitação de proxy amplo; runtime-green-1.log: passou após implementação.
- runtime-red-2.log: falha esperada por aceitação de configuração/segredo inválido; runtime-green-2.log: passou.
- runtime-red-3.log: falha esperada por aceitar header confiável não previsto; runtime-green-3.log: três testes passaram após validação de headers/datasource alternativo.
- runner-red.log: duas falhas esperadas por ausência de guardas de opt-in/propriedade; runner-green.log e reexecução: dois testes passaram.
- Não se alegam ciclos RED/GREEN independentes para cada assertiva nem se fabrica TDD retroativo de código preexistente. A exceção histórica 2.2 permanece exatamente como aprovada.
- Quatro rodadas reais intermediárias falharam e foram preservadas: a37df4ff-1bf5-49f3-b014-89c9dd2d7a36 (expectativa exata de Max-Age); 777b318f-0255-4c9c-bc86-e07c755a2b7e (cliente sobrescrevia seu CSRF inválido); fb369148-2e8c-46ad-848c-05bdc02bf288 (expectativa de Location necessariamente relativo); 29a24d33-d819-41eb-8021-39e76fda3e03 (preparação do schema vazio de destino do restore). Corrigidos cliente/asserções e preparação do banco novo, sem enfraquecer segurança, regras de negócio ou migrations. Uma recusa anterior de ACL Windows ocorreu antes de criar recursos Docker; corrigida identificação do SID.

Validações finais:

| Validação | Resultado |
|---|---|
| Maven verify sequencial, PostgreSQL condicionado desabilitado | **2674 testes: 2467 aprovados, 207 condicionais não habilitados, zero falhas/erros** |
| JaCoCo global, 279 classes | **4303/4475 linhas = 96,1564%; 2328/2749 ramos = 84,6853%**; ambos portões >=80% satisfeitos |
| Angular test:coverage, um worker | **1120 testes/98 arquivos aprovados**; linhas 3058/3109 = 98,35%; ramos 3067/3348 = 91,60% |
| Angular build | Aprovado; warning preexistente de orçamento SCSS portfolio-evolution (5,68kB para 4kB), sem alteração visual |
| OpenSpec strict / diff-check | Validações finais registradas nesta execução; nenhuma operação Git de escrita |

A suíte Maven inclui autenticação, administração, sessões, redefinição e regressão financeira. Os 207 condicionais não habilitados não são evidência nova de concorrência PostgreSQL; 10.4 já foi aprovada e suas evidências permanecem preservadas. Esta homologação usa PostgreSQL real para TLS/runtime/restart/backup e não reclassifica testes ignorados como aprovados.

## Recursos, arquivos e limites

Cinco execuções com manifestos: **47 containers/redes/volumes e 15 imagens de aplicação exclusivas**, todos removidos. Bases oficiais preservadas. Credenciais, certificados e backup criptografado ficam somente nos diretórios externos privados indicados nos manifestos, preservados para revisão; não confundir com recursos Docker ainda ativos. Não foram removidos materiais preexistentes.

Arquivos novos: compose.operacional.yaml; ops/backend.Dockerfile, frontend.Dockerfile, postgres.Dockerfile, postgres-init.sh, nginx.conf, homologacao.cjs e homologacao.test.cjs; application-operacional.properties; OperationalRuntimeConfig.java; OperationalRuntimeConfigTest.java; docs/OPERACIONAL-HTTPS.md e este relatório. Atualizações: .dockerignore, frontend/.dockerignore, .gitignore, .env.example, README, PRD, AUTENTICACAO e estado atual em proposal/design/tasks.

Limites: configuração homologada apenas para origem/localidade escolhidas e Docker Desktop Windows; CA de teste expira em dois dias; sem validação interativa de navegador, deploy público, backup automatizado, keyring, custódia externa de chaves ou SLA/RPO/RTO de produção. A limitação do Swagger Try it out permanece, com Postman como alternativa aprovada. A pendência preexistente de rotação externa de credencial anteriormente exposta no histórico Git não é resolvida por este recorte; nenhuma credencial antiga foi lida/reutilizada.

Baseline SHA-256 de 1235 arquivos e status inicial de 227 entradas preservados em target/auth-https. Conferência final: dez arquivos anteriores alterados exclusivamente no escopo, treze criados, nenhum removido e demais 1225 arquivos idênticos ao baseline. Git final: 86 modificados, 158 não rastreados, nenhum staged (244 entradas detalhadas). Os espaços duplos de quebra Markdown no início do PRD já existiam em HEAD e foram preservados; diff-check não aponta erro novo. A primeira linha preexistente do README também foi preservada.

OpenSpec --strict aprovado; git diff --check com saída zero. Varredura de 35 arquivos privados de fixtures contra arquivos visíveis ao Git não encontrou segredos de homologação. Graphify atualizado por AST local, sem LLM/API: 14508 nós, 31091 arestas e 954 comunidades. Avisos de 12 arquivos sem nós, parser SQL ausente e nomes de comunidades antigos foram registrados, sem instalar dependências ou gerar labels por LLM; isso não altera os testes/artefatos de produção.

9.4 marcada como concluída após suas verificações; change com 56/56, aguardando revisão. Nenhuma alteração em migrations, regras financeiras ou componentes Angular; nenhuma operação git add/commit/push/merge/archive. Aguardar revisão, sem arquivar automaticamente.
