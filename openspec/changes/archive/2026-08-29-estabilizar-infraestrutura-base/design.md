## Context

Veja `proposal.md` para a motivação. A classe de bootstrap está em `com.projeto.gestaoacoes`, enquanto configurações, serviços e recursos estão em pacotes irmãos sob `com.projeto`; por convenção, a raiz atual não alcança esses componentes nem prepara a descoberta automática de futuros elementos de persistência no mesmo namespace. O Graphify confirma a relação `DevConfig` → `DBService` e a chamada de inicialização anotada com `@PostConstruct`, mas o serviço não implementa essa operação. `DevConfig`, `DBService` e `TestConfig` serão preservados para uso futuro. A configuração padrão ativa `test`, H2 está disponível no classpath de runtime, a configuração `dev` contém dados concretos do PostgreSQL e utiliza criação automática do schema, e a configuração de teste está empacotada nos recursos principais.

As alterações devem preservar a aplicação Spring Boot e o build Maven existentes, evitar novas abstrações e não antecipar qualquer domínio descrito no PRD. Após a implementação original desta baseline, capabilities posteriores introduziram Liquibase e consolidaram o mesmo changelog nos ambientes `dev` e `test`; o estado final documentado por esta change incorpora essa evolução já implementada.

## Goals / Non-Goals

**Goals:**

- Tornar o bootstrap determinístico para toda a árvore `com.projeto`.
- Separar claramente configuração padrão e desenvolvimento PostgreSQL dos testes, mantendo H2 exclusivamente no ambiente de testes.
- Eliminar inicializadores sem responsabilidade real.
- Tornar configuração sensível externa e manter a gestão de schema não destrutiva, com Liquibase como fonte de evolução e Hibernate somente como validador.
- Manter uma verificação mínima e repetível de compilação e contexto.

**Non-Goals:**

- Definir entidades, repositories, dados iniciais ou regras de negócio.
- Introduzir ferramenta de migração de banco como parte da implementação original desta change; o Liquibase foi incorporado posteriormente e integra a baseline técnica atual.
- Introduzir biblioteca para carregar arquivos `.env` automaticamente ou adicionar Docker Compose nesta change.
- Reestruturar as camadas da aplicação ou adicionar dependências.
- Limpar histórico Git contendo segredos; credenciais já expostas devem ser revogadas ou rotacionadas operacionalmente.

## Decisions

### 1. Mover a classe principal para o package raiz `com.projeto`

A classe `GestaoacoesApplication` será movida de `com.projeto.gestaoacoes` para `com.projeto`. A anotação `@SpringBootApplication` passará a definir implicitamente a raiz comum para componentes Spring e para a descoberta futura de entidades e repositories sob o namespace da aplicação.

Alternativa considerada: manter a classe atual e usar `scanBasePackages="com.projeto"`. Foi rejeitada porque essa opção altera somente o component scan e exigiria configurações adicionais para entidades JPA e repositories, contrariando o objetivo de uma baseline simples e preparada para o domínio futuro.

### 2. Remover somente o bootstrap de dados inconsistente e preservar as classes

O vínculo de inicialização entre `DevConfig` e `DBService` será removido: injeção destinada exclusivamente à carga, `@PostConstruct` e chamada a `initDB()` deixarão de fazer parte do bootstrap. `DevConfig`, `DBService` e `TestConfig` permanecerão no projeto porque terão uso futuro, mas não receberão comportamento fictício nesta change. Não será adicionado método vazio, carga artificial ou `CommandLineRunner`.

Alternativas consideradas: implementar `DBService.initDB()` como no-op, o que apenas ocultaria a inconsistência; e remover as classes vazias, opção rejeitada porque elas foram reservadas para evolução futura do projeto.

### 3. Ativar profiles no ponto de execução apropriado

`application.properties` manterá apenas configuração comum e não definirá profile ativo. O ambiente de desenvolvimento selecionará `dev` por `SPRING_PROFILES_ACTIVE=dev`. Os testes declararão `test` explicitamente, e `application-test.properties` ficará em `src/test/resources` para não integrar o artefato de produção.

O driver H2 terá escopo de teste e o suporte ao console H2 será removido, pois não é necessário para testes automatizados. Dessa forma, uma execução normal ou com `dev` não poderá usar banco em memória como fallback. A inicialização sem profile e sem configuração de datasource poderá falhar, em vez de selecionar H2 silenciosamente.

Alternativa considerada: manter `spring.profiles.active=test` como padrão. Foi rejeitada porque mistura configuração de teste com execuções normais e pode mascarar problemas de ambiente.

### 4. Usar variáveis nativas do Spring com defaults locais não sensíveis

O profile `dev` aceitará `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. URL e usuário terão defaults locais destinados ao PostgreSQL de desenvolvimento, enquanto a senha continuará obrigatoriamente externa e sem valor padrão. Assim, a configuração local permanece simples sem versionar uma credencial ativa.

Um `.env.example` será versionado para documentar as variáveis nativas e as variáveis previstas para Docker Compose. O `.env` real será ignorado pelo Git. Nenhuma biblioteca de dotenv nem Docker Compose será adicionada nesta change: executar a aplicação diretamente continuará exigindo que o shell, a IDE ou outro orquestrador injete as variáveis no processo.

Alternativa considerada: exigir também URL e usuário externamente, sem defaults. A implementação final adotou defaults locais para esses dois valores por não serem secretos e por reduzirem configuração repetitiva; a senha permaneceu obrigatória para evitar credencial válida no repositório.

### 5. Evoluir o schema com Liquibase e validá-lo com Hibernate

Os profiles `dev` e `test` utilizam `spring.jpa.hibernate.ddl-auto=validate`. O Liquibase aplica o changelog versionado e as migrations 001–006 antes da criação do `EntityManagerFactory`; o Hibernate somente verifica a compatibilidade do mapeamento. O H2 permanece efêmero e restrito aos testes, mas seu schema também é criado pelo Liquibase, nunca por `create`, `create-drop` ou `update` do Hibernate.

A implementação original considerou `create-drop` exclusivamente para o H2 e adiou uma ferramenta de migração enquanto a baseline estava vazia. Essa alternativa foi superada pelas capabilities posteriores: o mesmo changelog Liquibase passou a atender PostgreSQL e H2, eliminando diferenças de estratégia de schema entre ambientes. `update` e qualquer criação automática pelo Hibernate permanecem rejeitados por reduzirem previsibilidade e controle.

### 6. Validar pelo build Maven e por teste de contexto focado

A verificação usa o wrapper Maven e a suíte de testes do projeto. O teste de contexto ativa `test` explicitamente, comprova que o bootstrap encontra componentes representativos sem acessar PostgreSQL e confirma H2, Liquibase e Hibernate `validate`. Testes adicionais permanecem limitados às fronteiras que não estejam cobertas pelo carregamento do contexto.

Alternativa considerada: criar uma suíte ampla de integração. Foi rejeitada por exceder a baseline e antecipar comportamentos de negócio inexistentes.

## Risks / Trade-offs

- [O uso de `validate` impede a inicialização quando o changelog não produziu o schema esperado] → Aplicar as migrations Liquibase antes do Hibernate e não reintroduzir criação automática destrutiva.
- [Senha ausente impede o profile `dev` de iniciar] → Documentar `SPRING_DATASOURCE_PASSWORD` e manter o erro explícito, sem senha padrão.
- [Os defaults locais de URL e usuário podem apontar para um banco diferente do pretendido] → Permitir sobrescrita pelas variáveis nativas e documentar os valores efetivamente usados em cada ambiente.
- [Copiar `.env.example` para `.env` não configura sozinho uma execução direta do Spring Boot] → Exigir exportação pelo shell, configuração da IDE ou injeção por orquestrador; não adicionar carregamento implícito.
- [Mover a classe principal altera seu nome totalmente qualificado e pode afetar referências de teste ou execução] → Atualizar referências de bootstrap e testes na mesma alteração e validar pelo Maven Wrapper.
- [Credenciais removidas do estado atual permanecem no histórico Git] → Manter documentada a necessidade de revogação ou rotação no ambiente PostgreSQL; não afirmar sua execução sem evidência e tratar qualquer reescrita de histórico como ação separada e coordenada.
- [Restringir H2 aos testes elimina o fallback conveniente em execuções locais sem profile] → Exigir seleção explícita de `dev` e configuração PostgreSQL externa, mantendo H2 somente para a suíte automatizada.
- [As classes preservadas continuam sem comportamento útil nesta baseline] → Manter apenas sua estrutura atual e adiar qualquer responsabilidade para a change funcional que efetivamente a exigir.

## Migration Plan

1. Remover credenciais ativas dos arquivos versionados, externalizar o datasource e documentar que qualquer credencial anteriormente exposta deve ser revogada ou rotacionada externamente; a execução dessa ação no PostgreSQL não é realizada nem comprovada pelo repositório.
2. Mover a classe principal para `com.projeto`, retirar somente o vínculo de inicialização inconsistente e preservar `DevConfig`, `DBService` e `TestConfig`.
3. Configurar `dev` com as variáveis nativas, defaults locais de URL e usuário e senha externa; restringir H2 ao classpath de teste; aplicar as migrations Liquibase 001–006 e usar Hibernate `validate` em ambos os ambientes.
4. Executar compilação e testes mínimos antes de disponibilizar a baseline.
5. Iniciar o ambiente de desenvolvimento com `dev` explicitamente selecionado e com a senha injetada pelo shell, IDE ou orquestrador; sobrescrever URL e usuário quando os defaults locais não forem adequados.

Rollback: reverter as alterações de bootstrap e profiles se necessário, mas nunca restaurar credenciais versionadas nem uma política destrutiva de schema. Como a estratégia proposta não altera o schema persistente, não há rollback de banco previsto.
