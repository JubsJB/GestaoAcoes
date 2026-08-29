## Context

O fluxo existente é `CorretoraResource → CorretoraService → CorretoraRepository → CorretoraMapper`. O cadastro usa `CnpjValidator.normalizeAndValidate`, que aceita exatamente o formato mascarado `NN.NNN.NNN/NNNN-NN` ou 14 dígitos, valida os dígitos verificadores e devolve 14 dígitos. O repository já declara `findByCnpj(String)`, e a coluna `corretora.cnpj` possui `VARCHAR(14)`, `NOT NULL` e unique constraint `uk_corretora_cnpj`.

As consultas atuais usam `@Transactional(readOnly = true)`, devolvem `CorretoraResponse`, lançam `ObjectNotFoundException` quando o registro não existe e não acionam providers. O contrato público aprovado para completar o RF06 é `GET /corretoras/por-cnpj?cnpj={cnpj}`.

## Goals / Non-Goals

**Goals:**

- completar o RF06 com uma consulta inequívoca de uma única Corretora pelo CNPJ;
- aceitar as duas representações já suportadas pelo validador sem duplicar normalização;
- reutilizar repository, mapper, DTO e tratamento centralizado existentes;
- preservar uma leitura local, read-only e sem efeitos colaterais;
- manter os três endpoints vigentes sem mudança de contrato.

**Non-Goals:**

- edição, exclusão, paginação ou pesquisa genérica de Corretoras;
- busca por nome ou razão social;
- revalidação por BrasilAPI, ViaCEP ou fonte de mercado financeiro;
- DTO, repository, query customizada, índice ou migration novos;
- alterações em Operações, frontend ou documentação OpenAPI.

## Decisions

### 1. Contrato REST da consulta por CNPJ

O contrato definitivo é `GET /corretoras/por-cnpj?cnpj={cnpj}`. A rota representa uma consulta singular, mantém a listagem inalterada, evita conflito com `/{id}` e transporta a máscara como query parameter sem depender de encoded slash. Não haverá alias alternativo.

**Alternativa A — `GET /corretoras/cnpj/{cnpj}`**

- Vantagens: rota explícita, resposta singular e separação clara de `GET /corretoras/{id}`.
- Desvantagens: a máscara oficial contém `/`; um CNPJ mascarado deixa de ser um único segmento de path. Depender de percent-encoding de barra é frágil e pode ser rejeitado pelo servidor/proxy antes de chegar ao resource.
- Impacto: seria necessário restringir o path a 14 dígitos ou criar tratamento de infraestrutura incompatível com a premissa de aceitar máscara.

**Alternativa B — `GET /corretoras?cnpj={cnpj}`**

- Vantagens: semântica comum de filtro de coleção, máscara transportada naturalmente como query parameter e facilidade no Angular.
- Desvantagens: o mesmo endpoint passaria a retornar lista sem parâmetro e objeto com parâmetro, ou teria de retornar lista de zero/um item. Ambas as opções conflitam com o requisito aprovado de resposta singular equivalente à consulta por ID e transformam a listagem em um mecanismo de filtro genérico fora do escopo.
- Impacto: aumenta a complexidade contratual de `GET /corretoras` e pode condicionar filtros futuros.

**Alternativa C — `GET /corretoras/por-cnpj?cnpj={cnpj}` (aprovada)**

- Vantagens: rota dedicada e inequívoca; resposta singular; não conflita com `/{id}`; aceita máscara sem barra no path; mantém `GET /corretoras` inalterado; é simples para Angular e deixa filtros futuros na rota de coleção.
- Desvantagens: usa um segmento de lookup e query parameter em vez de representar o CNPJ diretamente no path.
- Impacto: acrescenta somente um método GET dedicado no resource e não muda contratos existentes.

**Decisão:** utilizar exclusivamente a alternativa C. Ela satisfaz simultaneamente a aceitação de máscara, a resposta singular, a preservação da listagem e a ausência de dependência de encoded slash.

### 2. Reutilizar integralmente `CnpjValidator`

O service deverá chamar `normalizeAndValidate` antes do repository. Não há diferença necessária entre a validade de um CNPJ cadastrado e a de um CNPJ usado como chave de consulta: vazio, formato livre, quantidade incorreta, caracteres não aceitos, dígitos repetidos ou dígitos verificadores inválidos continuam produzindo `400 / CNPJ_INVALIDO`.

Alternativa descartada: criar um normalizador permissivo separado. Isso permitiria buscar valores que jamais poderiam ter sido persistidos e duplicaria regra de domínio.

### 3. Reutilizar `findByCnpj` e a infraestrutura de persistência existente

O método derivado já retorna `Optional<Corretora>` e resolve a consulta pela coluna única normalizada. Não será criada JPQL, native query, repository paralelo ou nova consulta de existência. A unique constraint existente fornece unicidade e índice adequado; não há necessidade de migration.

### 4. Estender o service e resource existentes

O futuro método do service será `@Transactional(readOnly = true)`, normalizará a entrada, executará uma consulta por CNPJ e mapeará com `CorretoraMapper`. Ausência produzirá `ObjectNotFoundException` com mensagem identificando o CNPJ normalizado, reutilizando o `404` centralizado e sem código novo.

O resource apenas receberá o parâmetro conforme a rota aprovada, delegará ao service e responderá `200 OK`. Não haverá body, persistência ou chamada a `CnpjProvider`, `CepProvider`, BrasilAPI ou ViaCEP.

### 5. Reutilizar `CorretoraResponse`

A consulta por CNPJ identifica o mesmo recurso já consultado por ID. Um DTO novo criaria dois contratos equivalentes e risco de divergência. Campos opcionais continuarão nulos quando ausentes.

### 6. Estratégia de testes da rota aprovada

- repository: `findByCnpj` encontra o valor normalizado, retorna vazio para inexistente e a unique constraint continua protegida;
- service: CNPJ com e sem máscara, normalização, inválidos, inexistente, DTO completo, transação read-only e ausência de interações externas/escritas;
- resource: `200` nas duas formas, `400 / CNPJ_INVALIDO`, `404` centralizado, contrato completo e ausência de conflito/regressão nos endpoints existentes;
- arquitetura: fluxo restrito a resource, service, validator, repository e mapper, sem adapters/providers;
- regressão: testes direcionados, suíte completa, `clean verify`, Liquibase/Hibernate validate, OpenSpec strict e Graphify.

## Risks / Trade-offs

- **CNPJ mascarado em path contém barra** → evitar path variable para o valor mascarado e usar query parameter na rota recomendada.
- **Service compartilhado ainda possui providers usados pelo POST** → testes de interação e arquitetura devem confirmar que o novo método não os chama.
- **Mensagem de 404 pode expor o CNPJ normalizado** → comportamento é coerente com as mensagens atuais que identificam a chave consultada; o CNPJ já integra o DTO público da Corretora.
- **Overload da rota de listagem poderia dificultar filtros futuros** → manter a consulta singular em rota dedicada.

## Migration Plan

A implementação futura deverá acrescentar somente o método read-only, o endpoint e seus testes. Não existe migração de banco ou dados. O rollback da futura implementação consiste em remover o endpoint e o método adicionados, sem afetar registros persistidos.
