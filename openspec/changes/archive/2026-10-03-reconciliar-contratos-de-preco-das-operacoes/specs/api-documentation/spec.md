## MODIFIED Requirements

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

### Requirement: Documentação das consultas de apoio à criação de Operações
A descrição do endpoint e o schema de resposta da prévia SHALL declarar consulta independente, informativa, não vinculante e sem efeitos colaterais, utilizável como sugestão inicial editável. MUST NOT afirmar que o POST reconsulta provider ou exige sucesso da prévia; preço final válido e demais validações SHALL continuar obrigatórios. O OpenAPI SHALL documentar os endpoints de prévia de COMPRA e sugestão de VENDA com parâmetros, formatos, DTOs e respostas. A documentação SHALL declarar que a previa usa fechamento historico exato em consulta independente e nao participa do POST manual, e que a sugestão de VENDA é editável, não vinculante, limitada à última COMPRA cronologicamente aplicável e não constitui preço médio, cotação atual ou recomendação financeira.

#### Scenario: Contrato documentado da prévia
- **WHEN** um consumidor consulta o OpenAPI de `GET /operacoes/previa-compra`
- **THEN** encontra os parâmetros obrigatórios, `PreviaPrecoCompraResponse`, `200`, `400`, `404`, `422`, `429`, `502`, `503` e `504`, incluindo os códigos padronizados aplicáveis, e encontra no endpoint e no schema a semântica de sugestão editável sem reconsulta histórica pelo POST

#### Scenario: Contrato documentado da sugestão
- **WHEN** um consumidor consulta o OpenAPI de `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda`
- **THEN** encontra os parâmetros obrigatórios, `SugestaoPrecoVendaResponse`, respostas `200`, `400` e `404` e a semântica de `precoUnitarioSugerido=null`

#### Scenario: Separação do POST documentada
- **WHEN** um consumidor compara as consultas com `POST /operacoes`
- **THEN** a documentação mantém COMPRA e VENDA com `precoUnitario` obrigatorio e ambas sem `ordemNoDia`
