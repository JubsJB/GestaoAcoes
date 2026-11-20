## MODIFIED Requirements

### Requirement: VENDA limitada pela posição cronologicamente disponível
Antes de persistir uma VENDA, o sistema SHALL reproduzir todas as operações da mesma carteira/ação incluindo candidata pela ordem financeira vigente e exigir saldo consolidado não negativo em todos os pontos. SHALL exigir também quantidade disponível suficiente antes da candidata no grupo da corretora informada ou no grupo sem corretora quando null/omitida, inclusive em carteiras sem custódias. Falha consolidada SHALL manter 409 POSICAO_INSUFICIENTE; falha local SHALL retornar 409 SALDO_CORRETORA_INSUFICIENTE. SHALL comparar efeitos posteriores pela regra diferencial e preservar custódias ativas. Operações legadas MUST NOT ser revalidadas como novas vendas locais. Nenhuma referência de outra carteira/ação SHALL fornecer saldo.

#### Scenario: Venda dentro da posição
- **WHEN** a quantidade vendida não torna negativo nenhum saldo cronológico
- **THEN** a VENDA é persistida com o preço unitário efetivamente informado

#### Scenario: Venda exatamente igual à posição
- **WHEN** a quantidade vendida é exatamente igual ao saldo disponível naquele ponto
- **THEN** a VENDA é persistida e o saldo derivado passa a zero

#### Scenario: Venda acima da posição
- **WHEN** a quantidade vendida excede o saldo disponível naquele ponto
- **THEN** o sistema responde `409 Conflict` com `POSICAO_INSUFICIENTE` e não persiste a VENDA

#### Scenario: Isolamento por Carteira e Ação
- **WHEN** existem compras da mesma Ação em outra Carteira ou de outra Ação na mesma Carteira
- **THEN** essas Operações não aumentam a posição disponível para a VENDA avaliada

#### Scenario: Consolidado suficiente e corretora insuficiente
- **WHEN** há compra A100 e nova venda B10
- **THEN** retorna 409 SALDO_CORRETORA_INSUFICIENTE, sem persistência

#### Scenario: Venda no grupo sem corretora
- **WHEN** compra A100 existe e nova venda omite corretora, sem saldo no grupo nulo
- **THEN** retorna 409 SALDO_CORRETORA_INSUFICIENTE, mesmo sem custódias

#### Scenario: Venda sem corretora com saldo próprio
- **WHEN** grupo nulo possui100 e candidata vende40 sem agravar sufixo
- **THEN** aceita, preservando corretora nula e consolidado

#### Scenario: Legado independente inalterado
- **WHEN** histórico contém déficit antigo em B e candidata em A tem saldo suficiente sem piorar B
- **THEN** aceita sem exigir regularização global

#### Scenario: Venda que invalida transferência
- **WHEN** compra A100 precede T1 A→B80 e candidata retroativa vende A30 antes de T1
- **THEN** retorna 409 MOVIMENTACAO_CUSTODIA_INVALIDADA e preserva T1, pois consolidado e saldo local passam e invalidação de custódia precede o déficit por grupo que T1 produziria

### Requirement: Inserção retroativa preserva toda a sequência
O sistema SHALL aceitar operação retroativa somente se replay financeiro consolidado permanecer válido e comparação diferencial quantitativa não criar, agravar ou prolongar déficit por grupo nem invalidar custódia ativa. SHALL alinhar união dos pontos por identidade discriminada/posição, transportando saldo sem evento; deficitDepois SHALL ser <= deficitAntes em cada ponto. Nova operação na mesma data SHALL receber MAX(ordemNoDia)+1, sem inserir entre ordens financeiras, reordenação manual ou horaOperacao. Custódias existentes MUST NOT ser reposicionadas: âncora após antiga última operação permanece antes da nova operação, inclusive em dia antes sem operações. Compras positivas SHALL poder reduzir déficits legados; fórmulas financeiras permanecem vigentes.

#### Scenario: Operação retroativa no mesmo dia
- **WHEN** a data retroativa já possui Operações para a combinação
- **THEN** a candidata recebe a próxima ordem e é reproduzida depois das Operações já existentes naquele dia

#### Scenario: Compra retroativa compatível
- **WHEN** uma COMPRA retroativa com preço informado válido é anexada ao fim de sua data e todo o replay permanece válido
- **THEN** o sistema persiste a COMPRA sem alterar Operações posteriores

#### Scenario: Venda retroativa que invalida venda posterior
- **WHEN** a candidata torna negativo o saldo em seu ponto ou em qualquer Operação posterior
- **THEN** o sistema responde `409 POSICAO_INSUFICIENTE` e preserva o histórico existente

#### Scenario: Compra retroativa reduz déficit legado
- **WHEN** nova compra válida aumenta saldo do grupo antes de venda legada deficitária
- **THEN** aceita quando todos os pontos melhoram ou permanecem iguais, sem editar venda antiga

#### Scenario: Compra não reposiciona custódia
- **WHEN** compra é adicionada após a última ordem na data de custódia existente
- **THEN** custódia conserva sua posição anterior à nova compra

#### Scenario: Comparação por identidade
- **WHEN** inserção muda índices da lista e um déficit posterior é agravado
- **THEN** rejeita pelo evento correspondente, não por comparação de índices

#### Scenario: Prolongamento indevido
- **WHEN** alteração mantém o pior saldo mas um ponto antes regularizado permanece negativo
- **THEN** retorna 409 CONFLITO_HISTORICO_QUANTITATIVO

### Requirement: Atomicidade e consistência concorrente
COMPRA e VENDA MUST NOT consultar providers no POST. Lock pessimista por carteira SHALL preceder confirmação de referências, geração de ordem, leitura financeira/custódia/estornos, replay antes/depois e persistência com flush. SHALL compartilhar proteção com registro/estorno de custódia, mantendo ordem consistente de locks e sem bloqueio global de carteiras. Escrita SHALL ser atômica, garantir ordem única e validar saldo consolidado, saldo local da nova venda, comparação diferencial e custódias ativas. MUST NOT validar candidatas por consulta pública em transação independente. Constraint cronológica SHALL ser última defesa; conflitos transitórios SHALL ser seguros, sem retry automático de escrita.

#### Scenario: Operações concorrentes financeiramente válidas
- **WHEN** duas Operações financeiramente válidas concorrem para a mesma Carteira, Ação e data sem consulta externa no cadastro
- **THEN** ambas são persistidas, exatamente duas Operações existem, suas ordens formam o conjunto `{1, 2}`, não há duplicidade e o replay final permanece válido

#### Scenario: Vendas concorrentes excedem a posição em conjunto
- **WHEN** duas VENDAS seriam válidas isoladamente, mas inválidas em conjunto
- **THEN** somente as Operações compatíveis com o saldo são persistidas e nenhum prefixo do replay fica negativo

#### Scenario: Falha durante a criação
- **WHEN** geração de ordem, replay, integridade ou persistência falha
- **THEN** nenhuma Operação parcial é persistida e registros relacionados permanecem inalterados

#### Scenario: Venda e transferência concorrentes
- **WHEN** requisições disputam saldo da mesma carteira
- **THEN** a segunda valida histórico após a primeira confirmada, sem consumir saldo duplicado

### Requirement: Compatibilidade das funcionalidades existentes
A evolução SHALL preservar entidade financeira, colunas NOT NULL, precisão, constraint cronológica, fórmulas, posição, realizado, consultas financeiras, snapshots e patrimônio. Novos POSTs SHALL receber a validação quantitativa ampliada, sem alterar dados legados, recalcular preços, renumerar ordens ou preencher corretoras. Custódia SHALL possuir schema aditivo separado; changeSet 004 MUST NOT ser editado. Enum financeiro SHALL permanecer COMPRA/VENDA. Diagnósticos da distribuição evoluem coordenadamente nesta change, sem contaminar schemas de OperacaoResponse/PosicaoResponse.

#### Scenario: Regressão das APIs existentes
- **WHEN** APIs de consulta, posição, resultado, patrimônio e snapshot são executadas após a evolução
- **THEN** preservam seus contratos e cálculos vigentes sobre os valores persistidos

#### Scenario: Inicialização com Liquibase e Hibernate
- **WHEN** Liquibase executa e Hibernate valida o schema
- **THEN** a aplicação inicia com o changeSet aditivo de custódia e mapeamentos financeiros inalterados

#### Scenario: Dados anteriores
- **WHEN** existem Operações persistidas antes da evolução
- **THEN** elas permanecem válidas e inalteradas

#### Scenario: Enum financeiro preservado
- **WHEN** request financeiro informa TRANSFERENCIA, ATRIBUICAO ou ESTORNO
- **THEN** continua retornando 400 REQUEST_INVALIDO
