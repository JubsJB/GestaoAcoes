# Frontend — Gestão de Ações

Baseline Angular do frontend do sistema Gestão de Ações. Este workspace é independente do build Maven/Spring Boot localizado na raiz do repositório.

## Baseline

- Angular e Angular CLI: `22.1.4`, fixados localmente no `package.json`;
- Node: `^22.22.3 || ^24.15.0 || ^26.0.0` (ambiente inicial: `24.19.0`);
- npm: `11.17.0`;
- standalone components, strict TypeScript/templates, routing e SCSS;
- HttpClient;
- Vitest com jsdom.

A Angular CLI global não faz parte do contrato. Os scripts npm usam `node_modules/.bin/ng` do próprio workspace.

## Comandos

Execute dentro de `frontend/`:

```bash
npm install
npm ci
npm start
npm test -- --watch=false
npm run build
npm run ng -- version
```

Use `npm ci` para instalação determinística pelo lockfile. Use `npm install` somente quando houver manutenção intencional das dependências e do `package-lock.json`.

## API e proxy local

O token injetável `API_BASE_URL` centraliza o prefixo da API e fornece `/api` por padrão. Código HTTP futuro deve compor URLs com esse token; não deve incluir `http://localhost:...` em componentes ou services.

Durante `npm start`, `proxy.conf.json` encaminha o prefixo `/api` ao backend local e remove somente esse prefixo. Exemplo:

```text
GET /api/corretoras  ->  GET /corretoras
```

O endereço absoluto do backend existe somente no proxy de desenvolvimento e não é incorporado ao bundle de produção. Um deploy futuro pode substituir o provider de `API_BASE_URL` ou usar same-origin, conforme sua própria change.

## Estrutura

```text
src/app/
├── core/
│   ├── config/    # configuração injetável da API
│   ├── errors/    # contratos de erro técnicos
│   └── http/      # interceptor e normalização HTTP
├── layout/        # shell e rota técnica da baseline
├── app.component.*
├── app.config.ts
└── app.routes.ts
```

A arquitetura-alvo é organizada em `core/layout/shared/features`. `shared/` e `features/` ainda não existem fisicamente porque não há artefato concreto para versionar nesses diretórios; serão criados junto às primeiras implementações que precisarem deles.

## Contratos OpenAPI

O documento OpenAPI do backend, disponível em `/v3/api-docs` e `/v3/api-docs.yaml`, é a fonte de verdade para services, DTOs e interfaces TypeScript futuros. A baseline atual documenta 18 paths funcionais e 24 operações HTTP.

DTOs futuros serão escritos manualmente e confrontados com o OpenAPI vigente. Esta baseline não contém gerador nem cliente OpenAPI gerado e não copia DTOs de negócio.

## Limites desta baseline

- não contém dashboard ou features de corretoras, ações, carteiras, operações ou indicadores;
- não contém autenticação, JWT, NgRx, Axios, E2E, gráficos ou biblioteca decimal;
- não contém services ou DTOs de domínio;
- não instala Angular Material nem Angular CDK; ambos pertencem à próxima change `criar-layout-navegacao`;
- não integra o frontend ao ciclo de build do Maven.
