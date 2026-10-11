# Implementacao da alternativa A

Status: implementada, aguardando validacao final do usuario. Nenhuma task marcada. Inventario oficial local ja concluido na etapa anterior; nao foi repetido e nenhum teste/escrita acessou localhost:5432/gestaoacoesdb nesta implementacao.

## Producao: tres arquivos

- src/main/java/com/projeto/persistence/ReferenciaDataJdbcType.java: herda binder JDBC 4.2 LocalDate direto, tipo SQL DATE; extractor civil direto, delegando BC PostgreSQL ao codec anterior via getDate/JavaType.wrap. Dialect PostgreSQL determina a compatibilidade, nao o timezone. Abrange ResultSet e CallableStatement por indice/nome; ORM exercita ResultSet.
- src/main/java/com/projeto/entities/Acao.java: import e @JdbcType somente no campo referenciaData.
- src/main/java/com/projeto/entities/HistoricoCotacao.java: mesma anotacao localizada no campo referenciaData.

Nenhum TypeContributor, conversor global ou registro de tipo global. Outros LocalDate, timestamps, validacao da candidata, regras financeiras, lock, atomicidade e historico nao foram modificados. SQL DATE e migration 007 permanecem byte a byte iguais ao inicio desta etapa. Nenhuma migration adicional.

## Antes e depois

Antes: DateJdbcType atravessava java.sql.Date/calendario hibrido/timezone. H2 alterava datas antigas; PostgreSQL normalizava entradas de 5-14/10/1582 para 15-24/10. Depois: novas DATAs 0001-9999 da matriz sao escritas e relidas como o DATE civil exato, inclusive todos os dias de outubro1582, sem alterar qualquer valor existente.

Leitura de SQL BC PostgreSQL continua com a interpretacao anterior (ex.: 1 BC -> AD0001 no codec legado). Casos BC bissextos anteriormente ilegíveis continuam propagando DateTimeException, sem apagar referencia ou transformar em DESCONHECIDA. O driver pode produzir esse erro ja em getObject, dependendo do modo de transferencia; as assercoes comparam a causa-raiz antiga e da leitura entidade/API. Novas candidatas continuam proibindo BC; fixtures fora do dominio sao somente testes de leitura legada.

O contrato de DATA sem piso1970/comparacao futura, INSTANTE com piso1970/microssegundos exatos/referencia <= obtencao propria/tolerancia zero, obtencao separada e legado dataHoraCotacao continuam intactos e cobertos pela suite existente.

## Testes efetivamente executados

- mvn -q test: 100 suites, 1.066 casos reportados, 975 executados, 91 skips opt-in/diagnosticos, zero falhas/erros. Log: target/alternativa-a-backend.log; XMLs Surefire. Inclui cadastro, historico, atualizacao individual/lote, cooldown/reuso, rejeicao sem escrita parcial, ordenacao, locks e atomicidade.
- npm test -- --watch=false: 70 arquivos, 660 testes, zero falhas. Log: target/alternativa-a-frontend.log. Nenhum codigo frontend modificado.
- npx tsc -p tsconfig.app.json --noEmit e tsconfig.spec.json --noEmit: exit0.
- Matriz backend por JVM UTC, America/Los_Angeles, Pacific/Kiritimati: 205 testes por fuso, zero falhas/erros/skips; H2 41, PostgreSQL JPA/API 89, aplicacao individual PostgreSQL44, lote PostgreSQL31. Logs: target/alternativa-a-temporal-{UTC,Los_Angeles,Kiritimati}.log. Total615 execucoes.
- Matriz frontend em cada um desses fusos: 7 arquivos, 134 testes por fuso, zero falhas. Inclui formatter temporal/offset, guards e quatro superficies (lista Acoes, detalhe Acao, posicoes, desempenho). Logs: target/alternativa-a-superficies-{UTC,America_Los_Angeles,Pacific_Kiritimati}.log. Total402 execucoes. Rodada adicional de 51 testes de formatter/guards em cada fuso tambem passou.

ReferenciaDataJpaDiagnosticoTest foi ampliado de 12 para 41 datas: 0001-01-01, 9999-12-31, bissextos e fronteiras antigas, mais cada dia de outubro1582. Grava Ação/historico, flush, clear, consulta coluna real como SQL DATE/getObject, recarrega ambas entidades e verifica GET real via MockMvc. Nao e apenas conversao em memoria.

ReferenciaDataJpaPostgresqlTest adiciona as mesmas41 datas escritas pelo codec antigo e relidas pelo novo (preservando a normalizacao observavel anterior), quatro BC legadas legiveis e tres BC bissextas com erro legado, verificando entidades e API. Ambas as classes usam os bindings anotados de producao, sem override de tipo do prototipo. Nenhuma nova prova de conceito; o prototipo opt-in antigo nao foi reexecutado para ratificar producao.

PostgreSQL 16.8 temporario exclusivo em target/jdbc-bc-poc-pg, porta55452, usuario de testes explicito; diretorio conferido antes de resetar schema temporario entre rodadas. Liquibase foi executado somente pelos testes nesse banco temporario e nos H2 efemeros. O cluster foi encerrado ao concluir. Nenhuma recriacao/limpeza/migration no banco local de desenvolvimento.

## Ocorrencias intermediarias

Frontend inicialmente bloqueado pelo sandbox (esbuild spawn EPERM); repetido com autorizacao de subprocesso e aprovado. A primeira assercao extra BC exigia getObject bem-sucedido onde o driver tambem pode reproduzir o erro antigo; corrigida para conferir DATE BC armazenado e comparar causa-raiz legada, entidade e API. Reexecucao no schema temporario reutilizado encontrou fixtures de concorrencia anteriores; schema descartavel resetado com diretorio de cluster conferido, depois todas as matrizes passaram. Nao houve relaxamento do contrato ou mudanca de producao para fazer testes passarem.

## Riscos e tasks

OpenSpec strict da change e 35 specs consolidadas: aprovados. git diff --check e whitespace dos arquivos desta correcao: aprovados. Graphify AST-only atualizado: 9.670 nodes/17.536 edges/793 comunidades; classe JDBC presente no grafo. Avisos nao bloqueantes: seis configs sem nodes, SQL sem parser tree_sitter_sql instalado e labels parcialmente defasados; nenhum pacote instalado ou chamada LLM para labels. Log: target/alternativa-a-graphify.log. Diff revisado, incluindo alteracoes preexistentes preservadas. Hashes confirmam que apenas as duas entidades mudaram entre os arquivos de producao anteriores, com uma nova classe JDBC; demais arquivos de producao, inclusive migration007, e tasks.md permanecem iguais.

Sem bloqueante de persistencia identificado na matriz aprovada. Importacoes/restauracoes futuras com AD SQL 5-14/10/1582 ou codecs desconhecidos exigem avaliacao; o inventario atual nao prova procedencia universal. BC que ja falhava permanece falhando. Compatibilidade depende do comportamento JDBC/Hibernate: manter regressao em upgrades. A matriz nao enumera todos os dias 0001-9999, e procedures/CallableStatement nao foram exercitados em integracao.

9.1 e 12.3 agora possuem evidencias automatizadas da correção de persistencia, formatter, guards, parsing lossless e quatro superficies nos tres fusos. Podem ser submetidas a ratificacao do usuario; permanecem abertas. 12.1 permanece pendente de validacao visual/assistiva apropriada, nao realizada nesta etapa. 9.2 permanece concluida. Contagem: 40 tasks, 27 concluidas, 13 pendentes. Nenhum staging, commit, push, merge ou archive.
