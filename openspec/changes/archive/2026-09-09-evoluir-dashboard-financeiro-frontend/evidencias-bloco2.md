# Evidências do Bloco 2 — 09/09/2026

## Entrega e autorização

O desktop do Bloco 1 foi aprovado visualmente pelo usuário. Esta rodada implementou somente Resultado não realizado e Rentabilidade por ativo, após Custo × Valor atual, dentro da Análise por ativo. Não houve reformulação do comparativo, indicadores, histórico, posições, resultados realizados ou correções responsivas. A revisão conjunta dos três gráficos ocorrerá depois desta entrega. Desktop prioritário; refinamento mobile detalhado explicitamente adiado, mantendo checagem de regressões graves.

Proposal/design/deltas/tasks foram reconciliados antes do código. A orientação atual substitui a escala percentual comum anteriormente planejada: cada moeda também tem seu domínio percentual independente. Nenhuma regra financeira foi criada.

## Apresentação

Dois painéis brancos com bordas discretas, lado a lado quando a largura útil comporta duas colunas de 24rem. Cada painel contém grupos BRL/USD internos, na ordem recebida. Os gráficos são barras horizontais divergentes: zero central, positivos à direita, negativos à esquerda. Cada moeda tem escala simétrica pela maior magnitude de seu próprio campo. Resultado usa resultadoNaoRealizado; rentabilidade usa rentabilidadePercentual. Nunca compartilham eixo entre métricas ou moedas.

Ticker, empresa, valor, sinal e estado Positivo/Negativo/Neutro permanecem em HTML visível. Zero e -0 não produzem retângulo. Valores abaixo da resolução mantêm sinal, string completa quando necessário e aviso textual, sem largura mínima falsa. SVG decorativo aria-hidden/focusable=false; nenhuma barra adiciona Tab, tooltip ou interação. O nome da métrica acompanha a estrutura textual de cada linha para tecnologia assistiva.

## Isolamento financeiro e HTTP

PositionPerformanceChartComponent recebe a mesma coleção readonly já carregada pelo Dashboard. O input metric seleciona somente o campo autoritativo; não há diferença valor−custo nem razão resultado/custo. As strings passam pelos formatadores lossless existentes. Sinal/zero vêm da análise textual do helper existente; proporções aproximadas alimentam somente x/width do SVG. Nenhum Number/parseFloat/parseInt financeiro foi adicionado. Modelos, parser, formatadores, services e helper geométrico não foram alterados.

HTTP antes/depois: exatamente GET resumo, posições, resultados-realizados e evolução-patrimonial por carga/reload, além da listagem compartilhada de Carteiras. Nenhum GET/POST próprio dos novos gráficos. Teste HttpTestingController existente foi ampliado para exigir os dois painéis renderizados antes de verificar ausência de requests remanescentes. Não há FX, soma entre moedas, persistência ou contrato novo.

## Arquivos

Criados em frontend/src/app/features/dashboard/position-analysis/:

- position-performance-chart.component.ts;
- position-performance-chart.component.html;
- position-performance-chart.component.scss;
- position-performance-chart.component.spec.ts.

Alterados nesta rodada:

- position-analysis.component.ts: integração dos dois painéis;
- position-analysis.component.spec.ts: mantém testes do comparativo com escopo em seu componente, sem misturar seus itens com os novos;
- dashboard-page.component.spec.ts: presença dos dois gráficos no teste HTTP;
- proposal.md, design.md, tasks.md e os dois deltas da change;
- este relatório.

Custo × Valor atual (TS/HTML/SCSS), Dashboard de produção, evolução, shell, cabeçalho compartilhado, backend, rotas, contexto, contratos, dependências e budgets ficaram intactos nesta rodada. Alterações anteriores continuam no working tree.

## Testes e navegador

- Focados Dashboard/gráficos: **128/128**, 11 arquivos.
- Suíte completa: **461/461**, 65 arquivos, VITEST_MAX_WORKERS=2.
- Foram acrescentados 21 casos: positivo/negativo/zero/-0, simetria, BRL/USD individuais e simultâneos, domínios independentes, campos deliberadamente incompatíveis com custo/valor, extremos 1e400/1e-400, decimais extensos, estado abaixo de centavos, dataset de 100 posições, alternativa textual sem tab stops, vazio e ordem de integração. A string de ticker no teste novo fica com oito caracteres, sem estender o contrato.
- Build production --stats-json aprovado.
- Edge headless isolado: **4/4** cenários, 1440×900, 1280×600, 390×844 e 320×740. Fonte/tokens existentes, reduced motion ativo. Dados de teste, sem carteira real.
- Em desktop os dois painéis ficam lado a lado; em 390/320 empilham, sem overflow detectado no main/Dashboard/painéis. Três ativos em cada novo gráfico e sete registros históricos preservados. Foco no seletor, drawer e Escape mantidos.
- Captura desktop-performance.png inspecionada: positivo, negativo e zero identificáveis; percentuais e dinheiro legíveis; zero central e ausência de barra em neutro; nenhuma revisão de Custo × Valor atual realizada.

Revisão mobile detalhada, fonte ampliada de todos os novos cenários e leitor de tela real continuam deliberadamente pendentes. Não foram repetidos os ensaios extensivos do shell cujo código não mudou. Essa economia não equivale a aceite visual humano dos gráficos novos.

Ensaios/capturas locais ignorados: frontend/.cache/bloco2/verify.mjs, visual-results.json, desktop-performance.png, mobile320-performance.png e demais viewports. Logs: frontend/test-bloco2-focused.log, test-bloco2-full.log, build-bloco2.log. Servidor e navegador isolados encerrados.

## Bundle comparável

Baseline final válido da correção responsiva reutilizado sem novo build anterior; stats preservado em frontend/.cache/bloco2-before-stats.json. Mesma configuração/instalação. CSS embutido não é contado novamente no initial.

| Métrica | Antes | Depois | Delta |
| --- | ---: | ---: | ---: |
| Initial | 514417 B | 514416 B | −1 B |
| Dashboard lazy | 44697 B | 50756 B | +6059 B |
| CSS Dashboard | 2522 B | 2522 B | 0 |
| CSS evolução | 4466 B | 4466 B | 0 |
| CSS comparativo aprovado | 1747 B | 1747 B | 0 |
| CSS contêiner da análise | 33 B | 178 B | +145 B |
| CSS novo painel (compartilhado pelas duas instâncias) | 0 | 1537 B | +1537 B |

O crescimento lazy corresponde ao componente de apresentação, template, estilos e integração; um único componente/stylesheet atende as duas métricas. Sem dependência nova ou promoção dos gráficos ao initial. Variação de 1 byte no initial decorre de saída minificada/imports, não de otimização funcional. Warnings existentes: initial 14416 B acima de 500 kB e evolução 466 B acima de 4 kB. Nenhum warning novo; budgets inalterados.

## OpenSpec, Git e revisão humana

OpenSpec strict change aprovado; global 32/32. git diff --check aprovado (somente avisos LF→CRLF). git status --short mantém as alterações anteriores e os diretórios não rastreados de análise/change; as novas implementações estão dentro desses diretórios. Nenhum staging, commit, push, merge, archive ou Graphify.

Revisão humana: conferir composição dos três gráficos no desktop, densidade dos dois painéis, leitura do zero/ganho/perda e agrupamentos BRL/USD com carteira real. Custo × Valor atual permanece como entregue; qualquer reconsideração aguarda essa revisão. Não avançar além do Bloco 2. A aprovação do desktop anterior não é registrada como aprovação dos novos gráficos.


## Revisão após diagnóstico aprovado — dot plot (09/09/2026)

Opção C aprovada: não implementar rentabilidade histórica. O histórico de patrimônio não mede retorno diante de compras, vendas e alterações de composição. Preservados endpoints, snapshots, fórmulas e significado financeiro. Os registros anteriores desta evidência descrevem a entrega inicial de barras; esta seção registra sua substituição somente na rentabilidade.

Alterados nesta revisão: position-performance-chart.component.html/.scss/.spec.ts, proposal.md, design.md, specs/frontend-position-analysis/spec.md, tasks.md e este relatório. TypeScript de apresentação, helper geométrico, formatadores, serviços e modelos não mudaram. Resultado continua com o mesmo SVG e estilos; Custo × Valor atual e evolução não foram editados nesta revisão.

Rentabilidade usa pontos de tamanho constante sobre linha percentual, zero central e coordenada 50 + sinal × razão × 50. Essa aritmética opera somente projeções geométricas existentes; textos continuam exclusivamente do campo rentabilidadePercentual. Cada moeda tem seu próprio máximo de magnitude, sem truncar dados. Margem interna de 6px mantém pontos extremos inteiros. Positivos preenchidos, negativos com contorno, neutros quadrados no centro; texto de estado e sinal sempre presente. Valores abaixo da resolução mantêm valor completo e aviso específico do ponto, sem deslocamento mínimo inventado. Nenhum foco, tooltip ou animação novo.

Validações: focados Dashboard/gráficos/evolução 128/128 (11 arquivos); suíte completa final 461/461 (65 arquivos), VITEST_MAX_WORKERS=2. A suíte completa foi repetida após corrigir o aviso textual do ponto e inclui os testes focados. Testes cobrem sinais, zero/-0, coordenadas proporcionais, moedas independentes, extremos, 100 ativos, lossless, acessibilidade textual e HTTP existente. Build production final aprovado. Tentativa inicial no sandbox falhou por spawn EPERM; execuções autorizadas fora do sandbox passaram.

Edge headless com fixtures locais: 1440×900, 1280×600, 390×844 e 320×740 passaram, sem overflow nos painéis e com seletor/drawer preservados. Três ativos por gráfico, sete registros históricos, quatro GETs financeiros existentes (resumo, posições, resultados-realizados, evolução) sem request de gráfico. Captura desktop inspecionada; pontos com 10×10px, positivo em 100%, negativo em 49,5768%, neutro em 50% para a fixture. Evidência geométrica real, não inferida do JSDOM. Scripts e capturas ignorados em frontend/.cache/dot-review; logs test-dot-focused.log, test-dot-full.log e build-dot.log.

| Bundle | Antes desta revisão | Depois |
| --- | ---: | ---: |
| Initial | 514416 B | 514417 B |
| Dashboard lazy | 50756 B | 52387 B |
| CSS Dashboard | 2522 B | 2522 B |
| CSS evolução | 4466 B | 4466 B |
| CSS painel compartilhado | 1537 B | 2244 B |

CSS já embutido no lazy, não somado novamente. Crescimento lazy de 1631 B pelo ramo de template/CSS dos pontos. Variação de 1 B no initial por referência/hash de chunk; permanece abaixo do checkpoint 514418 B. Warnings existentes: initial acima de 500 kB e evolução 4466 B acima de 4 kB; nenhum warning novo e budgets intactos.

OpenSpec strict change/global aprovados; git diff --check sem erro. Working tree mantém alterações anteriores do Bloco 1/shell e diretórios não rastreados da análise/change. Nenhum Graphify, staging, commit, push, merge ou archive.

Revisão humana pendente: composição dos três gráficos no desktop, leitura dos pontos próximos do zero e extremos, identificação BRL/USD, nomes/percentuais extensos e contraste. Mobile detalhado e leitor de tela real continuam adiados; verificações breves não equivalem a aceite humano. Não avançar Bloco 3.

## Aprovação visual humana do desktop — registro posterior

O usuário aprovou explicitamente o Bloco 2 no desktop: composição geral da Análise por ativo, separação BRL/USD, Custo × Valor atual, Resultado não realizado, dot plot de Rentabilidade por ativo, diferenciação entre as três análises, leitura do zero central, estados Positivo/Negativo/Neutro, percentuais e composição dos painéis. Este aceite substitui a pendência desktop registrada acima; não é inferido dos testes automatizados.

Refinamentos candidatos não bloqueantes, sem alteração nesta rodada:

1. Custo × Valor atual pode ser pouco intuitivo quando os valores são muito próximos.
2. Notação técnica como `0E-12 USD` deve ser considerada em revisão futura de apresentação, sem alterar precisão ou valor autoritativo.

Mobile detalhado, texto ampliado e leitor de tela permanecem adiados para a validação final da change. Tasks 10.4 e 11.4 concluídas somente no âmbito desktop aprovado. Nenhum código foi alterado para registrar o aceite; testes e build anteriores não foram repetidos.
