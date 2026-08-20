## Purpose

Estabelecer uma baseline de execução segura e verificável para que a aplicação Spring Boot possa evoluir sem falhas de bootstrap, exposição de credenciais ou recriação destrutiva do banco de dados.

## ADDED Requirements

### Requirement: Descoberta completa dos componentes da aplicação
A aplicação SHALL configurar seu bootstrap para que todos os componentes Spring pertencentes à árvore de pacotes estabelecida do projeto sejam elegíveis para descoberta automática.

#### Scenario: Carregamento da árvore de componentes da aplicação
- **WHEN** o contexto Spring Boot for iniciado
- **THEN** os componentes existentes nos pacotes de configuração, serviços e recursos da aplicação serão descobertos pelo escaneamento de componentes

### Requirement: Ausência de inicialização de dados sem responsabilidade definida
A aplicação SHALL iniciar sem executar rotinas de carga de dados que não possuam comportamento e necessidade explicitamente definidos.

#### Scenario: Inicialização no profile de desenvolvimento
- **WHEN** a aplicação for iniciada com o profile `dev`
- **THEN** o contexto não dependerá de um inicializador vazio, inexistente ou criado apenas para satisfazer o bootstrap

### Requirement: Ativação explícita e isolada de profiles
A aplicação SHALL manter os profiles `test` e `dev` isolados e SHALL NOT ativar o profile `test` globalmente na configuração padrão.

#### Scenario: Inicialização sem seleção de profile
- **WHEN** a aplicação for iniciada sem configuração externa de profile
- **THEN** o profile `test` não será ativado implicitamente

#### Scenario: Execução dos testes de contexto
- **WHEN** os testes automatizados da baseline forem executados
- **THEN** o profile `test` será ativado explicitamente e utilizará somente sua configuração de teste

#### Scenario: Inicialização de desenvolvimento
- **WHEN** o profile `dev` for selecionado explicitamente
- **THEN** somente a configuração destinada ao ambiente de desenvolvimento será aplicada

### Requirement: Banco H2 restrito aos testes
A aplicação SHALL disponibilizar o banco H2 somente durante testes automatizados e SHALL NOT usar H2 como fallback em execuções normais ou com o profile `dev`.

#### Scenario: Execução fora dos testes
- **WHEN** a aplicação for empacotada ou iniciada fora da execução de testes
- **THEN** o banco H2 e seu console não estarão disponíveis no ambiente de execução

#### Scenario: Execução dos testes com H2
- **WHEN** os testes automatizados da baseline forem executados
- **THEN** o banco H2 estará disponível somente para essa execução isolada

### Requirement: Configuração segura do PostgreSQL
A aplicação SHALL aceitar URL e usuário do PostgreSQL por meio de `SPRING_DATASOURCE_URL` e `SPRING_DATASOURCE_USERNAME`, podendo usar defaults locais não sensíveis quando essas variáveis não forem fornecidas. A senha SHALL ser obtida obrigatoriamente por `SPRING_DATASOURCE_PASSWORD`, sem valor padrão, e o projeto SHALL NOT manter credenciais ativas em arquivos versionados.

#### Scenario: Inicialização de desenvolvimento com configuração fornecida
- **WHEN** o profile `dev` for ativado por `SPRING_PROFILES_ACTIVE=dev` com todas as variáveis de conexão exigidas
- **THEN** a aplicação utilizará os valores externos para configurar a conexão PostgreSQL

#### Scenario: Inicialização de desenvolvimento sem URL ou usuário externos
- **WHEN** o profile `dev` for ativado sem `SPRING_DATASOURCE_URL` ou `SPRING_DATASOURCE_USERNAME`
- **THEN** a aplicação utilizará os defaults locais documentados de URL e usuário, sem obter uma senha de arquivo versionado

#### Scenario: Inicialização de desenvolvimento sem senha
- **WHEN** o profile `dev` for ativado sem `SPRING_DATASOURCE_PASSWORD`
- **THEN** a inicialização falhará explicitamente em vez de utilizar uma senha padrão ou versionada

### Requirement: Modelo local de variáveis sem carregamento implícito
O projeto SHALL versionar um `.env.example` com os nomes das variáveis usadas no desenvolvimento e no Docker Compose, SHALL manter o `.env` real ignorado pelo Git e SHALL NOT assumir que o Spring Boot carregará automaticamente arquivos `.env`.

#### Scenario: Preparação da configuração local
- **WHEN** uma pessoa preparar seu ambiente a partir do `.env.example`
- **THEN** ela poderá identificar as variáveis esperadas sem obter uma credencial PostgreSQL ativa do repositório

#### Scenario: Execução direta da aplicação
- **WHEN** a aplicação for executada diretamente fora de uma ferramenta que injete o `.env`
- **THEN** as variáveis necessárias deverão ser exportadas pelo shell, pela IDE ou por outro orquestrador compatível

### Requirement: Proteção do schema persistente
A configuração de desenvolvimento SHALL validar a compatibilidade do schema PostgreSQL sem criar, recriar ou remover automaticamente estruturas persistentes.

#### Scenario: Inicialização sobre banco de desenvolvimento existente
- **WHEN** a aplicação for iniciada com o profile `dev`
- **THEN** a política de schema não executará `create` nem `create-drop` sobre o PostgreSQL

#### Scenario: Schema de desenvolvimento incompatível
- **WHEN** a aplicação for iniciada com um schema PostgreSQL incompatível
- **THEN** a validação falhará sem tentar corrigir a incompatibilidade por meio de operações destrutivas

#### Scenario: Banco efêmero de teste
- **WHEN** os testes automatizados forem executados com banco H2 isolado
- **THEN** a configuração de teste poderá criar e descartar somente o schema efêmero dessa execução

### Requirement: Baseline verificável de build e contexto
O projeto SHALL compilar com a configuração de build versionada e SHALL possuir testes mínimos que comprovem o carregamento do contexto Spring Boot em ambiente isolado.

#### Scenario: Compilação da aplicação
- **WHEN** o build de verificação for executado
- **THEN** o código principal e o código de teste serão compilados sem erro

#### Scenario: Carregamento do contexto de teste
- **WHEN** a suíte mínima da baseline for executada
- **THEN** o contexto Spring Boot carregará usando o profile `test` e sem depender de PostgreSQL ou de credenciais externas
