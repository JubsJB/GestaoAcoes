## 1. Camada de consulta

- [x] 1.1 Adicionar ao `CorretoraService` a listagem transacional somente para leitura, consultando `CorretoraRepository.findAll` com ordenação `id ASC` e convertendo todas as entidades com o `CorretoraMapper` existente.
- [x] 1.2 Adicionar ao `CorretoraService` a consulta transacional somente para leitura por ID, convertendo o resultado com o mapper existente e lançando `ObjectNotFoundException` quando não houver registro.
- [x] 1.3 Confirmar no fluxo implementado que listagem e consulta individual dependem somente do repository e do mapper, sem chamadas a `CnpjProvider`, `CepProvider`, `CnpjValidator` ou ao fluxo de cadastro.

## 2. Endpoints REST

- [x] 2.1 Adicionar `GET /corretoras` ao `CorretoraResource`, retornando `200 OK` com a lista completa de `CorretoraResponse`, inclusive `[]` quando não houver registros.
- [x] 2.2 Adicionar `GET /corretoras/{id}` ao `CorretoraResource`, retornando `200 OK` com o `CorretoraResponse` correspondente e delegando o caso inexistente ao tratamento centralizado atual de `ObjectNotFoundException`.
- [x] 2.3 Preservar sem alteração comportamental o `POST /corretoras` e não adicionar consulta por CNPJ, paginação, atualização ou exclusão.

## 3. Testes automatizados

- [x] 3.1 Criar testes unitários do service para listagem com registros, verificando ordenação solicitada ao repository e mapeamento de todos os itens.
- [x] 3.2 Criar teste unitário do service para listagem vazia com retorno de coleção vazia.
- [x] 3.3 Criar testes unitários do service para consulta por ID existente e para lançamento de `ObjectNotFoundException` quando o ID não existir.
- [x] 3.4 Verificar nos testes do service que BrasilAPI e ViaCEP não são acionadas durante listagem, consulta existente ou consulta inexistente.
- [x] 3.5 Ampliar os testes de repository sobre H2 para cobrir recuperação por ID e listagem por `id` ascendente usando as operações herdadas de `JpaRepository`.
- [x] 3.6 Ampliar os testes HTTP para cobrir `GET /corretoras` com DTOs completos em ordem crescente, campos opcionais nulos e resposta `200 OK` com array vazio.
- [x] 3.7 Ampliar os testes HTTP para cobrir `GET /corretoras/{id}` com registro existente e `404 Not Found` no formato padronizado atual para ID inexistente.
- [x] 3.8 Verificar nos testes HTTP que nenhum provider externo é acionado pelos GETs e manter todos os testes independentes de chamadas reais à BrasilAPI ou ViaCEP.

## 4. Verificações finais

- [x] 4.1 Executar os testes direcionados de service, repository e endpoint pelo Maven Wrapper.
- [x] 4.2 Executar a suíte completa e o build pelo Maven Wrapper, confirmando também o carregamento do contexto com Liquibase e Hibernate sobre H2.
- [x] 4.3 Revisar a implementação para confirmar que não houve alteração de entidade, schema, changelog Liquibase, dependências, configuração de banco ou integrações externas.
- [x] 4.4 Validar a change `consulta-corretora` com OpenSpec em modo strict.
- [x] 4.5 Atualizar o Graphify após as alterações de código e consultar o grafo para confirmar as novas relações dos endpoints de leitura.
- [x] 4.6 Executar `git diff --check`, revisar `git diff` e registrar o `git status` final sem realizar commit, push, merge, rebase ou alteração do histórico.
