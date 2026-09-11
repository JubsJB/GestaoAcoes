# Bloco 3 — composição desktop final (09/09/2026 local)

## Aprovação e tarefas

Histórico aprovado humanamente pelo usuário antes desta rodada: task 2.8 concluída. Preservados diagnóstico de valores constantes, snapshots #1/#5/#8 e seleção individual. Implementadas tasks 3.1–3.5. Task 3.6 aberta para revisão humana da composição final; validação final 4.1–4.5 não executada/encerrada. Total 27: 21 concluídas, 6 abertas. Nenhum gráfico novo, archive ou versionamento.

## Layout

Ordem DOM/visual: cabeçalho, contexto/ações, indicadores BRL/USD, Análise por ativo com composição, custo/valor, resultado, rentabilidade, Histórico do patrimônio, posições abertas, resultados realizados.

- Contexto e ações Ver carteira/Registrar operação/Atualizar dados compartilham faixa desktop; Registrar patrimônio atual permanece no histórico. Mesmos handlers, estados e destinos.
- Indicadores em dois grupos monetários lado a lado acima de 70rem de viewport. Patrimônio destacado ocupa a primeira linha do grupo, três indicadores secundários abaixo. Nenhum indicador removido/totalizado.
- Primeira faixa analítica: composição à esquerda 40%, custo à direita 60%, quando container comporta 60rem. Em 1440×900, área útil 1105px, painéis ~435,59px e ~653,41px separados por 16px. A nova direção do usuário atualiza a ordem inicial do planejamento; specs/deltas ajustados sem regra financeira nova.
- Composição: apenas ajuste interno de layout coloca rosca ao lado da legenda quando couber no card. Custo: grupos BRL/USD usam duas colunas a partir de 36rem úteis. Escalas/valores/geometria continuam iguais; widths CSS adaptam os desenhos aprovados.
- Resultado e rentabilidade permanecem em duas colunas equivalentes. Histórico ocupa a largura disponível, com seus dois gráficos e seleção de registros preservados. Tabelas/detalhes continuam abaixo, sem ocultação, accordions ou paginação nova.
- Sem alturas fixas para igualar cards; alinhamento pelo topo, conteúdo integral determina altura. CSS responsivo empilha quando necessário. Não redesenhados marcadores, cores, tooltip ou dados dos gráficos.

## Comparação renderizada

Reutilizada infraestrutura Edge headless existente com GETs reais da carteira Americana id 5. Desktop 1440×900: indicadores lado a lado (~544,5px cada), painéis de composição/custo com mesmo Y, resultado/rentabilidade em pares; nenhuma duplicação ou overflow global detectado. Altura antes 4310,11px e depois 3581,16px na medição inicial equivalente, redução ~728,95px (~16,9%). Varia com dados/seleção/tooltip; não constitui meta fixa nem resultado para todo dataset. Não houve redução de dados.

768×1024, 1280×600, 390×844 e 320×740: checagem estrutural breve aprovada pelo ensaio existente, sem acabamento mobile fino. Estado do selector/drawer e seleção real do registro #5/tooltip preservados. Ensaios mantêm os diagnósticos internos previamente conhecidos de pontos/menu/tabelas, sem overflow global; não alegar revisão assistiva completa.

Ensaio adicional desktop com fixture existente de 100 ativos, nomes longos e valores extensos passou nos limites estruturais e contagem de ativos. Fixture serve para geometria/renderização, não evidência de cálculos financeiros. Capturas/relatórios/scripts somente em frontend/.cache/expansion-bloco3 ignorado. Desktop.png e desktop-composition.png inspecionados; aprovação estética permanece humana. Referências A/B seguidas conceitualmente conforme descritas pelo usuário, imagens originais não disponíveis nesta sessão.

## Arquivos alterados nesta rodada

- dashboard-page.component.html/.scss/.spec.ts: faixa de ações/contexto, grade compacta de indicadores, testes de ações, instâncias únicas e requests.
- position-analysis/position-analysis.component.ts: composição/custo agrupados em ordem final e grade por container.
- position-analysis/position-chart.scss: largura dos grupos de moeda no comparativo, sem geometria financeira.
- position-analysis/portfolio-composition.component.scss: rosca/legenda em layout horizontal quando legível.
- position-analysis/position-performance-chart.component.spec.ts: expectativa de ordem na integração e dois painéis preservados.
- OpenSpec: proposal/design/tasks, delta frontend-dashboard-management, aprovação em evidencias-diagnostico-historico e esta evidência nova. Nenhum componente novo criado.

Reutilizados todos os componentes existentes. Nenhuma alteração aos services, endpoints, DTOs, parser, models, fórmulas, contextos/rotas, histórico TS/geometry, biblioteca ou dependência. Quatro GET financeiros por carga: resumo, posições, resultados-realizados, evolução-patrimonial; listagem compartilhada do shell permanece separada. Nenhuma soma BRL/USD, FX ou métrica nova.

## Validações

- Focados Dashboard: 152/152, 14 arquivos.
- Suíte completa após os ajustes finais de CSS: 485/485, 68 arquivos, VITEST_MAX_WORKERS=2.
- Production com stats-json: aprovado.
- Testes existentes de valores/dataset/HTTP preservados; ajuste localizado de integração para ordem composição/custo e verificações de componente único, ações e quatro consultas. Sem reescrever testes financeiros.

| Medida (bytes) | Antes da rodada | Depois | Delta |
| --- | ---: | ---: | ---: |
| Initial | 514416 | 514416 | 0 |
| Dashboard lazy | 65017 | 66669 | +1652 |
| CSS Dashboard | 2522 | 3091 | +569 |
| CSS contêiner analítico | 178 | 415 | +237 |
| CSS custo | 1905 | 2046 | +141 |
| CSS composição | 1882 | 2168 | +286 |
| CSS resultado/dot | 2500 | 2500 | 0 |
| CSS evolução | 5685 | 5685 | 0 |

Fonte stats.json (initial por imports estáticos transitivos, Dashboard por entryPoint, CSS por outputs). Crescimento lazy ~2,54%, proveniente de wrappers/grade e CSS de composição; CSS embutido não somado novamente ao JS. Nenhum código promovido para initial. Baseline original da change permanece initial 514417 / lazy 52387; tabela acima compara estado real imediatamente anterior.

Warnings mantidos: initial +14416 B sobre aviso 500kB; evolução +1685 B sobre aviso 4kB. Budgets 500kB/1MB e 4/8kB intactos, critério initial <=514418 atendido. Nenhuma otimização ou dependência nova.

## Revisão humana pendente

Task 3.6: avaliar densidade dos indicadores, proporção 40/60, tamanho da rosca/legenda, leitura dos grupos BRL/USD no custo, conjunto resultado/rentabilidade, continuidade para histórico e detalhes. Task 2.8 já aprovada, não reabrir por mera reorganização. Parar após Bloco 3; não avançar para validação final/encerramento sem revisão do usuário.

Validação documental: strict da change aprovado, strict global 33/33, git diff --check sem erros. Git emitiu aviso de normalização LF para CRLF em position-chart.scss, sem erro de whitespace. Status mantém arquivos dos três blocos e documentação, sem staging; caches/capturas ignorados. Comparação SHA-256 com início da rodada confirma alterações somente nos sete arquivos frontend listados acima.

## Aprova??o humana posterior ? 09/09/2026

O usu?rio aprovou explicitamente a revis?o visual humana do Bloco 3 e autorizou concluir a task 3.6. O registro de revis?o pendente acima descreve o estado anterior ? aprova??o e permanece como hist?rico. Esta aprova??o cobre a composi??o desktop; n?o substitui as valida??es finais espec?ficas das tasks 4.1 e 4.2.
