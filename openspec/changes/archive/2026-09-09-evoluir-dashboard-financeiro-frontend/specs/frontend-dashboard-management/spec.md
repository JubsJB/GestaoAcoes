## MODIFIED Requirements

### Requirement: Precisão decimal lossless compartilhada
O frontend SHALL preservar como texto os tokens decimais dos campos `BigDecimal` de resumo, posições e resultados realizados. IDs MAY permanecer numéricos, mas valores financeiros autoritativos e sua apresentação textual MUST NOT passar por `Number`, `parseFloat` ou aritmética binária JavaScript. Exclusivamente para coordenadas da análise por ativo, uma representação aproximada separada MAY ser produzida conforme frontend-position-analysis, sem substituir strings, decidir sinal/zero por arredondamento nem alimentar cálculos, labels ou payloads. A infraestrutura lossless SHALL ser compartilhável e MUST NOT acoplar o Dashboard ao parser específico de Operações.

#### Scenario: Decimal longo
- **WHEN** o backend retorna um campo financeiro com mais precisão que a representação segura de `number`
- **THEN** seus dígitos são preservados integralmente no model frontend

#### Scenario: Valor negativo
- **WHEN** o backend retorna resultado ou rentabilidade negativa
- **THEN** o sinal e os dígitos são preservados e apresentados corretamente

#### Scenario: Formatação visual
- **WHEN** um valor financeiro textual é exibido
- **THEN** a formatação ocorre somente na apresentação sem alterar o valor preservado

#### Scenario: Aproximação exclusivamente geométrica
- **WHEN** uma posição é projetada como barra
- **THEN** somente razões e coordenadas visuais usam aproximação; valores textuais, modelos e payloads preservam a fonte lossless integral

### Requirement: Hierarquia financeira do Dashboard
O Dashboard SHALL apresentar, nesta ordem visual e semântica, cabeçalho/contexto compacto, ações existentes, indicadores por moeda, Análise por ativo (Custo × Valor atual, Resultado não realizado, Rentabilidade), Histórico do patrimônio, posições abertas e resultados realizados por Ação. A reorganização MUST preservar o carregamento e tratamento de erro independentes da evolução, o consumo do contexto global de Carteira, query params, atualização e registro manual existentes. O seletor SHALL permanecer exclusivamente no shell; o Dashboard MUST NOT duplicar seleção ou listagem de Carteiras.

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
- **WHEN** o Dashboard apresenta o bloco autorizado
- **THEN** contexto e ações compactos precedem indicadores com patrimônio destacado, somente Custo × Valor atual na análise, Histórico do patrimônio, posições e resultados; gráficos posteriores continuam adiados sem placeholders

#### Scenario: Modernização visual do histórico
- **WHEN** grid decorativo, linha e espaçamento são refinados
- **THEN** coordenadas, dataset, gaps, registros vazios, IDs, timestamps, moedas, tooltip, teclado, toque, Escape e ação manual são preservados, com gráficos lado a lado quando legíveis e histórico textual único abaixo


#### Scenario: Integração desktop do Bloco 2
- **WHEN** a análise apresenta os três gráficos autorizados
- **THEN** Custo × Valor atual precede Resultado não realizado e Rentabilidade por ativo, com os novos painéis lado a lado quando legíveis, sem alterar as demais seções, consultas ou a implementação aprovada do comparativo
