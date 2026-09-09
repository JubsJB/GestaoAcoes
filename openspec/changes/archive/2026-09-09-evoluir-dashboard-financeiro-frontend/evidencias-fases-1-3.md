# Evidências da entrega incremental — 08/09/2026

## Escopo e origem do aceite

O usuário aprovou o planejamento e autorizou somente Fases 1–3, com integração necessária do primeiro gráfico. A hierarquia aprovada é contexto → ações → indicadores → análise → Histórico do patrimônio → posições → resultados realizados. Aprovação humana também cobre decisões de projeto sobre moedas, precisão e ausência de interação; **não constitui revisão visual da implementação nova**. Resultado não realizado e Rentabilidade continuam fora desta rodada.

Checkpoint reconfirmado antes das alterações: `feature/frontend-angular`, HEAD `b30d8b6`, referência local origin sincronizada segundo git status (sem fetch). Somente `openspec/changes/evoluir-dashboard-financeiro-frontend/` estava não rastreado. Nenhuma modificação funcional anterior foi sobrescrita. AGENTS.md, PRD, proposta, design, tasks e ambos os deltas consultados. Classificação: A para disponibilidade dos contratos, B para implementação exclusivamente geométrica/apresentacional; nenhum dado novo (C) ou regra financeira nova (D).

## Auditoria de dados e escopo

`models/dashboard.ts` / `PosicaoResponse`: `acaoId: number`; `ticker`, `nomeEmpresa`, `mercado`, `moeda`; strings `quantidadeAtual`, `precoMedio`, `custoPosicao`, `cotacaoAtual`, `dataHoraCotacao`, `valorAtualPosicao`, `resultadoNaoRealizado`, `rentabilidadePercentual`. Nenhum campo alterado. Somente custoPosicao e valorAtualPosicao alimentam as séries; quantidade, cotação e preço médio não participam da geometria.

DashboardService permanece com responseType text, parseLosslessJson, DECIMAL_FIELDS e guards atuais. O forkJoin de resumo/posições/resultados já fornece a coleção; PositionAnalysis recebe `dashboard.posicoes`, sem serviço, HTTP, subscription ou cópia de estado financeiro. Evolução conserva GET independente e POST somente pelo registro manual. O teste HttpTestingController observa exatamente `/resumo`, `/posicoes`, `/resultados-realizados`, `/evolucao-patrimonial` por carga/reload e verifica ausência de qualquer requisição remanescente após renderizar barras reais.

Diff funcional restrito a import/registro do componente e uma inserção no template do Dashboard, mais arquivos novos dentro de position-analysis. Backend, migrations, models/DTOs, serviços, parser, formatadores globais, cálculos, contratos, rotas/query params, payloads, COMPRA/VENDA, contexto global, dependências, package-lock, angular.json e budgets permanecem inalterados. Não há FX, soma monetária entre grupos ou recomputação. `evolution-geometry.ts` e toda implementação de evolução permanecem intactos. Sem Graphify ou operações Git mutáveis.

## Matriz de rastreabilidade

| Task / cenário | Evidência automatizada e técnica | Evidência humana / limite | Resultado nesta rodada |
| --- | --- | --- | --- |
| 1.1 Auditoria | checkpoint Git; leitura de models, DashboardService, página, rotas, formatter, evolução e baseline arquivado | autorização de escopo acima | Concluída |
| 1.2 Fixtures | position-analysis.fixtures.ts: objetos/arrays congelados 0/1/3/100, BRL/USD, sinais, extremos; quantidade 0.123456, preço/cotação deliberadamente incompatíveis com custo/valor | sem inferência de fixture como dado real | Concluída |
| 1.3 Matriz/captura anterior | esta matriz registra fontes reais | Dashboard anterior não foi capturado renderizado nesta rodada; não apresentar captura atual como anterior | **Aberta**, matriz parcial concluída, captura pendente |
| 2.1–2.2 Geometria | 8 testes: igualdade decimal/científica, sinais simétricos, -0, domínio vazio/todos-zero, 1e400/1e-400, expoente extenso, subpixel, dígitos além de precisão binária, invalidade distinta de zero e imutabilidade | aproximação não prova legibilidade | Concluídas |
| 2.3 Estrutura | inputs readonly, computed/OnPush, Map com ordem estável, listas/dl, ticker/nome/moeda, SVG aria-hidden e focusable=false, nenhum tab stop | leitor de tela e densidade aguardam revisão | Concluída tecnicamente |
| 2.4 Texto lossless | formatFinancialMoney sem conversão numérica nova; texto completo adicional para científico/casas relevantes; testes extremos e dígitos 9007199254740993.01/.02 | sem nova regra de arredondamento | Concluída |
| 3.1–3.2 Comparação | 15 testes de componente: vazio, único, três, 100, cada moeda isolada e ambas, igualdade, maior/menor, zeros em cada série, escalas independentes, origem congelada e ordem | nenhuma revisão visual presumida | Concluídas |
| 3.3 Sem mínimo fictício | valores pequenos permanecem textuais; razão pode ser zero por underflow sem virar zero financeiro; custo em contorno recortado e atual preenchido; rótulos permanentes; teste de estilos estruturais | contraste renderizado/interpretação aguardam revisão | Concluída tecnicamente |
| 6.x Integração parcial | ordem de headings; quatro GETs; ausência de POST; análise ausente em loading/erro; troca de contexto apaga gráfico antigo; histórico de USD 345.67 permanece com posições vazias; evolução não remonta no carregamento financeiro | só o primeiro dos três gráficos está integrado | Abertas para fechamento final |
| 7.x Acessibilidade/reflow parcial | textos fora do SVG; 100 ativos e 200 valores; sem tab stops ou animação; grid auto-fit com min(100%,22rem) e séries com min(100%,9rem), min-width:0, overflow-wrap:anywhere | não houve medição de layout em navegador; 1440/768/390/320, 200%, zoom, baixa altura e leitor de tela pendentes | Abertas |
| 8.x Validação incremental | focados 160/160, completa 436/436; build e auditoria descritos abaixo | não equivale ao encerramento das três visualizações | Abertas para validação final |

Cada moeda possui máximo geométrico próprio, compartilhado entre custo/valor de todas as suas posições. Não há ordenação, top N, agregação ou downsampling. A única estrutura textual é permanente; barras decorativas não exigem tooltip, clique ou hover. Zero não cria retângulo; igualdade produz comprimentos iguais. Custo/valor negativos inesperados ou texto inválido conservam texto e aviso de indisponibilidade na escala, sem fingir valor positivo.

## Testes executados

Ambiente existente, sem instalação de dependências. `VITEST_MAX_WORKERS=2` nas duas execuções finais:

```powershell
npm test -- --watch=false --include='src/app/features/dashboard/**/*.spec.ts' --include='src/app/shared/formatters/*.spec.ts' --include='src/app/shared/portfolio-positions/*.spec.ts' --include='src/app/core/http/lossless-json.spec.ts' --include='src/app/core/carteira/*.spec.ts'
npm test -- --watch=false
npm run build -- --configuration production --stats-json
```

- Focados: **160/160**, 16 arquivos; log local ignorado `test-position-analysis-focused.log`, 23:02:58 UTC.
- Completa: **436/436**, 64 arquivos; log local ignorado `test-position-analysis-full.log`, 23:03:18 UTC. Baseline anterior 412/412; 24 testes novos.
- Build production: aprovado; log local ignorado `build-position-analysis.log`, 23:04:43 UTC.
- Primeiro teste foi bloqueado por spawn EPERM do esbuild no sandbox; execução fora do sandbox autorizada. Primeira execução efetiva revelou duas assertions novas de igualdade binária exata (diferença de aproximadamente 1e-16); corrigidas para tolerância somente em coordenadas aproximadas. Assertions de igualdade entre séries e strings financeiras continuam exatas. Nenhuma assertion anterior enfraquecida.

## Performance reproduzível

Fonte baseline: design e evidencias-fase-7.md do archive 2026-09-08-refinar-experiencia-visual-frontend; build novo: `frontend/dist/frontend/stats.json`. Para o initial, percorrer imports não dinâmicos a partir de main/styles e somar **somente arquivos efetivamente emitidos em browser/**. CSS virtual de componente nos metadados já está embutido no JS; somá-lo novamente duplicaria bytes (por exemplo, main-layout 3202 bytes).

| Métrica | Antes | Agora | Diferença |
| --- | ---: | ---: | ---: |
| Initial | 514418 bytes | **514418 bytes** | **0** |
| Transferência estimada initial | 122,08 kB | 122,07 kB | −0,01 kB estimado |
| Dashboard lazy | ~37,16 kB | **44258 bytes / 44,26 kB** | ~+7,10 kB |
| Transferência estimada Dashboard | baseline não reconstituído nesta rodada | 11,31 kB | não afirmar delta exato |
| CSS novo position-chart | inexistente | **1161 bytes** | +1161 bytes, dentro do lazy |
| CSS evolução | 4284 bytes | **4284 bytes** | 0 |
| Styles global | 16087 bytes | **16087 bytes** | 0 |

Initial emitido: main-U44NPXCE.js 129424; chunk-DI_k_10F.js 367386; chunk-DHAIEnAO.js 887; chunk-C7MwnfEW.js 634; styles-ZAG5RE6K.css 16087. Dashboard: chunk-G8GTtYCK.js. Nenhuma entrada position-analysis aparece no grafo de imports estáticos do initial. Dashboard/evolução continuam lazy.

Classificação do crescimento:

- **A — necessário:** nova geometria, agrupamento/apresentação e componentes/markup/CSS da visualização. Metadados atribuem contribuições brutas de 1047 bytes a geometry, 435 a presentation, 5634 a CostValueChart (inclui template/estilos) e 1068 a PositionAnalysis. São contribuições brutas, **não parcelas líquidas somáveis ao baseline**.
- **B — CSS duplicado:** nenhuma duplicação evitável identificada nesta entrega; stylesheet compilado uma vez, não por ativo. Não antecipamos três cópias para gráficos ainda inexistentes.
- **C — import/dependência acidental:** nenhuma encontrada; sem Material/CDK ou biblioteca novos.
- **D — promoção indevida ao initial:** nenhuma; initial exatamente estável e grafo estático sem entradas novas da análise.
- **E — duplicação evitável:** nenhuma identificada; agrupamento/representação local compartilháveis, sem framework genérico ou duplicação de estado HTTP.
- **F — outras causas:** minificação/identificadores e compressão impedem equiparar contribuições brutas ao delta líquido; diferença de transferência estimada não representa otimização funcional. Sem nova otimização agressiva.

Warnings preservados: initial 14,42 kB acima de 500 kB; CSS evolução 4284 bytes, 284 acima de 4 kB. Nenhum warning novo. Budgets não alterados. Critério incremental de initial ≤514418 satisfeito, sem herdar dispensa antiga; task 8.2 permanece para o conjunto final.

## Revisão humana pendente

Contraste técnico dos tokens sobre branco: texto principal #202b25 14,65:1; secundário #54635a 6,35:1; barras #24634b 7,09:1; origem #77877c 3,79:1. Cálculo de luminância sRGB realizado para os tokens usados; não substitui inspeção do contorno renderizado, forced colors ou leitor de tela.

Roteiro: conferir ordem entre indicadores/histórico; carteira vazia, 1, 3 e muitos ativos; BRL/USD em grupos próprios; custo igual/maior/menor e zeros; valor minúsculo com texto completo; nomes/valores longos. Usar 1440x900, 768x1024, 390x844 e 320 CSS px, fronteiras 959/960/961, texto 200%, zoom/reflow e 1280x600. Percorrer headings/leitor de tela e Tab (barras não focáveis); verificar informação sem cor e reduced motion. Trocar Carteira e atualizar; conferir histórico, consulta dos pontos e registro manual existentes. Não autorizar Resultado não realizado/Rentabilidade por inferência dessa revisão.

Nenhuma captura anterior renderizada foi obtida; 1.3 fica aberta, sem fabricar histórico. A aprovação humana do primeiro gráfico deve ser registrada em uma rodada posterior.

## Validação documental e estado final

- `openspec validate evoluir-dashboard-financeiro-frontend --strict`: aprovado.
- `openspec validate --all --strict`: **32/32**, zero falhas.
- `git diff --check`: aprovado; avisos locais LF→CRLF do Git não são erros de whitespace. Inspeção adicional dos arquivos não rastreados não encontrou whitespace final.
- Contagem real em tasks.md: **9/30 concluídas**, 21 abertas (1.3, 4.1–4.3, 5.1–5.3, 6.1–6.4, 7.1–7.5, 8.1–8.5).
- Status: três arquivos rastreados modificados (dashboard-page.component.ts, .html, .spec.ts); diretórios não rastreados position-analysis/ (9 arquivos) e a change ativa (7 artefatos contando .openspec.yaml e estas evidências). Logs e dist confirmados ignorados por git check-ignore.
- Nenhum código mudou depois dos testes finais/build; apenas design/tasks/evidências. SHA-256 agregado de frontend/src: `98c6ed9960bbf6f4764855cd8dff82488a53e45c1ef37975043bee7c61c6a36d`. Algoritmo: arquivos em ordem de Path, concatenar caminho relativo POSIX UTF-8 + NUL + bytes do arquivo + NUL para SHA-256.
- Sem implementação dos dois gráficos posteriores, alterações em specs canônicas, Graphify, staging, commit, push, merge ou archive. Branch/HEAD preservados. Aguardar revisão humana desta entrega.


# Evidências do Bloco 1 — 09/09/2026

## Escopo e decisão vigente

A versão anterior de Custo × Valor atual não foi aprovada visualmente. A direção atual substituiu o refinamento isolado e autorizou estrutura do Dashboard, comparativo e apresentação do histórico em conjunto. As evidências acima preservam a rodada anterior; não constituem aprovação visual. Resultado não realizado por ativo, Rentabilidade por ativo e composição/alocação não foram implementados, nem receberam placeholders.

Change, proposal/design/tasks, ambos os deltas, decisões anteriores, PRD e specs canônicas de Dashboard, evolução, identidade visual, shell e posição foram consultados antes da implementação. Os artefatos foram reconciliados com a autorização explícita; specs canônicas não foram modificadas. Nenhum Graphify, staging, commit, push, merge ou archive foi executado.

## Resultado visual

Ordem final: contexto → ações → indicadores por moeda → Análise por ativo/Custo × Valor atual → Histórico do patrimônio → Posições abertas → Resultados realizados por ação. Contagem estrutural agora acompanha o título do resumo. Cards de indicadores têm bordas suaves e tipografia tabular; patrimônio ganha maior largura e valor destacado no desktop. Contexto ficou compacto; o seletor permanece somente no shell.

Comparativo: cada ativo mantém ticker, empresa e duas séries explícitas. Custo em contorno, valor atual sólido, guias verticais discretas, origem zero, valores alinhados à direita e proporções existentes preservadas. Sem trilho arredondado de progress bar. Descrição simples: “Compare quanto foi investido em cada ativo com o valor atual da posição.” Grupos intitulados BRL/USD. Igualdade produz comprimentos iguais; 1566/1569 mantém diferença pequena. Todos os ativos e textos completos permanecem, inclusive 100 itens e valores extremos.

Histórico: grid horizontal decorativo, linha de 2,5 px, contornos legíveis e espaçamento refinado. Não foi usada área preenchida nem curva suavizada. Os paths, coordenadas, pontos e gaps são os existentes. Gráficos lado a lado quando há espaço; empilhados abaixo da largura útil mínima. Uma única seção textual abaixo, agora com linhas divisórias em vez de bordas em cada registro. Preservados zero/um/múltiplos registros, vazios, IDs, timestamps, horários locais DD/MM/YYYY, HH:mm, tooltip, teclado/toque/Escape, botão manual e ausência de coleta automática.

Variação: o campo autoritativo resultadoNaoRealizado existe e sua semântica é explicitamente valorAtualPosicao − custoPosicao em portfolio-position/PRD 12.6. Não foi recalculado nem acrescentado um rótulo genérico “Variação” ao comparativo. As duas séries solicitadas bastam; resultado continua nos indicadores e posições existentes.

BRL e USD mantêm dados, indicadores e escalas separados, sem total combinado, FX, conversão ou qualquer cálculo financeiro novo. Nenhuma alteração em backend, migrations, DTOs, modelos, services, endpoints, payloads, parser, formatadores, geometria, contexto global, rotas, query params, dependências, persistência ou budgets.

## Validações finais

- Focados: **163/163**, 16 arquivos, com VITEST_MAX_WORKERS=2.
- Suíte frontend completa: **439/439**, 64 arquivos, com VITEST_MAX_WORKERS=2.
- Build production com stats-json: aprovado.
- OpenSpec strict da change: aprovado; global: **32/32**.
- git diff --check: aprovado. Avisos LF→CRLF não são falhas de whitespace.
- Não houve alteração em código após os testes/build finais; apenas evidências/tasks.

Testes do Dashboard já protegem ordem de headings, quatro indicadores por moeda, ausência de total combinado/seletor local, estados, contexto e quatro GETs financeiros. Comparativo cobre vazio, um, múltiplos, igualdade, zero, diferença pequena/grande em ambas as moedas, valores extensos/científicos, 100 ativos, textos e ausência de tab stops. Histórico cobre vazios, moeda ausente, observação única, instantes próximos, interação, tooltip, timestamps, snapshot manual, concorrência e chamadas. Novo teste verifica que grid não altera coordenadas/paths nem cria séries ou chamadas. Três casos de teste acrescentados nesta rodada (dois parametrizados de comparação e um do histórico).

O esbuild foi bloqueado inicialmente com spawn EPERM no sandbox; testes/build foram executados com aprovação fora do sandbox. Um teste novo inicialmente acessava membros protegidos; foi corrigido para usar os helpers públicos existentes, sem alterar a API do componente. Inspeção renderizada motivou alinhamento das labels e quebra de texto a 200%; as validações finais foram repetidas após essas correções.

## Bundle comparável

Baseline medido antes das alterações desta rodada, mesma instalação/configuração production. Initial soma main/styles e imports estáticos emitidos, sem contar CSS virtual embutido duas vezes. Stats anterior preservado localmente em frontend/dist/bloco1-before-stats.json; final em frontend/dist/frontend/stats.json.

| Métrica | Antes (bytes) | Depois (bytes) | Delta |
| --- | ---: | ---: | ---: |
| Initial | 514418 | 514418 | 0 |
| Dashboard lazy | 44258 | 44701 | +443 |
| CSS Dashboard | 2639 | 2522 | −117 |
| CSS comparativo | 1161 | 1747 | +586 |
| CSS evolução | 4284 | 4466 | +182 |
| Styles global | 16087 | 16087 | 0 |

O crescimento lazy decorre de markup/grid/estilos locais; reduções de textos e limpeza de estilos compensam parte do acréscimo. CSS já integra o chunk, não deve ser somado novamente. Nenhuma promoção ao initial ou dependência nova. Mantidos warnings de initial acima de 500 kB (14418 bytes) e evolução acima de 4 kB (agora 466 bytes; antes 284). Nenhum budget elevado. O warning de evolução cresceu 182 bytes e permanece abaixo do limite de erro de 8 kB.

## Inspeção renderizada e limites

Edge headless com perfil isolado, build production e servidor local de fixtures, sem dados reais ou escrita no backend. Dez cenários: 1440×900, 768×1024, 390×844, 320×740, 1280×600, fonte raiz 200% em 1440, combinação 320+200%, vazio, um e 100 ativos. Reduced motion ativo. Fonte ampliada aplicada antes de iniciar Angular para evitar medidas antigas do drawer.

- Nas larguras normais, conteúdo do Dashboard coube na área de trabalho: 1105, 721, 343, 273 e 945 CSS px, respectivamente. Nenhum overflow novo identificado nas seções alteradas.
- Desktop e baixa altura: histórico lado a lado; tablet de 768 e mobile: empilhado para manter largura legível. Valores, títulos e alternativas textuais preservados.
- Texto 200% desktop: conteúdo com 785 px, sem overflow das seções alteradas após correção das labels.
- Dataset integral: 100 ativos e sete registros preservados; carregamento fez somente GET Carteiras + quatro GETs financeiros, nenhum POST.
- Consulta do ponto por foco/Enter abriu tooltip no navegador; Escape fechou. Regressões automatizadas cobrem Espaço, clique/toque, hover/foco e histórico.
- Barras estáticas não criam foco/interação. SVG decorativo está oculto da árvore acessível; valores ficam em HTML. Tokens de contraste existentes preservados (texto principal 14,65:1, secundário 6,35:1, verde 7,09:1 sobre branco, conforme auditoria anterior). Leitor de tela real e aceite estético continuam pendentes.

**Pendência real de reflow:** na combinação de 320 CSS px e fonte raiz a 200%, o shell vigente expande a área de trabalho para aproximadamente 497,5 px e corta conteúdo. O shell não foi alterado, conforme escopo. A inspeção também encontrou scroll horizontal na navegação lateral existente do desktop; não houve redesign/correção de navegação nesta rodada. Esses achados impedem declarar acessibilidade/reflow integralmente aprovados. As tarefas 9.6/9.7 continuam abertas para revisão e decisão humana; não há dispensa automática.

Artefatos locais ignorados em frontend/.cache/bloco1/: visual.mjs (ensaio), visual-results.json, capturas desktop.png, desktop-analysis.png, desktop-history.png, tablet.png, mobile390.png, mobile320.png, mobile320-analysis.png, mobile320-history.png, low.png, text200.png, reflow.png, empty.png, one.png e many.png. Capturas mostram fixtures, não carteira real. Perfil/servidor do ensaio foram encerrados. Logs ignorados: frontend/test-bloco1-focused.log, test-bloco1-full.log e build-bloco1.log. Não foi fabricada captura anterior da implementação rejeitada; a pendência histórica 1.3 permanece.

## Arquivos e estado

Alterados nesta rodada:

- frontend/src/app/features/dashboard/dashboard-page.component.html e .scss;
- frontend/src/app/features/dashboard/position-analysis/cost-value-chart.component.html, position-chart.scss e position-analysis.component.spec.ts;
- frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.html, .scss e .spec.ts;
- proposal.md, design.md, tasks.md, os dois deltas spec.md e este arquivo de evidências na change ativa.

Arquivos de ensaio/capturas foram criados somente em .cache/ e dist/ ignorados. Não foram criados novos componentes de produção nesta rodada. Alterações anteriores em .gitignore, dashboard-page.component.ts e dashboard-page.component.spec.ts foram preservadas, sem nova edição nesta rodada. O diretório position-analysis e a change já estavam não rastreados no início.

Status final esperado e confirmado por git status --short:

```text
 M .gitignore
 M frontend/src/app/features/dashboard/dashboard-page.component.html
 M frontend/src/app/features/dashboard/dashboard-page.component.scss
 M frontend/src/app/features/dashboard/dashboard-page.component.spec.ts
 M frontend/src/app/features/dashboard/dashboard-page.component.ts
 M frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.html
 M frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.scss
 M frontend/src/app/features/dashboard/evolution/portfolio-evolution.component.spec.ts
?? frontend/src/app/features/dashboard/position-analysis/
?? openspec/changes/evoluir-dashboard-financeiro-frontend/
```

Bloco 1 implementado e validado tecnicamente, com pendência de reflow descrita e revisão visual humana aberta. Não avançar aos gráficos seguintes. Tasks 9.1–9.5 concluídas; 9.6 parcialmente verificada, sem aceite; 9.7 pendente. Total da change: 14/37 tarefas concluídas, sem declarar a change completa.


# Correção das pendências responsivas — 09/09/2026

## Causas comprovadas antes da correção

Diagnóstico executado no build anterior em Edge headless, com perfil isolado e fixtures da rodada anterior, sem modificar produção durante a investigação.

1. **320 CSS px + fonte raiz 200%:** o host do shell media 320 px, mas a coluna implícita `auto` do grid media **497,516 px**, determinada pelo tamanho mínimo do cabeçalho flex em linha única (marca, botão e seletor). Toolbar, sidenav-container e main herdavam essa expansão. O drawer `16rem` media **512 px**, superior à viewport. Não era causado por tabelas, gráficos, `100vw` ou mudança de escala financeira.
2. **Navegação desktop:** container interno de **255 px** tinha `scrollWidth=267`. O link Material usava largura `100%` (**255 px**) além das margens de **12 px** em cada lado. A borda direita chegava a 267 px. `width:auto` reduziu o link a **231 px**, respeitando ambas as margens, sem ocultar overflow.
3. Ao devolver o conteúdo à largura correta (241 px úteis), descrições com palavras longas ainda excediam a largura, e ícone + gap do cabeçalho deixavam apenas 121 px para o texto. Esses efeitos secundários também foram tratados estruturalmente.

## Solução mínima

- Coluna explícita `minmax(0,1fr)` no grid do shell.
- Cabeçalho compacto em grid; marca/menu na primeira linha e seletor global na segunda, com largura disponível completa. O seletor continua único, com mesmas opções, label, status, teclado e lógica.
- Drawer conserva largura nominal, mas tem `max-width:100%`.
- Links da sidebar com `width:auto`, preservando margens, foco, ícones e destinos. Labels admitem quebra sem ellipsis imposto por nowrap.
- `overflow-wrap:anywhere` herdável no conteúdo principal, sem tamanho de fonte menor ou remoção de texto.
- Cabeçalho compartilhado passa a uma coluna quando sua largura útil é menor ou igual a 15rem; ícone permanece visível acima do texto. Container query considera espaço efetivamente disponível, incluindo ampliação.
- Declarações equivalentes e sobrescritas do mesmo stylesheet foram consolidadas, evitando duplicação. Nenhum novo `overflow-x:hidden` foi acrescentado; o overflow preexistente não foi usado para esconder o problema.

**Arquivos de produção alterados nesta rodada:** `frontend/src/app/layout/main-layout/main-layout.component.scss` e `frontend/src/app/shared/page-header/page-header.component.scss`. Teste adicionado em `frontend/src/app/layout/main-layout/main-layout.component.spec.ts`, protegendo seletor único, nome completo da carteira, valor selecionado e labels de navegação durante abertura do drawer compacto.

Impacto nas outras telas: o shell compartilhado recebe o mesmo comportamento responsivo e textos longos podem quebrar; cabeçalhos muito estreitos empilham o ícone. Nenhuma tela foi redesenhada. Dashboard, gráficos, indicadores, ordem, tabelas/cards, geometria, dados, contratos, HTTP, backend, contexto e rotas não receberam alterações de implementação nesta rodada.

## Validações finais

- Testes focados de layout, cabeçalho e contexto: **37/37**, cinco arquivos.
- Suíte frontend completa: **440/440**, 64 arquivos.
- `VITEST_MAX_WORKERS=2`; build production com stats-json aprovado.
- **10/10 ensaios em Edge real:** 1440×900, 768×1024, 390×844, 320×740, 1280×600, 1440×900 com fonte 200%, 320×740 com fonte 200%, 959×900, 960×900 e 961×900.
- Asserts de navegador verificam bounds, clientWidth/scrollWidth do shell, toolbar, seletor, main, Dashboard e drawer aberto; foco no seletor, abertura e Escape. Fonte 200% aplicada antes da inicialização Angular; reduced motion ativo. Não se usou JSDOM para concluir geometria.
- **320+200% depois:** shell/main com largura 320 e sem expansão; main clientWidth/scrollWidth **305/305**; Dashboard **241/241**; seletor com 288 px; drawer aberto **319/319**. Antes: coluna/main de 497,516 px e drawer de 512 px.
- **Desktop depois:** navegação interna **255/255**, sem scroll horizontal; main **1169/1169**, Dashboard **1105/1105**. Antes: navegação **255/267**.
- Demais cenários também sem scroll horizontal do conteúdo ou navegação. Tabelas/cards e os três ativos/sete registros das fixtures permaneceram no DOM; regressões financeiras/dataset integral do Bloco 1 foram preservadas e reexecutadas na suíte completa.
- As capturas finais foram inspecionadas, incluindo fonte 200%, cabeçalho em uma coluna e foco visível no drawer aberto. Quebras de palavras são permitidas no extremo 320+200%, sem redução artificial de fonte.

O ensaio local em `frontend/.cache/responsive/verify.mjs` acrescenta verificações geométricas com falha explícita; os resultados estão em `visual-results.json`, e diagnóstico antes/depois em `diagnose.log`. Capturas `desktop.png`, `reflow.png`, `reflow-drawer.png`, `reflow-analysis.png`, `reflow-history.png` e demais viewports ficam no mesmo diretório ignorado. Logs: `focused.log`, `full.log`, `build.log`. Navegador e servidor isolados foram encerrados; nenhuma consulta a carteira real ou mutação de domínio ocorreu.

## Bundle antes/depois

Baseline reutiliza o build final válido do Bloco 1, preservado em `.cache/responsive/before-stats.json`. Mesmo ambiente/configuração; initial inclui somente arquivos emitidos ligados por imports estáticos e styles, sem dupla contagem do CSS embutido.

| Métrica | Antes | Depois | Delta |
| --- | ---: | ---: | ---: |
| Initial | 514418 B | 514417 B | −1 B |
| Dashboard lazy | 44701 B | 44697 B | −4 B |
| CSS Dashboard | 2522 B | 2522 B | 0 |
| CSS evolução | 4466 B | 4466 B | 0 |
| CSS shell | 3202 B | 3274 B | +72 B |
| CSS cabeçalho compartilhado | 1110 B | 1228 B | +118 B |

Os deltas líquidos de JS incluem minificação/identificadores compartilhados; não indicam alteração de lógica do Dashboard. CSS de componentes já integra os chunks e não deve ser somado novamente. Dependências e budgets intactos. Warnings preservados: initial 14417 B acima de 500 kB; evolução 466 B acima de 4 kB, sem warning novo.

OpenSpec strict change aprovado; global **32/32**. `git diff --check` aprovado, apenas avisos de normalização LF/CRLF. Status conserva alterações anteriores e acrescenta somente os dois SCSS e o teste mencionados. Documentação pertinente atualizada em design.md, tasks.md e neste arquivo; nenhuma spec canônica foi alterada. Nenhum Graphify, git add, commit, push, merge ou archive executado.

## Revisão humana final pendente

A pendência técnica de reflow foi corrigida; task 9.6 concluída com evidência real. **9.7 continua aberta**, sem aceite humano presumido. Estado atual da change: **15/37 tarefas concluídas**.

Roteiro: conferir desktop sem barra horizontal na sidebar; em 390/320 e 320+200%, verificar seletor na segunda linha, nome/opções e foco; abrir drawer, percorrer links e fechar por Escape; rolar indicadores, barras comparativas, histórico com gaps/tooltip e posições/cards; conferir densidade, legibilidade e leitor de tela. Verificar transição 959→960→961 e baixa altura. Não iniciar Bloco 2 antes dessa revisão.
