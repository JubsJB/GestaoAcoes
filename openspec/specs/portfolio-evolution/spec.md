# portfolio-evolution Specification

## Purpose

Disponibilizar a sÃ©rie temporal do patrimÃ´nio histÃ³rico de uma Carteira a partir dos snapshots imutÃ¡veis jÃ¡ persistidos, preservando moedas e instantes sem reconstruÃ§Ã£o retroativa.

## Requirements

### Requirement: Consultar a evoluÃ§Ã£o patrimonial persistida
O sistema SHALL expor `GET /carteiras/{carteiraId}/evolucao-patrimonial`, sem body, filtros ou paginaÃ§Ã£o, para consultar exclusivamente todos os snapshots jÃ¡ persistidos da Carteira. Carteira existente SHALL responder `200 OK`; Carteira inexistente SHALL responder `404 Not Found` pelo tratamento centralizado vigente. A consulta MUST NOT criar snapshot nem alterar dado algum, e esta primeira versÃ£o MUST NOT criar alias em `/carteiras/{carteiraId}/evolucao`.

#### Scenario: Carteira existente com histÃ³rico
- **WHEN** o cliente consulta a evoluÃ§Ã£o de uma Carteira que possui snapshots
- **THEN** o sistema responde `200 OK` com a identidade da Carteira e sua sÃ©rie histÃ³rica persistida

#### Scenario: Carteira existente sem histÃ³rico
- **WHEN** o cliente consulta uma Carteira existente sem snapshots
- **THEN** o sistema responde `200 OK` com `carteiraId` e `pontos=[]`

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` nÃ£o identifica uma Carteira persistida
- **THEN** o sistema responde `404 Not Found` no formato centralizado vigente

### Requirement: Representar cada snapshot como ponto temporal
A resposta SHALL conter exclusivamente `carteiraId` e `pontos`. Cada ponto SHALL conter `snapshotId`, `dataHoraSnapshot` e `patrimonios`; cada patrimÃ´nio SHALL conter exclusivamente `moeda` e `patrimonioAtual`. `snapshotId` SHALL representar a identidade persistida da observaÃ§Ã£o e MUST NOT conferir capacidade pÃºblica de consultar individualmente, alterar ou excluir o snapshot.

#### Scenario: Snapshot com componentes monetÃ¡rios
- **WHEN** um snapshot possui componentes BRL e USD
- **THEN** um Ãºnico ponto representa o snapshot e contÃ©m componentes independentes ordenados por `moeda ASC`

#### Scenario: Snapshot vazio
- **WHEN** um snapshot nÃ£o possui componente monetÃ¡rio
- **THEN** ele permanece na sÃ©rie com sua identidade, `dataHoraSnapshot` e `patrimonios=[]`
- **AND** o sistema nÃ£o fabrica BRL ou USD com valor zero

### Requirement: Preservar ordem e granularidade temporais
A sÃ©rie SHALL conter todos os snapshots selecionados em `dataHoraSnapshot ASC` e, como estabilizador defensivo, `snapshotId ASC`; dentro de cada ponto, `patrimonios` SHALL usar `moeda ASC`. O sistema MUST NOT agrupar, consolidar, deduplicar ou selecionar somente o Ãºltimo snapshot do dia. `dataHoraSnapshot` SHALL permanecer `OffsetDateTime` em UTC e ser serializado em ISO-8601.

#### Scenario: MÃºltiplos snapshots no mesmo dia
- **WHEN** a Carteira possui mÃºltiplos snapshots em instantes diferentes no mesmo dia
- **THEN** todos aparecem como pontos distintos em sequÃªncia cronolÃ³gica crescente

#### Scenario: PatrimÃ´nio igual em instantes diferentes
- **WHEN** dois snapshots possuem os mesmos componentes patrimoniais em timestamps diferentes
- **THEN** ambos permanecem na sÃ©rie como observaÃ§Ãµes independentes

### Requirement: Manter patrimÃ´nios histÃ³ricos por moeda sem mÃ©tricas derivadas
A evoluÃ§Ã£o SHALL devolver exatamente o `patrimonioAtual` persistido em cada componente, como `BigDecimal` com a representaÃ§Ã£o suportada por `NUMERIC(38,12)`. BRL e USD MUST permanecer independentes. A consulta MUST NOT converter, combinar, recalcular, arredondar silenciosamente nem derivar variaÃ§Ã£o absoluta, variaÃ§Ã£o percentual, rentabilidade, custo, resultado realizado, resultado nÃ£o realizado, TWR ou XIRR.

#### Scenario: SÃ©rie multimoeda
- **WHEN** pontos histÃ³ricos contÃªm componentes BRL e USD
- **THEN** cada componente mantÃ©m sua moeda e valor persistidos sem total multimoeda ou conversÃ£o

#### Scenario: Valor histÃ³rico persistido
- **WHEN** o patrimÃ´nio ou a cotaÃ§Ã£o atual mudam depois da captura
- **THEN** a evoluÃ§Ã£o continua devolvendo exatamente o valor do snapshot, sem recalculÃ¡-lo pelo estado atual

### Requirement: Consultar exclusivamente os snapshots locais
A evoluÃ§Ã£o SHALL usar somente `SnapshotCarteira` e `SnapshotCarteiraMoeda` persistidos. Ela MUST NOT reproduzir OperaÃ§Ãµes, consultar `HistoricoCotacao`, calcular posiÃ§Ãµes, patrimÃ´nio ou resumo atuais, reconstruir caixa nem chamar BRAPI, Alpha Vantage ou qualquer provider externo.

#### Scenario: Providers indisponÃ­veis
- **WHEN** qualquer provider externo estÃ¡ indisponÃ­vel
- **THEN** a evoluÃ§Ã£o permanece consultÃ¡vel a partir dos snapshots locais

#### Scenario: OperaÃ§Ãµes ou cotaÃ§Ãµes posteriores
- **WHEN** OperaÃ§Ãµes ou cotaÃ§Ãµes mudam depois de um snapshot
- **THEN** a sÃ©rie histÃ³rica nÃ£o reexecuta nem reinterpreta esse snapshot

### Requirement: Recuperar a sÃ©rie em uma Ãºnica consulta sem N+1
A consulta SHALL recuperar, em uma Ãºnica ida ao banco, uma projeÃ§Ã£o plana contendo exclusivamente `snapshotId`, `dataHoraSnapshot`, `moeda` e `patrimonioAtual`, ancorada na Carteira consultada e usando semanticamente `LEFT JOIN` para snapshots e componentes. Zero linhas SHALL distinguir Carteira inexistente; uma linha com `snapshotId` ausente SHALL distinguir Carteira existente sem snapshots; snapshot presente com moeda ausente SHALL preservar pai sem filhos. A ordenaÃ§Ã£o da consulta SHALL ser compatÃ­vel com `dataHoraSnapshot ASC`, `snapshotId ASC`, `moeda ASC`. Ela MUST NOT carregar entidades completas sem necessidade, alterar associaÃ§Ãµes JPA, tornar coleÃ§Ãµes eager, criar repository paralelo quando o repository de snapshot puder ser estendido, nem executar consulta adicional de existÃªncia, snapshot, moeda ou componente.

#### Scenario: SÃ©rie com vÃ¡rios snapshots e moedas
- **WHEN** a Carteira possui vÃ¡rios snapshots, incluindo pontos vazios e pontos multimoeda
- **THEN** a resposta completa Ã© montada a partir de uma Ãºnica projeÃ§Ã£o plana, sem consulta adicional por ponto ou componente

### Requirement: Manter leitura histÃ³rica sem efeitos colaterais
A consulta SHALL executar com `@Transactional(readOnly = true)` e isolation padrÃ£o do banco/framework, mantendo a estratÃ©gia de uma Ãºnica statement SQL. Ela MUST NOT adquirir lock pessimista, usar `@Version`, persistir estado, usar `Clock` ou bloquear a criaÃ§Ã£o concorrente de snapshots. Um snapshot jÃ¡ confirmado antes da visÃ£o da statement SHALL poder integrar o resultado; um snapshot ainda nÃ£o confirmado SHALL aparecer somente em consulta posterior. Qualquer necessidade futura de abandonar a query Ãºnica MUST ser especificada antes de adotar outra polÃ­tica de isolamento.

#### Scenario: CriaÃ§Ã£o concorrente de snapshot
- **WHEN** um novo snapshot Ã© confirmado concorrentemente Ã  consulta
- **THEN** a resposta representa uma visÃ£o vÃ¡lida da sÃ©rie antes ou depois dessa inserÃ§Ã£o, sem ponto parcial nem mistura entre pai e filhos

### Requirement: Limitar a primeira versÃ£o Ã  sÃ©rie completa sem filtros ou paginaÃ§Ã£o
Esta primeira versÃ£o SHALL retornar a sÃ©rie completa e MUST NOT aceitar filtros temporais, paginaÃ§Ã£o, cursor, agrupamento ou limite configurÃ¡vel. O crescimento indefinido da sÃ©rie SHALL ser documentado como limitaÃ§Ã£o. Uma change futura poderÃ¡ avaliar filtros `OffsetDateTime` em UTC com limites explÃ­citos e paginaÃ§Ã£o por cursor temporal `(dataHoraSnapshot, snapshotId)`, sem incorporar esses contratos nesta capability.

#### Scenario: Consulta sem parÃ¢metros
- **WHEN** o cliente consulta a evoluÃ§Ã£o sem query parameters
- **THEN** o sistema devolve todos os snapshots da Carteira na ordem temporal aprovada

#### Scenario: AusÃªncia de contratos antecipados
- **WHEN** a primeira versÃ£o Ã© disponibilizada
- **THEN** nenhum formato de pÃ¡gina, cursor ou semÃ¢ntica de intervalo temporal Ã© incorporado implicitamente ao contrato

### Requirement: Representar fielmente a amostragem manual dos snapshots
A sÃ©rie SHALL refletir exclusivamente os snapshots efetivamente criados. Ela MAY ser vazia, irregular, esparsa, conter vÃ¡rios pontos no mesmo dia ou longos intervalos sem observaÃ§Ã£o. O sistema MUST NOT interpolar, preencher lacunas, fabricar pontos ou criar snapshot durante a consulta.

#### Scenario: Intervalo sem snapshots
- **WHEN** existem dois snapshots separados por longo intervalo sem captura
- **THEN** a sÃ©rie contÃ©m somente os dois pontos persistidos e nÃ£o fabrica observaÃ§Ãµes intermediÃ¡rias

#### Scenario: SÃ©rie vazia
- **WHEN** a Carteira existe e nenhum snapshot foi criado
- **THEN** a resposta permanece `200 OK` com `pontos=[]` sem disparar criaÃ§Ã£o automÃ¡tica

### Requirement: Preservar independÃªncia das capabilities atuais
A capability MUST NOT alterar criaÃ§Ã£o ou imutabilidade de snapshots, patrimÃ´nio atual, resumo atual, posiÃ§Ãµes, resultado realizado, histÃ³rico de cotaÃ§Ãµes ou proteÃ§Ã£o de exclusÃ£o de Carteira. Ela MUST NOT adicionar criaÃ§Ã£o automÃ¡tica, backfill, scheduler, job, endpoint pÃºblico de snapshot individual, update ou delete de snapshot.

#### Scenario: Consulta da evoluÃ§Ã£o
- **WHEN** a evoluÃ§Ã£o Ã© consultada
- **THEN** os endpoints e cÃ¡lculos atuais preservam integralmente seus contratos e nenhuma automaÃ§Ã£o Ã© disparada

