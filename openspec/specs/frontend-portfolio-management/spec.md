# frontend-portfolio-management Specification

## Purpose
Disponibilizar o gerenciamento frontend básico e acessível de Carteiras sobre os contratos REST existentes, sem antecipar operações ou informações financeiras.

## Requirements

### Requirement: Contrato frontend mínimo de Carteiras
A área de Carteiras SHALL consumir exclusivamente `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}`, `PATCH /carteiras/{id}` e `DELETE /carteiras/{id}` pela configuração central da API. O frontend SHALL representar `id`, `nome` e `dataCriacao` do `CarteiraResponse`, SHALL enviar somente `nome` nos requests de criação e edição e MUST NOT introduzir campos, endpoints ou dados derivados.

#### Scenario: Leitura do DTO básico
- **WHEN** o backend devolve uma Carteira
- **THEN** a aplicação preserva `id`, `nome` e `dataCriacao` sem acrescentar indicadores financeiros

#### Scenario: Requests mínimos
- **WHEN** o usuário cria ou edita uma Carteira
- **THEN** o frontend envia um corpo contendo exatamente `nome`

#### Scenario: Limite funcional
- **WHEN** a capability é apresentada
- **THEN** ela não consulta nem exibe Operações, posições, resultados, patrimônio, resumo, snapshots, evolução, gráficos, moedas ou conversão cambial

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
A página de listagem SHALL carregar `GET /carteiras` uma vez ao entrar, apresentar todas as Carteiras na ordem fornecida pelo backend e exibir nome e `dataCriacao` formatada somente na apresentação conforme o padrão temporal compartilhado. Ela SHALL diferenciar loading, coleção vazia, conteúdo e erro recuperável.

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
A aplicação SHALL apresentar em `/carteiras/{id}` somente nome, identificador e data de criação da Carteira, com ação textual de retorno para `/carteiras` e ações Editar e Excluir. A composição MAY reservar hierarquia e espaçamento para evolução futura, mas MUST NOT renderizar cards, placeholders ou valores que simulem informações financeiras ainda fora do escopo.

#### Scenario: Detalhe com estado transitório
- **WHEN** a navegação fornece `CarteiraResponse` compatível com o ID da rota
- **THEN** o detalhe usa o DTO sem GET redundante

#### Scenario: Detalhe sem estado transitório
- **WHEN** a rota é aberta diretamente ou recarregada
- **THEN** o detalhe consulta `GET /carteiras/{id}` e apresenta loading enquanto aguarda

#### Scenario: Carteira inexistente
- **WHEN** a consulta individual responde `404`
- **THEN** a página apresenta estado de não encontrado e caminho acessível para voltar à listagem

#### Scenario: Sem antecipação financeira
- **WHEN** o detalhe básico é exibido
- **THEN** nenhuma seção funcional de Operações, posições, resultados, resumo ou evolução é apresentada

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
