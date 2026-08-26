## Why

A posição consolidada já apresenta custo, valor atual e resultado não realizado, mas ainda não expressa proporcionalmente o ganho ou a perda potencial da posição aberta, requisito previsto no PRD. A mudança acrescenta esse indicador sem duplicar cálculos anteriores nem aproximar o histórico financeiro por outra fórmula.

## What Changes

- Estender `GET /carteiras/{carteiraId}/posicoes` e `PosicaoResponse` com `rentabilidadePercentual`.
- Definir como única fórmula oficial `(resultadoNaoRealizado / custoPosicao) × 100`, consumindo os valores já consolidados.
- Representar ganho, perda e neutralidade por percentual positivo, negativo ou zero, calculado somente para posições abertas e para o ciclo vigente.
- Usar aritmética exclusiva com `BigDecimal`, divisão em escala intermediária 24 por `HALF_EVEN`, saída percentual em escala 6 e precisão máxima 38.
- Reutilizar `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` quando a rentabilidade não puder ser representada pela política aprovada.
- Preservar o GET como consulta `readOnly`, sem providers, escrita, nova query, persistência do indicador ou mudança de schema.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-position`: acrescentar a rentabilidade percentual da posição aberta ao contrato da posição consolidada e ajustar os limites de indicadores fora do escopo.

## Impact

- API: acréscimo não destrutivo de `rentabilidadePercentual` ao `PosicaoResponse` de `GET /carteiras/{carteiraId}/posicoes`.
- Regra financeira: novo cálculo puro em `CalculadoraPosicao`, executado depois de `resultadoNaoRealizado`.
- Orquestração e projeção: adaptações mínimas em `PosicaoService` e `PosicaoMapper`.
- Testes: calculadora, ciclos financeiros, mercados, contrato HTTP, ausência de efeitos colaterais e regressões de posição, Operações e atualização de cotação.
- Persistência e integrações: nenhuma entidade, tabela, migration, dependência, query ou provider adicional.
