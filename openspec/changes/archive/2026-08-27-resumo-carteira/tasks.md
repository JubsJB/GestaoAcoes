## 1. Baseline e contrato

- [x] 1.1 Confirmar a baseline de `CarteiraResource`, `PosicaoService`, `PatrimonioService`, DTOs, mappers, política de erros e testes antes das alterações.
- [x] 1.2 Registrar testes de caracterização para o contrato vigente de `GET /patrimonio` e para os valores financeiros de `PosicaoResponse` que alimentarão o resumo.
- [x] 1.3 Confirmar que o fetch plan cronológico de `OperacaoRepository` carrega `Operacao.acao` sem N+1 e não requer nova query para a change.

## 2. Agregação compartilhada por moeda

- [x] 2.1 Criar a representação interna imutável dos totais por moeda com `custoTotalPosicoes`, `patrimonioAtual` e `resultadoNaoRealizadoTotal`.
- [x] 2.2 Implementar `AgregadorPosicoesPorMoeda` puro sobre uma coleção de `PosicaoResponse`, agrupando BRL e USD independentemente com `BigDecimal.add`.
- [x] 2.3 Normalizar somente os três acumulados finais para escala 12 com `RoundingMode.UNNECESSARY` e validar precisão máxima 38.
- [x] 2.4 Sinalizar falha de qualquer acumulado com contexto suficiente para tradução em `CALCULO_POSICAO_FORA_DA_PRECISAO`, sem resultado parcial e sem acoplamento HTTP no agregador.
- [x] 2.5 Garantir que o agregador não contenha repository, service, provider, `Clock`, transação, escrita, replay ou recálculo de valores individuais.

## 3. Services, DTOs, mapper e resource

- [x] 3.1 Adaptar `PatrimonioService` para chamar `PosicaoService.listarPorCarteira` uma única vez e consumir somente `patrimonioAtual` do agregador compartilhado.
- [x] 3.2 Preservar integralmente `PatrimonioResponse`, ordenação, transação e tradução de overflow do endpoint de patrimônio.
- [x] 3.3 Criar `ResumoCarteiraResponse` e `ResumoMoedaResponse` com exclusivamente os campos aprovados e listas defensivamente imutáveis.
- [x] 3.4 Criar `ResumoCarteiraMapper` para projetar totais já calculados, sem soma, normalização ou outra regra financeira.
- [x] 3.5 Criar `ResumoCarteiraService` read-only com `Isolation.REPEATABLE_READ`, uma única chamada a `PosicaoService` e nenhuma chamada a `PatrimonioService`.
- [x] 3.6 Adicionar somente `GET /carteiras/{carteiraId}/resumo` a `CarteiraResource`, preservando todos os endpoints existentes.

## 4. Testes unitários do agregador

- [x] 4.1 Testar agregação de uma e múltiplas posições BRL e USD, separação entre moedas e ordenação final por `moeda ASC`.
- [x] 4.2 Testar somas exatas de custo, patrimônio e resultado não realizado positivo, negativo e zero em escala 12.
- [x] 4.3 Testar que `resultadoNaoRealizadoTotal` usa a soma dos valores das posições e que `patrimonioAtual - custoTotalPosicoes` permanece apenas verificação matemática.
- [x] 4.4 Testar valores grandes, overflow de cada acumulado, precisão máxima 38, `RoundingMode.UNNECESSARY` e falha integral sem totais parciais.
- [x] 4.5 Testar o agregador isoladamente para comprovar ausência de replay, queries, providers, `Clock`, escrita e dependências de service.

## 5. Testes de service e HTTP

- [x] 5.1 Testar Carteira vazia, somente BRL, somente USD, BRL+USD e Carteira inexistente no `ResumoCarteiraService`.
- [x] 5.2 Testar que `PatrimonioService` e `ResumoCarteiraService` chamam `PosicaoService.listarPorCarteira` exatamente uma vez por consulta.
- [x] 5.3 Testar que `ResumoCarteiraService` não depende nem chama `PatrimonioService` e não provoca query ou replay adicional por indicador.
- [x] 5.4 Testar equivalência exata de `patrimonioAtual` por moeda entre `GET /patrimonio` e `GET /resumo` para o mesmo conjunto de posições.
- [x] 5.5 Testar o contrato HTTP de `GET /resumo`: `200`, lista vazia, `404`, campos e escalas JSON, ausência de body, filtros, paginação e `Location`.

## 6. Ciclos, efeitos colaterais e regressões

- [x] 6.1 Testar venda parcial, venda total, posição zerada e novo ciclo sem influência financeira do ciclo encerrado no resumo atual.
- [x] 6.2 Testar que resultado realizado e rentabilidade individual não participam dos acumulados e que resultado não realizado não é somado duas vezes ao patrimônio.
- [x] 6.3 Testar propagação de `409 / HISTORICO_OPERACOES_INCONSISTENTE`, ausência de resposta parcial e snapshot consistente sob `REPEATABLE_READ`.
- [x] 6.4 Executar regressões de posição consolidada, patrimônio, resultado realizado, resultado não realizado, rentabilidade, PATCH de cotação, Operações e DELETE protegido de Carteira.

## 7. Verificação e entrega

- [x] 7.1 Executar testes direcionados do agregador, services e endpoints de resumo e patrimônio.
- [x] 7.2 Executar a suíte completa e `mvnw clean verify`, incluindo Liquibase/Hibernate no H2 e PostgreSQL quando previsto e disponível.
- [x] 7.3 Validar `resumo-carteira` e o conjunto global OpenSpec em modo strict.
- [x] 7.4 Atualizar o Graphify após as alterações de código e conferir o fluxo `CarteiraResource → ResumoCarteiraService → PosicaoService → AgregadorPosicoesPorMoeda`.
- [x] 7.5 Auditar ausência de migration, schema, dependências, providers, queries financeiras e funcionalidades fora do escopo.
- [x] 7.6 Executar `git diff --check`, revisar `git diff` e `git status`, sem commit, push, pull, merge, rebase ou arquivamento.
