## Purpose

Definir a consulta da posição contábil consolidada das Ações de uma Carteira, calculada sob demanda e de forma determinística exclusivamente a partir das Operações persistidas.

## ADDED Requirements

### Requirement: Consulta REST das posições consolidadas da Carteira
O sistema SHALL expor `GET /carteiras/{carteiraId}/posicoes`, SHALL responder `200 OK` com uma lista de posições consolidadas atualmente abertas, SHALL ordenar a resposta por `mercado ASC`, `ticker ASC`, `acaoId ASC` e MUST NOT devolver `Location`. Essa ordenação SHALL servir somente à apresentação e MUST NOT participar do replay financeiro. Esta primeira versão MUST NOT adicionar consulta individual de posição, filtros ou paginação.

#### Scenario: Carteira com uma posição aberta
- **WHEN** a Carteira existe e seu histórico resulta em quantidade positiva para uma Ação
- **THEN** o sistema responde `200 OK` com a posição consolidada dessa Ação

#### Scenario: Carteira com múltiplas posições abertas
- **WHEN** a Carteira existe e possui quantidades positivas para Ações distintas
- **THEN** o sistema responde `200 OK` com uma posição independente para cada Ação, ordenada por mercado, ticker e identificador da Ação

#### Scenario: Ordenação de resposta não altera o replay
- **WHEN** a ordem de apresentação das posições difere da ordem cronológica das Operações
- **THEN** a lista usa mercado, ticker e `acaoId`, enquanto cada cálculo continua usando somente `dataOperacao` e `ordemNoDia`

#### Scenario: Contrato sem rota individual
- **WHEN** a capability é disponibilizada nesta primeira versão
- **THEN** somente a listagem por Carteira é acrescentada, sem `GET /carteiras/{carteiraId}/posicoes/{acaoId}`

### Requirement: Existência da Carteira e resposta vazia
O sistema SHALL validar a existência da Carteira antes da consolidação. Carteira existente sem Operações ou sem posição atualmente aberta SHALL produzir `200 OK` com `[]`. Carteira inexistente SHALL produzir `404 Not Found` no formato `StandardError` vigente, sem criar ou alterar qualquer registro.

#### Scenario: Carteira existente sem Operações
- **WHEN** `carteiraId` identifica uma Carteira persistida sem Operações
- **THEN** o sistema responde `200 OK` com o array vazio `[]`

#### Scenario: Carteira existente somente com posições encerradas
- **WHEN** todas as posições derivadas da Carteira terminam com quantidade igual a zero
- **THEN** o sistema responde `200 OK` com o array vazio `[]`, preservando o histórico nas Operações

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica uma Carteira persistida
- **THEN** o sistema responde `404 Not Found` pelo tratamento centralizado atual

### Requirement: Representação contábil da posição
Cada posição SHALL conter `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `quantidadeAtual`, `precoMedio` e `custoPosicao`. Os identificadores e dados descritivos da Ação SHALL refletir o relacionamento persistido. A resposta MUST NOT incluir cotação atual, data da cotação, valor de mercado, resultado realizado, resultado não realizado, rentabilidade, patrimônio ou snapshot.

#### Scenario: Posição brasileira
- **WHEN** uma posição aberta referencia Ação de `mercado=BRASIL` e `moeda=BRL`
- **THEN** a resposta identifica a Ação e apresenta os valores contábeis na moeda persistida, sem consultar cotação

#### Scenario: Posição americana
- **WHEN** uma posição aberta referencia Ação de `mercado=EUA` e `moeda=USD`
- **THEN** a resposta preserva mercado, moeda e quantidade fracionária aplicável sem conversão cambial

#### Scenario: Ausência de indicadores de mercado
- **WHEN** qualquer posição é retornada
- **THEN** a resposta contém somente identidade, descrição, moeda e valores contábeis aprovados

### Requirement: Fonte única e isolamento do cálculo
O sistema SHALL calcular cada posição usando exclusivamente as Operações persistidas da mesma combinação de Carteira e Ação. Operações de outra Carteira ou de outra Ação MUST NOT afetar quantidade, preço médio ou custo. `Operacao.precoUnitario` SHALL ser o único preço usado no replay; `Acao.cotacaoAtual`, cotação histórica e providers externos MUST NOT participar.

#### Scenario: Mesma Ação em Carteiras diferentes
- **WHEN** duas Carteiras possuem Operações da mesma Ação
- **THEN** cada Carteira apresenta posição calculada somente com o próprio histórico

#### Scenario: Ações diferentes na mesma Carteira
- **WHEN** uma Carteira possui Operações de Ações distintas
- **THEN** o sistema consolida cada Ação em estado independente

#### Scenario: Cotação diferente do preço negociado
- **WHEN** `Acao.cotacaoAtual` difere dos preços registrados nas Operações
- **THEN** quantidade, preço médio e custo permanecem determinados somente por `Operacao.precoUnitario`

### Requirement: Ordem financeira do replay
O sistema SHALL reproduzir as Operações de cada Carteira e Ação por `dataOperacao ASC` e `ordemNoDia ASC`. Quando a leitura global usar o identificador da Operação como desempate técnico, ele SHALL ocupar somente a terceira chave e MUST NOT substituir `ordemNoDia`, alterar a sequência financeira dentro da posição ou redefinir o histórico.

#### Scenario: Operações cadastradas fora de ordem cronológica
- **WHEN** o histórico contém Operações retroativas válidas que foram persistidas depois de Operações mais recentes
- **THEN** o replay usa data e ordem no dia, independentemente da ordem física de inserção

#### Scenario: Múltiplas Operações no mesmo dia
- **WHEN** existem Operações da mesma Carteira e Ação na mesma data
- **THEN** a menor `ordemNoDia` é processada primeiro

#### Scenario: ID usado somente para estabilidade de leitura
- **WHEN** a consulta do histórico usa `id` como terceira chave técnica entre grupos independentes
- **THEN** o cálculo de cada posição continua determinado exclusivamente por data e ordem no dia

### Requirement: Quantidade atual por mercado
O replay SHALL iniciar `quantidadeAtual` em zero, adicionar a quantidade de cada `COMPRA` e subtrair a quantidade de cada `VENDA` usando `BigDecimal`. A quantidade final SHALL preservar a representação exata admitida nas Operações: matematicamente inteira para `BRASIL` e com até seis casas decimais para `EUA`. O sistema MUST NOT usar `float`, `double`, arredondar ou truncar quantidade.

#### Scenario: Quantidade brasileira
- **WHEN** o histórico brasileiro contém compras e vendas válidas
- **THEN** a quantidade atual é um valor matematicamente inteiro igual ao total comprado menos o total vendido

#### Scenario: Quantidade americana fracionária
- **WHEN** o histórico americano contém quantidades fracionárias válidas
- **THEN** a quantidade atual preserva exatamente a diferença decimal entre compras e vendas

#### Scenario: Venda exatamente igual à quantidade
- **WHEN** uma VENDA consome toda a quantidade disponível naquele ponto
- **THEN** a quantidade da posição passa exatamente a zero

### Requirement: Compra recalcula preço médio ponderado
Uma `COMPRA` SHALL aumentar a quantidade e o custo pelo produto exato `quantidade × precoUnitario` e SHALL recalcular o preço médio como `novoCusto / novaQuantidade`. O cálculo MUST NOT usar média aritmética simples, preço de mercado, taxa, corretagem, emolumento ou imposto.

#### Scenario: Compra única
- **WHEN** a primeira Operação do ciclo é uma COMPRA de 100 unidades a 10
- **THEN** a posição passa a quantidade 100, preço médio 10 e custo 1000

#### Scenario: Compras com preços diferentes
- **WHEN** a posição recebe COMPRA de 100 unidades a 10 e depois COMPRA de 50 unidades a 11
- **THEN** o preço médio representa `1550 / 150`, com divisão em escala 24 por `HALF_EVEN` e apresentação em escala 12

#### Scenario: Compras no mesmo preço
- **WHEN** todas as compras do ciclo possuem o mesmo preço unitário
- **THEN** o preço médio permanece numericamente igual a esse preço

### Requirement: Venda preserva o preço médio remanescente
Uma `VENDA` SHALL reduzir a quantidade e o custo proporcionalmente ao preço médio vigente imediatamente antes dela. O preço recebido na VENDA MUST NOT recalcular o preço médio da posição remanescente. Para quantidade remanescente positiva, o preço médio SHALL permanecer igual ao vigente antes da VENDA, usando escala intermediária 24 e `HALF_EVEN` somente na divisão inevitável.

#### Scenario: Venda parcial
- **WHEN** uma posição de 100 unidades, preço médio 10 e custo 1000 vende 40 unidades
- **THEN** a posição remanescente possui quantidade 60, preço médio 10 e custo 600, independentemente do preço da VENDA

#### Scenario: Venda com preço diferente do preço médio
- **WHEN** uma VENDA possui `precoUnitario` maior ou menor que o preço médio vigente
- **THEN** o preço da VENDA não altera `precoMedio` nem define o custo das unidades remanescentes

### Requirement: Zeramento e início de novo ciclo
Quando uma VENDA reduzir a quantidade exatamente a zero, o replay SHALL definir quantidade, custo e preço médio internos como zero. Uma COMPRA cronologicamente posterior SHALL iniciar novo ciclo com custo e preço médio determinados somente pelas compras posteriores ao zeramento. Posições com quantidade final zero MUST NOT integrar a listagem de posições atuais.

#### Scenario: Venda total
- **WHEN** uma VENDA consome exatamente toda a quantidade restante
- **THEN** quantidade, custo e preço médio são zerados sem apagar as Operações

#### Scenario: Nova compra após encerramento
- **WHEN** uma posição compra 100 a 10, vende 100 e posteriormente compra 50 a 20
- **THEN** a posição final possui quantidade 50, preço médio 20 e custo 1000, sem carregar custo do ciclo encerrado

### Requirement: Compra posterior a venda parcial
Quando uma nova COMPRA ocorrer após VENDA parcial, o sistema SHALL usar o custo remanescente e a quantidade remanescente como base do novo preço médio ponderado.

#### Scenario: Reponderação após venda parcial
- **WHEN** a posição compra 100 a 10, vende 40 e depois compra 40 a 20
- **THEN** o replay calcula quantidade 100, custo 1400 e preço médio 14

### Requirement: Precisão e arredondamento explícitos
O replay SHALL usar aritmética `BigDecimal`, manter escala interna de cálculo 24 nas divisões proporcionais e aplicar `RoundingMode.HALF_EVEN` explicitamente. `precoMedio` SHALL ser apresentado com escala 12 e até 13 dígitos inteiros; `custoPosicao` SHALL ser apresentado com escala 12 e precisão total máxima 38. Multiplicações e somas exatas MUST NOT ser arredondadas antes de uma divisão inevitável, e a quantidade MUST permanecer exata. Resultado que não puder ser representado nesses limites SHALL falhar integralmente com erro padronizado, sem truncamento nem resposta parcial.

#### Scenario: Divisão exata
- **WHEN** custo dividido por quantidade possui representação decimal finita dentro dos limites
- **THEN** o valor é preservado e apresentado na escala aprovada sem perda numérica

#### Scenario: Divisão periódica
- **WHEN** o preço médio matemático é periódico, como `1550 / 150`
- **THEN** o sistema aplica `HALF_EVEN` na escala interna 24 e apresenta o resultado na escala 12 definida

#### Scenario: Resultado fora da precisão
- **WHEN** um estado não puder ser representado nos limites aprovados
- **THEN** o sistema responde `422 Unprocessable Entity` com código `CALCULO_POSICAO_FORA_DA_PRECISAO` e não devolve consolidação parcial

### Requirement: Histórico inconsistente falha de forma segura
O sistema SHALL considerar inconsistente qualquer histórico persistido que produza quantidade negativa em algum ponto cronológico ou viole invariantes essenciais de tipo, quantidade, preço, agrupamento ou ordem. A consulta MUST NOT corrigir, reordenar por ID, ignorar Operação ou devolver posição parcial. Ela SHALL responder `409 Conflict` no formato `StandardError` com código `HISTORICO_OPERACOES_INCONSISTENTE` e preservar todos os dados.

#### Scenario: Saldo cronológico negativo legado
- **WHEN** o replay encontra VENDA que torna a quantidade negativa
- **THEN** toda a consulta falha com `409 Conflict`, identifica o grupo e o ponto inconsistente nos detalhes e não altera o histórico

#### Scenario: Uma posição inconsistente entre posições válidas
- **WHEN** uma Carteira possui ao menos um grupo Carteira+Ação inconsistente
- **THEN** o sistema não devolve resposta parcial que possa ser interpretada como consolidação completa da Carteira

### Requirement: Consulta consistente e sem efeitos colaterais
A consolidação SHALL observar um conjunto transacionalmente consistente de Carteira e Operações persistidas e SHALL ser somente leitura. Ela MUST NOT adquirir lock pessimista de escrita, usar `Clock`, salvar posição, alterar Operação, Carteira ou Ação, executar consulta HTTP, atualizar cotação ou criar cache/materialização.

#### Scenario: Operação concorrente
- **WHEN** uma Operação é registrada concorrentemente com a consulta
- **THEN** a resposta reflete integralmente um estado transacional consistente anterior ou posterior à Operação, sem misturar estado parcial

#### Scenario: Consulta repetida sem mutação
- **WHEN** a mesma posição é consultada uma ou mais vezes sem nova Operação
- **THEN** o estado persistido permanece inalterado e o resultado financeiro é determinístico

#### Scenario: Providers indisponíveis
- **WHEN** BRAPI, Alpha Vantage, BrasilAPI ou ViaCEP estão indisponíveis
- **THEN** a consolidação continua determinada exclusivamente pelo banco de dados e não realiza chamada externa

### Requirement: Ausência de indicadores fora da primeira consolidação
Esta capability MUST NOT calcular resultado realizado, resultado não realizado, valor atual de mercado, rentabilidade, patrimônio, câmbio ou evolução patrimonial. Resultado realizado SHALL permanecer para capability própria que possa representar corretamente ciclos encerrados, sem misturá-lo ao preço médio ou ao custo da posição aberta.

#### Scenario: Venda com lucro ou prejuízo
- **WHEN** o histórico contém VENDA acima ou abaixo do preço médio vigente
- **THEN** a consolidação atualiza somente quantidade e custo contábil, sem expor ou persistir resultado realizado nesta resposta

### Requirement: Compatibilidade sem persistência adicional
A capability SHALL operar sobre as entidades e o schema vigentes, MUST NOT criar entidade ou tabela de posição, migration, snapshot ou campo consolidado e SHALL preservar `POST /operacoes`, as consultas de Operação, o replay de validação de VENDA, a proteção de DELETE de Carteira e os demais endpoints promovidos.

#### Scenario: Inicialização com schema vigente
- **WHEN** PostgreSQL ou H2 inicia com os changeSets 001 a 004
- **THEN** Liquibase e Hibernate continuam validando o mesmo schema sem migration de posição

#### Scenario: Regressão das Operações
- **WHEN** a consulta de posição é disponibilizada
- **THEN** registro, consulta, cronologia, concorrência e proteção do histórico de Operações preservam seus contratos atuais
