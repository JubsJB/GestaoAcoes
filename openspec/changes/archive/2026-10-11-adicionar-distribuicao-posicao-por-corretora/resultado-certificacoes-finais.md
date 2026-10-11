# Certificacoes finais e revisao visual

## Escopo e resultado

2026-10-11. Nenhuma alteracao de producao,estilos,contratos financeiros/HTTP,Dashboard,entidades,migrations ou specs consolidadas. Novo teste opt-in DistribuicaoPosicaoCorretoraPostgresqlTest reutiliza os oito testes reais de persistencia/consulta H2. Arquivos alterados nesta etapa: esse teste,tasks.md e este relatorio. Experimentos JDBC/BC preservados.

## Revisao visual e acessibilidade

Preview GET-only previamente criado em127.0.0.1:4287,proxy exclusivo para mock127.0.0.1:4288;sem banco/providers. Conferidos painel expansivel,espacamentos/alinhamento,avisos de zero historico/negativo/divergencia,duas corretoras incluindo categoria null,nome longo e quantidade999.999.999.999.999.999.999,123457. Sem problema comprovado que justifique reforma ou correcao visual. Matriz automatizada cobre varios grupos/avisos;preview nao constitui teste de volume/performance.

360px:documentElement.scrollWidth=innerWidth=360. Foco emSUMMARY,outline solid;Enter/Espaco certificados anteriormente,Tab confirmado novamente. Semantica de tabelas/caption/scope,regioes rotuladas e diagnosticos nomeados presentes na arvore acessivel. Avisos possuem texto/iconografia,nao somente cor. Contraste medido nos textos do tema atual:secundario5,90:1,principal15,96:1 sobreRGB245/247/246. Verificacao pontual,nao auditoria completa de todos os temas/estados. Zoom real125/150/200 e leitura efetiva por leitor de tela NAO EXECUTADOS nem aprovados. Screenshot de preview anterior continua emfrontend/.angular/broker-panel-360.jpg,ignorado;nao evidencia de dados reais.

## Build

npx ng build --configuration production:exit0. Initial517,02kB,transferencia estimada122,24kB;detalhe da Carteira lazy44,56kB;Dashboard lazy65,52kB. Limites inalterados:initial warning500kB/error1MB,componentStyle warning4kB/error8kB. Warnings:initial+17,02kB;portfolio-positions4,15kB(+153bytes);portfolio-evolution5,68kB(+1,69kB). Nenhum warning do painel novo. Os tres avisos ja existiam no build anterior;initial teve variacao pequena. Referencia arquivada516734bytes;saida atual arredondada517,02kB nao permite afirmar delta exato embytes semstats comparaveis. Bundle do detalhe cresceu com painel/consumidor lazy;nao e perda de financial precision nem alteracao deDashboard. Comparabilidade historica limitada,nenhum budget aumentado. Sem erros de compilacao/budget.

## Frontend

249testes/25arquivos aprovados,0falhas. Reexecutados carteiras/**/*.spec.ts,core/http/*.spec.ts,operacoes/*.spec.ts,dashboard.service.spec.ts,dashboard/evolution/*.spec.ts,shared/portfolio-positions/*.spec.ts via ng test --watch=false --include. TypeScript app/spec --noEmit aprovados. Preservacao da tabela consolidada durante falha,ausencia decomparacao entreGETs,precision lossless,avisos historicos,retry/cancelamento econtexto cobertos. Primeira execucao bloqueada porEPERM emcache temporario do sandbox,sem executar casos;reexecucao com permissao adequada passou. Nenhum teste ajustado para contornar falha funcional.

## Backend e financeiro

Matriz Maven offline comLiquibase false,ddl-auto create-drop eH2 exclusivamente emmemoria. Classes:DistribuicaoCorretoraResourceTest,DistribuicaoPosicaoCorretoraServiceIntegrationTest,CalculadoraDistribuicaoCorretoraTest,CarteiraResourceTest,OperacaoResourceTest,PosicaoResourceTest,OpenApiDocumentationTest,PosicaoServiceTest,CalculadoraPosicaoTest,CalculadoraRentabilidadeTest,OperacaoServiceTest,OperacaoOrchestrationTest,OperacaoConcurrencyTest,PosicaoConcurrencyTest,SnapshotCarteiraServiceTest,SnapshotCarteiraIntegrationTest,SnapshotCarteiraConcurrencyTest,SnapshotCarteiraArchitectureTest,AplicacaoCotacaoServiceTest,HistoricoCotacaoAtomicityTest,AtualizacaoIndividualCotacaoTest,PrecoOperacaoServiceTest.

217casos distintos certificados:213aprovados na matriz;4concorrentes inicialmente com erro Table OPERACAO not found devido ao compartilhamento deURLH2/create-drop entrecontextos. Reexecucao isolada OperacaoConcurrencyTest/PosicaoConcurrencyTest comnovoH2:4aprovados,0falhas/erros/ignorados. Nao ocultar falha inicial nem somar reexecucoes. Rodada anterior restrita pelo sandbox falhou ao abrirloopback;nao evidencia de regressao funcional. Logs ignorados emtarget/broker-final-regression.log ebroker-concurrency-regression.log. Cobertura decompra/venda/replay/preco medio/custo/realizado/rentabilidade/concorrencia/snapshots imutaveis/cotacoes;nenhuma regra financeira ou caminho de escrita modificado.

## PostgreSQL temporario

Dockerdaemon indisponivel,mas initdb/pg_ctl oficiais16 encontrados emC:/Program Files/PostgreSQL/16/bin. Criado cluster novo exclusivamente emtarget/broker-pg-disposable-20261011,usuario broker_disposable_test,UTF8/localeC,listen127.0.0.1:55486,databasepostgres. Nunca usado localhost5432/gestaoacoesdb nem credenciais de desenvolvimento. Trust somente no cluster efemero loopback;sem acesso externo. Schema descartavelHibernate create-drop,Liquibase false;nenhuma migration executada. Cluster encerrado via pg_ctl -D exato -m fast -w stop apos testes;arquivos temporarios ignorados preservados,sem exclusao automatica.

Comando:mvn -o -Dtest=DistribuicaoPosicaoCorretoraPostgresqlTest -Dbroker.test.postgresql.enabled=true -Dbroker.test.postgresql.url=jdbc:postgresql://127.0.0.1:55486/postgres test. 8aprovados,0falhas/erros/ignorados,BUILD SUCCESS. URL/product/user conferidos efetivamente emJDBC antes de cada caso. Teste rejeita URLfora doloopback/port5432;opt-in explicito. Testa snapshot sob escrita concorrente,nome/operacoes estaveis,SERIALIZABLE comcallerREAD_COMMITTED,2queries para12brokers,precisao0.123456/0.000001,isolamento/null/zero/negativos,GETrepetido seminsert/update/delete. Log target/broker-postgresql-regression.log ignorado. H2regressao mantida. Novo teste nao deve ser executado contra cluster existente:requer bootstrap descartavel como documentado.

## OpenSpec e grafo

Strictchange aprovado;36specs consolidadas aprovadas semalteracao. Graphify ASTupdate:10115nodes,18468edges,815communities. Avisos conhecidos:6configuracoes semnodes,parserSQL opcionalausente,rotulos comunitarios desatualizados. Nao reconstruida sincronizacao oficial emmemoria nesta etapa;7.2 continua pendente dessa verificacao final e diff completo. 7.3 permanece pendente deconferencia final de escopo/relacoes,embora update executado. Whitespace/diff verificados incluindo novos arquivos desta etapa.

## Tasks e roteiro manual minimo

Concluidas nesta etapa4.5,4.6,6.4.29total/22concluidas/7pendentes:1.1,1.2,7.1,7.2,7.3,7.4,7.5.1.1/1.2 possuem autorizacoes historicas mas continuam abertas aguardando ratificacao documental,semreinterpretar escopo. Fechamento ainda depende dedocumentacao/rebuild/conferencia/aprovacao evalidacao assistiva real;nao ha bloqueio funcional novo encontrado.

1. No detalhe da Carteira,usar zoom real do navegador125%,150%,200%;expandir uma posicao,conferir nomes/quantidades/avisos/foco/retry semcorte ouoverflow. Nao substituir porviewport equivalente.
2. Com leitor detela real,percorrer cabecalho/summary/tabela e expandir/recolher;conferir nomes/quantidades,estado expandido eaviso historico anunciado compreensivelmente,semavisos duplicados. Incluircarregamento/erro/retry apenas paraaceite dos anuncios,nao repetir algoritmo.
3. Usar aviso historico representativo no preview isolado se indisponivel emdados reais;corretora zerada somente noaviso,primeira ocorrencia legivel. Nao criarfixtures noPostgreSQLlocal.
4. Quando ambiente real estiver autorizado/disponivel,conferirGET integrado emCarteira existente eo painel seminterferir nasposicoes. Nenhum backendreal iniciado nesta etapa. Se dados reais nao possuem aviso,sua simulacao visual isolada basta paraesse aceite,mas nao substitui conectividade pontaaponta.

Nao repetir360px/Enter/Espaco ja aprovados,pois nenhum codigo visual foi alterado. Nao declarar leitura detela/zoom/integracao real aprovados antecipadamente. SemGit deescrita,archive oualteracao deDBlocal.
