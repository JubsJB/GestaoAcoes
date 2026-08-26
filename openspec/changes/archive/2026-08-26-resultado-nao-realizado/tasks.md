## 1. Confirmar baseline e limites

- [x] 1.1 Confirmar na baseline que `PATCH /acoes/{id}/cotacao` permanece o fluxo dedicado para atualizar `cotacaoAtual` e `dataHoraCotacao`, sem alteração de contrato nesta change.
- [x] 1.2 Confirmar que `GET /carteiras/{carteiraId}/posicoes` usa `@EntityGraph` ou fetch plan equivalente para carregar `Operacao.acao` sem N+1 e sem provider externo.
- [x] 1.3 Registrar o estado inicial de schema, changeSets 001–004, dependências e configurações para auditar que permanecerão inalterados.

## 2. Implementar cálculo financeiro puro

- [x] 2.1 Adicionar à `CalculadoraPosicao` a precisão máxima 38 do resultado não realizado, reutilizando a escala de saída 12.
- [x] 2.2 Implementar `resultadoNaoRealizado = valorAtualPosicao - custoPosicao` exclusivamente com `BigDecimal`.
- [x] 2.3 Normalizar a diferença para escala 12 com `RoundingMode.UNNECESSARY` e rejeitar precisão superior a 38.
- [x] 2.4 Garantir que o cálculo aceite resultados positivos, negativos e zero, sem `null`, truncamento ou arredondamento silencioso.
- [x] 2.5 Reutilizar o fluxo `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` para falha do novo cálculo, sem criar código de erro paralelo.

## 3. Estender o contrato e a orquestração

- [x] 3.1 Acrescentar somente `resultadoNaoRealizado` ao final de `PosicaoResponse`, preservando todos os campos e tipos existentes.
- [x] 3.2 Adaptar `PosicaoService` para calcular o resultado após o replay, o filtro de posição zerada e o cálculo de `valorAtualPosicao`.
- [x] 3.3 Adaptar `PosicaoMapper` para receber e projetar o resultado já calculado, sem executar regra financeira.
- [x] 3.4 Preservar `readOnly`, `Isolation.REPEATABLE_READ`, ordenação, ausência de lock, escrita, `Clock`, provider e queries adicionais no GET.

## 4. Testar a calculadora

- [x] 4.1 Cobrir ganho `3550 - 3200 = 350`, perda `3000 - 3200 = -200` e resultado zero em escala 12.
- [x] 4.2 Cobrir subtração exata, `RoundingMode.UNNECESSARY`, valores grandes e rejeição de resultado fora da precisão 38.
- [x] 4.3 Demonstrar que a fórmula oficial usa valor atual menos custo e não recalcula o resultado a partir do preço médio apresentado.

## 5. Testar ciclos e mercados

- [x] 5.1 Cobrir compra única e múltiplas compras, preservando preço médio ponderado e custo consolidado.
- [x] 5.2 Cobrir venda parcial com resultado somente sobre quantidade, custo e valor atual remanescentes, sem incorporar resultado realizado.
- [x] 5.3 Cobrir venda total com posição omitida e novo ciclo independente do ciclo encerrado.
- [x] 5.4 Cobrir posição BRASIL em BRL e posição EUA em USD com quantidade fracionária, sem conversão ou agregação cambial.
- [x] 5.5 Cobrir alteração da cotação persistida refletindo novo resultado sem alterar `precoMedio`, `custoPosicao` ou replay.

## 6. Testar serviço, persistência e performance

- [x] 6.1 Cobrir múltiplas posições, ordenação existente, Carteira vazia e Carteira somente com posições zeradas.
- [x] 6.2 Cobrir Carteira inexistente com `404` e cálculo inválido com `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`, sem resposta parcial.
- [x] 6.3 Verificar ausência de `save`, escrita, `Clock`, `CotacaoProvider`, BRAPI, Alpha Vantage e atualização de Ação durante o GET.
- [x] 6.4 Verificar que o novo cálculo não executa query adicional e que múltiplas Ações continuam sem N+1.
- [x] 6.5 Preservar e executar o teste de consistência concorrente da consulta sob `REPEATABLE_READ`.

## 7. Testar contrato HTTP e regressões

- [x] 7.1 Atualizar testes de contrato e HTTP para o novo campo em ganho, perda e zero, mantendo `200 OK`, ausência de `Location`, `[]` e `404`.
- [x] 7.2 Executar regressão completa da posição consolidada e da capability `cotacao-atual-posicao`, incluindo precisão e data/hora persistida.
- [x] 7.3 Executar regressão de `PATCH /acoes/{id}/cotacao` para BRASIL, EUA, timestamp monotônico, concorrência, preço igual posterior, ticker canônico divergente e preservação da última cotação.
- [x] 7.4 Executar regressão de cadastro/consulta de Ação, registro/consulta/replay de Operações, VENDA e DELETE protegido de Carteira.

## 8. Verificação final

- [x] 8.1 Executar testes direcionados e a suíte completa com `clean verify`, incluindo Liquibase/Hibernate no H2 e PostgreSQL quando previsto e disponível.
- [x] 8.2 Atualizar o Graphify após as alterações de código e confirmar que o grafo representa o novo cálculo sem dependência externa no GET.
- [x] 8.3 Validar `resultado-nao-realizado` e o conjunto global OpenSpec em modo strict; executar `git diff --check`, revisar `git diff` e `git status` e confirmar ausência de migration, schema, dependência ou funcionalidade fora do escopo.
