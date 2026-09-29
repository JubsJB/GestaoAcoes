## Why

O Dashboard atual apresenta os mesmos dados de posições em vários gráficos e novamente em uma tabela detalhada, o que dificulta a leitura rápida da carteira selecionada. Esta change reorganiza a página como visão geral analítica, preservando contratos e cálculos existentes enquanto reduz redundância e mantém os detalhes no fluxo de Carteira que já os oferece.

## What Changes

- Reordenar o Dashboard em cabeçalho/contexto, resumo por moeda, evolução patrimonial, distribuição da carteira, desempenho por ativo e resultados realizados secundários.
- Preservar, para cada moeda disponível, patrimônio, resultado não realizado, custo e rentabilidade, com maior destaque visual para patrimônio e resultado.
- Preservar a evolução patrimonial baseada exclusivamente nos snapshots manuais e contratos atuais, sem convertê-la em rentabilidade histórica ou reconstrução por Operações.
- Preservar a composição por ativo como resposta à distribuição do valor atual dentro de cada moeda.
- Consolidar resultado não realizado e rentabilidade por ativo em uma única apresentação de desempenho: resultado monetário principal e percentual complementar, sem remover a informação percentual nem recalcular valores.
- Remover somente do Dashboard a visualização independente `Custo × Valor atual` e a tabela completa de posições abertas.
- Manter `PortfolioPositionsComponent`, `GET /carteiras/{id}/posicoes`, contratos e uso no detalhe da Carteira.
- Manter resultados realizados por Ação no Dashboard como seção secundária temporária, sem totalização; uma migração futura para uma visão detalhada da Carteira não integra esta change.
- Preservar contexto global, `carteiraId`, ações de Ver carteira, Registrar operação e Atualizar dados, concorrência, estados, rotas e integração com shell e Operações contextualizadas, sem segundo seletor.
- Exigir reflow pelo espaço real do container, acessibilidade textual dos gráficos e validação em sidebar expandida/recolhida, mobile, 320 CSS px e zoom aumentado.
- Atualizar somente a documentação do README afetada pela reorganização, identificando capturas anteriores como históricas quando não houver substituição atual disponível.
- Incluir a extensão de escopo aprovada para o formulário de Operações: remover a identificação duplicada da Carteira no corpo e preservá-la no cabeçalho, tanto na página quanto no dialog, sem alterar regras financeiras, captura de contexto, POST ou navegação.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `frontend-dashboard-management`: alterar hierarquia, prioridade visual e conteúdo do Dashboard; retirar somente da página o comparativo Custo × Valor atual e a tabela detalhada de posições; manter resultados realizados como seção secundária e preservar contratos, contexto, evolução, distribuição, estados e ações.
- `frontend-position-analysis`: substituir os três comparativos atuais por uma análise mais simples de desempenho, sem Custo × Valor atual e sem gráfico independente de rentabilidade, mantendo resultado monetário principal, percentual complementar, moedas independentes, precisão lossless e dataset integral.

## Impact

Impacto concentrado no frontend Angular lazy do Dashboard, principalmente template/estilos da página, composição da análise por ativo, apresentação de desempenho e testes correspondentes, com extensão aprovada para a apresentação contextual do formulário de Operações e documentação afetada no README. Serão reutilizados DashboardService, EvolutionService, modelos, parser lossless, formatadores, PortfolioEvolutionComponent, PortfolioCompositionComponent, PositionPerformanceChartComponent ou sua lógica de apresentação, feedbacks, cards e contexto global.

Não haverá alteração de backend, banco, endpoint, DTO REST, fórmula financeira, conversão cambial, dependência, budget, PRD, shell ou regra de Operações. A extensão em Operações é exclusivamente de apresentação/contextualização, sem mudança funcional. A página de detalhe da Carteira e seu uso de PortfolioPositionsComponent permanecem intactos.
