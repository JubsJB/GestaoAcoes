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
