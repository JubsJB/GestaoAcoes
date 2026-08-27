## Why

O sistema preserva apenas a última cotação conhecida em `Acao`, embora o PRD já identifique Histórico de Cotação como entidade candidata. Registrar cada estado de mercado efetivamente aceito permitirá fundamentar futuras capabilities de snapshot e evolução patrimonial sem confundir cotação com o preço imutável das Operações.

## What Changes

- Introduzir persistência append-only de cotações historicamente aplicáveis por Ação, mantendo `Acao.cotacaoAtual` como fonte do estado corrente.
- Fazer cadastro e atualização de cotação persistirem estado atual e observação histórica de forma atômica, sem envolver providers dentro da transação.
- Preservar a política atual de ticker, mercado, moeda, precisão `NUMERIC(19,6)`, timestamp UTC e monotonicidade.
- Criar uma nova tabela por novo changeSet Liquibase, sem modificar os changeSets 001–004.
- Manter fora desta change consultas públicas do histórico, backfill, reconstrução retroativa, consulta de anos anteriores, preenchimento de pregões ausentes, scheduler, coleta automática, snapshots e valuation histórico.
- Preservar integralmente Operações, posição atual, patrimônio, resumo e resultado realizado.

## Capabilities

### New Capabilities

- `stock-quote-history`: Persistência temporal append-only das cotações aceitas para cada Ação, com consistência transacional, ordenação e constraints mínimas.

### Modified Capabilities

- `stock-registration`: Cadastro e PATCH de cotação passam a registrar atomicamente a observação histórica correspondente quando uma cotação é efetivamente persistida como estado atual.

## Impact

- Futuramente afetará entidade e repository próprios para histórico, serviços de persistência de Ação/cotação e um novo changeSet Liquibase.
- Não altera contratos REST atuais nem adiciona endpoint nesta proposta preliminar.
- Não altera providers/adapters nem adiciona dependências.
- As decisões de captura, unicidade, consulta, atomicidade e concorrência estão aprovadas e consolidadas no design e nas delta specs.
