# Plano de inventario PostgreSQL de referenciaData

Status: SQL validado em PostgreSQL temporario; EXECUCAO OFICIAL NAO AUTORIZADA/NEM REALIZADA. Nenhuma conexao ao banco oficial, acesso a credenciais, alteracao de configuracao oficial, migration ou producao nesta etapa. A nao foi incorporada, B descartada como definitiva, C nao projetada/implementada.

## Objetos confirmados no repositorio

- Acao.referenciaData: tabela `acao`, coluna nullable `referencia_data`, SQL DATE.
- HistoricoCotacao.referenciaData: tabela `historico_cotacao`, coluna nullable `referencia_data`, SQL DATE.
- Evidencia: anotacoes nas entidades e changelogs 002, 005 e 007. O schema oficial e a aplicacao efetiva dessas migrations nao foram verificados no servidor; nao presumir `public`. Q1 confirma objetos visiveis antes de substituir `__SCHEMA__` no arquivo SQL.
- 007 declara constraint/indice unico `uk_historico_cotacao_acao_data` sobre `(acao_id, referencia_data)` e outro por `(acao_id, referencia_instante)`. Acao possui unicidade ticker/mercado; nao ha indice dedicado a referencia_data declarado nesses changelogs. Q3 verifica indices reais posteriormente, sem criar nenhum.

## Consultas propostas

Arquivo: `inventario-referencia-data.sql`. Todas as instrucoes sao SELECT, sem SELECT INTO, funcoes de escrita, locks explicitos ou FOR UPDATE/SHARE.

| Consulta | Justificativa e resultado |
| --- | --- |
| Q1 | Confirmar exatamente as quatro colunas do schema escolhido: acao/historico_cotacao x referencia_data/referencia_tipo; expor tipos UDT e domains para revisao. |
| Q2 | Conferir current_database(), current_schema(), DateStyle, read-only, isolation e timeouts; nao configura a sessao. |
| Q3 | Estimar volume pelos catalogs, verificar indices e flags RLS; estimativas nao substituem contagens. |
| Q3b | Enumerar recursivamente raizes/descendentes, particoes/heranca, privilegios SELECT, flags RLS e quantidade de politicas; impedir assumir escopo completo sem essa revisao. |
| Q4 | Agregar total/nulas/preenchidas, min/max, faixas historicas, BC, outubro e intervalo ambiguo, contemporaneas e anomalias. Um statement fornece o mesmo snapshot MVCC para ambas as tabelas. |
| Q5 | Opcional: confirmar zero linhas em tabelas vazias, pois GROUP BY de Q4 nao emite linha para uma tabela vazia. |

BC usa `isfinite(referencia_data) AND referencia_data < DATE '0001-01-01'`. Foi comprovada equivalencia com `isfinite(referencia_data) AND referencia_data <= DATE '0001-12-31 BC'` em PostgreSQL 16.8 temporario. SQL nao possui ano zero; nao converter por Java, epoch, timestamp, timezone ou formula astronomica. MIN/MAX sao calculados como DATE e convertidos para texto somente na saida PostgreSQL; conservar a era BC e a representacao SQL, sem mapper da aplicacao. Se houver infinitos, min/max podem indica-los: isso e um alerta, nao ocultar ou sanear. As contagens anteriores_1970/anteriores_1582 incluem -infinity pela ordenacao SQL; BC, contemporaneas e ad_acima_9999 excluem infinitos explicitamente. Registrar DateStyle escolhido para apresentacao em eventual sessao, sem confundir texto com ordenacao.

Q4 pode omitir uma tabela sem registros: se ambas vazias, retorna zero linhas; se apenas uma vazia, retorna uma. Q5 sempre retorna duas linhas, com zero para cada tabela vazia. Esse comportamento foi validado para ambas vazias e para cada uma vazia separadamente; nao interpretar ausencia de grupo como erro de acesso ou como uma contagem completa sem Q5.

Contemporanea significa, apenas para este inventario, finita >=1970-01-01; ha tambem contagem >=2000. Nao e um piso financeiro de DATA nem validacao de futuro. Anteriores a 1582 significa <1582-01-01, nao todo o periodo anterior a reforma. Outubro usa intervalo fechado-aberto [1582-10-01,1582-11-01), com subcontagem [1582-10-05,1582-10-15). Faixas sobrepoem-se: nao somar. `total = nulas + preenchidas`; ambas as tabelas precisam ser avaliadas, pois historico pode conter valores ausentes na Acao atual.

Agregacoes bastam para a primeira decisao de escopo; nenhuma consulta retorna id, ticker, empresa, cotacao ou linhas individuais. Min/max ainda revelam limites temporais; manter o relatorio agregado com acesso restrito. Datas fora do dominio novo nao provam invalidade do registro legado.

## Execucao segura futura, condicionada a autorizacao

1. Operador autorizado confirma instancia oficial, schema e alcance do papel de leitura por procedimento externo; sem enviar credenciais ao agente. Q2 deve corresponder ao banco esperado, e current_schema nao substitui a confirmacao do schema das tabelas. Nenhuma conexao oficial criada neste plano.
2. Usar papel existente com CONNECT no banco, USAGE no schema e SELECT nas duas tabelas (ou somente colunas necessarias, permitindo count(*)). Sem superuser, BYPASSRLS, DML ou DDL; nao criar papel/objeto nesta tarefa. Confirmar com responsavel se RLS restringe linhas: contagens sob RLS podem nao representar todo o banco. Nao contornar RLS automaticamente.
3. Antes de executar o arquivo, abrir pelo cliente uma transacao curta READ ONLY, preferencialmente READ COMMITTED; configurar apenas limites da sessao pelo operador se aprovados. Sugestao inicial para aprovacao: lock_timeout 1s, statement_timeout 30s, idle_in_transaction_session_timeout 30s. Nao alterar configuracao persistente, papel ou banco. O arquivo permanece exclusivamente SELECT; a abertura/encerramento da transacao e responsabilidade do cliente, fora das consultas de inventario.
4. Q2 deve indicar read-only on e limites efetivos; abortar caso contrario. Read-only e defesa adicional, nao substitui revisao e privilegios restritos. Nao invocar funcoes customizadas ou extensoes. Q1/Q3/Q3b usam catalogs e builtins.
5. Executar Q2/Q1/Q3/Q3b antes de Q4; aprovar custo operacional e escopo. Q1 deve mostrar as quatro colunas no schema confirmado: referencia_data nativo DATE (udt_schema=pg_catalog, udt_name=date), referencia_tipo texto/varchar/char nativo compativel com 'DATA'. Domain, tipo customizado, coluna ausente, schemas ambiguos ou falta de visibilidade exigem revisao, nao cast automatico. Somente depois substituir __SCHEMA__ por quote_ident(nome_confirmado), e __SCHEMA_LITERAL__ por quote_literal(nome_confirmado), independentemente e por posicao/token; nunca substituir nome cru globalmente. Aspas duplas internas do identificador devem ser duplicadas; aspas simples do literal tambem. O SQL entregue e um template deliberadamente nao executavel ate esse passo, nao depende de search_path. Q4 em um unico statement evita divergencia entre snapshots de Acao/historico; Q5, se necessario, e posterior e pode observar alteracoes concorrentes. Encerrar transacao imediatamente, inclusive em erro/timeout; nao deixar snapshot aberto.
6. Preferir horario de menor carga, coordenar ausencia de DDL e nao executar durante migration. Timeout cancela, nao repetir em loop nem aumentar limites sem revisao. Nenhum EXPLAIN ANALYZE, ANALYZE, VACUUM, indice novo, temp table, COPY ou saneamento.

SELECT simples adquire ACCESS SHARE, sem bloquear INSERT/UPDATE/DELETE normais; nao e ausencia absoluta de locks. Conflita com ACCESS EXCLUSIVE de DDL/TRUNCATE e pode esperar por ele. Fila de DDL pode causar bloqueio indireto de writers: por isso lock_timeout curto e coordenacao operacional. Uma leitura longa tambem compete por CPU/I/O e retarda limpeza MVCC. Nao prometer impacto zero.

Referencias oficiais: [locks PostgreSQL 16](https://www.postgresql.org/docs/16/explicit-locking.html) e [transacoes read-only](https://www.postgresql.org/docs/16/sql-set-transaction.html).

## Custo e indices

Q1/Q3 sao consultas pequenas a metadados. Q4 tem custo O(N_acao + N_historico), uma passagem logica por tabela, agregacao sem ordenar registros individuais e resultado de ate duas linhas. Count exato precisa examinar linhas/entradas visiveis; o planner pode escolher sequential scan ou index-only scan conforme cobertura e visibility map. Nao ha garantia de uso de indice. O indice historico com acao_id primeiro nao e um indice eficiente dedicado a faixa global de DATA; pode servir como cobertura para certas consultas, mas Q4 tambem necessita referencia_tipo. Nao criar indice para este inventario.

Q3b deve confirmar relkind das raizes e descendentes. SELECT sem ONLY inclui heranca/particoes; nao somar manualmente contagens das filhas as raizes, nem somar ambas as raizes caso compartilhem descendentes. Multiplos pais, raizes ancestrais entre si, foreign tables/foreign partitions (relkind=f), views/materialized views, descendentes em outros schemas ou escopo inesperado exigem pausa antes de Q4, especialmente para evitar acesso remoto nao aprovado. reltuples/relpages da raiz particionada nao representam necessariamente o volume total; reavaliar custo dos descendentes sem criar objetos.

RLS pode filtrar silenciosamente resultados inclusive Q5. Q3b informa row_security_active para o papel corrente, flags e numero de politicas; nao certifica que a politica permite todos os registros. Politica da raiz governa leitura via raiz, podendo filtrar descendentes mesmo quando eles indicam RLS inativo; acesso direto ao filho pode ter outra politica. Privilegio SELECT no filho nao prova leitura completa da raiz. Se RLS estiver habilitada/ativa ou houver politica relevante, obter confirmacao do responsavel sobre alcance; nao desabilitar RLS, nao usar BYPASSRLS/superuser e nao declarar inventario completo automaticamente.

Sem volumes, estatisticas e hardware oficiais nao e honesto estimar segundos ou bytes de I/O. reltuples/relpages de Q3 sao aproximados e podem estar desatualizados; particoes/heranca podem exigir revisao de escopo e estimativas antes da execucao. Q5 repete scans apenas se necessario. Min/max sobre todas as linhas estao incluidos na mesma agregacao, nao exigem consultas individuais por faixa.

## Interpretacao e decisao A versus C

- Nenhum registro preenchido em ambas as tabelas reduz radicalmente o problema de leitura legada deste campo. Ainda exige snapshot completo, ausencia de RLS ocultando linhas e protecao contra novas gravacoes pelo caminho antigo entre inventario e rollout/restauracao/importacao futura.
- Zero BC e zero intervalo ambiguo demonstra apenas ausencia dessas faixas visiveis naquele snapshot. Pode permitir dispensar versionamento para os dados presentes se demais valores forem cobertos pela equivalencia de leitura demonstrada e se a procedencia/possiveis outros codecs forem avaliados separadamente. Nao e prova de procedencia.
- BC presente: A preservou o caminho antigo testado, inclusive erros preexistentes. O inventario nao prova que todo BC veio desse caminho nem que todos sejam legiveis. Nao aplicar conversao automatica ou saneamento.
- AD 5-14/10/1582 presente: ha divergencia OLD/direto sobre o mesmo SQL DATE. A simples estrategia BC de A nao garante preservacao dessas linhas. Considerar C ou uma decisao explicita sobre interpretacao; nenhuma esta autorizada.
- SQL 15-24/10/1582 pode representar uma entrada antiga normalizada ou entrada civil legitima. Outubro quantifica a exposicao, mas o inventario nao recupera entrada original e nao permite subtrair dias. C tambem nao recupera informacao ja perdida.
- Valores acima de 9999, infinitos ou inconsistencias tipo/data exigem analise separada, sem aplicar automaticamente o novo dominio de aquisicao na leitura.
- Mesmo datas contemporaneas nao provam produtor: SQL manual, importacoes, scripts, versoes antigas e JDBC distintos devem ser investigados por evidencias de processo, nao inferidos de faixas. Se interpretacoes legadas distintas precisarem coexistir para SQL iguais e origem nao puder ser certificada, considerar versionamento/metadados C; inventario sozinho nao os fabrica.

Condicoes para eventualmente aprovar A: resultado completo do inventario; nenhum caso presente conhecido reinterpretado por A, ou premissa de origem explicitamente comprovada/aceita que cubra os existentes; politica aprovada de preservar erros BC; rollout coordenado sem leitores/escritores antigos para novas datas civis; aprovacao expressa da incorporacao e regressao. Ausencia de historicos afetados pode eliminar a necessidade tecnica de versionamento para o conjunto atual, mas nao automaticamente nem como garantia para dados externos/restaurados futuros.

Condicoes para considerar C: linhas que exigem leitura legada e civil sobre o mesmo DATE; procedencia nao comprovavel com divergencia de interpretacao; exigencia de garantia por observacao para importacoes/coexistencia futura. C ainda demanda autorizacao de projeto e eventual migration aditiva; nao alterar 007.

## Validacao temporaria em 2026-10-10

PostgreSQL 16.8 em target/jdbc-bc-poc-pg, loopback porta 55452; SHOW data_directory confirmou esse diretorio antes das fixtures. Nenhuma migration executada: apenas tabelas isoladas sinteticas com DATE/VARCHAR(12), heranca, particoes e politica de teste. Setup/teardown usam DDL/DML somente nesse cluster descartavel; o arquivo de inventario continua exclusivamente SELECT. Todas as consultas foram executadas sob papel NOLOGIN restrito com USAGE/SELECT e BEGIN READ ONLY, timeouts 30s/1s, isolamento READ COMMITTED, DateStyle ISO/YMD. Schema com espaco e aspas (`Inventario "civil`) validou quoting independente de identificador e literal. As quatro colunas foram retornadas como pg_catalog.date e pg_catalog.varchar, sem domains.

Evidencias: target/inventario-validation.log e target/inventario-scope-validation.log; ambos psql ON_ERROR_STOP com exit 0. BC: zero divergencias nos 43 valores da matriz e em 2.003 casos extras (2.000 dias consecutivos cruzando BC/AD, NULL e ambos infinitos). A equivalencia tambem segue a ordenacao discreta de DATE: o sucessor de 0001-12-31 BC e 0001-01-01 AD; infinitos sao excluidos nas duas condicoes.

| Cenario | Q4 | Q5 |
| --- | --- | --- |
| Ambas vazias | zero linhas | acao=0, historico=0 |
| Ambas preenchidas | duas linhas, cada total=43 | 43/43 |
| Apenas Acao vazia | somente historico, total=43 | 0/43 |
| Apenas historico vazio | somente acao, total=43 | 43/0 |

Matriz por tabela: total 43, nulas 1, preenchidas 42, BC 3, outubro1582 31, intervalo5-14 10, contemporaneas>=1970 3, >=2000 3, AD>9999 1, infinitos 2; min=-infinity/max=infinity; anteriores1970=38 e anteriores1582=7 (incluem -infinity). Contem 1 BC janeiro/dezembro, 2 BC, 1 AD, bissexto 0004, 1500, 2000, 2026, ano10000 e todos os dias de outubro1582. Incoerencias tipo/data=0.

Heranca/particoes: Q3b enumerou cinco relacoes (duas raizes, filho herdado e duas particoes); Q4 incluiu o filho herdado e ambas as particoes, totais 2/2. Com politica RLS ativa na raiz Acao, Q3b detectou politica e atividade; Q4/Q5 passaram a mostrar apenas 1 Acao, apesar das 2 linhas fisicas incluindo filho sem RLS. Isso comprova por que contagem restrita nao deve ser declarada completa. Historico permaneceu total2, min=0001-01-01 BC/max=2026-10-02.

Schemas/papel de teste removidos e cluster temporario encerrado apos validacao. Nenhum resultado descreve o banco oficial; nele colunas, particoes, heranca, RLS e volume permanecem nao verificados. Somente SQL e este plano foram atualizados; tasks e producao intactos.

OpenSpec strict da change aprovado, git diff --check exit 0, SQL/plano sem whitespace final. Hashes dos arquivos de producao e tasks confirmados inalterados. Nenhum codigo modificado, portanto nenhuma regeneracao Graphify ou suite backend/frontend necessaria nesta revisao documental/SQL.

## Pendencias e estado

Proxima decisao: aprovar ou ajustar as consultas, alcance/schema, papel somente leitura e janela/timeouts de execucao. Depois autorizar explicitamente a execucao oficial por operador/agente definido. Somente apos os resultados decidir A versus estudo de C; nenhuma alternativa esta aprovada para producao.

Riscos remanescentes: visibilidade parcial/RLS, dados concorrentes apos snapshot, caminhos de escrita nao documentados, limite da matriz de testes anterior, leituras legadas que ja falham, efeitos de carga e DDL. Nenhuma task ratificada: 40 totais, 27 concluidas, 13 pendentes; 9.1 e 12.3 abertas, 12.1 pendente. Arquivos criados nesta etapa: este plano e SQL de inventario; nenhum codigo de producao alterado.
