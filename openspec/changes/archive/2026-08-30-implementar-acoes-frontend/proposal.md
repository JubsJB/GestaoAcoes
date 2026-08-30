## Why

A área de Ações ainda é somente um placeholder, embora o backend já ofereça contratos aprovados para cadastro, consultas e atualização manual de cotação. Esta change introduz a interface funcional dessa área sem levar integrações externas ou regras financeiras para o navegador.

## What Changes

- Substituir somente o placeholder lazy de Ações por páginas responsivas e acessíveis de listagem, cadastro e detalhe.
- Consumir exclusivamente `POST /acoes`, `GET /acoes`, `GET /acoes/{id}`, `GET /acoes/por-ticker?ticker=...&mercado=...` e `PATCH /acoes/{id}/cotacao` pela configuração central da API.
- Permitir cadastro apenas por ticker e mercado, busca exata explícita por esses dois valores e atualização manual de cotação somente no detalhe.
- Reutilizar respostas completas do backend como estado transitório nas navegações imediatas, sem cache global nem GET redundante.
- Apresentar moeda, cotação e data/hora somente por formatação visual, sem conversão cambial ou cálculo financeiro.
- Tratar de forma contextual erros de cadastro, consulta e providers preservando `StandardError`, a cotação anteriormente exibida e a decisão do backend.
- Manter Dashboard, Carteiras e Operações como placeholders estruturais e preservar a implementação funcional de Corretoras.
- Permanecem fora do escopo edição, exclusão, paginação, histórico de cotações, gráficos, atualização automática, operações, carteiras e cálculos financeiros.

## Capabilities

### New Capabilities

- `frontend-stock-management`: Gerenciamento frontend de Ações, incluindo listagem, busca exata, cadastro, detalhe, atualização manual de cotação, estados, erros, responsividade e acessibilidade.

### Modified Capabilities

- `frontend-application-shell`: Permitir que a capability funcional aprovada de Ações substitua exclusivamente seu placeholder, preservando shell, navegação, limite lazy, Corretoras funcional e os placeholders restantes.

## Impact

- Frontend Angular em `frontend/src/app/features/acoes/` e testes de rotas da aplicação.
- Nenhuma alteração no backend ou em seus contratos.
- Nenhuma dependência nova; Angular Material/CDK e a infraestrutura HTTP existentes serão reutilizados.
- A política npm, `API_BASE_URL`, o interceptor e o `packageManager` permanecem inalterados.
- O Graphify não será atualizado nesta change enquanto persistir o WinError 5 operacional conhecido; isso não altera os contratos planejados.
