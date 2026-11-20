# Modernização de Operações — implementação aguardando conferência

**Atualização 2026-11-14:** proposta visual aprovada explicitamente pelo usuário e implementação localizada de 6.5 autorizada. Implementados histórico, cadastro único página/diálogo e detalhe, com SCSS locais; aguardando conferência real da implementação. TDD, 170 regressões direcionadas, cobertura frontend (98,27% linhas/91,98% ramos), build e 24 cenários de geometria registrados em `target/visual-operacoes-20261114/relatorio.md`. Questões funcionais abaixo preservadas separadamente; sem avanço para Dashboard/custódia nem conclusão transversal. A descrição inicial abaixo documenta o estado histórico da proposta antes da aprovação.

Análise de 2026-11-14, restrita a planejamento. Nenhuma implementação autorizada nesta rodada, nenhum checkbox concluído. Referências: PRD (RF13–RF17 e regras financeiras/custódia), README, design desta change e spec principal frontend-operation-management. Pendência conhecida de sacudida horizontal continua aberta, com investigação encerrada e evidências em verificacao-movimentacao.md; não será reaberta em 6.5.

## Implementação atual e reaproveitamento

- /operacoes: OperacoesListPageComponent, carteira global sincronizada com carteiraId na URL; GET /carteiras/{id}/operacoes. Sem busca, filtros por tipo/data/ativo, paginação ou opção Todas as carteiras. Preserva ordem recebida (dataOperacao, ordemNoDia, id ascendentes). CorretoraNamesService resolve nomes, incluindo fallback; nove colunas e collection.scss compartilhado. Abaixo de 70rem de largura disponível vira cards de duas colunas; abaixo de 36rem, uma coluna. São breakpoints do container, não do viewport.
- /operacoes/nova: OperacaoFormPageComponent único para rota e MatDialog, usado por Dashboard e detalhe da carteira. Captura carteira de origem; carteiraId é desabilitado e não existe seletor local. Mudança do seletor global não transfere rascunho para outra carteira. URL de origem e carteira são preservadas no retorno; contexto removido/inválido bloqueia cadastro. Troca explícita de query/contexto invalida requisições e rascunho anteriores.
- COMPRA consulta GET /operacoes/previa-compra por ticker/mercado/data; VENDA consulta GET /carteiras/{id}/operacoes/sugestao-preco-venda. Ambas sugestões são editáveis; respostas antigas ou posteriores à edição manual não sobrescrevem o campo. Mudança de tipo/ativo/data invalida preço/estimativa. Falha da sugestão não impede preço manual; sugestão nula de venda mantém preço obrigatório.
- Campos atuais: tipo, data, ação (ticker+mercado), quantidade, preço unitário e corretora opcional. Carteira capturada é contexto fixo. Formulário em duas colunas até 42rem e uma abaixo; diálogo extended existente 48rem, gutters de 1rem e scroll do overlay. Mensagens de altura dinâmica; estimativa textual exata existente de quantidade×preço, apenas informativa. Preço/quantidade são strings lossless; POST não recebe ordemNoDia, valorTotal ou campos derivados. Total registrado é autoritativo do backend.
- /operacoes/:id: detalhe somente leitura com DTO transitório compatível ou GET no acesso direto/reload. Retorno por operationReturnUrl; valores existentes, corretora por ID ou Sem corretora, 404 e retry. Não exibe ordemNoDia; teste existente protege esse comportamento. Duas colunas, uma abaixo de 36rem.
- Histórico da Carteira é outro template, já aprovado, com seu próprio carregamento e operações/posições independentes; não transformar em componente universal agora. Formulário, DTOs, formatadores e serviços já são compartilhados.

## Diagnóstico visual

Os fluxos principais já existem e devem ser reutilizados. Diferenças de acabamento: tabela de nove colunas usa largura igual e estilos genéricos, dificultando acomodar nomes/preços; cabeçalhos e formulário não adotam integralmente a escala local refinada; carteira do formulário aparece principalmente na descrição, pouco destacada diante do seletor global; campos não têm agrupamento semântico explícito; diálogo acumula compactação global e estilos locais. Observação humana de identidade verde no diálogo já consta em 6.5. Sem alegar nova conferência real de desktop/mobile nesta análise.

## Composição proposta

Identidade azul aprovada: título 28px desktop/24px mobile, contexto 12px, descrição/tabela 14px, dados financeiros tabulares e controles com alvo mínimo 44px. Cards com borda discreta, raio 12px, fundo branco, sem sombras pesadas. Compra com rótulo azul suave e Venda com rótulo neutro: venda não significa prejuízo. Todo tipo permanece textual.

### Histórico desktop

Cabeçalho compacto Operações, descrição objetiva e Nova operação somente ADMIN. Contexto de carteira identificado por nome e ID, sem segundo seletor. Uma coleção principal, sem resumos financeiros inventados, filtros ou chamadas novas. Recomendação de sete colunas agrupadas: Ativo/tipo (ticker, mercado, moeda e badge), Registro (data, ordem e carteira ID), Quantidade, Preço unitário, Valor total, Corretora e Ações. Todos os dados atuais continuam disponíveis. Colunas com larguras adequadas ao conteúdo; números à direita e ação Ver detalhes preservada. Não reordenar registros. A prévia compacta usa exemplos; larguras reais deverão ser verificadas na implementação, sem forçar tabela ilegível onde cards são mais adequados.

### Mobile 360/320

Mesmo conjunto semântico de registros, sem tabela/cards duplicados ativos. Nome da carteira integral com quebra; ação principal em linha própria. Card: ticker/mercado/moeda e tipo no topo, data/ordem abaixo, carteira, quantidade, preço, total, corretora e Ver detalhes. Usar pares onde couberem e empilhamento em uma coluna quando texto/quantidades longas exigirem, inclusive em 320px. Nenhum truncamento necessário à decisão; sem rolagem horizontal global. Valores completos e ação de 44px. A página usa padding 16px; no diálogo, a área disponível será menor devido aos gutters do overlay, exigindo validação específica.

### Formulário em página e diálogo

1. Cabeçalho Nova operação; retorno somente em página. Diálogo com título/descrição próprios e foco existente, sem copiar o shell.
2. Faixa discreta Registrar na carteira, nome e ID integral; contexto fixo informativo, sem novo campo de seleção.
3. Dados da negociação: tipo e data; ação em linha inteira; quantidade e preço em pares no desktop. Preservar ordem de tabulação e campos atuais. Uma coluna no mobile.
4. Corretora opcional em largura inteira; Sem corretora e mensagem de ausência preservados. Não apresentar este campo como obrigatório para VENDA nem prometer saldo sem consulta existente.
5. Estimativa existente separada visualmente, com mensagem de consulta/falha do preço junto ao campo, sem alterar gatilhos ou fórmulas. Não confundir estimativa com total persistido.
6. Registrar operação e Cancelar, botões consistentes; no mobile, empilhados. Preservar bloqueios de envio/diálogo, Escape/cancelamento e restauração de foco nos estados em que já são permitidos. Em submissão, manter anúncio e prevenção de POST duplicado.

Não acrescentar taxas, saldo disponível, posição atual, resultado projetado, ordem editável, calendário de pregões, novos cálculos ou consultas.

### Detalhe

Refinamento local de título, retorno e cartão, com os mesmos campos atuais; total registrado em destaque. Manter corretora por ID conforme contrato atual, sem GET para enriquecimento. Ordem não será acrescentada ao detalhe. Retorno seguro, 404, retry, DTO transitório e USER/ADMIN somente leitura preservados. Avaliar localmente o padrão de 16px superior, 8px retorno/cabeçalho e 20px cabeçalho/cartão, sem tocar shell.

## Estados e invariantes

- Listagem distingue carregamento de carteiras e de operações, falha/retry de cada etapa, carteira inválida/removida, nenhuma carteira e histórico vazio. Preservar tudo. Nenhum sucesso é inferido só pela renderização da lista.
- Formulário já distingue opções/contexto carregando, referências indisponíveis, ausência de ação, ausência opcional de corretora, consulta de preço, falha da sugestão, validação local, erro do POST e submissão. Sugestão não bloqueia entrada manual; falha de referências bloqueia submit. Não criar retry novo sem aprovação funcional.
- Sucesso em página retorna à origem e mostra toast; em diálogo, fecha retornando o DTO e quem abriu mostra toast. Dashboard recarrega dados somente da carteira ainda selecionada; Carteira incorpora DTO no histórico, recarrega posições e invalida distribuição, sem repetir POST ou reconsultar desnecessariamente a entidade.
- Preservar quantidade positiva, limite de 13 inteiros/6 decimais, inteiro matemático BRASIL/fração EUA; preço positivo com escala vigente; data civil não futura no fuso do mercado. Backend conserva validação de posição cronológica, saldo local/consolidado e impactos sobre custódias futuras. Nenhuma fórmula financeira será reimplementada.
- ADMIN cria, USER consulta; guard de rota, appAdmin, sessão/CSRF e autorização backend não mudam. Não adicionar botões de editar/excluir operações.

## Questões funcionais separadas — sem correção nesta proposta

1. Falha ao carregar ações pode coexistir com aviso Cadastre uma ação, pois a coleção continua vazia. O formulário não oferece retry de referências próprio. Distinguir falha de consulta de ausência confirmada exige decisão/teste de comportamento à parte.
2. Mensagens vazias atuais convidam ao cadastro mesmo para USER, embora o botão seja protegido. Uma redação específica por perfil deve ser aprovada explicitamente, sem alterar permissões.
3. Após POST bem-sucedido em página, cancelamento/rejeição de navegação não recebe feedback específico em complete(); não confundir falha de retorno com falha de registro. Eventual correção precisa de autorização e teste próprio contra nova escrita indevida.

São constatações de código, não novos defeitos reproduzidos na aplicação real. Não serão absorvidas silenciosamente na modernização visual.

## Escopo de arquivos e impactos

- Alterações futuras: pages/operacoes-list-page.component.ts, pages/operacao-form-page.component.ts, pages/operacao-detail-page.component.ts; novos SCSS locais operacoes-list.scss, operacao-form.scss, operacao-detail.scss e, se reduzir duplicação local, _operacoes-typography.scss. Testes correspondentes apenas quando necessário.
- Reutilizar PageHeader, StickyBack, FeedbackAlert, AppIcon, Material e toast. Consumir tokens públicos com escopo local; sem mudar estilos globais, collection.scss, overlay compartilhado ou PageHeader nesta proposta. Diálogo pode trocar preset visual apenas no componente de Operações, com análise do efeito nos dois chamadores antes da implementação.
- Preservar services, validators, lossless-operation-json, DTOs, routes, operation-origin e contexto de carteira. Testá-los como invariantes, sem refatoração.
- Dashboard e Carteira recebem o novo visual do mesmo formulário contextual; exigem regressão de foco/cancelamento/pending e sucesso/troca de carteira. Não alterar o layout financeiro nem o histórico aprovado da Carteira; sem novas requisições, cálculos ou eventos.

## OpenSpec e validação futura

Tarefa principal 6.5: histórico contextual, página/diálogo e detalhe, incluindo a observação mobile já registrada. 6.6: regressões afetadas e equivalência de HTTP/payload/gatilhos/lossless. 8.1/8.2: evidência responsiva e acessibilidade, sem concluir o escopo transversal por esta etapa. 8.3/8.4: portões de cobertura/build preservados. 7.x de Dashboard/custódia não é autorizada por modernizar o formulário compartilhado.

Fronteiras propostas para TDD após aprovação: UI pública dos campos/mensagens/ações; integração de contexto e origem; abertura/fechamento MatDialog e resultado confirmado. Para comportamento novo aprovado, um cenário RED → implementação mínima GREEN → revisão por vez. Reutilizar testes existentes, sem alegar TDD retroativo e sem testes artificiais de CSS em jsdom.

Priorizar operacoes-list-page.component.spec.ts, operacao-form-page.component.spec.ts, operacao-detail-page.component.spec.ts, operacao-context.integration.spec.ts, operacoes.service.spec.ts, operacao-validators.spec.ts, lossless-operation-json.spec.ts e operacoes.routes.spec.ts. Acrescentar somente cenários dos dois chamadores em dashboard-page.component.spec.ts, carteira-detail-page.component.spec.ts e carteiras-dialog.integration.spec.ts quando afetados. Reaproveitar guards/perfis existentes para ADMIN/USER.

Durante implementação: ciclos direcionados, inspeção visual em desktop/360/320 (página e diálogo, nomes/preços longos, erros, teclado/foco), um build production final e OpenSpec estrito. Planejar uma execução de cobertura frontend ao concluir o escopo implementado conforme AGENTS.md, sem repetir o portão global a cada ajuste de CSS; manter limites globais de linhas/ramos 80% e budgets atuais. Não rodar backend sem alteração/justificativa. Nesta fase de análise não foram executados testes/build.

Prévias conceituais, com dados fictícios, preparadas fora do repositório na pasta de visualizações da conversa. Não comprovam renderização Angular/Material, foco real ou equivalência pixel a pixel. Implementação depende da aprovação explícita do usuário.
