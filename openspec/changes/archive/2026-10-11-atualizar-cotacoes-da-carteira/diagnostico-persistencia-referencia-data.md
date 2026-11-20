# Diagnóstico de persistência de referenciaData — 2026-10-10

Estado atual: alternativa A aprovada e implementada, aguardando validacao final. O inventario oficial local confirmou apenas DATA 2026-10-02 nas duas tabelas, sem BC, intervalo ambiguo ou valores fora do dominio. O binding pontual direto com leitura BC legada foi incorporado exclusivamente nos dois campos referenciaData. As secoes abaixo preservam o diagnostico historico, inclusive bloqueios/prototipos retirados; o resultado atual e as evidencias estao em implementacao-alternativa-a.md.

## Versões efetivamente utilizadas

Java Temurin 17.0.14; Hibernate ORM 7.4.1.Final; H2 2.4.240; driver PostgreSQL 42.7.11; servidor PostgreSQL temporário 16.8. Versões confirmadas por java -version, classpath dos XMLs Surefire, DatabaseMetaData e logs do contexto JPA. Não confundir as outras versões disponíveis no cache Maven com as utilizadas.

## Caminho e causa comprovados

1. A candidata e os construtores recebem LocalDate gregoriano proléptico correto. Acao/HistoricoCotacao guardam o mesmo valor em memória, e o mapper inicial o copia corretamente.
2. Sem override pontual de JDBC, LocalDateJavaType.unwrap no Hibernate converte para java.sql.Date.valueOf(LocalDate). DateJdbcType usa PreparedStatement.setDate, sem Calendar explícito para LocalDate. Bytecode dos jars instalados inspecionado com javap.
3. java.sql.Date.valueOf usa o construtor legado baseado em java.util.Date. A ponte usa calendário híbrido juliano/gregoriano e timezone default, não o calendário civil proléptico de LocalDate. O instante em milissegundos pode representar outra data no calendário proléptico. Em UTC, 0001-01-01 vira epochMillis -62135769600000, correspondente a 0000-12-30 nesse calendário.
4. H2 JdbcPreparedStatement.setDate chama LegacyDateTimeUtils.fromDate. Essa função usa Date.getTime, soma offset do timezone da sessão e calcula o DATE a partir de milissegundos locais no calendário proléptico. Aqui a divergência passa a ser armazenada na coluna DATE. Não é somente apresentação nem somente leitura.
5. Em America/Sao_Paulo há também diferença entre offsets históricos da ponte legada e ZoneId/H2: java.util.TimeZone forneceu -10800 segundos, enquanto ZoneId forneceu -03:06:28 para o mesmo instante. A combinação explica o deslocamento adicional de dia. O GregorianCalendar experimental informou corte em 1582-10-15T00:00:00Z.
6. Na volta, H2 getDate/LegacyDateTimeUtils.toDate reconstrói java.sql.Date pelos milissegundos; Hibernate LocalDateJavaType.wrap chama toLocalDate. Os campos do calendário híbrido são novamente usados para criar LocalDate proléptico. Pode haver nova divergência ou DateTimeException, por exemplo quando aparece 29/02 nos anos 0100/1500.
7. JDBC getObject(coluna, LocalDate.class) lê diretamente o DATE civil do banco, sem essa ponte. Em registros gravados pelo caminho anterior H2, esse valor difere da interpretação atual da entidade/API. Logo substituir o leitor altera registros antigos mesmo sem UPDATE.

PostgreSQL 42.7.11 possui tratamento próprio no PgPreparedStatement/TimestampUtils para datas legadas. Na matriz testada, ambos os bindings e leitores preservaram exatamente todas as datas. A causa é a interação entre a ponte legada Java/Hibernate e a conversão H2, não uma incapacidade de SQL DATE ou de LocalDate. Não se presume equivalência dos drivers.

## Reprodução mínima

ReferenciaDataJdbcDiagnosticoTest cria somente tabelas temporárias e compara setDate(Date.valueOf(original)) com setObject(original, Types.DATE). Na mesma linha SQL compara cast(DATE as varchar), getDate().toLocalDate e getObject(LocalDate.class), registrando divergência/erro. Executa 12 datas × 3 timezones × 2 bancos = 72 casos. Em H2 o timezone da sessão é explicitamente alinhado ao timezone experimental da JVM para evitar cache do fuso inicial; isso é controle de teste, não mudança de configuração de produção.

ReferenciaDataJpaDiagnosticoTest grava Ação e histórico pelo mapeamento ATUAL, faz flush/clear, consulta os valores SQL/JDBC, relê as duas entidades, verifica o mapper e executa GET /acoes/{id}. Cada caso usa transação revertida ao final. Registra também falhas reais de leitura das duas entidades e da API, sem transformar dados. São 12 casos H2 e os mesmos 12 PostgreSQL. A igualdade do mapper inicial é conferida antes do clear; a resposta HTTP inicial do cadastro 0001 já estava reproduzida na etapa anterior.

## Matriz do mapeamento atual — H2, America/Sao_Paulo

| Original | SQL DATE | Entidade/API depois da releitura |
| --- | --- | --- |
| 0001-01-01 | 0000-12-29 | 0001-12-31 |
| 0004-02-29 | 0004-02-26 | 0004-02-28 |
| 0100-03-01 | 0100-02-27 | erro: fevereiro 29 inválido |
| 0400-02-29 | 0400-02-29 | 0400-02-28 |
| 1500-03-01 | 1500-03-10 | erro: fevereiro 29 inválido |
| 1582-10-04 | 1582-10-13 | 1582-10-03 |
| 1582-10-15 | 1582-10-14 | 1582-10-04 |
| 1600-02-29 | 1600-02-28 | 1600-02-28 |
| 1900-03-01 | 1900-03-01 | 1900-03-01 |
| 2000-02-29 | 2000-02-29 | 2000-02-29 |
| 2026-10-02 | 2026-10-02 | 2026-10-02 |
| 9999-12-31 | 9999-12-31 | 9999-12-31 |

Mesmos resultados nas duas entidades. Em PostgreSQL os 12 valores SQL/JDBC/entidades/GET corresponderam ao original. No experimento de novos dados por JDBC direto, H2 e PostgreSQL preservaram os 12 valores nos três fusos UTC, America/Sao_Paulo e Pacific/Kiritimati.

## Compatibilidade e alternativas para decisão

Em H2, substituir getDate/toLocalDate por getObject(LocalDate.class) transforma a leitura de SQL 0000-12-29: antes a API interpreta 0001-12-31; com leitura direta retorna 0000-12-29. Não recupera automaticamente o original 0001-01-01. Preservar o leitor antigo e mudar somente a escrita também é incorreto: SQL novo 0001-01-01 é relido como 0001-01-03 em America/Sao_Paulo. Os valores SQL não possuem marcador de binding/calendário/timezone de origem; não é seguro inferir ou corrigir datas antigas pelo conteúdo.

A configuração dev versionada aponta por default para PostgreSQL; testes usam H2 efêmero com generate-unique-name. Isso não comprova ausência de qualquer base H2 persistida/configurada externamente. Nenhum banco real foi acessado e nenhum inventário real foi feito.

Alternativa A: autorizar o binding direto limitado aos dois referenciaData sob confirmação explícita de que as bases H2 abrangidas são efêmeras/recriáveis, sem registros antigos que precisem ser preservados. PostgreSQL antigo/novo continua sujeito às provas de compatibilidade. Manter SQL DATE, sem configuração global e sem migration 007. A anotação pontual JdbcTypeCode(SqlTypes.LOCAL_DATE) é uma opção técnica ainda não aplicada nem ratificada em JPA.

Alternativa B: se H2 persistido precisa ser preservado, definir antes uma estratégia explícita de versionamento/interpretação. Pode exigir metadados/migration próprios e autorização adicional. Não criar fallback, conversão heurística ou saneamento automático; apresentar proposta concreta antes de implementar.

Uma inspeção de base real, se desejada para decidir entre A/B, necessita autorização própria. Reduzir o domínio DATA ou usar timezone para tornar o erro invisível não é alternativa compatível com o contrato aprovado.

## Execuções e arquivos

- Novo src/test/java/com/projeto/repositories/ReferenciaDataJdbcDiagnosticoTest.java.
- Novo src/test/java/com/projeto/resources/ReferenciaDataJpaDiagnosticoTest.java.
- Este relatório; revisão-dominio-temporal.md recebeu vínculo/evidência da parada. Nenhum arquivo de produção, frontend, schema ou checkbox alterado nesta etapa.

Comando JDBC final: mvn -q -Dtest=ReferenciaDataJdbcDiagnosticoTest -Dtemporal.test.postgresql.url=jdbc:postgresql://127.0.0.1:55450/postgres -Dtemporal.test.postgresql.user=codex_etapa_d test. Resultado: 72 testes de diagnóstico aprovados, zero falhas/erros/skips. Isso comprova o diagnóstico e o experimento direto; não significa que o round-trip do mapeamento ATUAL H2 esteja correto.

JPA H2: mvn -q -Dtest=ReferenciaDataJpaDiagnosticoTest test. JPA PostgreSQL: mesmo teste com spring.datasource.url=jdbc:postgresql://127.0.0.1:55450/postgres, username=codex_etapa_d, password vazio e driver-class-name=org.postgresql.Driver. 12 cenários de diagnóstico aprovados em cada banco, zero falhas/erros/skips; H2 tem as divergências explicitamente medidas acima. Não somar reexecuções de reparo dos testes. Tabela de prova usa coluna civil_date porque value é reservada no H2; tentativa inicial de diagnóstico foi corrigida. Matrizes JDBC e JPA finais são executadas em JVMs separadas.

Logs: target/temporal-persistence-jdbc-final.log, target/temporal-persistence-jpa-h2.log e target/temporal-persistence-jpa-postgresql.log. Cluster exclusivamente temporário em target/temporal-persistence-probe-pg, loopback 55450; não se acessou gestaoacoesdb.

Suíte backend completa/regressão após correção não executada: nenhuma correção de produção foi aplicada, pois a compatibilidade exige decisão. O teste de cadastro originalmente falho permanece intacto e não foi flexibilizado. CotacaoCandidata, cadastro/PATCH/lote, histórico/atomicidade/ordenação e demais datas não foram modificados nesta etapa. Os 660 testes frontend e 134 por fuso continuam evidência da etapa anterior, não reexecuções.

Tasks: 40 totais, 27 concluídas, 13 pendentes. 9.1 e 12.3 abertas; 9.2 concluída e 12.1 pendente. Sem staging, commit, push, merge, archive ou validação manual.

BLOQUEANTE: decisão de compatibilidade H2 antes de mudar a interpretação de dados antigos. Recomendação não bloqueante: preservar matrizes das 12 datas e incluir mais combinações de calendário/fuso ao implementar a estratégia escolhida.

Verificações documentais finais: OpenSpec strict da change e strict das 35 specs consolidadas aprovados; git diff --check e whitespace dos quatro arquivos desta etapa aprovados. Graphify atualizado somente após estabilizar os testes de diagnóstico: 9579 nós, 17358 relações, 771 comunidades, dois novos testes representados. Avisos conhecidos não bloqueantes: seis configurações sem nós, 58 comunidades renomeadas pelo hub e visualização agregada. Log target/temporal-persistence-graphify.log. O cluster temporário foi encerrado; nenhum processo de testes ficou em execução.

## Correção autorizada após confirmação do escopo H2

Confirmação positiva antes de editar produção: pom.xml fornece H2 somente em scope test e PostgreSQL em runtime. application-test.properties existe somente em src/test/resources, ativa generate-unique-name e ddl-auto=validate; os testes ativam test explicitamente. GestaoacoesApplicationTests confere URL jdbc:h2:mem. As URLs H2 manuais usam mem e UUID; não foi encontrada URL file/tcp, console H2, arquivo mv.db/h2.db/trace.db ou profile persistente H2 no projeto. O schema em memória é criado por Liquibase e descartado com a JVM, sem dados a preservar entre execuções.

application-dev.properties, compose.yaml, Dockerfile, README e spec application-runtime-baseline são coerentes com PostgreSQL persistente e H2 somente nos testes. A configuração local .env foi inspecionada apenas para identificar engine/profile (postgresql/dev), sem divulgar credenciais. Uma frase RNF04 do PRD ainda permitia H2 em desenvolvimento: corrigida para a decisão humana atual; a lista de tecnologias também foi esclarecida. Artefatos arquivados descrevendo a configuração antiga não foram reescritos. Não houve conexão com banco real.

Solução aplicada: uma anotação JdbcTypeCode(SqlTypes.LOCAL_DATE) em cada referenciaData, de Acao e HistoricoCotacao. SQL DATE permanece confirmado por ResultSetMetaData; nenhuma migration ou configuração global necessária. Binding/extraction usam LocalDate direto sem java.sql.Date ou timezone. Outros campos, CotacaoCandidata, finanças, ordenação, locks, atomicidade, histórico, cooldown/reuso e frontend não alterados nesta etapa.

ReferenciaDataJpaDiagnosticoTest foi transformado em regressão estrita do mapeamento corrigido: todas as 12 datas devem ser exatamente iguais no SQL DATE, JDBC LocalDate, Ação, histórico, mapper e GET; exceções de calendário não são mais aceitas como diagnóstico. A matriz JDBC anterior continua disponível para reproduzir a causa, incluindo diferenças intencionais do binding antigo H2.

Novo ReferenciaDataJpaPostgresqlTest repete os 12 round-trips e acrescenta 12 cenários de compatibilidade: nos dois campos, regrava a coluna com setDate(Date.valueOf(original)), exatamente como o binding anterior, limpa o contexto e compara leitor JDBC antigo, leitor direto, SQL, entidades e API. Testes focados passaram: H2 29/29 (17 cadastro, incluindo originalmente falho, mais 12 fronteiras); PostgreSQL JPA 24/24; matriz JDBC H2/PostgreSQL em três fusos 72/72. São execuções focadas, não contagem adicional à suíte completa posterior.

Regressão completa inicial encontrou PosicaoContractTest ainda esperando somente 13 campos anteriores à extensão temporal já aprovada. Ajustada exclusivamente a expectativa do teste para incluir referenciaMercadoCotacao/cotacaoObtidaEm. DTO/mapper/contrato público não foram modificados; não se trata de mudança contratual nova.

A mesma execução encontrou limpeza incompleta em EvolucaoPatrimonialConcurrencyTest: o contexto H2 compartilhado já continha operações de outros testes; deleteAll de carteiras falhou na FK. A classe recebeu DirtiesContext(BEFORE_CLASS) para começar com H2 limpo, sem excluir dados de outras classes ou alterar serviços/regras financeiras. A reexecução integral usa schema exclusivamente temporário PostgreSQL limpo, evitando colisões de fixtures de execuções anteriores.

Arquivos do protótipo e seus testes/documentação: Acao.java, HistoricoCotacao.java, ReferenciaDataJpaDiagnosticoTest.java, novo ReferenciaDataJpaPostgresqlTest.java, PosicaoContractTest.java, EvolucaoPatrimonialConcurrencyTest.java, docs/PRD.md, design.md, este relatório e revisão-dominio-temporal.md. As duas alterações de produção nas entidades foram retiradas após a descoberta BC, deixando-as como estavam antes desta etapa. Checkboxes permanecem intactos em 40/27/13, com 9.1 e 12.3 abertas e 12.1 pendente.

## Resultado integral do protótipo e novo bloqueante PostgreSQL BC

O protótipo direto passou a suíte backend COMPLETA: 1084 testes em 99 classes, zero falhas/erros/skips, com integrações PostgreSQL habilitadas. Comando: mvn -q -Dtemporal.test.postgresql.enabled=true -Dtemporal.test.postgresql.url=jdbc:postgresql://127.0.0.1:55451/postgres -Dtemporal.test.postgresql.user=codex_etapa_d test. Log target/temporal-persistence-backend-full-final.log; XMLs desta execução preservados em target/temporal-persistence-full-reports (somente arquivos atualizados durante o run, excluindo resultados antigos). PostgreSQL JPA 24 casos e núcleo/lote PostgreSQL 75 casos estão incluídos nesses 1084. A matriz JDBC 72 e migration H2/PostgreSQL 6 também estão incluídas; não somar reexecuções. H2 JPA em UTC adicional aprovou 12 casos no protótipo. Frontend não foi modificado/reexecutado; 660 testes e 134 por fuso continuam evidência anterior.

Na revisão final ampliou-se a compatibilidade além das 12 datas do domínio NOVO. O contrato anterior podia produzir DATA ano zero; construtores/persistência também não impunham os limites atuais. Em PostgreSQL, java.sql.Date do binding antigo grava esses valores como BC e a leitura anterior perde a era ao usar toLocalDate. A troca para LocalDate direto muda a referência pública antiga mesmo sem escrita:

| Entrada antiga | SQL PostgreSQL | Leitor antigo/API anterior | Leitor direto |
| --- | --- | --- | --- |
| 0000-01-01 | 0001-01-01 BC | 0001-01-01 | 0000-01-01 |
| -0001-01-01 | 0002-01-01 BC | 0002-01-01 | -0001-01-01 |

A reprodução foi realizada somente em tabela temporária do cluster isolado. Em seguida, ReferenciaDataJpaPostgresqlTest acrescentou dois casos de regressão que escrevem pelo binding anterior e exigem a leitura anterior nas duas entidades/API. Após retirar o protótipo, 2/2 passaram: SQL, leitor antigo, leitor direto, Ação, histórico e GET comprovados em target/temporal-persistence-pg-bc-compat.log. Esses casos falhariam com a anotação genérica direta, pois exigem manter a referência anteriormente retornada, e não reinterpretar BC como ano zero/negativo.

Por instrução humana, PAREI e removi exclusivamente as duas anotações que havia adicionado. Não foi revertida nenhuma alteração preexistente. O teste H2 de round-trip estrito permanece, expondo o problema ainda não resolvido no mapeamento restaurado. Os 1084 resultados são do PROTÓTIPO antes dessa retirada; não certificam uma correção final compatível. Não reexecutei a suíte completa restaurada como se a correção estivesse concluída.

Não basta preservar o binding PostgreSQL antigo para tudo e corrigir somente H2: experimento com DATA gregoriana 1582-10-10 mostrou setDate antigo escrevendo 1582-10-20 no PostgreSQL. Novas aquisições precisam de escrita civil direta também no PostgreSQL; leitura antiga BC precisa de política separada. Nenhuma data foi automaticamente reparada e não se conhece a existência real de BC no banco oficial, que não foi acessado.

Decisão necessária: autorizar proposta de binding pontual com escrita direta civil e compatibilidade explícita de leitura BC para manter a referência pública anterior, ou autorizar primeiro inventário somente de leitura no banco oficial para fundamentar uma decisão de interpretação. Uma eventual autorização para reinterpretar BC/migrar dados é mudança de contrato separada; não está inferida desta tarefa. Não implementar fallback silencioso ou transformação heurística.

Estado final desta etapa: H2 exclusivamente efêmero CONFIRMADO; causa-raiz confirmada; compatibilidade PostgreSQL 0001–9999 comprovada no protótipo; compatibilidade do domínio anterior BC BLOQUEANTE. Nenhuma correção de produção permanece aplicada. Não há necessidade demonstrada de alterar SQL DATE ou migration 007. Tasks mantidas em 40/27/13. Limitação conhecida: os novos testes de fronteira ainda não passam no H2 com o mapeamento restaurado; não ratificar F4.

Verificações do estado final bloqueado: OpenSpec strict da change e das 35 specs consolidadas aprovados, git diff --check aprovado. Whitespace dos arquivos desta etapa verificado; os três hard breaks Markdown já existentes nas linhas 3–5 do PRD foram conferidos contra HEAD e preservados. Graphify reatualizado após retirar o protótipo e estabilizar o teste BC: 9598 nós, 17391 relações, 778 comunidades; seis configurações sem nós e 54 comunidades renomeadas pelo hub são avisos conhecidos não bloqueantes. Log target/temporal-persistence-final-blocked-graphify.log. PostgreSQL temporário em target/temporal-persistence-final-pg, loopback 55451, encerrado com pg_ctl; nenhum banco real acessado. Sem staging, commit, push, merge, archive, ratificação de task ou validação manual.

Arquivos com alterações líquidas nesta etapa: docs/PRD.md; src/test/java/com/projeto/resources/ReferenciaDataJpaDiagnosticoTest.java; novo src/test/java/com/projeto/resources/ReferenciaDataJpaPostgresqlTest.java; src/test/java/com/projeto/dto/PosicaoContractTest.java; src/test/java/com/projeto/services/EvolucaoPatrimonialConcurrencyTest.java; openspec/changes/atualizar-cotacoes-da-carteira/design.md, revisao-dominio-temporal.md e este relatório. As entidades não conservam as alterações do protótipo desta etapa. Demais mudanças preexistentes da change foram preservadas.
