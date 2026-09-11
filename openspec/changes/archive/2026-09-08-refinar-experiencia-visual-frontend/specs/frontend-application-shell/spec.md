## MODIFIED Requirements

### Requirement: Base visual consistente e reproduzível
A aplicação SHALL utilizar uma base visual Angular Material 3 clara e compatível com a foundation, sem depender de fontes, ícones ou assets remotos em runtime. As cores principais de branding e navegação SHALL usar a identidade verde refinada definida por `frontend-visual-experience`, mantendo cores de marca separadas das semânticas financeira, de warning e de erro.

#### Scenario: Tema global disponível
- **WHEN** qualquer destino estrutural é exibido
- **THEN** o tema claro global fornece aparência verde consistente e legível ao shell e ao conteúdo

#### Scenario: Recursos visuais locais
- **WHEN** a aplicação é carregada sem acesso a provedores externos de fontes, ícones ou assets
- **THEN** o shell mantém conteúdo, navegação e controles utilizáveis sem realizar requisição a esses provedores

#### Scenario: Semântica financeira independente
- **WHEN** o shell ou uma página usa a cor institucional da marca
- **THEN** essa cor não define por si só lucro, prejuízo, sucesso ou erro

### Requirement: Shell principal da aplicação
A aplicação SHALL apresentar um shell único ocupando a viewport, contendo toolbar, navegação lateral e uma área principal que renderiza o destino ativo. A toolbar SHALL exibir o nome “Gestão de Ações” com identidade verde refinada, conteúdo legível e contraste adequado, e a área principal SHALL aplicar o container centralizado aprovado sem impedir layouts fluidos. No desktop, toolbar e sidebar SHALL permanecer estruturalmente estáveis enquanto a área de trabalho ocupa o espaço restante; no compacto, a toolbar SHALL permanecer estável e o drawer SHALL continuar sobreposto. A composição MUST evitar conteúdo oculto, scroll horizontal e dois scrolls concorrentes. O chrome SHALL ser visualmente mais discreto que o conteúdo financeiro; superfícies, espaçamento e ícones SHALL seguir os tokens compartilhados sem alterar breakpoint, modo side/over, região de rolagem ou comportamento de navegação. O seletor global de Carteira SHALL permanecer no shell, preservando identificação, estados, acessibilidade, persistência e precedência canônicas.

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

#### Scenario: Conteúdo financeiro em primeiro plano
- **WHEN** o Dashboard é apresentado
- **THEN** toolbar e sidebar mantêm identificação legível sem competir com o patrimônio e os indicadores


### Requirement: Navegação principal entre áreas
O shell SHALL oferecer destinos para Dashboard, Corretoras, Ações e Carteiras com URLs `/dashboard`, `/corretoras`, `/acoes` e `/carteiras`. Cada item SHALL apresentar ícone local decorativo antes do label, com coluna visual, alinhamento e espaçamento consistentes no desktop e no drawer compacto, sem fonte de ícones ou asset remoto. O destino ativo SHALL usar superfície selecionada clara definida pelo tema, marcador estrutural e `aria-current`, MUST NOT ser identificado somente por cor e SHALL manter hover e foco discretos e perceptíveis.

#### Scenario: Destinos principais disponíveis
- **WHEN** a navegação principal é exibida
- **THEN** ela contém os destinos principais Dashboard, Corretoras, Ações e Carteiras com os respectivos rótulos e URLs

#### Scenario: Rota inicial
- **WHEN** o usuário acessa a raiz `/`
- **THEN** a aplicação realiza redirect exato para `/dashboard`

#### Scenario: Indicação do destino ativo
- **WHEN** um dos quatro destinos está ativo
- **THEN** seu item apresenta superfície selecionada, indicador adicional à cor e comunicação semântica de página atual

#### Scenario: Interação com item inativo
- **WHEN** um item de navegação recebe hover ou foco
- **THEN** seu estado permanece perceptível sem competir visualmente com o item ativo

#### Scenario: Composição iconográfica dos destinos
- **WHEN** a navegação principal é apresentada no desktop ou no drawer compacto
- **THEN** cada ícone aparece antes do respectivo label, usa a mesma coluna visual e fica oculto de tecnologia assistiva quando o texto já fornece o nome acessível

#### Scenario: Compatibilidade de Operações
- **WHEN** o usuário abre `/operacoes`, `/operacoes/nova` ou `/operacoes/{id}` diretamente
- **THEN** as rotas permanecem funcionais e lazy dentro do shell, embora Operações não apareça na sidebar nem no drawer

#### Scenario: Continuidade assistiva
- **WHEN** o refinamento é aplicado
- **THEN** skip link, aria-current, foco e acionamento das rotas existentes permanecem funcionais


