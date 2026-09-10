## MODIFIED Requirements

### Requirement: Contratos de entrada e saída fiéis
O documento efetivamente servido por `/v3/api-docs` SHALL representar `POST /operacoes` com `oneOf` para COMPRA/VENDA e discriminator `tipo`. Os schemas de COMPRA e VENDA SHALL declarar `precoUnitario` em `required`. Ambas as variantes SHALL omitir `ordemNoDia`, declarar propriedades adicionais proibidas e preservar `corretoraId` opcional e anulável. O response SHALL continuar documentando `precoUnitario`, `ordemNoDia` e `valorTotal` produzidos pelo backend, além dos demais campos vigentes.

#### Scenario: Inspeção de request e response
- **WHEN** um consumidor inspeciona o schema de criação de Operação
- **THEN** encontra `oneOf`, discriminator `tipo`, COMPRA e VENDA com preco obrigatorio, ausência de ordem nos requests e propriedades adicionais proibidas

#### Scenario: Inspeção do response
- **WHEN** um consumidor inspeciona `OperacaoResponse`
- **THEN** encontra `precoUnitario`, `ordemNoDia` e `valorTotal` como dados retornados, com preco como entrada e total/ordem gerados exclusivamente pelo backend

#### Scenario: Precisão e temporalidade
- **WHEN** um consumidor examina quantidade, preços, total e datas
- **THEN** o documento preserva formatos, precisão, escala e semântica temporal vigentes

#### Scenario: Documento efetivo coerente com binding
- **WHEN** os testes consultam o JSON real de `/v3/api-docs`
- **THEN** as restrições documentadas correspondem ao binding Jackson exercitado separadamente, sem depender apenas de annotations ou inspeção de classes

### Requirement: Respostas de erro públicas e proporcionais
O OpenAPI SHALL documentar erros de validacao, referencias, posicao e integridade do POST. MUST NOT indicar dependencia historica/externa em COMPRA ou VENDA. Erros de providers SHALL permanecer nas consultas historicas independentes.

#### Scenario: Erro plausível documentado
- **WHEN** um consumidor inspeciona as respostas de `POST /operacoes` para COMPRA
- **THEN** encontra erros de validacao e integridade plausiveis, sem erros de provider no POST

#### Scenario: Fallback de integridade
- **WHEN** uma violação de integridade ainda puder ocorrer como última defesa
- **THEN** a resposta padronizada correspondente permanece documentada

#### Scenario: VENDA sem provider
- **WHEN** um consumidor inspeciona a variante VENDA
- **THEN** a documentação não afirma que a criação consulta cotação histórica

#### Scenario: Ausencia de dependencia externa no POST
- **WHEN** um consumidor inspeciona a operação e seus schemas
- **THEN** identifica que COMPRA e VENDA nao consultam providers; erros externos permanecem nas consultas historicas

#### Scenario: Segurança do schema de erro
- **WHEN** qualquer erro é documentado
- **THEN** exemplos e schemas não expõem API keys, URLs sensíveis, stack traces ou detalhes internos


#### Scenario: Dependência externa exclusiva da COMPRA
- **WHEN** o cliente registra COMPRA ou VENDA com preco valido
- **THEN** o POST nao consulta fechamento historico nem depende do provider

### Requirement: Compatibilidade sem mudança funcional
A documentação SHALL descrever o preco manual em COMPRA e VENDA e a geração backend de `ordemNoDia`, sem alterar contratos de consulta de Operações ou demais endpoints. A mudança MUST NOT documentar preço médio acumulado, endpoint público de candles ou migration inexistente.

#### Scenario: Backend após habilitar documentação
- **WHEN** a descrição OpenAPI é regenerada após a evolução
- **THEN** o backend preserva contratos fora do registro e consultas de Operação continuam independentes de providers

#### Scenario: Credenciais internas
- **WHEN** a documentação OpenAPI é consultada
- **THEN** nenhuma credencial ou valor de API key é exposto

#### Scenario: Escopo backend
- **WHEN** os artefatos da change são aplicados
- **THEN** contrato e formulario sao coerentes quanto ao preco manual, sem alterar formulas contabeis

### Requirement: Documentação das consultas de apoio à criação de Operações
O OpenAPI SHALL documentar os endpoints de prévia de COMPRA e sugestão de VENDA com parâmetros, formatos, DTOs e respostas. A documentação SHALL declarar que a previa usa fechamento historico exato em consulta independente e nao participa do POST manual, e que a sugestão de VENDA é editável, não vinculante, limitada à última COMPRA cronologicamente aplicável e não constitui preço médio, cotação atual ou recomendação financeira.

#### Scenario: Contrato documentado da prévia
- **WHEN** um consumidor consulta o OpenAPI de `GET /operacoes/previa-compra`
- **THEN** encontra os parâmetros obrigatórios, `PreviaPrecoCompraResponse`, `200`, `400`, `404`, `422`, `429`, `502`, `503` e `504`, incluindo os códigos padronizados aplicáveis

#### Scenario: Contrato documentado da sugestão
- **WHEN** um consumidor consulta o OpenAPI de `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda`
- **THEN** encontra os parâmetros obrigatórios, `SugestaoPrecoVendaResponse`, respostas `200`, `400` e `404` e a semântica de `precoUnitarioSugerido=null`

#### Scenario: Separação do POST documentada
- **WHEN** um consumidor compara as consultas com `POST /operacoes`
- **THEN** a documentação mantém COMPRA e VENDA com `precoUnitario` obrigatorio e ambas sem `ordemNoDia`
