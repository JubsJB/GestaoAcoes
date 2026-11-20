## Context

Ver proposal.md para motivação. O Dashboard atual carrega, para a Carteira selecionada, resumo, posições e resultados realizados em um `forkJoin`, enquanto Evolução patrimonial mantém consulta e estado independentes. A mesma coleção de posições alimenta Composição, Custo × Valor atual, Resultado não realizado, Rentabilidade e a tabela completa; isso repete os mesmos ativos e campos em até cinco representações antes de resultados realizados.

Contratos vigentes e preservados:

| Fonte | Uso atual e futuro |
| --- | --- |
| `GET /carteiras/{id}/resumo` | quatro indicadores prontos por moeda |
| `GET /carteiras/{id}/posicoes` | distribuição, desempenho e detalhe da Carteira |
| `GET /carteiras/{id}/resultados-realizados` | resultados individuais, sem total frontend |
| `GET /carteiras/{id}/evolucao-patrimonial` | série de snapshots manuais |
| `POST /carteiras/{id}/snapshots` | registro manual explícito |

`DashboardService` e `EvolutionService` preservam `BigDecimal` como strings lossless. Coordenadas podem usar aproximação isolada, mas labels e estados usam a string autoritativa. `CarteiraContextService` e `CarteiraNavigationService` mantêm URL, precedência, concorrência e seletor único no shell.

O shell aprovado possui sidebar desktop expandida/recolhida, drawer sobreposto abaixo de 960px, área de conteúdo centralizada e rolagem no `main`. A largura útil do Dashboard varia sem que o viewport mude; por isso a composição interna não pode depender somente de media queries globais.

O detalhe de Carteira já reutiliza `PortfolioPositionsComponent` e apresenta integralmente as posições abertas. Resultados realizados ainda não têm apresentação equivalente fora do Dashboard.

## Goals / Non-Goals

**Goals:**

- tornar a primeira leitura do Dashboard analítica e previsível;
- reduzir representações redundantes sem remover dados ou contratos;
- manter as perguntas distintas: valor/custo/resultado/rentabilidade, evolução, distribuição e contribuição por ativo;
- preservar precisão lossless, moedas independentes, concorrência, estados e rotas;
- reutilizar componentes existentes e concentrar adaptações na feature lazy do Dashboard, com a extensão visual de Operações e a atualização documental descritas na decisão 8;
- manter acesso ao detalhamento de posições no fluxo de Carteira.

**Non-Goals:**

- alterar backend, banco, endpoints, DTOs, fórmulas, parsing ou ordem dos dados;
- converter ou consolidar BRL/USD;
- transformar snapshots em rentabilidade histórica, TWR, XIRR ou reconstrução por Operações;
- criar página conceitual nova de Carteira ou migrar resultados realizados nesta change;
- adicionar proventos, dividendos, transferência, corretora, setor, classe, alocação-alvo ou rebalanceamento;
- modificar shell, seletor, Ativos ou Corretoras além de testes de compatibilidade, ou alterar comportamento funcional/regras financeiras de Operações;
- instalar biblioteca, alterar budget ou realizar refatoração arquitetural ampla.

## Decisions

### 1. Estrutura anterior e estrutura proposta

Estrutura anterior:

```text
Cabeçalho/contexto e ações
Resumo por moeda
Análise por ativo
├── Composição
├── Custo × Valor atual
├── Resultado não realizado
└── Rentabilidade por ativo
Histórico do patrimônio
Posições abertas completas
Resultados realizados
```

Estrutura proposta:

```text
Cabeçalho/contexto e ações
Resumo da carteira por moeda
Evolução patrimonial
Distribuição da carteira
Desempenho por ativo
└── resultado monetário principal + rentabilidade complementar
Resultados realizados — seção secundária
```

A evolução sobe antes dos gráficos de posição porque responde à pergunta temporal geral da carteira. Distribuição e desempenho tornam-se seções irmãs, cada uma com uma pergunta única. Resultados realizados permanecem por ausência de destino equivalente.

Alternativa descartada: manter a ordem e apenas reduzir espaçamentos. Isso não elimina a repetição conceitual nem a tabela operacional na primeira tela.

### 2. Resumo por moeda sem cálculo novo

Manter a iteração sobre `resumo.resumos` e os quatro campos existentes. Dentro de cada grupo:

1. patrimônio atual;
2. resultado não realizado total;
3. custo total das posições;
4. rentabilidade percentual.

Patrimônio e resultado recebem maior peso tipográfico/superfície. Custo e rentabilidade continuam visíveis, mas como metadados complementares. Uma moeda gera um único grupo; duas moedas podem compartilhar linha somente quando a largura útil comportar; zero moedas produz o estado existente de carteira sem posições. Não criar cartões vazios para moeda ausente.

Alternativa descartada: total BRL + USD ou conversão para moeda-base, pois não há contrato de câmbio nem regra financeira aprovada.

### 3. Evolução patrimonial preservada como módulo independente

Reutilizar `PortfolioEvolutionComponent`, `EvolutionService`, models, geometria, interação e estados atuais. A reorganização muda sua posição no fluxo, não o significado dos dados. Permanecem:

- gráficos BRL/USD independentes;
- gaps, ponto único e snapshots vazios;
- histórico textual integral;
- registro manual de patrimônio;
- GET/POST, bloqueio de double-submit e proteção de contexto;
- foco, teclado, toque, tooltip e Escape.

Não haverá filtros de período, anotação de aporte/Operação, reconstrução ou nova série. Ajustes de densidade só são permitidos se não retirarem a alternativa textual nem alterarem coordenadas, dataset ou tab stops existentes.

Alternativa descartada: calcular uma série a partir de Operações, pois mudaria contrato e significado financeiro.

### 4. Distribuição reutiliza a composição existente

Reutilizar `PortfolioCompositionComponent`, `composition-geometry` e apresentação lossless. O componente continua recebendo a coleção já carregada, agrupando por moeda e derivando apenas ângulos. Nenhum request, percentual textual ou total sincronizado com o resumo será acrescentado.

A seção deixa de estar aninhada em um bloco genérico “Análise por ativo” junto a comparativos redundantes e passa a ter heading/finalidade próprios: “Onde o patrimônio da carteira está alocado?”. O gráfico decorativo e a legenda HTML integral permanecem.

Alternativa descartada: criar gráfico novo de setor/corretora, pois os dados não existem no contrato atual.

### 5. Desempenho consolida resultado e rentabilidade

Adaptar `PositionPerformanceChartComponent` ou extrair uma variante local equivalente para receber uma única configuração de desempenho. A coleção e a geometria divergente do resultado monetário permanecem a base principal. Cada linha deverá conter:

- ticker e empresa;
- resultado não realizado na moeda da posição;
- estado Positivo/Negativo/Neutro;
- rentabilidade percentual recebida, com rótulo e estado complementar.

Não haverá segundo `app-position-performance-chart` para `metric="return"`. A escala monetária não receberá o percentual; o percentual será texto complementar e não marcador no mesmo eixo. Campos com sinais divergentes serão apresentados como recebidos, sem “correção” frontend.

Preferência de implementação: adaptar o componente atual com API pequena e explícita, removendo o modo de gráfico percentual se ele ficar sem consumidor. Helpers de sinal, formatação lossless e agrupamento devem ser reutilizados. Não criar componente genérico de charts nem mover lógica para shared.

Alternativas descartadas:

- dois gráficos menores: ainda duplica todos os ativos e mantém excesso vertical;
- eixo duplo monetário/percentual: mistura unidades incompatíveis e dificulta acessibilidade;
- ranking/top-N: altera ordem e omite dataset, contrariando os contratos existentes.

### 6. Remoção limitada das redundâncias

Remover do template/composição do Dashboard:

- `CostValueChartComponent` como bloco visível;
- instância independente de rentabilidade;
- `PortfolioPositionsComponent` no Dashboard.

Não remover automaticamente arquivos/helpers. Durante implementação, somente código comprovadamente sem consumidor dentro da feature poderá ser excluído, acompanhado por atualização dos testes. `PortfolioPositionsComponent`, estilos, suíte e uso em Carteira são obrigatoriamente preservados. `custoPosicao` e `valorAtualPosicao` permanecem no DTO e em todos os contratos.

Alternativa descartada: remover endpoint ou campos porque a página deixa de exibi-los; distribuição, desempenho, detalhe e compatibilidade ainda dependem da coleção de posições.

### 7. Resultados realizados permanecem secundários

Manter a consulta e a coleção atuais. O bloco permanece depois das seções analíticas, com menor peso visual, vazio próprio e itens individuais. Não calcular total e não fundir realizado com não realizado.

Registrar como direção futura, sem task funcional nesta change, que uma visão detalhada de Carteira poderá receber esses resultados. Até existir capability aprovada e destino equivalente, o Dashboard continua responsável por apresentá-los.

Alternativa descartada: retirar agora e deixar disponível apenas pela API, pois isso causaria perda funcional na interface.

### 8. Cabeçalho, ações e contexto sem mudança contratual

Extensão de escopo aprovada na revisão final: o formulário de Operações deixa de repetir a Carteira em um bloco `.context` no corpo e mantém a identificação da Carteira capturada no cabeçalho, tanto na página quanto no dialog. A alteração é exclusivamente visual/contextual; preserva captura da origem durante troca global, reconstrução em nova instância, validações, cálculos, payload/POST, toast, fechamento e navegação. Os testes de integração devem verificar a identificação no cabeçalho e conservar as asserções funcionais posteriores.

O README será atualizado somente nos trechos afetados pela reorganização. Capturas antigas sem substituição real disponível serão explicitamente identificadas como históricas, sem gerar imagens artificiais nem alterar funcionalidades não relacionadas.

Preservar `PageHeaderComponent`, nome da Carteira ativa e as ações:

- Ver carteira → `/carteiras/{id}`;
- Registrar operação → dialog existente com Carteira capturada;
- Atualizar dados → refaz resumo, posições, resultados e evolução sem criar snapshot.

O layout pode agrupar contexto e ações de modo menos ruidoso, mas não muda peso funcional, destino, dialog, toast ou bloqueio durante carga. O seletor continua exclusivamente no shell. A troca de Carteira continua invalidando dados anteriores por `switchMap`/proteção vigente.

### 9. Estados permanecem distintos

Preservar a máquina de estados atual:

- carteiras carregando;
- erro ao carregar carteiras e retry;
- nenhuma carteira;
- seleção inválida/indisponível;
- espera por seleção;
- finanças carregando;
- erro financeiro conjunto e retry;
- carteira válida sem posições;
- conteúdo;
- evolução carregando, vazia, com ponto único, com dados ou com erro;
- criação de snapshot em andamento/erro;
- resultados realizados vazios.

O `forkJoin` das três fontes principais pode permanecer nesta change; redesenhar carregamentos parciais ampliaria escopo e concorrência. A evolução continua independente. Na troca A → B, nenhum bloco pode manter visualmente dados de A.

### 10. Responsividade orientada pelo container

O Dashboard e os componentes analíticos devem conservar `min-width: 0`, quebras naturais e SVG fluido. Grids internos deverão usar `auto-fit/minmax` e/ou container queries nos componentes cujo espaço muda com a sidebar. Media queries continuam válidas para comportamento global/mobile, mas não podem ser a única decisão de colunas.

Critérios por cenário:

- sidebar expandida: conteúdo funciona no espaço reduzido sem corte;
- sidebar recolhida: colunas podem aproveitar espaço adicional sem mudar semântica;
- intermediário/zoom: empilhar antes de comprimir cards ou texto;
- mobile/320 CSS px: uma coluna quando necessário;
- nomes/decimais longos: `overflow-wrap` e largura mínima zero;
- muitos ativos/snapshots: crescimento vertical na rolagem principal, sem scroll interno concorrente.

Evitar alturas fixas de conteúdo, largura mínima rígida, `white-space: nowrap` em valores essenciais, elipse, fonte reduzida para “fazer caber” e overflow horizontal da página.

### 11. Acessibilidade preserva equivalentes textuais

Manter um `h1` no cabeçalho, `h2` nas cinco seções e níveis internos coerentes para moeda/gráfico. A remoção de blocos também remove seus headings; não deixar saltos ou IDs órfãos.

SVGs estáticos de distribuição/desempenho permanecem decorativos e fora do foco, pois cada ativo terá equivalente HTML. Resultado e rentabilidade usam valor, sinal e estado textual; composição mantém número/padrão/legenda além de cor. Evolução conserva seus pontos interativos e alternativa completa.

Loading usa status anunciado; erros preservam feedback e retry; vazios têm títulos e mensagens próprios. Ações mantêm nomes acessíveis, alvos e foco visível. Nenhuma nova animação será introduzida; transições existentes devem respeitar movimento reduzido.

### 12. Compatibilidade e arquivos previstos

Arquivos prováveis de implementação:

- `dashboard-page.component.html`, `.scss` e testes;
- `position-analysis.component.ts` e testes;
- `position-performance-chart.component.*` e testes;
- eventualmente estilos/helpers locais se consumidores mudarem.

Reutilização obrigatória ou preferencial:

- `DashboardService`, models e parser lossless;
- `PortfolioEvolutionComponent` e `EvolutionService`;
- `PortfolioCompositionComponent` e geometria;
- helpers de agrupamento, formatação e projeção;
- `PageHeaderComponent`, `FeedbackAlertComponent`, cards e tokens;
- `PortfolioPositionsComponent` no detalhe de Carteira;
- contexto e navegação globais.

Regressões a proteger: detalhe de Carteira/posições, Operações contextualizadas e seus dialogs/formulários/detalhes, Ativos, Corretoras, seletor, sidebar, rotas lazy, voltar/avançar e reload.

## Risks / Trade-offs

- Rentabilidade perder clareza ao deixar o plot próprio → manter rótulo, valor, sinal e estado por ativo junto ao resultado e testar sinais divergentes.
- Remoção acidental do componente compartilhado → teste de Carteira e revisão de imports/consumidores antes de excluir qualquer arquivo.
- Composição e desempenho ainda crescerem muito com 100 ativos → preservar dataset e altura natural; aceitar página longa em vez de top-N ou scroll interno.
- Resultados realizados parecerem esquecidos → manter heading e vazio próprios, apenas reduzir prioridade espacial.
- Evolução dominar a primeira dobra → reorganizar espaçamento sem retirar ação, histórico ou acessibilidade.
- Container queries divergirem de viewport tests → testar largura real do host com sidebar nos dois estados e zoom, além de assertions estruturais.
- Estado vazio perder distinções após remoção da tabela → manter vazio financeiro, vazio da distribuição/desempenho, vazio da evolução e vazio dos realizados semanticamente separados.
- Código morto dos gráficos removidos aumentar manutenção → excluir somente o que estiver comprovadamente sem consumidor e sem afetar contratos/testes externos.
- Bundle/CSS continuar acima dos warnings vigentes → medir e registrar; não aumentar budgets nem realizar micro-otimização fora do escopo.

## Migration Plan

Após aprovação explícita, implementar incrementalmente dentro da feature lazy: estrutura e resumo; reposicionamento da evolução; distribuição; desempenho consolidado; resultados secundários; remoção dos dois blocos redundantes. Atualizar testes a cada etapa, executar suíte frontend e build, validar responsividade/acessibilidade manualmente, executar OpenSpec strict, `git diff --check` e atualizar Graphify somente após alterações de código.

Não há migração de dados, API ou schema. Rollback restaura template/composição anteriores sem alterar dados persistidos. Sincronização/arquivamento OpenSpec, staging, commit, push e merge permanecem etapas posteriores e separadamente autorizadas.

## Limitações conhecidas e evolução futura

- evolução continuará limitada aos snapshots manuais disponíveis;
- não haverá explicação de variações por aporte, compra ou venda;
- desempenho continuará representando posições abertas; não combinará realizado, não realizado e proventos;
- resultados realizados continuarão temporariamente no Dashboard;
- detalhe de Carteira atual continuará sendo o destino operacional das posições, sem nova página conceitual;
- distribuição continuará somente por ativo e moeda, sem setor, corretora, classe ou alvo;
- nenhuma ordenação por contribuição será introduzida; a ordem recebida permanece autoritativa para apresentação.
