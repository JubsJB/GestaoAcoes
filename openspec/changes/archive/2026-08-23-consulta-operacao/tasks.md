## 1. Persistência de leitura

- [x] 1.1 Adicionar a `OperacaoRepository` a consulta derivada por `carteiraId` ordenada por `dataOperacao ASC`, `ordemNoDia ASC` e `id ASC`.
- [x] 1.2 Reutilizar `findAll(Sort)` e `findById` herdados para a listagem geral e a consulta individual, sem criar novo repository ou camada de persistência.
- [x] 1.3 Preservar sem alteração as consultas de existência, unicidade e replay usadas pelo cadastro e pelo DELETE protegido de Carteira.
- [x] 1.4 Confirmar que a consulta não requer entidade, migration, changeSet 005, alteração do 004, changelog, schema, dependência ou configuração.

## 2. Serviço de consulta de Operações

- [x] 2.1 Definir em `OperacaoService` a ordenação geral explícita por `dataOperacao`, `ordemNoDia` e `id`, todos ascendentes.
- [x] 2.2 Implementar `listar()` com `@Transactional(readOnly = true)`, `findAll(Sort)` e projeção de todos os registros para `OperacaoResponse`.
- [x] 2.3 Implementar `buscarPorId(Long id)` como leitura read-only, reutilizando `ObjectNotFoundException` para Operação inexistente.
- [x] 2.4 Implementar `listarPorCarteira(Long carteiraId)` como leitura read-only, validando primeiro a Carteira com `CarteiraRepository.findById` e retornando somente seu histórico ordenado.
- [x] 2.5 Executar `OperacaoMapper.toResponse` ainda dentro da transação read-only e preservar exatamente ticker, mercado, Corretora nullable, tipo, quantidade, preço, data, ordem e valor total persistidos.
- [x] 2.6 Garantir que os novos métodos não usem lock de escrita, `Clock`, normalização, replay, cálculo, persistência ou integração externa e não alterem o fluxo de `cadastrar`.

## 3. Contratos HTTP

- [x] 3.1 Adicionar somente `GET /operacoes` a `OperacaoResource`, retornando `200 OK` com lista completa ou `[]`.
- [x] 3.2 Adicionar somente `GET /operacoes/{id}` a `OperacaoResource`, retornando `200 OK` com DTO completo ou o 404 centralizado existente.
- [x] 3.3 Adicionar `GET /carteiras/{carteiraId}/operacoes` a `CarteiraResource`, delegando exclusivamente a `OperacaoService.listarPorCarteira`, sem duplicar regra em `CarteiraService`.
- [x] 3.4 Garantir que os GETs não aceitem corpo, não retornem `Location`, não criem DTO novo e preservem o formato `StandardError` vigente.
- [x] 3.5 Confirmar ausência de GET adicional, filtro, paginação, PATCH, PUT ou DELETE de Operação e preservar POST de Operação e todos os handlers existentes de Carteira.

## 4. Testes automatizados

- [x] 4.1 Testar `OperacaoRepository` no H2 para listagem geral determinística por data, ordem e ID, incluindo empate técnico entre grupos independentes e lista vazia.
- [x] 4.2 Testar no repository a consulta por Carteira com isolamento entre Carteiras, diferentes Ações, Corretora presente/ausente e ordenação cronológica completa.
- [x] 4.3 Testar `OperacaoService.listar()` com múltiplas COMPRA/VENDA, lista vazia e preservação da ordem recebida do repository.
- [x] 4.4 Testar `OperacaoService.buscarPorId()` para Operação existente e inexistente, verificando `ObjectNotFoundException` e ausência de escrita.
- [x] 4.5 Testar `OperacaoService.listarPorCarteira()` para Carteira com histórico, Carteira sem Operações, Carteira inexistente, isolamento e diferentes Ações na mesma Carteira.
- [x] 4.6 Testar que `OperacaoResponse` preserva `quantidade`, `precoUnitario`, `valorTotal`, ticker, mercado, tipo, data, ordem e `corretoraId` presente ou nulo, sem campos financeiros adicionais.
- [x] 4.7 Testar `GET /operacoes` com `200 OK`, array vazio, múltiplos itens, DTO completo e ordem `dataOperacao/ordemNoDia/id`.
- [x] 4.8 Testar `GET /operacoes/{id}` com `200 OK` e com `404 Not Found` no `StandardError` centralizado.
- [x] 4.9 Testar `GET /carteiras/{carteiraId}/operacoes` com histórico, `[]`, Carteira inexistente, isolamento entre Carteiras e diferentes Ações.
- [x] 4.10 Testar explicitamente ausência de save/delete, lock de escrita, uso de `Clock`, replay, atualização de cotação e chamadas a BRAPI, Alpha Vantage, BrasilAPI ou ViaCEP nos três fluxos.
- [x] 4.11 Executar e preservar os testes existentes de `POST /operacoes`, replay, concorrência, DELETE protegido de Carteira e APIs de Carteira, Ação e Corretora.
- [x] 4.12 Confirmar por testes de contexto/repository que Liquibase e Hibernate validam o schema 004 vigente em H2 sem migration adicional e preservando os relacionamentos.

## 5. Verificação e encerramento técnico

- [x] 5.1 Executar os testes direcionados de Operação e Carteira, incluindo service, resource, mapper e repository.
- [x] 5.2 Executar a suíte completa do projeto e confirmar ausência de regressões.
- [x] 5.3 Executar `mvnw.cmd clean verify` e registrar o resultado.
- [x] 5.4 Executar `openspec validate consulta-operacao --strict` e a validação global em modo strict.
- [x] 5.5 Após alterações de código, atualizar o Graphify e consultar o grafo para conferir os novos fluxos de leitura; não atualizar o grafo nesta etapa de planejamento.
- [x] 5.6 Executar `git diff --check`, revisar diff e status e auditar a ausência de filtros, paginação, cálculos financeiros, APIs externas, schema e endpoints fora do escopo.
