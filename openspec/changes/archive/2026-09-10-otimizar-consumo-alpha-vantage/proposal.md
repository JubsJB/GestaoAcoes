## Why

Atualizar uma acao americana repete busca de identidade e cotacao, consumindo 2-3 chamadas mesmo em intervalos curtos. Reduzir consumo sem perder a ultima cotacao valida.

## What Changes

- Reutilizar cotacao persistida recente (15 minutos configuraveis) em atualizacoes EUA.
- Atualizacao de acao ja validada consulta somente GLOBAL_QUOTE, reutilizando nome/moeda persistidos.
- Serializar atualizacoes EUA por ticker e reutilizar resultado/falha durante cooldown de 15 minutos na instancia.
- Manter timeout existente explicito: conexao 2s, leitura 5s; sem retry automatico.

## Capabilities

### New Capabilities
- `alpha-vantage-consumption`: limites de consumo em atualizacoes americanas e preservacao em falhas.

### Modified Capabilities

- `stock-registration`: qualificar selecao do provider em atualizacao EUA com reutilizacao/cooldown.

Nenhuma mudanca de DTO publico. A politica especifica desta change qualifica a consulta externa na atualizacao EUA; demais garantias de stock-registration permanecem.

## Impact

AcaoService, contrato interno CotacaoProvider, AlphaVantageAdapter, configuracao e testes backend. Sem migrations, dependencias, frontend ou formulas. Cadastro e fechamento historico preservam validacoes atuais.
