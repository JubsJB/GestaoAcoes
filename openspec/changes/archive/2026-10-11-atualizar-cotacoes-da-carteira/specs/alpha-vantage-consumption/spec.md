## MODIFIED Requirements

### Requirement: Reutilizacao temporal especifica EUA
Após verificar que não há cooldown ativo, atualização EUA individual e lote SHALL reutilizar informação válida persistida com referência conhecida e cotacaoObtidaEm não futuro com idade menor que intervalo configurado (padrão 15m), sem HTTP ou histórico. Referência de mercado MUST NOT medir reuso. No limite exato, legado sem obtenção ou informação não reutilizável SHALL permitir consulta respeitando cooldown. Reuso SHALL classificar SEM_NOVA_COTACAO com motivo REUSO e MUST NOT alterar preço, referência ou cotacaoObtidaEm. BRAPI SHALL preservar seu fluxo sem novo cache ou política de coleta automática.

#### Scenario: Cotacao recente
- **WHEN** obtenção válida EUA tem cinco minutos, intervalo 15m e não há cooldown ativo
- **THEN** retorna estado sem Alpha Vantage, escrita ou histórico, preserva cotacaoObtidaEm e não contabiliza atualização nesta execução

#### Scenario: Cotacao antiga ou ausente
- **WHEN** não há obtenção reutilizável nem cooldown
- **THEN** consulta GLOBAL_QUOTE e aplica a regra temporal compartilhada

### Requirement: Consulta minima e deduplicacao
Atualizacao de acao EUA cadastrada SHALL usar somente GLOBAL_QUOTE com identidade persistida, validando simbolo e cotacao. Atualizacoes repetidas ou concorrentes do mesmo ticker na instancia SHALL compartilhar o resultado da tentativa concluida por cooldown configuravel de 15m. Atualizacao MUST NOT executar retry automatico, polling ou sleep. Cadastro SHALL preservar validacao externa de identidade e aguardar um intervalo configuravel positivo em milissegundos (default 1100) depois da busca validada e antes da cotacao, sem retry.
 O campo 07. latest trading day SHALL ser interpretado como LocalDate (DATA); MUST NOT ser descartado por parser de OffsetDateTime ou convertido em meia-noite. Cooldown SHALL usar conclusão da tentativa, não referência de mercado. Uma solicitação que reutiliza sucesso anterior SHALL retornar SEM_NOVA_COTACAO com motivo COOLDOWN_SUCESSO, sem contar novamente a persistência de outra execução; erro reutilizado SHALL ser FALHA. Coordenação SHALL ser compartilhada entre lote e individual na instância. Controle de tentativa/cooldown SHALL permanecer somente em memória local à instância, não persistido e passível de perda em restart; MUST NOT representar observação nem alterar preço/referência/obtenção ou histórico. Cooldown ativo SHALL preceder reuso: falha concluída retorna FALHA, sucesso concluído retorna SEM_NOVA_COTACAO com COOLDOWN_SUCESSO, ambos sem HTTP ou escrita. Somente sem cooldown ativo SHALL verificar reuso; nova chamada externa SHALL ocorrer apenas sem cooldown nem informação reutilizável. No limite exato, o intervalo correspondente SHALL ser considerado expirado.

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

#### Scenario: Cooldown de sucesso precede reuso
- **WHEN** existe cooldown ativo de sucesso e obtenção persistida ainda reutilizável
- **THEN** retorna SEM_NOVA_COTACAO com COOLDOWN_SUCESSO, sem HTTP, histórico ou alteração de cotacaoObtidaEm/preço/referência

#### Scenario: Cooldown de falha precede reuso
- **WHEN** existe cooldown ativo de falha, inclusive com estado persistido recente obtido por outra execução
- **THEN** preserva o erro vigente, classifica FALHA no lote e não executa HTTP nem altera cotacaoObtidaEm/preço/referência ou histórico

#### Scenario: Restart preserva somente estado persistido
- **WHEN** restart perde o controle operacional em memória
- **THEN** consulta explícita ainda verifica reuso pela obtenção persistida antes de permitir HTTP e não fabrica tentativa ou observação

### Requirement: Falha preserva estado e consumo
Rate limit, timeout, indisponibilidade e falha temporal SHALL preservar preço, referência e obtenção válidos; tentativa concluída SHALL alimentar somente cooldown. Timeouts SHALL permanecer configuráveis (conexão 2s/leitura 5s); não há retry. DTOs MAY receber somente extensão temporal aprovada, sem mudança de fórmula, FX, histórico de operações ou snapshots. Atualizar dados SHALL continuar somente leitura; Atualizar cotações SHALL processar lote manual segundo regras de consumo existentes.

#### Scenario: Falha repetida
- **WHEN** Alpha Vantage falha e cliente repete PATCH durante cooldown
- **THEN** nenhuma gravacao ou nova consulta ocorre e ultima cotacao valida permanece informada

#### Scenario: Retomada
- **WHEN** cooldown expira e cotacao permanece antiga
- **THEN** proxima solicitacao explicita pode tentar novamente sem coleta automatica

#### Scenario: Dashboard
- **WHEN** Atualizar dados recarrega as quatro consultas financeiras
- **THEN** continua lendo dados persistidos sem chamar Alpha Vantage
