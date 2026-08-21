## 1. Camada de consulta

- [x] 1.1 Adicionar `AcaoRepository` às dependências de `AcaoService`, preservando as dependências e o comportamento atuais do cadastro.
- [x] 1.2 Implementar `AcaoService.listar()` como operação transacional somente para leitura, usando `findAll(Sort)` com `id ASC` e convertendo todas as entidades com `AcaoMapper`.
- [x] 1.3 Implementar `AcaoService.buscarPorId(Long id)` como operação transacional somente para leitura, usando `findById`, o mapper existente e `ObjectNotFoundException` com identificação do ID inexistente.
- [x] 1.4 Confirmar por revisão que os métodos de consulta acessam somente repository e mapper, sem chamar `CotacaoProvider.consultar`, `AcaoPersistenceService`, relógio ou qualquer fluxo de atualização.

## 2. Endpoints REST

- [x] 2.1 Adicionar `GET /acoes` ao `AcaoResource`, retornando `200 OK` com a lista completa de `AcaoResponse`, inclusive `[]` quando não houver registros.
- [x] 2.2 Adicionar `GET /acoes/{id}` ao `AcaoResource`, retornando `200 OK` com o `AcaoResponse` correspondente e delegando o caso inexistente ao tratamento centralizado de `ObjectNotFoundException`.
- [x] 2.3 Preservar sem alteração comportamental o `POST /acoes` e não adicionar consulta por ticker ou mercado, filtros, paginação, atualização ou exclusão.

## 3. Testes unitários do service

- [x] 3.1 Ajustar a fixture de `AcaoServiceTest` para fornecer o repository sem enfraquecer os testes existentes do cadastro.
- [x] 3.2 Testar a listagem com registros, verificando a solicitação de ordenação `id ASC`, o mapeamento de todos os itens e a preservação de ticker, nome, mercado, moeda, cotação e data/hora persistidos.
- [x] 3.3 Testar a listagem sem registros, confirmando o retorno de coleção vazia.
- [x] 3.4 Testar a consulta por ID existente e o mapeamento para o `AcaoResponse` completo.
- [x] 3.5 Testar a consulta por ID inexistente, confirmando o lançamento de `ObjectNotFoundException` com identificação do ID.
- [x] 3.6 Verificar explicitamente, nos cenários de listagem, ID existente e ID inexistente, que BRAPI e Alpha Vantage não recebem chamada a `consultar` e que não há persistência ou atualização.

## 4. Testes de persistência e HTTP

- [x] 4.1 Ampliar `AcaoRepositoryTest` sobre H2 para cobrir `findAll(Sort)` por `id ASC` com registros inseridos fora da ordem do identificador.
- [x] 4.2 Ampliar `AcaoRepositoryTest` para cobrir `findById` existente e inexistente, confirmando que os valores persistidos de cotação e data/hora permanecem inalterados após a leitura.
- [x] 4.3 Substituir o teste HTTP que atualmente espera ausência dos GETs por cenários de `GET /acoes` com DTOs completos em ordem crescente e `200 OK` com `[]`.
- [x] 4.4 Adicionar testes HTTP de `GET /acoes/{id}` para resposta completa com ID existente e `404 Not Found` no formato padronizado atual para ID inexistente.
- [x] 4.5 Preservar e reexecutar os testes HTTP de `POST /acoes`, mantendo os testes independentes de chamadas reais à BRAPI ou Alpha Vantage.

## 5. Verificações finais

- [x] 5.1 Executar pelo Maven Wrapper os testes direcionados de `AcaoService`, `AcaoRepository` e `AcaoResource`.
- [x] 5.2 Executar a suíte completa e o build pelo Maven Wrapper, confirmando também o carregamento do contexto com Liquibase e Hibernate sobre H2 e a preservação dos testes existentes.
- [x] 5.3 Revisar a implementação para confirmar que não houve alteração de entidade, schema, changelog Liquibase, dependências, configurações ou integrações externas.
- [x] 5.4 Validar a change `consulta-acao` com OpenSpec em modo strict e reconciliar qualquer divergência entre proposal, spec, design, tasks e implementação.
- [x] 5.5 Atualizar o Graphify após as futuras alterações de código e consultar o grafo para confirmar as novas relações de `AcaoResource.listar`, `AcaoResource.buscarPorId`, `AcaoService`, `AcaoRepository` e `AcaoMapper`.
- [x] 5.6 Executar `git diff --check`, revisar `git diff` e registrar o `git status` final sem realizar commit, push, merge, rebase ou alteração do histórico Git.
- [x] 5.7 Confirmar por revisão final que não foram implementados consulta por ticker ou mercado, filtros, paginação, atualização de cotação, atualização manual, histórico, DELETE, Carteira, Operação, preço médio, cálculos financeiros ou frontend.
