# Correcao funcional e certificacao automatizada

Status: pronta para ratificacao humana. Nenhuma checkbox alterada:40 totais,29 concluidas,11 pendentes;12.1 manual permanece pendente. Nenhuma migration, regra financeira, contrato HTTP ou dado PostgreSQL local alterado. Nao houve nova prova temporal nem staging/commit/push/merge/archive.

## 10.2: feedback individual

AcaoResponse do PATCH retorna preco, referencia tipada e obtencao, nao a classificacao ATUALIZADA/REUSO/COOLDOWN. Preco/referencia/obtencao nao permitem identificar inequivocamente a origem da tentativa: uma nova consulta pode retornar a mesma cotacao; concorrencia pode devolver estado atualizado por outra tentativa. Preservar contrato significa nao inventar essa classificacao.

AcaoDetailPageComponent agora captura o estado exibido antes do PATCH e compara os campos de cotacao retornados (preco, tipo/data/instante e obtencao), mantendo o DTO integral. Quando iguais, informa "Solicitacao processada. Cotacao retornada sem alteracao."; quando diferentes, "Solicitacao processada. Dados da cotacao atualizados na tela.". Textos reais no codigo usam acentos. Nao afirma consulta ao provider nem cotacao nova/aplicada por aquela tentativa. Comparacao de estado textual para feedback nao e regra financeira ou classificacao de mercado. dataHoraCotacao legado nao decide referencia/feedback; nenhuma comparacao com relogio do navegador, request extra ou novo DTO.

Limite explicito: distingue estado retornado alterado versus mantido, nao proveniencia exata de REUSO. O criterio aprovado no delta frontend-stock-management e informar processamento/estado, sem prometer atualizacao ou HTTP externo. Exigir futuramente uma classificacao exata no PATCH precisaria de nova decisao contratual, nao implementada nem necessaria para esse criterio.

Producao modificada nesta etapa: somente frontend/src/app/features/acoes/pages/acao-detail-page.component.ts. Loading, guarda de duplo PATCH, substituicao do DTO, erro/preservacao e outras acoes mantidos.

## Testes ajustados/adicionados

- acao-detail-page.component.spec.ts: cinco casos adicionais (estado igual, referencia DATA alterada, referencia INSTANTE alterada, somente obtencao alterada, somente timestamp legado alterado). Confirmam mensagem honesta, preco igual com metadata novo, DTO retornado intacto e ausencia de GET. Teste existente de busy/duplo clique/preco alterado atualizado para mensagem de estado.
- AcaoCotacaoHttpIntegrationTest: um teste novo de PATCH real/H2 com compra10 a20, venda4 a25, outra carteira com compra2 a50 e uma terceira com Acao distinta. Grava snapshot historico180 antes do PATCH35. Verifica posicao6/preco medio20/custo120/valor210/nao realizado90; segunda carteira valor70/nao realizado-30; terceira permanece30. Realizado20 e patrimonio210 verificados nos GETs. Operacoes preservam valores, quantidades, precos, tipo, data e ordem; snapshot continua180/timestamp original. Contagens de operacoes, snapshots, componentes e historico e estado da Acao permanecem iguais apos GETs; nenhum provider acessado pelos GETs. Nao e fixture no PostgreSQL local.

## Certificacao 13.1,13.2,13.3

26 suites financeiras diretamente pertinentes,175 testes executados, zero falhas/erros/skips, dentro da suite backend completa. Assercoes e resultados XML conferidos, nao apenas nomes dos testes.

| Task | Cobertura conferida |
| --- | --- |
| 13.1 | CalculadoraPosicaoTest(19), OperacaoServiceTest(20), OperacaoResourceTest(13), orquestracao(5)/concorrencia(3); compras/vendas, preco manual exato, fracionamento, preco medio ponderado, venda parcial/total, reabertura de ciclo, replay retroativo e ordem diaria, rejeicao de saldo/precisao invalidos; realizado7service/3resource/1concorrencia; nenhum calculo de operacao usa cotacao atual para substituir precoUnitario. |
| 13.2 | PosicaoServiceTest(14)/resource(7)/concorrencia(1), PatrimonioServiceTest(8)/resource(5)/concorrencia(1), ResumoCarteiraServiceTest(12)/resource(5), snapshotservice(6)/integracao(3)/concorrencia(3)/repository(7)/arquitetura(2), evolucaoservice(4)/resource(3), AcaoCotacaoHttpIntegrationTest(8). Novo caso valida efeito global somente pela mesma Acao, sem afetar carteira/Acao distinta, custos/operacoes ou snapshot anterior. |
| 13.3 | PrecoOperacaoServiceTest(7)/resource(8): previa historica independente, compra exige preco manual; leituras de operacao sem replay/Clock/escrita, leituras financeiras e evolucao sem provider/snapshot automatico. Novo caso verifica contagens e estado depois dos GETs financeiros. GET explicito de previa tem provider historico proprio por contrato; nao confundir com GETs de leitura patrimonial que nao consultam provider. |

Nenhuma divergencia financeira encontrada; nenhuma regra financeira modificada. Financeiro usa BigDecimal/exatidao e replay existentes. Certificacao nao substitui validacao visual/assistiva/mobile12.1 nem revisao humana final15.2.

## Comandos/resultados

- mvn -q test:100 suites,1.067 casos reportados,976 executados,91 skips ja auditados de opt-in/diagnosticos;0 falhas/erros. Log target/certificacao-final-backend.log; XMLs target/surefire-reports. Rodada final inclui o novo caso financeiro e suas assercoes de realizado/patrimonio.
- mvn -q -Dtest=AcaoCotacaoHttpIntegrationTest test: primeira rodada focada aprovada; depois ampliadas duas assercoes financeiras e executada a suite completa final. Log target/certificacao-final-financeira-integration.log.
- npm test -- --watch=false:70 arquivos,665 testes aprovados. Log target/certificacao-final-frontend.log.
- npm test -- --watch=false --include=src/app/features/acoes/pages/acao-detail-page.component.spec.ts:34 testes aprovados. Log target/certificacao-final-feedback.log.
- npx tsc -p tsconfig.app.json --noEmit e tsconfig.spec.json --noEmit:exit0.
- npm run build -- --configuration production --stats-json:exit0, config original/budgets inalterados. Log target/certificacao-final-build.log; frontend/dist/frontend/stats.json e browser/ gerados.

Backend executado em profile test/H2 efemero; nenhum acesso/escrita no PostgreSQL local e nenhum novo cluster temporal iniciado. Frontend esbuild executado com permissao de subprocesso exigida pelo ambiente Windows. Node24.19.0/npm11.17.0; Angular22.1.4. Dependencias/package.json/package-lock.json/angular.json nao modificados.

OpenSpec strict da change e35 specs consolidadas aprovados. git diff --check e whitespace dos arquivos desta etapa aprovados, incluindo o relatorio nao rastreado. Diff da correcao/teste revisado, alteracoes preexistentes preservadas. Graphify AST-only atualizado e fontes afetadas presentes (9683 nodes/17560 edges/791 comunidades); log target/certificacao-final-graphify.log. Avisos conhecidos de parser SQL ausente/configs sem nodes e labels defasados nao invalidam o grafo de codigo; nenhum pacote/LLM adicionado. tasks.md nao alterado.

## 12.4: build e budgets

Tamanhos exatos obtidos dos outputs do stats.json (grafo estatico do main, sem dynamic-import, mais styles global):

| Initial | Bytes |
| --- | ---: |
| main | 132374 |
| chunk compartilhado | 367386 |
| chunk pequeno | 887 |
| styles global | 16087 |
| TOTAL | 516734 |

Referencia disponivel:514418 bytes. Diferenca:+2316 bytes/+0,4502%. Comparacao numerica feita com producao atual; angular.json e dependencias permanecem inalterados. Nao ha stats/buildlog/versionamento completo do checkpoint antigo disponivel para comprovar identidade exata de runtime/artefatos ou atribuir sua variacao por chunk. Portanto comparabilidade estrita de ambiente do checkpoint e ressalva para ratificacao12.4, nao motivo para inventar uma medicao anterior ou executar investigacao extensa.

Dashboard permanece lazy: chunk principal65512 bytes (estimativa de transferencia15,64kB do CLI); contribuicoes compiladas no stats: dashboard-page21439, position-analysis2795, performance-chart8070, composition8503, geometria1042+408, presentation1037. Componentes existentes compartilham esse chunk; bytesInOutput nao sao tamanhos isolados de novos downloads e nao devem ser somados como uma metrica de rede independente. Feedback10.2 pertence ao detalhe lazy: JS7437 bytes; nao adiciona biblioteca ou import ao initial.

CSS compilado: Dashboard3699; position-analysis381; performance-chart1907; composition2168; portfolio-positions4153; portfolio-evolution5685 bytes. Component styles sao resultados de build/inline no JS, nao arquivos adicionais carregados separadamente somados ao initial. Os .scss nao foram alterados nesta etapa.

Avisos (nao erros):initial acima do warning500000 em16734 bytes,portfolio-positions acima de4000 em153,portfolio-evolution acima de4000 em1685. Todos abaixo dos limites de erro:initial1MB/component8kB. Nenhum budget aumentado. Variacao pequena do initial registrada para aceite; nao constitui benchmark de performance runtime/mobile. Nao ha regressao impeditiva de build; aceite da comparabilidade historica/warnings permanece humano.

## Ratificacao proposta e pendencias

Evidencias suficientes para10.2 (criterio de processamento/estado sem promessa de provider),13.1,13.2,13.3.12.4 possui build/budgets/medicoes atuais completos e comparacao nominal; ratificacao depende da aceitacao/confirmacao da comparabilidade do checkpoint e dos warnings, conforme criterio de mesmo ambiente. Nenhuma checkbox marcada.

12.1 continua dependendo da sessao manual consolidada.14.1 (strict+reconstrucao oficial em memoria),14.2 (confronto documental final),14.3 (ratificacao do grafo) e15.1/15.2 (evidencias/revisao humana) permanecem abertas; evidencias parciais anteriores nao encerram tasks compostas automaticamente. Nao iniciar validacao manual nesta etapa.
