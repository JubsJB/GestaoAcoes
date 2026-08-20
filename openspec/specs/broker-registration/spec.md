# broker-registration Specification

## Purpose

Definir a primeira fatia funcional para cadastrar e persistir uma corretora por CNPJ, com dados cadastrais consultados na BrasilAPI, endereço validado pela ViaCEP e confirmação explícita para situação cadastral não ativa.

## Requirements

### Requirement: Contrato de entrada restrito ao CNPJ
O sistema SHALL expor `POST /corretoras` com uma solicitação inicial que aceita somente `cnpj` como dado da corretora. O controle opcional `confirmarSituacaoCadastralNaoAtiva` SHALL ser aceito exclusivamente para expressar a confirmação do fluxo de situação não ativa e SHALL NOT representar um dado cadastral persistido.

#### Scenario: Solicitação inicial válida
- **WHEN** o cliente envia uma solicitação inicial contendo somente `cnpj`
- **THEN** o sistema inicia as validações e consultas externas sem aceitar outros dados cadastrais do cliente

#### Scenario: Tentativa de fornecer dados externos
- **WHEN** o cliente inclui razão social, nome fantasia, e-mail, telefone, endereço, situação cadastral, validação no mercado financeiro, data de cadastro ou outro dado da corretora na solicitação
- **THEN** o sistema rejeita a solicitação com erro padronizado e não permite que o valor enviado substitua dados das fontes externas ou da aplicação

### Requirement: Normalização, validação e unicidade do CNPJ
O sistema SHALL aceitar CNPJ com ou sem máscara, SHALL normalizá-lo para somente 14 dígitos, SHALL validar localmente seus dígitos verificadores, SHALL consultar sua existência na BrasilAPI e SHALL persistir somente o valor normalizado. O CNPJ normalizado MUST ser único no banco.

#### Scenario: CNPJ mascarado válido
- **WHEN** o cliente informa um CNPJ válido com máscara
- **THEN** o sistema remove a máscara, valida os dígitos verificadores e usa os 14 dígitos normalizados nas consultas e na persistência

#### Scenario: CNPJ sem máscara válido
- **WHEN** o cliente informa um CNPJ válido com 14 dígitos sem máscara
- **THEN** o sistema valida os dígitos verificadores e mantém os 14 dígitos como representação normalizada

#### Scenario: CNPJ com dígitos verificadores inválidos
- **WHEN** o CNPJ normalizado não possui 14 dígitos ou falha na validação local dos dígitos verificadores
- **THEN** o sistema rejeita o cadastro como CNPJ inválido, não consulta as fontes externas e não persiste a corretora

#### Scenario: CNPJ inexistente
- **WHEN** a BrasilAPI informa que não existe empresa para o CNPJ normalizado
- **THEN** o sistema rejeita o cadastro como CNPJ inexistente e não persiste a corretora

#### Scenario: CNPJ duplicado
- **WHEN** já existe uma corretora com o mesmo CNPJ normalizado
- **THEN** o sistema rejeita a tentativa como cadastro duplicado e não cria outro registro

### Requirement: Dados cadastrais provenientes da BrasilAPI
O sistema SHALL obter ou validar na BrasilAPI os dados cadastrais relacionados ao CNPJ. CNPJ, razão social, CEP e situação cadastral SHALL estar disponíveis para o cadastro prosseguir; nome fantasia, e-mail, telefone, número e complemento SHALL ser preenchidos quando retornados e sua ausência SHALL NOT impedir o cadastro.

#### Scenario: Dados cadastrais obrigatórios disponíveis
- **WHEN** a BrasilAPI encontra o CNPJ e retorna razão social, CEP e situação cadastral
- **THEN** o sistema utiliza esses dados no fluxo sem solicitar seu preenchimento ao cliente

#### Scenario: Dado cadastral obrigatório ausente
- **WHEN** a BrasilAPI encontra o CNPJ, mas não fornece razão social, CEP ou situação cadastral
- **THEN** o sistema rejeita o cadastro como dados externos obrigatórios incompletos e não persiste a corretora

#### Scenario: Dados cadastrais opcionais ausentes
- **WHEN** nome fantasia, e-mail, telefone, número ou complemento não estiver disponível nas fontes externas
- **THEN** o sistema mantém o respectivo campo opcional sem valor e permite que o fluxo prossiga

### Requirement: Endereço validado e preenchido pela ViaCEP
O sistema SHALL consultar a ViaCEP usando o CEP obtido no fluxo da BrasilAPI. CEP, logradouro, bairro, cidade e UF SHALL ser validados e preenchidos por fontes externas antes da persistência, sem complemento manual pelo cliente.

#### Scenario: CEP existente com endereço obrigatório completo
- **WHEN** a ViaCEP encontra o CEP e disponibiliza logradouro, bairro, cidade e UF
- **THEN** o sistema utiliza os dados de endereço retornados e considera a validação do CEP concluída

#### Scenario: CEP inválido ou inexistente
- **WHEN** o CEP obtido não possui formato válido ou a ViaCEP informa que ele não existe
- **THEN** o sistema rejeita o cadastro com o erro correspondente e não persiste a corretora

#### Scenario: Endereço obrigatório incompleto
- **WHEN** a ViaCEP encontra o CEP, mas não disponibiliza logradouro, bairro, cidade ou UF
- **THEN** o sistema rejeita o cadastro como dados externos obrigatórios incompletos e não fabrica nem solicita ao cliente o dado ausente

### Requirement: Confirmação explícita para situação cadastral não ativa
O sistema SHALL permitir o fluxo normal quando a situação cadastral retornada pela BrasilAPI for exatamente `ATIVA`. Para qualquer outro valor, o sistema SHALL exigir confirmação explícita em uma nova requisição antes de persistir e SHALL preservar sem alteração a situação cadastral mais recente retornada pela fonte.

#### Scenario: Situação cadastral ativa
- **WHEN** a BrasilAPI retorna situação cadastral `ATIVA` e as demais validações têm sucesso
- **THEN** o sistema conclui o cadastro sem exigir confirmação adicional

#### Scenario: Primeira requisição com situação cadastral não ativa
- **WHEN** a BrasilAPI retorna uma situação diferente de `ATIVA` e a solicitação não possui `confirmarSituacaoCadastralNaoAtiva=true`
- **THEN** o sistema responde `409 Conflict` sem persistir, com código `SITUACAO_CADASTRAL_NAO_ATIVA` e detalhes que contêm a situação real e `confirmacaoNecessaria=true`

#### Scenario: Nova requisição explicitamente confirmada
- **WHEN** o cliente envia uma nova solicitação para o mesmo CNPJ com `confirmarSituacaoCadastralNaoAtiva=true`
- **THEN** o sistema repete as validações e consultas externas e, se elas tiverem sucesso, persiste a corretora mantendo exatamente a situação cadastral vigente retornada pela BrasilAPI

#### Scenario: Situação torna-se ativa na requisição confirmada
- **WHEN** a nova solicitação confirmada encontra a situação cadastral `ATIVA`
- **THEN** o sistema conclui o cadastro pelo fluxo normal e persiste `ATIVA`

### Requirement: Estado de validação no mercado financeiro
O sistema SHALL persistir e devolver `validadaMercadoFinanceiro=false` enquanto não existir uma fonte pública aprovada para essa validação. O valor `false` SHALL significar exclusivamente “ainda não validada no mercado financeiro” e MUST NOT ser interpretado como confirmação de que a instituição não pertence ao mercado financeiro.

#### Scenario: Cadastro sem fonte de validação no mercado financeiro
- **WHEN** uma corretora é cadastrada nesta fatia
- **THEN** o sistema persiste e devolve `validadaMercadoFinanceiro=false` com a semântica de validação ainda não realizada

### Requirement: Conteúdo e data do cadastro
O sistema SHALL persistir `id`, CNPJ, razão social, nome fantasia, e-mail, telefone, CEP, logradouro, número, complemento, bairro, cidade, UF, situação cadastral, validação no mercado financeiro e data de cadastro. `dataCadastro` SHALL ser um `OffsetDateTime` gerado pela aplicação em UTC no momento da persistência e MUST NOT ser aceito do cliente nem obtido das APIs externas.

#### Scenario: Persistência dos dados consolidados
- **WHEN** todas as validações obrigatórias terminam com sucesso e qualquer confirmação exigida foi fornecida
- **THEN** o sistema persiste uma única corretora com os dados consolidados das fontes externas, os campos opcionais ausentes sem valor e `validadaMercadoFinanceiro=false`

#### Scenario: Geração da data de cadastro
- **WHEN** a corretora é efetivamente persistida
- **THEN** a aplicação gera `dataCadastro` com offset UTC naquele instante e devolve o valor em formato ISO-8601

### Requirement: Resposta do cadastro concluído
O sistema SHALL responder ao cadastro concluído com `201 Created`, SHALL retornar o DTO completo da corretora persistida e SHALL incluir `Location` apontando para `/corretoras/{id}`.

#### Scenario: Resposta de sucesso
- **WHEN** a corretora é persistida com sucesso pelo fluxo ativo ou pelo fluxo explicitamente confirmado
- **THEN** a resposta possui status `201 Created`, header `Location: /corretoras/{id}` e corpo com todos os campos do cadastro, mantendo campos opcionais sem valor quando indisponíveis

### Requirement: Schema versionado e validado
O schema necessário ao cadastro SHALL ser criado e evoluído pelo Liquibase por meio do mesmo changelog aplicável aos ambientes de execução e teste. O Hibernate SHALL apenas validar a correspondência do mapeamento com o schema e MUST NOT usar `create`, `update` ou `create-drop` como estratégia desta funcionalidade.

#### Scenario: Inicialização sobre banco sem a estrutura de corretora
- **WHEN** a aplicação inicia em um ambiente no qual o changeSet de Corretora ainda não foi aplicado
- **THEN** o Liquibase aplica o changeSet antes de o Hibernate validar o mapeamento

#### Scenario: Inicialização dos testes
- **WHEN** os testes iniciam com o banco H2 efêmero
- **THEN** o mesmo changelog master cria a estrutura de Corretora e o Hibernate a valida com `ddl-auto=validate`

### Requirement: Integridade e tratamento de falhas
O sistema SHALL persistir somente após o sucesso de todas as validações e SHALL devolver erros no formato padronizado da API para validações rejeitadas, confirmação necessária, duplicidade, dados externos incompletos e falhas nas integrações.

#### Scenario: Falha antes da persistência
- **WHEN** qualquer validação, consulta externa ou requisito de confirmação falha
- **THEN** o sistema não cria cadastro total nem parcial de corretora

#### Scenario: Serviço externo indisponível ou com timeout
- **WHEN** a BrasilAPI ou a ViaCEP estiver indisponível ou exceder o tempo limite
- **THEN** o sistema informa a falha externa em erro padronizado e não persiste cadastro parcial

#### Scenario: Limite de requisições excedido
- **WHEN** a BrasilAPI ou a ViaCEP informar que seu limite de requisições foi excedido
- **THEN** o sistema informa a falha externa em erro padronizado e não persiste cadastro parcial
