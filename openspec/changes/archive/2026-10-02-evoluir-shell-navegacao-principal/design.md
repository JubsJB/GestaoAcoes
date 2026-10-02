## Context

Ver motivação em proposal.md. O grafo e a leitura dirigida confirmam MainLayoutComponent, NAVIGATION_ITEMS, CarteiraSelectorComponent, AppIconComponent, RouterLinkActive e Angular Material/CDK. O breakpoint é `(max-width: 959.98px)`, representando o limite estrutural de 960px. O estado compactDrawerOpened e o fechamento por NavigationEnd já existem. O header contém marca e seletor; main-content recebe foco pelo skip link e contém router-outlet.

Requisitos consolidados afetados (localização na baseline desta proposta):

| Spec e requisito | Localização | Delta |
| --- | --- | --- |
| frontend-application-shell / Shell principal da aplicação | spec.md:24–57 | Restringir a cláusula de preservação do comportamento de navegação para permitir os novos destinos/estados; preservar todos os demais cenários |
| frontend-application-shell / Navegação principal entre áreas | spec.md:59–88 | Cinco destinos ordenados, Ativos, Operações no menu; substitui explicitamente Compatibilidade de Operações, que vedava sidebar/drawer na linha 84 |
| frontend-application-shell / Navegação responsiva | spec.md:90–112 | Substituir largura invariável e desktop sempre aberto/expandido por estados expandido/recolhido, mantendo side/over e mobile |
| frontend-operation-management / Listagem cronológica e somente leitura | spec.md:47–64 | Remover exclusivamente a vedação à reintrodução de Operações na sidebar, na linha 48; preservar consulta, dados, estados e cenários |

Os deltas copiam requisitos completos para não perder cenários ao sincronizar futuramente. Specs consolidadas e arquivo histórico não são alterados nesta preparação.

Compatibilidade adicional: frontend-visual-experience / Responsividade e acessibilidade visual conserva 960px, side/over, foco, contraste e reduced motion; não requer delta porque o modo estrutural permanece. A foundation permite capabilities posteriores aprovadas. O PRD fundamenta responsividade/usabilidade e limita o produto a ações nesta etapa.

Inconsistências preexistentes fora do escopo: frontend-operation-management exige preço manual em COMPRA em Contratos frontend discriminados de Operações, mas Submissão explícita sem deduplicação ainda menciona bloqueio por prévia e exclusão do preço da prévia do request; o cenário Contratos de compra e venda preservados em Carteira fixa da abertura ao POST ainda diz COMPRA sem preço. Registrar para etapa própria; esta change não resolve nem reproduz essas divergências como novas regras. Preservar formulário e contratos vigentes, sem tarefas financeiras. Nenhum outro conflito de navegação foi localizado além dos quatro requisitos acima.

## Goals / Non-Goals

**Goals:** evoluir o shell único com estado desktop independente do drawer mobile e testes de contratos observáveis. Novos requisitos dentro da capability existente: Recolhimento desktop acessível e Compatibilidade da evolução do shell.

**Non-Goals:** consultar lista de exclusões em proposal.md. Não renomear entidades internas, services, models, endpoints ou a feature ações; não criar segundo shell nem reformular funcionalidades do header, marca, páginas, seletor ou componentes compartilhados. As correções responsivas autorizadas abaixo alteram somente sua apresentação. Não sincronizar/arquivar specs nesta etapa. A implementação já foi autorizada; esta etapa é de revisão final.

## Decisions

1. Manter NAVIGATION_ITEMS como fonte única dos cinco destinos, reutilizando AppIconComponent e ícones locais disponíveis. Alternativa de duplicar menus foi descartada por risco de ordem e acessibilidade divergentes.
2. Manter MatSidenav em side no desktop, alterando apresentação/largura local para recolher, sem fechar os links. Preservar largura expandida atual e escolher largura recolhida suficiente para alvos acessíveis, ícones e indicador ativo. Fechar completamente a sidebar esconderia destinos solicitados.
3. Introduzir estado desktop local separado de compactDrawerOpened. Iniciar expandido; manter entre rotas e ao voltar do mobile; reload reinicia expandido. Entrar no mobile fecha o drawer; retornar ao desktop remove backdrop e comportamento modal. Persistência seria desnecessária nesta etapa.
4. Controle desktop na região lateral com nomes “Recolher navegação”/“Expandir navegação”, aria-controls e aria-expanded. Manter o controle focalizado após alternância, sem remover seu nó. Links recolhidos conservam nome por texto visualmente oculto ou aria-label, sem depender de tooltip. Preservar routerLinkActive e ariaCurrentWhenActive também em rotas filhas. Usar o tratamento existente de movimento reduzido.
5. Preservar header, CarteiraSelectorComponent, skip link, main e router-outlet; ajustar somente espaço lateral sem mudar região de rolagem ou criar double scroll. O recolhimento não muda conteúdo nem exige nova arquitetura de estado.
6. Link Operações aponta somente para `/operacoes`, sem acrescentar carteiraId ou origem a esse acesso. Não alterar services, listagem, formulário ou regras de contexto para criar filtro. Entradas contextuais e globais existentes continuam com seus contratos.
7. Correções responsivas autorizadas na validação: alinhar marca e seletor no header mobile, mantendo label/select lado a lado e alvo de 44px; remover alturas calculadas e scroll interno das listagens de Ações, Carteiras e Corretoras para preservar a rolagem no main. Usar a largura disponível da coleção, via container queries nos limiares existentes, para reflow de tabelas/cards e filtros, inclusive posições. Não reorganizar Dashboard nem modificar dados, controles ou cálculos.

## Risks / Trade-offs

- Estado mobile vazando para desktop → testar os dois sentidos de transição com drawer aberto/fechado e desktop expandido/recolhido.
- Texto oculto retirando nome acessível ou foco → verificar árvore acessível, Tab/Enter/Espaço, foco visível e aria-current.
- Redução de largura ocultando marca, controles ou conteúdo → preservar header, testar zoom 200%, 320 CSS px, baixa altura e ausência de scroll horizontal/double scroll conforme spec visual.
- Confusão entre seleção global e histórico → teste de `/operacoes` com operações de duas Carteiras e troca do seletor sem filtragem ou requisição contextual nova.
- Ativos sugerir classes futuras → mudar somente o rótulo de navegação, sem links, páginas ou promessas de suporte futuro.

## Migration Plan

Após aprovação explícita, implementar alterações mínimas do shell/menu e testes previstos, executar suíte frontend e build de produção, atualizar Graphify por haver alteração de código e revisar o diff. Sem migração de dados ou API. Rollback futuro pode restaurar menu e apresentação anteriores sem tocar dados. Sincronização/arquivamento OpenSpec será etapa posterior própria; nenhuma publicação, commit ou merge integra esta preparação.

## Matriz de aceitação e validação futura

| Critério | Evidência prevista |
| --- | --- |
| 1. Cinco destinos na ordem definida | Teste da coleção e DOM desktop/mobile |
| 2. Operações diretamente pelo menu | Clique/Enter resolve `/operacoes` |
| 3. Ativos aponta para `/acoes` | Rótulo, href e resolução do destino |
| 4. URLs preservadas | Deep links, reload, rotas filhas, raiz, NotFound, voltar/avançar |
| 5. Desktop expandir/recolher | Controle por mouse e teclado, estado local e reload |
| 6. Ativo nos dois estados | Marcador estrutural, superfície e aria-current em rota raiz/filha |
| 7. Destinos sem texto visual acessíveis | Nomes acessíveis, Tab, Enter e foco visível |
| 8. Mobile preservado | Limite 960px, over, backdrop, Escape, navegação e transições |
| 9. Seletor preservado | Estados, teclado, seleção, precedência/persistência e header |
| 10. Histórico global sem filtro implícito | Duas Carteiras, troca do seletor e inspeção de requisições |
| 11. APIs e regras financeiras intactas | Testes existentes, payloads/retornos e revisão de diff |
| 12. Nenhuma função futura anunciada | Revisão dos cinco rótulos, destinos e conteúdo preservado |

Os testes de MainLayoutComponent, roteamento e regressão dos contratos carteiraId/origem, captura fixa e retornos foram executados na implementação. A revisão final reexecuta testes e build; resultados e limites da validação manual estão em tasks.md. Testes DOM isolados não comprovam geometria, contraste nem interação nativa por teclado.

## Decisões aprovadas no fechamento

- O usuário aceitou explicitamente, exclusivamente nesta change, o initial de aproximadamente 516,61 kB diante do baseline de 514,40 kB: +2,21 kB (~0,43%). Esta decisão constitui exceção aprovada ao critério consolidado de não crescimento, sem modificar a spec consolidada ou os budgets. O crescimento acompanha estado, bindings, template e CSS necessários à sidebar e à responsividade; não há dependência nova nem código claramente dispensável identificado. Não realizar micro-otimizações para recuperar esses bytes.
- O warning de portfolio-positions (4,15 kB, 153 bytes acima do budget de warning de 4 kB), introduzido pelos ajustes responsivos, também foi explicitamente aceito. Permanecem os warnings preexistentes de initial e portfolio-evolution. Nenhum budget será aumentado.
- A contextualização de `/operacoes` pela carteira selecionada foi identificada e analisada na validação, mas fica reservada à futura change `contextualizar-historico-operacoes-por-carteira`, ainda não criada. Esta entrega mantém histórico global via `GET /operacoes`, sem observação de CarteiraContextService pela listagem ou mudanças de services, formulário, detalhe e origem/retorno.
