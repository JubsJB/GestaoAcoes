## Purpose

Preservar as cotações válidas efetivamente aceitas e persistidas pelo sistema como estado atual de cada Ação, sem confundir esse registro incremental com uma série histórica completa de mercado ou alterar a semântica financeira das Operações e indicadores atuais.

## ADDED Requirements

### Requirement: Manter modelo histórico mínimo e unidirecional
Cada observação persistida SHALL ser representada por `HistoricoCotacao` contendo exclusivamente `id`, `acao`, `cotacao` e `dataHoraCotacao`. A associação com `Acao` SHALL ser obrigatória, unidirecional, `ManyToOne` e carregada de forma `LAZY`, sem coleção de históricos em `Acao` e sem exclusão em cascata. O histórico MUST NOT duplicar ticker, nome da empresa, mercado, moeda, provider, origem, payload ou timestamp adicional de persistência.

#### Scenario: Persistência do modelo mínimo
- **WHEN** uma observação histórica é persistida
- **THEN** ela contém somente seu identificador, a referência obrigatória à Ação, a cotação e `dataHoraCotacao`

#### Scenario: Navegação e ciclo de vida independentes
- **WHEN** a associação entre histórico e Ação é utilizada
- **THEN** a navegação parte somente de `HistoricoCotacao` para `Acao` com carregamento lazy
- **AND** nenhuma coleção é adicionada à Ação nem a exclusão da Ação é propagada em cascata ao histórico

### Requirement: Preservar contrato físico do histórico no schema
O schema controlado pelo Liquibase SHALL conter `historico_cotacao` com chave primária, `acao_id` obrigatório como chave estrangeira sem cascade delete, `cotacao NUMERIC(19,6) NOT NULL`, `data_hora_cotacao NOT NULL`, `CHECK (cotacao > 0)` e `UNIQUE (acao_id, data_hora_cotacao)`. A evolução SHALL permanecer no changeSet `005-create-historico-cotacao.yaml`, sem alteração dos changeSets 001–004, e o Hibernate SHALL continuar validando o schema por `ddl-auto=validate`.

#### Scenario: Inicialização do schema
- **WHEN** o Liquibase aplica os changeSets da aplicação
- **THEN** o changeSet 005 cria `historico_cotacao` com a chave, os tipos e as restrições aprovadas

#### Scenario: Integridade física da observação
- **WHEN** uma gravação viola obrigatoriedade, positividade, precisão, chave estrangeira ou unicidade temporal por Ação
- **THEN** o banco rejeita a gravação sem criar observação parcial

### Requirement: Registrar cada estado de cotação efetivamente persistido
Toda cotação válida que for efetivamente aceita e persistida como estado atual de uma Ação SHALL possuir exatamente uma observação correspondente no histórico. Esse histórico SHALL representar somente os estados aceitos pelo sistema a partir da integração desta capability e MUST NOT ser apresentado como série completa de mercado fornecida retroativamente por BRAPI ou Alpha Vantage. Uma candidata com timestamp igual ou anterior ao estado atual MUST NOT criar observação, pois não altera o estado persistido.

#### Scenario: Primeira cotação no cadastro
- **WHEN** uma Ação é cadastrada com cotação válida
- **THEN** o estado inicial da Ação e a primeira observação histórica são persistidos juntos

#### Scenario: Atualização temporalmente posterior
- **WHEN** o PATCH obtém cotação válida com timestamp posterior ao estado persistido
- **THEN** a Ação é atualizada e uma nova observação histórica correspondente é registrada

#### Scenario: Candidata igual ou anterior
- **WHEN** o PATCH obtém cotação válida com timestamp igual ou anterior ao estado persistido
- **THEN** o estado atual é devolvido sem nova observação histórica

### Requirement: Preservar granularidade intraday e semântica temporal vigente
Cada observação SHALL identificar a Ação, a cotação e `dataHoraCotacao` em granularidade de timestamp. O timestamp SHALL ser o timestamp utilizável da cotação fornecido pelo provider, normalizado para UTC, ou, quando ausente ou inutilizável, o instante UTC em que a aplicação obteve a cotação.

#### Scenario: Timestamp do provider
- **WHEN** o provider fornece timestamp utilizável associado à cotação aceita
- **THEN** a observação preserva o mesmo instante normalizado para UTC

#### Scenario: Fallback temporal
- **WHEN** o provider não fornece timestamp utilizável
- **THEN** a observação preserva o instante UTC em que a aplicação obteve a cotação

#### Scenario: Mesmo preço em instantes diferentes
- **WHEN** o mesmo valor de cotação é aceito em timestamps posteriores distintos
- **THEN** cada instante produz uma observação histórica distinta

### Requirement: Aplicar a mesma política numérica da cotação atual
A cotação histórica SHALL usar `BigDecimal`, ser positiva e exatamente representável com precisão total 19 e escala 6, sem `float`, `double`, truncamento ou arredondamento.

#### Scenario: Cotação representável
- **WHEN** uma cotação positiva pode ser representada exatamente em `NUMERIC(19,6)`
- **THEN** o histórico preserva exatamente o mesmo valor aceito como `Acao.cotacaoAtual`

#### Scenario: Cotação fora da precisão
- **WHEN** a candidata não pode ser representada na política vigente
- **THEN** o erro atual de cotação fora da precisão é preservado e nenhum estado ou histórico parcial é gravado

### Requirement: Manter estado atual e histórico atomicamente consistentes
A persistência do estado atual e da observação correspondente SHALL ser uma única operação atômica. Falha em qualquer gravação MUST deixar ambos inalterados, e atualizações concorrentes MUST preservar no histórico somente estados temporalmente aplicáveis.

#### Scenario: Falha ao registrar histórico no cadastro
- **WHEN** a observação inicial não pode ser persistida
- **THEN** o cadastro inteiro falha e nenhuma Ação parcial permanece

#### Scenario: Falha ao registrar histórico no PATCH
- **WHEN** a observação posterior não pode ser persistida
- **THEN** a atualização de `Acao.cotacaoAtual` e `dataHoraCotacao` também é revertida

#### Scenario: Atualizações concorrentes
- **WHEN** duas candidatas válidas da mesma Ação possuem timestamps diferentes
- **THEN** a seção transacional serializada aplica somente cada candidata posterior ao estado encontrado sob coordenação
- **AND** o estado final e a observação mais recente representam o maior timestamp aceito

### Requirement: Impedir duplicidade temporal por Ação
O sistema SHALL permitir no máximo uma observação por combinação de Ação e `dataHoraCotacao`, preservando várias observações da mesma cotação em instantes diferentes.

#### Scenario: Mesma Ação e timestamp
- **WHEN** uma tentativa repetida representa a mesma Ação e o mesmo timestamp
- **THEN** nenhuma segunda observação é criada

#### Scenario: Ações diferentes no mesmo timestamp
- **WHEN** Ações diferentes possuem cotações no mesmo instante
- **THEN** cada Ação pode possuir sua própria observação

### Requirement: Manter histórico desacoplado de Operações e consultas atuais
O histórico de cotação SHALL representar somente referência de mercado e MUST NOT substituir `Operacao.precoUnitario`, participar de preço médio, custo ou resultado realizado, nem adicionar consultas a histórico em posição, patrimônio ou resumo atuais.

#### Scenario: Operação com preço diferente
- **WHEN** `Operacao.precoUnitario` difere da cotação histórica
- **THEN** o preço informado permanece a única fonte financeira da Operação

#### Scenario: Consulta da posição atual
- **WHEN** posição, patrimônio ou resumo atuais são consultados
- **THEN** eles continuam usando `Acao.cotacaoAtual` sem query adicional ao histórico, provider ou N+1

### Requirement: Não expor consulta pública nesta primeira fatia
Esta capability SHALL somente persistir o fundamento temporal para consumidores futuros e MUST NOT introduzir endpoint REST de histórico, paginação pública, filtros, backfill, reconstrução retroativa, consulta de anos anteriores, preenchimento de pregões ausentes, scheduler, job, cron ou coleta automática diária.

#### Scenario: Consulta das APIs atuais
- **WHEN** um cliente utiliza os endpoints existentes de Ação ou Carteira
- **THEN** nenhum novo campo ou contrato de histórico é exposto

#### Scenario: Ausência de histórico externo retroativo
- **WHEN** a capability entra em operação
- **THEN** ela começa a registrar apenas novas cotações aceitas pelo fluxo atual
- **AND** não consulta endpoints históricos dos providers nem fabrica observações anteriores ou ausentes
