## 1. Artefatos Docker

- [x] 1.1 Criar `.dockerignore`, `.env.example`, `Dockerfile` e `compose.yaml` para backend/PostgreSQL.
- [x] 1.2 Criar Dockerfile e configuração Nginx production para o frontend.

## 2. Documentação e integração

- [x] 2.1 Atualizar README com pré-requisitos, configuração e ciclo Docker.
- [x] 2.2 Validar config, build, subida, healthchecks, endpoints, integração frontend/backend e persistência do volume.
## Homologação manual aprovada — 2026-09-12

Evidências humanas informadas pelo usuário, sem repetição dos comandos nesta etapa de encerramento:

- Docker/Compose funcionando no Windows + WSL2.
- `docker compose config --services` retornou `postgres`, `backend` e `frontend`.
- `docker compose build` concluiu backend e frontend com sucesso.
- `docker compose up -d` subiu os três serviços; PostgreSQL ficou healthy e backend/frontend funcionaram, aprovando a integração e os acessos previstos na task 2.2.
- Liquibase executou corretamente e criou as tabelas esperadas.
- Acesso ao PostgreSQL via `docker compose exec postgres psql` validado.
- Persistência após `docker compose down` validada.
- Alteração das portas externas via `.env` validada.
- `.env` confirmado como ignorado pelo Git e `.env.example` versionável.
- `docker compose down` executado ao final, preservando o volume.

A task 2.2 foi encerrada com base nessa homologação humana aprovada, substituindo a pendência anterior de indisponibilidade do Docker na sessão do agente.

Limitação externa: a Alpha Vantage atingiu rate limit durante a tentativa de cadastrar ação EUA. Trata-se de limitação do provider, não de falha da containerização.

Não foram repetidos build Docker, subida dos serviços, testes completos, homologação externa ou Graphify nesta etapa.
