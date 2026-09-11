## MODIFIED Requirements

### Requirement: Comparar custo e valor atual por ativo
Cada posição SHALL apresentar uma barra horizontal de Valor atual e uma marca de referência de Custo no mesmo eixo, diretamente de valorAtualPosicao e custoPosicao, com os dois labels e valores textuais sempre disponíveis. A referência SHALL continuar distinguível quando coincidir com o fim da barra e MUST NOT representar meta ou retorno calculado. A escala linear SHALL partir de zero e ser comum às duas séries de todos os ativos de uma mesma moeda. Comprimentos MUST NOT ser normalizados separadamente por ativo, empilhados como soma ou inflados por mínimo visual.

#### Scenario: Custo igual ao valor atual
- **WHEN** custoPosicao="100.00" e valorAtualPosicao="100.00"
- **THEN** a referência de custo coincide com o fim da barra de valor atual, permanece visível por tratamento não exclusivamente cromático e os dois valores textuais aparecem sem deslocamento artificial

#### Scenario: Valor atual maior ou menor
- **WHEN** duas posições BRL têm custo/valor de 100/120 e 100/80
- **THEN** as referências de custo ficam na mesma coordenada e as barras de valor correspondem proporcionalmente a 120 e 80 sem calcular resultado novo

#### Scenario: Valor atual zero
- **WHEN** custoPosicao="100" e valorAtualPosicao="0"
- **THEN** o custo continua visível e o valor atual é explicitamente zero, sem barra mínima que sugira valor positivo

#### Scenario: Entrega visual incremental do Bloco 1
- **WHEN** o Bloco 1 desta expansão refina Custo × Valor atual junto aos comparativos existentes
- **THEN** barra e marca de referência têm legenda curta, labels explícitos e valores completos, sem aparência de campos/progress bars, sem Variação calculada e sem placeholders de composição ainda não implementada

#### Scenario: Linguagem simples
- **WHEN** os grupos monetários são apresentados
- **THEN** títulos BRL/USD e descrição simples explicam investimento e valor atual, sem explicações técnicas sobre escala na interface

#### Scenario: Valores muito próximos
- **WHEN** custo e valor atual são 1566 e 1569 no mesmo grupo
- **THEN** a distância entre referência e extremidade permanece proporcionalmente pequena, ambos os valores são legíveis e a escala não é truncada para exagerar a diferença


## ADDED Requirements

### Requirement: Apresentação financeira amigável e exata
Os gráficos do Dashboard SHALL apresentar zeros simples, negativos e científicos como zero monetário/percentual amigável, inclusive nos suplementos, legendas e tooltips, sem alterar tokens autoritativos. MUST NOT mostrar notação E/e desnecessária como 0E-12 USD. Valores não nulos abaixo da precisão usual SHALL preservar sinal/estado real e representação textual exata acessível. Expansão limitada ou notação matemática compacta exata SHALL impedir perda de dígitos e consumo desproporcional para expoentes extremos; aproximação geométrica MUST NOT alimentar labels.

#### Scenario: Zero científico e negativo
- **WHEN** o valor é 0E-12, -0.00 ou 0e9999
- **THEN** apresenta R$ 0,00, US$ 0,00 ou 0,00% conforme a unidade, sem suplemento técnico redundante; a string original permanece intacta

#### Scenario: Não zero pequeno
- **WHEN** o valor recebido é -0.000000000001
- **THEN** a interface mantém Negativo e valor completo decimal localizado, sem classificá-lo como Neutro porque o formato usual arredonda

#### Scenario: Precisão extensa
- **WHEN** o valor é 9007199254740993.01 ou científico não nulo
- **THEN** dígitos exatos ficam disponíveis em texto sem Number/parseFloat financeiro; notação matemática compacta só aparece se necessária para limitar expansão, sem ocultar precisão

### Requirement: Linguagens visuais adequadas às perguntas
Custo versus valor SHALL distinguir valor atual da referência de custo; Resultado não realizado SHALL manter barras divergentes e Rentabilidade SHALL manter dot plot estático percentual. Cada gráfico SHALL comunicar sua pergunta com título/legenda curta, ticker, empresa, unidade e valores próximos da visualização, sem exigir explicação técnica ou hover. MUST NOT trocar tipos apenas por variedade, depender somente de cor ou criar interatividade/foco desnecessários. BRL/USD e escalas atuais SHALL permanecer independentes.

#### Scenario: Três leituras complementares
- **WHEN** os três comparativos estão visíveis
- **THEN** é possível distinguir custo/valor, ganho/perda monetária e desempenho percentual pela forma e pelos textos, com sinais e estados Positivo/Negativo/Neutro e zero explícitos

#### Scenario: Sem série histórica
- **WHEN** a rentabilidade atual é apresentada
- **THEN** os pontos representam posições atuais, sem eixo temporal ou derivação a partir do Histórico do patrimônio

#### Scenario: Forma distinta sem métrica nova
- **WHEN** a diferenciação dos comparativos é avaliada
- **THEN** custo usa barra com marca, resultado usa origem divergente e rentabilidade usa pontos sem barras preenchidas; legenda e forma identificam perguntas distintas, preservando valores autoritativos e sem ranking que elimine ou reordene posições nesta versão
