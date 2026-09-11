## Why

Os indicadores e posições atuais apresentam os valores da carteira, mas não facilitam a comparação visual entre ativos. Após o refinamento visual aprovado, três visualizações podem melhorar essa leitura usando exclusivamente campos financeiros já retornados, sem transformar registros patrimoniais manuais em histórico de mercado.

## What Changes

- Adicionar Análise por ativo: barras horizontais pareadas de Custo × Valor atual, barras divergentes de Resultado não realizado e dot plot percentual de Rentabilidade.
- Preservar valores textuais, identificação, ordem recebida e todas as posições; separar BRL/USD, inclusive em grupos percentuais, sem FX ou agregações.
- Integrar a análise entre indicadores e Histórico do patrimônio; preservar posições completas e resultados realizados abaixo, além de contexto, ações, loading, erros e navegação atuais.
- Manter gráficos comparativos não interativos, com equivalente textual sempre visível, sem tooltip obrigatório nem novos tab stops.
- Delimitar aproximação exclusivamente geométrica, sem alteração das strings lossless, formatadores financeiros ou regras de negócio.

Fora do escopo: alocação/pizza/rosca, composição, diversificação, setores, benchmark, dividendos, preço histórico/intraday, rentabilidade histórica, novas métricas, totalizações, FX, endpoints, DTOs, migrations, fórmulas, resultado realizado, mecanismo de snapshot, bibliotecas de gráficos e aumento de budgets. A análise não cria rota de detalhe de posição nem atualiza cotações. Histórico do patrimônio continua sendo registros feitos manualmente ao longo do tempo; não será substituído.

## Capabilities

### New Capabilities
- `frontend-position-analysis`: comparação visual acessível dos valores autoritativos das posições abertas, com três gráficos, escala geométrica e moedas independentes.

### Modified Capabilities
- `frontend-dashboard-management`: integrar a análise à hierarquia e explicitar a exceção de aproximação exclusivamente geométrica, preservando todos os cenários existentes de precisão e ordem relativa das seções atuais.

## Impact

Frontend Angular, limitado à feature lazy do Dashboard e testes correspondentes. Sem mudança de backend, contratos HTTP, parsing, payloads, dependências, contexto global ou regras financeiras. Os gráficos recebem a coleção já carregada por DashboardService, sem consultas próprias.

Base de produto: PRD RF17/RF20–RF23, seções 17–18, RNF07/RNF08/RNF10; specs canônicas de Dashboard, posições, carteira, operações e evolução. A restrição a novos gráficos na change visual arquivada era seu limite de escopo: esta proposta é a evolução separada explicitamente solicitada, sem alterar a capability de registros patrimoniais.

Checkpoint auditado: branch feature/frontend-angular, HEAD b30d8b6, working tree limpo e sincronizado com a referência local origin; nenhuma change ativa antes da criação. Strict global inicial 31/31. Baseline reutilizado: initial 514418 bytes, Dashboard lazy aproximadamente 37,16 kB, CSS evolução 4284 bytes. A dívida histórica de 506086 → 514418 bytes permanece aceita na change anterior, sem novo waiver automático para crescimento desta change. Esta rodada produz apenas planejamento; implementação exige autorização posterior.

## Direção vigente — Bloco 1 (09/09/2026)

A versão anterior de Custo × Valor atual não foi aprovada visualmente. O refinamento isolado foi substituído pela execução conjunta de estrutura do Dashboard, comparativo horizontal moderno e apresentação do Histórico do patrimônio. Implementação diretamente autorizada nesta rodada, preservando o planejamento dos gráficos de Resultado não realizado e Rentabilidade para depois da revisão humana, sem placeholders. Composição/alocação continuam fora do escopo.

Somente template/CSS do histórico podem mudar, preservando coordenadas, dados e interações. Contexto e ações compactos precedem indicadores com patrimônio protagonista por moeda, análise, histórico, posições e resultados. A aprovação visual humana continua pendente.


## Direção vigente — Bloco 2 (09/09/2026)

O usuário aprovou visualmente o desktop do Bloco 1 e autorizou Resultado não realizado e Rentabilidade por ativo. Desktop é prioridade; revisão mobile detalhada adiada deliberadamente, mantendo verificação de regressões graves. Custo × Valor atual, indicadores, histórico e demais seções não serão redesenhados. A eventual reconsideração do comparativo ocorrerá após revisão conjunta dos três gráficos.

Dois painéis brancos lado a lado quando houver espaço após Custo × Valor atual, cada um com grupos BRL/USD internos e barras divergentes estáticas. Fonte exclusiva: resultadoNaoRealizado e rentabilidadePercentual recebidos. Zero central, escala simétrica própria por gráfico e por moeda, labels/valores/sinais/estado sempre visíveis e equivalência textual sem foco novo. A instrução atual substitui a decisão anterior de escala percentual comum entre moedas: nesta rodada também a rentabilidade usa escala independente por moeda. Sem regra financeira nova ou HTTP adicional.


## Revisão do Bloco 2 — dot plot (09/09/2026)

Diagnóstico aprovado; opção C adotada. Patrimônio histórico não representa rentabilidade: compras, vendas e composição alteram seu valor. Não implementar rentabilidade temporal, reconstrução, persistência, endpoint, snapshot ou fórmula nova nesta change.
Somente Rentabilidade por ativo passa de barras para dot plot estático com eixo percentual divergente, zero central e marcador proporcional. Ticker, empresa, percentual autoritativo e Positivo/Negativo/Neutro permanecem visíveis. BRL/USD mantêm grupos e escalas simétricas independentes pela maior magnitude recebida, sem corte de extremos, FX ou retorno global. Zero e -0 ficam no centro; valores subpixel mantêm texto e aviso, sem deslocamento mínimo artificial. O tamanho do ponto identifica a observação, não uma magnitude financeira.
Resultado não realizado, Custo × Valor atual e Histórico do patrimônio permanecem intactos. Desktop prioritário; revisão mobile detalhada adiada. Revisão humana do Bloco 2 permanece pendente. Esta decisão substitui apenas a apresentação anterior da rentabilidade; registros anteriores são histórico das decisões.

## Estado após aprovação humana desktop do Bloco 2

O usuário aprovou a seção Análise por ativo e seus três gráficos no desktop, incluindo separação BRL/USD, dot plot, zero, estados e percentuais. A pendência desktop acima foi superada; mobile detalhado, texto ampliado e leitor de tela continuam adiados. Aceite e candidatos não bloqueantes em evidencias-bloco2.md. Nenhum requisito novo foi criado. A leitura integral não encontrou escopo de implementação para um Bloco 3; ver evidencias-bloco3.md antes de qualquer continuação.

## Auditoria para encerramento

Diagnóstico de ausência de Bloco 3 aprovado; não criar novo bloco nem próxima change. O usuário confirmou as entregas anteriores e leitura geral desktop sem regressão evidente. Intuitividade de custo/valor próximos, notação `0E-12 USD`, refinamento visual mobile profundo e novos gráficos são candidatos futuros não bloqueantes. Estado vigente em evidencias-encerramento.md: 42/45 tasks concluídas; pendências 1.3, 7.2 e 7.4, sem correção funcional conhecida. Não presumir leitor de tela/ampliação final validados nem captura histórica existente. Archive não executado.

## Encerramento final aprovado

O usuário aprovou a revisão humana final das tasks 7.2 e 7.4 e aceitou explicitamente a ausência da captura anterior de 1.3 como limitação documental. A captura não foi realizada e não existe como evidência. Estado final: 45/45 tasks encerradas (uma com dispensa parcial aceita), zero pendências bloqueantes; classificação A, pronta para archive, ainda não arquivada. Detalhes em evidencias-encerramento.md. Registros anteriores de pendência foram superados; testes/build, bundles e warnings reutilizados sem alteração de código.
