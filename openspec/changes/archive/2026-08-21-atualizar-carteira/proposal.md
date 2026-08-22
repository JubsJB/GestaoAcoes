## Why

A aplicação já permite criar e consultar Carteiras, mas ainda não oferece uma operação para corrigir ou atualizar seu nome. Esta change adiciona essa capacidade preservando a identidade e a data de criação do recurso, sem antecipar relacionamentos ou funcionalidades financeiras.

## What Changes

- Adicionar uma atualização parcial de Carteira em `PATCH /carteiras/{id}`, aceitando exclusivamente um novo `nome` para um identificador existente.
- Responder `200 OK` com `CarteiraResponse` completo e sem header `Location`.
- Criar `CarteiraUpdateRequest` específico, restrito ao campo `nome`.
- Reutilizar integralmente a política de validação e normalização mínima do nome aprovada na criação de Carteira.
- Preservar `id` e `dataCriacao`, permitir nomes duplicados e manter inalterados os contratos existentes de criação e consulta.
- Adicionar `Carteira.atualizarNome(String)` como única nova mutação da entidade nesta change.
- Tratar o mesmo nome normalizado como sucesso idempotente, sem alterar `id` ou `dataCriacao`.
- Retornar o estado completo da Carteira após uma atualização bem-sucedida e usar o tratamento centralizado atual para Carteira inexistente ou request inválido.
- Adicionar testes unitários, de persistência e HTTP proporcionais à operação, sem alterar schema, Liquibase, dependências ou configurações.

## Capabilities

### New Capabilities

- `portfolio-update`: atualização exclusiva do nome de uma Carteira persistida, incluindo validações, preservação dos demais campos, resposta HTTP e tratamento de ausência.

### Modified Capabilities

- Nenhuma.

## Impact

- API REST: inclusão de `PATCH /carteiras/{id}` em `CarteiraResource`, com `200 OK`, `CarteiraResponse` completo e sem `Location`.
- Aplicação: novo `CarteiraUpdateRequest` e método transacional em `CarteiraService`, reutilizando `CarteiraRepository`, `CarteiraMapper` e `CarteiraResponse`.
- Domínio: novo método restrito `Carteira.atualizarNome(String)` para alterar somente `nome`.
- Testes: ampliação dos testes existentes de Carteira, com regressão dos contratos de `POST` e `GET`.
- Persistência e infraestrutura: nenhuma alteração de tabela, changelog, constraint, dependência ou configuração.
