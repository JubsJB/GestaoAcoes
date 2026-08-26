## 1. Confirmar baseline e invariantes

- [x] 1.1 Confirmar o contrato atual de `PosicaoResponse`, o fluxo GET e a cadeia até `resultadoNaoRealizado` antes de alterar código.
- [x] 1.2 Confirmar no replay que toda posição aberta válida possui `quantidadeAtual > 0` e `custoPosicao > 0` e que posições zeradas são omitidas.
- [x] 1.3 Confirmar o fetch plan atual de `Operacao.acao`, a ausência de N+1 e a inexistência de provider ou query adicional no GET.
- [x] 1.4 Registrar o estado inicial de entidades, schema, changeSets 001–004, dependências, configurações e contrato do PATCH de cotação.

## 2. Implementar cálculo financeiro puro

- [x] 2.1 Adicionar à `CalculadoraPosicao` as constantes aprovadas de escala final 6 e precisão máxima 38 da rentabilidade.
- [x] 2.2 Implementar exclusivamente `(resultadoNaoRealizado / custoPosicao) × 100` com `BigDecimal` e sem fórmula paralela.
- [x] 2.3 Aplicar escala intermediária 24 e `HALF_EVEN` na divisão, multiplicar por 100 e normalizar a saída em escala 6 com `HALF_EVEN`.
- [x] 2.4 Rejeitar representação acima da precisão 38 reutilizando o fluxo `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`.

## 3. Preservar consistência e encadear o cálculo

- [x] 3.1 Detectar posição aberta com `custoPosicao <= 0` antes da divisão e reutilizar `409 / HISTORICO_OPERACOES_INCONSISTENTE`, sem fallback ou resposta parcial.
- [x] 3.2 Adaptar `PosicaoService` para calcular rentabilidade depois de valor atual e resultado não realizado, preservando `readOnly` e `REPEATABLE_READ`.
- [x] 3.3 Acrescentar somente `rentabilidadePercentual` ao final de `PosicaoResponse`.
- [x] 3.4 Adaptar `PosicaoMapper` para projetar o percentual pronto, sem cálculo financeiro.

## 4. Testar política numérica

- [x] 4.1 Cobrir ganho `350 / 3200 × 100 = 10.937500`, perda `-200 / 3200 × 100 = -6.250000` e zero não nulo em escala 6.
- [x] 4.2 Cobrir venda parcial `300 / 600 × 100 = 50.000000` e outras divisões exatas.
- [x] 4.3 Cobrir divisão periódica, escala intermediária 24 e arredondamento `HALF_EVEN` na saída em escala 6.
- [x] 4.4 Cobrir valores grandes, percentuais acima de 100 e falha quando o resultado exceder precisão 38.

## 5. Testar invariantes, ciclos e mercados

- [x] 5.1 Cobrir custo positivo após compra única e múltiplas compras e falha segura para posição aberta inconsistente com custo não positivo.
- [x] 5.2 Cobrir venda parcial usando somente custo e resultado remanescentes, sem incorporar resultado realizado.
- [x] 5.3 Cobrir venda total com posição omitida e nova COMPRA iniciando ciclo independente.
- [x] 5.4 Cobrir BRASIL/BRL e EUA/USD com quantidade fracionária, mantendo o percentual adimensional e sem conversão cambial.

## 6. Testar serviço, contrato HTTP e efeitos colaterais

- [x] 6.1 Cobrir novo campo no contrato e no HTTP para ganho, perda e zero, mantendo `200 OK` e ausência de `Location`.
- [x] 6.2 Cobrir Carteira vazia, somente posições encerradas e Carteira inexistente com os contratos `[]` e `404` existentes.
- [x] 6.3 Verificar que mudança de cotação altera valor atual, resultado não realizado e rentabilidade, sem alterar preço médio, custo ou replay.
- [x] 6.4 Verificar ausência de escrita, `save`, `Clock`, provider, PATCH interno, nova query e N+1 durante o GET.

## 7. Executar regressões funcionais

- [x] 7.1 Executar regressão completa de posição consolidada, cotação atual da posição e resultado não realizado.
- [x] 7.2 Executar regressão do PATCH de cotação para BRASIL, EUA, timestamp monotônico, concorrência e ticker canônico divergente.
- [x] 7.3 Executar regressão de cadastro/consulta de Ação e registro, consulta, replay e validação de VENDA de Operações.
- [x] 7.4 Executar regressão de DELETE protegido de Carteira e consistência concorrente do GET sob `REPEATABLE_READ`.

## 8. Verificar entrega

- [x] 8.1 Executar testes direcionados, suíte completa e `clean verify`, incluindo Liquibase/Hibernate no H2 e PostgreSQL quando previsto e disponível.
- [x] 8.2 Confirmar por auditoria que não houve entidade, repository, tabela, migration, dependência, configuração ou funcionalidade fora do escopo.
- [x] 8.3 Atualizar o Graphify após alterações de código e confirmar a cadeia financeira e ausência de dependência externa ou query adicional.
- [x] 8.4 Validar a change e o conjunto global OpenSpec em modo strict; executar `git diff --check`, revisar `git diff` e `git status`.
