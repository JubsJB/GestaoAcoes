## Context

Veja `proposal.md` para a motivação. A auditoria confrontou o PRD 1.1, as 16 specs principais, 27 archives funcionais/de infraestrutura, a change ativa `estabilizar-infraestrutura-base`, quatro Resources, os DTOs públicos, `ErrorCodes`, migrations 001–006, `pom.xml`, testes e o grafo Graphify.

O backend expõe 24 operações REST. A comparação automática entre capabilities presentes em archives e em `openspec/specs/` encontrou uma única ausência: `portfolio-creation`, originada por `2026-08-21-criacao-carteira`. O endpoint e seus componentes existem, mas o delta nunca se tornou spec principal. As demais capabilities arquivadas possuem spec principal correspondente.

O PRD declara que os endpoints iniciais podem ser refinados durante a modelagem. As diferenças de path/método hoje existentes foram aprovadas nas respectivas changes e não representam mudança funcional pendente: consulta singular de Corretora usa `/por-cnpj`, consulta singular de Ação usa ticker + mercado, atualização de cotação usa `PATCH`, operações possuem coleção própria e evolução patrimonial usa `/evolucao-patrimonial`.

## Goals / Non-Goals

**Goals:**

- restaurar a rastreabilidade documental de RF13 e `POST /carteiras` por meio de `portfolio-creation`;
- registrar a matriz final PRD → capability → endpoint/implementação;
- confirmar a cobertura das specs existentes e preparar uma base verificável para a futura documentação OpenAPI;
- manter a change exclusivamente documental.

**Non-Goals:**

- alterar PRD, Java, testes, migrations, schema, endpoints, DTOs, providers, configuração, dependências ou regras financeiras;
- instalar springdoc, Swagger UI ou adicionar annotations OpenAPI;
- transformar itens futuros em requisitos do MVP;
- corrigir a change ativa de infraestrutura ou arquivar qualquer change.

## Decisions

### D1. Existe lacuna real para criação de Carteira

Sim. `POST /carteiras` implementa RF13, possui archive próprio e delta `portfolio-creation`, porém `openspec/specs/portfolio-creation/spec.md` não existe. `portfolio-query`, `portfolio-update` e `portfolio-deletion` cobrem somente leitura, alteração e exclusão.

### D2. Preservar o nome `portfolio-creation`

Será usado `portfolio-creation`, pois é o identificador já aprovado no archive original e descreve exatamente a fatia. `portfolio-registration` criaria um segundo nome para a mesma decisão histórica; `portfolio-creation` também é consistente com as capabilities separadas por caso de uso (`portfolio-query`, `portfolio-update`, `portfolio-deletion`).

### D3. Não criar outras capabilities

A comparação archive → spec principal não encontrou outra promoção ausente. Os 23 endpoints restantes estão cobertos pelas 16 specs atuais. A change ativa `estabilizar-infraestrutura-base` possui delta `application-runtime-baseline`, mas ainda não arquivado; portanto não é lacuna de promoção.

### D4. Não modificar specs principais existentes

As specs atuais descrevem os contratos implementados relevantes. Não foram encontrados requisitos obsoletos que exigissem `MODIFIED` ou `REMOVED`. Refinamentos de rota aprovados já aparecem em `broker-registration`, `stock-registration` e `portfolio-evolution`.

### D5. Criar somente a spec faltante e preservar o PRD

A change adicionará somente `portfolio-creation`. O PRD permanecerá como documento de produto: sua seção de API é inicial e explicitamente refinável. Duplicar nela todos os detalhes OpenSpec aumentaria o risco de duas fontes contratuais concorrentes. O inventário abaixo será a base da futura change OpenAPI.

### D6. Tratar OpenAPI em change separada

`pom.xml` não contém springdoc/Swagger/OpenAPI; não há configuração, annotations ou Swagger UI. Instalação, configuração e documentação gerada alteram dependências e superfície operacional, portanto pertencem a uma futura `documentar-api-openapi`.

### D7. Nenhuma alteração funcional

Não há necessidade de código, teste funcional, migration, entidade, repository, endpoint, DTO, provider, configuração ou dependência nesta change.

## Matriz PRD → capability

| Requisitos | Estado documental | Capability principal |
|---|---|---|
| RF01–RF06 | Implementados e documentados | `broker-registration` |
| RF07–RF12 | Implementados e documentados | `stock-registration`; persistência histórica complementar em `stock-quote-history` |
| RF13 | Implementado, documentação principal ausente | nova promoção `portfolio-creation` |
| RF14–RF16 | Implementados e documentados | `operation-registration` |
| RF17–RF23 | Implementados e documentados | `portfolio-position` |
| RF24 | Implementado e documentado | `portfolio-valuation` |
| RF25 | Implementado e documentado | `portfolio-snapshot` + `portfolio-evolution` |
| RF26 | Implementado e documentado | `realized-result` |
| Consultas de Operação | Implementadas e documentadas | `operation-query` |
| Consulta/alteração/exclusão de Carteira | Implementadas e documentadas | `portfolio-query`, `portfolio-update`, `portfolio-deletion` |
| Resumo/rentabilidade atual da Carteira | Implementados e documentados | `portfolio-summary` |
| Fallback de integridade | Implementado e documentado | `api-error-handling` |

## Inventário REST e rastreabilidade

| Método e path | Entrada principal | Sucesso | Resposta | Capability |
|---|---|---:|---|---|
| `POST /corretoras` | `CorretoraCreateRequest` | 201 | `CorretoraResponse` | `broker-registration` |
| `GET /corretoras` | — | 200 | lista de `CorretoraResponse` | `broker-registration` |
| `GET /corretoras/{id}` | `id` | 200 | `CorretoraResponse` | `broker-registration` |
| `GET /corretoras/por-cnpj?cnpj=` | `cnpj` | 200 | `CorretoraResponse` | `broker-registration` |
| `POST /acoes` | `AcaoCreateRequest` | 201 | `AcaoResponse` | `stock-registration` |
| `GET /acoes` | — | 200 | lista de `AcaoResponse` | `stock-registration` |
| `GET /acoes/{id}` | `id` | 200 | `AcaoResponse` | `stock-registration` |
| `GET /acoes/por-ticker?ticker=&mercado=` | ticker + mercado | 200 | `AcaoResponse` | `stock-registration` |
| `PATCH /acoes/{id}/cotacao` | `id` | 200 | `AcaoResponse` | `stock-registration`, `stock-quote-history` |
| `POST /carteiras` | `CarteiraCreateRequest` | 201 | `CarteiraResponse` | `portfolio-creation` |
| `GET /carteiras` | — | 200 | lista de `CarteiraResponse` | `portfolio-query` |
| `GET /carteiras/{id}` | `id` | 200 | `CarteiraResponse` | `portfolio-query` |
| `PATCH /carteiras/{id}` | `CarteiraUpdateRequest` | 200 | `CarteiraResponse` | `portfolio-update` |
| `DELETE /carteiras/{id}` | `id` | 204 | sem corpo | `portfolio-deletion` |
| `GET /carteiras/{id}/operacoes` | `id` | 200 | lista de `OperacaoResponse` | `operation-query` |
| `GET /carteiras/{id}/posicoes` | `id` | 200 | lista de `PosicaoResponse` | `portfolio-position` |
| `GET /carteiras/{id}/resultados-realizados` | `id` | 200 | lista de `ResultadoRealizadoResponse` | `realized-result` |
| `GET /carteiras/{id}/patrimonio` | `id` | 200 | `PatrimonioResponse` | `portfolio-valuation` |
| `GET /carteiras/{id}/resumo` | `id` | 200 | `ResumoCarteiraResponse` | `portfolio-summary` |
| `POST /carteiras/{id}/snapshots` | `id` | 201 | `SnapshotCarteiraResponse` | `portfolio-snapshot` |
| `GET /carteiras/{id}/evolucao-patrimonial` | `id` | 200 | `EvolucaoPatrimonialResponse` | `portfolio-evolution` |
| `POST /operacoes` | `OperacaoCreateRequest` | 201 | `OperacaoResponse` | `operation-registration` |
| `GET /operacoes` | — | 200 | lista de `OperacaoResponse` | `operation-query` |
| `GET /operacoes/{id}` | `id` | 200 | `OperacaoResponse` | `operation-query` |

## DTOs públicos auditados

Requests: `CorretoraCreateRequest`, `AcaoCreateRequest`, `CarteiraCreateRequest`, `CarteiraUpdateRequest` e `OperacaoCreateRequest`.

Responses: `CorretoraResponse`, `AcaoResponse`, `CarteiraResponse`, `OperacaoResponse`, `PosicaoResponse`, `ResultadoRealizadoResponse`, `PatrimonioResponse`/`PatrimonioMoedaResponse`, `ResumoCarteiraResponse`/`ResumoMoedaResponse`, `SnapshotCarteiraResponse`/`SnapshotCarteiraMoedaResponse` e `EvolucaoPatrimonialResponse`/ponto/moeda. Os contratos relevantes estão representados nas respectivas specs; `CarteiraResponse` da criação será consolidado por `portfolio-creation`.

## Migrations auditadas

| Migration | Estrutura | Capabilities relacionadas |
|---|---|---|
| 001 | `corretora`, `uk_corretora_cnpj` | `broker-registration` |
| 002 | `acao`, `uk_acao_ticker_mercado` | `stock-registration` |
| 003 | `carteira` | `portfolio-creation`, query/update/deletion |
| 004 | `operacao`, FKs e `uk_operacao_carteira_acao_data_ordem` | operation registration/query e cálculos derivados |
| 005 | `historico_cotacao`, FK e unique temporal | `stock-quote-history` |
| 006 | snapshots pai/moeda, FKs e uniques temporal/monetária | `portfolio-snapshot`, `portfolio-evolution`, `portfolio-deletion` |

Nenhuma inconsistência funcional de schema foi encontrada; as migrations permanecem intocadas e o Hibernate continua com `ddl-auto=validate` conforme as specs pertinentes.

## ErrorCodes e erros públicos

Os códigos públicos de validação, providers, duplicidade, posição, exclusão protegida, snapshot, cronologia e precisão permanecem descritos nas capabilities funcionais quando materialmente relevantes. `api-error-handling` cobre `INTEGRIDADE_DADOS_VIOLADA`, fallback 409, classificação estruturada das constraints conhecidas e não exposição de detalhes internos. Não é necessário criar uma spec por `ErrorCode` interno.

## Risks / Trade-offs

- [O PRD mantém exemplos de paths anteriores aos refinamentos] → Preservar o PRD como requisito de produto e tratar as specs aprovadas como contrato detalhado; a futura OpenAPI será gerada a partir da API real.
- [A spec restaurada descreve comportamento já existente] → Validar somente documentação e regressão de arquivos, sem tocar na implementação.
- [A change ativa de infraestrutura ainda não possui spec principal] → Não tratá-la como lacuna: sua promoção pertence ao próprio archive futuro.
- [Inventário manual pode ficar desatualizado] → Usá-lo como baseline de auditoria; a futura OpenAPI deverá automatizar o contrato navegável.

## Migration Plan

1. Validar a delta `portfolio-creation` contra o archive, a implementação e RF13.
2. Confirmar novamente que nenhuma outra capability arquivada carece de spec principal.
3. Executar validações OpenSpec strict e inspeções Git sem alterar arquivos funcionais.
4. Ao arquivar esta change, promover `portfolio-creation` para `openspec/specs/portfolio-creation/spec.md`.

Rollback documental: remover somente a spec promovida e restaurar a change arquivada, sem qualquer rollback de banco ou aplicação.
