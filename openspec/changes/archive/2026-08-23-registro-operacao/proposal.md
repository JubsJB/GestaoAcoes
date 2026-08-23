## Why

O sistema já persiste Carteiras, Ações e Corretoras, mas ainda não registra as movimentações que formam o histórico financeiro previsto pelo PRD. Esta change introduz a primeira fatia de Operações de COMPRA e VENDA, preservando o preço efetivamente negociado e impedindo que uma venda ou a exclusão de uma Carteira torne o histórico inconsistente.

## What Changes

- Adicionar `POST /operacoes` para registrar uma Operação associada obrigatoriamente a uma Carteira existente e a uma Ação localizada por ticker normalizado + mercado, com Corretora existente opcional.
- Introduzir o modelo persistente mínimo de Operação, o enum `TipoOperacao` (`COMPRA`, `VENDA`), DTOs, mapper, service, repository e resource nos padrões atuais do projeto.
- Validar quantidade e preço unitário positivos com representação decimal exata, exigindo quantidade inteira para `BRASIL` e permitindo até seis casas decimais para `EUA`; calcular `valorTotal = quantidade × precoUnitario` sem usar cotação atual ou histórica e persistir a criação atomicamente.
- Validar VENDA pela reprodução cronológica das Operações da mesma Carteira e Ação, sem criar posição consolidada, rejeitando a nova Operação quando qualquer saldo cronológico se tornar negativo.
- Registrar `dataOperacao` como data civil e validar futuro pela zona do mercado (`America/Sao_Paulo` para `BRASIL` e `America/New_York` para `EUA`), usando `Clock` testável sem fabricar horário persistido.
- Exigir `ordemNoDia` positiva e única por Carteira, Ação e data, usando `dataOperacao ASC, ordemNoDia ASC` como ordem financeira sem recorrer ao ID.
- Evoluir o schema com `004-create-operacao.yaml`, FKs sem cascade delete, checks, índices e rollback explícito, mantendo Liquibase seguido por `ddl-auto=validate` em PostgreSQL e H2.
- Tornar efetiva a proteção já prevista para `DELETE /carteiras/{id}`: Carteira com uma ou mais Operações não poderá ser excluída, deverá responder `409 Conflict` e deverá preservar todo o histórico.
- Manter Ação e Corretora inalteradas durante o registro e impedir cadastro automático de Ação ou revalidação externa de Corretora.
- Manter a cotação histórica em capability/consulta auxiliar futura e separada do POST; nenhuma chamada histórica, persistência de cotação ou bloqueio por indisponibilidade de provider integra esta change.
- Manter fora do escopo posição persistida, preço médio consolidado, resultados, patrimônio, snapshots, histórico de cotações e frontend.

## Capabilities

### New Capabilities

- `operation-registration`: contrato REST, validações, relacionamentos, cronologia, posição disponível derivada, persistência e atomicidade do registro de COMPRA e VENDA.

### Modified Capabilities

- `portfolio-deletion`: transforma a restrição futura já promovida em bloqueio efetivo de exclusão para Carteiras que possuam Operações.

## Impact

- API: novo `POST /operacoes` com `201 Created`, `OperacaoResponse` e `Location: /operacoes/{id}`; `DELETE /carteiras/{id}` passa a responder `409 Conflict` quando houver histórico.
- Domínio e aplicação: novos componentes de Operação; reutilização de `CarteiraRepository`, `AcaoRepository`, `CorretoraRepository`, `TickerNormalizer`, `ApiException`, `ErrorCodes`, `StandardError` e `ResourceExceptionHandler`.
- Persistência: nova tabela `operacao`, novo método de busca de Ação por ticker + mercado, consultas cronológicas e de existência de Operações e coordenação concorrente sobre a Carteira; nenhuma coleção bidirecional ou cascade nos agregados existentes.
- Banco: novo changeSet `004-create-operacao.yaml` incluído ao final do master, sem editar os changeSets 001, 002 ou 003 e sem alterar dependências ou `ddl-auto`.
- Testes: unidade, HTTP, repository/H2, constraints, rollback, concorrência, cronologia, proteção de exclusão e regressão integral de Carteira, Ação e Corretora.
- Integrações: BRAPI e Alpha Vantage atuais permanecem inalteradas nesta change; a consulta histórica será objeto de capability futura e separada.
