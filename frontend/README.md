# Frontend — Gestão de Ações

Interface Angular do MVP com dashboard, indicadores e gráficos por moeda, histórico patrimonial, carteiras, ações, corretoras e registro/consulta de operações. O guia completo está no [README raiz](../README.md).

## Requisitos e comandos

Angular/CLI 22.1.4, Angular Material/CDK, TypeScript estrito, componentes standalone, RxJS e SCSS. Use Node `^22.22.3 || ^24.15.0 || ^26.0.0` e npm 11.17.0 conforme [package.json](package.json). Não é necessária CLI global.

Dentro de `frontend/`:

```sh
npm ci
npm start
npm test -- --watch=false
npm run build
```

Abra `http://localhost:4200` com backend em `http://localhost:8080`. `npm ci` usa o lockfile; `npm install` é reservado à manutenção intencional das dependências. Testes usam Vitest/jsdom. O build gera `dist/frontend/browser/` e atualmente apresenta avisos de tamanho do bundle e CSS de evolução.

## API

O provider central de [API_BASE_URL](src/app/core/config/api.config.ts) usa `/api`. No desenvolvimento, [proxy.conf.json](proxy.conf.json) encaminha `/api/corretoras` para `http://localhost:8080/corretoras` e remove o prefixo. Não existem chaves de providers no frontend.

O proxy não acompanha o build de produção. A hospedagem deve encaminhar `/api` ao backend e tratar rotas Angular, ou adaptar o provider central de URL. O build frontend é independente do Maven; não há deploy automatizado nesta entrega.

OpenAPI do backend: `/v3/api-docs`, `/v3/api-docs.yaml` e `/swagger-ui.html`. A [coleção Postman](../docs/api/gestao-acoes.postman_collection.json) usa a URL direta do backend.

## Organização

```text
src/app/
├── core/       configuração HTTP, erros e contexto de carteira
├── layout/     shell e navegação responsiva
├── shared/     componentes, feedback e formatação reutilizáveis
└── features/   dashboard, carteiras, acoes, corretoras e operacoes
```

Os serviços consomem DTOs backend. Cálculos de posição, preço médio e resultados permanecem no backend. A interface formata dados e constrói a geometria dos gráficos; não converte moedas nem implementa o cálculo contábil oficial.

O contexto de carteira orienta dashboard e operações. Atualização de cotação e criação de snapshot são manuais. Compra e venda recebem preço efetivo informado pelo usuário; prévia histórica e sugestão de venda são consultas independentes. Não há autenticação nem envio de ordens para corretoras.
