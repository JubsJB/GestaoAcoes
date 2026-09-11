# Plano de execução

Planejamento aprovado; implementação incremental autorizada. Todas as tasks abaixo permanecem abertas até execução/evidência ou aprovação humana correspondente. Trabalhar em rodadas de 2–3 etapas, respeitando as dependências e os pontos de parada. Não repetir suíte/build quando o código validado permanecer inalterado.

P0: todas as entregas funcionais e validações abaixo. P1: área decorativa opcional, avaliada na task 2.4; adiamento documentado não bloqueia linha P0. P2: séries históricas futuras constam apenas no design e não autorizam implementação.

## 1. BLOCO 1 — Refinar os gráficos existentes

Rodadas sugeridas: 1.1–1.3, 1.4–1.6 e 1.7–1.8.

- [x] 1.1 Obter aprovação humana deste planejamento/tipos; confirmar a base visual aprovada e registrar uma captura real do estado inicial desta nova change, sem alegar acesso às imagens ausentes ou à captura antiga inexistente.
- [x] 1.2 Corrigir somente a apresentação local de zeros e suplementos exatos; testar zero científico, sinal, pequeno não zero e valores extensos sem mudar strings autoritativas, parser ou formatadores globais.
- [x] 1.3 Substituir barras pareadas de Custo × Valor atual por barra de valor atual e marca de custo; testar igualdade, diferenças pequenas/grandes, extremos, dataset integral e escalas independentes por moeda.
- [x] 1.4 Refinar labels, legenda e leitura das barras divergentes de Resultado não realizado; preservar zero central, valor autoritativo, estados textuais e ausência de barra artificial para zero, com testes correspondentes.
- [x] 1.5 Refinar a apresentação do dot plot de Rentabilidade por ativo, preservando escala, sinal, texto autoritativo e natureza atual; testar positivos, negativos, zero e percentuais extremos.
- [x] 1.6 Executar testes focados afetados, protegendo acessibilidade textual, todas as posições, separação BRL/USD e os mesmos quatro GET financeiros do Dashboard.
- [x] 1.7 Validar desktop renderizado e ausência de regressão estrutural evidente em tablet/mobile; executar suíte com dois workers, production, comparação de bundles/CSS e strict; registrar evidência e warnings sem alterar budgets.
- [x] 1.8 Verificar diferenciação real de silhueta/função entre barra com referência, divergente e dot plot, não apenas CSS; obter e registrar revisão humana desktop do Bloco 1 (aprovada posteriormente pelo usuário na revisão conjunta com dados variados); não marcar aprovação automatizada como humana.

## 2. BLOCO 2 — Novas visualizações financeiras

Rodadas sugeridas: 2.1–2.3, 2.4–2.6 e 2.7–2.8. A visualização nova selecionada é composição; comparações redundantes avaliadas no design não são entregas adicionais.

- [x] 2.1 Implementar projeção isolada de ângulos a partir de valorAtualPosicao, exclusivamente para geometria por moeda; testar extremos e preservar tokens, sem total monetário ou percentual exibido derivado.
- [x] 2.2 Implementar como P0 rosca por moeda, centro identificado sem total calculado ou resumo presumido sincronizado, e legenda integral com ticker, nome e valor autoritativo; testar BRL, USD, ambos, uma posição e ausência de posições, sem dependência nova.
- [x] 2.3 Cobrir zeros, grupo integralmente zero, valor inválido/negativo, muitos ativos e fatias indistinguíveis; preservar todos os itens, ordem, alternativa textual e associação que não dependa somente de cor.
- [x] 2.4 Refinar como P0 linha ampla do Histórico do patrimônio, pontos discretos, grid, eixo e tooltip, explicando registros manuais; avaliar área segmentada P1 e registrar adoção segura ou adiamento; proteger coordenadas, segmentos retos, gaps, registros vazios, ponto único, timestamps próximos, IDs, datas completas e histórico textual único.
- [x] 2.5 Integrar composição com a resposta de posições existente e executar testes focados de HTTP e interação do histórico: teclado, toque, Escape e registro manual, sem GET adicional ou coleta automática.
- [x] 2.6 Conferir composição/histórico em desktop real e ausência de overflow estrutural evidente em tablet/mobile, mantendo dataset integral e sem antecipar redesign geral do Bloco 3.
- [x] 2.7 Executar suíte completa com dois workers, production, comparação de bundles/CSS e strict; registrar evidência dos dados, geometria e limitações, reaproveitando resultados ainda válidos.
- [x] 2.8 Obter e registrar revisão humana desktop do Bloco 2 antes de iniciar o Bloco 3.

## 3. BLOCO 3 — Composição final do Dashboard desktop

Rodadas sugeridas: 3.1–3.3 e 3.4–3.6. Este bloco organiza os gráficos definidos nos anteriores; não cria métricas ou novos gráficos.

- [x] 3.1 Compor primeira faixa analítica com composição e custo/valor, com proporção aproximada 40/60 quando as larguras mínimas legíveis couberem e empilhamento quando necessário, seguindo a referência A descrita no design.
- [x] 3.2 Compor resultado e rentabilidade lado a lado, histórico em largura total com grupos monetários internos e detalhes abaixo; preservar shell, ações, indicadores e ordem semântica definida no design.
- [x] 3.3 Ajustar espaçamento, alinhamento, títulos e densidade sem cards por ativo ou alturas vazias forçadas; testar hierarquia, dataset integral e ausência de ocultação de informação.
- [x] 3.4 Executar testes focados e conferir renderização desktop 1440×900, largura intermediária e baixa altura, incluindo valores/nomes longos; realizar somente verificação estrutural breve de tablet/mobile nesta rodada.
- [x] 3.5 Executar suíte completa com dois workers, production, comparação de initial/lazy/CSS, strict e diff check; registrar evidência, requests e preservação de budgets/dependências, reutilizando verificações válidas.
- [x] 3.6 Conferir cinco famílias visuais P0 (barra/referência, divergente, dot plot, rosca e linha) com perguntas claras e gráficos protagonistas; obter e registrar revisão humana da composição desktop final antes do fechamento; nenhuma aprovação anterior vale automaticamente para a nova composição.

## 4. Validação final

Rodadas sugeridas: 4.1–4.3 e 4.4–4.5.

- [x] 4.1 Consolidar revisão responsiva renderizada em tablet, 390px, 320px, 959/960/961px, baixa altura, fonte 200% e zoom desktop 400%; corrigir somente regressões desta change e documentar resultados reais.
- [x] 4.2 Conferir leitor de tela, headings, moeda/ativo/valor/estado, contraste e foco visível; preservar teclado/toque/Escape do histórico, reduced motion e ausência de focos desnecessários nos gráficos estáticos.
- [x] 4.3 Consolidar cobertura lossless, zeros/extremos, moedas independentes, dataset integral, ausência de recomputação/FX e quatro GET; reutilizar suíte/build aprovados se código inalterado ou reexecutar verificações afetadas e suíte/build se houver correção.
- [x] 4.4 Registrar baseline versus resultado final de initial, Dashboard lazy e todos os CSS afetados, warnings e budgets intactos; executar strict da change/global, git diff --check e git status --short, sem archive ou versionamento automático.
- [x] 4.5 Obter aprovação humana final e reconciliar tasks/evidências somente com resultados reais, registrando limitações expressamente aceitas; parar para autorização separada de encerramento/archive.
