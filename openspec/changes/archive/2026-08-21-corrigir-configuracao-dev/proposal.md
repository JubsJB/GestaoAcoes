## Why

O profile `dev` concatena atualmente o prefixo literal indevido `gi` ao valor de `SPRING_DATASOURCE_URL`, produzindo uma URL JDBC inválida mesmo quando a variável externa está correta. A correção é necessária para restabelecer a configuração de desenvolvimento definida pela baseline sem alterar qualquer outro comportamento da aplicação.

## What Changes

- Remover exclusivamente o prefixo `gi` que antecede o placeholder `SPRING_DATASOURCE_URL` em `application-dev.properties`.
- Manter `spring.datasource.url` vinculada diretamente a `SPRING_DATASOURCE_URL`, preservando o fallback PostgreSQL local já existente.
- Preservar sem alterações username, password, `spring.jpa.hibernate.ddl-auto=validate`, logging e todas as demais propriedades do profile `dev`.
- Executar uma verificação focada da configuração e o build/teste pelo Maven Wrapper em extensão suficiente para detectar regressões.

## Capabilities

### New Capabilities

- `development-datasource-configuration`: define a resolução íntegra da URL do datasource PostgreSQL pelo profile `dev` a partir de `SPRING_DATASOURCE_URL`, sem prefixos estranhos e sem modificar as demais propriedades desse profile.

### Modified Capabilities

Nenhuma.

## Impact

- Configuração afetada: somente `src/main/resources/application-dev.properties`, na propriedade `spring.datasource.url`.
- Validação: configuração focada e suíte/build existentes executados pelo Maven Wrapper.
- APIs, entidades, schema, Liquibase, dependências, integrações externas e funcionalidades de negócio não são alterados.
