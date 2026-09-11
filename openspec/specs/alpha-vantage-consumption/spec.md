# alpha-vantage-consumption Specification

## Purpose
Reduzir consultas externas de cotacao atual americana preservando a ultima cotacao valida e as regras financeiras existentes.

## Requirements

### Requirement: Reutilizacao temporal especifica EUA
Atualizacao EUA SHALL reutilizar cotacao positiva persistida com timestamp nao futuro e idade menor que intervalo configurado (padrao 15m), sem HTTP externo ou nova observacao. No limite exato ou sem cotacao reutilizavel SHALL permitir consulta, respeitando cooldown. BRAPI MUST permanecer inalterada.

#### Scenario: Cotacao recente
- **WHEN** PATCH EUA encontra cotacao de cinco minutos e intervalo 15m
- **THEN** retorna estado persistido sem Alpha Vantage

#### Scenario: Cotacao antiga ou ausente
- **WHEN** nao existe cotacao recente nem cooldown ativo
- **THEN** consulta Alpha Vantage e persiste somente candidata valida posterior pelas regras vigentes

### Requirement: Consulta minima e deduplicacao
Atualizacao de acao EUA cadastrada SHALL usar somente GLOBAL_QUOTE com identidade persistida, validando simbolo e cotacao. Atualizacoes repetidas ou concorrentes do mesmo ticker na instancia SHALL compartilhar o resultado da tentativa concluida por cooldown configuravel de 15m. MUST NOT executar retry automatico, polling ou sleep. Cadastro SHALL preservar validacao externa de identidade.

#### Scenario: Repeticao e concorrencia
- **WHEN** duas atualizacoes do mesmo ticker chegam em curto intervalo
- **THEN** somente uma tentativa externa e feita e a outra reutiliza estado ou erro correspondente

#### Scenario: Fluxo de cadastro
- **WHEN** uma acao americana nova e cadastrada
- **THEN** SYMBOL_SEARCH e GLOBAL_QUOTE continuam necessarios e OVERVIEW somente se faltar nome

### Requirement: Falha preserva estado e consumo
Rate limit, timeout ou indisponibilidade SHALL preservar cotacao e timestamp persistidos, comunicar erro vigente com cotacaoPreservada e impedir nova tentativa durante cooldown. Timeouts SHALL ser explicitos e configuraveis (conexao 2s/leitura 5s). Nenhuma formula, FX, DTO publico ou historico financeiro SHALL mudar.

#### Scenario: Falha repetida
- **WHEN** Alpha Vantage falha e cliente repete PATCH durante cooldown
- **THEN** nenhuma gravacao ou nova consulta ocorre e ultima cotacao valida permanece informada

#### Scenario: Retomada
- **WHEN** cooldown expira e cotacao permanece antiga
- **THEN** proxima solicitacao explicita pode tentar novamente sem coleta automatica

#### Scenario: Dashboard
- **WHEN** Atualizar dados recarrega as quatro consultas financeiras
- **THEN** continua lendo dados persistidos sem chamar Alpha Vantage
