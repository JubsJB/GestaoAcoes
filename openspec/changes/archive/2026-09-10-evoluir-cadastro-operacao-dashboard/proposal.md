## Why

O usuario aprovou preco unitario manual em COMPRA e VENDA e registro contextual no Dashboard sem navegar para outra pagina.

## What Changes

- **BREAKING** COMPRA exige precoUnitario informado positivo; POST deixa de consultar fechamento historico.
- Reutilizar formulario existente como dialog lazy no Dashboard com carteira capturada e fixa.
- Preservar VENDA, replay, quantidade, precisao, corretora opcional e rotas existentes.

## Capabilities

### Modified Capabilities
- `operation-registration`: preco manual nos dois tipos, sem consulta historica no POST.
- `purchase-price-preview`: consulta preservada, desvinculada do POST manual.
- `api-documentation`: schemas e respostas coerentes com preco manual.
- `frontend-operation-management`: COMPRA editavel e dialog contextual.

## Impact

DTO COMPRA, service e documentacao OpenAPI, formulario/model frontend, Dashboard e testes. Sem migrations, dependencias, budgets ou formulas novas. Endpoints historicos permanecem para consulta independente, usados como sugestao inicial editavel no cadastro COMPRA.

## Ajuste aprovado na revisao humana
COMPRA consulta previa-compra ao escolher acao/data e preenche sugestao lossless editavel. Mudanca de acao/data cancela resposta antiga e atualiza sugestao; edicao durante consulta nao e sobrescrita. POST usa o campo final, sem reconsulta. Falha historica mantem feedback existente e permite preco manual positivo. VENDA permanece inalterada. Form fields usam subscriptSizing dynamic para reservar mensagens multilinha sem sobreposicao. Auditoria PETR4 somente leitura, sem alteracao de formulas ou dados.
