## RENAMED Requirements

- FROM: `### Requirement: Mesma fonte histórica da criação de COMPRA`
- TO: `### Requirement: Fonte histórica da prévia de COMPRA`

## MODIFIED Requirements

### Requirement: Fonte histórica da prévia de COMPRA
A prévia SHALL reutilizar a capacidade vigente de fechamento histórico: BRAPI para `BRASIL`, Alpha Vantage para `EUA`, fechamento bruto não ajustado e correspondência exata com `dataOperacao`. A consulta MUST NOT usar cotação atual, `adjustedClose`, preço manual ou candle de pregão anterior ou posterior. A prévia SHALL reutilizar a validação centralizada da capability histórica, sem duplicar parser, classificação de erros ou limite de candles. Essa validação MUST NOT ser exigida pelo POST de Operação, que utiliza o preço final informado.

#### Scenario: Close bruto prevalece
- **WHEN** o provider apresenta fechamento bruto e fechamento ajustado diferentes
- **THEN** a prévia devolve exclusivamente o fechamento bruto como referência histórica e sugestão não vinculante

#### Scenario: Data sem pregão
- **WHEN** não existe candle exato em data classificável dentro da janela disponível
- **THEN** o sistema responde `422 Unprocessable Content` com `COTACAO_HISTORICA_INDISPONIVEL` e não substitui a data

#### Scenario: Data fora do alcance
- **WHEN** a resposta permite determinar que a data antecede o histórico disponível
- **THEN** o sistema responde `422 Unprocessable Content` com `HISTORICO_COTACAO_FORA_DO_ALCANCE`

#### Scenario: Erros do provider
- **WHEN** o provider sinaliza ticker inexistente, limite excedido, resposta inválida, indisponibilidade ou timeout
- **THEN** o sistema preserva respectivamente `404 TICKER_INEXISTENTE`, `429 LIMITE_REQUISICOES_EXCEDIDO`, `502 RESPOSTA_EXTERNA_INVALIDA`, `503 SERVICO_EXTERNO_INDISPONIVEL` ou `504 SERVICO_EXTERNO_TIMEOUT`

### Requirement: Prévia informativa e sem efeitos colaterais
A previa SHALL permanecer consulta historica independente, informativa e sem efeitos colaterais. POST /operacoes SHALL exigir precoUnitario manual em COMPRA e VENDA, sem chamar a previa nem substituir o preco informado. O formulario COMPRA SHALL consumir esta previa somente como sugestao inicial editavel, preservando o preco final do campo no POST. O usuário SHALL poder manter ou substituir a sugestão. Sucesso da prévia MUST NOT ser pré-condição do POST; falha, ausência ou indisponibilidade SHALL permitir preço manual válido, sem dispensar as demais validações.

#### Scenario: Consulta independente
- **WHEN** uma previa retorna um fechamento e depois o cliente envia COMPRA
- **THEN** POST usa exclusivamente o preco manual validado sem nova consulta historica

#### Scenario: Preco obrigatorio
- **WHEN** cliente omite precoUnitario ou envia nulo apos consultar previa
- **THEN** POST rejeita com 400 REQUEST_INVALIDO sem persistir

#### Scenario: Previa sem mutacao
- **WHEN** consulta termina com sucesso ou erro
- **THEN** nenhuma Acao, cotacao, Operacao, posicao ou snapshot e criado ou modificado

#### Scenario: Consulta não reserva preço
- **WHEN** o cliente consulta uma previa historica
- **THEN** a consulta nao reserva preco e o POST exige preco manual sem reconsulta

#### Scenario: Cliente tenta impor preço da COMPRA
- **WHEN** o cliente informa preco positivo no POST de COMPRA
- **THEN** o preco e obrigatorio e aceito pela nova regra, independentemente da previa

#### Scenario: Prévia sem mutação
- **WHEN** a consulta termina com sucesso ou erro
- **THEN** nenhuma Ação, cotação, Operação, posição ou snapshot é criado ou modificado
