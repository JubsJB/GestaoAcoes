## 1. Bootstrap e componentes Spring

- [x] 1.1 Mover `GestaoacoesApplication` para o package raiz `com.projeto`, atualizar referências e testes afetados e verificar a descoberta dos componentes existentes nos pacotes descendentes.
- [x] 1.2 Remover de `DevConfig` somente a injeção, o `@PostConstruct` e a chamada de inicialização inconsistentes, preservando `DevConfig`, `DBService` e `TestConfig` sem adicionar comportamento substituto.

## 2. Profiles e configuração de banco

- [x] 2.1 Remover a ativação global do profile `test` e manter em `application.properties` somente propriedades comuns.
- [x] 2.2 Transferir a configuração do profile `test` para `src/test/resources`, ativá-lo explicitamente nos testes, limitar o driver H2 ao escopo de teste e remover o suporte e a configuração do console H2.
- [x] 2.3 Configurar o profile `dev` com `SPRING_DATASOURCE_URL` e `SPRING_DATASOURCE_USERNAME` sobre defaults locais não sensíveis, manter `SPRING_DATASOURCE_PASSWORD` obrigatória e sem default e remover credenciais ativas dos arquivos versionados.
- [x] 2.4 Consolidar `ddl-auto=validate` nos profiles `dev` e `test`, com Liquibase aplicando as migrations 001–006, e confirmar que nenhuma configuração usa `create`, `create-drop` ou `update` como estratégia de schema.
- [x] 2.5 Documentar `SPRING_PROFILES_ACTIVE=dev`, as variáveis nativas de conexão e as variáveis previstas para Docker Compose em `.env.example`, manter `.env` ignorado e esclarecer que execuções diretas não carregam esse arquivo automaticamente nem usam H2 como fallback.
- [x] 2.6 Garantir que nenhuma credencial ativa permaneça no repositório e registrar a necessidade de revogação ou rotação externa de qualquer credencial anteriormente exposta, sem representar essa ação externa como executada ou comprovada pelo repositório.

## 3. Testes da baseline

- [x] 3.1 Ajustar o teste de contexto para usar explicitamente o profile `test`, permanecer independente do PostgreSQL e comprovar a descoberta de ao menos um componente localizado em um pacote descendente de `com.projeto`.
- [x] 3.2 Adicionar somente os testes mínimos adicionais necessários para validar isolamento de profiles e carregamento seguro da configuração, evitando cenários de negócio.

## 4. Verificação e consistência

- [x] 4.1 Executar o build Maven pelo wrapper e confirmar a compilação do código principal e de teste.
- [x] 4.2 Executar a suíte automatizada e confirmar que o contexto Spring Boot carrega integralmente com H2, profile `test`, Liquibase 001–006 e Hibernate `ddl-auto=validate`.
- [x] 4.3 Revisar os arquivos rastreados e o build para confirmar a ausência de credenciais PostgreSQL ativas, o isolamento do `.env`, a ausência de H2 fora do escopo de teste e de políticas destrutivas de schema em qualquer profile.
- [x] 4.4 Confirmar que nenhuma dependência, abstração ou funcionalidade de negócio fora do escopo foi introduzida.
- [x] 4.5 Atualizar o Graphify após as alterações de código e verificar que o grafo represente o bootstrap, os componentes e os testes resultantes.
