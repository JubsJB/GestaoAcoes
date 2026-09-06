## MODIFIED Requirements

### Requirement: Resumo por moeda e contagem estrutural
Para cada item de `resumos`, o Dashboard SHALL exibir patrimônio atual, custo total das posições, resultado não realizado total e rentabilidade percentual. A página MAY exibir a quantidade de posições abertas como o tamanho da coleção de posições retornada, mas SHALL identificá-la explicitamente como informação estrutural e não como cálculo financeiro. O patrimônio SHALL ter maior protagonismo visual que os demais indicadores; a composição MUST NOT exigir quatro cards idênticos. BRL e USD SHALL continuar separados. A faixa MUST NOT acrescentar resultado realizado total, variação diária, benchmark ou alocação percentual.

#### Scenario: Cards de resumo
- **WHEN** o backend retorna um ou mais resumos por moeda
- **THEN** cada moeda possui seus próprios indicadores sem combinação com outro item

#### Scenario: Quantidade de posições abertas
- **WHEN** a coleção de posições é recebida
- **THEN** sua quantidade pode ser apresentada com o rótulo “posições abertas” sem derivar qualquer valor financeiro

#### Scenario: Hierarquia por moeda
- **WHEN** um resumo é apresentado
- **THEN** patrimônio, custo, resultado não realizado e rentabilidade permanecem identificáveis, com patrimônio em destaque e sem métrica derivada nova


### Requirement: Posições abertas responsivas
O Dashboard SHALL apresentar, para cada posição devolvida, ticker, empresa, mercado, moeda, quantidade atual, preço médio, cotação atual, valor atual, resultado não realizado e rentabilidade. A apresentação SHALL permanecer legível em viewport compacto e MUST NOT oferecer rota inexistente de detalhe de posição. No desktop, posições SHALL usar tabela semântica com texto à esquerda, números à direita, tipografia tabular e ações existentes em posição estável; no mobile SHALL usar cards completos, conforme a representação acessível única de frontend-visual-experience. Custo e referência temporal da cotação já apresentados SHALL ser preservados quando disponíveis no contrato atual.

#### Scenario: Posição brasileira
- **WHEN** uma posição de mercado BRASIL é retornada
- **THEN** seus dados e valores em BRL são apresentados sem recalculá-los

#### Scenario: Posição americana
- **WHEN** uma posição de mercado EUA é retornada
- **THEN** seus dados e valores em USD são apresentados sem conversão cambial

#### Scenario: Viewport compacto
- **WHEN** a seção de posições é exibida em tela compacta
- **THEN** todos os dados essenciais continuam disponíveis por tabela responsiva ou representação acessível equivalente sem scroll horizontal obrigatório da página

#### Scenario: Comparação desktop
- **WHEN** há posições para apresentar em largura suficiente
- **THEN** cabeçalhos associados permitem comparar valores e moedas sem alterar a ordem recebida

#### Scenario: Equivalência mobile
- **WHEN** a apresentação passa para cards
- **THEN** nenhum campo ou ação existente é perdido e a tabela inativa não participa do foco nem da árvore acessível


### Requirement: Resultados realizados sem totalização
O Dashboard SHALL exibir cada `ResultadoRealizadoResponse` com ticker, empresa, mercado, moeda e resultado realizado. A página MUST NOT somar a coleção nem inferir um resultado total. Os resultados SHALL usar apresentação comparável por Ação, com identificação em posição estável, valores alinhados e sinal/semântica textual que não dependam apenas de cor.

#### Scenario: Resultados disponíveis
- **WHEN** o backend retorna resultados realizados
- **THEN** cada item é exibido individualmente com sua moeda

#### Scenario: Ausência de resultados
- **WHEN** o backend retorna uma coleção vazia
- **THEN** a seção informa normalmente que ainda não existem resultados realizados

#### Scenario: Comparação de resultados
- **WHEN** mais de uma Ação possui resultado realizado
- **THEN** cada resultado permanece individual, legível e identificado pela própria moeda sem rodapé totalizador


## ADDED Requirements

### Requirement: Hierarquia financeira do Dashboard
O Dashboard SHALL apresentar, nesta ordem visual e semântica, cabeçalho/contexto compacto, ações existentes, indicadores por moeda, evolução patrimonial, posições abertas e resultados realizados por Ação. A reorganização MUST preservar o carregamento e tratamento de erro independentes da evolução, seleção, query params, atualização e registro manual existentes.

#### Scenario: Evolução independente
- **WHEN** a evolução está carregando ou falha enquanto as demais seções têm conteúdo
- **THEN** os outros dados permanecem utilizáveis e a evolução comunica seu próprio estado sem bloquear a página

#### Scenario: Ordem coerente
- **WHEN** a página é percorrida visualmente ou por headings
- **THEN** contexto e ações precedem indicadores e evolução, seguidos por posições e resultados


