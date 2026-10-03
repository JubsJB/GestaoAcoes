## RENAMED Requirements

- FROM: `### Requirement: Cotação histórica integrada somente à COMPRA`
- TO: `### Requirement: Registro independente de fechamento histórico`

## MODIFIED Requirements

### Requirement: Contrato REST de criação de Operação
O sistema SHALL expor `POST /operacoes` com contrato discriminado pelo campo `tipo`. COMPRA SHALL aceitar exclusivamente `carteiraId`, `ticker`, `mercado`, `corretoraId`, `tipo=COMPRA`, `quantidade`, `dataOperacao` e `precoUnitario` obrigatorio; `ordemNoDia` MUST ser proibido. VENDA SHALL aceitar os mesmos campos com `tipo=VENDA` e SHALL exigir `precoUnitario`; `ordemNoDia` MUST ser proibido. `corretoraId` SHALL aceitar omissão ou valor nulo; quando informado, SHALL referenciar uma Corretora existente. Qualquer campo desconhecido ou controlado pela aplicação SHALL ser rejeitado.

#### Scenario: COMPRA com preço
- **WHEN** o cliente envia uma COMPRA válida com `precoUnitario` positivo, sem `ordemNoDia` e opcionalmente com `corretoraId`
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

#### Scenario: COMPRA sem preco obrigatorio
- **WHEN** o cliente omite `precoUnitario` em uma COMPRA ou informa nulo
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

#### Scenario: COMPRA sem preço
- **WHEN** uma COMPRA omite precoUnitario
- **THEN** o backend rejeita a entrada sem consultar fechamento historico

#### Scenario: COMPRA com preço proibido
- **WHEN** uma COMPRA informa precoUnitario positivo conforme a nova regra aprovada
- **THEN** o backend aceita o preco manual; a antiga proibicao deixa de vigorar

### Requirement: Resposta da criação concluída
Uma criação concluída SHALL responder `201 Created`, incluir `Location: /operacoes/{id}` e devolver `OperacaoResponse` contendo `id`, `carteiraId`, ticker normalizado, `mercado`, `corretoraId` anulável, `tipo`, `quantidade`, `precoUnitario`, `dataOperacao`, `ordemNoDia` e `valorTotal` efetivamente persistidos. Em COMPRA e VENDA, `precoUnitario` SHALL ser o preço final informado pelo cliente e validado pelo backend.

#### Scenario: Compra criada
- **WHEN** uma COMPRA válida informa preço válido e é persistida
- **THEN** o response contém o preço informado e validado, a ordem gerada e o total calculado

#### Scenario: Venda criada
- **WHEN** uma VENDA válida é persistida
- **THEN** o response contém o preço informado, a ordem gerada e o total calculado

### Requirement: Seleção obrigatória de Ação por ticker e mercado
O sistema SHALL normalizar o ticker pela regra vigente, localizar uma Ação persistida pela combinação exata de ticker e `Mercado` e aceitar somente `BRASIL` e `EUA`. O cliente MUST NOT informar `acaoId`; o registro MUST NOT cadastrar ou modificar Ação. COMPRA e VENDA SHALL confirmar as referências existentes e MUST NOT consultar provider no cadastro.

#### Scenario: Ação brasileira existente
- **WHEN** ticker e mercado identificam uma Ação persistida e o request é COMPRA
- **THEN** a Operação referencia a Ação brasileira sem consultar a BRAPI

#### Scenario: Ação americana existente
- **WHEN** ticker e `mercado=EUA` identificam uma Ação persistida
- **THEN** a Operação referencia a Ação americana sem consultar a Alpha Vantage

#### Scenario: Normalização do ticker
- **WHEN** o cliente informa ticker com espaços ou caixa não normalizada
- **THEN** o sistema procura e responde com o ticker normalizado pela regra vigente

#### Scenario: Ação não cadastrada
- **WHEN** nenhuma Ação corresponde ao ticker normalizado e mercado
- **THEN** o sistema responde `404 Not Found`, não consulta provider e não cadastra Ação

### Requirement: Valor total calculado com exatidão
O cliente MUST NOT informar `valorTotal`. O sistema SHALL calcular `valorTotal = quantidade × precoUnitario` com aritmética decimal exata e sem arredondamento ou truncamento, usando exclusivamente o preço final informado e validado em COMPRA e VENDA. O resultado SHALL caber em precisão 38 e escala 12.

#### Scenario: Cálculo do valor total
- **WHEN** uma COMPRA informa `precoUnitario=32.47` e possui `quantidade=100`
- **THEN** `valorTotal` representa exatamente `3247.00`

#### Scenario: Total de VENDA
- **WHEN** uma VENDA informa preço válido
- **THEN** o total é calculado exclusivamente pelo backend com quantidade e preço da VENDA

#### Scenario: Cotação não participa do cálculo
- **WHEN** a Operação é COMPRA ou VENDA
- **THEN** nenhuma cotação participa do total, que usa exclusivamente o preço informado e a quantidade

#### Scenario: Resultado fora da precisão
- **WHEN** o produto não pode ser representado exatamente nos limites aprovados
- **THEN** a criação é rejeitada sem persistência parcial

### Requirement: Registro de COMPRA sem consolidação financeira
Uma COMPRA válida SHALL usar o preço final informado e validado, persistir somente os dados da própria Operação e aumentar a quantidade cronologicamente disponível para validações subsequentes. Falha, ausência ou indisponibilidade da prévia MUST NOT impedir a COMPRA quando preço e demais validações forem válidos. O POST MUST NOT consultar a prévia ou provider histórico, manter lock associado a chamada de rede nem alterar cotação corrente, histórico de cotação corrente, dados existentes ou consolidações persistidas. Nesta change, a COMPRA MUST NOT persistir posição, recalcular ou armazenar preço médio, custo consolidado, resultado, rentabilidade, patrimônio ou snapshot.

#### Scenario: Primeira compra com fechamento exato
- **WHEN** uma primeira COMPRA válida informa preço válido e conclui a validação transacional
- **THEN** a Operação é persistida com o preço informado e validado e sua quantidade integra o saldo derivado

#### Scenario: Primeira compra
- **WHEN** uma COMPRA válida informa preço válido sem Operações anteriores para a mesma Carteira e Ação
- **THEN** a Operação é persistida sem consolidação financeira e sua quantidade integra o saldo derivado

#### Scenario: Compras múltiplas
- **WHEN** múltiplas COMPRAS válidas são registradas na ordem cronológica definida
- **THEN** suas quantidades integram a soma comprada sem criar posição ou preço médio persistido

#### Scenario: Falha externa antes da transação
- **WHEN** a prévia histórica falha e o cliente envia COMPRA com preço informado válido e demais validações satisfeitas
- **THEN** a COMPRA é persistida atomicamente sem consulta histórica no POST e nenhum lock de Carteira fica associado à rede ou timeout da prévia

### Requirement: Inserção retroativa preserva toda a sequência
O sistema SHALL aceitar Operação retroativa somente quando todo o replay da mesma Carteira e Ação permanecer sem saldo negativo. Uma nova Operação em data que já contém Operações SHALL ser anexada ao final daquele dia por `MAX(ordemNoDia)+1`; não haverá inserção entre ordens existentes, reordenação manual ou `horaOperacao`. O usuário SHALL cadastrar Operações do mesmo dia na sequência real desejada.

#### Scenario: Operação retroativa no mesmo dia
- **WHEN** a data retroativa já possui Operações para a combinação
- **THEN** a candidata recebe a próxima ordem e é reproduzida depois das Operações já existentes naquele dia

#### Scenario: Compra retroativa compatível
- **WHEN** uma COMPRA retroativa com preço informado válido é anexada ao fim de sua data e todo o replay permanece válido
- **THEN** o sistema persiste a COMPRA sem alterar Operações posteriores

#### Scenario: Venda retroativa que invalida venda posterior
- **WHEN** a candidata torna negativo o saldo em seu ponto ou em qualquer Operação posterior
- **THEN** o sistema responde `409 POSICAO_INSUFICIENTE` e preserva o histórico existente

### Requirement: Atomicidade e consistência concorrente
COMPRA e VENDA MUST NOT realizar consulta externa no POST. Dentro da transação, lock pessimista da Carteira, confirmação das referências, geração de ordem, cálculo do total, leitura do histórico, replay integral e persistência SHALL formar uma escrita atômica. O lock SHALL serializar Operações concorrentes da mesma Carteira antes de `MAX(ordemNoDia)+1`. Requisições concorrentes MUST preservar ordens únicas e impedir qualquer prefixo do replay com posição negativa; a constraint única SHALL permanecer somente como última defesa.

#### Scenario: Operações concorrentes financeiramente válidas
- **WHEN** duas Operações financeiramente válidas concorrem para a mesma Carteira, Ação e data sem consulta externa no cadastro
- **THEN** ambas são persistidas, exatamente duas Operações existem, suas ordens formam o conjunto `{1, 2}`, não há duplicidade e o replay final permanece válido

#### Scenario: Vendas concorrentes excedem a posição em conjunto
- **WHEN** duas VENDAS seriam válidas isoladamente, mas inválidas em conjunto
- **THEN** somente as Operações compatíveis com o saldo são persistidas e nenhum prefixo do replay fica negativo

#### Scenario: Falha durante a criação
- **WHEN** geração de ordem, replay, integridade ou persistência falha
- **THEN** nenhuma Operação parcial é persistida e registros relacionados permanecem inalterados

### Requirement: Preço unitário conforme o tipo da Operação
COMPRA e VENDA SHALL exigir precoUnitario informado pelo usuario, positivo e exatamente representavel em NUMERIC(19,6). POST MUST NOT consultar ou substituir preco por fechamento historico/cotacao atual. Total e replay SHALL permanecer conforme regras existentes. Endpoints historicos SHALL permanecer independentes.

#### Scenario: Preco manual
- **WHEN** COMPRA ou VENDA recebe preco valido
- **THEN** persiste exatamente o preco validado e calcula total no backend sem consulta historica

#### Scenario: Preco invalido
- **WHEN** preco falta, e nulo, zero, negativo ou excede precisao
- **THEN** rejeita sem persistencia e sem chamada externa

#### Scenario: Preço de COMPRA obtido do fechamento exato
- **WHEN** uma COMPRA informa preco diferente do fechamento historico
- **THEN** o preco manual prevalece; o fechamento nao e consultado

#### Scenario: COMPRA sem fechamento exato
- **WHEN** nao existe fechamento exato para a data de uma COMPRA valida
- **THEN** o registro manual independe desse fechamento

#### Scenario: Preço válido de VENDA
- **WHEN** o cliente informa em VENDA um preço positivo dentro da precisão e escala vigentes
- **THEN** o sistema preserva exatamente esse preço sem consultar provider

#### Scenario: Preço inválido de VENDA
- **WHEN** o preço da VENDA é ausente, zero, negativo ou excede precisão 19 ou escala 6
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO` e não persiste Operação

### Requirement: Registro independente de fechamento histórico
COMPRA e VENDA SHALL exigir precoUnitario informado pelo usuario, positivo e exatamente representavel em NUMERIC(19,6). POST MUST NOT consultar ou substituir preco por fechamento historico/cotacao atual. Total e replay SHALL permanecer conforme regras existentes. Endpoints historicos SHALL permanecer independentes.

#### Scenario: Preco manual
- **WHEN** COMPRA ou VENDA recebe preco valido
- **THEN** persiste exatamente o preco validado e calcula total no backend sem consulta historica

#### Scenario: Preco invalido
- **WHEN** preco falta, e nulo, zero, negativo ou excede precisao
- **THEN** rejeita sem persistencia e sem chamada externa

#### Scenario: Provider chamado somente para COMPRA
- **WHEN** COMPRA ou VENDA e registrada
- **THEN** nenhum provider historico e chamado pelo POST

#### Scenario: Falha histórica impede nova COMPRA
- **WHEN** o provider historico esta indisponivel
- **THEN** a COMPRA manual valida continua permitida pela nova regra

#### Scenario: Operações existentes preservadas
- **WHEN** a nova regra entra em vigor
- **THEN** preços e ordens existentes não são recalculados, consultados ou renumerados
