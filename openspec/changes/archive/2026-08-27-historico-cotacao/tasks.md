## 1. Baseline e caracterização

- [x] 1.1 Confirmar por testes a semântica atual de `POST /acoes` e `PATCH /acoes/{id}/cotacao`, incluindo providers por mercado, ticker canônico, precisão, timestamp UTC, erros e contratos HTTP.
- [x] 1.2 Confirmar a monotonicidade e a coordenação concorrente atuais do PATCH, incluindo `PESSIMISTIC_WRITE`, provider fora da transação e retorno `200 OK` para timestamp igual ou anterior.
- [x] 1.3 Confirmar que posição, patrimônio, resumo e resultado realizado usam somente as fontes atuais e não consultam histórico de cotação.

## 2. Schema e persistência do histórico

- [x] 2.1 Criar o changeSet Liquibase `005` para `historico_cotacao` com PK `BIGINT` identity, `acao_id` obrigatório, `cotacao NUMERIC(19,6)` obrigatória e `data_hora_cotacao TIMESTAMP WITH TIME ZONE` obrigatória.
- [x] 2.2 Adicionar ao changeSet a FK sem cascade para `acao(id)`, check de cotação positiva, unique `(acao_id, data_hora_cotacao)` e rollback explícito, sem índice redundante.
- [x] 2.3 Incluir somente o novo changeSet no `db.changelog-master.yaml`, preservando integralmente os changeSets 001–004 e `ddl-auto=validate`.
- [x] 2.4 Criar `HistoricoCotacao` com apenas `id`, associação obrigatória e LAZY para `Acao`, `cotacao` e `dataHoraCotacao`, sem coleção em `Acao`, cascade ou campos duplicados.
- [x] 2.5 Criar o repository mínimo de `HistoricoCotacao`, sem métodos especulativos de API e com apenas as consultas necessárias à persistência e aos testes aprovados.

## 3. Cadastro inicial atômico

- [x] 3.1 Evoluir a fronteira transacional do cadastro para persistir a Ação e exatamente uma observação inicial na mesma transação.
- [x] 3.2 Reutilizar na observação inicial exatamente `Acao.cotacaoAtual` e `Acao.dataHoraCotacao`, sem segunda chamada ao provider ou nova normalização divergente.
- [x] 3.3 Garantir rollback integral quando a persistência da Ação ou do histórico inicial falhar, preservando o tratamento de duplicidade do cadastro.
- [x] 3.4 Preservar o contrato público do POST, incluindo `201 Created`, `Location`, `AcaoResponse`, validações e seleção BRAPI/Alpha Vantage.

## 4. Atualização de cotação atômica

- [x] 4.1 Evoluir a seção transacional curta do PATCH para inserir uma observação somente quando a candidata posterior for efetivamente aplicada sob o lock atual.
- [x] 4.2 Persistir atomicamente `Acao.cotacaoAtual`, `Acao.dataHoraCotacao` e a observação com os mesmos valores, revertendo tudo diante de qualquer falha.
- [x] 4.3 Preservar timestamp igual ou anterior como no-op com `200 OK`, sem alteração da Ação e sem insert histórico.
- [x] 4.4 Preservar mesmo preço em timestamp posterior como atualização válida que avança o timestamp e cria nova observação.
- [x] 4.5 Preservar contrato sem body, ausência de `Location`, ticker canônico, erros externos, detalhes da última cotação e provider fora da transação.

## 5. Testes de modelo, repository e infraestrutura

- [x] 5.1 Testar associação obrigatória, FK sem cascade, isolamento entre Ações e ausência de coleção bidirecional em `Acao`.
- [x] 5.2 Testar cotação positiva e exatamente representável em `NUMERIC(19,6)`, rejeitando zero, negativo e valores fora da precisão.
- [x] 5.3 Testar unique por Ação e timestamp, múltiplos timestamps da mesma Ação e o mesmo timestamp para Ações diferentes.
- [x] 5.4 Testar ordenação temporal somente na consulta concreta necessária aos testes, sem criar contrato REST ou API especulativa.
- [x] 5.5 Validar o novo changeSet e rollback quando suportado sobre H2, com Liquibase seguido de Hibernate `validate`.

## 6. Testes do POST e PATCH

- [x] 6.1 Testar cadastro BRASIL e EUA criando Ação e primeira observação com cotação e timestamp exatamente idênticos.
- [x] 6.2 Testar que falha do histórico inicial reverte o cadastro e que cada fluxo continua chamando somente seu provider uma vez.
- [x] 6.3 Testar PATCH posterior atualizando Ação e criando exatamente uma observação, inclusive quando o preço permanece numericamente igual.
- [x] 6.4 Testar PATCH com timestamp igual e anterior sem alteração ou histórico adicional.
- [x] 6.5 Testar que divergência de ticker canônico, erro externo, dados inválidos e falha de precisão preservam estado e histórico.
- [x] 6.6 Testar rollback integral quando a atualização da Ação ou o insert histórico falha.

## 7. Concorrência e compatibilidade

- [x] 7.1 Testar dois PATCH concorrentes com timestamps diferentes, comprovando lock preservado, ausência de duplicidade e registro somente da sequência temporal aceita.
- [x] 7.2 Comprovar que o estado final de `Acao` corresponde à observação aceita mais recente e que nenhuma candidata stale concluída depois é inserida.
- [x] 7.3 Testar que GET de Ações, posições, patrimônio e resumo não consulta o novo repository, não chama provider e não introduz N+1.
- [x] 7.4 Executar regressões de cadastro/consulta/PATCH de Ação, Operações, preço médio, ciclos, resultado não realizado, rentabilidade da posição, patrimônio, resumo, resultado realizado e DELETE protegido de Carteira.

## 8. Verificação final

- [x] 8.1 Auditar que não foi criado endpoint, DTO público, filtro, paginação, backfill, chamada histórica aos providers, scheduler, job, cron, cache, snapshot ou campo não aprovado.
- [x] 8.2 Executar testes direcionados, suíte completa e `mvnw clean verify`, confirmando Liquibase/Hibernate em H2 e PostgreSQL quando o ambiente estiver disponível.
- [x] 8.3 Validar a change e o conjunto global OpenSpec em modo strict e atualizar/consultar o Graphify após as alterações de código.
- [x] 8.4 Executar `git diff --check` e revisar `git diff` e `git status`, confirmando que somente o novo changeSet altera schema e que nenhuma dependência, provider ou changeSet 001–004 foi modificado.
