# Etapa 1 — persistência de autenticação

Execução autorizada pelo pedido de 30/10/2026 exclusivamente para persistência, migrations e testes isolados. Essa autorização não se estende às etapas 2–10.

Revisão posterior autorizada exclusivamente para organizar a tarefa 1.3: persistência entregue separada do serviço funcional pendente na etapa 4. Esta revisão altera somente tasks.md e este relatório; não executa novamente testes de aplicação nem altera código, migrations, testes ou as evidências históricas abaixo.

## Fronteiras e escopo

As interfaces testadas são repositories/EntityManager públicos, contratos SQL/Liquibase em H2 descartável, DTO público e construtor do modelo. O pedido atual autoriza essas fronteiras de persistência. Não foram criados serviços funcionais, controllers, Spring Security, JWT/cookies/CSRF, inatividade funcional, redefinição funcional ou Angular. A estrutura física prepara essas funcionalidades, mas não as executa.

O backend real usa `com.projeto.entities`, `com.projeto.repositories` e `com.projeto.dto`; os novos tipos seguem essas camadas, isolados em subpacotes `autenticacao` para entidades/repositories. Não foi copiado o package `domains` mencionado conceitualmente no planejamento.

## Tarefas

- 1.1: aprovação de implementação restrita a esta etapa registrada pelo pedido; nada autoriza continuar automaticamente.
- 1.2: Git e convenções conferidos; Boot 4.1.0, BOM Security 7.1.0 (não adicionado), migrations existentes até 008. Fingerprints dos arquivos preexistentes registrados antes das edições em `target/auth-stage1/baseline-files.json`.
- 1.3: concluída no escopo de persistência efetivamente entregue: Usuario/Papel, UsuarioRepository, normalização/unicidade e UsuarioResponse com serialização sem senha/hash, validados por testes de modelo, persistência H2 e contrato do DTO. Não inclui serviço funcional ou resposta HTTP implementada. A responsabilidade de serviço público está explicitamente pendente em 4.1–4.5, com consulta/listagem em 4.1 e cadastro em 4.2; não houve implementação nem eliminação desse trabalho.
- 1.4: migration de usuários, singleton, auditoria mínima e integridade, validada somente em H2 descartável.
- 1.5: modelos, repositories e migration de sessões, autorizações e contadores, sem relações de propriedade com carteiras e sem lógica funcional de sessão/reset/rate limiting.

### Organização final da antiga responsabilidade de 1.3

| Responsabilidade | Tarefa e estado | Evidência ou requisito preservado |
| --- | --- | --- |
| Modelos, repository, normalização/unicidade e DTO seguro | 1.3 concluída | Testes de modelo, constraints SQL/JPA e serialização do DTO entregues na etapa 1 |
| Serviço público de consulta/listagem e integração HTTP | 4.1 pendente | ADMIN consulta/lista; USER negado; respostas sem segredos, conforme user-administration |
| Serviço público de cadastro e integração com código inicial | 4.2 pendente | Normalização/unicidade verificadas também pelo serviço, concorrência, conta sem credencial e código inicial integrado à etapa 5 |
| Serviço de perfil, bloqueio/habilitação e último ADMIN | 4.3–4.5 pendentes | Versão esperada, revogação, auditoria, códigos pendentes e concorrência mantidos |
| Homologação da migration 009 no PostgreSQL | 10.4 pendente | DDL, constraints/integridade e concorrência em ambiente descartável dedicado, mediante autorização futura |

Identificadores e total de 56 tarefas preservados, sem tarefa duplicada. O TDD via serviço público foi mantido na etapa funcional; nenhuma spec, requisito ou cenário foi removido ou alterado. As dependências de autenticação (etapa 2), autorização (etapa 3) e códigos de senha (etapa 5) continuam pendentes conforme o plano.

## Estruturas físicas

Migration aditiva `009-create-autenticacao.yaml`, changeset `009-create-autenticacao`, incluída ao final do master. Arquivos 001–008 preservados.

| Tabela | Responsabilidade persistente |
| --- | --- |
| usuario | Username canônico único; ADMIN/USER em texto; hash nullable enquanto sem credencial; ativo, credencial_definida, versao_seguranca, versão otimista e timestamps |
| sessao_autenticacao | sid UUID, FK de usuário, versão emitida, criação/última atividade/expiração absoluta, revogação/motivo e versão otimista; não armazena JWT |
| autorizacao_redefinicao_senha | ID UUID atribuído antes do INSERT, FKs de titular/autor, digest de verificação, motivo INICIAL/REDEFINICAO, prazos, tentativas restantes, consumo/invalidação e versão; não armazena código aberto |
| controle_seguranca | Única linha id=1, preparada para lock pessimista; não cria primeiro administrador nem aplica a regra do último ADMIN |
| auditoria_usuario | Ator/alvo, ação/resultado permitidos e timestamp, sem campos de segredo e sem subsistema avançado |
| contador_abuso | Tipo, chave em hash, janela, expiração, contagem e versão, com unicidade da janela; não executa limitação de tentativas |

FKs usam RESTRICT; constraints protegem papéis, username, consistência de credencial, versões não negativas, prazos, revogação, verificador, tentativas e estados exclusivos de autorização. Índices atendem acesso por usuário/prazo e expiração. TIMESTAMP WITH TIME ZONE e UUID são tipos suportados por PostgreSQL/H2. A única linha inserida pela migration é o singleton, sem usuário, senha ou código fixos.

Rollback de 009 é explicitamente bloqueado por StopChange, preservando identidades e auditoria conforme o design. Isso não altera nem contorna os bloqueios de 008. Recuperação operacional exige plano separado; nenhum rollback foi tentado em banco real.

O ID UUID da autorização é necessário porque D5 inclui esse ID no HMAC do código. IDENTITY obrigaria obter ID depois de inserir um digest ainda desconhecido; atribuir UUID antes da persistência elimina verificador provisório e coluna redundante. Não foi implementado HMAC nem geração funcional de códigos. Sessão e autorização, com IDs atribuídos pelo chamador, usam versão JPA `Long` inicialmente nula para que Spring Data reconheça entidade nova e faça INSERT, mantendo o valor persistido não nulo após o save. Usuários, auditoria e contadores seguem IDENTITY conforme as entidades existentes.

## TDD e revisão

Logs em `target/auth-stage1/` (artefatos locais de teste, não versionados):

| Ciclo | RED | GREEN |
| --- | --- | --- |
| 01 | Assertion de tabela USUARIO ausente | Migration cria usuário sem credencial e perfil persistido |
| 02 | Interfaces Usuario/Papel/repository ausentes, falha de compilação esperada | Persistência JPA e normalização, em H2 com ddl-auto=validate |
| 03 | Assertion de SESSAO_AUTENTICACAO ausente | Estrutura de sessão criada |
| 04 | Estruturas auxiliares ausentes | Singleton, auditoria, autorização e contador criados sem credenciais |
| 05 | Metamodelo somente com Usuario | Todos os seis modelos físicos representados por JPA |
| 06 | DTO público inexistente, falha de compilação esperada | JSON com os sete campos públicos previstos, sem hash persistido |
| 07 | Mutador persistente necessário ao teste de versão inexistente | Atualização obsoleta recusada por @Version e singleton serializando transações |
| 08 | Construtor da autorização com ID pré-atribuído inexistente | UUID disponível antes do INSERT e persistência compatível; 55 testes novos verdes |

REDs de compilação são estruturais pela ausência das interfaces propostas; não são apresentados como falhas funcionais de login/reset. Testes posteriores de constraints e hidratação são caracterização/regressão dos contratos, sem alegar TDD retroativo.

Depois de GREEN, houve revisão de imports e da compatibilidade dos testes históricos. `CustodiaMigrationH2Test` agora reconhece o master com 009. Testes de upgrade/rollback cujo contrato é especificamente 001–008 usam `src/test/resources/db/changelog/custodia-ate-008.yaml`, que referencia os arquivos reais sem copiá-los; seus validadores Hibernate excluem somente as entidades posteriores de autenticação. Preservam integralmente assertions financeiras, checksums, integridade e rollback de 008. O suporte de homologação PostgreSQL histórico recebeu o mesmo recorte, sem executar PostgreSQL/Docker.

O primeiro baseline integral e uma tentativa JPA no sandbox foram interrompidos por bloqueio do pipe de attach da JVM (access denied), não por regra de negócio. Os testes JPA foram repetidos fora dessa restrição com datasource H2 explícito e descartável. Não se alterou Mockito, JaCoCo, dependências ou portões para contornar o ambiente.

## Arquivos desta entrega

- `src/main/java/com/projeto/entities/autenticacao/`: Usuario, Papel, SessaoAutenticacao, AutorizacaoRedefinicaoSenha, MotivoRedefinicao, ControleSeguranca, AuditoriaUsuario, ContadorAbuso e TipoContadorAbuso.
- `src/main/java/com/projeto/repositories/autenticacao/`: UsuarioRepository, SessaoAutenticacaoRepository, AutorizacaoRedefinicaoSenhaRepository, ControleSegurancaRepository, AuditoriaUsuarioRepository e ContadorAbusoRepository.
- `src/main/java/com/projeto/dto/UsuarioResponse.java`.
- `src/main/resources/db/changelog/changes/009-create-autenticacao.yaml` e inclusão em `db.changelog-master.yaml`.
- `src/test/java/com/projeto/autenticacao/`: AutenticacaoMigrationTest, AutenticacaoPersistenceTest, AutenticacaoConcorrenciaTest e UsuarioModeloTest.
- `src/test/resources/db/changelog/custodia-ate-008.yaml`.
- Suporte de regressão ajustado: CustodiaMigrationH2Test, CustodiaUpgradeRollbackH2Test, CustodiaH2UpgradeTestConfiguration, CustodiaPostgresMigracoes19 e CustodiaPostgresConfiguracao. Nenhum teste financeiro foi removido ou enfraquecido.
- `tasks.md` e este relatório, nesta change. Proposal, design, specs, outras changes, POM e frontend preservados.

Os scripts de teste e seus logs/cobertura/fingerprints em `target/auth-stage1/` são artefatos gerados e não versionados. Graphify atualiza somente seus artefatos locais ignorados.

## Comandos reproduzíveis

Na raiz do repositório, regressão completa e portões:

```powershell
.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" verify
```

Testes focados: `.\mvnw.cmd -B "-Dtest=Autenticacao*Test,UsuarioModeloTest" test`. Log do GREEN final focado: `target/auth-stage1/green-08.log`. Log da regressão integral final: `target/auth-stage1/verify-final.log`. Relatórios JaCoCo em `target/site/jacoco/`. A primeira regressão integral (anterior ao ajuste de UUID) também passou: 2422 testes, zero falhas/erros e 102 ignorados; ela não substitui a regressão final do código ajustado.

## Validação final e limites

Regressão integral final concluída com BUILD SUCCESS: **2431 testes contabilizados, 0 falhas, 0 erros e 102 ignorados** (2329 executados sem falha). Os 55 testes novos passaram, sem ignorados. Os 102 ignorados pertencem às condições existentes de homologação/protótipo, que não foram habilitadas; não foram removidos para simular sucesso.

JaCoCo BUNDLE, incluindo produção não exercitada: **linhas 3627/3770 = 96,2069%; ramos 1879/2203 = 85,2928%**. Ambos os portões de 80% passaram, sem alteração de POM, limites ou exclusões. O relatório anterior disponível foi preservado: linhas 3524/3664 = 96,1790% e ramos 1877/2201 = 85,2794%. As métricas finais não diminuíram em relação a esse relatório; ele é preexistente, não uma suíte baseline nova concluída nesta execução. Entidades novas: 100/103 linhas (97,09%) e 2/2 ramos (100%); DTO novo: 3/3 linhas (100%). Os três pontos de linha não exercitados não foram ocultados por exclusão ou testes artificiais.

Comando final executado com jacoco.append=false, produzindo cobertura nova de toda a suíte, sem acumular os ciclos focados. Relatórios XML/HTML em target/site/jacoco e comparação em target/auth-stage1/coverage-summary.json. OpenSpec --strict passou. Graphify foi atualizado por AST, sem API/LLM; reportou limitações já locais de parser SQL ausente e alguns JSON sem nós, que não substituem nem invalidam os testes reais de Liquibase.

Organização final autorizada: tarefas **1.1, 1.2, 1.3, 1.4 e 1.5 concluídas**, limitadas às responsabilidades de persistência/H2 descritas. O serviço funcional de usuários permanece pendente em **4.1–4.5**. Nenhuma tarefa 2–10 foi marcada. Total da change: **5/56 tarefas marcadas e 51 pendentes**, incluindo a homologação PostgreSQL em 10.4. Aguardar revisão do usuário; não avançar para etapa 2 nem criar serviço funcional sem autorização específica.

**A homologação da migration 009 no PostgreSQL permanece pendente na tarefa 10.4.** PostgreSQL real não foi executado: compatibilidade do DDL é baseada nos tipos e comandos comuns já adotados no projeto (PostgreSQL 17/H2 2.5.250); concorrência e constraints verificadas na implementação são de H2. Concluir 1.3 não conclui nem dispensa essa homologação. Validação do último ADMIN e consumo/revogação funcionais dependem dos serviços das etapas posteriores, não de uma promessa de trigger ou restrição inexistente nesta entrega.

TDD, limites globais de 80% de linhas e ramos, rollback e testes financeiros existentes não foram enfraquecidos. Nenhum commit, stage, push, merge ou arquivamento está autorizado ou foi realizado. Aguardar revisão do usuário antes de continuar.

## Conferência do Git na implementação de persistência

Comparação SHA-256 dos 1106 arquivos preexistentes: somente os sete arquivos autorizados (master, cinco testes/suportes de migration e tasks.md) foram alterados. Vinte e três arquivos novos no escopo. README.md, o teste Angular e o ZIP já presentes no Git status inicial foram preservados integralmente; demais artefatos anteriores da change também. Detalhes locais em target/auth-stage1/scope-summary.json.

Git status --short (arquivos M/?? não foram staged; a pasta da change já estava não rastreada antes):

```text
 M README.md
 M frontend/src/app/shared/formatters/offset-date-time.formatter.spec.ts
 M src/main/resources/db/changelog/db.changelog-master.yaml
 M src/test/java/com/projeto/custodia/CustodiaMigrationH2Test.java
 M src/test/java/com/projeto/custodia/CustodiaUpgradeRollbackH2Test.java
 M src/test/java/com/projeto/custodia/postgresql/CustodiaPostgresConfiguracao.java
 M src/test/java/com/projeto/custodia/postgresql/CustodiaPostgresMigracoes19.java
 M src/test/java/com/projeto/custodia/support/CustodiaH2UpgradeTestConfiguration.java
?? openspec/changes/adicionar-autenticacao-e-gestao-de-usuarios/
?? revisao-movimentacoes-custodia.zip
?? src/main/java/com/projeto/dto/UsuarioResponse.java
?? src/main/java/com/projeto/entities/autenticacao/
?? src/main/java/com/projeto/repositories/autenticacao/
?? src/main/resources/db/changelog/changes/009-create-autenticacao.yaml
?? src/test/java/com/projeto/autenticacao/
?? src/test/resources/db/
```
