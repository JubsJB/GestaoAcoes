Estado final vigente: **70 concluídas por evidência, 2 dispensadas (1.3 e 7.5), 0 abertas sem justificativa**. Os dois checkboxes desmarcados indicam dispensa, não execução pendente. Ver registro final ao fim deste arquivo.

Esta lista acompanha a implementação. Graphify está explicitamente fora do escopo e não constitui task pendente. Marcar conclusão somente com evidência; registrar bloqueio ou aceite combinado formal sem afirmar reprodução manual inexistente.

Evidências históricas das Fases 1–3: [evidencias-fases-1-3.md](evidencias-fases-1-3.md). Fases 4–5 implementadas, com revisão visual humana e verificação complementar da task 5.7 aprovadas conforme os registros abaixo. Fase 6 concluída após aprovação humana integral registrada em 08/09/2026. Fase 7 não concluída.

## 1. Baseline e critérios de aceite

- [x] 1.1 Registrar branch, estado inicial e limites de arquivos; confirmar que backend, contratos, dependências e Graphify estão fora da execução.
- [x] 1.2 Registrar matriz dos requirements e cenários desta change contra os contratos e testes vigentes, sem duplicar regras financeiras.
- [ ] 1.3 Capturar baseline renderizado de Dashboard, Corretoras, Ações, Carteiras, Operações, detalhes, formulários e dialogs nos viewports do design.
- [x] 1.4 Registrar estados disponíveis de loading, empty, error, warning e success e limitações de cenários reais, sem fabricar evidência manual.
- [x] 1.5 Medir build e bundle inicial no ambiente da implementação, registrando configuração, lazy chunks e budgets de initial/estilos sem alterá-los.
- [x] 1.6 Registrar baseline de testes frontend e dos fluxos críticos: payloads, requests, precisão decimal, contexto, prévias e snapshots manuais.
- [x] 1.7 Definir matriz de contraste, teclado, leitor de tela, zoom, reduced-motion e responsividade para revisão incremental e final.

## 2. Design system, tokens e padrões

- [x] 2.1 Mapear tokens existentes e literais duplicados por papel, preservando nomes úteis e distinguindo ajustes locais de padrões compartilhados.
- [x] 2.2 Refinar paleta verde e superfícies claras com contraste medido; registrar valores escolhidos e comparação visual.
- [x] 2.3 Consolidar tokens de texto, bordas, marca, hover e foco sem introduzir dark mode, seletor ou persistência.
- [x] 2.4 Separar semântica de positivo/negativo financeiro, success, warning, error, info e destructive.
- [x] 2.5 Consolidar escala compartilhada de spacing, radius e elevation sem extrair abstrações locais desnecessárias.
- [x] 2.6 Refinar hierarquia tipográfica de títulos, contexto, indicadores, metadados e ajuda, preservando fontes locais.
- [x] 2.7 Aplicar tipografia numérica tabular e alinhamentos compartilhados sem modificar formatadores.
- [x] 2.8 Refinar superfícies .app-surface, .section-card e .app-form-surface e padrões de badges e estados.
- [x] 2.9 Revisar regressão visual transversal da fundação e budgets de CSS antes de avançar às páginas.

## 3. Shell, componentes compartilhados, dialogs e feedback

- [x] 3.1 Refinar toolbar, sidebar e região principal preservando 960px, side/over, rotas, skip link e rolagem existentes.
- [x] 3.2 Refinar navegação ativa, hover, foco e ícones sem remover aria-current nem indicação não cromática.
- [x] 3.3 Refinar PageHeader com e sem ícone, títulos/contexto e ações responsivas, incluindo página de rota desconhecida.
- [x] 3.4 Refinar AppIcon e sprite local em tamanhos/alinhamento e nomes decorativos/acionáveis, sem biblioteca nova.
- [x] 3.5 Padronizar .app-actions, botões primários, secundários e destrutivos com estados hover/focus/disabled.
- [x] 3.6 Refinar StickyBack preservando retorno, foco e visibilidade em mobile/baixa altura.
- [x] 3.7 Padronizar dimensões, superfícies, títulos e ações dos dialogs de cadastro e edição reutilizando componentes atuais.
- [x] 3.8 Padronizar confirmação de situação cadastral e exclusão de Carteira sem alterar gatilhos, cancelamento ou HTTP.
- [x] 3.9 Refinar FeedbackAlert, .app-state e spinner mantendo textos, alert/status, retry e distinção entre empty e erro.
- [x] 3.10 Refinar SuccessToast preservando posição, duração, fechamento, descarte e anúncios atuais.
- [x] 3.11 Verificar foco contido/restaurado, Escape/backdrop e ausência de double scroll nos dialogs e shell.

## 4. Listagens e detalhes

- [x] 4.1 Aplicar padrão de tabela semântica desktop e cards completos mobile à listagem de Operações, preservando ordem e campos.
- [x] 4.2 Aplicar o mesmo padrão ao histórico contextual da Carteira, sem consultas ou efeitos duplicados.
- [x] 4.3 Aplicar tabela desktop e cards completos mobile à listagem de Ações, preservando busca exata e cadastro existentes.
- [x] 4.4 Aplicar tabela semântica desktop/cards completos mobile a Corretoras mantendo instituição, CNPJ, localidade, situação textual, busca por CNPJ e ações, sem novas requisições.
- [x] 4.5 Aplicar tabela semântica desktop/cards completos mobile a Carteiras com nome, data no formatador atual, contexto Ativa derivado apenas do CarteiraContextService e Ver detalhes; preservar Nova carteira, sem novas requisições, métricas ou mudanças de seleção/persistência.
- [x] 4.6 Refinar detalhe de Corretora em identificação, contatos, endereço e situação, preservando dados opcionais e avisos.
- [x] 4.7 Refinar detalhe de Ação destacando identificação, última cotação registrada e data, mantendo atualização manual.
- [x] 4.8 Refinar detalhe de Carteira separando contexto, ações, posições abertas e histórico, mantendo edição/exclusão, rota reativa e estados independentes existentes.
- [x] 4.9 Refinar detalhe de Operação com grupos legíveis, mantendo campos aprovados e sem enriquecimento de IDs.
- [x] 4.10 Verificar equivalência de campos/ações e uma única representação acessível/focável ativa em cada breakpoint, sem novos filtros, sort, paginação ou HTTP.

## 5. Formulários

- [x] 5.1 Refinar formulário de Corretora somente CNPJ em página/dialog, mantendo prefill, confirmação e validações.
- [x] 5.2 Refinar formulário de Ação somente ticker e mercado em página/dialog, sem alterar máscaras ou requisições.
- [x] 5.3 Refinar criação e edição de Carteira somente nome em página/dialog, mantendo submissão e cancelamento.
- [x] 5.4 Agrupar visualmente contexto, tipo/movimentação, quantidade/preço/data, Corretora, estimativa e ações do formulário de Operação, preservando entrada principal contextual e rotas globais por compatibilidade.
- [x] 5.5 Verificar COMPRA com preço somente leitura, prévia existente e POST sem preço; preservar invalidação por mudança de Ação/data, bloqueios e Carteira capturada sem reatribuição por troca global.
- [x] 5.6 Verificar VENDA com sugestão editável, precisão, estimativa existente, strings decimais e data civil inalteradas.
- [x] 5.7 Verificar Carteira fixa desde abertura em página/dialog, inclusive entrada global de compatibilidade, retorno determinístico após reload/deep link, Corretora opcional, labels/ajudas/erros, foco, submit/cancel e double-submit em todos os formulários.

## 6. Dashboard e evolução patrimonial

- [x] 6.1 Reordenar visual e semanticamente Dashboard em contexto, ações, indicadores, evolução, posições e resultados.
- [x] 6.2 Refinar a apresentação do contexto e ações do Dashboard preservando o seletor global exclusivamente no shell, sem seletor ou listagem de Carteiras duplicados no Dashboard; manter query params, navegação contextual e conjunto funcional de requisições.
- [x] 6.3 Refinar os quatro indicadores autoritativos por moeda com patrimônio em destaque, sem total combinado nem métrica nova.
- [x] 6.4 Aplicar tabela semântica desktop e cards completos mobile às posições preservando todos os campos e moedas.
- [x] 6.5 Refinar resultados realizados comparáveis por Ação, sem totalização.
- [x] 6.6 Refinar dimensões, labels, contraste e histórico da evolução preservando SVG local, dados completos e datas/valores.
- [x] 6.7 Refinar tooltip para legibilidade e limites de viewport com conteúdo dispensável, persistente e alcançável quando aplicável.
- [x] 6.8 Implementar equivalência acessível dos pontos entre clique/toque, foco, Enter/Espaço e Escape sem efeito financeiro.
- [x] 6.9 Refinar alvos/spacing, foco e ordem de Tab sem deslocar pontos, alterar projeção financeira ou conectar gaps.
- [x] 6.10 Verificar loading/erro independentes da evolução, Atualizar dados sem POST e registro somente por ação manual.
- [x] 6.11 Verificar deterministicamente BRL, USD, moedas simultâneas, zero/um/múltiplos snapshots, gaps e snapshots vazios após o refinamento.

## 7. Validação final

- [x] 7.1 Revisar linguagem de todas as páginas, removendo jargão editorial dispensável sem perder diagnóstico ou avisos financeiros.
- [x] 7.2 Executar testes focados dos componentes e fluxos afetados, acrescentando somente cobertura relevante para semântica/interação e regressões.
- [x] 7.3 Executar suíte frontend completa e registrar resultado, sem declarar cobertura manual a partir dela.
- [x] 7.4 Executar build na mesma configuração do baseline, comparar initial/lazy e budgets de estilos, sem aumentar budgets ou dependências.
- [ ] 7.5 Comprovar initial não superior ao baseline medido e ausência de redução de dataset/funcionalidade para atingir o critério.
- [x] 7.6 Validar manualmente contraste textual/não textual de superfícies, estados, marca, resultados e ações destrutivas.
- [x] 7.7 Validar teclado, foco visível/não oculto, skip link, headings, labels, tabelas, leitor de tela e anúncios alert/status.
- [x] 7.8 Validar dialogs com foco contido/restaurado, cancelamento e ações pendentes em desktop/mobile/baixa altura.
- [x] 7.9 Validar gráfico com mouse, toque e teclado: role, Enter, Espaço, Escape, hover/focus, tooltip, alvos próximos, Tab e histórico textual completo.
- [x] 7.10 Validar desktop, tablet e mobile, fronteiras 959/960/961px, textos longos e valores extensos em todas as features.
- [x] 7.11 Validar ampliação textual 200%, reflow a 320 CSS px, zoom e baixa altura sem informação removida, foco oculto ou double scroll.
- [x] 7.12 Validar reduced-motion e ausência de dependência exclusiva de cor, ícone ou animação para feedback.
- [x] 7.13 Comparar fluxos HTTP/rotas/query params/payloads antes e depois, incluindo compra/venda, busca, dialogs, contexto concorrente, reload e snapshot manual.
- [x] 7.14 Confirmar parsing lossless, precisão, formatadores, BRL/USD e ausência de FX/cálculos novos, enriquecimento de IDs ou requests adicionais.
- [x] 7.15 Registrar evidências manuais por cenário e bloqueios externos; eventual aceite combinado exige justificativa explícita sem fabricar execução.
- [x] 7.16 Revisar diff de escopo, dependências e lazy loading; confirmar nenhum backend, contrato, banco, migration ou budget alterado.
- [x] 7.17 Executar OpenSpec strict da change e global, git diff --check e git status --short; relatar warnings e pendências sem Graphify, commit ou archive.
## Registro da revisão visual humana — 07/09/2026

O usuário aprovou os layouts desktop/mobile de Corretoras, Ações, Carteiras, Operações e Posições (tabela e reflow em cards); quantidades enxutas, precisão fracionária prevista pelo formatador, dinheiro com moeda e duas casas; alinhamento da cotação de Ações; espaçamento do helper Ticker; e reflow do Resumo por moeda. Não foram relatados bloqueadores visuais nesses layouts.

- **4.10 concluída por evidência combinada:** aprovação humana dos campos/layouts desktop/mobile e auditoria técnica anterior de uma única estrutura DOM por coleção, controles sem duplicação, ordem e navegação preservadas. A troca de apresentação não acrescenta filtros, ordenação, paginação ou HTTP. A consulta compartilhada de Corretoras, autorizada separadamente pelo usuário para resolver nomes, não é causada por reflow. Este registro não afirma teste manual com leitor de tela ou teclado não relatado.
- **5.7 permanecia aberta nesta primeira revisão:** a aprovação cobria apresentação, labels/ajudas e espaçamento informado, sem confirmar integralmente os demais comportamentos. A pendência foi posteriormente resolvida pela aprovação complementar registrada abaixo; o significado da task foi preservado.
- Evidência técnica anterior preservada: 211/211 testes focados; 387/387 na suíte completa (61 arquivos, VITEST_MAX_WORKERS=2); build production com initial 514,60 kB. Esta atualização é exclusivamente documental.
- A aprovação do reflow do Resumo por moeda não conclui a Fase 6. Posições e ajustes de resultados já compartilhados com o Dashboard devem ser reaproveitados na próxima fase, sem reimplementação automática.

## Aprovação humana complementar — 07/09/2026

O usuário declarou execução integral e aprovação dos cenários manuais solicitados para 2.2, 2.9, 3.11 e 5.7. As quatro tasks foram concluídas por evidência combinada, sem modificar seus critérios. A execução humana é atribuída ao usuário; não é inferida de JSDOM.

- **2.2:** medições dos tokens registradas nas evidências das Fases 1–3 e reconferidas na rodada anterior, combinadas com comparação visual aprovada de textos, controles, bordas, botões, badges, links, navegação, foco e estados semânticos/destrutivos. A borda sutil do seletor global mede 1,34:1 sobre branco; o usuário confirmou que o controle é identificável em estado normal e seu foco perceptível. Esse registro não afirma que a borda isolada passou no limiar de 3:1.
- **2.9:** revisão transversal humana aprovada em desktop/mobile, largura reduzida, zoom e demais cenários solicitados, sem cortes, sobreposições, perda de conteúdo/ações ou rolagem horizontal obrigatória indevida. Evidência técnica existente: suíte 387/387 (61 arquivos) e build sem warnings de CSS por componente, com budgets preservados. A revisão foi concluída nesta retomada, depois das alterações de páginas; não se afirma execução retroativa. O initial de 514,60 kB não comprova o critério distinto de 7.5 relativo ao baseline histórico de 506,09 kB.
- **3.11:** aprovação humana de foco, Tab/Shift+Tab, Escape, backdrop, cancelamento, retorno de foco e rolagem de dialogs/shell, inclusive altura/largura reduzidas, complementando as configurações e testes existentes.
- **5.7:** aprovação integral dos cenários solicitados nos formulários abrangidos: validações, labels/ajudas/erros, foco, submit/cancel, criação/edição, prevenção de double-submit, Corretora opcional, COMPRA/VENDA e Carteira fixa em página/dialog e entrada global. Troca global não reatribuiu a Operação; reload real, deep link e retornos foram aprovados, complementando a cobertura automatizada de contexto, payloads e navegação.

## Limitação histórica da task 1.3 — aceite explícito do usuário

O baseline renderizado anterior às alterações não foi capturado. Essa evidência histórica não pode ser reconstruída retrospectivamente de forma confiável. Capturas e revisões posteriores documentam seus respectivos estados e não devem ser apresentadas como baseline anterior.

O usuário revisou e aceitou explicitamente a ausência dessa evidência como limitação histórica para continuidade da change. A task 1.3 permanece aberta, com seu critério original preservado: este aceite não afirma execução da captura nem substitui a evidência histórica por capturas atuais.

## Implementação da Fase 6 — 07/09/2026

- 6.1–6.3: DOM em contexto, ações, indicadores, evolução, posições e resultados; contexto consumido exclusivamente do serviço global, sem seletor/listagem local. Quatro indicadores por moeda preservados; patrimônio com classe e hierarquia próprias. Testes verificam ordem de headings, quatro cards por moeda e ausência de seletor local. Acabamento renderizado será submetido à revisão humana, sem alegar validação de navegador por JSDOM.
- 6.4–6.5: PortfolioPositionsComponent e resultados realizados existentes reutilizados sem redesign. Integração após reordenação verificada pelos testes do Dashboard e componente compartilhado: campos, moedas, sinais, identificação e ausência de totalização preservados.
- 6.6–6.9 permanecem abertas para revisão renderizada/assistiva. Implementação entregue: gráficos independentes empilhados, SVG/projeção intactos, labels HTML, tooltip HTML no fluxo do card, hover/foco sustentados separadamente, Enter/Espaço/clique e Escape, SVG com role group e pontos com role button. Histórico completo oferece botões nativos de consulta de 44px como caminho equivalente para alvos SVG pequenos/sobrepostos. Não se afirma que 24 unidades SVG equivalem a 24 CSS px; a exceção por controle equivalente e a árvore assistiva exigem conferência real. Geometria, parsing, serviços, timestamps e strings financeiras não foram modificados. Não há animação nova.
- 6.10: integração com HttpTestingController confirma exatamente quatro GETs financeiros por carga/reload (resumo, posições, resultados e evolução), nenhum POST automático, estados independentes da evolução e ausência de remontagem ao finalizar/falhar indicadores. Testes existentes de snapshot manual, double-submit e respostas tardias permaneceram aprovados. Nenhuma consulta causada por interação com pontos.
- 6.11: matriz determinística aprovada para vazio, uma observação, múltiplas observações, BRL/USD, gaps, snapshots vazios, timestamps próximos, valores extensos e pontos coincidentes com controles individuais no histórico. Dataset original preservado; testes de geometria existentes continuam aprovados.
- Testes focados: 83/83, 10 arquivos. Suíte completa: 393/393, 61 arquivos. VITEST_MAX_WORKERS=2 somente no processo. Logs locais: frontend-phase6-focused.log e frontend-phase6-full.log. JSDOM não comprova geometria renderizada.
- Build production: initial 514,60 kB (514603 bytes antes e depois desta fase); transferência estimada 122,20 → 122,18 kB. Warning initial acima de 500 kB preservado; nenhum warning de CSS. CSS compilado do Dashboard 2639 bytes e evolução 3863 bytes, abaixo de 4 kB. Dashboard lazy 29390 → 34819 bytes (+5429); main mudou de hash, com os mesmos 129709 bytes; styles globais e demais initial chunks mantiveram nomes e tamanhos. Outros hashes lazy foram substituídos sem alteração dos tamanhos observados. Nenhuma feature da evolução foi promovida ao initial.
- Baseline histórico continua 506,09 kB, diferença aproximada de +8,51 kB. 7.5 permanece aberta. Auditoria: A — crescimento local justificado no lazy por apresentação/interação; B — estilos de coleção compartilhados são compilados em apresentações existentes, oportunidade a medir, sem atribuição causal ao initial; C/D — sem dependência/import acidental ou promoção de Dashboard/evolução ao initial identificados nesta fase; E — oportunidade de revisar regras legadas/sobrepostas de CSS, sem refatoração adicional; F — o desvio histórico exige comparação de composição de builds anteriores antes de atribuir causa exata. Não houve otimização de performance adicional nem mudança de budgets.

Revisão humana pendente da entrega: desktop 1440x900, tablet 768x1024, mobile 390x844 e 320 CSS px; 1280x600, texto 200% e reflow/zoom. Conferir patrimônio em destaque, ordem e integração de posições/resultados; labels e tooltip completos nas bordas; persistência em hover/foco e dispensa com Escape; Enter/Espaço/toque; foco visível e pontos na árvore assistiva; Tab cronológico por série e saída do gráfico; controles equivalentes no histórico para pontos sobrepostos; moedas, gaps e observação única sem tendência artificial. Nenhuma task da Fase 7 foi iniciada ou marcada por estas validações de escopo da Fase 6.

## Reconciliação aprovada — Histórico do patrimônio

As tasks 6.6–6.9 continuam com seus critérios originais e abertas para revisão humana renderizada/assistiva. A apresentação da evolução passa a “Histórico do patrimônio”, com ação “Registrar patrimônio atual” e identificação “Registro #ID”. Contratos, métodos, DTOs e persistência de snapshot permanecem intactos. Não há novos gráficos por ativo nesta change, nem alteração das fórmulas USD, cuja auditoria foi aprovada.

Validação da reconciliação de linguagem: 83/83 testes focados (10 arquivos), 393/393 na suíte completa (61 arquivos), VITEST_MAX_WORKERS=2 somente no processo. Build production aprovado: initial 514,60 kB, transferência estimada 122,17 kB; estável contra 514,60 kB e aproximadamente +8,51 kB sobre o baseline histórico de 506,09 kB. Dashboard lazy 34,82 → 35,05 kB. Warning initial acima de 500 kB permanece; nenhum warning de CSS, sem alteração de budgets. Logs locais frontend-history-focused.log, frontend-history-full.log e frontend-history-build.log.

6.6 permanece aberta para confirmar dimensões, labels e reflow reais; 6.7 para legibilidade/limites e persistência real do tooltip; 6.8 para interação assistiva real; 6.9 para alvos, foco e percurso real, incluindo alternativa do histórico. Testes automatizados de interação e ausência de HTTP foram preservados e passaram. Revisar em 1440x900, 768x1024, 390x844, 320 CSS px, baixa altura e zoom/texto ampliado. Não se considera JSDOM evidência de geometria renderizada.

## Correção de labels temporais — task 6.6

Causa: candidatos selecionados por índice (até quatro) eram posicionados pela geometria temporal sem verificar a largura dos textos. Dois instantes próximos no desktop podiam sobrepor labels. O CSS mobile já ocultava intermediários e foi preservado.

Correção: diretiva de apresentação mede retângulos reais de labels/eixo, prioriza primeiro e último quando cabem e aceita intermediários cronológicos somente sem interseção e com margem de 8 CSS px. Labels fora dos limites são suprimidos por visibility, preservando sua medição; candidatos ocultos pelo CSS mobile continuam inelegíveis. ResizeObserver acompanha eixo/texto e a medição após renderização acompanha mudanças de dados. A altura acompanha labels visíveis e textos extensos podem quebrar linha. Pontos, timestamps, DTOs, geometria, tooltips e histórico não foram alterados; evolution-geometry.ts permanece intacto.

Testes focados: 69/69 em 8 arquivos. Suíte completa: 401/401 em 62 arquivos, VITEST_MAX_WORKERS=2 somente no processo. Cobertura determinística de zero/um/dois/múltiplos labels, espaçamento desktop, colisões no início/meio, extremos, larguras 1200/768/390/320, texto ampliado e preservação dos pontos/registros/consulta após supressão. Retângulos simulados em testes não equivalem a renderização real.

Build production aprovado: initial 514,60 → 514,71 kB (+0,11 kB), aproximadamente +8,62 kB sobre o baseline histórico de 506,09 kB. Transferência estimada 122,17 → 122,15 kB. Dashboard lazy 35,05 → 36,13 kB. Warning initial acima de 500 kB permanece; nenhum warning de CSS; budgets e dependências intactos. Logs locais frontend-axis-focused.log, frontend-axis-full.log e frontend-axis-build.log.

6.6 permanece aberta até nova aprovação visual: reproduzir os três registros, com dois próximos em 05/09 e o terceiro em 07/09, em BRL/USD; conferir desktop/tablet/mobile, texto 200%, zoom/reflow, extremos e labels extensos. Confirmar que todo ponto continua consultável via tooltip e histórico. Os critérios/checklists 6.7–6.9 não foram alterados nem concluídos nesta rodada.

## Refinamento de extremidades e mini gráficos — 6.6 aberta

A revisão humana identificou quebra caractere por caractere no último label: posicionamento absoluto com largura automática próximo da borda, transform aplicado após layout e overflow-wrap:anywhere comprimiam o texto. A correção usa width:max-content e white-space:nowrap, ancoragem dos extremos para dentro e clamp medido por retângulos renderizados. Primeiro e último têm prioridade; quando não cabem juntos, tenta-se data curta (dia/mês), sem alterar timestamps. Intermediários conflitantes são suprimidos; nenhum ponto ou registro é removido. Texto que sozinho não cabe permanece consultável integralmente no tooltip/histórico, sem overflow obrigatório.

Em largura de contêiner até 36rem, a grade tenta colunas de no mínimo 10rem com intervalo de 0,5rem; duas colunas requerem 20,5rem disponíveis (328px com rem de 16px). Com o padding atual do shell, 390px oferece aproximadamente 358px e permite duas colunas; 320px oferece aproximadamente 288px e empilha. Estas dimensões são deduzidas do CSS, não são evidência de navegador. Títulos compactos BRL/USD, SVG proporcional, legendas contínua/tracejada e escalas independentes preservados; espaço reservado vazio do tooltip reduzido. Tooltip completo e histórico textual único permanecem disponíveis, inclusive controles equivalentes para pontos sobrepostos.

Validações: focados 79/79 (8 arquivos), suíte completa 411/411 (62 arquivos), VITEST_MAX_WORKERS=2 somente no processo. Testes novos cobrem clamp, extremos, larguras 1200/768/390/320/160, texto natural extenso/ampliado, abreviação e restauração no resize, preservação de registros/pontos e ausência de HTTP adicional. Retângulos simulados e assertions de CSS não comprovam layout renderizado.

Build production aprovado: initial 514,71 kB, estável contra o último estado; transferência estimada 122,12 kB. Dashboard lazy 36,13 → 37,11 kB. Baseline histórico 506,09 kB permanece distinto (+8,62 kB); 7.5 não foi marcada. Warning initial acima de 500 kB permanece e há novo warning de CSS da evolução: 4,22 kB, 224 bytes acima do limiar de aviso de 4 kB. Budgets e dependências inalterados. Logs locais frontend-mini-focused.log, frontend-mini-full.log e frontend-mini-build.log.

6.6–6.9 permanecem abertas, sem modificação dos critérios. Revisão humana necessária: extremos e instantes próximos em desktop, 768px, 390px e 320 CSS px; mini gráficos somente quando legíveis; texto 200%/zoom e strings extensas sem quebra vertical/scroll horizontal; tooltip completo nas bordas e por teclado/toque; Tab cronológico, foco e controles equivalentes do histórico. Total preservado: 50/72 concluídas, 22 abertas. Nenhuma alteração em evolution-geometry.ts, cálculos, dados, contratos, HTTP, backend ou Fase 7.

## Refinamento desktop e horário local — 08/09/2026

Causa do empilhamento desktop: a grade padrão impunha uma coluna; auto-fit existia somente no container até 36rem. Agora a grade padrão usa repeat(auto-fit,minmax(min(100%,16rem),1fr)), gap de 1,5rem e cabeçalho com flex-wrap. A regra compacta aprovada permanece integral: mínimo de 10rem e gap de 0,5rem até 36rem. Desktop 1440x900 e tablet com espaço suficiente devem mostrar BRL/USD lado a lado; 390px mantém duas colunas com o shell atual; 320px empilha. Texto ampliado/zoom permite empilhamento conforme largura disponível e rem. Estes comportamentos são derivados do CSS e cobertos estruturalmente, não constituem validação renderizada.

O formatador local existente da evolução agora apresenta DD/MM/YYYY, HH:mm (horário local) em tooltip, registros, descrição da consulta e nomes acessíveis. Timestamps recebidos e atributos datetime permanecem completos; eixo compacto, pontos, datasets, gaps, geometria e interações preservados.

Validações: Dashboard/evolução 80/80 testes em 8 arquivos; suíte completa 412/412 em 62 arquivos, VITEST_MAX_WORKERS=2. Build production aprovado: initial 514,71 → 514,71 kB; Dashboard lazy 37,11 → 37,16 kB; CSS evolução 4,22 → 4,28 kB (+60 bytes, warning agora 284 bytes acima de 4 kB), pelo auto-fit e flex-wrap. Sem alteração de budgets/dependências. Logs: frontend-refinement-focused.log, frontend-refinement-full.log e frontend-refinement-build.log. OpenSpec strict change aprovado e global 32/32; git diff --check sem erros, apenas avisos LF/CRLF. Git status mantém o conjunto preexistente de arquivos modificados/não rastreados.

6.6–6.9 continuam abertas, aguardando revisão humana: conferir 1440x900, tablet, 390px, 320px, texto 200% e zoom; headings/legendas sem compressão, extremos sem colisão, um único histórico abaixo, horários iguais no ponto/registro e navegação por foco, Enter/Espaço, Escape e toque. Nenhuma execução renderizada/assistiva foi inferida dos testes. Nenhum Graphify, archive ou Git mutável executado.

## Aprovação humana integral da Fase 6 — 08/09/2026

O usuário declarou concluída e aprovada a revisão visual/manual da Fase 6 e explicitamente satisfeitas as pendências de 6.6–6.9. A aprovação, combinada com a evidência técnica já registrada (80/80 testes focados e 412/412 na suíte completa), satisfaz os critérios das quatro tasks sem alteração de requisitos:

- 6.6: BRL/USD, layout responsivo desktop/mobile, labels do eixo sem sobreposição, horário local, histórico textual e apresentação geral aprovados; preservação de SVG, dados, valores e timestamps coberta pelos testes anteriores.
- 6.7: tooltip/consulta dos pontos e sua apresentação aprovados; comportamento dispensável, persistente e alcançável coberto pela aprovação integral das pendências e pelos testes existentes.
- 6.8: interação por mouse/toque e teclado, foco e Escape aprovados; equivalência Enter/Espaço e ausência de efeito financeiro protegidas pelos testes existentes.
- 6.9: aprovação integral da navegação/interação e apresentação resolve a pendência humana de alvos, espaçamento, foco e percurso; pontos, projeção e gaps permanecem protegidos pela evidência técnica anterior.

A execução humana é atribuída ao usuário. Este registro não afirma nova execução pelo agente, nem inventa medições, viewports específicos ou uso de leitor de tela não individualizados no relato. A auditoria transversal assistiva da Fase 7 permanece separada. Registros anteriores de pendência são históricos e ficam superados por esta aprovação, sem reescrita retroativa.

Contagem conferida nos checkboxes: 50/72 antes, 54/72 após concluir exclusivamente 6.6–6.9; 18 abertas (1.3 e 7.1–7.17). Fase 6: 11/11. Nenhuma task da Fase 7 foi marcada ou implementada nesta rodada; sua análise é somente de leitura. Sem alteração de código-fonte, Graphify, archive ou operações Git mutáveis.

## Consolidação da Fase 7 — 08/09/2026

Auditoria e evidências finais: [evidencias-fase-7.md](evidencias-fase-7.md). Retomada sem nova alteração de código nem repetição dos testes/build já concluídos: 247/247 focados, 412/412 completos; initial 514418 bytes (514,42 kB), redução de 293 bytes, ainda 8332 bytes acima do baseline reproduzido de 506086 bytes.

Concluídas individualmente: 7.1–7.4, 7.6, 7.8–7.9 e 7.13–7.17. A conclusão de 7.14 registra expressamente a exceção autorizada de CorretoraNamesService; não afirma ausência absoluta de consultas adicionais/enriquecimento de IDs. Abertas: 7.5 (critério de bundle não atendido), 7.7 (leitor de tela/anúncios), 7.10 (matriz específica de fronteiras/tablet/textos extensos), 7.11 (200%/320 CSS px no conjunto final) e 7.12 (reduced motion real). Aprovações gerais anteriores não foram convertidas em execuções específicas não individualizadas.

Contagem: 66/72 concluídas; Fase 7: 12/17. A task 1.3 permanece aberta como limitação histórica aceita. Fase 6 permanece 11/11. Critérios originais e registros históricos preservados. Nenhum archive, Graphify ou Git mutável.

## Encerramento por evidência e dispensas formais — 08/09/2026

Registro final autorizado explicitamente pelo usuário nesta conversa, após declarar que todos os cenários manuais solicitados foram executados e passaram. Este registro supera os estados de pendência anteriores, que permanecem como histórico; não altera o significado dos critérios nem atribui a execução humana ao agente.

- **7.7 concluída:** evidência técnica A1/A2 de skip link, foco, semântica e alert/status, agora complementada pela aprovação humana integral de acessibilidade transversal, leitor de tela e anúncios, incluindo os cenários solicitados de headings, labels e tabelas.
- **7.10 concluída:** evidência estrutural anterior complementada pela aprovação humana de tablet, fronteiras 959/960/961px e conteúdo extenso; somada à aprovação anterior de desktop/mobile, satisfaz o critério integral.
- **7.11 concluída:** evidência técnica de reflow complementada pela aprovação humana explícita de texto 200%, reflow 320px, zoom e baixa altura, sem perda de informação, foco oculto ou double scroll.
- **7.12 concluída:** regras técnicas de reduced-motion e feedback textual complementadas pela execução humana aprovada com redução de movimento e informação independente de animação/cor/ícone.

Origem humana H5: mensagem de aprovação final do usuário em 08/09/2026, nomeando as quatro tasks e confirmando execução e aprovação de todos os cenários solicitados. Não foram inventados navegador, leitor de tela específico, capturas ou medições não fornecidos.

**1.3 — DISPENSADA por decisão humana — baseline visual histórico não disponível; encerrada por aceite, não por execução.**

O baseline renderizado anterior não foi capturado, não existe e não pode ser reconstruído de forma confiável. Capturas atuais não o substituem. As revisões posteriores foram efetivamente realizadas e aprovadas; o usuário aceitou conscientemente a ausência histórica para encerramento. Checkbox desmarcado preservado, sem afirmar captura realizada.

**7.5 — DISPENSADA por decisão humana — critério literal de initial não atingido; desvio analisado, otimizações P0 seguras aplicadas e crescimento estrutural aprovado aceito para encerramento da change.**

Baseline mantido em 506,09 kB (506086 bytes); final 514,42 kB (514418 bytes), diferença 8332 bytes, aproximadamente +8,33 kB. P0 economizaram 293 bytes. A comparação histórica reproduziu +8515 bytes na introdução do contexto global de Carteira aprovado. Dashboard/evolução permanecem lazy, sem promoção indevida, dependência nova, aumento de budgets ou remoção funcional/dataset. As oportunidades restantes exigem refatorações de shell/Material/contexto ou deduplicações profundas, de risco desproporcional nesta etapa. O usuário aceita formalmente o desvio para encerramento; não autoriza nova otimização/refatoração e não afirma atendimento literal. Checkbox desmarcado preservado.

Contagem final conferida: **70/72 concluídas por execução/evidência; 2 dispensadas (1.3 e 7.5); 0 abertas sem justificativa**. Fase 7: 16 concluídas e uma dispensada. Fases 1–7 encerradas funcionalmente; nenhum cenário obrigatório sem evidência ou aceite explícito. A contagem mecânica de checkboxes permanece 70/72, não 72/72.

Revisão pré-archive: proposal/design/specs preservam requisitos originais; as exceções de aceite estão expressamente registradas aqui e nas evidências, sem alegar cumprimento literal dos requisitos dispensados. Referências anteriores a pausas/revisões pendentes são históricas, superadas pelos registros finais. CorretoraNamesService continua documentado como exceção autorizada, consulta compartilhada por instância quando necessária, sem N+1; não se declara ausência absoluta de consultas adicionais. Backend/migrations/DTOs/endpoints, parsing lossless, cálculos, BRL/USD separados sem FX, rotas/contexto global, lazy loading, dependências e budgets preservados. Novos gráficos financeiros continuam fora do escopo; nenhuma nova change criada.

Últimas evidências técnicas permanecem válidas, sem alteração posterior de código: 247/247 focados, 412/412 completos e build production 514,42 kB. Não houve repetição de testes/build. Warnings conhecidos mantidos: initial acima de 500 kB e CSS evolução 4284 bytes, 284 bytes acima de 4 kB. Classificação **A — change formalmente encerrada, sem pendência funcional e pronta para archive**, mediante os aceites explícitos acima; archive ainda não executado. Nenhum Graphify, commit, push ou merge.
