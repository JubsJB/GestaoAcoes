# Resultado da API REST

## Escopo

Etapa autorizada em 2026-10-11: DTOs, GET separado, documentacao OpenAPI e testes HTTP. Nucleo, servico de consulta, repositories, entidades, regras financeiras e estrategia transacional nao foram alterados nesta etapa.

GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras retorna application/json. DistribuicaoCorretoraResource injeta o servico existente pelo proxy Spring e mapeia resultados imutaveis com DistribuicaoCorretoraMapper. Mapper copia identificacoes, decimais, colecoes e diagnosticos sem recalcular, filtrar, reordenar ou acessar entidades gerenciadas. PosicaoResponse/OperacaoResponse e endpoints existentes permanecem intactos.

## Arquivos desta etapa

Producao criados:
- src/main/java/com/projeto/dto/DistribuicaoCarteiraResponse.java
- src/main/java/com/projeto/dto/PosicaoDistribuicaoResponse.java
- src/main/java/com/projeto/dto/QuantidadeCorretoraResponse.java
- src/main/java/com/projeto/dto/DiagnosticoCorretoraResponse.java
- src/main/java/com/projeto/dto/PrimeiroDeficitResponse.java
- src/main/java/com/projeto/dto/DiagnosticoReconciliacaoResponse.java
- src/main/java/com/projeto/mappers/DistribuicaoCorretoraMapper.java
- src/main/java/com/projeto/resources/DistribuicaoCorretoraResource.java
- src/main/java/com/projeto/resources/DistribuicaoCorretoraExamples.java

Producao modificado: src/main/java/com/projeto/services/exceptions/ErrorCodes.java, exclusivamente novo codigo FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS. Nenhum handler global alterado.

Testes: novo DistribuicaoCorretoraResourceTest; ampliados OpenApiDocumentationTest e DistribuicaoPosicaoCorretoraServiceIntegrationTest. Documentacao: tasks.md e este relatorio.

## Contrato e respostas

Todos os campos sao presentes, inclusive nulos. JsonInclude.ALWAYS localizado nos seis DTOs; listas copiadas imutaveis. IDs Long, ordem Integer, flags boolean, quantidades BigDecimal e data civil LocalDate. Nenhum double, arredondamento ou limite artificial de magnitude derivada.

200 para Carteira existente, incluindo vazia e inconsistencias informativas. 404 ObjectNotFoundException com StandardError/code:null/details:{}. Parametro nao conversivel Long retorna400 REQUEST_INVALIDO pelo handler existente. Replay consolidado invalido preserva409 HISTORICO_OPERACOES_INCONSISTENTE. RuntimeException inesperada, inclusive CannotSerializeTransactionException, e traduzida localmente em500 FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS com mensagem exata "Nao foi possivel consultar a distribuicao por corretora." e details:{}. Sem SQL/stack/credencial no response, sem retry ou resposta parcial.

Cinco exemplos completos do contrato-http.md sao publicados no OpenAPI e comparados estruturalmente com a serializacao real em testes HTTP:
- Reconciliada: consolidado100,Corretora A80 eSem corretora20; diagnostico globalnull.
- Negativo atual: consolidado60,A100,B-40; flags deBtrue/true e primeira evidencia-40.
- Historico regularizado: consolidado110,B10; flag atualfalse/historicatrue e primeira evidencia-10 preservada.
- Zerada com deficit: consolidado100,somente linhaA100; diagnosticoB0 preservado com primeira evidencia-10.
- Divergencia: consolidado101,total atribuido100,reconciliadafalse e codigoDIVERGENCIA_QUANTIDADES separado de grupos.

Vazio: {"carteiraId":1,"posicoes":[]}. Divergencia e decimal de grande magnitude sao fixtures isoladas do servico mockado, nunca dados alterados para forcar inconsistencia em banco. PrimeiroDeficit permanece civil e nao recebe limites temporais financeiros de referencia de cotacao.

## Testes executados

Primeira rodada focada:40 testes aprovados (13 HTTP novos,7 H2 e20 do nucleo).

Matriz de regressao:

```powershell
mvn -o '-Dtest=DistribuicaoCorretoraResourceTest,DistribuicaoPosicaoCorretoraServiceIntegrationTest,CalculadoraDistribuicaoCorretoraTest,CarteiraResourceTest,OperacaoResourceTest,PosicaoResourceTest,OpenApiDocumentationTest,ResourceExceptionHandlerTest,PosicaoServiceTest,CalculadoraPosicaoTest' '-Dspring.liquibase.enabled=false' '-Dspring.jpa.hibernate.ddl-auto=create-drop' '-Dspring.datasource.url=jdbc:h2:mem:broker-http-regression;DB_CLOSE_DELAY=-1' '-Dspring.datasource.driver-class-name=org.h2.Driver' test
```

129 testes,0 falhas,0 erros,0 ignorados;BUILD SUCCESS. H2 exclusivamente em memoria com schema efemero Hibernate; Liquibase desabilitado, nenhuma migration executada. Classe de integracao dedicada utiliza H2 broker-distribution. Suite financeira ampla e PostgreSQL temporario nao foram executados nem certificados nesta etapa.

Apos acrescentar teste explicito de ordenacao HTTP e corrigir whitespace nos DTOs, reexecucao final somente de DistribuicaoCorretoraResourceTest:14 testes aprovados,0 falhas/erros/ignorados. Nao somar reexecucoes:130 casos distintos nesta selecao (14 HTTP,8 H2,20 nucleo,20 Carteira,13 Operacao,7 PosicaoResource,14 OpenAPI,1 handler,14 PosicaoService,19 CalculadoraPosicao).

Cobertura HTTP inclui os cinco exemplos normativos integrais (tambem tipos/campos/nulos e arrays sem propriedades extras), vazio,404,400 por texto e overflowLong,409,500 seguro por falha de serializacao sem retry, decimal numerico999999999999999999999.123457 sem perda/string e ordenacao BRASIL antes deEUA mesmo quando tickerEUA precederia alfabeticamente. OpenAPI real confirma novo path, cinco status, cinco exemplos, seis schemas com todos os campos obrigatorios, numero decimal e data civil. Regressoes de contratos anteriores aprovadas.

GET real com servico transacional e persistencia H2: duas chamadas produzem JSON identico,quatro statements totais e zero inserts/updates/deletes; cotacao,quantidade e preco persistidos permanecem intactos. Nao depende de OSIV para serializar. Testes anteriores de snapshot concorrente, isolamento JDBC SERIALIZABLE com chamador READ_COMMITTED e duas consultas para12corretoras seguem aprovados. Nenhum provider chamado pelo fluxo novo.

## Consistencia e riscos

Controller nao possui transacao externa e usa bean injetado,sem self-invocation. REQUIRES_NEW/readOnly/SERIALIZABLE foi preservado. Resultados ja materializados e independentes de JPA sao mapeados e serializados apos retorno do servico; nao ha segunda leitura ou consulta por nome durante serializacao.

Riscos anteriores continuam: historico grande requer acompanhar custo; chamador transacional futuro pode exigir conexao adicional; SERIALIZABLE pode abortar por concorrencia. Endpoint traduz abortos em500 seguro,sem retries automaticos. PostgreSQL temporario continua pendente da task4.6;nenhum acesso ao PostgreSQL local. Nao ha nova decisao funcional necessaria identificada.

## Tasks e proxima etapa

Validacoes finais:OpenSpec strict da change aprovado;git diff --check aprovado;verificacao explicita de whitespace/EOF dos15arquivos desta etapa (incluindo nao rastreados) sem problemas. Avisos LF/CRLF em arquivos existentes nao sao erros de whitespace. Diff revisado com arquivos novos;nenhuma alteracao de contratos financeiros,entidades,migrations,frontend ou experimentos. Graphify AST atualizado:10049nodes,18300edges,811communities;avisos conhecidos de6arquivos semnodes,parserSQL opcional ausente e rotulos comunitarios defasados,sem bloqueio funcional.

Somente3.1,3.2,4.3,4.4 concluidas. Estado:29 tasks,12 concluidas,17 pendentes. 4.3 certificada em H2 e GET;4.6 permanece aberta para certificacao especifica PostgreSQL. 4.5 e tarefas frontend/documentacao final/fechamento permanecem abertas. Notas antigas nas evidencias sao historicas.

Proximo passo recomendado: modelos/guards/parsing lossless e consumidor frontend conforme contrato,sem ampliar escopo;concluir PostgreSQL temporario antes do fechamento. Sem migrations,Git de escrita,alteracao de specs consolidadas ou experimentos JDBC/BC.
