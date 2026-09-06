# Evidências das Fases 1–3

Data: 2026-09-06. Change: refinar-experiencia-visual-frontend.
Escopo autorizado: fundação visual, shell, componentes compartilhados e dialogs. Fases 4–7 não iniciadas.

## Baseline técnico e limites

- Branch: feature/frontend-angular.
- Estado inicial: apenas o diretório desta change não rastreado; nenhum arquivo rastreado modificado.
- Ambiente: Windows, Node v24.19.0, npm 11.17.0; dependências já presentes.
- Build baseline: npm run build -- --configuration production.
- Main: 492,09 kB; CSS global: 13,99 kB; initial: 506,09 kB; transferência estimada: 116,15 kB.
- Lazy baseline (exemplos): Dashboard 37,13 kB; Ações listagem 14,02 kB; Corretoras listagem 13,21 kB; Carteira detalhe 11,52 kB. Demais chunks mantidos pelo build Angular.
- Budgets inalterados: initial warning 500 kB / error 1 MB; estilo por componente warning 4 kB / error 8 kB.
- Testes baseline: npm test -- --watch=false: 54 arquivos, 325/325 aprovados.
- Build/testes no sandbox falharam com spawn EPERM do esbuild. Reexecução autorizada fora do sandbox passou. Nenhuma dependência foi instalada.
- Sem alterações em backend, services, models, HTTP, parsing, formatadores, rotas, query params, manifests, lockfile ou angular.json.

## Matriz de rastreabilidade e regressão

| Capabilities / requisitos | Evidência técnica / proteção | Revisão visual |
| --- | --- | --- |
| Visual: tokens, superfícies, tipografia e estados | SCSS compartilhado, pares de contraste abaixo, build de produção | Paleta, densidade, hierarquia e estados pendentes |
| Shell: side/over, destinos, skip link, aria-current e rolagem | main-layout.component.spec.ts: navegação, foco no main, drawer, Escape/backdrop e estrutura; TS/HTML do shell inalterados | Viewports, foco encoberto e double scroll pendentes |
| PageHeader | Testes de h1 único, projeção de ação, ícone decorativo e novo cenário sem ícone | Reflow e ação em nome longo pendentes |
| Feedback e toast | Testes existentes de role/live region, StandardError, mensagem, fechamento, duração e serviço de toast | Contraste renderizado e overlays pendentes |
| Dialogs de Corretoras, Ações e Carteiras | Specs unitárias/integradas de dialogs incluídas nos testes focados; gatilhos/handlers/configurações preservados | Contenção/restauração real de foco e baixa altura pendentes |
| Operações e Carteira contextual | Suíte existente completa protege COMPRA/VENDA, prévias, sugestões, payloads e contexto; páginas/services não alterados | Fluxos visuais específicos ficam nas fases posteriores |
| Dashboard e evolução | Suíte existente completa protege precisão, moedas, snapshots, gaps e concorrência; implementação destas features não alterada | Refinamento específico não iniciado |
| Precisão, formatos e HTTP | Specs existentes de lossless-json, financial-value.formatter, normalização HTTP e serviços na suíte completa | Nenhum novo cálculo ou request |

Os testes determinísticos usam a infraestrutura já existente. Não houve produção de dados reais, manipulação de banco ou fabricação de evidência manual.

## Estados e inventário compartilhado

Inspeção estática confirmou estados existentes: spinner/loading textual; .app-state para empty/loading; FeedbackAlert com success/info/warning/error e detalhes; SuccessToast com role=status e fechamento; validação contextual e confirmação em dialogs. Nenhum estado foi criado com números financeiros simulados.

Tokens compartilhados realmente utilizados: marca/superfícies/textos/bordas/radius em shell, PageHeader e padrões; success/error em feedback e toast; positive/negative financeiro nos consumidores existentes. Hardcodes repetidos de feedback e badges foram substituídos por papéis semânticos. Gradientes ornamentais e sombras repetidas da fundação foram simplificados. Estilos duplicados de dialogs foram centralizados em shared/dialog/dialog.scss, consumido pelas quatro classes de dialog sem criar wrapper ou alterar seus handlers. Estilos específicos de exclusão continuam locais; não são carregados globalmente por necessidade de uma única ação.

Tabular-nums é herdado da base tipográfica, incluindo valores atuais, sem alterar formatação ou reorganizar coleções. Alinhamento e tabelas específicas continuam reservados às fases posteriores.

## Identidade e contraste estático

Marca anterior #5f6f52 passa a #24634b; toolbar antes preenchida pela marca passa a superfície clara com marca localizada. Superfícies neutras, menor ornamentação, labels compartilhadas de .875rem e raios discretos. Não há dark mode.

Cálculo de contraste por luminância relativa sRGB dos tokens sólidos (não equivale a inspeção do CSS computado ou validação WCAG completa):

| Primeiro plano | Fundo | Razão |
| --- | --- | --- |
| on-brand-primary | brand-primary | 7.09:1 |
| on-brand-primary | primary-hover | 9.84:1 |
| text-secondary | surface-card | 6.35:1 |
| text-secondary | surface-app | 5.90:1 |
| success | surface-success | 6.37:1 |
| warning | surface-warning | 6.41:1 |
| error | surface-error | 6.42:1 |
| info | surface-info | 6.27:1 |
| financial-positive | surface-card | 6.12:1 |
| negative | surface-card | 6.87:1 |
| on-brand-primary | destructive | 8.00:1 |
| focus | surface-card | 7.22:1 |
| focus | surface-selected | 6.19:1 |
| border-control | surface-card | 3.79:1 |
| on-brand-primary | accent | 5.49:1 |

Marca/primária, hover, foco, resultado positivo/negativo, sucesso, aviso, erro, informação e destruição têm papéis separados. Token negativo existente foi preservado nominalmente. Tokens novos incluem info/destructive/focus, superfícies de feedback, borda de controle, spacing, sombras e escala de texto. Não houve renomeação cosmética.

## Matriz para a parada humana

Não foram realizadas capturas renderizadas, interação manual de navegador ou inspeção com leitor de tela nesta etapa. A task 1.3 permanece aberta; não há baseline visual capturado para alegar comparação antes/depois. A inspeção estática e os testes não substituem essa evidência.

Validar o conjunto nas larguras 1440x900, 768x1024, 390x844 e 320 CSS px; fronteiras 959/960/961px; 1280x600 e paisagem compacta; texto 200% e zoom/reflow equivalente a 320 CSS px. Cobrir:
- Toolbar/sidebar, PageHeader com/sem ícone, nomes longos e ações.
- Alertas de todas as variantes e toast sem cobrir controle essencial.
- Cadastro/edição/ausência local/confirmacão/exclusão em dialog, Escape/backdrop, Tab, foco contido e restaurado.
- Skip link, foco visível/não encoberto, ordem de headings, labels e alert/status com leitor de tela.
- Contraste computado, hover, disabled, alvos, reduced-motion e feedback sem dependência exclusiva de cor.
- Retorno StickyBack, baixa altura, ausência de overflow/double scroll.
- Compatibilidade visual global de páginas existentes, sem executar redesign das Fases 4–6.

Pendências explícitas: 1.3 (capturas baseline), 2.2 (comparação visual da paleta; implementação e cálculo prontos), 2.9 (regressão visual transversal; budgets técnicos verificados), 3.11 (verificação manual completa de foco/scroll; cobertura automatizada passou).

## Resultado técnico desta etapa

- Focados: 17 arquivos, 61/61 aprovados (shared, layout e specs de dialogs).
- Suíte final: 54 arquivos, 326/326 aprovados. Um teste acrescentado para PageHeader sem ícone.
- Build final production aprovado: main 491,84 kB + styles 14,25 kB = initial 506,09 kB.
- Initial antes/depois: 506,09 → 506,09 kB, estável na precisão arredondada exibida pelo CLI; transferência estimada 116,15 → 116,12 kB. Não se afirma igualdade em bytes a partir de valores arredondados.
- CSS global isolado aumentou 13,99 → 14,25 kB; main diminuiu. Não se afirma redução de todos os chunks: a consolidação de estilos dos dialogs altera os chunks lazy; rotas e limites lazy permanecem intactos.
- Warning preexistente permanece: initial 6,09 kB acima de 500 kB. Nenhum warning de estilo por componente no build final.
- Build e suíte executados como validação do escopo 1–3, sem concluir ou iniciar tasks da Fase 7.
- Única revisão textual: “A situação cadastral retornada pelo backend não é ATIVA.” → “A situação cadastral consultada não é ATIVA.” Mensagem do erro e situação retornada preservadas.

O token app-action-primary referencia a marca inicialmente, mantendo papel independente para ações. A última alteração após a suíte foi somente esse alias de tema e remoção de uma declaração CSS redundante; o build production foi repetido.

OpenSpec strict da change aprovado; strict global 32/32; git diff --check sem erros. Git avisa normalização futura LF/CRLF nos arquivos editados, sem comandos Git mutáveis executados. As 23 tasks marcadas pertencem exclusivamente às Fases 1–3.
