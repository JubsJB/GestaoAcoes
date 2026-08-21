## 1. Revisão do estado atual

- [x] 1.1 Confirmar em `application-dev.properties` que o defeito está restrito ao prefixo literal `gi` antes de `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/gestaoacoesdb}`.
- [x] 1.2 Registrar o conteúdo das demais propriedades do profile `dev` para verificar que username, password, `ddl-auto=validate`, logging e opções JPA sejam preservados.

## 2. Correção isolada da configuração

- [x] 2.1 Remover somente o prefixo `gi` da propriedade `spring.datasource.url`, mantendo intactos o placeholder `SPRING_DATASOURCE_URL` e o fallback PostgreSQL existente.
- [x] 2.2 Revisar o diff focado de `application-dev.properties` e confirmar que a única alteração seja a remoção dos dois caracteres indevidos.

## 3. Verificação e encerramento

- [x] 3.1 Verificar diretamente que a propriedade corrigida resolve uma URL externa sem transformação e conserva `jdbc:postgresql://localhost:5432/gestaoacoesdb` como fallback.
- [x] 3.2 Executar o teste de contexto existente `GestaoacoesApplicationTests` pelo Maven Wrapper.
- [x] 3.3 Executar `clean verify` pelo Maven Wrapper e confirmar que a suíte completa e o build terminem com sucesso.
- [x] 3.4 Validar a change `corrigir-configuracao-dev` com OpenSpec em modo strict.
- [x] 3.5 Executar `git diff --check`, revisar o diff e o status final, confirmando que nenhum código-fonte, teste, schema, changelog, dependência ou configuração fora da URL alvo foi alterado por esta change.
