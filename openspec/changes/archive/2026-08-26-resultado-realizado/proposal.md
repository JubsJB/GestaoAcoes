## Why

O histórico de Operações já contém os fatos necessários para calcular lucro ou prejuízo concretizado em VENDAS, mas o sistema ainda não oferece uma consulta que preserve resultados de posições encerradas e múltiplos ciclos. Esta change completa a separação financeira entre resultado realizado histórico e os indicadores da posição atualmente aberta, conforme RF26, RN14, RN16 e a seção 12.3 do PRD.

## What Changes

- Adicionar `GET /carteiras/{carteiraId}/resultados-realizados`, sem body, filtros, paginação ou `Location`, com `200 OK`, `[]` sem VENDAS e `404` para Carteira inexistente.
- Retornar `ResultadoRealizadoResponse` acumulado por Carteira+Ação somente para Ações com ao menos uma VENDA, incluindo resultado acumulado zero como `0.000000000000`.
- Calcular cada VENDA por `(precoUnitarioVenda - precoMedioVigenteAntesDaVenda) × quantidadeVendida` e somar todas as VENDAS de todos os ciclos da mesma Carteira+Ação.
- Evoluir o replay financeiro único para produzir posição final, resultado realizado acumulado e indicador interno de existência de VENDA, sem duplicar cronologia, preço médio, custo, zeramento ou novo ciclo.
- Preservar resultados de posições atualmente abertas e encerradas, sem incorporá-los a `precoMedio`, `custoPosicao`, `resultadoNaoRealizado` ou `rentabilidadePercentual`.
- Aplicar `BigDecimal`, preço médio interno em escala 24, acumulação antes da normalização, saída em escala 12 com `HALF_EVEN` e precisão máxima 38.
- Reutilizar `409 / HISTORICO_OPERACOES_INCONSISTENTE` para histórico persistido impossível e `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` para falha numérica, preservando `POSICAO_INSUFICIENTE` no cadastro de VENDA inválida.
- Manter a consulta read-only em `REPEATABLE_READ`, com uma leitura cronológica, fetch de Ação sem N+1, sem provider, Clock, escrita ou persistência do resultado.

## Capabilities

### New Capabilities

- `realized-result`: consulta REST e cálculo histórico acumulado do resultado realizado por Carteira+Ação.

### Modified Capabilities

Nenhuma. Os contratos normativos de Operações, posição consolidada, Carteiras e atualização de cotação permanecem inalterados.

## Impact

- API: novo endpoint aninhado em Carteira e novo DTO de resposta.
- Aplicação: novo service e mapper de consulta; extensão compatível da saída interna de `CalculadoraPosicao`.
- Persistência: reutilização de `CarteiraRepository` e `OperacaoRepository`, inclusive do fetch plan existente de `Operacao.acao`; nenhuma entidade, tabela, migration ou query por VENDA/Ação.
- Compatibilidade: sem alteração de `Operacao`, `Acao`, `Carteira`, `PosicaoResponse`, providers, PATCH de cotação, changeSets 001–004, dependências ou configurações.
