## ADDED Requirements

### Requirement: Resultado não realizado da posição aberta
Para cada posição aberta, o sistema SHALL calcular `resultadoNaoRealizado = valorAtualPosicao - custoPosicao`. O indicador SHALL representar somente a diferença potencial da posição atualmente mantida, MUST NOT incluir resultado realizado de vendas nem dividendos, taxas, impostos, corretagem, câmbio ou eventos corporativos e MUST NOT participar do replay que determina quantidade, preço médio ou custo.

#### Scenario: Ganho potencial
- **WHEN** a posição possui `valorAtualPosicao=3550.000000000000` e `custoPosicao=3200.000000000000`
- **THEN** `resultadoNaoRealizado` é `350.000000000000`, indicando ganho potencial

#### Scenario: Perda potencial
- **WHEN** a posição possui `valorAtualPosicao=3000.000000000000` e `custoPosicao=3200.000000000000`
- **THEN** `resultadoNaoRealizado` é `-200.000000000000`, indicando perda potencial

#### Scenario: Resultado potencial nulo
- **WHEN** `valorAtualPosicao` é numericamente igual a `custoPosicao`
- **THEN** `resultadoNaoRealizado` é zero com escala 12 e não é `null`

#### Scenario: Equivalência matemática
- **WHEN** quantidade, preço médio, custo e valor atual representam a mesma posição consolidada válida
- **THEN** o resultado calculado por `valorAtualPosicao - custoPosicao` é matematicamente equivalente a `(cotacaoAtual - precoMedio) × quantidadeAtual`, sem tornar a fórmula equivalente uma segunda fonte de cálculo

### Requirement: Resultado restrito ao ciclo atualmente aberto
O resultado não realizado SHALL considerar exclusivamente quantidade, custo e valor atual do ciclo que permanece aberto ao final do replay. Uma VENDA parcial SHALL reduzir a base ao estado remanescente; uma VENDA total SHALL omitir a posição; uma COMPRA posterior ao zeramento SHALL iniciar resultado independente dos ciclos encerrados.

#### Scenario: Venda parcial
- **WHEN** uma posição compra 100 unidades a 10, vende 40 e permanece com quantidade 60, custo 600 e valor atual 900
- **THEN** `resultadoNaoRealizado` é `300.000000000000` somente sobre as 60 unidades remanescentes, sem incorporar o resultado da VENDA

#### Scenario: Venda total
- **WHEN** uma VENDA encerra totalmente a posição
- **THEN** a posição é omitida de `GET /carteiras/{carteiraId}/posicoes` e nenhum resultado não realizado é devolvido para o ciclo encerrado

#### Scenario: Novo ciclo após zeramento
- **WHEN** uma posição é zerada e uma COMPRA cronologicamente posterior inicia novo ciclo
- **THEN** `resultadoNaoRealizado` usa somente custo, quantidade e valor atual do novo ciclo, sem carregar custo ou resultado potencial anterior

### Requirement: Moeda do resultado não realizado
`resultadoNaoRealizado` SHALL permanecer expresso na moeda da Ação da própria posição. A capability MUST NOT converter moedas nem agregar resultados de posições em moedas diferentes.

#### Scenario: Posição brasileira
- **WHEN** a posição pertence a Ação em `BRL`
- **THEN** seu resultado não realizado permanece em BRL

#### Scenario: Posição americana fracionária
- **WHEN** a posição pertence a Ação em `USD` e possui quantidade fracionária
- **THEN** seu resultado não realizado permanece em USD e preserva o cálculo decimal aprovado, sem conversão

## MODIFIED Requirements

### Requirement: Representação contábil e de mercado da posição
Cada posição SHALL conter `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `quantidadeAtual`, `precoMedio`, `custoPosicao`, `cotacaoAtual`, `dataHoraCotacao`, `valorAtualPosicao` e `resultadoNaoRealizado`. Os identificadores, dados descritivos, moeda, `cotacaoAtual` e `dataHoraCotacao` da Ação SHALL refletir exatamente o relacionamento e o estado persistidos. `valorAtualPosicao` e `resultadoNaoRealizado` SHALL ser derivados conforme as regras desta capability. A resposta MUST NOT incluir resultado realizado, rentabilidade, patrimônio ou snapshot.

#### Scenario: Posição brasileira com cotação
- **WHEN** uma posição aberta referencia Ação de `mercado=BRASIL`, `moeda=BRL` e cotação persistida válida
- **THEN** a resposta identifica a Ação, preserva os valores contábeis em BRL e inclui a cotação persistida, sua data/hora, o valor atual e o resultado não realizado em BRL

#### Scenario: Posição americana com quantidade fracionária
- **WHEN** uma posição aberta referencia Ação de `mercado=EUA`, `moeda=USD`, possui quantidade fracionária e cotação persistida válida
- **THEN** a resposta preserva mercado, moeda, quantidade fracionária, cotação, valor atual e resultado não realizado em USD, sem conversão cambial

#### Scenario: Indicadores fora do escopo ausentes
- **WHEN** qualquer posição é retornada
- **THEN** a resposta não contém resultado realizado, rentabilidade, patrimônio ou snapshot

### Requirement: Precisão e arredondamento explícitos
O replay SHALL continuar usando aritmética `BigDecimal`, escala interna de cálculo 24 nas divisões proporcionais e `RoundingMode.HALF_EVEN` explicitamente. `precoMedio` SHALL ser apresentado com escala 12 e precisão máxima 25; `custoPosicao`, `valorAtualPosicao` e `resultadoNaoRealizado` SHALL ser apresentados com escala 12 e precisão total máxima 38. A quantidade SHALL permanecer exata e `cotacaoAtual` SHALL preservar escala 6 e precisão máxima 19 conforme persistida. Multiplicações, somas e a subtração do resultado não realizado MUST NOT ser arredondadas; normalizar `valorAtualPosicao` e `resultadoNaoRealizado` para escala 12 SHALL usar `RoundingMode.UNNECESSARY` e rejeitar qualquer perda de informação. Resultado que não puder ser representado nesses limites SHALL falhar integralmente com erro padronizado, sem truncamento nem resposta parcial.

#### Scenario: Divisão exata
- **WHEN** custo dividido por quantidade possui representação decimal finita dentro dos limites
- **THEN** o valor é preservado e apresentado na escala aprovada sem perda numérica

#### Scenario: Divisão periódica
- **WHEN** o preço médio matemático é periódico, como `1550 / 150`
- **THEN** o sistema aplica `HALF_EVEN` na escala interna 24 e apresenta o resultado na escala 12 definida

#### Scenario: Produto dentro dos limites aprovados
- **WHEN** quantidade e cotação respeitam `NUMERIC(19,6)`
- **THEN** o produto cabe em precisão 38 e escala 12 e é devolvido exatamente, sem arredondamento

#### Scenario: Subtração exata positiva, negativa ou nula
- **WHEN** `valorAtualPosicao` e `custoPosicao` estão normalizados em escala 12 e dentro da precisão aprovada
- **THEN** sua diferença é calculada exatamente, aceita sinal positivo, negativo ou zero e é apresentada em escala 12 sem arredondamento

#### Scenario: Preservação dos cálculos contábeis
- **WHEN** o resultado não realizado é incluído na posição
- **THEN** as escalas, o `HALF_EVEN` das divisões inevitáveis, `precoMedio` e `custoPosicao` permanecem conforme a política já aprovada

#### Scenario: Resultado fora da precisão
- **WHEN** algum estado calculado, inclusive `resultadoNaoRealizado`, não puder ser representado nos limites aprovados
- **THEN** o sistema responde `422 Unprocessable Entity` com código `CALCULO_POSICAO_FORA_DA_PRECISAO` e não devolve consolidação parcial

### Requirement: Ausência de indicadores fora desta consolidação
Esta capability MUST NOT calcular resultado realizado, rentabilidade, patrimônio, câmbio ou evolução patrimonial. Resultado realizado SHALL permanecer para capability própria que possa representar corretamente vendas e ciclos encerrados, sem misturá-lo ao resultado não realizado, ao preço médio ou ao custo da posição aberta.

#### Scenario: Venda com lucro ou prejuízo
- **WHEN** o histórico contém VENDA acima ou abaixo do preço médio vigente e ainda resta posição aberta
- **THEN** a consolidação atualiza quantidade e custo contábil, calcula valor atual e resultado não realizado somente da posição remanescente e não expõe nem persiste resultado realizado

