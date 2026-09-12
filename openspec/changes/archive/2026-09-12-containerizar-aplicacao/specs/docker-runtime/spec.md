## Purpose

Permitir executar o MVP completo com backend, PostgreSQL e frontend em containers coordenados, com persistência local e configuração segura por variáveis de ambiente.

## ADDED Requirements

### Requirement: Imagem backend reproduzível e não-root
O projeto SHALL produzir uma imagem backend multi-stage com Maven 3.9.x e Eclipse Temurin 17 no build, copiando `pom.xml` antes do código e executando `dependency:go-offline`; o runtime SHALL usar JRE Temurin 17, copiar somente o JAR final, expor 8080 e executar como usuário não-root com `java -jar`.

#### Scenario: Build da imagem backend
- **WHEN** a imagem backend é construída
- **THEN** o build gera o JAR sem repetir testes e a imagem final não contém Maven ou código-fonte

### Requirement: Compose com PostgreSQL saudável e persistente
O Compose SHALL usar `postgres:17-alpine`, volume nomeado persistente, `restart: unless-stopped`, `pg_isready` healthcheck e credenciais/banco/porta externa configuráveis por variáveis. O backend SHALL depender do estado healthy e usar o hostname interno do serviço PostgreSQL.

#### Scenario: Inicialização coordenada
- **WHEN** `docker compose up -d --build` é executado com `.env` preenchido
- **THEN** PostgreSQL fica healthy antes do backend iniciar, Liquibase aplica as migrations e os dados permanecem após `docker compose down` sem `-v`

### Requirement: Frontend production integrado ao backend
O Compose SHALL iniciar uma imagem production do Angular servida por Nginx, acessível pela porta externa configurável, mantendo chamadas relativas `/api` e encaminhando-as ao serviço backend. O frontend SHALL carregar sem URL de API hardcoded incompatível com a rede Compose.

#### Scenario: Acesso integrado
- **WHEN** o usuário acessa a porta publicada do frontend
- **THEN** os arquivos Angular são servidos e uma chamada `/api` alcança o backend pelo hostname Compose

### Requirement: Configuração sem secrets versionados
Os artefatos Docker SHALL fornecer `.env.example` somente com placeholders seguros para banco, portas, profile e integrações, sem versionar `.env` ou embutir chaves.

#### Scenario: Configuração segura
- **WHEN** o consumidor copia `.env.example` para `.env`
- **THEN** pode preencher valores locais sem que secrets sejam incorporados ao Dockerfile, imagem ou Compose versionado
