## MODIFIED Requirements

### Requirement: Listagem cronológica e somente leitura
A listagem global SHALL consultar `GET /operacoes` uma vez ao entrar e apresentar a ordem recebida do backend, garantida por `dataOperacao`, `ordemNoDia` e `id` ascendentes. Ela SHALL exibir tipo, ativo, mercado, data, ordem, quantidade, preço, valor total e Corretora, diferenciar loading, vazio, conteúdo e erro recuperável e MUST NOT recalcular ou reordenar o histórico. A listagem SHALL usar tabela semântica desktop e cards completos mobile conforme frontend-visual-experience, com números alinhados à direita, moeda explícita, tipo textual e ações em posição estável. O histórico contextual SHALL seguir esse mesmo padrão sem reordenar dados ou adicionar consultas. As rotas globais SHALL permanecer por compatibilidade/deep link, sem filtro implícito por Carteira e com acesso direto a `/operacoes` pelo menu principal; a entrada principal de cadastro permanece contextual à Carteira.

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

#### Scenario: Entrada pelo menu principal
- **WHEN** o usuário aciona Operações no menu com uma Carteira selecionada
- **THEN** acessa `/operacoes` com histórico de todas as Carteiras, preservando a consulta global existente sem inserir filtro ou origem contextual implicitamente

#### Scenario: Troca do seletor no histórico global
- **WHEN** o usuário troca a Carteira no seletor estando em `/operacoes`
- **THEN** o histórico continua global sem filtragem local ou requisição de histórico por Carteira introduzida por esta evolução

