# Prototipo isolado de compatibilidade JDBC/BC

Data: 2026-10-10. Status: pronto para decisao, nao incorporado a producao.

## Escopo e seguranca

- PostgreSQL continua sendo o banco persistente oficial. A investigacao anterior confirmou H2 como dependencia de teste, em bancos `jdbc:h2:mem` efemeros e recriaveis, sem H2 persistente da aplicacao.
- Esta prova usa apenas H2 em memoria e um cluster PostgreSQL 16.8 criado em `target/jdbc-bc-poc-pg`, usuario `codex_etapa_d`, loopback, porta 55452. O teste verifica URL, usuario e `SHOW data_directory` antes de criar schemas UUID exclusivos.
- Liquibase aplica os changelogs existentes em cada banco temporario. SQL DATE e migration 007 nao foram alterados. Os schemas temporarios foram removidos e o cluster encerrado ao concluir a prova. Nenhum banco real foi acessado.
- A substituicao de tipos existe apenas em SessionFactories de teste que mapeiam Acao e HistoricoCotacao reais. Assercoes certificam que os unicos LocalDate dessas fabricas sao seus dois campos `referenciaData`. Nao copiar o registro global do tipo desta fabrica isolada para a aplicacao completa.
- Nenhum arquivo `src/main` ou checkbox foi alterado nesta etapa; hashes dos 130 arquivos de producao e de tasks.md permaneceram iguais ao inicio da prova. Estado: 40 tasks, 27 concluidas, 13 pendentes; 9.1, 12.3 e 12.1 abertas.
- Nenhuma alteracao de contrato temporal, saneamento, migration, staging, commit, push, merge, archive ou validacao manual.

## Causa-raiz consolidada

O caminho anterior Hibernate `DateJdbcType` converte LocalDate por java.sql.Date. Essa ponte passa pela representacao de calendario hibrido dos tipos legados, enquanto LocalDate representa calendario gregoriano proleptico. Drivers H2 e PostgreSQL nao produzem resultados identicos ao atravessar essa ponte. Em especial, datas civis validas de 5 a 14 de outubro de 1582 sao normalizadas pelo caminho antigo PostgreSQL para 15 a 24 de outubro.

JDBC 4.2 com `setObject(LocalDate, DATE)` e `getObject(..., LocalDate.class)` evita a ponte na escrita e na leitura civil nova. Isso resolve os round-trips positivos testados, mas leitura direta indiscriminada altera valores BC antigos. A correcao H2 exclusiva em testes nao corrige o dominio positivo completo no PostgreSQL.

## Prototipo e comportamento

Arquivos novos, exclusivamente de teste:

- `src/test/java/com/projeto/temporalpoc/BcCompatibilityJdbcType.java`: binder direto herdado de LocalDateJdbcType; extractor direto, exceto quando PostgreSQL retorna ano Java <= 0. Nesse caso delega a `getDate` e ao mesmo `JavaType.wrap` do caminho anterior.
- `src/test/java/com/projeto/temporalpoc/ReferenciaDataBcPrototypeTest.java`: comparacao OLD, A_BC_COMPAT, B_H2_ONLY e C_DIRECT, entidades reais e resposta HTTP via MockMvc.

A escrita A nao usa java.sql.Date. A leitura A preserva valores civis novos AD e delega explicitamente BC ao codec anterior; nao aplica uma formula aritmetica de conversao de ano. Nulos continuam nulos, com referencia legitimamente ausente DESCONHECIDA na API. Erros antigos nao viram DESCONHECIDA.

PostgreSQL SQL DATE representa 1 BC seguido de 1 AD, sem ano SQL zero. JDBC direto converte 1 BC para ano astronomico Java 0, 2 BC para -1. O predicado `year <= 0` identifica a era BC nesse driver, mas nao identifica a procedencia de uma linha. DATA nova aprovada nao permite BC/ano zero; estas fixtures representam exclusivamente leitura legada, nao novas candidatas.

| Valor SQL antigo | Leitura anterior e A | Leitura direta C |
| --- | --- | --- |
| 0001-01-01 BC | 0001-01-01 AD | 0000-01-01 Java |
| 0002-01-01 BC | 0002-01-01 AD | -0001-01-01 Java |
| 0001-02-29 BC | DateTimeException: ano 1 nao bissexto | 0000-02-29 Java |
| 0005-02-29 BC | DateTimeException: ano 5 nao bissexto | -0004-02-29 Java |
| 0401-02-29 BC | DateTimeException: ano 401 nao bissexto | -0400-02-29 Java |
| 10000-01-01 AD | +10000-01-01 | +10000-01-01 |

Preservar compatibilidade aqui inclui preservar erros de leitura preexistentes; A nao torna todos os BC antigos legiveis. Tornar esses casos legiveis exigiria decisao funcional separada. As tres implementacoes de extractor (ResultSet e CallableStatement por indice/nome) foram preparadas; a prova ORM exercita ResultSet, nao procedures.

## Ambiguidade demonstrada

1. OLD escreve entrada Java 1582-10-10 como SQL 1582-10-20. A escreve entrada Java 1582-10-20 como o mesmo SQL. Ambos sao lidos publicamente como 20. O DATE sozinho nao recupera a entrada original perdida. Nao subtrair dez dias de registros armazenados.
2. Um registro anterior com SQL explicito 1582-10-10 e um registro novo A com entrada civil 1582-10-10 possuem o mesmo DATE. Leitura OLD do primeiro resulta 20; leitura A resulta 10. O SQL nao determina qual interpretacao deve ser preservada por registro.

O segundo caso e uma fixture de outro caminho de escrita, nao evidencia de sua existencia no banco real. Nao houve inspecao desse banco. Para os registros produzidos pelo caminho Hibernate anterior confirmado, A preservou valor ou erro em toda a matriz PostgreSQL testada. Essa evidencia nao autoriza assumir que todos os registros existentes tenham essa origem.

Metadados adicionais nao sao necessarios para detectar BC; sao necessarios para uma garantia incondicional quando interpretacoes legada/civil distintas devem coexistir sobre DATEs iguais sem procedencia comprovada. Um seletor de origem em memoria demonstrou, para Acao, historico e API, leitura LEGACY=20 e CIVIL=10 sobre SQL=10. Isso e apenas simulacao: nenhum metadado persistido, mapping versionado, migration ou backfill foi implementado. Um marcador preservaria a interpretacao observavel anterior, nao recuperaria entradas ja normalizadas/perdidas.

## Testes e evidencias

Execucao opt-in final:

```powershell
mvn -q '-Dtest=ReferenciaDataBcPrototypeTest' '-Dtemporal.poc.enabled=true' '-Dtemporal.test.postgresql.url=jdbc:postgresql://127.0.0.1:55452/postgres' '-Dtemporal.test.postgresql.user=codex_etapa_d' test
```

Evidencias locais: `target/jdbc-bc-poc-final.log` e `target/surefire-reports/TEST-com.projeto.temporalpoc.ReferenciaDataBcPrototypeTest.xml`. Resultado: 6 grupos JUnit, zero falhas, erros ou skips. Java 17.0.14, Hibernate 7.4.1, H2 2.4.240, PostgreSQL 16.8, driver PostgreSQL 42.7.11.

41 datas positivas distintas: as 12 exigidas (0001-01-01, 0004-02-29, 0100-03-01, 0400-02-29, 1500-03-01, 1582-10-04, 1582-10-15, 1600-02-29, 1900-03-01, 2000-02-29, 2026-10-02, 9999-12-31), mais todos os dias de outubro de 1582. Total: 246 combinacoes data/banco/fuso. Fixtures PostgreSQL extras: sete entradas BC/ano zero e uma expandida AD por fuso, total 24.

| Fuso JVM e sessao SQL | H2 A novo | PostgreSQL A novo | PostgreSQL OLD -> A | B PostgreSQL | Ambiguidade |
| --- | --- | --- | --- | --- | --- |
| UTC | 41/41 exatas | 41/41 exatas | valores/erros preservados | desloca 5-14/10/1582 | confirmada |
| America/Los_Angeles | 41/41 exatas | 41/41 exatas | valores/erros preservados | desloca 5-14/10/1582 | confirmada |
| Pacific/Kiritimati | 41/41 exatas | 41/41 exatas | valores/erros preservados | desloca 5-14/10/1582 | confirmada |

Por fuso: 124 verificacoes API H2 e 232 PostgreSQL, mais duas do seletor versionado simulado. Foram comparadas leituras antigas de escritas novas, novas de escritas antigas, entidades e historico em sessoes independentes, e tipo SQL DATE via metadata. A leitura OLD de novas datas civis no intervalo de reforma nao e correta: coexistencia com leitores antigos requer cuidado de rollout.

AcaoResource, AcaoMapper e DTO reais foram exercitados; AcaoService e mockado como ponte para a SessionFactory de cada alternativa. Erros BC sao comparados pela causa-raiz ServletException no MockMvc standalone, nao por um contrato de corpo HTTP 500 do servidor completo. Nao se certificou aqui aquisicao, locks, lote, cooldown ou suite completa Spring. Nao houve alteracao frontend nem repeticao de sua suite. O prototipo nao certifica a incorporacao definitiva ou todos os 3,65 milhoes de dias do dominio; certifica a matriz explicitada.

Verificacoes finais: OpenSpec strict da change aprovado; 35 specs consolidadas aprovadas; `git diff --check` exit 0; arquivos novos sem whitespace final ou tabs. Graphify AST-only atualizado (9.642 nodes, 17.494 edges, 779 comunidades), sem custo de API. Avisos nao bloqueantes do Graphify: seis arquivos de configuracao sem nodes e nomes de comunidades parcialmente defasados; nenhuma extracao semantica de documentacao ou relabel com LLM foi executado. Evidencia: `target/jdbc-bc-poc-graphify.log`. O diff dos dois arquivos de teste e deste relatorio foi revisado; hashes finais confirmam producao e tasks intactos.

## Comparacao das alternativas

| Criterio | A: direto + BC legado | B: manter PostgreSQL, corrigir H2 teste | C: leitura versionada por observacao |
| --- | --- | --- | --- |
| Complexidade | baixa/moderada, extractor pontual dependente do driver | baixa/moderada, override restrito a teste | alta, selecao explicita por versao persistida |
| PostgreSQL | novo dominio da matriz exato; legado OLD preservado; AD de outra origem condicionado | falha novas datas AD de 5-14/10/1582 | preserva codec anterior e civil novo por origem; somente seletor simulado |
| H2 | novo dominio da matriz exato | testes civis exatos com override | civil novo exato; H2 nao exige preservacao de bancos antigos |
| Risco dados existentes | nao reescreve DATE, mas pode reinterpretar AD-gap de origem diferente | mantem comportamento antigo, mas novas escritas incorretas | pode preservar leitura anterior; classificacao/rollout requer projeto aprovado |
| Entidades | futuramente @JdbcType somente nos dois campos | producao intacta | marcador interno em Acao e historico + mapping localizado |
| Testes | integra ORM/driver/API e matriz temporal permanente | mascara divergencia entre teste e producao | cobre versao, leitura, propagacao atomica e rollback |
| Migration | dispensavel somente com premissa de procedencia aprovada | nenhuma, mas nao atende objetivo | provavelmente aditiva para metadado; autorizacao obrigatoria, sem alterar 007 |
| Manutencao | validar upgrades JDBC/Hibernate e politica BC explicita | divergencia permanente de ambientes | maior, mas procedencia e politica de leitura explicitas |

C_DIRECT e controle experimental, nao a alternativa C versionada: direto indiscriminado e rejeitado por mudar BC e erros antigos silenciosamente.

## Recomendacao e decisoes pendentes

Nao recomendar B: fazer H2 passar mantendo PostgreSQL atual deixa novas DATAs validas incorretas. Nao incorporar direto indiscriminado.

A e a menor solucao plausivel se for explicitamente aceita e comprovada a premissa de procedencia dos registros antigos pelo codec Hibernate testado, ou ausencia de AD-gap de outros caminhos. Sem essa premissa, recomendar projetar C versionada para garantir preservacao por registro. O prototipo demonstra por que so o DATE nao pode satisfazer ambas as interpretacoes universalmente; nao escolhe uma regra silenciosa.

Decisoes necessarias antes de producao:

1. Aceitar a premissa restrita para A e definir como comprova-la sem acesso real nao autorizado, ou autorizar projeto de metadados C.
2. Preservar explicitamente falhas BC preexistentes ou aprovar tratamento separado para tornarem-se legiveis. A implementa apenas a primeira opcao neste experimento.
3. Se C: aprovar migration aditiva, versao por observacao e sua propagacao atomica para historico; nao alterar 007, nao modificar DATE nem DTO publico implicitamente.
4. Planejar rollout sem leitores antigos concorrentes sobre novas datas civis da reforma e reexecutar integracao/suite completa quando houver implementacao aprovada.

Arquivos futuros para A: novo JdbcType de producao, somente anotacoes/bindings de Acao.referenciaData e HistoricoCotacao.referenciaData, testes de integracao e documentacao aprovada. Outros LocalDate permanecem intactos. Para C: acrescentam-se mapping/metadados localizados das duas observacoes, persistencia/copia de versao para historico e migration aditiva autorizada; isso supera o escopo de incorporacao atualmente permitido.

Bloqueante para incorporacao: decisao de procedencia versus metadados e autorizacao explicita de producao. Recomendavel: ampliar geracao de datas e testes de upgrades/procedures se utilizados. Nao bloqueante para esta prova: frontend e validacao manual nao executados, pois nao alterados nem autorizados. Nenhuma task ratificada.
