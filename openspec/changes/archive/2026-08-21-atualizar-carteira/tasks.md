## 1. Proteção de escopo

- [x] 1.1 Registrar o estado inicial dos componentes de Carteira, dos testes, do schema, das configurações e do Git para proteger o escopo durante a implementação.

## 2. Contrato de entrada e domínio

- [x] 2.1 Criar `CarteiraUpdateRequest` contendo somente `nome` e rejeitando `id`, `dataCriacao` e propriedades desconhecidas.
- [x] 2.2 Aplicar ao request as validações de obrigatoriedade e máximo de 255 caracteres após `trim`, preservando espaços internos, acentos e caixa.
- [x] 2.3 Adicionar à entidade somente `Carteira.atualizarNome(String)` para substituir `nome`, sem setter genérico e sem permitir alteração de `id` ou `dataCriacao`.

## 3. Atualização transacional no service

- [x] 3.1 Implementar `CarteiraService.atualizar(Long id, CarteiraUpdateRequest request)` como transação de escrita, localizando a Carteira com `findById` e lançando `ObjectNotFoundException` quando ausente.
- [x] 3.2 Reutilizar `normalizeAndValidateName` para aplicar exatamente a política já usada no cadastro, sem utilizar `Clock` ou recalcular `dataCriacao`.
- [x] 3.3 Aplicar o nome normalizado pelo método restrito da entidade, permitindo nomes duplicados e tratando o mesmo nome como sucesso idempotente sem erro especial.
- [x] 3.4 Persistir com `saveAndFlush` e reutilizar `CarteiraMapper.toResponse` para devolver o estado efetivamente salvo.

## 4. Endpoint e tratamento de erros

- [x] 4.1 Adicionar ao `CarteiraResource` somente `PATCH /carteiras/{id}`, recebendo `Long id` por path e `CarteiraUpdateRequest` validado no corpo.
- [x] 4.2 Responder `200 OK` com `CarteiraResponse` completo e sem `Location`.
- [x] 4.3 Confirmar que nome inválido ou campo não permitido resulta em `400/REQUEST_INVALIDO` e ID inexistente resulta em `404` pelo `ResourceExceptionHandler` e `StandardError` existentes.
- [x] 4.4 Revisar o resource para confirmar que os contratos atuais de `POST` e `GET` não foram alterados e que nenhuma rota fora do escopo foi adicionada.

## 5. Testes unitários do request, domínio e service

- [x] 5.1 Testar o método de domínio restrito, confirmando alteração exclusiva de `nome` e preservação de `id` e `dataCriacao`.
- [x] 5.2 Testar atualização válida, `trim` nas extremidades e preservação de espaços internos, acentos e caixa.
- [x] 5.3 Testar rejeição de nome nulo, ausente, vazio, somente com espaços e acima de 255 caracteres após normalização.
- [x] 5.4 Testar nomes duplicados permitidos e o sucesso idempotente para nome normalizado igual ao persistido.
- [x] 5.5 Testar ID inexistente com `ObjectNotFoundException`, sem invocar persistência.
- [x] 5.6 Testar `saveAndFlush`, mapeamento completo, ausência de uso do `Clock` e preservação exata de `id` e `dataCriacao`.

## 6. Testes de persistência com H2

- [x] 6.1 Ajustar `CarteiraRepositoryTest` para atualizar somente `nome` e confirmar a preservação de `id` e `dataCriacao` no registro real.
- [x] 6.2 Testar que duas Carteiras podem permanecer com o mesmo nome após uma atualização, sem consulta ou constraint de unicidade.
- [x] 6.3 Confirmar que a atualização executa com o changelog Liquibase e o Hibernate `ddl-auto=validate` existentes, sem mudança de schema.

## 7. Testes HTTP e regressão

- [x] 7.1 Testar `PATCH /carteiras/{id}` com atualização válida, `200 OK`, `CarteiraResponse` completo e ausência de `Location`.
- [x] 7.2 Testar pelo endpoint o `trim` e a preservação de espaços internos, acentos e caixa.
- [x] 7.3 Testar `400/REQUEST_INVALIDO` para nome nulo, ausente, vazio, somente com espaços e acima de 255 caracteres.
- [x] 7.4 Testar rejeição de `id`, `dataCriacao` e propriedades desconhecidas no corpo.
- [x] 7.5 Testar ID inexistente com `404 Not Found` no formato centralizado atual.
- [x] 7.6 Testar nomes duplicados, mesmo nome e preservação de `id`, `dataCriacao` e dos demais registros.
- [x] 7.7 Preservar e reexecutar todos os testes existentes de `POST /carteiras`, `GET /carteiras` e `GET /carteiras/{id}`.

## 8. Verificação final

- [x] 8.1 Executar pelo Maven Wrapper os testes direcionados de request, entidade, mapper, service, repository e resource de Carteira.
- [x] 8.2 Executar a suíte completa existente pelo Maven Wrapper e confirmar ausência de regressões.
- [x] 8.3 Executar `./mvnw.cmd clean verify` e confirmar build, testes, Liquibase e Hibernate `validate` no H2.
- [x] 8.4 Validar `atualizar-carteira` e o conjunto global do OpenSpec em modo strict.
- [x] 8.5 Atualizar o Graphify após as alterações de código e confirmar a nova rota e as relações entre resource, service, entidade, repository e mapper.
- [x] 8.6 Executar `git diff --check`, revisar `git diff` e `git status` e confirmar ausência de mudanças em Liquibase, schema, dependências, configurações, `@Version`, locks ou funcionalidades fora do escopo.
