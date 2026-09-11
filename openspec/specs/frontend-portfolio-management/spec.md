# frontend-portfolio-management Specification

## Purpose
Disponibilizar o gerenciamento frontend básico e acessível de Carteiras sobre os contratos REST existentes, sem antecipar operações ou informações financeiras.

## Requirements

### Requirement: Contrato frontend mínimo de Carteiras
A área de Carteiras SHALL consumir `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}`, `PATCH /carteiras/{id}` e `DELETE /carteiras/{id}` pela configuração central da API. Para integrar o histórico contextual aprovado, o detalhe SHALL também consumir `GET /carteiras/{carteiraId}/operacoes` por meio da capability `frontend-operation-management` e `GET /carteiras/{id}/posicoes` pelo contrato existente de posições. O frontend SHALL representar `id`, `nome` e `dataCriacao` do `CarteiraResponse`, SHALL enviar somente `nome` nos requests de criação e edição e MUST NOT introduzir campos, endpoints ou dados derivados.

#### Scenario: Leitura do DTO básico
- **WHEN** o backend devolve uma Carteira
- **THEN** a aplicação preserva `id`, `nome` e `dataCriacao` sem acrescentar indicadores financeiros

#### Scenario: Requests mínimos
- **WHEN** o usuário cria ou edita uma Carteira
- **THEN** o frontend envia um corpo contendo exatamente `nome`

#### Scenario: Limite funcional
- **WHEN** a capability é apresentada
- **THEN** ela consulta e exibe os dados básicos, histórico de Operações e posições abertas aprovadas, sem acrescentar resumo, patrimônio agregado, evolução, snapshots, gráficos ou conversão cambial

### Requirement: Rotas funcionais e carregamento lazy
A área SHALL substituir somente o placeholder de Carteiras e SHALL manter seu limite lazy. Ela SHALL oferecer `/carteiras` para listagem, `/carteiras/nova` para cadastro direto, `/carteiras/{id}` para detalhe e `/carteiras/{id}/editar` para edição direta, com as rotas estáticas resolvidas antes do parâmetro de identificador.

#### Scenario: Listagem direta
- **WHEN** o usuário acessa `/carteiras`
- **THEN** a listagem funcional é carregada dentro do shell pelo limite lazy existente

#### Scenario: Cadastro direto
- **WHEN** o usuário acessa ou recarrega `/carteiras/nova`
- **THEN** o formulário de cadastro vazio permanece disponível sem request automático

#### Scenario: Detalhe direto
- **WHEN** o usuário acessa `/carteiras/{id}` sem estado transitório compatível
- **THEN** a aplicação consulta `GET /carteiras/{id}`

#### Scenario: Edição direta
- **WHEN** o usuário acessa ou recarrega `/carteiras/{id}/editar`
- **THEN** a aplicação obtém a Carteira quando necessário e apresenta o formulário preenchido

#### Scenario: Outras áreas preservadas
- **WHEN** Carteiras se torna funcional
- **THEN** Dashboard e Operações continuam placeholders e Corretoras e Ações mantêm seus limites e comportamentos

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

### Requirement: Cadastro contextual e direto
A aplicação SHALL criar Carteiras por Typed Reactive Form contendo somente `nome`, com validação estrutural de obrigatoriedade, conteúdo não branco e máximo de 255 caracteres. A listagem SHALL iniciar o cadastro em dialog acessível, enquanto `/carteiras/nova` SHALL reutilizar o mesmo formulário em página. O backend SHALL permanecer autoridade final e nomes duplicados MUST NOT ser rejeitados localmente.

#### Scenario: Cadastro iniciado pela listagem
- **WHEN** o usuário aciona o CTA de cadastro
- **THEN** a aplicação abre dialog com nome vazio e não realiza HTTP antes da submissão

#### Scenario: Validação estrutural
- **WHEN** o nome é ausente, branco ou excede 255 caracteres
- **THEN** o formulário apresenta validação local e não envia POST

#### Scenario: Submissão explícita
- **WHEN** o usuário submete um nome estruturalmente válido
- **THEN** a aplicação envia um único `POST /carteiras` com exatamente `nome` e bloqueia submissão concorrente

#### Scenario: Cadastro contextual concluído
- **WHEN** o POST iniciado em dialog devolve o `CarteiraResponse`
- **THEN** o dialog fecha, a listagem incorpora o DTO sem novo GET e apresenta toast de sucesso

#### Scenario: Cadastro direto concluído
- **WHEN** o POST iniciado em `/carteiras/nova` devolve o `CarteiraResponse`
- **THEN** a aplicação abre `/carteiras/{id}` com o DTO transitório sem GET redundante

#### Scenario: Cancelamento
- **WHEN** o usuário cancela o cadastro antes da submissão
- **THEN** o dialog ou página retorna ao contexto anterior sem POST

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

### Requirement: Edição exclusiva do nome
A aplicação SHALL editar somente `nome` por Typed Reactive Form preenchido com o valor atual. O detalhe SHALL iniciar a edição em dialog acessível e `/carteiras/{id}/editar` SHALL reutilizar o mesmo formulário em página. A aplicação MUST NOT enviar `id`, `dataCriacao` ou outro campo.

#### Scenario: Abertura contextual
- **WHEN** o usuário aciona Editar no detalhe
- **THEN** o dialog abre com o nome atual e sem PATCH automático

#### Scenario: Submissão da edição
- **WHEN** o usuário submete nome estruturalmente válido
- **THEN** a aplicação envia um único `PATCH /carteiras/{id}` com exatamente `nome`

#### Scenario: Edição contextual concluída
- **WHEN** o PATCH no dialog devolve `CarteiraResponse`
- **THEN** o dialog fecha, o detalhe substitui seu DTO local pela resposta sem GET e apresenta toast de sucesso

#### Scenario: Edição direta concluída
- **WHEN** o PATCH em `/carteiras/{id}/editar` devolve `CarteiraResponse`
- **THEN** a aplicação abre o detalhe com a resposta como estado transitório sem GET redundante

#### Scenario: Cancelamento da edição
- **WHEN** o usuário cancela antes de submeter
- **THEN** o estado atual permanece visível sem PATCH

#### Scenario: Erro da edição
- **WHEN** o PATCH falha
- **THEN** o formulário permanece aberto, preserva entrada e estado anterior e apresenta `StandardError` sem retry automático

### Requirement: Exclusão explicitamente confirmada
A aplicação SHALL solicitar confirmação acessível antes de `DELETE /carteiras/{id}`, identificando a Carteira pelo nome e oferecendo Cancelar e Excluir com hierarquia inequívoca. A aplicação MUST NOT excluir por abertura do dialog, navegação, backdrop, Escape ou cancelamento.

#### Scenario: Abertura da confirmação
- **WHEN** o usuário aciona Excluir no detalhe
- **THEN** um dialog descreve o efeito, identifica a Carteira e aguarda decisão explícita sem HTTP

#### Scenario: Cancelamento da exclusão
- **WHEN** o usuário cancela, pressiona Escape ou fecha pelo backdrop
- **THEN** nenhum DELETE é enviado e o detalhe permanece inalterado

#### Scenario: Confirmação da exclusão
- **WHEN** o usuário confirma explicitamente
- **THEN** a aplicação envia um único `DELETE /carteiras/{id}` e bloqueia nova confirmação enquanto aguarda

#### Scenario: Exclusão concluída
- **WHEN** o DELETE responde `204 No Content`
- **THEN** a aplicação navega para `/carteiras`, apresenta toast de sucesso e a listagem carregada não contém a Carteira removida

#### Scenario: Conflito de exclusão
- **WHEN** o backend responde `409` por Operações ou snapshots existentes
- **THEN** a aplicação preserva `code`, `message` e `details`, mantém a Carteira e não inventa elegibilidade local

#### Scenario: Falha de exclusão
- **WHEN** o DELETE falha com outro erro
- **THEN** o detalhe permanece disponível e apresenta a falha sem remoção otimista definitiva ou retry automático

### Requirement: Feedback, responsividade e acessibilidade
A feature SHALL reutilizar cabeçalho, feedback contextual, toast superior, retorno textual, padrões de superfície e tokens Financial Olive compartilhados. Páginas, cards, formulários e dialogs SHALL permanecer operáveis em desktop e mobile, com foco visível, nomes acessíveis, ordem de foco coerente, restauração de foco, estados ocupados anunciados e informação não dependente somente de cor.

#### Scenario: Erro normalizado
- **WHEN** uma operação recebe `StandardError`
- **THEN** o feedback apresenta mensagem e detalhes sem criar outro formato de erro

#### Scenario: Sucesso transitório
- **WHEN** cadastro, edição ou exclusão conclui
- **THEN** a aplicação usa o toast compartilhado superior com descarte e fechamento existentes

#### Scenario: Dialog acessível
- **WHEN** cadastro, edição ou confirmação é aberto em dialog
- **THEN** título e descrição são associados, foco fica contido, Escape e backdrop respeitam cancelamento e o foco retorna ao acionador

#### Scenario: Viewport compacto
- **WHEN** a feature é exibida em tela compacta
- **THEN** cards, nomes longos, formulários, feedbacks e ações permanecem legíveis e operáveis sem overflow horizontal obrigatório

### Requirement: Posições abertas no detalhe da Carteira
O detalhe SHALL consumir `GET /carteiras/{id}/posicoes` pelo serviço e modelo existentes e apresentar ticker, empresa, mercado, moeda, quantidade atual, preço médio, cotação atual, valor atual, resultado não realizado e rentabilidade autoritativos. SHALL reutilizar parsing lossless e formatadores sem modificá-los, manter BRL e USD identificados sem soma ou conversão e MUST NOT recalcular valores, inferir saldo pelo histórico ou criar rota de posição. Loading, vazio e erro com retry manual SHALL ser independentes do histórico e dos dados básicos.

#### Scenario: Dados disponíveis
- **WHEN** a Carteira válida retorna posições
- **THEN** os valores retornados são exibidos integralmente em apresentação responsiva e acessível, com a respectiva moeda

#### Scenario: Posições vazias ou falha
- **WHEN** a consulta retorna vazia ou falha
- **THEN** a seção mostra respectivamente ausência de posições abertas ou erro recuperável sem esconder identificação e histórico

#### Scenario: Operação registrada
- **WHEN** um cadastro contextual conclui com 201 e a Carteira de origem continua aberta
- **THEN** o histórico incorpora o DTO conforme contrato vigente e as posições são consultadas novamente no backend, sem cálculo local nem criação de snapshot

### Requirement: Detalhe reativo e seleção global
O detalhe SHALL reagir a mudanças do identificador na mesma rota, validar a Carteira, selecionar e persistir a abertura válida, e invalidar imediatamente dados e requests do contexto anterior. Estado transitório SHALL ser aceito somente quando compatível com o ID atual. Trocar a Carteira no shell estando no detalhe SHALL navegar ao novo detalhe.

#### Scenario: Troca sem recriar componente
- **WHEN** `/carteiras/{id}` muda para outro ID na mesma instância de página
- **THEN** dados básicos, posições e histórico acompanham o novo ID e respostas tardias não sobrescrevem a nova Carteira

#### Scenario: Detalhe indisponível
- **WHEN** o ID é malformado ou a Carteira não existe
- **THEN** o detalhe mostra erro ou não encontrado, não ativa silenciosamente outra Carteira e oferece retorno à listagem

### Requirement: Sincronização de mutações de Carteira
Criação e edição confirmadas SHALL incorporar o CarteiraResponse à coleção compartilhada; exclusão confirmada com 204 SHALL remover a Carteira. Cancelamento, erro e conflito MUST NOT alterar o contexto como se houvesse sucesso. A criação em dialog SHALL preservar seleção válida anterior; na ausência dela SHALL resolver o fallback. A criação direta SHALL selecionar a Carteira ao abrir seu detalhe. Edição SHALL atualizar o nome exibido sem trocar o ID ativo. Exclusão da ativa SHALL limpar sua preferência, resolver a primeira restante por id ASC ou estado vazio e preservar retorno a `/carteiras`.

#### Scenario: Criação e edição confirmadas
- **WHEN** POST ou PATCH retorna CarteiraResponse
- **THEN** coleção e nome no seletor são atualizados pela resposta sem GET redundante obrigatório, respeitando a seleção descrita

#### Scenario: Exclusão da ativa ou da última
- **WHEN** DELETE da Carteira ativa responde 204
- **THEN** a seleção deixa de referenciá-la, passa à primeira restante ou fica vazia, e nenhum dado financeiro anterior permanece como atual

#### Scenario: Exclusão de outra Carteira
- **WHEN** DELETE de Carteira não ativa responde 204
- **THEN** somente ela é removida da coleção, mantendo seleção e preferência válidas

#### Scenario: Mutação não confirmada
- **WHEN** a ação é cancelada ou o backend retorna erro, incluindo 409 por operações ou snapshots
- **THEN** a Carteira e a seleção permanecem preservadas e a mensagem normalizada é apresentada
