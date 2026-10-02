# frontend-operation-management Specification

## Purpose
Disponibilizar no frontend o registro e a consulta acessível de compras e vendas, preservando integralmente o contrato discriminado e a autoridade financeira do backend.

## Requirements

### Requirement: Contratos frontend discriminados de Operações
O formulario SHALL exigir precoUnitario editavel e positivo em COMPRA e VENDA, preservando strings lossless e validacao vigente. COMPRA SHALL consultar previa-compra para acao/data validas e preencher preco historico como sugestao inicial editavel; SHALL enviar o valor final do campo. Respostas obsoletas e respostas posteriores a edicao manual MUST NOT sobrescrever o campo. VENDA SHALL preservar sugestao editavel existente. Estimativa visual existente MUST NOT substituir valorTotal autoritativo.

#### Scenario: Compra manual
- **WHEN** usuario preenche COMPRA
- **THEN** informa preco obrigatorio, enviado explicitamente no POST sem reconsulta historica pelo backend

#### Scenario: Preco ausente
- **WHEN** falta preco positivo em qualquer tipo
- **THEN** formulario nao submete e apresenta validacao

#### Scenario: Payload exato de COMPRA
- **WHEN** o formulario envia COMPRA
- **THEN** envia precoUnitario manual lossless junto aos campos existentes, sem ordemNoDia ou valorTotal

#### Scenario: Payload exato de VENDA
- **WHEN** o usuário submete uma VENDA válida
- **THEN** o frontend envia um único POST com `precoUnitario` e sem `ordemNoDia`, `id`, `acaoId` ou `valorTotal`

#### Scenario: Corretora opcional
- **WHEN** o usuário não seleciona Corretora
- **THEN** o request contém `corretoraId=null` ou omite a propriedade conforme a convenção vigente

#### Scenario: Response completo
- **WHEN** o backend devolve uma Operação
- **THEN** o frontend preserva `precoUnitario`, `ordemNoDia`, `valorTotal` e os demais campos retornados

### Requirement: Rotas globais e carregamento lazy
A área SHALL substituir somente o placeholder de Operações e manter seu limite lazy. Ela SHALL oferecer `/operacoes` para listagem, `/operacoes/nova` para cadastro e `/operacoes/{id}` para detalhe, resolvendo a rota estática `nova` antes do identificador.

#### Scenario: Acesso às rotas
- **WHEN** o usuário acessa ou recarrega uma das três rotas
- **THEN** a tela correspondente é carregada dentro do shell sem tornar funcional outro placeholder

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

#### Scenario: Troca do seletor na listagem de Operações
- **WHEN** o usuário troca a Carteira no seletor estando na listagem
- **THEN** contexto e URL refletem a seleção e o histórico é consultado para essa Carteira, sem filtragem de uma lista global

#### Scenario: Consulta global preservada fora da página principal
- **WHEN** um consumidor existente usa a leitura global de operações
- **THEN** GET /operacoes e seu método de serviço permanecem disponíveis e compatíveis, embora não sejam usados pela listagem principal

### Requirement: Cadastro com referências persistidas
O formulário SHALL usar Carteira existente fixa resolvida na abertura pelo contexto de entrada e permitir selecionar Ação pelo par ticker/mercado, Corretora opcional e tipo COMPRA ou VENDA. Ele MUST NOT cadastrar referências ausentes, consultar providers externos ou aceitar combinações livres de ticker e mercado.

#### Scenario: Referências disponíveis
- **WHEN** o cadastro global é aberto
- **THEN** a Carteira é resolvida pelo contexto global ou pela URL explícita validada, permanece visível e não editável, e Ações e Corretoras são carregadas pelos serviços existentes como opções identificáveis

#### Scenario: Referência obrigatória ausente
- **WHEN** não existe Carteira ou Ação selecionável
- **THEN** o formulário explica a dependência, oferece caminho à área correspondente e não envia POST

### Requirement: Formulário discriminado de COMPRA
O formulario SHALL exigir precoUnitario editavel e positivo em COMPRA e VENDA, preservando strings lossless e validacao vigente. COMPRA SHALL consultar previa-compra para acao/data validas e preencher preco historico como sugestao inicial editavel; SHALL enviar o valor final do campo. Respostas obsoletas e respostas posteriores a edicao manual MUST NOT sobrescrever o campo. VENDA SHALL preservar sugestao editavel existente. Estimativa visual existente MUST NOT substituir valorTotal autoritativo.

#### Scenario: Compra manual
- **WHEN** usuario preenche COMPRA
- **THEN** informa preco obrigatorio, enviado explicitamente no POST sem reconsulta historica pelo backend

#### Scenario: Preco ausente
- **WHEN** falta preco positivo em qualquer tipo
- **THEN** formulario nao submete e apresenta validacao

#### Scenario: Campos de COMPRA
- **WHEN** o usuario escolhe COMPRA
- **THEN** quantidade e precoUnitario sao editaveis e obrigatorios positivos; corretora permanece opcional

#### Scenario: Prévia carregada
- **WHEN** a previa da acao e data atuais retorna
- **THEN** ela preenche sugestao editavel lossless se nao houve edicao manual durante a consulta

#### Scenario: Prévia pendente ou inválida
- **WHEN** o formulario de COMPRA e preenchido
- **THEN** apresenta loading ou erro normalizado da previa e exige preco final positivo; permite entrada manual sem substituir data

#### Scenario: Data sem substituição de pregão
- **WHEN** o usuário escolhe uma data sem pregão
- **THEN** o frontend envia exatamente a data escolhida e não procura nem substitui por pregão anterior ou posterior

#### Scenario: Alternância de VENDA para COMPRA
- **WHEN** o usuario alterna de VENDA para COMPRA
- **THEN** preco permanece editavel e obrigatorio; consulta sugestao historica para acao/data atuais

#### Scenario: Contexto alterado durante a prévia
- **WHEN** o contexto de uma consulta anterior muda
- **THEN** uma resposta tardia nao substitui o preco manual nem a carteira capturada; a sugestao da nova acao/data substitui a anterior, sem reutilizar resposta obsoleta

### Requirement: Formulário discriminado de VENDA
Quando `tipo=VENDA`, o formulário SHALL exibir `precoUnitario` editável, obrigatório, positivo, com no máximo 13 dígitos inteiros e 6 fracionários. Havendo Carteira, Ação, mercado e data suficientes, SHALL consultar a sugestão da Carteira. Preço sugerido SHALL apenas preencher inicialmente o campo e poderá ser livremente aumentado ou reduzido pelo usuário; `null` SHALL manter o campo vazio sem erro técnico. Ordem no dia permanecerá ausente.

#### Scenario: Campos de VENDA
- **WHEN** o usuário seleciona VENDA
- **THEN** preço unitário é exibido como obrigatório e ordem no dia permanece ausente

#### Scenario: Preço inválido de VENDA
- **WHEN** o preço está ausente, não é positivo ou excede precisão ou escala
- **THEN** o formulário identifica o campo e não envia POST

#### Scenario: Alternância de COMPRA para VENDA
- **WHEN** o usuário muda de COMPRA para VENDA
- **THEN** a prévia é descartada, o campo torna-se editável e vazio enquanto a sugestão do contexto atual é consultada

#### Scenario: Sugestão encontrada e editável
- **WHEN** o backend retorna `precoUnitarioSugerido`
- **THEN** o valor preenche inicialmente o campo, mas o POST usa o valor válido que o usuário deixar ao confirmar

#### Scenario: Edição durante a consulta
- **WHEN** o usuário digita um preço de VENDA enquanto a sugestão está pendente
- **THEN** a resposta posterior não sobrescreve o valor manual

#### Scenario: Ausência normal de sugestão
- **WHEN** o backend retorna `precoUnitarioSugerido=null`
- **THEN** o campo permanece vazio, editável e obrigatório sem apresentar erro técnico

#### Scenario: Contexto alterado durante a sugestão
- **WHEN** Carteira, Ação, mercado ou data muda antes de a consulta anterior terminar
- **THEN** a sugestão anterior é removida imediatamente e sua resposta atrasada não altera o novo contexto

### Requirement: Quantidade e data preservadas
`quantidade` SHALL ser texto decimal positivo com no máximo 13 dígitos inteiros e 6 fracionários. BRASIL SHALL aceitar somente quantidade matematicamente inteira e EUA SHALL aceitar inteiro ou fração de até seis casas. `dataOperacao` SHALL ser enviada exatamente como `YYYY-MM-DD`, sem hora, conversão UTC ou ajuste automático de pregão.

#### Scenario: Quantidade por mercado
- **WHEN** a quantidade é validada
- **THEN** quantidade fracionária é aceita para EUA e rejeitada para BRASIL sem coerção, arredondamento ou truncamento

#### Scenario: Data civil
- **WHEN** a data é válida e não futura segundo a zona do mercado
- **THEN** o frontend envia exatamente o texto `YYYY-MM-DD`

### Requirement: Precisão lossless e valores autoritativos
Os campos decimais `quantidade`, `precoUnitario`, `precoUnitarioSugerido` e `valorTotal` dos responses SHALL ser preservados sem perda de precisão. O frontend MUST NOT inventar preço de COMPRA, substituir ou enviar `valorTotal`, calcular preço médio, posição ou resultado financeiro e MUST NOT introduzir nova biblioteca decimal. O formulário MAY multiplicar textualmente quantidade por preço somente para exibir um total explicitamente estimado, separado do `valorTotal` autoritativo do response e sem coerção binária.

#### Scenario: Response com decimal longo
- **WHEN** o backend retorna números além da precisão segura de JavaScript
- **THEN** lista, detalhe e histórico preservam e exibem os valores sem arredondamento ou truncamento

#### Scenario: Valores somente autoritativos
- **WHEN** uma criação retorna com sucesso
- **THEN** a interface usa exclusivamente preço, ordem e total presentes no response

#### Scenario: Total estimado no formulário
- **WHEN** quantidade e preço válidos estão disponíveis durante o cadastro
- **THEN** o formulário pode exibir quantidade × preço como estimativa visual em BRL ou USD, sem enviar `valorTotal` ou realizar outro cálculo financeiro

### Requirement: Erros históricos e externos acionáveis
A feature SHALL preservar message e details dos erros normalizados dos GETs e POST. Falhas SHALL manter o formulario aberto e permitir nova tentativa sem submissao duplicada. Erros tecnicos SHALL usar o tratamento central existente. O cadastro manual MUST NOT depender de disponibilidade do fechamento historico.

#### Scenario: Falha no registro manual
- **WHEN** o POST de COMPRA ou VENDA falha
- **THEN** o formulario permanece aberto com os valores informados e mensagem normalizada

#### Scenario: Consulta historica independente
- **WHEN** uma consulta historica independente encontra indisponibilidade ou rate limit
- **THEN** a falha nao substitui o preco manual nem troca automaticamente a data da Operacao

#### Scenario: Fechamento indisponível
- **WHEN** uma consulta historica independente recebe `422 COTACAO_HISTORICA_INDISPONIVEL`
- **THEN** o formulário permanece aberto e informa que não houve fechamento disponível para a data escolhida

#### Scenario: Histórico fora do alcance
- **WHEN** uma consulta historica independente recebe `422 HISTORICO_COTACAO_FORA_DO_ALCANCE`
- **THEN** o formulário permanece aberto e informa que a data está fora do histórico disponível pelo provedor

#### Scenario: Limite do provider
- **WHEN** uma consulta historica independente recebe `429 LIMITE_REQUISICOES_EXCEDIDO`
- **THEN** a interface preserva o erro e nao dispara retry automatico; o preco manual do cadastro nao depende dessa consulta

#### Scenario: Falha técnica externa
- **WHEN** o backend responde `502`, `503` ou `504`
- **THEN** a interface usa o tratamento técnico central e preserva os dados do formulário

#### Scenario: Erro real da prévia
- **WHEN** o GET da prévia produz `StandardError` em um `HttpErrorResponse`
- **THEN** os erros normalizados preservam codigo, mensagem e detalhes sem bloquear a COMPRA manual por falta de previa

### Requirement: Submissão explícita sem deduplicação
O envio SHALL bloquear nova submissão enquanto o POST atual estiver pendente. O request SHALL utilizar o preço final válido presente no campo precoUnitario, que MAY ter sido inicialmente sugerido pela prévia. Uma prévia válida MUST NOT ser condição para realizar COMPRA; falha da prévia MUST NOT impedir o cadastro quando houver preço manual válido e as demais validações forem satisfeitas. A feature MUST NOT realizar retry automático, criar idempotency key nem rejeitar operações legitimamente idênticas por comparação de payload.

#### Scenario: Clique duplicado pendente
- **WHEN** o usuário aciona o submit novamente enquanto o POST está pendente
- **THEN** somente uma requisição é enviada

#### Scenario: Novo envio deliberado
- **WHEN** o POST anterior já terminou
- **THEN** uma nova ação explícita pode enviar outra Operação, mesmo com payload igual

### Requirement: Venda e cronologia permanecem autoritativas no backend
O frontend MUST NOT calcular saldo, preço médio, lucro, elegibilidade de VENDA ou replay cronológico. Ele SHALL preservar o formulário quando o backend rejeitar uma VENDA ou inserção retroativa.

#### Scenario: Posição insuficiente
- **WHEN** o backend responde `409 POSICAO_INSUFICIENTE`
- **THEN** a interface explica a posição insuficiente, preserva a entrada e não apresenta sucesso

### Requirement: Detalhe fiel
O detalhe SHALL apresentar os dados relevantes do `OperacaoResponse`, incluindo preço e total autoritativos formatados, mostrar “Sem corretora” quando aplicável, permitir retorno ao contexto de origem e MUST NOT exibir `ordemNoDia`, cotação, posição, preço médio, resultados, edição ou exclusão. `ordemNoDia` SHALL permanecer no contrato e continuar disponível para ordenação interna. O detalhe SHALL organizar contexto, movimentação, quantidade, preço, total, data e Corretora em grupos legíveis, mantendo identificadores quando forem os únicos dados disponíveis e sem consultas para convertê-los em nomes.

#### Scenario: Estado transitório ou reload
- **WHEN** há response transitório compatível ou a rota é recarregada
- **THEN** o detalhe usa o DTO compatível sem GET redundante ou consulta `GET /operacoes/{id}` no reload

#### Scenario: Operação inexistente
- **WHEN** a consulta responde `404`
- **THEN** a página apresenta estado de não encontrado e caminho à listagem

#### Scenario: Detalhe sem enriquecimento
- **WHEN** um identificador não possui nome no DTO disponível
- **THEN** o identificador continua compreensível no contexto sem nova chamada HTTP

### Requirement: Cadastro contextual reutiliza as mesmas regras
O cadastro iniciado no detalhe de Carteira SHALL reutilizar o mesmo formulário, pipeline consultivo e construtor de payload do fluxo global, com Carteira pré-selecionada, visível e não editável. A sugestão de VENDA SHALL usar essa Carteira; COMPRA oferece previa historica editavel e exige preco final positivo. Após `201`, SHALL fechar o dialog, apresentar sucesso e incorporar o response no histórico por `dataOperacao`, `ordemNoDia` e `id`, sem GET obrigatório ou cálculo financeiro.

#### Scenario: COMPRA contextual
- **WHEN** o usuário registra COMPRA a partir de uma Carteira
- **THEN** o request usa o `carteiraId` contextual e inclui preco manual e omite ordem

#### Scenario: VENDA contextual
- **WHEN** o usuário registra VENDA a partir de uma Carteira
- **THEN** o request usa o `carteiraId` contextual, inclui preço e omite ordem

#### Scenario: Atualização do histórico
- **WHEN** o cadastro contextual retorna com sucesso
- **THEN** o histórico passa a exibir o DTO autoritativo retornado

### Requirement: Experiência acessível e responsiva
A feature SHALL reutilizar feedback, toast e padrões visuais existentes, preservar foco e navegação por teclado e manter lista, formulário, detalhe e dialog legíveis em viewport compacto sem depender somente de cor. O formulário SHALL agrupar visualmente contexto, tipo/movimentação, quantidade/preço/data, Corretora, estimativa existente e ações. A reorganização MUST preservar compra/venda, preco manual editavel e obrigatorio em COMPRA, sugestão editável em VENDA, estimativa, Carteira capturada na abertura e fixa/não editável até conclusão ou cancelamento em página/dialog, inclusive na entrada global de compatibilidade, Corretora opcional, strings decimais, data civil, máscaras, validações, payloads com preco nos dois tipos e previa historica apenas como sugestao editavel no formulario COMPRA.

#### Scenario: Uso assistivo ou compacto
- **WHEN** a feature é usada por teclado, tecnologia assistiva ou tela compacta
- **THEN** campos condicionais, mensagens, ações, foco e valores permanecem compreensíveis e operáveis

#### Scenario: Formulário agrupado
- **WHEN** uma operação é preparada em página ou dialog
- **THEN** grupos e ajudas facilitam leitura sem adicionar campos ou alterar condições de edição, bloqueio, submissão ou cancelamento, preservando origem, retorno determinístico após reload/deep link e isolamento perante troca global de Carteira

### Requirement: Carteira fixa da abertura ao POST
Cada abertura de cadastro SHALL capturar uma Carteira válida e mantê-la visível e não editável até cancelamento ou conclusão. Sugestão de VENDA e `carteiraId` do POST SHALL usar essa identidade capturada, nunca reler a seleção global no submit. A prévia de COMPRA SHALL preservar seu contrato independente da Carteira. Mudanças externas de contexto MUST NOT redirecionar a operação em edição a outra Carteira. Sem Carteira válida SHALL bloquear submissão e oferecer recuperação explícita.

#### Scenario: Troca global durante preenchimento
- **WHEN** o formulário abriu para A e a seleção global muda para B
- **THEN** formulário, sugestão de VENDA e POST continuam vinculados a A, identificada visivelmente, e não são reatribuídos a B

#### Scenario: Origem removida ou inválida
- **WHEN** a Carteira capturada deixa de estar disponível ou o backend rejeita sua referência
- **THEN** o formulário informa a indisponibilidade e não substitui a Carteira por um fallback nem apresenta sucesso

#### Scenario: Contratos de compra e venda preservados
- **WHEN** um formulário contextual ou global é submetido
- **THEN** reutiliza o construtor vigente: COMPRA e VENDA com precoUnitario e sem ordemNoDia, mantendo a Carteira capturada, Corretora opcional, validações e bloqueio de submissão concorrente

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

### Requirement: Mensagens de validacao sem sobreposicao
O formulario SHALL reservar altura dinamica para erros e ajudas multilinha. Quantidade e preco SHALL permanecer lado a lado quando houver espaco; Corretora SHALL iniciar abaixo das mensagens, sem ocultar conteudo.

#### Scenario: Erros simultaneos
- **WHEN** quantidade e preco exibem erros multilinha
- **THEN** as mensagens participam do fluxo e nao sobrepoem Corretora

### Requirement: Dialog contextual no Dashboard
Dashboard SHALL abrir o mesmo formulario em dialog com carteira capturada na abertura, sem selecao local. Troca global MUST NOT reatribuir a operacao. Sucesso SHALL fechar e atualizar dados financeiros da origem somente se ela ainda estiver visivel; MUST NOT criar snapshots. Erro SHALL manter dialog aberto e impedir submissao duplicada durante POST. Rota existente SHALL permanecer.

#### Scenario: Origem fixa
- **WHEN** carteira global muda com dialog aberto
- **THEN** POST continua usando carteira da abertura

#### Scenario: Sucesso e erro
- **WHEN** POST conclui
- **THEN** sucesso fecha e atualiza origem visivel; erro preserva formulario e informa falha sem POST duplicado

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
