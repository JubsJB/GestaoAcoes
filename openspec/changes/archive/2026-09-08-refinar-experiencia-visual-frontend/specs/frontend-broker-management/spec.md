## MODIFIED Requirements

### Requirement: Listagem das Corretoras cadastradas
A aplicação SHALL apresentar em `/corretoras` todas as Corretoras devolvidas por `GET /corretoras`, preservando a ordem recebida e oferecendo acesso ao cadastro e ao detalhe de cada registro. A coleção SHALL usar uma tabela semântica desktop que reflui para cards completos mobile, com uma única estrutura acessível e focável. As colunas SHALL ser Instituição, CNPJ, Localidade, Situação cadastral e Ações, preservando razão social, nome fantasia quando distinto, dados atuais, badge textual e Ver detalhes. A busca por CNPJ e o conjunto de requisições MUST permanecer inalterados; a coleção MUST NOT acrescentar dados ou métricas.

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

#### Scenario: Densidade cadastral
- **WHEN** há várias Corretoras cadastradas
- **THEN** os registros são comparáveis na tabela desktop e refluem em cards completos no mobile sem perda de dados, ações ou duplicação de controles acessíveis


### Requirement: Detalhe completo da Corretora
A aplicação SHALL disponibilizar `/corretoras/:id`, apresentar o contrato completo e formatar `dataCadastro` somente na apresentação como `dd/MM/yyyy às HH:mm`, em `pt-BR` e timezone local do navegador. O DTO SHALL permanecer inalterado e o estado transitório compatível SHALL continuar evitando GET redundante. O detalhe SHALL agrupar visualmente identificação, contato, endereço e situação, com tipografia e superfícies compartilhadas; informação ausente e validação pendente SHALL continuar explícitas.

#### Scenario: Detalhe existente
- **WHEN** o backend devolve a Corretora solicitada por ID
- **THEN** a página apresenta identificação, contatos, endereço, situação, validação financeira e data de cadastro no padrão aprovado

#### Scenario: Detalhe sem estado transitório
- **WHEN** `/corretoras/:id` é acessada diretamente, recarregada ou aberta sem DTO previamente disponível
- **THEN** a aplicação consulta `GET /corretoras/{id}` e apresenta o contrato devolvido

#### Scenario: Campos opcionais ausentes
- **WHEN** nome fantasia, e-mail, telefone, número ou complemento possuem valor nulo
- **THEN** a interface indica a ausência sem inventar, ocultar o registro ou exibir `null`

#### Scenario: Validação financeira pendente
- **WHEN** `validadaMercadoFinanceiro` é falso
- **THEN** a interface comunica que a validação ainda não foi realizada sem afirmar que a instituição não pertence ao mercado financeiro

#### Scenario: Detalhe inexistente
- **WHEN** a consulta por ID responde que a Corretora não existe
- **THEN** a página apresenta estado de não encontrado e forma de retornar à listagem

#### Scenario: Seções cadastrais
- **WHEN** o detalhe completo é exibido
- **THEN** headings e pares label/valor permitem localizar informações sem inventar ou ocultar campos


