## Context

Veja `proposal.md` para a motivação e `specs/frontend-application-foundation/spec.md` para o contrato. Não existe `frontend/` no repositório; o backend Spring Boot e sua OpenAPI estão concluídos e permanecem fora da área de alteração.

O ambiente informado possui Node `24.19.0`, npm `11.17.0` e Angular CLI global `22.1.4`, mas a CLI global falhou ao detectar npm. A matriz oficial do Angular 22 aceita Node `^22.22.3 || ^24.15.0 || ^26.0.0`; assim, Node 24.19.0 é compatível. A versão estável confirmada para framework e CLI é `22.1.4`, e o Angular recomenda alinhar as versões de core e CLI.

O backend publica OpenAPI code-first em `/v3/api-docs` e `/v3/api-docs.yaml`, com 24 operações em 18 paths funcionais. Seu `StandardError` público contém `timeStamp`, `status`, `error`, `message`, `path`, `code` e `details`.

## Goals / Non-Goals

**Goals:**

- tornar a criação futura de `frontend/` determinística e executável somente por ferramentas locais do projeto;
- estabelecer limites claros entre configuração transversal, layout e features;
- deixar routing, HttpClient, API base URL, proxy e erros técnicos testáveis antes das features;
- definir validações mínimas de instalação, testes e build.

**Non-Goals:**

- desenhar ou implementar telas e services de negócio;
- instalar Angular Material sem um componente que o utilize;
- gerar cliente OpenAPI, duplicar DTOs de domínio ou integrar um backend real nos testes;
- adicionar autenticação, JWT, Axios, NgRx, biblioteca decimal, gráficos, E2E ou regras financeiras;
- alterar qualquer arquivo do backend ou integrar seu build ao Maven.

## Decisions

### D1. Capability

Usar `frontend-application-foundation`. O nome descreve a fundação completa e verificável da aplicação, incluindo build e comunicação técnica, sem reduzir o contrato somente a runtime. `frontend-runtime-baseline` foi considerado, mas omitiria aspectos essenciais de workspace, testes e tooling.

### D2. Versões e package manager

Fixar `@angular/core` e os demais pacotes Angular, além de `@angular/cli`, em `22.1.4` no `package.json`, sem intervalos `^` ou `~` para a baseline. Usar npm `11.17.0` e versionar o `package-lock.json`; registrar `engines.node` compatível com a matriz do Angular 22 e `packageManager` para tornar o contrato explícito.

A implementação deverá invocar a CLI 22.1.4 de forma local e explícita, por exemplo via `npx @angular/cli@22.1.4 new frontend ...` a partir da raiz, ou procedimento equivalente que produza o mesmo workspace. Depois da criação, scripts npm serão a interface normal. A CLI global não integra o contrato.

### D3. Opções do workspace

Gerar uma única aplicação standalone com routing, strict mode, SCSS, Git desabilitado no gerador e package manager npm. O workspace ficará diretamente em `frontend/`; não haverá monorepo Angular vazio nem biblioteca interna nesta fase.

O resultado futuro deverá incluir ao menos `package.json`, `package-lock.json`, `angular.json`, configurações TypeScript, `src/main.ts`, `src/index.html`, estilos SCSS, configuração de app/rotas e testes gerados ou ajustados. Nenhum `frontend/` é criado durante o planejamento.

### D4. Build e desenvolvimento

Manter os builders e budgets padrão do Angular CLI 22.1.4, alterando somente o necessário para associar o proxy ao target de desenvolvimento. Scripts esperados:

- `npm start` para servir a aplicação com a configuração de desenvolvimento e proxy;
- `npm test -- --watch=false` para validação unitária única em CI/revisão;
- `npm run build` para build de produção;
- opcionalmente `npm run ng -- <comando>` para acesso explícito à CLI local.

Não adicionar ESLint, Prettier ou outro lint/formatter nesta change, pois não fazem parte obrigatória da baseline escolhida e introduziriam decisões independentes. A formatação seguirá os defaults gerados e as convenções Angular até uma change dedicada, se necessária.

### D5. Bootstrap e routing

Manter `app.config.ts` como composição dos providers e `app.routes.ts` como fonte única das rotas. O componente raiz será standalone e conterá apenas o outlet/composição mínima necessária ao bootstrap. A rota inicial será técnica e sem dados de negócio; um fallback central poderá redirecionar para ela. Não criar páginas de dashboard ou navegação definitiva.

### D6. Estrutura inicial de `src/app`

Planejar a seguinte árvore concreta; diretórios marcados como futuros não serão criados vazios:

```text
frontend/src/app/
├── core/
│   ├── config/
│   │   ├── api-base-url.token.ts
│   │   └── api.config.ts
│   ├── errors/
│   │   ├── standard-error.ts
│   │   └── normalized-http-error.ts
│   └── http/
│       ├── http-error.interceptor.ts
│       └── http-error-normalizer.ts
├── layout/
│   └── app-shell.component.ts
├── app.component.ts
├── app.component.scss
├── app.component.html
├── app.config.ts
└── app.routes.ts
```

`shared/` e `features/` são convenções aprovadas, mas serão criados apenas quando a primeira implementação concreta exigir conteúdo. O README documentará a estrutura-alvo `core/layout/shared/features`. Isso evita placeholders como `.gitkeep` e preserva a arquitetura por features.

### D7. API base URL e proxy

Definir um `InjectionToken<string>` (nome conceitual `API_BASE_URL`) fornecido centralmente por `app.config.ts`. A baseline usará um prefixo relativo, recomendado `/api`, para que services futuros componham URLs sem origem hardcoded. Um arquivo `proxy.conf.json` na raiz do frontend encaminhará `/api` para o backend local e poderá remover o prefixo se os endpoints reais do backend não o possuírem.

O endereço do backend local ficará somente no proxy de desenvolvimento. Configurações de deploy poderão substituir o provider ou usar same-origin em change própria; não serão inventados ambientes de produção agora.

### D8. HttpClient

Registrar `provideHttpClient(withInterceptors([...]))` no bootstrap. Não criar services de domínio ou wrappers genéricos sobre HttpClient. A alternativa de Axios foi rejeitada porque HttpClient é nativo, integra DI/interceptors/testes Angular e já foi aprovado.

### D9. StandardError e normalização

Incluir nesta change um tipo TypeScript manual mínimo correspondente ao schema OpenAPI de `StandardError`, um erro técnico normalizado e funções puras de detecção/normalização. Um interceptor funcional preservará `StandardError` válido e converterá falhas de rede ou corpos desconhecidos para formato previsível antes de repassar o erro.

Isso pertence à baseline porque todas as features consumirão a mesma API e porque o interceptor pode ser testado sem regra de negócio. Não haverá catálogo de mensagens, toast, tradução, tratamento visual ou códigos específicos por feature. O OpenAPI, não a classe Java, será a referência contratual futura.

### D10. Angular Material

Escolher a opção B: instalar Angular Material somente na próxima change `criar-layout-navegacao`. Nesta change não existe componente visual real que justifique tema, tipografia, animações ou imports Material. Adiar mantém a fundação pequena e permite validar instalação, tema e acessibilidade junto ao primeiro shell de navegação definitivo, evitando configuração ociosa ou retrabalho.

### D11. Testes

Usar Vitest, runner padrão de novos projetos Angular 22, com jsdom e integração do Angular CLI. Planejar testes pequenos para:

- criação do componente raiz e bootstrap dos providers;
- resolução da rota inicial e fallback;
- disponibilidade de HttpClient sem rede real;
- valor padrão e override de `API_BASE_URL`;
- preservação de um `StandardError` válido pelo normalizador/interceptor;
- normalização de falha de rede e corpo desconhecido;
- build de produção como validação separada da suíte.

Usar utilitários HTTP de teste do Angular e providers substituíveis. Não iniciar o backend, chamar rede real ou criar testes de domínio/E2E.

### D12. OpenAPI e DTOs

Registrar no README do frontend que `/v3/api-docs` ou `/v3/api-docs.yaml` é a fonte de verdade e que os 18 paths/24 operações formam a baseline atual. DTOs/interfaces futuros serão escritos manualmente e confrontados com esse documento. Não adicionar gerador, artefato gerado ou cópia completa do OpenAPI ao frontend nesta change.

### D13. Documentação

Criar `frontend/README.md` com pré-requisitos, versões fixadas, comandos npm, proxy, API base URL, árvore arquitetural e limites da baseline. O README deverá afirmar que Angular Material entra na change seguinte e que `shared/`/`features/` surgem com conteúdo real.

## Risks / Trade-offs

- [Angular 22.1.4 é recente e pode receber patches] → Fixar exatamente 22.1.4 nesta change; upgrades terão validação própria e não ocorrerão implicitamente pelo lockfile.
- [Node 24.19.0 é compatível hoje, mas ambientes podem usar versões fora da matriz] → Documentar `engines.node`, validar versão antes da implementação e preferir uma linha LTS suportada em CI.
- [A inconsistência da CLI global pode reaparecer] → Nunca depender dela; invocar versão local explícita e scripts npm.
- [Prefixo `/api` não existe nos paths reais] → Configurar rewrite apenas no proxy de desenvolvimento e testar a composição; manter o token independente do target.
- [Interceptor prematuro pode esconder detalhes úteis] → Preservar payload público válido, manter normalização pura e testada, e não traduzir mensagens nesta fase.
- [Tipo manual `StandardError` pode divergir] → Mantê-lo mínimo e confrontá-lo com OpenAPI; alterações contratuais futuras exigem revisão do tipo.
- [Adiar Material exige uma instalação posterior] → A change de layout assumirá explicitamente dependência, tema e testes visuais no momento de uso.
- [Diretórios arquiteturais ausentes podem surpreender] → Documentar a estrutura-alvo e criar `shared/`/`features/` somente junto ao primeiro artefato concreto.

## Migration Plan

Não há migração de dados ou deploy nesta baseline. A implementação futura criará `frontend/` isoladamente, validará instalação determinística, testes e build, e atualizará Graphify após o código. O rollback consiste em remover somente a nova árvore `frontend/`, sem tocar no backend.

## Open Questions

Nenhuma decisão material permanece pendente. A versão 22.1.4, Vitest, npm, tratamento técnico de erros e adiamento do Angular Material estão definidos para esta change.
