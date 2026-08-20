## Context

A change `cadastro-corretora` estabeleceu a entidade persistida `Corretora`, o DTO completo `CorretoraResponse`, o `CorretoraMapper`, o `CorretoraRepository`, o `CorretoraService`, o `CorretoraResource` e o tratamento padronizado de erros. Atualmente, o recurso expõe apenas o cadastro por `POST /corretoras` e o serviço de cadastro coordena as integrações com BrasilAPI e ViaCEP.

Esta change acrescenta a leitura das corretoras já persistidas por meio de `GET /corretoras` e `GET /corretoras/{id}`. A consulta deve permanecer totalmente local: nenhuma leitura pode acionar `CnpjProvider`, `CepProvider` ou outra integração externa. O schema, o modelo persistente e o contrato de resposta já existentes são suficientes para essa fatia.

O Graphify atualizado confirma a estrutura em camadas e as relações que serão reaproveitadas: `CorretoraResource` delega ao `CorretoraService`, que utiliza `CorretoraRepository` e `CorretoraMapper`; `CorretoraRepository` já estende `JpaRepository`; e `ObjectNotFoundException` já é convertido pelo `ResourceExceptionHandler` para o padrão HTTP atual.

## Goals / Non-Goals

**Goals:**

- adicionar os dois endpoints de consulta ao resource e ao service existentes;
- retornar o `CorretoraResponse` completo já adotado no cadastro;
- listar todas as corretoras em ordem determinística por `id` ascendente;
- retornar `404 Not Found` no padrão atual quando o ID não existir;
- garantir por testes que os GETs consultem somente o repositório e não acionem integrações externas;
- preservar o comportamento atual do cadastro.

**Non-Goals:**

- criar um serviço separado de consulta, CQRS ou novas abstrações arquiteturais;
- adicionar paginação, filtros ou busca por CNPJ;
- modificar a entidade, o schema, os changelogs Liquibase ou as dependências;
- implementar cadastro, atualização ou exclusão nesta change;
- revalidar CNPJ, CEP ou qualquer outro dado em APIs externas;
- adicionar cache ou outras otimizações prematuras;
- implementar funcionalidades de outros domínios ou frontend.

## Decisions

### 1. Estender o resource e o service existentes

Os métodos de consulta serão adicionados ao `CorretoraResource` e ao `CorretoraService` já existentes. O resource continuará enxuto e apenas traduzirá HTTP para chamadas do service:

- `GET /corretoras` retorna a coleção de `CorretoraResponse`;
- `GET /corretoras/{id}` retorna um único `CorretoraResponse`.

Criar outro resource ou outro service apenas para esta primeira fatia aumentaria a estrutura sem uma separação de responsabilidades que o escopo atual exija. O `POST /corretoras` permanecerá inalterado.

### 2. Reutilizar as operações herdadas de JpaRepository

A listagem utilizará `findAll(Sort)` com `Sort.by(Sort.Direction.ASC, "id")`, e a consulta individual utilizará `findById(id)`. Não será criado método de repository ou consulta customizada porque `JpaRepository` já oferece as operações necessárias.

Os métodos de leitura do service serão transacionais somente para leitura, sem alterar a transação usada pelo fluxo de cadastro.

### 3. Listagem não paginada e ordenada por ID ascendente

Esta primeira fatia retornará uma lista JSON não paginada. A ordenação padrão será `id ASC`, por ser estável, simples e equivalente em H2 e PostgreSQL. A ausência de registros produzirá `200 OK` com `[]`.

Ordenar por razão social dependeria de regras de collation e de uma regra de produto que o PRD não define. Paginação também não integra o escopo solicitado e permanece uma evolução futura.

### 4. Reutilizar CorretoraMapper e CorretoraResponse

Cada entidade recuperada será convertida pelo `CorretoraMapper.toResponse`. O contrato retornará todos os campos já presentes em `CorretoraResponse`, inclusive `id`, `validadaMercadoFinanceiro` e `dataCadastro`. Campos opcionais ausentes permanecerão `null`, como no cadastro.

Não serão expostas entidades JPA diretamente e não será criado um segundo DTO com conteúdo equivalente.

### 5. Reutilizar o tratamento atual para ID inexistente

Quando `findById(id)` não encontrar uma corretora, o service lançará `ObjectNotFoundException` com mensagem que identifique o ID consultado. O `ResourceExceptionHandler` existente continuará responsável por converter a exceção em `404 Not Found` usando o `StandardError` atual.

Não será criado um novo tipo de erro nem será alterado o formato global de respostas apenas para esta consulta.

### 6. Manter as integrações externas fora das consultas

Os métodos GET dependerão exclusivamente do `CorretoraRepository` e do `CorretoraMapper`. Eles não chamarão `CnpjProvider`, `CepProvider`, `CnpjValidator` nem o fluxo de persistência usado pelo cadastro.

Essa separação será verificada nos testes de service e de endpoint por meio de mocks das integrações e asserções de ausência de interação.

### 7. Estratégia de testes

Os testes serão proporcionais à funcionalidade e não realizarão chamadas reais à BrasilAPI ou ViaCEP:

- testes unitários do service para listagem ordenada, lista vazia, consulta por ID existente e ID inexistente;
- verificações de que os providers externos não são acionados em nenhuma consulta;
- testes do repository para leitura ordenada e busca por ID sobre o banco de teste;
- testes de integração HTTP para os dois endpoints, incluindo conteúdo completo do DTO, ordenação, lista vazia e o erro `404` padronizado;
- reexecução dos testes existentes do cadastro para confirmar ausência de regressão.

## Risks / Trade-offs

- **Crescimento da listagem sem paginação:** a resposta poderá ficar grande no futuro. A escolha mantém esta fatia pequena e segue o escopo atual; paginação deverá ser especificada quando houver requisito para ela.
- **Ordenação por ID não é alfabética:** ela representa a sequência estável de persistência, não uma ordenação de negócio. Evita introduzir regras de collation não definidas pelo PRD.
- **Service compartilhado entre cadastro e consulta:** o service mantém dependências de integração necessárias ao POST, embora os GETs não as utilizem. Isso preserva a arquitetura existente e os testes impedirão o uso acidental dos providers durante consultas.
- **Formato atual do erro:** o `StandardError` existente será preservado mesmo que alguns campos opcionais permaneçam vazios. Alterar globalmente o contrato de erros está fora desta change.

## Migration Plan

1. Adicionar os métodos de leitura no service e no resource existentes.
2. Reutilizar as operações herdadas do repository e o mapper atual.
3. Adicionar os testes unitários e de integração definidos.
4. Executar testes e build pelo Maven Wrapper, validar a change em modo strict e atualizar o Graphify após a futura alteração de código.

Não há migração de dados ou schema, alteração de configuração, dependência nova ou etapa especial de implantação. Um rollback consiste em remover os endpoints, métodos e testes acrescentados, sem impacto nos dados persistidos.
