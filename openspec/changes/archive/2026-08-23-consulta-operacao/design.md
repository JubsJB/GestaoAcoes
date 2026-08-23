## Context

Consulte `proposal.md` para a motivação e `specs/operation-query/spec.md` para o comportamento observável. O modelo `Operacao`, seu repository, service, mapper, response e resource já existem para `POST /operacoes`; os relacionamentos com Carteira e Ação são obrigatórios, Corretora é opcional e todos são `LAZY`. O projeto já implementa listagens e consultas individuais em services com `@Transactional(readOnly = true)`, `findAll(Sort)`, `findById`, mapper e `ObjectNotFoundException`.

O Graphify confirma a cadeia atual `Resource → Service → Repository → Mapper`, `OperacaoResource` sob `/operacoes`, `CarteiraResource` sob `/carteiras` e a dependência já existente de `OperacaoService` em `CarteiraRepository`. Não há necessidade de nova entidade, migration, integração ou camada.

## Goals / Non-Goals

**Goals:**

- Acrescentar as três leituras sem alterar o fluxo transacional de criação.
- Produzir listas determinísticas e DTOs completos a partir do estado persistido.
- Manter o mapeamento de relacionamentos `LAZY` seguro ao projetar o DTO dentro de transações read-only.
- Preservar o tratamento centralizado de 404 e todos os contratos existentes.
- Manter a implementação pequena, usando consultas derivadas ou herdadas do Spring Data.

**Non-Goals:**

- Não executar replay, validar novamente uma VENDA nem derivar posição ou indicador financeiro.
- Não adicionar filtros, paginação, busca por ticker/mercado/tipo/Corretora ou intervalo de datas.
- Não consultar integrações externas, usar `Clock` nas leituras ou atualizar cotação.
- Não alterar `Operacao`, `OperacaoResponse`, Liquibase, schema, dependências ou configuração.
- Não adicionar endpoint de escrita, alteração ou exclusão de Operação.

## Decisions

### 1. Reutilizar o repository e consultas Spring Data simples

`OperacaoRepository` continuará sendo o único acesso persistente de Operação. A implementação usará:

- listagem geral: reutilizar `findAll(Sort)` herdado de `JpaRepository`;
- consulta individual: reutilizar `findById(id)` herdado;
- histórico da Carteira: adicionar `findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(Long carteiraId)`;
- preservar sem alteração `findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc`, usado pelo replay do cadastro.

A consulta derivada por Carteira expressa diretamente filtro e ordenação, sem JPQL customizada, repository adicional ou agregação financeira. O mapper será executado dentro da transação read-only, permitindo resolver os relacionamentos `LAZY` já existentes. `@EntityGraph` ou fetch join ficam fora da primeira implementação enquanto não houver evidência de problema de consultas N+1.

Alternativas consideradas:

- JPQL/fetch join: evita leituras adicionais, mas adiciona uma query especializada antes de existir necessidade comprovada.
- Novo read repository ou query service: aumenta camadas sem alterar o domínio ou o contrato.

### 2. Usar data, ordem no dia e ID como ordenação total

A ordenação será `dataOperacao ASC`, `ordemNoDia ASC`, `id ASC`. As duas primeiras chaves definem a cronologia financeira aprovada. A unicidade de `ordemNoDia` existe por Carteira, Ação e data; portanto, a listagem geral e o histórico completo de uma Carteira podem conter empate entre Ações ou Carteiras independentes. `id` resolve somente esse empate de apresentação e nunca substitui `ordemNoDia` dentro de um mesmo grupo financeiro.

Na listagem geral, `OperacaoService` construirá um `Sort` explícito com as três chaves. Na listagem por Carteira, o método derivado declarará a mesma sequência. Nenhuma consulta altera ou recalcula a ordem persistida.

Alternativas consideradas:

- Somente data e ordem: mantém a ordem financeira, porém permite resposta não determinística entre grupos independentes empatados.
- ID como primeira chave: seria determinístico, mas violaria a cronologia do domínio.

### 3. Acrescentar três métodos read-only ao service existente

`OperacaoService` receberá:

- `List<OperacaoResponse> listar()`;
- `OperacaoResponse buscarPorId(Long id)`;
- `List<OperacaoResponse> listarPorCarteira(Long carteiraId)`.

Todos usarão `@Transactional(readOnly = true)`. `listar` apenas ordenará e mapeará. `buscarPorId` lançará `ObjectNotFoundException` quando necessário. `listarPorCarteira` validará a existência da Carteira exclusivamente com `CarteiraRepository.findById`, sem lock, lançará `ObjectNotFoundException` quando ausente e, quando existente, consultará somente as Operações daquele ID na ordenação aprovada antes de mapeá-las para `OperacaoResponse`. `CarteiraService` não receberá nem duplicará essa regra. Nenhum desses métodos chamará `validateReplay`, `TickerNormalizer`, `Clock`, `save`, `saveAndFlush`, delete ou provider.

Reutilizar `CarteiraService.buscarPorId` foi descartado porque produziria um DTO que não é necessário e criaria acoplamento entre services; `OperacaoService` já possui acesso direto ao repository de Carteira no fluxo atual.

### 4. Reutilizar integralmente OperacaoResponse e OperacaoMapper

O DTO e o mapper atuais já contêm todos os campos aprovados e preservam `corretoraId=null`. As consultas apenas aplicarão `mapper.toResponse` às entidades recuperadas. Não haverá novo DTO de resumo, DTO de histórico ou transformação de valor.

Essa escolha garante que POST e GET exponham a mesma representação persistida e impede que cotação, posição ou resultado financeiro sejam introduzidos por conveniência da consulta.

### 5. Manter as rotas de Operação no resource raiz e a rota aninhada no resource de Carteira

`GET /operacoes` e `GET /operacoes/{id}` ficarão em `OperacaoResource`, que já possui `@RequestMapping("/operacoes")` e delegará a `OperacaoService`.

`GET /carteiras/{carteiraId}/operacoes` ficará em `CarteiraResource`, cuja raiz já é `/carteiras`. O resource receberá `OperacaoService` e delegará diretamente a `listarPorCarteira`, permanecendo sem regra de negócio. Isso evita remover ou ampliar o mapping de classe de `OperacaoResource`, evita criar outro controller e mantém a URI aninhada junto ao recurso pai representado no caminho.

Alternativas consideradas:

- Colocar a rota aninhada em `OperacaoResource`: exigiria alterar o mapping de classe ou explicitar todos os caminhos, aumentando o risco de regressão do POST.
- Criar um resource específico para histórico: duplicaria responsabilidade para apenas uma rota.

### 6. Não introduzir escrita, lock ou integração nas consultas

As leituras não adquirem o lock pessimista usado por cadastro e exclusão, não abrem transação de escrita e não fazem chamada HTTP. Uma Operação concorrente pode aparecer ou não conforme a visibilidade normal da transação/banco no instante da consulta, mas cada item retornado deve refletir um registro persistido completo. Não há requisito de snapshot consistente entre requisições distintas.

### 7. Preservar schema e contratos promovidos

Não haverá changeSet 005 nem modificação do 004, changelog master, entidade, `ddl-auto`, dependência ou configuração. Os testes existentes de POST, replay, concorrência, integridade e DELETE protegido continuarão como regressão obrigatória.

## Risks / Trade-offs

- [Listas sem paginação podem crescer] → cumprir a primeira fatia explicitamente não paginada e registrar paginação como evolução; não antecipar contrato incompatível.
- [Relacionamentos `LAZY` podem causar N+1] → mapear dentro da transação read-only e medir antes de introduzir `@EntityGraph` ou JPQL; a decisão pode ser otimizada futuramente sem mudar o contrato.
- [Empates entre grupos independentes tornam a ordem instável] → aplicar `id ASC` somente como terceira chave técnica.
- [Injeção de `OperacaoService` altera o construtor de `CarteiraResource`] → atualizar apenas os testes e wiring afetados, preservando todos os handlers existentes.
- [Consulta por Carteira pode confundir ausência da Carteira com lista vazia] → validar a Carteira antes de consultar o histórico e cobrir os dois casos separadamente.
- [Mapeamento fora da transação falharia com relações lazy] → concluir toda a projeção para `OperacaoResponse` ainda no método read-only do service.

## Migration Plan

1. Acrescentar somente os métodos de leitura no repository, service e resources existentes.
2. Adicionar testes unitários, HTTP e de repository antes da suíte de regressão.
3. Executar Liquibase/Hibernate sobre o schema vigente para confirmar que nenhuma migration é necessária.
4. Em rollback de aplicação, remover apenas os novos handlers e métodos de leitura; nenhum dado ou schema precisará ser revertido.
