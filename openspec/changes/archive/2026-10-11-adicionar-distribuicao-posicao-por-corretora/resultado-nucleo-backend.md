# Nucleo backend de distribuicao informativa

Etapa autorizada em2026-10-11:calculo puro e testes,sem controller/endpoint/frontend/repositorios/migrations. Tasks29/5concluidas/24pendentes.

## Arquivos e arquitetura

- src/main/java/com/projeto/services/CalculadoraDistribuicaoCorretora.java:novo componente sem repositorios/Clock/providers. Recebe carteiraId e lista de operacoes ja adquirida pelo chamador em conjunto consistente. Filtra Carteira,agrupa Acao,ordena COPIA das listas por dataOperacao/ordemNoDia/id e usa CalculadoraPosicao.validarQuantidade para consolidado. Nenhuma mudanca em CalculadoraPosicao ou entidade/servico de operacoes.
- src/test/java/com/projeto/services/CalculadoraDistribuicaoCorretoraTest.java:20 invocacoes unitarias,entidades em memoria,sem conexao DB.
- tasks.md:somente cinco checkboxes do nucleo/testes concluidas. Este relatorio registra evidencias,nunca conclui7.1/7.5.

Resultados internos sao records,nao DTOs HTTP novos. Colecoes copiadas/imutaveis;BigDecimal exato. Gruposnull usamSem corretora informada;nomes seguem trim/fallback contratado. Quantidades negativas nao impedem distribuicao de consolidado valido. Zero excluido SOMENTE da lista quantitativa;diagnostico de todo grupo mantido,inclusive saudavel. Consolidadozero excluido.

PrimeiroDeficit contem operacaoId,dataOperacao,ordemNoDia,saldoAposOperacao negativo. Capturado somente uma vez,sem trocar pelo pior deficit ou resetar em regularizacao/novo ciclo. Flags atuais/historicas independentes e coherentes;reconciliada nao significa ausencia de deficits.

Reconciliacao soma todos os finais com sinal,zeros e categoria null e compara porcompareTo com quantidade do replay oficial do MESMO conjunto. Funcao interna package-private permite teste isolado da divergencia guardada,sem fabricar operacoes/persistencia divergentes. Diagnostico DIVERGENCIA_QUANTIDADES separado de corretoras. Nenhuma correção automatica.

Ordenacao ordinalString.compareTo,nome/id e null-last;posicoesBRASIL/EUA,ticker/id. Nenhuma collation,timezone ou arredondamento. Nenhum limite19digitos artificial em acumuladores derivados.

## Testes efetivamente executados

Primeira rodada:mvn -o -Dtest=CalculadoraDistribuicaoCorretoraTest,CalculadoraPosicaoTest test:38aprovados/0falhas-erros-skips (19novos antes da cobertura adicional de venda financeira).

Rodada final:mvn -o -Dtest=CalculadoraDistribuicaoCorretoraTest,CalculadoraPosicaoTest,CalculadoraRentabilidadeTest,PosicaoServiceTest,ResultadoRealizadoServiceTest,OperacaoServiceTest,OperacaoOrchestrationTest test:89aprovados/0falhas-erros-skips,BUILD SUCCESS. Composicao20+19+4+14+7+20+5. XMLs emtarget/surefire-reports/TEST-com.projeto.services.<classe>.xml. Nao somar reexecucoes como cobertura nova.

Cobertura:uma/varias corretoras,null com deficit,negativo atual/flags,primeira ocorrencia diferente do minimo,regularizacao,zero saudavel/historico,ciclo posterior,consolidadozero,retroativa/entrada fora de ordem/idnaofinanceiro,BRASIL/EUA,decimal0.123457,compensacao derivada acima19digitos,reconciliacao com negativos/null/zero e divergencia sintetica,isolamento de Carteiras/Acoes,ordenacao comempates/caixa/acentos,nomesfallback,resultadoimutavel,replayconsolidado invalido e quantidadefracionariaBRASILrejeitada pelo replay existente. Venda com lucro confirma preco medio10/custo600/realizado200 iguais antes/depois e entidades/precos inalterados.

Regressao SOMENTE UNITARIA. Classes Mockito usam attachJVM autorizado/offline;sem alteracao da ferramenta/contratos. Nao executadas suitesSpring/Liquibase,H2/PG,replay de snapshots em DB oufrontend;nenhuma migration executada. Avisos de compilacao deprecacao/unchecked em classes existentes,sem falha.

## Limites e proxima etapa

Validacao:OpenSpec strict da change aprovado apos tasks;diff/whitespace incluindo novos arquivos verificados. Graphify atualizado AST-only apos codigo estabilizado:9965nodes/18010edges/804comunidades. Avisos conhecidos:6configs semnodes,parserSQLausente e labels parcialmente defasados;nenhuma instalacao/LLM. Nao concluir7.2/7.3 por validacao parcial desta etapa.

2.1/2.2/2.6 parciais em termos de algoritmo,mas NAO concluídas:fronteira transacional e consultaCarteira/nomebatch/snapshot/N+1 pendentes. Chamador futuro deve adquirir entidades/nomes num snapshot consistente;acessar entidadeCorretora no calculador nao comprova ausenciaN+1. Identidade inexistente de Carteira sera distinguida de lista vazia na fronteira futura,nao pelo calculador puro.

HTTP,DTOs publicos,404/400/500 e serializacao ficam para etapa posterior. Nucleo apenas preserva erro409HISTORICO_OPERACOES_INCONSISTENTE do replay invalido. Teste divergencia usa funcao de reconciliacao,nao garante que exista divergencia real em dados nem deve enfraquecer integridade para produzi-la.

Sem nova decisao funcional ou bloqueante encontrado. Proxima etapa recomendada:fronteira de leitura consistente e carga de nomes semN+1,com integracao isoladaH2/PG quando autorizada;depois HTTP. Nao iniciar automaticamente. Nenhum acesso aoPostgreSQLdev,alteracao experimental,spec consolidada ou operacaoGit de escrita.
