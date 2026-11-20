# Pendencias finais da change

Status: alternativa A aprovada tecnicamente; 9.1 e 12.3 ratificadas conforme autorizacao condicionada a evidencia. 40 tasks, 29 concluidas, 11 pendentes. Nenhuma implementacao ou teste funcional novo nesta etapa; nenhuma validacao manual executada.

Atualizacao posterior:10.2 implementada e13.1/13.2/13.3 certificadas automaticamente, sem marcar tasks; build12.4 aprovado com warnings/ressalva de comparabilidade do checkpoint. Ver certificacao-funcional-financeira-build.md. A lista abaixo preserva o roteiro original de pendencias; a correcao10.2 ja nao e implementacao por fazer, mas aguarda ratificacao.

## Ratificacao por criterio

| Task | Criterio | Evidencia verificada |
| --- | --- | --- |
| 9.1 | DATA civil sem precisao fabricada ou timezone | formatCivilDate reorganiza texto sem Date; testes impedem construcao de Date, incluindo 0001/9999, bissextos e legado. |
| 9.1 | INSTANTE local sem mudar instante/DTO | formatter de offset inclui segundos; oraculos literais nos tres fusos, piso1970/virada dia/ano e fracao proxima de minuto; textos congelados permanecem iguais. Date e usado para apresentacao, nao validade financeira/precisao. |
| 9.1 | DESCONHECIDA sem fallback/defasagem | mensagem explicita, sem consulta a relogio ou uso do timestamp legado; formatter nao calcula classificacao de defasagem. |
| 9.1 | Obtencao independente | formatCotacaoObtidaEm separado, nulo="Nao informada" na UI (com acento no codigo); nenhuma reinterpretacao financeira pelo navegador. |
| 12.3 | Formatters e quatro superficies | 134 testes/7 arquivos por fuso UTC, America/Los_Angeles, Pacific/Kiritimati; lista/detalhe de Acao, posicoes e desempenho. |
| 12.3 | Parsing/representacao lossless | lossless-json.spec e DashboardService na suite completa 660/70 arquivos, preservando tokens BigDecimal, texto de offset/fracao e nulabilidade. Guards de aquisicao separados da leitura persistida. |

Fonte consolidada: implementacao-alternativa-a.md; logs target/alternativa-a-frontend.log e target/alternativa-a-superficies-{UTC,America_Los_Angeles,Pacific_Kiritimati}.log conferidos. Persistencia real ampliada para41 datas H2/PG, API e compatibilidade BC:205 testes backend/fuso, zero falhas/erros/skips. Esses resultados nao substituem validacao manual assistiva/mobile da12.1.

## Auditoria dos 91 skips

XMLs Surefire da suite completa: 1.066 casos reportados, 975 executados, 91 skips, zero falhas/erros. Condicoes inspecionadas nos fontes; nao sao testes quebrados desativados com @Disabled.

| Suite | Skips no comando padrao | Motivo / cobertura obrigatoria |
| --- | --- | --- |
| ReferenciaDataJdbcDiagnosticoTest | 36 | Assumption exige URL temporaria para parametros PostgreSQL. Diagnostico do binding antigo/direto; cobertura de producao equivalente/superior na matriz JPA/API41datas/fuso, nao exige repetir experimento historico. H2 executado no comando padrao. |
| ReferenciaTemporalMigrationTest | 3 | Assumption PostgreSQL nao configurado no comando padrao. Cobertura obrigatoria executada na Etapa D: XML target/etapa-d-reports-pg/TEST-com.projeto.repositories.ReferenciaTemporalMigrationTest.xml confirma6 testes H2+PG, zero falhas/erros/skips. Migration007 inalterada depois disso. |
| ReferenciaDataJpaPostgresqlTest | 4 | Classe opt-in temporal.test.postgresql.enabled; metodos parametrizados nao expandidos ao pular classe. Matriz atual89 casos/fuso aprovados, incluindo herdados, old/new, BC e API. |
| AplicacaoCotacaoPostgresqlTest | 25 | Mesmo opt-in; matriz atual44 casos/fuso aprovados com locks/atomicidade/aplicacao. |
| AtualizacaoCotacoesCarteiraPostgresqlTest | 22 | Mesmo opt-in; matriz atual31 casos/fuso aprovados com lote e sucesso parcial. |
| ReferenciaDataBcPrototypeTest | 1 | Opt-in temporal.poc.enabled do prototipo historico; nao e teste do binding anotado atual. Nao repetido conforme instrucao de nao criar/executar novas provas. Cobertura BC e old/new transferida aos testes de producao. |

Total36+3+4+25+22+1=91. Numeros de metodos ignorados nao equivalem ao numero de invocacoes parametrizadas executadas quando opt-in ativo. Nenhum skip deixa lacuna para ratificar9.1/12.3. As tasks abertas de certificacao final permanecem independentes.

## Todas as tasks restantes

### Pendencia de implementacao

- 10.2: ajustar feedback individual para nao prometer atualizacao/consulta HTTP em reuso. Evidencia concreta: AcaoDetailPageComponent mostra sempre "Cotacao atualizada com sucesso" no next da chamada, inclusive se backend conservou/reutilizou observacao. Menor proxima alteracao: mensagem neutra compativel com o contrato existente e teste focado; sem novo DTO/refatoracao.

### Pendencias de testes automatizados e certificacao tecnica

- 12.4: build de producao, budgets e comparacao initial com514418 bytes no mesmo ambiente; medir chunk lazy/CSS Dashboard/frontend-position-analysis sem biblioteca/budget novo. Suites de testes/tsc nao substituem esse build.
- 13.1: certificacao final de operacoes, precoUnitario, quantidade, custo, preco medio, realizado e replay; confrontar assercoes ja aprovadas, sem reimplementar calculos nem repetir manualmente matriz financeira.
- 13.2: certificar efeito global por Acao, avaliacoes e imutabilidade dos snapshots antigos usando cobertura existente; confirmar evidencias pertinentes no estado final.
- 13.3: certificar previa independente e ausencia de provider/snapshot automatico em GET usando testes existentes.
- 14.1: strict change/specs ja aprovado, mas falta certificar conjuntamente reconstrução oficial em memoria e o estado final; task composta permanece aberta.
- 14.3: Graphify ja atualizado por AST e classe JDBC presente; falta ratificacao final das relacoes afetadas apos ultima alteracao de codigo. Nenhuma regeneracao necessaria nesta etapa apenas documental.

### Pendencias de validacao manual e revisao documental final

- 12.1: completar validacao visual/mobile/teclado/leitor de tela/contraste; POST/disabled/loading ja cobertos automaticamente, nao duplicar como bateria manual extensa.
- 14.2: comparacao final requisitos/cenarios/Purposes e whitespace de todo escopo, inclusive nao rastreados. Ha evidencias parciais, ainda nao ratifica a task completa.
- 15.1: consolidar comandos/resultados por task final, incluindo evidencias manuais ainda ausentes; registros anteriores preservados, sem inventar sucesso.
- 15.2: revisao humana final do diff completo, escopo e contratos. A aprovacao tecnica de A nao e aprovacao de todas as pendencias da change.

Sao tres grupos de organizacao; o terceiro inclui revisao documental humana, nao somente navegacao visual. Nenhuma outra task marcada.

## Bloqueantes reais

Nao ha bloqueante tecnico de persistencia/concurrency conhecido que impeça testar os fluxos implementados. O nucleo individual/lote/Dashboard esta implementado e coberto; isso nao significa change encerrada.

Para aceite operacional:10.2 e pendencia concreta de fidelidade de feedback;12.1 e validacao de usabilidade/acessibilidade ainda nao comprovada;12.4 precisa demonstrar build de entrega/budgets.13.x sao gates de integridade financeira que nao podem ser dispensados, embora a suite esteja verde e nao haja regressao conhecida.14.x/15.x sao gates obrigatorios de encerramento/documentacao/revisao, nao funcionalidades novas ou evidencia de falha operacional. Se build/manual/regressao revelar defeito, ele passa a ser bloqueante; nao antecipar sucesso.

## Roteiro manual minimo

Usar ambiente dev autorizado e fluxo normal da aplicacao; preservar dados, nao resetar banco, nao criar snapshot automaticamente ou alterar provider/configuracao para forcar falha. Registrar navegador, viewport, fuso, resultado esperado/observado e uma evidencia por estado. Nao repetir as centenas de fronteiras temporais, concorrencia, precision/migration, payloads invalidos ou replay ja automatizados.

1. Acao individual: abrir detalhe por teclado, acionar atualizar uma vez e observar busy/retorno, cotacao/referencia e obtencao separadas. Confirmar feedback honesto apos10.2, inclusive ao acionar novamente dentro do cooldown; conferir foco/anuncio, nao contar chamadas para reproduzir testes de rede.
2. Carteira/Dashboard: selecionar carteira com posicoes, acionar lote uma vez e observar retorno, contadores e coerencia visual dos paineis/posicoes/desempenho apos recarga. Conferir que nao ha sobreposicao ou estado ocupado preso. Troca de carteira/cancelamento/ordem de GETs permanecem na cobertura automatizada, nao repetir exaustivamente.
3. Feedback parcial e total: em sessao de validacao de UI controlada, usar respostas interceptadas/fixtures ja testadas, sem enviar escrita adicional nem mudar backend/provider/dados para fabricar erro. Conferir legibilidade das falhas, diferenciacao parcial/total e recuperacao disponivel. Identificar explicitamente evidencia como UI simulada, nao prova de integracao real; sucesso real e passo2.
4. Referencia temporal: nas quatro superficies, inspecionar uma DATA civil, um INSTANTE local e um legado DESCONHECIDA disponiveis; se ausentes, usar fixture de UI explicitamente identificada. Obtencao nula="Nao informada", sem fallback. Um fuso local documentado basta visualmente; UTC/LA/Kiritimati e fronteiras ja automatizados.
5. Uma passada transversal desktop/mobile estreito com teclado e leitor de tela: botao identificado, disabled/busy anunciados, foco visivel, feedback acessivel, contraste e reflow sem perda de texto em tabela/cards/grafico. Cobrir os estados dos passos anteriores, sem outra bateria de requests.

Nao executado nesta etapa. Interceptacao e somente na sessao de navegador de teste, sem editar codigo/repositorio ou registrar credenciais.

## Ordem minima para concluir

1. Corrigir10.2 pontualmente e seu teste.
2. Executar build12.4 e consolidar certificacao13.x com testes existentes.
3. Uma sessao manual consolidada12.1, registrar15.1; nao iniciar outra investigacao temporal.
4. Fechar auditoria14.x, revisar diff final15.2 e solicitar aprovacao de fechamento. Marcar demais tasks somente com evidencias e autorizacao, sem staging/commit/push/merge/archive automaticos.
