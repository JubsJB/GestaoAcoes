## MODIFIED Requirements

### Requirement: Fonte única das posições e identificação completa
A análise SHALL consumir somente as posições já carregadas para a Carteira atual. Cada ativo SHALL manter acaoId, ticker, nomeEmpresa e moeda identificáveis, com ordem recebida preservada dentro de cada moeda. Quantidade de itens MUST NOT causar top-N, amostragem, agregação, omissão ou duplicação. A análise SHALL apresentar uma única leitura de desempenho por ativo usando resultadoNaoRealizado e rentabilidadePercentual recebidos, sem solicitar HTTP, recomputar campos financeiros ou enriquecer identificadores.

#### Scenario: Carteira sem posições
- **WHEN** a Carteira válida retorna `posicoes=[]`
- **THEN** distribuição e desempenho apresentam estado sem posições, sem gráficos ou moedas artificiais, e a evolução permanece disponível

#### Scenario: Um ativo
- **WHEN** existe uma posição PETR4 em BRL
- **THEN** o desempenho apresenta exatamente esse ativo, seu resultado monetário e sua rentabilidade complementar sem sugerir tendência temporal

#### Scenario: Múltiplos ativos
- **WHEN** são recebidos três ativos em determinada ordem
- **THEN** todos aparecem uma vez no desempenho na ordem relativa recebida em seu grupo, sem classificação automática

#### Scenario: Muitos ativos
- **WHEN** são recebidas 100 posições com nomes extensos
- **THEN** todas permanecem identificáveis com valores textuais completos e linhas legíveis, sem truncar a coleção nem comprimir sua altura

#### Scenario: Sem recomputação financeira
- **WHEN** uma posição retorna resultadoNaoRealizado="7" e rentabilidadePercentual="3"
- **THEN** a apresentação usa respectivamente 7 e 3 recebidos, sem derivar um do outro ou reconciliá-los com custo, valor, quantidade ou cotação

#### Scenario: Quantidades fracionárias
- **WHEN** quantidadeAtual e preço médio possuem precisão extensa
- **THEN** suas strings permanecem intactas e o desempenho usa somente resultadoNaoRealizado e rentabilidadePercentual recebidos, sem multiplicar ou dividir campos

#### Scenario: Sem HTTP adicional
- **WHEN** distribuição e desempenho são montados, redimensionados ou reapresentados
- **THEN** nenhum GET, POST ou consulta por ativo é disparado para construí-los

### Requirement: Resultado não realizado divergente
O desempenho por ativo SHALL utilizar `resultadoNaoRealizado` diretamente como informação principal, com eixo de zero central e escala linear simétrica por moeda. Valores negativos SHALL estender-se à esquerda e positivos à direita; zero SHALL ser identificado como Neutro sem largura artificial. Cada linha SHALL apresentar texto monetário, estado Positivo/Negativo/Neutro e `rentabilidadePercentual` recebida como informação complementar claramente rotulada, sem segundo gráfico percentual independente.

#### Scenario: Lucro não realizado
- **WHEN** resultadoNaoRealizado="25.00" e rentabilidadePercentual="12.50"
- **THEN** a marca monetária fica à direita de zero e a linha informa resultado positivo de 25,00 na moeda e rentabilidade complementar de 12,50%

#### Scenario: Prejuízo não realizado
- **WHEN** resultadoNaoRealizado="-25.00" e rentabilidadePercentual="-12.50"
- **THEN** a marca monetária fica à esquerda de zero e ambos os valores recebidos permanecem identificados como negativos sem derivação entre eles

#### Scenario: Resultado neutro com percentual recebido
- **WHEN** resultadoNaoRealizado="0.000" ou "-0.00"
- **THEN** a linha informa resultado Neutro e apresenta a rentabilidade percentual recebida sem inventar barra monetária

#### Scenario: Resultado zero
- **WHEN** resultadoNaoRealizado="0.000" ou "-0.00"
- **THEN** a linha informa Neutro e zero, sem marca positiva ou negativa fictícia, preservando o percentual complementar recebido

#### Scenario: Todos os resultados neutros
- **WHEN** todos os resultados monetários do grupo são zero
- **THEN** eixo, linhas e percentuais complementares permanecem disponíveis sem extremos monetários inventados ou divisão por zero

#### Scenario: Moedas independentes
- **WHEN** ativos BRL e USD coexistem
- **THEN** resultados monetários usam grupos e escalas independentes, e cada percentual permanece associado ao próprio ativo e moeda sem FX

### Requirement: Rentabilidade percentual por ativo
A análise SHALL manter `rentabilidadePercentual` diretamente como texto complementar de cada ativo na visualização de resultado não realizado. O frontend MUST NOT derivar percentual de outros campos, somar ou calcular média de percentuais, misturar unidade percentual com a escala monetária ou exigir gráfico independente dedicado somente à rentabilidade. Sinal, valor percentual e estado SHALL permanecer compreensíveis sem depender de cor ou hover.

#### Scenario: Rentabilidade positiva
- **WHEN** rentabilidadePercentual="12.34"
- **THEN** a linha do ativo apresenta +12,34% e estado Positivo como complemento do resultado monetário

#### Scenario: Rentabilidade negativa
- **WHEN** rentabilidadePercentual="-12.34"
- **THEN** a linha do ativo apresenta -12,34% e estado Negativo como complemento do resultado monetário

#### Scenario: Rentabilidade zero
- **WHEN** rentabilidadePercentual="0" ou "-0.00"
- **THEN** a linha apresenta 0,00% e Neutro sem marcador percentual fictício

#### Scenario: Resultado e rentabilidade com sinais diferentes
- **WHEN** os campos autoritativos recebidos possuem sinais diferentes
- **THEN** ambos são apresentados fielmente com seus próprios rótulos e estados, sem correção ou reconciliação pelo frontend

#### Scenario: Sem gráfico percentual independente
- **WHEN** o desempenho por ativo é renderizado
- **THEN** a rentabilidade continua disponível por ativo, mas não existe painel ou plot separado dedicado exclusivamente a ela

#### Scenario: Revisão estética da rentabilidade no Bloco 2
- **WHEN** a rentabilidade por ativo é apresentada após a reorganização
- **THEN** ticker, empresa, percentual autoritativo e estado permanecem visíveis junto ao resultado monetário, sem série temporal, plot próprio, corte de extremos ou request novo

### Requirement: Integrar sem alterar o histórico e os estados existentes
A análise SHALL ser dividida conceitualmente em Distribuição da carteira e Desempenho por ativo, depois da Evolução patrimonial e antes dos Resultados realizados. SHALL preservar contexto global, navegação, query params, resultados realizados e carregamento/erro independente da evolução. A tabela completa de posições SHALL permanecer no detalhe da Carteira, mas MUST NOT ser duplicada no Dashboard. O Histórico do patrimônio SHALL continuar representando snapshots manuais com o mesmo dataset, gaps, moedas, timestamps, tooltip, teclado/toque e POST explícito.

#### Scenario: Troca de contexto durante carga
- **WHEN** a Carteira muda enquanto uma resposta anterior está pendente
- **THEN** distribuição e desempenho anteriores deixam de ser atuais e respostas antigas não contaminam a nova Carteira

#### Scenario: Loading ou erro financeiro
- **WHEN** o bloco financeiro carrega ou falha
- **THEN** distribuição e desempenho não apresentam dados fictícios ou antigos e não desmontam nem bloqueiam a evolução independente

#### Scenario: Histórico sem posições atuais
- **WHEN** posições atuais estão vazias mas existem snapshots patrimoniais anteriores
- **THEN** a evolução continua integralmente disponível e não é convertida em posições, retorno ou cotação histórica

#### Scenario: Detalhamento fora do Dashboard
- **WHEN** o usuário precisa consultar quantidade, preço médio, custo, cotação e demais campos da posição
- **THEN** usa o detalhe da Carteira, sem tabela equivalente no Dashboard

#### Scenario: Atualização e registro manual
- **WHEN** Atualizar dados é acionado ou uma visualização analítica é apresentada
- **THEN** nenhum snapshot é criado; somente Registrar patrimônio atual pode executar o POST manual existente

### Requirement: Linguagens visuais adequadas às perguntas
Distribuição SHALL usar a composição existente para comunicar onde o valor atual está alocado. Desempenho SHALL usar uma única apresentação divergente baseada em resultado não realizado monetário, acompanhada da rentabilidade percentual textual por ativo. Títulos, rótulos, ticker, empresa, moeda, valor, percentual e estado SHALL permanecer próximos à visualização e disponíveis sem hover. A análise MUST NOT apresentar comparativo Custo × Valor atual, plot percentual independente, série temporal, ranking que reordene ou elimine posições, interatividade desnecessária ou significado exclusivamente cromático.

#### Scenario: Duas perguntas complementares
- **WHEN** Distribuição e Desempenho estão visíveis
- **THEN** é possível distinguir onde o patrimônio está alocado e quais posições contribuem positiva ou negativamente, com valor monetário principal e percentual complementar

#### Scenario: Três leituras complementares
- **WHEN** a antiga composição de três comparativos é substituída
- **THEN** custo permanece no resumo/detalhe, distribuição comunica alocação e desempenho comunica ganho ou perda monetária com percentual complementar, sem ocultar informação contratual

#### Scenario: Sem série histórica de rentabilidade
- **WHEN** a rentabilidade atual é apresentada
- **THEN** ela permanece associada à posição atual e não é derivada da evolução patrimonial

#### Scenario: Sem série histórica
- **WHEN** a rentabilidade atual é apresentada
- **THEN** o percentual representa a posição atual, sem eixo temporal ou derivação dos snapshots patrimoniais

#### Scenario: Sem comparativo redundante
- **WHEN** a análise reorganizada é avaliada
- **THEN** custo e valor atual não possuem gráfico próprio, permanecendo disponíveis nos contratos e destinos que já os apresentam

#### Scenario: Forma acessível
- **WHEN** cor ou desenho não pode ser percebido
- **THEN** textos, sinais, estados e unidades permitem compreender distribuição e desempenho sem conteúdo financeiro oculto

#### Scenario: Forma distinta sem métrica nova
- **WHEN** distribuição e desempenho são avaliados
- **THEN** rosca e eixo divergente mantêm perguntas distintas, enquanto a rentabilidade textual complementa o resultado sem criar métrica, ranking ou eixo incompatível

## REMOVED Requirements

### Requirement: Comparar custo e valor atual por ativo

**Reason**: A visualização independente repete custo e valor atual já disponíveis no resumo, na composição e no detalhe da Carteira, enquanto sua diferença conceitual já é comunicada pelo resultado não realizado.

**Migration**: Remover somente o bloco Custo × Valor atual do Dashboard. Preservar `custoPosicao`, `valorAtualPosicao`, endpoint, modelos, helpers ainda utilizados e apresentações existentes no resumo, composição e detalhe da Carteira; não migrar nem recalcular dados.
