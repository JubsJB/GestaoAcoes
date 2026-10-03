## MODIFIED Requirements

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
- **THEN** o mesmo formulário discriminado do fluxo global é aberto com a Carteira pré-selecionada e não editável, aplicando sugestão inicial editável em COMPRA e sugestão editável em VENDA, enviando o preço final do campo nos dois tipos, com Corretora opcional, sempre sem ordem manual e com os mesmos payloads vigentes do POST

#### Scenario: Sucesso no cadastro contextual
- **WHEN** uma criação contextual é concluída
- **THEN** o histórico é atualizado com preço, ordem e total do DTO retornado sem GET redundante obrigatório nem cálculo financeiro

#### Scenario: Sem antecipação financeira
- **WHEN** o detalhe é exibido
- **THEN** somente posições abertas e histórico são apresentados como seções financeiras; resumo, patrimônio agregado e evolução não são adicionados

#### Scenario: Histórico visual consistente
- **WHEN** o histórico contextual está disponível
- **THEN** seus campos, ordem e estados permanecem equivalentes aos de Operações, com apenas uma representação acessível ativa
