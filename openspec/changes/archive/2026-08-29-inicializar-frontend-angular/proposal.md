## Why

O projeto concluiu o backend do MVP, mas ainda não possui um workspace frontend versionado e reproduzível. É necessário estabelecer uma base Angular local, isolada do backend e sem funcionalidades de negócio, para que as próximas changes possam implementar layout e features sobre contratos técnicos estáveis.

## What Changes

- Criar futuramente o workspace Angular em `frontend/`, com Angular e Angular CLI locais fixados em `22.1.4`, npm e lockfile versionado.
- Configurar aplicação standalone, strict mode, routing, SCSS, build, desenvolvimento e testes unitários com Vitest.
- Estabelecer bootstrap mínimo com `provideHttpClient`, configuração central da URL base da API e proxy de desenvolvimento, sem `localhost` em services futuros.
- Criar apenas a estrutura arquitetural que tenha responsabilidade imediata: configuração e HTTP em `core/`, rotas/layout mínimo de bootstrap e pontos de extensão documentados para `shared/` e `features/`, sem diretórios vazios.
- Incluir infraestrutura técnica mínima para representar `StandardError`, normalizar falhas HTTP e aplicar um interceptor funcional, sem mensagens ou services específicos de features.
- Registrar OpenAPI como fonte de verdade dos contratos futuros, sem gerar cliente nem DTOs de domínio nesta change.
- Adiar Angular Material para a change `criar-layout-navegacao`, mantendo esta fundação sem dependência visual e permitindo que a instalação seja validada junto ao primeiro uso real.
- Não incluir lint adicional, formatter adicional, E2E, autenticação, gerenciamento de estado global, gráficos ou qualquer tela/serviço de negócio.

## Capabilities

### New Capabilities

- `frontend-application-foundation`: define a base Angular reproduzível, seu bootstrap, configuração HTTP/API, tratamento técnico mínimo de erros, testes e limites arquiteturais para futuras features.

### Modified Capabilities

Nenhuma.

## Impact

- A implementação futura ficará restrita à nova árvore `frontend/` e aos artefatos de documentação desta change.
- Serão introduzidos futuramente `package.json`, `package-lock.json`, configurações Angular/TypeScript, fontes mínimas e testes do frontend.
- O backend, seu Maven build, Liquibase, código, recursos, testes e documento OpenAPI permanecerão inalterados.
- As dependências futuras ficarão limitadas à baseline gerada pelo Angular CLI 22.1.4; Angular Material será tratado separadamente.
