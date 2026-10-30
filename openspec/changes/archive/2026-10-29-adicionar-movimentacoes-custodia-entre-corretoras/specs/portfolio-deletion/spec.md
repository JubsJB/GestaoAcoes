## MODIFIED Requirements

### Requirement: Exclusão física isolada no modelo atual
Carteira existente SHALL ser excluída fisicamente somente quando não possuir operações, snapshots, movimentações ou estornos de custódia. Exclusão SHALL atingir apenas a carteira indicada, sem exclusão lógica, novas relações ou remoção de histórico/auditoria. MUST NOT excluir outras carteiras ou referências.

#### Scenario: Remoção efetiva do registro
- **WHEN** uma Carteira existente e elegível é excluída com sucesso
- **THEN** o registro deixa de existir na persistência e consultas posteriores por seu ID respondem `404 Not Found`

#### Scenario: Preservação das demais Carteiras
- **WHEN** uma Carteira é excluída e existem outras Carteiras persistidas
- **THEN** todas as demais Carteiras preservam exatamente seus identificadores, nomes e datas de criação


### Requirement: Compatibilidade com o schema vigente
Exclusão SHALL preservar entidade Carteira, changeSet003 e schema financeiro existente. Schema aditivo de custódia SHALL usar FKs restritivas sem cascade delete; inicialização Liquibase/Hibernate SHALL validar novos mapeamentos em PostgreSQL/H2 sem alterar changes históricos. Exclusão elegível SHALL funcionar com schema atualizado.

#### Scenario: Inicialização e exclusão no H2
- **WHEN** os testes iniciam o banco H2 pelo changelog Liquibase vigente e o Hibernate valida os mapeamentos
- **THEN** uma Carteira pode ser persistida e excluída com migration aditiva de custódia e sem alterar a entidade Carteira ou migrations antigas


## ADDED Requirements

### Requirement: Proteção da auditoria de custódia na exclusão
DELETE de carteira SHALL adquirir mesmo lock das escritas quantitativas e recusar existência de movimentação ou estorno de custódia, incluindo originais estornadas, com409 CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA. Bloqueios atuais de operações/snapshots SHALL permanecer e ter precedência vigente. Integridade referencial SHALL impedir remoção da auditoria mesmo em falha de serviço. Carteira sem qualquer histórico SHALL continuar elegível.

#### Scenario: Histórico de custódia preservado
- **WHEN** carteira possui custódia, inclusive estornada, e não há bloqueio financeiro anterior
- **THEN** retorna409 CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA sem apagar carteira ou registros

#### Scenario: Exclusão concorrente
- **WHEN** registro quantitativo e exclusão competem pela mesma carteira
- **THEN** lock determina um resultado coerente sem referência órfã ou auditoria apagada

#### Scenario: Bloqueio existente preservado
- **WHEN** carteira possui operações ou snapshots
- **THEN** mantém códigos e precedência atuais sem mudança para erro de custódia
