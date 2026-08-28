## 1. Infraestrutura mínima de classificação

- [x] 1.1 Adicionar `INTEGRIDADE_DADOS_VIOLADA` a `ErrorCodes`, sem alterar ou reutilizar incorretamente os códigos específicos existentes.
- [x] 1.2 Criar `com.projeto.services.exceptions.ConstraintNameExtractor` como componente sem estado, com API pública mínima `Optional<String> extractConstraintName(Throwable exception)`.
- [x] 1.3 Implementar no extrator somente a travessia da cadeia de causas e a leitura estruturada de `ConstraintViolationException#getConstraintName()`, retornando vazio para exceção ausente, nome nulo, vazio ou não disponível.
- [x] 1.4 Manter o extrator independente de HTTP, handlers, DTOs, repositories, SQL, request path, drivers e regras de domínio, sem dependência nova.

## 2. Fallback global seguro

- [x] 2.1 Alterar exclusivamente o handler de `DataIntegrityViolationException` para responder `409 Conflict / INTEGRIDADE_DADOS_VIOLADA`, mensagem `A operação viola uma regra de integridade dos dados` e `details` vazio.
- [x] 2.2 Adicionar logging interno do path e da exceção original usando o facade já disponível, sem criar correlation ID, tracing ou infraestrutura nova.
- [x] 2.3 Garantir que o `StandardError` público do fallback não use a mensagem da exceção nem exponha SQL, SQLState, stack trace, constraint, tabela, índice, coluna ou detalhes de Hibernate/H2/PostgreSQL.

## 3. Traduções específicas nos casos de uso

- [x] 3.1 Alterar `CorretoraPersistenceService` para converter em `CORRETORA_DUPLICADA` somente a constraint estruturada `uk_corretora_cnpj`, propagando qualquer outra violação ao fallback global.
- [x] 3.2 Alterar `AcaoPersistenceService` para converter em `ACAO_DUPLICADA` somente a constraint estruturada `uk_acao_ticker_mercado`, preservando como genéricas violações da Ação ou do histórico inicial que não correspondam a ela.
- [x] 3.3 Alterar `OperacaoService` para converter em `ORDEM_OPERACAO_DUPLICADA` somente a constraint estruturada `uk_operacao_carteira_acao_data_ordem`, sem modificar cronologia, replay, locks, cálculos ou persistência funcional.
- [x] 3.4 Alterar `SnapshotCarteiraService` para converter em `SNAPSHOT_CARTEIRA_DUPLICADO` somente a constraint estruturada `uk_snapshot_carteira_carteira_data_hora`, sem modificar timestamp, transação, atomicidade ou cálculo.
- [x] 3.5 Preservar `uk_historico_cotacao_acao_data_hora`, `uk_snapshot_carteira_moeda_snapshot_moeda`, FKs, checks, not null, PKs e constraints desconhecidas sem ErrorCode específico novo, deixando-as alcançar o fallback genérico.
- [x] 3.6 Comparar constraint conhecida com `equalsIgnoreCase`, sem remover schema, aspas ou prefixos; correspondência estruturada não exata deve permanecer não classificada.

## 4. Eliminação do parsing textual

- [x] 4.1 Remover de `OperacaoService` e `SnapshotCarteiraService` os helpers que identificam constraint por `getMessage`, `contains`, regex ou transformação textual equivalente.
- [x] 4.2 Confirmar por busca e inspeção que nenhum fluxo afetado substituiu o parsing removido por outro parser de mensagem nativa.
- [x] 4.3 Confirmar que a classificação não importa `PSQLException`, não usa SQLState e não depende diretamente do driver PostgreSQL.

## 5. Testes unitários, do handler e dos services

- [x] 5.1 Criar testes de `ConstraintNameExtractor` para causa direta e causa aninhada contendo `ConstraintViolationException`, confirmando o nome estruturado retornado.
- [x] 5.2 Testar no extrator ausência de `ConstraintViolationException`, `constraintName` nulo/vazio e mensagem que menciona constraint sem causa estruturada, confirmando retorno vazio e ausência de parsing.
- [x] 5.3 Adicionar cobertura direta do `ResourceExceptionHandler` para violação genérica, validando status 409, código, mensagem pública, path e `details` vazio.
- [x] 5.4 Confirmar no teste do handler que SQL, SQLState, stack trace, mensagem nativa e nome de constraint não aparecem no payload; verificar logging interno sem acoplamento frágil ao texto completo do log.
- [x] 5.5 Atualizar testes de `CorretoraPersistenceService` para constraint correta produzindo `CORRETORA_DUPLICADA` e constraint ausente/desconhecida permanecendo genérica.
- [x] 5.6 Atualizar testes de `AcaoPersistenceService` para constraint correta produzindo `ACAO_DUPLICADA` e outras violações da Ação ou histórico permanecendo genéricas.
- [x] 5.7 Atualizar testes de `OperacaoService` para unique correta produzindo `ORDEM_OPERACAO_DUPLICADA`, outra constraint usando fallback e mensagem textual isolada não classificando a causa.
- [x] 5.8 Atualizar testes de `SnapshotCarteiraService` para unique temporal correta produzindo `SNAPSHOT_CARTEIRA_DUPLICADO`, unique monetária/outra constraint usando fallback e mensagem textual isolada não classificando a causa.
- [x] 5.9 Cobrir violação não modelada do Histórico de Cotação com o fallback genérico, sem criar código específico nem alterar regras de atualização/no-op.
- [x] 5.10 Preservar nos testes de resource os contratos específicos existentes e demonstrar que uma violação desconhecida nunca retorna código de Corretora, Ação, Operação ou Snapshot.
- [x] 5.11 Adicionar inspeção arquitetural compatível com o projeto para confirmar extrator com responsabilidade única, handler sem catálogo de constraints e ausência de dependência PostgreSQL/parsing textual.

## 6. Regressões funcionais e de banco

- [x] 6.1 Executar testes direcionados de Corretora, Ação, Operação, Histórico de Cotação, Snapshot, handler e extrator, registrando total, failures, errors e skipped.
- [x] 6.2 Executar testes integrados em H2 para constraints conhecidas e fallback desconhecido sem depender de diferenças textuais das mensagens do banco.
- [x] 6.3 Confirmar regressões da exclusão protegida de Carteira e dos conflitos específicos já modelados.
- [x] 6.4 Confirmar que endpoints, DTOs, providers, transações e cálculos financeiros permanecem inalterados.

## 7. Validações finais

- [x] 7.1 Executar a suíte completa com `./mvnw.cmd -q test`, registrando exit code, total, failures, errors e skipped.
- [x] 7.2 Executar `./mvnw.cmd -q clean verify` e confirmar BUILD SUCCESS, H2, Liquibase 001–006 e Hibernate `ddl-auto=validate` sem migration nova.
- [x] 7.3 Validar PostgreSQL somente se datasource e credenciais estiverem disponíveis, registrando explicitamente quando não for executado.
- [x] 7.4 Executar `openspec validate robustecer-tratamento-integridade --strict` e `openspec validate --all --strict`.
- [x] 7.5 Atualizar e consultar o Graphify para confirmar o fluxo `service → ConstraintNameExtractor → ApiException específica` ou `DataIntegrityViolationException → ResourceExceptionHandler → fallback genérico`.
- [x] 7.6 Executar `git diff --check`, revisar `git diff` e registrar `git status`, confirmando ausência de alterações em repositories, entidades, migrations, endpoints, DTOs, configurações ou dependências.
