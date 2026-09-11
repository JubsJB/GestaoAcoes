## Context

Motivação e limites em proposal.md. Auditoria realizada no checkpoint b30d8b6, branch feature/frontend-angular, inicialmente limpa e sincronizada com origin conforme git status (sem fetch nesta rodada). AGENTS.md mantém finanças no domínio/backend; PRD RF17/RF20–23 e seções 17–18 permitem visualizações que esclareçam posições sem poluir o Dashboard. As specs específicas vedam novas métricas e rotas fictícias de posição.

### Contratos encontrados

`frontend/src/app/features/dashboard/models/dashboard.ts`, interface `PosicaoResponse`:

| Campos | Tipo atual / uso |
| --- | --- |
| acaoId | number; identidade estável de linha |
| ticker, nomeEmpresa | string; identificação visível |
| mercado | Mercado (BRASIL/EUA) |
| moeda | Currency (BRL/USD); agrupamento pelo campo recebido |
| quantidadeAtual, precoMedio | string; disponíveis, não usados para recalcular barras |
| custoPosicao, valorAtualPosicao | string; comparação de duas séries |
| cotacaoAtual | string; permanece na seção de posições |
| dataHoraCotacao | string; referência temporal existente, não timestamp comum inventado |
| resultadoNaoRealizado | string; divergência monetária |
| rentabilidadePercentual | string; divergência percentual |

DashboardService usa `responseType: 'text'`, `parseLosslessJson`, DECIMAL_FIELDS e guards que aceitam decimal simples/científico. Obtém `/carteiras/{id}/resumo`, `/posicoes` e `/resultados-realizados`. DashboardPageComponent usa forkJoin dessas três fontes, cancela contexto anterior via switchMap e compartilha `data().posicoes`. O componente de evolução consulta `/evolucao-patrimonial` separadamente; POST `/snapshots` continua manual. Não criar subscriptions, GETs por gráfico/ativo, refresh próprio ou estado duplicado. Uma falha nas três fontes hoje falha o bloco financeiro conjunto: esta change não redesenha esse comportamento.

`ResumoCarteiraResponse` contém carteiraId e resumos por moeda (custoTotalPosicoes, patrimonioAtual, resultadoNaoRealizadoTotal, rentabilidadePercentual). `ResultadoRealizadoResponse` contém identificação/moeda e resultadoRealizado. Nenhum desses dados é necessário para derivar barras de posições. Contrato backend em `portfolio-position` já fornece os quatro campos financeiros usados; sem dependência de operação individual, nome de corretora ou dado novo.

Classificação: todos os gráficos são **A quanto à disponibilidade dos contratos / B quanto ao trabalho necessário de geometria/apresentação**. Nenhum C (dado novo) ou D (regra financeira nova).

## Goals / Non-Goals

**Goals:** componentes de apresentação determinísticos, mesma coleção como fonte, fronteira explícita entre valor financeiro e coordenada, semântica textual suficiente sem interação, integração lazy e regressão verificável.

**Non-Goals:** modificar evolution-geometry.ts, parser/model/service, formatadores globais, shell/contexto, tabela de posições ou seu detalhe em Carteira; introduzir framework de gráficos, gráficos de mercado, cálculo financeiro ou abstração genérica antecipada.

## Decisions

### Componentes locais à feature

Criar, durante implementação autorizada, diretório `features/dashboard/position-analysis/` com `PositionAnalysisComponent` recebendo `readonly PosicaoResponse[]`, componentes pequenos `CostValueChartComponent`, `UnrealizedResultChartComponent` e `ReturnChartComponent`, helper puro `position-chart-geometry.ts` e estilos locais compartilhados somente onde houver repetição demonstrada. Componentes OnPush/computed com track por moeda/acaoId; não injetam HttpClient ou serviços financeiros. Não importar esses componentes no shell/shared usado pelo initial. Reutilizar tokens, formatFinancialMoney, formatFinancialPercent e financialOutcomeLabel.

Alternativas: três componentes com estado HTTP próprio duplicariam consultas; biblioteca externa contrariaria escopo; reutilizar o componente de evolução acoplaria snapshots/tempo e barras estáticas. Compartilhar o princípio geométrico e funções locais pequenas, não a implementação temporal.

### Fonte autoritativa e geometria extrema

Manter as strings originais por referência ou campos readonly. Labels monetários/percentuais usam formatadores existentes sem Number/parseFloat. Sinal e zero são reconhecidos textualmente, inclusive -0 e 0eN, antes da aproximação. Nunca calcular custo = quantidade × preço, resultado = valor − custo ou percentual = resultado/custo.

`evolution-geometry.ts` mantém authoritativeValue separado de x/y, mas converte diretamente com Number e ignora valores não finitos. Reutilizar somente essa separação; não copiar omissão de linhas, overflow ou underflow para gráficos novos.

O novo helper deve normalizar magnitude a partir de significando decimal e expoente antes de produzir razões limitadas [0,1]. Comparação textual de magnitude, ajuste de expoente e divisão de significandos servem apenas à escala de pixels. Evitar expandir potências gigantes, arrays proporcionais ao expoente ou Number aplicado ao valor financeiro integral; Number só para razão/coordenada limitada. Decimais científicos como 1e400/1e-400 não podem apagar linhas. Se razão subpixel/underflow não produzir barra discernível, preservar texto e identificar valor não nulo abaixo da resolução visual, sem largura mínima artificial. Valores inválidos não viram zero: preservar diagnóstico/fallback textual, sem corrigir contratos.

Formatadores existentes arredondam dinheiro/percentual para duas casas. Para valor pequeno não nulo que apareça como 0,00 e para precisão adicional, disponibilizar texto lossless de origem rotulado como valor completo, na própria linha, com unidade; preferir apresentação textual simples, sem alterar formatadores globais. Nenhum tooltip, clique ou Tab pode ser necessário para acessar esse texto. Essa informação pode usar notação recebida para expoentes extensos, evitando expansão excessiva. Não confundir o arredondamento visual com igualdade financeira ou neutralidade.

### Escalas e séries

Todos os gráficos agrupam BRL e USD separadamente; grupos ausentes não recebem placeholders monetários. Cada grupo monetário tem escala própria, identificada como tal. Rentabilidade também fica em grupos identificados por moeda para consistência, mas usa **uma escala percentual comum entre ambos os grupos**, sem mistura com dinheiro ou agregação de percentuais.

Custo × Valor atual: duas barras horizontais paralelas por ativo, com rótulos textuais Custo/Valor atual e tratamentos distinguíveis sem cor. Zero na origem; domínio compartilhado entre ambas as séries de todos os ativos da mesma moeda, até máximo observado. Sem escala individual por linha nem barras empilhadas. Igualdade gera duas barras iguais e distinguíveis, sem fabricar diferença.

Resultado e rentabilidade: eixo central de zero, negativos à esquerda e positivos à direita; domínio simétrico pela maior magnitude do respectivo grupo monetário ou do conjunto percentual. Magnitudes iguais têm comprimentos iguais. Zero mantém a linha e texto Neutro, sem barra financeira mínima. Todos-zero usa eixo neutro sem ticks monetários/percentuais inventados; único ativo não sugere tendência temporal. Rótulos Positivo/Negativo/Neutro e unidade sempre presentes.

### Hierarquia, densidade e estados

Ordem proposta: contexto → ações → indicadores → Análise por ativo (Custo × Valor atual, Resultado não realizado, Rentabilidade) → Histórico do patrimônio → Posições abertas → Resultados realizados. A ordem sugerida pelo usuário favorece interpretação antes dos detalhes, preservando ordem relativa das seções antigas. A tabela completa continua útil para quantidade/preço/cotação/data; não duplicar esses detalhes em cada gráfico. Sem tabs, filtros, sorting, top-N, truncamento, paginação ou colapsos automáticos nesta proposta.

Gráficos em faixas verticais, cada qual com pares de moedas lado a lado somente quando couberem; evitar três painéis densos lado a lado. Altura acompanha número de ativos, sem comprimir barras até ilegibilidade, sem scroll interno obrigatório; crescimento O(n), todas as posições preservadas na ordem recebida dentro de cada grupo. Fixtures de muitos ativos: 100 itens, além de 0/1/3. Sem Carteira não montar análise; posição vazia mostra um único estado vazio local. Loading/erro financeiro não mostra análise antiga. A evolução permanece montada independentemente do sucesso/erro das três fontes, preservando seleção de ponto, gaps, snapshots vazios, timestamps e todas as interações aprovadas.

### Acessibilidade e responsividade

Uma lista semântica por gráfico com ticker, empresa, moeda, rótulo da série e valores visíveis; SVG decorativo aria-hidden/focusable=false, sem tabindex, eventos ou tooltip obrigatório. A lista fornece equivalente textual completo; evitar árvore textual duplicada desktop/mobile. O gráfico não adiciona tab stops. Headings h2 da análise, h3 para cada gráfico e agrupamento de moeda associado. Não usar cor como único significado: rótulos/sinais e contorno/preenchimento diferenciam séries.

Texto em HTML fora do viewBox para não encolher fonte ao reduzir SVG; quebras naturais de nomes/valores e largura mínima zero nos contêineres. Empilhar grupos pela largura disponível (grid auto-fit/minmax), não pelo dispositivo. Conferir 1440x900, 768x1024, 390x844, 320 CSS px, 959/960/961, texto 200%, zoom/reflow 400% quando aplicável e 1280x600. Sem animação de entrada/contagem; respeitar reduced motion existente. Medir contraste textual 4,5:1 normal, 3:1 grande e 3:1 para marcas necessárias; aprovação renderizada/leitor de tela complementa testes DOM.

### Performance e testes

Baseline reutilizado do build do checkpoint, sem novo build de planejamento: initial 514418 bytes (transfer 122,08 kB), Dashboard lazy ~37160 bytes, CSS evolução 4284 bytes; baseline histórico 506086 bytes não é meta retroativa desta change. Logs/evidência em archive/2026-09-08-refinar-experiencia-visual-frontend/evidencias-fase-7.md. Suite anterior 412/412; focados 247/247. Warnings: initial >500 kB, evolução >4 kB em 284 bytes. Budgets permanecem iguais.

Aceite futuro: initial não superior ao checkpoint 514418 bytes na mesma configuração/ambiente; crescimento lazy deve ser medido e explicado por componentes/estilos, sem limite novo inventado ou biblioteca. Se o initial crescer, investigar imports/promoção ao shell; qualquer exceção exige decisão humana, não herda a dispensa anterior. Não perseguir baseline histórico com refatoração estrutural fora do escopo. Compilar estilos compartilhados com cuidado para não duplicá-los em cada linha.

Unitários geométricos: razões finitas/limitadas, origem zero, simetria, igualdade, extremos e strings imutáveis, diferença pequena em grande magnitude, notação científica. Componentes: 0/1/3/100 posições, todas as moedas, sinais, valores textuais e ausência de tab stops. Fixtures deliberadamente não deriváveis entre si provam que resultado/percentual vêm do campo recebido, não de recálculo. Integração HttpTestingController: mesmos quatro GETs financeiros por carga/reload, zero HTTP de gráficos, troca concorrente e nenhum POST automático. Reutilizar dashboard.service.spec, dashboard-page.component.spec, dashboard.routes.spec, evolution-geometry.spec, portfolio-evolution.component.spec, testes lossless/formatadores/posições e contexto. Não enfraquecer assertions: atualizar somente ordem intencional de headings.

## Risks / Trade-offs

- Magnitudes muito diferentes tornam barras pequenas imperceptíveis → texto completo e indicação de limite visual, sem falsificar área/comprimento ou mudar para log silenciosamente.
- Três leituras dos mesmos ativos alongam a página → layout vertical simples, dados mínimos por linha e revisão humana com 100 ativos; sem amputar dataset.
- Precisão textual pode exceder duas casas → manter formato usual e representação lossless adicional quando necessária, sem novo arredondamento financeiro.
- Norma canônica geral de precisão veda Number → delta explícito limita exceção a coordenadas, mantendo todos os cenários de precisão; proibição financeira permanece.
- Referência antiga “Registrar snapshot” em requisito de reload contrasta com label atual aprovado → usar comportamento canônico específico de evolução; não renomear contratos nem reabrir archive nesta change.

## Migration Plan

Sem migração de dados/contratos. Implementar fases pequenas após aprovação, integrar por inputs e validar; eventual reversão futura restringe-se aos novos componentes/imports/markup de análise sem tocar histórico. Esta rodada não autoriza implementação, commit, deploy ou archive.

### Autorização incremental posterior ao planejamento — 08/09/2026

O usuário aprovou hierarquia, escala percentual comum com grupos identificados, fronteira lossless/geometria e ausência de interação. Autorizou nesta rodada somente auditoria, fundação e Custo × Valor atual, incluindo sua integração entre indicadores e histórico. Resultado não realizado e Rentabilidade não são implementados nem recebem placeholders. A restrição de implementação acima registra a rodada original de planejamento; a autorização posterior não altera os requisitos finais dos deltas.

Fundação entregue: `PositionAnalysisComponent`, `CostValueChartComponent`, `position-chart-geometry.ts` e `position-chart-presentation.ts`, locais ao Dashboard lazy. A partição por moeda preserva primeira ocorrência dos grupos e ordem dos ativos. A projeção usa magnitude decimal normalizada, expoente bigint e significando limitado a 16 dígitos somente para razão geométrica; strings e sinal continuam exatos. Contorno do custo é recortado pelo viewport da própria barra para não ultrapassar sua largura, inclusive quando muito pequena. Texto lossless suplementar aparece para notação científica ou precisão relevante além de centavos. O aviso de barra possivelmente imperceptível usa razão inferior a 0,005 como indicação conservadora; não mede pixels nem modifica a largura. Validação manual renderizada permanece pendente; detalhes e resultados em evidencias-fases-1-3.md.

### Direção vigente do Bloco 1 — 09/09/2026

A versão anterior de Custo × Valor atual não foi aprovada visualmente. A autorização atual substitui o refinamento isolado e permite implementar as três etapas juntas. Evidências anteriores são históricas e não representam aceite visual.

- Dashboard: contexto e ações compactos, cards de borda suave, patrimônio com maior peso, seções delimitadas por espaço e títulos. Mesma ordem semântica, shell e demais telas.
- Comparativo: pares horizontais com guias discretas, custo em contorno e valor atual sólido, valores completos em HTML. Reutilizar helper e razões, zero comum e máximo por moeda; sem trilhos de progress bar. Descrição simples e títulos BRL/USD, sem explicações matemáticas na interface.
- Histórico: grid decorativo e linha mais legível, sem área, suavização ou coordenadas novas. Duas colunas quando legíveis, empilhamento compacto. Preservar tooltip, gaps, pontos, vazios, histórico textual único, DD/MM/YYYY, HH:mm local, teclado/toque/Escape e ação manual. Não modificar evolution-geometry.ts ou TypeScript do histórico.

Variação: PosicaoResponse.resultadoNaoRealizado tem semântica autoritativa de valorAtualPosicao menos custoPosicao (portfolio-position/PRD 12.6). Não calcular diferença nem acrescentar o rótulo genérico Variação; apresentar somente as duas séries neste bloco, preservando resultado nos indicadores/posições.

Arquivos adicionais autorizados: templates/estilos Dashboard e evolução, testes e artefatos desta change. Sem alterações de finanças, backend, contratos, dependências ou budgets. Testes/build por bloco não encerram os gráficos posteriores. Revisão humana obrigatoriamente pendente.

### Correção responsiva autorizada — 09/09/2026

Após o Bloco 1, o usuário autorizou corrigir somente reflow e scroll horizontal no shell. A restrição anterior de não tocar shell foi substituída para essas correções estruturais; conteúdo, contexto, navegação e finanças permanecem intactos.

Causas medidas no navegador: coluna implícita auto do grid adotava 497,5 px mínimos do cabeçalho flex sem quebra em viewport 320 + fonte 200%; drawer 16rem atingia 512 px. Na navegação desktop, links Material de width:100% somados às margens de 12 px excediam o container de 255 px (scrollWidth 267). Ao restringir o shell, palavras longas em descrições também precisaram de wrapping; o ícone do cabeçalho deixava coluna textual excessivamente estreita.

Correção limitada ao SCSS do shell e cabeçalho compartilhado: coluna minmax(0,1fr), seletor em segunda linha no compacto, drawer limitado à largura disponível, links com width:auto, wrapping textual e cabeçalho em uma coluna apenas quando sua largura útil for pequena. Sem novo overflow-x:hidden, fonte menor, mudanças de gráficos, dataset ou comportamento. Teste de integração preserva seletor e labels ao abrir drawer; geometria exige ensaio em Edge real. Evidências finais serão acrescentadas sem apagar o diagnóstico anterior. Revisão humana continua pendente.


## Direção vigente — Bloco 2 (09/09/2026)

O usuário aprovou visualmente o desktop do Bloco 1 e autorizou Resultado não realizado e Rentabilidade por ativo. Desktop é prioridade; revisão mobile detalhada adiada deliberadamente, mantendo verificação de regressões graves. Custo × Valor atual, indicadores, histórico e demais seções não serão redesenhados. A eventual reconsideração do comparativo ocorrerá após revisão conjunta dos três gráficos.

Dois painéis brancos lado a lado quando houver espaço após Custo × Valor atual, cada um com grupos BRL/USD internos e barras divergentes estáticas. Fonte exclusiva: resultadoNaoRealizado e rentabilidadePercentual recebidos. Zero central, escala simétrica própria por gráfico e por moeda, labels/valores/sinais/estado sempre visíveis e equivalência textual sem foco novo. A instrução atual substitui a decisão anterior de escala percentual comum entre moedas: nesta rodada também a rentabilidade usa escala independente por moeda. Sem regra financeira nova ou HTTP adicional.

Implementação: um componente local PositionPerformanceChartComponent parametrizado por métrica, pois os dois painéis compartilham a mesma estrutura divergente. Input readonly das posições; helper geométrico existente preserva strings/sinal e gera somente razões. Formatação lossless existente e texto completo adicional para precisão/extremos; nenhuma coordenada alimenta valores financeiros. Sem services próprios.


## Revisão do Bloco 2 — dot plot (09/09/2026)

Diagnóstico aprovado; opção C adotada. Patrimônio histórico não representa rentabilidade: compras, vendas e composição alteram seu valor. Não implementar rentabilidade temporal, reconstrução, persistência, endpoint, snapshot ou fórmula nova nesta change.
Somente Rentabilidade por ativo passa de barras para dot plot estático com eixo percentual divergente, zero central e marcador proporcional. Ticker, empresa, percentual autoritativo e Positivo/Negativo/Neutro permanecem visíveis. BRL/USD mantêm grupos e escalas simétricas independentes pela maior magnitude recebida, sem corte de extremos, FX ou retorno global. Zero e -0 ficam no centro; valores subpixel mantêm texto e aviso, sem deslocamento mínimo artificial. O tamanho do ponto identifica a observação, não uma magnitude financeira.
Resultado não realizado, Custo × Valor atual e Histórico do patrimônio permanecem intactos. Desktop prioritário; revisão mobile detalhada adiada. Revisão humana do Bloco 2 permanece pendente. Esta decisão substitui apenas a apresentação anterior da rentabilidade; registros anteriores são histórico das decisões.

## Estado após aprovação humana desktop do Bloco 2

Composição e três visualizações aprovadas explicitamente pelo usuário no desktop, incluindo grupos BRL/USD, zero central, estados, percentuais e dot plot. Preservar essa implementação. Os registros anteriores de aceite pendente foram superados apenas nesse âmbito; validação final mobile/texto ampliado/leitor de tela permanece adiada.

Sem implementação agora: considerar na revisão final a leitura de custo/valor muito próximos e a apresentação de notação técnica como `0E-12 USD`, sem perda de precisão. Não existe tarefa adicional de implementação definida como Bloco 3; a inspeção e o ponto de parada estão documentados em evidencias-bloco3.md.

## Auditoria para encerramento

O objetivo vigente é concluir somente a change existente. Sem novo trabalho funcional ou otimização de bundle. A aprovação humana cobre desktop e entregas anteriores; os quatro candidatos futuros (comparativo próximo, notação técnica, mobile profundo e novos gráficos) não bloqueiam esta change. Evidências reutilizadas e classificação das seis tasks anteriormente abertas estão em evidencias-encerramento.md. Fechadas 7.3, 7.5 e 9.7; mantidas 1.3, 7.2 e 7.4 por ausência de evidência específica, sem alterar requisitos para simular conclusão. Nenhum teste/build repetido por esta atualização documental.

## Encerramento final aprovado

O usuário aprovou a revisão humana final das tasks 7.2 e 7.4 e aceitou explicitamente a ausência da captura anterior de 1.3 como limitação documental. A captura não foi realizada e não existe como evidência. Estado final: 45/45 tasks encerradas (uma com dispensa parcial aceita), zero pendências bloqueantes; classificação A, pronta para archive, ainda não arquivada. Detalhes em evidencias-encerramento.md. Registros anteriores de pendência foram superados; testes/build, bundles e warnings reutilizados sem alteração de código.
