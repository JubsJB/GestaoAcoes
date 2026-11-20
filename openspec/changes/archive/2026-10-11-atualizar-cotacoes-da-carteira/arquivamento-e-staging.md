# Archive oficial e selecao atualizada de staging

Operacao autorizada: openspec archive atualizar-cotacoes-da-carteira --yes. Exit0. Validacao e sincronizacao normais;nenhum --skip-specs/--no-validate. Change arquivada em openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira. Branch confirmada:dev.40tasks concluidas/0pendentes antes/depois. Nenhum git add/commit/push/merge,nenhuma alteracao de codigo/testes/banco ou migration executada nesta etapa.

## Preservacao e validacao

Todos os29arquivos originais da pasta da change foram comparados porSHA256 antes/depois,com0divergencias/0perdas. Este relatorio acrescenta o30oarquivo ao archive. Pasta ativa original ausente apos a movimentacao oficial. Os caminhos da change eram nao rastreados noGit;por isso status mostra arquivosnovos noarchive,nao renames rastreados. Nao equivale a perda de historico.

Preservados integralmente:PETR4 inicialmente FALHOU,correcao deClock/feedback,revalidacao manual PASSOU;loading do lote NAO OBSERVADO e recarga visual NAO CONFIRMADA VISUALMENTE;testes automatizados complementares. Nao reescritos relatorios antigos para mudar resultados/contagens historicas. preparacao-commit-arquivamento.md e revisao-final-pos-validacao-manual.md sao snapshots anteriores;ESTE documento substitui a lista antiga de staging.

- Strict da change imediatamente antes:valida.
- Strict consolidado depois: openspec validate --specs --strict --no-interactive:36pass/0fail.
- Nove spec.md gravadas comSHA256 exatamente igual ao rebuild do merger oficial capturado ANTES doarchive. Portanto diferencas somente as operacoes esperadas9ADDED/21MODIFIED/0REMOVED/0RENAMED.
- Nomes de todos os requisitos e cenarios anteriores preservados dentro de cada requisito pelo parser oficial;0titulos duplicados nas9specs.112requisitos/451cenarios finais nesse conjunto,nao em todas36capabilities.
- git diff --check aprovado. Whitespace horizontal das29fontesarquivadas:0ocorrencias. Specs e novo relatorio conferidos separadamente.
- Unico aviso daCLI:considerar dividir changes com mais de10deltas. Recomendacao nao bloqueante de tamanho,sem conflito ou warning de merger.

## Specs sincronizadas

| Capability | Antes requisitos/cenarios | Depois requisitos/cenarios | Tipo | Hash esperado |
| --- | --- | --- | --- | --- |
| alpha-vantage-consumption | 3/12 | 3/15 | Atualizada | Confere |
| api-documentation | 8/22 | 9/25 | Atualizada | Confere |
| frontend-dashboard-management | 14/75 | 15/82 | Atualizada | Confere |
| frontend-position-analysis | 10/50 | 11/54 | Atualizada | Confere |
| frontend-stock-management | 11/62 | 11/64 | Atualizada | Confere |
| portfolio-position | 24/72 | 24/72 | Atualizada | Confere |
| portfolio-quote-refresh | 0/0 | 4/18 | Criada | Confere |
| stock-quote-history | 9/22 | 9/22 | Atualizada | Confere |
| stock-registration | 24/82 | 26/99 | Atualizada | Confere |

Oito specs existentes foram atualizadas e portfolio-quote-refresh criada. Nenhuma divergencia impeditiva ou perda/duplicacao encontrada. Nenhuma suite completa reexecutada porque archive nao muda codigo.

## Conteudo experimental/documental movido

Nao ha .java/.class/.jar/.log/.db/.dump na pastaarchive. Os tres arquivos Java experimentais permanecem FORA da pasta e FORA da selecao,sem exclusao automatica.

Ha documentos sobre experimentos e diagnosticos noarchive:
- diagnostico-persistencia-referencia-data.md;
- diagnostico-precisao-petr4-manual.md;
- prototipo-compatibilidade-jdbc-bc.md;
- plano-inventario-postgresql.md;
- inventario-referencia-data.sql.

Sao registros historicos/documentacao ja incluidos na selecao aprovada,devidamente preservados;nao componentes de runtime nem provas de conceito executadas nesta etapa. SQLinventario e artefato de leitura revisado,nunca migration ou script de startup. Recomendo mantê-los no commit para rastreabilidade. Caso o usuario prefira reduzir documentacao,solicitar decisao explicita;nao apagar nem retirar silenciosamente. Nenhum arquivo executavel experimental foi incorporado pelo archive.

## Lista explicita atualizada

148 caminhos selecionados: 51 producao,57 testes e40 documentacao. Inclui as3delecoes aprovadas. Contagem anterior138 +9specs consolidadas +1relatorio =148. Fora4caminhos. Os78nao rastreados originais foram revisados na etapa anterior;preparacao acrescentou1,e esta etapa acrescenta1capability e1relatorio,total81nao rastreados no status.

Nao enviar comandos abrangentes git add . ou git add -A. Use SOMENTE os caminhos abaixo,apos autorizacao de staging. Nenhum deles foi staged aqui. As3delecoes devem ser registradas explicitamente na mesma selecao (nao recriar os arquivos).

| Status atual | Categoria | Caminho |
| --- | --- | --- |
| M | Documentacao | `docs/PRD.md` |
| M | Testes | `frontend/src/app/features/acoes/acoes.routes.spec.ts` |
| M | Testes | `frontend/src/app/features/acoes/acoes.service.spec.ts` |
| M | Producao | `frontend/src/app/features/acoes/models/acao.ts` |
| M | Testes | `frontend/src/app/features/acoes/pages/acao-create-dialog.spec.ts` |
| M | Testes | `frontend/src/app/features/acoes/pages/acao-create-page.component.spec.ts` |
| M | Testes | `frontend/src/app/features/acoes/pages/acao-detail-page.component.spec.ts` |
| M | Producao | `frontend/src/app/features/acoes/pages/acao-detail-page.component.ts` |
| M | Testes | `frontend/src/app/features/acoes/pages/acoes-list-page.component.spec.ts` |
| M | Producao | `frontend/src/app/features/acoes/pages/acoes-list-page.component.ts` |
| M | Testes | `frontend/src/app/features/carteiras/pages/carteira-detail-page.component.spec.ts` |
| M | Testes | `frontend/src/app/features/carteiras/pages/carteiras-dialog.integration.spec.ts` |
| M | Producao | `frontend/src/app/features/dashboard/dashboard-page.component.html` |
| M | Testes | `frontend/src/app/features/dashboard/dashboard-page.component.spec.ts` |
| M | Producao | `frontend/src/app/features/dashboard/dashboard-page.component.ts` |
| M | Testes | `frontend/src/app/features/dashboard/dashboard.service.spec.ts` |
| M | Producao | `frontend/src/app/features/dashboard/dashboard.service.ts` |
| M | Producao | `frontend/src/app/features/dashboard/models/dashboard.ts` |
| M | Testes | `frontend/src/app/features/dashboard/position-analysis/position-analysis.fixtures.ts` |
| M | Producao | `frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.html` |
| M | Testes | `frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.spec.ts` |
| M | Producao | `frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.ts` |
| M | Producao | `frontend/src/app/shared/formatters/offset-date-time.formatter.ts` |
| M | Testes | `frontend/src/app/shared/portfolio-positions/portfolio-positions.component.spec.ts` |
| M | Producao | `frontend/src/app/shared/portfolio-positions/portfolio-positions.component.ts` |
| M | Documentacao | `openspec/specs/alpha-vantage-consumption/spec.md` |
| M | Documentacao | `openspec/specs/api-documentation/spec.md` |
| M | Documentacao | `openspec/specs/frontend-dashboard-management/spec.md` |
| M | Documentacao | `openspec/specs/frontend-position-analysis/spec.md` |
| M | Documentacao | `openspec/specs/frontend-stock-management/spec.md` |
| M | Documentacao | `openspec/specs/portfolio-position/spec.md` |
| M | Documentacao | `openspec/specs/stock-quote-history/spec.md` |
| M | Documentacao | `openspec/specs/stock-registration/spec.md` |
| M | Producao | `src/main/java/com/projeto/config/OpenApiConfig.java` |
| M | Producao | `src/main/java/com/projeto/config/TimeConfig.java` |
| M | Producao | `src/main/java/com/projeto/dto/AcaoResponse.java` |
| M | Producao | `src/main/java/com/projeto/dto/PosicaoResponse.java` |
| M | Producao | `src/main/java/com/projeto/entities/Acao.java` |
| M | Producao | `src/main/java/com/projeto/entities/HistoricoCotacao.java` |
| M | Producao | `src/main/java/com/projeto/integrations/cotacao/AlphaVantageAdapter.java` |
| M | Producao | `src/main/java/com/projeto/integrations/cotacao/BrapiAdapter.java` |
| M | Producao | `src/main/java/com/projeto/integrations/cotacao/CotacaoData.java` |
| M | Producao | `src/main/java/com/projeto/integrations/cotacao/CotacaoProvider.java` |
| M | Producao | `src/main/java/com/projeto/mappers/AcaoMapper.java` |
| M | Producao | `src/main/java/com/projeto/mappers/PosicaoMapper.java` |
| M | Producao | `src/main/java/com/projeto/repositories/AcaoRepository.java` |
| M | Producao | `src/main/java/com/projeto/resources/AcaoResource.java` |
| D | Producao | `src/main/java/com/projeto/services/AcaoCotacaoPersistenceService.java` |
| M | Producao | `src/main/java/com/projeto/services/AcaoPersistenceService.java` |
| M | Producao | `src/main/java/com/projeto/services/AcaoService.java` |
| M | Producao | `src/main/java/com/projeto/services/PosicaoService.java` |
| M | Producao | `src/main/java/com/projeto/services/exceptions/ErrorCodes.java` |
| M | Producao | `src/main/resources/db/changelog/db.changelog-master.yaml` |
| M | Testes | `src/test/java/com/projeto/dto/PosicaoContractTest.java` |
| M | Testes | `src/test/java/com/projeto/entities/AcaoTest.java` |
| M | Testes | `src/test/java/com/projeto/repositories/HistoricoCotacaoRepositoryTest.java` |
| M | Testes | `src/test/java/com/projeto/resources/AcaoEuaRegistrationRequestsTest.java` |
| M | Testes | `src/test/java/com/projeto/resources/OpenApiDocumentationTest.java` |
| M | Testes | `src/test/java/com/projeto/resources/PatrimonioResourceTest.java` |
| M | Testes | `src/test/java/com/projeto/resources/PosicaoResourceTest.java` |
| D | Testes | `src/test/java/com/projeto/services/AcaoCotacaoConcurrencyTest.java` |
| D | Testes | `src/test/java/com/projeto/services/AcaoCotacaoPersistenceServiceTest.java` |
| M | Testes | `src/test/java/com/projeto/services/AcaoPersistenceServiceTest.java` |
| M | Testes | `src/test/java/com/projeto/services/AcaoServiceTest.java` |
| M | Testes | `src/test/java/com/projeto/services/AlphaVantageConsumptionTest.java` |
| M | Testes | `src/test/java/com/projeto/services/EvolucaoPatrimonialConcurrencyTest.java` |
| M | Testes | `src/test/java/com/projeto/services/HistoricoCotacaoArchitectureTest.java` |
| M | Testes | `src/test/java/com/projeto/services/HistoricoCotacaoAtomicityTest.java` |
| M | Testes | `src/test/java/com/projeto/services/PosicaoServiceTest.java` |
| M | Testes | `src/test/java/com/projeto/services/SnapshotCarteiraConcurrencyTest.java` |
| ?? | Producao | `frontend/src/app/features/dashboard/models/atualizacao-cotacoes.ts` |
| ?? | Testes | `frontend/src/app/shared/formatters/cotacao-temporal.fixtures.ts` |
| ?? | Testes | `frontend/src/app/shared/formatters/cotacao-temporal.formatter.spec.ts` |
| ?? | Producao | `frontend/src/app/shared/formatters/cotacao-temporal.formatter.ts` |
| ?? | Testes | `frontend/src/app/shared/models/cotacao-temporal.spec.ts` |
| ?? | Producao | `frontend/src/app/shared/models/cotacao-temporal.ts` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/.openspec.yaml` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/certificacao-final-e-roteiro-manual.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/certificacao-funcional-financeira-build.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/checklist-validacao-manual.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/correcao-petr4-precisao-feedback.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/design.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/diagnostico-persistencia-referencia-data.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/diagnostico-precisao-petr4-manual.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/implementacao-alternativa-a.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/inventario-referencia-data.sql` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/pendencias-finais.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/plano-inventario-postgresql.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/preparacao-commit-arquivamento.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/proposal.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/prototipo-compatibilidade-jdbc-bc.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/resultado-correcao-dominio-temporal.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/revisao-dominio-temporal.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/revisao-final-pos-validacao-manual.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/alpha-vantage-consumption/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/api-documentation/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/frontend-dashboard-management/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/frontend-position-analysis/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/frontend-stock-management/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/portfolio-position/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/portfolio-quote-refresh/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/stock-quote-history/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/specs/stock-registration/spec.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/tasks.md` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/validacao-postgresql.md` |
| ?? | Documentacao | `openspec/specs/portfolio-quote-refresh/spec.md` |
| ?? | Producao | `src/main/java/com/projeto/dto/AtualizacaoCotacoesCarteiraResponse.java` |
| ?? | Producao | `src/main/java/com/projeto/dto/ErroAtualizacaoCotacaoResponse.java` |
| ?? | Producao | `src/main/java/com/projeto/dto/EstadoCotacaoResponse.java` |
| ?? | Producao | `src/main/java/com/projeto/dto/ReferenciaMercadoCotacaoResponse.java` |
| ?? | Producao | `src/main/java/com/projeto/dto/ResultadoAtualizacaoCotacaoResponse.java` |
| ?? | Producao | `src/main/java/com/projeto/dto/ResumoAtualizacaoCotacoesResponse.java` |
| ?? | Producao | `src/main/java/com/projeto/entities/ReferenciaTipoCotacao.java` |
| ?? | Producao | `src/main/java/com/projeto/integrations/cotacao/CotacaoCandidata.java` |
| ?? | Producao | `src/main/java/com/projeto/mappers/AtualizacaoCotacoesCarteiraMapper.java` |
| ?? | Producao | `src/main/java/com/projeto/persistence/ReferenciaDataJdbcType.java` |
| ?? | Producao | `src/main/java/com/projeto/resources/AtualizacaoCotacoesCarteiraResource.java` |
| ?? | Producao | `src/main/java/com/projeto/resources/exceptions/AtualizacaoCotacoesCarteiraExceptionHandler.java` |
| ?? | Producao | `src/main/java/com/projeto/services/AplicacaoCotacaoService.java` |
| ?? | Producao | `src/main/java/com/projeto/services/AtualizacaoCotacoesCarteiraService.java` |
| ?? | Producao | `src/main/java/com/projeto/services/RegraDecisaoCotacao.java` |
| ?? | Producao | `src/main/java/com/projeto/services/ResultadoAtualizacaoCotacao.java` |
| ?? | Producao | `src/main/resources/db/changelog/changes/007-add-referencia-temporal-cotacao.yaml` |
| ?? | Testes | `src/test/java/com/projeto/config/TimeConfigTest.java` |
| ?? | Testes | `src/test/java/com/projeto/entities/AcaoObservacaoMonetariaTest.java` |
| ?? | Testes | `src/test/java/com/projeto/entities/ReferenciaTemporalCotacaoTest.java` |
| ?? | Testes | `src/test/java/com/projeto/integrations/cotacao/BrapiPrecisionTest.java` |
| ?? | Testes | `src/test/java/com/projeto/integrations/cotacao/CotacaoCandidataProvidersTest.java` |
| ?? | Testes | `src/test/java/com/projeto/integrations/cotacao/CotacaoCandidataTest.java` |
| ?? | Testes | `src/test/java/com/projeto/integrations/cotacao/CotacaoDominioTemporalTest.java` |
| ?? | Testes | `src/test/java/com/projeto/integrations/cotacao/CotacaoPrecisaoTemporalTest.java` |
| ?? | Testes | `src/test/java/com/projeto/mappers/AtualizacaoCotacoesCarteiraMapperTest.java` |
| ?? | Testes | `src/test/java/com/projeto/repositories/ReferenciaTemporalMigrationTest.java` |
| ?? | Testes | `src/test/java/com/projeto/resources/AcaoCadastroTemporalIntegrationTest.java` |
| ?? | Testes | `src/test/java/com/projeto/resources/AcaoCotacaoHttpIntegrationTest.java` |
| ?? | Testes | `src/test/java/com/projeto/resources/AtualizacaoCotacoesCarteiraHttpIntegrationTest.java` |
| ?? | Testes | `src/test/java/com/projeto/resources/AtualizacaoCotacoesCarteiraResourceTest.java` |
| ?? | Testes | `src/test/java/com/projeto/resources/ReferenciaDataJpaDiagnosticoTest.java` |
| ?? | Testes | `src/test/java/com/projeto/resources/ReferenciaDataJpaPostgresqlTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/AplicacaoCotacaoPostgresqlTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/AplicacaoCotacaoServiceTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/AtualizacaoCotacoesCarteiraPostgresqlTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/AtualizacaoCotacoesCarteiraServiceTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/AtualizacaoIndividualCotacaoTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/PostgresqlTemporalSupport.java` |
| ?? | Testes | `src/test/java/com/projeto/services/RegraDecisaoCotacaoTest.java` |
| ?? | Testes | `src/test/java/com/projeto/services/ResultadoAtualizacaoCotacaoTest.java` |
| ?? | Documentacao | `openspec/changes/archive/2026-10-11-atualizar-cotacoes-da-carteira/arquivamento-e-staging.md` |

## Fora da selecao - sem apagar

| Status atual | Caminho | Motivo |
| --- | --- | --- |
| M | `frontend/src/app/shared/formatters/offset-date-time.formatter.spec.ts` | Sem diff textual;ruido de normalizacao |
| ?? | `src/test/java/com/projeto/repositories/ReferenciaDataJdbcDiagnosticoTest.java` | Experimento/diagnostico JDBC-BC,nao regressao obrigatoria de producao |
| ?? | `src/test/java/com/projeto/temporalpoc/BcCompatibilityJdbcType.java` | Experimento/diagnostico JDBC-BC,nao regressao obrigatoria de producao |
| ?? | `src/test/java/com/projeto/temporalpoc/ReferenciaDataBcPrototypeTest.java` | Experimento/diagnostico JDBC-BC,nao regressao obrigatoria de producao |

Generated folders target/**,frontend/dist/**,frontend/.angular/**,node_modules/**,graphify-out/**,logs,dumps,bancos locais econfiguracoes pessoais continuam fora. Nao ha esses arquivos na selecao. ReferenciaDataJpaDiagnosticoTest ePostgresqlTemporalSupport permanecem selecionados pois sao regressao REAL do binding de producao/SQL/API,H2/PG;nao confundir nome diagnostico com prototipo descartavel.

## Riscos e proximo passo

Sem bloqueante novo. RessalvasJDBC/BC/provider/budgets e limites da validacao visual permanecem conforme relatorios aprovados. Aviso detamanho daCLI nao afeta sincronizacao. Graphify existente foi consultado;nenhum codigo modificado,nenhuma reexecucao de geracao necessaria para este archive documental.

Proximo passo:usuario autorizar STAGING EXPLICITO desta lista,conferir git diff --cached integral,secrets/gerados/exclusoes e40tasks noarchive. Commit/push/merge exigem autorizacoes separadas;nao foram executados. Mensagem sugerida permanece:feat(cotacoes): atualizar cotacoes da carteira com referencia temporal exata.
