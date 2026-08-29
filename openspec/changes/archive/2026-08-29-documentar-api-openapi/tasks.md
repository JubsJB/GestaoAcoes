## 1. Dependência e configuração global

- [x] 1.1 Adicionar ao `pom.xml` somente `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0`, sem starter ou biblioteca Swagger/OpenAPI redundante.
- [x] 1.2 Confirmar que `java.version`, compiler source/target, toolchains e configuração de JDK permanecem inalterados em Java 17.
- [x] 1.3 Criar configuração OpenAPI global mínima com título, descrição e versão documental `0.0.1-SNAPSHOT` aprovados.
- [x] 1.4 Manter `/v3/api-docs`, `/v3/api-docs.yaml`, `/swagger-ui.html` e `/swagger-ui/index.html` nos paths padrão, disponíveis em todos os profiles e sem servers ou security schemes fictícios.

## 2. Documentação de Corretoras

- [x] 2.1 Adicionar a tag `Corretoras` ao `CorretoraResource` sem alterar seus mappings.
- [x] 2.2 Documentar `POST /corretoras` com request, resposta `201`, `Location` e erros públicos plausíveis.
- [x] 2.3 Documentar `GET /corretoras` com resposta de listagem e contrato de erro aplicável.
- [x] 2.4 Documentar `GET /corretoras/{id}` com path parameter, resposta e `404` aplicável.
- [x] 2.5 Documentar `GET /corretoras/por-cnpj?cnpj=...` com query parameter, normalização aceita, resposta e erros `400`/`404` aplicáveis.

## 3. Documentação de Ações

- [x] 3.1 Adicionar a tag `Ações` ao `AcaoResource` sem alterar seus mappings.
- [x] 3.2 Documentar `POST /acoes` com request, resposta `201`, `Location` e erros de validação, conflito e providers aplicáveis.
- [x] 3.3 Documentar `GET /acoes` com resposta de listagem e contrato de erro aplicável.
- [x] 3.4 Documentar `GET /acoes/{id}` com path parameter, resposta e `404` aplicável.
- [x] 3.5 Documentar `GET /acoes/por-ticker?ticker=...&mercado=...` com ambos os parâmetros, enum `Mercado`, resposta e erros `400`/`404` aplicáveis.
- [x] 3.6 Documentar `PATCH /acoes/{id}/cotacao` com path parameter, resposta e erros públicos de domínio/provider realmente possíveis.

## 4. Documentação de Carteiras e indicadores

- [x] 4.1 Adicionar as tags `Carteiras`, `Operações` e `Indicadores da Carteira` às operações correspondentes do `CarteiraResource`, sem alterar mappings.
- [x] 4.2 Documentar `POST /carteiras` com request, resposta `201`, `Location` e erros aplicáveis.
- [x] 4.3 Documentar `GET /carteiras` com resposta de listagem.
- [x] 4.4 Documentar `GET /carteiras/{id}` com path parameter, resposta e `404` aplicável.
- [x] 4.5 Documentar `PATCH /carteiras/{id}` com path parameter, request, resposta e erros aplicáveis.
- [x] 4.6 Documentar `DELETE /carteiras/{id}` com resposta `204` e conflitos/ausência aplicáveis.
- [x] 4.7 Documentar `GET /carteiras/{id}/operacoes` com path parameter, lista ordenada e `404` aplicável.
- [x] 4.8 Documentar `GET /carteiras/{id}/posicoes` com path parameter, response schema e `404` aplicável.
- [x] 4.9 Documentar `GET /carteiras/{id}/resultados-realizados` com path parameter, response schema e `404` aplicável.
- [x] 4.10 Documentar `GET /carteiras/{id}/patrimonio` com path parameter, patrimônio por moeda e `404` aplicável.
- [x] 4.11 Documentar `GET /carteiras/{id}/resumo` com path parameter, resumo por moeda e `404` aplicável.
- [x] 4.12 Documentar `POST /carteiras/{id}/snapshots` com path parameter, resposta `201`, `Location` e conflitos/ausência aplicáveis.
- [x] 4.13 Documentar `GET /carteiras/{id}/evolucao-patrimonial` com path parameter, série persistida e `404` aplicável.

## 5. Documentação de Operações

- [x] 5.1 Adicionar a tag `Operações` ao `OperacaoResource` sem alterar seus mappings.
- [x] 5.2 Documentar `POST /operacoes` com request, resposta `201`, `Location` e erros de validação, ausência e conflito aplicáveis.
- [x] 5.3 Documentar `GET /operacoes` com resposta de listagem e ordenação existente.
- [x] 5.4 Documentar `GET /operacoes/{id}` com path parameter, resposta e `404` aplicável.

## 6. Schemas públicos e erros

- [x] 6.1 Revisar os DTOs públicos dos 24 endpoints e adicionar `@Schema` somente onde houver semântica, exemplo, formato, enum, unidade, moeda, timestamp ou restrição relevante.
- [x] 6.2 Confirmar que annotations OpenAPI não foram adicionadas a entities, repositories, services, providers ou classes internas sem contrato público.
- [x] 6.3 Documentar `StandardError` como schema reutilizável com seus campos públicos e exemplos neutros.
- [x] 6.4 Mapear por operação apenas os status plausíveis entre `400`, `404`, `409`, `422`, `429`, `502`, `503` e `504`, incluindo `INTEGRIDADE_DADOS_VIOLADA` quando aplicável.
- [x] 6.5 Auditar o documento para garantir ausência de SQL, SQLState, constraints, tabelas, índices, colunas, stack traces, mensagens nativas, detalhes Hibernate/H2/PostgreSQL e secrets.

## 7. Testes direcionados de OpenAPI e Swagger UI

- [x] 7.1 Criar teste que confirme `GET /v3/api-docs` com sucesso e documento OpenAPI parseável.
- [x] 7.2 Confirmar no documento exatamente as 24 operações funcionais e seus 18 paths distintos, excluindo endpoints e assets técnicos do springdoc e sem endpoints fictícios.
- [x] 7.3 Testar título, descrição, versão documental e as cinco tags aprovadas.
- [x] 7.4 Testar path/query parameters, request bodies e responses representativos dos quatro Resources.
- [x] 7.5 Testar a presença dos schemas públicos relevantes e de `StandardError` nos componentes reutilizáveis.
- [x] 7.6 Testar ausência de security schemes, autenticação fictícia, `/v1` e informações sensíveis.
- [x] 7.7 Testar acesso ou redirecionamento de `/swagger-ui.html` e disponibilidade de `/swagger-ui/index.html` sem acoplamento ao HTML interno.
- [x] 7.8 Executar a suíte direcionada OpenAPI/Swagger e registrar total, failures, errors, skipped e exit code.

## 8. Regressão e validação final

- [x] 8.1 Executar a suíte completa com `./mvnw -q test` ou `mvnw.cmd -q test` e registrar exit code, total, failures, errors e skipped.
- [x] 8.2 Executar `clean verify` e registrar exit code, BUILD SUCCESS/FAILURE e total de testes.
- [x] 8.3 Confirmar por startup/testes H2, aplicação das migrations Liquibase 001–006 e Hibernate `ddl-auto=validate` sem alteração de schema.
- [x] 8.4 Confirmar que endpoints, DTOs funcionais, regras financeiras, providers, configurações sensíveis e migrations permanecem semanticamente inalterados.
- [x] 8.5 Executar `openspec validate documentar-api-openapi --strict` e confirmar todas as tarefas concluídas.
- [x] 8.6 Executar `openspec validate --all --strict` e registrar o resultado global.
- [x] 8.7 Atualizar Graphify e confirmar que a mudança adicionou somente dependência/configuração/metadados/testes documentais, sem fluxo funcional novo.
- [x] 8.8 Executar `git diff --check`, revisar `git diff` e `git status`, auditando ausência de arquivos fora do escopo e de operações Git proibidas.
