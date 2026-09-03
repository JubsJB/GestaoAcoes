## Why

O frontend precisa mostrar, antes do registro, o fechamento histórico que será usado em uma COMPRA e oferecer um valor inicial coerente para o preço editável de uma VENDA. Essas informações dependem de regras e dados que pertencem ao backend: providers históricos já integrados e cronologia persistida das Operações.

## What Changes

- Adicionar uma consulta REST informativa do fechamento histórico exato para a prévia de COMPRA, reutilizando a mesma abstração e os mesmos adapters de `CotacaoHistoricaProvider` usados pelo registro.
- Adicionar uma consulta REST da COMPRA cronologicamente mais recente da mesma Carteira e Ação até a data da nova VENDA, retornando seu preço como sugestão editável.
- Tratar normalmente a inexistência de COMPRA anterior com resposta `200 OK` e sugestão nula.
- Documentar os novos endpoints, DTOs, semântica temporal e respostas de erro no OpenAPI.
- Preservar integralmente o contrato e a autoridade final de `POST /operacoes`: COMPRA continua sem `precoUnitario` e consulta novamente o fechamento; VENDA continua recebendo o preço escolhido pelo usuário; nenhuma variante recebe `ordemNoDia`.
- Não persistir prévias ou sugestões, não introduzir cache, migration, lock consultivo, dependência ou integração externa nova.

## Capabilities

### New Capabilities

- `purchase-price-preview`: consulta backend, sem efeitos colaterais, do fechamento histórico bruto e exato que informa a criação de uma COMPRA.
- `sale-price-suggestion`: consulta backend, sem efeitos colaterais, do preço da COMPRA cronologicamente aplicável mais recente para preencher inicialmente uma VENDA.

### Modified Capabilities

- `api-documentation`: documentação pública dos dois endpoints consultivos, de seus DTOs, da ausência normal de sugestão e dos erros padronizados da prévia.

## Impact

- API REST: dois novos endpoints `GET` sob os recursos de Operações e Carteiras.
- Aplicação: novos DTOs de resposta e serviços consultivos; extração/reuso da validação do fechamento histórico para impedir divergência com `POST /operacoes`.
- Persistência: nova consulta eficiente em `OperacaoRepository`, sem alteração de schema.
- Integrações: reutilização de BRAPI para `BRASIL` e Alpha Vantage para `EUA`, sem expor detalhes dos providers.
- Testes: contratos REST/OpenAPI, serviços, repository e regressão explícita do `POST /operacoes`, sempre com providers simulados.
