## Purpose

Permitir que usuários listem, cadastrem, busquem e consultem Corretoras pelo frontend, usando exclusivamente os contratos vigentes do backend e sem reproduzir suas regras ou integrações externas.

## ADDED Requirements

### Requirement: Integração restrita aos contratos de Corretoras
A área de Corretoras SHALL consumir exclusivamente os contratos `POST /corretoras`, `GET /corretoras`, `GET /corretoras/{id}` e `GET /corretoras/por-cnpj?cnpj=...` por meio da configuração central da API. O frontend MUST NOT consultar BrasilAPI, ViaCEP ou outro provider diretamente, nem calcular ou inferir dados cadastrais controlados pelo backend.

#### Scenario: Uso da configuração central
- **WHEN** a área de Corretoras realiza uma operação HTTP
- **THEN** a URL é composta com a configuração central da API e um dos quatro contratos aprovados

#### Scenario: Ausência de integração externa direta
- **WHEN** cadastro, listagem, busca ou detalhe são utilizados
- **THEN** o navegador não realiza requisições diretas à BrasilAPI, ViaCEP ou outro provider de dados cadastrais

### Requirement: Listagem das Corretoras cadastradas
A aplicação SHALL apresentar em `/corretoras` todas as Corretoras devolvidas por `GET /corretoras`, preservando a ordem recebida e oferecendo acesso ao cadastro e ao detalhe de cada registro.

#### Scenario: Listagem com registros
- **WHEN** o backend devolve uma ou mais Corretoras
- **THEN** a página apresenta cada registro com identificação suficiente e ação para consultar seus dados completos

#### Scenario: Listagem vazia
- **WHEN** o backend devolve um array vazio
- **THEN** a página apresenta um estado vazio compreensível e uma forma de iniciar o cadastro

#### Scenario: Carregamento da listagem
- **WHEN** a consulta da listagem está em andamento
- **THEN** a página comunica o carregamento sem apresentar resultado vazio prematuramente

#### Scenario: Falha da listagem
- **WHEN** a consulta da listagem falha
- **THEN** a página apresenta erro recuperável e permite tentar novamente

### Requirement: Busca exata por CNPJ
A página de Corretoras SHALL permitir buscar uma Corretora por CNPJ usando `GET /corretoras/por-cnpj?cnpj=...` somente após ação explícita do usuário. A busca SHALL aceitar a entrada visual com ou sem máscara, MUST NOT realizar consulta durante a digitação e MUST NOT transformar `GET /corretoras` em filtro genérico nem substituir permanentemente a coleção completa já carregada.

#### Scenario: Busca com resultado
- **WHEN** o usuário informa um CNPJ com formato aceito e o backend devolve uma Corretora
- **THEN** a aplicação conduz o usuário ao detalhe do registro devolvido

#### Scenario: Busca sem correspondência
- **WHEN** o backend responde que não existe Corretora para o CNPJ informado
- **THEN** a página informa que nenhum registro foi encontrado sem alterar a listagem persistida

#### Scenario: Busca sem entrada utilizável
- **WHEN** o CNPJ está vazio ou não possui o formato básico aceito
- **THEN** a aplicação orienta a correção e não envia a consulta

#### Scenario: Limpeza da busca
- **WHEN** o usuário limpa ou cancela a busca após a listagem completa ter sido carregada
- **THEN** a aplicação volta imediatamente à coleção preservada sem realizar automaticamente novo `GET /corretoras`

### Requirement: Cadastro inicial somente por CNPJ
A aplicação SHALL disponibilizar `/corretoras/nova` com um formulário que solicita somente o CNPJ como dado permanente. A primeira tentativa SHALL enviar apenas `cnpj`; o frontend MUST NOT solicitar, preencher ou enviar preventivamente os demais dados cadastrais nem o controle de confirmação de situação não ativa.

#### Scenario: Primeira tentativa válida
- **WHEN** o usuário submete um CNPJ com formato básico aceito
- **THEN** a aplicação envia `POST /corretoras` com um corpo contendo somente `cnpj`

#### Scenario: Validação de experiência
- **WHEN** o campo está vazio, incompleto ou contém formato não aceito
- **THEN** o formulário apresenta orientação acessível e não envia a requisição

#### Scenario: Autoridade do backend
- **WHEN** o CNPJ possui formato básico aceito pelo frontend
- **THEN** validade algorítmica, existência, unicidade, dados cadastrais, endereço e situação continuam sendo decididos pelo backend

#### Scenario: Submissão em andamento
- **WHEN** uma tentativa de cadastro está em andamento
- **THEN** o formulário evita submissões concorrentes e comunica o processamento

### Requirement: Confirmação contextual de situação cadastral não ativa
Somente quando a primeira tentativa responder `409 Conflict` com `code=SITUACAO_CADASTRAL_NAO_ATIVA`, a aplicação SHALL informar que a situação devolvida não é ativa e solicitar confirmação explícita. O frontend MUST NOT determinar por conta própria a situação nem cadastrar ou reenviar automaticamente.

#### Scenario: Solicitação de confirmação
- **WHEN** o backend responde `409` com o código específico de situação não ativa
- **THEN** a aplicação apresenta a situação informada pelo backend e oferece escolhas explícitas para confirmar ou cancelar

#### Scenario: Confirmação aceita
- **WHEN** o usuário confirma conscientemente o cadastro após o conflito específico
- **THEN** a aplicação realiza nova requisição com o mesmo `cnpj` e `confirmarSituacaoCadastralNaoAtiva=true`

#### Scenario: Confirmação cancelada
- **WHEN** o usuário cancela ou fecha a confirmação
- **THEN** nenhuma nova requisição de cadastro é realizada

#### Scenario: Outro conflito
- **WHEN** o backend responde `409` com qualquer outro código
- **THEN** a falha segue o tratamento normal de erro e o fluxo de confirmação não é apresentado

#### Scenario: Controle não permanente
- **WHEN** o formulário é exibido antes ou depois do fluxo excepcional
- **THEN** `confirmarSituacaoCadastralNaoAtiva` não aparece como campo permanente nem fica selecionado para uma tentativa futura

### Requirement: Detalhe completo da Corretora
A aplicação SHALL disponibilizar `/corretoras/:id` e apresentar o contrato completo da Corretora, distinguindo campos opcionais ausentes de dados obrigatórios e sem alterar o significado dos valores recebidos. A transição imediatamente posterior ao cadastro SHALL poder usar o `CorretoraResponse` completo devolvido pelo POST; acesso direto, refresh ou navegação sem esse DTO SHALL obter o registro por `GET /corretoras/{id}`.

#### Scenario: Detalhe existente
- **WHEN** o backend devolve a Corretora solicitada por ID
- **THEN** a página apresenta identificação, contatos, endereço, situação cadastral, estado de validação financeira e data de cadastro

#### Scenario: Detalhe sem estado transitório
- **WHEN** `/corretoras/:id` é acessada diretamente, recarregada ou aberta sem DTO previamente disponível
- **THEN** a aplicação consulta `GET /corretoras/{id}` e apresenta o contrato devolvido

#### Scenario: Campos opcionais ausentes
- **WHEN** nome fantasia, e-mail, telefone, número ou complemento possuem valor nulo
- **THEN** a interface indica a ausência sem inventar, ocultar o registro ou exibir o texto literal `null`

#### Scenario: Validação financeira pendente
- **WHEN** `validadaMercadoFinanceiro` é falso
- **THEN** a interface comunica que a validação ainda não foi realizada, sem afirmar que a instituição não pertence ao mercado financeiro

#### Scenario: Detalhe inexistente
- **WHEN** a consulta por ID responde que a Corretora não existe
- **THEN** a página apresenta um estado de não encontrado e uma forma de retornar à listagem

### Requirement: Resultado e erros das operações
A área SHALL comunicar sucesso e falha de forma contextual, preservando `status`, `code`, `message` e `details` dos erros padronizados disponibilizados pela infraestrutura. Após cadastro concluído, a aplicação SHALL informar o sucesso e conduzir o usuário ao detalhe da Corretora criada.

#### Scenario: Cadastro concluído
- **WHEN** a primeira tentativa ou a tentativa explicitamente confirmada devolve a Corretora criada
- **THEN** a aplicação apresenta feedback de sucesso, navega para `/corretoras/{id}` reutilizando o DTO devolvido e não realiza `GET /corretoras/{id}` imediatamente apenas para buscar os mesmos dados

#### Scenario: Erro padronizado
- **WHEN** uma operação falha com erro padronizado diferente do conflito contextual aprovado
- **THEN** a interface apresenta mensagem recuperável coerente com a falha sem perder seus dados técnicos nem inventar regra de negócio

#### Scenario: Falha técnica
- **WHEN** uma operação falha sem corpo padronizado
- **THEN** a interface apresenta a falha técnica normalizada e permite recuperação quando aplicável

### Requirement: Experiência responsiva e acessível
Listagem, busca, cadastro, confirmação e detalhe SHALL permanecer utilizáveis em desktop e viewports compactos. Estados dinâmicos e validações SHALL ser perceptíveis por teclado e tecnologias assistivas, com ordem de foco coerente, rótulos acessíveis e foco direcionado quando um erro ou confirmação exigir atenção.

#### Scenario: Uso em viewport compacto
- **WHEN** a área é utilizada em uma largura compacta
- **THEN** conteúdo, ações e dados permanecem legíveis e operáveis sem rolagem horizontal obrigatória para concluir a tarefa principal

#### Scenario: Operação por teclado
- **WHEN** o usuário percorre listagem, busca, formulário, confirmação e detalhe somente por teclado
- **THEN** os controles podem ser alcançados e acionados com foco visível e ordem coerente

#### Scenario: Anúncio de estado dinâmico
- **WHEN** carregamento, sucesso, estado vazio ou erro altera o conteúdo apresentado
- **THEN** a mudança relevante é comunicada semanticamente sem depender apenas de cor
