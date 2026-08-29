## Context

Veja `proposal.md` para a motivação. O projeto usa Spring MVC anotado e expõe 24 operações em `CorretoraResource`, `AcaoResource`, `CarteiraResource` e `OperacaoResource`. Não há springdoc, Swagger/OpenAPI, configuração ou annotations atuais.

A fonte oficial do springdoc recomenda `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0`; a release 3.1.0 foi atualizada para Spring Boot 4.1.0 e inclui Swagger UI 5.32.11. Os defaults documentados são `/v3/api-docs`, `/v3/api-docs.yaml` e `/swagger-ui.html`, com os recursos da UI também acessíveis sob `/swagger-ui/index.html`. As 24 operações funcionais ocupam 18 paths funcionais distintos; endpoints técnicos e assets do springdoc não participam dessa contagem.

O baseline aprovado para esta change é o estado real do repositório: `pom.xml` contém `<java.version>17</java.version>`, `java -version` retorna Temurin 17.0.14 e o Maven Wrapper executa com Java 17. A eventual migração para Java 25 fica explicitamente fora do escopo e deverá ocorrer em change independente.

## Goals / Non-Goals

**Goals:**

- gerar OpenAPI code-first para todos os 24 endpoints;
- oferecer documentação navegável e schemas reutilizáveis;
- concentrar descrições operacionais nos Resources e manter annotations de schema seletivas;
- provar por testes que a documentação é gerada sem mudar a aplicação.

**Non-Goals:**

- criar ou alterar endpoints, DTOs funcionais, regras, migrations ou providers;
- produzir um `openapi.yaml` manual e duplicado;
- introduzir autenticação, CORS, Actuator, `/v1` ou exposição de secrets;
- corrigir o baseline Java nesta change.

## Inventário dos contratos

| Tag | Operações |
|---|---|
| Corretoras | `POST /corretoras`; `GET /corretoras`; `GET /corretoras/{id}`; `GET /corretoras/por-cnpj` |
| Ações | `POST /acoes`; `GET /acoes`; `GET /acoes/{id}`; `GET /acoes/por-ticker`; `PATCH /acoes/{id}/cotacao` |
| Carteiras | `POST /carteiras`; `GET /carteiras`; `GET /carteiras/{id}`; `PATCH /carteiras/{id}`; `DELETE /carteiras/{id}` |
| Operações | `POST /operacoes`; `GET /operacoes`; `GET /operacoes/{id}`; `GET /carteiras/{id}/operacoes` |
| Indicadores da Carteira | `GET /carteiras/{id}/posicoes`; `GET /carteiras/{id}/resultados-realizados`; `GET /carteiras/{id}/patrimonio`; `GET /carteiras/{id}/resumo`; `POST /carteiras/{id}/snapshots`; `GET /carteiras/{id}/evolucao-patrimonial` |

Os requests públicos são `CorretoraCreateRequest`, `AcaoCreateRequest`, `CarteiraCreateRequest`, `CarteiraUpdateRequest` e `OperacaoCreateRequest`. Os responses públicos são `CorretoraResponse`, `AcaoResponse`, `CarteiraResponse`, `OperacaoResponse`, `PosicaoResponse`, `ResultadoRealizadoResponse`, `PatrimonioResponse`, `ResumoCarteiraResponse`, `SnapshotCarteiraResponse`, `EvolucaoPatrimonialResponse` e seus componentes monetários/temporais.

`StandardError` possui `timeStamp`, `status`, `error`, `message`, `path`, `code` e `details`. Os status públicos materiais são 400, 404, 409, 422, 429, 502, 503 e 504, além dos sucessos 200, 201 e 204.

## Decisions

### D1. Artifact recomendado

Usar somente `org.springdoc:springdoc-openapi-starter-webmvc-ui`. O projeto é Spring MVC e precisa tanto da descrição quanto da UI; adicionar também o starter `api` ou bibliotecas Swagger separadas seria redundante.

### D2. Versão recomendada

Usar `3.1.0`, versão publicada no Maven Central e indicada pela documentação oficial atual.

### D3. Compatibilidade

Spring Boot 4.1.0: confirmada pela release 3.1.0, que usa exatamente Boot 4.1.0. Jakarta/Spring MVC: suportados pela linha 3.x. Java 17: confirmado como baseline oficial e real do repositório e aprovado para esta change. Não serão alterados `java.version`, compiler source/target, toolchains ou configuração de JDK.

### D4. Abordagem code-first

Usar springdoc code-first. Resources, Bean Validation, tipos Java e DTOs permanecem a fonte executável; annotations complementam sem duplicar toda a API em YAML. A alternativa spec-first manual foi rejeitada pelo risco de divergência.

### D5. Capability

Usar `api-documentation`. O nome descreve o contrato transversal para consumidores sem acoplar a capability permanentemente a uma ferramenta. `openapi-documentation` foi preterido porque OpenAPI é a implementação escolhida, não o domínio documentado.

### D6. Profiles

OpenAPI e Swagger UI ficarão disponíveis em todos os profiles nesta fase do MVP acadêmico, sem configuração profile-specific. A documentação não poderá expor tokens, senhas de datasource, variáveis secretas ou detalhes internos. Se surgir deploy público ou ambiente formal de produção, o endurecimento dessa exposição deverá ser tratado em change futura.

### D7. Paths

Usar defaults: `/v3/api-docs`, `/v3/api-docs.yaml` e `/swagger-ui.html` (entrada que pode encaminhar para `/swagger-ui/index.html`). Não criar aliases ou paths customizados.

### D8. Configuração global

Criar uma classe pequena de configuração com bean `OpenAPI`/`Info`. Título: `Sistema de Gestão e Controle de Carteira de Investimentos API`. Descrição: `API REST para gerenciamento de corretoras, ações, carteiras, operações e indicadores de uma carteira de investimentos.` Versão documental recomendada: `0.0.1-SNAPSHOT`, igual ao projeto Maven, sem `/v1`.

### D9. Tags

Usar `@Tag` nos quatro Resources. `CarteiraResource` terá operações distribuídas entre Carteiras, Operações e Indicadores da Carteira por anotação operacional quando necessário.

### D10. Operações

Usar `@Operation` nos 24 métodos para summary, description e tag explícita. Não documentar comportamento apenas com nomes de métodos Java.

### D11. Responses

Usar `@ApiResponses` por endpoint, limitando-se ao sucesso e erros plausíveis. `@Content` referenciará o response real ou `StandardError`; não repetir todos os status em todas as operações.

### D12. Schemas

Usar inferência springdoc como base e `@Schema` seletivamente nos DTOs públicos somente quando houver valor documental real: semântica não óbvia, exemplo útil, formato, enum, unidade, moeda, timestamp ou restrição relevante. Não anotar mecanicamente campos triviais, entities, services, repositories, providers ou classes internas sem contrato público.

### D13. StandardError

Documentar `StandardError` como schema reutilizável, incluindo campos e exemplo público neutro. Os exemplos não conterão stack trace, SQL, constraints, nomes internos ou secrets.

### D14. Migration

Nenhuma.

### D15. Alteração funcional

Nenhuma.

### D16. Endpoints

Nenhum endpoint funcional será criado, removido ou renomeado. Apenas os endpoints técnicos do springdoc serão adicionados.

### D17. DTOs

Nenhuma alteração funcional. As annotations `@Schema` aprovadas serão somente metadados documentais seletivos.

### D18. pom.xml

Sim. Adicionar uma única dependência `springdoc-openapi-starter-webmvc-ui:3.1.0`, sem plugin Maven adicional e sem bibliotecas Swagger redundantes.

## Estratégia de testes

- iniciar o contexto Spring com H2/Liquibase/Hibernate validate;
- verificar `GET /v3/api-docs` com 200 e documento OpenAPI válido;
- confirmar exatamente as 24 operações REST distribuídas em 18 paths funcionais distintos, excluindo endpoints e assets técnicos do springdoc;
- verificar título, descrição, versão, tags, requests, responses e `StandardError`;
- verificar a entrada da Swagger UI por status/redirect/recurso, sem comparar HTML interno;
- confirmar ausência de security schemes fictícios e de secrets;
- executar toda a suíte e `clean verify`.

## Risks / Trade-offs

- [Migração futura para Java 25] → Mantê-la fora desta change e preservar Java 17.
- [Swagger UI disponível globalmente pode ampliar superfície em implantação futura] → Política aprovada para o MVP; revisar em change separada quando houver produção pública/autenticação.
- [Annotations extensas podem ficar desatualizadas] → Concentrar operações nos Resources, reutilizar `StandardError` e usar `@Schema` seletivamente.
- [springdoc 3.1.0 possui issues recentes envolvendo Spring Data REST] → O projeto não usa `spring-boot-starter-data-rest`; ainda assim, validar `/v3/api-docs` no stack real antes de considerar a implementação concluída.
- [Exemplos de erro podem prometer códigos indevidos] → Derivar a matriz de errors dos services/handler/specs e testar operações representativas.

## Migration Plan

Não há migração de dados. A implementação deverá adicionar a dependência, configuração e annotations documentais; implementar testes direcionados; executar a suíte, `clean verify`, validações OpenSpec e atualizar Graphify. Rollback consiste em remover somente metadados/configuração/dependência e testes OpenAPI.

## Open Questions

Nenhuma decisão material permanece pendente. Java 17, disponibilidade em todos os profiles e uso seletivo de `@Schema` foram aprovados.
