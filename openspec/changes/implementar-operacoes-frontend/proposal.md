## Why

O frontend parcial de Operações já foi alinhado ao POST discriminado, mas o backend do commit `de4a849` acrescentou consultas para visualizar previamente o fechamento usado na COMPRA e sugerir o preço inicial da VENDA. A mesma active change precisa incorporar esses contratos antes da implementação final.

## What Changes

- Manter a área global de Operações com listagem cronológica, detalhe e cadastro de `COMPRA` e `VENDA`.
- Modelar a criação como união discriminada: COMPRA sem `precoUnitario` e VENDA com `precoUnitario` obrigatório; nenhum request de criação contém `ordemNoDia`.
- Manter `precoUnitario`, `ordemNoDia` e `valorTotal` somente como dados autoritativos do response.
- Exibir um único campo visual de preço: somente leitura em COMPRA, preenchido por `GET /operacoes/previa-compra`, e editável/obrigatório em VENDA, inicialmente preenchido por `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda` quando houver sugestão.
- Invalidar imediatamente preços de contextos anteriores e impedir respostas assíncronas atrasadas de sobrescrever ação, mercado, carteira, data ou tipo atuais.
- Bloquear o POST de COMPRA enquanto a prévia estiver carregando, ausente ou inválida, sem incluir o preço exibido no payload e sem consultar providers diretamente.
- Preservar quantidade por mercado, data civil `YYYY-MM-DD`, Corretora opcional, precisão decimal lossless e ausência de cálculos financeiros no frontend.
- Mapear os erros da prévia, incluindo `TICKER_INEXISTENTE`, e preservar o tratamento técnico central para `502`, `503` e `504`; ausência normal de sugestão de VENDA não é erro.
- Preservar o bloqueio de submissão concorrente enquanto o POST estiver pendente, sem idempotency key, detecção de payload duplicado ou retry automático.
- Manter o mesmo formulário discriminado no fluxo global e no contexto de Carteira, com Carteira pré-selecionada no segundo caso.
- Não oferecer edição ou exclusão de Operação e não introduzir provider, dependência decimal, hora da operação, ordem manual ou redesign amplo.

## Capabilities

### New Capabilities

- `frontend-operation-management`: cadastro discriminado, listagem, detalhe, validações e feedback de Operações no frontend.

### Modified Capabilities

- `frontend-application-shell`: acrescentar Operações à navegação global e às rotas promovidas da aplicação.
- `frontend-portfolio-management`: incorporar histórico e cadastro contextual de Operações à página `/carteiras/:id` reutilizando as mesmas regras discriminadas.

## Impact

- Afeta somente o frontend Angular de Operações, a integração visual no detalhe de Carteira e seus testes.
- Consome também `GET /operacoes/previa-compra` e `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda`, além dos endpoints já planejados.
- Não altera backend, banco, OpenAPI, dependências, regras financeiras, providers externos, archives ou specs promovidas diretamente.
