## Purpose

Oferecer uma visão financeira global orientada por carteira, fiel aos valores autoritativos do backend, com precisão decimal, moedas independentes e estados acessíveis.

## ADDED Requirements

### Requirement: Dashboard funcional e contextual por carteira
A aplicação SHALL substituir o placeholder de `/dashboard` por uma página funcional carregada no shell e SHALL usar uma carteira selecionada como contexto exclusivo dos indicadores. A página SHALL listar as carteiras disponíveis, SHALL selecionar automaticamente somente quando existir exatamente uma carteira e MUST NOT escolher arbitrariamente uma carteira quando existirem duas ou mais.

#### Scenario: Nenhuma carteira disponível
- **WHEN** a listagem de Carteiras retorna vazia
- **THEN** o Dashboard apresenta estado vazio e ação para cadastrar uma Carteira sem solicitar indicadores financeiros

#### Scenario: Única carteira disponível
- **WHEN** a listagem retorna exatamente uma Carteira e não existe seleção válida na URL
- **THEN** essa Carteira é selecionada automaticamente por não haver ambiguidade e seus dados financeiros são carregados

#### Scenario: Múltiplas carteiras sem seleção
- **WHEN** a listagem retorna duas ou mais Carteiras e não existe seleção válida na URL
- **THEN** o Dashboard aguarda seleção explícita e não consulta indicadores de qualquer Carteira

#### Scenario: Seleção explícita
- **WHEN** o usuário seleciona uma Carteira disponível
- **THEN** somente os indicadores dessa Carteira são carregados e apresentados

#### Scenario: Troca de carteira
- **WHEN** o usuário troca a Carteira selecionada
- **THEN** os dados anteriores deixam de ser apresentados como atuais e o Dashboard carrega o novo contexto

### Requirement: Seleção persistida na URL
O Dashboard SHALL representar a seleção atual pelo query parameter `carteiraId`, SHALL restaurar uma seleção válida após acesso direto ou reload e SHALL tratar valores inválidos ou inexistentes sem quebrar a página nem consultar um identificador não validado contra a coleção disponível.

#### Scenario: Query parameter válido
- **WHEN** `/dashboard?carteiraId={id}` referencia uma Carteira retornada pela listagem
- **THEN** essa Carteira é selecionada e seus dados financeiros são carregados

#### Scenario: Query parameter ausente
- **WHEN** o Dashboard é acessado sem `carteiraId`
- **THEN** aplica-se a regra de seleção baseada na quantidade de Carteiras disponíveis

#### Scenario: Query parameter malformado
- **WHEN** `carteiraId` não representa um identificador positivo válido
- **THEN** o Dashboard apresenta estado tratável de seleção inválida sem falha de runtime nem consulta financeira para esse valor

#### Scenario: Carteira indicada não existe mais
- **WHEN** `carteiraId` não corresponde a qualquer Carteira retornada
- **THEN** o Dashboard informa que a seleção não está disponível e permite escolher outra Carteira

### Requirement: Contratos financeiros autoritativos
Para a Carteira selecionada, o Dashboard SHALL consumir `GET /carteiras/{id}/resumo`, `GET /carteiras/{id}/posicoes` e `GET /carteiras/{id}/resultados-realizados`. O frontend MUST apresentar os valores devolvidos e MUST NOT recalcular preço médio, custo, valor atual, patrimônio, resultado realizado, resultado não realizado ou rentabilidade. O Dashboard MUST NOT consumir `/patrimonio` nem `/evolucao-patrimonial` nesta capability.

#### Scenario: Carregamento financeiro
- **WHEN** uma Carteira válida é selecionada
- **THEN** resumo, posições e resultados realizados dessa Carteira são solicitados sem endpoint financeiro adicional

#### Scenario: Valor financeiro autoritativo
- **WHEN** qualquer resposta financeira é apresentada
- **THEN** o valor corresponde ao campo devolvido pelo backend e não a um recálculo do frontend

#### Scenario: Resultado realizado por ação
- **WHEN** resultados realizados são retornados
- **THEN** cada resultado é apresentado por Ação e o frontend não cria um total calculado a partir da coleção

### Requirement: Precisão decimal lossless compartilhada
O frontend SHALL preservar como texto os tokens decimais dos campos `BigDecimal` de resumo, posições e resultados realizados. IDs MAY permanecer numéricos, mas campos financeiros MUST NOT passar por `Number`, `parseFloat` ou aritmética binária JavaScript. A infraestrutura lossless SHALL ser compartilhável e MUST NOT acoplar o Dashboard ao parser específico de Operações.

#### Scenario: Decimal longo
- **WHEN** o backend retorna um campo financeiro com mais precisão que a representação segura de `number`
- **THEN** seus dígitos são preservados integralmente no model frontend

#### Scenario: Valor negativo
- **WHEN** o backend retorna resultado ou rentabilidade negativa
- **THEN** o sinal e os dígitos são preservados e apresentados corretamente

#### Scenario: Formatação visual
- **WHEN** um valor financeiro textual é exibido
- **THEN** a formatação ocorre somente na apresentação sem alterar o valor preservado

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
Para cada item de `resumos`, o Dashboard SHALL exibir patrimônio atual, custo total das posições, resultado não realizado total e rentabilidade percentual. A página MAY exibir a quantidade de posições abertas como o tamanho da coleção de posições retornada, mas SHALL identificá-la explicitamente como informação estrutural e não como cálculo financeiro.

#### Scenario: Cards de resumo
- **WHEN** o backend retorna um ou mais resumos por moeda
- **THEN** cada moeda possui seus próprios indicadores sem combinação com outro item

#### Scenario: Quantidade de posições abertas
- **WHEN** a coleção de posições é recebida
- **THEN** sua quantidade pode ser apresentada com o rótulo “posições abertas” sem derivar qualquer valor financeiro

### Requirement: Posições abertas responsivas
O Dashboard SHALL apresentar, para cada posição devolvida, ticker, empresa, mercado, moeda, quantidade atual, preço médio, cotação atual, valor atual, resultado não realizado e rentabilidade. A apresentação SHALL permanecer legível em viewport compacto e MUST NOT oferecer rota inexistente de detalhe de posição.

#### Scenario: Posição brasileira
- **WHEN** uma posição de mercado BRASIL é retornada
- **THEN** seus dados e valores em BRL são apresentados sem recalculá-los

#### Scenario: Posição americana
- **WHEN** uma posição de mercado EUA é retornada
- **THEN** seus dados e valores em USD são apresentados sem conversão cambial

#### Scenario: Viewport compacto
- **WHEN** a seção de posições é exibida em tela compacta
- **THEN** todos os dados essenciais continuam disponíveis por tabela responsiva ou representação acessível equivalente sem scroll horizontal obrigatório da página

### Requirement: Resultados realizados sem totalização
O Dashboard SHALL exibir cada `ResultadoRealizadoResponse` com ticker, empresa, mercado, moeda e resultado realizado. A página MUST NOT somar a coleção nem inferir um resultado total.

#### Scenario: Resultados disponíveis
- **WHEN** o backend retorna resultados realizados
- **THEN** cada item é exibido individualmente com sua moeda

#### Scenario: Ausência de resultados
- **WHEN** o backend retorna uma coleção vazia
- **THEN** a seção informa normalmente que ainda não existem resultados realizados

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
O Dashboard SHALL oferecer atualização explícita que refaça resumo, posições e resultados realizados para a Carteira selecionada. As três respostas MAY ser solicitadas em paralelo; o frontend MUST NOT reconciliar diferenças temporais entre elas por cálculo nem executar retry automático.

#### Scenario: Atualização solicitada
- **WHEN** o usuário aciona “Atualizar dados” com uma Carteira selecionada
- **THEN** as três consultas financeiras são novamente realizadas para o mesmo contexto

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
O Dashboard SHALL possuir título principal, seções hierárquicas, label claro para seleção, nomes acessíveis para ações e estados dinâmicos anunciados sem deslocar foco indevidamente. Resultado positivo e negativo MUST NOT ser distinguido somente por cor.

#### Scenario: Uso por tecnologia assistiva
- **WHEN** a página é percorrida por tecnologia assistiva
- **THEN** seleção, cards, posições, resultados, loading e erros possuem estrutura e nomes compreensíveis

#### Scenario: Resultado com semântica não cromática
- **WHEN** um resultado positivo ou negativo é apresentado
- **THEN** sinal e texto comunicam seu significado independentemente da cor

#### Scenario: Interação por teclado
- **WHEN** o usuário opera a página apenas por teclado
- **THEN** seletor, retry, reload e navegação possuem foco visível e ordem coerente
