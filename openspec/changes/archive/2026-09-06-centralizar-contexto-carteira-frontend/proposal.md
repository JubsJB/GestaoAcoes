## Why

A seleção de carteira hoje pertence ao Dashboard, enquanto detalhes e operações mantêm contextos separados. Centralizar a carteira torna a navegação coerente e permite consultar posições e registrar movimentações no contexto correto antes de retomar o refinamento visual das páginas.

## What Changes

- Introduzir contexto frontend leve com coleção validada, carteira ativa, inicialização/erro/vazio e preferência local, sem dados financeiros.
- Adicionar seletor próximo à identificação da aplicação, com URL explícita válida prevalecendo sobre preferência, primeira carteira por id ASC como fallback e tratamento explícito de URL inválida.
- Persistir somente a última seleção explícita em localStorage isolado/versionado, com fallback em memória e sem sincronização live entre abas.
- Sincronizar criação, edição e exclusão confirmadas; abrir detalhe seleciona/persiste a carteira e trocar no detalhe navega para a nova carteira.
- **BREAKING (experiência frontend):** substituir a seleção exclusiva do Dashboard e a espera com múltiplas carteiras pelo contexto global; remover Operações somente do menu principal.
- Adicionar posições abertas autoritativas ao detalhe da carteira, preservando histórico e reagindo a mudanças de paramMap.
- Reutilizar registro de operação com origem fixa e retorno determinístico; manter /operacoes global e todas as suas rotas para compatibilidade.
- Preservar os contratos financeiros/HTTP existentes, evolução independente, precisão e registro manual de snapshots.

## Capabilities

### New Capabilities

Nenhuma. O contexto transversal será especificado na capability existente do shell.

### Modified Capabilities

- `frontend-application-shell`: contexto global, seletor, persistência, precedência e navegação principal.
- `frontend-dashboard-management`: consumo do contexto compartilhado sem seletor/listagem duplicados, estados e sincronização da URL.
- `frontend-portfolio-management`: detalhe com posições, rota reativa e sincronização das mutações no contexto.
- `frontend-operation-management`: origem fixa, entrada contextual, retorno após reload e compatibilidade global.

## Impact

Predominantemente frontend Angular: serviço/store com signals/computed e RxJS, coordenador de navegação, shell, páginas de Dashboard/Carteira/Operação e testes. OnPush e limites lazy serão preservados; nenhuma dependência nova.

IN SCOPE: seleção global, persistência, fallback, mutações de Carteira, posições e histórico contextual, operação de origem fixa, sidebar, deep links, retorno e acessibilidade/testes relacionados.

OUT OF SCOPE: backend, novos DTOs/endpoints, banco/migrations, cálculos ou métricas financeiras, parsing lossless, precisão, formatadores, FX, autenticação/usuários, sincronização entre abas, logos, dark mode, redesign e retomada das Fases 4–7 da change visual.

Contratos reutilizados: CarteirasService; DashboardService (incluindo GET /carteiras/{id}/posicoes); OperacoesService; EvolutionService. Referências sem delta: frontend-portfolio-evolution, frontend-visual-experience, frontend-stock-management, frontend-broker-management, portfolio-query/creation/update/deletion/position/summary/evolution/snapshot, operation-query/registration, purchase-price-preview e sale-price-suggestion.

A change refinar-experiencia-visual-frontend permanece pausada com suas Fases 1–3 preservadas. Esta change será concluída primeiro; design documenta os artefatos e tasks visuais a reconciliar posteriormente, sem modificá-los agora. PRD: seção 9.3, RF13–RF26 e seções 17–18.
