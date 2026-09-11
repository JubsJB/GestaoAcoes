Implementação incremental autorizada pelo usuário em 08/09/2026: somente Fases 1–3 e integração necessária de Custo × Valor atual no Dashboard. Resultado não realizado e Rentabilidade aguardam autorização posterior. Evidências desta rodada em evidencias-fases-1-3.md. Conclusão exige evidência específica, sem inferir validação visual de testes DOM. Graphify, Git mutável e archive permanecem fora desta execução.

## 1. Auditoria e contratos

- [x] 1.1 Reconfirmar checkpoint, contratos de posições, limites de arquivos e baselines de teste/build registrados no design antes de implementar.
- [x] 1.2 Preparar fixtures readonly de 0/1/3/100 posições, BRL/USD, sinais, igualdade, extremos e quantidades fracionárias, incluindo campos deliberadamente não deriváveis entre si.
- [x] 1.3 Registrar matriz de rastreabilidade dos cenários; encerrada com aceite explícito da ausência da captura visual anterior como limitação documental. A captura não existe e não foi realizada.

## 2. Fundação compartilhada dos gráficos

- [x] 2.1 Implementar helper geométrico puro local à feature com normalização de magnitude, razões limitadas, zero/sinal lossless e preservação das strings originais, sem alterar evolution-geometry.ts.
- [x] 2.2 Testar igualdade, simetria, todos-zero, decimais extensos, notação científica 1e400/1e-400, razão subpixel e imutabilidade de origem.
- [x] 2.3 Criar estrutura de análise com agrupamento por moeda, identificação estável, estilos/tokens compartilhados e equivalente textual visível sem tab stops.
- [x] 2.4 Integrar formatadores existentes e representação completa lossless para precisão adicional/valores abaixo da resolução, sem mudar formatadores globais.

## 3. Custo × Valor atual

- [x] 3.1 Implementar duas barras paralelas por ativo, rótulos de série e escala linear comum por moeda com origem zero.
- [x] 3.2 Testar custo igual/maior/menor que valor atual, valor zero, uma/múltiplas posições e escalas monetárias independentes.
- [x] 3.3 Verificar textos completos, séries distinguíveis sem cor e preservação de pequenos valores sem mínimo artificial.

## 4. Resultado não realizado

- [x] 4.1 Implementar barras divergentes usando resultadoNaoRealizado recebido e escala simétrica por moeda.
- [x] 4.2 Testar lucro/prejuízo de igual magnitude, neutro/-0, todos-zero, extremos e ausência de recálculo por custo/valor.
- [x] 4.3 Verificar valor monetário e estado textual sempre visíveis, sem tooltip obrigatório ou largura fictícia para zero.

## 5. Rentabilidade

- [x] 5.1 Implementar dot plot percentual divergente (revisão do Bloco 2) em grupos BRL/USD com escalas percentuais independentes por moeda, separadas dos eixos monetários.
- [x] 5.2 Testar positivo/negativo/zero, percentuais iguais entre moedas, valores extensos e ausência de soma/média/recálculo.
- [x] 5.3 Verificar identificação de ativo/moeda e percentual completo com sinal/estado, inclusive abaixo da precisão visual usual.

## 6. Integração e hierarquia do Dashboard

- [x] 6.1 Inserir análise após indicadores e antes do Histórico do patrimônio, usando somente data.posicoes já carregado e preservando seções existentes.
- [x] 6.2 Integrar vazio/loading/erro e troca de Carteira sem dados antigos, novo seletor, consultas ou subscriptions financeiras duplicadas.
- [x] 6.3 Testar quatro GETs financeiros por carga/reload, ausência de HTTP dos gráficos, respostas obsoletas, rotas/query params e POST somente manual.
- [x] 6.4 Proteger regressão do histórico: montagem independente, moedas/gaps/timestamps/observações, tooltip, teclado/toque e registros vazios; posições e resultados realizados intactos.

## 7. Acessibilidade e responsividade

- [x] 7.1 Testar estrutura semântica, equivalente textual único por gráfico, ausência de tab stops decorativos e preservação dos valores de 100 ativos.
- [x] 7.2 Validar manualmente leitor de tela, headings, nomes, ordem de leitura, foco existente, contraste e interpretação sem cor.
- [x] 7.3 Validar 1440x900, 768x1024, 390x844, 320 CSS px e 959/960/961 com 0/1/poucos/muitos ativos, nomes longos e valores extensos.
- [x] 7.4 Validar texto 200%, zoom/reflow, baixa altura 1280x600 e reduced motion, sem conteúdo perdido, foco oculto ou scroll concorrente.
- [x] 7.5 Obter aprovação humana da densidade e hierarquia final, mantendo o Histórico do patrimônio com a finalidade original.

## 8. Regressão, performance e encerramento

- [x] 8.1 Executar testes focados de geometria/componentes/Dashboard/evolução/contexto e suíte frontend completa com VITEST_MAX_WORKERS=2, sem enfraquecer assertions.
- [x] 8.2 Executar build production comparável e registrar initial/transfer/Dashboard lazy/CSS/warnings; comprovar initial não superior a 514418 bytes e explicar crescimento lazy, sem aumentar budgets.
- [x] 8.3 Auditar diff, contratos, lossless, fórmulas, HTTP, ausência de FX e preservação lazy/dependências; não herdar automaticamente dispensa histórica de performance.
- [x] 8.4 Consolidar evidências automatizadas/manuais e limitações por cenário, distinguindo execução, aceite e pendência.
- [x] 8.5 Executar OpenSpec strict change/global, git diff --check e git status --short; apresentar relatório e aguardar autorização separada para archive/versionamento.

Dependências: 1 → 2 → 3/4/5 → 6 → 7 → 8. As três visualizações compartilham a fundação e podem ser desenvolvidas em sequência sem alterar contratos. Testes unitários acompanham cada fase; revisão humana não é substituída por strict/build. Contagem inicial: 0/30 concluídas.

Estado após a rodada incremental: **9/30 concluídas; 21 abertas**. A task 1.3 tem matriz técnica registrada, mas permanece aberta por ausência de captura anterior renderizada; não há dispensa nem evidência visual fabricada. Tasks 6.x–8.x mantidas abertas para a integração/validação final das três visualizações: os testes e o build executados nesta rodada cobrem apenas a entrega autorizada. Revisão humana do primeiro gráfico ainda pendente em 7.2–7.5. Aprovação do planejamento não equivale a aprovação da implementação renderizada.

## 9. Bloco 1 — direção vigente (09/09/2026)

A versão anterior do comparativo não foi aprovada visualmente. Fases 4–5 continuam adiadas; checks históricos não representam aceite visual. Esta rodada substitui o refinamento isolado e autoriza somente as tarefas abaixo.

- [x] 9.1 Revisar change, specs e decisões; reconciliar artefatos e registrar rejeição visual anterior.
- [x] 9.2 Reorganizar estrutura visual e indicadores sem alterar contexto ou finanças.
- [x] 9.3 Refazer comparativo com guias, pares contorno/sólido e linguagem simples; preservar dataset, geometria e texto lossless.
- [x] 9.4 Modernizar histórico por template/CSS preservando coordenadas, dados e interações.
- [x] 9.5 Validar testes focados/completos, build/bundle antes/depois, auditoria, strict change/global e diff check/status.
- [x] 9.6 Verificar layout renderizado em desktop, tablet, 390/320 CSS px, baixa altura e texto ampliado/reflow.
- [x] 9.7 Obter revisão visual humana do Bloco 1 incluindo acessibilidade/densidade; parar sem avançar gráficos posteriores.

Validação do Bloco 1: 9.1–9.5 concluídas; 9.6 permanece aberta pela limitação de reflow no shell em 320 px + fonte 200%; 9.7 aguarda aprovação visual humana. Evidências e arquivos em evidencias-fases-1-3.md, seção Bloco 1. Estado atual: **14/37** tarefas concluídas. Não avançar Fases 4–5.

Correção responsiva posterior: 9.6 concluída após diagnóstico e 10 cenários em Edge real; causas e solução em evidencias-fases-1-3.md, seção Correção das pendências responsivas. Revisão humana 9.7 continua aberta. Estado atual: **15/37**. O registro anterior de pendência foi preservado como histórico, não como estado vigente.


## 10. Bloco 2 — desktop prioritário

Desktop do Bloco 1 aprovado visualmente pelo usuário. 9.7 permanece aberta somente quanto à revisão humana ampla de mobile/acessibilidade; não reabrir desktop aprovado. Implementar fases 4–5 e concluir integração aplicável, sem reabrir o comparativo. Nova direção substitui adiamento anterior e escala percentual comum: escalas percentuais independentes por moeda. Revisão mobile detalhada adiada para acelerar entrega, sem aceitar regressões graves.

- [x] 10.1 Reconciliar direção, fontes autoritativas, moedas e aceite desktop do Bloco 1.
- [x] 10.2 Validar novos painéis em desktop e verificar regressão grave em 320/390, preservando correções do shell.
- [x] 10.3 Registrar testes, build, bundle, strict, diff/status e evidências do Bloco 2.
- [x] 10.4 Obter revisão humana conjunta dos três gráficos no desktop; não antecipar reformulação de Custo × Valor atual.

Bloco 2 concluído tecnicamente: fases 4–6, teste 7.1 e validações 8.x concluídos com evidências específicas em evidencias-bloco2.md. 10.2–10.3 concluídos; 10.4 aguarda revisão conjunta. Mobile detalhado e validação assistiva humana permanecem pendentes, conforme prioridade desktop autorizada. Contagens anteriores registram checkpoints históricos.

## 11. Revisão do Bloco 2 — dot plot

- [x] 11.1 Registrar diagnóstico aprovado e direção est?tica sem extensão financeira.
- [x] 11.2 Substituir somente barras de rentabilidade por marcadores proporcionais e testar sinais, zero, extremos, moedas e dataset.
- [x] 11.3 Executar testes focados/completos, build, strict e diff check; registrar evidências.
- [x] 11.4 Obter aprovação visual humana do Bloco 2 no desktop; não avançar sem escopo aprovado para o Bloco 3.

## Aceite desktop do Bloco 2 e verificação do próximo bloco

O usuário aprovou a composição da Análise por ativo, os três gráficos (incluindo o dot plot), a separação BRL/USD, a diferenciação visual, o zero central, os estados e percentuais e a composição desktop dos painéis. Tasks 10.4 e 11.4 concluídas nesse âmbito. Registros anteriores de revisão desktop pendente são históricos e foram superados por este aceite.

Continuam abertas 1.3 (captura anterior ausente, sem fabricar evidência), 7.2–7.4 (validações detalhadas), 7.5 (aceite final da change) e 9.7 (revisão ampla além do desktop já aprovado). Mobile detalhado, texto ampliado e leitor de tela permanecem adiados para a validação final. Estado: **39/45 tarefas concluídas**, seis abertas.

Candidatos não bloqueantes para revisão final, sem implementação agora: legibilidade de Custo × Valor atual quando os valores são próximos; apresentação de notação técnica como `0E-12 USD`, preservando precisão e dado autoritativo.

A releitura integral dos artefatos não identificou tasks de implementação de um Bloco 3. A seção 3 é a fase de Custo × Valor atual, já concluída; não representa um novo bloco. Não reclassificar as validações adiadas como implementação nem criar escopo novo. Diagnóstico em evidencias-bloco3.md; continuação depende de identificar/definir o escopo com o usuário.

## Auditoria para encerramento — estado vigente

O usuário confirmou aprovação desktop geral, ausência de regressão visual evidente e aceite das entregas anteriores. Não haverá Bloco 3 funcional nem próxima change nesta rodada. O estado acima descreve o checkpoint anterior; esta seção o substitui quanto à contagem e às pendências.

Fechadas 7.3 (C: ensaios Edge por camada, testes de dataset/extremos e aceite humano), 7.5 (B: aprovação humana de leitura, hierarquia e composição) e 9.7 (C: correção responsiva comprovada e aceite das entregas anteriores). A cobertura de 7.3 é amostral, não o produto cartesiano de todos os dados por viewport; mobile profundo foi explicitamente declarado não bloqueante. O fechamento de 9.7 não afirma execução de leitor de tela, que continua na task específica 7.2.

**42/45 tasks concluídas; três abertas: 1.3, 7.2 e 7.4. Nenhuma task dispensada.** Classificação individual, evidências reutilizadas e roteiro mínimo em evidencias-encerramento.md. Não foi encontrada regressão funcional que exija código. Captura histórica ausente não foi inventada nem dispensada implicitamente. Aprovação desktop não foi convertida em aceite de leitor de tela ou de texto ampliado dos painéis novos.

Quatro candidatos aceitos como não bloqueantes e destinados a trabalho futuro quando aplicável: intuitividade do comparativo com valores próximos, apresentação de `0E-12 USD`, refinamentos visuais profundos de mobile e gráficos adicionais. Não são tasks novas desta change. Archive continua não autorizado nesta rodada.

## Encerramento final após revisão humana

O usuário aprovou explicitamente 7.2: leitor de tela confirmou headings, ativo, moeda, valores e estados compreensíveis, navegação adequada, foco e interação do Histórico do patrimônio. Aprovou 7.4: 320 px com fonte 200% e desktop com zoom 400% sem perda de conteúdo/funcionalidade, cortes relevantes ou foco inacessível; baixa altura e reduced motion reutilizam as evidências anteriores válidas.

Para 1.3, o usuário aceitou formalmente encerrar sem captura visual anterior. A matriz existe; a captura não existe e não é registrada como realizada. Encerramento por aceite explícito de limitação documental, com dispensa apenas dessa captura.

**45/45 tasks encerradas; zero abertas: 44 concluídas por execução/evidência/aceite e uma (1.3) encerrada com limitação documental explicitamente aceita.** Nenhuma pendência funcional, manual ou documental bloqueante. Classificação A — change formalmente encerrada e pronta para archive, ainda não arquivada. Resultados anteriores pendentes são checkpoints históricos superados por este registro. Não repetir testes/build por alteração documental; números e warnings preservados em evidencias-encerramento.md.
