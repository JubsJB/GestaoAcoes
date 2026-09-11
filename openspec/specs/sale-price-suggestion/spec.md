# sale-price-suggestion Specification

## Purpose
Fornecer uma sugestão inicial e não vinculante para o preço de VENDA a partir da COMPRA historicamente aplicável mais recente da mesma Carteira e Ação.

## Requirements

### Requirement: Consulta REST da sugestão de preço de VENDA
O sistema SHALL expor `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda` com os query parameters obrigatórios `ticker`, `mercado` e `dataOperacao` no formato `YYYY-MM-DD`. Carteira e Ação SHALL existir, ticker SHALL ser normalizado e mercado SHALL aceitar somente `BRASIL` e `EUA`. O sistema SHALL responder `200 OK` com `SugestaoPrecoVendaResponse` contendo exclusivamente `precoUnitarioSugerido`, representado por `BigDecimal` quando presente ou `null` quando ausente.

#### Scenario: Sugestão encontrada
- **WHEN** existe COMPRA cronologicamente aplicável da Ação na Carteira
- **THEN** o sistema responde `200 OK` com o `precoUnitario` persistido dessa COMPRA em `precoUnitarioSugerido`

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica Carteira persistida
- **THEN** o sistema responde `404 Not Found` no formato `StandardError`

#### Scenario: Ação inexistente
- **WHEN** ticker e mercado normalizados não identificam Ação persistida
- **THEN** o sistema responde `404 Not Found` no formato `StandardError`

#### Scenario: Entrada inválida
- **WHEN** ticker, mercado ou data está ausente ou inválido, ou a data é futura segundo a regra vigente do mercado
- **THEN** o sistema responde `400 Bad Request` com `REQUEST_INVALIDO`

### Requirement: COMPRA mais recente limitada pela data da nova VENDA
A sugestão SHALL considerar somente Operações de `tipo=COMPRA` pertencentes à mesma Carteira e à mesma identidade persistida de Ação, identificada por ticker normalizado e mercado, com `dataOperacao` anterior ou igual à `dataOperacao` informada. Entre as elegíveis, SHALL selecionar deterministicamente por `dataOperacao DESC`, `ordemNoDia DESC` e `id DESC`. VENDA, preço médio, cotação atual, valor total, posição e fechamento consultado no momento MUST NOT participar da escolha ou do valor sugerido.

#### Scenario: Múltiplas compras e venda posterior
- **WHEN** há várias compras elegíveis e uma VENDA mais recente que elas
- **THEN** o sistema ignora a VENDA e sugere o preço da COMPRA de maior `dataOperacao`, `ordemNoDia` e `id`, nessa ordem

#### Scenario: Operação retroativa
- **WHEN** a nova VENDA possui data anterior a uma COMPRA já persistida
- **THEN** a COMPRA posterior à data informada não é elegível para a sugestão

#### Scenario: Compra na mesma data
- **WHEN** há uma ou mais compras persistidas na mesma data informada para a nova VENDA
- **THEN** o sistema considera essas compras elegíveis e seleciona a de maior `ordemNoDia`, usando `id` somente como desempate técnico

#### Scenario: Isolamento por Carteira
- **WHEN** outra Carteira possui COMPRA mais recente da mesma Ação
- **THEN** essa COMPRA não participa da sugestão

#### Scenario: Isolamento por Ação e mercado
- **WHEN** existem compras de outro ticker ou mercado
- **THEN** essas compras não participam da sugestão

### Requirement: Ausência normal e sugestão não vinculante
Quando não existir COMPRA elegível, o sistema SHALL responder `200 OK` com `precoUnitarioSugerido=null`; a ausência MUST NOT ser tratada como erro técnico. A sugestão SHALL ser apenas conveniência de preenchimento, MUST NOT modificar estado nem validar ou reservar o preço final. O contrato de VENDA em `POST /operacoes` SHALL continuar exigindo o `precoUnitario` positivo escolhido pelo usuário e proibindo `ordemNoDia`.

#### Scenario: Nenhuma compra anterior
- **WHEN** não existe COMPRA da mesma Carteira e Ação com data anterior ou igual à data consultada
- **THEN** o sistema responde `200 OK` com `precoUnitarioSugerido=null`

#### Scenario: Usuário altera a sugestão
- **WHEN** o usuário escolhe outro preço válido após receber uma sugestão
- **THEN** o POST de VENDA usa o preço enviado, sem vínculo com a sugestão

#### Scenario: Consulta sem efeitos colaterais ou provider
- **WHEN** a sugestão é consultada com resultado presente ou ausente
- **THEN** o sistema não persiste dados, não adquire lock de escrita e não chama provider externo
