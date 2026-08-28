## Purpose

Disponibilizar a série temporal do patrimônio histórico de uma Carteira a partir dos snapshots imutáveis já persistidos, preservando moedas e instantes sem reconstrução retroativa.

## ADDED Requirements

### Requirement: Consultar a evolução patrimonial persistida
O sistema SHALL expor `GET /carteiras/{carteiraId}/evolucao-patrimonial`, sem body, filtros ou paginação, para consultar exclusivamente todos os snapshots já persistidos da Carteira. Carteira existente SHALL responder `200 OK`; Carteira inexistente SHALL responder `404 Not Found` pelo tratamento centralizado vigente. A consulta MUST NOT criar snapshot nem alterar dado algum, e esta primeira versão MUST NOT criar alias em `/carteiras/{carteiraId}/evolucao`.

#### Scenario: Carteira existente com histórico
- **WHEN** o cliente consulta a evolução de uma Carteira que possui snapshots
- **THEN** o sistema responde `200 OK` com a identidade da Carteira e sua série histórica persistida

#### Scenario: Carteira existente sem histórico
- **WHEN** o cliente consulta uma Carteira existente sem snapshots
- **THEN** o sistema responde `200 OK` com `carteiraId` e `pontos=[]`

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica uma Carteira persistida
- **THEN** o sistema responde `404 Not Found` no formato centralizado vigente

### Requirement: Representar cada snapshot como ponto temporal
A resposta SHALL conter exclusivamente `carteiraId` e `pontos`. Cada ponto SHALL conter `snapshotId`, `dataHoraSnapshot` e `patrimonios`; cada patrimônio SHALL conter exclusivamente `moeda` e `patrimonioAtual`. `snapshotId` SHALL representar a identidade persistida da observação e MUST NOT conferir capacidade pública de consultar individualmente, alterar ou excluir o snapshot.

#### Scenario: Snapshot com componentes monetários
- **WHEN** um snapshot possui componentes BRL e USD
- **THEN** um único ponto representa o snapshot e contém componentes independentes ordenados por `moeda ASC`

#### Scenario: Snapshot vazio
- **WHEN** um snapshot não possui componente monetário
- **THEN** ele permanece na série com sua identidade, `dataHoraSnapshot` e `patrimonios=[]`
- **AND** o sistema não fabrica BRL ou USD com valor zero

### Requirement: Preservar ordem e granularidade temporais
A série SHALL conter todos os snapshots selecionados em `dataHoraSnapshot ASC` e, como estabilizador defensivo, `snapshotId ASC`; dentro de cada ponto, `patrimonios` SHALL usar `moeda ASC`. O sistema MUST NOT agrupar, consolidar, deduplicar ou selecionar somente o último snapshot do dia. `dataHoraSnapshot` SHALL permanecer `OffsetDateTime` em UTC e ser serializado em ISO-8601.

#### Scenario: Múltiplos snapshots no mesmo dia
- **WHEN** a Carteira possui múltiplos snapshots em instantes diferentes no mesmo dia
- **THEN** todos aparecem como pontos distintos em sequência cronológica crescente

#### Scenario: Patrimônio igual em instantes diferentes
- **WHEN** dois snapshots possuem os mesmos componentes patrimoniais em timestamps diferentes
- **THEN** ambos permanecem na série como observações independentes

### Requirement: Manter patrimônios históricos por moeda sem métricas derivadas
A evolução SHALL devolver exatamente o `patrimonioAtual` persistido em cada componente, como `BigDecimal` com a representação suportada por `NUMERIC(38,12)`. BRL e USD MUST permanecer independentes. A consulta MUST NOT converter, combinar, recalcular, arredondar silenciosamente nem derivar variação absoluta, variação percentual, rentabilidade, custo, resultado realizado, resultado não realizado, TWR ou XIRR.

#### Scenario: Série multimoeda
- **WHEN** pontos históricos contêm componentes BRL e USD
- **THEN** cada componente mantém sua moeda e valor persistidos sem total multimoeda ou conversão

#### Scenario: Valor histórico persistido
- **WHEN** o patrimônio ou a cotação atual mudam depois da captura
- **THEN** a evolução continua devolvendo exatamente o valor do snapshot, sem recalculá-lo pelo estado atual

### Requirement: Consultar exclusivamente os snapshots locais
A evolução SHALL usar somente `SnapshotCarteira` e `SnapshotCarteiraMoeda` persistidos. Ela MUST NOT reproduzir Operações, consultar `HistoricoCotacao`, calcular posições, patrimônio ou resumo atuais, reconstruir caixa nem chamar BRAPI, Alpha Vantage ou qualquer provider externo.

#### Scenario: Providers indisponíveis
- **WHEN** qualquer provider externo está indisponível
- **THEN** a evolução permanece consultável a partir dos snapshots locais

#### Scenario: Operações ou cotações posteriores
- **WHEN** Operações ou cotações mudam depois de um snapshot
- **THEN** a série histórica não reexecuta nem reinterpreta esse snapshot

### Requirement: Recuperar a série em uma única consulta sem N+1
A consulta SHALL recuperar, em uma única ida ao banco, uma projeção plana contendo exclusivamente `snapshotId`, `dataHoraSnapshot`, `moeda` e `patrimonioAtual`, ancorada na Carteira consultada e usando semanticamente `LEFT JOIN` para snapshots e componentes. Zero linhas SHALL distinguir Carteira inexistente; uma linha com `snapshotId` ausente SHALL distinguir Carteira existente sem snapshots; snapshot presente com moeda ausente SHALL preservar pai sem filhos. A ordenação da consulta SHALL ser compatível com `dataHoraSnapshot ASC`, `snapshotId ASC`, `moeda ASC`. Ela MUST NOT carregar entidades completas sem necessidade, alterar associações JPA, tornar coleções eager, criar repository paralelo quando o repository de snapshot puder ser estendido, nem executar consulta adicional de existência, snapshot, moeda ou componente.

#### Scenario: Série com vários snapshots e moedas
- **WHEN** a Carteira possui vários snapshots, incluindo pontos vazios e pontos multimoeda
- **THEN** a resposta completa é montada a partir de uma única projeção plana, sem consulta adicional por ponto ou componente

### Requirement: Manter leitura histórica sem efeitos colaterais
A consulta SHALL executar com `@Transactional(readOnly = true)` e isolation padrão do banco/framework, mantendo a estratégia de uma única statement SQL. Ela MUST NOT adquirir lock pessimista, usar `@Version`, persistir estado, usar `Clock` ou bloquear a criação concorrente de snapshots. Um snapshot já confirmado antes da visão da statement SHALL poder integrar o resultado; um snapshot ainda não confirmado SHALL aparecer somente em consulta posterior. Qualquer necessidade futura de abandonar a query única MUST ser especificada antes de adotar outra política de isolamento.

#### Scenario: Criação concorrente de snapshot
- **WHEN** um novo snapshot é confirmado concorrentemente à consulta
- **THEN** a resposta representa uma visão válida da série antes ou depois dessa inserção, sem ponto parcial nem mistura entre pai e filhos

### Requirement: Limitar a primeira versão à série completa sem filtros ou paginação
Esta primeira versão SHALL retornar a série completa e MUST NOT aceitar filtros temporais, paginação, cursor, agrupamento ou limite configurável. O crescimento indefinido da série SHALL ser documentado como limitação. Uma change futura poderá avaliar filtros `OffsetDateTime` em UTC com limites explícitos e paginação por cursor temporal `(dataHoraSnapshot, snapshotId)`, sem incorporar esses contratos nesta capability.

#### Scenario: Consulta sem parâmetros
- **WHEN** o cliente consulta a evolução sem query parameters
- **THEN** o sistema devolve todos os snapshots da Carteira na ordem temporal aprovada

#### Scenario: Ausência de contratos antecipados
- **WHEN** a primeira versão é disponibilizada
- **THEN** nenhum formato de página, cursor ou semântica de intervalo temporal é incorporado implicitamente ao contrato

### Requirement: Representar fielmente a amostragem manual dos snapshots
A série SHALL refletir exclusivamente os snapshots efetivamente criados. Ela MAY ser vazia, irregular, esparsa, conter vários pontos no mesmo dia ou longos intervalos sem observação. O sistema MUST NOT interpolar, preencher lacunas, fabricar pontos ou criar snapshot durante a consulta.

#### Scenario: Intervalo sem snapshots
- **WHEN** existem dois snapshots separados por longo intervalo sem captura
- **THEN** a série contém somente os dois pontos persistidos e não fabrica observações intermediárias

#### Scenario: Série vazia
- **WHEN** a Carteira existe e nenhum snapshot foi criado
- **THEN** a resposta permanece `200 OK` com `pontos=[]` sem disparar criação automática

### Requirement: Preservar independência das capabilities atuais
A capability MUST NOT alterar criação ou imutabilidade de snapshots, patrimônio atual, resumo atual, posições, resultado realizado, histórico de cotações ou proteção de exclusão de Carteira. Ela MUST NOT adicionar criação automática, backfill, scheduler, job, endpoint público de snapshot individual, update ou delete de snapshot.

#### Scenario: Consulta da evolução
- **WHEN** a evolução é consultada
- **THEN** os endpoints e cálculos atuais preservam integralmente seus contratos e nenhuma automação é disparada
