# portfolio-deletion Specification

## Purpose

Definir a exclusão REST explícita, atômica e segura de uma Carteira persistida, preservando os demais registros e impedindo que a evolução futura apague histórico financeiro por cascata.

## Requirements

### Requirement: Contrato REST de exclusão de Carteira
O sistema SHALL expor `DELETE /carteiras/{id}` e, quando o identificador corresponder a uma Carteira existente e elegível, SHALL excluir o recurso e responder `204 No Content`, sem corpo de resposta e sem header `Location`.

#### Scenario: Exclusão concluída
- **WHEN** o cliente solicita a exclusão de uma Carteira existente e elegível
- **THEN** o sistema exclui a Carteira e responde `204 No Content` sem corpo

#### Scenario: Contrato sem payload de entrada ou saída
- **WHEN** o cliente solicita `DELETE /carteiras/{id}`
- **THEN** a operação não exige corpo de requisição e uma conclusão bem-sucedida não devolve representação da Carteira

### Requirement: Carteira inexistente e repetição da exclusão
Quando o identificador informado não corresponder a uma Carteira persistida, o sistema SHALL responder `404 Not Found` no formato `StandardError` vigente e MUST NOT criar, excluir ou alterar qualquer registro. A exclusão repetida após um primeiro sucesso SHALL seguir o mesmo comportamento de recurso inexistente. A operação SHALL permanecer idempotente quanto aos efeitos sobre o estado persistido, embora a primeira resposta seja `204 No Content` e as respostas sequenciais posteriores sejam `404 Not Found`.

#### Scenario: Identificador nunca persistido
- **WHEN** o cliente solicita a exclusão de um ID sem Carteira correspondente
- **THEN** o sistema responde `404 Not Found` no formato `StandardError` vigente e não altera registro algum

#### Scenario: Segunda exclusão do mesmo identificador
- **WHEN** uma primeira exclusão respondeu `204 No Content` e o cliente repete a exclusão do mesmo ID
- **THEN** o sistema responde `404 Not Found` no formato `StandardError` vigente e não produz efeito adicional

### Requirement: Exclusão física isolada no modelo atual
Enquanto a Carteira persistir somente `id`, `nome` e `dataCriacao` e não possuir Operações associadas, o sistema SHALL realizar exclusão física exclusivamente da Carteira identificada. A exclusão MUST NOT alterar ou excluir outras Carteiras e MUST NOT criar exclusão lógica, relacionamentos, Operações, posições, histórico ou snapshots.

#### Scenario: Remoção efetiva do registro
- **WHEN** uma Carteira existente e elegível é excluída com sucesso
- **THEN** o registro deixa de existir na persistência e consultas posteriores por seu ID respondem `404 Not Found`

#### Scenario: Preservação das demais Carteiras
- **WHEN** uma Carteira é excluída e existem outras Carteiras persistidas
- **THEN** todas as demais Carteiras preservam exatamente seus identificadores, nomes e datas de criação

### Requirement: Preservação futura de Operações e histórico financeiro
Quando a capability de Operações introduzir associação persistida entre Operação e Carteira, o sistema MUST considerar inelegível para exclusão física qualquer Carteira que possua ao menos uma Operação, MUST preservar todo o histórico financeiro e MUST NOT utilizar exclusão em cascata para remover Operações. Essa verificação, sua exception e seu código de erro SHALL ser definidos e adicionados juntamente com a associação de Operações; esta change MUST NOT criar tabela, relacionamento, repository, service, exception, consulta ou proteção artificial para antecipá-los.

#### Scenario: Estado atual sem modelo de Operações
- **WHEN** esta capability é implementada antes da existência da associação persistida com Operações
- **THEN** o sistema implementa somente a exclusão das Carteiras atualmente elegíveis, sem estruturas artificiais para simular Operações

#### Scenario: Evolução futura com Operações associadas
- **WHEN** a associação com Operações existir e uma Carteira possuir ao menos uma Operação
- **THEN** a exclusão física será recusada com erro de negócio apropriado, recomendado como `409 Conflict`, e nenhuma Carteira ou Operação será removida

### Requirement: Atomicidade e compatibilidade dos contratos existentes
A exclusão SHALL ser atômica e MUST NOT utilizar a data ou o relógio da aplicação. Uma falha antes da conclusão MUST preservar o estado persistido. Os contratos existentes de criação, listagem, consulta individual e atualização de nome SHALL permanecer inalterados.

#### Scenario: Falha durante a exclusão
- **WHEN** a persistência não consegue concluir a exclusão
- **THEN** a transação não deixa remoção parcial nem alteração em qualquer Carteira

#### Scenario: Regressão dos endpoints existentes
- **WHEN** a capability de exclusão é disponibilizada
- **THEN** `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}` e `PATCH /carteiras/{id}` mantêm seus status, payloads, validações e efeitos vigentes

### Requirement: Compatibilidade com o schema vigente
A exclusão de Carteira SHALL operar sobre o schema atual e MUST NOT exigir alteração da entidade Carteira, do changeSet `003-create-carteira.yaml`, do changelog master, das dependências ou das configurações. Liquibase e Hibernate SHALL continuar inicializando e validando o mesmo schema em PostgreSQL e H2.

#### Scenario: Inicialização e exclusão no H2
- **WHEN** os testes iniciam o banco H2 pelo changelog Liquibase vigente e o Hibernate valida os mapeamentos
- **THEN** uma Carteira pode ser persistida e excluída sem migration ou ajuste de schema

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
