## Context

Veja `proposal.md` para a motivação e `specs/stock-registration/spec.md` para o contrato observável. A change arquivada `cadastro-acao` estabeleceu `Acao`, `AcaoResponse`, `AcaoMapper`, `AcaoRepository`, `AcaoService`, `AcaoPersistenceService`, `AcaoResource` e os adapters de BRAPI e Alpha Vantage. Atualmente, o resource expõe somente `POST /acoes`, e o service usa os providers apenas para validar e enriquecer o cadastro antes da persistência.

O Graphify atualizado confirma que `AcaoRepository` estende `JpaRepository`, `AcaoMapper` já produz o DTO completo, e `ObjectNotFoundException` já é convertido pelo `ResourceExceptionHandler` em `404 Not Found` no formato `StandardError`. O precedente implementado em `consulta-corretora` usa o mesmo encadeamento resource → service → repository/mapper, `findAll(Sort)` com `id ASC`, `findById` e transações somente para leitura.

Esta change acrescentará somente operações locais de leitura. Não há necessidade de alterar o agregado `Acao`, o changelog Liquibase, configurações, dependências ou contratos dos providers.

## Goals / Non-Goals

**Goals:**

- adicionar os dois métodos de consulta ao resource e ao service existentes;
- reutilizar o DTO e o mapper completos já adotados pelo cadastro;
- obter uma listagem estável e equivalente no PostgreSQL e no H2;
- delimitar os métodos de consulta como operações somente para leitura;
- garantir por testes que nenhum `CotacaoProvider` seja consultado durante os GETs;
- preservar o comportamento atual de `POST /acoes`.

**Non-Goals:**

- criar outro service, outro resource, CQRS ou uma abstração genérica de consulta;
- adicionar paginação, filtros, busca por ticker ou mercado;
- atualizar cotação, timestamp ou qualquer campo da Ação durante leitura;
- modificar entidade, schema, Liquibase, dependências ou configurações;
- adicionar cache, chamadas externas ou funcionalidades de outros domínios.

## Decisions

### 1. Estender AcaoResource e AcaoService existentes

`AcaoResource` receberá dois métodos adicionais:

- `GET /acoes`, retornando `ResponseEntity<List<AcaoResponse>>`;
- `GET /acoes/{id}`, retornando `ResponseEntity<AcaoResponse>`.

Ambos apenas delegarão ao `AcaoService`. O `POST /acoes` e seu contrato permanecerão inalterados. O service existente receberá acesso direto ao `AcaoRepository` para as leituras; `AcaoPersistenceService` continuará responsável somente pela seção transacional curta e pela proteção de unicidade do cadastro.

Alternativa considerada: criar um `AcaoQueryService`. Foi rejeitada porque as duas consultas são simples, não possuem modelo de leitura próprio e a arquitetura existente já concentra cadastro e consulta de Corretora no mesmo service.

### 2. Reutilizar operações herdadas de JpaRepository

A listagem usará `AcaoRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))`. A consulta individual usará `AcaoRepository.findById(id)`. Não será criado método customizado ou JPQL porque `JpaRepository` já oferece todas as operações necessárias.

Os métodos `listar()` e `buscarPorId(Long id)` do service serão anotados com `@Transactional(readOnly = true)`. Essa delimitação não envolverá o fluxo de cadastro nem abrirá transação durante chamadas de rede.

Alternativa considerada: declarar `findAllByOrderByIdAsc()`. Foi rejeitada por duplicar uma capacidade já disponível por `findAll(Sort)` sem acrescentar regra de domínio.

### 3. Ordenar a listagem por id ascendente e não paginar

A ordenação padrão será `id ASC`. Ela é determinística, simples, compatível com PostgreSQL e H2 e segue a decisão já implementada em `consulta-corretora`. A ausência de registros resultará naturalmente em uma lista vazia, devolvida como `200 OK` com `[]`.

Ordenar por ticker ou nome exigiria definir prioridade entre mercados, sensibilidade a caixa e collation, decisões não estabelecidas pelo PRD. A paginação permanece fora do escopo; quando necessária, deverá ser especificada como evolução explícita do contrato.

### 4. Reutilizar AcaoMapper e AcaoResponse sem alterar dados

Cada entidade recuperada será convertida por `AcaoMapper.toResponse`. O DTO continuará contendo `id`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `cotacaoAtual` e `dataHoraCotacao`.

Os valores serão apenas lidos e mapeados. Em particular, `cotacaoAtual` continuará significando a última cotação persistida e `dataHoraCotacao` continuará representando a referência temporal persistida; os GETs não consultarão relógio, não aplicarão fallback temporal e não atualizarão registros.

Alternativa considerada: criar um DTO resumido para a lista. Foi rejeitada porque o escopo exige reutilizar o contrato completo existente e não há requisito de projeção distinta.

### 5. Reutilizar ObjectNotFoundException e o tratamento global atual

Quando `findById(id)` não encontrar uma ação, o service lançará `ObjectNotFoundException` com mensagem que identifique o ID consultado, seguindo a forma `Ação não encontrada para o id: {id}`. O `ResourceExceptionHandler` existente converterá a exceção em `404 Not Found` usando o `StandardError` atual.

Não será criado um novo código de erro nem alterado o formato global de erros, pois a solicitação exige reutilizar o padrão existente e uma revisão geral desse contrato está fora da change.

### 6. Manter providers e mutações fora dos GETs

Os dois métodos de consulta acessarão somente `AcaoRepository` e `AcaoMapper`. Eles não chamarão `CotacaoProvider.consultar`, `BrapiAdapter`, `AlphaVantageAdapter`, `AcaoPersistenceService`, `Clock` ou qualquer fluxo de atualização.

Como `AcaoService` continuará contendo as dependências necessárias ao POST, os testes verificarão explicitamente que `consultar` nunca é chamado nos providers durante listagem, consulta existente e consulta inexistente. A simples indexação dos providers feita no construtor não será confundida com uma chamada externa.

Alternativa considerada: retirar os providers do service por meio de refatoração ampla. Foi rejeitada porque não é necessária para implementar a leitura e poderia alterar o cadastro já estabilizado.

### 7. Estratégia de testes

Os testes permanecerão sem rede real:

- testes unitários do `AcaoService` cobrirão listagem com registros, ordem solicitada ao repository, lista vazia, ID existente, ID inexistente e ausência de chamadas a `CotacaoProvider.consultar`;
- testes do `AcaoRepository` sobre H2 cobrirão `findAll(Sort)` por `id ASC`, `findById` existente e inexistente e preservação dos valores persistidos;
- testes HTTP do `AcaoResource` cobrirão `200 OK` com lista ordenada, `200 OK` com `[]`, `200 OK` por ID e `404 Not Found` no formato atual;
- o teste que hoje afirma que os GETs estão fora do escopo será substituído pelos novos cenários;
- toda a suíte existente, especialmente os testes de cadastro e dos adapters, será executada para detectar regressões.

Não serão criados testes que dependam de BRAPI, Alpha Vantage ou PostgreSQL reais.

## Risks / Trade-offs

- [A listagem sem paginação poderá crescer] → manter esta primeira fatia conforme o escopo e especificar paginação separadamente quando houver requisito.
- [Ordenação por ID não é uma ordenação alfabética de negócio] → documentar o contrato como ordem técnica estável e não introduzir regras de collation não aprovadas.
- [AcaoService continuará reunindo dependências de cadastro e consulta] → limitar os novos métodos a repository/mapper e proteger a separação com verificações de ausência de chamadas aos providers.
- [O erro 404 atual não possui código específico] → preservar o padrão atual solicitado; qualquer evolução global do contrato de erros deverá ocorrer em outra change.

## Migration Plan

1. Adicionar o repository ao service e implementar os métodos de leitura somente para leitura.
2. Expor os dois GETs no resource existente.
3. Ampliar os testes unitários, de repository e HTTP, preservando os cenários do POST.
4. Executar testes e build pelo Maven Wrapper, validar a change em modo strict e atualizar o Graphify após a futura alteração de código.

Não há migração de dados, schema, configuração ou dependência. O rollback consiste em remover os métodos e testes acrescentados, sem alterar registros persistidos nem executar rollback Liquibase.
