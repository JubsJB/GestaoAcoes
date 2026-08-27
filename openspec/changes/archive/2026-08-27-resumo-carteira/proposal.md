## Why

As posições abertas já expõem custo, valor atual e resultado não realizado por Ação, mas a Carteira ainda não oferece uma visão atual consolidada desses indicadores por moeda. O resumo permitirá responder essa necessidade sem misturar BRL e USD, sem incorporar resultado realizado como caixa e sem criar fórmulas financeiras paralelas.

## What Changes

- Adicionar `GET /carteiras/{carteiraId}/resumo`, sem body, filtros, paginação ou `Location`.
- Retornar custo total das posições abertas, patrimônio atual e resultado não realizado total, consolidados separadamente por moeda.
- Consumir uma única lista produzida por `PosicaoService.listarPorCarteira`, preservando o replay financeiro oficial, o fetch plan e a consulta consistente.
- Compartilhar a agregação monetária por moeda com `PatrimonioService`, para que `patrimonioAtual` permaneça idêntico nos dois contratos sem executar um segundo replay.
- Aplicar `BigDecimal`, soma exata e normalização final em escala 12, precisão máxima 38 e `RoundingMode.UNNECESSARY`, reutilizando `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`.
- Manter fora do resumo resultado realizado, rentabilidade consolidada, caixa, conversão cambial, histórico e persistência de agregados.

## Capabilities

### New Capabilities

- `portfolio-summary`: consulta do resumo financeiro atual da Carteira, consolidado por moeda a partir das posições abertas.

### Modified Capabilities

- Nenhuma. O contrato de `portfolio-valuation` permanece inalterado; a equivalência com o novo resumo é especificada na nova capability e garantida pelo design compartilhado.

## Impact

- API: novo `GET /carteiras/{carteiraId}/resumo` em `CarteiraResource`.
- Aplicação: novo serviço e mapper de resumo; evolução interna de `PatrimonioService` para compartilhar uma agregação pura por moeda.
- DTOs: novos `ResumoCarteiraResponse` e `ResumoMoedaResponse`.
- Financeiro: nenhuma alteração no replay, em `PosicaoResponse` ou nas fórmulas das posições; os agregados consomem valores já calculados.
- Persistência e integrações: nenhuma entidade, tabela, migration, repository, dependência ou provider novo.
