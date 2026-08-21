## ADDED Requirements

### Requirement: Listagem das ações persistidas
O sistema SHALL expor `GET /acoes` e SHALL responder `200 OK` com um array contendo todas as ações já persistidas no contrato completo de resposta de Ação. A listagem SHALL ser ordenada de forma determinística por `id` em ordem crescente e SHALL NOT exigir paginação nesta primeira fatia.

#### Scenario: Listagem com ações cadastradas
- **WHEN** existem ações persistidas e o cliente solicita `GET /acoes`
- **THEN** o sistema retorna `200 OK` com todas as ações como DTOs completos, ordenadas por `id` crescente

#### Scenario: Listagem sem ações cadastradas
- **WHEN** não existe nenhuma ação persistida e o cliente solicita `GET /acoes`
- **THEN** o sistema retorna `200 OK` com um array vazio

#### Scenario: Valores persistidos preservados na listagem
- **WHEN** uma ação integra a resposta de `GET /acoes`
- **THEN** o sistema retorna seu ticker, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação conforme persistidos, sem recalcular ou substituir esses valores

### Requirement: Consulta de ação persistida por ID
O sistema SHALL expor `GET /acoes/{id}` para recuperar uma ação já persistida e SHALL devolver seus dados completos no mesmo contrato de resposta utilizado pelo cadastro.

#### Scenario: Consulta por ID existente
- **WHEN** o cliente solicita `GET /acoes/{id}` com o identificador de uma ação persistida
- **THEN** o sistema retorna `200 OK` com o DTO completo da ação correspondente

#### Scenario: Consulta por ID inexistente
- **WHEN** o cliente solicita `GET /acoes/{id}` com um identificador que não corresponde a uma ação persistida
- **THEN** o sistema retorna `404 Not Found` no formato padronizado atual de erros da API

### Requirement: Consultas independentes dos provedores de cotação
As operações `GET /acoes` e `GET /acoes/{id}` SHALL usar exclusivamente os dados persistidos e MUST NOT consultar BRAPI, Alpha Vantage ou qualquer outro serviço externo. As consultas MUST NOT atualizar `cotacaoAtual`, `dataHoraCotacao` ou qualquer outro dado da Ação.

#### Scenario: Listagem sem chamadas externas
- **WHEN** o cliente solicita a listagem de ações, existam ou não registros
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BRAPI, à Alpha Vantage ou a outro serviço externo e sem alteração dos registros

#### Scenario: Consulta individual sem chamadas externas
- **WHEN** o cliente solicita uma ação por ID, exista ela ou não
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BRAPI, à Alpha Vantage ou a outro serviço externo e sem alteração dos registros
