## Why

O RF10 exige consultar uma Ação por ID ou ticker, mas a API atual cobre apenas a consulta por ID. Como a identidade persistida é composta por ticker normalizado e mercado, a nova consulta singular precisa usar ambos para completar o requisito sem ambiguidade.

## What Changes

- Adicionar `GET /acoes/por-ticker?ticker={ticker}&mercado={mercado}` como consulta read-only singular pelo par `ticker + mercado`, operando exclusivamente sobre dados persistidos e sem aliases.
- Aceitar ticker conforme a normalização já consolidada: `trim`, conversão para maiúsculas com `Locale.ROOT`, preservação de caracteres internos e limite de 30 caracteres.
- Exigir `mercado` com os valores vigentes `BRASIL` ou `EUA`, sem escolher mercado padrão quando o mesmo ticker puder existir nos dois mercados.
- Responder com o `AcaoResponse` completo, usar o tratamento centralizado de entrada inválida e retornar `404 Not Found` para combinação válida inexistente.
- Reutilizar `AcaoResource`, `AcaoService`, `TickerNormalizer`, `AcaoRepository.findByTickerAndMercado` e `AcaoMapper`.
- Preservar `POST /acoes`, `GET /acoes`, `GET /acoes/{id}` e `PATCH /acoes/{id}/cotacao` sem alteração comportamental.
- Manter a consulta sem providers, escrita, locks, migration, entidade, configuração ou dependência nova.
- Tratar ticker ausente ou inválido com `400 / TICKER_INVALIDO` e mercado ausente ou inválido com `400 / REQUEST_INVALIDO`, reutilizando os códigos existentes.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `stock-registration`: completar a consulta de Ação persistida com busca singular pelo ticker normalizado e mercado, conforme o RF10 e a identidade composta vigente.

## Impact

- Futura alteração limitada ao endpoint de leitura em `AcaoResource`, ao método read-only em `AcaoService` e aos testes existentes de Ação; `AcaoRepository.findByTickerAndMercado` já atende à persistência.
- Nenhuma chamada à BRAPI, Alpha Vantage ou outro provider; nenhuma atualização de ticker, nome, moeda, cotação ou timestamp.
- Nenhuma mudança de schema: `uk_acao_ticker_mercado` já garante a identidade singular e fornece suporte à busca pelo par.
- A implementação futura deverá permanecer restrita ao fluxo aprovado e às respectivas validações e testes.
