## MODIFIED Requirements

### Requirement: Representação contábil e de mercado da posição
Cada posição SHALL conter `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `quantidadeAtual`, `precoMedio`, `custoPosicao`, `cotacaoAtual`, `dataHoraCotacao` e `valorAtualPosicao`. Os identificadores, dados descritivos, moeda, `cotacaoAtual` e `dataHoraCotacao` da Ação SHALL refletir exatamente o relacionamento e o estado persistidos. `valorAtualPosicao` SHALL ser derivado conforme a regra desta capability. A resposta MUST NOT incluir resultado realizado, resultado não realizado, rentabilidade, patrimônio ou snapshot.

#### Scenario: Posição brasileira com cotação
- **WHEN** uma posição aberta referencia Ação de `mercado=BRASIL`, `moeda=BRL` e cotação persistida válida
- **THEN** a resposta identifica a Ação, preserva os valores contábeis em BRL e inclui a cotação persistida, sua data/hora e o valor atual em BRL

#### Scenario: Posição americana com quantidade fracionária
- **WHEN** uma posição aberta referencia Ação de `mercado=EUA`, `moeda=USD`, possui quantidade fracionária e cotação persistida válida
- **THEN** a resposta preserva mercado, moeda, quantidade fracionária, cotação e valor atual em USD, sem conversão cambial

#### Scenario: Indicadores fora do escopo ausentes
- **WHEN** qualquer posição é retornada
- **THEN** a resposta não contém resultado realizado, resultado não realizado, rentabilidade, patrimônio ou snapshot

### Requirement: Fonte única do estado contábil e separação da cotação
O sistema SHALL calcular quantidade, preço médio e custo de cada posição usando exclusivamente as Operações persistidas da mesma combinação de Carteira e Ação. Operações de outra Carteira ou de outra Ação MUST NOT afetar esses valores. `Operacao.precoUnitario` SHALL ser o único preço usado no replay contábil. `Acao.cotacaoAtual` SHALL ser usada exclusivamente como a última cotação de mercado conhecida para calcular `valorAtualPosicao` e MUST NOT participar de `precoMedio` ou `custoPosicao`.

#### Scenario: Mesma Ação em Carteiras diferentes
- **WHEN** duas Carteiras possuem Operações da mesma Ação
- **THEN** cada Carteira apresenta estado contábil calculado somente com o próprio histórico e usa a cotação persistida da Ação apenas no respectivo valor atual

#### Scenario: Ações diferentes na mesma Carteira
- **WHEN** uma Carteira possui Operações de Ações distintas
- **THEN** o sistema consolida cada Ação em estado independente e aplica a cotação persistida correspondente a cada posição

#### Scenario: Cotação diferente do preço negociado
- **WHEN** `Acao.cotacaoAtual` difere dos preços registrados nas Operações
- **THEN** quantidade, preço médio e custo permanecem determinados somente por `Operacao.precoUnitario`, enquanto apenas `valorAtualPosicao` reflete a cotação atual

#### Scenario: Atualização de cotação não reescreve histórico
- **WHEN** `Acao.cotacaoAtual` e `dataHoraCotacao` são atualizadas pelo fluxo dedicado de cotação
- **THEN** uma consulta posterior reflete os novos dados de mercado sem alterar Operações, preço médio ou custo da posição

### Requirement: Valor atual da posição
Para cada posição aberta, o sistema SHALL calcular `valorAtualPosicao = quantidadeAtual × cotacaoAtual` com `BigDecimal`. A multiplicação SHALL usar os valores exatos da quantidade consolidada e da cotação persistida, MUST NOT usar `float` ou `double` e MUST NOT arredondar ou truncar silenciosamente.

#### Scenario: Cálculo de valor atual
- **WHEN** uma posição possui `quantidadeAtual=100.000000` e `cotacaoAtual=35.500000`
- **THEN** `valorAtualPosicao` é `3550.000000000000`

#### Scenario: Cálculo fracionário exato
- **WHEN** uma posição americana possui quantidade fracionária e cotação com até seis casas decimais
- **THEN** o produto preserva exatamente as casas resultantes dos dois operandos e é apresentado conforme a política numérica desta capability

#### Scenario: Múltiplas moedas sem agregação
- **WHEN** a resposta contém posições em BRL e USD
- **THEN** cada `valorAtualPosicao` permanece expresso na `moeda` da própria Ação, sem soma ou conversão cambial

### Requirement: Cotação persistida e referência temporal preservada
`cotacaoAtual` e `dataHoraCotacao` SHALL ser obtidas exatamente da Ação persistida associada à posição. A consulta MUST NOT gerar nova data/hora, usar `Clock` para substituir `dataHoraCotacao`, obter nova cotação, atualizar a Ação ou persistir qualquer alteração. No schema suportado, ambos os campos SHALL ser obrigatórios e `cotacaoAtual` SHALL ser estritamente positiva; portanto, esta capability MUST NOT criar fallback, valor sintético ou contrato anulável para estados proibidos pelo modelo.

#### Scenario: Data e hora persistidas
- **WHEN** a Ação associada possui `dataHoraCotacao` persistida
- **THEN** a posição devolve exatamente essa referência temporal, sem substituição pelo instante da consulta

#### Scenario: Consulta repetida
- **WHEN** a mesma posição é consultada repetidamente sem atualização da Ação
- **THEN** `cotacaoAtual` e `dataHoraCotacao` permanecem iguais aos valores persistidos em todas as respostas

#### Scenario: Estado suportado da Ação
- **WHEN** uma posição aberta é consultada sobre o schema Liquibase vigente
- **THEN** sua Ação possui cotação não nula e positiva e data/hora não nula, sem necessidade de tratamento artificial para ausência, zero ou valor negativo

### Requirement: Precisão e arredondamento explícitos
O replay SHALL continuar usando aritmética `BigDecimal`, escala interna de cálculo 24 nas divisões proporcionais e `RoundingMode.HALF_EVEN` explicitamente. `precoMedio` SHALL ser apresentado com escala 12 e precisão máxima 25; `custoPosicao` e `valorAtualPosicao` SHALL ser apresentados com escala 12 e precisão total máxima 38. A quantidade SHALL permanecer exata e `cotacaoAtual` SHALL preservar escala 6 e precisão máxima 19 conforme persistida. Multiplicações e somas exatas MUST NOT ser arredondadas; normalizar `valorAtualPosicao` para escala 12 SHALL usar política que rejeite perda de informação. Resultado que não puder ser representado nesses limites SHALL falhar integralmente com erro padronizado, sem truncamento nem resposta parcial.

#### Scenario: Divisão exata
- **WHEN** custo dividido por quantidade possui representação decimal finita dentro dos limites
- **THEN** o valor é preservado e apresentado na escala aprovada sem perda numérica

#### Scenario: Divisão periódica
- **WHEN** o preço médio matemático é periódico, como `1550 / 150`
- **THEN** o sistema aplica `HALF_EVEN` na escala interna 24 e apresenta o resultado na escala 12 definida

#### Scenario: Produto dentro dos limites aprovados
- **WHEN** quantidade e cotação respeitam `NUMERIC(19,6)`
- **THEN** o produto cabe em precisão 38 e escala 12 e é devolvido exatamente, sem arredondamento

#### Scenario: Preservação dos cálculos contábeis
- **WHEN** a cotação é incluída na posição
- **THEN** as escalas, o `HALF_EVEN` das divisões inevitáveis, `precoMedio` e `custoPosicao` permanecem conforme a política já aprovada

#### Scenario: Resultado fora da precisão
- **WHEN** algum estado calculado não puder ser representado nos limites aprovados
- **THEN** o sistema responde `422 Unprocessable Entity` com código `CALCULO_POSICAO_FORA_DA_PRECISAO` e não devolve consolidação parcial

### Requirement: Consulta consistente, eficiente e sem efeitos colaterais
A consolidação SHALL observar um conjunto transacionalmente consistente de Carteira, Operações e Ações persistidas e SHALL ser somente leitura. Ela MUST obter as Ações associadas sem executar uma consulta adicional por Ação, MUST NOT adquirir lock pessimista de escrita, usar `Clock`, salvar posição, alterar Operação, Carteira ou Ação, executar consulta HTTP, atualizar cotação ou criar cache/materialização.

#### Scenario: Operação concorrente
- **WHEN** uma Operação é registrada concorrentemente com a consulta
- **THEN** a resposta reflete integralmente um estado transacional consistente anterior ou posterior à Operação, sem misturar estado parcial

#### Scenario: Consulta repetida sem mutação
- **WHEN** a mesma posição é consultada uma ou mais vezes sem nova Operação nem atualização dedicada de cotação
- **THEN** o estado persistido permanece inalterado e os resultados contábil e de mercado são determinísticos

#### Scenario: Providers indisponíveis
- **WHEN** BRAPI, Alpha Vantage, BrasilAPI ou ViaCEP estão indisponíveis
- **THEN** a consolidação continua determinada exclusivamente pelo banco de dados e não realiza chamada externa

#### Scenario: Múltiplas posições sem N+1 de Ação
- **WHEN** uma Carteira possui múltiplas posições abertas de Ações distintas
- **THEN** a consolidação obtém as Ações associadas junto ao histórico necessário, sem uma consulta adicional para cada Ação

### Requirement: Ausência de indicadores fora desta consolidação
Esta capability MUST NOT calcular resultado realizado, resultado não realizado, rentabilidade, patrimônio, câmbio ou evolução patrimonial. Resultado realizado SHALL permanecer para capability própria que possa representar corretamente ciclos encerrados, sem misturá-lo ao preço médio ou ao custo da posição aberta.

#### Scenario: Venda com lucro ou prejuízo
- **WHEN** o histórico contém VENDA acima ou abaixo do preço médio vigente
- **THEN** a consolidação atualiza somente quantidade e custo contábil e calcula o valor atual da posição remanescente, sem expor ou persistir resultados realizado ou não realizado

### Requirement: Compatibilidade sem persistência adicional
A capability SHALL operar sobre as entidades e o schema vigentes, MUST NOT criar entidade ou tabela de posição, migration, snapshot ou campo consolidado e SHALL preservar `POST /operacoes`, as consultas de Operação, o replay de validação de VENDA, a atualização dedicada de cotação da Ação, a proteção de DELETE de Carteira e os demais endpoints promovidos.

#### Scenario: Inicialização com schema vigente
- **WHEN** PostgreSQL ou H2 inicia com os changeSets 001 a 004
- **THEN** Liquibase e Hibernate continuam validando o mesmo schema sem migration de posição ou cotação

#### Scenario: Regressão das Operações
- **WHEN** os dados de mercado são acrescentados à consulta de posição
- **THEN** registro, consulta, cronologia, concorrência e proteção do histórico de Operações preservam seus contratos atuais

#### Scenario: Atualização dedicada preservada
- **WHEN** a atualização externa de cotação está disponível conforme a capability promovida de Ação
- **THEN** ela permanece responsável por consultar o provider e persistir `cotacaoAtual` e `dataHoraCotacao`, sem transferir essa responsabilidade para `GET /carteiras/{carteiraId}/posicoes`
