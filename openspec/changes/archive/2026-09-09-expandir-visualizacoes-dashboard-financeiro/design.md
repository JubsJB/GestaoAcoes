## Context

Ver proposal.md para motivação e limites. Auditoria somente leitura no checkpoint a0429c8, branch feature/frontend-angular, sem alterações anteriores no working tree. PRD RF17–24 centraliza custo, valor, resultados e rentabilidade no backend; RNF07/08/10 orientam responsividade, clareza e manutenção. As specs canônicas atuais exigem moedas independentes, HTTP estável, dataset integral e histórico manual. A exclusão de composição na change arquivada era seu limite; esta nova proposta a inclui sem alterar finanças.

### Inventário de dados e viabilidade

URLs abaixo são relativas à API configurada. Todos os campos financeiros chegam como strings lossless no frontend; IDs permanecem numéricos.

| Visualização/pergunta | Fonte existente | Temporalidade | Sem backend? / cálculo necessário | Moedas e limites |
| --- | --- | --- | --- | --- |
| Situação atual, indicadores | GET /carteiras/{id}/resumo: resumos[].moeda, patrimonioAtual, custoTotalPosicoes, resultadoNaoRealizadoTotal, rentabilidadePercentual | Atual | Sim; nenhum cálculo, apresentação existente | BRL/USD separados; não somar, comparar timestamps ou reconciliar com posições |
| Custo versus valor de cada posição | GET /carteiras/{id}/posicoes: custoPosicao, valorAtualPosicao | Atual | Sim; somente proporções geométricas | Escala zero/máximo comum a custo e valor de todos os ativos da mesma moeda; moedas independentes |
| Ganhos/perdas em dinheiro | Mesmo GET: resultadoNaoRealizado | Atual | Sim; somente posição/comprimento e sinal textual | Zero central, domínio simétrico por moeda; não subtrair custo/valor |
| Desempenho percentual | Mesmo GET: rentabilidadePercentual | Atual | Sim; somente coordenada do ponto | Por moeda, domínio percentual próprio; não dividir resultado por custo, não agregar retornos |
| Distribuição/concentração e maiores valores | Mesmo GET: valorAtualPosicao, moeda, acaoId, ticker, nomeEmpresa | Atual | Sim; pesos normalizados e ângulos exclusivamente visuais, sem percentual textual ou total monetário novo | Rosca por moeda; zeros/muitos ativos requerem legenda integral; não representa saldo de caixa ou diversificação por setor |
| Histórico do patrimônio | GET /carteiras/{id}/evolucao-patrimonial: pontos[].snapshotId, dataHoraSnapshot, patrimonios[].moeda/patrimonioAtual | Observações manuais | Sim; geometria já existente, sem fórmula nova | Duas séries monetárias em gráficos independentes; não é rentabilidade nem cotação contínua |
| Resultado realizado por ativo | GET /carteiras/{id}/resultados-realizados: acaoId, ticker, nomeEmpresa, mercado, moeda, resultadoRealizado | Acumulado atual, sem datas de vendas na resposta | Gráfico estático seria viável, sem cálculo; não selecionado | Repetiria barras existentes e alongaria página; preservar detalhes atuais |
| Eventos de negociações/preços executados | GET /carteiras/{id}/operacoes: tipo, dataOperacao, ordemNoDia, quantidade, precoUnitario, valorTotal, ticker/mercado | Histórico de operações | Endpoint existe, mas exigiria GET adicional; fora do escopo escolhido | Preço executado não é cotação histórica; operação não equivale a aporte/resgate |
| Histórico de cotações de mercado | HistoricoCotacao: cotacao, dataHoraCotacao por Acao; serviço de fechamento pontual para operações | Observações locais esparsas; fechamento pontual externo | Não disponível no contrato do Dashboard; exigiria exposição/coleta adicional | Não é série completa nem fonte suficiente de retorno; excluído |
| Rentabilidade histórica, benchmark, dividendos, distribuição por setor/corretora | Nenhum campo/série correspondente nas quatro respostas do Dashboard | Ausente | Não viáveis com os contratos consumidos; exigiriam dados/regra/contrato adicionais | Excluídos, sem reconstrução, inferência ou FX |

Outros campos de posição: quantidadeAtual, precoMedio, cotacaoAtual, dataHoraCotacao e mercado. Permanecem nos detalhes; não alimentam cálculos para novos gráficos. O endpoint /patrimonio existe, mas não será consumido. Carteiras usa a listagem compartilhada do shell. Total de requests financeiros permanece quatro por carga/reload; POST /carteiras/{id}/snapshots somente pelo botão manual existente.

Fontes lidas: frontend models/dashboard.ts, dashboard.service.ts, evolution.models.ts/evolution.service.ts, position-analysis/* e financial-value.formatter.ts; backend CarteiraResource, PosicaoService, SnapshotCarteiraService, OperacaoResponse e HistoricoCotacao. PosicaoService reproduz operações e usa cotação corrente para retornar apenas posições abertas; SnapshotCarteiraService armazena somente patrimônio por moeda. Não foram consultados dados reais do banco/provedores; nenhuma cobertura histórica é presumida.

Snapshots não conservam custo, posições, retorno ou caixa/fluxos daquele instante. Compra adicional pode aumentar patrimônio sem valorização; venda pode reduzi-lo mesmo com lucro. Logo, variação de patrimônio não será apresentada como retorno, e operações não serão usadas para reconstruí-lo.

### Infraestrutura e origem do zero técnico

Angular 22.1.4, Material/CDK existentes, SVG/CSS e componentes locais OnPush; não existe biblioteca de gráficos instalada. groupPositions preserva ordem; projectFinancialValues isola magnitude/sinal lossless e razões aproximadas. Ele divide pelo máximo, não pela soma: composição precisa de helper geométrico próprio, não alterar seu significado para os gráficos atuais.

formatFinancialMoney/Percent já reconhecem zero simples/científico e usam BigInt/string para apresentação. moneyPresentation e PositionPerformanceChart marcam todo token com E/e como complete e mostram o token bruto em “Valor completo”; isso explica `0E-12 USD` mesmo com label principal correto. Corrigir apenas apresentação local do Dashboard: zero é reconhecido antes do suplemento, inclusive -0; não mudar parser, DTO ou política financeira de arredondamento.

## Goals / Non-Goals

**Goals:** compreensão rápida das cinco perguntas complementares (custo, resultado, percentual, distribuição, patrimônio registrado), precisão textual, composição desktop consistente com referências A/B e infraestrutura lazy existente.

**Non-Goals:** mudar fórmulas ou reconciliar respostas, criar métricas/requests, ranking/top-N, “Outros” agregado, histórico novo, calibrar escala para exagerar ganhos, modificar shell/rotas, redesenhar detalhes ou introduzir biblioteca.

## Decisions

### Referências visuais e aceite

Referência A é a autoridade conceitual para organização, cards, espaçamento, densidade e protagonismo dos gráficos. Referência B governa tipo adequado, legenda, tooltip e leitura imediata; nomes de terceiros não autorizam copiar identidade. As imagens não estão disponíveis nesta rodada: não inventar cores, proporções ou componentes supostamente vistos. Conservar verde/neutros, tipografia e navegação do Dashboard aprovado. O plano fixa a proposta abaixo para revisão; fidelidade exata às imagens não foi verificada. Se a revisão exigir comparação direta, obter novamente as referências antes de alegar correspondência visual. Não buscar imagens externas substitutas.

### BLOCO 1 — Refinar os gráficos existentes

1. **Custo × Valor atual:** uma barra horizontal sólida mostra valorAtualPosicao; traço vertical com contorno e extensão acima/abaixo da barra marca custoPosicao no mesmo eixo. Dois labels permanentes “Custo” e “Valor atual”, com respectivos valores e legenda visual curta. Pergunta: “Quanto a posição vale hoje em relação ao seu custo?”. A referência é custo das posições abertas, não meta nem total histórico aportado. Escala começa em zero, até máximo de ambas as séries por moeda. Custo maior que valor pode ficar fora da barra, ainda dentro do eixo. Iguais coincidem sem perder traço/labels; diferença pequena continua pequena. Não usar linha temporal, progress track, faixas de meta ou diferença calculada. Alternativas: barras pareadas mantêm o problema relatado; dumbbell pode esconder coincidência dos pontos; área/rosca não expressa adequadamente duas grandezas que não são partes de um todo.
2. **Resultado não realizado:** conservar barra divergente; enfatizar zero, valor e estado, reduzir guias/texto redundante e alinhar labels. Pergunta: “Onde estou ganhando ou perdendo dinheiro?”. A magnitude é resultadoNaoRealizado, sem recálculo. Zero não tem barra mínima.
3. **Rentabilidade por ativo:** conservar dot plot aprovado; melhorar alinhamento/legenda e leitura dos pontos sem nova interação. Pergunta: “Qual o desempenho percentual atual de cada posição?”. Escala simétrica por moeda própria desse campo. Não repetir barras nem transformar em série temporal. Pontos iguais coincidem na escala, não se deslocam artificialmente.
4. **Zeros e precisão:** labels/tooltip/legenda/suplemento usam R$ 0,00, US$ 0,00 ou 0,00% para zeros inclusive -0.00, 0E-12 e 0e9999; token original fica intacto no model. Suplemento exato de não zero usa representação decimal localizada sem E/e quando a expansão for limitada (reutilizar limite existente de expoente até 1000, sem mudar formato global). Para expoentes extremos não expansíveis com segurança, permitir representação exata compacta e explicitamente identificada como valor completo, por exemplo 1 × 10⁴⁰⁰⁰ %, sem fingir zero nem truncar dígitos. Notação técnica desnecessária fica proibida; não proibir matemática necessária para evitar milhares de caracteres. Valores pequenos com label arredondado mantêm estado real e suplemento exato. Considerar custo de expansão com budget de caracteres antes de materializar; transformação é textual, não financeira.

### BLOCO 2 — Novas visualizações financeiras

**Composição por moeda:** nova visualização escolhida: uma rosca BRL e outra USD quando presentes, com legenda completa e valores autoritativos. Pergunta: “Onde está concentrado o valor das minhas posições?”. Centro identifica moeda/finalidade no P0, sem total calculado. Valor central foi avaliado: resumo.patrimonioAtual é autoritativo, mas vem de resposta independente e não há garantia de instante comum com posições; não o apresentar como total das fatias. Um total coerente exigiria garantia contratual futura. Não somar posições para preencher o centro. Não mostrar percentual de participação nesta versão, pois o backend não o fornece. Texto breve pode dizer “Distribuição do valor atual das posições”. Proporção é visual, não nova rentabilidade ou percentual financeiro publicado.

Helper puro local recebe strings positivas por moeda e normaliza magnitudes antes de somar pesos limitados: cada peso relativo ao máximo; cada ângulo é peso / soma dos pesos × volta completa. Essa soma não é custo/patrimônio/total financeiro e não sai da geometria. Não converter strings integrais para Number; reutilizar filosofia de significandos/expoentes limitados. Não usar resumo como denominador de posições que podem ter timestamps diferentes. Labels e legenda usam apenas valores originais formatados. Não persistir proporções, emitir percentuais ou alimentar outra métrica.

Um ativo positivo forma a volta completa. Ativos zero permanecem na legenda, sem fatia artificial; todos zero mostram “Sem valor para distribuir”, sem rosca preenchida. Valores negativos/inválidos não são convertidos em módulo nem omitidos para renormalizar os demais: manter alternativa textual e informar composição indisponível naquele grupo. Fatias minúsculas não ganham ângulo mínimo; valor continua visível com indicação simples se a fatia não for distinguível. Muitos ativos: todos na ordem recebida, sem top-N/Outros/scroll interno obrigatório, nomes/valores quebram naturalmente. Legenda numerada e correspondência visual não exclusivamente cromática; separadores de fatia não devem aumentar/consumir artificialmente ângulos. Sem tab stop por fatia; equivalente textual sempre presente. Cores/padrões complementares dentro da identidade, sem presumir significados de lucro/prejuízo.

Comparações adicionais auditadas: barras de maiores posições repetiriam valor/composição; ranking de resultado/retorno repetiria as duas análises e mudaria ordem; gráficos de realizados repetiriam detalhes. **Não adicionar outro painel só por variedade.** Os comparativos existentes mais a rosca respondem às perguntas prioritárias com uma única visualização nova.

**Histórico do patrimônio:** refinar grid, hierarquia do tooltip e labels temporais dentro do componente existente. Manter linhas retas, pontos observados e gaps; não suavizar nem gerar pontos entre snapshots. Linha clara com pontos discretos em painel amplo é P0. Área suave é P1 opcional, apenas decoração sob cada segmento existente, até a base visual já existente, sem alterar domínio/coordenadas/linha, sem cruzar gaps ou preencher observação isolada. Se sugerir continuidade ou exigir mudar geometria financeira, omitir a área. Não representa volume, integração ou dados entre observações. Comunicar: “Patrimônio registrado nos momentos em que houve um registro”. Preservar geometria/paths e dataset, timestamps/IDs, DD/MM/YYYY, HH:mm local, teclado/toque/Enter/Espaço/Escape, seleção e histórico textual único. Não exigir hover para obter valores, não automatizar captura. Não recalcular coordenadas para “embelezar”.

### BLOCO 3 — Composição final do Dashboard desktop

Ordem semântica: contexto/ações existentes → indicadores por moeda (patrimônio protagonista) → Análise por ativo: composição, custo/valor, resultado, rentabilidade → Histórico do patrimônio → posições → resultados realizados.

Em 1440×900, respeitar shell existente (cerca de 1105 CSS px úteis no ensaio anterior). Primeira faixa analítica: custo/valor ocupa aproximadamente 60% e composição 40%, **somente se** couberem larguras legíveis de aproximadamente 34rem e 22rem mais gap; caso contrário empilhar sem reduzir fonte. Dentro da rosca, moedas empilham na faixa estreita; comparativo usa grupos internos conforme espaço. Segunda faixa: resultado/rentabilidade em duas colunas equivalentes, como a base aprovada. Histórico merece largura total com BRL/USD lado a lado quando couberem. Não forçar todos os gráficos na primeira dobra nem fixar alturas iguais com grandes vazios; alinhar topos e espaçamentos, deixar o conteúdo determinar altura. Dataset integral pode alongar a página legitimamente.

Painéis brancos, bordas suaves, tipografia tabular e valores fortes. Um container por pergunta ou grupo quando necessário, sem card por ativo nem nova faixa de KPIs. Reduzir subtítulos redundantes, conservar explicação essencial do histórico manual e da composição. Leitura lógica igual no DOM e no desktop/mobile, sem ordenação CSS que contradiga leitor de tela. Posições/resultados ficam completos abaixo. Nenhum seletor local, filtros/rotas novos, ocultação de tabelas ou modificação do contexto.

### Reutilização e arquivos previstos

- Reutilizar PositionAnalysisComponent para composição das faixas; CostValueChartComponent para barra/referência; PositionPerformanceChartComponent para resultado/dot plot, sem duplicação gratuita.
- Reutilizar groupPositions, projectFinancialValues, tokens e formatFinancialMoney/Percent. Criar somente helper geométrico de composição e componente local de rosca/legenda; helper local de texto exato apenas se a apresentação existente não bastar.
- Arquivos prováveis: features/dashboard/position-analysis/{cost-value-chart.component.*,position-chart.scss,position-chart-presentation.ts,position-performance-chart.component.*,position-analysis.component.*,position-analysis.fixtures.ts}; novos portfolio-composition.component.* e composition-geometry.* no mesmo limite lazy; dashboard-page.component.html/.scss/.spec.ts; evolution/portfolio-evolution.component.html/.scss/.spec.ts, TS somente para apresentação de tooltip se necessário.
- Não alterar evolution-geometry.ts, services/models, HTTP/parser, global formatter, shell, backend ou dependências. SVG/CSS/Angular satisfazem os casos; biblioteca exigiria custo/bundle e abstração sem benefício demonstrado. Nenhuma instalação.

### Performance e validação

Baseline válido do commit a0429c8, evidencias-encerramento.md no archive 2026-09-09-evoluir-dashboard-financeiro-frontend e stats existentes; não executar build só para planejamento:

| Medida | Bytes |
| --- | ---: |
| Initial | 514417 |
| Dashboard lazy | 52387 |
| CSS Dashboard | 2522 |
| CSS evolução | 4466 |
| CSS comparativo | 1747 |
| CSS painel resultado/dot plot | 2244 |
| CSS contêiner da análise | 178 |

Warnings conhecidos: initial +14417 B sobre 500 kB; evolução +466 B sobre 4 kB. Budgets existentes 500 kB/1 MB initial e 4/8 kB por estilo; não elevar. Preservar critério canônico initial ≤514418 B, comparando também com baseline atual; investigar qualquer crescimento, sem waiver herdado. Medir lazy/CSS e justificar composição/helper, sem duplicar CSS embutido na soma. Código dos gráficos permanece lazy; não otimizar por iniciativa própria. Validações históricas 128/128 focados, 461/461 completos e production aprovado são baseline, não evidência de implementação futura.

Rodadas de 2–3 tarefas, com testes focados ao alterar código e suíte/build ao fechar cada bloco (VITEST_MAX_WORKERS=2); não repetir resultados se nada correspondente mudou. Cada bloco termina com evidência desktop e aceite humano; não avançar automaticamente. Validar vazio, um, vários, 100 ativos, nomes/tickers longos dentro do contrato, BRL/USD isolados/juntos, igualdade, quase igualdade, sinais, zeros científicos, valores extremos e imutabilidade. Composição: ângulos finitos/soma de volta somente geométrica, proporções, zeros/negativos/invalidade/subpixel e quantidade integral. HTTP: exatamente os quatro GETs, zero chamada do gráfico, nenhum POST automático; contexto concorrente e histórico independente. Final: Edge real 1440×900, 768, 390, 320, 959/960/961, 320+200%, zoom 400%, baixa altura, teclado/leitor de tela/reduced motion; não usar JSDOM como prova geométrica. Separar execução técnica de aceite estético. Captura inicial desta nova change pode ser feita antes do primeiro código, sem reconstruir a captura ausente da change anterior.

## Risks / Trade-offs

- Marca de custo pode ser entendida como meta → label “Custo”, legenda e aceite visual; não desenhar faixas de objetivo. Igualdade deve preservar referência sem deslocamento.
- Rosca com muitos ativos perde discriminação → legenda integral, números/estilos e valores exatos; sem fatias mínimas/Outros. Não inventar cores infinitamente distinguíveis; reconhecer limite visual.
- Escalas de moedas podem parecer comparáveis → títulos BRL/USD e painéis claramente separados, nenhuma legenda ou eixo monetário comum.
- Texto exato pode alongar cards → layout com wrapping e suplemento preciso apenas quando necessário; limite de expansão para extremos, sem truncar dado.
- Referências visuais indisponíveis → planejar pelos princípios explicitados, exigir revisão do usuário antes de implementar; não declarar fidelidade às imagens não vistas.
- Novas proporções confundidas com métrica → nenhum percentual textual ou total derivado; geometria local sem exportação financeira.
- Alterações de layout prejudicarem leitura assistiva → preservar DOM, foco, títulos e histórico; não remover testes existentes.

## Migration Plan

Sem migração de contratos/dados. Implementar apenas após aprovação do planejamento, em blocos explícitos e rodadas pequenas conforme tasks.md. Reversão futura limitada aos componentes/templates/estilos locais, sem mudar snapshots, fórmulas ou parser. Versionamento/archive não autorizados nesta rodada.

## Revisão visual e auditoria temporal — 09/09/2026

Esta revisão explicita que modernização exige variedade real de famílias gráficas, não somente CSS. Seguir pergunta → campo autoritativo → disponibilidade/temporalidade → tipo. A referência A continua governando a composição e a B a leitura; imagens originais continuam indisponíveis nesta sessão. Nenhuma implementação ou nova autorização de backend decorre desta auditoria.

### Evidência técnica e classificação temporal

Auditoria estática de entidades, migrations 004/005/006, CarteiraResource, PosicaoService, ResultadoRealizadoService, CalculadoraPosicao, SnapshotCarteiraService, models e services Angular. Operacao persiste quantidade/preço/valorTotal BigDecimal, data civil e ordemNoDia por ativo; não persiste lucro por venda. ResultadoRealizadoService faz replay cronológico e retorna somente acumulado por ativo. CalculadoraPosicao mantém custo e resultado realizado durante o replay, mas não expõe trajetória. HistoricoCotacao persiste observações por ativo/instante, sem garantia de cobertura ou alinhamento temporal com operações. SnapshotCarteira e SnapshotCarteiraMoeda guardam ID, instante e patrimônio por moeda, sem posições/custo/resultado. Não houve consulta ao banco real: existência de tabelas não comprova cobertura histórica.

A = série autoritativa existente; B = dados históricos permitem derivação correta; C = entrega exige backend/endpoint/cálculo; D = interpretação incorreta com dados atuais. B e C descrevem eixos diferentes: reconstrução conceitualmente possível não autoriza cálculo frontend.

| Possibilidade | Classe | Fonte/limitação e significado correto | Entrega |
| --- | --- | --- | --- |
| 1. Patrimônio temporal | A | evolucao-patrimonial, patrimonioAtual por snapshot/moeda; somente valores registrados, não retorno | P0, linha |
| 2. Não realizado temporal | C; D se diferença entre snapshots | Falta custo e avaliação das posições no mesmo instante; quote atual não serve retroativamente | P2 |
| 3. Realizado temporal | B nos dados, C no contrato | Operações ordenadas + replay oficial podem produzir resultado por venda/data ou acumulado; resposta atual só entrega acumulado final | P2, candidato futuro mais delimitado |
| 4. Rentabilidade da carteira temporal | C; D se patrimônio normalizado | Faltam observações do indicador e, para retorno de investimento ajustado a fluxos, fluxos externos/avaliações e definição financeira aprovada | P2 |
| 5. Rentabilidade por ativo temporal | C | Campo atual sem série; exige custos/quantidades e avaliações compatíveis por instante ou observações futuras do campo oficial | P2 |
| 6. Custo da carteira temporal | B nos dados, C no contrato | Replay oficial de operações pode produzir custo remanescente por data e moeda; não é soma de compras nem aporte líquido | P2 |
| 7. Valor histórico das posições | Atual disponível; C para histórico | Quantidade/custo reconstruíveis, mas falta série autoritativa de avaliação por posição; observações locais de preço não garantem cobertura nem associação intradiária | P2 histórico; P0 atual nos comparativos |
| 8. Evolução BRL | A | Componente BRL dos snapshots, com ausências preservadas | P0, eixo monetário próprio |
| 9. Evolução USD | A | Componente USD dos snapshots, com ausências preservadas | P0, eixo monetário próprio |

BRL/USD podem compartilhar contexto temporal e painel amplo, mas ficam em subgráficos monetários independentes. Não usar eixo monetário comum, eixo duplo que induza equivalência, base 100 ou percentual de diferença patrimonial apresentado como retorno. Uma comparação temporal percentual futura seria possível somente com séries de mesma definição financeira aprovadas por moeda; nenhuma existe hoje.

### Evoluções futuras, sem tasks de implementação nesta change

- Realizado por data: reutilizar replay BigDecimal oficial com saídas por venda ou fim de dia; novo contrato/endpoint e testes de ordem, venda parcial/total e precisão. Persistência adicional não é necessariamente necessária. Impacto moderado e delimitável; é o primeiro candidato se houver autorização separada antes da entrega. Não inventar horário intradiário a partir de LocalDate/ordem por ativo.
- Custo por data: mesma origem, agregação oficial independente por moeda e contrato temporal. Impacto moderado, sem nova fórmula, mas requer validação de reconstrução e desempenho; preferir pontos de eventos/datas explícitos, não amostras fictícias.
- Não realizado e valor histórico: snapshots futuros dos valores autoritativos/custo/posições ou avaliações históricas com política de instante e cobertura. Novo endpoint e possivelmente persistência/coleta; impacto maior. Não preencher passado com cotação atual.
- Rentabilidade: armazenar futuramente observações do indicador atual produziria histórico do resultado percentual das posições abertas naquele momento, afetado por mudanças de composição; não equivale a retorno total da carteira ajustado a fluxos. Este último exige definição aprovada (incluindo fluxos externos e avaliações), dados e cálculo backend próprios. Impacto alto; não recomendar inclusão nesta semana sem escopo separado. Snapshots novos não recuperam passado inexistente.

### Prioridades e variedade verificável

**P0 / MUST HAVE:** zeros amigáveis; barra/referência de custo sem trilho de progresso; resultado divergente com origem central; dot plot percentual sem preenchimento de barras; rosca de composição por moeda; linha patrimonial ampla com pontos discretos; indicadores e composição desktop final. Não considerar concluído mediante alterações apenas de cor/borda/espaçamento. A revisão humana deve distinguir silhuetas, perguntas e unidades sem tooltip.

**P1 / NICE TO HAVE:** área decorativa segmentada do histórico, somente se não comprometer clareza e prazo; não bloqueia aceite da linha P0 e requer registro de adoção ou adiamento, sem aprovação presumida. Nenhuma biblioteca necessária.

**P2 / evolução futura:** séries de realizado, custo, valor, não realizado e rentabilidade descritas acima; percentual textual de composição/total central somente após avaliação semântica e contratual separada. Sem tasks funcionais P2 nesta change.

Rosca versus pizza: ambas codificam composição por ângulo e perdem precisão comparativa com muitas fatias. A rosca é escolhida por preferência explícita e espaço central para identificar moeda/finalidade, favorecendo integração ao painel; não se alega maior precisão. Pizza não oferece ganho suficiente para contrariar essa direção. Valores exatos e legenda integral compensam limites de ângulos; nenhuma posição desaparece. Percentuais derivados não serão exibidos.

### Wireframe desktop final P0

```text
[ Contexto da carteira e ações existentes                       ]
[ Indicadores BRL                   | Indicadores USD            ]
[ Patrimônio em destaque por moeda; sem total combinado          ]
Análise por ativo
[ Composição — ~40%                 | Custo × Valor atual — ~60%  ]
[ ROSCAS BRL / USD                  | BARRA + MARCA DE CUSTO      ]
[ legendas integrais                | valores autoritativos      ]
[ Resultado não realizado           | Rentabilidade por ativo    ]
[ BARRAS DIVERGENTES                 | DOT PLOT percentual        ]
[ Histórico do patrimônio — painel amplo                        ]
[ LINHA BRL                         | LINHA USD                  ]
[ Registros manuais, datas, tooltip; histórico textual único     ]
[ Posições abertas                                              ]
[ Resultados realizados                                         ]
```

A revisão autorizada do Bloco 3 coloca composição antes de custo, conforme direção mais recente do usuário; a presença da rosca na primeira faixa é essencial, não opcional. Larguras adaptam à área útil, sem min-width que provoque overflow. Gráficos circulares, pontos, linha e barras devem permanecer visualmente distintos no conjunto, com espaço horizontal efetivo para o histórico. Não substituir essa diversidade por painéis de barras uniformizados.

## Revisão humana do Bloco 1 e autorização do Bloco 2

O usuário não aprovou integralmente o resultado visual do Bloco 1: diferenciação percebida insuficiente entre barra/referência, divergente e dot plot. Task 1.8 permanece aberta, candidata a refinamento posterior. As evidências técnicas não demonstram alcance do objetivo visual da change. Por instrução explícita, preservar implementação/testes do Bloco 1 e avançar somente às tasks existentes 2.1–2.7; 2.8 depende de nova revisão humana. Esta autorização substitui apenas a dependência de aceite prévio do Bloco 1, sem convertê-lo em aprovado.

## Revisão humana posterior — conjunto aceito

Usuário aprovou explicitamente Custo × Valor atual (leitura com dados variados/PETR4), Resultado divergente e Rentabilidade dot plot no conjunto. Task 1.8 concluída por este aceite posterior, preservando o registro da rejeição anterior. Tipo donut aprovado; task 2.8 continua aberta exclusivamente quanto ao Histórico. Não realizar novos refinamentos nos três comparativos nem enriquecer donut com percentuais/totais derivados.

## Composição do Bloco 3 autorizada

Histórico aprovado humanamente, inclusive diagnóstico de valores constantes e seleção de registros próximos: task 2.8 concluída. Preservar todos os gráficos. Ordem refinada pelo usuário: contexto/ações, indicadores por moeda, composição à esquerda (~40%) e custo à direita (~60%), resultado/rentabilidade em pares, histórico amplo, posições e resultados. Esta ordem prevalece sobre o wireframe inicial custo/composição acima. Em desktop, indicadores por moeda lado a lado com patrimônio destacado e três indicadores secundários; contexto e ações compartilham faixa. Ajustes internos mínimos de containers/legendas acomodam painéis, sem mudar geometria ou dados. Bloco 3 exige revisão humana própria.
