## Purpose

Permitir neutralização retroativa, única e auditável de transferências e atribuições sem apagar os registros originais ou comprometer eventos posteriores ativos.

## ADDED Requirements

### Requirement: Estorno próprio e vinculado
O sistema SHALL expor POST /carteiras/{carteiraId}/movimentacoes-custodia/{id}/estornos e aceitar somente motivo opcional, string ou null, com até 1000 caracteres; corpo vazio ou {} SHALL significar ausência de motivo. SHALL criar registro próprio e preservar original, posição e dados quantitativos. Quantidade, origem, destino, data efetiva, IDs ou campos desconhecidos no request MUST NOT ser aceitos. Estorno SHALL devolver 201 e Location do detalhe original que contém o estorno criado; nenhuma rota individual adicional será exigida no MVP. Identidade original fora da carteira SHALL retornar 404. Situação original SHALL passar de ATIVA para ESTORNADA por vínculo, e estorno SHALL possuir natureza ESTORNO e dataRegistro própria.

#### Scenario: Estorno válido de transferência
- **WHEN** movimentação A→B40 ativa não tem dependência impeditiva e request tem motivo opcional
- **THEN** cria estorno vinculado, mantém original e devolve 201 com detalhe e identificação do estorno

#### Scenario: Estorno válido de atribuição
- **WHEN** atribuição do grupo nulo para A está ativa e a neutralização é válida
- **THEN** neutraliza a atribuição desde sua posição sem alterar a compra original

#### Scenario: Campos proibidos no estorno
- **WHEN** request tenta mudar quantidade, origem, destino ou data efetiva
- **THEN** retorna 400 REQUEST_INVALIDO, sem estorno ou modificação

#### Scenario: Estorno inexistente ou fora do contexto
- **WHEN** ID original não existe na carteira solicitada
- **THEN** retorna 404 sem expor registro de outra carteira

### Requirement: Neutralização na posição original
Estorno SHALL neutralizar o débito/crédito original desde sua data e posição efetivas, sem constituir transferência inversa na data de registro. Replay efetivo SHALL tratar a original como efeito zero; auditoria SHALL manter ambos os registros e distinguir dataRegistro do estorno e dataEfetiva herdada. O ponto da original SHALL continuar disponível para comparação diferencial mesmo sem efeito. O sistema MUST NOT reposicionar eventos ou criar substituta automaticamente.

#### Scenario: Neutralização retroativa
- **WHEN** transferência efetiva no dia1 é estornada no dia20
- **THEN** replay atual omite seus efeitos desde dia1, enquanto auditoria exibe registros e seus instantes distintos

#### Scenario: Sem transferência inversa
- **WHEN** atribuição é estornada
- **THEN** não cria direção corretora→grupo nulo como nova movimentação e não aplica débito na data de registro

#### Scenario: Novo lançamento independente
- **WHEN** estorno é confirmado e eventual substituição posterior falha
- **THEN** estorno permanece confirmado e nenhuma substituição é criada ou revertida automaticamente

### Requirement: Proteção do histórico posterior no estorno
Estorno SHALL comparar antes/depois pela mesma regra diferencial de registro e validar origens de todas as custódias que permaneçam ativas. Déficits novos, agravados ou prolongados SHALL bloquear; déficits legados independentes inalterados MUST NOT bloquear. Neutralização que invalida custódia ativa SHALL falhar com identificação do evento. MUST NOT realizar cascata automática.

#### Scenario: Estorno cria déficit
- **WHEN** A→B40 financiou venda posterior B30 e não existe outra cobertura
- **THEN** rejeita neutralização porque o novo replay cria déficit em B

#### Scenario: Estorno invalida outra custódia
- **WHEN** T1 A→B40 financiou T2 B→C30 ativa
- **THEN** retorna 409 MOVIMENTACAO_CUSTODIA_INVALIDADA, sem estornar T1/T2

#### Scenario: Déficit legado permanece igual
- **WHEN** grupo C tem déficit antigo idêntico antes/depois e estorno A/B é válido
- **THEN** aceita sem exigir saneamento de C

#### Scenario: Déficit legado agravado
- **WHEN** remover crédito da original aumenta déficit posterior existente
- **THEN** rejeita pelo primeiro ponto conflitante da ordem determinística

### Requirement: Unicidade e transação de estorno
Estorno SHALL ser único por movimentação original, garantido no serviço e por restrição de integridade; original e vínculo SHALL ser consultados após lock da carteira. Escrita SHALL ser atômica, compartilhar lock de operações/custódias, não repetir automaticamente e não apagar auditoria.

#### Scenario: Estorno duplicado
- **WHEN** original já possui estorno
- **THEN** retorna 409 ESTORNO_DUPLICADO e mantém exatamente um registro

#### Scenario: Dois estornos simultâneos
- **WHEN** duas requisições estornam a mesma original
- **THEN** uma confirma e outra retorna ESTORNO_DUPLICADO; integridade impede duplicação mesmo em falha de coordenação

#### Scenario: Rollback integral
- **WHEN** persistência de estorno falha
- **THEN** original permanece ativa, sem registro/vínculo parcial ou alteração quantitativa

#### Scenario: Invariância financeira do estorno
- **WHEN** estorno válido é confirmado com operações e cotações iguais
- **THEN** quantidade consolidada e todos os resultados financeiros permanecem iguais
