# Evidências do Bloco 1 — 09/09/2026 (horário local)

## Escopo e aprovação

Planejamento/direção aprovados explicitamente pelo usuário antes desta execução. Implementadas etapas 1–3, tasks 1.1–1.7. Task 1.8 permanece aberta: revisão humana do resultado visual não foi executada pelo agente. Total 27 tasks: 7 concluídas, 20 abertas. Blocos 2/3 e validação final não iniciados. Referências A/B consideradas conforme descrição aprovada; imagens originais não disponíveis nesta sessão.

Captura inicial real obtida do build anterior ainda existente, antes de sua substituição: frontend/.cache/expansion-before/desktop.png e desktop-performance.png, com fixtures do ensaio Edge existente. Não representa captura antiga inexistente nem dados de produção. Capturas locais ignoradas não serão versionadas.

## Implementação

- Custo: uma barra sólida de valorAtualPosicao, marcador vertical de custoPosicao na mesma escala por moeda, legenda curta e dois valores sempre visíveis. Marcador estende além da espessura da barra, inclusive igualdade. Padding do viewBox protege o traço nos extremos sem modificar proporções. Sem duas barras, mínimo artificial ou cálculo de variação.
- Resultado: barras divergentes existentes, zero central e labels Perda/Ganho, valores e estados explícitos.
- Rentabilidade: dot plot preservado, pontos maiores, eixo discreto e separadores de linhas; sem barra preenchida ou temporalidade.
- Apresentação: reutiliza formatadores existentes. Suplemento exato local reconhece zero antes da notação científica, apresenta frações com vírgula e expande expoentes limitados por manipulação textual/BigInt. Expoentes extremos usam representação matemática exata compacta, sem perder dígitos. Strings autoritativas e geometria existente não são modificadas. Não zero pequeno conserva sinal/estado e suplemento exato.

Criado position-chart-presentation.spec.ts; alterados cost-value-chart.component.html, position-chart.scss, position-chart-presentation.ts, position-analysis.component.spec.ts e quatro arquivos de position-performance-chart.component (TS/HTML/SCSS/spec). Nenhum serviço, model, parser, formatter global, backend, endpoint, dependência ou budget alterado. Histórico e restante do Dashboard intactos.

## Validação automatizada

- Focados: 138/138, 12 arquivos, comando npm --prefix frontend test -- --watch=false --include=src/app/features/dashboard/**/*.spec.ts; VITEST_MAX_WORKERS=2.
- Suíte completa: 471/471, 66 arquivos; VITEST_MAX_WORKERS=2.
- Production com stats-json: aprovado.
- Cobertura reaproveitada/ajustada: custo menor/maior/igual, diferença mínima, zero, extremos, moedas, 100 ativos/ordem integral, strings preservadas, sinais de resultado/rentabilidade, pontos e HTTP. Dez casos novos de apresentação incluem 0E-12, -0.00, 0e9999, frações extensas e expoentes extremos.
- Primeira tentativa sandbox falhou com spawn EPERM; execução autorizada fora do sandbox. Primeira execução focada detectou codificação incorreta durante edição, corrigida antes dos resultados finais acima. Nenhuma falha residual.
- Quatro GET financeiros antes/depois: resumo, posições, resultados-realizados e evolução-patrimonial; gráficos usam coleção existente. Sem fórmula financeira nova, FX, soma BRL/USD ou request novo.

## Navegador real

Reutilizado frontend/.cache/dot-review/verify.mjs, adaptando somente destino de diagnóstico/capturas e cenário tablet. Edge headless com fixtures locais: 1440×900, 768×1024, 1280×600, 390×844, 320×740, todos passaram nas verificações existentes de limites do shell/Dashboard e controles. Sem overflow horizontal global; selector/drawer preservados. Detector genérico aponta dot-scale porque marcador extremo ultrapassa o eixo em meio diâmetro; padding do dot-plot contém o ponto dentro do painel, sem corte/global overflow. Não confundir esse detalhe com falha de página.

Capturas posteriores e JSON: frontend/.cache/expansion-after/. Desktop-cost.png e desktop-performance.png inspecionados: marcador coincidente e diferença pequena preservados, legenda/valores visíveis, divergente e dot plot distintos. Nome real da captura: desktop-cost.png. Testes renderizados não são aprovação visual humana, nem validação financeira das fixtures. Mobile detalhado, texto ampliado e leitor de tela permanecem na validação final.

## Bundle production (bytes)

| Medida | Antes | Depois | Delta |
| --- | ---: | ---: | ---: |
| Initial | 514417 | 514416 | -1 |
| Dashboard lazy | 52387 | 54144 | +1757 |
| CSS Dashboard | 2522 | 2522 | 0 |
| CSS evolução | 4466 | 4466 | 0 |
| CSS custo | 1747 | 1905 | +158 |
| CSS resultado/dot | 2244 | 2500 | +256 |
| CSS contêiner analítico | 178 | 178 | 0 |

Fonte: stats.json, soma transitiva dos imports estáticos do initial, entryPoint do Dashboard e outputs CSS. Crescimento lazy de cerca de 3,35% corresponde ao helper de texto exato e templates/estilos locais; nenhuma promoção para initial. CSS é reportado separadamente sem somar novamente ao JS embutido. Warnings: initial +14416 B sobre 500 kB; evolução +466 B sobre 4 kB. Limites inalterados, initial dentro do critério canônico 514418 B.

## Revisão humana pendente

Conferir em desktop: barra = valor atual, traço = custo; igualdade/proximidade sem exagero; distinção entre três famílias; clareza de valores/suplementos e densidade. Nenhum donut, composição ou novo histórico implementado. Parar no Bloco 1.

Validação documental final: strict da change aprovado; strict global 33/33; git diff --check sem erros. Git: oito arquivos rastreados modificados no diretório position-analysis, um teste novo e diretório da change não rastreados; nenhum staging, cache ou screenshot listado.

## Revisão humana do Bloco 1 e autorização do Bloco 2

O usuário não aprovou integralmente o resultado visual do Bloco 1: diferenciação percebida insuficiente entre barra/referência, divergente e dot plot. Task 1.8 permanece aberta, candidata a refinamento posterior. As evidências técnicas não demonstram alcance do objetivo visual da change. Por instrução explícita, preservar implementação/testes do Bloco 1 e avançar somente às tasks existentes 2.1–2.7; 2.8 depende de nova revisão humana. Esta autorização substitui apenas a dependência de aceite prévio do Bloco 1, sem convertê-lo em aprovado.

## Revisão humana posterior — conjunto aceito

Usuário aprovou explicitamente Custo × Valor atual (leitura com dados variados/PETR4), Resultado divergente e Rentabilidade dot plot no conjunto. Task 1.8 concluída por este aceite posterior, preservando o registro da rejeição anterior. Tipo donut aprovado; task 2.8 continua aberta exclusivamente quanto ao Histórico. Não realizar novos refinamentos nos três comparativos nem enriquecer donut com percentuais/totais derivados.
