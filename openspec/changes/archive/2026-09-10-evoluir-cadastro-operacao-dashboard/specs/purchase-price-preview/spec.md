## MODIFIED Requirements

### Requirement: Prévia informativa e sem efeitos colaterais
A previa SHALL permanecer consulta historica independente, informativa e sem efeitos colaterais. POST /operacoes SHALL exigir precoUnitario manual em COMPRA e VENDA, sem chamar a previa nem substituir o preco informado. O formulario COMPRA SHALL consumir esta previa somente como sugestao inicial editavel, preservando o preco final do campo no POST.

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
