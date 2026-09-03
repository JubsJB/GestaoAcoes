# frontend-operation-management Specification

## Purpose
Disponibilizar no frontend o registro e a consulta acessível de compras e vendas, preservando integralmente o contrato discriminado e a autoridade financeira do backend.

## Requirements

### Requirement: Contratos frontend discriminados de Operações
A área de Operações SHALL consumir `POST /operacoes`, `GET /operacoes`, `GET /operacoes/{id}`, `GET /carteiras/{carteiraId}/operacoes`, `GET /operacoes/previa-compra` e `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda` pela configuração central da API. A criação SHALL ser uma união discriminada: COMPRA contém exclusivamente `carteiraId`, `ticker`, `mercado`, `corretoraId`, `tipo`, `quantidade` e `dataOperacao`; VENDA contém esses campos e `precoUnitario` obrigatório. Nenhum create request SHALL conter `ordemNoDia`. Responses financeiros SHALL preservar representação decimal lossless.

#### Scenario: Payload exato de COMPRA
- **WHEN** o usuário submete uma COMPRA válida
- **THEN** o frontend envia um único POST sem `precoUnitario`, inclusive nulo, sem `ordemNoDia`, `id`, `acaoId`, `valorTotal` ou qualquer cotação

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
A listagem global SHALL consultar `GET /operacoes` uma vez ao entrar e apresentar a ordem recebida do backend, garantida por `dataOperacao`, `ordemNoDia` e `id` ascendentes. Ela SHALL exibir tipo, ativo, mercado, data, ordem, quantidade, preço, valor total e Corretora, diferenciar loading, vazio, conteúdo e erro recuperável e MUST NOT recalcular ou reordenar o histórico.

#### Scenario: Histórico retornado
- **WHEN** a consulta devolve compras e vendas
- **THEN** a página exibe preço, ordem e total retornados na ordem recebida

#### Scenario: Estados da coleção
- **WHEN** a consulta está pendente, retorna vazia ou falha
- **THEN** a página apresenta respectivamente loading, estado vazio ou erro com retry manual explícito

#### Scenario: Sem mutações inexistentes
- **WHEN** uma Operação é apresentada
- **THEN** a interface não oferece edição nem exclusão e não realiza PUT, PATCH ou DELETE de Operações

### Requirement: Cadastro com referências persistidas
O formulário SHALL permitir selecionar Carteira existente, Ação pelo par ticker/mercado, Corretora opcional e tipo COMPRA ou VENDA. Ele MUST NOT cadastrar referências ausentes, consultar providers externos ou aceitar combinações livres de ticker e mercado.

#### Scenario: Referências disponíveis
- **WHEN** o cadastro global é aberto
- **THEN** Carteiras, Ações e Corretoras são carregadas pelos serviços existentes e apresentadas como opções identificáveis

#### Scenario: Referência obrigatória ausente
- **WHEN** não existe Carteira ou Ação selecionável
- **THEN** o formulário explica a dependência, oferece caminho à área correspondente e não envia POST

### Requirement: Formulário discriminado de COMPRA
Quando `tipo=COMPRA`, o formulário SHALL manter visível um campo de preço unitário somente leitura e sem validators de preço. Havendo Carteira, Ação, mercado e data suficientes, o frontend SHALL consultar `GET /operacoes/previa-compra` com ticker, mercado e `dataOperacao`, exibir o preço e a moeda retornados e informar que se trata do fechamento exato usado informativamente. O campo MUST NOT permitir edição, usar `cotacaoAtual`, substituir a data ou integrar o POST.

#### Scenario: Campos de COMPRA
- **WHEN** o usuário seleciona COMPRA
- **THEN** o preço fica visível e somente leitura, ordem no dia permanece ausente e o texto explicativo do fechamento histórico é apresentado

#### Scenario: Prévia carregada
- **WHEN** Ação, mercado e data válidos produzem uma prévia
- **THEN** o campo exibe `precoUnitario` com `BRL` ou `USD` retornado pelo backend e o POST permanece sem preço

#### Scenario: Prévia pendente ou inválida
- **WHEN** a prévia está carregando, falha ou ainda não existe para o contexto atual
- **THEN** o campo não apresenta preço válido e Registrar operação permanece bloqueado

#### Scenario: Data sem substituição de pregão
- **WHEN** o usuário escolhe uma data sem pregão
- **THEN** o frontend envia exatamente a data escolhida e não procura nem substitui por pregão anterior ou posterior

#### Scenario: Alternância de VENDA para COMPRA
- **WHEN** existe preço digitado em VENDA e o usuário muda para COMPRA
- **THEN** o valor é removido imediatamente, o campo torna-se somente leitura, uma nova prévia é consultada e nenhum preço aparece no payload de COMPRA

#### Scenario: Contexto alterado durante a prévia
- **WHEN** Ação, mercado ou data muda antes de a consulta anterior terminar
- **THEN** o preço anterior é invalidado imediatamente e uma resposta atrasada não pode sobrescrever o contexto novo

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
A feature SHALL preservar o erro normalizado dos GETs e do POST e acrescentar orientação específica sem ocultar `message` e `details`. `COTACAO_HISTORICA_INDISPONIVEL`, `HISTORICO_COTACAO_FORA_DO_ALCANCE`, `TICKER_INEXISTENTE` e `LIMITE_REQUISICOES_EXCEDIDO` SHALL possuir feedback apropriado, sem preço manual ou troca automática de data. Erros `502`, `503` e `504` SHALL usar o tratamento técnico central existente. O caminho real de `HttpErrorResponse` até a mensagem SHALL ser integrado à normalização central.

#### Scenario: Fechamento indisponível
- **WHEN** uma COMPRA recebe `422 COTACAO_HISTORICA_INDISPONIVEL`
- **THEN** o formulário permanece aberto e informa que não houve fechamento disponível para a data escolhida

#### Scenario: Histórico fora do alcance
- **WHEN** uma COMPRA recebe `422 HISTORICO_COTACAO_FORA_DO_ALCANCE`
- **THEN** o formulário permanece aberto e informa que a data está fora do histórico disponível pelo provedor

#### Scenario: Limite do provider
- **WHEN** uma COMPRA recebe `429 LIMITE_REQUISICOES_EXCEDIDO`
- **THEN** a interface apresenta orientação técnica amigável sem oferecer preço manual ou retry automático do POST

#### Scenario: Falha técnica externa
- **WHEN** o backend responde `502`, `503` ou `504`
- **THEN** a interface usa o tratamento técnico central e preserva os dados do formulário

#### Scenario: Erro real da prévia
- **WHEN** o GET da prévia produz `StandardError` em um `HttpErrorResponse`
- **THEN** interceptor, service e formulário preservam código, mensagem e detalhes e mantêm a COMPRA bloqueada até uma prévia válida

### Requirement: Submissão explícita sem deduplicação
O envio SHALL bloquear nova submissão enquanto o POST atual estiver pendente e, para COMPRA, enquanto não existir prévia válida correspondente ao contexto atual. O preço da prévia MUST NOT integrar o request. A feature MUST NOT realizar retry automático, criar idempotency key nem rejeitar operações legitimamente idênticas por comparação de payload.

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
O detalhe SHALL apresentar os dados relevantes do `OperacaoResponse`, incluindo preço e total autoritativos formatados, mostrar “Sem corretora” quando aplicável, permitir retorno ao contexto de origem e MUST NOT exibir `ordemNoDia`, cotação, posição, preço médio, resultados, edição ou exclusão. `ordemNoDia` SHALL permanecer no contrato e continuar disponível para ordenação interna.

#### Scenario: Estado transitório ou reload
- **WHEN** há response transitório compatível ou a rota é recarregada
- **THEN** o detalhe usa o DTO compatível sem GET redundante ou consulta `GET /operacoes/{id}` no reload

#### Scenario: Operação inexistente
- **WHEN** a consulta responde `404`
- **THEN** a página apresenta estado de não encontrado e caminho à listagem

### Requirement: Cadastro contextual reutiliza as mesmas regras
O cadastro iniciado no detalhe de Carteira SHALL reutilizar o mesmo formulário, pipeline consultivo e construtor de payload do fluxo global, com Carteira pré-selecionada, visível e não editável. A sugestão de VENDA SHALL usar essa Carteira; a prévia de COMPRA continuará independente dela. Após `201`, SHALL fechar o dialog, apresentar sucesso e incorporar o response no histórico por `dataOperacao`, `ordemNoDia` e `id`, sem GET obrigatório ou cálculo financeiro.

#### Scenario: COMPRA contextual
- **WHEN** o usuário registra COMPRA a partir de uma Carteira
- **THEN** o request usa o `carteiraId` contextual e omite preço e ordem

#### Scenario: VENDA contextual
- **WHEN** o usuário registra VENDA a partir de uma Carteira
- **THEN** o request usa o `carteiraId` contextual, inclui preço e omite ordem

#### Scenario: Atualização do histórico
- **WHEN** o cadastro contextual retorna com sucesso
- **THEN** o histórico passa a exibir o DTO autoritativo retornado

### Requirement: Experiência acessível e responsiva
A feature SHALL reutilizar feedback, toast e padrões visuais existentes, preservar foco e navegação por teclado e manter lista, formulário, detalhe e dialog legíveis em viewport compacto sem depender somente de cor.

#### Scenario: Uso assistivo ou compacto
- **WHEN** a feature é usada por teclado, tecnologia assistiva ou tela compacta
- **THEN** campos condicionais, mensagens, ações, foco e valores permanecem compreensíveis e operáveis
