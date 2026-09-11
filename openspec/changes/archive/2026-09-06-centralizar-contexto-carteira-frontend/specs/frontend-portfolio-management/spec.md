## MODIFIED Requirements

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

### Requirement: Detalhe básico da Carteira
A aplicação SHALL apresentar em `/carteiras/{id}` nome, identificador e data de criação da Carteira, com ação textual de retorno para `/carteiras` e ações Editar e Excluir. O detalhe SHALL incorporar uma seção de histórico de Operações e uma ação “Registrar operação” fornecidas pela capability `frontend-operation-management`, incorporando posições abertas pelo contrato existente e sem acrescentar indicadores não solicitados.

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

## ADDED Requirements

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
