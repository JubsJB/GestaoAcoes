## 1. Revisão do estado atual

- [x] 1.1 Confirmar em `application-dev.properties` a ocorrência exata de `spring.datasource.pass word=${SPRING_DATASOURCE_PASSWORD}` e que o defeito está restrito ao nome da propriedade.
- [x] 1.2 Registrar URL, username, placeholder da senha, `spring.jpa.hibernate.ddl-auto=validate`, logging e opções JPA para verificar sua preservação após a correção.

## 2. Correção isolada da configuração

- [x] 2.1 Substituir somente `spring.datasource.pass word` por `spring.datasource.password`, mantendo `${SPRING_DATASOURCE_PASSWORD}` inalterado.
- [x] 2.2 Confirmar por inspeção que a chave incorreta com espaço não permanece no arquivo e que a chave nativa correta aparece uma única vez.
- [x] 2.3 Revisar o diff focado de `application-dev.properties` e confirmar que nenhuma outra linha, espaçamento, ordem ou valor foi alterado.

## 3. Verificação e encerramento

- [x] 3.1 Executar `GestaoacoesApplicationTests` pelo Maven Wrapper para confirmar a baseline de contexto, Liquibase e Hibernate no profile `test`.
- [x] 3.2 Executar `./mvnw.cmd clean verify` e confirmar que compilação, testes e empacotamento terminam sem regressões.
- [x] 3.3 Validar `corrigir-password-datasource-dev` e o conjunto global do OpenSpec em modo strict.
- [x] 3.4 Executar `git diff --check`, revisar `git diff` e `git status`, confirmando que a implementação alterou somente a propriedade alvo e os artefatos desta change, sem modificar código Java, testes, dependências, Liquibase, schema ou funcionalidades de negócio.
