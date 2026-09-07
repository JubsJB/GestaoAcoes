## MODIFIED Requirements

### Requirement: Navegação principal entre áreas
O shell SHALL oferecer destinos para Dashboard, Corretoras, Ações e Carteiras com URLs `/dashboard`, `/corretoras`, `/acoes` e `/carteiras`. Cada item SHALL apresentar ícone local decorativo antes do label, com coluna visual, alinhamento e espaçamento consistentes no desktop e no drawer compacto, sem fonte de ícones ou asset remoto. O destino ativo SHALL usar superfície sage clara, marcador estrutural e `aria-current`, MUST NOT ser identificado somente por cor e SHALL manter hover e foco discretos e perceptíveis.

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

## ADDED Requirements

### Requirement: Contexto global leve de Carteira
O shell SHALL disponibilizar um único seletor de Carteira próximo à identificação da aplicação, com coleção validada, identificação da Carteira ativa e estados de inicialização, vazio e erro recuperável. O contexto compartilhado MUST NOT armazenar posições, operações, resumos, resultados, cotações, evolução ou snapshots. O seletor SHALL ter label acessível, foco visível e operação por teclado em desktop e mobile.

#### Scenario: Inicialização compartilhada
- **WHEN** shell e Dashboard precisam da coleção durante a mesma inicialização
- **THEN** compartilham a carga de Carteiras sem listagens duplicadas entre esses consumidores e nenhum dado financeiro é solicitado antes de contexto válido

#### Scenario: Vazio ou falha
- **WHEN** a carga retorna vazia ou falha
- **THEN** o contexto distingue vazio com caminho para cadastro de erro com tentativa manual, sem inventar Carteira ativa nem interpretar erro como vazio

### Requirement: Precedência e preferência local de Carteira
Uma URL explícita válida SHALL prevalecer sobre seleção em memória e preferência local. Sem URL explícita, a aplicação SHALL usar seleção em memória válida, depois preferência local validada e, na ausência destas, a primeira Carteira por id ASC. Somente a última seleção explícita SHALL ser persistida em chave isolada e versionada de localStorage; abrir detalhe válido SHALL selecionar e persistir sua Carteira. Fallback automático MUST NOT sobrescrever preferência explícita. Falha do armazenamento SHALL permitir continuidade em memória. A aplicação MUST NOT sincronizar seleção ao vivo entre abas.

#### Scenario: URL vence preferência
- **WHEN** a URL indica Carteira válida diferente da preferência local
- **THEN** a Carteira da URL determina o contexto e os dados da página

#### Scenario: Fallback determinístico
- **WHEN** não existe URL explícita, seleção em memória válida nem preferência válida e há várias Carteiras
- **THEN** a Carteira de menor id é ativada sem aguardar escolha nem gravar essa escolha automática como preferência

#### Scenario: URL inválida não usa fallback silencioso
- **WHEN** a URL contém identificador malformado ou inexistente
- **THEN** a página informa seleção inválida ou indisponível, não consulta finanças com esse valor nem apresenta outra Carteira como se fosse a solicitada, e permite recuperação explícita

#### Scenario: Preferência inválida ou armazenamento indisponível
- **WHEN** a preferência aponta para Carteira ausente, está corrompida ou localStorage falha
- **THEN** a aplicação ignora a preferência inválida, continua com a resolução válida em memória e mantém navegação utilizável

#### Scenario: Troca e navegação do histórico
- **WHEN** o usuário troca a seleção ou usa voltar e avançar do navegador
- **THEN** o contexto acompanha a URL válida sem loops, e trocar no detalhe abre `/carteiras/{novoId}` enquanto trocar no Dashboard atualiza `carteiraId`
