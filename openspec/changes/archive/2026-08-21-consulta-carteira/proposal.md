## Why

O PRD prevê a consulta das Carteiras e a criação já disponibiliza uma identidade estável em `/carteiras/{id}`, mas a aplicação ainda não oferece leitura dos registros persistidos. Esta change completa a primeira fatia de consulta sem introduzir operações, posições ou alterações no estado das Carteiras.

## What Changes

- Adicionar `GET /carteiras` com `200 OK`, corpo composto por `CarteiraResponse`, ordenação determinística por `id ASC` e lista vazia quando não houver registros.
- Adicionar `GET /carteiras/{id}` com `200 OK` para uma Carteira existente e `404 Not Found` no padrão centralizado atual quando o identificador não existir.
- Reutilizar os componentes existentes de Carteira e consultar exclusivamente os dados persistidos, preservando `id`, `nome` e `dataCriacao` sem qualquer mutação.
- Adicionar testes proporcionais para service, repository e endpoints HTTP, preservando os testes e o comportamento de `POST /carteiras`.
- Não alterar entidade, schema, Liquibase, dependências, relacionamentos ou funcionalidades financeiras.

## Capabilities

### New Capabilities

- `portfolio-query`: define a listagem determinística e a consulta individual das Carteiras persistidas, incluindo lista vazia, resposta completa e tratamento de identificador inexistente.

### Modified Capabilities

Nenhuma. A capability de criação de Carteira não está presente em `openspec/specs` no estado atual; sua change arquivada permanece como fonte do contrato existente de `POST /carteiras`, que esta change deve preservar sem sincronização retroativa ou modificação.

## Impact

- API afetada: inclusão de `GET /carteiras` e `GET /carteiras/{id}` no resource existente.
- Componentes afetados: `CarteiraResource`, `CarteiraService` e seus testes; `CarteiraRepository`, `CarteiraMapper` e `CarteiraResponse` serão reutilizados sem necessidade de novos contratos ou consultas customizadas.
- Persistência: apenas leitura da tabela `carteira` existente, sem migração ou alteração de schema.
- Não há impacto em Ação, Corretora, Operação, integrações externas, cálculos financeiros, frontend ou dependências.
