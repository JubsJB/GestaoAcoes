## MODIFIED Requirements

### Requirement: Documento OpenAPI derivado da API real
O sistema SHALL disponibilizar documento code-first springdoc refletindo exatamente métodos, paths, parâmetros, bodies, respostas e schemas públicos implementados. SHALL incluir o novo POST de atualização das cotações da Carteira e extensões temporais somente quando implementados. Contagem SHALL corresponder à API real, sem perpetuar contagem histórica fixa nem contabilizar assets técnicos. MUST NOT declarar comportamentos inexistentes.

#### Scenario: Consulta da descrição JSON
- **WHEN** um consumidor acessa /v3/api-docs em qualquer profile
- **THEN** recebe documento válido com operações e paths funcionais realmente implementados, incluindo o lote quando entregue

#### Scenario: Consulta da descrição YAML
- **WHEN** um consumidor acessa `/v3/api-docs.yaml` em qualquer profile do MVP
- **THEN** o sistema responde com a representação YAML da mesma descrição OpenAPI

#### Scenario: Ausência de contrato fictício
- **WHEN** o documento OpenAPI é inspecionado
- **THEN** ele não contém autenticação, versionamento `/v1`, filtros, paginação ou endpoints que não existam no backend

## ADDED Requirements

### Requirement: Documentação do lote e precisão temporal de cotação
OpenAPI SHALL documentar POST /carteiras/{carteiraId}/atualizacoes-cotacoes sem lista arbitrária de Ações, 200 com resultados individuais e contadores, vazio e erros globais padronizados. SHALL documentar ATUALIZADA, SEM_NOVA_COTACAO, FALHA, motivos públicos, referência tipada e nullability legada, mantendo dataHoraCotacao como compatibilidade. PATCH individual SHALL manter path/response e erros externos, acrescentando erros temporais 422. MUST NOT prometer tempo real, classificação de defasagem, rollback global de sucesso parcial ou alteração de preços de operações.

#### Scenario: Resultado parcial documentado
- **WHEN** o consumidor inspeciona o lote
- **THEN** encontra 200 inclusive parcial, contadores e estados por ativo

#### Scenario: Schemas temporais
- **WHEN** inspeciona Ação e posição
- **THEN** encontra INSTANTE/DATA/DESCONHECIDA, precisão e campos legados preservados

#### Scenario: Falha temporal individual
- **WHEN** PATCH recebe tipos incompatíveis ou referência inválida
- **THEN** OpenAPI documenta erro padronizado temporal sem alteração da cotação
