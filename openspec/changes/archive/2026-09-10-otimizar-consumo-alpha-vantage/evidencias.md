# Evidencias da otimizacao

## Auditoria e comportamento

Antes: cadastro EUA 2 chamadas (SYMBOL_SEARCH + GLOBAL_QUOTE), 3 se OVERVIEW necessario; PATCH repetia 2-3 em cada solicitacao. Duplicata cadastrada rejeitada antes do provider. Dashboard Atualizar dados e GET de acoes: zero Alpha. Fechamento de operacao TIME_SERIES_DAILY: uma chamada por consulta, fluxo preservado (previa e registro podem consultar separadamente). Sem retry aplicativo.

Depois: cadastro e fechamento inalterados; PATCH EUA recente: zero. PATCH antigo/sem timestamp reutilizavel fora do cooldown: uma GLOBAL_QUOTE. Repetidos/concorrentes do mesmo ticker na instancia: zero adicionais por 15m desde conclusao. Cooldown tambem memoriza erro sem o converter em sucesso. Persistencia posterior/atomica mantida. Nenhuma API real consumida em testes.

ALPHA_VANTAGE_QUOTE_REUSE_INTERVAL=15m; idade estritamente menor, timestamp nao futuro, cotacao positiva. Na fronteira exata pode consultar se cooldown expirado. ALPHA_VANTAGE_REFRESH_COOLDOWN=15m. Conexao 2s e leitura 5s existentes preservadas, configuraveis. Reinicio conserva cotacao persistida mas perde cooldown em memoria; nenhuma garantia de quota diaria ou coordenacao entre replicas. Atualizacao de BRAPI segue caminho original sem cooldown.

## Arquivos desta parte

Modificados: src/main/java/com/projeto/services/AcaoService.java; integrations/cotacao/AlphaVantageAdapter.java e CotacaoProvider.java; src/main/resources/application.properties; testes AcaoServiceTest e AlphaVantageAdapterTest. Criado AlphaVantageConsumptionTest. Artefatos proposal/design/tasks e deltas alpha-vantage-consumption/stock-registration criados nesta change.

Sem alteracoes nesta parte em frontend, formulas, DTOs publicos, entidades, migrations, dependencias ou budgets. O working tree preserva a implementacao anterior do Dashboard e seu archive, sem staging/commit/push.

## Testes

Focados finais: 55/55 (AcaoServiceTest, AlphaVantageConsumptionTest, AlphaVantageAdapterTest, BrapiAdapterTest, AcaoCotacaoPersistenceServiceTest). Cobertura inclui idade/fronteira, timestamp ausente, sucesso exato, repeticao, concorrencia, resposta persistida frente a entidade antiga, timeout, rate limit, indisponibilidade, expiracao de cooldown e BRAPI sem alteracao. Mocks/stubs sem API real. Expectativas antigas de metodo do mock de atualizacao ajustadas ao novo caminho; validacoes de cadastro preservadas.

Suite completa: relatorios Surefire 459 testes, 77 classes, zero falhas/erros/skips. Uma execucao redirecionada pelo PowerShell retornou NativeCommandError pelo aviso JVM em stderr apesar de relatorios aprovados; confirmacao final por cmd com captura direta do exit code registrada abaixo.

Warnings conhecidos backend: JVM class sharing, open-in-view, endpoints SpringDoc habilitados e violacoes SQL esperadas em testes negativos. Nao alterados por otimizacao. Frontend: resultados 152/152, 485/485 e production anteriores reutilizados, sem nova execucao.

## Resultado final

Confirmacao final: mvnw test terminou com exit code 0 e BUILD SUCCESS, 459/459 em 77 classes. Focados 55/55. Strict da change aprovado e global 34/34; diff check sem erros, apenas avisos LF/CRLF nos arquivos existentes do Dashboard/specs promovidas. 5/5 tasks concluidas. Nenhuma pendencia tecnica identificada; aguarda revisao do usuario, nova change nao arquivada. Git permanece sem staging e com alteracoes das duas changes preservadas.
