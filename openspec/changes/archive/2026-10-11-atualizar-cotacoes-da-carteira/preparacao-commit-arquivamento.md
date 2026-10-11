# Preparacao segura de commit e arquivamento

Status: change concluida por aprovacao explicita do usuario. Somente12.1,15.1,15.2 ratificadas nesta etapa. 40tasks/40concluidas/0pendentes. Branch atual:dev. Nenhum staging/commit/push/merge/archive executado;nenhuma exclusao/descarte automatico.

## Evidencias preservadas

O checklist e a revisao final conservam PETR4 inicialmente FALHOU,correcao do Clock/feedback e revalidacao PASSOU. Loading do lote NAO OBSERVADO;recarga visual dos indicadores NAO CONFIRMADA VISUALMENTE. Testes automatizados complementares de loading/duplo disparo/quatro GETs/data aplicada foram aceitos explicitamente pelo usuario. Nao substituir estados manuais por PASSOU.

Backend/frontend/build/matrizPG-H2/fusos/Graphify continuam com os resultados anteriores apresentados em revisao-final-pos-validacao-manual.md e correcao-petr4-precisao-feedback.md. Nao repetidas suites completas sem alteracao de codigo. JDBC localizado,Clock,frontend,contratos e regressao completos conforme revisao final aprovada.

## OpenSpec e fechamento

- openspec validate atualizar-cotacoes-da-carteira --strict:valida.
- openspec validate --specs --strict --no-interactive:35pass/0fail.
- openspec status --change atualizar-cotacoes-da-carteira --json:isComplete=true;proposal/design/specs/tasks done. Esse status de artefatos nao substitui contagem de checkboxes.
- Reconstrucao oficial atual em memoria:findSpecUpdates/buildUpdatedSpec + Validator(true).validateSpecContent:9/9validas,nenhum warning/erro;9ADDED/21MODIFIED/0REMOVED/0RENAMED. Nao invocados writeUpdatedSpec/Archive/migrations.
- Specs consolidadas AINDA nao incorporam os deltas:8existentes exigem atualizacao e portfolio-quote-refresh e nova. Trata-se de sincronizacao esperada no archive,nao conflito impeditivo. Capabilities:alpha-vantage-consumption,api-documentation,frontend-dashboard-management,frontend-position-analysis,frontend-stock-management,portfolio-position,portfolio-quote-refresh,stock-quote-history,stock-registration.
- Ready para archive com sincronizacao normal das specs;NAO usar --skip-specs ou --no-validate. Arquivamento nao executado nesta autorizacao de preparacao.

A funcao oficial foi apenas importada para rebuild em memoria;nenhum comando archive foi executado. A lista de arquivos abaixo usa os caminhos ATUAIS. Depois de archive autorizado,substituir caminhos openspec/changes/atualizar-cotacoes-da-carteira pela pasta archive retornada pela CLI e adicionar as9specs consolidadas resultantes;revisar novamente antes de staging.

## Classificacao e criterio de commit

Incluir codigo de producao/DTOs/persistencia/providers/frontend e007+master aprovados;testes de regressao efetivos e fixtures;PRD da arquiteturaH2;artefatosOpenSpec e relatorios historicos definitivos. Relatorios cujo titulo diz diagnostico/prototipo sao documentacao auditavel,nao codigo experimental de producao.

Os78nao rastreados existentes foram enumerados;este plano acrescenta um79o. Tres excluidos rastreados devem integrar a mudanca como delecoes justificadas:AcaoCotacaoPersistenceService,AcaoCotacaoConcurrencyTest eAcaoCotacaoPersistenceServiceTest,substituidos pelo nucleo compartilhado/testes deAplicacaoCotacaoService H2/PG. Nao restaurar automaticamente.

Fora do staging,sem apagar:
- src/test/java/com/projeto/repositories/ReferenciaDataJdbcDiagnosticoTest.java:comparacao experimental anterior/direto,tabelas temporarias/logging e assertions historicas;nao regressao obrigatoria da implementacao.
- src/test/java/com/projeto/temporalpoc/BcCompatibilityJdbcType.java eReferenciaDataBcPrototypeTest.java:prototipo opt-in isolado,superado pelo binding de producao/testes efetivos.
- frontend/src/app/shared/formatters/offset-date-time.formatter.spec.ts:statusM mas git diff --quiet exit0,sem mudanca textual necessaria. Nao normalizar/restaurar automaticamente.

IMPORTANTE:ReferenciaDataJpaDiagnosticoTest.java DEVE entrar,apesar do nome:regressao real do binding de producao/SQL/Acao/historico/API,base tambem do testePostgreSQL. PostgresqlTemporalSupport e suites PG opt-in DEVE entrar;skips padrao nao significam descartaveis. Fixtures temporais frontend tambem necessarias aos testes.

Gerados fora:target/**,frontend/dist/**,frontend/.angular/**,node_modules/**,graphify-out/**,clusters/bancos locais,logs,dumps e configuracoes pessoais. Nenhum deles figura na lista proposta. Nenhum application-*.properties/yml,docker credential ou.env modificado no inventario. Nao usar git add . ou git add -A:seriam abrangentes e incluiriam os experimentos nao rastreados. Nao ha alteracao funcional de outra change identificada;adaptações de fixtures em Carteiras/Acoes/snapshots pertencem a regressao dosDTOs/nucleo.

git check-ignore confirmou target,frontend/dist,frontend/.angular,frontend/node_modules egraphify-out ignorados. Scan local de assinaturas de chaves privadas/tokens nos arquivos alterados/novos retornou0matches,sem imprimir conteudos;nao constitui garantia universal de ausencia de segredos. git diff --check aprovado;scan horizontal de whitespace dos quatro documentos desta etapa sem ocorrencias. Na revisao de staging futura,conferir novamente conteudo e exclusoes.

## Mensagem sugerida

```text
feat(cotacoes): atualizar cotacoes da carteira com referencia temporal exata
```

Corpo opcional:regra compartilhada individual/lote,atomicidade porAcao,historico tipado,DATE civil localizado,Clock micros e feedback acessivel com recarga doDashboard. Registrar concluida a changeatualizar-cotacoes-da-carteira.

## Sequencia futura recomendada - requer autorizacao separada

1. Aprovar a selecao de staging e autorizar archive com sync das specs.
2. Executar archive oficial com validacao normal;conferir9specs consolidadas/pasta datada retornada,40tasks e strict/diff/whitespace. Nao executar banco/migrations.
3. Atualizar a lista de staging apenas quanto aos caminhos movidos e specs sincronizadas. Conferir status,arquivosnovos/delecoes e ausencia de gerados/secrets.
4. Autorizar/realizar staging explicito e revisar git diff --cached integral,especialmente tres delecoes e9specs. Nao incluir experimentos locais.
5. Commit na branchdev com mensagem aprovada. Push/merge sao autorizacoes distintas.

Alternativa se o usuario preferir checkpoint antes do archive:commit da lista atual e depois archive/specs em segundo commit explicitamente autorizado. Nao deixar um suposto commit de fechamento omitir a sincronizacao das specs sem explicar que e checkpoint.

## Riscos remanescentes

Sem bloqueante tecnico identificado. Permanecem rejeicao conservadora do provider,ressalvasBC/importacoes antigas,upgradeJDBC,budgets/warnings aprovados e limitacao visual registrada. Relatorios historicos conservam contagens/status antigos DATADOS;novo estado40/40tem precedencia. Logs ignorados devem ser preservados localmente ate concluir fechamento;nenhuma copia/limpeza feita automaticamente. Fora do staging nao equivale a apagar ou deixar arquivos sem avaliacao.

## Lista proposta para staging - caminhos atuais

138 caminhos (3 delecoes incluidas). Lista explicita,nao comandos executados. Todos relativos ao repositorio C:/Users/julia/Projetos/GestaoAcoes/gestaoacoes.

| Status | Categoria | Caminho |
| --- | --- | --- |
| M | Documentacao definitiva e historico | `docs/PRD.md` |
| M | Testes necessarios | `frontend/src/app/features/acoes/acoes.routes.spec.ts` |
| M | Testes necessarios | `frontend/src/app/features/acoes/acoes.service.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/acoes/models/acao.ts` |
| M | Testes necessarios | `frontend/src/app/features/acoes/pages/acao-create-dialog.spec.ts` |
| M | Testes necessarios | `frontend/src/app/features/acoes/pages/acao-create-page.component.spec.ts` |
| M | Testes necessarios | `frontend/src/app/features/acoes/pages/acao-detail-page.component.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/acoes/pages/acao-detail-page.component.ts` |
| M | Testes necessarios | `frontend/src/app/features/acoes/pages/acoes-list-page.component.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/acoes/pages/acoes-list-page.component.ts` |
| M | Testes necessarios | `frontend/src/app/features/carteiras/pages/carteira-detail-page.component.spec.ts` |
| M | Testes necessarios | `frontend/src/app/features/carteiras/pages/carteiras-dialog.integration.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/dashboard/dashboard-page.component.html` |
| M | Testes necessarios | `frontend/src/app/features/dashboard/dashboard-page.component.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/dashboard/dashboard-page.component.ts` |
| M | Testes necessarios | `frontend/src/app/features/dashboard/dashboard.service.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/dashboard/dashboard.service.ts` |
| M | Producao necessaria | `frontend/src/app/features/dashboard/models/dashboard.ts` |
| M | Testes necessarios | `frontend/src/app/features/dashboard/position-analysis/position-analysis.fixtures.ts` |
| M | Producao necessaria | `frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.html` |
| M | Testes necessarios | `frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.spec.ts` |
| M | Producao necessaria | `frontend/src/app/features/dashboard/position-analysis/position-performance-chart.component.ts` |
| M | Producao necessaria | `frontend/src/app/shared/formatters/offset-date-time.formatter.ts` |
| M | Testes necessarios | `frontend/src/app/shared/portfolio-positions/portfolio-positions.component.spec.ts` |
| M | Producao necessaria | `frontend/src/app/shared/portfolio-positions/portfolio-positions.component.ts` |
| M | Producao necessaria | `src/main/java/com/projeto/config/OpenApiConfig.java` |
| M | Producao necessaria | `src/main/java/com/projeto/config/TimeConfig.java` |
| M | Producao necessaria | `src/main/java/com/projeto/dto/AcaoResponse.java` |
| M | Producao necessaria | `src/main/java/com/projeto/dto/PosicaoResponse.java` |
| M | Producao necessaria | `src/main/java/com/projeto/entities/Acao.java` |
| M | Producao necessaria | `src/main/java/com/projeto/entities/HistoricoCotacao.java` |
| M | Producao necessaria | `src/main/java/com/projeto/integrations/cotacao/AlphaVantageAdapter.java` |
| M | Producao necessaria | `src/main/java/com/projeto/integrations/cotacao/BrapiAdapter.java` |
| M | Producao necessaria | `src/main/java/com/projeto/integrations/cotacao/CotacaoData.java` |
| M | Producao necessaria | `src/main/java/com/projeto/integrations/cotacao/CotacaoProvider.java` |
| M | Producao necessaria | `src/main/java/com/projeto/mappers/AcaoMapper.java` |
| M | Producao necessaria | `src/main/java/com/projeto/mappers/PosicaoMapper.java` |
| M | Producao necessaria | `src/main/java/com/projeto/repositories/AcaoRepository.java` |
| M | Producao necessaria | `src/main/java/com/projeto/resources/AcaoResource.java` |
| D | Producao necessaria | `src/main/java/com/projeto/services/AcaoCotacaoPersistenceService.java` |
| M | Producao necessaria | `src/main/java/com/projeto/services/AcaoPersistenceService.java` |
| M | Producao necessaria | `src/main/java/com/projeto/services/AcaoService.java` |
| M | Producao necessaria | `src/main/java/com/projeto/services/PosicaoService.java` |
| M | Producao necessaria | `src/main/java/com/projeto/services/exceptions/ErrorCodes.java` |
| M | Producao necessaria | `src/main/resources/db/changelog/db.changelog-master.yaml` |
| M | Testes necessarios | `src/test/java/com/projeto/dto/PosicaoContractTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/entities/AcaoTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/repositories/HistoricoCotacaoRepositoryTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/resources/AcaoEuaRegistrationRequestsTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/resources/OpenApiDocumentationTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/resources/PatrimonioResourceTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/resources/PosicaoResourceTest.java` |
| D | Testes necessarios | `src/test/java/com/projeto/services/AcaoCotacaoConcurrencyTest.java` |
| D | Testes necessarios | `src/test/java/com/projeto/services/AcaoCotacaoPersistenceServiceTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/AcaoPersistenceServiceTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/AcaoServiceTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/AlphaVantageConsumptionTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/EvolucaoPatrimonialConcurrencyTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/HistoricoCotacaoArchitectureTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/HistoricoCotacaoAtomicityTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/PosicaoServiceTest.java` |
| M | Testes necessarios | `src/test/java/com/projeto/services/SnapshotCarteiraConcurrencyTest.java` |
| ?? | Producao necessaria | `frontend/src/app/features/dashboard/models/atualizacao-cotacoes.ts` |
| ?? | Testes necessarios | `frontend/src/app/shared/formatters/cotacao-temporal.fixtures.ts` |
| ?? | Testes necessarios | `frontend/src/app/shared/formatters/cotacao-temporal.formatter.spec.ts` |
| ?? | Producao necessaria | `frontend/src/app/shared/formatters/cotacao-temporal.formatter.ts` |
| ?? | Testes necessarios | `frontend/src/app/shared/models/cotacao-temporal.spec.ts` |
| ?? | Producao necessaria | `frontend/src/app/shared/models/cotacao-temporal.ts` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/.openspec.yaml` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/certificacao-final-e-roteiro-manual.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/certificacao-funcional-financeira-build.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/checklist-validacao-manual.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/correcao-petr4-precisao-feedback.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/design.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/diagnostico-persistencia-referencia-data.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/diagnostico-precisao-petr4-manual.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/implementacao-alternativa-a.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/inventario-referencia-data.sql` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/pendencias-finais.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/plano-inventario-postgresql.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/proposal.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/prototipo-compatibilidade-jdbc-bc.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/resultado-correcao-dominio-temporal.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/revisao-dominio-temporal.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/revisao-final-pos-validacao-manual.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/alpha-vantage-consumption/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/api-documentation/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/frontend-dashboard-management/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/frontend-position-analysis/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/frontend-stock-management/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/portfolio-position/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/portfolio-quote-refresh/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/stock-quote-history/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/specs/stock-registration/spec.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/tasks.md` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/validacao-postgresql.md` |
| ?? | Producao necessaria | `src/main/java/com/projeto/dto/AtualizacaoCotacoesCarteiraResponse.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/dto/ErroAtualizacaoCotacaoResponse.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/dto/EstadoCotacaoResponse.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/dto/ReferenciaMercadoCotacaoResponse.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/dto/ResultadoAtualizacaoCotacaoResponse.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/dto/ResumoAtualizacaoCotacoesResponse.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/entities/ReferenciaTipoCotacao.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/integrations/cotacao/CotacaoCandidata.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/mappers/AtualizacaoCotacoesCarteiraMapper.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/persistence/ReferenciaDataJdbcType.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/resources/AtualizacaoCotacoesCarteiraResource.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/resources/exceptions/AtualizacaoCotacoesCarteiraExceptionHandler.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/services/AplicacaoCotacaoService.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/services/AtualizacaoCotacoesCarteiraService.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/services/RegraDecisaoCotacao.java` |
| ?? | Producao necessaria | `src/main/java/com/projeto/services/ResultadoAtualizacaoCotacao.java` |
| ?? | Producao necessaria | `src/main/resources/db/changelog/changes/007-add-referencia-temporal-cotacao.yaml` |
| ?? | Testes necessarios | `src/test/java/com/projeto/config/TimeConfigTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/entities/AcaoObservacaoMonetariaTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/entities/ReferenciaTemporalCotacaoTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/integrations/cotacao/BrapiPrecisionTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/integrations/cotacao/CotacaoCandidataProvidersTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/integrations/cotacao/CotacaoCandidataTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/integrations/cotacao/CotacaoDominioTemporalTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/integrations/cotacao/CotacaoPrecisaoTemporalTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/mappers/AtualizacaoCotacoesCarteiraMapperTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/repositories/ReferenciaTemporalMigrationTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/resources/AcaoCadastroTemporalIntegrationTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/resources/AcaoCotacaoHttpIntegrationTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/resources/AtualizacaoCotacoesCarteiraHttpIntegrationTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/resources/AtualizacaoCotacoesCarteiraResourceTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/resources/ReferenciaDataJpaDiagnosticoTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/resources/ReferenciaDataJpaPostgresqlTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/AplicacaoCotacaoPostgresqlTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/AplicacaoCotacaoServiceTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/AtualizacaoCotacoesCarteiraPostgresqlTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/AtualizacaoCotacoesCarteiraServiceTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/AtualizacaoIndividualCotacaoTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/PostgresqlTemporalSupport.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/RegraDecisaoCotacaoTest.java` |
| ?? | Testes necessarios | `src/test/java/com/projeto/services/ResultadoAtualizacaoCotacaoTest.java` |
| ?? | Documentacao definitiva e historico | `openspec/changes/atualizar-cotacoes-da-carteira/preparacao-commit-arquivamento.md` |

## Fora do staging - preservados no workspace

| Status | Categoria | Caminho |
| --- | --- | --- |
| M | Fora: sem diff textual | `frontend/src/app/shared/formatters/offset-date-time.formatter.spec.ts` |
| ?? | Fora: diagnostico/prototipo experimental | `src/test/java/com/projeto/repositories/ReferenciaDataJdbcDiagnosticoTest.java` |
| ?? | Fora: diagnostico/prototipo experimental | `src/test/java/com/projeto/temporalpoc/BcCompatibilityJdbcType.java` |
| ?? | Fora: diagnostico/prototipo experimental | `src/test/java/com/projeto/temporalpoc/ReferenciaDataBcPrototypeTest.java` |
