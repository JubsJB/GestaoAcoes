## 1. Contratos de leitura

- [x] 1.1 Criar a projeÃ§Ã£o plana de repository com `snapshotId`, `dataHoraSnapshot`, `moeda` e `patrimonioAtual`, admitindo campos nulos para os resultados dos `LEFT JOIN`.
- [x] 1.2 Criar o DTO do componente patrimonial contendo exclusivamente `moeda` e `patrimonioAtual` como `BigDecimal`.
- [x] 1.3 Criar o DTO do ponto temporal contendo exclusivamente `snapshotId`, `dataHoraSnapshot` e `patrimonios`.
- [x] 1.4 Criar o DTO wrapper da evoluÃ§Ã£o contendo exclusivamente `carteiraId` e `pontos`.
- [x] 1.5 Garantir cÃ³pias imutÃ¡veis das coleÃ§Ãµes `pontos` e `patrimonios` nos contratos de resposta.

## 2. Consulta eficiente no repository

- [x] 2.1 Estender `SnapshotCarteiraRepository` com uma Ãºnica query de projeÃ§Ã£o ancorada em Carteira e `LEFT JOIN` para snapshots e componentes.
- [x] 2.2 Restringir a projeÃ§Ã£o aos quatro campos aprovados, sem carregar entidades completas ou alterar associaÃ§Ãµes JPA.
- [x] 2.3 Ordenar a query por `dataHoraSnapshot ASC`, `snapshotId ASC` e `moeda ASC`.
- [x] 2.4 Preservar na projeÃ§Ã£o a linha marcadora de Carteira existente sem snapshots e a linha de snapshot sem componentes.
- [x] 2.5 Confirmar que a consulta nÃ£o usa paginaÃ§Ã£o, filtros temporais, queries subsequentes ou repository paralelo.

## 3. ServiÃ§o de evoluÃ§Ã£o patrimonial

- [x] 3.1 Criar o service dedicado de evoluÃ§Ã£o com `@Transactional(readOnly = true)` e isolation padrÃ£o.
- [x] 3.2 Executar exatamente uma chamada Ã  query aprovada para consultar Carteira, snapshots e componentes.
- [x] 3.3 Converter ausÃªncia total de linhas em `404` pelo padrÃ£o vigente de Carteira nÃ£o encontrada, sem novo cÃ³digo de erro.
- [x] 3.4 Converter a linha marcadora sem snapshot em resposta `200` com `pontos=[]`.
- [x] 3.5 Agrupar as linhas planas por `snapshotId`, preservando a ordem temporal recebida da query.
- [x] 3.6 Converter snapshot sem componente em ponto com `patrimonios=[]`, sem fabricar BRL ou USD.
- [x] 3.7 Preservar componentes BRL/USD independentes e ordenados por moeda dentro de cada ponto.
- [x] 3.8 Projetar `patrimonioAtual` diretamente como `BigDecimal` persistido, sem cÃ¡lculo, conversÃ£o, MathContext ou arredondamento.

## 4. Endpoint REST

- [x] 4.1 Integrar o service ao recurso de Carteira seguindo a injeÃ§Ã£o por construtor vigente.
- [x] 4.2 Expor somente `GET /carteiras/{carteiraId}/evolucao-patrimonial`, sem body e sem `Location`.
- [x] 4.3 Responder `200 OK` com o wrapper aprovado para Carteira existente com sÃ©rie cheia, vazia ou contendo snapshots vazios.
- [x] 4.4 NÃ£o criar alias `/evolucao`, endpoint individual de snapshot, filtros, paginaÃ§Ã£o, update ou delete.

## 5. Testes do repository

- [x] 5.1 Testar projeÃ§Ã£o de Carteira com mÃºltiplos snapshots e componentes BRL/USD em uma Ãºnica consulta ordenada.
- [x] 5.2 Testar que Carteira existente sem snapshots produz a linha marcadora esperada.
- [x] 5.3 Testar que snapshot sem componentes permanece na projeÃ§Ã£o por causa do `LEFT JOIN`.
- [x] 5.4 Testar distinÃ§Ã£o entre Carteira inexistente, Carteira sem snapshots e snapshot vazio.
- [x] 5.5 Testar mÃºltiplos snapshots no mesmo dia e valores patrimoniais iguais em instantes diferentes sem deduplicaÃ§Ã£o.

## 6. Testes do service

- [x] 6.1 Testar agrupamento determinÃ­stico de mÃºltiplas linhas monetÃ¡rias no ponto temporal correto.
- [x] 6.2 Testar resposta vazia para Carteira existente sem snapshots e `404` para Carteira inexistente.
- [x] 6.3 Testar preservaÃ§Ã£o de snapshot vazio com `patrimonios=[]` e sem moedas artificiais.
- [x] 6.4 Testar sÃ©rie cronolÃ³gica crescente e componentes ordenados por moeda.
- [x] 6.5 Testar preservaÃ§Ã£o exata de valores `BigDecimal` em escala 12 sem recÃ¡lculo ou arredondamento.
- [x] 6.6 Verificar que o service chama o repository exatamente uma vez e nÃ£o interage com services financeiros ou providers.

## 7. Testes REST, arquitetura e concorrÃªncia

- [x] 7.1 Testar o contrato JSON completo e `200 OK` de `GET /carteiras/{carteiraId}/evolucao-patrimonial`.
- [x] 7.2 Testar `404` centralizado para Carteira inexistente e ausÃªncia dos novos cÃ³digos de erro proibidos.
- [x] 7.3 Testar Carteira sem snapshots e snapshot vazio no endpoint REST.
- [x] 7.4 Testar que `/carteiras/{carteiraId}/evolucao` nÃ£o foi criado e que filtros/paginaÃ§Ã£o nÃ£o integram o contrato.
- [x] 7.5 Criar teste arquitetural confirmando ausÃªncia de dependÃªncia de OperaÃ§Ãµes, `HistoricoCotacao`, posiÃ§Ã£o, patrimÃ´nio, resumo, resultado realizado e providers.
- [x] 7.6 Criar teste concorrente confirmando que a consulta observa a sÃ©rie antes ou depois do commit de novo snapshot, nunca pai com filhos parciais e sem locks adicionais.

## 8. RegressÃµes e validaÃ§Ã£o final

- [x] 8.1 Executar os testes direcionados de repository, service, resource, arquitetura e concorrÃªncia da evoluÃ§Ã£o patrimonial.
- [x] 8.2 Executar regressÃµes de criaÃ§Ã£o de snapshots, snapshot vazio, BRL/USD, atomicidade e concorrÃªncia de `portfolio-snapshot`.
- [x] 8.3 Executar regressÃµes de patrimÃ´nio atual, resumo, posiÃ§Ãµes, resultado realizado, histÃ³rico de cotaÃ§Ã£o e proteÃ§Ã£o de DELETE.
- [x] 8.4 Executar a suÃ­te completa com `./mvnw -q test` ou `./mvnw.cmd -q test` conforme o ambiente.
- [x] 8.5 Executar `clean verify` pelo Maven Wrapper e confirmar Liquibase 001â€“006 e Hibernate `ddl-auto=validate` sem migration nova.
- [x] 8.6 Validar PostgreSQL somente se datasource e credenciais estiverem disponÃ­veis, registrando a ausÃªncia quando nÃ£o estiverem.
- [x] 8.7 Executar `openspec validate evolucao-patrimonial --strict` e `openspec validate --all --strict`.
- [x] 8.8 Atualizar o Graphify, consultar o fluxo endpoint â†’ service â†’ projeÃ§Ã£o/repository e executar `git diff --check`, `git diff` e `git status` sem operaÃ§Ãµes Git de histÃ³rico.
