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

### Requirement: Cotação histórica integrada somente à COMPRA
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

### Requirement: Separação dos conceitos de preço
O sistema SHALL manter cotacao corrente, historico de cotacoes e preco da Operacao separados. COMPRA e VENDA SHALL persistir exclusivamente o precoUnitario positivo informado pelo cliente. Apenas o preco persistido SHALL participar de valorTotal, custo, preco medio e resultado; cotacoes MUST NOT substituir o preco enviado nem ser alteradas pelo cadastro.

#### Scenario: Fontes independentes
- **WHEN** COMPRA ou VENDA informa preco diferente da cotacao corrente ou historica
- **THEN** o preco informado e persistido participa dos calculos existentes sem alterar as cotacoes


#### Scenario: Fontes de preço por tipo
- **WHEN** COMPRA ou VENDA e registrada
- **THEN** o preco informado pelo cliente e persistido sem substituicao por cotacao


#### Scenario: Preço real como base financeira
- **WHEN** uma Operação possui preço persistido diferente da cotação corrente ou de `HistoricoCotacao`
- **THEN** somente o preço persistido conforme o tipo da Operação participa do valor total e dos cálculos financeiros futuros


#### Scenario: Histórico corrente permanece separado
- **WHEN** o preco manual difere do historico corrente
- **THEN** o cadastro preserva as cotacoes e usa somente o preco manual na operacao
