## Why

O PRD exige o cadastro de ações brasileiras e americanas, mas o projeto atualmente possui somente a baseline técnica e o domínio de Corretora. Esta change introduz a primeira fatia vertical de Ação, validando o ticker no provedor correspondente antes de persistir os dados mínimos do ativo e de sua cotação atual.

## What Changes

- Adicionar `POST /acoes` para receber o ticker e o mercado informado pelo cliente.
- Normalizar o ticker com `trim` e uppercase, sem transformar pontuação ou sufixos, distinguir `BRASIL` de `EUA` e selecionar BRAPI ou Alpha Vantage conforme o mercado.
- Validar externamente a existência do ticker e obter nome da empresa, moeda, última cotação disponibilizada pelo provider e referência temporal da cotação antes da persistência.
- Persistir o ticker canônico quando a BRAPI informar explicitamente que o ticker brasileiro solicitado foi alterado ou renomeado.
- Fixar `BRASIL→BRL` e `EUA→USD`, exigindo cotação presente e maior que zero.
- Usar preferencialmente o timestamp confiável da cotação retornado pelo provider, normalizado para UTC, com fallback para o instante UTC em que a aplicação obteve a cotação.
- Validar ações americanas por correspondência exata em `SYMBOL_SEARCH`, consultar `GLOBAL_QUOTE` somente após essa confirmação e usar `OVERVIEW` apenas quando a busca não fornecer nome utilizável.
- Persistir somente ações validadas, impedindo duplicidade pelo par normalizado `(ticker, mercado)` tanto na aplicação quanto no banco.
- Isolar BRAPI e Alpha Vantage atrás de uma abstração de cotação, com `BRAPI_API_KEY`, `ALPHA_VANTAGE_API_KEY`, URLs e timeouts configurados externamente e sem impedir o outro mercado quando somente um provider estiver sem configuração.
- Tratar ticker inexistente, mercado inválido, cotação ou dados obrigatórios ausentes, resposta inválida, indisponibilidade, timeout e limite de requisições usando o padrão atual de erros.
- Evoluir o changelog Liquibase apenas com a tabela e as constraints necessárias para Ação, mantendo Hibernate em `ddl-auto=validate` no PostgreSQL e no H2 de testes.
- Criar testes unitários e de integração sem chamadas reais à BRAPI, Alpha Vantage ou PostgreSQL.
- Manter fora desta change listagem, consulta individual, atualização de cotação, histórico de cotações, Carteira, Operação, preço médio, rentabilidade, patrimônio e frontend.
- Responder o cadastro concluído com `201 Created`, `AcaoResponse` completo e `Location: /acoes/{id}`.

## Capabilities

### New Capabilities

- `stock-registration`: cadastro de ação brasileira ou americana a partir de ticker e mercado, com validação e enriquecimento pelo provedor externo correspondente, persistência única e resposta REST padronizada.

### Modified Capabilities

- Nenhuma.

## Impact

- API: novo endpoint `POST /acoes` e novos códigos de erro relacionados a Ação e integrações de mercado.
- Aplicação: novos DTOs, resource, service, mapper, entidade, repository, abstração de provedor e adapters para BRAPI e Alpha Vantage, seguindo a estrutura existente sob `com.projeto`.
- Configuração: novas propriedades externas para `BRAPI_API_KEY`, `ALPHA_VANTAGE_API_KEY`, URLs e timeouts, sem segredos versionados e com falha somente quando o provider não configurado for utilizado.
- Persistência: novo changeSet sequencial incluído no changelog master, criando somente a tabela `acao`, `cotacao_atual NUMERIC(19,6)`, `ticker VARCHAR(30)`, `nome_empresa VARCHAR(255)` e a unicidade de `(ticker, mercado)`.
- Dependências: nenhuma nova biblioteca é proposta; será reutilizado o `RestClient` síncrono já presente no Spring Web, o Liquibase atual e o relógio UTC da aplicação.
- Domínio existente: não altera Corretora nem cria relacionamento obrigatório entre Corretora e Ação.
