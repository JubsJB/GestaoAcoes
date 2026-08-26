## Purpose

Definir a consulta do lucro ou prejuízo realizado acumulado pelas VENDAS de cada Ação em uma Carteira, preservando ciclos encerrados e mantendo-o independente da posição atualmente aberta.

## ADDED Requirements

### Requirement: Consulta REST de resultados realizados da Carteira
O sistema SHALL expor `GET /carteiras/{carteiraId}/resultados-realizados`, SHALL responder `200 OK` com uma lista quando a Carteira existir e MUST NOT exigir body, aceitar filtros ou paginação nem devolver `Location`. Esta primeira versão MUST NOT expor endpoint específico de resultado realizado por Ação ou detalhamento individual por VENDA.

#### Scenario: Carteira com resultados realizados
- **WHEN** a Carteira existe e ao menos uma combinação Carteira+Ação possui VENDA histórica
- **THEN** o sistema responde `200 OK` com uma entrada acumulada para cada Ação que possui VENDA

#### Scenario: Carteira sem Operações
- **WHEN** a Carteira existe e não possui Operações
- **THEN** o sistema responde `200 OK` com `[]`

#### Scenario: Carteira com somente COMPRAS
- **WHEN** a Carteira existe e nenhuma de suas Ações possui VENDA
- **THEN** o sistema responde `200 OK` com `[]`

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica uma Carteira persistida
- **THEN** o sistema responde `404 Not Found` pelo tratamento centralizado vigente e não altera registro algum

#### Scenario: Contrato sem rotas adicionais
- **WHEN** a capability é disponibilizada nesta primeira versão
- **THEN** não são criados endpoint por Ação, detalhamento por VENDA, filtros ou paginação

### Requirement: Representação acumulada por Carteira e Ação
Cada item SHALL representar o resultado realizado acumulado de uma única combinação Carteira+Ação e SHALL conter exclusivamente `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda` e `resultadoRealizado`. Somente Ações com pelo menos uma VENDA SHALL integrar a resposta. A lista SHALL ser ordenada por `mercado ASC`, `ticker ASC`, `acaoId ASC`; essa ordem de apresentação MUST NOT participar da cronologia financeira.

#### Scenario: DTO de resultado realizado
- **WHEN** uma Ação possui uma ou mais VENDAS na Carteira consultada
- **THEN** sua entrada contém os dados persistidos da Ação e um único `resultadoRealizado` acumulado

#### Scenario: Ação sem VENDA omitida
- **WHEN** uma Ação possui somente COMPRAS na Carteira
- **THEN** ela não integra a resposta de resultados realizados

#### Scenario: Resultado acumulado nulo matematicamente
- **WHEN** uma Ação possui VENDAS cujos resultados acumulam exatamente zero
- **THEN** ela integra a resposta com `resultadoRealizado=0.000000000000`, nunca `null`

#### Scenario: Ordenação determinística
- **WHEN** múltiplas Ações possuem VENDAS na mesma Carteira
- **THEN** as entradas são ordenadas por mercado, ticker e identificador da Ação sem alterar o replay de cada grupo

### Requirement: Fórmula oficial de cada VENDA
Para cada VENDA, o sistema SHALL calcular `resultadoRealizadoVenda = (precoUnitarioVenda - precoMedioVigenteAntesDaVenda) × quantidadeVendida`. Resultado positivo SHALL representar lucro realizado, resultado negativo SHALL representar prejuízo realizado e resultado zero SHALL representar VENDA sem lucro ou prejuízo. COMPRA MUST NOT gerar resultado realizado.

#### Scenario: Venda com lucro
- **WHEN** uma posição com preço médio vigente `10` vende `40` unidades a `15`
- **THEN** o resultado realizado dessa VENDA é `200`

#### Scenario: Venda com prejuízo
- **WHEN** uma posição com preço médio vigente `20` vende `30` unidades a `15`
- **THEN** o resultado realizado dessa VENDA é `-150`

#### Scenario: Venda no preço médio
- **WHEN** o preço unitário da VENDA é numericamente igual ao preço médio vigente
- **THEN** o resultado realizado dessa VENDA é zero

#### Scenario: Compra não realiza resultado
- **WHEN** uma ou mais COMPRAS alteram quantidade, custo e preço médio
- **THEN** o resultado realizado permanece inalterado até ocorrer uma VENDA

### Requirement: Acumulação de todas as VENDAS da Ação
Para cada Carteira+Ação, `resultadoRealizado` SHALL ser a soma dos resultados de todas as VENDAS de todos os ciclos cronológicos desse grupo. O sistema MUST NOT arredondar cada resultado de VENDA antes da soma nem descartar resultados porque a posição atual está zerada.

#### Scenario: Múltiplas vendas com sinais diferentes
- **WHEN** uma posição compra `100` a `10`, vende `20` a `15` e vende `30` a `8`
- **THEN** os resultados `100` e `-60` são acumulados e `resultadoRealizado` é `40.000000000000`

#### Scenario: Múltiplas compras antes da venda
- **WHEN** COMPRAS sucessivas formam um preço médio ponderado e uma VENDA ocorre depois delas
- **THEN** a VENDA usa exatamente o preço médio vigente resultante das COMPRAS anteriores

#### Scenario: Compra após venda parcial
- **WHEN** uma VENDA parcial realiza resultado e uma COMPRA posterior recalcula o preço médio da posição remanescente
- **THEN** o resultado já realizado permanece acumulado e não participa do novo preço médio

### Requirement: Venda parcial, encerramento e novo ciclo
Uma VENDA parcial SHALL calcular e acumular o resultado da quantidade vendida antes de reduzir quantidade e custo, e SHALL preservar o preço médio da posição remanescente. Uma VENDA total SHALL calcular e acumular o resultado antes de zerar quantidade, custo e preço médio internos. COMPRA posterior ao zeramento SHALL iniciar novo ciclo contábil, enquanto o resultado realizado histórico dos ciclos anteriores SHALL permanecer no acumulado da mesma Carteira+Ação.

#### Scenario: Venda parcial
- **WHEN** uma posição de `100` unidades com preço médio `10` vende `40` a `15`
- **THEN** acumula resultado realizado `200` e permanece com quantidade `60`, custo `600` e preço médio `10`

#### Scenario: Venda total preserva resultado
- **WHEN** uma VENDA encerra integralmente uma posição
- **THEN** seu resultado é acumulado antes do zeramento e continua disponível na consulta mesmo sem posição aberta

#### Scenario: Novo ciclo independente
- **WHEN** uma posição compra `100` a `10`, vende `100` a `15` e depois compra `50` a `20`
- **THEN** o novo ciclo possui quantidade `50`, preço médio `20` e custo `1000`, enquanto o resultado realizado `500` do ciclo anterior permanece acumulado separadamente

#### Scenario: Múltiplos ciclos
- **WHEN** uma Carteira+Ação encerra e reinicia a posição uma ou mais vezes
- **THEN** cada ciclo começa contabilmente do zero e todas as VENDAS continuam contribuindo para o mesmo acumulado histórico do grupo

### Requirement: Independência da posição atualmente aberta
O resultado realizado SHALL representar exclusivamente o efeito financeiro das quantidades vendidas. Ele MUST NOT alterar ou ser incorporado a `quantidadeAtual`, `precoMedio`, `custoPosicao`, `cotacaoAtual`, `dataHoraCotacao`, `valorAtualPosicao`, `resultadoNaoRealizado` ou `rentabilidadePercentual`, e MUST NOT ser somado automaticamente ao resultado não realizado ou formar resultado total.

#### Scenario: Posição encerrada com resultado histórico
- **WHEN** a quantidade atual de uma Ação é zero e existe VENDA histórica
- **THEN** a Ação é omitida de `GET /carteiras/{carteiraId}/posicoes` e permanece presente na consulta de resultados realizados

#### Scenario: Posição aberta com vendas anteriores
- **WHEN** uma Ação possui posição atualmente aberta e uma ou mais VENDAS históricas
- **THEN** a consulta de resultados devolve todas as VENDAS acumuladas e a consulta de posições preserva exclusivamente os indicadores da posição aberta

#### Scenario: Cotação alterada
- **WHEN** `cotacaoAtual` ou `dataHoraCotacao` da Ação é atualizada
- **THEN** o resultado realizado histórico permanece inalterado

### Requirement: Política numérica do resultado realizado
O cálculo SHALL usar exclusivamente `BigDecimal`. O preço médio vigente usado por cada VENDA SHALL ser o estado interno em escala 24, e MUST NOT ser o preço médio já projetado em escala 12. A diferença entre preço de venda e preço médio interno e sua multiplicação pela quantidade exata MUST NOT sofrer arredondamento adicional; os resultados das VENDAS SHALL ser acumulados antes de qualquer normalização final. Somente ao fim do replay do grupo, o acumulado SHALL ser normalizado para escala 12 com `RoundingMode.HALF_EVEN` e precisão total máxima 38. O sistema MUST NOT usar `float`, `double`, truncamento ou normalização individual de cada VENDA.

#### Scenario: Preço médio periódico
- **WHEN** COMPRAS produzem preço médio periódico antes de uma VENDA
- **THEN** o cálculo usa as 24 casas do estado interno e normaliza somente o acumulado final para escala 12

#### Scenario: Soma antes da normalização
- **WHEN** múltiplas VENDAS produzem resultados internos com mais de 12 casas decimais
- **THEN** o sistema soma os resultados internos e aplica `HALF_EVEN` uma única vez ao acumulado final

#### Scenario: Resultado positivo, negativo ou zero
- **WHEN** o acumulado final possui qualquer um desses sinais
- **THEN** ele é devolvido com escala 12, incluindo zero não nulo

#### Scenario: Resultado fora da precisão
- **WHEN** o acumulado não pode ser representado com precisão máxima 38 e escala 12
- **THEN** toda a consulta falha com `422 Unprocessable Entity` e código `CALCULO_POSICAO_FORA_DA_PRECISAO`, sem resposta parcial

### Requirement: Cronologia e histórico inconsistente
O replay SHALL processar cada Carteira+Ação por `dataOperacao ASC` e `ordemNoDia ASC`. O identificador da Operação MAY estabilizar a leitura global somente depois dessas chaves e MUST NOT substituir `ordemNoDia` nem redefinir a ordem financeira. Histórico persistido que viole saldo, tipo, quantidade, preço, agrupamento ou cronologia SHALL fazer toda a consulta falhar com `409 Conflict` e código `HISTORICO_OPERACOES_INCONSISTENTE`, sem ignorar, corrigir ou devolver grupos parciais.

#### Scenario: Ordem intradiária altera preço médio vigente
- **WHEN** COMPRAS e VENDAS ocorrem na mesma data
- **THEN** `ordemNoDia` determina o preço médio disponível imediatamente antes de cada VENDA

#### Scenario: ID é somente desempate técnico
- **WHEN** a leitura global contém Operações de Ações distintas
- **THEN** o ID pode estabilizar a consulta sem participar do replay financeiro dentro de cada grupo

#### Scenario: Saldo cronológico negativo persistido
- **WHEN** uma VENDA persistida excede a quantidade disponível no ponto cronológico
- **THEN** a consulta responde `409 / HISTORICO_OPERACOES_INCONSISTENTE` e não devolve resultado parcial

#### Scenario: Venda inválida no cadastro
- **WHEN** o cliente tenta cadastrar nova VENDA superior à posição disponível
- **THEN** o cadastro preserva `409 / POSICAO_INSUFICIENTE`, sem substituir esse código pelo erro de consulta histórica

### Requirement: Moeda e isolamento entre Carteiras
Cada resultado realizado SHALL permanecer na moeda persistida da respectiva Ação: `BRL` para BRASIL e `USD` para EUA. Operações de outra Carteira ou Ação MUST NOT afetar o acumulado do grupo. O sistema MUST NOT converter moedas nem somar diretamente resultados em BRL e USD em um único valor monetário de Carteira.

#### Scenario: Ação brasileira
- **WHEN** a Ação com VENDAS pertence ao mercado BRASIL
- **THEN** seu resultado realizado permanece em BRL

#### Scenario: Ação americana fracionária
- **WHEN** a Ação com VENDAS pertence ao mercado EUA e usa quantidade fracionária válida
- **THEN** seu resultado realizado permanece em USD e preserva a política decimal aprovada

#### Scenario: Carteira com moedas diferentes
- **WHEN** uma Carteira possui resultados realizados em Ações BRL e USD
- **THEN** a resposta contém itens independentes por Ação e não apresenta total monetário da Carteira

#### Scenario: Mesma Ação em Carteiras diferentes
- **WHEN** duas Carteiras possuem VENDAS da mesma Ação
- **THEN** cada consulta acumula exclusivamente as Operações da Carteira solicitada

### Requirement: Consulta consistente e sem efeitos colaterais
A consulta SHALL observar Carteira, Operações e Ações em transação read-only com `Isolation.REPEATABLE_READ`, MUST NOT adquirir lock pessimista, escrever dados, usar `Clock`, executar chamada HTTP, consultar BRAPI, Alpha Vantage ou `CotacaoProvider`, invocar internamente o PATCH de cotação, executar consulta por VENDA ou por Ação, criar N+1, cache ou materialização. A Ação associada SHALL ser obtida junto ao histórico necessário, e o cálculo SHALL ocorrer em memória em um único replay linear por grupo.

#### Scenario: Consistência concorrente
- **WHEN** uma Operação é registrada concorrentemente com a consulta
- **THEN** a resposta reflete integralmente um snapshot anterior ou posterior à Operação, sem misturar estado parcial

#### Scenario: Múltiplas Ações sem N+1
- **WHEN** a Carteira possui VENDAS de múltiplas Ações
- **THEN** o histórico e as Ações associadas são carregados sem uma consulta adicional por Ação ou VENDA

#### Scenario: Providers indisponíveis
- **WHEN** providers de cotação estão indisponíveis
- **THEN** a consulta continua determinada exclusivamente pelas Operações persistidas

#### Scenario: Consulta sem mutação
- **WHEN** a consulta é executada uma ou mais vezes sem nova Operação
- **THEN** nenhum registro é criado ou alterado e o resultado permanece determinístico

### Requirement: Compatibilidade sem persistência adicional
A capability SHALL operar sobre as entidades, repositories e schema vigentes e MUST NOT persistir resultado realizado nem criar entidade, repository exclusivo, tabela, migration ou snapshot. Os contratos de registro e consulta de Operações, posição consolidada, DELETE de Carteira e `PATCH /acoes/{id}/cotacao` SHALL permanecer inalterados. Resultado detalhado por VENDA, resultado total de Carteira, patrimônio, rentabilidade realizada ou total, TWR, XIRR, dividendos, juros, taxas, impostos, conversão cambial e cotação histórica MUST NOT ser introduzidos nesta change.

#### Scenario: Schema vigente
- **WHEN** H2 ou PostgreSQL inicializa com os changeSets 001–004 e Hibernate em validação
- **THEN** a consulta funciona sem nova migration, coluna ou tabela

#### Scenario: Posição consolidada preservada
- **WHEN** a capability de resultado realizado é disponibilizada
- **THEN** todos os campos e cálculos vigentes de `GET /carteiras/{carteiraId}/posicoes` permanecem inalterados

#### Scenario: Atualização dedicada de cotação preservada
- **WHEN** `PATCH /acoes/{id}/cotacao` atualiza a cotação persistida
- **THEN** seu contrato permanece independente e nenhum resultado realizado é recalculado por cotação

#### Scenario: Ausência de detalhamento e agregações futuras
- **WHEN** o cliente consulta resultados realizados nesta primeira versão
- **THEN** não recebe eventos por VENDA, total monetário da Carteira, patrimônio ou indicador de rentabilidade histórica
