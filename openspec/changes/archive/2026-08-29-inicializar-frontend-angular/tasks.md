## 1. Inicialização Angular

- [x] 1.1 Confirmar que a implementação parte da branch aprovada, que `frontend/` ainda não existe e que Node/npm satisfazem a baseline registrada.
- [x] 1.2 Criar `frontend/` com a Angular CLI local explícita `22.1.4`, usando standalone, strict mode, routing, SCSS, npm e sem inicialização Git interna.
- [x] 1.3 Verificar que `package.json` fixa Angular e Angular CLI em `22.1.4`, declara Node compatível e não contém Angular Material, Axios, NgRx, gerador OpenAPI, biblioteca decimal, gráficos ou E2E.
- [x] 1.4 Gerar e versionar `package-lock.json`, comprovando que a CLI global não integra os scripts nem o contrato do workspace.

## 2. Configuração TypeScript e build

- [x] 2.1 Revisar `tsconfig.json` e configurações derivadas para manter strict TypeScript e strict Angular template checking habilitados.
- [x] 2.2 Revisar `angular.json` para manter builders, SCSS, budgets e configurações de desenvolvimento/produção coerentes com os defaults do Angular CLI 22.1.4.
- [x] 2.3 Definir scripts npm para `start`, `test`, `build` e acesso à CLI local, sem adicionar lint ou formatter não aprovados.
- [x] 2.4 Confirmar que o workspace Angular e seus outputs permanecem isolados do Maven e que nenhum arquivo do backend é referenciado como fonte do build frontend.

## 3. Routing e bootstrap

- [x] 3.1 Manter o componente raiz standalone e remover conteúdo demonstrativo gerado que não tenha função na baseline.
- [x] 3.2 Configurar `app.config.ts` como composição central de providers da aplicação.
- [x] 3.3 Configurar `app.routes.ts` com rota inicial técnica e fallback central, sem rotas ou telas de negócio.
- [x] 3.4 Criar shell mínimo em `layout/` somente para hospedar o outlet e validar o bootstrap, sem navegação definitiva ou Angular Material.

## 4. Configuração HTTP e API

- [x] 4.1 Criar em `core/config/` o token injetável da URL base e seu provider padrão com prefixo relativo `/api`.
- [x] 4.2 Registrar `provideHttpClient` e o interceptor funcional em `app.config.ts`.
- [x] 4.3 Criar `proxy.conf.json` para encaminhar `/api` ao backend local, com rewrite compatível com os paths reais e uso restrito ao servidor de desenvolvimento.
- [x] 4.4 Associar o proxy ao script/target de desenvolvimento e confirmar por inspeção que o endereço local não entra no build de produção.
- [x] 4.5 Confirmar que nenhuma origem `localhost` foi hardcoded em fontes TypeScript e que não foi criado service de negócio ou wrapper HTTP genérico.

## 5. Estrutura arquitetural e erros técnicos

- [x] 5.1 Criar somente `core/config`, `core/errors`, `core/http` e `layout` com arquivos de responsabilidade concreta, sem `.gitkeep` ou diretórios vazios.
- [x] 5.2 Criar a interface mínima `StandardError` confrontada com o schema OpenAPI vigente e seus sete campos públicos.
- [x] 5.3 Criar a representação de erro técnico normalizado e a função pura que distingue payload padronizado de falha desconhecida.
- [x] 5.4 Implementar interceptor que preserve `StandardError` válido e normalize falhas de rede/corpos inválidos sem mensagens específicas de features.
- [x] 5.5 Confirmar que `shared/` e `features/` não foram criados vazios e que nenhum DTO de domínio, autenticação ou estado global foi introduzido.

## 6. Configuração de testes

- [x] 6.1 Manter Vitest e jsdom como configuração de testes da baseline gerada pelo Angular CLI 22.1.4.
- [x] 6.2 Testar criação do componente raiz, providers de bootstrap e disponibilidade do HttpClient sem rede real.
- [x] 6.3 Testar resolução da rota inicial e o fallback para rota desconhecida.
- [x] 6.4 Testar o valor padrão e a substituição de `API_BASE_URL` por provider de teste.
- [x] 6.5 Testar preservação de `StandardError` válido e normalização de falha de rede/corpo desconhecido pelo interceptor.

## 7. Validação

- [x] 7.1 Executar instalação determinística limpa com `npm ci` e registrar que `npm install` permanece o comando esperado para manutenção intencional do lockfile.
- [x] 7.2 Executar `npm test -- --watch=false` e registrar arquivos/testes aprovados e exit code.
- [x] 7.3 Executar `npm run build` e registrar resultado, budgets e exit code.
- [x] 7.4 Executar uma inicialização de desenvolvimento controlada por `npm start` apenas para validar bootstrap, routing e carregamento do proxy, encerrando o processo após a verificação.
- [x] 7.5 Executar `openspec validate inicializar-frontend-angular --strict` e revisar que nenhuma feature de negócio ou alteração de backend entrou no diff.
- [x] 7.6 Atualizar o grafo com `graphify update .` após as alterações de código e verificar o status final do repositório sem realizar commit.

## 8. Documentação

- [x] 8.1 Criar `frontend/README.md` com Node/npm/Angular fixados, pré-requisitos e comandos `npm install`, `npm ci`, `npm start`, `npm test -- --watch=false` e `npm run build`.
- [x] 8.2 Documentar API base URL, proxy de desenvolvimento, ausência de `localhost` em services e estratégia de configuração para ambientes futuros.
- [x] 8.3 Documentar a estrutura-alvo `core/layout/shared/features`, esclarecendo por que `shared/` e `features/` ainda não existem fisicamente.
- [x] 8.4 Registrar OpenAPI como fonte de verdade dos 18 paths e 24 operações, DTOs manuais futuros, ausência de cliente gerado e adiamento do Angular Material para `criar-layout-navegacao`.

## 9. Política de scripts de instalação npm

- [x] 9.1 Registrar em `package.json` a política `allowScripts`, aprovando somente `esbuild@0.28.2` e negando `@parcel/watcher`, `lmdb` e `msgpackr-extract`.
- [x] 9.2 Habilitar `strict-allow-scripts=true` em `frontend/.npmrc` sem permitir scripts de forma ampla.
- [x] 9.3 Confirmar por consulta read-only que não restam scripts de instalação pendentes de revisão.
- [x] 9.4 Executar instalação determinística limpa com `npm ci` sob a política estrita.
- [x] 9.5 Executar `npm test -- --watch=false` após a instalação limpa.
- [x] 9.6 Executar `npm run build` após a instalação limpa e confirmar os budgets.
- [x] 9.7 Confirmar que a validação da política não produziu drift inesperado em `package-lock.json`.
