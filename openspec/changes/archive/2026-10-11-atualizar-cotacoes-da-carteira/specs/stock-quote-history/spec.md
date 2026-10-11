## MODIFIED Requirements

### Requirement: Manter modelo histórico mínimo e unidirecional
HistoricoCotacao SHALL conter id, acao, cotacao, dataHoraCotacao legado e os novos referenciaTipo, referenciaInstante, referenciaData, cotacaoObtidaEm. Associação SHALL permanecer obrigatória, unidirecional ManyToOne LAZY sem coleção em Acao ou cascade delete. MUST NOT duplicar ticker, empresa, moeda, provider, payload, OHLC ou tentativas. Os metadados SHALL corresponder à informação de mercado aceita.

#### Scenario: Persistência do modelo mínimo
- **WHEN** uma observação é persistida
- **THEN** contém identidade, Ação, preço e metadados temporais aprovados, sem dados cadastrais duplicados

#### Scenario: Navegação e ciclo de vida independentes
- **WHEN** a associação entre histórico e Ação é utilizada
- **THEN** a navegação parte somente de `HistoricoCotacao` para `Acao` com carregamento lazy
- **AND** nenhuma coleção é adicionada à Ação nem a exclusão da Ação é propagada em cascata ao histórico

### Requirement: Preservar contrato físico do histórico no schema
Liquibase SHALL preservar tabela, PK, FK obrigatória sem cascade delete, cotacao NUMERIC(19,6) positiva e data_hora_cotacao legado NOT NULL. Um novo changeSet aditivo SHALL adicionar tipo, instante de mercado nullable, DATE nullable e obtenção nullable para legado; MUST NOT editar 005 nem changeSets anteriores. CHECKs SHALL garantir coerência de tipo e campos. A unicidade antiga baseada no legado SHALL ser substituída pela unicidade de mercado; PostgreSQL e H2 SHALL executar o mesmo changelog com ddl-auto=validate.

#### Scenario: Inicialização do schema
- **WHEN** Liquibase executa em PostgreSQL ou H2
- **THEN** 005 permanece intacto e novo changeSet adiciona modelo temporal e constraints antes da validação Hibernate

#### Scenario: Integridade física da observação
- **WHEN** uma gravação viola obrigatoriedade, positividade, precisão, chave estrangeira ou unicidade temporal por Ação
- **THEN** o banco rejeita a gravação sem criar observação parcial

### Requirement: Registrar cada estado de cotação efetivamente persistido
cotacaoObtidaEm SHALL representar o instante em que a observação atualmente armazenada foi obtida validamente, não o maior instante histórico de consulta. Sua monotonicidade SHALL ser local à mesma referência/preço. Baseline inicial ou referência posterior aceita SHALL substituir a observação integralmente com preço, tipo, referência e obtenção própria da candidata, inclusive quando a obtenção for anterior à da observação substituída; o histórico SHALL corresponder a essa observação aceita. MUST NOT combinar obtenções de observações diferentes usando max nem usar obtenção para ordenar referências de mercado ou desempatar DATA × INSTANTE.
Cadastro válido SHALL registrar exatamente uma observação inicial. Atualização com baseline confiável inicial ou referência posterior aceita SHALL criar exatamente uma observação. Consultas com mesma referência, referência anterior, reuso, cooldown ou falha MUST NOT criar observação. HistoricoCotacao MUST NOT representar série retroativa completa ou tentativas. Nova consulta real válida da mesma referência/preço SHALL avançar somente cotacaoObtidaEm na Ação quando posterior (ou preencher obtenção nula), sem criar nem modificar observação histórica; obtenção igual/anterior, reuso e cooldown MUST NOT avançar esse campo.

#### Scenario: Primeira cotação no cadastro
- **WHEN** uma Ação é cadastrada com cotação válida
- **THEN** o estado inicial da Ação e a primeira observação histórica são persistidos juntos

#### Scenario: Atualização temporalmente posterior
- **WHEN** PATCH individual ou lote aceita referência posterior do mesmo tipo ou estabelece baseline inicial
- **THEN** persiste estado e uma observação correspondente

#### Scenario: Candidata igual ou anterior
- **WHEN** a candidata possui referência igual, anterior, inválida ou incompatível
- **THEN** não cria observação e preserva estado conforme classificação compartilhada

### Requirement: Preservar granularidade intraday e semântica temporal vigente
O histórico SHALL respeitar INSTANTE, DATA e DESCONHECIDA sem exigir granularidade intraday artificial. Obtenção SHALL ser separada da referência de mercado. Legado SHALL permanecer desconhecido quando não houver evidência. Horários ou datas MUST NOT ser fabricados. O título histórico deste requisito é preservado apenas por identidade nominal.

#### Scenario: Timestamp do provider
- **WHEN** uma cotação com INSTANTE é aceita
- **THEN** preserva instante de mercado UTC e obtenção separada

#### Scenario: Fallback temporal
- **WHEN** o provider fornece DATA ou não há evidência temporal em um registro legado
- **THEN** preserva DATA sem hora ou DESCONHECIDA, sem transformar obtenção em referência de mercado

#### Scenario: Mesmo preço em instantes diferentes
- **WHEN** o mesmo preço é aceito em referências posteriores distintas do mesmo tipo
- **THEN** cada referência aceita gera observação, mas repetição de consulta não gera

### Requirement: Impedir duplicidade temporal por Ação
O histórico SHALL permitir no máximo uma observação conhecida por (acao_id, referenciaInstante) e por (acao_id, referenciaData), com CHECKs de tipo. Registros legados DESCONHECIDA SHALL preservar identidade por id sem deduplicação artificial. Valores nulos SHALL permitir legado em PostgreSQL e H2; a constraint antiga (acao_id, data_hora_cotacao) SHALL deixar de ser autoridade. Novo índice MUST ser justificado por acesso/uniquidade, sem índice duplicado.

#### Scenario: Mesma Ação e timestamp
- **WHEN** uma tentativa repete a mesma referência conhecida da Ação
- **THEN** não cria segunda observação, inclusive DATA

#### Scenario: Ações diferentes no mesmo timestamp
- **WHEN** Ações distintas recebem mesma DATA ou INSTANTE
- **THEN** cada Ação pode ter sua própria observação

### Requirement: Manter estado atual e histórico atomicamente consistentes
A persistência do estado atual e da observação correspondente SHALL ser uma única operação atômica. Falha em qualquer gravação MUST deixar ambos inalterados, e atualizações concorrentes MUST preservar no histórico somente estados temporalmente aplicáveis.

#### Scenario: Falha ao registrar histórico no cadastro
- **WHEN** a observação inicial não pode ser persistida
- **THEN** o cadastro inteiro falha e nenhuma Ação parcial permanece

#### Scenario: Falha ao registrar histórico no PATCH
- **WHEN** a observação posterior não pode ser persistida
- **THEN** a atualização de `Acao.cotacaoAtual` e `dataHoraCotacao` também é revertida

#### Scenario: Atualizações concorrentes
- **WHEN** duas candidatas com referência confiável do mesmo tipo concorrem
- **THEN** o lock serializa comparação com estado relido e aceita somente referência inicial ou posterior; histórico não registra rejeitadas
