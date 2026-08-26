## ADDED Requirements

### Requirement: Rentabilidade percentual da posição aberta
Para cada posição aberta, o sistema SHALL calcular `rentabilidadePercentual = (resultadoNaoRealizado / custoPosicao) × 100` usando exclusivamente `resultadoNaoRealizado` e `custoPosicao` já consolidados. O valor SHALL representar ganho percentual potencial quando positivo, perda percentual potencial quando negativo e neutralidade quando zero. A expressão `(cotacaoAtual / precoMedio - 1) × 100` SHALL ser apenas uma equivalência matemática quando aplicável e MUST NOT constituir segunda fonte de cálculo.

#### Scenario: Ganho percentual potencial
- **WHEN** a posição possui `resultadoNaoRealizado=350.000000000000` e `custoPosicao=3200.000000000000`
- **THEN** `rentabilidadePercentual` é `10.937500`

#### Scenario: Perda percentual potencial
- **WHEN** a posição possui `resultadoNaoRealizado=-200.000000000000` e `custoPosicao=3200.000000000000`
- **THEN** `rentabilidadePercentual` é `-6.250000`

#### Scenario: Rentabilidade percentual nula
- **WHEN** `resultadoNaoRealizado` é zero e a posição possui custo positivo
- **THEN** `rentabilidadePercentual` é zero com escala 6 e não é `null`

### Requirement: Custo positivo da posição aberta
Toda posição devolvida pelo endpoint SHALL possuir `quantidadeAtual > 0` e `custoPosicao > 0`, pois cada COMPRA válida usa quantidade e preço positivos, a VENDA não pode exceder a quantidade disponível e o zeramento omite a posição. A rentabilidade MUST NOT criar fallback, valor sintético ou contrato anulável para divisão por zero em estados proibidos pelas invariantes do replay.

#### Scenario: Posição aberta válida
- **WHEN** o replay termina com quantidade positiva
- **THEN** o custo consolidado da posição também é positivo e pode ser usado como denominador da rentabilidade

#### Scenario: Posição totalmente encerrada
- **WHEN** o replay termina com quantidade igual a zero e custo igual a zero
- **THEN** a posição é omitida antes do cálculo da rentabilidade

#### Scenario: Custo não positivo em posição aberta inconsistente
- **WHEN** o replay ou a consolidação produzir quantidade positiva com `custoPosicao` menor ou igual a zero
- **THEN** toda a consulta falha com `409 Conflict` e código `HISTORICO_OPERACOES_INCONSISTENTE`, sem calcular percentual, fabricar denominador, mascarar o estado ou devolver resposta parcial

### Requirement: Rentabilidade restrita ao ciclo aberto
A rentabilidade percentual SHALL usar somente resultado não realizado e custo do ciclo que permanece aberto. Resultado realizado de VENDA, ciclos encerrados, dividendos, taxas, impostos, corretagem, câmbio e eventos corporativos MUST NOT participar do indicador.

#### Scenario: Venda parcial
- **WHEN** a posição remanescente possui `custoPosicao=600.000000000000` e `resultadoNaoRealizado=300.000000000000`
- **THEN** `rentabilidadePercentual` é `50.000000`, sem incorporar resultado realizado das unidades vendidas

#### Scenario: Venda total
- **WHEN** uma VENDA encerra totalmente a posição
- **THEN** nenhum `PosicaoResponse` nem rentabilidade é devolvido para o ciclo encerrado

#### Scenario: Novo ciclo após zeramento
- **WHEN** uma COMPRA posterior ao zeramento inicia novo ciclo
- **THEN** a rentabilidade usa somente custo e resultado não realizado do novo ciclo

### Requirement: Representação numérica da rentabilidade
`rentabilidadePercentual` SHALL ser um número percentual já multiplicado por 100, de modo que `10.937500` represente `10,9375%`. O cálculo SHALL usar somente `BigDecimal`, dividir em escala intermediária 24 com `RoundingMode.HALF_EVEN`, multiplicar por 100 e apresentar escala final 6 com `RoundingMode.HALF_EVEN` e precisão total máxima 38. O sistema MUST NOT usar `float`, `double`, truncar ou arredondar implicitamente. Valor que não puder ser representado nesses limites SHALL falhar integralmente com `422 Unprocessable Entity` e código `CALCULO_POSICAO_FORA_DA_PRECISAO`.

#### Scenario: Divisão exata
- **WHEN** resultado não realizado dividido pelo custo possui representação decimal finita
- **THEN** o percentual é preservado e apresentado com escala 6

#### Scenario: Divisão periódica
- **WHEN** a divisão produz dízima periódica
- **THEN** o sistema aplica `HALF_EVEN` na escala intermediária 24 e na normalização final para escala 6

#### Scenario: Percentual fora da precisão
- **WHEN** a rentabilidade calculada não pode ser representada com precisão máxima 38 e escala 6
- **THEN** o sistema responde `422 Unprocessable Entity` com código `CALCULO_POSICAO_FORA_DA_PRECISAO` e não devolve consolidação parcial

#### Scenario: Ausência de limites percentuais artificiais
- **WHEN** os operandos válidos produzem percentual superior a 100 ou resultado negativo admitido pela fórmula
- **THEN** o sistema devolve o valor calculado sem impor teto de 100% ou validação artificial de faixa

## MODIFIED Requirements

### Requirement: Representação contábil e de mercado da posição
Cada posição SHALL conter `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `quantidadeAtual`, `precoMedio`, `custoPosicao`, `cotacaoAtual`, `dataHoraCotacao`, `valorAtualPosicao`, `resultadoNaoRealizado` e `rentabilidadePercentual`. Os identificadores, dados descritivos, moeda, `cotacaoAtual` e `dataHoraCotacao` da Ação SHALL refletir exatamente o relacionamento e o estado persistidos. `valorAtualPosicao`, `resultadoNaoRealizado` e `rentabilidadePercentual` SHALL ser derivados conforme as regras desta capability. A resposta MUST NOT incluir resultado realizado, patrimônio ou snapshot.

#### Scenario: Posição brasileira com cotação
- **WHEN** uma posição aberta referencia Ação de `mercado=BRASIL`, `moeda=BRL` e cotação persistida válida
- **THEN** a resposta identifica a Ação, preserva os valores contábeis em BRL e inclui cotação, data/hora, valor atual, resultado não realizado e rentabilidade percentual

#### Scenario: Posição americana com quantidade fracionária
- **WHEN** uma posição aberta referencia Ação de `mercado=EUA`, `moeda=USD`, possui quantidade fracionária e cotação persistida válida
- **THEN** a resposta preserva mercado, moeda, quantidade fracionária, cotação, valor atual, resultado não realizado e rentabilidade percentual, sem conversão cambial

#### Scenario: Indicadores fora do escopo ausentes
- **WHEN** qualquer posição é retornada
- **THEN** a resposta não contém resultado realizado, patrimônio ou snapshot

### Requirement: Precisão e arredondamento explícitos
O replay SHALL continuar usando aritmética `BigDecimal`, escala interna de cálculo 24 nas divisões proporcionais e `RoundingMode.HALF_EVEN` explicitamente. `precoMedio` SHALL ser apresentado com escala 12 e precisão máxima 25; `custoPosicao`, `valorAtualPosicao` e `resultadoNaoRealizado` SHALL ser apresentados com escala 12 e precisão total máxima 38; `rentabilidadePercentual` SHALL ser apresentada com escala 6 e precisão total máxima 38. A quantidade SHALL permanecer exata e `cotacaoAtual` SHALL preservar escala 6 e precisão máxima 19 conforme persistida. Multiplicações, somas e a subtração do resultado não realizado MUST NOT ser arredondadas; normalizar `valorAtualPosicao` e `resultadoNaoRealizado` para escala 12 SHALL usar `RoundingMode.UNNECESSARY`. Somente as divisões inevitáveis, inclusive a rentabilidade, SHALL usar escala intermediária 24 e `HALF_EVEN`; a rentabilidade SHALL ser normalizada para escala 6 também com `HALF_EVEN`. Resultado que não puder ser representado nesses limites SHALL falhar integralmente com erro padronizado, sem truncamento nem resposta parcial.

#### Scenario: Divisão exata
- **WHEN** custo dividido por quantidade possui representação decimal finita dentro dos limites
- **THEN** o valor é preservado e apresentado na escala aprovada sem perda numérica

#### Scenario: Divisão periódica
- **WHEN** o preço médio matemático ou a rentabilidade são periódicos
- **THEN** o sistema aplica `HALF_EVEN` na escala intermediária 24 e apresenta cada valor em sua escala final definida

#### Scenario: Produto dentro dos limites aprovados
- **WHEN** quantidade e cotação respeitam `NUMERIC(19,6)`
- **THEN** o produto cabe em precisão 38 e escala 12 e é devolvido exatamente, sem arredondamento

#### Scenario: Subtração exata positiva, negativa ou nula
- **WHEN** `valorAtualPosicao` e `custoPosicao` estão normalizados em escala 12 e dentro da precisão aprovada
- **THEN** sua diferença é calculada exatamente, aceita sinal positivo, negativo ou zero e é apresentada em escala 12 sem arredondamento

#### Scenario: Preservação dos cálculos contábeis
- **WHEN** a rentabilidade é incluída na posição
- **THEN** quantidade, `precoMedio`, `custoPosicao`, valor atual, resultado não realizado e respectivas políticas numéricas anteriores permanecem inalterados

#### Scenario: Resultado fora da precisão
- **WHEN** algum estado calculado, inclusive `rentabilidadePercentual`, não puder ser representado nos limites aprovados
- **THEN** o sistema responde `422 Unprocessable Entity` com código `CALCULO_POSICAO_FORA_DA_PRECISAO` e não devolve consolidação parcial

### Requirement: Ausência de indicadores fora desta consolidação
Esta capability MUST NOT calcular resultado realizado, patrimônio, rentabilidade consolidada da Carteira, rentabilidade histórica, TWR, XIRR, câmbio ou evolução patrimonial. Resultado realizado SHALL permanecer para capability própria que possa representar corretamente vendas e ciclos encerrados, sem misturá-lo ao resultado não realizado, à rentabilidade da posição aberta, ao preço médio ou ao custo.

#### Scenario: Venda com lucro ou prejuízo
- **WHEN** o histórico contém VENDA acima ou abaixo do preço médio vigente e ainda resta posição aberta
- **THEN** a consolidação calcula rentabilidade somente sobre resultado não realizado e custo remanescentes, sem expor ou persistir resultado realizado
