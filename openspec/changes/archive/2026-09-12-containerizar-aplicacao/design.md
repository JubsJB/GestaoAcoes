## Context

Ver proposal.md e a Aula 12 referenciada. O backend já usa Liquibase, profile `dev` e propriedades por variáveis; o frontend usa `/api` como base.

## Goals / Non-Goals

**Goals:** três serviços integrados, build multi-stage, persistência e healthcheck verificáveis.

**Non-Goals:** alterar domínio, endpoints, migrations, autenticação ou adicionar dependências Angular.

## Decisions

O Compose terá `postgres`, `backend` e `frontend`. O backend recebe `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/...`; o frontend Nginx fará proxy de `/api/` para `http://backend:8080/`, mantendo o contrato existente. O runtime backend usará `eclipse-temurin:17-jre-jammy` e usuário `spring` conforme a Aula 12. O frontend usará `node:22-alpine` no build e `nginx:1.27-alpine` no runtime.

## Risks / Trade-offs

[Risco] providers externos podem falhar ou exigir chaves → Compose recebe chaves por `.env`, sem chamadas automáticas adicionais.

[Risco] porta do host já ocupada → portas são variáveis no `.env.example`.

[Risco] o primeiro build requer registry e Maven/npm → instruções deixam explícito o acesso à rede necessário.
