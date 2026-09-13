## Why

O cadastro EUA consome duas chamadas no caminho normal e até três quando falta nome na busca. Precisamos limitar o consumo por tentativa sem aceitar identidade, nome ou cotação inválidos (PRD RF07, RF08 e RF12).

## What Changes

- Corrigir o burst comprovado na homologação: SYMBOL_SEARCH retornou 200/bestMatches e GLOBAL_QUOTE, cerca de 156 ms depois, retornou 200/Information pedindo 1 request por segundo. Inserir intervalo configurável de 1100 ms antes da cotação, sem retry e sem alterar a quota diária do provider.

- Reutilizar identidade, região, moeda e nome de SYMBOL_SEARCH e consultar GLOBAL_QUOTE uma única vez.
- Remover o fallback OVERVIEW: nome ausente encerra com DADOS_EXTERNOS_INCOMPLETOS antes da cotação.
- Provar o orçamento de chamadas desde POST /acoes com HTTP externo simulado, inclusive falhas 429 sem retry.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `alpha-vantage-consumption`: limitar cadastro a duas requests e interromper imediatamente em erro.
- `stock-registration`: rejeitar busca EUA sem nome utilizável sem consulta complementar.

## Impact

Somente adapter Alpha Vantage, testes e esta change. Sem alteração de BRAPI, contratos públicos, regras financeiras, frontend, banco, Docker ou refresh. Rate limit real permanece 429; a otimização não recupera cota externa esgotada.
