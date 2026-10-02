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

### Requirement: Moedas e unidades independentes
Valores monetários SHALL ser agrupados por moeda recebida, com BRL/R$ e USD/US$ explícitos, sem FX, soma entre moedas ou escala monetária compartilhada. Valores monetários MUST NOT ser somados ou usados em médias entre moedas, nem convertidos implicitamente. Rentabilidade SHALL ser apresentada por ativo como informação textual complementar do desempenho monetário, identificada com sua unidade percentual e associada ao grupo da moeda correspondente. Percentuais de ativos de moedas distintas SHALL permanecer semanticamente independentes, sem inferência de equivalência monetária, soma ou média de percentuais. A apresentação da rentabilidade MUST NOT exigir escala percentual gráfica, barras percentuais, séries percentuais ou plot percentual. Cada escala monetária SHALL identificar sua unidade e seu âmbito de comparação.

#### Scenario: Somente BRL
- **WHEN** todas as posições são BRL
- **THEN** os gráficos exibem somente o grupo BRL e seus valores, sem grupo USD vazio artificial

#### Scenario: Somente USD
- **WHEN** todas as posições são USD
- **THEN** os gráficos exibem somente o grupo USD e seus valores, sem conversão para reais

#### Scenario: BRL e USD simultâneos
- **WHEN** posições BRL e USD coexistem
- **THEN** gráficos monetários usam grupos e escalas independentes claramente identificados, sem comparação de comprimento como equivalência cambial

#### Scenario: Percentuais em moedas distintas
- **WHEN** um ativo BRL e um USD têm rentabilidadePercentual="5"
- **THEN** cada ativo permanece no grupo da sua moeda, com sua rentabilidade de 5% apresentada textualmente como complemento do desempenho monetário; os percentuais permanecem semanticamente independentes, sem escala ou comparação gráfica percentual, soma, média, conversão cambial implícita ou inferência de igualdade monetária

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

### Requirement: Preservar precisão e limitar aproximação à geometria
Strings financeiras autoritativas SHALL permanecer integrais e alimentar formatadores existentes. Somente razões normalizadas e coordenadas geométricas MAY usar aproximação numérica; MUST NOT substituir modelos, labels, valores completos, sinal, zero ou payloads. Valores extensos, científicos e diferenças de magnitude SHALL ser tratados sem Infinity/NaN, desaparecimento de ativos ou largura mínima falsa. Comparação de magnitudes para escala não constitui autorização para recomputar finanças.

#### Scenario: Decimal além da precisão binária
- **WHEN** duas posições da mesma moeda retornam resultadoNaoRealizado="9007199254740993.01" e resultadoNaoRealizado="9007199254740993.02", respectivamente
- **THEN** ambos os textos preservam a diferença recebida mesmo que os comprimentos das barras de resultado não realizado sejam visualmente indistinguíveis

#### Scenario: Extremos científicos
- **WHEN** uma fixture contém valores "1e400" e "1e-400"
- **THEN** todas as linhas e valores completos permanecem disponíveis e a geometria usa coordenadas finitas limitadas, sem converter overflow em zero ou remover o ativo

#### Scenario: Valor abaixo da resolução visual
- **WHEN** um valor não nulo é pequeno demais para produzir comprimento distinguível na escala
- **THEN** a linha identifica o valor não nulo e a limitação visual, preserva seu texto completo e não inventa comprimento mínimo

#### Scenario: Arredondamento textual usual
- **WHEN** um valor recebido não nulo tem casas que o formatador usual arredonda para 0,00
- **THEN** o estado usa o sinal original e o valor lossless completo permanece visível com unidade, sem classificar o dado como Neutro por arredondamento

#### Scenario: Dados de origem imutáveis
- **WHEN** gráficos são construídos e redimensionados a partir de uma coleção congelada
- **THEN** nenhuma string, ordem de origem ou objeto financeiro é modificado, e nenhuma coordenada retorna ao contrato financeiro

### Requirement: Gráficos acessíveis sem interação desnecessária
Cada gráfico SHALL oferecer uma estrutura textual semântica sempre visível com ativo, moeda, série, valor e estado aplicáveis. A representação gráfica complementar MUST NOT acrescentar foco, tab stops ou exigir hover/clique/toque para revelar dados. Cores SHALL ser complementadas por rótulos, sinais e tratamento de séries. Valores completos MUST NOT ser truncados. Contraste, headings e leitura assistiva SHALL seguir os critérios visuais vigentes.

#### Scenario: Leitura sem visão do gráfico
- **WHEN** o usuário usa leitor de tela ou consulta somente o texto
- **THEN** compreende cada ativo/série/valor/estado sem ler SVG decorativo duplicado ou depender de tooltip

#### Scenario: Teclado
- **WHEN** o usuário percorre o Dashboard por Tab
- **THEN** barras comparativas não recebem foco e os controles existentes permanecem operáveis na ordem prevista

#### Scenario: Cor indisponível
- **WHEN** resultados monetários positivos, negativos e neutros e suas rentabilidades percentuais complementares são lidos sem distinguir cores
- **THEN** rótulos, valores, sinais e estados textuais identificam separadamente resultado monetário e rentabilidade complementar; a direção das barras monetárias complementa essa informação sem exigir desenho ou cor

#### Scenario: Reflow e texto ampliado
- **WHEN** a página usa 320 CSS px, texto 200%, tablet ou fronteiras 959/960/961px
- **THEN** grupos empilham quando necessário, texto permanece legível e completo, e não há scroll horizontal obrigatório da página ou representação acessível duplicada

#### Scenario: Reduced motion e baixa altura
- **WHEN** a preferência de movimento reduzido está ativa ou a viewport tem baixa altura
- **THEN** os gráficos não dependem de animação e todo conteúdo permanece alcançável na rolagem da página sem foco encoberto

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
