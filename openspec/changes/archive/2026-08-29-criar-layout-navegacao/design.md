## Context

Conforme `proposal.md`, a foundation Angular 22.1.4 já fornece bootstrap standalone, strict mode, Router, SCSS, HttpClient e infraestrutura técnica de API/erros. A apresentação atual é um shell mínimo com um `RouterOutlet` e uma rota técnica. Esta change cria a primeira composição visual sem alterar `core/`, o backend ou os contratos de domínio.

A capability `frontend-application-shell` depende conceitualmente de `frontend-application-foundation`: reutiliza o bootstrap, o Router e as políticas npm existentes e estabelece a evolução estrutural da aplicação. A restrição original da foundation vedava corretamente a antecipação das rotas de negócio durante a criação da baseline, mas sua redação absoluta também impediria rotas estruturais posteriores. Por isso esta change inclui uma delta `MODIFIED` que preserva a foundation como infraestrutura inicial, permite evolução por capabilities aprovadas e continua proibindo comportamento funcional antecipado.

## Goals / Non-Goals

**Goals:**

- Estabelecer um shell Material 3 claro, profissional e responsivo.
- Criar navegação estável para cinco áreas, cada uma como limite lazy independente.
- Tornar o shell utilizável por teclado e tecnologias assistivas.
- Preservar instalação determinística e a política estrita de lifecycle scripts.
- Manter componentes e abstrações proporcionais ao comportamento real desta etapa.

**Non-Goals:**

- Implementar dashboard, CRUD de corretoras, ações ou carteiras, ou cadastro/listagem real de operações.
- Consumir backend, criar DTOs/services de domínio, forms ou tabelas de negócio.
- Introduzir indicadores, gráficos, cálculos financeiros, autenticação, guards, NgRx, cliente OpenAPI, biblioteca decimal ou gráfica.
- Implementar tema escuro ou design system avançado.
- Separar toolbar e sidenav em componentes sem reutilização ou responsabilidade independente concreta.

## Decisions

### D0. Relação entre foundation e application shell

`frontend-application-foundation` define exclusivamente a infraestrutura inicial: workspace, bootstrap, routing técnico, HTTP, erros, testes e organização mínima. `frontend-application-shell` é uma evolução estrutural posterior e aprovada, responsável pelas rotas navegáveis e pelos placeholders das áreas.

A delta `MODIFIED` é necessária porque a redação original proibia nominalmente qualquer rota de Dashboard, Corretoras, Ações, Carteiras e Operações. A nova redação mantém essa proibição para antecipação na baseline, mas autoriza capabilities posteriores a introduzirem rotas estruturais explicitamente especificadas. Uma rota estrutural não constitui feature funcional: CRUD, HTTP de domínio, DTOs, services, forms, tabelas, indicadores e cálculos continuam reservados a changes futuras.

Alternativas rejeitadas: ignorar o conflito entre specs ou considerar que a capability mais recente substitui implicitamente a anterior. Ambas deixariam contratos normativos simultâneos e contraditórios.

### D1. Dependências Material alinhadas à foundation

Serão adicionados `@angular/material@22.1.4` e `@angular/cdk@22.1.4` como dependências diretas e exatas. O CDK é direto porque o shell utilizará sua API de layout responsivo. O `packageManager` permanecerá `npm@11.17.0`.

Alternativas rejeitadas: biblioteca visual própria, outra biblioteca de componentes ou faixas de versão. Elas reduziriam alinhamento com a decisão aprovada ou permitiriam drift de versões.

`@angular/animations` não será incluído preventivamente. Só poderá ser adicionado se a integração real do Material 22.1.4 demonstrar necessidade concreta e a alteração for validada contra a baseline; APIs antigas ou depreciadas não justificam sua inclusão.

### D2. Tema Material 3 claro e local

O tema global será configurado em Sass com as APIs públicas do Material 3, densidade padrão e identidade azul-petróleo/teal moderada. Verde e vermelho serão reservados para semântica financeira futura. O design deverá usar tokens públicos do tema em vez de sobrescrever detalhes internos dos componentes Material.

Não haverá fontes, ícones ou assets remotos em runtime. A tipografia usará uma stack local/sistema compatível com boa leitura. Ícones só serão empregados por meio local e sempre acompanhados de texto quando representarem destinos; se isso aumentar desnecessariamente o escopo, a navegação permanecerá textual.

Como referência não normativa, o layout pode iniciar com toolbar próxima de 64px no desktop e 56px no modo compacto, sidenav próxima de 256px e espaçamento de conteúdo entre 16px e 24px. Esses valores poderão ser ajustados sem alterar o contrato.

### D3. MainLayout coeso

O componente raiz comporá um `MainLayoutComponent`, responsável por toolbar, container/sidenav, lista de navegação, controle compacto e `RouterOutlet`. Toolbar e sidenav permanecerão no mesmo componente porque compartilham estado responsivo e ainda não possuem reutilização independente.

Os cinco itens serão descritos por configuração tipada única, evitando duplicação entre template e comportamento. Não será criado `shared/` nesta change. A estrutura planejada é:

```text
src/app/
├── core/
├── layout/
│   ├── main-layout/
│   ├── not-found/
│   └── navigation-items.ts
├── features/
│   ├── dashboard/
│   ├── corretoras/
│   ├── acoes/
│   ├── carteiras/
│   └── operacoes/
├── app.config.ts
└── app.routes.ts
```

Cada diretório novo deverá conter código ou teste com responsabilidade concreta; nenhum diretório vazio será criado.

### D4. Fluxo de navegação e organização lazy

As rotas de nível superior usarão o `MainLayoutComponent` e seus filhos. A raiz fará redirect exato para `/dashboard`. Dashboard, Corretoras, Ações, Carteiras e Operações terão arquivos de rota próprios e serão carregados por limites lazy independentes. Dentro de cada limite, um componente standalone mínimo apresentará somente o título da área.

Fluxo conceitual:

```text
/ -> /dashboard
MainLayout
├── dashboard  -> limite lazy -> placeholder
├── corretoras -> limite lazy -> placeholder
├── acoes      -> limite lazy -> placeholder
├── carteiras  -> limite lazy -> placeholder
├── operacoes  -> limite lazy -> placeholder
└── **         -> NotFound
```

Um placeholder genérico compartilhado foi rejeitado: os cinco componentes pequenos tornam os limites de feature concretos sem criar abstração compartilhada prematura. Eles não farão chamadas HTTP nem terão services, DTOs, forms ou tabelas.

### D5. Wildcard dentro do shell

O wildcard será o último filho do layout e carregará um componente técnico NotFound na área principal. Ele preservará toolbar e navegação, manterá a URL desconhecida e oferecerá retorno explícito a um destino válido. Redirecionar silenciosamente para dashboard foi rejeitado porque mascara URLs inválidas e reduz clareza ao usuário.

### D6. Responsividade por BreakpointObserver e signal

O breakpoint único será 960px:

- `>= 960px`: sidenav em modo `side`, aberta e persistente;
- `< 960px`: sidenav em modo `over`, inicialmente fechada, com botão de menu visível.

`BreakpointObserver` do CDK fornecerá o estado do viewport, convertido por `toSignal` para integração declarativa e limpeza automática, sem subscription manual. Backdrop e Escape usarão o comportamento nativo do sidenav.

No modo compacto, a seleção de um link fechará o drawer. O shell também observará o término bem-sucedido de navegações do Router para fechá-lo após navegação programática. A reação será condicionada ao modo compacto para não recolher a navegação desktop.

Alternativas rejeitadas: múltiplos breakpoints sem comportamento distinto, listeners manuais de resize e estado global, por aumentarem complexidade sem benefício nesta etapa.

### D7. Navegação ativa e acessibilidade

Os links usarão integração com Router e estado ativo. A página atual será comunicada por `aria-current`, e o destaque combinará mais de um sinal visual, como fundo, peso ou marcador, sem depender apenas da cor.

O shell incluirá:

- navegação com nome acessível;
- botão compacto com nome, estado expandido e associação ao drawer;
- foco visível com contraste apropriado;
- skip link para uma área principal focável e identificável;
- um `h1` em cada placeholder e no NotFound;
- links e controles utilizáveis por teclado e com alvo interativo adequado;
- ícones, caso existam, acompanhados de texto ou com semântica corretamente ocultada quando decorativos.

### D8. Estratégia de testes

Testes de componente e integração com Router cobrirão criação do layout, título, cinco links/URLs, redirect raiz, lazy loading, wildcard dentro do shell, estado ativo, modos desktop/compacto, toggle, fechamento após seleção e navegação programática, semântica acessível e ausência de HTTP nos placeholders.

Harnesses do Angular Material serão usados apenas quando fornecerem API pública mais estável que consultas ao DOM interno, especialmente para sidenav e controles. Testes de texto, Router e atributos semânticos permanecerão orientados ao comportamento observável.

### D9. Política npm e instalação controlada

A política `allowScripts` existente será preservada, bem como `strict-allow-scripts=true`. A instalação não aprovará automaticamente Material, CDK ou dependências transitivas. Após alterar dependências, a implementação deverá revisar `package.json` e `package-lock.json`, listar lifecycle scripts pendentes e interromper diante de qualquer `ESTRICTALLOWSCRIPTS` ainda não analisado.

Somente depois dessa verificação será executado `npm ci`, seguido de testes e build. O lockfile poderá mudar apenas pelo grafo legítimo das novas dependências; versões, `resolved`, `integrity` e scripts deverão ser revisados contra drift inesperado.

## Risks / Trade-offs

- [Material elevar o bundle ou exceder budgets] → Importar somente os módulos usados, executar build de produção e revisar budgets sem ampliá-los automaticamente.
- [Schematic adicionar fonte, ícone ou configuração remota] → Revisar integralmente o diff e manter somente recursos locais; preferir configuração manual controlada quando necessário.
- [Nova dependência possuir lifecycle script] → Manter strict policy, listar scripts pendentes e parar para análise individual antes de alterar `allowScripts`.
- [Estado do drawer divergir entre clique e navegação programática] → Centralizar o fechamento no término da navegação em modo compacto e cobrir ambos os fluxos por teste.
- [Testes frágeis por detalhes internos do Material] → Preferir RouterTestingHarness, atributos públicos e Material harnesses quando reduzirem acoplamento.
- [Placeholders parecerem funcionalidades prontas] → Limitar conteúdo a identificação estrutural, sem dados fictícios, HTTP, forms, tabelas ou ações de negócio.
- [Baixo contraste ou foco imperceptível no tema customizado] → Validar contraste e navegação por teclado, usando tokens e indicadores oficiais do Material.

## Migration Plan

1. Confirmar novamente a foundation, branch e estado do repositório.
2. Adicionar Material/CDK exatos e revisar manifesto, lockfile, recursos gerados e scripts npm.
3. Configurar tema global e compor o novo shell sobre o bootstrap existente.
4. Substituir a rota técnica por rotas filhas lazy, placeholders e NotFound.
5. Adicionar comportamento responsivo e acessibilidade.
6. Executar instalação limpa, testes, build, budgets e validações OpenSpec/Git.

Rollback: remover as dependências e arquivos introduzidos pela change e restaurar a composição/rota técnica da foundation. Nenhuma migração de dados ou alteração de backend é necessária.
