# Etapa D — evidência PostgreSQL da task 11.2

Validação em 2026-10-04. PostgreSQL 16.8, 64 bits, compilado por Visual C++ 1942; driver PostgreSQL JDBC utilizado pelo projeto, Hibernate PostgreSQLDialect, isolamento READ_COMMITTED. Nenhuma produção alterada nesta etapa: hashes de todos os arquivos em src/main idênticos antes/depois. Somente testes e evidências adicionados.

## Ambiente e encerramento

Instalação nativa: `C:\Program Files\PostgreSQL\16\bin`. Cluster criado do zero em `target/pg16-etapa-d-89d028a94cf74040bbebabd1d3f39f13`, porta livre alocada para esta execução: 62423. Escuta exclusivamente em 127.0.0.1. Role descartável codex_etapa_d, trust local e senha vazia; nenhuma credencial real registrada. Não foi consultada configuração do banco de desenvolvimento nem conectado outro cluster. Docker não utilizado.

Comandos de inicialização, com caminhos absolutos correspondentes ao diretório acima:

```powershell
& 'C:\Program Files\PostgreSQL\16\bin\initdb.exe' -D $taskDir -U codex_etapa_d -A trust --encoding=UTF8 --locale=C
& 'C:\Program Files\PostgreSQL\16\bin\pg_ctl.exe' -D $taskDir -l (Join-Path $taskDir 'server.log') -o '-h 127.0.0.1 -p 62423' -w start
```

Manifesto local ignorado: target/etapa-d-pg.json. O sandbox impediu o início com token restrito; pg_ctl autorizado iniciou somente o novo cluster. Ao final, o sinal de parada restrito também foi impedido. Encerramento autorizado com `pg_ctl kill TERM 17836`, após validar o PID, executável e CommandLine contendo exatamente o diretório temporário; verificada a ausência do processo do cluster e removido seu diretório. Evidência: target/etapa-d-cleanup.txt (`STOPPED verified_owned_pid=17836 directory_removed=True`). Logs locais preservados fora do diretório removido. Nenhum serviço/cluster de desenvolvimento parado ou alterado.

## Execução e resultados

Em cluster descartável limpo, com URL explícita da execução:

```powershell
mvn -q '-Dtest=ReferenciaTemporalMigrationTest,AplicacaoCotacaoPostgresqlTest,AtualizacaoCotacoesCarteiraPostgresqlTest,AcaoCadastroTemporalIntegrationTest,AcaoCotacaoHttpIntegrationTest,AtualizacaoCotacoesCarteiraHttpIntegrationTest,HistoricoCotacaoAtomicityTest' '-Dtemporal.test.postgresql.enabled=true' '-Dtemporal.test.postgresql.url=jdbc:postgresql://127.0.0.1:62423/postgres' '-Dtemporal.test.postgresql.user=codex_etapa_d' '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:62423/postgres' '-Dspring.datasource.username=codex_etapa_d' '-Dspring.datasource.password=' '-Dspring.datasource.driver-class-name=org.postgresql.Driver' test
```

| Classe | Casos | Banco |
|---|---:|---|
| ReferenciaTemporalMigrationTest | 6 | 3 PostgreSQL + 3 H2 |
| AplicacaoCotacaoPostgresqlTest | 44 | PostgreSQL |
| AtualizacaoCotacoesCarteiraPostgresqlTest | 31 | PostgreSQL |
| AcaoCadastroTemporalIntegrationTest | 10 | PostgreSQL |
| AcaoCotacaoHttpIntegrationTest | 5 | PostgreSQL |
| AtualizacaoCotacoesCarteiraHttpIntegrationTest | 7 | PostgreSQL |
| HistoricoCotacaoAtomicityTest | 2 | PostgreSQL |

105 testes aprovados, zero falhas/erros/skips; 102 realmente em PostgreSQL. Testes específicos opt-in recusam URL sem loopback explícito/role descartável e conferem versão 16 e current_user. As duas classes herdam os cenários já aprovados em H2, sem removê-los; o helper de contenção H2 é substituído por observação PostgreSQL. Logs: target/etapa-d-postgresql-final.log. XMLs copiados antes da regressão: target/etapa-d-reports-pg. Primeira elaboração do conjunto ampliado falhou somente em duas comparações de escala do fixture legado; fixture ajustado a 10.000000. Conjunto final integral reexecutado e aprovado.

Regressão H2 2.4.240:

```powershell
mvn -q '-Dtest=AplicacaoCotacaoServiceTest,AtualizacaoCotacoesCarteiraServiceTest,CotacaoPrecisaoTemporalTest,AcaoCadastroTemporalIntegrationTest,AcaoCotacaoHttpIntegrationTest,AtualizacaoCotacoesCarteiraHttpIntegrationTest' test
```

81 testes aprovados (24 + 24 + 11 + 10 + 5 + 7), zero falhas/erros/skips. Log: target/etapa-d-h2-regressao.log. Suíte completa não executada.

## Matriz de evidências

| Exigência | Evidência verificada |
|---|---|
| Migration real | Liquibase master 001–007 em schema limpo; Hibernate validate, 7 changeSets; teste dedicado repete cadeia em schemas isolados e os remove. |
| Contrato físico | TIMESTAMP WITH TIME ZONE, precisão 6, DATE, VARCHAR(12) NOT NULL/default DESCONHECIDA, NUMERIC(19,6), checks e FK obrigatória sem cascade. Consulta física em target/etapa-d-physical-evidence.txt. |
| Legado/NULL | Migration mantém preço/legado, referências/obtenção nulas, sem backfill; múltiplas referências nulas permitidas. Leitura JPA DESCONHECIDA com obtenção nula ou conhecida. |
| Rollback 007 | Rollback estrutural com legado conserva dados/restaura unicidade antiga; caso de colisões do legado falha antes de remover metadados e conserva dados. Não é promessa universal; rollback bem-sucedido perde as novas informações, como previsto. |
| Precisão | SQL direto, JPA relida e histórico iguais à entrada para segundos/.1/.123456/.123456000/.999999000. Offset -03:00 mantém instante UTC; zeros de formatação podem desaparecer, sem perder valor. |
| Precisão inválida | .123456001/.123456789/.999999001 rejeitados pela construção da candidata antes de chamar repository/núcleo; HTTP de cadastro confirma referência/Clock inválidos sem persistência. PostgreSQL não corrige entradas da aplicação. |
| DATA | Baseline inicial e referência posterior; somente DATE, instante nulo, obtenção exata fracionária própria, inclusive anterior à obtenção substituída; dois históricos corretos. |
| INSTANTE | Baseline, posteriores, preço igual, offsets, obtenção própria, históricos correspondentes e unicidade real por Ação/instante. |
| Atomicidade | Falha real no UPDATE por CHECK (23514), falha de gravação, falha após INSERT/flush, violações reais DATA/INSTANTE e beforeCommit. Leitura em nova transação confere preço/tipo/duas referências/obtenção/legado, histórico anterior completo e nenhum residual. |
| Unicidade residual | DataIntegrityViolationException com causa raiz org.postgresql.util.PSQLException, SQLSTATE 23505, uk_historico_cotacao_acao_data ou uk_historico_cotacao_acao_instante. Uma tentativa, sem retry/provider adicional/remoção de histórico/conversão semântica. |
| FK real | INSERT órfão e DELETE de Ação com histórico rejeitados com SQLSTATE 23503; observação existente intacta. |
| Mesma Ação | Latches mantêm primeira transação; pg_blocking_pids e pg_stat_activity comprovam segunda esperando Lock/transactionid na sessão da primeira, Future pendente antes da liberação. Sem Thread.sleep. |
| Reread | Segunda transação carrega entidade antiga deliberadamente antes de bloquear; após liberação, refresh protegido descarta o estado stale. Posterior/posterior nas duas ordens, igualdade, conflito de preço e tipos incompatíveis preservam decisões/histórico. |
| Ações distintas | Enquanto A permanece bloqueada, aplicação de B conclui; nenhum lock global serializa indevidamente B. |
| Lote × PATCH/lote | Instâncias de AcaoService independentes disputam o lock real, com sessões observadas. Segunda usa estado relido e aceita sua referência posterior/obtenção própria anterior. Também repetidos cenários de coordenação local, resposta antiga tardia e igualdade. |
| Lote | Sequencial, commit antes do próximo provider, ausência de transação global, ordem/deduplicação/replay, rollback de uma Ação sem desfazer outras. Snapshot completo pós-falha/rollback validado, incluindo EntityManager previamente stale. |
| Cooldown/reuso | Precedência conservada; snapshot imutável do cooldown da tentativa, sem nova aplicação/histórico; restart preserva reuso. Conhecida com obtenção válida/legado antigo reutiliza; desconhecida e obtenção expirada/legado recente consultam; um microssegundo dentro da janela reutiliza, limite exato consulta. |

Evidências de lock da execução final incluem blocker=11772/blocked=11628 (núcleo) e blocker=15704/blocked=31780 (lote × PATCH/lote), `wait_event_type=Lock`, `wait_event=transactionid`. Hibernate gera `FOR NO KEY UPDATE OF a1_0` no PostgreSQL para PESSIMISTIC_WRITE; comprovação é pelo bloqueio real, não pela grafia SQL usada no H2.

Nenhuma diferença funcional incompatível encontrada entre H2 e PostgreSQL. Diferenças técnicas observadas: consulta diagnóstica de locks/dialeto SQL e PSQLException/SQLSTATE nos erros. Não houve lost update, histórico duplicado indevido ou rollback de outra Ação. Falhas SQL e falha global registradas nos logs são cenários negativos intencionais.

## Conclusão e limites

Task 11.2 integralmente satisfeita: migration, tipos/constraints, persistência/histórico, precisão, rollback, atomicidade e locks com PostgreSQL real. Somente 11.2 marcada; 40 tasks, 22 concluídas, 18 pendentes. Nenhuma alteração de produção, contrato HTTP, frontend ou migration. Nenhum bloqueante PostgreSQL remanescente ou nova decisão para aprovação. Recomendável operacional: reutilizar estes testes opt-in em execução automatizada com cluster 16 descartável; não condiciona o resultado desta validação.

O cenário de conclusão transacional injeta SQL inválido em beforeCommit após INSERT/flush; não pretende simular interrupção de rede durante COMMIT físico. A certificação abrange PostgreSQL 16.8 utilizado, sem alegar outros bancos/versões. Validações finais/Graphify e estado Git registrados em tasks.md e artefatos locais da Etapa D. Sem staging, commit, push, merge ou archive.
