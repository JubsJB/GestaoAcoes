## Why

O fallback global de `DataIntegrityViolationException` atribui hoje toda violação de integridade a uma Corretora duplicada, mesmo quando a causa pertence a outra entidade, constraint ou operação. Além disso, algumas traduções específicas nos services abrangem exceções demais ou dependem de texto de mensagem, criando risco de resposta semanticamente falsa e comportamento diferente entre H2 e PostgreSQL.

## What Changes

- Introduzir um contrato transversal de tratamento seguro para violações de integridade persistente que não tenham sido traduzidas por um caso de uso específico.
- Substituir o fallback incorreto `CORRETORA_DUPLICADA` por `409 Conflict / INTEGRIDADE_DADOS_VIOLADA`, com mensagem pública neutra e sem detalhes internos.
- Preservar os erros específicos vigentes para CNPJ duplicado, Ação duplicada, ordem de Operação duplicada e colisão temporal de Snapshot somente quando a constraint conhecida puder ser identificada com segurança.
- Identificar de forma estruturada e centralizada o nome da constraint pela cadeia de causas do Hibernate, sem dependência direta do driver PostgreSQL e sem parsing de mensagens do banco; quando a identificação não estiver disponível, usar o fallback genérico.
- Manter as traduções de domínio nos services responsáveis e o handler global como última barreira de segurança, sem alterar contratos de sucesso, endpoints, cálculos financeiros ou persistência.
- Registrar internamente a exceção original no fallback, sem expor SQL, SQLState, tabela, coluna, constraint, stack trace ou mensagem nativa ao cliente.
- Não alterar schema, migrations, entidades, repositories, integrações externas ou dependências.

## Capabilities

### New Capabilities

- `api-error-handling`: definir o contrato transversal e seguro para respostas de violações de integridade não traduzidas e para preservação dos conflitos específicos conhecidos.

### Modified Capabilities

Nenhuma. Os contratos específicos já consolidados permanecem inalterados; a nova capability descreve o fallback transversal e os limites de sua tradução.

## Impact

- Planejamento para `ResourceExceptionHandler`, `ErrorCodes`, o componente compartilhado `ConstraintNameExtractor` e os services de persistência que hoje capturam `DataIntegrityViolationException`.
- Testes diretos do handler e regressões dos conflitos específicos de Corretora, Ação, Operação e Snapshot.
- Nenhum endpoint ou DTO de sucesso muda; o único contrato público novo é o fallback genérico para uma violação não classificada, em substituição à atribuição incorreta a Corretora.
- Estratégia híbrida, código/status do fallback, localização da classificação, capability transversal e logging interno estão aprovados e consolidados.
