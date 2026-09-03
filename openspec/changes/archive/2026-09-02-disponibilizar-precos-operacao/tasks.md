## 1. Baseline e contratos

- [x] 1.1 Confirmar branch, estado do worktree e baseline de testes antes da implementação
- [x] 1.2 Adicionar `PreviaPrecoCompraResponse` com ticker, mercado, moeda, data civil e preço em `BigDecimal`
- [x] 1.3 Adicionar `SugestaoPrecoVendaResponse` com `precoUnitarioSugerido` anulável em `BigDecimal`
- [x] 1.4 Adicionar testes de serialização e precisão decimal dos dois DTOs

## 2. Regra compartilhada de fechamento histórico

- [x] 2.1 Extrair a seleção de `CotacaoHistoricaProvider` e a validação do retorno para uma colaboração reutilizável
- [x] 2.2 Preservar BRAPI para BRASIL, Alpha Vantage para EUA e o fechamento raw/unadjusted da data exata
- [x] 2.3 Preservar normalização de ticker, validação de data por mercado e verificação prévia da Ação cadastrada
- [x] 2.4 Fazer o ramo COMPRA de `POST /operacoes` reutilizar a colaboração sem alterar request, response ou persistência
- [x] 2.5 Provar por teste que VENDA não consulta provider e COMPRA continua consultando novamente no POST
- [x] 2.6 Provar por teste que COMPRA continua rejeitando `precoUnitario` nulo ou preenchido e ambas rejeitam `ordemNoDia`

## 3. Prévia de preço de COMPRA

- [x] 3.1 Implementar serviço consultivo da prévia sem persistência, cache, transaction de escrita ou lock
- [x] 3.2 Expor `GET /operacoes/previa-compra` com ticker, mercado e dataOperacao obrigatórios
- [x] 3.3 Mapear a resposta validada para ticker normalizado, mercado, moeda, dataCotacao e precoUnitario
- [x] 3.4 Cobrir prévia BRASIL com fechamento bruto encontrado e provider EUA não chamado
- [x] 3.5 Cobrir prévia EUA com `4. close` bruto encontrado e provider BRASIL não chamado
- [x] 3.6 Cobrir data sem pregão sem fallback, ajuste de data ou cotação atual
- [x] 3.7 Cobrir histórico fora do alcance sem fallback
- [x] 3.8 Cobrir Ação não cadastrada sem chamada ao provider
- [x] 3.9 Cobrir propagação de 404 de ticker, 429, 502, 503 e 504 dos adapters simulados
- [x] 3.10 Provar que sucesso e falha da prévia não modificam Ação, histórico de cotação, Operação, posição ou snapshot

## 4. Sugestão de preço de VENDA

- [x] 4.1 Adicionar query limitada à última COMPRA por Carteira, Ação e `dataOperacao <= limite`, ordenada por data, ordem e id descendentes
- [x] 4.2 Cobrir a query com múltiplas compras e garantir que uma VENDA mais recente seja ignorada
- [x] 4.3 Cobrir isolamento entre Carteiras, Ações e mercados
- [x] 4.4 Cobrir operação retroativa e garantir que COMPRA posterior à nova VENDA não seja usada
- [x] 4.5 Cobrir múltiplas compras na mesma data, selecionando maior ordem e usando id somente como desempate
- [x] 4.6 Implementar serviço consultivo que valida Carteira, Ação, ticker, mercado e data sem chamar providers
- [x] 4.7 Expor `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda`
- [x] 4.8 Retornar `200 OK` com preço persistido exato quando houver COMPRA elegível
- [x] 4.9 Retornar `200 OK` com `precoUnitarioSugerido=null` quando não houver COMPRA elegível
- [x] 4.10 Cobrir `400 REQUEST_INVALIDO` e `404` para entradas ou referências inválidas
- [x] 4.11 Provar que a sugestão não adquire lock de escrita, não persiste estado e não consulta provider
- [x] 4.12 Provar que o preço sugerido não altera a validação nem o preço escolhido no POST de VENDA

## 5. API e documentação

- [x] 5.1 Documentar parâmetros, schema de sucesso e respostas 400/404/422/429/502/503/504 da prévia no OpenAPI
- [x] 5.2 Documentar parâmetros, schema 200 presente/nulo e respostas 400/404 da sugestão no OpenAPI
- [x] 5.3 Documentar que a prévia é informativa, o POST reconsulta o fechamento e não aceita preço em COMPRA
- [x] 5.4 Documentar que a sugestão é editável, temporalmente limitada e não é preço médio, cotação ou recomendação financeira
- [x] 5.5 Atualizar testes de `/v3/api-docs` para os endpoints, DTOs, nulabilidade, códigos e preservação do contrato discriminado do POST

## 6. Regressão e validação

- [x] 6.1 Executar testes focados de DTOs, services, repositories, resources, adapters históricos e OpenAPI
- [x] 6.2 Executar a suíte backend completa sem chamadas reais a providers
- [x] 6.3 Confirmar que nenhuma migration, tabela, cache, dependência ou configuração de provider foi adicionada
- [x] 6.4 Confirmar que o frontend e `openspec/changes/archive/**` permaneceram intactos
- [x] 6.5 Executar build/package do backend conforme o fluxo vigente
- [x] 6.6 Executar OpenSpec strict da change e strict global
- [x] 6.7 Executar `git diff --check` e revisar `git status --short`
