# Fechamento documental e ferramentas — aceite local verificado

Recorte autorizado em 31/10/2026: 2.2 por exceção processual, planejamento de 6.5 e implementação/validação de 9.1–9.3. Não autoriza infraestrutura de produção 9.4, execução PostgreSQL 10.4, limpeza automática, alteração financeira, modernização visual ou operações Git de escrita.

Baseline anterior às edições: 1.215 arquivos SHA-256, 78 rastreados modificados, 125 não rastreados e zero staged; evidências em target/auth-fechamento/baseline.json e status-inicial.txt. Preservar todas as alterações anteriores, inclusive README, frontend, backend, migrations, testes e ZIP.

## 2.2 — conclusão por exceção processual explicitamente aceita

Redação original: “Implementar BCrypt e login em ciclos separados: credencial válida, incorreta, inexistente, bloqueada, não inicializada e senha acima de 72 bytes UTF-8; usar resultados independentes.”

Comportamentos já implementados e verificados: BCrypt com salt e custo 10; autenticação por username/senha; recusa de senha incorreta, identidade inexistente, conta bloqueada e conta sem credencial; mínimo de oito caracteres/máximo de 72 bytes UTF-8, sem trim ou truncamento; resposta equivalente para credenciais inválidas de contas inelegíveis. Senha fora do formato tem validação própria, sem revelar existência de conta. JWT só no cookie, sem senha/hash nas respostas.

Testes existentes: CredenciaisBootstrapTest.bootstrapPermiteAutenticarPrimeiroAdministradorComSenhaRuntime, senhaIncorretaNaoAutenticaNemMudaSenha, identidadeInexistenteUsaMesmaResposta, bloqueadaNaoAutenticaMesmoComSenhaCorreta, contaSemCredencialNaoAutentica, limiteDeSenhaConsideraBytesUtf8SemTruncarOuTrim e bcryptUsaSaltEHashPersistidoNuncaContemSenha; contratos HTTP em AutenticacaoHttpTest.contasInvalidasTemMesmoContratoHttpSemSegredos e loginEmiteCookieProtegidoEReloadRestauraIdentidade. Evidências históricas em backend-integrado.md e suíte completa posterior em etapa-5-redefinicao.md.

Lacuna histórica: não existem evidências de RED individual anterior à implementação para todos os cenários negativos. Testes verdes posteriores não reconstituem a ordem histórica. Nenhum teste/log foi fabricado, nenhuma proteção correta foi removida e nenhuma falha artificial foi produzida para simular TDD.

Aceitação: neste pedido, o solicitante declarou expressamente “Aprovo o registro formal da exceção histórica referente à ausência de evidências RED anteriores à implementação para determinados cenários negativos” e determinou registrar a conclusão por exceção. Justificativa: comportamento e regressão comprovados, lacuna de processo explicitada e ausência de benefício em reescrever funcionalidade correta apenas para criar falha artificial. A tarefa é concluída **por exceção processual aceita**, sem afirmar que todos os ciclos RED/GREEN ocorreram. Essa aceitação não dispensa TDD dos novos comportamentos deste recorte.

## 6.5 — plano

Consultar plano-limpeza-autenticacao.md. A medição de cem interações/duas chamadas sequenciais já está verificada em SessionCoordinator. Em 31/10/2026 o solicitante aprovou explicitamente sete dias para sessões/autorizações após os prazos finais e 24 horas para contadores após fim da janela, preservando auditoria/usuários/singleton. Plano inclui elegibilidade, retenção, lotes, frequência, falhas, locks/transações e independência da decisão de acesso. 6.5 é concluída no escopo de planejamento/medição; nenhum job ou exclusão implementado/executado.

## 9.1–9.3 — implementação e evidências focadas

OpenAPI declara somente cookieAuth (apiKey, cookie GESTAOACOES_AUTH), segurança por operação e exceções públicas explícitas. Documenta papéis, CSRF obrigatório, handshake/rotação, cookies, limites, erros, Retry-After, Location e no-store; DTO seguro separa entradas writeOnly de código administrativo readOnly. Anotações ocultam argumentos de injeção Spring: CsrfToken não é query enviado pelo cliente; JWT HttpOnly não é parâmetro Cookie preenchido manualmente. Não altera handlers, filtros, regras financeiras ou autenticação. JSON e YAML são obtidos pelos endpoints reais e comparados, não por snapshots simulados.

Evidências RED/GREEN em target/auth-fechamento (arquivos ignorados, sem segredos):

| Ciclo | RED real | Verificação posterior |
| --- | --- | --- |
| 01 | cookieAuth ausente (01-cookie-red-efetivo.log) | esquema exclusivo correto |
| 02 | segurança por operação ausente | públicas/protegidas e papéis |
| 03 | header CSRF obrigatório ausente | mutações exigem X-XSRF-TOKEN |
| 04 | respostas 401/403/503 ausentes | contratos financeiros preservados com erros de segurança adicionais |
| 05 | senha sem writeOnly | DTOs sem campos sensíveis de saída |
| 06 | fluxo Postman ausente; posteriormente JSON de quatro corpos incompleto | scripts, corpos escapados e exportação segura |
| 09 | handshake inventava query token obrigatório | GET /auth/csrf sem parâmetro e HTTP 204 |
| 10 | exemplo de erro de segurança com status incorreto | exemplos específicos e respostas dos fluxos públicos coerentes |
| 11 | logout/atividade inventavam parâmetro JWT manual | cookie somente no security scheme |

A primeira tentativa Maven falhou por restrição ambiental de loopback e não constitui RED funcional. Na validação YAML, o harness foi corrigido para UTF-8 em vez do decoding padrão ISO-8859-1; nenhuma asserção de contrato foi removida. A verificação inicial de Swagger procurava CSRF no JSON swagger-config; a versão instalada o configura no JavaScript swagger-initializer efetivamente servido, agora testado. Essa correção do harness não é apresentada como RED funcional. A contagem de parâmetros do teste financeiro passou de um a dois pela adição legítima do header CSRF, preservando todas as asserções de negócio.

Postman: pasta isolada de 28 passos usa cookie jar, handshake antes das mutações e depois de login/logout, cadastro/definição inicial, consultas, perfil/situação, emissão/redefinição e demonstrações 200/401/403. Variáveis exportadas de credenciais estão vazias; código somente em pm.variables temporário, removido após consumo; sem Cookie/Bearer manual, redirects ou retry de escrita. Guard exige confirmação de ambiente descartável e destino restrito; teste negativo comprovou zero requests sem confirmação. Quatro testes estáticos validam scripts, JSON com caracteres especiais, fluxos e ausência de credenciais configuradas. Subárvores financeiras anteriores são comparadas integralmente com a cópia inicial.

Newman 6.2.3 instalado somente sob target/auth-fechamento/runner (não dependência da aplicação). Execução utiliza ADMIN de fixture runtime e H2 exclusivo em memória, loopback, providers direcionados a porta local indisponível e sem dados reais. Relatório exporta só contagens e nomes de asserções; não respostas, códigos, cookies ou ambiente. Foram 28 itens, 41 requests (13 handshakes internos) e 44 asserções sem falhas. O harness inicialmente esperava 28 requests; foi corrigido para distinguir itens de handshakes, sem alterar asserções do fluxo. Problema real de JSON da coleção foi detectado e corrigido antes da primeira escrita de login.

PRD/README e docs/AUTENTICACAO.md registram matriz ADMIN/USER, compartilhamento, fronteiras de autorização, onboarding/bootstrap, códigos/política de senha, versões/revogação e seus limites, runtime e pendências. README preexistente foi editado pontualmente, preservando inclusive a alteração local anterior no título. Não há declaração de prontidão de produção.

Swagger: springdoc instalado gera interceptor CSRF no swagger-initializer.js, com verificação same-origin, cookie XSRF-TOKEN/header X-XSRF-TOKEN e sem Web Storage/persistência de autorização, coberto por teste do recurso real. Navegador em origem loopback exclusiva comprovou handshake 204 sem query fictício e consulta financeira anônima 401/AUTENTICACAO_NECESSARIA. Limitação real: Try it out exige preenchimento do header obrigatório antes do interceptor; login com corpo válido e header vazio foi bloqueado no cliente com “Required field is not provided”. Não houve bypass ou relaxamento. 9.2 permite expressamente documentar essa limitação e usar Postman como alternativa; não se declara suporte completo de mutações Swagger. Aba e servidor exclusivos foram encerrados antes da suíte completa.

9.4 e 10.4 continuam pendentes. Não houve implementação de infraestrutura, job, execução PostgreSQL ou migration sobre dados reais.

## Aceite final — 31/10/2026

| Verificação | Resultado |
| --- | --- |
| OpenApiDocumentationTest focado final | 28 aprovados, zero falhas/erros/ignorados; 11-cookie-injetado-green.log |
| Testes estáticos da coleção | Quatro aprovados; node --test docs/api/validar-colecao.test.cjs |
| Newman contra build final | 28 itens, 41 requests, 44 asserções, zero falhas; 08-newman-final-build.log |
| Guard sem confirmação de ambiente | Recusa esperada e zero requests; 08-guard-final.log |
| Coleção financeira anterior | Subárvores anteriores integralmente iguais à cópia inicial (deepEqual), sem alterar exemplos/contratos |
| Swagger real same-origin | Handshake 204 e financeiro anônimo 401; limitação de mutação descrita acima |
| Maven verify completo sequencial | BUILD SUCCESS em 5min38s; 2.564 testes: 2.462 aprovados, zero falhas/erros, 102 condicionais ignorados; 12-verify.log |
| JaCoCo global | Linhas 4.269/4.441 = 96,1270%; ramos 2.297/2.709 = 84,7914%; ambos portões de 80% preservados e aprovados |
| OpenSpec --strict | Change válida; 15-openspec.log |
| git diff --check | Exit 0; somente avisos de conversão LF/CRLF, sem erro de whitespace |

Comando de regressão executado na raiz do backend (sem paralelismo e sem PostgreSQL real):

```powershell
.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" verify
```

A suíte completa inclui autenticação/cookies/CSRF, autorização, administração, reset, sessões, inatividade, revogação, concorrência/atomicidade/reinício em testes isolados e regressão financeira. Os 102 testes condicionais ignorados não são apresentados como homologação realizada; nenhuma evidência PostgreSQL real foi produzida. Relatórios padrão em target/surefire-reports e target/site/jacoco (HTML/XML). Não houve alteração Angular neste recorte: os 1.120 testes e cobertura anteriores permanecem evidência da integração aprovada, sem nova execução alegada.

## Arquivos e preservação

Comparação SHA-256 com os 1.215 arquivos da baseline: dez arquivos existentes alterados neste recorte, quatro criados e zero removidos. Os outros 1.205 arquivos permaneceram idênticos, inclusive frontend completo, migrations, infraestrutura e todos os demais testes/código. Inventário detalhado em target/auth-fechamento/inventario-final.json.

Existentes alterados:

- src/main/java/com/projeto/config/OpenApiConfig.java — documentação de contratos reais.
- src/main/java/com/projeto/resources/AutenticacaoResource.java — somente três anotações para ocultar argumentos injetados na documentação.
- src/main/resources/application.properties — somente configuração Swagger CSRF/armazenamento.
- src/test/java/com/projeto/resources/OpenApiDocumentationTest.java — nove testes adicionais e adaptação das expectativas de segurança documentada, preservando asserções financeiras.
- docs/api/gestao-acoes.postman_collection.json — scripts/fluxos seguros e variáveis vazias; exemplos financeiros preservados.
- README.md e docs/PRD.md — documentação operacional/escopo aprovado, preservando alterações locais anteriores.
- proposal.md, design.md e tasks.md desta change — somente estado atual e rastreabilidade do fechamento, preservando os IDs e o planejamento histórico.

Criados:

- docs/AUTENTICACAO.md — guia operacional e ferramentas.
- docs/api/validar-colecao.test.cjs — testes estáticos da coleção publicada.
- fechamento-documentacao.md e plano-limpeza-autenticacao.md nesta change — evidências, exceção processual e plano aprovado.

Arquivos temporários de runner, fixture, logs, cópias e inventários ficaram sob target/auth-fechamento, ignorado pelo Git. Nenhum artefato de aplicação ganhou credencial fixa. Exportação Postman mantém variáveis sensíveis vazias; revisão dos arquivos do recorte não encontrou senha runtime da fixture, JWT utilizável, chave privada ou variável do harness incorporada. Não se afirma varredura/rotação do histórico Git: a pendência de credencial PostgreSQL histórica já registrada no README permanece operacional.

Atualização AST do grafo concluída sem LLM/API (zero tokens de LLM): 14.321 nós, 30.663 arestas e 956 comunidades, somente outputs ignorados. Avisos conhecidos: doze JSON/configurações sem nós e parser SQL opcional ausente; nomes de comunidades usam fallback local. Não foi instalado parser nem executada rotulagem semântica paga. O grafo não substitui os documentos/contratos e testes diretamente verificados.

Git final: **82 arquivos rastreados modificados, 129 não rastreados e zero staged**; snapshot completo em target/auth-fechamento/status-final.txt. Antes eram 78/125/zero. Nenhum arquivo foi apagado, restaurado, movido ou preparado; alterações anteriores fora do recorte foram preservadas pelo inventário. Não foram executados git add, commit, push, merge ou arquivamento.

## Tarefas e limites restantes

Concluídas neste recorte: **2.2 por exceção processual aceita; 6.5 como plano/medição aprovado; 9.1, 9.2 e 9.3 com os aceites acima**. Total atual: 54/56. 9.2 conclui a alternativa explicitamente prevista de documentar a limitação Swagger e validar Postman, sem declarar mutações Swagger plenamente operacionais.

Permanecem desmarcadas **9.4** (HTTPS, mesma origem/proxy confiável e segredos runtime/Compose) e **10.4** (homologação PostgreSQL real da migration 009 e concorrência em ambiente descartável autorizado). A configuração atual não é aceite de produção; a credencial histórica deve ser tratada pelo operador antes de eventual reutilização. Limpeza automática futura exige implementação/testes/autorização próprios. Nenhuma proteção de cookie, CSRF, origem ou autenticação foi reduzida para contornar as pendências.

Implementação interrompida ao final deste recorte, aguardando revisão do solicitante. Não foi iniciada infraestrutura de produção, homologação PostgreSQL, funcionalidade financeira adicional ou modernização visual.
