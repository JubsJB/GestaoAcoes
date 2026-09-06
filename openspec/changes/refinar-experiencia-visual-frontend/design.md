## Context

A base atual utiliza Angular Material/CDK, tema claro, tokens SCSS, padrões globais, fontes locais/do sistema e sprite SVG local. PageHeader, FeedbackAlert, SuccessToast, StickyBack e AppIcon já constituem a fundação compartilhada. A mudança refina essa arquitetura, sem novo framework ou biblioteca.

A auditoria anterior foi estrutural, baseada em templates, estilos e contratos. Capturas renderizadas comparáveis e medições atuais de bundle ainda deverão compor o baseline da implementação; o planejamento não afirma validação visual executada. O bundle informado de aproximadamente 506,09 kB ultrapassa o warning de 500 kB; esse número é referência histórica, não medição desta etapa.

As oito capabilities existentes recebem apenas deltas necessários de apresentação. Requirements financeiros, HTTP, rotas, concorrência, parsing e formatadores continuam vigentes sem alteração. Blocos MODIFIED preservam os cenários anteriores. Não se cria capability nova.

## Goals / Non-Goals

**Goals**
- Tornar a leitura financeira mais rápida por hierarquia, alinhamento e densidade adequada à função de cada página.
- Consolidar padrões realmente compartilhados e distinguir marca, sucesso, lucro, prejuízo, erro e ação destrutiva.
- Melhorar interação acessível e responsividade sem perda de dados, ações ou comportamento.
- Permitir revisão incremental em sete fases e provar ausência de regressão funcional.

**Non-Goals**
- Alterar backend, contratos, DTOs, banco, migrations, regras financeiras, precisão, parsing, máscaras, formatadores, payloads, HTTP, rotas ou query params.
- Criar métricas, totalizações, dados, enriquecimento de IDs, chamadas adicionais, filtros, ordenação, paginação ou buscas novas.
- Implementar tema escuro, seletor de tema ou persistência de preferência.
- Instalar dependências, elevar budgets, executar Graphify ou arquivar como parte da implementação visual.

## Decisions

### 1. Uma change com sete fases internas
Adotar uma change com fundação antes das páginas, em vez de uma alteração monolítica sem pontos de revisão. Separar design system e cada feature em changes distintas aumentaria coordenação e repetiria critérios transversais nesta base pequena. Cada fase terá critérios verificáveis e poderá originar revisão/commit intermediário quando futuramente autorizado; este planejamento não autoriza Git mutável.

### 2. Evoluir tokens existentes
Manter os nomes úteis atuais e introduzir semântica somente para lacunas compartilhadas. Centralizar background, superfície normal/muted, texto principal/secundário, bordas, marca e hover/focus; separar positivo/negativo financeiro de success/error e destructive. Cor coincidente não torna dois papéis intercambiáveis.

Consolidar spacing, radius, elevation, tipografia e estados nos arquivos de tema/tokens/padrões existentes. Remover literais repetidos quando representarem o mesmo papel, sem extrair cada ajuste local. Evitar wrappers genéricos ou renomeação puramente cosmética.

Verde continua principal; o oliva pode ser refinado. Os hexadecimais serão escolhidos na fase 2 mediante contraste medido e comparação do conjunto, sem obrigação de preservar a paleta exata. Superfícies claras, bordas discretas e elevação moderada sustentam conteúdo financeiro. Preparar semântica não implica implementar dark mode.

### 3. Tipografia e ícones
Preservar fonte do sistema e sprite SVG local. AppIcon permanece o ponto de uso dos ícones; tamanhos/alinhamento e peso visual devem ser consistentes, com ícone decorativo oculto quando o label já nomeia a ação. Não misturar biblioteca externa nem fonte de ícones.

Definir papéis de título, contexto, indicador, metadado e ajuda; evitar labels minúsculas como solução de densidade. Usar tabular-nums em dinheiro, quantidades e percentuais, texto à esquerda e números à direita. Valores extensos e sinais devem permanecer completos, sem alterar as funções aprovadas de formatação.

### 4. Shell e componentes compartilhados
Preservar breakpoint funcional de 960px, modos side/over, largura estrutural da sidebar, toolbar aproximadamente 64/56px, skip link, aria-current, rotas e região de rolagem existentes. Refinar contraste, espaços, item ativo e superfícies para que o shell seja discreto.

PageHeader deve funcionar com ou sem ícone, acomodar ações e manter hierarquia semântica. Aplicar a mesma família visual a .app-surface, .section-card, .app-form-surface, .app-actions, .data-list, badges e .app-state. StickyBack mantém seu comportamento e não pode ocultar foco ou ações em baixa altura.

Padronizar dialogs de cadastro, edição, confirmação e exclusão em largura, padding, título, conteúdo e ações. Preservar focus trap, restauração, cancelamento, Escape/backdrop e bloqueios pendentes existentes. Destructive tem apresentação própria, sem converter a ação primária de marca em exclusão.

FeedbackAlert mantém semântica e conteúdo; SuccessToast mantém duração, posição, descarte e fechamento atuais. Spinner e anúncio textual continuam; skeleton não é necessário e, se usado, será decorativo, sem números financeiros fictícios.

### 5. Coleções e detalhes por função
| Área | Desktop | Mobile | Particularidade |
| --- | --- | --- | --- |
| Posições | Tabela semântica | Cards completos | Valores e moedas comparáveis; nenhuma rota nova |
| Operações | Tabela semântica | Cards completos | Ordem recebida, sem sort ou mutações |
| Histórico da Carteira | Mesmo padrão de Operações | Cards completos | Mesmo contexto e conjunto HTTP |
| Ações | Tabela semântica | Cards completos | Ticker, empresa e cotação registrada |
| Corretoras | Lista compacta | Lista em coluna | Não forçar tabela cadastral |
| Carteiras | Cards compactos | Cards compactos | Nome e data, sem KPI novo |
| Resultados realizados | Linhas comparáveis por Ação | Itens completos | Sem totalização |

Preferir tabela HTML nativa com estilos existentes; Material/CDK pode ser usado quando já necessário, sem importar módulos pesados por aparência. Cabeçalhos associados, títulos/captions acessíveis e moeda explícita devem permitir leitura fora do contexto visual.

O breakpoint de coleção será escolhido pela largura real necessária, sem alterar o breakpoint do shell. Somente uma representação poderá estar ativa na árvore acessível e ordem de foco. Se duas estruturas de apresentação forem necessárias, devem compartilhar o mesmo estado carregado; ocultar visualmente por opacity/posição não basta. Não criar subscriptions, HTTP, anúncios ou efeitos duplicados.

Detalhes usam grupos semânticos de pares label/valor. Corretora agrupa identificação, contato, endereço e situação; Ação destaca ticker e cotação com data; Carteira mantém contexto e histórico; Operação mantém seus dados aprovados, sem exibir ordemNoDia no detalhe. Nenhum ID será resolvido por chamada extra.

### 6. Formulários sem mudança de fluxo
Corretora continua somente CNPJ; Ação ticker e mercado; Carteira somente nome. Reutilizar o formulário atual em página e dialog, com largura adequada, labels/ajudas, ações estáveis e feedback junto aos campos.

Operação organiza contexto, tipo/movimentação, quantidade/preço/data, Corretora, estimativa existente e ações. Não criar wizard, etapas ou submissões adicionais. Preservar prévia de COMPRA somente leitura e ausente no POST, sugestão de VENDA editável, estimativa, Carteira fixa quando contextual, Corretora opcional, datas civis, strings decimais, máscaras, validações e proteção contra respostas antigas/double-submit.

### 7. Dashboard financeiro
Ordem visual e de leitura: contexto compacto; ações existentes; indicadores separados por moeda; evolução; posições; resultados realizados por Ação. Não apenas reposicionar com CSS produzindo ordem assistiva divergente.

Usar somente patrimônio atual, custo total das posições, resultado não realizado e rentabilidade de cada resumo. Patrimônio pode ocupar composição maior; os quatro indicadores não precisam ser cards idênticos. BRL e USD nunca são somados. Resultado realizado total, variação diária, benchmark e alocação estão excluídos.

Reordenar a seção de evolução sem acoplar seu loading/erro ao restante. Seletor, atualização, navegação contextual, query params e criação manual de snapshot permanecem iguais.

### 8. Evolução e interação SVG
Preservar SVG local, dataset completo, projeção financeira, segmentos, gaps, snapshots vazios, moeda, estilos contínuo/tracejado e timestamps. Ajustes de dimensões usam a projeção vigente; não alterar cálculo financeiro ou normalização para obter aparência.

Refinar labels e tooltip para que não sejam reduzidos indevidamente pelo viewBox nem cortados nas bordas. Área de interação pode ser maior que o marcador, sem mover coordenadas financeiras ou conectar lacunas. Conteúdo textual completo permanece disponível.

Pontos acionáveis mantêm semântica de botão coerente com Enter/Espaço e clique/toque. Hover e foco permitem consulta; Escape fecha o tooltip sem efeito financeiro. Implementar comportamento dispensável, persistente e alcançável quando aplicável. Percurso de Tab segue cronologia e permite sair do gráfico. Não introduzir roving tabindex que torne pontos inacessíveis sem um modelo de teclado explicitamente testado.

### 9. Linguagem
Revisar texto editorial orientando-o ao investidor: por exemplo, cotação registrada em vez de persistida. Evitar expor “valores autoritativos”, “backend” ou “fonte acessível principal” em ajuda desnecessária. Não suprimir códigos, mensagens e detalhes necessários a erro/diagnóstico, nem enfraquecer avisos sobre cotação histórica, estimativa, gaps ou snapshot manual.

### 10. Acessibilidade e responsividade como aceite
Aplicar WCAG 2.2 AA pertinente: contraste textual 4,5:1 normal e 3:1 grande, não textual 3:1, teclado, foco visível e não oculto, nomes/papéis/estados, labels, headings, tabelas e alert/status. Alvos atendem 24x24 CSS px ou espaçamento/exceções do critério 2.5.8; alvos maiores são preferíveis quando viáveis.

Validar desktop 1440x900, tablet 768x1024 e mobile 390x844/320px; fronteiras do shell 959/960/961px; baixa altura 1280x600 e paisagem compacta. Verificar ampliação textual 200% e reflow equivalente a 320 CSS px (incluindo zoom 400% quando aplicável). Esses valores são pontos de teste, não novos breakpoints funcionais.

Validar shell, header, KPI, cards/tabelas, forms, dialogs, gráficos, ações e textos longos. Sem perda essencial, foco encoberto, overflow horizontal obrigatório da página ou rolagens concorrentes. Preferência reduced-motion deve suprimir movimento não essencial; não introduzir animações decorativas pesadas.

### 11. Performance e evidência
Usar zero dependências novas, fontes/assets locais, lazy loading existente e budgets inalterados. Medir initial no mesmo ambiente/configuração antes e depois; o aceite exige não superar o baseline medido da fase 1, sem reduzir dataset ou funcionalidade. O warning histórico deve ser relatado separadamente de qualquer crescimento. Observar também budgets de estilo por componente.

Na implementação, executar testes focados das áreas alteradas, suíte frontend e build. Proteger contratos com testes de payload/contagem de requests e concorrência existentes, complementando apenas lacunas relevantes. Testes não substituem revisão visual renderizada de contraste, foco, reflow e tooltip.

Registrar evidência por cenário: viewport/zoom, estado, resultado e limitação. Não fabricar execução manual nem manipular banco para fechar tarefa. Quando cenário real depender de provider indisponível, registrar bloqueio e cobertura determinística existente; eventual aceite combinado deve ser explícito, não automático.

## Risks / Trade-offs

- Estilos globais afetam todas as features: comparar páginas representativas após fundação e repetir matriz final.
- Tabela pode aumentar densidade além da legibilidade: migrar para cards antes de comprimir labels ou omitir campos.
- Duas representações podem duplicar foco/efeitos: compartilhar estado e testar árvore acessível e contagem HTTP.
- Tooltip SVG e alvos próximos exigem validação real: não aceitar apenas inspeção de markup.
- Cor de marca pode ser confundida com resultado: tokens separados, sinais e texto preservados.
- Bundle já tem warning: não aumentar budget; deduplicar estilos e evitar novos módulos desnecessários.
- Copy pode retirar diagnóstico útil: revisar textos editoriais sem modificar normalização HTTP.
- Specs canônicas contêm cenários históricos de placeholders/rotas que não são objeto desta change; preservar blocos não afetados e seguir os contratos específicos vigentes, sem usar redação histórica para regredir features já funcionais.

## Migration Plan

Não há migração de dados, backend ou contratos. Implementar fases 1 a 7 em ordem; revisão incremental após cada grupo visual, com regressão funcional antes da entrega. Mudanças visuais deverão ser reversíveis por revisão de alterações localizadas, sem alterar dados. Nenhum procedimento de deploy, commit ou archive é autorizado por este planejamento.

Rastreabilidade: fase 2 cobre fundação, tipografia e performance transversal; fase 3 shell, componentes, estados e dialogs; fase 4 capabilities de coleções/detalhes; fase 5 formulários e Operações; fase 6 Dashboard/evolução; fases 1 e 7 estabelecem e verificam a matriz completa. Linguagem, acessibilidade e preservação funcional atravessam todas as fases.

## Open Questions

Nenhuma decisão funcional bloqueia o planejamento. Hexadecimais finais, densidade e breakpoint visual das coleções serão definidos por evidência na implementação e submetidos à revisão visual do resultado. Dark mode permanece explicitamente fora. Qualquer necessidade de novo campo, chamada, dependência ou alteração financeira exigirá outra decisão de escopo, sem implementação silenciosa nesta change.

