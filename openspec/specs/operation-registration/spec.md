# operation-registration Specification

## Purpose

Definir o registro REST atômico de compras e vendas de Ações em uma Carteira, preservando o preço efetivamente negociado e a consistência cronológica do histórico financeiro.

## Requirements

### Requirement: Contrato REST de criação de Operação
O sistema SHALL expor `POST /operacoes` e SHALL aceitar exclusivamente `carteiraId`, `ticker`, `mercado`, `corretoraId`, `tipo`, `quantidade`, `precoUnitario`, `dataOperacao` e `ordemNoDia`. `carteiraId`, `ticker`, `mercado`, `tipo`, `quantidade`, `precoUnitario`, `dataOperacao` e `ordemNoDia` SHALL ser obrigatórios; `corretoraId` SHALL ser opcional. O cliente MUST NOT informar `id`, `acaoId`, `valorTotal`, cotação atual, cotação histórica ou qualquer outro campo controlado pela aplicação.

#### Scenario: Request mínimo sem Corretora
- **WHEN** o cliente envia todos os campos obrigatórios válidos e omite `corretoraId`
- **THEN** o sistema processa a criação com associação de Corretora ausente

#### Scenario: Request com Corretora
- **WHEN** o cliente envia todos os campos obrigatórios válidos e um `corretoraId` existente
- **THEN** o sistema processa a criação usando exatamente a Corretora persistida identificada

#### Scenario: Campo ausente ou desconhecido
- **WHEN** o cliente omite campo obrigatório ou envia `id`, `acaoId`, `valorTotal`, cotação ou propriedade não admitida
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e não persiste Operação

### Requirement: Resposta da criação concluída
Uma criação concluída SHALL responder `201 Created`, SHALL incluir `Location: /operacoes/{id}` e SHALL devolver um `OperacaoResponse` contendo `id`, `carteiraId`, ticker normalizado, `mercado`, `corretoraId` anulável, `tipo`, `quantidade`, `precoUnitario`, `dataOperacao`, `ordemNoDia` e `valorTotal` efetivamente persistidos. O response MUST NOT apresentar cotação atual ou histórica como preço da Operação.

#### Scenario: Compra criada
- **WHEN** uma COMPRA válida é persistida
- **THEN** o sistema responde `201 Created` com o DTO completo e `Location` baseado no identificador gerado

#### Scenario: Venda criada
- **WHEN** uma VENDA válida é persistida
- **THEN** o sistema responde `201 Created` com o DTO completo e `Location` baseado no identificador gerado

### Requirement: Associação obrigatória com Carteira existente
O sistema SHALL exigir `carteiraId`, SHALL localizar a Carteira persistida antes da criação e SHALL associar a Operação exatamente ao registro encontrado. Carteira inexistente SHALL produzir `404 Not Found` no formato `StandardError` vigente. O registro MUST NOT modificar nome, data de criação ou qualquer outro estado da Carteira.

#### Scenario: Carteira existente
- **WHEN** `carteiraId` identifica uma Carteira persistida
- **THEN** a nova Operação referencia exatamente essa Carteira sem alterar seus dados

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica Carteira persistida
- **THEN** o sistema responde `404 Not Found` no formato padronizado e não persiste Operação

### Requirement: Seleção obrigatória de Ação por ticker e mercado
O sistema SHALL normalizar o ticker pela mesma regra vigente no cadastro de Ação e SHALL localizar uma Ação já persistida pela combinação exata do ticker normalizado com o enum `Mercado`. Os únicos mercados aceitos SHALL permanecer `BRASIL` e `EUA`. O cliente MUST NOT informar `acaoId`, e o registro de Operação MUST NOT cadastrar Ação, consultar provider externo ou modificar qualquer dado da Ação.

#### Scenario: Ação brasileira existente
- **WHEN** o cliente informa ticker normalizável para `PETR4` e `mercado=BRASIL`, e essa combinação está persistida
- **THEN** a Operação referencia exatamente a Ação brasileira encontrada

#### Scenario: Ação americana existente
- **WHEN** o cliente informa ticker normalizável para `AAPL` e `mercado=EUA`, e essa combinação está persistida
- **THEN** a Operação referencia exatamente a Ação americana encontrada

#### Scenario: Normalização do ticker
- **WHEN** o cliente informa `" petr4 "` com `mercado=BRASIL`
- **THEN** o sistema procura e responde com `PETR4`, reutilizando a normalização vigente sem criar segunda regra

#### Scenario: Ação não cadastrada
- **WHEN** nenhuma Ação persistida corresponde ao ticker normalizado e mercado informados
- **THEN** o sistema responde `404 Not Found` no formato padronizado, não consulta BRAPI ou Alpha Vantage e não cadastra Ação

### Requirement: Associação opcional com Corretora existente
Quando `corretoraId` estiver ausente ou nulo, o sistema SHALL permitir a criação e persistir a associação como nula. Quando informado, o sistema SHALL localizar e associar a Corretora persistida ou responder `404 Not Found` no formato vigente quando o ID não existir. O fluxo MUST NOT consultar BrasilAPI, ViaCEP ou modificar a Corretora.

#### Scenario: Corretora omitida
- **WHEN** uma Operação válida omite ou informa `corretoraId=null`
- **THEN** o sistema persiste a Operação com Corretora nula

#### Scenario: Corretora existente
- **WHEN** `corretoraId` identifica uma Corretora persistida
- **THEN** a Operação referencia exatamente essa Corretora sem revalidá-la externamente

#### Scenario: Corretora inexistente
- **WHEN** `corretoraId` é informado e não identifica Corretora persistida
- **THEN** o sistema responde `404 Not Found` e não persiste Operação

### Requirement: Tipo de Operação restrito
O campo `tipo` SHALL aceitar exclusivamente o enum `TipoOperacao` com os valores textuais `COMPRA` e `VENDA`. Valor ausente, nulo ou diferente desses valores SHALL produzir `400 Bad Request` com código `REQUEST_INVALIDO`.

#### Scenario: Tipos válidos
- **WHEN** o cliente informa `COMPRA` ou `VENDA`
- **THEN** o sistema interpreta e persiste exatamente o tipo informado

#### Scenario: Tipo inválido
- **WHEN** o cliente informa outro texto, enum ordinal, valor nulo ou omite `tipo`
- **THEN** o sistema responde `400 Bad Request` e não persiste Operação

### Requirement: Quantidade positiva e válida para o mercado
`quantidade` SHALL usar `BigDecimal`, SHALL ser obrigatória, maior que zero e exatamente representável em `NUMERIC(19,6)`. Para `BRASIL`, o valor SHALL ser matematicamente inteiro e qualquer componente fracionário diferente de zero SHALL ser rejeitado. Para `EUA`, o sistema SHALL aceitar quantidades inteiras ou fracionárias com até seis casas decimais. O sistema MUST NOT usar `float` ou `double`, arredondar, truncar ou converter silenciosamente valores fora desses limites.

#### Scenario: Quantidade inteira
- **WHEN** o cliente informa `quantidade=100` para `BRASIL` ou `EUA`
- **THEN** o sistema aceita o valor matematicamente inteiro como quantidade exata da Operação

#### Scenario: Quantidade fracionária no Brasil
- **WHEN** o cliente informa para `BRASIL` uma quantidade com componente fracionário diferente de zero
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO`, identifica `quantidade` nos detalhes e não persiste Operação

#### Scenario: Quantidade fracionária nos EUA
- **WHEN** o cliente informa para `EUA` uma quantidade positiva com até seis casas decimais e dentro da precisão aprovada
- **THEN** o sistema preserva exatamente a quantidade fracionária informada

#### Scenario: Quantidade inválida
- **WHEN** a quantidade é ausente, zero, negativa ou excede precisão 19 ou escala 6
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO`, identifica `quantidade` nos detalhes e não persiste Operação

### Requirement: Preço unitário efetivamente negociado
`precoUnitario` SHALL ser informado pelo cliente, SHALL representar exclusivamente o preço efetivamente negociado naquela COMPRA ou VENDA, SHALL ser obrigatório, positivo e exatamente representável com precisão total máxima 19 e escala máxima 6. `Acao.cotacaoAtual` e qualquer cotação histórica MUST NOT substituir, preencher, ajustar ou validar por divergência o preço informado.

#### Scenario: Preço real diferente da cotação atual
- **WHEN** o cliente informa preço unitário positivo diferente de `Acao.cotacaoAtual`
- **THEN** o sistema preserva o preço informado e não altera nem utiliza a cotação atual no registro

#### Scenario: Preço real diferente da referência histórica
- **WHEN** existir uma referência histórica diferente do preço unitário informado
- **THEN** a divergência não altera nem impede a Operação e nenhuma tolerância percentual é aplicada

#### Scenario: Preço inválido
- **WHEN** o preço unitário é ausente, zero, negativo ou excede precisão 19 ou escala 6
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO`, identifica `precoUnitario` nos detalhes e não persiste Operação

### Requirement: Valor total calculado com exatidão
O cliente MUST NOT informar `valorTotal`. O sistema SHALL calcular `valorTotal = quantidade × precoUnitario` usando aritmética decimal exata, sem cotação atual ou histórica e sem arredondamento ou truncamento. O resultado SHALL ser exatamente representável com precisão total máxima 38 e escala máxima 12; entradas individualmente válidas MUST ser rejeitadas sem persistência se o resultado não couber nesses limites.

#### Scenario: Cálculo do valor total
- **WHEN** a quantidade é `100` e o preço unitário é `32.47`
- **THEN** o valor total representa exatamente `3247.00`

#### Scenario: Cotação não participa do cálculo
- **WHEN** a cotação atual ou histórica difere de `precoUnitario`
- **THEN** o valor total continua sendo calculado exclusivamente com quantidade e preço unitário da Operação

#### Scenario: Resultado fora da precisão
- **WHEN** o produto não puder ser representado exatamente nos limites aprovados
- **THEN** o sistema rejeita a criação com erro padronizado e não deixa o banco arredondar ou persistir parcialmente

### Requirement: Data da Operação sem horário fabricado
`dataOperacao` SHALL usar `LocalDate`/SQL `DATE`, SHALL ser obrigatória e representar somente a data civil conhecida da negociação, sem horário ou offset inventados. O sistema SHALL aceitar datas passadas e a data civil corrente do mercado da Ação e SHALL rejeitar datas futuras com `400 Bad Request` e `REQUEST_INVALIDO`. A referência de mercado SHALL ser `America/Sao_Paulo` para `BRASIL` e `America/New_York` para `EUA`, calculada a partir de um `Clock` testável. O sistema MUST NOT comparar a entrada somente com uma data UTC global nem confundi-la com data de cadastro, `Acao.dataHoraCotacao` ou referência histórica.

#### Scenario: Operação passada
- **WHEN** o cliente informa uma data válida anterior à data corrente
- **THEN** o sistema preserva exatamente essa data na Operação

#### Scenario: Operação no dia corrente
- **WHEN** o cliente informa a data corrente determinada pelo mesmo instante do `Clock` na zona correspondente ao mercado da Ação
- **THEN** o sistema permite o registro sem acrescentar horário

#### Scenario: Operação futura
- **WHEN** o cliente informa data posterior à data civil corrente em `America/Sao_Paulo` para `BRASIL` ou `America/New_York` para `EUA`
- **THEN** o sistema responde `400 Bad Request` e não persiste Operação

### Requirement: Ordem cronológica explícita dentro do dia
`ordemNoDia` SHALL ser um inteiro positivo fornecido pelo cliente e SHALL ser único por combinação de Carteira, Ação e `dataOperacao`. A cronologia SHALL usar `dataOperacao ASC, ordemNoDia ASC` e MUST NOT usar ID ou ordem física de inserção como substituto da ordem de negócio.

#### Scenario: Operações em datas diferentes
- **WHEN** duas Operações da mesma Carteira e Ação possuem datas diferentes
- **THEN** a data anterior é processada primeiro independentemente da ordem de cadastro

#### Scenario: Operações na mesma data
- **WHEN** duas Operações da mesma Carteira e Ação possuem a mesma data e ordens distintas
- **THEN** a menor `ordemNoDia` é processada primeiro

#### Scenario: Ordem duplicada
- **WHEN** já existe Operação com a mesma Carteira, Ação, data e `ordemNoDia`
- **THEN** o sistema responde `409 Conflict` com código `ORDEM_OPERACAO_DUPLICADA` e não persiste outra Operação ambígua

### Requirement: Registro de COMPRA sem consolidação financeira
Uma COMPRA válida SHALL aumentar a quantidade cronologicamente disponível para validações subsequentes e SHALL persistir somente seus próprios dados. Nesta change, a COMPRA MUST NOT persistir posição, recalcular ou armazenar preço médio, custo consolidado, resultado, rentabilidade, patrimônio ou snapshot e MUST NOT alterar Carteira, Ação ou Corretora.

#### Scenario: Primeira compra
- **WHEN** uma COMPRA válida é registrada sem Operações anteriores para a mesma Carteira e Ação
- **THEN** a Operação é persistida e sua quantidade passa a integrar o saldo derivado para validações posteriores

#### Scenario: Compras múltiplas
- **WHEN** múltiplas COMPRAS válidas são registradas na ordem cronológica definida
- **THEN** suas quantidades integram a soma comprada sem criar posição ou preço médio persistido

### Requirement: VENDA limitada pela posição cronologicamente disponível
Antes de persistir uma VENDA, o sistema SHALL reproduzir todas as Operações da mesma Carteira e Ação, incluindo a candidata em sua posição cronológica, somando COMPRA e subtraindo VENDA. A criação SHALL ser permitida somente quando o saldo permanecer maior ou igual a zero em todos os pontos da sequência; caso contrário, SHALL responder `409 Conflict` com código `POSICAO_INSUFICIENTE` e não persistir a candidata. Operações de outras Carteiras ou Ações MUST NOT participar do saldo.

#### Scenario: Venda dentro da posição
- **WHEN** a quantidade vendida não torna negativo nenhum saldo cronológico
- **THEN** a VENDA é persistida com o preço unitário efetivamente informado

#### Scenario: Venda exatamente igual à posição
- **WHEN** a quantidade vendida é exatamente igual ao saldo disponível naquele ponto
- **THEN** a VENDA é persistida e o saldo derivado passa a zero

#### Scenario: Venda acima da posição
- **WHEN** a quantidade vendida excede o saldo disponível naquele ponto
- **THEN** o sistema responde `409 Conflict` com `POSICAO_INSUFICIENTE` e não persiste a VENDA

#### Scenario: Isolamento por Carteira e Ação
- **WHEN** existem compras da mesma Ação em outra Carteira ou de outra Ação na mesma Carteira
- **THEN** essas Operações não aumentam a posição disponível para a VENDA avaliada

### Requirement: Inserção retroativa preserva toda a sequência
O sistema SHALL aceitar uma Operação retroativa somente se, depois de inseri-la por data e ordem, toda a sequência da mesma Carteira e Ação continuar sem saldo negativo. Uma candidata que torne inválida qualquer VENDA posterior MUST ser rejeitada integralmente.

#### Scenario: Compra retroativa compatível
- **WHEN** uma COMPRA retroativa é inserida com ordem não duplicada e toda a sequência permanece válida
- **THEN** o sistema persiste a COMPRA sem alterar as Operações posteriores

#### Scenario: Venda retroativa que invalida venda posterior
- **WHEN** uma VENDA retroativa deixaria negativo o saldo em seu ponto ou em uma VENDA posterior já persistida
- **THEN** o sistema responde `409 Conflict` com `POSICAO_INSUFICIENTE` e preserva todas as Operações existentes

### Requirement: Atomicidade e consistência concorrente
A localização dos relacionamentos, a validação cronológica e a persistência SHALL compor uma única operação de escrita atômica. Requisições concorrentes para a mesma Carteira MUST ser coordenadas de modo que duas VENDAS não sejam ambas aceitas quando sua combinação exceder a posição disponível. Falha de validação, integridade ou persistência MUST deixar Carteira, Ação, Corretora e Operações existentes inalteradas.

#### Scenario: Vendas concorrentes excedem a posição em conjunto
- **WHEN** duas VENDAS concorrentes seriam válidas isoladamente, mas excederiam juntas a posição
- **THEN** no máximo a combinação cronologicamente válida é persistida e a outra solicitação recebe erro padronizado

#### Scenario: Falha durante a criação
- **WHEN** qualquer etapa transacional falha antes da conclusão
- **THEN** nenhuma Operação parcial é persistida e nenhum registro relacionado é alterado

### Requirement: Persistência relacional sem cascade delete
A Operação SHALL possuir identificador próprio, referências obrigatórias a Carteira e Ação, referência anulável a Corretora e os demais campos do contrato. As referências MUST preservar integridade no banco e MUST NOT usar cascade delete. A capability MUST NOT criar coleção bidirecional obrigatória nos agregados existentes, tabela de posição, histórico de cotação, snapshot ou campos financeiros consolidados.

#### Scenario: Corretora nula na persistência
- **WHEN** uma Operação é criada sem Corretora
- **THEN** somente a foreign key de Corretora permanece nula e as referências a Carteira e Ação continuam obrigatórias

#### Scenario: Tentativa de remover entidade referenciada
- **WHEN** uma Operação persistida referencia Carteira, Ação ou Corretora
- **THEN** a integridade relacional impede que a Operação seja apagada por cascade a partir dessas entidades

### Requirement: Cotação histórica desacoplada do registro
`POST /operacoes` MUST NOT consultar BRAPI, Alpha Vantage ou outro provider para obter cotação histórica, MUST NOT persistir referência histórica e MUST NOT incluir essa referência no `OperacaoResponse`. Uma capability futura e separada poderá apresentar o `close` bruto da data exata como referência — pela BRAPI para `BRASIL` e por `TIME_SERIES_DAILY` ou endpoint futuramente aprovado da Alpha Vantage para `EUA` — e MUST NOT selecionar automaticamente outro pregão quando a data não possuir cotação. Sua indisponibilidade, timeout, rate limit, data sem pregão ou limitação de plano MUST NOT impedir o registro de uma Operação válida com preço real informado.

#### Scenario: Registro independente dos providers
- **WHEN** o cliente registra uma Operação válida e os providers estão indisponíveis ou limitados
- **THEN** a Operação é processada exclusivamente com dados persistidos e preço informado, sem chamada externa

#### Scenario: Data sem referência de pregão
- **WHEN** não existe cotação histórica para a data da Operação
- **THEN** a ausência não altera `precoUnitario`, `valorTotal` ou a elegibilidade do registro

### Requirement: Separação dos conceitos de preço
O sistema SHALL tratar `Acao.cotacaoAtual` como a última cotação de mercado conhecida, `Operacao.precoUnitario` como o preço real efetivamente negociado e a cotação histórica como referência auxiliar de mercado. Somente `Operacao.precoUnitario` SHALL participar de `valorTotal` e dos futuros cálculos de custo, preço médio e resultado. Os três conceitos MUST NOT ser tratados como sinônimos nem substituir uns aos outros.

#### Scenario: Preço real como base financeira
- **WHEN** uma Operação é registrada com cotações atual ou histórica diferentes do preço informado
- **THEN** o sistema preserva `precoUnitario` como única base do valor total e dos futuros cálculos financeiros

### Requirement: Compatibilidade das funcionalidades existentes
A criação de Operação SHALL preservar os contratos e dados existentes de Carteira, Ação e Corretora, inclusive os endpoints atualmente promovidos. O schema SHALL ser evoluído por novo changeSet, sem modificar os changeSets anteriores, dependências ou `spring.jpa.hibernate.ddl-auto=validate`.

#### Scenario: Regressão das APIs existentes
- **WHEN** a capability de Operações é disponibilizada
- **THEN** os endpoints existentes de Carteira, Ação e Corretora mantêm seus contratos, salvo o bloqueio explicitamente especificado para excluir Carteira com histórico

#### Scenario: Inicialização com Liquibase e Hibernate
- **WHEN** PostgreSQL ou H2 inicia com o changelog atualizado
- **THEN** Liquibase cria somente a estrutura nova de Operação antes de o Hibernate validar o schema


