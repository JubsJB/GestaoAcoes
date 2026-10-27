## ADDED Requirements

### Requirement: Documentação dos contratos internos de custódia
OpenAPI SHALL documentar rotas, requests discriminados, respostas, filtros de natureza/situação, ordenação de registro, corpo opcional de motivo no estorno e Location do detalhe original. SHALL identificar controle interno, ausência de preços, posição gerada, precisão BR/EUA, políticas de datas, legado e neutralização retroativa. SHALL incluir exemplos de transferência, atribuição, estorno, original ativa/estornada e coleção vazia. MUST NOT documentar endpoint individual de estorno não implementado, paginação inexistente ou transferência institucional.

#### Scenario: Contratos navegáveis
- **WHEN** documentação é gerada após implementação autorizada
- **THEN** schemas e exemplos correspondem às rotas reais e às variantes, com campos controlados proibidos no request

#### Scenario: Consulta do estorno
- **WHEN** POST de estorno retorna201
- **THEN** Location aponta ao detalhe original consultável que contém o novo registro

### Requirement: Documentação da validação quantitativa e evidências genéricas
OpenAPI SHALL refletir nova validação local de vendas e comparação histórica, códigos de erro estáveis, schema discriminado de primeiroDeficit e posições exatas. Exemplos de legado SHALL explicitar que negativos são preexistentes, não registros novos aceitos por POST. Documentação SHALL ser atualizada junto a backend/parser, sem versão paralela, segredos ou alteração de fórmulas.

#### Scenario: Schema do primeiro déficit
- **WHEN** OpenAPI e exemplos da distribuição são consultados
- **THEN** dominio/id/natureza/dataEfetiva/posicaoCronologica/saldoAposEvento substituem campos exclusivos de operação

#### Scenario: Erros e legado
- **WHEN** exemplo contém déficit histórico e contrato de nova venda
- **THEN** documenta preservação do legado e rejeição de novo déficit, sem contradição

## MODIFIED Requirements

### Requirement: Compatibilidade sem mudança funcional
A documentação SHALL preservar preço manual de COMPRA/VENDA, ordem financeira gerada e contratos de consulta financeira. SHALL descrever mudanças funcionais aprovadas nesta change: escrita de custódia, validação local/diferencial das operações e evolução coordenada da distribuição. Demais endpoints/cálculos MUST NOT ser alterados por documentação. MUST NOT expor preço médio por corretora, candles públicos, migration inexistente ou credenciais.

#### Scenario: Backend após habilitar documentação
- **WHEN** a descrição OpenAPI é regenerada após a evolução
- **THEN** o backend preserva contratos fora do registro e consultas de Operação continuam independentes de providers

#### Scenario: Credenciais internas
- **WHEN** a documentação OpenAPI é consultada
- **THEN** nenhuma credencial ou valor de API key é exposto

#### Scenario: Escopo backend
- **WHEN** os artefatos da change são aplicados
- **THEN** contrato e formulario sao coerentes quanto ao preco manual, sem alterar formulas contabeis
