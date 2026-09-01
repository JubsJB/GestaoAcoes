## Context

Veja `proposal.md` para a motivação. O frontend usa Angular Material 22.1.4/Material 3, tema claro com `mat.$cyan-palette` e `mat.$azure-palette`, system fonts, density 0 e strong focus indicators. O shell já possui toolbar, sidenav responsivo em 960px, skip link, `aria-current` e lazy boundaries; Corretoras e Ações repetem page headers, grids, estados, mensagens e ações em estilos locais. Ações formata data com `Intl.DateTimeFormat('pt-BR')`, enquanto Corretoras usa `DatePipe:'medium'` sem locale configurado.

As specs desta change definem a experiência transversal e modificam somente shell, Corretoras e Ações. `frontend-application-foundation`, contratos HTTP, models de domínio e backend permanecem intactos.

## Goals / Non-Goals

**Goals:**

- Aplicar identidade oliva por tokens Material e tokens semânticos próprios, sem confundir marca com finanças.
- Reduzir duplicação real com `PageHeader`, `FeedbackAlert`, formatter temporal e poucos padrões estruturais.
- Melhorar busca sem correspondência e transição busca→cadastro sem persistência ou nova fonte de estado.
- Preservar acessibilidade, breakpoints estruturais, Typed Reactive Forms, `StandardError` e comportamento HTTP.
- Manter crescimento de CSS e bundle observável dentro dos budgets existentes.

**Non-Goals:**

- Criar componentes genéricos para todo card, formulário, detalhe, loading ou estado de resultado.
- Alterar regras de domínio, endpoints, DTOs, providers, confirmação excepcional ou semântica temporal do backend.
- Introduzir tema escuro, biblioteca visual, pacote de ícones, fonte remota, store global ou persistência de preferências.
- Tornar Dashboard, Carteiras ou Operações funcionais.

## Decisions

### D1. Material 3 com paleta oliva e tokens semânticos

O tema continuará sendo composto pelo Sass do Angular Material, substituindo cyan/azure por uma paleta compatível derivada da referência aprovada. Tokens Material serão a fonte para componentes Material; tokens de aplicação nomearão papéis não cobertos ou que precisam permanecer semanticamente independentes.

Referência de papéis:

- `brand-primary`: `#5F6F52`; `on-brand-primary`: branco;
- `brand-secondary`: `#A9B388`; texto grafite;
- `surface-app`: `#F8F8F3`; `surface-card`: branco;
- `surface-selected`: sage claro derivado de `#A9B388`;
- `accent`: `#B99470`; `warning`: `#C4661F` quando a combinação atender contraste;
- `text-primary`: grafite próximo de `#1F241D`; `text-secondary`: grafite/cinza de menor ênfase;
- `success`, `financial-positive`, `negative`, `error` e `warning` serão tokens distintos.

`#783D19` pode apoiar detalhes quentes, mas não será imposto como erro. `#A9B388` e `#B99470` não usarão texto branco em combinações insuficientes. Informação semântica sempre terá texto, estrutura, ícone local simples ou atributo assistivo além da cor.

Alternativas rejeitadas: manter cyan e aplicar olive apenas no shell, por gerar dois sistemas; hardcode de hex em cada componente, por duplicação; segunda biblioteca/theme package, por custo e ausência de necessidade.

### D2. Arquitetura SCSS em quatro níveis

1. `styles.scss` permanece ponto de composição, reset mínimo e aplicação do tema.
2. Partials globais pequenos concentram definição de tema/tokens e padrões estruturais transversais, por exemplo `styles/_theme.scss`, `_tokens.scss` e `_patterns.scss`.
3. `shared/` contém estilos encapsulados de `PageHeader` e `FeedbackAlert`.
4. Features mantêm grid, conteúdo de cards, formulários e detalhes específicos.

Os padrões globais se limitarão a container/page surface, ritmo, formulário surface e estados estruturais quando realmente repetidos. Não serão usados `::ng-deep`, seletores internos frágeis do Material ou `!important`; qualquer override inevitável deverá ser localizado e justificado.

Alternativas rejeitadas: `styles.scss` monolítico; copiar os mesmos blocos para Corretoras e Ações; criar utility framework próprio.

### D3. Geometria concreta e container

O container principal terá `max-width: 76rem`, margem inline automática, padding de `2rem` em desktop e `1rem` em compacto. O shell mantém sidebar de `16rem`; toolbar usa aproximadamente `4rem` no desktop e `3.5rem` no compacto. Form surfaces usarão `max-width: 42rem`.

O shell ocupará a viewport como uma estrutura de aplicação. No desktop, toolbar e sidebar permanecem estruturalmente estáveis e a área de trabalho usa o espaço restante; no compacto, a toolbar permanece estável e o drawer continua sobreposto. A composição será feita pela geometria do `MatSidenavContainer`, sidenav e content, não por `position: fixed` isolado. O conteúdo respeitará a altura da toolbar, sem scroll horizontal nem dois scrolls concorrentes.

Páginas de coleção separarão cabeçalho/contexto, controles de busca/ações e região de registros. Quando a coleção exceder a altura disponível, a região de resultados será a área principal de rolagem, preservando cabeçalho e controles quando isso não gerar scroll duplo nem reduzir a usabilidade em viewport compacto. Não haverá virtual scrolling ou paginação sem contrato backend.

Cards usarão raio de `0.875rem` (14px), borda por token outline-variant, elevação baixa equivalente ao nível 1 e espaçamento interno coerente. Hover será aplicado apenas ao elemento que realmente aciona navegação ou ação.

Esses valores são decisões de implementação, não contratos pixel-perfect; specs protegem a relação estrutural e a usabilidade.

### D4. Toolbar e sidebar preservam comportamento

A toolbar passa a usar brand-primary, conteúdo claro, sombra discreta e alinhamento visual com o container, mantendo título e botão mobile. A sidebar usa superfície neutra; item ativo usa sage claro, peso de texto, marcador lateral e `aria-current`. Hover e foco não competem com o ativo.

Cada destino usa o sprite SVG local já aprovado com o ícone decorativo antes do label, em uma coluna visual de largura consistente, alinhamento central e espaçamento uniforme. O texto continua sendo o nome acessível; o SVG permanece oculto de tecnologia assistiva para não duplicá-lo. A mesma composição vale no desktop e no drawer compacto, sem Material Icons font ou asset remoto.

O breakpoint `960px`, modos `side/over`, estado inicial, Escape, backdrop e fechamento após navegação permanecem inalterados. Alterar largura ou fluxo do drawer foi rejeitado por não haver necessidade funcional.

### D5. `PageHeader` compartilhado e sem domínio

Um componente standalone em `shared/` recebe título, descrição opcional e conteúdo projetado para ação. Ele renderiza exatamente um `h1`, não conhece rotas, services ou DTOs e oferece layout desktop/compacto. Nomes extensos quebram naturalmente, com `clamp()` moderado para tamanho e line-height legível, sem ellipsis obrigatório.

Feedback permanece como irmão imediatamente posterior na página. Embuti-lo no header foi rejeitado porque resultado de busca pertence à área de resultados e field errors pertencem ao campo.

### D6. `FeedbackAlert` reutiliza o erro existente

Um componente standalone aceita variante `success|info|warning|error`, mensagem e details opcionais. A variante define tratamento visual e defaults semânticos; o chamador poderá escolher urgência quando o contexto exigir. Erro padronizado continuará sendo `NormalizedHttpError`/`StandardError`; o componente apenas apresenta conteúdo recebido e não normaliza nem cria contrato paralelo.

`error` e warning urgente usam `role="alert"`; success/info ordinários usam `role="status"` e `aria-live="polite"`. Field validation continua em `mat-error`. Sucessos transitórios de operações usam a infraestrutura Material existente como toast leve, com texto curto, ícone semântico, fechamento opcional e descarte automático próximo de dez segundos (`10000 ms`). O toast fica no canto superior direito da área visível no desktop e próximo ao topo, com margens laterais seguras, no mobile, sem cobrir toolbar ou conteúdo essencial. Ele não substitui feedback persistente relevante nem recebe erros técnicos ou de provider.

Alternativa rejeitada: um componente genérico que também controla retry, navegação e regras de negócio.

### D7. Estados visuais por significado

Coleção vazia, busca sem resultado, erro técnico e erro externo terão estruturas/mensagens distintas. Loading e empty inicialmente reutilizam classes/padrões estruturais; não viram componentes até surgir API realmente estável. A ausência local de busca usa `MatDialog`, enquanto entity cards, form surfaces e detail cards permanecem locais.

Feedback técnico relevante fica após `PageHeader`. Coleção vazia permanece inline. Somente a ausência local produzida por busca explícita abre `MatDialog`; erro técnico ou de provider continua no `FeedbackAlert`. Isso mantém a ausência local informativa sem convertê-la em alerta vermelho.

### D8. Cadastro contextual e prefill transitório

Corretoras tratará como ausência local apenas `status===404 && code===null`; Ações preserva a mesma regra já existente. Nesses casos, a listagem mantém termos e coleção e abre dialog centralizado com título, descrição, termo pesquisado, ação secundária de fechar/cancelar e CTA de cadastro. Fechar por ação, backdrop ou Escape não navega nem realiza HTTP; o foco fica preso durante a abertura e retorna ao acionador ao fechar. O dialog usa título/descrição associados, largura confortável no desktop e adaptação no mobile sem fullscreen desnecessário.

O fluxo normal iniciado pelo botão de cadastro da listagem ou pelo CTA de ausência local abre um segundo `MatDialog` com o formulário da feature. Quando originado pela ausência local, o resultado tipado do primeiro dialog transporta somente os termos pesquisados para a abertura do formulário: CNPJ em Corretoras; ticker e mercado em Ações. O formulário valida esse shape antes de preencher os controles, não submete nem consulta automaticamente e não usa estado global ou persistente. O dialog informativo fecha antes da abertura do formulário de cadastro, preservando restauração e gerenciamento de foco entre overlays.

Após `POST` bem-sucedido no dialog, a resposta completa atualiza a coleção local preservada sem `GET` redundante, o dialog fecha, a listagem continua sendo o contexto ativo e o toast comunica sucesso. Cancelar não realiza HTTP e mantém busca e coleção. A confirmação contextual `SITUACAO_CADASTRAL_NAO_ATIVA` permanece dentro do mesmo fluxo de Corretoras.

As rotas `/corretoras/nova` e `/acoes/nova` e suas páginas permanecem disponíveis para acesso direto. Elas reutilizam o mesmo formulário/contrato, iniciam vazias sem estado transitório válido e preservam o comportamento aprovado da navegação direta. `NavigationExtras.info` continua válido nos fluxos de rota já existentes, mas não é necessário para transportar dados entre dialogs no fluxo normal. Não haverá submit, provider call, query param, local/session storage ou store automáticos. Payloads e confirmação `SITUACAO_CADASTRAL_NAO_ATIVA` permanecem intactos.

Alternativas rejeitadas: query parameters, por expor estado de UX na URL; history state persistente, storage ou store, por ampliar lifecycle; service compartilhado, por criar estado global desnecessário.

### D9. Formatter temporal compartilhado

Um formatter puro compartilhado receberá string de `OffsetDateTime`, criará apenas uma representação para display e usará `Intl.DateTimeFormat('pt-BR')` no timezone local do navegador. A composição produzirá `dd/MM/yyyy às HH:mm`; entrada inválida continuará visível sem mutar o DTO.

A regra não será reutilizada automaticamente para `YYYY-MM-DD` de futura `dataOperacao`, que é data civil. Configurar timezone fixo ou converter na camada de dados foi rejeitado por contrariar a decisão aprovada.

### D10. Hierarquia de botões, forms e cards

Filled será reservado à ação principal; outlined a buscar, atualizar e retry; text a voltar, limpar e cancelar. Em compacto, grupos podem empilhar e ações principais podem ocupar largura total. Typed Reactive Forms e validações permanecem.

Em páginas longas, a ação textual de retorno permanece disponível dentro da região de trabalho, sem sobreposição da toolbar ou cobertura do conteúdo. A rota, o comportamento funcional, o nome acessível completo, o foco visível e a operação por teclado permanecem adequados em desktop e mobile. Esta change não exige detectar rolagem nem alternar dinamicamente a apresentação do controle.

Search e form surfaces usam card/surface local; cards de entidade e detalhe compartilham tokens geométricos, mas preservam markup específico. Não será criado `EntityCard` genérico.

As coleções usarão cards/list items mais compactos, com ícone local, nome principal, metadados existentes em hierarquia secundária e CTA inequívoco. Corretoras poderá apresentar nome complementar, CNPJ, cidade/UF e situação somente quando esses dados existirem no DTO; Ações aplicará a mesma densidade aos campos já disponíveis, sem indicadores ou cálculos novos. Desktop prioriza mais registros visíveis e mobile mantém uma coluna legível, nomes completos e wrap.

Na listagem de Corretoras, a situação cadastral usa badge/chip compacto e não se expande por toda a largura do item. O frontend apenas mapeia visualmente o texto recebido: `ATIVA` recebe semântica positiva; valores explicitamente de atenção podem usar warning; vermelho fica restrito a valores que expressem de fato condição inválida/inativa. Nenhum status novo é inferido ou inventado.

### D11. Estratégia responsiva e acessível

O shell conserva 960px. Um breakpoint visual próximo de 36rem adapta header, actions, alerts, forms, grids, definition lists e tipografia. Testes verificam estrutura, ordem, roles, `aria-live`, labels, busy/disabled e comportamento de drawer; não congelam hex, sombras ou pixels cosméticos.

Contraste será revisado por combinação de papel, incluindo foco. Active navigation continua com marcador e `aria-current`; feedback e finanças nunca dependerão apenas de cor.

Dialogs de ausência local usarão `MatDialog` com backdrop, focus trap, Escape, restauração de foco, título e descrição associados e ordem de ações previsível. No desktop, Cancelar e ação principal ficam lado a lado e alinhados à direita; em viewport estreita, ficam empilhados, com mesma largura e altura, gap consistente e ordem Cancelar antes da ação principal, sem overflow. Testes estruturais verificarão que toolbar/sidebar permanecem estáveis, que a região correta rola e que não há conteúdo oculto ou overflow horizontal, sem congelar detalhes pixel-perfect.

### D12. Estratégia de testes e validação

- Testes unitários de `PageHeader`, `FeedbackAlert` e formatter temporal cobrirão API, semântica e entradas relevantes.
- Testes de páginas cobrirão 404 local/code null, provider code preenchido, preservação de termos, CTA real do dialog, abertura do cadastro contextual, prefill válido/inválido e rotas diretas vazias.
- Testes existentes protegerão confirmação não ativa, StandardError, requests, DTO transitório, retry e ausência de chamadas automáticas.
- Testes de shell protegerão breakpoints e navegação; inspeção manual cobrirá contraste percebido, nomes longos, toolbar/sidebar e viewports reais.
- Testes de shell também protegerão estabilidade estrutural de toolbar/sidebar, região de scroll, ausência de double scroll e ordem ícone→label sem duplicação do nome acessível.
- Testes de busca abrirão o dialog apenas para ausência local, verificarão conteúdo, foco/teclado, fechamento sem navegação ou HTTP, preservação da coleção e o retorno real do CTA; erros de provider não abrirão o dialog. Testes do cadastro em dialog cobrirão cancelamento sem HTTP, prefill, submissão explícita, atualização local pela resposta do POST sem GET redundante e toast.
- Build registrará initial bundle, lazy chunks, CSS por componente e warning atual sem alterar budgets.

## Future considerations explicitamente adiadas

Carteiras poderá futuramente se tornar o workspace principal de posições, operações, patrimônio, resultados e evolução, com a ação “Nova operação” contextualizada nesse workspace. Esta change não remove `/operacoes`, não altera navegação ou endpoints e não implementa Carteiras, Operações, tabs, dashboard ou indicadores. A decisão exige capabilities futuras próprias antes de qualquer implementação.

Refinamento futuro: transformar a ação textual de retorno em controle compacto durante a rolagem do workspace. Isso inclui detectar a rolagem, substituir temporariamente texto e seta por um controle compacto e restaurar o texto ao retornar ao topo; não é requisito desta change e deverá ser tratado em change futura.

Nota operacional: após a conclusão da fase de implementação do frontend será realizado reset controlado dos dados de desenvolvimento utilizados nos testes manuais, antes de uma nova rodada de testes integrados. O reset não faz parte desta implementação, não exige código ou migration Liquibase e não deve apagar dados nesta etapa.

## Risks / Trade-offs

- **Tema global causa regressão visual ampla** → aplicar por tokens, revisar todas as páginas e executar inspeção manual em desktop/compacto.
- **Contraste insuficiente em cores claras** → validar combinações por papel; usar texto grafite e não assumir branco.
- **Marca se confunde com lucro** → manter tokens e conteúdo distintos para brand e financial-positive.
- **CSS global cresce ou vaza** → limitar globals a tema/tokens/padrões estruturais; manter domínio local.
- **Shared components viram abstração excessiva** → compartilhar apenas PageHeader/FeedbackAlert; demais padrões começam como styles.
- **Prefill muda fluxo sem intenção** → type guards, ausência de submit automático e testes de refresh/estado inválido.
- **404 de provider vira ausência local** → exigir simultaneamente status 404 e code null em Corretoras e Ações.
- **Formatter desloca data civil** → restringir helper a OffsetDateTime e testar que DTO/string não é reescrito.
- **Quebra mobile** → preservar 960px, testar 36rem estruturalmente e inspecionar viewports reais.
- **Bundle cresce sobre warning existente** → nenhuma dependência; acompanhar baseline implementada de ~505,07 kB, excesso ~5,07 kB, lazy chunks e component CSS sem elevar budgets.
- **Graphify falha com WinError 5** → tentar workflow normal após implementação e registrar o risco; não elevar privilégio nem alterar ACL.

## Migration Plan

1. Introduzir tema/tokens e padrões transversais mantendo contratos e rotas.
2. Adotar compartilhados e formatter nas páginas existentes de modo incremental.
3. Refinar estados/CTA/prefill com testes antes de remover markup antigo equivalente.
4. Validar suíte, build, budgets, strict OpenSpec, Graphify normal e inspeção visual manual.
5. Em rollback, reverter shared/padrões e restaurar o tema anterior sem migração de dados, backend ou estado persistido.
