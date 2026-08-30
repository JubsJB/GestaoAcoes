# frontend-application-shell Specification

## Purpose

Oferecer um shell visual consistente, responsivo e acessível para que as áreas do frontend possam evoluir sobre navegação estável sem antecipar comportamento de negócio.

## Requirements

### Requirement: Base visual consistente e reproduzível
A aplicação SHALL utilizar uma base visual Angular Material 3 clara e compatível com a foundation, sem depender de fontes, ícones ou assets remotos em runtime. As cores principais de branding e navegação MUST usar identidade azul-petróleo/teal e MUST NOT usar verde ou vermelho como cor principal, reservando essas cores para semântica financeira futura.

#### Scenario: Tema global disponível
- **WHEN** qualquer destino estrutural é exibido
- **THEN** o tema claro global fornece aparência consistente e legível ao shell e ao conteúdo

#### Scenario: Recursos visuais locais
- **WHEN** a aplicação é carregada sem acesso a provedores externos de fontes, ícones ou assets
- **THEN** o shell mantém conteúdo, navegação e controles utilizáveis sem realizar requisição a esses provedores

### Requirement: Shell principal da aplicação
A aplicação SHALL apresentar um shell único contendo toolbar, navegação lateral e uma área principal que renderiza o destino ativo. A toolbar SHALL exibir o nome “Gestão de Ações”.

#### Scenario: Renderização do shell
- **WHEN** uma rota pertencente à aplicação é acessada
- **THEN** toolbar, navegação principal e área de conteúdo são apresentadas como uma estrutura visual coesa

#### Scenario: Identificação da aplicação
- **WHEN** o shell é renderizado
- **THEN** a toolbar exibe o texto “Gestão de Ações”

#### Scenario: Conteúdo do destino ativo
- **WHEN** a navegação resolve um destino estrutural
- **THEN** seu conteúdo é renderizado na área principal sem substituir o shell

### Requirement: Navegação principal entre áreas
O shell SHALL oferecer destinos para Dashboard, Corretoras, Ações, Carteiras e Operações com URLs `/dashboard`, `/corretoras`, `/acoes`, `/carteiras` e `/operacoes`. O destino ativo SHALL ser perceptível e MUST NOT ser identificado somente por diferença de cor.

#### Scenario: Destinos principais disponíveis
- **WHEN** a navegação principal é exibida
- **THEN** ela contém os destinos principais Dashboard, Corretoras, Ações, Carteiras e Operações com os respectivos rótulos e URLs

#### Scenario: Rota inicial
- **WHEN** o usuário acessa a raiz `/`
- **THEN** a aplicação realiza redirect exato para `/dashboard`

#### Scenario: Indicação do destino ativo
- **WHEN** um dos cinco destinos está ativo
- **THEN** seu item de navegação apresenta indicação visual adicional à cor e comunica que representa a página atual

### Requirement: Navegação responsiva
O shell SHALL adaptar a navegação ao viewport. Em largura igual ou superior a 960px, a navegação lateral SHALL permanecer aberta em modo persistente. Em largura inferior a 960px, ela SHALL operar como drawer sobreposto, iniciar fechada e oferecer controle acessível para abertura e fechamento.

#### Scenario: Navegação persistente em desktop
- **WHEN** o viewport possui largura igual ou superior a 960px
- **THEN** a navegação lateral permanece aberta ao lado do conteúdo principal

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
Cada área principal SHALL constituir um limite independente de carregamento sob demanda. Enquanto uma área não possuir capability funcional aprovada, ela SHALL apresentar somente um placeholder estrutural. Uma capability posterior explicitamente aprovada MAY substituir o placeholder de sua própria área por comportamento funcional, preservando o shell e o limite lazy sem tornar funcionais as demais áreas.

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
- **WHEN** uma capability aprovada introduz comportamento funcional para uma área principal
- **THEN** somente o placeholder dessa área é substituído, mantendo o limite lazy, o shell e os placeholders das áreas ainda não implementadas

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
