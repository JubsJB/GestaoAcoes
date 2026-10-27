## Purpose

Permitir mudanças internas e auditáveis da distribuição quantitativa de ações entre corretoras, preservando saldos históricos e a posição financeira consolidada.

## ADDED Requirements

### Requirement: Cadastro discriminado de movimentação de custódia
O sistema SHALL expor POST /carteiras/{carteiraId}/movimentacoes-custodia. TRANSFERENCIA SHALL exigir ticker, mercado, natureza, corretoraOrigemId, corretoraDestinoId, quantidade e dataEfetiva; origem/destino SHALL existir e ser distintos. ATRIBUICAO SHALL aceitar os mesmos campos com origem omitida ou null, exclusivamente do grupo sem corretora para destino cadastrado. Carteira e ação SHALL existir; ticker SHALL seguir normalização vigente. Destino ausente, origem cadastrada na atribuição, natureza desconhecida, campos financeiros e controlados pelo servidor MUST NOT ser aceitos. Não haverá carteira de destino, criação de referências nem consulta externa. Criação válida SHALL retornar 201, Location individual e DTO persistido; referências inexistentes SHALL retornar 404.

#### Scenario: Transferência válida
- **WHEN** há compra A100 e é solicitada TRANSFERENCIA A→B40 válida
- **THEN** registra um evento, distribui A60/B40, retorna 201 e não modifica a compra

#### Scenario: Atribuição válida
- **WHEN** há compra sem corretora 100 e é solicitada ATRIBUICAO para A40
- **THEN** debita 40 do grupo nulo e credita A40 sem alterar corretora da compra nem criar corretora fictícia

#### Scenario: Direções e campos proibidos
- **WHEN** destino é null, origem igual ao destino, ATRIBUICAO tem origem cadastrada ou request informa preço, total, posição, ID, situação, registro ou outra carteira
- **THEN** retorna 400 REQUEST_INVALIDO e não persiste

#### Scenario: Referência inexistente ou de outra carteira
- **WHEN** carteira/ação/corretora não existe ou o contexto tenta acessar identidade de outra carteira
- **THEN** retorna 404 sem movimentação, cadastro de referência ou vazamento de outra carteira

### Requirement: Quantidade exata e datas distintas
Quantidade SHALL ser positiva e exatamente representável em NUMERIC(19,6), com até 13 dígitos inteiros; BRASIL SHALL exigir inteiro matemático e EUA SHALL aceitar até seis casas. O sistema MUST NOT arredondar ou truncar excedentes. Data efetiva SHALL ser LocalDate, passada ou corrente conforme America/Sao_Paulo para BRASIL e America/New_York para EUA, com relógio testável. Data futura SHALL produzir 400 DATA_EFETIVA_FUTURA. Data de registro SHALL ser instante do servidor, independente da efetividade; MUST NOT fabricar data de registro de operações legadas.

#### Scenario: Limites de quantidade
- **WHEN** quantidade é ausente, zero, negativa, tem fração BRASIL, sete casas ou excede precisão
- **THEN** retorna 400 REQUEST_INVALIDO com campo quantidade e não grava

#### Scenario: Precisão BR e EUA
- **WHEN** são informadas BRASIL 100.000000 e EUA 0.000001 dentro dos limites
- **THEN** preserva exatamente os valores sem conversão binária

#### Scenario: Data futura por mercado
- **WHEN** dataEfetiva supera a data corrente na zona do mercado
- **THEN** retorna 400 DATA_EFETIVA_FUTURA, mesmo que uma comparação UTC produzisse outro resultado

#### Scenario: Retroatividade auditável
- **WHEN** registro de hoje informa data efetiva anterior válida
- **THEN** preserva dataEfetiva e registra o instante atual em dataRegistro, sem inventar horário efetivo

### Requirement: Primeira posição válida e estabilidade cronológica
O sistema SHALL procurar espaços em ordem cronológica na data solicitada, antes/depois de eventos financeiros e entre custódias existentes, selecionando a primeira posição que satisfaz origem e todo histórico posterior. SHALL persistir posição exata e estável; MUST NOT renumerar operações, reposicionar custódias existentes ou mudar a data para encontrar saldo. Operações financeiras futuras no cadastro SHALL continuar anexadas por sua ordem financeira vigente. Uma custódia após a antiga última operação SHALL permanecer antes da próxima operação adicionada no dia, inclusive quando o dia inicialmente não tinha operações. Empates SHALL ser resolvidos pela primeira posição no replay determinístico, sem depender de ordem de consulta ou identificadores entre domínios.

#### Scenario: Primeira posição cronológica válida
- **WHEN** há compra A100 ordem1 e venda B60 ordem2 e é solicitado A→B60
- **THEN** seleciona o espaço após a compra e antes da venda, preservando ordens1/2

#### Scenario: Inserção entre custódias existentes
- **WHEN** T1 fornece saldo em B e T2 consome esse saldo; somente um espaço entre elas satisfaz a nova movimentação e o sufixo
- **THEN** insere nesse espaço e preserva as chaves de T1/T2

#### Scenario: Ausência de posição válida
- **WHEN** há compra A100 seguida de venda A80 e é solicitado A→B50 no dia
- **THEN** retorna 409 POSICAO_CUSTODIA_INDISPONIVEL e mantém data e registros existentes

#### Scenario: Nova operação no mesmo dia
- **WHEN** custódia está após ordem1 e uma operação posterior recebe ordem2
- **THEN** custódia permanece após ordem1 e antes da ordem2; a operação é validada nessa sequência

#### Scenario: Dia sem operações financeiras
- **WHEN** custódia válida ocupa data com saldo trazido de dia anterior e uma operação é adicionada depois nessa data
- **THEN** custódia mantém âncora anterior à primeira operação e não passa a depender automaticamente dela

#### Scenario: Múltiplos espaços válidos
- **WHEN** mais de um espaço cronológico satisfaz as regras
- **THEN** persistência escolhe o primeiro espaço e a posição não muda nas consultas seguintes

#### Scenario: Limite físico da chave
- **WHEN** após verificação técnica isolada da representação, a primeira posição quantitativamente válida requer chave exata além do limite confirmado
- **THEN** retorna 409 LIMITE_POSICAO_CUSTODIA sem renumerar, reposicionar ou escolher outro espaço silenciosamente

### Requirement: Saldo local e comparação diferencial integral
A origem SHALL possuir saldo suficiente imediatamente antes da candidata. O sistema SHALL reconstruir antes/depois sobre os mesmos dados protegidos e comparar deficit=max(0,-saldo) por grupo em cada ponto da união dos históricos, alinhado por identidade discriminada e posição estável, transportando saldo quando não houver evento. SHALL exigir deficitDepois<=deficitAntes e preservar validade de todas as custódias ativas. Comparar saldo final, pior déficit ou índice da lista isoladamente MUST NOT substituir essa regra. Legado independente inalterado SHALL ser permitido; eventos posteriores conhecidos SHALL ser considerados, inclusive em posições encerradas.

#### Scenario: Transferência sem saldo
- **WHEN** origem nunca tem a quantidade requerida em nenhum espaço da data
- **THEN** rejeita sem usar compra posterior ou saldo de outra corretora/carteira/ação

#### Scenario: Atribuição sem saldo
- **WHEN** grupo sem corretora não tem a quantidade requerida
- **THEN** rejeita sem usar saldo das corretoras cadastradas

#### Scenario: Retroativa válida
- **WHEN** movimentação encontra origem suficiente e não agrava nenhum déficit nem invalida custódia
- **THEN** aceita e reconstrói o histórico preservando posições existentes

#### Scenario: Retroativa que agrava déficit
- **WHEN** origem é suficiente no lançamento mas uma venda posterior fica negativa ou piora
- **THEN** rejeita a candidata com conflito histórico identificável

#### Scenario: Déficit legado independente
- **WHEN** B possui déficit legado inalterado e uma movimentação legítima entre A/C não muda nenhum ponto de B
- **THEN** aceita sem exigir regularização global

#### Scenario: Déficit reduzido ou regularizado
- **WHEN** entrada retroativa válida reduz ou elimina negativo de destino e não piora outros grupos
- **THEN** aceita; diagnósticos posteriores refletem o histórico efetivo recalculado

#### Scenario: Prolongamento de déficit
- **WHEN** pior saldo não muda mas o novo replay continua negativo num ponto em que antes já estava regularizado
- **THEN** rejeita pelo ponto correspondente, sem aceitar apenas porque o mínimo global é igual

#### Scenario: Cadeia de atribuições e transferências
- **WHEN** compra sem corretora100 precede atribuição para A80 e transferência A→B50
- **THEN** preserva saldos sem corretora20/A30/B50 e a validade de cada origem

#### Scenario: Checkpoint exclusivo com transporte de saldo
- **WHEN** candidata A→C5 é inserida entre COMPRA A100 e os eventos legados COMPRA B10, VENDA B20 e COMPRA B10, mantendo posições existentes
- **THEN** compara a união dos checkpoints por identidade discriminada/posição, transporta saldo anterior (A100,B0,C0) no ponto sem candidata e aceita porque déficit B10 na venda permanece igual antes/depois, sem alinhar índices de listas

#### Scenario: Déficit intermediário apesar de finais positivos
- **WHEN** compra A100 em D precede compra B100 em D+1, venda A90 em D+2 e transferência B→A20 em D+3, e candidata A→B20 é solicitada em D
- **THEN** rejeita o espaço após a compra em D porque saldo A após a venda passa de10 para-10, mesmo com saldos finais positivos e consolidado final110; sem outro espaço válido em D retorna 409 POSICAO_CUSTODIA_INDISPONIVEL com causa CONFLITO_HISTORICO_QUANTITATIVO, sem mudar a data

### Requirement: Atomicidade e invariância financeira do cadastro
Registro SHALL ser atômico sob bloqueio transacional por carteira compartilhado com operações e estornos, sem bloqueio global entre carteiras. Replay SHALL ocorrer na transação protegida e MUST NOT usar consulta pública independente para validar a candidata. Custódia SHALL conservar quantidade consolidada, preço médio, custo, realizado, não realizado e rentabilidade para mesmos preços/cotações; MUST NOT introduzir tipos no enum financeiro, snapshots, saldos materializados ou integrações reais.

#### Scenario: Venda concorrente com transferência
- **WHEN** venda e transferência competem pelo mesmo saldo de origem
- **THEN** execuções serializadas revalidam o histórico confirmado; somente as combinações válidas são persistidas

#### Scenario: Carteiras independentes
- **WHEN** duas carteiras distintas registram movimentações
- **THEN** não compartilham lock global de custódia

#### Scenario: Falha após validação
- **WHEN** flush ou confirmação falha
- **THEN** nenhuma parte do lançamento é persistida e não há débito/crédito parcial

#### Scenario: Invariância dos cálculos
- **WHEN** transferência ou atribuição é registrada mantendo operações financeiras e cotações
- **THEN** todos os resultados financeiros consolidados permanecem iguais
