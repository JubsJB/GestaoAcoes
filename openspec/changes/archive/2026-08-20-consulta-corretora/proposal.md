## Why

As corretoras já podem ser cadastradas e persistidas, mas ainda não existe uma API para recuperar esses registros. Esta change completa a primeira capacidade de leitura prevista no PRD, permitindo listar corretoras e consultar uma corretora por seu identificador sem depender novamente de serviços externos.

## What Changes

- Adicionar `GET /corretoras` para retornar as corretoras persistidas como uma lista de `CorretoraResponse`, sem paginação nesta primeira fatia.
- Definir ordenação padrão e determinística da listagem por `id` em ordem crescente e retornar `200 OK` com lista vazia quando não houver registros.
- Adicionar `GET /corretoras/{id}` para retornar a corretora persistida correspondente ao identificador informado.
- Responder `404 Not Found` no formato padronizado atual quando o identificador não existir.
- Reutilizar entidade, DTO de resposta, mapper, service, repository e tratamento centralizado de erros introduzidos pela change `cadastro-corretora`.
- Garantir que as duas consultas leiam somente o banco de dados e não acionem BrasilAPI, ViaCEP ou qualquer nova integração externa.
- Criar testes unitários e de integração para listagem, ordenação, lista vazia, consulta por ID, ID inexistente e ausência de chamadas externas.
- Manter fora desta change cadastro, consulta por CNPJ, paginação, atualização, exclusão, revalidação externa, Ação, Carteira, Operação, preço médio, frontend e demais funcionalidades não relacionadas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `broker-registration`: ampliar a capability existente de Corretora com listagem dos registros persistidos e consulta individual por ID, sem revalidação externa.

## Impact

- Ampliação de `CorretoraResource` e `CorretoraService` com operações de leitura.
- Reutilização das operações de consulta e ordenação já oferecidas por `CorretoraRepository`/Spring Data JPA e do mapeamento de `Corretora` para `CorretoraResponse`.
- Ampliação dos testes de service, repository e endpoint, preservando os testes do cadastro e da baseline.
- Nenhuma alteração de entidade, tabela, changelog Liquibase, configuração de banco, dependência ou integração externa.
