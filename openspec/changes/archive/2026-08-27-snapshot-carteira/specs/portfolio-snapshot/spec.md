## Purpose

Preservar fotografias patrimoniais imutáveis do estado atual conhecido de uma Carteira, separadas por moeda e aptas a fundamentar uma futura evolução patrimonial sem reconstrução retroativa.

## ADDED Requirements

### Requirement: Criar snapshot patrimonial atual explicitamente
O sistema SHALL expor `POST /carteiras/{carteiraId}/snapshots`, sem body, para capturar exclusivamente o estado atual conhecido e já persistido da Carteira. Uma criação concluída SHALL responder `201 Created`, devolver o snapshot criado e incluir `Location` para sua identidade canônica. Carteira inexistente SHALL responder `404 Not Found` pelo tratamento centralizado vigente.

#### Scenario: Criação concluída
- **WHEN** o cliente solicita um snapshot de uma Carteira existente
- **THEN** o sistema responde `201 Created` com `id`, `carteiraId`, `dataHoraSnapshot` e os patrimônios capturados por moeda
- **AND** inclui `Location: /carteiras/{carteiraId}/snapshots/{snapshotId}`

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica uma Carteira persistida
- **THEN** o sistema responde `404 Not Found` e não persiste snapshot ou componente monetário

### Requirement: Persistir somente patrimônio consolidado por moeda
Cada snapshot SHALL possuir identidade própria, a Carteira e `dataHoraSnapshot`, além de zero ou mais componentes contendo exclusivamente moeda e `patrimonioAtual`. Para cada moeda presente, `patrimonioAtual` SHALL ser exatamente a soma oficial de `valorAtualPosicao` das posições abertas daquela moeda. O snapshot MUST NOT persistir posições individuais, custo, resultado não realizado, rentabilidade, quantidade de posições, resultado realizado ou total multimoeda.

#### Scenario: Snapshot BRL e USD
- **WHEN** a Carteira possui posições abertas em BRL e USD
- **THEN** o snapshot contém um componente BRL e um componente USD, ordenados por moeda
- **AND** nenhum valor soma, converte ou calcula média entre moedas

#### Scenario: Múltiplas posições da mesma moeda
- **WHEN** a Carteira possui várias posições abertas na mesma moeda
- **THEN** o componente persiste a soma oficial dos respectivos `valorAtualPosicao`, sem média ou segundo cálculo de patrimônio

#### Scenario: Moeda ausente
- **WHEN** a Carteira não possui posição aberta em determinada moeda
- **THEN** nenhum componente artificial de valor zero é criado para essa moeda

### Requirement: Representar Carteira sem posições por snapshot vazio
Uma Carteira existente sem posições abertas SHALL produzir um snapshot válido com identidade e `dataHoraSnapshot`, mas sem componentes monetários. O sistema MUST NOT rejeitar a captura, fabricar BRL ou USD nem omitir a observação temporal.

#### Scenario: Carteira vazia
- **WHEN** uma Carteira existente não possui posições abertas no instante capturado
- **THEN** o sistema persiste o snapshot e responde com `patrimonios=[]`

#### Scenario: Somente posições encerradas
- **WHEN** todas as posições da Carteira foram zeradas antes da captura
- **THEN** o snapshot não inclui os ciclos encerrados nem o resultado realizado histórico

### Requirement: Preservar a fonte financeira oficial
A criação SHALL obter as posições abertas uma única vez pelo replay oficial e agregar os valores uma única vez pela política monetária compartilhada. Ela MUST NOT executar segundo replay, segundo cálculo de posição, segundo agregador, nova query por posição, provider externo ou consulta ao histórico de cotação. Para o mesmo estado consistente, cada valor persistido SHALL ser exatamente igual ao `patrimonioAtual` retornado por `GET /carteiras/{carteiraId}/patrimonio` para a mesma moeda.

#### Scenario: Equivalência com patrimônio atual
- **WHEN** snapshot e consulta de patrimônio observam o mesmo estado consistente da Carteira
- **THEN** os patrimônios por moeda são matematicamente idênticos

#### Scenario: Providers indisponíveis
- **WHEN** BRAPI, Alpha Vantage ou outro provider está indisponível
- **THEN** a criação usa somente Operações e cotações atuais já persistidas e permanece independente de chamadas externas

#### Scenario: Ausência de N+1
- **WHEN** a Carteira possui várias posições e moedas
- **THEN** a criação preserva o fetch plan vigente sem consulta histórica ou consulta adicional por posição, Ação, moeda ou indicador

### Requirement: Distinguir instante do snapshot das referências de cotação
`dataHoraSnapshot` SHALL representar o instante UTC em que a fotografia patrimonial atual foi capturada, obtido uma única vez de relógio testável e representado por `OffsetDateTime`. Esse instante MUST NOT substituir, exigir igualdade ou ser derivado de `Acao.dataHoraCotacao`; posições diferentes MAY utilizar cotações persistidas em instantes diferentes.

#### Scenario: Cotações em instantes distintos
- **WHEN** as posições capturadas utilizam cotações atuais com timestamps diferentes
- **THEN** o snapshot é válido e possui uma única `dataHoraSnapshot` independente dessas referências

#### Scenario: Múltiplos snapshots no mesmo dia
- **WHEN** duas capturas ocorrem em timestamps diferentes no mesmo dia
- **THEN** ambas são observações históricas distintas, mesmo com patrimônio numericamente igual

### Requirement: Preservar unicidade temporal sem deduplicar conteúdo
O sistema SHALL permitir no máximo um snapshot por combinação de Carteira e `dataHoraSnapshot`. Solicitações da mesma Carteira que colidam no mesmo timestamp SHALL produzir `409 Conflict` com código específico, sem resposta ou persistência parcial. Patrimônio igual em timestamps distintos MUST NOT ser deduplicado.

#### Scenario: Mesmo timestamp para a mesma Carteira
- **WHEN** já existe snapshot da Carteira no mesmo `dataHoraSnapshot`
- **THEN** a nova criação responde `409 Conflict` com `SNAPSHOT_CARTEIRA_DUPLICADO`

#### Scenario: Mesmo timestamp para Carteiras diferentes
- **WHEN** Carteiras diferentes são capturadas no mesmo instante
- **THEN** cada Carteira pode possuir seu próprio snapshot

#### Scenario: Mesmo conteúdo em instantes diferentes
- **WHEN** duas capturas da mesma Carteira possuem patrimônios iguais e timestamps distintos
- **THEN** ambas são persistidas como snapshots distintos

### Requirement: Manter snapshot append-only e imutável
Depois de persistido, um snapshot MUST NOT ser recalculado ou alterado automaticamente por nova Operação, atualização de cotação, encerramento de posição ou mudança do patrimônio atual. Esta capability MUST NOT expor criação para data passada, atualização ou exclusão pública de snapshot.

#### Scenario: Operação posterior
- **WHEN** uma Operação altera a posição depois da criação do snapshot
- **THEN** o snapshot anterior permanece exatamente como persistido

#### Scenario: Cotação posterior
- **WHEN** uma nova cotação altera o patrimônio atual depois da criação do snapshot
- **THEN** o snapshot anterior permanece exatamente como persistido

### Requirement: Persistir fotografia completa atomicamente
A leitura do estado atual, a consolidação e a persistência do snapshot SHALL ocorrer sob uma única visão transacional `REPEATABLE_READ`. O snapshot pai e todos os componentes monetários SHALL ser confirmados ou revertidos como uma única unidade. A criação MUST NOT usar lock pessimista adicional nem permitir snapshot parcialmente persistido.

#### Scenario: Falha em um componente monetário
- **WHEN** qualquer componente não pode ser persistido
- **THEN** o snapshot pai e todos os componentes da tentativa são revertidos

#### Scenario: Operação concorrente
- **WHEN** uma Operação é registrada concorrentemente com a criação
- **THEN** o snapshot representa integralmente o estado consistente anterior ou posterior à Operação, sem misturar ambos

#### Scenario: Atualização de cotação concorrente
- **WHEN** uma cotação é atualizada concorrentemente com a criação
- **THEN** o snapshot representa integralmente uma visão consistente das cotações persistidas, sem estado parcial da atualização

### Requirement: Preservar relacionamentos obrigatórios sem exclusão em cascata
O snapshot SHALL referenciar obrigatoriamente sua Carteira e cada componente monetário SHALL referenciar obrigatoriamente seu snapshot pai. Os relacionamentos SHALL ser unidirecionais e carregados de forma lazy. O sistema MUST NOT adicionar coleção de snapshots à Carteira, MUST NOT adicionar coleção de componentes ao snapshot e MUST NOT configurar cascade JPA ou `ON DELETE CASCADE`; pai e filhos SHALL ser persistidos explicitamente na mesma transação.

#### Scenario: Persistência explícita e atômica
- **WHEN** um snapshot com componentes monetários é criado
- **THEN** pai e filhos são gravados explicitamente dentro da mesma transação, sem depender de cascade JPA

#### Scenario: Tentativa de excluir a Carteira de origem
- **WHEN** a Carteira possui ao menos um snapshot
- **THEN** a proteção de exclusão recusa a operação e nenhuma FK histórica é removida por cascade

### Requirement: Aplicar política numérica do patrimônio atual
Cada `patrimonioAtual` SHALL usar exclusivamente `BigDecimal`, soma exata sem `MathContext` ou arredondamento intermediário, escala final 12 com `RoundingMode.UNNECESSARY` e precisão máxima 38. Falha de representação SHALL reusar `422 Unprocessable Entity / CALCULO_POSICAO_FORA_DA_PRECISAO` e reverter integralmente a criação.

#### Scenario: Patrimônio representável
- **WHEN** cada acumulado por moeda pode ser representado em escala 12 e precisão máxima 38
- **THEN** o snapshot preserva exatamente os mesmos valores normalizados da capability de patrimônio atual

#### Scenario: Patrimônio fora da precisão
- **WHEN** qualquer acumulado não pode ser representado pela política aprovada
- **THEN** a criação responde `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` sem snapshot parcial

### Requirement: Não expor leitura ou evolução patrimonial nesta change
Esta capability SHALL limitar a API pública à criação explícita e MUST NOT expor listagem ou consulta individual de snapshots, evolução patrimonial, gráficos, agrupamento diário ou mensal, snapshot retroativo, backfill, scheduler, job ou geração automática.

#### Scenario: Contratos públicos disponíveis
- **WHEN** a capability é disponibilizada
- **THEN** nenhum `GET`, `PATCH` ou `DELETE` de snapshot é criado
- **AND** os contratos atuais de posição, patrimônio, resumo, resultado realizado e histórico de cotação permanecem inalterados
