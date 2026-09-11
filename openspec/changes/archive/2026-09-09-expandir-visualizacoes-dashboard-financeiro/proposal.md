## Why

O Dashboard aprovado apresenta dados corretos, mas custo versus valor atual exige interpretação excessiva e falta uma leitura direta da distribuição das posições. Esta change melhora compreensão e composição visual sem mudar o significado financeiro, seguindo a referência A (primeiro modelo do usuário, organização geral) e a referência B (demais exemplos financeiros, leitura dos gráficos).

## What Changes

- **BLOCO 1 — Refinar os gráficos existentes:** propor Custo × Valor atual como barra de valor atual com marca de custo, manter/refinar barras divergentes de resultado e dot plot de rentabilidade, simplificar labels/legendas e apresentar zeros sem notação técnica. Não exagerar diferenças pequenas.
- **BLOCO 2 — Novas visualizações financeiras:** entregar como P0 Composição por moeda em rosca, baseada exclusivamente em valorAtualPosicao, com proporções somente geométricas e valores monetários autoritativos na legenda. Avaliar comparações adicionais sem duplicar perguntas já respondidas. Refinar o histórico como linha de registros manuais, sem criar série temporal nova.
- **BLOCO 3 — Composição final do Dashboard desktop:** organizar composição e custo/valor na primeira faixa analítica, resultado e rentabilidade na seguinte e histórico em largura total; preservar indicadores, ações, contexto e detalhes existentes.
- **Validação final:** testes, navegador, precisão, HTTP, performance e revisão humana por bloco; acabamento mobile detalhado concentrado no fechamento.

As imagens anteriores não estão disponíveis nesta rodada: não alegar comparação visual executada com elas. A referência A governa hierarquia/cards/espaço/densidade e a B tipos/legendas/leitura, conforme descrição explícita do usuário; o Dashboard aprovado é a base existente. Antes do código, o usuário revisará estes tipos e a composição proposta. Se necessário para conferir fidelidade, as imagens devem ser disponibilizadas novamente; não buscar ou inventar substitutas.

Fora do escopo: backend, contratos, endpoints, requests adicionais, persistência, novas fórmulas, FX, total global, percentuais de composição exibidos/calculados como métricas, rentabilidade histórica, benchmark, dividendos, setores, alocação por corretora e reconstrução de posições históricas. Nenhuma alteração à change arquivada. Planejamento aprovado; Bloco 2 autorizado explicitamente sem aprovar o resultado visual do Bloco 1, preservado para revisão posterior.

## Capabilities

### New Capabilities
- `frontend-portfolio-composition`: distribuição visual das posições por moeda, com ângulos derivados somente para desenho e valores autoritativos acessíveis.

### Modified Capabilities
- `frontend-position-analysis`: substituir barras pareadas por comparação barra/referência; padronizar apresentação lossless amigável, preservar resultado divergente e dot plot e distinguir perguntas visuais.
- `frontend-dashboard-management`: composição desktop com nova distribuição e prioridade visual dos gráficos, preservando contexto e seções.
- `frontend-portfolio-evolution`: apresentação de linha temporal mais clara, mantendo integralmente registros manuais, coordenadas e interações existentes.

## Impact

Frontend Angular lazy do Dashboard, seus componentes de análise/evolução e testes. Reutilizar SVG/CSS, agrupamento e formatadores existentes; nenhum pacote de gráficos necessário. Mudanças em helpers de apresentação limitadas à feature, sem alterar parser, models ou comportamento financeiro global. Specs canônicas serão apenas propostas como deltas nesta rodada.

Base: PRD RF17–RF24, seções 12.5–12.8 e RNF07/08/10; contratos atuais auditados no design. Checkpoint a0429c8, branch feature/frontend-angular, working tree inicialmente limpo. Baseline aprovado reutilizado: initial 514417 B, Dashboard lazy 52387 B; detalhes de CSS e warnings no design. Nenhuma otimização, teste/build ou implementação nesta rodada.

## Prioridades da revisão visual

Modernização exige variedade real: P0 inclui barra/referência, divergente, dot plot, rosca e linha temporal ampla, além de zeros amigáveis e composição desktop. A escolha segue pergunta → dado autoritativo → temporalidade → gráfico, não somente CSS. P1: área decorativa segmentada opcional no histórico, sem alterar pontos ou gaps. P2: novas séries históricas de resultado/custo/rentabilidade, com dados faltantes e impactos documentados no design, fora da implementação desta change. Rosca é obrigatória, preferida à pizza; centro identifica moeda, sem total derivado ou falsa sincronização com resumo.
