## Purpose

Definir a consulta sob demanda do patrimônio atual de uma Carteira, consolidado separadamente por moeda a partir do valor atual das posições abertas.

## ADDED Requirements

### Requirement: Consultar patrimônio atual da Carteira
O sistema SHALL disponibilizar `GET /carteiras/{carteiraId}/patrimonio` para consultar o patrimônio atual de uma Carteira, sem body, filtros ou paginação e sem retornar `Location`.

#### Scenario: Carteira existente com posições abertas
- **WHEN** o cliente consulta o patrimônio de uma Carteira existente que possui posições abertas
- **THEN** o sistema responde `200 OK` com um `PatrimonioResponse`

#### Scenario: Carteira existente sem posições abertas
- **WHEN** o cliente consulta o patrimônio de uma Carteira existente sem posições abertas
- **THEN** o sistema responde `200 OK` com `carteiraId` e `patrimonios` igual a `[]`

#### Scenario: Carteira inexistente
- **WHEN** o cliente consulta o patrimônio de uma Carteira inexistente
- **THEN** o sistema responde `404 Not Found` pelo tratamento centralizado vigente

### Requirement: Representar patrimônio consolidado por moeda
O `PatrimonioResponse` SHALL conter exclusivamente `carteiraId` e `patrimonios`, e cada `PatrimonioMoedaResponse` SHALL conter exclusivamente `moeda` e `patrimonioAtual`.

#### Scenario: Carteira com posições BRL e USD
- **WHEN** uma Carteira possui posições abertas em BRL e USD
- **THEN** a resposta contém um item independente para BRL e outro para USD, ordenados por `moeda ASC`

#### Scenario: Carteira com apenas uma moeda
- **WHEN** uma Carteira possui posições abertas somente em uma moeda
- **THEN** a resposta contém somente o item dessa moeda e não cria itens de valor zero para moedas ausentes

### Requirement: Calcular patrimônio a partir do valor atual consolidado
Para cada moeda, o sistema SHALL calcular `patrimonioAtual = soma(valorAtualPosicao)` de todas as posições abertas dessa moeda, utilizando exclusivamente o `valorAtualPosicao` já produzido pela consolidação oficial de posições.

#### Scenario: Uma posição aberta
- **WHEN** a Carteira possui uma única posição aberta em uma moeda
- **THEN** `patrimonioAtual` dessa moeda é igual ao `valorAtualPosicao` da posição

#### Scenario: Múltiplas posições da mesma moeda
- **WHEN** a Carteira possui múltiplas posições abertas na mesma moeda
- **THEN** `patrimonioAtual` é a soma exata dos respectivos `valorAtualPosicao`

#### Scenario: Posição zerada
- **WHEN** o histórico contém uma posição cuja `quantidadeAtual` foi zerada
- **THEN** essa posição não participa do patrimônio e não é recriada artificialmente

#### Scenario: Venda parcial e novo ciclo
- **WHEN** uma venda parcial deixa posição aberta ou uma nova compra inicia ciclo após zeramento
- **THEN** o patrimônio considera somente o `valorAtualPosicao` da posição aberta resultante do replay oficial

### Requirement: Manter moedas financeiramente separadas
O sistema MUST acumular cada moeda independentemente e MUST NOT converter ou somar valores monetários de moedas diferentes.

#### Scenario: Patrimônio multimoeda
- **WHEN** a Carteira contém posições BRL e USD
- **THEN** o sistema não produz um total único resultante de BRL mais USD

### Requirement: Aplicar política numérica do patrimônio
O sistema SHALL utilizar exclusivamente `BigDecimal`, somar exatamente os valores sem normalização individual adicional e, somente ao final de cada acumulado por moeda, normalizar para escala 12 com `RoundingMode.UNNECESSARY` e validar precisão máxima 38.

#### Scenario: Soma representável
- **WHEN** o acumulado exato por moeda pode ser representado com escala 12 e precisão máxima 38
- **THEN** o sistema retorna `patrimonioAtual` em escala 12 sem truncamento ou arredondamento silencioso

#### Scenario: Acumulado fora da precisão
- **WHEN** o acumulado final de uma moeda excede a precisão máxima 38 ou não pode ser normalizado sem perda
- **THEN** a consulta inteira falha com `422 Unprocessable Entity` e código `CALCULO_POSICAO_FORA_DA_PRECISAO`, sem resposta parcial

### Requirement: Não duplicar resultados financeiros no patrimônio
O patrimônio atual SHALL representar somente o valor atual das posições abertas e MUST NOT somar resultado realizado, resultado não realizado ou rentabilidade como parcelas adicionais.

#### Scenario: Resultado realizado de posição encerrada
- **WHEN** uma Ação possui resultado realizado histórico, mas nenhuma posição aberta
- **THEN** esse resultado não integra o patrimônio atual porque o modelo não controla caixa

#### Scenario: Resultado não realizado de posição aberta
- **WHEN** uma posição possui `resultadoNaoRealizado`
- **THEN** o sistema não o soma separadamente porque ele já está refletido em `valorAtualPosicao`

#### Scenario: Rentabilidade da posição
- **WHEN** uma posição possui `rentabilidadePercentual`
- **THEN** o sistema não soma nem calcula média desse percentual para formar o patrimônio

### Requirement: Preservar separação da atualização de cotação
O patrimônio SHALL utilizar apenas as cotações já persistidas que compõem `valorAtualPosicao` e MUST NOT chamar BRAPI, Alpha Vantage, `CotacaoProvider` ou `PATCH /acoes/{id}/cotacao`, nem atualizar Ação durante a consulta.

#### Scenario: Provider indisponível
- **WHEN** um provider externo está indisponível e a Carteira possui posições consolidadas com cotação persistida
- **THEN** a consulta de patrimônio permanece independente do provider

### Requirement: Consultar patrimônio com visão consistente e sem persistência
A consulta SHALL ser read-only sob `Isolation.REPEATABLE_READ`, sem lock pessimista, escrita, `Clock`, persistência do patrimônio, nova query por posição ou novo replay financeiro.

#### Scenario: Consulta do patrimônio
- **WHEN** o patrimônio é consultado
- **THEN** o sistema reutiliza a consolidação de posições e seu fetch plan sem N+1, agregando os resultados em memória dentro de uma visão transacional consistente

### Requirement: Não inventar referência temporal agregada
O contrato de patrimônio MUST NOT retornar uma única `dataHoraCotacao`, pois as posições agregadas podem possuir referências temporais distintas.

#### Scenario: Cotações com instantes distintos
- **WHEN** posições da mesma moeda possuem `dataHoraCotacao` diferentes
- **THEN** o patrimônio soma os valores atuais sem fabricar ou selecionar um timestamp agregado
