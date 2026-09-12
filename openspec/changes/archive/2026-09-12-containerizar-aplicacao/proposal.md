## Why

O MVP hoje exige instalação local de Java, Maven, PostgreSQL e Node. A containerização deve reproduzir o ambiente da Aula 12 e permitir iniciar backend, banco e frontend com um único Compose, preservando migrations e configuração por ambiente.

## What Changes

- Criar imagem multi-stage do backend com Maven/Temurin 17 e runtime não-root.
- Criar imagem production do Angular servida por Nginx, com proxy interno `/api` para o backend.
- Criar Compose com PostgreSQL 17 Alpine, volume persistente, healthcheck e dependência saudável.
- Adicionar `.dockerignore`, `.env.example` e instruções Docker no README.

## Capabilities

### New Capabilities

- `docker-runtime`: execução reproduzível do MVP completo via Docker Compose.

### Modified Capabilities

Nenhuma regra funcional é alterada.

## Impact

Novos artefatos Docker na raiz e no frontend, configuração de ambiente e documentação. Nenhuma alteração em endpoints, DTOs, cálculos, migrations ou dependências Angular.
