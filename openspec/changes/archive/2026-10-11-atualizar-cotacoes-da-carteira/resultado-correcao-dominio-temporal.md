# Correção temporal/F4 — resultado parcial para revisão

Data: 2026-10-10. Implementação interrompida por incompatibilidade concreta de persistência DATA em H2. Não declarar correção integral concluída. Tasks 9.1 e 12.3 permanecem abertas.

## Arquivos desta continuidade

- src/main/java/com/projeto/integrations/cotacao/CotacaoCandidata.java
- frontend/src/app/shared/models/cotacao-temporal.ts
- frontend/src/app/shared/models/cotacao-temporal.spec.ts (novo)
- src/test/java/com/projeto/integrations/cotacao/CotacaoDominioTemporalTest.java (novo)
- src/test/java/com/projeto/integrations/cotacao/CotacaoPrecisaoTemporalTest.java
- src/test/java/com/projeto/services/AplicacaoCotacaoServiceTest.java
- src/test/java/com/projeto/services/AtualizacaoCotacoesCarteiraServiceTest.java
- src/test/java/com/projeto/services/AtualizacaoCotacoesCarteiraPostgresqlTest.java
- src/test/java/com/projeto/resources/AcaoCadastroTemporalIntegrationTest.java
- src/test/java/com/projeto/resources/AcaoCotacaoHttpIntegrationTest.java
- src/test/java/com/projeto/resources/AtualizacaoCotacoesCarteiraHttpIntegrationTest.java
- frontend/src/app/shared/formatters/cotacao-temporal.fixtures.ts
- frontend/src/app/shared/formatters/cotacao-temporal.formatter.spec.ts
- frontend/src/app/features/acoes/pages/acoes-list-page.component.spec.ts
- frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.spec.ts
- openspec/changes/atualizar-cotacoes-da-carteira/tasks.md
- openspec/changes/atualizar-cotacoes-da-carteira/revisao-dominio-temporal.md
- este relatório (novo)

## Regras e comportamento

DATA: calendário gregoriano real validado pelo LocalDate backend e por aritmética frontend, ano 0001–9999; sem limite futuro/piso 1970. Alpha já exige texto de quatro dígitos. Formatter civil existente preserva o texto e não usa Date. Persistência H2 da fronteira inferior falha; PostgreSQL 16.8 preserva-a no teste HTTP completo.

INSTANTE: microssegundos exatos, UTC semanticamente equivalente, piso inclusivo epoch, referência <= obtenção da própria candidata, tolerância ZERO. Offsets com segundos e zeros excedentes são preservados semanticamente; precisão significativa excedente rejeitada. Não houve mudança de ordenação entre observações.

Obtenção: Clock backend próprio, microssegundos exatos, sem herdar piso financeiro ou limites de DATA; apresentação separada, nulo = "Não informada". Não há relógio do navegador na validação. Legado dataHoraCotacao permanece intacto, sem limites financeiros novos/fallback.

Compatibilidade: guards públicos de leitura existentes não foram endurecidos; isNovaAquisicaoCotacao é fronteira explícita separada e não substitui validação dos DTOs retornados por GET/lote. A API não traz versão de aquisição e um EstadoCotacao pode conter observação antiga, inclusive em respostas de reuso/cooldown. Mappers/entidades/migration 007 não alterados. Testes preservam leitura de referências anteriores a 1970 ou posteriores à obtenção e apresentação civil anterior negativa/expandida. Não se apaga, transforma ou oculta estado antigo.

## Frontend e quatro superfícies

Suíte completa UTC: 660 testes, zero falhas/erros, incluindo F1/F2/F3, parser lossless, guards, serviços, Dashboard, formatters e superfícies.

Suíte temporal focada: 134 testes, zero falhas/erros em cada execução UTC, America/Los_Angeles e Pacific/Kiritimati. Inclui listagem Ações, detalhe Ação, posições e desempenho por ativo. Novas fixtures compartilhadas exercitam DATA 0001/9999/bissexto e epoch com offsets equivalentes/segundos em todas as superfícies; testes de formatter exercitam frações próximas de minuto, DTO imutável, obtenção separada e ausência explícita. Reflow/assistiva/manual final não executados; 12.1 pendente.

Epoch local observado e comparado com oráculos literais:

| Timezone | 1970-01-01T00:00:00Z |
| --- | --- |
| UTC | 01/01/1970 às 00:00 |
| America/Los_Angeles | 31/12/1969 às 16:00 |
| Pacific/Kiritimati | 31/12/1969 às 13:20 |

Os resultados locais de 1969 são apresentações válidas de INSTANTE real no piso; não se aplica o piso ao calendário local exibido. DATA permanece idêntica nos três fusos.

Comandos: ng test --watch=false --reporters=junit; execução focada com --include dos dois formatters, cotacao-temporal.spec.ts e quatro componentes. XMLs/logs: target/temporal-domain-{UTC,west,east} e target/temporal-domain-frontend-regression. TypeScript --noEmit em tsconfig.app.json e tsconfig.spec.json aprovado.

## Backend e limitação de persistência

Testes abrangem candidata/providers, microssegundos, comparação equivalente, cadastro/PATCH/lote, reuso/cooldown, histórico, atomicidade e locks. Fixtures de concorrência foram ajustadas para manter a obtenção de cada candidata >= sua própria referência, preservando a obtenção anterior da observação posterior frente à observação substituída. Nenhuma regra de produção de concorrência foi alterada.

Reprodução H2: AcaoCadastroTemporalIntegrationTest, 17 casos, 1 falha DATA 0001, zero erros/skips. SQL registra 0000-12-29; teste anterior também mostrou releitura 0001-12-31. Reprodução da mesma classe em PostgreSQL temporário: 17/17 aprovados, incluindo SQL DATE e releitura. Logs target/temporal-domain-civil-{reproduction,postgresql}.log.

Execução intermediária com PostgreSQL reutilizado encontrou colisões de tickers de testes anteriores; isso não prova regressão de produção. Schema exclusivamente temporário foi recriado após verificar SHOW data_directory e current_user. Resultado definitivo da suíte limpa será registrado abaixo; não somar reexecuções.

Resultado definitivo em schema temporário limpo: 489 testes, 488 aprovados, 1 falha DATA 0001/H2, zero erros/skips. As 81 execuções de AplicacaoCotacaoPostgresqlTest, AtualizacaoCotacoesCarteiraPostgresqlTest e ReferenciaTemporalMigrationTest (incluindo casos H2/PostgreSQL da migration) passaram; fazem parte dos 489, não são contagem adicional. Log target/temporal-domain-backend-clean.log e XMLs target/surefire-reports. A execução PostgreSQL exclusiva de cadastro 17/17 é evidência adicional separada.

Comando backend final: mvn -q -Dtest=CotacaoDominioTemporalTest,CotacaoCandidataTest,CotacaoCandidataProvidersTest,CotacaoPrecisaoTemporalTest,BrapiAdapterTest,BrapiPrecisionTest,AlphaVantageAdapterTest,AlphaVantageConsumptionTest,RegraDecisaoCotacaoTest,AcaoServiceTest,AcaoCadastroTemporalIntegrationTest,AcaoCotacaoHttpIntegrationTest,AtualizacaoCotacoesCarteiraHttpIntegrationTest,AtualizacaoIndividualCotacaoTest,AtualizacaoCotacoesCarteiraServiceTest,AplicacaoCotacaoServiceTest,HistoricoCotacaoAtomicityTest,HistoricoCotacaoRepositoryTest,ReferenciaTemporalMigrationTest,AplicacaoCotacaoPostgresqlTest,AtualizacaoCotacoesCarteiraPostgresqlTest -Dtemporal.test.postgresql.enabled=true -Dtemporal.test.postgresql.url=jdbc:postgresql://127.0.0.1:55449/postgres -Dtemporal.test.postgresql.user=codex_etapa_d test. URL exclusiva do cluster criado em target/temporal-domain-pg; nenhum acesso ao banco de desenvolvimento.

## Graphify e revisão

Graphify atualizado por AST, sem LLM: 9563 nós, 17306 relações, 781 comunidades. Candidata, novo teste backend, modelos temporais e novo teste frontend encontrados por source_file. Avisos não bloqueantes: seis configurações sem nós, 79 comunidades renomeadas pelo hub, visualização agregada por tamanho. Logs target/temporal-domain-graphify.log; saída ignorada no Git.

OpenSpec strict da change aprovado; strict das 35 specs consolidadas aprovado. git diff --check aprovado; whitespace dos arquivos desta continuidade, incluindo novos/não rastreados, verificado. Revisão da correção preservou alterações preexistentes: produção limitada à candidata e funções novas de validação de aquisição frontend, sem alteração dos guards de leitura existentes ou núcleo de aplicação/persistência/concorrência. Nenhuma correção do mapeamento civil foi implementada após a identificação do bloqueante.

## Tasks, bloqueante e decisão necessária

40 tasks, 27 concluídas, 13 pendentes. Somente 9.1 e 12.3 foram desmarcadas sob autorização formal; 9.2 continua concluída, 12.1 pendente. Nenhuma task ratificada novamente nesta continuidade.

BLOQUEANTE: DATA 0001 não preservada em H2. F4 não pode ser encerrada com esse defeito.

Decisão necessária: autorizar investigação/aplicação de binding JDBC direto de LocalDate somente nos dois campos referenciaData, mantendo DATE e comprovando compatibilidade de leitura, ou reservar isso a uma etapa separada mantendo F4 aberta. Não alterar dados antigos nem migration 007 automaticamente. Nenhuma configuração de calendário/timezone de mercado foi proposta.

RECOMENDÁVEIS: após decidir o mapeamento, ampliar fronteiras civis anteriores a 1582 e comprovar leitura dos valores armazenados pelo binding anterior em ambos os bancos.

NÃO BLOQUEANTES: avisos Graphify e LF/CRLF; validação visual/assistiva permanece trabalho pendente separado. Sem staging, commit, push, merge ou archive; nenhuma próxima etapa iniciada.
