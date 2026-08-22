## Context

Veja `proposal.md` para a motivação e `specs/portfolio-update/spec.md` para o comportamento proposto. O PRD define `Carteira(id, nome, dataCriacao)`, mas não especifica uma rota de atualização. A implementação atual possui `CarteiraResource`, `CarteiraService`, `CarteiraRepository`, `CarteiraMapper`, `CarteiraResponse` e os endpoints de criação e consulta. O service já centraliza a política de nome em `normalizeAndValidateName`, enquanto a entidade expõe somente construtor e getters.

As specs já aprovadas exigem que o nome receba apenas `trim`, preserve espaços internos, acentos e caixa, tenha no máximo 255 caracteres após a normalização, aceite duplicidade e use `id` como identidade estável. `dataCriacao` é gerada em UTC pela aplicação e deve permanecer imutável nesta operação.

## Goals / Non-Goals

**Goals:**

- Acrescentar uma atualização transacional e atômica restrita ao nome.
- Reutilizar os componentes, validações, DTO de resposta e tratamento de erros atuais.
- Impedir que o contrato HTTP ou o domínio permita alterar `id` ou `dataCriacao`.
- Preservar integralmente os comportamentos existentes de `POST` e `GET`.

**Non-Goals:**

- Atualizar `dataCriacao`, criar novos campos ou modelar relacionamentos.
- Implementar exclusão, operações, posições, cálculos, histórico, snapshot ou frontend.
- Alterar schema, Liquibase, dependências, configurações ou estratégia de concorrência.
- Criar nova camada de persistência ou abstração genérica para uma operação local simples.

## Decisions

### 1. Usar `PATCH /carteiras/{id}` com `200 OK`

O contrato definido é:

```http
PATCH /carteiras/{id}
Content-Type: application/json

{
  "nome": "Novo nome da carteira"
}
```

Em sucesso, a resposta será `200 OK` com `CarteiraResponse(id, nome, dataCriacao)` completo e sem `Location`. `PATCH` representa melhor a alteração parcial de um único campo; `PUT` sugeriria substituição integral do recurso, e `204 No Content` impediria confirmar o estado efetivamente persistido.

### 2. Usar um DTO específico `CarteiraUpdateRequest`

O request de atualização conterá somente `nome`, com `@NotBlank`, `@Size(max = 255)` e rejeição explícita de propriedades desconhecidas. Seu setter aplicará apenas `trim`, como o request de criação, garantindo que o limite seja avaliado após a normalização. O service continuará aplicando `normalizeAndValidateName`, preservando uma única regra autoritativa antes da persistência.

Reutilizar `CarteiraCreateRequest` economizaria uma classe, mas manteria semântica e mensagens ligadas a cadastro, além de acoplar dois contratos que podem evoluir separadamente. Um novo DTO é a menor separação necessária; não será criado novo mapper ou DTO de resposta. O contrato específico impede que `id`, `dataCriacao` ou propriedades desconhecidas sejam aceitos no PATCH.

### 3. Adicionar um método de domínio restrito para o nome

A entidade receberá somente `atualizarNome(String nome)`, responsável por substituir o campo `nome`. Não será adicionado setter genérico nem método capaz de alterar `id` ou `dataCriacao`. A validação e normalização permanecem no service; o método de domínio limita a superfície de mutação da entidade.

Alternativas consideradas: reflexão ou acesso direto ao campo, incompatíveis com o encapsulamento; setter público genérico, que amplia desnecessariamente a mutabilidade; e reconstruir a entidade, que arriscaria perder identidade ou data de criação.

### 4. Executar a atualização no `CarteiraService` em uma transação de escrita

O método `atualizar(Long id, CarteiraUpdateRequest request)` será anotado com `@Transactional`, seguirá esta sequência e não usará `Clock`:

1. localizar a Carteira com `repository.findById(id)`;
2. lançar `ObjectNotFoundException` se ausente;
3. normalizar e validar o nome com a mesma função usada no cadastro;
4. chamar o método restrito da entidade;
5. concluir a persistência com `saveAndFlush` e mapear o estado salvo com `CarteiraMapper.toResponse`.

Não será criado `CarteiraPersistenceService`: não existem chamadas externas, locks prolongados ou outra coordenação que exija separar a persistência. A transação abrangerá somente leitura, alteração local e gravação.

Alternativa considerada: depender apenas do dirty checking. Embora suficiente tecnicamente, `saveAndFlush` deixa explícita a conclusão da persistência antes de construir a resposta e segue o padrão atual do cadastro.

### 5. Tratar o mesmo nome pelo fluxo normal

Quando o valor normalizado for igual ao persistido, o fluxo retornará `200 OK` com o mesmo estado. Não haverá erro, código específico ou atualização de `dataCriacao`. O método de domínio poderá receber o mesmo valor; o provedor JPA decidirá que não existe alteração suja, sem necessidade de ramificação especial. Esse comportamento constitui sucesso idempotente.

Alternativas consideradas: `409 Conflict`, `304 Not Modified` ou `204 No Content`. Foram rejeitadas por introduzirem semântica especial sem benefício e porque `304` pertence a requisições condicionais de cache.

### 6. Reutilizar tratamento centralizado de erros

ID inexistente continuará usando `ObjectNotFoundException`, convertida por `ResourceExceptionHandler` em `404 Not Found`. Nome inválido, JSON inválido ou propriedade não permitida continuarão no contrato `400/REQUEST_INVALIDO`, com `details.nome` quando aplicável. Não será criado novo handler ou código de erro.

### 7. Não alterar schema nem introduzir controle concorrente

A tabela já possui `nome VARCHAR(255) NOT NULL`; por isso não haverá changeSet, alteração no changelog master ou mudança de `ddl-auto=validate`. Também não será introduzido `@Version`, lock pessimista ou outra coluna. Atualizações concorrentes continuarão sob a semântica transacional padrão do banco, e um requisito futuro de detecção de conflito deverá ser especificado separadamente.

### 8. Cobrir a mudança em três níveis

- Testes unitários do service verificarão validação, normalização, duplicidade permitida, ausência, mesmo nome, preservação de `id`/`dataCriacao`, ausência de `Clock` e persistência atômica.
- Testes do repository com H2 verificarão a atualização de `nome`, preservação dos demais campos e aceitação de nomes duplicados sobre o schema Liquibase existente.
- Testes HTTP verificarão `PATCH /carteiras/{id}`, `200`, DTO completo, ausência de `Location`, `400`, `404`, rejeição de campos não permitidos e regressão dos endpoints existentes.

## Risks / Trade-offs

- [Duplicar anotações simples entre requests de criação e atualização] → Manter DTOs semanticamente independentes e reutilizar a regra autoritativa do service, sem criar abstração prematura.
- [Um método de domínio aceita um valor já validado externamente] → Restringir o método a um único campo e protegê-lo por testes de preservação de `id` e `dataCriacao`.
- [Atualizações concorrentes podem resultar em última gravação prevalecendo] → Manter o escopo sem nova coluna/lock e especificar controle de concorrência em change própria se surgir requisito.
- [Regressão acidental em criação ou consulta] → Reexecutar todos os testes atuais de Carteira e revisar o diff para garantir que somente o novo PATCH e seus suportes foram introduzidos.

## Migration Plan

1. Adicionar o DTO específico de atualização e o método restrito de domínio.
2. Implementar o método transacional no service e a rota no resource existentes.
3. Executar testes direcionados, suíte completa, `clean verify`, validações OpenSpec e atualização do Graphify.

Não há migração ou rollback de banco. O rollback da implementação consiste em remover a rota, o método de service, o request específico, o método restrito da entidade e seus testes, preservando os endpoints existentes.
