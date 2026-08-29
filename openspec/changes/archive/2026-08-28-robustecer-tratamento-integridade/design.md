## Context

Veja `proposal.md` para a motivação e a spec `api-error-handling` para o comportamento aprovado. O handler global possui um único `@ExceptionHandler(DataIntegrityViolationException.class)` que sempre responde `409 / CORRETORA_DUPLICADA` e a mensagem de CNPJ duplicado. Não existe logging no handler.

Quatro fluxos capturam a exceção antes do handler:

- `CorretoraPersistenceService.saveUnique` converte qualquer `DataIntegrityViolationException` de `saveAndFlush` em `CORRETORA_DUPLICADA`;
- `AcaoPersistenceService.saveUnique` converte qualquer violação ao salvar a Ação em `ACAO_DUPLICADA`, enquanto falha ao salvar o histórico inicial propaga ao fallback;
- `OperacaoService.cadastrar` converte somente quando encontra `uk_operacao_carteira_acao_data_ordem` por `contains` na mensagem da cadeia de causas;
- `SnapshotCarteiraService.criar` converte somente quando encontra `uk_snapshot_carteira_carteira_data_hora` pelo mesmo tipo de parsing textual.

Spring traduz violações de persistência para `DataIntegrityViolationException`, mas essa classe não expõe nome de constraint. Hibernate, já presente na stack JPA, oferece `org.hibernate.exception.ConstraintViolationException#getConstraintName()` na cadeia de causas. Essa informação pode variar ou estar ausente conforme banco/dialeto; ausência deve resultar em classificação conservadora, nunca em parsing de texto.

### Inventário das constraints 001–006

| Migration/tabela | Constraint nomeada | Tipo | Tradução atual | Possível fallback |
|---|---|---|---|---|
| 001 `corretora` | `pk_corretora` | PK | nenhuma | sim |
| 001 `corretora` | `uk_corretora_cnpj` | unique | `CORRETORA_DUPLICADA`, porém o catch atual não confirma a constraint | sim, após correção conservadora |
| 001 `corretora` | colunas obrigatórias | not null | indevidamente pode virar `CORRETORA_DUPLICADA` no save | sim |
| 002 `acao` | `pk_acao` | PK | nenhuma | sim |
| 002 `acao` | `uk_acao_ticker_mercado` | unique | `ACAO_DUPLICADA`, porém o catch atual não confirma a constraint | sim, após correção conservadora |
| 002 `acao` | `ck_acao_mercado`, `ck_acao_moeda`, `ck_acao_mercado_moeda`, `ck_acao_cotacao_positiva` | check | nenhuma | sim |
| 002 `acao` | colunas obrigatórias | not null | indevidamente pode virar `ACAO_DUPLICADA` no save | sim |
| 003 `carteira` | `pk_carteira` e colunas obrigatórias | PK/not null | nenhuma | sim |
| 004 `operacao` | `pk_operacao` | PK | nenhuma | sim |
| 004 `operacao` | `fk_operacao_carteira`, `fk_operacao_acao`, `fk_operacao_corretora` | FK | existência é antecipada; proteção de DELETE usa códigos próprios | corrida ou estado inesperado pode chegar |
| 004 `operacao` | `uk_operacao_carteira_acao_data_ordem` | unique | `ORDEM_OPERACAO_DUPLICADA` por parsing textual | sim quando parsing falha |
| 004 `operacao` | seis `chk_operacao_*` | check | validações antecipadas | estado inesperado pode chegar |
| 004 `operacao` | colunas obrigatórias | not null | validações antecipadas | estado inesperado pode chegar |
| 005 `historico_cotacao` | `pk_historico_cotacao` | PK | nenhuma | sim |
| 005 `historico_cotacao` | `fk_historico_cotacao_acao` | FK | relação controlada pelo fluxo | sim em falha inesperada |
| 005 `historico_cotacao` | `uk_historico_cotacao_acao_data_hora` | unique | sem ErrorCode específico | sim |
| 005 `historico_cotacao` | `ck_historico_cotacao_positiva` e colunas obrigatórias | check/not null | validações antecipadas | sim |
| 006 `snapshot_carteira` | PK, `fk_snapshot_carteira_carteira` | PK/FK | existência antecipada | sim em corrida/estado inesperado |
| 006 `snapshot_carteira` | `uk_snapshot_carteira_carteira_data_hora` | unique | `SNAPSHOT_CARTEIRA_DUPLICADO` por parsing textual | sim quando parsing falha |
| 006 `snapshot_carteira_moeda` | PK, `fk_snapshot_carteira_moeda_snapshot` | PK/FK | nenhuma | sim |
| 006 `snapshot_carteira_moeda` | `uk_snapshot_carteira_moeda_snapshot_moeda` | unique | sem ErrorCode específico | sim |
| 006 `snapshot_carteira_moeda` | dois `ck_snapshot_*` e colunas obrigatórias | check/not null | domínio antecipa moeda/precisão/positividade | sim em falha inesperada |

## Goals / Non-Goals

**Goals:**

- impedir atribuição falsa de uma violação genérica a Corretora ou outro domínio;
- manter traduções específicas somente para constraints comprovadamente conhecidas;
- tornar a classificação reutilizável, conservadora e independente do driver;
- manter resposta pública estável e segura em H2 e PostgreSQL;
- proteger os contratos atuais com testes do handler, services e resources.

**Non-Goals:**

- remodelar todas as exceções da aplicação;
- criar código específico para cada PK, FK, check ou not null existente;
- mudar validações, transações, repositories, entidades ou schema;
- alterar erros 400, 404, 422, 429, 502, 503 ou 504;
- introduzir observabilidade, correlation ID ou dependência nova.

## Decisions

As decisões abaixo estão aprovadas e são normativas para a implementação.

### D1 — Estratégia híbrida conservadora

Usar tradução específica próxima do caso de uso somente para constraints conhecidas e identificadas de forma estruturada; qualquer outra `DataIntegrityViolationException` chega ao fallback genérico.

- **Alternativa A — somente fallback genérico:** simples e segura, mas perderia contratos específicos em colisões concorrentes que somente o banco pode arbitrar.
- **Alternativa B — mapa central de todas as constraints:** concentra conhecimento, mas acopla o handler HTTP aos domínios e transforma toda constraint técnica em contrato público.
- **Alternativa C — híbrida recomendada:** preserva semântica de domínio nos services e mantém o handler como barreira final neutra.

Impacto: Corretora e Ação deixam de converter toda falha; Operação e Snapshot deixam de analisar mensagens; conflitos conhecidos permanecem iguais.

### D2 — Novo fallback `409 / INTEGRIDADE_DADOS_VIOLADA`

Adicionar um único ErrorCode transversal. A mensagem pública estável será `A operação viola uma regra de integridade dos dados`. `details` permanece vazio.

- `409 Conflict` representa conflito com o estado/regras persistentes e coincide com os conflitos de integridade específicos atuais.
- `400` atribuiria ao cliente uma entrada inválida que pode não ser a causa.
- `500` seria defensável para bugs internos de not null/check, mas impediria um contrato único e seguro para a exceção já traduzida por Spring; logs internos ainda permitem investigação.

Impacto: somente o fallback antes incorreto muda de contrato; códigos específicos corretos não mudam.

### D3 — Extrator compartilhado de constraint estruturada

Criar `com.projeto.services.exceptions.ConstraintNameExtractor`, componente pequeno e sem estado cuja única API pública será `Optional<String> extractConstraintName(Throwable exception)`. Ele percorre a cadeia de causas, localiza `org.hibernate.exception.ConstraintViolationException` e devolve somente `getConstraintName()` quando não nulo e não vazio. O componente não conhece HTTP, handler, DTO, repository, banco, request path ou códigos de domínio.

Cada service compara o valor estruturado com sua constante local por `equalsIgnoreCase`. Não remover schema, aspas ou prefixos e não aplicar qualquer transformação baseada em mensagem. Se a representação estruturada não corresponder exatamente, desconsiderada somente a caixa, a violação permanece não classificada.

- Não usar `SQLException`, `PSQLException`, SQLState ou classes do driver.
- Não usar `contains`, regex ou parsing da mensagem de H2/PostgreSQL.
- Hibernate não seria dependência nova: já é a implementação ORM da aplicação.
- Se Hibernate/dialeto não fornecer o nome, o comportamento seguro é o fallback genérico.

Alternativa: manter helpers locais por mensagem. Rejeitada por fragilidade e divergência H2/PostgreSQL.

### D4 — Localização da tradução

O handler central produz apenas o fallback genérico e logging. Os services responsáveis continuam criando `ApiException` específica após consultar o extrator compartilhado:

- Corretora: somente `uk_corretora_cnpj`;
- Ação: somente `uk_acao_ticker_mercado`;
- Operação: somente `uk_operacao_carteira_acao_data_ordem`;
- Snapshot: somente `uk_snapshot_carteira_carteira_data_hora`.

Não criar traduções específicas para histórico de cotação, componente monetário, FK, checks, not null ou constraints desconhecidas nesta change; elas usam o fallback genérico.

### D5 — Capability transversal nova

Criar `api-error-handling`. As specs atuais são organizadas por domínio, enquanto `development-datasource-configuration` cobre configuração de datasource, não contratos HTTP. Modificar quatro capabilities de domínio repetiria o mesmo fallback e sugeriria que ele pertence a cada cadastro isoladamente.

### D6 — Logging interno sem nova infraestrutura

Adicionar logging pelo facade já disponível na aplicação ao handler de fallback, incluindo o path e a exceção original. Não há correlation/request ID atual; não criar um nesta change. A resposta nunca copia a mensagem da exceção.

## Risks / Trade-offs

- [O Hibernate/dialeto pode não extrair o nome em algum banco] → usar fallback genérico; correção semântica tem prioridade sobre especificidade.
- [Uma constraint conhecida pode chegar com qualificação ou caixa diferente] → normalizar somente a representação estruturada de forma documentada e comparar o identificador lógico completo, sem recorrer à mensagem.
- [O fallback 409 pode ocultar internamente um bug de programação] → registrar a exceção original em nível adequado e manter o payload público seguro.
- [Services atuais possuem testes com exceções sintéticas sem causa estruturada] → atualizar os testes para construir causa estruturada para o caso conhecido e esperar fallback/propagação para a causa desconhecida.
- [Mudança transversal pode afetar regressões] → validar handler direto, os quatro conflitos específicos, H2, suíte completa e arquitetura sem driver PostgreSQL.

## Migration Plan

Nenhuma migration ou transformação de dados. Após aprovação, a implementação poderá ser revertida restaurando o handler e os classificadores anteriores, sem impacto no schema ou nos registros persistidos.
