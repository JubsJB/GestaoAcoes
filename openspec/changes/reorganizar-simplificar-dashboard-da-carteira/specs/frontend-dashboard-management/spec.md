## MODIFIED Requirements

### Requirement: Resumo por moeda e contagem estrutural
Para cada item de `resumos`, o Dashboard SHALL exibir patrimônio atual, resultado não realizado total, custo total das posições e rentabilidade percentual. Patrimônio e resultado não realizado SHALL possuir maior protagonismo visual; custo e rentabilidade SHALL permanecer identificáveis como informações complementares. A página MAY exibir a quantidade de posições abertas como o tamanho da coleção retornada, identificando-a como informação estrutural e não cálculo financeiro. BRL e USD SHALL permanecer em grupos independentes, sem conversão, soma ou patrimônio consolidado. A faixa MUST NOT acrescentar resultado realizado total, variação diária, benchmark ou alocação percentual.

#### Scenario: Resumo somente BRL
- **WHEN** o backend retorna resumo somente em BRL
- **THEN** a página apresenta patrimônio, resultado não realizado, custo e rentabilidade no grupo BRL, sem grupo USD artificial

#### Scenario: Resumo somente USD
- **WHEN** o backend retorna resumo somente em USD
- **THEN** a página apresenta patrimônio, resultado não realizado, custo e rentabilidade no grupo USD, sem conversão para BRL

#### Scenario: Resumo nas duas moedas
- **WHEN** o backend retorna resumos em BRL e USD
- **THEN** a página apresenta os quatro indicadores em grupos distintos e não produz total combinado

#### Scenario: Cards de resumo
- **WHEN** o backend retorna um ou mais resumos por moeda
- **THEN** cada moeda possui seus próprios indicadores com hierarquia entre informações principais e complementares, sem combinação com outro item

#### Scenario: Hierarquia dos indicadores
- **WHEN** um resumo por moeda é apresentado
- **THEN** patrimônio e resultado não realizado recebem maior destaque que custo e rentabilidade, sem ocultar ou recalcular qualquer indicador

#### Scenario: Hierarquia por moeda
- **WHEN** um resumo é apresentado
- **THEN** patrimônio, resultado não realizado, custo e rentabilidade permanecem identificáveis, sem métrica derivada nova

#### Scenario: Quantidade de posições abertas
- **WHEN** a coleção de posições é recebida
- **THEN** sua quantidade pode ser apresentada como informação estrutural rotulada, sem derivar valor financeiro

#### Scenario: Carteira sem posições
- **WHEN** uma Carteira existente retorna `resumos=[]` e `posicoes=[]`
- **THEN** o Dashboard apresenta estado vazio de carteira sem posições, sem cards monetários artificiais nem tratar a ausência como erro

### Requirement: Posições abertas responsivas
O Dashboard MUST NOT apresentar a tabela ou listagem completa de posições abertas. Quantidade, preço médio, cotação, referência temporal, custo, valor atual, resultado e rentabilidade detalhados SHALL continuar disponíveis no detalhe da Carteira por meio da apresentação existente, sem remover componente compartilhado, endpoint, contrato ou cobertura necessária a esse fluxo. O Dashboard SHALL continuar consumindo a coleção de posições para distribuição, desempenho e contagem estrutural, sem consulta adicional ou recálculo.

#### Scenario: Dashboard com posições abertas
- **WHEN** a Carteira selecionada possui uma ou mais posições
- **THEN** o Dashboard usa a coleção nas visualizações resumidas e não renderiza a tabela completa de posições

#### Scenario: Posição brasileira
- **WHEN** uma posição de mercado BRASIL é retornada
- **THEN** seus valores BRL alimentam distribuição e desempenho sem recálculo, enquanto seus campos detalhados permanecem no detalhe da Carteira

#### Scenario: Posição americana
- **WHEN** uma posição de mercado EUA é retornada
- **THEN** seus valores USD alimentam distribuição e desempenho sem conversão, enquanto seus campos detalhados permanecem no detalhe da Carteira

#### Scenario: Viewport compacto
- **WHEN** o Dashboard é exibido em largura compacta
- **THEN** não existe tabela de posições para refluir na página e os blocos resumidos preservam os dados essenciais sem scroll horizontal obrigatório

#### Scenario: Comparação desktop
- **WHEN** há posições e largura suficiente no Dashboard
- **THEN** distribuição e desempenho permitem leitura por ativo sem reproduzir as colunas da tabela detalhada

#### Scenario: Equivalência mobile
- **WHEN** o Dashboard passa para uma coluna
- **THEN** distribuição e desempenho mantêm valores e identificação, e o detalhamento integral continua disponível na página de Carteira

#### Scenario: Detalhe da Carteira preservado
- **WHEN** o usuário aciona Ver carteira e acessa `/carteiras/{id}`
- **THEN** a página de Carteira continua apresentando as posições abertas e todos os campos vigentes com sua responsividade existente

#### Scenario: Contratos preservados
- **WHEN** a tabela deixa de ser usada pelo Dashboard
- **THEN** `GET /carteiras/{id}/posicoes`, seu DTO, o componente compartilhado e demais consumidores permanecem compatíveis

### Requirement: Resultados realizados sem totalização
O Dashboard SHALL manter uma seção secundária de resultados realizados após resumo, evolução, distribuição e desempenho. Cada `ResultadoRealizadoResponse` SHALL apresentar ticker, empresa, mercado, moeda e resultado realizado individual, sem soma, total inferido ou conversão. A apresentação SHALL usar identificação estável e sinal/semântica textual além de cor. Esta seção MUST NOT ser removida até existir mudança aprovada que forneça destino equivalente.

#### Scenario: Resultados disponíveis
- **WHEN** o backend retorna resultados realizados
- **THEN** cada item é exibido individualmente com sua moeda em uma seção de prioridade visual secundária

#### Scenario: Ausência de resultados
- **WHEN** o backend retorna uma coleção vazia
- **THEN** a seção informa normalmente que ainda não existem resultados realizados sem confundir vazio com falha

#### Scenario: Comparação sem totalização
- **WHEN** mais de uma Ação possui resultado realizado
- **THEN** cada resultado permanece identificado e legível sem rodapé totalizador ou conversão entre moedas

#### Scenario: Comparação de resultados
- **WHEN** mais de uma Ação possui resultado realizado
- **THEN** identificação, moeda, valor e estado de cada item permanecem comparáveis em sua seção secundária sem totalização

#### Scenario: Destino futuro não antecipado
- **WHEN** o Dashboard reorganizado é entregue
- **THEN** resultados realizados continuam na página e nenhuma nova visão de Carteira é criada ou simulada por esta change

### Requirement: Experiência acessível do Dashboard
O Dashboard SHALL possuir um único título principal e seções hierárquicas para resumo, evolução patrimonial, distribuição, desempenho por ativo e resultados realizados, além de identificação clara da Carteira ativa. Ações SHALL ter nomes acessíveis e foco visível; loading, vazio e erro SHALL manter textos e regiões dinâmicas apropriados sem deslocar foco indevidamente. Gráficos SHALL possuir representação textual suficiente, MUST NOT depender somente de cor e MUST NOT adicionar interação obrigatória quando os dados puderem ser apresentados estaticamente. Valores monetários e percentuais SHALL indicar unidade, sinal e significado de forma compreensível. Movimento decorativo MUST respeitar `prefers-reduced-motion` e nenhuma informação SHALL depender de animação.

#### Scenario: Hierarquia por tecnologia assistiva
- **WHEN** a página é percorrida por headings ou tecnologia assistiva
- **THEN** o título, contexto e as cinco seções principais são encontrados em ordem coerente, sem heading da tabela de posições removida

#### Scenario: Uso por tecnologia assistiva
- **WHEN** a página é percorrida por tecnologia assistiva
- **THEN** contexto, resumo, evolução, distribuição, desempenho, resultados, loading, vazios e erros possuem estrutura e nomes compreensíveis

#### Scenario: Resultado com semântica não cromática
- **WHEN** resultado ou rentabilidade positiva, negativa ou neutra é apresentado
- **THEN** sinal, valor e texto comunicam o significado independentemente da cor

#### Scenario: Interação por teclado
- **WHEN** o usuário opera a página apenas por teclado
- **THEN** seletor global, ações do cabeçalho, retry, registro de patrimônio e controles existentes possuem foco visível e ordem coerente

#### Scenario: Gráfico compreensível sem desenho
- **WHEN** SVG, cor ou percepção visual do gráfico não está disponível
- **THEN** moeda, ativo, resultado monetário e rentabilidade complementar permanecem compreensíveis em texto

#### Scenario: Movimento reduzido
- **WHEN** `prefers-reduced-motion` está ativo
- **THEN** a página permanece completa e operável sem depender de animação ou transição para comunicar dados e estados

### Requirement: Hierarquia financeira do Dashboard
O Dashboard SHALL apresentar, nesta ordem visual e semântica: cabeçalho/contexto e ações existentes; resumo por moeda; Evolução patrimonial; Distribuição da carteira; Desempenho por ativo; e Resultados realizados por Ação como seção secundária. Distribuição SHALL responder onde o valor atual está alocado. Desempenho SHALL apresentar resultado não realizado monetário como informação principal e rentabilidade percentual como informação complementar na mesma leitura por ativo. O Dashboard MUST NOT apresentar o comparativo independente Custo × Valor atual, gráfico independente dedicado somente à rentabilidade ou tabela completa de posições. A reorganização SHALL preservar contexto global, `carteiraId`, concorrência, atualização, registro manual de snapshot e tratamento independente da evolução, sem seletor local.

#### Scenario: Ordem coerente
- **WHEN** a página é percorrida visualmente ou por headings
- **THEN** contexto e ações precedem resumo, evolução, distribuição, desempenho e resultados realizados nesta ordem

#### Scenario: Evolução independente
- **WHEN** a evolução carrega ou falha enquanto as demais consultas possuem conteúdo
- **THEN** os demais blocos permanecem utilizáveis e a evolução comunica seu próprio estado sem bloquear a página

#### Scenario: Distribuição preservada
- **WHEN** posições abertas estão disponíveis
- **THEN** a composição por ativo usa a coleção já carregada, separa moedas e permanece antes do desempenho sem request próprio

#### Scenario: Análise derivada da coleção carregada
- **WHEN** as posições da Carteira atual estão disponíveis
- **THEN** distribuição e desempenho usam essa mesma coleção sem consulta adicional nem condicionar o estado independente da evolução

#### Scenario: Desempenho consolidado
- **WHEN** uma posição possui resultado não realizado e rentabilidade percentual
- **THEN** ambos aparecem na mesma leitura do ativo, com o valor monetário em destaque e o percentual como complemento

#### Scenario: Redundâncias ausentes
- **WHEN** o Dashboard reorganizado é renderizado
- **THEN** não existem o bloco Custo × Valor atual, o gráfico percentual independente nem a tabela completa de posições

#### Scenario: Ações e contexto preservados
- **WHEN** existe Carteira válida selecionada
- **THEN** seu nome e as ações Ver carteira, Registrar operação e Atualizar dados permanecem disponíveis sem segundo seletor

#### Scenario: Composição visual do Bloco 1
- **WHEN** o Dashboard reorganizado substitui a composição histórica do Bloco 1
- **THEN** contexto e ações compactos precedem resumo, evolução, distribuição, desempenho e resultados, sem restaurar comparativos ou tabela removidos

#### Scenario: Modernização visual do histórico
- **WHEN** a evolução ocupa sua nova posição na página
- **THEN** coordenadas, dataset, gaps, registros vazios, IDs, timestamps, moedas, tooltip, teclado, toque, Escape, histórico textual e ação manual permanecem preservados

#### Scenario: Integração desktop do Bloco 2
- **WHEN** a antiga análise de três comparativos é substituída no desktop
- **THEN** distribuição precede um único desempenho com resultado e rentabilidade complementar, sem Custo × Valor atual ou painel percentual independente

### Requirement: Composição desktop orientada pelas referências financeiras
O Dashboard SHALL preservar a identidade verde/neutra, tokens, tipografia e linguagem financeira existentes, mas SHALL priorizar leitura analítica com menos painéis equivalentes. Cada bloco SHALL responder a uma pergunta clara e manter valores textuais próximos à visualização. A composição SHALL adaptar-se à largura útil dentro do shell, inclusive quando a sidebar estiver expandida ou recolhida, usando reflow pelo container quando adequado. A página MUST NOT criar scroll horizontal, conteúdo cortado, cards espremidos, fonte artificialmente reduzida ou gráficos ilegíveis em desktop, larguras intermediárias, mobile, zoom aumentado ou aproximadamente 320 CSS px.

#### Scenario: Desktop com sidebar expandida
- **WHEN** o shell desktop reserva a largura da sidebar expandida
- **THEN** cards e painéis usam o espaço restante sem corte nem dependência de recolher a navegação

#### Scenario: Composição ampla
- **WHEN** a largura útil comporta mais de uma coluna com texto legível
- **THEN** grupos de resumo e painéis aproveitam a largura sem alterar a ordem semântica, repetir perguntas financeiras ou esconder equivalentes textuais

#### Scenario: Desktop com sidebar recolhida
- **WHEN** a sidebar é recolhida e a área útil aumenta
- **THEN** os blocos podem aproveitar a largura sem alterar ordem, dados, consultas ou semântica

#### Scenario: Espaço intermediário ou zoom
- **WHEN** a largura real do container diminui por sidebar, viewport ou zoom de 125%, 150% ou 200%
- **THEN** colunas empilham antes de perder legibilidade, sem depender somente do breakpoint global da viewport

#### Scenario: Espaço insuficiente
- **WHEN** tablet, mobile, sidebar ou ampliação impedem as colunas previstas
- **THEN** painéis empilham na ordem semântica sem overflow, corte, redução artificial de fonte ou perda de detalhes

#### Scenario: Mobile e 320 CSS px
- **WHEN** a página é usada em mobile ou aproximadamente 320 CSS px
- **THEN** os blocos reorganizam-se em uma coluna quando necessário, com ações, textos, valores e gráficos completos e sem scroll horizontal da página

#### Scenario: Dataset extenso
- **WHEN** distribuição ou desempenho recebe muitos ativos e nomes ou valores longos
- **THEN** o dataset integral permanece disponível sem top-N, truncamento funcional, scroll interno obrigatório ou sobreposição de elementos

#### Scenario: Revisão das referências
- **WHEN** a composição for submetida ao aceite humano
- **THEN** a avaliação considera perguntas de cada bloco, hierarquia, densidade, responsividade e distinção visual, sem presumir validação não executada

#### Scenario: Variedade real no desktop final
- **WHEN** o Dashboard final é revisado
- **THEN** resumo, linha temporal, rosca e desempenho divergente respondem a perguntas distintas sem exigir comparativo Custo × Valor, plot percentual ou tabela detalhada na página
