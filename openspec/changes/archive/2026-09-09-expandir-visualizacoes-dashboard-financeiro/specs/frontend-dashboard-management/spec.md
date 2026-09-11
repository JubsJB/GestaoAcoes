## MODIFIED Requirements

### Requirement: Hierarquia financeira do Dashboard
O Dashboard SHALL apresentar, nesta ordem visual e semântica, cabeçalho/contexto compacto, ações existentes, indicadores por moeda, Análise por ativo (Composição por moeda, Custo × Valor atual, Resultado não realizado, Rentabilidade), Histórico do patrimônio, posições abertas e resultados realizados por Ação. A reorganização MUST preservar o carregamento e tratamento de erro independentes da evolução, o consumo do contexto global de Carteira, query params, atualização e registro manual existentes. O seletor SHALL permanecer exclusivamente no shell; o Dashboard MUST NOT duplicar seleção ou listagem de Carteiras.

#### Scenario: Evolução independente
- **WHEN** a evolução está carregando ou falha enquanto as demais seções têm conteúdo
- **THEN** os outros dados permanecem utilizáveis e a evolução comunica seu próprio estado sem bloquear a página

#### Scenario: Ordem coerente
- **WHEN** a página é percorrida visualmente ou por headings
- **THEN** identificação do contexto global e ações precedem indicadores, análise por ativo e Histórico do patrimônio, seguidos por posições e resultados, sem seletor local duplicado

#### Scenario: Análise derivada da coleção carregada
- **WHEN** as posições da Carteira atual estão disponíveis
- **THEN** a análise usa essa mesma coleção sem consulta adicional e sem condicionar montagem ou estado independente do Histórico do patrimônio ao seu próprio conteúdo

#### Scenario: Composição visual do Bloco 1
- **WHEN** o Dashboard apresenta o Bloco 1 da expansão, antes da composição nova
- **THEN** contexto e ações compactos precedem indicadores com patrimônio destacado, os três comparativos existentes refinados na análise, Histórico do patrimônio, posições e resultados; a composição ainda não implementada não recebe placeholder

#### Scenario: Modernização visual do histórico
- **WHEN** grid decorativo, linha e espaçamento são refinados
- **THEN** coordenadas, dataset, gaps, registros vazios, IDs, timestamps, moedas, tooltip, teclado, toque, Escape e ação manual são preservados, com gráficos lado a lado quando legíveis e histórico textual único abaixo


#### Scenario: Integração desktop do Bloco 2
- **WHEN** a análise apresenta os três gráficos autorizados
- **THEN** Custo × Valor atual precede Resultado não realizado e Rentabilidade por ativo, com resultado e rentabilidade lado a lado quando legíveis, preservando a hierarquia relativa, consultas e as seções de detalhe

## ADDED Requirements

### Requirement: Composição desktop orientada pelas referências financeiras
O Dashboard SHALL manter linguagem visual da referência A do usuário para organização, cards, espaço, densidade e distribuição; a referência B SHALL orientar leitura, tipos, valores, legendas e tooltips. SHALL preservar identidade verde/neutra, tipografia e navegação do projeto, sem copiar marcas. Cada gráfico SHALL responder a uma pergunta financeira clara, com valores textuais disponíveis e pouca explicação técnica. O desktop SHALL priorizar gráficos após indicadores, antes de tabelas/detalhes, sem card por ativo, repetição de painéis equivalentes ou decoração sem função informacional.

#### Scenario: Composição ampla
- **WHEN** a largura útil comporta os painéis com texto legível
- **THEN** composição e custo/valor compartilham a primeira faixa analítica com maior espaço para o comparativo, resultado/rentabilidade compartilham a segunda e o histórico ocupa largura total com gráficos por moeda internamente

#### Scenario: Espaço insuficiente
- **WHEN** tablet/mobile ou ampliação impedem as colunas previstas
- **THEN** painéis empilham na ordem semântica, sem overflow estrutural, corte de informação, redução artificial de fonte ou mudança de contexto; todos os detalhes continuam disponíveis

#### Scenario: Revisão das referências
- **WHEN** a composição desktop é submetida ao aceite humano
- **THEN** a avaliação considera as referências A/B fornecidas, pergunta de cada gráfico, hierarquia, densidade e distinção visual; a falta de acesso às imagens deve ser declarada em vez de presumir comparação executada

#### Scenario: Variedade real no desktop final
- **WHEN** o Bloco 3 é revisado
- **THEN** o conjunto P0 contém barra/referência, barras divergentes, dot plot, rosca e linha temporal ampla, além dos indicadores; mudanças apenas de CSS entre painéis semelhantes não satisfazem a diferenciação, e tabelas permanecem disponíveis abaixo
