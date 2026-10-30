# Evidências de conclusão — homologações 1.9/1.10 e Etapa 3

**Acompanhamento atualizado em 29/10/2026:** 58 tarefas concluídas e duas desmarcadas (5.10 e 6.5 parciais com exceções aceitas). A auditoria 6.9 e a validação final 6.10 estão concluídas e aprovadas tecnicamente. O registro de 6.10 está ao final; as afirmações e métricas das rodadas anteriores permanecem históricas, sem substituição de seus resultados. Consulte [excecoes-homologacao.md](excecoes-homologacao.md) e [validacao-final.md](validacao-final.md).

Estado atual em **25/10/2026**: concluídas 1.9, 1.10 e 3.1–3.10, total de **12 tarefas concluídas e 48 desmarcadas**, conforme [tasks.md](tasks.md). Desmarcação não significa ausência de implementação. O fechamento da Etapa 3, sua rastreabilidade e a validação global atual estão registrados ao final deste documento; não encerram outras etapas nem autorizam novas implementações, execuções ou publicação Git.

**Registro histórico de 24/10/2026:** naquele fechamento documental da change `adicionar-movimentacoes-custodia-entre-corretoras`, somente 1.9 e 1.10 foram encerradas, permanecendo 58 tarefas desmarcadas. As seções seguintes preservam as evidências e os limites daquele registro. Afirmações de escopo e pendências daquela data não substituem o fechamento posterior da Etapa 3 abaixo.

## Ambiente homologado

Os dois logs PostgreSQL reais identificam:
- PostgreSQL **17.11**, versão numérica `170011`.
- Imagem `docker.io/library/postgres:17.11-alpine3.24`.
- Digest `sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb`.
- Plataforma `linux/amd64`.
- Provisionamento isolado e recursos descartáveis pelos componentes `CustodiaPostgres*`.

## Tarefa 1.9 — PostgreSQL real

- Classe executora: `com.projeto.custodia.postgresql.CustodiaPostgresHomologacao19RealTest`.
- Execução em **24/10/2026**, encerrada às **00:17:39 -03:00** (America/Sao_Paulo), conforme o log.
- Log relativo à raiz do repositório: `target/custodia-19-real-20261024-001523.log`.
- Maven `BUILD SUCCESS`: um teste JUnit executado, contendo os dois cenários; zero falhas, erros ou skips. `ExitCode=0` confirmado pelo usuário.
- Instalação limpa 001–008 aprovada. Upgrade 001–007 para 008 aprovado: sete changeSets anteriores e dois changeSets da 008, totalizando **nove changeSets** reconhecidos.
- Preservação dos **16 registros legados** verificada no upgrade, com comparação de dados, estrutura e histórico. Hash do retrato registrado pelo executor: `734374e0985a55676fa0890997a2dc4d7ad9eeba1a399045134799c230ac18e0`.
- Aprovados nos dois cenários: schema, Hibernate `validate`, persistência BR/EUA, repositórios, ponte de replay quantitativo, integridade, índices, idempotência e rollback protegido.
- Índice integrado: **32.768 bytes em cada cenário**.
- O rollback protegido exige a interrupção contratual por `StopChangeException`, seguida da verificação de preservação de auditoria, schema e histórico e liberação do lock. As mensagens dessa interrupção esperada não representam falha da homologação aprovada.

| Cenário | UUID | Relatório final |
| --- | --- | --- |
| INSTALACAO | `47195a59-bb7a-47ea-9618-5348ca7d28ff` | `completo=true; limpo=true; pendentes={}; incertos=0` |
| UPGRADE | `e5756038-050d-42c6-a8db-09e05e3e1734` | `completo=true; limpo=true; pendentes={}; incertos=0` |

**Inspeção independente:** o usuário realizou inspeção Docker após a execução e confirmou ausência de containers, volumes e redes associados a ambos os UUIDs. Essa confirmação externa é registrada conforme o relato do usuário; nenhuma inspeção Docker foi repetida neste fechamento documental.

## Tarefa 1.9 — evidência H2 existente

Fonte: `target/custodia-19-final.log`. São **114 testes H2 aprovados**, distribuídos assim:

| Classe | Testes aprovados |
| --- | ---: |
| `CustodiaIntegridadeH2Test` | 68 |
| `CustodiaMigrationH2Test` | 1 |
| `CustodiaPersistenceH2Test` | 13 |
| `CustodiaPontePersistenciaH2Test` | 10 |
| `CustodiaRepositoriesH2Test` | 21 |
| `CustodiaUpgradeRollbackH2Test` | 1 |
| **Total H2** | **114** |

Esses testes verificam instalação/upgrade/rollback, persistência, integridade, repositórios e ponte de replay em H2 isolado. São evidências distintas da homologação PostgreSQL real acima. O log completo dessa rodada contém 282 casos no total, com zero falhas/erros e um teste real opt-in ignorado; apenas os 114 casos da tabela são contabilizados como H2.

## Tarefa 1.10 — capacidade PostgreSQL real

- Classe executora: `com.projeto.custodia.postgresql.CustodiaPostgresCapacidadeRealTest`.
- Execução em **23/10/2026**, encerrada às **22:29:19 -03:00** (America/Sao_Paulo), antes da homologação final de 1.9.
- Log relativo à raiz do repositório: `target/custodia-110-real.log`.
- UUID: `189651d9-7312-4546-9e08-1b71a039afec`.
- Mesma imagem, digest e arquitetura identificados na seção de ambiente homologado.
- Maven `BUILD SUCCESS`: um teste executado, zero falhas, erros ou skips.
- **Oito amostras de componentes de 512 dígitos**, com seed `11017`.
- **2.467 roundtrips** e **2.449 posições adversariais**, com crescimento por medianta alternada até o limite explícito.
- Índice composto de seis colunas, medido em **2.154.496 bytes**; bloco PostgreSQL de 8.192 bytes.
- Provas concluídas: comparação racional matemática, roundtrip de 512 dígitos, chave composta, equivalência não canônica, formato, crescimento adversarial, índice e duplicidade.
- Relatório: `etapa=CONCLUIDO; experimentoCompleto=true; limpezaCompleta=true; recursosPendentes={}; criacoesIncertas=0; recursosRemovidos=true`.
- Limite de 512 dígitos e representação racional textual preservados. As evidências preliminares H2 2.4.240/PostgreSQL 16.8 descritas no planejamento permanecem como histórico; este fechamento utiliza o resultado real PostgreSQL 17.11 acima.

## Integridade e conservação das evidências

Hashes **SHA-256 calculados sobre os bytes dos arquivos locais disponíveis neste fechamento**, sem reexecutar testes ou homologações. Todos os caminhos abaixo são relativos à raiz do repositório.

| Arquivo local | SHA-256 |
| --- | --- |
| `target/custodia-19-real-20261024-001523.log` | `a8936e5a906b010a6cd132fa7587b2e416aaa3e2e11d27e33a689660f38c554a` |
| `target/custodia-110-real.log` | `d1282949881efb20dbb692d871cf6e6a0c30459502c2260578c32602721ec96c` |
| `target/custodia-19-final.log` | `2859cedff90c1a5595e841f074f600328eaa3ddcd51b294b23f1a4c500492554` |
| `target/custodia-19-verify.log` | `e767f4b5f3a34fc7712df45cebc733b186c4cdef85fc11c4a305162c380c0203` |
| `target/custodia-19-real-20261023-231253.log` | `8704581a3aee8a0faf1e3f031d9174349291262f509f7a9715734135feb6d103` |
| `target/custodia-19-real-20261023-234042.log` | `b2def069ec70a1335f5edfd8aa746f27c41816085e21870d34eb1f8d300673d2` |
| `target/custodia-temporal-final.log` | `6a88970923bddd50f9af17662c98f950b9ccec636b21cdea97d5a743ae8a1e7c` |

`target/` e os logs `.log` são ignorados pelo Git. Este documento registra o resumo durável e os hashes; não incorpora os arquivos brutos. Os logs precisam ser conservados separadamente antes de limpeza de `target/`. O SHA-256 permite conferir os mesmos bytes, mas não substitui a conservação do artefato nem constitui nova execução.

## Relatórios preparatórios, tentativas anteriores e pendências

- `target/custodia-110-relatorio.md` e `target/custodia-19-relatorio.md` documentam a preparação dos executores. Suas afirmações de homologação ainda não executada referem-se àquele momento; os resultados finais estão nas seções anteriores.
- `target/custodia-19-real-20261023-231253.log`: tentativa anterior interrompida na integridade, antes da correção da expectativa do caso de natureza inválida.
- `target/custodia-19-real-20261023-234042.log`: instalação aprovada e preparação do upgrade interrompida antes da correção de precisão temporal dos volumes.
- `target/custodia-temporal-final.log`: regressão local de **221 testes puros/simulados aprovados**, após a correção temporal; não é homologação PostgreSQL.
- `target/custodia-19-verify.log`: o verify global anterior permanece reprovado, com 1.864 testes, zero falhas de asserção, 363 erros e 101 skips. O diagnóstico existente identifica falha de conexão loopback na criação de clientes HTTP e erros subsequentes de contexto. Esse portão global não é declarado aprovado nem foi repetido neste fechamento.
- Escopo do fechamento de 24/10/2026: as homologações 1.9/1.10 encerraram seus critérios específicos; integração de serviços/transações, APIs, frontend, concorrência integrada e validações finais permaneciam sujeitas às tarefas correspondentes e a autorização específica. A marcação das demais tarefas não foi revisada naquele fechamento. A atualização de 25/10/2026 abaixo encerra somente a Etapa 3, sem alterar o resultado das execuções antigas.

## Limpeza operacional da tentativa anterior

O volume descartável `custodia-pg17-data-b181ca5b-c801-4eea-b74c-e31b84f2122e` foi **removido manualmente**, após inspeção de identidade, propriedade e ausência de containers utilizando o recurso. A remoção foi confirmada pelo Docker e pelo usuário, conforme confirmação fornecida nesta solicitação de fechamento.

Esse volume **não é pendência atual**. Não foi fornecida uma data/hora específica da remoção ou um artefato local dessa inspeção para cálculo de hash; tais evidências não são inventadas. Nenhuma remoção ou comando Docker foi executado nesta atualização documental.

## Fechamento técnico e documental da Etapa 3 — 25/10/2026

Critérios 3.1–3.10 conferidos individualmente contra os serviços, testes e relatórios existentes. Conclusão autorizada após os Incrementos 1–6, homologação PostgreSQL real 3.10 e validação global do backend aprovada. Neste fechamento não foram executados Maven, testes, Docker ou PostgreSQL; as evidências são das execuções anteriores identificadas abaixo. Somente documentação da change foi atualizada; nenhum critério original das tarefas foi reescrito.

### Rastreabilidade por tarefa

Os nomes de serviços e testes nesta matriz correspondem aos arquivos existentes em `src/main/java/com/projeto/` e `src/test/java/com/projeto/`. As evidências H2 e PostgreSQL possuem escopos distintos e são complementadas pelo portão global.

| Tarefa concluída | Implementação conferida | Testes e evidências que encerram o critério |
| --- | --- | --- |
| 3.1 | `TransferenciaCustodiaService`, comando imutável, `ValidacaoEntradaCustodia` e `TransferenciaCustodiaPersistenceService.persistir/registrar`: referências reais, origem/destino distintos, Clock por mercado, histórico integral e posição devolvida pelo núcleo sob lock. | `TransferenciaCustodiaServiceTest`, `TransferenciaCustodiaTransacaoH2Test`; transferência/transferência, transferência/venda e carteiras independentes na matriz PostgreSQL; regressão global aprovada. |
| 3.2 | `AtribuicaoCustodiaService` e `persistirAtribuicao`: origem nula, destino existente, uma movimentação ATRIBUICAO, âncora/fração do núcleo e compra original inalterada. | `AtribuicaoCustodiaServiceTest`, `AtribuicaoCustodiaTransacaoH2Test`: parcial/integral, grupo nulo, precisão BR/EUA e preservação financeira; atribuição/venda nula no PostgreSQL. |
| 3.3 | `EstornoCustodiaService` e `persistirEstorno`: consulta original/vínculo após lock, motivo opcional, neutralização histórica, original preservada, vínculo único, sem transferência inversa, cascata ou substituição. | `EstornoCustodiaServiceTest`, `EstornoCustodiaTransacaoH2Test`; estorno/venda, estorno/transferência e dois estornos concorrentes na matriz PostgreSQL. |
| 3.4 | `OperacaoPersistenceService.persistir`: validação consolidada preservada, saldo local da venda por corretora/grupo nulo e comparação diferencial, inclusive sem custódias. | `OperacaoCustodiaTransacaoH2Test`, `OperacaoCustodiaEntradaTest`, testes financeiros existentes e `OperacaoResourceTest`; venda/transferência e venda/atribuição no PostgreSQL. |
| 3.5 | Mesmo writer financeiro e `HistoricoQuantitativoCustodia`: fontes completas, incluindo eventos posteriores; proteção das custódias e legado; MAX(ordemNoDia)+1 e fórmulas preservados. | Casos de compra/retroatividade, legado independente e custódia posterior em `OperacaoCustodiaTransacaoH2Test`; regressões da calculadora financeira e testes HTTP na suíte global. |
| 3.6 | Writers de custódia, operação e exclusão: REQUIRED, READ_COMMITTED, escrita habilitada, recusa de contexto incompatível, carteira como primeiro acesso de negócio, histórico na própria transação e rollback. Registro usa saveAndFlush; exclusão usa delete com flush transacional, sem consulta pública REQUIRES_NEW para candidata. | Testes H2 de transação própria/externa, readOnly/isolation, lock, flush e beforeCommit; PostgreSQL comprova isolamento físico, espera/releitura e rejeição de COMMIT. |
| 3.7 | `FalhasEscritaOperacao` nas fachadas; constraint específica de estorno prioritária; `CarteiraExclusaoPersistenceService` mantém operações → snapshots → originais/estornos, inclusive estornados, sem cascade. | `CustodiaConflitosTransacionaisH2Test`, `CarteiraCustodiaExclusaoH2Test`, testes de handler; timeout55P03, estorno único e registro/exclusão no PostgreSQL. |
| 3.8 | Cobertura integrada dos registros, atribuições, estornos e novas vendas; grupo cadastrado/nulo, isolamento, legado independente e proteção histórica. | Classes H2 dos Incrementos 1–6 e testes puros dos serviços/núcleo existentes, incluídos na seleção global; não se deduz percentual de cobertura da quantidade de casos. |
| 3.9 | Falhas propagadas para rollback, erros de referências preservados, ausência de providers/retries e tradução fora do proxy quando observável. `StandardError` compatível com detalhes nulos, mantendo cópia defensiva e imutabilidade. | Flush/beforeCommit em H2; COMMIT rejeitado no servidor nas três fronteiras; referências e duplicidade; `StandardErrorTest`, `ResourceExceptionHandlerTest`, `ApiExceptionDetailsTest` e método HTTP original antes reprovado agora aprovado. |
| 3.10 | `CustodiaPostgresConcorrenciaRealTest`, contexto restrito com configuração explícita, coordenador e infraestrutura descartável `CustodiaPostgres*` existente. | 26 cenários reais aprovados no PostgreSQL 17.11, UUID e XML preservado abaixo; limpeza do executor concluída e ausência de recursos confirmada independentemente pelo usuário. |

### Evidências H2 e regressões direcionadas

As seguintes classes estão aprovadas na execução global final. São evidências transacionais H2; não substituem a homologação PostgreSQL.

| Classe | Casos aprovados |
| --- | ---: |
| `TransferenciaCustodiaTransacaoH2Test` | 26 |
| `AtribuicaoCustodiaTransacaoH2Test` | 23 |
| `EstornoCustodiaTransacaoH2Test` | 33 |
| `OperacaoCustodiaTransacaoH2Test` | 38 |
| `CarteiraCustodiaExclusaoH2Test` | 20 |
| `CustodiaConflitosTransacionaisH2Test` | 46 |

Fontes de acompanhamento: `target/custodia-etapa3-relatorio.md`, `target/custodia-incremento2-relatorio.md` até `target/custodia-incremento6-relatorio.md`. As seleções históricas de 473, 515, 555, 622, 654 e 467 casos pertencem a escopos incrementais distintos; não são somadas como testes únicos nem apresentadas como a validação global atual.

### Homologação PostgreSQL real 3.10

- Data: **25/10/2026**. PostgreSQL **17.11**, imagem/digest/plataforma do ambiente homologado acima, com banco e recursos exclusivos da execução.
- Classe: `com.projeto.custodia.postgresql.CustodiaPostgresConcorrenciaRealTest`.
- UUID: **`85ba577f-97ef-4eac-b157-02ee72688f06`**.
- Evidência real preservada: `target/validacao-etapa3-20261025-183918-891a0b03/antes/surefire-reports/TEST-com.projeto.custodia.postgresql.CustodiaPostgresConcorrenciaRealTest.xml`.
- Esse XML registra **um teste JUnit, zero falhas, erros ou skips**, contendo **26 cenários internos**: 18 casos nas duas ordens, cinco timeouts e três rejeições de COMMIT. BUILD SUCCESS e aprovação da execução real confirmados pelo usuário. O XML global atual, no qual essa classe está ignorada, não é utilizado como prova da execução PostgreSQL.
- Cenários nas duas ordens: transferência/transferência; transferência/venda; atribuição/venda sem corretora; estorno/venda; estorno/transferência dependente; dois estornos; compra/exclusão; custódia/exclusão; carteiras independentes.
- Evidências: PIDs reais e bloqueadores observados por `pg_blocking_pids`/`pg_stat_activity`, espera e revalidação após lock, contagens/estado persistido, códigos e causas específicos e ausência de escrita parcial/retries. Dois estornos produzem uma confirmação e `ESTORNO_DUPLICADO`, preservando exatamente um vínculo.
- Timeout de lock: **SQLState55P03**, traduzido nas cinco fachadas testadas. COMMIT: DML/flush aceito e tentativa JDBC observada; rejeição real pelo servidor com **SQLState40001** e rollback nas fronteiras de custódia, operação financeira e exclusão.
- O40001 é deliberadamente injetado por constraint trigger diferido no banco descartável; não comprova colisão espontânea de serialização. A tradução de40P01 permanece coberta pelos testes simulados/H2; não se declara deadlock real provocado.
- Relatório: **`completo=true; limpo=true; pendentes=[]; incertos=0`**. O usuário confirmou, por inspeção Docker independente após a execução, ausência de containers, volumes e redes associados ao UUID. Nenhuma inspeção foi repetida neste fechamento.

### Histórico de falhas e correções da Etapa 3

As tentativas abaixo permanecem historicamente reprovadas; não são reclassificadas como sucessos pelo fechamento posterior.

- Pré-verificações anteriores interrompidas por caminho Docker incorreto e restrição do sandbox à API do Engine: nenhuma matriz ou recurso foi iniciado nessas tentativas. A execução real posterior foi realizada no ambiente externo autorizado.
- `target/validacao-etapa3-20261025-183918-891a0b03/`: 2.177 casos, zero falhas de asserção, **340 erros** e 102 skips. A configuração auxiliar PostgreSQL, anotada como Configuration, contaminou o scan geral: 14 erros iniciais por Observador ausente, um por repositório duplicado e 325 erros de threshold. Além disso, o script reconheceu apenas63 skips e classificou39 assumptions PostgreSQL incorretamente.
- Correção: uso de `TestConfiguration` mantendo registro explícito pelo executor; classificador passou a considerar `skipped.InnerText` apenas nas classes/justificativas já reconhecidas. `target/custodia-etapa3-correcao-20261025-focada/relatorio.md`: 13 testes focados aprovados com loopback permitido; 102 skips reconhecidos e zero indevidos nos XMLs preservados. A tentativa focada anterior no sandbox falhou por loopback e permanece registrada separadamente.
- `target/validacao-etapa3-20261025-185530-e24837f8/`: 2.177 casos, zero falhas de asserção, **um erro** e 102 skips. A venda com consolidado insuficiente foi corretamente rejeitada, mas `StandardError` lançou NullPointerException ao copiar detalhes com valores nulos, impedindo a resposta HTTP409. Maven interrompeu antes do check JaCoCo.
- Correção: política de cópia de `StandardError` alinhada à de `ApiException`, no construtor e setter, com valores nulos, chaves não nulas, cópia defensiva e mapa não modificável. `target/custodia-standarderror-20261025-focada/relatorio.md`: RED/GREEN documentado e **62 testes focados aprovados**. O método `returnsStandardizedValidationAndBusinessErrorsWithoutPartialPersistence` passou sem mudar asserts: HTTP409/POSICAO_INSUFICIENTE, quantidadeDisponivel2 e apenas duas compras persistidas.
- Os relatórios globais anteriores e as evidências reais PostgreSQL foram preservados antes das novas execuções, com backups conferidos por SHA-256. Relatórios preparatórios datados que afirmam pendência de execução continuam descrevendo somente seu momento original.

### Validação global final do backend — resultado atual

- Pasta: **`target/validacao-etapa3-20261025-191727-b7af948d/`**.
- Execução única do procedimento `validar-etapa3.ps1`, em25/10/2026, encerrada às **19:19:12 -03:00**, com branch dev e ausência de Maven/Surefire concorrente conferidas antes da execução.
- Fontes: `verify.maven.log`, `verify.exitcode.txt`, `resultado.json`, `skips.csv`, `diario.ndjson` e `git-antes.txt`/`git-depois.txt` nessa pasta; relatórios novos em `target/surefire-reports/` e `target/site/jacoco/`.
- **BUILD SUCCESS; ExitCode0; 2.184 casos contabilizados; 2.082 aprovados; zero falhas; zero erros; 102 skips esperados; zero inesperados.**
- Skips: três homologações reais de custódia; 60 casos das demais classes condicionais PostgreSQL/protótipo; 36 ramificações PostgreSQL de `ReferenciaDataJdbcDiagnosticoTest` e três de `ReferenciaTemporalMigrationTest`. Nenhum teste condicional externo executado e nenhum relatório condicional ausente. As homologações1.9/1.10/3.10 não foram repetidas.
- Coleta JaCoCo nova: arquivo anterior preservado e retirado do caminho ativo, `append=false`, um fork reutilizado. `jacoco:report` e `jacoco:check` efetivamente executados; mensagem **All coverage checks have been met**.

| Portão BUNDLE | Cobertos / total | Percentual efetivo | Mínimo | Resultado |
| --- | ---: | ---: | ---: | --- |
| LINE | 3.331 / 3.464 | 96,16050808314087% | 80% | Aprovado |
| BRANCH | 1.800 / 2.105 | 85,5106888361045% | 80% | Aprovado |

O módulo analisado contém212 classes. Os percentuais arredondados de acompanhamento são LINE96,16% e BRANCH85,51%; os contadores são extraídos do nível BUNDLE do XML. Nenhum limite, exclusão ou assert foi enfraquecido. Os valores parciais antigos de cobertura não são apresentados como resultados desta rodada. `EstadoGitPreservado=true` e `Aprovado=true` registrados pelo procedimento.

### Integridade dos artefatos da Etapa 3

SHA-256 calculado sobre os arquivos locais neste fechamento, sem nova execução de testes. Caminhos relativos à raiz do repositório.

| Arquivo | SHA-256 |
| --- | --- |
| `target/validacao-etapa3-20261025-191727-b7af948d/resultado.json` | `ae648b88d9d513f014437d54b305cc48b7859a8ce62712fe5da5ac7484a110ff` |
| `target/validacao-etapa3-20261025-191727-b7af948d/verify.maven.log` | `fdfa50c50da4c8c088bd5c25b235a73c070152835ea992cc5bfb43e497037ac8` |
| `target/validacao-etapa3-20261025-191727-b7af948d/skips.csv` | `06335437a1735148368a989dfb4f86981438bae8d8f658599caa30a2bd5270f7` |
| `target/site/jacoco/jacoco.xml` | `8ac4985074e07a2432a6f4999c9f9d2e74fdf335da4ea347c3b75c684920135f` |
| `target/validacao-etapa3-20261025-183918-891a0b03/antes/surefire-reports/TEST-com.projeto.custodia.postgresql.CustodiaPostgresConcorrenciaRealTest.xml` | `ec995b9f26340005306a08afad2d8f5417dbacfc93c50b1c80f803eb3ddbb296` |
| `target/custodia-incremento6-relatorio.md` | `1c0f420152f5c35293cd949fc752d22b7ab5bd44abf75d89b9314ed3be3bf32d` |
| `target/custodia-etapa3-correcao-20261025-focada/relatorio.md` | `f4aed6906a51f208654e318da8a4ca131b331b219eabc420004fea9206fca7b3` |
| `target/custodia-standarderror-20261025-focada/relatorio.md` | `5e68ffb96dbe7a7726b91175f5155b5ba39906bced05480c08db0044b4286c74` |

Os artefatos em `target/` não são versionados. Preservar a pasta de validação final, os backups das execuções anteriores e o XML real de3.10 antes de futuras limpezas ou sobrescritas; o XML global ignorado não substitui o real preservado. O hash do XML JaCoCo refere-se ao arquivo disponível nesta data, sujeito a substituição por futuras validações.

### Escopo encerrado e pendências remanescentes

Não foi identificada pendência bloqueante dos critérios3.1–3.10. O fechamento não encerra a change inteira, não reconcilia as caixas ainda desmarcadas das Etapas1/2, não encerra6.10 nem autoriza APIs/frontend. As Etapas4–6 permanecem integralmente desmarcadas; a Etapa4 não foi implementada neste fechamento. Implementação futura, reconciliação de outros blocos, arquivamento e operações Git exigem autorização separada.

### Validação documental do fechamento

Em25/10/2026, `openspec validate adicionar-movimentacoes-custodia-entre-corretoras --type change --strict --no-interactive --json` aprovou a change: `valid=true`, `issues=[]`, um item aprovado, zero reprovados e ExitCode0. A conferência documental confirmou60 tarefas,12 concluídas e48 desmarcadas, com somente3.1–3.10 alteradas para conclusão e critérios originais preservados. O conteúdo das Etapas4–6 permaneceu idêntico ao anterior. Essa validação estática não representa nova execução de testes ou homologações.


## Fechamento técnico e documental da Etapa 4 — 26/10/2026

Esta seção atualiza o estado de acompanhamento após os Incrementos 1–4, adaptação Angular, complemento final e validações globais aprovados. As seções anteriores preservam seus resultados e escopos históricos; afirmações de Etapa 4 ainda pendente referem-se à data daqueles fechamentos. Nenhum teste ou homologação foi repetido neste fechamento documental. As tarefas 4.1–4.10 foram conferidas individualmente contra os contratos do design/specs, implementação e relatórios preservados; seus critérios originais não foram alterados.

### Rastreabilidade por tarefa

Classes abaixo existem sob `src/main/java/com/projeto/` e `src/test/java/com/projeto/`. Os XMLs da execução global atual estão preservados em `target/validacao-etapa4-global-20261026-214309/backend-surefire-final/`, independentemente da pasta Surefire ativa. Todas as classes citadas foram aprovadas; a validação global complementa as evidências direcionadas, sem substituir a inspeção do critério.

| Tarefa concluída | Implementação e contrato conferidos | Evidência verificável |
| --- | --- | --- |
| 4.1 | `CustodiaCreateRequest`, variantes `TransferenciaCustodiaCreateRequest`/`AtribuicaoCustodiaCreateRequest`, `EstornoCustodiaCreateRequest`; `RegistroCustodiaResponse`, `DetalheCustodiaResponse`, `PosicaoCustodiaResponse` e `CustodiaMapper`. Carteira somente do path, campos controlados/financeiros rejeitados; quantidade BigDecimal direta, número JSON ou string decimal, sem double/arredondamento; respostas numéricas, nulos explícitos e frações textuais. | `CadastroCustodiaRestH2Test` (68), `EstornoCustodiaRestH2Test` (55), `ConsultaCustodiaRestH2Test` (32) e `OpenApiDocumentationTest` (19): precisão BR/EUA, valores acima de 2^53, origem conforme natureza, discriminador e campos proibidos. |
| 4.2 | `MovimentacaoCustodiaResource`: POST da coleção e POST `/{movimentacaoId}/estornos`, 201/Location do detalhe original. Motivo sem corpo/{}/string/null, branco normalizado, texto não branco preservado, máximo 1.000 caracteres. `CadastroCustodiaService`/`CadastroCustodiaResponsePersistenceService` reutilizam os writers aprovados; refresh/vínculo/DTO na própria transação antes do commit, sem GET independente depois da confirmação. | Classes HTTP de cadastro/estorno: corpo/Location/context path, referências/contexto, flush, montagem, beforeCommit e ausência de persistência parcial. Limite preservado: em transação externa o COMMIT final pertence ao chamador. |
| 4.3 | Dois GETs de histórico unificado/detalhe original em `ConsultaMovimentacaoCustodiaService`. Snapshot SERIALIZABLE/readOnly/REQUIRES_NEW, vínculos em lote, filtros com regra específica para ESTORNO, identidade `(tipoRegistro,id)`, dataRegistro por instante DESC, MOVIMENTACAO antes de ESTORNO e id DESC; sem paginação/truncamento. | `ConsultaCustodiaRestH2Test` (32), `ConsultaMovimentacaoCustodiaServiceTest` (10): histórico completo, vazio, detalhe ativo/estornado, isolamento, empates, filtros individuais/combinados e posições encerradas. |
| 4.4 | Reutilizados `StandardError`, `ApiException`, handler, códigos/details quantitativos e classificadores conservadores aprovados na Etapa 3. Valores nulos permitidos nos detalhes, cópia defensiva/imutabilidade mantidas; erro financeiro POSICAO_INSUFICIENTE preservado. Sem exposição de SQL/constraints; integridade desconhecida não vira duplicidade ou conflito transitório fictício. | Testes HTTP de cadastro/estorno/precedência, `ResourceExceptionHandlerTest`, `StandardErrorTest`, `ApiExceptionDetailsTest`, `CustodiaConflitosTransacionaisH2Test` e `OperacaoResourceTest`, incluídos no portão global aprovado. Não foi necessária nova alteração de DTO de erro para encerrar esta etapa. |
| 4.5 | `DistribuicaoPosicaoCorretoraService` carrega operações, originais, estornos e referências no mesmo snapshot; `CalculadoraDistribuicaoCorretora` usa replay quantitativo efetivo e consolidado exclusivamente financeiro, sem providers/escrita. | `DistribuicaoCustodiaH2Test` (12), `DistribuicaoPosicaoCorretoraServiceIntegrationTest` (8), `CalculadoraDistribuicaoCorretoraTest` (20): transferência, atribuição, neutralização, invariância e quatro consultas constantes. |
| 4.6 | `PrimeiroDeficitResponse`, posições discriminadas, `DistribuicaoCorretoraMapper`, exemplos e endpoint existente: dominio/id/natureza/dataEfetiva/posicaoCronologica/saldoAposEvento. Sem IDs financeiros fictícios; primeira ocorrência, regra de abertas, arrays e ordenações preservadas. Parser/guard/modelos/template Angular compatíveis, saldo lossless e fração textual até 512 dígitos. | Testes H2 da distribuição, schemas/exemplos OpenAPI, `DistribuicaoCorretoraResourceTest` (14); adaptação Angular com 179 testes direcionados e suíte global frontend atual com 763 aprovados. |
| 4.7 | OpenAPI documenta somente as rotas existentes, variantes de cadastro, motivo opcional, filtros/ordenação, Location, controle interno, datas/precisão, erros e legado. `CustodiaCadastroSchema` representa entrada number|string; saída permanece number. Exemplos de original ativa/estornada, detalhe com estorno e histórico vazio/auditável em `CustodiaRestExamples`; sem endpoint individual de estorno ou custódia externa. | 19 testes de `OpenApiDocumentationTest`, incluindo validação dos exemplos publicados e documentação financeira/custódia/legado. Schemas nulos corrigidos no complemento sem alterar binding ou regras. |
| 4.8 | REST 201/400/404/409, isolamento, filtros, detalhe, Location, duplicidade e precedência do design; distinção entre erro externo POSICAO_CUSTODIA_INDISPONIVEL e seu motivo. | Classes HTTP dos três primeiros incrementos e seis casos novos em `CustodiaPrecedenciaRestH2Test`; matriz abaixo liga os conflitos simultâneos aos testes, com estado persistido inalterado nas rejeições. |
| 4.9 | Contratos de schemas/evidências/lossless verificados, com tipos e nulos exatos, frações strings, quantidade numérica na resposta e variantes de entrada coerentes com exemplos. Contratos financeiros regredidos sem alteração de fórmulas. | `OpenApiDocumentationTest`, HTTP de cadastro/estorno/consulta, testes de distribuição e parser Angular; complemento final 209 testes únicos e portões globais atuais aprovados. O helper de exemplos verifica os keywords usados pelos contratos; não é apresentado como validador genérico de todo JSON Schema. |
| 4.10 | Snapshot coerente de distribuição/histórico/detalhe; neutralização e diagnósticos efetivos, posições encerradas, consultas sem escrita/N+1, histórico completo com filtros/empates e exclusão protegida. | `ConsultaCustodiaConcorrenteH2Test` (2), `DistribuicaoCustodiaH2Test` (12), `ConsultaCustodiaRestH2Test` (32), `CarteiraCustodiaExclusaoH2Test` (20), além das regressões existentes. Metodologia de completude/desempenho e limites de comprovação abaixo. |

### Rodadas direcionadas preservadas

Todos os caminhos são relativos à raiz do repositório; os relatórios descrevem o estado de sua própria rodada. Não somar seleções que repetem classes como se fossem testes únicos.

| Rodada | Relatório | Resultado aprovado e limites |
| --- | --- | --- |
| Incremento 1 — GETs/DTOs | `target/custodia-etapa4-incremento1-20261025/relatorio.md` | 104 testes na regressão final, BUILD SUCCESS/ExitCode 0. |
| Incremento 2 — cadastro | `target/custodia-etapa4-incremento2-20261025/relatorio.md` | 221 testes na regressão final, BUILD SUCCESS/ExitCode 0. |
| Incremento 3 — estorno | `target/custodia-etapa4-incremento3-20261025/relatorio.md` | 228 únicos aprovados nos últimos relatórios por classe; rodada integrada antiga falhou no novo assert OpenAPI, corrigido e revalidado na classe inteira. Não declarar a rodada antiga como BUILD SUCCESS. |
| Incremento 4 — distribuição | `target/custodia-etapa4-incremento4-20261025/relatorio.md` e `impactos-angular.md` | 105 únicos aprovados nos últimos relatórios por classe; falha anterior do assert de consultas documentada e corrigida. Relatório de impactos é diagnóstico anterior à adaptação, não pendência atual do parser. |
| Compatibilidade Angular | `target/custodia-etapa4-compatibilidade-angular-20261026/relatorio.md` | 179 aprovados em sete arquivos; guard, serviço, DOM e consumidores/Dashboard preservados, sem novas telas/fluxos. |
| Complemento final | `target/custodia-etapa4-complemento-final-20261026/relatorio.md` | BUILD SUCCESS/ExitCode 0, 209 únicos: 10 novos e 199 regressões; zero falhas/erros/skips. |

Os relatórios e logs mantêm RED/GREEN, falhas de fixture/compilação/metadados e suas correções, sem converter tentativas reprovadas em aprovação. O complemento registra a sobreposição acidental de invocações Maven e a posterior execução sequencial. Os percentuais da Etapa 3 permanecem históricos e não comprovam a cobertura da Etapa 4.

### Precedência HTTP conferida

| Conflitos simultâneos | Teste/evidência | Resultado prioritário |
| --- | --- | --- |
| Quantidade inválida + destino ausente | `CustodiaPrecedenciaRestH2Test.quantidadeInvalidaPrecedeDestinoInexistente` | 400 REQUEST_INVALIDO |
| Consolidado insuficiente + local zero | `vendaConsolidadaInsuficientePrecedeSaldoLocalZero` na mesma classe | 409 POSICAO_INSUFICIENTE |
| Consolidado suficiente + local insuficiente | `vendaComConsolidadoSuficienteESaldoLocalInsuficiente` | 409 SALDO_CORRETORA_INSUFICIENTE |
| Origem insuficiente em todos os espaços + custódia posterior impeditiva | `origemInsuficienteEmTodosEspacosPrecedeCustodiaPosteriorImpeditiva` | SALDO_ORIGEM_INSUFICIENTE |
| Espaço sem saldo + outro com saldo que invalida custódia | `CadastroCustodiaRestH2Test.custodiaPosteriorInvalidadaPreservaIdentidadeECausaNoErroHttp` | POSICAO_CUSTODIA_INDISPONIVEL com motivo MOVIMENTACAO_CUSTODIA_INVALIDADA |
| Venda retroativa invalida custódia e agrava déficit | `CustodiaPrecedenciaRestH2Test.vendaRetroativaInvalidaCustodiaAntesDeClassificarDeficit` | MOVIMENTACAO_CUSTODIA_INVALIDADA antes do conflito diferencial |
| Duplicidade + novo histórico inválido | `EstornoCustodiaRestH2Test.estornoDuplicadoTemPrecedenciaSobreNovoHistoricoInvalido` | ESTORNO_DUPLICADO |
| Duplicidade + campo quantidade proibido | `CustodiaPrecedenciaRestH2Test.quantidadeProibidaPrecedeEstornoDuplicado` | 400 REQUEST_INVALIDO |
| Original fora da carteira, mesmo estornada | `EstornoCustodiaRestH2Test.referenciaInexistenteOuForaDaCarteiraRetorna404` e teste de duplicidade acima | 404 contextual, sem revelar a situação de outra carteira |

### Snapshot, completude e desempenho

- `ConsultaCustodiaRestH2Test.historicoRepresentativoSemTruncamentoOuNMaisUmENenhumaEscrita`: fixture confirmada de 83 registros e 62 corretoras, conjunto/contagem/ordem completos; filtro ATIVA com 41 originais, empates e identidades discriminadas. Três statements por leitura válida, sem crescimento por linha. Retratos de seis tabelas preservados e zero inserts/updates/deletes. Não há LIMIT, paginação ou teto de produto.
- Medição histórica do Incremento 1: payload de 34.998 bytes e aproximadamente 112,9 ms em uma consulta de teste; memória não aferida. Isso é observação local, não benchmark, SLA ou justificativa para truncamento. O código do teste registra novamente payload/tempo/consultas quando executado, inclusive nos XMLs globais preservados.
- Distribuição: quatro consultas tanto para uma quanto para 31 corretoras, sem escrita; primeira evidência efetiva, neutralização, encerradas e invariância dos DTOs financeiros cobertas. As duas ordens de snapshot transferência/estorno foram coordenadas no Incremento 4.
- `ConsultaCustodiaConcorrenteH2Test`: dois casos coordenam confirmação real do estorno em conexão independente entre a leitura de originais e vínculos. Future com timeout, confirmação posterior em transação separada e isolamento JDBC observado, sem depender de sleep; primeira resposta integralmente ATIVA/sem estorno e segunda integralmente ESTORNADA/com vínculo. Configuração SERIALIZABLE/readOnly existente mantida.
- Essas provas de leitura concorrente são H2. Não constituem nova homologação PostgreSQL nem alegam provocar deadlock real. A homologação de concorrência de escrita 3.10, assim como 1.9/1.10, mantém sua evidência própria já aprovada e não foi repetida.

### Validação global da Etapa 4 — rodada atual de 26/10/2026

Fonte consolidada: `target/validacao-etapa4-global-20261026-214309/resultado-consolidado.json`. Backend executado uma única vez pelo procedimento previamente aprovado, com adaptação somente do prefixo da pasta de evidências; sem clean, testes ignorados para obter aprovação ou mudança de limites. PostgreSQL condicional desativado explicitamente, sem Docker. Agente Mockito 5.23.0 validado. Relatórios anteriores copiados/verificados e retirados dos caminhos ativos antes de coletar dados novos.

**Backend:** `target/validacao-etapa4-20261026-214326-af7a66da/resultado.json` e `verify.maven.log`: BUILD SUCCESS, ExitCode 0, **2.374 contabilizados, 2.272 aprovados, zero falhas/erros, 102 skips esperados e zero inesperados**. Nenhum condicional externo executado ou relatório condicional ausente. JaCoCo `report` e `check` efetivamente executados, coleta nova com append=false e mensagem All coverage checks have been met. XML e Surefire desta rodada preservados em `target/validacao-etapa4-global-20261026-214309/backend-jacoco-final/` e `backend-surefire-final/`.

| Portão backend BUNDLE | Cobertos / total | Percentual | Mínimo | Resultado |
| --- | ---: | ---: | ---: | --- |
| LINE | 3.524 / 3.664 | 96,17903930131004% (96,18%) | 80% | Aprovado |
| BRANCH | 1.877 / 2.201 | 85,27941844616083% (85,28%) | 80% | Aprovado |

**Frontend:** após aprovação do backend, `npm run test:coverage`, ExitCode 0, **763 aprovados em 73 arquivos, zero falhas/erros/skips**. Cobertura anterior preservada separadamente; relatórios novos em `target/validacao-etapa4-global-20261026-214309/frontend-coverage-final/`, log em `frontend-test-coverage.log`. Limites globais configurados de linhas/ramos aprovados sem exclusões adicionais.

| Portão frontend global | Cobertos / total | Percentual publicado | Mínimo | Resultado |
| --- | ---: | ---: | ---: | --- |
| Linhas | 2.007 / 2.035 | 98,62% | 80% | Aprovado |
| Ramos | 1.844 / 1.966 | 93,79% | 80% | Aprovado |

Branch dev, ausência de processos concorrentes conferida, estado Git antes/depois igual e zero divergências de hashes de arquivos preexistentes registrados no consolidado. As execuções exigiram permissão para loopback/subprocessos do ambiente; não houve acesso ao PostgreSQL de desenvolvimento. Não reutilizar estes percentuais para uma futura alteração de código sem validação aplicável.

### Escopo encerrado e próximos limites

Não foi identificada pendência bloqueante das tarefas 4.1–4.10. Os critérios estão encerrados com as evidências específicas e globais acima. A adaptação do consumidor existente resolve a incompatibilidade relatada em impactos-angular.md; publicação deve continuar coordenando backend/parser, sem cliente antigo contra o novo contrato. Ela não conclui automaticamente tarefas da Etapa 5.

A change tem **60 tarefas: 22 concluídas e 38 desmarcadas**, com alteração de caixas limitada a 4.1–4.10 neste fechamento. Etapas 1/2 ainda desmarcadas não significam ausência de implementação; sua reconciliação não integra esta autorização. Etapas 5/6, testes manuais de novas telas/fluxos, documentação geral e validação final da change permanecem sujeitos às tarefas e futuras autorizações. Nenhuma implementação da Etapa 5, arquivamento, commit, push ou merge foi realizado ou autorizado por esta marcação.

### Integridade dos artefatos da Etapa 4

Hashes SHA-256 a seguir calculados sobre os arquivos locais existentes durante este fechamento documental, sem nova execução. Caminhos relativos à raiz do projeto. Os artefatos em target/ não são versionados; preservar os backups e as pastas identificadas antes de qualquer limpeza futura. Hashes não substituem a conservação dos arquivos brutos.

| Arquivo | SHA-256 |
| --- | --- |
| `target/custodia-etapa4-incremento1-20261025/relatorio.md` | `99b18d3f1e3ddac0181be86c491e2ff3548aa8e301ce03e9748a7fab76f421d6` |
| `target/custodia-etapa4-incremento2-20261025/relatorio.md` | `15c50d4a3746ab130486117e0642a3e62b3b68b58913845f4b038cf13b3646a0` |
| `target/custodia-etapa4-incremento3-20261025/relatorio.md` | `d2c4dd4d5415ebe4888c1a00a4f8fd5a853fd48195dcf3998efe1d73ab2714ba` |
| `target/custodia-etapa4-incremento4-20261025/relatorio.md` | `efc2c77e4124305d238856a253a984577281dc28499ee44aa1f4dec481e80cad` |
| `target/custodia-etapa4-incremento4-20261025/impactos-angular.md` | `632c16f210782e25bfb2998e552a6fdb05c5fcad0890de5446de07ef61b5ee51` |
| `target/custodia-etapa4-compatibilidade-angular-20261026/relatorio.md` | `a6a336da8ce9ddf8fa63e847b2b41447aff3783a90fba4c40c5e27060d9a0720` |
| `target/custodia-etapa4-complemento-final-20261026/relatorio.md` | `62fad7f7a21542a04fdd2ec5d4352c0ece30379d95e57eea3261f0838e03eca3` |
| `target/validacao-etapa4-global-20261026-214309/resultado-consolidado.json` | `16d5aacadc05871add8caeecb04506024ebf4325a6d79991ad9b16f82b313643` |
| `target/validacao-etapa4-20261026-214326-af7a66da/verify.maven.log` | `4720b8196f4ec0620acea17b19757190a7c5e4d2f2ce67e003e4f548e027e810` |
| `target/validacao-etapa4-global-20261026-214309/backend-jacoco-final/jacoco.xml` | `a06e8fea6f0749f8f820d4d8df142aa7d7c3d1b9fc515f64ed120ec01336ad79` |
| `target/validacao-etapa4-global-20261026-214309/frontend-test-coverage.log` | `7b96f0058fe67981d41ff5c51ab942f3bbb3dc69ca4216d8413175eab46a7018` |
| `target/validacao-etapa4-global-20261026-214309/frontend-coverage-final/frontend/coverage-summary.json` | `97dfa3433c21761e117ef6be8daf382cb6bf838f4f908f02e931da3092e6d094` |

### Validação documental do fechamento da Etapa 4

Em 26/10/2026, `openspec validate adicionar-movimentacoes-custodia-entre-corretoras --type change --strict --no-interactive --json` retornou **ExitCode 0, valid=true, issues=[], um item aprovado e zero reprovados**. A validação é estática e não constitui execução de Maven, Angular ou PostgreSQL. Conferência final: 60 tarefas, 22 concluídas e 38 desmarcadas; somente caixas 4.1–4.10 alteradas neste fechamento, sem reescrever critérios. Etapas 5/6 e caixas das demais etapas preservadas. Apenas tasks.md e evidencias.md foram atualizados na documentação versionável da change; relatório JSON da validação e conferências locais foram conservados em target/.
## Regularização documental após auditoria 6.9 — 29/10/2026

**Registro histórico anterior à validação final 6.10:** a contagem de 57/60 e as referências a 6.10 pendente nesta seção retratam a regularização daquele momento. O estado posterior aprovado é 58/60, registrado na seção seguinte.

Auditoria e matriz integral aprovadas pelo usuário. Fonte local: `target/custodia-auditoria69-20261029-145858/relatorio.md` e `matriz-60-tarefas.json`, com os critérios originais, implementação, testes, resultados e ação por tarefa. A conferência desta rodada releu as 60 linhas e verificou os hashes das 186 fontes registradas, sem divergências antes da atualização documental. Não houve nova execução de teste ou homologação.

| Itens agora marcados | Justificativa e evidência existente |
| --- | --- |
| 1.1–1.8 | Entidades separadas, reconstrução estrita, posição racional, repositórios em lote, migration 008 e constraints. Testes de domínio/hidratação, 114 H2 de persistência/integridade/repositórios/upgrade e homologação PostgreSQL 1.9 já registrada. Fontes: `target/custodia-19-final.log`, XMLs em `target/validacao-etapa4-global-20261026-214309/backend-surefire-final/` e matriz 6.9. |
| 2.1–2.10 | Eventos/normalizador/adaptador, replay puro, busca determinística e comparação diferencial; casos de déficit novo/agravado/prolongado/inalterado/reduzido, checkpoints e limite. XMLs globais históricos: racional 60, racional transitório 22, eventos 12, normalizador 21, adaptador 52, replay 30, buscador 33, posição cronológica 5, validador 41 e núcleo integrado 29. São contagens por classe, não percentuais de cobertura nem nova execução. Capacidade física complementar na homologação 1.10 preservada. |
| 5.1–5.2 | Modelos, serviço HTTP e parsing lossless: 84 casos novos e 179 regressões no Incremento 1; distribuição genérica reutilizada da Etapa 4, sem reimplementação. Fonte: `target/custodia-etapa5-incremento1-20261027-210804/relatorio.md`. |
| 5.3–5.6 | Formulário contextual, saldo auxiliar, histórico separado e confirmação de estorno. Fontes: `target/custodia-etapa5-incremento2-20261027-212027/relatorio.md` (346 únicos consolidados) e `target/custodia-etapa5-incremento3-20261027-213517/relatorio.md` (388 últimos resultados únicos). Não somar seleções que repetem arquivos. |
| 5.7–5.9 | Gerações/contexto, mensagens e detalhes exatos, sucesso separado da falha de leitura, retry exclusivamente GET. Fonte: `target/custodia-etapa5-incremento4-20261027-215100/relatorio.md` (426 únicos em 18 arquivos). Correções posteriores de foco e layout: 75 e 82 testes direcionados, respectivamente; resultados sobrepostos não são somados. A prova manual da falha seletiva é exceção de 6.5, não uma aprovação fictícia. |
| 6.1, 6.2, 6.4 | `target/custodia-etapa6-incremento1-20261028-201406/relatorio.md` e `surefire-final/`: 107 únicos em 11 classes, zero falhas/erros/skips. Dois novos casos BR/EUA com invariância de posição, resultados, patrimônio e snapshots; legado negativo consultável, precisão/cronologia/rollback. Nenhuma fixture incompatível remanescente identificada na seleção. |
| 6.3 | `target/custodia-etapa6-reconciliacao63-20261028-202330/relatorio.md`: Opção A, reaproveitamento integral dos 26 cenários PostgreSQL 3.10, incluindo espera/releitura e isolamento após lock. Não houve nova homologação. |
| 6.6–6.8 | PRD, README, AGENTS.md e Postman harmonizados com o código. `target/custodia-etapa6-documentacao-20261028-202744/relatorio.md` e `validacao-documental.json`: conferência estática aprovada, sete requests/quatro rotas de custódia e 25 respostas salvas; oito arquivos de migration/nove changeSets. Nenhuma request Postman ou deploy executado. |
| 6.9 | Matriz completa das 60 tarefas, rastreabilidade dos 32 critérios mínimos e extras, 36 requisitos em dez deltas, inspeção financeira/contratual e preservação. Auditoria aprovada pelo usuário, incluindo reconhecimento das duas exceções não homologadas. Nenhuma nova falha funcional conhecida identificada; isso não promete ausência absoluta de bugs. |

Foram alteradas somente 35 checkboxes anteriormente desmarcadas, preservando literalmente descrições e numeração. As 22 conclusões anteriores permanecem intactas. **Estado documental: 57 concluídas, três desmarcadas.** 5.10 e 6.5 são parciais, com dispensas e riscos residuais aceitos; 6.10 continua pendente. A auditoria concluída não equivale ao encerramento de toda a change.

### Exceções e resultados históricos

O registro durável em [excecoes-homologacao.md](excecoes-homologacao.md) distingue implementação atendida, evidência automatizada e verificação manual não executada. Avaliação sonora real e falha seletiva de GET após POST no navegador **não foram aprovadas**; foram dispensadas para esta entrega por decisão explícita do usuário. Os critérios originais não foram alterados nem essas duas tarefas marcadas como integralmente concluídas.

As correções de foco e responsividade foram aprovadas com testes direcionados e confirmação em navegador real; a massa visual extensa também foi aprovada. A homologação funcional preservou três originais estornadas e três estornos, saldo final observado de 100 PETR4 sem corretora/A0/B0, operações financeiras intactas e vínculos únicos. A rodada assistida não criou a atribuição extra condicionada ao bloqueio de GET. Fontes: `target/custodia-homologacao-retomada-20261029-065213/relatorio.md`, `target/custodia-correcao-layout-20261029-071541/relatorio.md`, `target/custodia-homologacao-residual-20261029-073111/relatorio.md` e `target/custodia-homologacao-assistida-20261029-143802/relatorio.md`.

As homologações PostgreSQL 1.9/1.10/3.10 e seus hashes/limites permanecem nas seções anteriores. BUILD SUCCESS, contagens e percentuais globais de 25/10 e 26/10 não são resultados da regularização ou da futura 6.10. Falhas históricas e respectivas correções não foram apagadas nem reinterpretadas como rodadas aprovadas.

### Preparação de 6.10, sem execução

Plano em [validacao-final.md](validacao-final.md): uma global backend Maven/H2 pelo script existente, cobertura global Angular após aprovação do backend, build Angular e OpenSpec estrito, além de preservação/revisão de Git, Graphify e migrations. Regressões aplicáveis já integram as suítes; não repetir PostgreSQL ou homologações manuais dispensadas. Limites de linhas/ramos mantidos em 80%, exigindo artefatos e resultados novos.

Nenhum Maven, teste Angular, build de aplicação, PostgreSQL, Docker, OpenSpec estrito, nova movimentação ou operação Git de escrita foi executado nesta regularização. A conferência documental é leitura/comparação de critérios, hashes, caminhos e contagens, não a validação final 6.10. Serviços não foram reiniciados ou encerrados.

### Integridade e conservação

| Fonte aprovada | SHA-256 conferido nesta regularização |
| --- | --- |
| `target/custodia-auditoria69-20261029-145858/relatorio.md` | 854cbfe554df76fcb709a414879fcadc663d45619396e6ace12b82d1f01c085d |
| `target/custodia-auditoria69-20261029-145858/matriz-60-tarefas.json` | ee6ca1a7f04d3fdd2133a1c7b12daf754837fe4fcccb9b2bfeda373e68ccd34e |

Esses artefatos em target/ não são versionados; preservar relatórios, backups e hashes. A nova pasta de regularização registra as 35 marcações e a preservação dos arquivos anteriores. Não executar clean para encerrar a change.

## Conclusão da validação final 6.10 — 29/10/2026

A validação final foi executada e aprovada tecnicamente; o usuário autorizou posteriormente a marcação de 6.10. Estado atual: **58 tarefas marcadas e duas desmarcadas, 5.10 e 6.5**. A aprovação não preenche retroativamente as verificações manuais dispensadas nem autoriza arquivamento ou operações Git.

Fonte da execução: `target/custodia-validacao-final-20261029-190655/relatorio.md` e `resultado-consolidado.json`. A contagem de 57 marcadas no consolidado é o retrato correto da execução, anterior à autorização para marcar 6.10; o artefato histórico não foi alterado.

| Verificação | Resultado aprovado dessa rodada |
| --- | --- |
| Backend Maven/H2 | BUILD SUCCESS, ExitCode 0; 2.376 contabilizados, 2.274 aprovados, zero falhas/erros, 102 skips esperados e nenhum inesperado; nenhum PostgreSQL real executado. |
| JaCoCo BUNDLE | Dados novos, report/check executados; LINE 96,18% e BRANCH 85,28%, limites de 80% atendidos. Não são percentuais reutilizados da Etapa 4. |
| Angular | 976 testes aprovados em 81 arquivos, zero falhas/erros/skips, ExitCode 0; linhas 98,71% e ramos 93,33%, limites de 80% atendidos. |
| Build de produção | ExitCode 0, com três warnings não bloqueantes de tamanho de bundle/estilos; nenhum limite alterado. |
| Migrations | Oito arquivos, nove changeSets únicos, hashes preservados; instalação/upgrade/Hibernate/integridade/rollback H2 aprovados na global, sem repetir PostgreSQL. |
| OpenSpec estrito | ExitCode 0, valid=true, issues=[]; um item aprovado, zero reprovados. |
| Preservação | Fontes, testes, configurações e documentos preservados na execução; serviços mantidos ativos; cache estrutural Graphify atualizado conforme o plano, sem alterar fontes. |

As exceções de 5.10 e 6.5 permanecem exatamente as aceitas em [excecoes-homologacao.md](excecoes-homologacao.md). Não houve nova homologação manual, movimento, teste Maven/Angular ou execução PostgreSQL nesta atualização dos resumos. Os registros históricos e os três warnings do build foram preservados.
