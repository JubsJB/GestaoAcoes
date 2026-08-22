## 1. Revisão do estado atual e proteção de escopo

- [x] 1.1 Confirmar no código e no Graphify a estrutura atual de `CarteiraResource`, `CarteiraService`, `CarteiraRepository`, `CarteiraMapper`, `CarteiraResponse` e o padrão de consulta já usado por Corretora e Ação.
- [x] 1.2 Registrar o comportamento e os testes atuais de `POST /carteiras` e confirmar que entidade, DTO, Liquibase, schema, dependências, configurações e funcionalidades fora do escopo não precisarão ser alterados.

## 2. Operações de leitura no service

- [x] 2.1 Implementar `CarteiraService.listar()` com `@Transactional(readOnly = true)`, `findAll(Sort.by(Sort.Direction.ASC, "id"))` e conversão de todas as entidades por `CarteiraMapper.toResponse`.
- [x] 2.2 Implementar `CarteiraService.buscarPorId(Long id)` com `@Transactional(readOnly = true)`, `findById()` e `ObjectNotFoundException` específica para Carteira quando o registro não existir.
- [x] 2.3 Revisar os dois métodos para confirmar ausência de `Clock`, normalização, `save`, `saveAndFlush` ou qualquer mutação, preservando integralmente `cadastrar()`.

## 3. Endpoints HTTP de consulta

- [x] 3.1 Adicionar `GET /carteiras` ao `CarteiraResource`, reutilizando o service existente e retornando `ResponseEntity<List<CarteiraResponse>>` com `200 OK`.
- [x] 3.2 Adicionar `GET /carteiras/{id}` ao `CarteiraResource`, recebendo `Long id` por path e retornando `CarteiraResponse` com `200 OK` quando encontrado.
- [x] 3.3 Confirmar que os endpoints reutilizam `CarteiraResponse`, não expõem a entidade e não introduzem consulta por nome, filtros, paginação, atualização, exclusão ou outra rota fora do escopo.

## 4. Testes unitários do service

- [x] 4.1 Testar a listagem com registros, verificando o `Sort` por `id ASC`, a ordem retornada e o mapeamento completo para `CarteiraResponse`.
- [x] 4.2 Testar que a listagem sem registros retorna uma lista vazia.
- [x] 4.3 Testar a busca por ID existente, preservando exatamente `id`, `nome` e `dataCriacao` no DTO retornado.
- [x] 4.4 Testar a busca por ID inexistente com `ObjectNotFoundException` e verificar que os fluxos de leitura não invocam operações de escrita no repository.

## 5. Testes de persistência com H2

- [x] 5.1 Ajustar `CarteiraRepositoryTest` para validar `findAll(Sort)` por `id ASC` com múltiplos registros e o resultado vazio sem registros.
- [x] 5.2 Testar `findById()` para identificador existente e ausente usando o repository real.
- [x] 5.3 Confirmar que as consultas preservam `nome` e `dataCriacao` e executam com o changelog e o `ddl-auto=validate` existentes, sem alteração de schema.

## 6. Testes dos endpoints HTTP e regressão

- [x] 6.1 Testar `GET /carteiras` com múltiplos registros, `200 OK`, ordenação por `id ASC` e corpo completo.
- [x] 6.2 Testar `GET /carteiras` sem registros, confirmando `200 OK` e `[]`.
- [x] 6.3 Testar `GET /carteiras/{id}` existente, confirmando `200 OK` e preservação de `id`, `nome` e `dataCriacao`.
- [x] 6.4 Testar `GET /carteiras/{id}` inexistente, confirmando `404 Not Found` no `StandardError` centralizado.
- [x] 6.5 Testar que os GETs não alteram os registros e preservar/reexecutar todos os testes existentes de `POST /carteiras`, incluindo `201 Created`, DTO e `Location`.

## 7. Verificação final

- [x] 7.1 Executar os testes direcionados de `CarteiraServiceTest` pelo Maven Wrapper.
- [x] 7.2 Executar os testes direcionados de `CarteiraRepositoryTest` pelo Maven Wrapper.
- [x] 7.3 Executar os testes direcionados de `CarteiraResourceTest` pelo Maven Wrapper.
- [x] 7.4 Executar a suíte completa existente pelo Maven Wrapper e confirmar ausência de regressões no POST e nas demais funcionalidades.
- [x] 7.5 Executar `./mvnw.cmd clean verify` e confirmar compilação, testes, empacotamento, Liquibase e Hibernate `validate` no H2.
- [x] 7.6 Validar `consulta-carteira` e o conjunto global do OpenSpec em modo strict.
- [x] 7.7 Atualizar o Graphify após as alterações de código e confirmar que os GETs e as relações entre resource, service, repository e mapper estão representados.
- [x] 7.8 Executar `git diff --check`, revisar `git diff` e `git status` e confirmar que nenhuma alteração de entidade, DTO, Liquibase, schema, dependência, configuração ou funcionalidade fora do escopo foi introduzida.
