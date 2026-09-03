# operation-registration Specification

## Purpose

Definir o registro REST atômico de compras e vendas de Ações em uma Carteira, preservando o preço efetivamente negociado e a consistência cronológica do histórico financeiro.

## Requirements

### Requirement: Contrato REST de criação de Operação
O sistema SHALL expor `POST /operacoes` com contrato discriminado pelo campo `tipo`. COMPRA SHALL aceitar exclusivamente `carteiraId`, `ticker`, `mercado`, `corretoraId`, `tipo=COMPRA`, `quantidade` e `dataOperacao`; `precoUnitario` e `ordemNoDia` MUST ser proibidos. VENDA SHALL aceitar os mesmos campos com `tipo=VENDA` e SHALL exigir `precoUnitario`; `ordemNoDia` MUST ser proibido. `corretoraId` SHALL aceitar omissão ou valor nulo; quando informado, SHALL referenciar uma Corretora existente. Qualquer campo desconhecido ou controlado pela aplicação SHALL ser rejeitado.

#### Scenario: COMPRA sem preço
- **WHEN** o cliente envia uma COMPRA válida sem `precoUnitario`, sem `ordemNoDia` e opcionalmente com `corretoraId`
- **THEN** o sistema processa o request conforme o contrato de COMPRA

#### Scenario: Request mínimo sem Corretora
- **WHEN** o cliente envia COMPRA ou VENDA com todos os campos exigidos por sua variante e omite `corretoraId`
- **THEN** o sistema processa a criação com associação de Corretora ausente

#### Scenario: Request com Corretora
- **WHEN** o cliente envia todos os campos exigidos por sua variante e um `corretoraId` existente
- **THEN** o sistema processa a criação usando exatamente a Corretora persistida identificada

#### Scenario: Corretora omitida ou nula
- **WHEN** o cliente omite `corretoraId` ou informa `corretoraId=null` em uma variante válida
- **THEN** o sistema processa a criação com associação de Corretora ausente

#### Scenario: Corretora informada
- **WHEN** o cliente envia uma variante válida com `corretoraId` existente
- **THEN** o sistema processa a criação usando exatamente a Corretora persistida identificada

#### Scenario: COMPRA com preço proibido
- **WHEN** o cliente envia `precoUnitario` em uma COMPRA, inclusive nulo
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

#### Scenario: VENDA com preço
- **WHEN** o cliente envia uma VENDA válida com `precoUnitario` positivo e sem `ordemNoDia`
- **THEN** o sistema processa o request conforme o contrato de VENDA

#### Scenario: VENDA sem preço
- **WHEN** o cliente omite ou informa nulo em `precoUnitario` numa VENDA
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

#### Scenario: Discriminador inválido
- **WHEN** o cliente omite `tipo`, informa `tipo=null`, valor desconhecido ou caixa diferente de `COMPRA` e `VENDA`
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

#### Scenario: Campo ausente ou desconhecido
- **WHEN** o cliente envia `ordemNoDia`, `id`, `acaoId`, `valorTotal`, cotação ou qualquer propriedade não admitida em COMPRA ou VENDA
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

### Requirement: Resposta da criação concluída
Uma criação concluída SHALL responder `201 Created`, incluir `Location: /operacoes/{id}` e devolver `OperacaoResponse` contendo `id`, `carteiraId`, ticker normalizado, `mercado`, `corretoraId` anulável, `tipo`, `quantidade`, `precoUnitario`, `dataOperacao`, `ordemNoDia` e `valorTotal` efetivamente persistidos. Em COMPRA, `precoUnitario` SHALL ser o fechamento histórico bruto obtido; em VENDA, SHALL ser o preço informado.

#### Scenario: Compra criada
- **WHEN** uma COMPRA válida obtém o fechamento exato e é persistida
- **THEN** o response contém o preço histórico usado, a ordem gerada e o total calculado

#### Scenario: Venda criada
- **WHEN** uma VENDA válida é persistida
- **THEN** o response contém o preço informado, a ordem gerada e o total calculado

### Requirement: Associação obrigatória com Carteira existente
O sistema SHALL exigir `carteiraId`, SHALL localizar a Carteira persistida antes da criação e SHALL associar a Operação exatamente ao registro encontrado. Carteira inexistente SHALL produzir `404 Not Found` no formato `StandardError` vigente. O registro MUST NOT modificar nome, data de criação ou qualquer outro estado da Carteira.

#### Scenario: Carteira existente
- **WHEN** `carteiraId` identifica uma Carteira persistida
- **THEN** a nova Operação referencia exatamente essa Carteira sem alterar seus dados

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica Carteira persistida
- **THEN** o sistema responde `404 Not Found` no formato padronizado e não persiste Operação

### Requirement: Seleção obrigatória de Ação por ticker e mercado
O sistema SHALL normalizar o ticker pela regra vigente, localizar uma Ação persistida pela combinação exata de ticker e `Mercado` e aceitar somente `BRASIL` e `EUA`. O cliente MUST NOT informar `acaoId`; o registro MUST NOT cadastrar ou modificar Ação. A consulta histórica de COMPRA SHALL ocorrer somente depois de confirmar preliminarmente que Carteira, Ação e Corretora opcional existem; VENDA MUST NOT consultar provider.

#### Scenario: Ação brasileira existente
- **WHEN** ticker e mercado identificam uma Ação persistida e o request é COMPRA
- **THEN** a Operação referencia a Ação brasileira e o fechamento é consultado na BRAPI

#### Scenario: Ação americana existente
- **WHEN** ticker e `mercado=EUA` identificam uma Ação persistida
- **THEN** a Operação referencia a Ação americana e somente COMPRA consulta a Alpha Vantage

#### Scenario: Normalização do ticker
- **WHEN** o cliente informa ticker com espaços ou caixa não normalizada
- **THEN** o sistema procura e responde com o ticker normalizado pela regra vigente

#### Scenario: Ação não cadastrada
- **WHEN** nenhuma Ação corresponde ao ticker normalizado e mercado
- **THEN** o sistema responde `404 Not Found`, não consulta provider e não cadastra Ação

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

### Requirement: Valor total calculado com exatidão
O cliente MUST NOT informar `valorTotal`. O sistema SHALL calcular `valorTotal = quantidade × precoUnitario` com aritmética decimal exata e sem arredondamento ou truncamento, usando o fechamento obtido em COMPRA ou o preço informado em VENDA. O resultado SHALL caber em precisão 38 e escala 12.

#### Scenario: Cálculo do valor total
- **WHEN** uma COMPRA obtém `close=32.47` e possui `quantidade=100`
- **THEN** `valorTotal` representa exatamente `3247.00`

#### Scenario: Total de VENDA
- **WHEN** uma VENDA informa preço válido
- **THEN** o total é calculado exclusivamente pelo backend com quantidade e preço da VENDA

#### Scenario: Cotação não participa do cálculo
- **WHEN** a Operação é VENDA
- **THEN** nenhuma cotação participa do total, que usa exclusivamente o preço informado e a quantidade

#### Scenario: Resultado fora da precisão
- **WHEN** o produto não pode ser representado exatamente nos limites aprovados
- **THEN** a criação é rejeitada sem persistência parcial

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

### Requirement: Registro de COMPRA sem consolidação financeira
Uma COMPRA válida SHALL obter o fechamento histórico bruto da data exata antes da transação curta, persistir somente os dados da própria Operação e aumentar a quantidade cronologicamente disponível para validações subsequentes. A falha externa MUST impedir a nova COMPRA antes de qualquer persistência. A chamada de rede MUST NOT manter lock pessimista nem alterar cotação corrente, histórico de cotação corrente, dados existentes ou consolidações persistidas. Nesta change, a COMPRA MUST NOT persistir posição, recalcular ou armazenar preço médio, custo consolidado, resultado, rentabilidade, patrimônio ou snapshot.

#### Scenario: Primeira compra com fechamento exato
- **WHEN** uma primeira COMPRA válida obtém o fechamento exato e conclui a validação transacional
- **THEN** a Operação é persistida com o fechamento como preço e sua quantidade integra o saldo derivado

#### Scenario: Primeira compra
- **WHEN** uma COMPRA válida obtém o fechamento exato sem Operações anteriores para a mesma Carteira e Ação
- **THEN** a Operação é persistida sem consolidação financeira e sua quantidade integra o saldo derivado

#### Scenario: Compras múltiplas
- **WHEN** múltiplas COMPRAS válidas são registradas na ordem cronológica definida
- **THEN** suas quantidades integram a soma comprada sem criar posição ou preço médio persistido

#### Scenario: Falha externa antes da transação
- **WHEN** a consulta histórica da COMPRA falha
- **THEN** nenhuma Operação é persistida e nenhum lock de Carteira permanece aberto durante rede ou timeout

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
O sistema SHALL aceitar Operação retroativa somente quando todo o replay da mesma Carteira e Ação permanecer sem saldo negativo. Uma nova Operação em data que já contém Operações SHALL ser anexada ao final daquele dia por `MAX(ordemNoDia)+1`; não haverá inserção entre ordens existentes, reordenação manual ou `horaOperacao`. O usuário SHALL cadastrar Operações do mesmo dia na sequência real desejada.

#### Scenario: Operação retroativa no mesmo dia
- **WHEN** a data retroativa já possui Operações para a combinação
- **THEN** a candidata recebe a próxima ordem e é reproduzida depois das Operações já existentes naquele dia

#### Scenario: Compra retroativa compatível
- **WHEN** uma COMPRA retroativa com fechamento exato é anexada ao fim de sua data e todo o replay permanece válido
- **THEN** o sistema persiste a COMPRA sem alterar Operações posteriores

#### Scenario: Venda retroativa que invalida venda posterior
- **WHEN** a candidata torna negativo o saldo em seu ponto ou em qualquer Operação posterior
- **THEN** o sistema responde `409 POSICAO_INSUFICIENTE` e preserva o histórico existente

### Requirement: Atomicidade e consistência concorrente
A consulta externa de COMPRA SHALL preceder a transação curta. Dentro da transação, lock pessimista da Carteira, confirmação das referências, geração de ordem, cálculo do total, leitura do histórico, replay integral e persistência SHALL formar uma escrita atômica. O lock SHALL serializar Operações concorrentes da mesma Carteira antes de `MAX(ordemNoDia)+1`. Requisições concorrentes MUST preservar ordens únicas e impedir qualquer prefixo do replay com posição negativa; a constraint única SHALL permanecer somente como última defesa.

#### Scenario: Operações concorrentes financeiramente válidas
- **WHEN** duas Operações financeiramente válidas concorrem para a mesma Carteira, Ação e data sem rede real
- **THEN** ambas são persistidas, exatamente duas Operações existem, suas ordens formam o conjunto `{1, 2}`, não há duplicidade e o replay final permanece válido

#### Scenario: Vendas concorrentes excedem a posição em conjunto
- **WHEN** duas VENDAS seriam válidas isoladamente, mas inválidas em conjunto
- **THEN** somente as Operações compatíveis com o saldo são persistidas e nenhum prefixo do replay fica negativo

#### Scenario: Falha durante a criação
- **WHEN** geração de ordem, replay, integridade ou persistência falha
- **THEN** nenhuma Operação parcial é persistida e registros relacionados permanecem inalterados

### Requirement: Persistência relacional sem cascade delete
A Operação SHALL possuir identificador próprio, referências obrigatórias a Carteira e Ação, referência anulável a Corretora e os demais campos do contrato. As referências MUST preservar integridade no banco e MUST NOT usar cascade delete. A capability MUST NOT criar coleção bidirecional obrigatória nos agregados existentes, tabela de posição, histórico de cotação, snapshot ou campos financeiros consolidados.

#### Scenario: Corretora nula na persistência
- **WHEN** uma Operação é criada sem Corretora
- **THEN** somente a foreign key de Corretora permanece nula e as referências a Carteira e Ação continuam obrigatórias

#### Scenario: Tentativa de remover entidade referenciada
- **WHEN** uma Operação persistida referencia Carteira, Ação ou Corretora
- **THEN** a integridade relacional impede que a Operação seja apagada por cascade a partir dessas entidades

### Requirement: Separação dos conceitos de preço
O sistema SHALL tratar `Acao.cotacaoAtual` como a última cotação corrente conhecida, `HistoricoCotacao` como observações dessa cotação corrente e `Operacao.precoUnitario` como o valor financeiro persistido da Operação. Para nova COMPRA, `Operacao.precoUnitario` SHALL ser preenchido exclusivamente pelo fechamento histórico bruto da data exata; para VENDA, SHALL ser o preço informado pelo cliente. Somente o `precoUnitario` persistido na Operação SHALL participar de `valorTotal`, custo, preço médio e resultado. Cotação corrente e `HistoricoCotacao` MUST NOT substituir o fechamento externo da COMPRA nem ser alterados por ela.

#### Scenario: Fontes de preço por tipo
- **WHEN** novas COMPRA e VENDA são registradas
- **THEN** a COMPRA usa o fechamento bruto exato, a VENDA usa o preço informado e ambas calculam resultados futuros somente a partir do preço persistido

#### Scenario: Preço real como base financeira
- **WHEN** uma Operação possui preço persistido diferente da cotação corrente ou de `HistoricoCotacao`
- **THEN** somente o preço persistido conforme o tipo da Operação participa do valor total e dos cálculos financeiros futuros

#### Scenario: Histórico corrente permanece separado
- **WHEN** o fechamento externo usado numa COMPRA difere da cotação corrente ou de `HistoricoCotacao`
- **THEN** a COMPRA preserva o fechamento como preço sem alterar as demais fontes

### Requirement: Compatibilidade das funcionalidades existentes
A evolução SHALL preservar entidade, colunas NOT NULL, precisão, constraint cronológica, replay, posição atual, resultado realizado, consultas, snapshots e patrimônio existentes. SHALL aplicar a nova regra apenas a novos `POST /operacoes`, sem migration e sem modificar o changeSet histórico `004`. Operações existentes MUST NOT ter preços recalculados, ordens renumeradas nem consultas históricas retroativas.

#### Scenario: Regressão das APIs existentes
- **WHEN** APIs de consulta, posição, resultado, patrimônio e snapshot são executadas após a evolução
- **THEN** preservam seus contratos e cálculos vigentes sobre os valores persistidos

#### Scenario: Inicialização com Liquibase e Hibernate
- **WHEN** Liquibase executa e Hibernate valida o schema
- **THEN** a aplicação inicia sem novo changeSet para esta evolução

#### Scenario: Dados anteriores
- **WHEN** existem Operações persistidas antes da evolução
- **THEN** elas permanecem válidas e inalteradas

### Requirement: Preço unitário conforme o tipo da Operação
Para COMPRA, o sistema SHALL obter `precoUnitario` exclusivamente do `close` bruto do candle exatamente correspondente a `dataOperacao`; o cliente MUST NOT informá-lo e o sistema MUST NOT usar `adjustedClose`, cotação atual, `HistoricoCotacao`, `GLOBAL_QUOTE`, pregão anterior ou preço manual como fallback. Para VENDA, `precoUnitario` SHALL ser obrigatório, informado pelo cliente, positivo e exatamente representável com precisão máxima 19 e escala máxima 6, sem consulta histórica.

#### Scenario: Preço de COMPRA obtido do fechamento exato
- **WHEN** o provider retorna o candle da data solicitada
- **THEN** o sistema usa e persiste seu `close` bruto como `precoUnitario`

#### Scenario: COMPRA sem fechamento exato
- **WHEN** não existe fechamento exatamente em `dataOperacao` ou a data está fora do alcance determinável
- **THEN** o sistema retorna o erro histórico correspondente e não persiste a COMPRA

#### Scenario: Preço válido de VENDA
- **WHEN** o cliente informa em VENDA um preço positivo dentro da precisão e escala vigentes
- **THEN** o sistema preserva exatamente esse preço sem consultar provider

#### Scenario: Preço inválido de VENDA
- **WHEN** o preço da VENDA é ausente, zero, negativo ou excede precisão 19 ou escala 6
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

### Requirement: Ordem cronológica gerada dentro do dia
`ordemNoDia` SHALL ser gerada exclusivamente pelo backend como `MAX(ordemNoDia)+1` por Carteira, Ação e `dataOperacao`, depois do lock pessimista e dentro da transação de escrita. A primeira Operação da combinação e data SHALL receber 1. A soma SHALL rejeitar overflow antes de exceder o limite do tipo inteiro. A cronologia SHALL permanecer `dataOperacao ASC, ordemNoDia ASC`, seguida pelo critério técnico de desempate vigente quando aplicável, e a constraint única SHALL permanecer como última defesa.

#### Scenario: Primeira ordem do dia
- **WHEN** não existe Operação para a Carteira, Ação e data
- **THEN** o sistema atribui `ordemNoDia=1`

#### Scenario: Próxima ordem do dia
- **WHEN** a maior ordem existente para a combinação e data é 3
- **THEN** o sistema atribui `ordemNoDia=4`

#### Scenario: Limite da ordem
- **WHEN** a maior ordem não permite soma inteira exata de uma unidade
- **THEN** a criação falha de forma padronizada antes de overflow e sem persistência parcial

#### Scenario: Colisão inesperada da última defesa
- **WHEN** a constraint única ainda detecta colisão apesar da coordenação por lock
- **THEN** o sistema responde conflito de integridade padronizado sem orientar o cliente a informar ou alterar `ordemNoDia`

### Requirement: Cotação histórica integrada somente à COMPRA
`POST /operacoes` SHALL consultar fechamento histórico exclusivamente para nova COMPRA, depois das verificações preliminares de referências e antes da transação curta. Sua indisponibilidade SHALL impedir essa COMPRA. VENDA e consultas de Operação MUST permanecer independentes de BRAPI e Alpha Vantage. O fechamento obtido MUST ser usado somente no novo registro e MUST NOT modificar Operações existentes, cotação corrente ou `HistoricoCotacao`.

#### Scenario: Provider chamado somente para COMPRA
- **WHEN** requests válidos de COMPRA e VENDA são processados
- **THEN** somente a COMPRA consulta o provider histórico do mercado

#### Scenario: Falha histórica impede nova COMPRA
- **WHEN** o fechamento exato não pode ser obtido com classificação sustentada
- **THEN** a nova COMPRA falha com o erro correspondente e nenhuma escrita ocorre

#### Scenario: Operações existentes preservadas
- **WHEN** a nova regra entra em vigor
- **THEN** preços e ordens existentes não são recalculados, consultados ou renumerados
