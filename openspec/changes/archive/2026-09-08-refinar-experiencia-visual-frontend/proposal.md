## Why

O frontend já é funcional e possui uma base Material/CDK reutilizável, mas cards homogêneos, estilos duplicados, hierarquia financeira discreta e linguagem técnica dificultam leitura e comparação. Refinar essa base agora permitirá uma experiência própria de plataforma de investimentos moderna, limpa e profissional, preservando integralmente o comportamento aprovado.

## What Changes

- Consolidar tokens, superfícies, tipografia, espaçamento, elevação, foco e feedback; manter verde como marca, permitindo refinar o oliva e seus hexadecimais com contraste verificável.
- Refinar shell, PageHeader, alertas, toast, retorno, sprite local, badges e dialogs, distinguindo marca, sucesso, lucro, prejuízo, erro e ação destrutiva.
- Adotar apresentação comparável no desktop e cards completos no mobile para posições, Operações e histórico contextual; usar tabela desktop/cards mobile para Ações e Carteiras e tabela desktop/cards mobile para Corretoras. Carteiras indica o contexto ativo apenas por leitura do serviço global existente.
- Organizar visualmente formulários e detalhes sem alterar campos, validações, máscaras, formatadores ou fluxos.
- Ordenar o Dashboard em contexto, ações, indicadores por moeda, evolução, posições e resultados realizados por ação, mantendo evolução independente.
- Refinar legibilidade e interação acessível do SVG, tooltip e histórico sem mudar dataset, geometria financeira, gaps, timestamps ou registro manual.
- Usar linguagem do investidor, preservando detalhes técnicos necessários para diagnóstico e requisitos.
- Validar acessibilidade WCAG 2.2 AA aplicável, responsividade, regressão funcional e bundle com zero dependências novas e budgets inalterados.

## Capabilities

### New Capabilities

Nenhuma. A base visual e as oito áreas abaixo já possuem especificações.

### Modified Capabilities

- `frontend-visual-experience`: identidade refinável, tokens, hierarquia tipográfica, padrões compartilhados, linguagem, coleções, acessibilidade e preservação funcional/performance.
- `frontend-application-shell`: apresentação discreta e coerente com a identidade refinada, sem alterar navegação, breakpoint ou rolagem.
- `frontend-dashboard-management`: hierarquia da página, protagonismo do patrimônio e apresentação comparável de posições/resultados.
- `frontend-portfolio-evolution`: legibilidade responsiva e interação acessível de pontos e tooltip.
- `frontend-broker-management`: tabela desktop/cards mobile e detalhe cadastral consistente.
- `frontend-stock-management`: apresentação tabular desktop, cards mobile e linguagem clara sobre cotação registrada.
- `frontend-portfolio-management`: tabela desktop/cards mobile com indicação passiva de contexto ativo, detalhe hierárquico com posições abertas existentes e apresentação do histórico conforme Operações.
- `frontend-operation-management`: histórico tabular responsivo, detalhe e agrupamento visual do formulário existente.

## Impact

A change permanece pausada após as Fases 1–3. A base funcional vigente incorpora a change arquivada `centralizar-contexto-carteira-frontend`: seletor e contexto de Carteira no shell, Dashboard sem seletor local, detalhe com posições abertas e cadastro de Operação prioritariamente contextual com Carteira fixa desde a abertura. Operações permanece fora da sidebar; suas rotas globais continuam por compatibilidade/deep link. Esta reconciliação documental não retoma as Fases 4–7 nem altera evidências anteriores.

A futura implementação será restrita à apresentação Angular, estilos, assets SVG locais quando necessários e testes relacionados. A change será implementada em sete fases revisáveis; nesta etapa somente seus artefatos são criados.

IN SCOPE: fundação visual, shell, componentes compartilhados, todas as páginas das cinco features, evolução, estados e validação visual/interativa. Referências conceituais: XP (acabamento), Investidor10 (comparação financeira), Status Invest (organização modular) e BRAPI (clareza tecnológica e verde), sem copiar identidade, layout ou assets proprietários.

OUT OF SCOPE: backend, endpoints, DTOs, banco, migrations, regras financeiras, arredondamento, parsing lossless, máscaras/formatadores, FX, totalizações, novos dados ou requisições, rotas/query params, filtros/ordenação/paginação/buscas novas, automação de snapshots, dependências e aumento de budgets. Dark mode, seletor e persistência de tema ficam fora; apenas preparar a semântica dos tokens é permitido. Graphify não será executado nesta change por instrução explícita.

Base de produto: PRD RNF07/RNF08/RNF10, seções 17 e 18; os contratos específicos vigentes delimitam o que pode ser apresentado. Resultado realizado total não existe no resumo e não será criado no frontend.


## Apresentação aprovada do histórico patrimonial

A evolução patrimonial será apresentada como “Histórico do patrimônio”, explicando registros manuais por moeda; a ação visível será “Registrar patrimônio atual”. Esta reconciliação é somente de linguagem/apresentação: nomes técnicos e contratos de snapshot permanecem intactos. Novos gráficos por ativo ficam fora desta change.
