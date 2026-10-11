# Resultado da camada de consulta

## Escopo e revisao previa

Nucleo revisado contra design, contrato-http e deltas: nenhuma divergencia relevante encontrada. Nao foi alterado. A primeira ocorrencia continua cronologica, zeros preservam diagnosticos, negativos integram a soma e nenhum calculo monetario e reimplementado.

Implementacao em 2026-10-11: DistribuicaoPosicaoCorretoraService valida Carteira com findById/ObjectNotFoundException e obtem todo historico daquela carteira por uma consulta dedicada de OperacaoRepository. EntityGraph carrega Acao e Corretora (inclusive associacao opcional) no mesmo SELECT. Nenhuma corretora ficticia ou consulta individual por nome. O nucleo resolve fallback e ordenacao ordinal ja aprovados e retorna records sem entidades gerenciadas.

## Consistencia efetiva

Fronteira publica via proxy Spring: REQUIRES_NEW, readOnly=true, SERIALIZABLE. A transacao propria evita herdar READ_COMMITTED ou entidades previamente carregadas de um chamador. Operacoes, ativos e nomes sao obtidos juntos; replay quantitativo e distribuicao usam exatamente a mesma lista. Nao ha chamada a PosicaoService ou comparacao entre snapshots independentes.

REPEATABLE_READ foi experimentado no teste H2 e REPROVADO: depois de ler Carteira, outra transacao alterou nome da corretora e inseriu uma operacao. O H2 retornou operacoes anteriores com nome posterior. Portanto readOnly ou REPEATABLE_READ isoladamente nao foram considerados prova suficiente. SERIALIZABLE passou nesse mesmo teste: resultado antigo coerente durante a consulta, resultado novo coerente na consulta seguinte.

Outro teste confirma pela conexao JDBC o isolamento SERIALIZABLE e pelo Spring o atributo read-only, mesmo dentro de um chamador READ_COMMITTED; confirma tambem a restauracao do isolamento do chamador. Read-only e uma configuracao de transacao, nao uma barreira universal contra escrita no H2: ausencia de efeitos tambem e verificada por contagem SQL e estatisticas de inserts/updates/deletes, e por revisao do codigo.

PostgreSQL: SERIALIZABLE e escolhido para a mesma fronteira, mas sua certificacao concorrente ainda nao foi executada. Falhas de serializacao devem seguir o tratamento tecnico seguro da futura fronteira HTTP; nao ha retry automatico ou saneamento. Transacao propria exige uma conexao adicional se houver chamador transacional; manter consulta curta e pool adequado.

## Testes executados

Comando final:

```powershell
mvn -o '-Dtest=DistribuicaoPosicaoCorretoraServiceIntegrationTest,CalculadoraDistribuicaoCorretoraTest,CalculadoraPosicaoTest,CalculadoraRentabilidadeTest,PosicaoServiceTest,ResultadoRealizadoServiceTest,OperacaoServiceTest,OperacaoOrchestrationTest' test
```

Resultado:96 testes,0 falhas,0 erros,0 ignorados;BUILD SUCCESS. Sao 7 novos testes H2 e 89 testes do nucleo/regressoes diretamente relacionadas.

Reexecucao final somente dos 7 testes H2 apos marcar a configuracao como TestConfiguration:7 aprovados,0 falhas/erros/ignorados. OpenSpec strict da change aprovado;git diff --check aprovado (apenas aviso LF/CRLF de arquivo frontend preexistente nao alterado nesta etapa). Verificacao explicita de whitespace/EOF inclui os arquivos novos,sem problemas. Graphify AST atualizado:10002 nodes,18128 edges,807 communities na ultima execucao;avisos conhecidos de seis arquivos sem nodes,parser SQL opcional ausente e rotulos comunitarios defasados,sem bloqueio funcional.

H2 em memoria exclusivo broker-distribution, contexto restrito a JPA/servico/calculadoras, Liquibase desabilitado e schema efemero create-drop pelo Hibernate (nenhuma migration). Dados gravados em transacoes e efetivamente recuperados pelo servico em outra transacao:

- Carteira inexistente: erro padronizado e somente um SELECT.
- Carteira vazia e posicao consolidada encerrada: colecao vazia.
- Multiplas corretoras/null, carteira distinta, negativos e grupo zero com primeira evidencia historica.
- N+1: exatamente dois statements (Carteira e historico com fetch) tanto com poucos grupos quanto com 12 corretoras; nomes ordinais e desempate por ID.
- Nenhum insert/update/delete na consulta medida.
- EUA:0.123456 e0.000001 persistidos; fotografias antiga0.123456 e nova0.123457 coerentes com nome antes/depois.
- Isolamento JDBC efetivo e suspensao/restauracao da transacao mais fraca do chamador.

Tentativas iniciais foram interrompidas por restricoes de loopback/attach do ambiente; reexecucao autorizada fora dessa restricao. Houve tambem ajuste da instrumentacao Mockito de repository proxy (metodo abstrato nao suporta callRealMethod); o teste instrumentado usa EntityManager.find para a leitura real de Carteira. Os demais testes usam o repository real, inclusive contagem SQL. Nenhuma mudanca financeira motivada por essas falhas de teste.

## Pendencias e tasks

29 tasks:8 concluidas,21 pendentes. Nesta etapa somente2.1,2.2,2.6 concluidas. 4.3 permanece aberta ate a verificacao do GET, apesar da evidencia parcial no servico. 4.6 permanece aberta: nao ha instancia PostgreSQL temporaria configurada nesta etapa; initdb/pg_ctl nao estao no PATH e o suporte temporal existente nao constitui fixture isolada deste servico sem migrations. Dependencia: disponibilizar cluster temporario dedicado com bootstrap descartavel e executar a mesma matriz de snapshot/consultas/precisao. Nunca substituir pelo PostgreSQL local.

HTTP/frontend/documentacao final/certificacao ampla continuam pendentes. Proximo passo recomendado: DTOs/endpoint separado e testes de contrato, integrando este servico por proxy Spring; concluir PostgreSQL temporario antes da certificacao final. Sem migrations, acesso ao PostgreSQL local ou operacoes Git de escrita. Experimentos preservados.
