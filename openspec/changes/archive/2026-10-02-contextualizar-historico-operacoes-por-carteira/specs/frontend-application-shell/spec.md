## MODIFIED Requirements

### Requirement: Contexto global leve de Carteira
O shell SHALL disponibilizar um único seletor de Carteira próximo à identificação da aplicação, com coleção validada, identificação da Carteira ativa e estados de inicialização, vazio e erro recuperável. O contexto compartilhado MUST NOT armazenar posições, operações, resumos, resultados, cotações, evolução ou snapshots. O seletor SHALL ter label acessível, foco visível e operação por teclado em desktop e mobile.

#### Scenario: Inicialização compartilhada
- **WHEN** shell, Dashboard e histórico de Operações precisam da coleção durante a mesma inicialização
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
- **THEN** o contexto acompanha a URL válida sem loops, e trocar no detalhe abre `/carteiras/{novoId}` enquanto trocar no Dashboard ou na listagem `/operacoes` atualiza `carteiraId`; cadastro e detalhe de Operação mantêm suas identidades capturadas

#### Scenario: Normalização da listagem sem parâmetro
- **WHEN** `/operacoes` é acessada sem carteiraId e o contexto resolve uma Carteira válida
- **THEN** a URL passa a `/operacoes?carteiraId={id}` substituindo a entrada atual, sem duplicar consulta, criar loop ou persistir fallback automático como preferência explícita

#### Scenario: Acesso direto e reload contextual
- **WHEN** `/operacoes?carteiraId=A` é aberta diretamente ou recarregada
- **THEN** A é validada na coleção compartilhada, prevalece sobre memória/preferência e determina header e histórico, sem consulta de operações antes da validação

#### Scenario: Troca de Carteira e histórico do navegador
- **WHEN** o usuário troca de A para B na listagem e depois usa voltar ou avançar
- **THEN** a troca explícita cria entrada com carteiraId=B e a navegação do navegador restaura A ou B conforme a URL, sincronizando seletor e dados sem reescrever a entrada restaurada ou produzir loops

#### Scenario: Seleção repetida e outros parâmetros
- **WHEN** o usuário seleciona a Carteira já ativa ou normaliza a URL da listagem
- **THEN** não há entrada nem consulta redundante para o mesmo contexto; parâmetros não relacionados permanecem preservados sem aceitar destinos de retorno arbitrários

#### Scenario: Recuperação de seleção inválida
- **WHEN** carteiraId é malformado ou inexistente e o usuário escolhe uma Carteira válida no seletor global
- **THEN** a URL inválida é substituída pela seleção explícita na navegação e a página carrega somente a Carteira escolhida; até essa escolha não há fallback nem consulta financeira
