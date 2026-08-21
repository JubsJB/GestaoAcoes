## MODIFIED Requirements

### Requirement: Preservação das demais configurações de desenvolvimento
O profile `dev` SHALL configurar a senha do datasource pela chave exata `spring.datasource.password` e SHALL manter `${SPRING_DATASOURCE_PASSWORD}` como seu valor externo, sem espaços ou alterações no nome da propriedade. A correção dessa chave SHALL NOT modificar a origem ou o valor configurado de URL, username, política de validação do schema, logging ou qualquer outra propriedade existente no profile `dev`.

#### Scenario: Aplicação da correção isolada
- **WHEN** a configuração corrigida do profile `dev` for comparada com o estado que contém `spring.datasource.pass word=${SPRING_DATASOURCE_PASSWORD}`
- **THEN** a única alteração será `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}`, permanecendo URL, username, `spring.jpa.hibernate.ddl-auto=validate`, logging e as demais propriedades inalteradas

#### Scenario: Senha externa fornecida ao profile dev
- **WHEN** o profile `dev` for ativado com `SPRING_DATASOURCE_PASSWORD` configurada
- **THEN** o Spring Boot reconhecerá o valor pela propriedade nativa `spring.datasource.password`, sem transformar o placeholder nem usar a chave incorreta com espaço
