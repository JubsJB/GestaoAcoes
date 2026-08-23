## ADDED Requirements

### Requirement: Bloqueio efetivo de exclusão de Carteira com Operações
Com a introdução da persistência de Operações, o sistema SHALL considerar inelegível para exclusão física qualquer Carteira que possua uma ou mais Operações. `DELETE /carteiras/{id}` SHALL responder `409 Conflict` no formato `StandardError` com código `CARTEIRA_POSSUI_OPERACOES`, MUST preservar a Carteira e todas as Operações e MUST NOT usar cascade delete. Carteira sem Operações SHALL conservar o contrato vigente de exclusão com `204 No Content`. A exclusão e o registro concorrente de Operações SHALL ser coordenados pelo mesmo lock pessimista curto sobre a Carteira, limitado à transação de banco necessária e sem `@Version`.

#### Scenario: Carteira sem Operações
- **WHEN** o cliente exclui uma Carteira existente que não possui Operações
- **THEN** o sistema remove fisicamente somente a Carteira e responde `204 No Content` sem corpo ou `Location`

#### Scenario: Carteira com histórico financeiro
- **WHEN** o cliente tenta excluir uma Carteira que possui ao menos uma Operação
- **THEN** o sistema responde `409 Conflict` com código `CARTEIRA_POSSUI_OPERACOES` e não remove Carteira nem Operação

#### Scenario: Criação de Operação concorrente com exclusão
- **WHEN** criação de Operação e exclusão disputam a mesma Carteira
- **THEN** o resultado transacional preserva a integridade: ou a exclusão conclui antes e a criação encontra Carteira ausente, ou a Operação conclui e a exclusão é recusada, sem Operação órfã ou histórico apagado

#### Scenario: Exclusão de Carteira inexistente
- **WHEN** o cliente solicita a exclusão de um ID sem Carteira correspondente
- **THEN** o sistema mantém o contrato vigente de `404 Not Found` no formato `StandardError`
