## Why

As Operações persistidas já constituem o histórico financeiro autoritativo da Carteira, mas ainda não podem ser consultadas pela API. Esta change disponibiliza leituras determinísticas e fiéis desse histórico, formando a base de consulta necessária às futuras capabilities financeiras sem antecipar cálculos ou depender de integrações externas.

## What Changes

- Adicionar `GET /operacoes` com `200 OK`, lista de `OperacaoResponse`, array vazio quando não houver registros e ordenação determinística por `dataOperacao ASC`, `ordemNoDia ASC` e `id ASC`, sendo o ID apenas desempate técnico.
- Adicionar `GET /operacoes/{id}` com `200 OK` para Operação existente e `404 Not Found` no padrão centralizado para ID inexistente.
- Adicionar `GET /carteiras/{carteiraId}/operacoes` em `CarteiraResource`, com delegação a `OperacaoService`, histórico isolado da Carteira, `[]` para Carteira existente sem Operações e `404 Not Found` para Carteira inexistente, sem duplicar regra em `CarteiraService`.
- Reutilizar `OperacaoResponse` e devolver somente os valores e relacionamentos já persistidos, preservando Corretora ausente como `null`.
- Executar as consultas como operações read-only, sem escrita, replay de posição, recálculo de `valorTotal`, normalização adicional, uso de `Clock` ou chamada a providers externos.
- Preservar integralmente `POST /operacoes`, a proteção de exclusão de Carteira e os contratos existentes de Carteira, Ação e Corretora.
- Manter filtros, paginação, posição, preço médio, resultados financeiros, cotações históricas e frontend fora do escopo.

## Capabilities

### New Capabilities

- `operation-query`: Consulta geral, consulta individual e histórico por Carteira das Operações persistidas, com representação fiel, ordenação determinística e ausência de efeitos colaterais.

### Modified Capabilities

- Nenhuma.

## Impact

- APIs afetadas: inclusão de `GET /operacoes`, `GET /operacoes/{id}` e `GET /carteiras/{carteiraId}/operacoes`.
- Componentes previstos: `OperacaoRepository`, `OperacaoService`, `OperacaoResource`, `CarteiraResource`, `OperacaoMapper` e testes relacionados; `OperacaoService` permanece responsável por validar a Carteira, consultar, ordenar e mapear o histórico da rota aninhada.
- Persistência: somente leituras sobre a tabela e os relacionamentos existentes de `operacao`; nenhuma migration ou alteração de schema.
- Integrações e configuração: nenhuma chamada externa, dependência, configuração ou mudança no Liquibase/Hibernate.
