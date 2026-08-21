## Context

Veja `proposal.md` para a motivação. O projeto já utiliza uma arquitetura em camadas sob `com.projeto`, DTOs separados das entidades, tratamento centralizado por `ApiException`/`StandardError`, um `Clock.systemUTC()` compartilhado e Liquibase seguido por Hibernate com `ddl-auto=validate`. O changelog master contém `001-create-corretora.yaml` e `002-create-acao.yaml`; não existe ainda entidade ou tabela de Carteira.

O PRD define somente os atributos `id`, `nome` e `dataCriacao`, estabelece `POST /carteiras` e dispensa vínculo com usuário autenticado no MVP. As regras antes ausentes do PRD foram aprovadas para esta change: nome obrigatório com normalização mínima e limite de 255 caracteres, duplicidade permitida e resposta de criação com `201 Created`, DTO completo e `Location`.

## Goals / Non-Goals

**Goals:**

- Introduzir o menor agregado persistente de Carteira compatível com o PRD.
- Reutilizar os padrões atuais de API, tempo, persistência, migração e testes.
- Manter o modelo preparado para associações futuras sem antecipá-las.
- Aplicar de forma consistente as decisões funcionais aprovadas para nome e resposta HTTP.

**Non-Goals:**

- Modelar operações, posições, ações da carteira, corretora da operação, cálculos, snapshots ou histórico.
- Implementar consultas, atualização ou exclusão de Carteira.
- Introduzir usuário, autenticação, ownership ou múltiplas carteiras por usuário.
- Adicionar dependências, integrações externas ou abstrações genéricas de investimentos.

## Decisions

### 1. Repetir a estrutura em camadas existente sem uma camada adicional de persistência

A implementação usará `CarteiraResource`, `CarteiraCreateRequest`, `CarteiraResponse`, `CarteiraService`, `CarteiraRepository`, `CarteiraMapper` e a entidade `Carteira` nos pacotes já existentes. O resource tratará o contrato HTTP, o service coordenará validação, geração temporal e transação, o mapper separará entidade e resposta, e o repository permanecerá restrito ao Spring Data JPA.

Como não há chamada externa a manter fora da transação nem regra concorrente específica aprovada, `CarteiraService` poderá executar diretamente uma transação curta de criação. Não será criado `CarteiraPersistenceService` nesta fatia.

Alternativa considerada: repetir `CorretoraPersistenceService` e `AcaoPersistenceService`. Foi rejeitada porque esses componentes isolam persistência de fluxos com integrações externas e condições de corrida específicas, necessidades ausentes na criação simples de Carteira.

### 2. Restringir o request ao nome

O contrato será:

```json
{
  "nome": "Carteira principal"
}
```

`CarteiraCreateRequest` não aceitará `id`, `dataCriacao` ou campos futuros. Propriedades desconhecidas serão rejeitadas pelo mesmo mecanismo já usado nos requests existentes, resultando em `400/REQUEST_INVALIDO`.

Alternativa considerada: aceitar `dataCriacao` ou objetos vazios. Foi rejeitada porque a data pertence à aplicação e o nome é o único atributo descritivo previsto pelo PRD.

### 3. Aplicar a política aprovada para o nome

A política definida é:

- `nome` obrigatório;
- remover somente espaços nas extremidades antes da persistência;
- rejeitar nulo, vazio ou somente espaços;
- preservar espaços internos, acentos e caixa;
- limitar o valor normalizado a 255 caracteres, coerente com os tamanhos já usados para nomes textuais no schema;
- permitir nomes duplicados, pois `id` é a identidade estável.

Bean Validation cobrirá obrigatoriedade e limite do request, e o service persistirá o valor após `trim()`. Não haverá transformação para maiúsculas/minúsculas nem colapso de espaços internos. Não haverá método de consulta de duplicidade nem constraint `UNIQUE` em `nome`.

Alternativas consideradas: impor unicidade global, que dificultaria nomes naturais e uma futura associação por usuário sem suporte no PRD; e não definir limite, que deixaria o contrato incompatível com uma coluna portável. Ambas foram rejeitadas pelas decisões aprovadas.

### 4. Gerar `dataCriacao` com o relógio UTC já existente

`CarteiraService` usará o bean `Clock` atual e gerará `OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)` imediatamente antes da persistência. A entidade e o DTO de resposta usarão `OffsetDateTime`; o banco usará `TIMESTAMP WITH TIME ZONE`; a representação HTTP será ISO-8601 em UTC.

Alternativas consideradas: `LocalDate` ou `LocalDateTime`. Foram rejeitadas porque o projeto já adotou um instante com offset para datas de criação e cotação, e tipos sem offset tornariam o instante ambíguo entre ambientes.

### 5. Aplicar o padrão REST de criação já adotado

O resource responderá `201 Created`, retornará `CarteiraResponse(id, nome, dataCriacao)` e construirá `Location: /carteiras/{id}` com o identificador gerado. A URI define a identidade futura do recurso, embora `GET /carteiras/{id}` permaneça fora desta change.

Alternativas consideradas: `200 OK`, `204 No Content` e resposta sem `Location`. Foram preteridas porque o endpoint cria um novo recurso e os fluxos existentes de Corretora e Ação já retornam o estado persistido com a URI estável.

### 6. Persistir um único agregado sem relacionamentos antecipados

O modelo final é:

| Campo Java | Tipo Java | Coluna | Tipo de banco | Restrição |
|---|---|---|---|---|
| `id` | `Long` | `id` | `BIGINT` | chave primária, identidade, não nulo |
| `nome` | `String` | `nome` | `VARCHAR(255)` | não nulo; sem unicidade |
| `dataCriacao` | `OffsetDateTime` | `data_criacao` | `TIMESTAMP WITH TIME ZONE` | não nulo, UTC |

A entidade não conterá coleções de operações, posições ou ações, nem referência a usuário. Essas associações serão introduzidas somente pelas changes que definirem suas invariantes e ciclo de vida.

Alternativa considerada: adicionar agora `@OneToMany` vazio para operações. Foi rejeitada porque antecipa ownership, cascatas e carregamento sem requisito nesta fatia.

### 7. Evoluir Liquibase com `003-create-carteira.yaml`

O changelog master incluirá, após os dois arquivos imutáveis existentes:

```text
src/main/resources/db/changelog/
├── db.changelog-master.yaml
└── changes/
    ├── 001-create-corretora.yaml
    ├── 002-create-acao.yaml
    └── 003-create-carteira.yaml
```

O novo changeSet criará somente `carteira`, sua chave primária e as constraints `NOT NULL` definidas. Não haverá chave estrangeira nem índice ou constraint única para nome. PostgreSQL e H2 executarão o mesmo changelog; Hibernate continuará somente validando. Os changeSets 001 e 002 não serão alterados.

O rollback explícito removerá somente `carteira` e deverá ser tratado como operação destrutiva fora de bancos descartáveis.

### 8. Reutilizar o tratamento de erros sem criar códigos desnecessários

Erros de Bean Validation, JSON inválido e propriedades não admitidas continuarão usando `400 Bad Request`, `REQUEST_INVALIDO`, `StandardError` e `details` por campo quando disponíveis. Como nomes duplicados são permitidos, não será criado `CARTEIRA_DUPLICADA` nem tratamento paralelo.

Falhas inesperadas de integridade não deverão ser remapeadas para `CORRETORA_DUPLICADA`; se a implementação revelar que o handler genérico atual produz essa classificação incorreta fora de Corretora, o ajuste deverá ser mínimo e preservar os contratos existentes, sem inventar um erro de negócio para Carteira.

### 9. Cobrir a fatia sem rede e com o changelog real

Os testes unitários do service usarão `Clock` fixo e repository substituto para verificar nome normalizado, data UTC e mapeamento. O teste de resource cobrirá request, rejeições, `201`, DTO e `Location`. O teste de repository com profile `test` executará Liquibase no H2, validará o mapeamento Hibernate e confirmará a persistência dos três campos e a permissão de nomes duplicados.

A suíte existente será preservada e executada pelo Maven Wrapper. Não haverá chamadas externas nem necessidade de chaves. A compatibilidade PostgreSQL será sustentada pelo mesmo changeSet portável e poderá receber validação adicional no ambiente `dev` quando um banco configurado estiver disponível, sem inventar credenciais.

## Risks / Trade-offs

- [Permitir nomes duplicados pode gerar carteiras visualmente indistinguíveis] → Usar `id` como identidade estável e deixar qualquer unicidade futura depender do contexto de usuário/ownership que ainda não existe.
- [Proibir duplicidade global agora pode dificultar a evolução para múltiplos usuários] → Não criar constraint sem requisito explícito; revisar quando o modelo de usuário entrar no escopo.
- [O rollback da tabela destrói dados] → Executá-lo somente como ação operacional explicitamente autorizada ou em banco descartável.
- [O handler genérico atual associa toda `DataIntegrityViolationException` a Corretora] → Evitar depender desse fallback; validar a criação de Carteira antes da gravação e, se necessário, corrigir apenas a classificação genérica sem alterar contratos de negócio existentes.

## Migration Plan

1. Criar os componentes de Carteira na estrutura atual e manter a transação restrita à criação local.
2. Adicionar `003-create-carteira.yaml` e incluí-lo ao final do changelog master, sem editar changeSets anteriores.
3. Validar Liquibase e Hibernate no H2, executar os testes direcionados e a suíte completa pelo Maven Wrapper.
4. Quando houver PostgreSQL de desenvolvimento configurado, aplicar o mesmo changelog e confirmar a validação do Hibernate antes da disponibilização do endpoint nesse ambiente.

Rollback: reverter os componentes da API e, somente com autorização operacional ou em ambiente descartável, executar o rollback de `003-create-carteira`. Nenhum changeSet anterior deverá ser revertido ou modificado.
