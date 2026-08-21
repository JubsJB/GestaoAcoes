## ADDED Requirements

### Requirement: Listagem das corretoras persistidas
O sistema SHALL expor `GET /corretoras` e SHALL responder `200 OK` com um array contendo os dados completos já persistidos de cada corretora. A listagem SHALL ser ordenada de forma determinística por `id` em ordem crescente e SHALL NOT exigir paginação nesta primeira fatia.

#### Scenario: Listagem com corretoras cadastradas
- **WHEN** existem corretoras persistidas e o cliente solicita `GET /corretoras`
- **THEN** o sistema retorna `200 OK` com todas as corretoras como DTOs completos, ordenadas por `id` crescente

#### Scenario: Listagem sem corretoras cadastradas
- **WHEN** não existe nenhuma corretora persistida e o cliente solicita `GET /corretoras`
- **THEN** o sistema retorna `200 OK` com um array vazio

#### Scenario: Campos opcionais ausentes na listagem
- **WHEN** uma corretora listada possui campos opcionais sem valor
- **THEN** o DTO mantém esses campos com valor nulo sem omitir ou impedir a corretora de integrar a resposta

### Requirement: Consulta de corretora persistida por ID
O sistema SHALL expor `GET /corretoras/{id}` para recuperar uma corretora já persistida e SHALL devolver seus dados completos no mesmo contrato de resposta utilizado pelo cadastro.

#### Scenario: Consulta por ID existente
- **WHEN** o cliente solicita `GET /corretoras/{id}` com o identificador de uma corretora persistida
- **THEN** o sistema retorna `200 OK` com o DTO completo da corretora correspondente

#### Scenario: Consulta por ID inexistente
- **WHEN** o cliente solicita `GET /corretoras/{id}` com um identificador que não corresponde a uma corretora persistida
- **THEN** o sistema retorna `404 Not Found` no formato padronizado atual de erros da API

### Requirement: Consultas sem revalidação externa
As operações `GET /corretoras` e `GET /corretoras/{id}` SHALL usar exclusivamente os dados persistidos e MUST NOT consultar BrasilAPI, ViaCEP ou qualquer outro serviço externo.

#### Scenario: Listagem independente das integrações
- **WHEN** o cliente solicita a listagem de corretoras
- **THEN** o resultado é obtido do banco de dados sem chamada à BrasilAPI ou à ViaCEP

#### Scenario: Consulta individual independente das integrações
- **WHEN** o cliente solicita uma corretora persistida por ID, exista ela ou não
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BrasilAPI ou à ViaCEP
