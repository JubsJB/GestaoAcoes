# Certificacao final e roteiro manual

Data:2026-10-11. Status: certificacao tecnica apresentada para aprovacao; sessao manual NAO executada; change NAO encerrada.

Somente10.2,12.4,13.1,13.2,13.3 marcadas conforme autorizacao humana expressa.12.4 ratificada com aceite do aumento2316bytes/+0,45%, warnings abaixo dos limites de erro e limitacao de comparabilidade historica. Nenhum budget elevado. Estado40tasks/34concluidas/6pendentes:12.1,14.1,14.2,14.3,15.1,15.2. Nenhuma outra checkbox marcada.

Esta etapa alterou apenas tasks e documentacao/evidencias. Sem codigo de producao, banco, credenciais, migrations, archive ou operacao Git de escrita. Suites/build verdes nao repetidos; resultados anteriores abaixo identificados como anteriores.

## 14.1 - strict e reconstrucao oficial em memoria

Executado nesta etapa:

```powershell
openspec validate atualizar-cotacoes-da-carteira --strict
openspec validate --specs --strict
```

Change aprovada;35 specs consolidadas aprovadas. Reconstrucao com os modulos oficiais instalados de @fission-ai/openspec, via node --input-type=module:findSpecUpdates,buildUpdatedSpec,Validator(true).validateSpecContent,MarkdownParser e parser oficial requirement-blocks. Nao chamado archive/writeUpdatedSpec/retireSpec. Somente strings em memoria; specs consolidadas nao escritas. Hashes de cada base lida iguais antes/depois.

Evidencia integral:target/certificacao-final-reconstrucao.log (exit0,officialMemoryOnly=true,allPassed=true).

| Capability | Operacoes added/modified | Requisitos finais | Cenarios finais | Resultado |
| --- | --- | ---: | ---: | --- |
| alpha-vantage-consumption | 0/3 | 3 | 15 | strict/pass |
| api-documentation | 1/1 | 9 | 25 | strict/pass |
| frontend-dashboard-management | 1/0 | 15 | 82 | strict/pass |
| frontend-position-analysis | 1/0 | 11 | 54 | strict/pass |
| frontend-stock-management | 0/2 | 11 | 64 | strict/pass |
| portfolio-position | 0/3 | 24 | 72 | strict/pass |
| portfolio-quote-refresh (nova) | 4/0 | 4 | 18 | strict/pass |
| stock-quote-history | 0/6 | 9 | 22 | strict/pass |
| stock-registration | 2/6 | 26 | 99 | strict/pass |

Total9capabilities,9ADDED/21MODIFIED,112requisitos/451cenarios finais (incluem os previamente consolidados, nao sao todos novos). Nenhum REMOVED/RENAMED. O merger oficial reconstitui os blocos; ADDED/MODIFIED conferidos raw a raw contra o resultado, sem divergencia. findMissingCurrentScenarios retornou zero perdas em todos os MODIFIED. Purposes das8specs existentes preservados; Purpose autoral de portfolio-quote-refresh carregado na nova spec, sem TBD. Nenhum warning do merger. Validator apresentou apenas notas INFO de requisitos longos; nenhum erro/warning impeditivo.

Nao e aplicacao definitiva dos deltas, nem ratificacao automatica14.1; aguarda aprovacao.

## 14.2 - requisitos, cenarios, Purposes e whitespace

Proposal enumera uma capability nova e oito modificadas, correspondentes exatamente aos9deltas. Conferencia normativa/fonte/evidencia:

| Contrato | Conferencia |
| --- | --- |
| Modelo temporal/cadastro | CotacaoCandidata centraliza dominio de aquisicao; DATA civil0001-9999 sem piso1970/futuro; INSTANTE piso1970, micros exatos, <=obtencao propria/zero tolerancia; Clock backend e legado separados. Guard de leitura nao reaplica novos limites indiscriminadamente. |
| Persistencia civil | @JdbcType apenas nas duas referenciasDATA, SQL DATE/007 preservados; A aprovada apos inventario; BC segue valores/erros anteriores; nenhum versionamentoC/backfill. |
| Individual | PATCH sem payload funcional retorna AcaoResponse existente, sem classificacao de origem; feedback descreve processamento/estado, nao promete cotacao nova/provider. Referencia nao usa dataHoraCotacao legado. |
| Lote | POST sem body; selecao por replay/deduplicacao/zero excluido, sincrono sequencial, commit por ativo, sem transacao global/retry/rede sob lock.200 inclui vazio/parcial/todas falhas isoladas;500 global preserva possibilidade de commits anteriores. |
| Observacoes/historico | Lock+releitura; posterior/baseline atomicos; nova consulta igual avanca somente obtencao propria posterior; reuso/cooldown nao escrevem; tipos incompatíveis falham sem converter. |
| Frontend | Captura origem da carteira, guarda de duplo POST, feedback isolado, quatro GETs apos conclusao; erro de recarga distinto. Formatter tipado nas quatro superficies, DTO textual/lossless preservado. |
| Financeiro/GET/OpenAPI | Operacoes/preco/custo/replay e snapshots preservados; previa independente; GETs patrimoniais sem provider/escrita; OpenAPI derivado de recursos/DTOs reais. GET explicito de previa historica tem provider proprio por contrato, nao contradiz ausencia de consulta automatica nos GETs patrimoniais. |

Referencia de evidencia:implementacao-alternativa-a.md,certificacao-funcional-financeira-build.md,validacao-postgresql.md e registros historicos tasks.md. Nomes historicos de cenarios como Fallback temporal foram preservados por identidade, mas o corpo aprovado explicita ausencia de fallback; nao interpretar o nome antigo como autorizacao funcional.

git diff --check:exit0. Scan de131arquivos alterados/nao rastreados antes deste novo relatorio (Java/TS/HTML/SCSS/MD/SQL/JSON/YAML/properties):unicas ocorrencias foram tres hard breaks Markdown intencionais preexistentes em docs/PRD.md:3,4,5. Sem limpeza fora de escopo. Relatorio novo tambem conferido. Nenhuma mudanca nas specs consolidadas para simular fechamento.

Nenhuma divergencia normativa bloqueante encontrada no escopo revisado. Artefatos de planejamento/diagnosticos guardam historia e contagens antigas datadas, nao substituem o estado atual.14.2 permanece aberta para aprovacao.

## 14.3 - grafo e relacoes

Graphify atualizado na ultima etapa de codigo:9683nodes/17560edges/791comunidades. Nenhum codigo mudou depois disso; nao regenerado inutilmente nesta etapa documental. Consulta graphify query e paths/inspecao dos links EXTRACTED conferidos com fontes:

- AtualizacaoCotacoesCarteiraService -> AcaoService -> AplicacaoCotacaoService (references), coerente com nucleo compartilhado de individual/lote.
- AplicacaoCotacaoService -> AcaoRepository (references), com lock/releitura do estado e persistencia de historico no codigo.
- dashboard-page.component.ts -> dashboard.service.ts (imports_from); arquivo da pagina -> DashboardService (imports). Busca dirigida entre os dois nodes de CLASSE nao encontrou path, pois import esta no node ARQUIVO, nao significa dependencia ausente. Nao inventado edge de classe.
- Acao.java e HistoricoCotacao.java -> ReferenciaDataJdbcType (imports), e ReferenciaDataJdbcType -> org.hibernate.type.descriptor.jdbc.LocalDateJdbcType (inherits).
- Fontes dos testes de certificacao/formatter/candidata e componentes presentes no grafo; chamadas e relacoes centrais confrontadas com codigo, nao tratadas como prova de execucao financeira.

Comandos conferidos:graphify query,graphify path AtualizacaoCotacoesCarteiraService AplicacaoCotacaoService,graphify path AplicacaoCotacaoService AcaoRepository,graphify path ReferenciaDataJdbcType LocalDateJdbcType e path de arquivos Dashboard --undirected. CLI avisou ambiguidade de matching de nomes em algumas buscas; IDs/source_file/links exatos conferidos para resolver, sem usar caminho como garantia runtime.

Limites conhecidos nao bloqueantes:parser SQL ausente,6configs sem nodes e labels parcialmente defasados; grafo AST nao substitui testes ou documentacao normativa.14.3 aguarda aprovacao, sem nova instalacao/LLM.

## 15.1 - indice consolidado de evidencias

| Evidencia/etapa anterior | Comando ou metodo | Resultado/artifact |
| --- | --- | --- |
| Backend final | mvn -q test | 100suites/1067reportados/976executados/91skips auditados/0falhas-erros;target/certificacao-final-backend.log e target/surefire-reports |
| Financeiro | Assercoes de26suites dentro da suite completa,175testes | certificacao-funcional-financeira-build.md;novo PATCH real/H2 com duas carteiras relacionadas, terceira isolada e snapshot anterior |
| Frontend final | npm test -- --watch=false | 70arquivos/665pass;target/certificacao-final-frontend.log |
| Feedback individual | ng test include acao-detail-page.component.spec.ts | 34pass;target/certificacao-final-feedback.log |
| TypeScript | npx tsc -p tsconfig.app.json --noEmit e spec | ambos exit0, registrado na certificacao anterior |
| Build | npm run build -- --configuration production --stats-json | exit0;initial516734bytes;target/certificacao-final-build.log e frontend/dist/frontend/stats.json;12.4 ressalvas aceitas pelo usuario |
| H2/PG temporal real | mvn opt-in ReferenciaDataJpaDiagnosticoTest,ReferenciaDataJpaPostgresqlTest,AplicacaoCotacaoPostgresqlTest,AtualizacaoCotacoesCarteiraPostgresqlTest por fuso | 205pass/fuso UTC/LA/Kiritimati;target/alternativa-a-temporal-{UTC,Los_Angeles,Kiritimati}.log;cluster temporario encerrado |
| Quatro superficies/fusos | ng test com7arquivos por TZ | 134pass/fuso;target/alternativa-a-superficies-{UTC,America_Los_Angeles,Pacific_Kiritimati}.log |
| Migration H2/PG | XMLs preservados da EtapaD | target/etapa-d-reports-pg/TEST-com.projeto.repositories.ReferenciaTemporalMigrationTest.xml:6pass/0skip;007inalterada |
| Inventario local anterior | SELECT/read-only/timeouts, sem dados individuais | agregado registrado na decisao A:acao5/1DATA e historico9/1DATA,ambas2026-10-02,sem BC/ambiguidade/inconsistencia;nao repetido |
| Graphify | graphify update . AST-only anterior + consultas nesta etapa | target/certificacao-final-graphify.log;relacoes detalhadas acima |
| Certificacao atual | strict change/specs + merger oficial em memoria | target/certificacao-final-reconstrucao.log;9/9pass e35specs/strict |

91skips explicados no relatorio pendencias-finais.md, sem cobertura obrigatoria ausente para tasks ratificadas. Numeros de metodos opt-in nao expandidos diferem das invocacoes parametrizadas quando ativados. Resultados acima nao representam reexecucao nesta etapa.

Evidencias tecnicas consolidadas; falta anexar o resultado da sessao manual quando executada.15.1 permanece aberta para aprovacao; nao registrar screenshots, leitura de tela ou sucessos manuais que nao ocorreram.

## 15.2 - pacote de revisao humana

Inventario do diff inclui arquivos rastreados alterados e nao rastreados; git diff sozinho nao inclui os novos arquivos. Conferir em conjunto:

1. Dominio/providers/cadastro:adicao dos metadados, fronteira candidata, parsing DATA/INSTANTE, precisao, ausencia legitima vs invalida. Contratos legados preservados.
2. Aplicacao/concorrencia/lote:novo nucleo/DTOs/resource/handler/mappers e retirada do antigo AcaoCotacaoPersistenceService/testes substituidos pelo nucleo compartilhado. Verificar que retirada nao elimina cobertura (suites unitarias/integracao/concorrencia novas e existentes verdes).
3. SQL:007aditiva ja existente na change; fronteira JDBC apenas dois campos. Nenhuma alteracao nova de migration ou limpeza de dados nesta certificacao.
4. Frontend:Dashboard command/estados/quatro recargas;guard/parsing/formatter;referencia em quatro superficies;feedback10.2 nao promete proveniencia. Sem nova biblioteca/FX/formula ou polling.
5. Financeiro:efeito global porAcao sem alterar operacoes ou snapshots;cenario novo de certificacao e assercoes de valor/custo/realizado. Sem alteracao de regras financeiras.
6. Documentos/PRD:alteracao PRD restrita a arquitetura H2somente testes;deltas e registros de decisoes;ressalvas BC/importacoes e build ja aceitas. Aprovar o diff integral, inclusive arquivos novos, nao apenas patch da ultima etapa.

Pontos a aprovar:conformidade final de contratos/diff e evidencias14.x/15.1;aceite visual/assistivo12.1;confirmacao final15.2 depois da sessao. Nao ha autorizacao automatica para archive/Git nem fechamento neste relatorio.

## 12.1 - uma sessao manual minima

Preparacao:ambiente dev/teste normal autorizado, dados preservados, uma carteira com posicoes e uma Acao existente. Registrar navegador/versao,fuso,viewport,metodo assistivo,esperado/observado e evidencia curta. Nao executar migration/reset ou alterar credenciais/providers para fabricar erro. Esta etapa NAO iniciou servidores ou sessao manual.

### Validavel diretamente na interface

| Passo | Acao | Aceite visual/assistivo |
| --- | --- | --- |
| M1 Individual/desktop | Abrir detalhe por teclado, acionar Atualizar cotacao uma vez | busy/disabled perceptiveis, foco previsivel, retorno libera acao;estado retornado exibido e mensagem de processamento honesta;erro natural preserva cotacao. |
| M2 Estado mantido | Se ocorrer resposta igual ou reuso/cooldown normal, observar uma nova solicitacao manual | mensagem sem promessa de cotacao nova/provider. Nao exigir provar REUSO pelo PATCH, pois response nao classifica origem;se estado nao ocorrer, usar T1. |
| M3 Lote/Dashboard | Selecionar carteira, acionar Atualizar cotacoes separado de Atualizar dados | loading anunciado,botao ocupado,feedback/contadores identificam origem;paineis/posicoes/desempenho coerentes apos recarga;acao liberada,sem layout quebrado. Carteira vazia, se disponivel,mostra disabled com explicacao. |
| M4 Temporalidade | Inspecionar lista/detalhe de Acoes,posicoes e desempenho com registros disponiveis | DATA somente civil,INSTANTE hora local,DESCONHECIDA explicita,obtencao separada/nula Nao informada;sem fallback ao legado. Tipos ausentes vao a T2,nao editar dados. |
| M5 Mobile/assistiva | Uma passagem em viewport estreito (ex.360px) nos estados acima, teclado e leitor de tela | sem sobreposicao/perda de texto,foco visivel,nomes e motivos disabled compreensiveis,loading/feedback anunciados,contraste adequado. Registrar leitor usado;DOM automatizado nao certifica anuncio real. |

Nao precisa repetir chamadas para testar concorrencia,ordem de GETs,troca/cancelamento,round-tripBC/1582/0001/9999,fusos,precision,replay,rollback ou todas as combinacoes HTTP:ja automatizados. Na sessao manual,a meta e experiencia visual/assistiva de poucos estados representativos,nao revalidar algoritmo financeiro. Sucesso depende do provider;nao inventar sucesso se retornar falha conservadora/limite.

### Exige simulacao tecnica se nao houver estado real disponivel

- T1:resposta individual igual e erro representativo;feedback de lote parcial e todas as falhas isoladas. Usar fixtures/respostas ja validadas numa sessao UI isolada com interceptacao ANTES de enviar mutacao real. Usuario apenas inspeciona mensagens,contraste,anuncios e recuperacao;nao precisa provocar falhas de provider ou alterar configuracao. Todas as falhas isoladas podem ser HTTP200;nao confundir com erro global500/resultado nao confirmado.
- T2:tipos temporais ausentes nos dados locais e obtencao nula. Simular DTOs completos ja testados para conferir somente renderizacao nas superficies,sem gravar fixtures no PostgreSQL.
- Opcional somente se visualmente necessario:resultado nao confirmado/erro de recarga usando fixture aprovada;logica e recuperacao ja automatizadas. Nao exigir novamente como bateria completa.

Simulacoes precisam ser identificadas como UI simulada,nao sucesso de integracao. Nenhuma ferramenta/fixture nova implantada nesta etapa;apoio tecnico prepara a sessao se necessario,sem editar producao ou dados. Falhas/reuso/tipos que ja aparecerem naturalmente dispensam simulacao equivalente.

## Estado e proxima aprovacao

Nenhuma divergencia tecnica bloqueante encontrada nesta certificacao. Restam a sessao12.1 e aprovacao das evidencias14.1/14.2/14.3/15.1 e revisao humana15.2. Todas continuam abertas;nao chamar a change concluida antes dessas aprovacoes. Proximo passo minimo:aprovar certificacao tecnica apresentada,realizar uma sessao manual consolidada e aprovar o diff final. Sem nova investigacao temporal ou repeticao de suites verdes.
