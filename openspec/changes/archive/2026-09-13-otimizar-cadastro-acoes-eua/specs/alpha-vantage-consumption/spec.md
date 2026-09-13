## MODIFIED Requirements

### Requirement: Consulta minima e deduplicacao
Atualizacao de acao EUA cadastrada SHALL usar somente GLOBAL_QUOTE com identidade persistida, validando simbolo e cotacao. Atualizacoes repetidas ou concorrentes do mesmo ticker na instancia SHALL compartilhar o resultado da tentativa concluida por cooldown configuravel de 15m. Atualizacao MUST NOT executar retry automatico, polling ou sleep. Cadastro SHALL preservar validacao externa de identidade e aguardar um intervalo configuravel positivo em milissegundos (default 1100) depois da busca validada e antes da cotacao, sem retry.

#### Scenario: Espacamento das chamadas de cadastro
- **WHEN** SYMBOL_SEARCH retorna identidade, moeda e nome validos
- **THEN** GLOBAL_QUOTE SHALL ocorrer somente depois de aguardar pelo menos o intervalo configurado apos a resposta da busca, mantendo exatamente duas chamadas no sucesso

#### Scenario: Configuracao invalida do intervalo
- **WHEN** o intervalo configurado nao e um inteiro positivo em milissegundos
- **THEN** a inicializacao SHALL falhar explicitamente

#### Scenario: Espera interrompida
- **WHEN** a thread e interrompida durante a espera antes de GLOBAL_QUOTE
- **THEN** o adapter SHALL preservar a interrupcao, encerrar com indisponibilidade e nao executar a segunda chamada

#### Scenario: Repeticao e concorrencia
- **WHEN** duas atualizacoes do mesmo ticker chegam em curto intervalo
- **THEN** somente uma tentativa externa e feita e a outra reutiliza estado ou erro correspondente

#### Scenario: Fluxo de cadastro
- **WHEN** uma acao americana nova e cadastrada
- **THEN** o cadastro SHALL fazer exatamente uma SYMBOL_SEARCH e uma GLOBAL_QUOTE quando os dados forem válidos, reutilizando símbolo exato, região EUA, moeda USD e nome da busca; MUST NOT consultar OVERVIEW ou repetir consultas na mesma tentativa

#### Scenario: Interrupção antes da cotação
- **WHEN** a busca falha, não confirma identidade ou moeda compatíveis ou não fornece nome utilizável
- **THEN** o cadastro SHALL encerrar com o erro padronizado correspondente após uma única request, sem cotação, fallback ou persistência

#### Scenario: Limite externo em qualquer etapa
- **WHEN** SYMBOL_SEARCH ou GLOBAL_QUOTE retorna HTTP 429 ou payload reconhecido de limite
- **THEN** o cadastro SHALL retornar 429 LIMITE_REQUISICOES_EXCEDIDO após uma ou duas requests respectivamente, sem retry ou persistência
