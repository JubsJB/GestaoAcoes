## 1. Preparação

- [x] 1.1 Revisar os testes atuais de `CarteiraService`, `CarteiraResource` e `CarteiraRepository` para reutilizar fixtures e preservar os contratos existentes.

## 2. Serviço e persistência

- [x] 2.1 Adicionar ao `CarteiraService` um método de exclusão `void`, transacional para escrita, que localize a Carteira por ID e reutilize `ObjectNotFoundException` quando ausente.
- [x] 2.2 Excluir a entidade localizada com `CarteiraRepository.delete(carteira)`, sem usar `Clock`, mapper, nova camada de persistência ou alterações em outros registros.
- [x] 2.3 Adicionar testes unitários do service para exclusão válida, ID inexistente, ausência de escrita quando ausente, uso de `delete` somente na entidade localizada e ausência de uso de `Clock`.

## 3. Contrato REST e erros

- [x] 3.1 Adicionar `DELETE /carteiras/{id}` ao `CarteiraResource`, sem request body, retornando `204 No Content` sem corpo e sem `Location`.
- [x] 3.2 Adicionar testes do resource para sucesso `204`, corpo vazio, ausência de `Location`, `404 StandardError` para ID inexistente e `404` na segunda exclusão sequencial.
- [x] 3.3 Verificar por testes que o endpoint reutiliza o tratamento centralizado de `ObjectNotFoundException` e não cria novo padrão de erro.

## 4. Integração com banco e isolamento

- [x] 4.1 Adicionar teste de repository no H2 que persista uma Carteira, execute a exclusão e confirme que o registro foi fisicamente removido.
- [x] 4.2 Adicionar teste de repository que exclua uma Carteira e confirme a preservação integral das demais Carteiras.
- [x] 4.3 Verificar nos testes de integração que Liquibase cria e Hibernate valida o schema vigente sem alteração em `Carteira`, `003-create-carteira.yaml` ou no changelog master.
- [x] 4.4 Adicionar cobertura de rollback/atomicidade no nível tecnicamente adequado, confirmando que uma falha de persistência não altera outras Carteiras.

## 5. Regressão e limites de escopo

- [x] 5.1 Executar e, somente se necessário para cobertura de regressão, complementar testes que preservem `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}` e `PATCH /carteiras/{id}`.
- [x] 5.2 Confirmar que a implementação não adiciona Operação, posição, cálculo financeiro, histórico, snapshot, cascade delete, soft delete, frontend, dependência, configuração ou mecanismo concorrente.
- [x] 5.3 Confirmar que nenhuma proteção artificial para Operações foi implementada e que a restrição normativa futura permanece documentada sem antecipar exception ou código de erro.

## 6. Verificação final

- [x] 6.1 Executar os testes automatizados relevantes de Carteira e corrigir somente regressões relacionadas a esta change.
- [x] 6.2 Executar a suíte completa do projeto e confirmar que Liquibase/Hibernate inicializam corretamente no H2.
- [x] 6.3 Validar `excluir-carteira` com OpenSpec em modo strict e validar também o conjunto global em modo strict.
- [x] 6.4 Atualizar o Graphify com `graphify update .` após as alterações de código e revisar o diff final para confirmar o escopo aprovado.
