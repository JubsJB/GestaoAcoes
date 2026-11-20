## Purpose

Apresentar histórico interno de custódia com transferências, atribuições e estornos, preservando vínculos, datas e situações de forma separada das negociações financeiras.

## ADDED Requirements

### Requirement: Histórico unificado e detalhe contextual
O sistema SHALL expor GET /carteiras/{carteiraId}/movimentacoes-custodia como array de registros de transferência, atribuição e estorno e GET /carteiras/{carteiraId}/movimentacoes-custodia/{id} como detalhe de uma original com estorno associado ou null. Histórico SHALL incluir todas as ações, inclusive posições encerradas, isolado por carteira; MUST NOT alterar GETs de operações financeiras. Cada linha SHALL identificar tipoRegistro, id, natureza, situacao, carteiraId, acaoId, ticker, mercado, corretoraOrigemId, corretoraDestinoId, quantidade, dataEfetiva, dataRegistro, posicaoCronologica, movimentacaoOriginalId, estornoId e motivo conforme design. Estorno SHALL usar origem/destino da original como dados neutralizados, não como nova direção inversa. Decimais SHALL permanecer exatos.

#### Scenario: Histórico auditável
- **WHEN** uma transferência e uma atribuição existem e uma delas foi estornada
- **THEN** coleção inclui três registros, original ESTORNADA e estorno com vínculo, natureza ESTORNO e situação null

#### Scenario: Posição encerrada
- **WHEN** quantidade consolidada da ação é zero
- **THEN** histórico de custódia continua disponível, embora distribuição pública de abertas omita a ação

#### Scenario: Carteira vazia e ausente
- **WHEN** carteira existe sem custódias ou não existe
- **THEN** retorna respectivamente 200 [] ou 404 padronizado

#### Scenario: Detalhe com estorno
- **WHEN** original estornada é consultada
- **THEN** retorna original completa e registro de estorno associado; não exige endpoint individual extra

#### Scenario: Separação financeira
- **WHEN** consumidor consulta histórico de operações
- **THEN** continua recebendo apenas COMPRA/VENDA, sem linhas de custódia ou datas de registro fabricadas

### Requirement: Filtros e ordenação de auditoria
A coleção SHALL ordenar por dataRegistro DESC, tipoRegistro na ordem MOVIMENTACAO antes de ESTORNO e id DESC como desempate, independente de collation. SHALL aceitar filtros opcionais natureza=TRANSFERENCIA|ATRIBUICAO|ESTORNO e situacao=ATIVA|ESTORNADA; valores desconhecidos SHALL retornar 400 REQUEST_INVALIDO. Situação SHALL filtrar originais; estornos SHALL aparecer quando natureza=ESTORNO mesmo com filtro de situação, ou quando não há filtro de situação e a natureza os inclui. Natureza específica TRANSFERENCIA/ATRIBUICAO SHALL excluir estornos. MVP SHALL retornar o conjunto completo abrangido pelos filtros, em coleção não paginada, sem truncamento no backend ou frontend e sem limite arbitrário de registros. Eventual paginação SHALL depender de evolução futura, sem implementação nesta change.

#### Scenario: Filtros de natureza e situação
- **WHEN** natureza TRANSFERENCIA e situação ATIVA são solicitadas
- **THEN** retorna somente originais de transferência ativas na ordem de registro

#### Scenario: Estornos e situação
- **WHEN** natureza ESTORNO e situação ATIVA são solicitadas
- **THEN** retorna estornos, pois situação não se aplica a essa natureza; cada situação permanece null

#### Scenario: Somente situação
- **WHEN** situacao ESTORNADA é solicitada sem natureza
- **THEN** retorna somente originais estornadas, sem linhas de estorno

#### Scenario: Sem filtros
- **WHEN** nenhum filtro é enviado
- **THEN** inclui originais ativas/estornadas e estornos, com desempate completo

#### Scenario: Filtro inválido
- **WHEN** natureza ou situação tem valor desconhecido ou caixa inválida
- **THEN** retorna 400 REQUEST_INVALIDO, sem coleção parcial

#### Scenario: Histórico representativo completo
- **WHEN** um histórico representativo contém originais ativas/estornadas, estornos e datas de registro empatadas, com ou sem filtros
- **THEN** retorna todas as identidades que satisfazem os filtros na ordem determinística, sem corte silencioso; cenários de verificação registram também evidências de desempenho sem impor máximo de registros ao produto

#### Scenario: Falha não equivale a histórico parcial
- **WHEN** ocorre falha técnica ao obter ou entregar a coleção completa
- **THEN** apresenta erro seguro em vez de retornar subconjunto como histórico completo, sem introduzir paginação ou limitação silenciosa

### Requirement: Consulta consistente e sem efeitos
Histórico e detalhe SHALL ser obtidos de snapshot de leitura consistente, com situação e vínculo coerentes, sem escrita, provider, snapshot patrimonial ou consulta por corretora/linha. A consulta SHALL explicitar controle interno sem comprovar custódia externa.

#### Scenario: Estorno concorrente com leitura
- **WHEN** um estorno confirma durante consulta
- **THEN** resposta representa um único estado coerente, sem original ativa acompanhada de estorno confirmado na mesma resposta

#### Scenario: Consulta repetida
- **WHEN** histórico ou detalhe é consultado novamente
- **THEN** não altera registros e quantidade de consultas não cresce por linha/corretora
