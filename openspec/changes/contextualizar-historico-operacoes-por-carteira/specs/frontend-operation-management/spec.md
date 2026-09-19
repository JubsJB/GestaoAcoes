## MODIFIED Requirements

### Requirement: Listagem cronológica e somente leitura
A página principal `/operacoes` SHALL apresentar exclusivamente o histórico da Carteira válida selecionada no contexto global e representada por carteiraId na URL, consultando `GET /carteiras/{id}/operacoes`. SHALL identificar a Carteira pelo nome sem segundo seletor e SHALL manter acesso pelo menu principal. SHALL exibir tipo, ativo, mercado, data, ordem, quantidade, preço, valor total e Corretora na ordem recebida do backend, garantida por dataOperacao, ordemNoDia e id ascendentes, sem recalcular ou reordenar o histórico. SHALL preservar tabela semântica desktop/cards completos mobile, números alinhados à direita, moeda explícita, tipo textual e ações estáveis. O histórico já existente no detalhe de Carteira SHALL conservar seus contratos de consulta e apresentação. A página principal MUST NOT usar `GET /operacoes` como fallback nem oferecer “Todas as carteiras”; o endpoint global e seu método de leitura existente SHALL continuar disponíveis para outros usos, sem alteração de backend ou contrato de DTO.
#### Scenario: Histórico retornado
- **WHEN** a consulta devolve compras e vendas
- **THEN** a página exibe preço, ordem e total retornados na ordem recebida

#### Scenario: Estados da coleção
- **WHEN** a consulta está pendente, retorna vazia ou falha
- **THEN** a página apresenta respectivamente loading, estado vazio ou erro com retry manual explícito

#### Scenario: Sem mutações inexistentes
- **WHEN** uma Operação é apresentada
- **THEN** a interface não oferece edição nem exclusão e não realiza PUT, PATCH ou DELETE de Operações

#### Scenario: Comparação cronológica
- **WHEN** o histórico é exibido em desktop ou mobile
- **THEN** todos os campos e a ordem recebida permanecem disponíveis e somente uma representação participa da acessibilidade e do teclado
#### Scenario: Entrada pelo menu principal
- **WHEN** o usuário aciona Operações no menu com A selecionada
- **THEN** acessa a listagem contextual de A com carteiraId=A na URL e consulta somente GET /carteiras/A/operacoes

#### Scenario: Troca do seletor no histórico contextual
- **WHEN** o usuário troca a Carteira no seletor estando na listagem
- **THEN** contexto e URL refletem a seleção e o histórico é consultado para essa Carteira, sem filtragem de uma lista global

#### Scenario: Consulta global preservada fora da página principal
- **WHEN** um consumidor existente usa a leitura global de operações
- **THEN** GET /operacoes e seu método de serviço permanecem disponíveis e compatíveis, embora não sejam usados pela listagem principal

### Requirement: Origem e retorno determinísticos de Operações
Entradas de Dashboard, detalhe de Carteira e histórico contextual de Operações SHALL transportar Carteira e origem em URL para o fluxo em página, permitindo reconstrução após reload sem depender de history.state. Cadastro em dialog SHALL preservar origem na instância e retornar ao detalhe de origem ao fechar. A origem `operacoes` com Carteira válida SHALL retornar a `/operacoes?carteiraId={origemId}` em cancelamento, conclusão e retorno do detalhe. URLs sem contexto recuperável SHALL manter fallback seguro para `/operacoes`, cuja listagem resolve o contexto vigente. O detalhe de Operação SHALL preservar a origem contextual recebida em links e usar o DTO compatível ou GET existente após reload. Retornos SHALL usar somente destinos internos conhecidos e MUST NOT aceitar redirecionamento arbitrário.

#### Scenario: Cadastro vindo do Dashboard
- **WHEN** o usuário abre nova Operação pelo Dashboard e recarrega a página
- **THEN** a Carteira explícita validada permanece fixa e cancelar ou concluir retorna a `/dashboard?carteiraId={origemId}`

#### Scenario: Cadastro ou detalhe vindo da Carteira
- **WHEN** o usuário abre fluxo em página a partir de uma Carteira e recarrega
- **THEN** a origem válida é reconstruída e o retorno leva a `/carteiras/{origemId}`

#### Scenario: Compatibilidade global
- **WHEN** o usuário abre `/operacoes`, `/operacoes/nova` ou `/operacoes/{id}` sem origem contextual
- **THEN** a listagem resolve a Carteira pelo contexto e normaliza sua URL, o cadastro captura a Carteira válida na abertura e cadastro/detalhe sem origem contextual conservam retorno seguro para `/operacoes`

#### Scenario: Resposta tardia da origem
- **WHEN** a operação de A conclui depois de a página ativa passar para B
- **THEN** seu DTO, erro ou atualização não contamina histórico, posições ou confirmação de B

#### Scenario: Reload de cadastro global
- **WHEN** o cadastro global captura uma Carteira válida sem origem contextual e a página é recarregada
- **THEN** o ID capturado fica representado na URL, a mesma Carteira validada é restaurada e o retorno continua `/operacoes`

#### Scenario: Origem de retorno inválida
- **WHEN** a URL contém origem desconhecida ou destino arbitrário
- **THEN** o retorno usa `/operacoes` sem redirecionamento externo; um ID contextual inválido continua exigindo recuperação explícita sem substituição silenciosa de Carteira
#### Scenario: Cadastro iniciado no histórico de A
- **WHEN** o usuário inicia cadastro em `/operacoes?carteiraId=A`
- **THEN** abre `/operacoes/nova?carteiraId=A&origem=operacoes`, captura A de forma fixa e cancelar ou concluir retorna a `/operacoes?carteiraId=A`, restaurando A no seletor e consultando novamente seu histórico

#### Scenario: Troca global durante cadastro contextual
- **WHEN** o formulário capturou A e o seletor muda para B, inclusive antes de concluir ou cancelar
- **THEN** a URL de captura e o POST permanecem vinculados a A; o retorno explícito ao histórico de origem restaura A, sem inserir a operação nos dados de B

#### Scenario: Reload de cadastro vindo de Operações
- **WHEN** o formulário com origem=operacoes e carteiraId=A é recarregado
- **THEN** captura e retorno são reconstruídos pela URL após validação de A, sem depender de history.state

#### Scenario: Detalhe vindo do histórico contextual
- **WHEN** uma operação de A é aberta pelo histórico de A
- **THEN** o link mantém ID da operação, carteiraId=A e origem=operacoes; o detalhe usa DTO compatível ou GET /operacoes/{id} após reload e retorna ao histórico de A

#### Scenario: Troca global durante detalhe
- **WHEN** o seletor muda para B enquanto é exibida uma operação de A
- **THEN** ID, DTO e URL de origem da operação permanecem inalterados; voltar pelo controle de retorno restaura o histórico de A

#### Scenario: Origem conflitante ou não recuperável
- **WHEN** o DTO do detalhe pertence a Carteira diferente da indicada na origem, a origem é desconhecida ou não há contexto recuperável
- **THEN** a operação nunca é reatribuída e o retorno usa `/operacoes`; ID explícito inválido no cadastro continua bloqueando captura/submissão, sem fallback silencioso ou redirecionamento externo

## ADDED Requirements

### Requirement: Estados explícitos do histórico por Carteira
A página SHALL distinguir contexto carregando, ausência de Carteiras, seleção inválida, erro de Carteiras, operações carregando, histórico vazio, conteúdo e erro de operações. Ações de cadastro SHALL exigir Carteira válida e identificar a Carteira de destino; nenhuma falha SHALL ser apresentada como vazio ou disparar fallback global. Estados e mudanças SHALL ser acessíveis por texto/status e controles com foco, preservando a responsividade existente.

#### Scenario: Contexto carregando
- **WHEN** a coleção de Carteiras ainda está sendo resolvida
- **THEN** apresenta carregamento de contexto sem dados antigos, sem consulta de operações e sem cadastro habilitado

#### Scenario: Nenhuma Carteira cadastrada
- **WHEN** a coleção carregou com sucesso e está vazia, sem ID explícito inválido
- **THEN** apresenta orientação e caminho para criar Carteira, sem consulta de operações ou cadastro de operação habilitado

#### Scenario: Carteira válida sem operações
- **WHEN** a consulta da Carteira válida retorna lista vazia
- **THEN** apresenta “Nenhuma operação nesta carteira” ou equivalente, nome da Carteira e ação de cadastro contextual

#### Scenario: Carteira válida com operações
- **WHEN** a consulta da Carteira válida retorna registros
- **THEN** apresenta somente esses registros, nome da Carteira e links contextuais de cadastro/detalhe, sem alterar valores ou ordem

#### Scenario: Carteira explicitamente inválida
- **WHEN** carteiraId é malformado ou não pertence à coleção validada, inclusive quando a coleção está vazia
- **THEN** apresenta seleção inválida/indisponível e recuperação explícita pelo seletor ou cadastro de Carteira, sem consultar histórico ou escolher outra automaticamente

#### Scenario: Erro ao carregar Carteiras
- **WHEN** a consulta da coleção falha
- **THEN** apresenta erro de contexto com tentativa manual, sem concluir que não há Carteiras e sem consultar operações

#### Scenario: Erro ao carregar operações
- **WHEN** a consulta de histórico falha para uma Carteira válida
- **THEN** mantém identificação dessa Carteira e apresenta erro específico com tentativa manual para o contexto ainda ativo, sem registros antigos nem fallback global; 404 informa indisponibilidade e exige recuperação explícita

### Requirement: Isolamento das consultas do histórico contextual
Cada seleção válida distinta SHALL carregar seu histórico. Ao mudar ou invalidar contexto, a página SHALL remover visualmente registros/erros anteriores e cancelar ou ignorar respostas obsoletas, inclusive erros e finalizações de carregamento. Normalização de URL e seleção repetida MUST NOT duplicar consultas; tentativa manual SHALL consultar apenas a Carteira ativa. O contexto global MUST NOT armazenar o histórico financeiro.

#### Scenario: Resposta tardia de A após seleção de B
- **WHEN** A está carregando e o usuário seleciona B antes da resposta de A
- **THEN** dados de A deixam de estar visíveis e somente resposta, erro e estado de carregamento de B podem atualizar a página; sucesso ou erro tardio de A é ignorado

#### Scenario: Contexto invalidado durante consulta
- **WHEN** a Carteira ativa deixa de ser válida ou o contexto entra em erro/carregamento
- **THEN** a consulta anterior não publica dados e a página apresenta o estado de contexto correspondente

#### Scenario: Sequência A para B e voltar
- **WHEN** o usuário troca A por B e usa voltar antes de B responder
- **THEN** a URL e o seletor voltam a A, seu histórico é carregado novamente e a resposta tardia de B não o substitui

#### Scenario: Recuperação e nova entrada
- **WHEN** o usuário tenta novamente após erro de operações ou retorna do cadastro concluído à listagem de origem
- **THEN** a página consulta novamente o endpoint contextual vigente sem inserir registros em outra Carteira nem alterar a ordenação recebida
