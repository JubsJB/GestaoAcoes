## Why

As ações brasileiras e americanas já podem ser cadastradas e persistidas, mas ainda não há endpoints para recuperar esses registros. Esta change entrega a primeira capacidade de leitura prevista por `RF09` e pela consulta por ID de `RF10`, usando exclusivamente o estado persistido e sem depender novamente dos provedores de mercado.

## What Changes

- Adicionar `GET /acoes` para retornar todas as ações persistidas como `AcaoResponse`, sem paginação nesta primeira fatia.
- Ordenar a listagem de forma determinística por `id` em ordem crescente e retornar `200 OK` com `[]` quando não houver registros.
- Adicionar `GET /acoes/{id}` para retornar o `AcaoResponse` completo da ação persistida correspondente.
- Responder `404 Not Found` no formato padronizado atual quando o identificador não existir.
- Reutilizar `AcaoResource`, `AcaoService`, `AcaoRepository`, `AcaoMapper`, `AcaoResponse`, `ObjectNotFoundException` e `ResourceExceptionHandler` existentes.
- Garantir que as consultas usem somente os dados persistidos, sem chamar BRAPI, Alpha Vantage ou qualquer outro serviço externo e sem atualizar cotação ou `dataHoraCotacao`.
- Criar testes unitários, de repository e de endpoints para listagem, ordenação, lista vazia, consulta por ID, ID inexistente e ausência de chamadas externas, preservando os testes existentes.
- Manter fora desta change o cadastro já implementado, consulta por ticker ou mercado, filtros, paginação, atualização de cotação, atualização manual, histórico de cotações, exclusão, Carteira, Operação, preço médio, cálculos financeiros e frontend.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `stock-registration`: ampliar a capability existente de Ação com listagem dos registros persistidos e consulta individual por ID, sem consulta ou revalidação em provedores externos.

## Impact

- API: novos endpoints `GET /acoes` e `GET /acoes/{id}`, sem alteração no contrato de `POST /acoes`.
- Aplicação: ampliação dos componentes existentes de resource e service para operações de leitura, reutilizando repository, mapper, DTO e tratamento global de erros.
- Testes: ampliação dos testes de service, repository e HTTP, incluindo verificação explícita de ausência de interações com providers externos.
- Persistência e configuração: nenhuma alteração de entidade, schema, changelog Liquibase, dependência, configuração de banco ou integração externa.
