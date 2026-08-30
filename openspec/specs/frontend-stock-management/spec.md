# frontend-stock-management Specification

## Purpose
Disponibilizar o gerenciamento frontend de Ações brasileiras e americanas sobre os contratos vigentes do backend, com consultas, cadastro e atualização manual de cotação acessíveis, responsivos e sem duplicar integrações ou regras financeiras.

## Requirements

### Requirement: Integração exclusiva com os contratos backend de Ações
A área de Ações SHALL consumir exclusivamente `POST /acoes`, `GET /acoes`, `GET /acoes/{id}`, `GET /acoes/por-ticker?ticker=...&mercado=...` e `PATCH /acoes/{id}/cotacao` pela configuração central da API. O frontend MUST NOT consultar BRAPI, Alpha Vantage ou outro provider diretamente, nem enviar ou calcular dados controlados pelo backend.

#### Scenario: Contratos HTTP da feature
- **WHEN** o usuário lista, busca, cadastra, detalha ou atualiza a cotação de uma Ação
- **THEN** a aplicação usa somente o método, caminho, parâmetros e corpo definidos pelo endpoint backend correspondente

#### Scenario: Ausência de integração externa no navegador
- **WHEN** qualquer fluxo da área de Ações é executado
- **THEN** o navegador não chama BRAPI, Alpha Vantage ou outro provider de cotação diretamente

#### Scenario: Contrato completo de resposta
- **WHEN** o backend devolve uma Ação
- **THEN** a aplicação preserva `id`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `cotacaoAtual` e `dataHoraCotacao` como dados obrigatórios do contrato

#### Scenario: Dados autoritativos do backend
- **WHEN** ticker, moeda, cotação ou referência temporal são apresentados
- **THEN** a aplicação usa os valores devolvidos pelo backend sem inferir ticker canônico, converter moeda ou recalcular cotação

### Requirement: Rotas funcionais sob o limite lazy de Ações
A área SHALL fornecer destinos para listagem, cadastro e detalhe sob `/acoes`, preservando o limite lazy existente e o shell da aplicação. Somente o placeholder de Ações SHALL ser substituído por comportamento funcional.

#### Scenario: Destinos da feature
- **WHEN** o usuário acessa `/acoes`, `/acoes/nova` ou `/acoes/{id}`
- **THEN** a aplicação apresenta respectivamente listagem, cadastro ou detalhe dentro do shell

#### Scenario: Precedência da rota de cadastro
- **WHEN** o usuário acessa `/acoes/nova`
- **THEN** a rota resolve o cadastro e não interpreta `nova` como identificador

#### Scenario: Demais áreas preservadas
- **WHEN** a capability de Ações é disponibilizada
- **THEN** Corretoras permanece funcional e Dashboard, Carteiras e Operações permanecem estruturais

### Requirement: Listagem responsiva das Ações persistidas
A aplicação SHALL carregar `GET /acoes` ao entrar na listagem e SHALL representar distintamente carregamento, conteúdo, coleção vazia e falha. A resposta SHALL ser apresentada na ordem recebida, sem paginação ou ordenação semântica adicional.

#### Scenario: Listagem com registros
- **WHEN** o backend responde com Ações persistidas
- **THEN** a aplicação apresenta cada registro em estrutura fluida com acesso ao detalhe

#### Scenario: Listagem vazia
- **WHEN** o backend responde com array vazio
- **THEN** a aplicação apresenta estado vazio e uma ação clara para cadastrar uma Ação

#### Scenario: Falha e retry da listagem
- **WHEN** a listagem falha
- **THEN** a aplicação apresenta a falha preservada e oferece retry explícito que executa uma nova tentativa de `GET /acoes`

#### Scenario: Acesso ao cadastro
- **WHEN** o usuário aciona o cadastro a partir da listagem
- **THEN** a aplicação navega para `/acoes/nova`

### Requirement: Busca exata e explícita por ticker e mercado
A listagem SHALL permitir consulta singular somente por ação explícita do usuário, exigindo ticker e mercado. A busca MUST NOT realizar requests durante digitação, SHALL preservar a coleção completa já carregada e SHALL usar conjuntamente os dois parâmetros sem escolher mercado implicitamente.

#### Scenario: Busca explícita encontrada
- **WHEN** o usuário informa ticker e mercado válidos e confirma a busca
- **THEN** a aplicação solicita `GET /acoes/por-ticker` com ambos os query parameters e navega para `/acoes/{id}` usando a resposta como estado transitório

#### Scenario: Digitação sem consulta
- **WHEN** o usuário digita ou altera ticker ou mercado sem submeter a busca
- **THEN** nenhuma requisição de busca é realizada

#### Scenario: Busca incompleta
- **WHEN** ticker ou mercado não foi informado
- **THEN** a aplicação comunica a validação de UX e não realiza a requisição

#### Scenario: Combinação local inexistente
- **WHEN** a busca responde `404` sem código específico
- **THEN** a aplicação informa contextualmente que a Ação não foi encontrada sem classificar o caso como ticker inexistente no provider

#### Scenario: Limpeza da busca
- **WHEN** o usuário limpa ou cancela a busca
- **THEN** a coleção completa preservada volta a ser apresentada imediatamente sem novo `GET /acoes`

### Requirement: Cadastro somente por ticker e mercado
A aplicação SHALL cadastrar uma Ação enviando exclusivamente ticker e mercado. O frontend SHALL limitar sua validação à experiência de entrada compatível com o contrato e MUST NOT aceitar nome, moeda, cotação, data, provider ou outra propriedade definida pelo usuário.

#### Scenario: Formulário inicial
- **WHEN** o cadastro é apresentado
- **THEN** somente ticker e seleção obrigatória entre `BRASIL` e `EUA` são solicitados

#### Scenario: Payload do cadastro
- **WHEN** o usuário submete um formulário válido
- **THEN** a aplicação envia `POST /acoes` com exatamente `ticker` e `mercado`, sem propriedades adicionais

#### Scenario: Normalização leve de entrada
- **WHEN** o ticker contém espaços nas extremidades ou letras minúsculas
- **THEN** a aplicação pode aplicar trim e uppercase sem alterar caracteres internos, sufixos ou decidir um ticker canônico

#### Scenario: Submissão concorrente
- **WHEN** um cadastro está em andamento
- **THEN** nova submissão pelo mesmo formulário permanece bloqueada até o fluxo terminar

#### Scenario: Cadastro concluído
- **WHEN** o backend responde `201 Created` com `AcaoResponse`
- **THEN** a aplicação comunica sucesso, navega para `/acoes/{id}` e transporta a resposta como estado transitório

#### Scenario: Cadastro duplicado
- **WHEN** o cadastro responde `409` com `code=ACAO_DUPLICADA`
- **THEN** a aplicação apresenta o conflito e seus detalhes no contexto do formulário sem diálogo, retry automático ou segundo POST

### Requirement: Detalhe fiel da Ação
O detalhe SHALL apresentar o contrato completo da Ação e SHALL usar estado transitório somente quando seu identificador corresponder à rota. Sem estado compatível, SHALL consultar `GET /acoes/{id}`.

#### Scenario: Transição com DTO compatível
- **WHEN** cadastro ou busca navega ao detalhe com DTO cujo ID corresponde à rota
- **THEN** a aplicação apresenta esse DTO sem executar imediatamente GET redundante por ID

#### Scenario: Acesso direto ou refresh
- **WHEN** o detalhe é acessado sem estado transitório compatível
- **THEN** a aplicação carrega a Ação por `GET /acoes/{id}`

#### Scenario: Detalhe inexistente
- **WHEN** a consulta por ID responde `404` sem código específico
- **THEN** a aplicação apresenta estado próprio de Ação não encontrada com forma de retornar à listagem

#### Scenario: Falha recuperável do detalhe
- **WHEN** o carregamento do detalhe falha por outro motivo
- **THEN** a aplicação preserva o erro normalizado e oferece retry que realiza novo GET por ID

### Requirement: Atualização manual de cotação somente no detalhe
O detalhe SHALL oferecer uma ação explícita para solicitar `PATCH /acoes/{id}/cotacao` sem dados de cotação fornecidos pelo usuário. A aplicação MUST NOT atualizar automaticamente, repetir ou agendar essa operação e MUST NOT disponibilizá-la na listagem.

#### Scenario: Solicitação manual
- **WHEN** o usuário aciona a atualização no detalhe
- **THEN** a aplicação envia exatamente um PATCH sem payload funcional de cotação, timestamp, ticker, mercado, moeda ou nome

#### Scenario: Operação em andamento
- **WHEN** o PATCH está em andamento
- **THEN** a ação permanece ocupada, anunciada semanticamente e protegida contra outra solicitação concorrente

#### Scenario: Atualização concluída
- **WHEN** o PATCH responde com `AcaoResponse`
- **THEN** a aplicação substitui o DTO local pela resposta, atualiza cotação e data/hora exibidas e comunica sucesso sem GET adicional

#### Scenario: Falha preserva estado anterior
- **WHEN** o PATCH falha
- **THEN** ticker, cotação, moeda e data/hora anteriormente exibidos são preservados, o erro é apresentado e uma nova tentativa só pode ocorrer por ação manual posterior

#### Scenario: Limite de requisições
- **WHEN** o PATCH responde `429` com `code=LIMITE_REQUISICOES_EXCEDIDO`
- **THEN** a aplicação informa o limite no contexto da atualização sem retry automático ou repetição temporizada

#### Scenario: Ticker canônico divergente
- **WHEN** o PATCH responde `409` com `code=TICKER_CANONICO_DIVERGENTE`
- **THEN** a aplicação informa que a atualização não foi aplicada, preserva o ticker e o DTO locais e não tenta aceitar a substituição

#### Scenario: Indisponibilidade ou timeout
- **WHEN** o PATCH responde com indisponibilidade ou timeout do provider
- **THEN** a aplicação preserva a última cotação exibida, mantém `message` e `details` do erro e libera a ação ao finalizar

### Requirement: Tratamento contextual preservando StandardError
A feature SHALL reutilizar a normalização HTTP central e SHALL interpretar apenas o contexto necessário de cadastro, busca, detalhe e atualização. Ela MUST preservar `status`, `code`, `message` e `details` e MUST NOT criar outro interceptor.

#### Scenario: Ticker inexistente no provider
- **WHEN** cadastro ou atualização responde `404` com `code=TICKER_INEXISTENTE`
- **THEN** a aplicação apresenta o erro de validação externa sem confundi-lo com a ausência de registro em consulta local

#### Scenario: Dados ou cotação inválidos
- **WHEN** o backend responde `DADOS_EXTERNOS_INCOMPLETOS`, `COTACAO_INDISPONIVEL` ou `COTACAO_FORA_DA_PRECISAO`
- **THEN** a aplicação apresenta a falha correspondente sem inventar cotação nem alterar o DTO atual

#### Scenario: Resposta externa inválida
- **WHEN** o backend responde `502` com `code=RESPOSTA_EXTERNA_INVALIDA`
- **THEN** a aplicação mantém mensagem e detalhes disponíveis para o fluxo atual

#### Scenario: Serviço externo indisponível
- **WHEN** o backend responde `503` com `code=SERVICO_EXTERNO_INDISPONIVEL`
- **THEN** a aplicação informa indisponibilidade sem chamada direta ao provider nem retry automático

#### Scenario: Timeout externo
- **WHEN** o backend responde `504` com `code=SERVICO_EXTERNO_TIMEOUT`
- **THEN** a aplicação informa timeout e permite apenas nova tentativa manual posterior

### Requirement: Apresentação responsiva sem cálculo financeiro
A aplicação SHALL apresentar ticker, empresa, mercado, moeda, última cotação persistida e referência temporal de modo responsivo. Formatação SHALL ser somente visual e MUST NOT converter moedas, somar valores de moedas distintas, transformar o DTO temporal ou apresentar a cotação como garantia de tempo real.

#### Scenario: Mercado e moeda apresentados
- **WHEN** uma Ação é exibida
- **THEN** `BRASIL` é apresentado amigavelmente como Brasil, `EUA` como EUA e a cotação é formatada conforme `BRL` ou `USD` recebido

#### Scenario: Data e cotação
- **WHEN** cotação e `dataHoraCotacao` são exibidas
- **THEN** a interface as identifica como última cotação persistida e data de atualização, formatando apenas a apresentação

#### Scenario: Viewport compacto
- **WHEN** listagem ou detalhe é exibido em tela compacta
- **THEN** cards e ações permanecem legíveis e operáveis em fluxo de coluna sem depender de tabela rígida

### Requirement: Interação acessível da área de Ações
As páginas SHALL possuir títulos principais, labels, erros associados, nomes acessíveis, foco visível e feedback dinâmico compreensível. Estados assíncronos SHALL ser comunicados semanticamente e mercado, moeda ou resultado MUST NOT depender somente de cor.

#### Scenario: Formulário acessível
- **WHEN** o usuário interage com busca ou cadastro por teclado
- **THEN** ticker, mercado, validações e ações possuem labels, associação semântica e ordem de foco coerentes

#### Scenario: Loading anunciado
- **WHEN** listagem, busca, cadastro, detalhe ou atualização está em andamento
- **THEN** a aplicação fornece indicação visual e anúncio semântico conciso do estado

#### Scenario: Feedback dinâmico
- **WHEN** uma ação termina com sucesso ou erro
- **THEN** o feedback é compreensível por tecnologia assistiva e não depende somente de cor

#### Scenario: Ações inequívocas
- **WHEN** o usuário acessa cadastro, detalhe, retry ou atualização de cotação
- **THEN** cada controle possui nome acessível que comunica seu efeito
