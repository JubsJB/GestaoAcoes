## MODIFIED Requirements

### Requirement: Listagem e estados da coleção
A página de listagem SHALL carregar `GET /carteiras` uma vez ao entrar, apresentar todas as Carteiras na ordem fornecida pelo backend e exibir nome e `dataCriacao` formatada somente na apresentação conforme o padrão temporal compartilhado. Ela SHALL diferenciar loading, coleção vazia, conteúdo e erro recuperável. A listagem SHALL usar tabela semântica no desktop e refluir para cards completos no mobile, com uma única representação acessível e focável, preservando nome, data de criação, contexto e ação Ver detalhes. O nome SHALL ser principal, sem destaque de ID técnico; a data SHALL manter o formatador atual. O contexto SHALL indicar textualmente “Ativa” apenas quando o ID corresponder ao activeId do CarteiraContextService, sem selecionar, persistir ou inicializar consultas por renderização. A listagem MUST NOT acrescentar requisições, métricas ou edição/exclusão inline e SHALL preservar Nova carteira no cabeçalho.

#### Scenario: Listagem com registros
- **WHEN** `GET /carteiras` devolve uma ou mais Carteiras
- **THEN** cada registro apresenta nome, data de criação e ação com nome acessível para abrir `/carteiras/{id}`

#### Scenario: Coleção vazia
- **WHEN** `GET /carteiras` devolve `[]`
- **THEN** a página apresenta estado vazio inline e CTA para cadastrar a primeira Carteira

#### Scenario: Carregamento
- **WHEN** a listagem está aguardando resposta
- **THEN** a página anuncia estado ocupado sem simular registros

#### Scenario: Falha na listagem
- **WHEN** `GET /carteiras` falha
- **THEN** a página apresenta o erro normalizado e oferece tentativa manual explícita

#### Scenario: Abertura do detalhe
- **WHEN** o usuário aciona uma Carteira listada
- **THEN** a aplicação abre seu detalhe e MAY transportar o DTO completo como estado transitório para evitar GET imediato redundante

#### Scenario: Tabela desktop e cards mobile
- **WHEN** Carteiras com nomes longos são exibidas
- **THEN** nome, data, contexto e Ver detalhes permanecem completos e legíveis em uma linha por Carteira no desktop e cards no mobile, sem truncamento obrigatório, controles duplicados ou métricas novas


#### Scenario: Indicação passiva do contexto global
- **WHEN** a Carteira ativa muda no CarteiraContextService
- **THEN** somente a Carteira correspondente recebe a indicação textual Ativa, sem nova requisição, mudança de seleção ou persistência causada pela listagem; quando não há correspondência, nenhum item é marcado ativo


### Requirement: Detalhe básico da Carteira
A aplicação SHALL apresentar em `/carteiras/{id}` nome, identificador e data de criação da Carteira, com ação textual de retorno para `/carteiras` e ações Editar e Excluir. O detalhe SHALL incorporar uma seção de histórico de Operações e uma ação “Registrar operação” fornecidas pela capability `frontend-operation-management`, incorporando posições abertas pelo contrato existente e sem acrescentar indicadores não solicitados. O detalhe SHALL separar contexto cadastral, ações, posições abertas e histórico por hierarquia visual, preservando os campos autoritativos existentes, reação à mudança de rota e estados independentes de posições e histórico. O histórico SHALL adotar o mesmo padrão semântico de tabela desktop e cards mobile de Operações, sem duplicar consultas nem alterar o formulário contextual.

#### Scenario: Detalhe com estado transitório
- **WHEN** a navegação fornece `CarteiraResponse` compatível com o ID da rota
- **THEN** o detalhe usa o DTO sem GET redundante e ainda consulta o histórico contextual

#### Scenario: Detalhe sem estado transitório
- **WHEN** a rota é aberta diretamente ou recarregada
- **THEN** o detalhe consulta `GET /carteiras/{id}` e apresenta loading enquanto aguarda os dados básicos

#### Scenario: Carteira inexistente
- **WHEN** a consulta individual responde `404`
- **THEN** a página apresenta estado de não encontrado, não simula histórico e oferece caminho acessível para voltar à listagem

#### Scenario: Histórico contextual
- **WHEN** a Carteira existe
- **THEN** o detalhe consulta `GET /carteiras/{carteiraId}/operacoes`, apresenta a ordem recebida e diferencia loading, vazio, conteúdo e erro do histórico sem ocultar os dados básicos

#### Scenario: Cadastro contextual
- **WHEN** o usuário aciona “Registrar operação” no detalhe
- **THEN** o mesmo formulário discriminado do fluxo global é aberto com a Carteira pré-selecionada e não editável, aplicando prévia somente leitura em COMPRA e sugestão editável em VENDA, sempre sem ordem manual e com os mesmos payloads do POST

#### Scenario: Sucesso no cadastro contextual
- **WHEN** uma criação contextual é concluída
- **THEN** o histórico é atualizado com preço, ordem e total do DTO retornado sem GET redundante obrigatório nem cálculo financeiro

#### Scenario: Sem antecipação financeira
- **WHEN** o detalhe é exibido
- **THEN** somente posições abertas e histórico são apresentados como seções financeiras; resumo, patrimônio agregado e evolução não são adicionados

#### Scenario: Histórico visual consistente
- **WHEN** o histórico contextual está disponível
- **THEN** seus campos, ordem e estados permanecem equivalentes aos de Operações, com apenas uma representação acessível ativa


