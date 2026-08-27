## Why

O PRD exige a apresentação do patrimônio atual da Carteira, mas hoje o sistema expõe somente o valor atual de cada posição aberta. A nova consulta deve responder quanto a Carteira vale em cada moeda, sem somar BRL e USD, duplicar o replay financeiro ou tratar resultado realizado como caixa inexistente no modelo.

## What Changes

- Adicionar `GET /carteiras/{carteiraId}/patrimonio`, sem body, filtros, paginação ou `Location`, com `200 OK` para Carteira existente e `404 Not Found` para Carteira inexistente.
- Retornar o patrimônio consolidado por moeda em um `PatrimonioResponse` identificado por `carteiraId`, com itens `PatrimonioMoedaResponse` contendo somente `moeda` e `patrimonioAtual`.
- Calcular cada acumulado exclusivamente pela soma dos `valorAtualPosicao` das posições abertas da mesma moeda, omitindo moedas sem posição aberta e retornando `patrimonios=[]` para Carteira vazia.
- Preservar BRL e USD em acumulados independentes, sem conversão cambial ou total monetário único.
- Reutilizar a consolidação vigente de posições, sem recalcular quantidade, preço médio, custo, cotação ou valor atual e sem executar segundo replay ou consulta adicional.
- Aplicar soma exata com `BigDecimal`, normalização final em escala 12 por `RoundingMode.UNNECESSARY`, precisão máxima 38 e `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` quando o acumulado não for representável.
- Manter resultado realizado, resultado não realizado, rentabilidade, caixa e indicadores históricos fora do patrimônio atual desta primeira versão.
- Manter consulta read-only em `REPEATABLE_READ`, sem lock pessimista, escrita, Clock, provider, cache, persistência ou alteração de schema.

## Capabilities

### New Capabilities

- `portfolio-valuation`: consulta e cálculo sob demanda do patrimônio atual de uma Carteira, consolidado separadamente por moeda a partir das posições abertas.

### Modified Capabilities

Nenhuma. Os contratos normativos de Carteira, Operações, posição consolidada, resultado realizado e atualização de cotação permanecem inalterados.

## Impact

- API: novo endpoint aninhado em Carteira e dois DTOs mínimos de resposta.
- Aplicação: novo service e mapper de patrimônio, reutilizando `PosicaoService` e seus resultados já consolidados.
- Domínio financeiro: agregação em memória por moeda dos valores atuais prontos, sem nova interpretação das Operações.
- Persistência e integrações: nenhuma entidade, repository, query, migration, dependência ou chamada externa adicional.
