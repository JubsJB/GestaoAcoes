## ADDED Requirements

### Requirement: Proteger snapshots patrimoniais na exclusão da Carteira
Uma Carteira que possua ao menos um snapshot patrimonial SHALL ser inelegível para exclusão física. `DELETE /carteiras/{id}` SHALL responder `409 Conflict` com código `CARTEIRA_POSSUI_SNAPSHOTS`, preservar a Carteira e todos os snapshots e MUST NOT usar cascade delete. A proteção vigente para Carteira com Operações SHALL permanecer inalterada.

#### Scenario: Carteira sem Operações mas com snapshot vazio
- **WHEN** uma Carteira possui snapshot, ainda que sem componentes monetários, e sua exclusão é solicitada
- **THEN** o sistema responde `409 Conflict` com `CARTEIRA_POSSUI_SNAPSHOTS` e não remove registro algum

#### Scenario: Carteira com Operações e snapshots
- **WHEN** uma Carteira possui Operações e snapshots
- **THEN** a verificação vigente de Operações ocorre primeiro e responde `409 Conflict` com `CARTEIRA_POSSUI_OPERACOES`
- **AND** nenhum snapshot, Operação ou Carteira é removido

#### Scenario: Carteira sem Operações ou snapshots
- **WHEN** a Carteira existente não possui Operações nem snapshots
- **THEN** o contrato vigente permanece `204 No Content`, sem corpo ou `Location`
