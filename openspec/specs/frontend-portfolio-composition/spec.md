# frontend-portfolio-composition Specification

## Purpose
Apresentar a distribuição visual do valor atual das posições abertas dentro de cada moeda, com dados autoritativos acessíveis e proporções exclusivamente geométricas.

## Requirements

### Requirement: Composição com fonte autoritativa e moedas independentes
A composição SHALL consumir somente valorAtualPosicao e identificação das posições já carregadas no Dashboard. SHALL apresentar uma rosca por moeda presente com legenda integral de ticker, empresa e valor monetário autoritativo. MUST NOT criar HTTP, FX, soma monetária, total global, percentual textual derivado, persistência de participação ou métrica financeira nova. A visualização SHALL responder onde está concentrado o valor atual das posições, não saldo de caixa ou retorno.

#### Scenario: BRL e USD
- **WHEN** posições das duas moedas coexistem
- **THEN** cada rosca/legenda é identificada pela moeda, com proporções internas próprias e sem comparação de montantes entre moedas

#### Scenario: Uma moeda e um ativo
- **WHEN** há somente uma posição de valor positivo em USD
- **THEN** existe apenas rosca USD, preenchida por essa posição e acompanhada de seu valor recebido, sem grupo BRL artificial ou percentual publicado

#### Scenario: Mesma coleção e contexto
- **WHEN** a carteira troca, carrega, falha ou retorna posições vazias
- **THEN** a composição acompanha os estados existentes sem dados antigos, placeholders financeiros ou request próprio; o histórico permanece independente

### Requirement: Proporções limitadas ao desenho
Somente ângulos e coordenadas da composição MAY derivar pesos normalizados dos valores positivos dentro da mesma moeda. Aproximações SHALL permanecer isoladas de labels, tooltip, modelos, payloads, persistência e outras métricas. A geometria MUST NOT substituir as strings originais nem reutilizar um resumo obtido em instante diferente como denominador. Proporções iguais SHALL produzir ângulos equivalentes sem ângulo mínimo artificial.

#### Scenario: Valores iguais e proporcionais
- **WHEN** valores do grupo são 100, 100 e 200
- **THEN** os dois primeiros segmentos têm ângulos iguais e o terceiro o dobro, enquanto a legenda apresenta exclusivamente os valores recebidos

#### Scenario: Extremos científicos
- **WHEN** magnitudes muito grandes e muito pequenas coexistem
- **THEN** a geometria permanece finita e cada posição mantém texto exato; segmentos subpixel não são ampliados para simular participação e seu limite visual é informado sem apagar ativos

### Requirement: Zeros e dados indisponíveis sem participação fictícia
Posições zero SHALL permanecer na legenda sem segmento positivo. Um grupo todo zero SHALL mostrar ausência de valor para distribuir sem rosca preenchida. Valores negativos ou inválidos SHALL manter diagnóstico textual e tornar a composição daquele grupo indisponível, sem módulo, omissão silenciosa ou renormalização de subconjunto. Grupos válidos independentes SHALL continuar disponíveis.

#### Scenario: Zeros misturados
- **WHEN** um grupo tem uma posição positiva e outra zero científico
- **THEN** só a positiva ocupa o anel e ambas constam na legenda, com zero apresentado amigavelmente e sem alterar o token original

#### Scenario: Todos zero
- **WHEN** todos os valores do grupo são zero
- **THEN** aparece Sem valor para distribuir com os itens preservados, sem divisão por zero ou fatias iguais inventadas

#### Scenario: Valor inválido ou negativo
- **WHEN** um componente não pode representar participação não negativa
- **THEN** o grupo informa composição indisponível, preserva informação textual e não exibe uma rosca falsamente completa dos demais ativos

### Requirement: Composição acessível com dataset integral
A legenda SHALL preservar todos os ativos na ordem recebida, com nomes/valores completos e identificação textual que não dependa exclusivamente da cor das fatias. MUST NOT aplicar top-N, Outros agregado, amostragem, truncamento do dataset ou foco por fatia sem necessidade. Os valores essenciais SHALL estar disponíveis sem hover/toque. Marcadores de legenda e setores SHALL permitir associação visual sem deslocar ângulos financeiros.

#### Scenario: Muitos ativos
- **WHEN** há 100 posições, nomes/tickers longos e fatias pequenas
- **THEN** a legenda conserva os 100 itens com valores legíveis, identificadores/padrões complementares à cor, sem scroll horizontal obrigatório ou mínimos artificiais

#### Scenario: Tecnologia assistiva
- **WHEN** a composição é lida sem o desenho
- **THEN** moeda, ativo e valor recebido são compreensíveis na legenda sem exigir tooltip, com gráfico decorativo fora da navegação por foco

#### Scenario: Rosca essencial e centro fiel
- **WHEN** a composição P0 é apresentada
- **THEN** usa rosca, com moeda/finalidade no centro e valores autoritativos na legenda, sem total calculado, percentual derivado exibido ou patrimonioAtual do resumo tratado como total sincronizado das fatias
