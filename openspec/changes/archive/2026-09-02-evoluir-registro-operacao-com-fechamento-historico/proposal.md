## Why

O registro atual exige que o cliente informe `precoUnitario` e `ordemNoDia` para qualquer Operação, embora a decisão vigente determine que novas COMPRAS usem o fechamento histórico bruto da data exata e que a ordem intradiária seja controlada pelo backend. A evolução precisa reconciliar contrato, integrações e cronologia sem alterar operações existentes nem manter locks de banco durante chamadas externas.

## What Changes

- **BREAKING**: substituir o request uniforme de `POST /operacoes` por contrato discriminado: COMPRA proíbe `precoUnitario`, VENDA o exige e ambos rejeitam `ordemNoDia` e demais campos desconhecidos.
- Consultar, somente para COMPRA, o `close` diário bruto exatamente em `dataOperacao`, usando BRAPI para `BRASIL` e Alpha Vantage `TIME_SERIES_DAILY` para `EUA`, sem `adjustedClose`, cotação atual, pregão anterior ou preço manual como fallback.
- Classificar alcance do `outputsize=compact` da Alpha Vantage por regra objetiva: somente uma data anterior à menor retornada com pelo menos 100 candles válidos/distintos produz `HISTORICO_COTACAO_FORA_DO_ALCANCE`; série menor produz resposta externa inválida.
- Introduzir uma capability separada de cotação histórica diária, sem ampliar a responsabilidade de `CotacaoProvider` e sem reutilizar a tabela de histórico de cotação corrente como armazenamento OHLC.
- Gerar `ordemNoDia` como `MAX(ordemNoDia) + 1` por Carteira, Ação e data, após lock pessimista e dentro de transação curta; manter a constraint única como última defesa e anexar inserções posteriores ao fim do mesmo dia.
- Executar a consulta HTTP antes da transação curta, de modo que falhas externas impeçam qualquer persistência sem manter lock durante rede ou timeout.
- Preservar no response e na persistência `precoUnitario`, `ordemNoDia` e `valorTotal`; calcular `valorTotal` exclusivamente no backend.
- Documentar os novos erros `COTACAO_HISTORICA_INDISPONIVEL` e `HISTORICO_COTACAO_FORA_DO_ALCANCE`, preservando o mapeamento dos erros externos existentes.
- Manter dados existentes sem recalcular preços, renumerar ordens ou consultar histórico retroativamente; não criar migration nem alterar o changeSet histórico `004`.
- Manter a change frontend `implementar-operacoes-frontend` bloqueada até sua reconciliação posterior com este contrato, sem alterar `frontend/**` nesta change.

## Capabilities

### New Capabilities

- `historical-closing-price`: consulta tipada do fechamento bruto diário por mercado e data exata, com seleção de provider, validação do candle e classificação honesta de ausência, alcance e falhas externas.

### Modified Capabilities

- `operation-registration`: altera o contrato de criação, a origem do preço de COMPRA, a obrigatoriedade do preço de VENDA, a geração automática de `ordemNoDia` e a fronteira transacional do registro.
- `stock-quote-history`: separa o histórico de cotações correntes persistidas da nova consulta externa de fechamento diário e impede que `historico_cotacao` seja tratado como OHLC.
- `api-documentation`: atualiza o OpenAPI de `POST /operacoes` para o request discriminado, response preservado, dependência externa de COMPRA e novos erros históricos.

## Impact

- Backend REST: DTOs e validação de `POST /operacoes`, documentação OpenAPI e tratamento padronizado de erros.
- Aplicação/domínio: orquestração de COMPRA, transação curta, lock pessimista, geração de ordem e replay cronológico; cálculos financeiros existentes permanecem autoritativos.
- Integrações: nova abstração `CotacaoHistoricaProvider`, modelos de retorno e adapters históricos para BRAPI e Alpha Vantage, reutilizando configuração, `RestClient`, autenticação, timeouts e mapeamento comum quando coerente.
- Persistência: repository ganha consulta de máximo por data; entidade, colunas, constraints, índices e dados existentes permanecem inalterados, sem Liquibase novo.
- Testes: contrato, adapters, serviço, concorrência, regressão financeira, OpenAPI, Liquibase/Hibernate e validação OpenSpec.
- Especificações promovidas: remover formalmente os contracts antigos de preço universal, ordem informada e provider proibido no POST, substituindo-os sem coexistência contraditória após promoção.
- Frontend: nenhuma alteração nesta change; a implementação frontend em outro worktree deverá ser reconciliada somente após aprovação do novo contrato backend.
