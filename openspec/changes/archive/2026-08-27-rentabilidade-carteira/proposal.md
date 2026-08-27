## Why

O resumo atual da Carteira consolida custo, patrimônio e resultado não realizado por moeda, mas ainda não apresenta a rentabilidade percentual correspondente. A extensão permite interpretar o ganho ou a perda potencial das posições abertas sem criar outro replay, outra agregação ou uma métrica histórica incompatível com o estado atual.

## What Changes

- Estender cada item de `GET /carteiras/{carteiraId}/resumo` com `rentabilidadePercentual`, mantendo o endpoint, o envelope `ResumoCarteiraResponse` e todos os comportamentos atuais.
- Calcular por Carteira e moeda exclusivamente `(resultadoNaoRealizadoTotal / custoTotalPosicoes) × 100`, usando os valores oficiais já consolidados no resumo.
- Reutilizar uma única política percentual pura também usada pela rentabilidade individual da posição, com `BigDecimal`, escala intermediária 24, `HALF_EVEN`, multiplicação por 100, escala final 6 e precisão máxima 38.
- Tratar custo total não positivo em grupo com posições abertas como histórico inconsistente, sem percentual sintético ou resposta parcial.
- Preservar separação BRL/USD, ciclos abertos, transação consistente, ausência de providers, persistência, nova query, segundo replay ou segunda chamada a `PosicaoService`.
- Manter fora do escopo resultado realizado, rentabilidade realizada ou histórica, aportes, resgates, caixa, TWR, XIRR, conversão cambial, snapshots e frontend.

## Capabilities

### New Capabilities

Nenhuma. A rentabilidade atual por moeda integra a capability existente de resumo da Carteira.

### Modified Capabilities

- `portfolio-summary`: acrescentar a rentabilidade percentual atual por moeda ao resumo, sua fórmula, política numérica, invariantes, cenários de ciclo e restrições de arquitetura.

## Impact

- API: alteração aditiva de `ResumoMoedaResponse` em `GET /carteiras/{carteiraId}/resumo`; nenhum endpoint novo.
- Domínio financeiro: reutilização dos acumulados oficiais do resumo e da política percentual já aprovada para posições.
- Arquitetura: extrair a política hoje em `CalculadoraPosicao` para `CalculadoraRentabilidade`, componente puro compartilhado por posição individual e resumo, sem mudança dos resultados existentes.
- Componentes esperados: `CalculadoraPosicao`, `PosicaoService`, `AgregadorPosicoesPorMoeda`, `ResumoCarteiraService`, `ResumoCarteiraMapper`, DTOs e testes relacionados.
- Persistência e integrações: nenhuma entidade, tabela, migration, repository, dependência, provider, adapter, chamada externa ou escrita adicional.
