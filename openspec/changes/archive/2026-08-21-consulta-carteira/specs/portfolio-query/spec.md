## Purpose

Definir a leitura REST das Carteiras persistidas, com ordenação determinística, representação fiel e ausência de efeitos colaterais sobre os dados existentes.

## ADDED Requirements

### Requirement: Listagem determinística das Carteiras persistidas
O sistema SHALL expor `GET /carteiras`, SHALL responder `200 OK` e SHALL retornar todas as Carteiras persistidas como uma lista de `CarteiraResponse`, ordenada por `id` em ordem crescente. Cada item SHALL conter `id`, `nome` e `dataCriacao` correspondentes ao registro persistido.

#### Scenario: Listagem com múltiplas Carteiras
- **WHEN** existem Carteiras persistidas e o cliente solicita `GET /carteiras`
- **THEN** o sistema responde `200 OK` com todos os registros ordenados por `id ASC`, independentemente da ordem de inserção ou recuperação do banco

#### Scenario: Listagem sem Carteiras
- **WHEN** não existe Carteira persistida e o cliente solicita `GET /carteiras`
- **THEN** o sistema responde `200 OK` com o array vazio `[]`

### Requirement: Consulta individual de Carteira por identificador
O sistema SHALL expor `GET /carteiras/{id}` e SHALL responder `200 OK` com o `CarteiraResponse` completo quando o identificador corresponder a uma Carteira persistida. Quando o identificador não existir, o sistema SHALL responder `404 Not Found` no formato centralizado de erros vigente.

#### Scenario: Carteira existente
- **WHEN** o cliente solicita `GET /carteiras/{id}` com o identificador de uma Carteira persistida
- **THEN** o sistema responde `200 OK` com `id`, `nome` e `dataCriacao` do registro encontrado

#### Scenario: Carteira inexistente
- **WHEN** o cliente solicita `GET /carteiras/{id}` com um identificador sem Carteira correspondente
- **THEN** o sistema responde `404 Not Found` no formato padronizado atual e não cria nem altera registro algum

### Requirement: Leitura fiel e sem efeitos colaterais
As consultas de Carteira SHALL usar exclusivamente os dados já persistidos e MUST NOT modificar `nome`, `dataCriacao` ou qualquer outro estado da Carteira. As consultas MUST NOT criar relacionamentos, operações, posições, snapshots ou histórico e SHALL preservar o comportamento existente de `POST /carteiras`.

#### Scenario: Preservação dos dados consultados
- **WHEN** uma Carteira é retornada pela listagem ou pela consulta individual
- **THEN** o `CarteiraResponse` reflete o `id`, o `nome` e a `dataCriacao` persistidos sem recalcular, normalizar ou substituir esses valores

#### Scenario: Consultas consecutivas sem mutação
- **WHEN** o cliente executa uma ou mais consultas de Carteira
- **THEN** os registros persistidos permanecem inalterados e nenhuma funcionalidade externa ou de domínio futuro é acionada

#### Scenario: Compatibilidade com a criação existente
- **WHEN** a funcionalidade de consulta é disponibilizada
- **THEN** `POST /carteiras` mantém seu contrato, validações, persistência, resposta `201 Created` e header `Location` existentes
