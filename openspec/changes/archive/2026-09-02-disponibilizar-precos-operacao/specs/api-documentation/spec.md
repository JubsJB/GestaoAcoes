## ADDED Requirements

### Requirement: Documentação das consultas de apoio à criação de Operações
O OpenAPI SHALL documentar os endpoints de prévia de COMPRA e sugestão de VENDA com parâmetros, formatos, DTOs e respostas. A documentação SHALL declarar que a prévia usa fechamento histórico exato mas não substitui a validação autoritativa do POST, e que a sugestão de VENDA é editável, não vinculante, limitada à última COMPRA cronologicamente aplicável e não constitui preço médio, cotação atual ou recomendação financeira.

#### Scenario: Contrato documentado da prévia
- **WHEN** um consumidor consulta o OpenAPI de `GET /operacoes/previa-compra`
- **THEN** encontra os parâmetros obrigatórios, `PreviaPrecoCompraResponse`, `200`, `400`, `404`, `422`, `429`, `502`, `503` e `504`, incluindo os códigos padronizados aplicáveis

#### Scenario: Contrato documentado da sugestão
- **WHEN** um consumidor consulta o OpenAPI de `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda`
- **THEN** encontra os parâmetros obrigatórios, `SugestaoPrecoVendaResponse`, respostas `200`, `400` e `404` e a semântica de `precoUnitarioSugerido=null`

#### Scenario: Separação do POST documentada
- **WHEN** um consumidor compara as consultas com `POST /operacoes`
- **THEN** a documentação mantém COMPRA sem `precoUnitario`, VENDA com `precoUnitario` obrigatório e ambas sem `ordemNoDia`
