Esta lista acompanha a implementação. Graphify está explicitamente fora do escopo e não constitui task pendente. Marcar conclusão somente com evidência; registrar bloqueio ou aceite combinado formal sem afirmar reprodução manual inexistente.

Evidências técnicas e pendências manuais desta etapa: [evidencias-fases-1-3.md](evidencias-fases-1-3.md). Fases 4–7 não iniciadas.

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
- [ ] 2.2 Refinar paleta verde e superfícies claras com contraste medido; registrar valores escolhidos e comparação visual.
- [x] 2.3 Consolidar tokens de texto, bordas, marca, hover e foco sem introduzir dark mode, seletor ou persistência.
- [x] 2.4 Separar semântica de positivo/negativo financeiro, success, warning, error, info e destructive.
- [x] 2.5 Consolidar escala compartilhada de spacing, radius e elevation sem extrair abstrações locais desnecessárias.
- [x] 2.6 Refinar hierarquia tipográfica de títulos, contexto, indicadores, metadados e ajuda, preservando fontes locais.
- [x] 2.7 Aplicar tipografia numérica tabular e alinhamentos compartilhados sem modificar formatadores.
- [x] 2.8 Refinar superfícies .app-surface, .section-card e .app-form-surface e padrões de badges e estados.
- [ ] 2.9 Revisar regressão visual transversal da fundação e budgets de CSS antes de avançar às páginas.

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
- [ ] 3.11 Verificar foco contido/restaurado, Escape/backdrop e ausência de double scroll nos dialogs e shell.

## 4. Listagens e detalhes

- [ ] 4.1 Aplicar padrão de tabela semântica desktop e cards completos mobile à listagem de Operações, preservando ordem e campos.
- [ ] 4.2 Aplicar o mesmo padrão ao histórico contextual da Carteira, sem consultas ou efeitos duplicados.
- [ ] 4.3 Aplicar tabela desktop e cards completos mobile à listagem de Ações, preservando busca exata e cadastro existentes.
- [ ] 4.4 Refinar lista compacta de Corretoras mantendo dados, status, busca por CNPJ e ações.
- [ ] 4.5 Refinar cards de Carteiras com nome, data e ações hierárquicos, sem indicadores adicionais.
- [ ] 4.6 Refinar detalhe de Corretora em identificação, contatos, endereço e situação, preservando dados opcionais e avisos.
- [ ] 4.7 Refinar detalhe de Ação destacando identificação, última cotação registrada e data, mantendo atualização manual.
- [ ] 4.8 Refinar detalhe de Carteira separando contexto, ações e histórico, mantendo edição/exclusão existentes.
- [ ] 4.9 Refinar detalhe de Operação com grupos legíveis, mantendo campos aprovados e sem enriquecimento de IDs.
- [ ] 4.10 Verificar equivalência de campos/ações e uma única representação acessível/focável ativa em cada breakpoint, sem novos filtros, sort, paginação ou HTTP.

## 5. Formulários

- [ ] 5.1 Refinar formulário de Corretora somente CNPJ em página/dialog, mantendo prefill, confirmação e validações.
- [ ] 5.2 Refinar formulário de Ação somente ticker e mercado em página/dialog, sem alterar máscaras ou requisições.
- [ ] 5.3 Refinar criação e edição de Carteira somente nome em página/dialog, mantendo submissão e cancelamento.
- [ ] 5.4 Agrupar visualmente contexto, tipo/movimentação, quantidade/preço/data, Corretora, estimativa e ações do formulário de Operação.
- [ ] 5.5 Verificar COMPRA com preço somente leitura, prévia existente e POST sem preço; preservar troca de contexto e bloqueios.
- [ ] 5.6 Verificar VENDA com sugestão editável, precisão, estimativa existente, strings decimais e data civil inalteradas.
- [ ] 5.7 Verificar Carteira fixa contextual, Corretora opcional, labels/ajudas/erros, foco, submit/cancel e double-submit em todos os formulários.

## 6. Dashboard e evolução patrimonial

- [ ] 6.1 Reordenar visual e semanticamente Dashboard em contexto, ações, indicadores, evolução, posições e resultados.
- [ ] 6.2 Refinar seletor e ações mantendo query params, navegação contextual e conjunto funcional de requisições.
- [ ] 6.3 Refinar os quatro indicadores autoritativos por moeda com patrimônio em destaque, sem total combinado nem métrica nova.
- [ ] 6.4 Aplicar tabela semântica desktop e cards completos mobile às posições preservando todos os campos e moedas.
- [ ] 6.5 Refinar resultados realizados comparáveis por Ação, sem totalização.
- [ ] 6.6 Refinar dimensões, labels, contraste e histórico da evolução preservando SVG local, dados completos e datas/valores.
- [ ] 6.7 Refinar tooltip para legibilidade e limites de viewport com conteúdo dispensável, persistente e alcançável quando aplicável.
- [ ] 6.8 Implementar equivalência acessível dos pontos entre clique/toque, foco, Enter/Espaço e Escape sem efeito financeiro.
- [ ] 6.9 Refinar alvos/spacing, foco e ordem de Tab sem deslocar pontos, alterar projeção financeira ou conectar gaps.
- [ ] 6.10 Verificar loading/erro independentes da evolução, Atualizar dados sem POST e registro somente por ação manual.
- [ ] 6.11 Verificar deterministicamente BRL, USD, moedas simultâneas, zero/um/múltiplos snapshots, gaps e snapshots vazios após o refinamento.

## 7. Validação final

- [ ] 7.1 Revisar linguagem de todas as páginas, removendo jargão editorial dispensável sem perder diagnóstico ou avisos financeiros.
- [ ] 7.2 Executar testes focados dos componentes e fluxos afetados, acrescentando somente cobertura relevante para semântica/interação e regressões.
- [ ] 7.3 Executar suíte frontend completa e registrar resultado, sem declarar cobertura manual a partir dela.
- [ ] 7.4 Executar build na mesma configuração do baseline, comparar initial/lazy e budgets de estilos, sem aumentar budgets ou dependências.
- [ ] 7.5 Comprovar initial não superior ao baseline medido e ausência de redução de dataset/funcionalidade para atingir o critério.
- [ ] 7.6 Validar manualmente contraste textual/não textual de superfícies, estados, marca, resultados e ações destrutivas.
- [ ] 7.7 Validar teclado, foco visível/não oculto, skip link, headings, labels, tabelas, leitor de tela e anúncios alert/status.
- [ ] 7.8 Validar dialogs com foco contido/restaurado, cancelamento e ações pendentes em desktop/mobile/baixa altura.
- [ ] 7.9 Validar gráfico com mouse, toque e teclado: role, Enter, Espaço, Escape, hover/focus, tooltip, alvos próximos, Tab e histórico textual completo.
- [ ] 7.10 Validar desktop, tablet e mobile, fronteiras 959/960/961px, textos longos e valores extensos em todas as features.
- [ ] 7.11 Validar ampliação textual 200%, reflow a 320 CSS px, zoom e baixa altura sem informação removida, foco oculto ou double scroll.
- [ ] 7.12 Validar reduced-motion e ausência de dependência exclusiva de cor, ícone ou animação para feedback.
- [ ] 7.13 Comparar fluxos HTTP/rotas/query params/payloads antes e depois, incluindo compra/venda, busca, dialogs, contexto concorrente, reload e snapshot manual.
- [ ] 7.14 Confirmar parsing lossless, precisão, formatadores, BRL/USD e ausência de FX/cálculos novos, enriquecimento de IDs ou requests adicionais.
- [ ] 7.15 Registrar evidências manuais por cenário e bloqueios externos; eventual aceite combinado exige justificativa explícita sem fabricar execução.
- [ ] 7.16 Revisar diff de escopo, dependências e lazy loading; confirmar nenhum backend, contrato, banco, migration ou budget alterado.
- [ ] 7.17 Executar OpenSpec strict da change e global, git diff --check e git status --short; relatar warnings e pendências sem Graphify, commit ou archive.
