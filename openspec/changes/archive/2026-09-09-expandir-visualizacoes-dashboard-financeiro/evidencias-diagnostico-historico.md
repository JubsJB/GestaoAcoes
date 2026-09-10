# Diagnóstico real e apresentação do Histórico — 09/09/2026 local

## Revisão humana

Conjunto visual do Bloco 1 aprovado explicitamente pelo usuário nesta rodada; task 1.8 encerrada pelo aceite posterior, sem apagar a rejeição anterior. Donut aprovado como tipo. Task 2.8 permanece pendente exclusivamente quanto ao Histórico do patrimônio. Nenhum enriquecimento de centro/legenda do donut, percentual ou refinamento dos comparativos. Total atual: 27 tasks, 15 concluídas e 12 abertas.

## Fonte real e valores

Consultas somente leitura ao backend local http://localhost:8080. Carteira correspondente aos valores relatados: Americana, id 5. Fonte do histórico: GET /carteiras/5/evolucao-patrimonial. Números abaixo são tokens autoritativos preservados, não valores recalculados.

| Snapshot | Timestamp original UTC | BRL | USD | X SVG | Y SVG (ambas) |
| --- | --- | --- | --- | ---: | ---: |
| 1 | 2026-09-05T17:09:43.932463Z | 1569.000000000000 | 1599.850000000000 | 44 | 120 |
| 5 | 2026-09-05T17:25:20.545814Z | 1569.000000000000 | 1599.850000000000 | 47.28403186509436 | 120 |
| 8 | 2026-09-07T21:17:26.957196Z | 1569.000000000000 | 1599.850000000000 | 702 | 120 |

São três snapshots contendo ambas as moedas, isto é, três observações BRL e três USD. Não há gap/registro vazio nesta carteira. Os valores são exatamente iguais dentro de cada moeda, não apenas próximos. Portanto linha horizontal é correta. A rotina buildEvolutionGeometry existente foi executada sobre tokens preservados para conferência; valor constante resulta no centro vertical y=120. A temporalidade real coloca #5 próximo a #1. Em Edge desktop 1440×900, centros X BRL 339.481/341.754/795.019 px e USD 903.981/906.254/1359.519 px: separação inicial ~2,27 CSS px, menor que diâmetro dos círculos. Os três elementos g.point existem em cada moeda. O label temporal intermediário é ocultado pelo mecanismo de prevenção de colisão já existente, não o ponto/dataset.

Gaps foram protegidos pelos testes existentes de moeda ausente, registro vazio e área segmentada; não fabricar um gap para esta carteira nem confundir intervalo longo sem registro com ausência explícita de moeda em snapshot.

## Histórico USD versus patrimônio atual

- Histórico: snapshot #8 retorna USD patrimonioAtual=1599.850000000000, apresentado como US$ 1.599,85 e data local 07/09/2026, 18:17.
- GET /carteiras/5/resumo retorna USD patrimonioAtual=959.910000000000, apresentado como US$ 959,91; BRL atual=2040.100000000000.
- GET /carteiras/5/posicoes retorna AAPL, acaoId=3, quantidadeAtual=3.000000 e valorAtualPosicao=959.910000000000. O valor apresentado vem diretamente desse campo, sem multiplicação frontend.
- SnapshotCarteiraService persiste patrimonioAtual do agregador no instante manual; ResumoCarteiraService consulta posições atuais e agregador oficial. O componente histórico consome evolução, os indicadores consomem resumo. Fontes corretas, sem troca ou erro de totalização demonstrado. Não foi reconstruída a causa operacional da diferença, nem inferido lucro/perda a partir dela; histórico e estado atual podem divergir legitimamente.

## Correção mínima dentro de 2.4/2.5

Não há defeito financeiro ou perda de registro. Há limitação de percepção/seleção por sobreposição. Adicionados botões locais identificados por snapshot/data junto ao gráfico, reutilizando showPoint/activatePoint/blurPoint/Escape existentes. Cada observação pode ser selecionada individualmente sem alterar coordenadas. Estado aria-pressed e contorno do alvo ativo destacam seleção; rótulo acessível inclui ID, moeda, valor e data completa. Datas no eixo continuam reais e mecanismo anticollision permanece. Nenhuma interpolação, jitter, espaçamento temporal artificial ou valor novo.

Alterados somente portfolio-evolution.component.html, .scss e .spec.ts nesta rodada, além da documentação. TS de evolução, geometry, services, parser, contratos e snapshots não alterados. Três gráficos do Bloco 1 e donut intactos. Quatro GET financeiros por carga de Dashboard continuam iguais; consultas diagnósticas separadas não são requests novos da aplicação.

## Validações

- Focados: 152/152 em 14 arquivos.
- Suíte: 485/485 em 68 arquivos, VITEST_MAX_WORKERS=2.
- Build production com stats-json: aprovado.
- Novo teste reproduz timestamps/valores deste caso, seis marcadores, X/Y exatos, seleção de #5 em ambas as moedas, tooltip/foco/Escape e ausência de consulta/POST extra.
- Edge com respostas reais do backend (proxy somente GET na infraestrutura existente): 1440×900, 768×1024, 1280×600, 390×844, 320×740 sem overflow global; coordenadas antes/depois idênticas. Seleção de #5 confirmada visualmente e pelo tooltip. Dados brutos, JSON/capturas antes/depois apenas em frontend/.cache/history-real ignorado. Nenhuma captura é aprovação humana.

| Medida (bytes) | Antes | Depois |
| --- | ---: | ---: |
| Initial | 514417 | 514416 |
| Dashboard lazy | 63134 | 65017 |
| CSS evolução | 4875 | 5685 |

Demais CSS de Dashboard/comparativos/composição inalterados. Crescimento lazy +1883 B e CSS +810 B provêm do seletor de registros e estado de foco. Warnings: initial +14416 B sobre 500 kB, evolução +1685 B sobre 4 kB; limites de erro e budgets intactos, sem dependências novas. Initial segue dentro de 514418 B. Testes/build não repetidos por documentação.

## Pendente

Somente aceite visual do Histórico dentro da task 2.8. Rever seleção dos três registros e foco sem exigir variação quando dados são constantes. Bloco 3 não iniciado; nenhuma autorização de archive/versionamento usada.

Strict da change aprovado; strict global 33/33; git diff --check aprovado. Status final preserva alterações anteriores e desta rodada, sem staging e sem caches/capturas listados.

Aprovação humana posterior: Histórico aprovado explicitamente pelo usuário, sem defeito financeiro; task 2.8 encerrada. Bloco 3 autorizado para composição desktop apenas.
