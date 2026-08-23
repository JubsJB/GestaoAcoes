## Context

Veja `proposal.md` para a motivação e `specs/portfolio-deletion/spec.md` para o contrato proposto. O modelo atual de Carteira contém somente `id`, `nome` e `dataCriacao`; não existem entidade, tabela ou relacionamento de Operação. `CarteiraResource`, `CarteiraService` e `CarteiraRepository` já implementam os fluxos de criação, consulta e atualização, e `ObjectNotFoundException` já é convertido pelo `ResourceExceptionHandler` em `404 Not Found` no formato `StandardError`.

O PRD prevê Carteira como agregadora futura de Operações e posições, mas não define exclusão, exclusão lógica, status HTTP ou tratamento de Carteira com histórico. As decisões abaixo foram aprovadas para esta change e constituem a referência técnica para a implementação.

## Goals / Non-Goals

**Goals:**

- Acrescentar exclusão explícita com semântica HTTP previsível e alinhada aos componentes existentes.
- Manter a exclusão atômica, restrita ao registro identificado e testável no H2 com o schema atual.
- Evitar uma escolha atual que permita apagar em cascata o futuro histórico financeiro.
- Preservar os contratos e a arquitetura das capabilities de criação, consulta e atualização.

**Non-Goals:**

- Modelar ou implementar Operação, posição, histórico, snapshot ou cálculo financeiro.
- Introduzir soft delete, auditoria, restauração, retenção ou endpoint de arquivamento.
- Alterar entidade, schema, Liquibase, configurações, dependências ou frontend.
- Resolver concorrência distribuída ou antecipar a estratégia completa do futuro agregado financeiro.

## Decisions

### 1. Usar `DELETE /carteiras/{id}` com `204 No Content`, sem corpo ou `Location`

O resource delegará a exclusão ao service e devolverá `ResponseEntity.noContent().build()`. Não haverá request DTO, corpo de resposta nem `Location`, pois nenhum recurso é criado e o cliente não precisa receber uma representação que deixou de existir.

Alternativas consideradas: `200 OK` com `CarteiraResponse`, que devolve uma representação já removida sem necessidade; e `202 Accepted`, inadequado porque o processamento será síncrono.

### 2. Tratar ID inexistente como `404 Not Found`, inclusive após exclusão anterior

O service localizará a Carteira por `repository.findById(id)` e lançará `ObjectNotFoundException` com a mensagem já usada nos fluxos de consulta e atualização. O handler existente produzirá `StandardError`, sem novo padrão ou novo código de erro.

Uma repetição sequencial após `204` encontrará o recurso ausente e devolverá `404`. A operação permanece idempotente quanto ao estado final — o recurso continua ausente — mas não quanto à igualdade do status entre a primeira e a segunda resposta.

Alternativa considerada: sempre responder `204`, inclusive para ID inexistente. Embora simplifique idempotência observável, esconderia a ausência e diverge do padrão atual de identificação por ID.

### 3. Fazer exclusão física enquanto não existe histórico associado

Não há requisito no PRD para retenção de Carteiras vazias, restauração ou auditoria, e o modelo atual não possui Operações. A menor alteração coerente é remover a linha fisicamente. Não serão adicionados `ativo`, `excluido`, `dataExclusao`, filtros globais ou mudanças em consultas.

Alternativa considerada: exclusão lógica. Ela aumentaria schema, regras de consulta, unicidade e testes sem requisito que justifique essa complexidade.

### 4. Localizar a entidade e usar `repository.delete(carteira)`

O fluxo proposto é: localizar por ID, falhar explicitamente se ausente e excluir a entidade encontrada. `JpaRepository.delete(carteira)` é suficiente dentro da transação; não será criada camada adicional nem será necessário `deleteAndFlush` para compor resposta.

`deleteById(id)` isolado foi descartado porque não expressa de forma tão clara e testável o contrato determinístico de `404` antes da exclusão. Um `existsById` seguido de `deleteById` também faria duas operações e manteria uma janela de corrida sem benefício.

### 5. Executar a exclusão em transação de escrita e sem `Clock`

O método do service será anotado com `@Transactional`, abrangerá a localização e a exclusão e retornará `void`. Ele não consultará `Clock`, não mapeará resposta e não tocará outras Carteiras. Falhas de persistência provocarão rollback pela política transacional vigente.

### 6. Não adicionar controle concorrente nesta primeira fatia

Não serão introduzidos `@Version`, locks pessimistas ou novas abstrações. A operação é local, curta e baseada em uma única linha; o banco e o JPA permanecem responsáveis pela integridade transacional básica. O contrato garante o comportamento sequencial, mas não promete que duas requisições concorrentes de exclusão retornarão combinações específicas de status.

Se relacionamentos futuros trouxerem disputa entre criar Operação e excluir Carteira, a capability que introduzir Operações deverá definir a estratégia de integridade e concorrência junto com a foreign key e a regra de elegibilidade.

### 7. Preservar Operações futuras sem antecipar seu modelo

Quando Operação existir, uma Carteira com qualquer Operação deverá ser inelegível à exclusão física. A implementação futura deverá verificar a associação e retornar erro de negócio, recomendado como `409 Conflict`, sem cascade delete. A foreign key futura deverá impedir remoção acidental do histórico no banco.

Nesta change não haverá interface provisória, método sempre falso, tabela, relacionamento, repository, service, consulta, exception ou proteção dedicada a essa regra, porque qualquer dessas opções inventaria um modelo ainda não especificado. A escolha de `409 Conflict` permanece uma recomendação aprovada para a evolução futura; a exception e o código de erro específicos serão decididos com a capability de Operações.

### 8. Não alterar schema, entidade ou configuração

A exclusão usa a tabela existente e a API padrão do repository. `Carteira`, `003-create-carteira.yaml`, o changelog master, dependências, profiles e propriedades permanecem intocados. Os testes de repository deverão demonstrar que Liquibase cria e Hibernate valida o mesmo schema no H2.

## Risks / Trade-offs

- [Uma Carteira vazia é removida definitivamente] → Exigir endpoint explícito e limitar esta fatia a Carteiras sem histórico; restauração ou auditoria dependem de requisito futuro.
- [A proteção contra Operações não existe no código atual] → Não há Operação para consultar; registrar a restrição normativa e torná-la requisito da capability que introduzir a associação.
- [Duas exclusões concorrentes podem observar resultado dependente do provedor] → Não prometer semântica concorrente nesta fatia e reavaliar junto com Operações/foreign keys.
- [Um erro genérico de integridade poderia receber mensagem inadequada do handler atual] → Não depender desse handler para a regra futura; definir exception/código de negócio somente quando Operações forem modeladas.
- [A criação ainda não aparece como spec principal promovida] → Usar o change arquivado de criação como contrato de regressão e não tentar corrigir essa organização fora do escopo.

## Migration Plan

1. Disponibilizar o endpoint e o método transacional sem migration de banco.
2. Executar os testes de unidade, resource e repository/H2, incluindo regressão de `POST`, `GET` e `PATCH`.
3. Implantar como alteração retrocompatível da API.
4. Em rollback, remover somente o endpoint e o método de exclusão; nenhuma reversão de schema será necessária. Registros já excluídos fisicamente não poderão ser restaurados por rollback de código.
