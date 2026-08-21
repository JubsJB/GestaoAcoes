## ADDED Requirements

### Requirement: Contrato da atualização de cotação
O sistema SHALL expor `PATCH /acoes/{id}/cotacao` como operação dedicada para atualizar a última cotação conhecida de uma Ação persistida. A operação SHALL NOT aceitar cotação, timestamp, ticker, mercado, moeda, nome da empresa ou qualquer outro dado de negócio no corpo da requisição. Quando concluída, SHALL responder `200 OK` com o `AcaoResponse` completo que representa o estado persistido ao final da solicitação e SHALL NOT incluir o header `Location`.

#### Scenario: Atualização solicitada sem corpo
- **WHEN** o cliente solicita `PATCH /acoes/{id}/cotacao` sem corpo para uma Ação existente e uma nova cotação aplicável é obtida
- **THEN** o sistema responde `200 OK` com o DTO completo da Ação no estado persistido ao final da operação

#### Scenario: Tentativa de informar a cotação manualmente
- **WHEN** o cliente envia corpo na solicitação de atualização, inclusive com cotação ou timestamp
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO`, sem consultar provider e sem alterar a Ação

### Requirement: Seleção da Ação e do provider pelo estado persistido
O sistema SHALL localizar a Ação pelo ID antes de qualquer chamada externa e SHALL usar exclusivamente o ticker e o mercado persistidos para solicitar a nova cotação. O sistema SHALL consultar BRAPI quando o mercado persistido for `BRASIL` e Alpha Vantage quando for `EUA`, sem permitir seleção do provider pelo cliente.

#### Scenario: Ação brasileira persistida
- **WHEN** a Ação encontrada possui `mercado=BRASIL`
- **THEN** o sistema consulta somente a BRAPI usando o ticker persistido

#### Scenario: Ação americana persistida
- **WHEN** a Ação encontrada possui `mercado=EUA`
- **THEN** o sistema consulta somente a Alpha Vantage usando o ticker persistido

#### Scenario: Ação inexistente
- **WHEN** o ID informado não corresponde a uma Ação persistida
- **THEN** o sistema responde `404 Not Found` no formato padronizado atual e não consulta provider

### Requirement: Validação da nova cotação e preservação da Ação
O sistema SHALL concluir a atualização somente com cotação presente, numérica, maior que zero e exatamente representável em `NUMERIC(19,6)`. Uma atualização aplicável SHALL alterar somente `cotacaoAtual` e `dataHoraCotacao` e MUST preservar `id`, ticker, nome da empresa, mercado e moeda, ainda que o provider devolva outro nome para a empresa.

#### Scenario: Nova cotação válida
- **WHEN** o provider correspondente devolve identidade compatível, dados obrigatórios e cotação positiva representável
- **THEN** o sistema persiste a nova cotação e sua referência temporal sem modificar os demais campos

#### Scenario: Nome externo diferente
- **WHEN** o provider devolve nome da empresa diferente do nome persistido durante uma atualização válida
- **THEN** o sistema preserva `nomeEmpresa` e usa os dados externos somente para validar e obter a cotação

#### Scenario: Cotação ausente, não numérica, zero ou negativa
- **WHEN** o provider não fornece uma cotação positiva utilizável
- **THEN** o sistema responde `422 Unprocessable Entity` com código `COTACAO_INDISPONIVEL` e preserva integralmente a Ação

#### Scenario: Cotação fora da precisão suportada
- **WHEN** a nova cotação não pode ser representada exatamente em `NUMERIC(19,6)`
- **THEN** o sistema responde `422 Unprocessable Entity` com código `COTACAO_FORA_DA_PRECISAO`, sem truncar, arredondar ou persistir o valor

### Requirement: Semântica temporal e cotação numericamente igual
O sistema SHALL representar `dataHoraCotacao` como `OffsetDateTime` em UTC. O sistema SHALL preferir o timestamp utilizável e confiável da cotação fornecido pelo provider e SHALL usar como fallback o instante UTC em que a aplicação obteve a cotação. Uma cotação numericamente igual à persistida SHALL continuar sendo uma observação válida quando possuir referência temporal posterior.

#### Scenario: Timestamp confiável do provider
- **WHEN** o provider fornece timestamp utilizável associado à nova cotação
- **THEN** o sistema normaliza esse timestamp para UTC antes de avaliar e persistir a atualização

#### Scenario: Fallback temporal da aplicação
- **WHEN** o provider omite o timestamp ou fornece valor temporal não utilizável
- **THEN** o sistema usa o instante do relógio UTC da aplicação em que a cotação foi obtida

#### Scenario: Mesmo valor com referência temporal posterior
- **WHEN** a cotação obtida é numericamente igual à atual e seu timestamp normalizado é posterior ao persistido
- **THEN** o sistema preserva o valor e atualiza `dataHoraCotacao`, respondendo `200 OK` com o estado resultante

#### Scenario: Resultado sem referência temporal posterior
- **WHEN** a cotação validada possui timestamp igual ou anterior ao `dataHoraCotacao` persistido
- **THEN** o sistema não regride nem sobrescreve o estado e responde `200 OK` com o `AcaoResponse` atualmente persistido

### Requirement: Divergência de ticker canônico
O sistema MUST NOT alterar automaticamente o ticker persistido durante a atualização de cotação. Quando o provider indicar explicitamente renomeação e devolver ticker canônico diferente, o sistema SHALL responder `409 Conflict` com código `TICKER_CANONICO_DIVERGENTE`, informar os tickers persistido e retornado em `details` e preservar integralmente a Ação para que uma futura funcionalidade de migração de identidade trate o caso.

#### Scenario: BRAPI confirma o ticker persistido
- **WHEN** a BRAPI confirma o mesmo ticker da Ação brasileira
- **THEN** o fluxo normal de validação da cotação pode prosseguir

#### Scenario: BRAPI informa ticker canônico diferente
- **WHEN** a BRAPI indica explicitamente que o ticker persistido foi renomeado e devolve ticker canônico diferente
- **THEN** o sistema responde `409 Conflict` com os dois tickers em `details`, sem atualizar ticker, nome, cotação ou timestamp

#### Scenario: Provider devolve ticker incompatível sem renomeação explícita
- **WHEN** o provider devolve ticker diferente sem sinalização explícita de renomeação admitida
- **THEN** o sistema responde `502 Bad Gateway` com código `RESPOSTA_EXTERNA_INVALIDA` e preserva a Ação

### Requirement: Preservação da última cotação em falhas
Quando uma Ação existente não puder obter uma nova cotação aplicável, o sistema SHALL preservar a última cotação válida e SHALL informar no erro padronizado, quando aplicável, `acaoId`, `cotacaoPreservada=true`, `ultimaCotacaoValida` e `dataHoraUltimaCotacao`. O erro SHALL manter o status e o código correspondentes à causa original.

#### Scenario: Ticker deixou de ser encontrado
- **WHEN** o provider correspondente não encontra mais o ticker persistido
- **THEN** o sistema responde `404 Not Found` com código `TICKER_INEXISTENTE`, inclui a última cotação em `details` e não altera o registro

#### Scenario: Limite de requisições excedido
- **WHEN** o provider informa rate limit por status ou payload reconhecido
- **THEN** o sistema responde `429 Too Many Requests` com código `LIMITE_REQUISICOES_EXCEDIDO`, informa a preservação e não altera o registro

#### Scenario: Provider indisponível ou sem configuração
- **WHEN** o provider selecionado está indisponível, não configurado ou apresenta falha de conexão que não seja timeout
- **THEN** o sistema responde `503 Service Unavailable` com código `SERVICO_EXTERNO_INDISPONIVEL`, informa a preservação e não altera o registro

#### Scenario: Timeout do provider
- **WHEN** a consulta externa excede o timeout configurado
- **THEN** o sistema responde `504 Gateway Timeout` com código `SERVICO_EXTERNO_TIMEOUT`, informa a preservação e não altera o registro

#### Scenario: Resposta inválida ou dados externos incompletos
- **WHEN** o provider responde com payload incompatível, identidade ou moeda incompatível, ou omite dado obrigatório do contrato de cotação
- **THEN** o sistema responde com o status e código padronizados de resposta externa inválida ou dados externos incompletos, informa a preservação e não altera o registro

### Requirement: Concorrência e persistência final consistente
O sistema SHALL manter chamadas externas fora da transação de persistência. A seção final SHALL serializar atualizações concorrentes da mesma Ação e MUST NOT substituir uma cotação com referência temporal mais nova por outra igual ou mais antiga. Nenhum fluxo SHALL criar registro parcial ou histórico de cotação.

#### Scenario: Duas atualizações concorrentes com timestamps diferentes
- **WHEN** duas solicitações para a mesma Ação obtêm cotações válidas com referências temporais diferentes
- **THEN** o estado final mantém a cotação de timestamp mais recente, independentemente da ordem em que as chamadas externas terminem

#### Scenario: Falha antes da seção final
- **WHEN** a busca inicial, a chamada externa ou a validação falha
- **THEN** nenhuma transação de escrita altera a Ação

#### Scenario: Persistência concluída sem histórico
- **WHEN** uma nova cotação temporalmente aplicável é persistida
- **THEN** somente `cotacaoAtual` e `dataHoraCotacao` da Ação existente são atualizados, sem criar tabela ou registro de histórico
