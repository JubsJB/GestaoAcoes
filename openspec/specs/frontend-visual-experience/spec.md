# frontend-visual-experience Specification

## Purpose
Estabelecer uma experiência visual transversal, profissional, responsiva e acessível para o frontend, com identidade oliva, tokens semânticos e padrões consistentes de conteúdo, ações e feedback sem confundir marca com resultado financeiro.

## Requirements

### Requirement: Identidade visual e tokens semânticos
A aplicação SHALL aplicar uma identidade Material 3 clara cuja cor principal de marca é `#5F6F52`, usando `#A9B388` para apoio, seleção e hover, superfície geral neutra próxima de `#F8F8F3`, cards claros e texto grafite com contraste adequado. A aplicação MUST usar tokens distintos para marca, `success`, `financial-positive`, `negative`, `error` e `warning`, e MUST NOT usar o verde institucional como única representação de lucro.

#### Scenario: Identidade oliva aplicada
- **WHEN** o shell e uma página funcional são apresentados
- **THEN** a identidade principal usa verde oliva e superfícies neutras sem manter cyan ou azure como branding dominante

#### Scenario: Marca separada de resultado financeiro
- **WHEN** marca, lucro, prejuízo, sucesso, warning ou erro são apresentados
- **THEN** cada conceito usa semântica visual própria e informação adicional à cor

#### Scenario: Contraste das combinações
- **WHEN** texto ou ícone é exibido sobre uma cor da paleta
- **THEN** a combinação atende contraste adequado ao uso e cores claras como `#A9B388` e `#B99470` não recebem texto branco quando isso for insuficiente

### Requirement: Tema local e reproduzível
A identidade visual SHALL permanecer baseada em Angular Material 3, tema claro, fontes do sistema, densidade compatível e indicadores fortes de foco. Ela MUST NOT exigir segunda biblioteca visual, fonte, ícone ou asset remoto obrigatório.

#### Scenario: Execução sem recursos visuais remotos
- **WHEN** a aplicação é carregada sem acesso a recursos visuais externos
- **THEN** tema, tipografia, navegação e controles permanecem íntegros e utilizáveis

#### Scenario: Foco forte preservado
- **WHEN** um usuário navega por teclado
- **THEN** controles interativos continuam apresentando foco visível sobre as novas superfícies

### Requirement: Container e superfícies de conteúdo
O conteúdo principal SHALL usar container centralizado com largura máxima estrutural entre `72rem` e `80rem`, padding responsivo e fluidez suficiente para desktop, tablet e mobile. Cards e superfícies SHALL usar fundo claro, borda sutil, raio e elevação discretos e espaçamento consistente, com hover somente quando o elemento for interativo. Coleções SHALL priorizar densidade útil, hierarquia clara e dados existentes sem impor tabela rígida ao mobile nem criar informações derivadas.

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

### Requirement: Cabeçalho compartilhado de página
Páginas funcionais SHALL usar um padrão compartilhado de cabeçalho que forneça um único `h1`, descrição opcional e área opcional para ação principal, sem conter lógica de negócio. Feedback relevante SHALL aparecer imediatamente após o cabeçalho quando essa posição for semanticamente apropriada.

#### Scenario: Cabeçalho com ação no desktop
- **WHEN** uma página possui ação principal e espaço horizontal suficiente
- **THEN** título e descrição permanecem agrupados e a ação é apresentada com hierarquia clara

#### Scenario: Cabeçalho em viewport compacto
- **WHEN** o cabeçalho é exibido em viewport compacto
- **THEN** conteúdo e ação podem empilhar sem perda de ordem semântica ou usabilidade

#### Scenario: Nome longo de entidade
- **WHEN** um título ou nome empresarial é extenso
- **THEN** o texto quebra em múltiplas linhas com escala e line-height legíveis, sem truncamento obrigatório nem overflow horizontal

### Requirement: Feedback contextual compartilhado
A aplicação SHALL oferecer feedback contextual nas variantes `success`, `info`, `warning` e `error`, com texto, detalhes opcionais e semântica assistiva compatível com o propósito. Feedback MUST NOT depender somente de cor nem criar outro formato de erro; mensagens e detalhes de `StandardError` SHALL permanecer disponíveis quando aplicáveis. Sucesso transitório de operação SHALL ser apresentado como toast acessível próximo ao topo da área visível, sem cobrir toolbar ou conteúdo essencial, enquanto erros técnicos ou externos SHALL permanecer no feedback contextual da página.

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

### Requirement: Hierarquia consistente de ações e formulários
A aplicação SHALL diferenciar ação principal, busca/atualização/retry e ações de cancelar/limpar/voltar por peso visual coerente. Uma ação textual de retorno SHALL permanecer disponível sem sobrepor toolbar ou conteúdo, SHALL navegar ao destino previsto e SHALL preservar operação por teclado e nome acessível completo. Formulários SHALL preservar Typed Reactive Forms, campos fluidos, superfície própria, largura confortável entre `40rem` e `44rem` como referência e agrupamento responsivo de ações, inclusive quando apresentados em dialog contextual.

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

### Requirement: Estados visuais distintos
Listas e buscas SHALL diferenciar coleção vazia, busca sem correspondência, erro técnico e erro externo por título, mensagem, ação e semântica apropriados. Uma ausência local decorrente de busca explícita SHALL usar dialog contextual acessível, enquanto coleção vazia SHALL permanecer inline e erros técnicos ou externos SHALL permanecer no feedback de página. Uma busca sem correspondência MUST NOT ser apresentada como coleção vazia ou erro técnico.

#### Scenario: Coleção vazia
- **WHEN** a coleção carregada não possui registros
- **THEN** a página informa que ainda não há registros e oferece ação inicial quando aplicável

#### Scenario: Busca sem correspondência
- **WHEN** uma consulta local válida não encontra registro
- **THEN** um dialog informativo identifica o termo pesquisado e oferece CTA contextual sem substituir a coleção preservada

#### Scenario: Dialog de ausência local acessível
- **WHEN** o dialog de ausência local é aberto
- **THEN** ele possui título e descrição associados, backdrop, foco contido, fechamento por Escape, restauração de foco e ações principal e secundária operáveis sem depender somente de cor

#### Scenario: Erro técnico recuperável
- **WHEN** o carregamento da página falha tecnicamente
- **THEN** a página apresenta mensagem clara e retry explícito quando aplicável

#### Scenario: Erro externo
- **WHEN** uma integração externa falha por meio do backend
- **THEN** a página mantém o contexto e os dados padronizados da falha sem classificá-la como ausência local

### Requirement: Apresentação consistente de data e hora
Valores `OffsetDateTime` recebidos como string SHALL permanecer inalterados na camada de dados e SHALL ser convertidos somente na apresentação com locale `pt-BR`, timezone local do navegador e padrão `dd/MM/yyyy às HH:mm`. Datas civis como futura `dataOperacao` em `YYYY-MM-DD` MUST NOT receber conversão de timezone por esta regra.

#### Scenario: OffsetDateTime apresentado
- **WHEN** uma data/hora de cadastro ou cotação é exibida
- **THEN** ela aparece em português do Brasil no padrão aprovado e no timezone local do navegador

#### Scenario: DTO preservado
- **WHEN** o valor temporal é formatado
- **THEN** a string original do DTO não é alterada nem substituída

#### Scenario: Data civil preservada
- **WHEN** uma data sem horário representar futuramente uma data civil de operação
- **THEN** ela não é deslocada por timezone como se fosse `OffsetDateTime`

### Requirement: Responsividade e acessibilidade visual
Os padrões visuais SHALL preservar o breakpoint estrutural de `960px` do shell e MAY usar breakpoint visual adicional próximo de `36rem` para conteúdo. Contraste, foco, teclado, labels, títulos, regiões dinâmicas, estado ocupado e navegação ativa além da cor MUST permanecer verificáveis.

#### Scenario: Shell estrutural preservado
- **WHEN** o viewport cruza o breakpoint de `960px`
- **THEN** somente o comportamento já aprovado de navegação side/over é aplicado

#### Scenario: Conteúdo compacto
- **WHEN** cards, formulários, alertas, títulos ou ações são exibidos em viewport compacto
- **THEN** permanecem legíveis e operáveis sem overflow horizontal obrigatório

#### Scenario: Estado não dependente de cor
- **WHEN** feedback, seleção ou resultado financeiro é exibido
- **THEN** texto, estrutura, ícone local ou atributo semântico comunica o significado além da cor
