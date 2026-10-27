## ADDED Requirements

### Requirement: Conflitos quantitativos e de custódia estáveis
Escritas de custódia e novas operações financeiras SHALL preservar StandardError {timeStamp,status,error,message,path,code,details}. Entrada inválida SHALL usar400 REQUEST_INVALIDO; data futura de custódia400 DATA_EFETIVA_FUTURA; referências inexistentes404 conforme padrão atual de code=null. Conflito consolidado SHALL preservar409 POSICAO_INSUFICIENTE. Novos409 SHALL distinguir SALDO_ORIGEM_INSUFICIENTE, SALDO_CORRETORA_INSUFICIENTE, CONFLITO_HISTORICO_QUANTITATIVO, MOVIMENTACAO_CUSTODIA_INVALIDADA, ESTORNO_DUPLICADO, POSICAO_CUSTODIA_INDISPONIVEL e LIMITE_POSICAO_CUSTODIA. Integridade não classificada SHALL preservar INTEGRIDADE_DADOS_VIOLADA. Details SHALL identificar contexto, grupo e evento conflitante de forma discriminada, com saldos exatos quando disponíveis, sem SQL, stacktrace ou credenciais.

#### Scenario: Saldo e evento identificáveis
- **WHEN** validação encontra conflito local ou posterior
- **THEN** resposta identifica carteira/ação, grupo cadastrado ou nulo, evento/data/posição e valores exatos, sem confundir IDs

#### Scenario: Estorno duplicado no banco
- **WHEN** constraint conhecida de unicidade do vínculo é violada
- **THEN** traduz para409 ESTORNO_DUPLICADO sem expor nome interno da constraint

#### Scenario: Integridade desconhecida
- **WHEN** violação não corresponde a constraint conhecida
- **THEN** mantém409 INTEGRIDADE_DADOS_VIOLADA, sem fabricar duplicidade

#### Scenario: Nenhum espaço válido
- **WHEN** cadastro examinou todos os espaços do dia e ao menos um tem saldo local suficiente, mas nenhum preserva as validações posteriores
- **THEN** retorna409 POSICAO_CUSTODIA_INDISPONIVEL com motivo representativo e nenhum evento persistido

#### Scenario: Saldo da nova venda
- **WHEN** consolidado passa mas grupo informado/nulo falha
- **THEN** retorna409 SALDO_CORRETORA_INSUFICIENTE sem substituir falha consolidada por código local

### Requirement: Precedência determinística de erros simultâneos
O sistema SHALL priorizar estrutura/campos permitidos antes de referências/contexto; em estorno, duplicidade confirmada após lock SHALL preceder simulação quantitativa. Em operações financeiras e estornos admissíveis SHALL priorizar consolidado, saldo local da candidata quando aplicável, custódia ativa invalidada e comparação diferencial, nessa ordem. Busca de custódia SHALL descartar espaços sem origem suficiente, retornar SALDO_ORIGEM_INSUFICIENTE somente se todos falham por saldo e retornar POSICAO_CUSTODIA_INDISPONIVEL com causa representativa quando existe espaço com saldo, mas nenhum válido. Dentro de cada classe SHALL identificar primeiro checkpoint conflitante e grupo em ordem determinística. MUST NOT introduzir novo código, simular neutralização duplicada ou expor dados de outra carteira para escolher erro.

#### Scenario: Request inválido e referência ausente
- **WHEN** TRANSFERENCIA tem quantidade0 e destino inexistente
- **THEN** retorna 400 REQUEST_INVALIDO antes de resolver referência, sem priorizar404

#### Scenario: Consolidado e corretora insuficientes
- **WHEN** VENDA B20 encontra consolidado10 e saldo B0
- **THEN** retorna 409 POSICAO_INSUFICIENTE, preservando prioridade financeira em vez de SALDO_CORRETORA_INSUFICIENTE

#### Scenario: Origem insuficiente e invalidação hipotética
- **WHEN** origem A tem50 em todos os espaços e candidata A→B60 também invalidaria custódia posterior se aplicada
- **THEN** retorna 409 SALDO_ORIGEM_INSUFICIENTE, sem simular evento cuja pré-condição falhou

#### Scenario: Espaço com saldo e custódia posterior inválida
- **WHEN** alguns espaços não têm saldo, outro passa saldo mas invalida custódia ativa e nenhum é válido
- **THEN** retorna 409 POSICAO_CUSTODIA_INDISPONIVEL com causa MOVIMENTACAO_CUSTODIA_INVALIDADA, não o erro local do primeiro espaço descartado

#### Scenario: Custódia invalidada e déficit agravado
- **WHEN** compra A100 precede T1 A→B80 ativa e venda candidata A30 anterior a T1 passa consolidado/saldo local, mas deixa T1 sem80 e produziria A-10
- **THEN** retorna 409 MOVIMENTACAO_CUSTODIA_INVALIDADA antes de CONFLITO_HISTORICO_QUANTITATIVO, sem modificar T1

#### Scenario: Estorno duplicado e conflitos quantitativos
- **WHEN** request admissível referencia original da carteira que já possui estorno e a repetição hipotética produziria conflitos
- **THEN** retorna 409 ESTORNO_DUPLICADO após lock, sem segunda neutralização ou avaliação quantitativa dessa repetição

#### Scenario: Campo proibido em estorno duplicado
- **WHEN** tentativa de estorno já existente também envia quantidade proibida
- **THEN** retorna 400 REQUEST_INVALIDO antes da consulta de duplicidade

#### Scenario: Referência fora da carteira e estorno existente
- **WHEN** original pertence a outra carteira, mesmo que possua estorno nela
- **THEN** retorna 404 com code=null conforme padrão atual, sem expor duplicidade ou situação de outro contexto

### Requirement: Conflitos transacionais sem repetição automática
Falhas reconhecidas de serialização, aquisição de lock ou deadlock SHALL ter tratamento seguro409 CONFLITO_TRANSACIONAL_TRANSITORIO, com rollback e sem detalhes internos. MUST NOT ser classificadas como saldo insuficiente/estorno duplicado sem evidência. Não haverá retry automático de escrita; interface SHALL preservar entrada e permitir decisão manual.

#### Scenario: Conflito transitório
- **WHEN** escrita sofre falha reconhecida de lock/serialização/deadlock
- **THEN** retorna409 CONFLITO_TRANSACIONAL_TRANSITORIO e não confirma estado parcial nem repete a escrita
