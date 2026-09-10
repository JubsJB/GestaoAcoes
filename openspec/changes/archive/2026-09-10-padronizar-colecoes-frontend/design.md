## Context
Posicoes usa PortfolioPositionsComponent compartilhado com detalhe de Carteira. Acoes, Corretoras e Carteiras usam collection.scss e tabela unica; Carteiras apresenta o mesmo acabamento plano. PRD preservado: consulta de ativos/corretoras/posicoes, sem novos dados.

## Goals / Non-Goals
Acabamento desktop consistente e denso, sem duplicar tabelas. Nao alterar graficos, formularios, ordenacao, filtros, formatadores, rotas, HTTP ou dominio.

## Decisions
Reutilizar collection.scss por uma folha refined-collection.scss opt-in nas quatro colecoes; evitar alterar outras colecoes como Operacoes. Bordas suaves, caption integrada, cabecalhos discretos, identidade forte e metadados secundarios. Valores atuais em destaque e estados com texto e cor via label lossless existente. Reutilizar status-badge atual em mercado/moeda e situacao, sem nova classificacao funcional.
Carteiras entra somente pelo acabamento compartilhado e link de detalhes. Componente de posicoes reflete a mesma apresentacao no detalhe de carteira sem duplicar implementacao.
Manter breakpoints e uma unica estrutura, com wrapping. Sem overflow hidden novo e sem hover de linha falsamente clicavel.

## Risks / Trade-offs
SCSS compilado nos componentes lazy pode crescer modestamente: medir production e budgets sem alteracao. Densidade de oito colunas: validar 1440x900 e reflow estrutural, mantendo textos completos.
## Migration Plan
Somente frontend; sem migracao, contratos, dependencias ou budgets alterados. Revisao humana ao final, sem archive automatico.
