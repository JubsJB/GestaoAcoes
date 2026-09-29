## 1. Reorganização estrutural

- [x] 1.1 Revisar integralmente a change aprovada, specs consolidadas, PRD e implementação vigente antes de alterar código; confirmar que o diff inicial está restrito ao frontend previsto.
- [x] 1.2 Reordenar o template do Dashboard para cabeçalho/contexto, resumo, evolução, distribuição, desempenho e resultados realizados, preservando a evolução independente do bloco financeiro.
- [x] 1.3 Preservar identificação da Carteira e ações Ver carteira, Registrar operação e Atualizar dados com os mesmos destinos, dialog, captura e gatilhos HTTP, sem seletor local.

## 2. Resumo por moeda

- [x] 2.1 Reorganizar cada grupo BRL/USD para destacar patrimônio e resultado não realizado, mantendo custo e rentabilidade como informações complementares com os valores recebidos.
- [x] 2.2 Preservar grupos independentes para somente BRL, somente USD e ambas as moedas, sem conversão ou total combinado.
- [x] 2.3 Manter estado específico de Carteira sem posições e eventual contagem estrutural, sem cards ou indicadores financeiros fictícios.

## 3. Evolução patrimonial

- [x] 3.1 Reposicionar PortfolioEvolutionComponent após o resumo sem alterar GET, POST manual, inputs, refresh, dataset, geometria, moedas ou significado dos snapshots.
- [x] 3.2 Preservar vazio, ponto único, gaps, snapshots vazios, histórico textual, criação pendente/erro, retry e isolamento contra troca de Carteira.
- [x] 3.3 Confirmar por testes que Atualizar dados não cria snapshot e que somente Registrar patrimônio atual executa o POST vigente.

## 4. Distribuição da carteira

- [x] 4.1 Apresentar PortfolioCompositionComponent em seção própria de Distribuição, com heading e descrição orientados à alocação do valor atual por ativo.
- [x] 4.2 Preservar coleção recebida, ordem, agrupamento BRL/USD, legenda textual integral, zeros/invalidade e geometria exclusivamente visual sem requests adicionais.
- [x] 4.3 Cobrir distribuição vazia, uma moeda, duas moedas, valores extremos e muitos ativos sem top-N, percentual publicado ou total novo.

## 5. Consolidação de desempenho por ativo

- [x] 5.1 Adaptar a apresentação atual para uma única linha/painel por ativo com resultado não realizado monetário como informação principal e rentabilidade percentual recebida como complemento.
- [x] 5.2 Manter eixo monetário divergente e escalas independentes por moeda, sem inserir percentual no eixo monetário nem derivar um campo do outro.
- [x] 5.3 Preservar ticker, empresa, moeda, valores lossless, sinais e estados Positivo/Negativo/Neutro em texto, inclusive quando resultado e rentabilidade possuírem sinais diferentes.
- [x] 5.4 Remover a instância de gráfico dedicada exclusivamente à rentabilidade, preservando a informação percentual e eliminando somente código local comprovadamente sem consumidor.

## 6. Resultados realizados

- [x] 6.1 Manter a consulta e os itens de resultados realizados no fim do Dashboard, ajustando apenas sua prioridade visual secundária.
- [x] 6.2 Preservar identificação, moeda, sinal, estado vazio e ausência de totalização, fusão com resultado não realizado ou destino novo em Carteira.

## 7. Remoção das representações redundantes

- [x] 7.1 Remover do Dashboard o bloco Custo × Valor atual e seus imports/integração, sem remover campos, contratos, endpoints ou apresentações usadas em outros fluxos.
- [x] 7.2 Remover do Dashboard a utilização de PortfolioPositionsComponent e o heading da tabela, preservando componente, estilos, testes e uso no detalhe da Carteira.
- [x] 7.3 Revisar referências e código local sem consumidor após as remoções; excluir somente artefatos comprovadamente exclusivos das representações removidas e atualizar testes correspondentes.
- [x] 7.4 Confirmar que GET de posições continua ocorrendo uma única vez por carga/reload para alimentar contagem, distribuição e desempenho.

## 8. Responsividade e acessibilidade

- [x] 8.1 Ajustar grids e componentes pelo espaço real do container, usando min-width zero, reflow e container queries quando adequadas, sem alterar o breakpoint estrutural do shell.
- [x] 8.2 Garantir uma coluna quando necessário, textos/valores longos quebráveis, SVGs legíveis e ausência de largura/altura rígida, scroll interno concorrente ou overflow horizontal.
- [x] 8.3 Revisar hierarquia h1/h2/h3/h4, IDs e associações após remover seções, mantendo uma ordem semântica coerente.
- [x] 8.4 Preservar foco visível, nomes acessíveis, teclado, status de loading, feedback de erro, vazios distintos e informação textual que não dependa de cor ou hover.
- [x] 8.5 Confirmar que gráficos estáticos permanecem fora da ordem de foco com equivalente HTML e que movimento reduzido não oculta informação nem altera comportamento.

## 9. Testes automatizados

- [x] 9.1 Atualizar DashboardPageComponent specs para a nova ordem, ações, contexto, resumo BRL, USD, ambas as moedas e Carteira sem posições.
- [x] 9.2 Testar ausência de Custo × Valor atual, gráfico percentual independente e tabela completa no Dashboard, além da permanência de distribuição, evolução e resultados realizados.
- [x] 9.3 Atualizar testes de desempenho para resultado principal, rentabilidade complementar, sinais positivos/negativos/neutros e sinais divergentes sem recomputação.
- [x] 9.4 Preservar testes lossless, moedas independentes, extremos, zero, 100 ativos, ausência de HTTP dos gráficos e respostas obsoletas na troca de Carteira.
- [x] 9.5 Executar regressão do detalhe da Carteira e PortfolioPositionsComponent, incluindo todos os campos e reflow, para comprovar que a remoção é exclusiva do Dashboard.
- [x] 9.6 Executar regressão de contexto/URL, shell/sidebar, Operações contextualizadas, formulário/detalhe/dialogs, Ativos, Corretoras, rotas lazy, reload e voltar/avançar.

## 10. Validação manual

Evidência parcial aprovada para o Resumo por moeda: Carteira somente BRL e Carteira mista BRL + USD foram validadas em desktop com sidebar expandida/recolhida e em mobile, com uso correto da largura, ordem dos indicadores, reflow e ausência de overflow horizontal nesse bloco. Esta evidência não conclui isoladamente as tasks 10.1 ou 10.2, que também abrangem Carteira somente USD, nomes/valores longos, larguras intermediárias, aproximadamente 320 CSS px e as demais seções e gráficos do Dashboard.

Evidência aprovada para zoom, baixa altura e rolagem: o Dashboard foi validado em 125%, 150% e 200% de zoom e em janela desktop com baixa altura. Todo o conteúdo permaneceu acessível pela rolagem vertical principal, sem overflow horizontal relevante, cortes permanentes, sobreposição ou rolagens internas indevidas em cards e gráficos. Cards, textos, distribuição e desempenho permaneceram legíveis e utilizáveis. O corte anteriormente observado na ação do page-header em 200% foi corrigido com container query, retestado e aprovado.

Evidência parcial aprovada para acessibilidade: navegação da aplicação, seletor de Carteira, ações do Dashboard e controles do formulário/modal foram percorridos e acionados por teclado, com ordem coerente e foco visível. Esta evidência não conclui isoladamente a task 10.4, que também exige validação manual de headings/ordem de leitura, retry, pontos da evolução, contraste e compreensão sem depender exclusivamente de cor.

Evidência aprovada para desktop, hierarquia e conteúdo longo: a Carteira de Investimentos Internacionais 2026 foi validada com sidebar expandida e recolhida, sem corte, sobreposição ou overflow horizontal. O nome longo, a identificação da Carteira, as ações, o resumo e o histórico permaneceram legíveis e com hierarquia adequada. Após registrar posição USD, os quatro indicadores do resumo permaneceram íntegros, inclusive com valores como US$ 4.937,80, US$ 0,00 e 0,00%. Em conjunto com as evidências anteriores de Carteiras BRL, USD e mistas, esta validação conclui a task 10.1.

Evidência parcial adicional para responsividade: em viewport mobile, o nome longo da Carteira quebrou naturalmente em múltiplas linhas, sem corte ou overflow; as ações Ver carteira, Registrar operação e Atualizar dados foram reorganizadas verticalmente; o resumo por moeda e a indicação de uma posição aberta permaneceram legíveis. Esta evidência é complementada pela validação abaixo.

Evidência complementar de responsividade fornecida pelo usuário em 24/09/2026: a validação manual relatada cobre larguras intermediárias e aproximadamente 320 CSS px, incluindo cabeçalho e ações principais, resumo por moeda, histórico do patrimônio, distribuição/composição da carteira, gráfico de composição por ativo, desempenho por ativo e resultados realizados por ação. Os componentes reorganizaram-se para fluxo vertical em espaço reduzido, sem cortes, sobreposições, overflow horizontal ou perda de legibilidade de textos, valores, percentuais e informações dos gráficos. Em aproximadamente 320 CSS px:

- os cards do resumo passaram para uma única coluna e as ações principais foram reorganizadas verticalmente, sem compressão inadequada;
- o histórico permaneceu legível e utilizável;
- a composição reorganizou o donut e a relação de ativos sem preservar inadequadamente o layout desktop; AAPL e NVDA mantiveram a associação correta com seus respectivos nomes e valores;
- o desempenho por ativo passou ao fluxo vertical, preservando ativo, empresa, resultado não realizado, classificação textual, representação gráfica, rentabilidade e rentabilidade completa;
- o estado vazio de resultados realizados permaneceu compreensível;
- não foi identificado conteúdo permanentemente inacessível devido ao cabeçalho durante a rolagem.

Em conjunto com a evidência mobile anterior, o relato atende integralmente aos critérios da task 10.2: larguras intermediárias, mobile e aproximadamente 320 CSS px sem scroll horizontal, corte, sobreposição, cards espremidos ou gráficos ilegíveis. A task 10.2 está concluída com base nas evidências fornecidas pelo usuário; nenhuma nova execução manual foi realizada nesta atualização documental. Este aceite não substitui as verificações específicas de acessibilidade ainda pendentes na task 10.4.

Evidência parcial adicional para estados: uma Carteira recém-criada e sem posições apresentou zero posições abertas, Resumo por moeda com estado "Resumo ainda indisponível", explicação "Esta carteira não possui posições abertas" e evolução vazia com "Nenhum registro de patrimônio". Após registrar uma operação, o Dashboard transitou corretamente para uma posição aberta e resumo USD com cards financeiros. A evidência complementar de 24/09/2026 também confirma que o estado vazio de resultados realizados permanece compreensível em aproximadamente 320 CSS px. A task 10.5 permanece pendente enquanto não forem validados os demais estados de Carteiras (carregamento, erro, nenhuma Carteira e espera por seleção), seleção inválida/indisponível, loading/erro financeiro, distribuição/desempenho vazios e erro de evolução.

- [x] 10.1 Validar hierarquia, densidade e legibilidade em desktop com sidebar expandida e recolhida, incluindo nomes e valores longos e Carteiras BRL, USD e mistas.
- [x] 10.2 Validar larguras intermediárias, mobile e aproximadamente 320 CSS px sem scroll horizontal, corte, sobreposição, cards espremidos ou gráficos ilegíveis.
- [x] 10.3 Validar zoom 125%, 150% e 200%, texto ampliado, baixa altura e rolagem única no main do shell.
- [x] 10.4 Validar teclado, foco visível, ordem de headings/leitura, ações, retry, pontos da evolução, contraste e compreensão sem depender exclusivamente de cor.
- [x] 10.5 Confirmar os estados de Carteiras, seleção inválida, finanças loading/erro/vazio, distribuição/desempenho vazios, evolução vazia/erro e resultados vazios, combinando validações manuais com testes automatizados; aceitar a cobertura automatizada existente para loading, erro e ausência de Carteiras.

Evidência complementar final de acessibilidade fornecida pelo usuário para a task 10.4:

- Teclado, acionamento das ações e foco visível: considerada a evidência parcial aprovada anteriormente, com navegação e acionamento por teclado em ordem coerente e foco visível.
- Headings e leitura: "Dashboard" foi verificado como H1; "Resumo por moeda", "Histórico do patrimônio", "Distribuição da carteira", "Desempenho por ativo" e "Resultados realizados por ação" foram verificados como H2. A sequência visual e estrutural acompanha a ordem de leitura esperada.
- Compreensão sem depender exclusivamente de cor: resultados negativos no desempenho apresentam valor monetário com sinal negativo, texto "Negativo" e rentabilidade com sinal negativo, além da cor vermelha.
- Contraste: o usuário verificou no DevTools a cor negativa #a33139 e relatou razão de contraste de 6.87 sobre o fundo utilizado.
- Pontos da evolução: em mobile, os registros também aparecem como controles textuais abaixo do gráfico, identificados por número, data e horário. Em desktop, foi validada a seleção de um ponto, com destaque visual e correspondência com o registro textual.
- Retry: o bloqueio local de `/api/carteiras/3/evolucao-patrimonial` pelo DevTools exibiu "Falha técnica na comunicação HTTP." e "Tentar novamente o histórico". Após desativar o bloqueio sem recarregar a página e acionar o retry, uma nova requisição de evolução foi realizada, o erro desapareceu e o gráfico foi restaurado com os quatro registros anteriores.

Em conjunto com a evidência anterior, o relato cobre os critérios enumerados na redação atual da task 10.4, que está concluída. O cenário de recuperação da evolução atende ao item retry; este aceite não afirma validação dos demais botões de retry nem de toda a matriz de estados da task 10.5. A task 10.5 permanece pendente, embora seu estado de erro de evolução agora tenha evidência manual. Nenhuma nova execução manual foi realizada pelo agente nesta atualização documental.

Evidência final e aceite combinado da task 10.5 em 29/09/2026: o usuário aprovou expressamente combinar validações manuais com testes automatizados para os três estados da listagem de Carteiras. Este registro substitui as indicações anteriores de pendência da 10.5, preservadas acima como histórico.

Evidências observadas manualmente e relatadas pelo usuário:

- Finanças: loading ao atrasar `/api/carteiras/3/resumo`, com "Atualizando indicadores de Mobile…"; erro; vazio; e retry com recuperação bem-sucedida das consultas `resumo`, `posicoes`, `resultados-realizados` e `evolucao-patrimonial`.
- Carteira sem posições: "0 posições abertas" e "Resumo ainda indisponível"; distribuição com "Nenhuma posição para distribuir"; desempenho com "Nenhuma posição aberta para comparar".
- Seleção inválida/indisponível: acesso com `?carteiraId=10`, aviso "A carteira indicada na URL não está disponível. Selecione uma carteira válida." e estado "Selecione uma carteira", sem fallback silencioso para a carteira exibida no seletor.
- Evolução vazia; evolução com erro de comunicação e retry com recuperação do gráfico, conforme evidência anteriormente registrada na 10.4.
- Resultados realizados vazios e transição de carteira sem posições para posição aberta após registrar operação.

Evidências aceitas por cobertura automatizada, sem afirmar observação manual desses três estados:

- Loading da listagem de Carteiras: `DashboardPageComponent` — "anuncia loading e permite retry após erro da lista"; `CarteiraContextService` — "shares an in-flight collection and chooses minimum id without persisting fallback".
- Erro da listagem de Carteiras e retry: `DashboardPageComponent` — "anuncia loading e permite retry após erro da lista"; `CarteiraContextService` — "keeps empty collection distinct from an error and supports explicit retry".
- Ausência de Carteiras: `DashboardPageComponent` — "mostra empty state e não consulta finanças com zero carteiras"; o teste de contexto acima também distingue coleção vazia de erro e confirma limpeza do erro após retry.

Execução realizada nesta etapa, no diretório `frontend`: `npm test -- --watch=false --include="src/app/features/dashboard/dashboard-page.component.spec.ts" --include="src/app/core/carteira/carteira-context.service.spec.ts"`. Resultado: 2 arquivos aprovados, 47 testes aprovados, 0 falhos, código de saída 0. A primeira tentativa foi impedida pelo sandbox com `spawn EPERM` no esbuild, antes da execução dos testes; a repetição autorizada fora do sandbox passou. Nenhuma correção de implementação ou teste foi necessária.

Com as evidências manuais relatadas e os testes efetivamente executados, a task 10.5 está concluída segundo o critério combinado aprovado. Nenhuma nova execução manual foi realizada pelo agente nesta etapa.

## 11. Build, OpenSpec e Graphify

- [x] 11.1 Executar `npm test -- --watch=false` no frontend e corrigir somente regressões pertencentes à change, registrando arquivos, total, aprovados e falhos.
- [x] 11.2 Executar `npm run build`, registrar initial, transferência, chunk lazy/CSS relevantes e warnings, sem alterar budgets, dependências ou fazer micro-otimizações não relacionadas.
- [x] 11.3 Executar `openspec validate reorganizar-simplificar-dashboard-da-carteira --strict` e `git diff --check`, corrigindo somente problemas desta change.
- [x] 11.4 Atualizar Graphify com `graphify update .` após as alterações de código e registrar o resultado conforme AGENTS.md.
- [x] 11.5 Revisar o diff completo e confirmar ausência de backend, banco, endpoint, contrato REST, fórmula, câmbio, página nova de Carteira, migração de resultados, funcionalidade futura, shell desnecessário, dependência e alteração de budget.
- [x] 11.6 Apresentar resultados automatizados/manuais, tasks concluídas e pendências reais para revisão, sem staging, commit, push, merge, sincronização ou arquivamento.
