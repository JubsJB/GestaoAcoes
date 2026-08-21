## Purpose

Garantir que o profile `dev` resolva uma URL JDBC PostgreSQL válida a partir da configuração externa prevista, preservando integralmente as demais propriedades de desenvolvimento.

## ADDED Requirements

### Requirement: Resolução íntegra da URL do datasource no profile dev
Quando o profile `dev` estiver ativo, o sistema SHALL definir `spring.datasource.url` diretamente pelo placeholder `SPRING_DATASOURCE_URL`, sem acrescentar prefixos ou sufixos ao valor fornecido, e SHALL preservar o fallback PostgreSQL local já existente quando a variável não for informada.

#### Scenario: URL externa fornecida ao profile dev
- **WHEN** o profile `dev` for ativado com `SPRING_DATASOURCE_URL` contendo uma URL JDBC PostgreSQL válida
- **THEN** `spring.datasource.url` será exatamente o valor fornecido, sem o prefixo literal `gi` ou qualquer outra transformação

#### Scenario: URL externa ausente no profile dev
- **WHEN** o profile `dev` for ativado sem `SPRING_DATASOURCE_URL`
- **THEN** `spring.datasource.url` será resolvida para o fallback existente `jdbc:postgresql://localhost:5432/gestaoacoesdb`

### Requirement: Preservação das demais configurações de desenvolvimento
A correção da URL SHALL NOT modificar a origem ou o valor configurado de username, password, política de validação do schema, logging ou qualquer outra propriedade existente no profile `dev`.

#### Scenario: Aplicação da correção isolada
- **WHEN** a configuração corrigida do profile `dev` for comparada com a configuração anterior
- **THEN** somente a remoção do prefixo `gi` em `spring.datasource.url` será observada, permanecendo `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `spring.jpa.hibernate.ddl-auto=validate` e as demais propriedades inalteradas

### Requirement: Verificação de regressão da configuração
O projeto SHALL continuar compilando e sua suíte automatizada SHALL continuar passando após a correção isolada da URL do datasource.

#### Scenario: Verificação pelo Maven Wrapper
- **WHEN** a verificação do projeto for executada pelo Maven Wrapper após a correção
- **THEN** o build e os testes existentes serão concluídos sem regressões atribuíveis à alteração da configuração `dev`
