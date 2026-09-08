# Evidências finais da Fase 7 — 08/09/2026

**Estado vigente após aprovação final:** 70 concluídas, 2 dispensadas (1.3 e 7.5), nenhuma pendência funcional ou task aberta sem justificativa. A matriz e o roteiro abaixo documentam a auditoria anterior à aprovação; suas pendências foram resolvidas pelo registro final H5 ao fim deste documento. As dispensas não constituem execução nem atendimento literal.

## Recuperação e limites

Branch conferida: `feature/frontend-angular`. Retomada da execução interrompida, sem reiniciar implementação. Os logs confirmam 247/247 testes focados (29 arquivos), 412/412 na suíte (62 arquivos) e build production de 514,42 kB. Nenhum arquivo em `frontend/src` tem modificação posterior ao log dos testes focados; o working tree preserva as três correções abaixo. Não houve alteração de código nesta retomada nem repetição de testes/build.

Na recuperação havia 39 arquivos rastreados modificados e oito entradas não rastreadas no `git status --short`, incluindo diretórios. Esse conjunto acumula trabalho anterior e não representa 39 alterações da Fase 7. Os arquivos anteriores abrangem páginas/testes das cinco features, Dashboard/evolução, posições compartilhadas, formatação de apresentação de Operações e artefatos da change; os não rastreados incluem CorretoraNamesService/teste, diretiva de labels/teste, estilos de coleção/formulário e estilos/testes de posições. Nenhum foi descartado. Todas as 7.x ainda estavam abertas (54/72); a matriz abaixo constitui sua primeira atualização.

Somente estes três arquivos de código foram modificados pela execução da Fase 7 antes da interrupção:

- `frontend/src/app/layout/main-layout/main-layout.component.ts`: substituir MatListModule pelos quatro imports standalone efetivamente usados: MatNavList, MatListItem, MatListItemIcon e MatListItemTitle. Template, estilos, eventos e rotas intactos.
- `frontend/src/app/features/acoes/pages/acoes-list-page.component.ts`: remover MatCardModule sem uso e três seletores mobile órfãos (`.entity-card__heading`, `.market-badge`, `.entity-card__data`), sem correspondência no template atual.
- `frontend/src/app/features/operacoes/pages/operacoes-list-page.component.ts`: remover MatCardModule sem uso.

Nesta retomada, somente este documento e `tasks.md` foram editados. Proposal, design e deltas anteriores permanecem preservados, inclusive seus registros históricos. Não houve Graphify, archive, instalação, Git mutável ou modificação de budgets.

## Fontes e matriz de evidências

**A1:** `frontend-phase7-focused.log`, 247/247, 29 arquivos, 08/09 às 11:52; shell, contexto global, Ações, Operações e Dashboard/evolução. **A2:** `frontend-phase7-full.log`, 412/412, 62 arquivos, às 11:54. Ambos com `VITEST_MAX_WORKERS=2`, sem alterar assertions nesta fase. **A3:** `frontend-phase7-final-build.log`, production com `--stats-json`, concluído às 11:54:41. Logs são artefatos locais; resultados essenciais estão transcritos aqui.

**H1:** aprovação humana de 07/09 registrada em tasks.md para coleções/posições, desktop/mobile e formatação. **H2:** aprovação complementar integral de 07/09 de 2.2, 2.9, 3.11 e 5.7: contraste, revisão transversal, dialogs, formulários, contexto, reload e deep links. **H3:** aprovação integral explícita da Fase 6 em 08/09, incluindo BRL/USD, labels, tooltip, mouse/toque/teclado, foco, Escape e histórico. **H4:** instrução explícita do usuário autorizando consulta compartilhada de Corretoras e aceitando a limitação histórica de 1.3. Essas execuções são atribuídas ao usuário; não constituem nova sessão manual do agente.

| Task / objetivo | Automatizada | Técnica/read-only | Manual / origem | Resultado e limitação |
| --- | --- | --- | --- | --- |
| 1.3 Baseline renderizado anterior | Não substituível por testes | Ausência registrada nas evidências das Fases 1–3 | H4 aceita a limitação | **Aberta**. Não existe captura histórica; aceite não prova execução. |
| 7.1 Linguagem transversal | A2 protege fluxos/mensagens | Auditoria de páginas, detalhes, formulários, dialogs, loading/vazio/erro e histórico; termos técnicos restantes são identificadores internos ou diagnóstico necessário | H1–H3 aprovam apresentação anterior | **Concluída**. Nenhuma nova edição editorial necessária. |
| 7.2 Testes focados | A1: 247/247 | Escopo cobre os três componentes alterados e integrações afetadas | Não aplicável | **Concluída**. Não prova renderização. |
| 7.3 Suíte completa | A2: 412/412 | 62 arquivos, workers=2 | Não aplicável | **Concluída**. Reutilizada sem alteração posterior de código. |
| 7.4 Build e comparação | A3 | Comparação histórica reproduzida abaixo; budgets intactos | Não aplicável | **Concluída**, com warnings explicitados. |
| 7.5 Initial não superior ao baseline | A3 | 514418 > 506086 bytes | Sem aceite de alteração do critério | **Aberta**, diferença 8332 bytes. |
| 7.6 Contraste | Medições históricas dos tokens | Tokens/sinais/estados preservados pelas P0 | H2 (2.2), H3 para gráfico | **Concluída por evidência combinada**; borda sutil do seletor 1,34:1 sobre branco não passa isoladamente em 3:1. Aceite humano de identificação/foco já registrado; não se afirma certificação universal WCAG. |
| 7.7 Teclado, semântica e leitor de tela | A1/A2: skip link, foco, semântica e alert/status | Headings, labels, tabelas únicas e papéis presentes | H2/H3 cobrem teclado/foco; leitor de tela não individualizado | **Aberta** para leitura real das tabelas/cards, headings/labels e anúncios dinâmicos. |
| 7.8 Dialogs | A2: contenção/restauração, cancelamento e pendência | Configuração Material existente preservada | H2 (3.11 e 5.7), inclusive largura/altura reduzidas | **Concluída por evidência combinada**, sem repetir revisão aprovada. |
| 7.9 Gráfico | A1/A2: Enter/Espaço/Escape, consulta, dados, gaps, Tab | SVG local, controles equivalentes no histórico, sem HTTP na interação | H3 aprova integralmente 6.6–6.9 | **Concluída por evidência combinada**. Não implica leitor de tela transversal de 7.7. |
| 7.10 Viewports, fronteiras e textos extensos | A1/A2: breakpoint/estrutura/labels com retângulos simulados | Shell mantém 959.98px; coleções usam largura disponível | H1–H3 aprovam desktop/mobile e revisão geral | **Aberta**: falta registro específico final de tablet, 959/960/961 e textos/valores extensos em todas as features. Aprovação geral não individualiza essa matriz. |
| 7.11 Texto 200%, 320px, zoom e baixa altura | A1/A2: CSS e retângulos simulados | Reflow e regras de baixa altura presentes | H2/H3 aprovam zoom/reflow geral e dialogs em baixa altura | **Aberta**: falta confirmação específica de texto 200% e reflow equivalente a 320 CSS px no conjunto final, sem perda/foco oculto/double scroll. |
| 7.12 Reduced motion e redundância do feedback | A2: textos/estados | Regra global suprime animação/transição; sinais, moeda, texto e tipos não dependem só de cor | Preferência real reduced-motion não relatada especificamente | **Aberta** para execução com preferência ativada. |
| 7.13 HTTP/rotas/query/payloads | A1/A2: compra/venda, contexto concorrente, navegação e snapshots | Auditoria de serviços, rotas, formulário e origem abaixo | H2 (5.7): reload/deep link reais e contexto fixo | **Concluída**, com exceção H4 de Corretoras explicitada. |
| 7.14 Lossless, precisão, moedas e consultas | A2: parsing, strings, dados, geometria e requests | Sem alteração de parsing/DTO/fórmula; formatação visual autorizada | H3 e H4 | **Concluída sob a exceção explícita H4**: há resolução corretoraId → nome por coleção. Não se afirma ausência absoluta de enriquecimento/requests nem se reescreve o requisito histórico. |
| 7.15 Consolidar evidências/limites | A1–A3 referenciadas | Este documento e histórico preservado | H1–H4 com origem e escopo | **Concluída**. Lacunas ficam abertas, sem fabricar execução. |
| 7.16 Escopo e lazy loading | A2/A3 | Diff acumulado, arquivos novos e comparação histórica; contratos/dependências/budgets preservados | Exceção H4 explicitada | **Concluída**. Contexto global é evolução histórica aprovada e distinta das P0. |
| 7.17 Validações documentais | OpenSpec strict change/global | diff --check e status finais | Não aplicável | **Concluída** após validações finais; não substitui performance ou revisão manual. |

Resultado: **66/72**, Fase 7 **12/17**. Abertas: **1.3, 7.5, 7.7, 7.10, 7.11 e 7.12**. Fase 6 permanece 11/11. Os critérios e evidências históricas não foram reescritos.

## Escopo, HTTP e integridade financeira

Auditoria do diff atual incluindo arquivos não rastreados e comparação com `8a92f87` (baseline), `d20f819` (base visual) e `f8578d7` (contexto global): backend, banco/migrations, DTOs/modelos financeiros, endpoints, parsing lossless, fórmulas, dependências/package-lock e budgets inalterados. Arquivos de rotas preservados; features continuam em `loadChildren`/`loadComponent`.

O contexto global introduzido historicamente não deve ser atribuído às três P0. Serviços de contexto/navegação mantêm precedência URL válida > memória > preferência validada > primeira carteira por ID, compartilhamento da carga pendente e proteção contra respostas antigas. Carteira capturada pelo formulário não muda por troca global. Origem e carteira nos query params permitem retorno após reload/deep link; dados transitórios da navegação não substituem consulta após reload. Rotas globais de Operações continuam disponíveis por compatibilidade.

COMPRA mantém prévia somente leitura e POST sem preço; VENDA mantém sugestão editável e preço em string decimal. Datas civis, quantidade, carteiraId, corretoraId e demais payloads preservados. Estimativa existente não foi convertida em regra financeira nova. A alteração anterior em `operacao-validators.ts` reutiliza formatador de quantidade para apresentação; parsing e multiplicação decimal permanecem intactos.

Dashboard mantém quatro GETs financeiros por atualização (resumo, posições, resultados realizados e evolução), com loading/erro independentes; atualização e interação com pontos não fazem POST. Registro patrimonial exige ação manual e mantém POST existente com corpo nulo, proteção contra double-submit e origem capturada. Buscas e dialogs mantêm endpoints e gatilhos anteriores.

**Exceção HTTP autorizada:** CorretoraNamesService, fornecido nas páginas de Operações e detalhe de Carteira, consulta a coleção de Corretoras quando há IDs a resolver. Uma consulta compartilhada por instância da página, sem uma consulta por linha/ID (sem N+1); a guarda de solicitação evita repetição na mesma instância. Usa nome fantasia/razão social e mantém fallback para ID/ausência em falha. Não é cache global permanente. Essa evolução foi explicitamente aprovada pelo usuário durante revisão visual e reafirmada na instrução da Fase 7. Logo, a afirmação absoluta “nenhuma nova requisição HTTP/enriquecimento de IDs” seria incorreta.

BRL/USD permanecem independentes, sem FX, soma entre moedas, fórmula nova ou redução de observações/gaps. `evolution-geometry.ts` intacto. Timestamp original e datetime completos; somente apresentação local específica usa DD/MM/YYYY, HH:mm, com eixo compacto. Formatadores financeiros e parsing lossless preservados.

## Performance reproduzida

Ambiente usado na execução anterior: Node 24.19.0, npm 11.17.0, Angular 22.1.4; dependências locais existentes. Comando: `npm run build -- --configuration production --stats-json`. Árvores históricas exportadas por `git ls-tree`/`git show` para diretórios isolados em `frontend/tmp/phase7-audit`, com junction para node_modules existente, sem checkout, instalação ou mudança do working tree. Scripts locais `export.py`, `analyze.py`, logs/builds por revisão e `before-stats.json` preservam a investigação. Essa reconstrução de build não é captura visual histórica da task 1.3.

A soma considera main JS, styles globais e imports estáticos transitivos; exclui imports dinâmicos e saídas virtuais de CSS de componente. Assim, mover código de main para chunk compartilhado inicial não é contabilizado como economia.

| Estado | Initial bytes | Variação sequencial |
| --- | ---: | ---: |
| `8a92f87` baseline medido | 506086 | referência (506,09 kB) |
| `d20f819` base visual | 506088 | +2 |
| `f8578d7` contexto global | 514603 | +8515 |
| Antes das P0 da Fase 7 | 514711 | +108 |
| Final, após P0 | **514418** | **−293** |

Diferença final para baseline: **8332 bytes (+8,332 kB)**, aproximadamente **+8,33 kB** comparando números publicados. O requisito de 7.5 não foi atendido.

| Initial final | Bytes |
| --- | ---: |
| `chunk-DI_k_10F.js` | 367386 |
| `main-JJVIMI5O.js` | 129424 |
| `styles-ZAG5RE6K.css` | 16087 |
| `chunk-DHAIEnAO.js` | 887 |
| `chunk-C7MwnfEW.js` | 634 |

Transferência estimada final **122,08 kB**. Dashboard lazy **37,16 kB** (estável); CSS evolução **4284 bytes**, estável. Ações lazy aproximadamente **15,69 → 15,45 kB**; Operações lazy **7,81 → 7,77 kB**. Essas economias lazy não integram os 293 bytes do initial. Log completo mantém os demais chunks. Warnings permanecem: initial 14,42 kB acima de 500 kB e CSS evolução 284 bytes acima de 4 kB. Build aprovado; budgets não alterados.

### Causas A–F e oportunidades

- **A — estrutural/necessária:** etapa do contexto global explica +8515 bytes líquidos. CSS global cresceu 1837 bytes nessa etapa por estilos de overlay/dialog fora do encapsulamento. Metadados de entradas apontam contexto (4416), seletor (3394), navegação (1858), serviço de carteiras (680) e shell (+662). São contribuições brutas do compilador, com compensações de framework/minificação: não devem ser somadas como parcelas líquidas independentes. Dashboard/evolução/CorretoraNames não aparecem no grafo initial. Serviço de Carteiras alcançável pelo shell é necessário ao contexto aprovado.
- **B — CSS duplicado:** `collection.scss` compartilhado é emitido em componentes lazy que o consomem. Há oportunidade de estudo, mas não causa demonstrada do crescimento initial. Globalizar pode aumentar initial e alterar cascata; **P1, não executada**.
- **C — imports desnecessários:** MatCardModule sem uso em duas listagens e MatListModule amplo no shell. **P0 aplicada**, preservando somente diretivas usadas. Nenhuma dependência nova/acidental foi instalada.
- **D — promoção indevida ao initial:** nenhuma feature de Dashboard/evolução promovida identificada. A promoção deliberada do serviço de Carteiras corresponde a A, não a carregamento acidental.
- **E — duplicação/desperdício evitável:** três regras mobile órfãs em Ações removidas como **P0**. Não foi comprovada outra remoção simples suficiente para atingir baseline. Deduplicações profundas permanecem **P1**.
- **F — outras causas:** base visual +2 bytes líquidos; evolução posterior +108 bytes líquidos antes das P0, incluindo variações de código compartilhado/framework e minificação. Metadados mostram +95 no chunk interno de Angular, sem permitir atribuir cada byte final a uma única funcionalidade. Baseline→pré-P0: JS +6532 e CSS global +2093 = +8625 bytes. P0 do shell reduz main em 293 bytes; CSS global permanece igual.

**P0 concluídas:** imports standalone da lista no shell, dois imports MatCard órfãos e CSS órfão de Ações. **P1 não executadas:** reestruturar dependências Material/CDK/shell, serviços de contexto, carregamento de overlays ou deduplicação global de estilos encapsulados. Exigem investigação/refatoração com risco de foco, cascata, navegação e aumento do initial. **P2 legítimas:** contexto global, seletor/navegação, apresentação de dialogs e suporte de acessibilidade necessários às funcionalidades aprovadas.

Não há evidência de P0 restante capaz de recuperar 8332 bytes. Decisão humana necessária para 7.5: autorizar investigação/refatoração estrutural delimitada ou decidir formalmente como tratar o desvio conhecido no aceite da change. Até essa decisão e sua formalização, 7.5 continua aberta com o baseline original, sem waiver implícito nem perseguição de bytes por remoção funcional.

## Revisão editorial e acessibilidade complementar

Nenhuma alteração editorial nova na Fase 7. A auditoria preservou as melhorias anteriores (“cotação registrada”, “Histórico do patrimônio”, “Registrar patrimônio atual”), avisos financeiros e diagnóstico HTTP. Termos backend/persistido/autoritativo/snapshot foram examinados no contexto: identificadores internos e mensagens técnicas necessárias não foram removidos para satisfazer busca textual.

Não repetir mouse/toque/teclado do gráfico, fluxos de formulário ou dialogs já aprovados. Roteiro apenas das lacunas:

1. **7.7:** com leitor de tela, verificar skip link/região principal, hierarquia de títulos e nomes/ajudas de campos; navegar tabela desktop e seus cards mobile confirmando associação dos valores aos cabeçalhos, uma única representação e anúncios loading/erro/sucesso sem duplicação. Registrar leitor/navegador e resultado.
2. **7.10:** confirmar tablet 768x1024 e fronteiras 959/960/961px; conferir nomes/textos longos e valores extensos nas cinco features, detalhes/formulários, sem ações/campos cortados. Desktop/mobile ordinários já aprovados não precisam ser repetidos indiscriminadamente.
3. **7.11:** confirmar texto 200% e reflow a 320 CSS px (zoom equivalente quando aplicável), incluindo conteúdo final do Dashboard/coleções e ações em baixa altura 1280x600/paisagem; nenhum dado removido, foco encoberto ou rolagem concorrente. Reutilizar a aprovação de dialogs em baixa altura.
4. **7.12:** ativar preferência de reduzir movimento; observar loading, feedback, navegação e abertura/fechamento de interface. Movimento não essencial deve ser suprimido e estado continuar compreensível pelo texto, sinais e nomes, sem depender apenas da animação/cor/ícone.

## Validação documental e decisão de encerramento

OpenSpec strict da change aprovado; global **32/32**. `git diff --check` sem erros de whitespace (avisos de conversão LF/CRLF podem aparecer). `git status --short` preserva o conjunto acumulado: 39 entradas modificadas e nove entradas não rastreadas após adicionar este documento; diretórios agrupados não equivalem a contagem individual de arquivos. Nada staged.

**Ainda não pronta para archive:** 7.5 e as lacunas manuais 7.7/7.10/7.11/7.12 permanecem; 1.3 é limitação histórica explicitamente aceita, ainda aberta. Nenhum archive ou Git mutável executado. Strict não substitui esses critérios.

## Encerramento por evidência e dispensas formais — 08/09/2026

Registro final autorizado explicitamente pelo usuário nesta conversa, após declarar que todos os cenários manuais solicitados foram executados e passaram. Este registro supera os estados de pendência anteriores, que permanecem como histórico; não altera o significado dos critérios nem atribui a execução humana ao agente.

- **7.7 concluída:** evidência técnica A1/A2 de skip link, foco, semântica e alert/status, agora complementada pela aprovação humana integral de acessibilidade transversal, leitor de tela e anúncios, incluindo os cenários solicitados de headings, labels e tabelas.
- **7.10 concluída:** evidência estrutural anterior complementada pela aprovação humana de tablet, fronteiras 959/960/961px e conteúdo extenso; somada à aprovação anterior de desktop/mobile, satisfaz o critério integral.
- **7.11 concluída:** evidência técnica de reflow complementada pela aprovação humana explícita de texto 200%, reflow 320px, zoom e baixa altura, sem perda de informação, foco oculto ou double scroll.
- **7.12 concluída:** regras técnicas de reduced-motion e feedback textual complementadas pela execução humana aprovada com redução de movimento e informação independente de animação/cor/ícone.

Origem humana H5: mensagem de aprovação final do usuário em 08/09/2026, nomeando as quatro tasks e confirmando execução e aprovação de todos os cenários solicitados. Não foram inventados navegador, leitor de tela específico, capturas ou medições não fornecidos.

**1.3 — DISPENSADA por decisão humana — baseline visual histórico não disponível; encerrada por aceite, não por execução.**

O baseline renderizado anterior não foi capturado, não existe e não pode ser reconstruído de forma confiável. Capturas atuais não o substituem. As revisões posteriores foram efetivamente realizadas e aprovadas; o usuário aceitou conscientemente a ausência histórica para encerramento. Checkbox desmarcado preservado, sem afirmar captura realizada.

**7.5 — DISPENSADA por decisão humana — critério literal de initial não atingido; desvio analisado, otimizações P0 seguras aplicadas e crescimento estrutural aprovado aceito para encerramento da change.**

Baseline mantido em 506,09 kB (506086 bytes); final 514,42 kB (514418 bytes), diferença 8332 bytes, aproximadamente +8,33 kB. P0 economizaram 293 bytes. A comparação histórica reproduziu +8515 bytes na introdução do contexto global de Carteira aprovado. Dashboard/evolução permanecem lazy, sem promoção indevida, dependência nova, aumento de budgets ou remoção funcional/dataset. As oportunidades restantes exigem refatorações de shell/Material/contexto ou deduplicações profundas, de risco desproporcional nesta etapa. O usuário aceita formalmente o desvio para encerramento; não autoriza nova otimização/refatoração e não afirma atendimento literal. Checkbox desmarcado preservado.

Contagem final conferida: **70/72 concluídas por execução/evidência; 2 dispensadas (1.3 e 7.5); 0 abertas sem justificativa**. Fase 7: 16 concluídas e uma dispensada. Fases 1–7 encerradas funcionalmente; nenhum cenário obrigatório sem evidência ou aceite explícito. A contagem mecânica de checkboxes permanece 70/72, não 72/72.

Revisão pré-archive: proposal/design/specs preservam requisitos originais; as exceções de aceite estão expressamente registradas aqui e nas evidências, sem alegar cumprimento literal dos requisitos dispensados. Referências anteriores a pausas/revisões pendentes são históricas, superadas pelos registros finais. CorretoraNamesService continua documentado como exceção autorizada, consulta compartilhada por instância quando necessária, sem N+1; não se declara ausência absoluta de consultas adicionais. Backend/migrations/DTOs/endpoints, parsing lossless, cálculos, BRL/USD separados sem FX, rotas/contexto global, lazy loading, dependências e budgets preservados. Novos gráficos financeiros continuam fora do escopo; nenhuma nova change criada.

Últimas evidências técnicas permanecem válidas, sem alteração posterior de código: 247/247 focados, 412/412 completos e build production 514,42 kB. Não houve repetição de testes/build. Warnings conhecidos mantidos: initial acima de 500 kB e CSS evolução 4284 bytes, 284 bytes acima de 4 kB. Classificação **A — change formalmente encerrada, sem pendência funcional e pronta para archive**, mediante os aceites explícitos acima; archive ainda não executado. Nenhum Graphify, commit, push ou merge.
