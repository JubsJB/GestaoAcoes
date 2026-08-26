## Why

O PRD exige que a posição aberta apresente seu ganho ou prejuízo potencial, mas a posição consolidada atualmente expõe somente custo e valor atual. Esta change completa esse indicador sem misturá-lo ao resultado realizado de vendas e sem acoplar a consulta a provedores de cotação.

## What Changes

- Estender `GET /carteiras/{carteiraId}/posicoes` e `PosicaoResponse` com `resultadoNaoRealizado` para cada posição atualmente aberta.
- Definir `resultadoNaoRealizado = valorAtualPosicao - custoPosicao`, com suporte explícito a ganho, perda e resultado zero.
- Calcular o indicador sob demanda com `BigDecimal`, precisão lógica máxima 38, escala 12 e sem arredondamento ou truncamento silencioso.
- Manter quantidade, preço médio e custo derivados exclusivamente do replay de Operações e usar somente a cotação já persistida em Ação para a avaliação de mercado.
- Preservar posições zeradas fora da resposta, ciclos encerrados fora do cálculo, moedas sem conversão e o fluxo dedicado `PATCH /acoes/{id}/cotacao` para atualização externa.
- Manter resultado realizado, rentabilidade, patrimônio, câmbio, dividendos, taxas, impostos e persistência de posições fora do escopo.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-position`: incluir o resultado não realizado da posição aberta no contrato e formalizar seu cálculo, sua política numérica e sua separação do replay contábil e do resultado realizado.

## Impact

- API: acréscimo compatível do campo `resultadoNaoRealizado` ao DTO retornado pelo endpoint existente de posições.
- Regra financeira: extensão da `CalculadoraPosicao`, mantendo o mapper como projeção sem cálculo.
- Testes: cálculo, contrato HTTP, ciclos de posição, precisão, efeitos colaterais e regressões de Operações, cotação e posição consolidada.
- Persistência e integrações: nenhuma entidade, tabela, migration, query ou chamada externa adicional; nenhuma dependência nova.
