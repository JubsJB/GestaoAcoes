## Why

A aplicação precisa de uma baseline técnica confiável antes da implementação das funcionalidades de negócio. A configuração atual não garante a descoberta de todos os componentes Spring, contém uma inicialização inconsistente entre `DevConfig` e `DBService`, mantém o profile de teste ativo globalmente e expõe configuração sensível e destrutiva do PostgreSQL em arquivo versionado.

## What Changes

- Posicionar a classe principal no pacote raiz `com.projeto`, garantindo uma raiz comum para componentes Spring, entidades JPA e repositories futuros.
- Remover a inicialização de dados sem comportamento definido e resolver a dependência inconsistente entre `DevConfig` e `DBService`, preservando `DevConfig`, `DBService` e `TestConfig` para uso futuro, sem criar um inicializador vazio ou dados artificiais.
- Tornar a ativação dos profiles `test` e `dev` explícita e adequada a cada ambiente, sem fixar `test` como profile global da aplicação.
- Restringir H2 ao ambiente de testes, sem fallback para banco em memória em execuções normais ou de desenvolvimento.
- Configurar o PostgreSQL pelos nomes nativos do Spring, mantendo a senha obrigatoriamente externa e permitindo defaults locais não sensíveis para URL e usuário no profile `dev`.
- Versionar um `.env.example` para documentar as variáveis de desenvolvimento e Docker Compose, mantendo o `.env` real ignorado e sem adicionar mecanismo automático de carregamento desse arquivo à aplicação.
- Garantir que nenhuma credencial ativa permaneça no repositório e registrar a necessidade de revogação ou rotação externa de qualquer credencial anteriormente exposta, sem afirmar que essa ação externa foi executada sem evidência e sem reescrever automaticamente o histórico Git.
- Substituir a criação destrutiva de schema por uma política segura na qual o Liquibase aplica as migrations versionadas e o Hibernate apenas valida o resultado, tanto em desenvolvimento quanto nos testes com H2 efêmero.
- Garantir compilação e carregamento do contexto Spring Boot por meio de testes mínimos da baseline técnica.
- Preservar a estrutura existente e não adicionar dependências ou abstrações sem necessidade demonstrada.
- Manter fora desta change todas as funcionalidades de negócio, integrações externas, documentação de API e frontend.

## Capabilities

### New Capabilities

- `application-runtime-baseline`: Define os requisitos para bootstrap Spring Boot, profiles, configuração externa do banco de dados, proteção do schema, compilação e teste de carregamento do contexto.

### Modified Capabilities

Nenhuma. Ainda não existem capabilities principais no OpenSpec e esta change não altera requisitos funcionais do PRD.

## Impact

- Código de bootstrap, incluindo a movimentação da classe principal para `com.projeto`, e configuração Spring sob `src/main/java/com/projeto`.
- Arquivos `application*.properties` dos ambientes padrão, `dev` e `test`.
- Configuração de build existente para limitar H2 ao classpath de testes, aplicar as migrations Liquibase 001–006 e validar o schema com Hibernate nos testes do contexto Spring Boot.
- Configuração operacional do PostgreSQL, incluindo defaults locais não sensíveis para URL e usuário, senha externa obrigatória e documentação das variáveis em `.env.example`.
- Regras de versionamento local, com `.env` ignorado pelo Git e `.env.example` disponível como modelo sem credencial ativa.
- A remoção e a externalização de credenciais no repositório estão concluídas; a revogação ou rotação efetiva da credencial antiga permanece uma responsabilidade operacional externa não comprovada pelo repositório.
- Nenhuma API, entidade de negócio, cálculo financeiro, integração externa ou componente frontend será criado ou alterado por esta change.
