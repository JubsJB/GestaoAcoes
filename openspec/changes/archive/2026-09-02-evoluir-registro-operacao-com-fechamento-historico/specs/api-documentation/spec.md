## MODIFIED Requirements

### Requirement: Contratos de entrada e saída fiéis
O documento efetivamente servido por `/v3/api-docs` SHALL representar `POST /operacoes` com `oneOf` para COMPRA/VENDA e discriminator `tipo`. O schema de COMPRA SHALL omitir `precoUnitario`; o schema de VENDA SHALL declarar `precoUnitario` em `required`. Ambas as variantes SHALL omitir `ordemNoDia`, declarar propriedades adicionais proibidas e preservar `corretoraId` opcional e anulável. O response SHALL continuar documentando `precoUnitario`, `ordemNoDia` e `valorTotal` produzidos pelo backend, além dos demais campos vigentes.

#### Scenario: Inspeção de request e response
- **WHEN** um consumidor inspeciona o schema de criação de Operação
- **THEN** encontra `oneOf`, discriminator `tipo`, COMPRA sem preço, VENDA com preço obrigatório, ausência de ordem nos requests e propriedades adicionais proibidas

#### Scenario: Inspeção do response
- **WHEN** um consumidor inspeciona `OperacaoResponse`
- **THEN** encontra `precoUnitario`, `ordemNoDia` e `valorTotal` como dados retornados, não como entradas universais

#### Scenario: Precisão e temporalidade
- **WHEN** um consumidor examina quantidade, preços, total e datas
- **THEN** o documento preserva formatos, precisão, escala e semântica temporal vigentes

#### Scenario: Documento efetivo coerente com binding
- **WHEN** os testes consultam o JSON real de `/v3/api-docs`
- **THEN** as restrições documentadas correspondem ao binding Jackson exercitado separadamente, sem depender apenas de annotations ou inspeção de classes

### Requirement: Respostas de erro públicas e proporcionais
O OpenAPI SHALL documentar, para COMPRA, `422 COTACAO_HISTORICA_INDISPONIVEL`, `422 HISTORICO_COTACAO_FORA_DO_ALCANCE`, `404 TICKER_INEXISTENTE`, `429 LIMITE_REQUISICOES_EXCEDIDO`, `502 RESPOSTA_EXTERNA_INVALIDA`, `503 SERVICO_EXTERNO_INDISPONIVEL` e `504 SERVICO_EXTERNO_TIMEOUT`, além dos erros de validação, posição e integridade vigentes. Para VENDA, MUST NOT sugerir dependência de provider ou erros históricos impossíveis nesse fluxo.

#### Scenario: Erro plausível documentado
- **WHEN** um consumidor inspeciona as respostas de `POST /operacoes` para COMPRA
- **THEN** encontra os dois erros 422 e os erros externos relevantes com o schema público vigente

#### Scenario: Fallback de integridade
- **WHEN** uma violação de integridade ainda puder ocorrer como última defesa
- **THEN** a resposta padronizada correspondente permanece documentada

#### Scenario: VENDA sem provider
- **WHEN** um consumidor inspeciona a variante VENDA
- **THEN** a documentação não afirma que a criação consulta cotação histórica

#### Scenario: Dependência externa exclusiva da COMPRA
- **WHEN** um consumidor inspeciona a operação e seus schemas
- **THEN** identifica que somente COMPRA pode produzir os erros históricos e externos 404, 429, 502, 503 e 504 relacionados ao provider

#### Scenario: Segurança do schema de erro
- **WHEN** qualquer erro é documentado
- **THEN** exemplos e schemas não expõem API keys, URLs sensíveis, stack traces ou detalhes internos

### Requirement: Compatibilidade sem mudança funcional
A documentação SHALL descrever a dependência externa exclusiva de novas COMPRAS e a geração backend de `ordemNoDia`, sem alterar contratos de consulta de Operações ou demais endpoints. A mudança MUST NOT documentar preço médio acumulado, endpoint público de candles ou migration inexistente.

#### Scenario: Backend após habilitar documentação
- **WHEN** a descrição OpenAPI é regenerada após a evolução
- **THEN** o backend preserva contratos fora do registro e consultas de Operação continuam independentes de providers

#### Scenario: Credenciais internas
- **WHEN** a documentação OpenAPI é consultada
- **THEN** nenhuma credencial ou valor de API key é exposto

#### Scenario: Escopo backend
- **WHEN** os artefatos da change são aplicados
- **THEN** somente o contrato backend é alterado e a reconciliação frontend permanece fora desta change
