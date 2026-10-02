# frontend-dashboard-management Specification

## Purpose
Oferecer uma visão financeira global orientada por carteira, fiel aos valores autoritativos do backend, com precisão decimal, moedas independentes e estados acessíveis.

## Requirements

### Requirement: Dashboard funcional e contextual por carteira
A aplicação SHALL substituir o placeholder de `/dashboard` por uma página funcional carregada no shell e SHALL usar uma carteira selecionada como contexto exclusivo dos indicadores. A página SHALL consumir o contexto global do shell, sem seletor local próprio nem listagem duplicada de Carteiras, aplicando a precedência e o fallback especificados em frontend-application-shell.

#### Scenario: Nenhuma carteira disponível
- **WHEN** a listagem de Carteiras retorna vazia
- **THEN** o Dashboard apresenta estado vazio e ação para cadastrar uma Carteira sem solicitar indicadores financeiros

#### Scenario: Única carteira disponível
- **WHEN** a listagem retorna exatamente uma Carteira e a URL não contém seleção explícita
- **THEN** essa Carteira é selecionada automaticamente por não haver ambiguidade e seus dados financeiros são carregados

#### Scenario: Múltiplas carteiras sem seleção
- **WHEN** a listagem retorna duas ou mais Carteiras e a URL não contém seleção explícita
- **THEN** o Dashboard usa a seleção global válida ou o fallback por id ASC e carrega somente seus indicadores

#### Scenario: Seleção explícita
- **WHEN** o usuário seleciona uma Carteira disponível no shell
- **THEN** somente os indicadores dessa Carteira são carregados e apresentados

#### Scenario: Troca de carteira
- **WHEN** o usuário troca a Carteira selecionada no shell
- **THEN** os dados anteriores deixam de ser apresentados como atuais e o Dashboard carrega o novo contexto

### Requirement: Seleção persistida na URL
O Dashboard SHALL representar a seleção atual pelo query parameter `carteiraId`, SHALL restaurar uma seleção válida após acesso direto ou reload e SHALL tratar valores inválidos ou inexistentes sem quebrar a página nem consultar um identificador não validado contra a coleção disponível.

#### Scenario: Query parameter válido
- **WHEN** `/dashboard?carteiraId={id}` referencia uma Carteira retornada pela listagem
- **THEN** essa Carteira é selecionada e seus dados financeiros são carregados

#### Scenario: Query parameter ausente
- **WHEN** o Dashboard é acessado sem `carteiraId`
- **THEN** aplica-se a precedência do contexto global e a seleção válida resolvida é representada em carteiraId sem criar ciclo de navegação

#### Scenario: Query parameter malformado
- **WHEN** `carteiraId` não representa um identificador positivo válido
- **THEN** o Dashboard apresenta estado tratável de seleção inválida sem falha de runtime nem consulta financeira para esse valor

#### Scenario: Carteira indicada não existe mais
- **WHEN** `carteiraId` não corresponde a qualquer Carteira retornada
- **THEN** o Dashboard informa que a seleção não está disponível e permite escolher outra Carteira

### Requirement: Contratos financeiros autoritativos
Para a Carteira selecionada, o Dashboard SHALL consumir `GET /carteiras/{id}/resumo`, `GET /carteiras/{id}/posicoes`, `GET /carteiras/{id}/resultados-realizados` e, por meio de `frontend-portfolio-evolution`, `GET /carteiras/{id}/evolucao-patrimonial`. O frontend MUST apresentar os valores devolvidos e MUST NOT recalcular preço médio, custo, valor atual, patrimônio, resultado realizado, resultado não realizado, rentabilidade ou evolução. O Dashboard MUST NOT consumir `/patrimonio`; criação de snapshot SHALL ocorrer exclusivamente por ação manual conforme `frontend-portfolio-evolution`.

#### Scenario: Carregamento financeiro
- **WHEN** uma Carteira válida é selecionada
- **THEN** resumo, posições, resultados realizados e evolução dessa Carteira são solicitados sem endpoint financeiro não aprovado

#### Scenario: Valor financeiro autoritativo
- **WHEN** qualquer resposta financeira é apresentada
- **THEN** o valor corresponde ao campo devolvido pelo backend e não a um recálculo do frontend

#### Scenario: Resultado realizado por ação
- **WHEN** resultados realizados são retornados
- **THEN** cada resultado é apresentado por Ação e o frontend não cria um total calculado a partir da coleção

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

### Requirement: Moedas independentes
O Dashboard SHALL apresentar BRL e USD em agrupamentos visualmente e semanticamente distintos, usando `R$` para BRL e `US$` para USD. O frontend MUST NOT somar moedas, converter valores, assumir taxa cambial nem produzir patrimônio global entre Carteiras.

#### Scenario: Somente BRL
- **WHEN** o resumo contém apenas BRL
- **THEN** a página apresenta somente o grupo BRL com símbolo `R$`

#### Scenario: Somente USD
- **WHEN** o resumo contém apenas USD
- **THEN** a página apresenta somente o grupo USD com símbolo `US$`

#### Scenario: BRL e USD simultâneos
- **WHEN** o resumo contém BRL e USD
- **THEN** a página apresenta grupos separados e nenhum total combinado

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

### Requirement: Estados financeiros e recuperação explícita
O Dashboard SHALL diferenciar carregamento da lista de Carteiras, erro dessa lista, ausência de Carteiras, espera por seleção, carregamento financeiro, conteúdo, Carteira sem posições, ausência de resultados e erro financeiro. Erros 404, 409, 422 e técnicos SHALL usar a normalização HTTP existente e oferecer recuperação adequada sem retry automático.

#### Scenario: Loading de Carteiras
- **WHEN** a coleção de Carteiras está pendente
- **THEN** a página anuncia o carregamento e não simula conteúdo financeiro

#### Scenario: Erro ao listar Carteiras
- **WHEN** a coleção de Carteiras falha
- **THEN** o erro normalizado é apresentado com retry explícito

#### Scenario: Loading financeiro
- **WHEN** as consultas financeiras estão pendentes
- **THEN** a página anuncia o estado ocupado e não mantém dados de outra Carteira como atuais

#### Scenario: Carteira sem posições
- **WHEN** resumo e posições são vazios para uma Carteira existente
- **THEN** a página apresenta estado sem posições abertas em vez de erro

#### Scenario: Erro financeiro conhecido
- **WHEN** uma consulta financeira falha com 404, 409 ou 422
- **THEN** a mensagem normalizada é apresentada e o usuário pode recuperar ou trocar a seleção

#### Scenario: Erro técnico
- **WHEN** uma consulta financeira falha sem `StandardError` válido
- **THEN** o erro técnico normalizado é apresentado com retry explícito

### Requirement: Reload e consistência observável
O Dashboard SHALL oferecer atualização explícita que refaça resumo, posições, resultados realizados e evolução patrimonial para a Carteira selecionada. As respostas MAY ser solicitadas em paralelo; o frontend MUST NOT reconciliar diferenças temporais entre elas por cálculo nem executar retry automático. Atualizar dados MUST NOT criar snapshot; somente a ação explícita “Registrar snapshot” MAY executar o POST correspondente.

#### Scenario: Atualização solicitada
- **WHEN** o usuário aciona “Atualizar dados” com uma Carteira selecionada
- **THEN** as consultas financeiras, incluindo a evolução, são novamente realizadas para o mesmo contexto sem criar snapshot

#### Scenario: Respostas de contextos diferentes
- **WHEN** o usuário troca de Carteira enquanto consultas anteriores estão pendentes
- **THEN** respostas do contexto anterior não substituem nem contaminam o Dashboard atual

### Requirement: Navegação contextual
O Dashboard SHALL oferecer ações para acessar a Carteira selecionada e registrar uma nova Operação vinculada a ela, reutilizando as rotas e regras funcionais existentes.

#### Scenario: Acessar Carteira
- **WHEN** o usuário aciona a ação de acessar a Carteira
- **THEN** a aplicação navega para `/carteiras/{id}` da seleção atual

#### Scenario: Registrar Operação
- **WHEN** o usuário aciona a ação de registrar Operação
- **THEN** a aplicação abre o fluxo existente de nova Operação com a Carteira selecionada como contexto

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
