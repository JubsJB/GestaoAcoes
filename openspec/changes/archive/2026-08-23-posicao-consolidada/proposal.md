## Why

O histórico de Operações já preserva compras, vendas, quantidade, preço efetivamente negociado e ordem financeira, mas o sistema ainda não expõe a posição contábil atual de cada Ação de uma Carteira. Esta change atende aos RF17–RF20 do PRD por meio de consolidação determinística sob demanda, mantendo as Operações como única fonte de verdade.

## What Changes

- Adicionar somente `GET /carteiras/{carteiraId}/posicoes`, com `200 OK`, lista determinística por `mercado ASC`, `ticker ASC`, `acaoId ASC`, `[]` para Carteira sem posição aberta e `404 Not Found` para Carteira inexistente.
- Derivar separadamente cada posição por Carteira e Ação, reproduzindo `COMPRA` e `VENDA` por `dataOperacao ASC` e `ordemNoDia ASC`.
- Calcular `quantidadeAtual`, preço médio ponderado e custo contábil da posição usando somente `Operacao.precoUnitario`, sem cotação ou provider externo.
- Formalizar venda parcial, zeramento, novo ciclo após encerramento e compra posterior a venda parcial.
- Retornar `PosicaoResponse` com `acaoId`, ticker, nome da empresa, mercado, moeda, quantidade atual, preço médio e custo da posição, sem indicadores de mercado ou resultado.
- Aplicar preço médio com precisão lógica 25/escala 12, custo com precisão lógica 38/escala 12 e divisões intermediárias em escala 24 com `RoundingMode.HALF_EVEN`.
- Reutilizar a leitura cronológica existente e extrair de forma localizada uma calculadora financeira pura e testável, sem persistir posição e sem criar migration.
- Rejeitar de forma padronizada a consolidação de histórico legado inconsistente, sem corrigir ou reescrever Operações silenciosamente.
- Preservar integralmente cadastro, consulta e integridade concorrente de Operações, bem como os contratos existentes de Carteira e Ação.

## Capabilities

### New Capabilities

- `portfolio-position`: consulta e cálculo sob demanda da posição contábil consolidada de cada Ação de uma Carteira a partir do histórico de Operações.

### Modified Capabilities

- Nenhuma.

## Impact

- API: novo endpoint de leitura sob `/carteiras/{carteiraId}/posicoes`; nenhuma rota de escrita ou consulta específica por Ação nesta primeira fatia.
- Backend: novo DTO de saída e responsabilidades específicas de serviço/cálculo, com integração enxuta ao `CarteiraResource` e reutilização de `CarteiraRepository` e `OperacaoRepository`.
- Domínio financeiro: centralização do replay de quantidade, custo e preço médio, com reutilização localizada pela validação de VENDA já existente e preservação integral do `POST /operacoes`.
- Persistência: somente leitura das tabelas atuais; nenhuma entidade, tabela, migration, cache, snapshot ou posição materializada.
- Integrações: nenhuma chamada à BRAPI, Alpha Vantage, BrasilAPI, ViaCEP ou outro provider.
- Testes: cobertura unitária do cálculo, HTTP, service/repository, consistência de leitura e regressão das capabilities promovidas.
