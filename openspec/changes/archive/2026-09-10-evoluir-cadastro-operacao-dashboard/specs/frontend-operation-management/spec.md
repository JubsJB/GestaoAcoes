## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Dialog contextual no Dashboard
Dashboard SHALL abrir o mesmo formulario em dialog com carteira capturada na abertura, sem selecao local. Troca global MUST NOT reatribuir a operacao. Sucesso SHALL fechar e atualizar dados financeiros da origem somente se ela ainda estiver visivel; MUST NOT criar snapshots. Erro SHALL manter dialog aberto e impedir submissao duplicada durante POST. Rota existente SHALL permanecer.

#### Scenario: Origem fixa
- **WHEN** carteira global muda com dialog aberto
- **THEN** POST continua usando carteira da abertura

#### Scenario: Sucesso e erro
- **WHEN** POST conclui
- **THEN** sucesso fecha e atualiza origem visivel; erro preserva formulario e informa falha sem POST duplicado

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Mensagens de validacao sem sobreposicao
O formulario SHALL reservar altura dinamica para erros e ajudas multilinha. Quantidade e preco SHALL permanecer lado a lado quando houver espaco; Corretora SHALL iniciar abaixo das mensagens, sem ocultar conteudo.

#### Scenario: Erros simultaneos
- **WHEN** quantidade e preco exibem erros multilinha
- **THEN** as mensagens participam do fluxo e nao sobrepoem Corretora
