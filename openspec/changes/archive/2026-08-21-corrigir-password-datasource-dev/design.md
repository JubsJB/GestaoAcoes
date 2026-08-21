## Context

Veja `proposal.md` para a motivação. O arquivo `src/main/resources/application-dev.properties` contém `spring.datasource.pass word=${SPRING_DATASOURCE_PASSWORD}`; o espaço interrompe o nome nativo reconhecido pelo Spring Boot. A spec principal `development-datasource-configuration` já determina que URL, username, password, `ddl-auto=validate`, logging e demais propriedades do profile sejam preservadas.

A alteração é declarativa, restrita a uma linha e não exige mudança arquitetural, dependência, schema ou teste de negócio. O Graphify relaciona o profile `dev` à baseline de configuração e aos componentes descobertos pelo Spring, sem indicar necessidade de alteração nesses componentes.

## Goals / Non-Goals

**Goals:**

- Restaurar a chave nativa de senha do datasource sem alterar o placeholder externo.
- Tornar a revisão do escopo determinística por uma comparação de uma única linha.
- Confirmar que a correção não introduz regressão de compilação, testes ou empacotamento.

**Non-Goals:**

- Alterar valores ou nomes de variáveis externas.
- Iniciar PostgreSQL, modificar credenciais ou alterar a seleção de profiles.
- Modificar código Java, testes, Liquibase, schema, dependências, integrações ou funcionalidades de negócio.

## Decisions

### 1. Substituir exclusivamente o nome incorreto da propriedade

A implementação substituirá:

```properties
spring.datasource.pass word=${SPRING_DATASOURCE_PASSWORD}
```

por:

```properties
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

O placeholder permanecerá byte a byte igual. URL, username, `ddl-auto`, logging e opções JPA não serão reformatados ou reordenados.

Alternativas consideradas: renomear a variável externa, fornecer senha default ou mover a configuração para outro arquivo. Foram rejeitadas porque ampliariam o escopo e poderiam expor ou alterar a estratégia de credenciais.

### 2. Validar a correção declarativa por inspeção exata

A verificação focada confirmará a presença da linha correta, a ausência de `spring.datasource.pass word` e um diff limitado à substituição aprovada. Não será criado teste específico que duplique o conteúdo do arquivo em código.

Alternativa considerada: adicionar um teste automatizado exclusivo para a string da propriedade. Foi rejeitada porque criaria manutenção paralela para uma correção de uma linha; a inspeção determinística valida diretamente o defeito.

### 3. Usar a suíte existente para regressão geral

O teste de contexto existente e `clean verify` serão executados pelo Maven Wrapper. A suíte usa o profile `test` e H2, portanto comprova ausência de regressão geral, enquanto a inspeção focada comprova a chave do profile `dev`.

Alternativa considerada: tornar PostgreSQL uma dependência obrigatória da validação. Foi rejeitada porque a correção do nome da chave independe da disponibilidade externa do banco ou de credenciais locais.

## Risks / Trade-offs

- [A suíte com profile `test` não carrega `application-dev.properties`] → Verificar explicitamente a linha e o diff focado antes do build.
- [O worktree contém alterações anteriores não relacionadas] → Restringir a implementação e a revisão ao arquivo alvo e aos artefatos desta change.
- [Uma validação real com PostgreSQL depende de ambiente e credencial externos] → Não torná-la critério obrigatório nem inventar valores para contornar indisponibilidade.

## Migration Plan

1. Alterar somente a chave da propriedade de senha em `application-dev.properties`.
2. Confirmar por inspeção e diff focado que nenhuma outra linha mudou.
3. Executar o teste de contexto existente e `clean verify` pelo Maven Wrapper.
4. Validar a change em modo strict e revisar `git diff --check`, `git diff` e `git status`.

Rollback: restaurar a linha anterior apenas se explicitamente solicitado, reconhecendo que isso reintroduziria a chave inválida. Não existe migração de banco ou dado.
