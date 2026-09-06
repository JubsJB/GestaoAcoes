## MODIFIED Requirements

### Requirement: Listagem e estados da coleção
A página de listagem SHALL carregar `GET /carteiras` uma vez ao entrar, apresentar todas as Carteiras na ordem fornecida pelo backend e exibir nome e `dataCriacao` formatada somente na apresentação conforme o padrão temporal compartilhado. Ela SHALL diferenciar loading, coleção vazia, conteúdo e erro recuperável. A listagem SHALL manter cards, tornando-os compactos e hierárquicos, com nome em destaque, data como metadado e ação estável, sem acrescentar indicadores financeiros.

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

#### Scenario: Cards compactos
- **WHEN** Carteiras com nomes longos são exibidas
- **THEN** nome e data permanecem legíveis e a densidade melhora sem truncamento obrigatório nem métricas novas


### Requirement: Detalhe básico da Carteira
A aplicação SHALL apresentar em `/carteiras/{id}` nome, identificador e data de criação da Carteira, com ação textual de retorno para `/carteiras` e ações Editar e Excluir. O detalhe SHALL incorporar uma seção de histórico de Operações e uma ação “Registrar operação” fornecidas pela capability `frontend-operation-management`, sem renderizar posição, preço médio, resultados ou outros indicadores financeiros não solicitados. O detalhe SHALL separar contexto cadastral, ações e histórico por hierarquia visual. O histórico SHALL adotar o mesmo padrão semântico de tabela desktop e cards mobile de Operações, sem duplicar consultas nem alterar o formulário contextual.

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
- **THEN** nenhuma seção funcional de posições, preço médio, resultados, resumo, patrimônio ou evolução é apresentada

#### Scenario: Histórico visual consistente
- **WHEN** o histórico contextual está disponível
- **THEN** seus campos, ordem e estados permanecem equivalentes aos de Operações, com apenas uma representação acessível ativa


