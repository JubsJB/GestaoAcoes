## Why

O profile `dev` usa atualmente a chave incorreta `spring.datasource.pass word`, de modo que o Spring Boot não reconhece `SPRING_DATASOURCE_PASSWORD` como a senha do datasource. A correção deve restaurar exclusivamente o nome nativo `spring.datasource.password` sem modificar qualquer outra configuração ou comportamento da aplicação.

## What Changes

- Substituir somente `spring.datasource.pass word` por `spring.datasource.password` em `application-dev.properties`.
- Preservar integralmente o placeholder `${SPRING_DATASOURCE_PASSWORD}`.
- Preservar URL, username, ativação do profile `dev`, `spring.jpa.hibernate.ddl-auto=validate`, logging e todas as demais propriedades existentes.
- Executar inspeção focada, testes existentes e build pelo Maven Wrapper em extensão suficiente para detectar regressões.
- Não alterar código Java, testes, dependências, Liquibase, schema, integrações ou funcionalidades de negócio.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `development-datasource-configuration`: explicitar que o profile `dev` deve vincular a senha externa pela chave exata `spring.datasource.password`, preservando o placeholder e as demais configurações.

## Impact

- Configuração afetada: somente a propriedade de senha em `src/main/resources/application-dev.properties`.
- Validação: inspeção exata do diff, testes existentes e build pelo Maven Wrapper.
- APIs, entidades, regras de negócio, Carteira, Ação, Corretora, Liquibase, schema e dependências não são afetados.
