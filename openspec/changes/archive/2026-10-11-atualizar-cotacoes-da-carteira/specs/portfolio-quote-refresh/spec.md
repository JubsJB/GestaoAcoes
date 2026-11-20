## Purpose

Permitir a atualização manual das cotações das posições abertas de uma Carteira, com seleção autoritativa pelo backend, processamento com sucesso parcial e preservação dos contratos financeiros e dos dados históricos.

## ADDED Requirements

### Requirement: Lote manual orientado à Carteira
O sistema SHALL expor POST /carteiras/{carteiraId}/atualizacoes-cotacoes sem request body, validar Carteira e determinar autoritativamente posições abertas pelo replay oficial. Requisição sem body SHALL ser válida. Qualquer conteúdo funcional, inclusive {}, arrays, listas de IDs, objetos com campos e demais conteúdos, SHALL ser rejeitado com 400 REQUEST_INVALIDO antes de processar o lote, sem provider, aplicação transacional ou processamento de Ações. MUST NOT criar DTO de request vazio. Conteúdo exclusivamente whitespace SHALL seguir o comportamento normal da infraestrutura HTTP para ausência de conteúdo, sem regra de negócio específica baseada nos bytes e sem permitir body funcional. SHALL capturar conjunto inicial, excluir quantidades zeradas e ativos apenas cadastrados, deduplicar por acaoId e ordenar por id. MUST NOT manter lock da Carteira durante rede ou introduzir replay financeiro divergente.

#### Scenario: Requisição sem body
- **WHEN** uma requisição válida não envia body
- **THEN** o backend determina as Ações exclusivamente pelo replay oficial das posições abertas

#### Scenario: Body rejeitado antes do lote
- **WHEN** a requisição envia {}, array, lista de IDs, objeto com campos ou outro conteúdo funcional
- **THEN** retorna 400 REQUEST_INVALIDO antes do lote, sem provider, aplicação transacional ou processamento de Ações

#### Scenario: Seleção autoritativa
- **WHEN** a Carteira tem várias operações de AAPL aberta, PETR4 zerada e outra Ação apenas cadastrada
- **THEN** processa AAPL uma vez e não consulta as demais

#### Scenario: Carteira vazia
- **WHEN** Carteira existente não tem posições abertas
- **THEN** retorna 200 com itens vazios e contadores zero, sem providers ou gravação

#### Scenario: Carteira inexistente
- **WHEN** o id não existe
- **THEN** retorna 404 padronizado antes de provider

#### Scenario: Histórico inconsistente
- **WHEN** o replay oficial impede determinar as posições
- **THEN** falha globalmente antes do processamento de ativos

#### Scenario: Conjunto capturado
- **WHEN** posição muda após captura
- **THEN** mantém conjunto inicial sem lock de Carteira na rede

### Requirement: Processamento sequencial com sucesso parcial
O lote SHALL processar sincronamente e sequencialmente usando a mesma regra individual, provider fora da transação e uma transação curta independente por Ação sob lock. Orquestrador MUST NOT envolver lote em transação única. Falha isolada SHALL ser coletada após rollback daquele ativo e MUST NOT desfazer sucessos anteriores. MUST NOT introduzir fila, job, scheduler, paralelização ou retry.

#### Scenario: Falha isolada
- **WHEN** AAPL atualiza e outro ativo falha na rede ou persistência
- **THEN** AAPL permanece confirmada e o lote continua com FALHA no ativo afetado

#### Scenario: Lote versus individual
- **WHEN** individual e lote disputam mesma Ação
- **THEN** compartilham lock/regra e não regridem referência

#### Scenario: Lote versus lote
- **WHEN** dois lotes incluem a mesma Ação
- **THEN** referência é comparada ao estado relido e resultado não duplica histórico

#### Scenario: Resposta antiga atrasada
- **WHEN** referência anterior chega depois de referência nova confirmada
- **THEN** preserva nova e classifica anterior SEM_NOVA_COTACAO

### Requirement: Resultado por ativo e HTTP proporcional
Lote processado SHALL retornar 200 com carteiraId, resumo {atualizadas, semNovaCotacao, falhas} e resultados contendo acaoId, ticker, mercado, classificação, motivo e estado de cotação resultante com referência/obtenção; falhas SHALL manter código/mensagem públicos apropriados sem dados sensíveis. Contadores SHALL somar exatamente itens. Reuso/cooldown de sucesso SHALL ser SEM_NOVA_COTACAO; cooldown de falha SHALL ser FALHA. Somente aceitação e persistência de baseline inicial ou referência posterior por esta execução SHALL contar ATUALIZADA. Avanço isolado de cotacaoObtidaEm em nova consulta válida de referência/preço iguais SHALL continuar SEM_NOVA_COTACAO, sem histórico; reuso/cooldown MUST NOT avançar obtenção ou alterar preço/referência. Falha global MUST NOT alegar rollback de sucessos já confirmados.

#### Scenario: Resultados mistos
- **WHEN** o lote tem atualização, ausência de novidade e falha
- **THEN** retorna 200 com três categorias, motivos e contadores exatos

#### Scenario: Reuso não duplica sucesso
- **WHEN** solicitação reutiliza sucesso de outra execução
- **THEN** classifica SEM_NOVA_COTACAO indicando reuso/cooldown sem nova contagem

#### Scenario: Erro global tardio
- **WHEN** processamento é interrompido após sucesso confirmado
- **THEN** erro público não promete reversão daqueles sucessos

#### Scenario: Snapshot da tentativa em cooldown de sucesso
- **GIVEN** cooldown de sucesso ativo e outra instância atualizou a Ação após a tentativa concluída
- **WHEN** o lote processa essa Ação
- **THEN** retorna SEM_NOVA_COTACAO com COOLDOWN_SUCESSO e o mesmo snapshot imutável daquela tentativa, sem garantir o estado persistido mais recente
- **AND** não executa releitura adicional apenas para atualizar a apresentação, provider, candidata, aplicação, obtenção ou histórico

#### Scenario: Conexão encerrada
- **WHEN** cliente deixa de receber resposta
- **THEN** servidor não depende de cancelamento UI e nenhum retry automático é prescrito

### Requirement: Preservar cotação global e finanças
A cotação SHALL pertencer à Ação e ser utilizada por outras Carteiras na próxima leitura. Lote MUST NOT alterar operações, preços negociados, quantidade, custo histórico, preço médio, resultado realizado, replay, histórico de operações ou snapshots anteriores. Somente avaliações dependentes de cotação e snapshots futuros gerados manualmente pelo fluxo vigente SHALL refletir novo preço. MUST NOT consultar prévia para determinar preço de operação ou criar snapshot ao atualizar.

#### Scenario: Outra Carteira
- **WHEN** duas Carteiras possuem AAPL e uma atualiza sua cotação
- **THEN** ambas usam estado global na próxima leitura sem misturar suas operações

#### Scenario: Finanças históricas
- **WHEN** cotação muda
- **THEN** valor atual e não realizado podem mudar, mas contabilidade e snapshots anteriores permanecem
