## Purpose

Estabelecer uma aplicação frontend Angular reproduzível e isolada, com bootstrap, navegação e comunicação HTTP técnicas prontas para receber futuras features sem antecipar comportamento de negócio.

## ADDED Requirements

### Requirement: Workspace frontend reproduzível e isolado
O projeto SHALL possuir um workspace Angular em `frontend/`, gerenciado por npm, com manifesto e lockfile versionados e versões locais fixadas do Angular e Angular CLI. O workspace MUST permanecer independente do build Maven e MUST NOT exigir a Angular CLI global.

#### Scenario: Instalação limpa por lockfile
- **WHEN** um desenvolvedor obtém o repositório em ambiente compatível e executa a instalação determinística dentro de `frontend/`
- **THEN** npm resolve a baseline versionada sem depender da CLI global nem alterar arquivos do backend

#### Scenario: Execução pela CLI local
- **WHEN** um comando Angular previsto pelo projeto é acionado por script npm
- **THEN** a versão local fixada da CLI é utilizada

### Requirement: Baseline Angular estrita
A aplicação SHALL usar componentes standalone, strict mode, routing e SCSS. O build de produção SHALL aplicar verificação estrita de TypeScript e templates Angular.

#### Scenario: Bootstrap da aplicação
- **WHEN** a aplicação é iniciada
- **THEN** o componente raiz standalone é inicializado pela configuração da aplicação sem `NgModule` raiz

#### Scenario: Verificação estrita
- **WHEN** código ou template incompatível com as regras strict é compilado
- **THEN** a compilação falha explicitamente

### Requirement: Navegação inicial sem feature de negócio
A aplicação SHALL possuir configuração central de rotas suficiente para inicializar e validar o roteamento, mas MUST NOT incluir rotas, telas ou dados de dashboard, corretoras, ações, carteiras, operações, indicadores, snapshots, evolução patrimonial ou gráficos.

#### Scenario: Rota inicial
- **WHEN** o navegador acessa a URL inicial da aplicação
- **THEN** o router resolve uma rota técnica mínima sem carregar funcionalidade de negócio

#### Scenario: Rota desconhecida
- **WHEN** o navegador acessa uma rota não reconhecida
- **THEN** a aplicação aplica o comportamento de fallback definido centralmente sem introduzir uma tela de negócio

### Requirement: Configuração central da API
A aplicação SHALL fornecer um único mecanismo injetável para a URL base da API. Código HTTP futuro MUST compor URLs a partir desse mecanismo e MUST NOT conter origem `localhost` hardcoded.

#### Scenario: Resolução da URL base
- **WHEN** um consumidor HTTP injeta a configuração da API
- **THEN** ele recebe a URL base definida pela configuração central da aplicação

#### Scenario: Configuração substituível em teste
- **WHEN** um teste fornece uma URL base alternativa
- **THEN** o consumidor usa o valor de teste sem depender de servidor real

### Requirement: Comunicação HTTP e proxy de desenvolvimento
A aplicação SHALL disponibilizar o cliente HTTP no bootstrap e SHALL oferecer proxy de desenvolvimento para encaminhar chamadas relativas da API ao backend local. O proxy MUST ser aplicado apenas ao fluxo de desenvolvimento e MUST NOT definir o endereço de produção.

#### Scenario: Cliente HTTP disponível
- **WHEN** um componente de infraestrutura solicita o cliente HTTP
- **THEN** a injeção é resolvida pela configuração global da aplicação

#### Scenario: Encaminhamento no desenvolvimento
- **WHEN** a aplicação é servida em modo de desenvolvimento e realiza chamada pelo prefixo relativo configurado
- **THEN** o servidor de desenvolvimento encaminha a chamada ao backend definido no proxy

#### Scenario: Build sem destino local embutido
- **WHEN** o bundle de produção é gerado
- **THEN** a configuração de proxy não incorpora o endereço local de desenvolvimento ao bundle

### Requirement: Normalização técnica de erros HTTP
A aplicação SHALL representar o contrato público `StandardError` do backend com os campos `timeStamp`, `status`, `error`, `message`, `path`, `code` e `details`, e SHALL normalizar falhas HTTP por infraestrutura central. A normalização MUST preservar dados públicos válidos do backend, MUST produzir uma falha técnica previsível quando o corpo não seguir o contrato e MUST NOT definir mensagens específicas de features.

#### Scenario: Erro padronizado do backend
- **WHEN** uma resposta HTTP de erro contém um `StandardError` válido
- **THEN** a infraestrutura preserva seus campos públicos para o consumidor

#### Scenario: Erro não padronizado
- **WHEN** uma falha de rede ou resposta inválida não contém `StandardError`
- **THEN** a infraestrutura produz uma representação técnica normalizada sem inventar regra ou mensagem de negócio

### Requirement: Estrutura arquitetural mínima e evolutiva
O código inicial SHALL separar responsabilidades transversais em `core/` e composição visual em `layout/`. Áreas `shared/` e `features/` SHALL ser criadas somente quando contiverem um artefato com responsabilidade concreta; diretórios vazios e services ou DTOs de domínio MUST NOT ser adicionados nesta baseline.

#### Scenario: Inspeção da árvore inicial
- **WHEN** a estrutura de `src/app` é revisada após a implementação
- **THEN** cada diretório versionado possui conteúdo necessário à baseline e nenhuma feature de negócio está presente

### Requirement: Verificação automatizada da baseline
O workspace SHALL fornecer scripts npm documentados para desenvolvimento, teste e build. Testes automatizados SHALL verificar bootstrap, routing, configuração HTTP, URL base e normalização de erros; o build de produção SHALL concluir sem depender de backend ativo.

#### Scenario: Testes unitários
- **WHEN** a suíte é executada uma única vez em ambiente de validação
- **THEN** os testes de infraestrutura concluem sem acessar rede real

#### Scenario: Build de produção
- **WHEN** o script de build é executado
- **THEN** a aplicação é compilada em modo de produção sem código funcional de negócio

### Requirement: Contratos futuros orientados por OpenAPI
O README do frontend SHALL registrar o OpenAPI do backend como fonte de verdade para services, DTOs e interfaces futuros, incluindo a baseline atual de 18 paths funcionais e 24 operações HTTP. Esta change MUST NOT gerar cliente OpenAPI nem introduzir DTOs de domínio.

#### Scenario: Evolução futura de contrato
- **WHEN** uma change futura criar um service ou DTO de domínio no frontend
- **THEN** sua revisão é orientada pelo documento OpenAPI vigente do backend

#### Scenario: Baseline sem geração de cliente
- **WHEN** as dependências e fontes desta change são inspecionadas
- **THEN** não existe gerador ou cliente OpenAPI gerado
