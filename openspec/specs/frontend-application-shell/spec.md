# frontend-application-shell Specification

## Purpose

Oferecer um shell visual consistente, responsivo e acessível para que as áreas do frontend possam evoluir sobre navegação estável sem antecipar comportamento de negócio.

## Requirements

### Requirement: Base visual consistente e reproduzível
A aplicação SHALL utilizar uma base visual Angular Material 3 clara e compatível com a foundation, sem depender de fontes, ícones ou assets remotos em runtime. As cores principais de branding e navegação SHALL usar a identidade verde oliva definida por `frontend-visual-experience`, mantendo cores de marca separadas das semânticas financeira, de warning e de erro.

#### Scenario: Tema global disponível
- **WHEN** qualquer destino estrutural é exibido
- **THEN** o tema claro global fornece aparência oliva consistente e legível ao shell e ao conteúdo

#### Scenario: Recursos visuais locais
- **WHEN** a aplicação é carregada sem acesso a provedores externos de fontes, ícones ou assets
- **THEN** o shell mantém conteúdo, navegação e controles utilizáveis sem realizar requisição a esses provedores

#### Scenario: Semântica financeira independente
- **WHEN** o shell ou uma página usa a cor institucional da marca
- **THEN** essa cor não define por si só lucro, prejuízo, sucesso ou erro

### Requirement: Shell principal da aplicação
A aplicação SHALL apresentar um shell único ocupando a viewport, contendo toolbar, navegação lateral e uma área principal que renderiza o destino ativo. A toolbar SHALL exibir o nome “Gestão de Ações” com identidade oliva, conteúdo claro e contraste adequado, e a área principal SHALL aplicar o container centralizado aprovado sem impedir layouts fluidos. No desktop, toolbar e sidebar SHALL permanecer estruturalmente estáveis enquanto a área de trabalho ocupa o espaço restante; no compacto, a toolbar SHALL permanecer estável e o drawer SHALL continuar sobreposto. A composição MUST evitar conteúdo oculto, scroll horizontal e dois scrolls concorrentes.

#### Scenario: Renderização do shell
- **WHEN** uma rota pertencente à aplicação é acessada
- **THEN** toolbar, navegação principal e área de conteúdo são apresentadas como uma estrutura visual coesa

#### Scenario: Identificação da aplicação
- **WHEN** o shell é renderizado
- **THEN** a toolbar exibe o texto “Gestão de Ações” com hierarquia e contraste adequados

#### Scenario: Conteúdo do destino ativo
- **WHEN** a navegação resolve um destino estrutural
- **THEN** seu conteúdo é renderizado no container da área principal sem substituir o shell

#### Scenario: Toolbar responsiva
- **WHEN** o shell alterna entre desktop e viewport compacto
- **THEN** a toolbar mantém altura coerente com aproximadamente 64px e 56px respectivamente, preservando título e botão de menu mobile

#### Scenario: Área de trabalho no desktop
- **WHEN** o shell desktop apresenta conteúdo maior que a viewport
- **THEN** toolbar e sidebar permanecem estruturalmente estáveis e a rolagem ocorre na região de trabalho definida sem mover todo o documento nem criar double scroll

#### Scenario: Área de trabalho no mobile
- **WHEN** o shell compacto apresenta conteúdo rolável
- **THEN** a toolbar permanece disponível, o conteúdo respeita sua altura e o drawer sobreposto não introduz scroll horizontal

#### Scenario: Regiões de uma página de coleção
- **WHEN** uma página separa cabeçalho, controles e uma coleção longa
- **THEN** a coleção constitui a região principal de rolagem quando houver altura disponível, sem ocultar contexto ou controles nem exigir virtual scrolling

### Requirement: Navegação principal entre áreas
O shell SHALL oferecer destinos para Dashboard, Corretoras, Ações, Carteiras e Operações com URLs `/dashboard`, `/corretoras`, `/acoes`, `/carteiras` e `/operacoes`. Cada item SHALL apresentar ícone local decorativo antes do label, com coluna visual, alinhamento e espaçamento consistentes no desktop e no drawer compacto, sem fonte de ícones ou asset remoto. O destino ativo SHALL usar superfície sage clara, marcador estrutural e `aria-current`, MUST NOT ser identificado somente por cor e SHALL manter hover e foco discretos e perceptíveis.

#### Scenario: Destinos principais disponíveis
- **WHEN** a navegação principal é exibida
- **THEN** ela contém os destinos principais Dashboard, Corretoras, Ações, Carteiras e Operações com os respectivos rótulos e URLs

#### Scenario: Rota inicial
- **WHEN** o usuário acessa a raiz `/`
- **THEN** a aplicação realiza redirect exato para `/dashboard`

#### Scenario: Indicação do destino ativo
- **WHEN** um dos cinco destinos está ativo
- **THEN** seu item apresenta superfície selecionada, indicador adicional à cor e comunicação semântica de página atual

#### Scenario: Interação com item inativo
- **WHEN** um item de navegação recebe hover ou foco
- **THEN** seu estado permanece perceptível sem competir visualmente com o item ativo

#### Scenario: Composição iconográfica dos destinos
- **WHEN** a navegação principal é apresentada no desktop ou no drawer compacto
- **THEN** cada ícone aparece antes do respectivo label, usa a mesma coluna visual e fica oculto de tecnologia assistiva quando o texto já fornece o nome acessível

### Requirement: Navegação responsiva
O shell SHALL adaptar a navegação ao viewport preservando a largura estrutural atual da sidebar. Em largura igual ou superior a 960px, a navegação lateral SHALL permanecer aberta em modo persistente. Em largura inferior a 960px, ela SHALL operar como drawer sobreposto, iniciar fechada e oferecer controle acessível para abertura e fechamento.

#### Scenario: Navegação persistente em desktop
- **WHEN** o viewport possui largura igual ou superior a 960px
- **THEN** a navegação lateral permanece aberta ao lado do conteúdo principal sobre superfície clara e neutra

#### Scenario: Drawer em viewport compacto
- **WHEN** o viewport possui largura inferior a 960px
- **THEN** a navegação inicia fechada, sobrepõe o conteúdo ao abrir e pode ser fechada pelo backdrop ou pela tecla Escape

#### Scenario: Controle de menu compacto
- **WHEN** o shell está em viewport compacto
- **THEN** um controle de menu identificável permite alternar a abertura do drawer

#### Scenario: Fechamento após seleção
- **WHEN** o usuário seleciona um destino no drawer em viewport compacto
- **THEN** a navegação ocorre e o drawer é fechado

#### Scenario: Fechamento após navegação programática
- **WHEN** uma navegação programática termina enquanto o shell está em viewport compacto
- **THEN** o drawer é fechado sem alterar o destino resolvido

### Requirement: Destinos estruturais carregados sob demanda
Cada área principal SHALL constituir um limite independente de carregamento sob demanda. Enquanto uma área não possuir capability funcional aprovada, ela SHALL apresentar somente um placeholder estrutural. Capabilities posteriores explicitamente aprovadas MAY substituir os placeholders de suas próprias áreas por comportamento funcional, preservando o shell e cada limite lazy sem tornar funcionais as demais áreas. As capabilities `frontend-broker-management` e `frontend-stock-management` SHALL fornecer respectivamente o comportamento funcional de Corretoras e Ações, sem transferir essas responsabilidades de domínio ao shell.

#### Scenario: Resolução por limite lazy
- **WHEN** o usuário navega para uma das cinco áreas principais
- **THEN** a rota é resolvida pelo limite lazy configurado para a área e seu conteúdo é exibido dentro do shell

#### Scenario: Placeholder identificável
- **WHEN** uma área ainda não possui capability funcional aprovada
- **THEN** ela apresenta um título principal que identifica a área sem simular funcionalidade de negócio

#### Scenario: Placeholder sem integração
- **WHEN** um destino permanece apenas como placeholder
- **THEN** nenhuma requisição HTTP é realizada e nenhum contrato ou service de domínio é necessário

#### Scenario: Evolução funcional aprovada
- **WHEN** as capabilities aprovadas de Corretoras e Ações introduzem comportamento funcional em suas áreas
- **THEN** somente esses placeholders são substituídos, mantendo os limites lazy, o shell e os placeholders de Dashboard, Carteiras e Operações

### Requirement: Tratamento de rota desconhecida
A aplicação SHALL apresentar um estado técnico de página não encontrada para URLs desconhecidas dentro do shell. A aplicação MUST NOT redirecionar silenciosamente uma URL desconhecida para o dashboard.

#### Scenario: URL desconhecida
- **WHEN** o usuário acessa uma URL que não corresponde a uma rota configurada
- **THEN** o estado NotFound é exibido na área principal com o shell preservado e a URL não é redirecionada para `/dashboard`

#### Scenario: Recuperação a partir de NotFound
- **WHEN** o estado NotFound está visível
- **THEN** o usuário dispõe de uma forma clara de retornar a um destino válido da aplicação

### Requirement: Interação acessível do shell
O shell SHALL permitir navegação por teclado, expor foco visível e contraste adequado, identificar semanticamente a navegação e a área principal e fornecer nomes acessíveis aos controles. A página atual SHALL ser comunicada por atributo apropriado, cada destino SHALL possuir um título principal e um skip link SHALL permitir acesso direto ao conteúdo.

#### Scenario: Navegação por teclado
- **WHEN** o usuário percorre e aciona os controles do shell usando somente o teclado
- **THEN** links, controle do drawer e conteúdo principal são alcançáveis e apresentam foco visível

#### Scenario: Semântica assistiva
- **WHEN** o shell é inspecionado por tecnologia assistiva
- **THEN** a navegação possui nome acessível, o controle do menu comunica sua finalidade e estado, a página atual é indicada e a área principal é identificável

#### Scenario: Acesso direto ao conteúdo
- **WHEN** o usuário de teclado aciona o skip link
- **THEN** o foco é transferido para a área principal do destino ativo

#### Scenario: Identificação dos destinos
- **WHEN** um destino estrutural ou o estado NotFound é exibido
- **THEN** seu conteúdo possui um título principal coerente e os controles interativos possuem alvos adequados
