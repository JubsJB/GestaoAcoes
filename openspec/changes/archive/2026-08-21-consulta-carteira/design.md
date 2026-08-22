## Context

Veja `proposal.md` para a motivação e `specs/portfolio-query/spec.md` para o contrato. A implementação atual já possui `CarteiraResource`, `CarteiraService`, `CarteiraRepository`, `CarteiraMapper`, `CarteiraResponse` e a entidade mínima `Carteira(id, nome, dataCriacao)`, mas expõe somente `POST /carteiras`.

O PRD lista `GET /carteiras` e `GET /carteiras/{id}`. As consultas existentes de Corretora e Ação estabelecem o padrão técnico aplicável: métodos no resource existente, métodos `listar()` e `buscarPorId()` no service com `@Transactional(readOnly = true)`, `findAll(Sort)` e `findById()` do Spring Data, mapeamento para o DTO existente e `ObjectNotFoundException` tratado por `ResourceExceptionHandler`.

A spec de criação de Carteira está preservada na change arquivada `2026-08-21-criacao-carteira`, mas não existe atualmente uma spec principal `portfolio-creation` em `openspec/specs`. Para não sincronizar retroativamente outra change nem misturar criação e consulta, este planejamento introduz a capability independente `portfolio-query` e trata o contrato arquivado de criação como restrição de compatibilidade.

## Goals / Non-Goals

**Goals:**

- Acrescentar leitura de coleção e recurso individual à estrutura atual de Carteira.
- Garantir ordenação explícita, transações somente de leitura e mapeamento fiel dos dados persistidos.
- Reutilizar o padrão já comprovado pelas consultas de Corretora e Ação.
- Proteger o comportamento existente de criação com testes de regressão.

**Non-Goals:**

- Introduzir componentes paralelos, consultas customizadas, paginação, filtros ou consulta por nome.
- Modificar entidade, DTO de resposta, schema, Liquibase, dependências ou configurações.
- Modelar relações com Ação, Corretora, Operação, posição, histórico, snapshot ou usuário.
- Implementar atualização, exclusão, cálculos financeiros ou frontend.
- Promover ou alterar retroativamente os artefatos arquivados de `criacao-carteira`.

## Decisions

### 1. Reutilizar integralmente os componentes existentes de Carteira

`CarteiraResource` receberá os dois métodos GET, `CarteiraService` coordenará as leituras, `CarteiraRepository` fornecerá as operações Spring Data e `CarteiraMapper` continuará convertendo a entidade para `CarteiraResponse`. Não será criado outro resource, service, mapper, DTO ou persistence service.

Alternativa considerada: criar um service exclusivo de consulta. Foi rejeitada porque a leitura não introduz integração, transação de escrita ou regra que justifique nova abstração, e os domínios de Corretora e Ação mantêm cadastro e consulta no mesmo service de aplicação.

### 2. Fixar `id ASC` como ordenação da coleção

`CarteiraService.listar()` usará `repository.findAll(Sort.by(Sort.Direction.ASC, "id"))`, mapeará cada entidade e retornará a lista resultante. Essa ordenação é explícita, estável para os identificadores existentes e consistente com `GET /corretoras` e `GET /acoes`.

Alternativas consideradas: depender da ordem natural do banco, ordenar por nome ou criar query customizada. A ordem implícita não é determinística; nomes podem se repetir; e o Spring Data já expressa a ordenação necessária sem código adicional.

### 3. Executar as duas consultas como transações somente de leitura

`listar()` e `buscarPorId()` serão anotados individualmente com `@Transactional(readOnly = true)`. O método existente `cadastrar()` manterá sua transação de escrita e seu comportamento atual.

Alternativas consideradas: anotar a classe inteira como read-only, o que exigiria sobrescrever a semântica do cadastro, e omitir transação, divergindo do padrão atual de consulta. A anotação por método deixa a intenção explícita sem afetar o POST.

### 4. Usar `findById` e o tratamento centralizado para ausência

`buscarPorId(Long id)` usará `repository.findById(id)` e lançará `ObjectNotFoundException` com mensagem específica de Carteira quando o resultado estiver ausente. O `ResourceExceptionHandler` existente converterá essa exceção em `404 Not Found` no `StandardError` atual. Nenhum novo código de erro ou handler será criado.

Alternativas consideradas: retornar `null`, `Optional` no contrato HTTP ou criar `CARTEIRA_NAO_ENCONTRADA`. Foram rejeitadas porque romperiam o padrão já adotado por Corretora e Ação ou inventariam um contrato não exigido.

### 5. Mapear sem transformar os dados persistidos

As duas consultas reutilizarão `CarteiraMapper.toResponse`, que copia diretamente `id`, `nome` e `dataCriacao`. O fluxo não usará `Clock`, não repetirá a normalização de nome do cadastro e não recalculará a data. A entidade não será modificada nem salva durante os GETs.

Alternativa considerada: reutilizar validações ou normalização do cadastro na leitura. Foi rejeitada porque a consulta deve representar o estado persistido, e uma transformação posterior poderia ocultar dados ou produzir resposta diferente do banco.

### 6. Manter repository e persistência sem extensões

`CarteiraRepository` continuará estendendo `JpaRepository<Carteira, Long>` sem novos métodos. `findAll(Sort)` e `findById()` atendem completamente ao contrato. Não haverá alteração de tabela, changelog, constraint, índice ou relacionamento.

Alternativa considerada: adicionar métodos derivados ou JPQL para fixar a ordem. Foi rejeitada porque duplicaria funcionalidade nativa e ampliaria a superfície de persistência sem benefício.

### 7. Validar a funcionalidade em três níveis

- Testes unitários de `CarteiraService` verificarão listagem, ordenação solicitada ao repository, lista vazia, busca existente, exceção de ausência, mapeamento fiel e inexistência de gravação.
- Testes de `CarteiraRepository` com H2 verificarão `findAll(Sort)` por `id ASC`, `findById()` existente e ausente e preservação de `nome` e `dataCriacao`, usando o changelog real já existente.
- Testes HTTP em `CarteiraResourceTest` verificarão os dois endpoints, `200`, `[]`, ordem, DTO completo e `404` padronizado, além de manter e reexecutar todos os cenários atuais do POST.

Não haverá mock ou chamada de provider externo, pois Carteira não depende de integração e as consultas usarão somente o repository.

## Risks / Trade-offs

- [A listagem sem paginação pode crescer em volume] → Manter o contrato pequeno exigido nesta change e introduzir paginação somente quando houver requisito próprio, sem alterar silenciosamente o formato atual.
- [A ordem natural do banco pode variar] → Passar `Sort` explícito por `id ASC` e cobrir a ordem nos testes de service, repository e HTTP.
- [Uma transformação acidental pode alterar nome ou data na resposta] → Reutilizar o mapper direto e testar valores persistidos conhecidos sem `Clock` ou normalização no fluxo de leitura.
- [Alterações preexistentes no worktree podem contaminar a implementação] → Restringir o futuro apply aos componentes e testes de Carteira indicados nas tarefas e revisar o diff focado.
- [A capability de criação não está promovida nas specs principais] → Não corrigir essa lacuna dentro desta change; manter `portfolio-query` independente e registrar a inconsistência para tratamento OpenSpec separado, se desejado.

## Migration Plan

1. Acrescentar os métodos de leitura ao service e ao resource existentes sem modificar o fluxo de criação.
2. Ajustar somente os testes de Carteira necessários para cobrir service, repository e HTTP.
3. Executar testes direcionados e a suíte completa pelo Maven Wrapper, validando Liquibase/Hibernate no H2 sem alterar o schema.
4. Validar a change e o conjunto global do OpenSpec, atualizar o Graphify após as alterações de código e revisar o diff final.

Não há migração de banco ou dados. O rollback da implementação consiste em remover somente os métodos GET e seus testes novos; registros persistidos e `POST /carteiras` permanecem inalterados.
