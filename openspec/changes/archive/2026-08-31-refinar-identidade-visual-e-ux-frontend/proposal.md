## Why

O frontend possui uma base funcional e acessível, mas ainda usa a identidade cyan/azure próxima do Material padrão, repete estruturas visuais entre Corretoras e Ações e apresenta feedbacks, estados de busca e datas com hierarquia inconsistente. A change consolida uma experiência profissional baseada em verde oliva, melhora a clareza dos fluxos existentes e mantém marca, erros e semântica financeira como conceitos visuais distintos.

## What Changes

- Introduzir uma experiência visual transversal baseada em Material 3, tokens semânticos, superfícies neutras, container central, hierarquia de ações e padrões responsivos, sem dependência nova.
- Refinar toolbar e sidebar preservando nome, navegação, lazy boundaries, breakpoint estrutural de 960px, comportamento compacto e acessibilidade existentes, com shell estável na viewport e scroll controlado na área de trabalho.
- Criar `PageHeader` e `FeedbackAlert` compartilhados, mantendo cards, formulários, estados de resultado e detalhes locais às features quando não houver abstração real.
- Padronizar cards, formulários, botões, estados de coleção/resultado/erro e apresentação de `OffsetDateTime` em `pt-BR`, no timezone local do navegador, como `dd/MM/yyyy às HH:mm`.
- Tornar explícitos em dialog os resultados locais sem correspondência de Corretoras e Ações, preservando os termos pesquisados e distinguindo `404` sem `code` de erros padronizados de provider.
- Aumentar a densidade útil das listagens com hierarquia compacta e responsiva sobre os dados já existentes, sem transformar as coleções em tabelas rígidas nem criar indicadores.
- Preferir, a partir das listagens e dos resultados locais sem correspondência, cadastro contextual em `MatDialog` com prefill transitório validado, sem submissão automática, persistência, query parameters, store global ou mudança de contrato HTTP; as rotas diretas `/corretoras/nova` e `/acoes/nova` permanecem disponíveis.
- Preservar Typed Reactive Forms, `StandardError`, toast transitório de sucesso baseado na infraestrutura Material existente, confirmação de situação cadastral não ativa, semântica financeira própria e todos os fluxos funcionais existentes.
- Permanecem fora do escopo novas funcionalidades de Dashboard, Carteiras ou Operações, backend, endpoints, autenticação, gráficos, cálculos financeiros, FX, polling, WebSocket, paginação e CRUD adicional.

Como consideração futura, Carteiras poderá evoluir para o workspace de posições, operações e resultados, com operações contextualizadas na carteira. Essa direção não altera `/operacoes`, navegação, endpoints ou qualquer comportamento nesta change e deverá ser especificada em changes próprias.

Também fica explicitamente adiado para uma change futura o refinamento de transformar a ação textual de retorno em controle compacto durante a rolagem do workspace, incluindo detectar a rolagem e restaurar o texto ao retornar ao topo. Nesta change, a ação de retorno precisa apenas permanecer disponível, navegar ao destino correto e conservar teclado e nome acessível adequados.

Como nota operacional, após a conclusão da fase de implementação do frontend será realizado reset controlado dos dados de desenvolvimento utilizados nos testes manuais, antes de uma nova rodada de testes integrados. Esse reset não integra a implementação desta change, não exige código ou migration Liquibase e não autoriza apagar dados agora.

## Capabilities

### New Capabilities

- `frontend-visual-experience`: Identidade visual, tokens semânticos, Material theme, superfícies, container, cabeçalhos, feedback, hierarquia de ações, responsividade e acessibilidade visual transversais.

### Modified Capabilities

- `frontend-application-shell`: Substituir a identidade cyan/teal pela identidade oliva aprovada e refinar toolbar, sidebar e container sem alterar a navegação funcional.
- `frontend-broker-management`: Diferenciar estados de Corretoras, tratar somente `404` sem `code` como ausência local, oferecer CTA/prefill de CNPJ e padronizar feedback e datas.
- `frontend-stock-management`: Reforçar o estado local de Ação não cadastrada, oferecer CTA/prefill de ticker e mercado e padronizar feedback e datas sem enfraquecer `StandardError`.

## Impact

- Tema e estilos globais do workspace Angular, shell/layout e componentes compartilhados de apresentação.
- Páginas e testes de Corretoras e Ações, sem alteração de models de domínio, services HTTP ou contratos backend.
- Main specs `frontend-application-shell`, `frontend-broker-management` e `frontend-stock-management` após futura sincronização; `frontend-application-foundation` permanece intacta.
- Nenhuma dependência nova, fonte/ícone/asset remoto, segunda biblioteca visual ou alteração de budgets.
- O bundle inicial implementado de aproximadamente 505,07 kB excede o warning budget de 500 kB em cerca de 5,07 kB e deverá ser acompanhado sem elevar limites por conveniência.
- O Graphify permanece sujeito ao risco operacional conhecido `[WinError 5] Acesso negado`, sem previsão de elevação de privilégio ou alteração de ACL.
