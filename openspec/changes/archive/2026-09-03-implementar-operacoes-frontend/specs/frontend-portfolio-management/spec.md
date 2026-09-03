## MODIFIED Requirements

### Requirement: Contrato frontend mínimo de Carteiras
A área de Carteiras SHALL consumir `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}`, `PATCH /carteiras/{id}` e `DELETE /carteiras/{id}` pela configuração central da API. Para integrar o histórico contextual aprovado, o detalhe SHALL também consumir `GET /carteiras/{carteiraId}/operacoes` por meio da capability `frontend-operation-management`. O frontend SHALL representar `id`, `nome` e `dataCriacao` do `CarteiraResponse`, SHALL enviar somente `nome` nos requests de criação e edição e MUST NOT introduzir campos, endpoints ou dados derivados.

#### Scenario: Leitura do DTO básico
- **WHEN** o backend devolve uma Carteira
- **THEN** a aplicação preserva `id`, `nome` e `dataCriacao` sem acrescentar indicadores financeiros

#### Scenario: Requests mínimos
- **WHEN** o usuário cria ou edita uma Carteira
- **THEN** o frontend envia um corpo contendo exatamente `nome`

#### Scenario: Limite funcional
- **WHEN** a capability é apresentada
- **THEN** ela consulta e exibe somente os dados básicos da Carteira e o histórico de Operações aprovado, sem antecipar posições, resultados, patrimônio, resumo, snapshots, evolução, gráficos, moedas ou conversão cambial

### Requirement: Detalhe básico da Carteira
A aplicação SHALL apresentar em `/carteiras/{id}` nome, identificador e data de criação da Carteira, com ação textual de retorno para `/carteiras` e ações Editar e Excluir. O detalhe SHALL incorporar uma seção de histórico de Operações e uma ação “Registrar operação” fornecidas pela capability `frontend-operation-management`, sem renderizar posição, preço médio, resultados ou outros indicadores financeiros não solicitados.

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
