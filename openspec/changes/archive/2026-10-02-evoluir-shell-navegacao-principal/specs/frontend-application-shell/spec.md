## MODIFIED Requirements

### Requirement: Shell principal da aplicação
A aplicação SHALL apresentar um shell único ocupando a viewport, contendo toolbar, navegação lateral e uma área principal que renderiza o destino ativo. A toolbar SHALL exibir o nome “Gestão de Ações” com identidade verde refinada, conteúdo legível e contraste adequado, e a área principal SHALL aplicar o container centralizado aprovado sem impedir layouts fluidos. No desktop, toolbar e sidebar SHALL permanecer estruturalmente estáveis enquanto a área de trabalho ocupa o espaço restante; no compacto, a toolbar SHALL permanecer estável e o drawer SHALL continuar sobreposto. A composição MUST evitar conteúdo oculto, scroll horizontal e dois scrolls concorrentes. O chrome SHALL ser visualmente mais discreto que o conteúdo financeiro; superfícies, espaçamento e ícones SHALL seguir os tokens compartilhados sem alterar breakpoint, modo side/over ou região de rolagem; os destinos e estados desktop SHALL seguir os requisitos de navegação desta capability. O seletor global de Carteira SHALL permanecer no shell, preservando identificação, estados, acessibilidade, persistência e precedência canônicas.

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
- **THEN** a toolbar mantém altura mínima de aproximadamente 64px e 56px respectivamente e pode crescer para acomodar título, botão de menu e seletor sem corte em mobile ou sob zoom

#### Scenario: Área de trabalho no desktop
- **WHEN** o shell desktop apresenta conteúdo maior que a viewport
- **THEN** toolbar e sidebar permanecem estruturalmente estáveis e a rolagem ocorre na região de trabalho definida sem mover todo o documento nem criar double scroll

#### Scenario: Área de trabalho no mobile
- **WHEN** o shell compacto apresenta conteúdo rolável
- **THEN** a toolbar permanece disponível, o conteúdo respeita sua altura e o drawer sobreposto não introduz scroll horizontal

#### Scenario: Regiões de uma página de coleção
- **WHEN** uma página separa cabeçalho, controles e uma coleção longa
- **THEN** cabeçalho, controles e coleção permanecem acessíveis pela rolagem da região principal do shell, sem altura fixa na página ou scroll interno concorrente que corte conteúdo sob zoom, e sem exigir virtual scrolling

#### Scenario: Reflow conforme espaço disponível
- **WHEN** a sidebar muda de largura ou a ampliação reduz a largura disponível para uma coleção
- **THEN** tabelas e cards se adaptam à largura do componente, preservando todos os campos e ações em uma representação semântica única, sem reorganizar o Dashboard ou alterar dados e cálculos

#### Scenario: Conteúdo financeiro em primeiro plano
- **WHEN** o Dashboard é apresentado
- **THEN** toolbar e sidebar mantêm identificação legível sem competir com o patrimônio e os indicadores

### Requirement: Navegação principal entre áreas
O shell SHALL oferecer, nesta ordem, Dashboard (`/dashboard`), Carteiras (`/carteiras`), Operações (`/operacoes`), Ativos (`/acoes`) e Corretoras (`/corretoras`). Cada item SHALL apresentar ícone local decorativo antes do label no desktop expandido e no drawer compacto, com coluna visual, alinhamento e espaçamento consistentes, sem fonte de ícones ou asset remoto. No desktop recolhido SHALL manter ícone e nome acessível completo com texto visual oculto. O destino ativo SHALL usar superfície selecionada clara definida pelo tema, marcador estrutural e `aria-current`, MUST NOT ser identificado somente por cor e SHALL manter hover e foco discretos e perceptíveis em ambos os estados desktop e no drawer mobile. Ativos SHALL ser somente nomenclatura de navegação para ações; Carteiras SHALL manter o gerenciamento das carteiras cadastradas.

#### Scenario: Destinos principais disponíveis
- **WHEN** a navegação principal é exibida
- **THEN** ela contém Dashboard, Carteiras, Operações, Ativos e Corretoras, exatamente nesta ordem, com os respectivos rótulos e URLs

#### Scenario: Rota inicial
- **WHEN** o usuário acessa a raiz `/`
- **THEN** a aplicação realiza redirect exato para `/dashboard`

#### Scenario: Indicação do destino ativo
- **WHEN** um dos cinco destinos ou uma de suas rotas filhas está ativo, inclusive com a sidebar recolhida
- **THEN** seu item apresenta superfície selecionada, indicador adicional à cor e comunicação semântica de página atual

#### Scenario: Interação com item inativo
- **WHEN** um item de navegação recebe hover ou foco
- **THEN** seu estado permanece perceptível sem competir visualmente com o item ativo

#### Scenario: Composição iconográfica dos destinos
- **WHEN** a navegação principal é apresentada no desktop expandido ou no drawer compacto
- **THEN** cada ícone aparece antes do respectivo label, usa a mesma coluna visual e fica oculto de tecnologia assistiva quando o texto já fornece o nome acessível

#### Scenario: Compatibilidade de Operações
- **WHEN** o usuário abre `/operacoes`, `/operacoes/nova` ou `/operacoes/{id}` diretamente
- **THEN** as rotas permanecem funcionais e lazy dentro do shell, e a listagem também pode ser acessada diretamente pelo item Operações na sidebar e no drawer

#### Scenario: Continuidade assistiva
- **WHEN** o refinamento é aplicado
- **THEN** skip link, aria-current, foco e acionamento das rotas existentes permanecem funcionais

### Requirement: Navegação responsiva
O shell SHALL adaptar a navegação ao viewport preservando a largura atual da sidebar expandida e permitindo largura reduzida no desktop recolhido. Em largura igual ou superior a 960px, a navegação SHALL permanecer lateral em modo side, iniciar expandida e permitir recolher e expandir sem remover os destinos. Em largura inferior a 960px, ela SHALL operar como drawer sobreposto, iniciar fechada e oferecer controle acessível para abertura e fechamento. O drawer mobile SHALL exibir ícones e textos e MUST NOT herdar a apresentação recolhida desktop.

#### Scenario: Navegação persistente em desktop
- **WHEN** o viewport possui largura igual ou superior a 960px
- **THEN** a navegação lateral permanece ao lado do conteúdo principal sobre superfície clara e neutra e permite alternar entre ícones com nomes visíveis e ícones com nomes acessíveis

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

#### Scenario: Entrada no mobile
- **WHEN** a viewport passa de desktop expandido ou recolhido para largura inferior a 960px
- **THEN** o drawer inicia fechado e, ao abrir, apresenta ícones e textos com backdrop, preservando header e seletor

#### Scenario: Retorno ao desktop
- **WHEN** a viewport retorna para largura igual ou superior a 960px, inclusive com drawer mobile aberto
- **THEN** restaura o último estado desktop da instância, sem backdrop ou armadilha de foco mobile e sem alterar rota ou Carteira selecionada

## ADDED Requirements

### Requirement: Recolhimento desktop acessível
O shell SHALL oferecer controle desktop por teclado com nome acessível correspondente a recolher ou expandir a navegação, associação com a região controlada e estado expandido comunicado semanticamente. A alternância SHALL manter foco visível no controle. Destinos recolhidos SHALL conservar nomes acessíveis completos, acionamento por teclado e indicação ativa por marcador além de cor e `aria-current`. O estado SHALL ser local à instância do layout, iniciar expandido e ser preservado entre navegações, sem persistência após reload. O shell SHALL preservar suporte a movimento reduzido e MUST NOT exigir animação para comunicar estado.

#### Scenario: Alternância pelo teclado
- **WHEN** o usuário focaliza o controle desktop e pressiona Enter ou Espaço
- **THEN** alterna o estado, mantém foco visível no controle e obtém nome e estado acessíveis atualizados para expandir ou recolher novamente

#### Scenario: Destinos sem texto visível
- **WHEN** a sidebar está recolhida e o usuário percorre os links com Tab e os aciona com Enter
- **THEN** os cinco destinos mantêm nomes acessíveis, foco visível e URLs correspondentes, inclusive indicação visual e semântica do destino ativo e de suas rotas filhas

#### Scenario: Estado local e reload
- **WHEN** o usuário recolhe, navega entre páginas e depois recarrega a aplicação
- **THEN** a navegação conserva o recolhimento e a nova instância inicia expandida sem preferência persistida

#### Scenario: Movimento reduzido
- **WHEN** a preferência de movimento reduzido está ativa e a sidebar alterna de estado
- **THEN** a transição respeita o tratamento atual de movimento reduzido e os controles e estados permanecem utilizáveis

### Requirement: Compatibilidade da evolução do shell
A evolução SHALL preservar header, marca, seletor global de Carteira, seus estados, precedência e persistência, skip link, região principal e renderização dos destinos no shell único. URLs, rotas filhas, limites lazy, parâmetros `carteiraId` e `origem`, captura de Carteira pelo formulário e retornos SHALL manter os contratos existentes. A mudança MUST NOT alterar conteúdo do Dashboard, funcionalidades das páginas, contratos de API ou regras financeiras; MUST NOT apresentar funcionalidades futuras como implementadas.

#### Scenario: Seletor e skip link preservados
- **WHEN** o usuário opera o seletor por teclado e aciona o skip link em desktop expandido, recolhido ou mobile
- **THEN** o seletor mantém o contrato de contexto existente e o skip link transfere foco à região principal sem ocultar header ou conteúdo

#### Scenario: URLs existentes preservadas
- **WHEN** o usuário abre diretamente, recarrega ou usa voltar e avançar em uma URL existente, incluindo rotas filhas e parâmetros contextuais
- **THEN** o mesmo destino e contrato de origem/retorno permanecem disponíveis no shell, com redirect exato de `/` para `/dashboard` e tratamento existente de URL desconhecida

#### Scenario: Formulário conserva Carteira capturada
- **WHEN** o cadastro abriu para Carteira A e o seletor global muda para B
- **THEN** o formulário conserva A até conclusão ou cancelamento, inclusive após reload com URL de captura, e mantém retorno definido por `origem` sem alteração de payload ou regra financeira

#### Scenario: Ativos e Carteiras mantêm o escopo
- **WHEN** o usuário aciona Ativos ou Carteiras
- **THEN** acessa respectivamente `/acoes` com somente ações suportadas ou `/carteiras` para gerenciar carteiras cadastradas, sem outras classes de investimentos ou nova visão conceitual Carteira

