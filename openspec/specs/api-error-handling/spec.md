# API Error Handling Specification

## Purpose

Definir respostas públicas seguras e semanticamente corretas para violações de integridade persistente, preservando conflitos de domínio conhecidos sem atribuir causas falsas nem expor detalhes internos do banco de dados.

## Requirements

### Requirement: Fallback seguro para violação de integridade não classificada
Quando uma violação de integridade persistente não tiver sido traduzida com segurança para um conflito de domínio específico, o sistema SHALL responder `409 Conflict` com código `INTEGRIDADE_DADOS_VIOLADA`, mensagem pública estável `A operação viola uma regra de integridade dos dados.` e `details` vazio. O sistema MUST NOT atribuir a violação a Corretora, Ação, Operação, Snapshot ou outro domínio sem identificação confiável da causa.

#### Scenario: Violação genérica sem causa classificável
- **WHEN** uma operação alcança o tratamento centralizado com uma violação de integridade cuja constraint não foi classificada com segurança
- **THEN** a resposta é `409 Conflict` com código `INTEGRIDADE_DADOS_VIOLADA` e não usa `CORRETORA_DUPLICADA` nem outro código específico

#### Scenario: Violação futura desconhecida
- **WHEN** uma nova constraint ou outra forma de integridade persistente produz uma exceção ainda não reconhecida pelo domínio
- **THEN** o sistema aplica o mesmo fallback genérico sem inventar a causa

### Requirement: Preservação dos conflitos específicos conhecidos
O sistema SHALL preservar os contratos específicos vigentes de duplicidade de Corretora, duplicidade de Ação, ordem de Operação duplicada e colisão temporal de Snapshot somente quando a violação conhecida correspondente for identificada com segurança. Uma falha diferente ocorrida no mesmo caso de uso MUST NOT ser traduzida para o código específico daquele domínio.

#### Scenario: CNPJ duplicado identificado
- **WHEN** a constraint conhecida `uk_corretora_cnpj` é identificada durante o cadastro concorrente de Corretora
- **THEN** o sistema mantém `409 Conflict / CORRETORA_DUPLICADA`

#### Scenario: Falha diferente ao persistir Corretora
- **WHEN** a persistência de Corretora falha por integridade sem identificação da constraint de CNPJ
- **THEN** o sistema não responde `CORRETORA_DUPLICADA` e deixa a violação seguir para o fallback seguro

#### Scenario: Ação duplicada identificada
- **WHEN** a constraint conhecida `uk_acao_ticker_mercado` é identificada durante o cadastro concorrente de Ação
- **THEN** o sistema mantém `409 Conflict / ACAO_DUPLICADA`

#### Scenario: Falha diferente ao persistir Ação
- **WHEN** a persistência da Ação ou de sua observação histórica inicial falha por integridade sem identificação da constraint de ticker e mercado
- **THEN** o sistema não responde `ACAO_DUPLICADA` e deixa a violação seguir para o fallback seguro

#### Scenario: Ordem de Operação duplicada identificada
- **WHEN** a constraint conhecida `uk_operacao_carteira_acao_data_ordem` é identificada
- **THEN** o sistema mantém `409 Conflict / ORDEM_OPERACAO_DUPLICADA`

#### Scenario: Snapshot temporal duplicado identificado
- **WHEN** a constraint conhecida `uk_snapshot_carteira_carteira_data_hora` é identificada
- **THEN** o sistema mantém `409 Conflict / SNAPSHOT_CARTEIRA_DUPLICADO`

#### Scenario: Constraint monetária de Snapshot não classificada
- **WHEN** a constraint `uk_snapshot_carteira_moeda_snapshot_moeda` ou outra constraint sem conflito público específico é violada
- **THEN** o sistema aplica `409 Conflict / INTEGRIDADE_DADOS_VIOLADA`

#### Scenario: Integridade do Histórico de Cotação não classificada
- **WHEN** uma violação de integridade do Histórico de Cotação não possui conflito de domínio específico
- **THEN** o sistema aplica `409 Conflict / INTEGRIDADE_DADOS_VIOLADA`

### Requirement: Classificação portável e conservadora
A classificação de uma violação específica SHALL usar informação estruturada disponível na cadeia de causas e correspondência exata com constraints conhecidas. O sistema MUST NOT depender de parsing textual sobre mensagens nativas, de SQLState ou diretamente de classes do driver PostgreSQL e SHALL usar o fallback genérico quando a informação estruturada estiver ausente ou não for reconhecida.

#### Scenario: Constraint conhecida disponível de forma estruturada
- **WHEN** a cadeia de causas fornece de forma estruturada o nome de uma constraint conhecida
- **THEN** o caso de uso pode aplicar o conflito específico correspondente

#### Scenario: Informação estruturada indisponível
- **WHEN** H2, PostgreSQL, Hibernate ou outra camada não fornece um nome de constraint utilizável na cadeia de causas
- **THEN** o sistema aplica o fallback genérico sem tentar inferir a causa pela mensagem textual

#### Scenario: Nome estruturado decorado ou não correspondente
- **WHEN** o banco fornece um nome estruturado com schema, índice, prefixo ou outra decoração que impeça correspondência confiável com o nome conhecido
- **THEN** o sistema aplica o fallback genérico sem substring, regex, parsing ou normalização textual adicional

#### Scenario: Constraint técnica não modelada
- **WHEN** ocorre violação de FK, check, not null, PK, unique desconhecida ou constraint futura sem tradução de domínio específica
- **THEN** o sistema aplica `409 Conflict / INTEGRIDADE_DADOS_VIOLADA`

### Requirement: Resposta sem detalhes internos
A resposta pública para violação de integridade não classificada MUST NOT conter SQL, SQLState, stack trace, mensagem nativa, nome de tabela, coluna, índice ou constraint, nem detalhes de Hibernate, PostgreSQL ou H2. A aplicação SHALL preservar internamente informação diagnóstica do erro original segundo o logging vigente, sem introduzir identificador de correlação inexistente.

#### Scenario: Payload público do fallback
- **WHEN** uma violação não classificada é tratada
- **THEN** o payload segue `StandardError`, contém somente status, mensagem pública, path, código genérico e detalhes vazios

#### Scenario: Diagnóstico interno
- **WHEN** o fallback centralizado é acionado
- **THEN** a exceção original e o path da requisição são registrados internamente sem copiar detalhes técnicos para a resposta

### Requirement: Compatibilidade dos contratos e persistência
O tratamento robustecido SHALL preservar os status e códigos de conflitos de domínio já corretamente traduzidos e MUST NOT alterar endpoints, DTOs de sucesso, regras financeiras, providers, schema ou transações dos casos de uso. A resposta genérica SHALL ser independente do banco suportado pela aplicação.

#### Scenario: Regressão dos conflitos modelados
- **WHEN** ocorre um conflito de domínio que já possui tradução específica correta
- **THEN** seu contrato público permanece inalterado

#### Scenario: Execução com H2 ou PostgreSQL
- **WHEN** o fallback recebe uma violação de integridade em qualquer banco suportado
- **THEN** produz o mesmo contrato público genérico sem depender do driver específico

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
