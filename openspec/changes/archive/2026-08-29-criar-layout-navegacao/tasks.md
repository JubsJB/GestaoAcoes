## 1. Pré-validação

- [x] 1.1 Confirmar branch, status inicial do Git e ausência de alterações prévias em frontend e backend
- [x] 1.2 Reler proposal, design, as duas delta specs e a capability principal `frontend-application-foundation`
- [x] 1.3 Confirmar versões e configurações da baseline Angular antes de iniciar a implementação

## 2. Dependências Angular Material e CDK

- [x] 2.1 Adicionar `@angular/material@22.1.4` e `@angular/cdk@22.1.4` como dependências diretas e exatas
- [x] 2.2 Preservar `packageManager` como `npm@11.17.0` e as demais versões da baseline
- [x] 2.3 Não adicionar `@angular/animations` sem necessidade concreta demonstrada pelo Material 22.1.4
- [x] 2.4 Revisar conjuntamente os diffs de `package.json` e `package-lock.json`, incluindo árvore, versões, `resolved` e `integrity`
- [x] 2.5 Confirmar ausência de dependências visuais paralelas e de fontes, ícones ou assets remotos adicionados pela instalação

## 3. Política npm e lockfile

- [x] 3.1 Preservar integralmente `allowScripts`, `strict-allow-scripts=true` e as aprovações/negações existentes
- [x] 3.2 Consultar em modo read-only os lifecycle/install scripts pendentes após adicionar Material e CDK
- [x] 3.3 Interromper diante de qualquer `ESTRICTALLOWSCRIPTS` não analisado, sem ampliar a política automaticamente
- [x] 3.4 Executar `npm ci` sem flags de contorno e confirmar instalação reproduzível
- [x] 3.5 Confirmar após `npm ci` que manifesto e lockfile não sofreram drift inesperado

## 4. Tema global Material 3

- [x] 4.1 Configurar tema Material 3 claro em Sass usando somente APIs públicas suportadas
- [x] 4.2 Aplicar identidade azul-petróleo/teal e reservar verde/vermelho para semântica financeira
- [x] 4.3 Configurar tipografia local/sistema e garantir ausência de requisições a provedores visuais externos
- [x] 4.4 Aplicar estilos globais de superfície, texto, foco e responsividade usando tokens públicos do tema

## 5. Shell principal

- [x] 5.1 Criar o `MainLayoutComponent` standalone e coeso no diretório de layout
- [x] 5.2 Compor toolbar, sidenav container, sidenav, área principal e `RouterOutlet` no layout
- [x] 5.3 Exibir “Gestão de Ações” na toolbar e integrar o componente raiz ao novo shell
- [x] 5.4 Remover somente o shell e o estado técnico substituídos pela nova composição
- [x] 5.5 Confirmar que toolbar e sidenav permanecem no mesmo componente e que nenhum diretório vazio ou abstração prematura foi criado

## 6. Navegação principal

- [x] 6.1 Criar configuração tipada com Dashboard, Corretoras, Ações, Carteiras e Operações e suas URLs aprovadas
- [x] 6.2 Renderizar os cinco destinos principais com Angular Router e componentes Material adequados
- [x] 6.3 Configurar estado ativo perceptível por mais de um sinal visual
- [x] 6.4 Comunicar a página atual por `aria-current` ou mecanismo público equivalente

## 7. Responsividade

- [x] 7.1 Integrar `BreakpointObserver` ao estado responsivo com signal/`toSignal` e breakpoint único de 960px
- [x] 7.2 Configurar sidenav aberta e persistente em modo `side` para viewport igual ou superior a 960px
- [x] 7.3 Configurar drawer `over`, inicialmente fechado, para viewport inferior a 960px
- [x] 7.4 Implementar controle acessível de abertura e fechamento no modo compacto
- [x] 7.5 Confirmar fechamento compacto por backdrop, Escape e seleção de destino
- [x] 7.6 Fechar o drawer após navegação programática concluída sem alterar o estado desktop

## 8. Rotas lazy e placeholders

- [x] 8.1 Configurar o `MainLayoutComponent` como shell e o redirect exato `/` → `/dashboard`
- [x] 8.2 Criar limites lazy independentes para Dashboard, Corretoras, Ações, Carteiras e Operações
- [x] 8.3 Criar os cinco placeholders standalone mínimos dentro dos respectivos limites
- [x] 8.4 Garantir `h1` coerente e conteúdo estritamente estrutural em cada placeholder
- [x] 8.5 Confirmar ausência de HTTP, dados fictícios, forms, tabelas, services, DTOs e regras de negócio nos placeholders

## 9. Rota não encontrada

- [x] 9.1 Criar componente técnico NotFound com `h1` e retorno explícito a um destino válido
- [x] 9.2 Configurar o wildcard como última rota filha, preservando shell e URL desconhecida
- [x] 9.3 Confirmar ausência de redirect silencioso de rota desconhecida para dashboard

## 10. Acessibilidade

- [x] 10.1 Adicionar skip link funcional para a área principal identificável e focável
- [x] 10.2 Fornecer nome acessível à navegação e nome/estado/associação ao controle do drawer
- [x] 10.3 Garantir operação por teclado, fechamento por Escape e ordem de foco coerente
- [x] 10.4 Garantir foco visível, contraste adequado e estado ativo não dependente somente de cor
- [x] 10.5 Garantir alvos interativos adequados e semântica correta para eventuais ícones locais

## 11. Testes automatizados

- [x] 11.1 Testar criação do `MainLayoutComponent`, estrutura do shell e título da toolbar
- [x] 11.2 Testar os cinco links principais, rótulos, URLs e indicação ativa semântica/visual
- [x] 11.3 Testar redirect exato da raiz para dashboard
- [x] 11.4 Testar que os cinco destinos resolvem seus limites lazy e placeholders correspondentes
- [x] 11.5 Testar sidenav persistente no desktop e drawer inicialmente fechado no modo compacto
- [x] 11.6 Testar toggle e fechamento compacto por seleção de link
- [x] 11.7 Testar fechamento compacto após navegação programática
- [x] 11.8 Testar wildcard, NotFound dentro do shell e preservação da URL desconhecida
- [x] 11.9 Testar skip link, navegação por teclado, foco e atributos ARIA, usando Material harnesses quando reduzirem acoplamento ao DOM interno
- [x] 11.10 Testar que a renderização dos placeholders não realiza chamadas HTTP

## 12. Build e budgets

- [x] 12.1 Executar a suíte Vitest em modo não interativo e registrar testes, failures, errors e skipped
- [x] 12.2 Executar o build de produção com a CLI local
- [x] 12.3 Revisar bundles e confirmar budgets existentes sem ampliá-los automaticamente

## 13. OpenSpec e revisão final

- [x] 13.1 Confirmar versões efetivas, política npm, ausência de recursos remotos, features de negócio e dependências proibidas
- [x] 13.2 Confirmar por diff que frontend contém apenas o escopo planejado e que backend não foi alterado
- [x] 13.3 Executar validação strict da change e validação global strict
- [x] 13.4 Atualizar o Graphify e revisar os relacionamentos arquiteturais do novo shell
- [x] 13.5 Executar `git diff --check` e revisar todos os arquivos novos/modificados contra o escopo antes de concluir
