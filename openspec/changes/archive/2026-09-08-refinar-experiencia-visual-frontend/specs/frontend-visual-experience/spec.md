## MODIFIED Requirements

### Requirement: Identidade visual e tokens semânticos
A aplicação SHALL aplicar uma identidade Material 3 clara, financeira e orientada a dados, com verde como marca principal, superfícies neutras, cards claros, texto grafite e ornamentação discreta. O oliva existente MAY ter tonalidades, contraste e cores de apoio refinados sem conservar hexadecimais exatos. A aplicação MUST usar tokens distintos para marca, `success`, `financial-positive`, `negative`, `error` e `warning`, e MUST NOT usar o verde institucional como única representação de lucro. A semântica SHALL abranger info, ação destrutiva, bordas, superfícies, texto, hover e foco; marca, sucesso e lucro, assim como erro e prejuízo, MUST NOT ser tratados como o mesmo papel. Valores visuais equivalentes compartilhados SHALL ser centralizados sem renomear tokens apenas por cosmética.

#### Scenario: Identidade oliva aplicada
- **WHEN** o shell e uma página funcional são apresentados
- **THEN** a identidade principal preserva o verde, com refinamento permitido do oliva e superfícies neutras, sem usar cyan ou azure como branding dominante

#### Scenario: Marca separada de resultado financeiro
- **WHEN** marca, lucro, prejuízo, sucesso, warning ou erro são apresentados
- **THEN** cada conceito usa semântica visual própria e informação adicional à cor

#### Scenario: Contraste das combinações
- **WHEN** texto ou ícone é exibido sobre uma cor da paleta
- **THEN** a combinação atende WCAG 2.2 AA aplicável e superfícies de apoio claras não recebem texto branco quando o contraste for insuficiente

#### Scenario: Ação destrutiva distinguível
- **WHEN** uma ação Excluir é apresentada
- **THEN** seu tratamento comunica destruição por texto e semântica visual própria, distinguindo-a da ação primária de marca sem mudar confirmação ou efeito

#### Scenario: Refinamento de tonalidades
- **WHEN** as tonalidades de marca e apoio são refinadas
- **THEN** verde permanece como identidade principal e as combinações de texto, estados e superfícies são verificadas sem obrigação de conservar os hexadecimais anteriores


### Requirement: Tema local e reproduzível
A identidade visual SHALL permanecer baseada em Angular Material 3, tema claro, fontes do sistema, densidade compatível e indicadores fortes de foco. Ela MUST NOT exigir segunda biblioteca visual, fonte, ícone ou asset remoto obrigatório. Angular Material/CDK, fontes do sistema e sprite SVG local SHALL ser preservados. Esta evolução MUST NOT implementar tema escuro, seletor de tema ou persistência de preferência; tokens MAY apenas preparar semântica para evolução futura.

#### Scenario: Execução sem recursos visuais remotos
- **WHEN** a aplicação é carregada sem acesso a recursos visuais externos
- **THEN** tema, tipografia, navegação e controles permanecem íntegros e utilizáveis

#### Scenario: Foco forte preservado
- **WHEN** um usuário navega por teclado
- **THEN** controles interativos continuam apresentando foco visível sobre as novas superfícies

#### Scenario: Tema claro sem seleção nova
- **WHEN** o usuário abre a aplicação refinada
- **THEN** a experiência permanece clara, sem controle de alternância, preferência persistida ou recurso visual remoto obrigatório


### Requirement: Container e superfícies de conteúdo
O conteúdo principal SHALL usar container centralizado com largura máxima estrutural entre `72rem` e `80rem`, padding responsivo e fluidez suficiente para desktop, tablet e mobile. Cards e superfícies SHALL usar fundo claro, borda sutil, raio e elevação discretos e espaçamento consistente, com hover somente quando o elemento for interativo. Coleções SHALL priorizar densidade útil, hierarquia clara e dados existentes sem impor tabela rígida ao mobile nem criar informações derivadas. Spacing, radius e elevação SHALL seguir escalas compartilhadas por papel, com densidade adequada à tarefa e maior protagonismo dos dados que da decoração. Formulários simples e gestão cadastral MUST NOT receber indicadores ou se transformar em dashboards.

#### Scenario: Conteúdo em viewport amplo
- **WHEN** uma página é exibida em tela ampla
- **THEN** seu conteúdo permanece centralizado e legível sem ocupar desnecessariamente toda a largura disponível

#### Scenario: Superfícies com papéis distintos
- **WHEN** cards de entidade, busca, formulário, detalhe ou estado informativo são exibidos
- **THEN** eles compartilham linguagem visual coerente sem perder sua função semântica

#### Scenario: Card não interativo
- **WHEN** uma superfície não possui ação sobre o card completo
- **THEN** ela não apresenta hover que sugira clicabilidade inexistente

#### Scenario: Coleção com densidade responsiva
- **WHEN** uma listagem possui múltiplos registros
- **THEN** seus itens apresentam identificação, metadados existentes e ação em composição compacta no desktop e em uma coluna legível no mobile, com suporte a nomes longos

#### Scenario: Função da página preservada
- **WHEN** uma página de cadastro, detalhe ou coleção é refinada
- **THEN** sua composição continua orientada à tarefa original, reutiliza superfícies e não acrescenta dados para ornamentação


### Requirement: Cabeçalho compartilhado de página
Páginas funcionais SHALL usar um padrão compartilhado de cabeçalho que forneça um único `h1`, descrição opcional e área opcional para ação principal, sem conter lógica de negócio. Feedback relevante SHALL aparecer imediatamente após o cabeçalho quando essa posição for semanticamente apropriada. O cabeçalho SHALL acomodar presença ou ausência de ícone sem coluna vazia que comprometa título/ação. Título, contexto, metadado e ajuda SHALL possuir hierarquia consistente e descrição concisa; a página não encontrada SHALL compartilhar essa linguagem sem mudar seu destino ou URL.

#### Scenario: Cabeçalho com ação no desktop
- **WHEN** uma página possui ação principal e espaço horizontal suficiente
- **THEN** título e descrição permanecem agrupados e a ação é apresentada com hierarquia clara

#### Scenario: Cabeçalho em viewport compacto
- **WHEN** o cabeçalho é exibido em viewport compacto
- **THEN** conteúdo e ação podem empilhar sem perda de ordem semântica ou usabilidade

#### Scenario: Nome longo de entidade
- **WHEN** um título ou nome empresarial é extenso
- **THEN** o texto quebra em múltiplas linhas com escala e line-height legíveis, sem truncamento obrigatório nem overflow horizontal

#### Scenario: Cabeçalho sem ícone
- **WHEN** o título é apresentado sem ícone decorativo
- **THEN** o texto ocupa a largura útil, a ação mantém posição coerente e não há sobreposição em desktop, mobile ou zoom


### Requirement: Feedback contextual compartilhado
A aplicação SHALL oferecer feedback contextual nas variantes `success`, `info`, `warning` e `error`, com texto, detalhes opcionais e semântica assistiva compatível com o propósito. Feedback MUST NOT depender somente de cor nem criar outro formato de erro; mensagens e detalhes de `StandardError` SHALL permanecer disponíveis quando aplicáveis. Sucesso transitório de operação SHALL ser apresentado como toast acessível próximo ao topo da área visível, sem cobrir toolbar ou conteúdo essencial, enquanto erros técnicos ou externos SHALL permanecer no feedback contextual da página. Variantes SHALL usar dimensões, spacing, ícones locais e superfícies coerentes também em dialogs. Loading SHALL preservar spinner e texto acessível; skeletons não são obrigatórios e, se usados, MUST ser decorativos, sem valores financeiros fictícios ou substituição do anúncio de carregamento.

#### Scenario: Feedback urgente
- **WHEN** um erro ou warning exige atenção imediata
- **THEN** o feedback usa semântica de alerta e é apresentado em posição de destaque sem remover validações locais dos campos

#### Scenario: Feedback informativo
- **WHEN** uma informação ou sucesso contextual é anunciado sem urgência
- **THEN** o feedback usa região de status adequada e não interrompe desnecessariamente tecnologia assistiva

#### Scenario: StandardError preservado
- **WHEN** uma feature fornece `message` e `details` de erro padronizado
- **THEN** o feedback os apresenta sem substituir, ocultar ou reinterpretar seus dados técnicos

#### Scenario: Toast de sucesso transitório
- **WHEN** uma operação conclui com sucesso transitório
- **THEN** a infraestrutura Material existente apresenta toast curto com semântica de sucesso, descarte automático em oito segundos (`8000 ms`) e posicionamento superior responsivo, sem se tornar a única comunicação de informação persistente relevante

#### Scenario: Carregamento sem dados simulados
- **WHEN** uma página ou seção aguarda resposta
- **THEN** spinner e mensagem textual identificam o carregamento; eventual skeleton não contém valores financeiros e não gera anúncios duplicados

#### Scenario: Toast e overlays
- **WHEN** há feedback superior ou dialog em viewport compacto ou baixa altura
- **THEN** mensagem e fechamento ficam legíveis sem cobrir controles essenciais e o toast preserva duração, descarte e semântica existentes


### Requirement: Hierarquia consistente de ações e formulários
A aplicação SHALL diferenciar ação principal, busca/atualização/retry e ações de cancelar/limpar/voltar por peso visual coerente. Uma ação textual de retorno SHALL permanecer disponível sem sobrepor toolbar ou conteúdo, SHALL navegar ao destino previsto e SHALL preservar operação por teclado e nome acessível completo. Formulários SHALL preservar Typed Reactive Forms, campos fluidos, superfície própria, largura confortável entre `40rem` e `44rem` como referência e agrupamento responsivo de ações, inclusive quando apresentados em dialog contextual. Dialogs de busca, cadastro, edição, confirmação e exclusão SHALL compartilhar convenções de cabeçalho, largura, espaçamento e ações conforme sua função. Ação destrutiva SHALL ser distinta da primária; máscaras, formatadores, campos, validators, prefill, cancelamento e submissão MUST permanecer inalterados.

#### Scenario: Hierarquia de botões
- **WHEN** uma página apresenta ações primária, secundária e terciária
- **THEN** elas usam respectivamente ênfase filled, outlined e text quando apropriado, sem receber todas o mesmo peso

#### Scenario: Ação de retorno disponível
- **WHEN** uma página apresenta ação textual de retorno
- **THEN** a ação permanece operável por teclado, possui nome acessível completo e navega ao destino previsto sem sobrepor toolbar ou conteúdo

#### Scenario: Formulário em dialog contextual
- **WHEN** um cadastro é iniciado pela listagem ou por CTA de busca sem correspondência
- **THEN** o formulário é apresentado em dialog acessível e responsivo com ações explícitas de cancelar e cadastrar, sem alterar seus campos ou contrato HTTP

#### Scenario: Formulário amplo
- **WHEN** um formulário é exibido no desktop
- **THEN** ele permanece em largura confortável e não se expande para toda a área disponível

#### Scenario: Ações no mobile
- **WHEN** formulário ou cabeçalho é exibido em viewport compacto
- **THEN** ações podem ocupar a largura disponível e empilhar na ordem coerente

#### Scenario: Formulário simples
- **WHEN** cadastro de Corretora, Ação ou Carteira é exibido
- **THEN** somente os campos atuais são solicitados, em superfície confortável, sem wizard ou etapas novas

#### Scenario: Confirmação coerente
- **WHEN** uma confirmação cadastral ou destrutiva é aberta
- **THEN** título, mensagem e ações seguem o padrão compartilhado, preservando decisão explícita, foco, cancelamento e bloqueios pendentes


### Requirement: Responsividade e acessibilidade visual
Os padrões visuais SHALL preservar o breakpoint estrutural de `960px` do shell e MAY usar breakpoint visual adicional próximo de `36rem` para conteúdo. Contraste, foco, teclado, labels, títulos, regiões dinâmicas, estado ocupado e navegação ativa além da cor MUST permanecer verificáveis. A apresentação SHALL atender aos critérios aplicáveis de WCAG 2.2 nível AA: texto normal com contraste mínimo 4.5:1, texto grande 3:1, componentes e indicadores gráficos necessários 3:1; teclado, foco visível não encoberto, nomes/papéis/estados, labels, headings, associações de erro e regiões alert/status. Alvos SHALL atender 24×24 CSS px ou espaçamento/exceções documentadas do critério 2.5.8. Texto SHALL admitir ampliação a 200% e conteúdo SHALL refluir a 320 CSS px, sem perda de informação/ação ou rolagem horizontal obrigatória nesta interface. Baixa altura, reduced motion e ausência de double scroll SHALL ser verificados.

#### Scenario: Shell estrutural preservado
- **WHEN** o viewport cruza o breakpoint de `960px`
- **THEN** somente o comportamento já aprovado de navegação side/over é aplicado

#### Scenario: Conteúdo compacto
- **WHEN** cards, formulários, alertas, títulos ou ações são exibidos em viewport compacto
- **THEN** permanecem legíveis e operáveis sem overflow horizontal obrigatório

#### Scenario: Estado não dependente de cor
- **WHEN** feedback, seleção ou resultado financeiro é exibido
- **THEN** texto, estrutura, ícone local ou atributo semântico comunica o significado além da cor

#### Scenario: Zoom e baixa altura
- **WHEN** a página é ampliada a 200%, avaliada a 320 CSS px ou usada em viewport de baixa altura
- **THEN** conteúdo, formulário, ações e dialog continuam alcançáveis na região de rolagem prevista, sem cortes ou foco encoberto

#### Scenario: Movimento reduzido
- **WHEN** a preferência prefers-reduced-motion está ativa
- **THEN** movimento decorativo é removido ou reduzido sem ocultar progresso ou alterar resposta funcional

#### Scenario: Teclado e diálogo
- **WHEN** a interação ocorre sem mouse
- **THEN** ordem de foco é coerente, labels e mensagens são compreensíveis, o dialog contém e restaura foco e cancelamento preserva o fluxo aprovado


## ADDED Requirements

### Requirement: Hierarquia tipográfica e leitura financeira
A interface SHALL distinguir título, contexto, indicador, metadado e ajuda, evitando labels essenciais excessivamente pequenas. Coleções financeiras SHALL apresentar texto à esquerda e dinheiro, quantidade e percentual alinhados à direita onde comparáveis, com algarismos tabulares e moeda explícita no contexto necessário. A apresentação de quantidades SHALL remover zeros decimais finais sem perder casas significativas, reutilizando formatação textual lossless. Valores monetários SHALL usar R$ ou US$ e duas casas com o arredondamento visual existente; percentuais SHALL manter o padrão aprovado. As strings originais, sinais, modelos, payloads, máscaras editáveis e cálculos MUST permanecer inalterados, sem conversão financeira para Number/parseFloat nem regravação de valores formatados.

#### Scenario: Comparação de valores
- **WHEN** uma coleção exibe dinheiro, quantidade e percentual
- **THEN** colunas numéricas mantêm alinhamento e algarismos tabulares, enquanto identificação e moeda permanecem claras

#### Scenario: Valor extenso e resultado
- **WHEN** há decimal longo, valor negativo ou nome extenso
- **THEN** apresentação preserva conteúdo e sinal, oferece a informação textual de resultado exigida e não trunca dados para caber


### Requirement: Linguagem orientada ao investidor
A interface SHALL apresentar linguagem orientada ao domínio do investidor e SHALL evitar terminologia de implementação técnica quando ela não for necessária para compreensão, diagnóstico ou requisito funcional. Textos editoriais como “valores autoritativos”, “persistida”, “backend” e “fonte acessível principal” SHALL ser revisados sem mudar o significado dos dados; mensagem e detalhes de erro, avisos financeiros necessários e nomes assistivos MUST ser preservados quando exigidos.

#### Scenario: Texto de apoio editorial
- **WHEN** um cabeçalho ou descrição explica informação financeira sem necessidade de diagnóstico
- **THEN** o texto usa linguagem do investidor e mantém moeda, referência temporal, caráter estimado ou registrado e demais distinções necessárias

#### Scenario: Diagnóstico preservado
- **WHEN** um erro ou requisito exige detalhe técnico
- **THEN** mensagem, código e detalhes relevantes continuam disponíveis; a revisão editorial não os suprime nem altera a classificação funcional


### Requirement: Coleções comparáveis com representação responsiva única
Coleções tabulares SHALL usar cabeçalhos associados e semântica de tabela no desktop e cards completos no mobile, com separadores discretos e ações estáveis. Apenas uma representação SHALL estar ativa para tecnologia assistiva e teclado por breakpoint. A troca de apresentação MUST NOT duplicar efeitos, requisições, dados ou anúncios nem introduzir ordenação, filtros, paginação ou buscas. Hover de linha inteira MUST NOT sugerir uma ação inexistente.

#### Scenario: Tabela no desktop
- **WHEN** a largura útil comporta a coleção tabular
- **THEN** cabeçalhos identificam células, números são comparáveis e ações têm nomes acessíveis sem tornar toda linha falsamente clicável

#### Scenario: Cards no mobile
- **WHEN** a largura útil exige apresentação compacta
- **THEN** cada card preserva todos os campos apresentados no desktop e somente a representação visível participa da leitura e foco

#### Scenario: Mudança de largura
- **WHEN** o viewport cruza o breakpoint da coleção
- **THEN** ordem, conteúdo e contexto permanecem iguais sem novas requisições ou controles funcionais


### Requirement: Refinamento visual sem alteração funcional
A modernização SHALL preservar contratos e comportamento aprovados: backend, endpoints, DTOs, banco, migrations, regras financeiras, compra/venda, preço médio, arredondamento, parsing lossless, precisão decimal, BRL/USD sem FX, snapshots manuais, rotas, query params, validações, máscaras, formatadores, payloads e conjunto funcional de requisições. MUST NOT criar dados, métricas, totalizações, enriquecimento de IDs com chamadas adicionais ou mudanças de gatilhos HTTP. Interações locais de apresentação e acessibilidade MAY ser refinadas sem efeitos de domínio.

#### Scenario: Mesma ação antes e depois
- **WHEN** uma ação funcional é executada com as mesmas entradas e contexto
- **THEN** métodos, URLs, parâmetros, payloads, regras de disparo e tratamento de resposta permanecem equivalentes

#### Scenario: Identificadores sem enriquecimento
- **WHEN** o contrato fornece apenas um ID
- **THEN** a interface mantém a identificação disponível sem novas consultas ou nome inventado

#### Scenario: Ajuste visual ou assistivo
- **WHEN** um breakpoint, tooltip, foco ou estilo muda
- **THEN** nenhuma requisição de domínio, cálculo ou mutação de carteira é causada pelo ajuste


### Requirement: Entrega visual sem expansão de dependências e budgets
A modernização MUST NOT adicionar dependências, fontes ou assets remotos obrigatórios, elevar budgets ou desfazer limites lazy. O initial bundle de produção SHALL ser comparado antes/depois na mesma configuração e ambiente e não superar o baseline medido antes das alterações; o valor informado de aproximadamente 506,09 kB é referência, não medição desta change. Budgets vigentes de initial e estilos por componente SHALL permanecer inalterados. Otimização MUST NOT reduzir dataset ou funcionalidades.

#### Scenario: Comparação reproduzível
- **WHEN** a implementação é validada para aceite
- **THEN** relatório registra ambiente, initial antes/depois, delta e warnings; não há crescimento sobre o baseline medido nem budget elevado

#### Scenario: Dependências e carregamento
- **WHEN** o frontend refinado é construído e suas rotas carregadas
- **THEN** manifestos e lockfile permanecem inalterados, limites lazy são preservados e nenhum recurso visual remoto passa a ser obrigatório


