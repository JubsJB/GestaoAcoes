## Context

Auditoria localizada: AcaoService.cadastrar -> consultar -> SYMBOL_SEARCH + GLOBAL_QUOTE (2 GET /query), OVERVIEW somente quando falta nome (3 GET). Duplicata persistida e rejeitada antes do provider. Atualizacao hoje repete o mesmo caminho (2-3 GET por solicitacao, sem cooldown). Nao ha retry aplicativo. ExternalApiConfig usa conexao 2s/leitura 5s configuraveis. Note/Information/HTTP429 indicam limite; timeout/5xx sao mapeados para ApiException. AcaoService informa cotacaoPreservada e AcaoCotacaoPersistenceService somente grava candidata posterior atomicamente com historico.

Dashboard Atualizar dados recarrega resumo/posicoes/resultados/evolucao: zero chamadas Alpha, pois usa estado persistido. GET de acoes tambem zero. PATCH de cotacao e o fluxo de atualizacao externa. FechamentoHistoricoService usa TIME_SERIES_DAILY separado (1 chamada por consulta de fechamento; previa/registro podem repetir); nao reutilizar cotacao corrente como fechamento. Esse fluxo de Operacao nao sera alterado nesta change.

## Goals / Non-Goals

Minimizar PATCH repetido e eliminar buscas de identidade ja validada. Sem alterar BRAPI, cadastro/operacoes/formulas ou introduzir scheduler/cache distribuido.

## Decisions

Cotacao positiva com timestamp nao futuro e idade estritamente menor que 15m e reutilizada sem chamada/grava. Na fronteira exata, pode consultar. Configuracao ALPHA_VANTAGE_QUOTE_REUSE_INTERVAL.

Cooldown ALPHA_VANTAGE_REFRESH_COOLDOWN, padrao 15m desde conclusao da tentativa EUA, em memoria por ticker. Durante cooldown, sucesso retorna estado persistido e falha reapresenta erro padronizado com ultima cotacao, sem nova chamada. Falhas nao viram sucesso silencioso. A referencia temporal financeira nao e alterada para indicar tentativa. Serializacao local cobre releitura, consulta e persistencia, evitando duas atualizacoes simultaneas na mesma instancia. Sem transacao de banco durante HTTP. Reinicio perde cooldown, mas conserva cotacao recente persistida. Nao promete coordenacao entre replicas.

Atualizacao usa metodo interno especifico com ticker/nome/moeda persistidos: uma GLOBAL_QUOTE validando simbolo e preco pelas mesmas regras. Cadastro continua 2-3 chamadas necessarias para existencia/regiao/USD/nome. BRAPI mantem consultar original. Nenhum retry/polling/sleep. Timeouts existentes 2s/5s ja sao curtos; preservados inclusive para historico.

## Risks / Trade-offs

- Cache local nao limita quota diaria global -> 15m e padrao conservador por ticker, nao garantia de plano do provedor; configurar conforme uso.
- Cotacao pode ficar desatualizada durante janela -> timestamp original sempre preservado; apos janela atualizacao explicita permitida.
- Sem novo campo de ultima consulta -> cooldown de falha se perde ao reiniciar; aceitavel para instancia atual, sem migration.

## Migration Plan

Sem migration. Configuracoes externas opcionais com padrao 15m; duracoes negativas/zero rejeitadas. Reversao por codigo, sem transformar dados.

O resultado de sucesso confirmado pela persistencia tambem e retido durante cooldown para atender requisicoes concorrentes cujo contexto JPA ainda contenha entidade anterior. Entre resposta persistida retida e entidade lida, retorna a referencia temporal mais nova, sem calcular ou alterar timestamp. Memoria limitada aos tickers atualizados nesta instancia; sem cache de precos de operacoes.
