# frontend-position-analysis Specification

## Purpose

Permitir comparar custo, valor atual, resultado não realizado e rentabilidade das posições abertas de uma Carteira com visualizações acessíveis, preservando valores autoritativos e moedas independentes.

## Requirements

### Requirement: Fonte única das posições e identificação completa
A análise SHALL consumir somente as posições já carregadas para a Carteira atual. Cada ativo SHALL manter acaoId, ticker, nomeEmpresa e moeda identificáveis, com ordem recebida preservada dentro de cada moeda. Quantidade de itens MUST NOT causar top-N, amostragem, agregação, omissão ou duplicação de dados. Nenhum gráfico SHALL solicitar HTTP, recomputar campos financeiros ou enriquecer identificadores.

#### Scenario: Carteira sem posições
- **WHEN** a Carteira válida retorna posicoes=[]
- **THEN** a análise apresenta um estado sem posições abertas, sem barras ou moedas artificiais, e o Histórico do patrimônio continua disponível

#### Scenario: Um ativo
- **WHEN** existe uma posição PETR4 em BRL
- **THEN** os três gráficos apresentam exatamente esse ativo e seus campos, sem sugerir tendência temporal

#### Scenario: Múltiplos ativos
- **WHEN** são recebidos três ativos em determinada ordem
- **THEN** todos aparecem uma vez em cada gráfico na ordem relativa recebida em seu grupo, sem classificação automática

#### Scenario: Muitos ativos
- **WHEN** são recebidas 100 posições com nomes extensos
- **THEN** todas permanecem identificáveis com valores textuais completos e linhas legíveis, sem truncar a coleção nem comprimir a altura de cada linha

#### Scenario: Sem recomputação financeira
- **WHEN** uma fixture retorna custoPosicao="100", valorAtualPosicao="120", resultadoNaoRealizado="7" e rentabilidadePercentual="3"
- **THEN** as visualizações usam respectivamente 100/120, 7 e 3 recebidos, sem reconciliar a fixture por diferença, multiplicação ou divisão financeira

#### Scenario: Quantidades fracionárias
- **WHEN** quantidadeAtual="1.23456789" e precoMedio possuem precisão extensa
- **THEN** suas strings permanecem intactas e a análise usa custoPosicao/valorAtualPosicao recebidos sem multiplicar quantidade por preço

#### Scenario: Sem HTTP adicional
- **WHEN** os gráficos são montados, redimensionados ou reapresentados
- **THEN** nenhum GET, POST ou consulta por ativo é disparado para construí-los

### Requirement: Moedas e unidades independentes
Valores monetários SHALL ser agrupados por moeda recebida, com BRL/R$ e USD/US$ explícitos, sem FX, soma entre moedas ou escala monetária compartilhada. Rentabilidade SHALL ter grupos identificados por moeda e escalas percentuais independentes por moeda, sem inferência monetária, soma ou média de percentuais. Cada escala SHALL identificar sua unidade e seu âmbito de comparação.

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
- **THEN** cada ativo permanece no grupo da sua moeda, com escala percentual própria desse grupo, sem comparar comprimentos entre moedas ou inferir igualdade monetária

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

### Requirement: Resultado não realizado divergente
A visualização SHALL utilizar resultadoNaoRealizado diretamente, com eixo de zero central e escala linear simétrica por moeda. Valores negativos SHALL estender-se à esquerda e positivos à direita. Zero SHALL ser identificado como Neutro sem largura financeira artificial. Texto monetário e estado Positivo/Negativo/Neutro SHALL permanecer visíveis.

#### Scenario: Lucro não realizado
- **WHEN** resultadoNaoRealizado="25.00"
- **THEN** a barra fica à direita de zero e a linha informa Positivo e o valor monetário recebido

#### Scenario: Prejuízo não realizado
- **WHEN** resultadoNaoRealizado="-25.00" no mesmo grupo que um resultado positivo de 25
- **THEN** a barra fica à esquerda, tem igual comprimento absoluto e informa Negativo e seu valor

#### Scenario: Resultado zero
- **WHEN** resultadoNaoRealizado="0.000" ou "-0.00"
- **THEN** a linha informa Neutro e zero, sem barra positiva ou negativa fictícia

#### Scenario: Todos os resultados neutros
- **WHEN** todos os resultados do grupo são zero
- **THEN** o eixo de zero e as linhas continuam disponíveis sem divisão por zero ou extremos monetários inventados

### Requirement: Rentabilidade percentual por ativo
A visualização SHALL usar diretamente rentabilidadePercentual em dot plot estático divergente, com escala linear percentual própria por moeda e simétrica em torno de zero. MUST NOT derivar percentual de outros campos, misturar eixo percentual e monetário ou produzir rentabilidade agregada.

#### Scenario: Rentabilidade positiva
- **WHEN** rentabilidadePercentual="12.34"
- **THEN** o marcador fica ? direita de 0% com texto percentual +12,34% e estado Positivo

#### Scenario: Rentabilidade negativa
- **WHEN** rentabilidadePercentual="-12.34"
- **THEN** o marcador fica ? esquerda de 0% com texto -12,34% e estado Negativo

#### Scenario: Rentabilidade zero
- **WHEN** rentabilidadePercentual="0"
- **THEN** o marcador fica exatamente em 0%, com texto 0,00% e Neutro, sem barra mínima, inclusive quando todo o conjunto é zero

#### Scenario: Revisão est?tica da rentabilidade no Bloco 2
- **WHEN** a rentabilidade por ativo ? apresentada
- **THEN** ticker, empresa, percentual autoritativo e estado permanecem visíveis com pontos proporcionais em grupos BRL/USD independentes; não há série temporal, corte de extremos, novos requests ou interpretação do patrimônio como retorno

### Requirement: Preservar precisão e limitar aproximação à geometria
Strings financeiras autoritativas SHALL permanecer integrais e alimentar formatadores existentes. Somente razões normalizadas e coordenadas geométricas MAY usar aproximação numérica; MUST NOT substituir modelos, labels, valores completos, sinal, zero ou payloads. Valores extensos, científicos e diferenças de magnitude SHALL ser tratados sem Infinity/NaN, desaparecimento de ativos ou largura mínima falsa. Comparação de magnitudes para escala não constitui autorização para recomputar finanças.

#### Scenario: Decimal além da precisão binária
- **WHEN** custoPosicao="9007199254740993.01" e valorAtualPosicao="9007199254740993.02"
- **THEN** ambos os textos preservam a diferença recebida mesmo que os comprimentos de barra sejam visualmente indistinguíveis

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
- **WHEN** positivo, negativo, neutro e séries de custo/valor são lidos sem distinguir cores
- **THEN** texto, direção e identificação das séries permitem compreender os dados

#### Scenario: Reflow e texto ampliado
- **WHEN** a página usa 320 CSS px, texto 200%, tablet ou fronteiras 959/960/961px
- **THEN** grupos empilham quando necessário, texto permanece legível e completo, e não há scroll horizontal obrigatório da página ou representação acessível duplicada

#### Scenario: Reduced motion e baixa altura
- **WHEN** a preferência de movimento reduzido está ativa ou a viewport tem baixa altura
- **THEN** os gráficos não dependem de animação e todo conteúdo permanece alcançável na rolagem da página sem foco encoberto

### Requirement: Integrar sem alterar o histórico e os estados existentes
A análise SHALL ocupar a seção após indicadores e antes do Histórico do patrimônio. MUST preservar contexto global, navegação, query params, posições completas, resultados realizados e carregamento/erro independentes da evolução. Histórico do patrimônio SHALL continuar como registros manuais ao longo do tempo, com mesmo dataset, gaps, moedas, timestamps, tooltip, teclado/toque e POST manual, sem virar gráfico de cotação ou ser substituído.

#### Scenario: Troca de contexto durante carga
- **WHEN** a Carteira muda enquanto uma resposta anterior está pendente
- **THEN** barras anteriores deixam de ser atuais e respostas antigas não contaminam a análise da nova Carteira

#### Scenario: Loading ou erro financeiro
- **WHEN** o bloco financeiro carrega ou falha
- **THEN** a análise não apresenta barras fictícias ou antigas, reutiliza o estado atual e não desmonta nem bloqueia o histórico independente

#### Scenario: Histórico sem posições atuais
- **WHEN** posições atuais estão vazias mas existem registros patrimoniais anteriores
- **THEN** esses registros continuam integralmente disponíveis e não são convertidos em posições ou cotações

#### Scenario: Atualização e registro manual
- **WHEN** Atualizar dados é acionado ou um gráfico comparativo é apresentado
- **THEN** nenhum snapshot é criado; somente Registrar patrimônio atual pode executar o POST manual existente

### Requirement: Preservar carregamento lazy e limites de performance
A análise MUST NOT instalar bibliotecas, elevar budgets, promover código de Dashboard ao initial ou reduzir posições para economizar recursos. O initial de produção SHALL ser comparado no mesmo ambiente/configuração ao checkpoint de 514418 bytes e não superá-lo sem nova decisão explícita. O crescimento do chunk lazy e CSS SHALL ser medido e justificado; a dispensa histórica de bundle não autoriza crescimento automático nesta change.

#### Scenario: Build comparável
- **WHEN** a implementação for validada em production
- **THEN** initial, transferência, Dashboard lazy, estilos e warnings são registrados contra o checkpoint, com dependências e budgets intactos

#### Scenario: Carregamento inicial e dataset
- **WHEN** o shell carrega antes da feature Dashboard
- **THEN** componentes dos gráficos permanecem no limite lazy e nenhuma posição é descartada como otimização

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
