## MODIFIED Requirements

### Requirement: Listagem cronológica e somente leitura
A listagem global SHALL consultar `GET /operacoes` uma vez ao entrar e apresentar a ordem recebida do backend, garantida por `dataOperacao`, `ordemNoDia` e `id` ascendentes. Ela SHALL exibir tipo, ativo, mercado, data, ordem, quantidade, preço, valor total e Corretora, diferenciar loading, vazio, conteúdo e erro recuperável e MUST NOT recalcular ou reordenar o histórico. A listagem SHALL usar tabela semântica desktop e cards completos mobile conforme frontend-visual-experience, com números alinhados à direita, moeda explícita, tipo textual e ações em posição estável. O histórico contextual SHALL seguir esse mesmo padrão sem reordenar dados ou adicionar consultas.

#### Scenario: Histórico retornado
- **WHEN** a consulta devolve compras e vendas
- **THEN** a página exibe preço, ordem e total retornados na ordem recebida

#### Scenario: Estados da coleção
- **WHEN** a consulta está pendente, retorna vazia ou falha
- **THEN** a página apresenta respectivamente loading, estado vazio ou erro com retry manual explícito

#### Scenario: Sem mutações inexistentes
- **WHEN** uma Operação é apresentada
- **THEN** a interface não oferece edição nem exclusão e não realiza PUT, PATCH ou DELETE de Operações

#### Scenario: Comparação cronológica
- **WHEN** o histórico é exibido em desktop ou mobile
- **THEN** todos os campos e a ordem recebida permanecem disponíveis e somente uma representação participa da acessibilidade e do teclado


### Requirement: Detalhe fiel
O detalhe SHALL apresentar os dados relevantes do `OperacaoResponse`, incluindo preço e total autoritativos formatados, mostrar “Sem corretora” quando aplicável, permitir retorno ao contexto de origem e MUST NOT exibir `ordemNoDia`, cotação, posição, preço médio, resultados, edição ou exclusão. `ordemNoDia` SHALL permanecer no contrato e continuar disponível para ordenação interna. O detalhe SHALL organizar contexto, movimentação, quantidade, preço, total, data e Corretora em grupos legíveis, mantendo identificadores quando forem os únicos dados disponíveis e sem consultas para convertê-los em nomes.

#### Scenario: Estado transitório ou reload
- **WHEN** há response transitório compatível ou a rota é recarregada
- **THEN** o detalhe usa o DTO compatível sem GET redundante ou consulta `GET /operacoes/{id}` no reload

#### Scenario: Operação inexistente
- **WHEN** a consulta responde `404`
- **THEN** a página apresenta estado de não encontrado e caminho à listagem

#### Scenario: Detalhe sem enriquecimento
- **WHEN** um identificador não possui nome no DTO disponível
- **THEN** o identificador continua compreensível no contexto sem nova chamada HTTP


### Requirement: Experiência acessível e responsiva
A feature SHALL reutilizar feedback, toast e padrões visuais existentes, preservar foco e navegação por teclado e manter lista, formulário, detalhe e dialog legíveis em viewport compacto sem depender somente de cor. O formulário SHALL agrupar visualmente contexto, tipo/movimentação, quantidade/preço/data, Corretora, estimativa existente e ações. A reorganização MUST preservar compra/venda, preço informativo somente leitura em COMPRA, sugestão editável em VENDA, estimativa, Carteira fixa no contexto, Corretora opcional, strings decimais, data civil, máscaras, validações, payloads e gatilhos HTTP.

#### Scenario: Uso assistivo ou compacto
- **WHEN** a feature é usada por teclado, tecnologia assistiva ou tela compacta
- **THEN** campos condicionais, mensagens, ações, foco e valores permanecem compreensíveis e operáveis

#### Scenario: Formulário agrupado
- **WHEN** uma operação é preparada em página ou dialog
- **THEN** grupos e ajudas facilitam leitura sem adicionar campos ou alterar condições de edição, bloqueio, submissão ou cancelamento


