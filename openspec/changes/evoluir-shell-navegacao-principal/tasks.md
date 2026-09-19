## 1. Pré-condição de implementação

- [x] 1.1 Confirmar aprovação explícita do usuário desta change antes de alterar código; revisar deltas e a matriz dos 12 critérios de aceitação em design.md.

## 2. Navegação no shell existente

- [x] 2.1 Atualizar NAVIGATION_ITEMS para Dashboard, Carteiras, Operações, Ativos e Corretoras na ordem e URLs especificadas, usando ícones locais existentes sem renomear features ou contratos.
- [x] 2.2 Implementar estado local desktop expandido/recolhido no MainLayoutComponent, controle acessível e largura recolhida, preservando header, marca, CarteiraSelectorComponent, AppIconComponent, skip link, main e router-outlet.
- [x] 2.3 Separar estado desktop do drawer mobile e tratar transições no breakpoint atual, preservando side/over, fechamento por Escape, backdrop e navegação, sem persistir recolhimento após reload.
- [x] 2.4 Preservar nomes acessíveis dos links com textos ocultos, aria-current em rotas filhas, foco visível, foco no controle ao alternar e suporte a movimento reduzido.

## 3. Testes de comportamento e regressão

- [x] 3.1 Atualizar testes de MainLayoutComponent e itens de navegação para ordem, rótulos, URLs, Operações, Ativos, destino ativo em ambos os estados e rotas filhas.
- [x] 3.2 Testar alternância desktop por clique/Enter/Espaço, estado entre navegações/reload, transições desktop/mobile nos dois sentidos, drawer inicialmente fechado, Escape, backdrop e fechamento por seleção/NavigationEnd.
- [x] 3.3 Testar Tab/Enter, nomes e estados acessíveis, foco visível e preservado, skip link transferindo foco ao conteúdo e funcionamento do seletor em todos os modos.
- [x] 3.4 Cobrir acesso pelo menu ao histórico global com dados de duas Carteiras e troca do seletor sem filtro implícito, nova consulta contextual ou inserção automática de parâmetros no link.
- [x] 3.5 Executar regressão de URLs/deep links/reload, raiz e NotFound, voltar/avançar, carteiraId/origem, captura fixa da Carteira no formulário, cancelamento/conclusão e retorno determinístico, sem alterar contratos ou cálculos.

## 4. Validação final após implementação aprovada

- [x] 4.1 Verificar visualmente desktop expandido/recolhido e mobile, header/seletor, rota ativa, teclado/foco, reduced motion, zoom 200%, 320 CSS px, baixa altura, alvos/contraste e ausência de overflow/double scroll conforme specs existentes.
- [x] 4.2 Executar testes frontend existentes e build de produção pelos scripts vigentes, registrar resultados e corrigir somente regressões causadas por esta change.
- [x] 4.3 Revisar diff e os 12 critérios de aceitação, confirmar preservação de Dashboard/páginas, APIs e regras financeiras e ausência de funcionalidades futuras, alterações backend/banco e dependências novas.
- [x] 4.4 Executar `graphify update .` após alterações de código e apresentar resultado da implementação para revisão, sem presumir autorização de commit, merge ou arquivamento.

## Evidências de implementação e validação manual final

- Estado final: 14 tasks concluídas, nenhuma pendente. As tasks 3.2, 3.3, 3.5 e 4.1 foram concluídas com a cobertura automatizada já registrada e a confirmação explícita do usuário de que realizou e aprovou todas as validações manuais restantes.
- 3.2: ativação dos controles por teclado, sidebar recolhida preservada durante navegação e expandida após reload; drawer fecha por Escape, backdrop e navegação; transições desktop/mobile sem backdrop ou foco residual. Testes automatizados complementam clique, drawer inicialmente fechado, estados e NavigationEnd.
- 3.3: Tab e Shift+Tab com foco visível e ativação por teclado aprovados. A aprovação das validações restantes complementa os testes DOM de nomes/estados acessíveis, foco preservado, skip link e seletor.
- 3.5: voltar/avançar, acesso direto e reload das rotas, indicação ativa em rotas filhas e fluxos carteiraId/origem aprovados conforme contrato vigente. Testes existentes cobrem raiz/NotFound, captura fixa da Carteira, cancelamento/conclusão e retorno determinístico.
- 4.1: confirmação visual anterior de header, Dashboard e Operações mobile, cards e sidebar expandida/recolhida; confirmação final de rota ativa nos dois estados, zoom 200%, 320 CSS px, ausência de scroll horizontal/conteúdo cortado e contraste, foco e alvos visualmente adequados. O usuário declarou aprovadas todas as validações manuais restantes da matriz; não foram produzidas novas medições ou capturas pelo agente.
## Escopo e coerência documental

- Menu/shell e dois arquivos de testes; correções CSS no header/seletor, Ações, Carteiras, Corretoras, coleções compartilhadas e posições. Removidas alturas calculadas, overflow oculto e scroll interno concorrente das três listagens. Reflow por largura do componente nos limiares existentes. Sem reorganização do Dashboard, alterações backend/banco, contratos, cálculos, dependências ou budgets.
- Proposal/design e delta do shell reconciliados com as correções responsivas autorizadas: rolagem no main, conteúdo sem corte e tabela semântica única. Delta de Operações preservado. PRD e specs consolidadas não alterados.
- README.md: o prefixo acidental `codex` foi removido exclusivamente da primeira linha, conforme autorização explícita no fechamento. O arquivo voltou a `# Gestão de Ações`, sem outras diferenças em relação a HEAD. Nenhuma outra edição foi realizada no README.
- Carteiras e Corretoras aparecem tanto em `git diff --stat` quanto em `git status --short` nesta revisão. Ambos contêm alterações CSS necessárias à correção de corte. Sem alterações staged ou flags assume-unchanged/skip-worktree; origem da omissão no relatório anterior não determinável sem as saídas originais.

## Histórico técnico e bundle

- Ambiente registrado na implementação: Windows, Node 24.19.0, npm 11.17.0, Angular 22.1.4; scripts em frontend/.
- Antes da change: initial 514,40 kB. Primeira implementação: 516,52 kB (+2,12 kB). Após correções responsivas: 516,61 kB (+0,09 kB; +2,21 kB sobre baseline). Valores anteriores são evidências registradas na implementação, não reconstruções desta revisão.
- Execução anterior às correções e execução posterior: 68 arquivos, 505 testes aprovados. Build posterior: sucesso, transferência estimada 122,47 kB; initial 516,61 kB, portfolio-positions 4,15 kB e portfolio-evolution 5,68 kB.
- Initial: warning preexistente agravado pela change. Portfolio-evolution: preexistente, sem modificação do componente/estilo. Portfolio-positions: warning introduzido pelas correções responsivas e CSS compartilhado. Budgets preservados (500 kB initial/4 kB estilos para warning; 1 MB/8 kB para erro).
- Causa provável do aumento inicial: estado e bindings da sidebar, controle/template e CSS do shell/seletor. Correções de coleção afetam principalmente chunks lazy e estilos dos componentes. Testes são excluídos de tsconfig.app.json; package.json, lockfile e angular.json não possuem diff. Nenhuma biblioteca ou novo módulo externo foi adicionado; computed/effect vêm de Angular já utilizado. Não há atribuição exata de bytes por causa sem comparação de builds instrumentados.
- Aceite explícito no fechamento: o usuário aprovou o crescimento do initial de 514,40 kB para 516,61 kB (+2,21 kB; ~0,43%) como exceção específica desta change ao critério consolidado de não crescimento. Também aprovou o warning de portfolio-positions. Estado, bindings, template e CSS necessários justificam o crescimento; nenhuma nova dependência, budget alterado ou código claramente dispensável. Não realizar micro-otimizações nem aumentar budgets. Decisão registrada em design.md.

## Graphify

- Última atualização após correções de código: 8346 nodes, 14359 edges e 725 communities; graph.json, graph.html e GRAPH_REPORT.md atualizados. AST sem LLM. Avisos registrados: seis arquivos de configuração/JSON sem nodes e 50 comunidades renomeadas; visualização agregada acima de 5000 nodes.
- Revisão final consultou o grafo existente. Nenhuma alteração estrutural/de código neste fechamento; somente artefatos de planejamento e remoção do prefixo acidental do README, portanto sem nova atualização. graphify-out/ é ignorado pelo Git.
- Nenhum git add, commit, push, merge ou arquivamento autorizado nesta revisão.

## Validações técnicas da revisão anterior

- `npm test -- --watch=false`: 68 arquivos e 505 testes aprovados, zero falhas (26,53 s). Primeira tentativa impedida por `spawn EPERM` no sandbox; repetição com escalonamento autorizado concluída.
- `npm run build`: sucesso; initial 516,61 kB, transferência estimada 122,47 kB. Confirmados os três warnings e tamanhos do histórico acima; nenhum erro de budget.
- `openspec validate evoluir-shell-navegacao-principal --strict`: change válida.
- `git diff --check`: sem erros de whitespace; avisos de conversão LF/CRLF em cinco arquivos frontend. Os artefatos OpenSpec ainda não rastreados também foram inspecionados separadamente, pois não entram no diff padrão.
- Nesta revisão foram editados somente proposal.md, design.md, delta de frontend-application-shell e tasks.md; corrigidos registros desatualizados e texto corrompido nas evidências das tasks. Código, README e PRD preservados.

## Fechamento autorizado

- Todas as 14 tasks foram revisadas e concluídas. A validação manual restante foi aprovada explicitamente pelo usuário, complementando a inspeção de código e os testes já registrados. Commit e arquivamento continuam sem autorização.
- Histórico de Operações preservado global via GET /operacoes. A futura change contextualizar-historico-operacoes-por-carteira não foi criada; não foram antecipados observação de contexto, endpoint contextual ou contratos de retorno.
- Neste fechamento, apenas proposal.md, design.md, tasks.md e o prefixo acidental do README foram editados. Deltas revisados e mantidos: representam navegação, responsividade e histórico global efetivamente implementados. Sem novas funcionalidades, alterações de código, backend, banco, dependências ou budgets.
- Validação final: `npm test -- --watch=false` aprovou 68 arquivos e 505 testes, zero falhas, em 10,20 s. A tentativa no sandbox falhou com spawn EPERM; a primeira execução escalonada teve 7 testes falhos em 6 arquivos, com timeouts de 5 s. A repetição isolada do mesmo comando passou integralmente, sem alterações de código, configuração ou timeout. As falhas não se reproduziram; possível contenção de recursos, sem causa comprovada.
- `npm run build`: sucesso, initial 516,61 kB e transferência estimada 122,47 kB. Warnings: initial +16,61 kB sobre 500 kB; portfolio-positions 4,15 kB (+153 bytes); portfolio-evolution 5,68 kB (+1,69 kB). Nenhum impede o build; crescimento e warning novo aceitos explicitamente conforme design.md.
- `openspec validate evoluir-shell-navegacao-principal --strict`: válido. `git diff --check`: sem erros de whitespace, somente avisos LF/CRLF. README sem diff e index sem alterações staged. Nenhum commit, push, merge ou arquivamento realizado.
