## Why

A spec principal `stock-registration` exige a atualização sob demanda da cotação de uma Ação, mas a implementação correspondente ficou isolada no commit órfão `999fc4aba4a083a6d2d2f5488ca61a5d2c44764e` e não integra a baseline atual. Esta change restaura a conformidade entre código e contrato aprovado sem modificar requisitos funcionais nem regredir as evoluções posteriores de Carteira, Operações e posição consolidada.

## What Changes

- Restaurar `PATCH /acoes/{id}/cotacao`, sem body, com `200 OK`, `AcaoResponse` completo e sem `Location`; rejeitar body não vazio com `400 / REQUEST_INVALIDO`.
- Localizar a Ação antes de consultar o provider e selecionar BRAPI ou Alpha Vantage exclusivamente pelo mercado persistido.
- Restaurar uma mutação de domínio restrita a `cotacaoAtual` e `dataHoraCotacao`, preservando todos os dados cadastrais.
- Separar a chamada HTTP externa da transação curta de escrita, relendo a Ação com `PESSIMISTIC_WRITE` apenas na persistência final.
- Impedir regressão temporal: aplicar somente timestamp estritamente posterior e retornar o estado persistido para timestamp igual ou anterior.
- Restaurar `409 / TICKER_CANONICO_DIVERGENTE` e preservar/enriquecer a última cotação válida nos erros previstos pela spec principal.
- Reutilizar providers, adapters, validações e infraestrutura de erros atuais, sem alterar schema, Liquibase, dependências, Operações ou posição consolidada.
- Recriar a cobertura automatizada do contrato, concorrência e regressões sobre a baseline atual.

## Capabilities

### New Capabilities

Nenhuma. A capability já está definida em `openspec/specs/stock-registration/spec.md`.

### Modified Capabilities

Nenhuma. Não há mudança comportamental no contrato aprovado; esta é uma restauração de conformidade de implementação. Por isso, a change declara `skip_specs: true` e não cria delta spec.

## Impact

- API: restauração do endpoint em `AcaoResource`.
- Domínio e aplicação: adaptação de `Acao`, `AcaoService` e componente transacional dedicado à persistência da cotação.
- Persistência: adição de busca por ID com lock em `AcaoRepository`, preservando os métodos atuais, inclusive `findByTickerAndMercado`.
- Integrações e erros: reutilização de `CotacaoProvider`, `BrapiAdapter`, `AlphaVantageAdapter`, `ExternalApiErrorMapper`, `ErrorCodes` e `ResourceExceptionHandler`.
- Testes: domínio, service, persistência/lock, HTTP, concorrência, integrações simuladas e regressões de Ação, Operações, posição, Liquibase/Hibernate e suíte completa.
- Sem impacto em schema, migrations, dependências, novos providers ou contratos OpenSpec principais.
