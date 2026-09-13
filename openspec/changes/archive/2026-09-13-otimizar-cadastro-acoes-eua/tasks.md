## Homologa??o manual aprovada e encerramento ? 2026-09-13

Evid?ncia fornecida pelo usu?rio: NVDA / EUA cadastrada com sucesso, cota??o correta em USD. SYMBOL_SEARCH retornou HTTP 200, fields=[bestMatches]; GLOBAL_QUOTE retornou HTTP 200, fields=[Global Quote]. Ambas tiveram classification=NO_RATE_LIMIT_MARKER, sem Note, Information ou ErrorMessage. Intervalos observados: aproximadamente 1,35 s e 1,26 s em duas tentativas.

- [x] Remover classe AlphaVantageDiagnostics, interceptor, atributos/UUID de diagn?stico e testes exclusivos de logs, preservando o intervalo configur?vel de 1100 ms, duas chamadas, aus?ncia de retry e erros reais.
- [x] Executar testes focados finais, OpenSpec strict change/global e git diff --check antes do archive.

A instrumenta??o mencionada nas evid?ncias hist?ricas abaixo foi tempor?ria e n?o integra a implementa??o final.

## 1. Cadastro EUA

- [x] 1.1 Remover fallback OVERVIEW preservando validações e erros existentes.
- [x] 1.2 Ajustar testes do adapter e contar requests HTTP desde POST /acoes para sucesso e falhas.

## 2. Verificação

- [x] 2.1 Executar testes focados do cadastro e regressão do consumo Alpha Vantage.
- [x] 2.2 Validar OpenSpec strict change/global e git diff --check, registrar resultados sem archive.

## 3. Correcao de burst comprovado na homologacao

- [x] 3.1 Inserir intervalo configuravel positivo, default 1100 ms, apenas entre busca validada e cotacao do cadastro EUA; preservar diagnostico, erros e duas chamadas.
- [x] 3.2 Testar intervalo com sleeper simulado, validacao, interrupcao e regressao focada; executar OpenSpec strict.

### Resultado da correcao de burst

- Causa posteriormente comprovada pelo usuario: GLOBAL_QUOTE respondeu HTTP 200/Information com limite de 1 request por segundo, cerca de 156 ms apos SYMBOL_SEARCH bem-sucedido. Esta evidencia substitui a conclusao inconclusiva da auditoria historica abaixo.
- Intervalo default de 1100 ms apos busca validada; configuravel por `integration.alpha-vantage.registration-interval-ms` / `ALPHA_VANTAGE_REGISTRATION_INTERVAL_MS`, inteiro positivo. Diagnostico mantido. Sem retry, terceira chamada ou mudanca na classificacao de limite diario.
- `.\mvnw.cmd "-Dtest=AlphaVantageAdapterTest,AcaoEuaRegistrationRequestsTest,AlphaVantageConsumptionTest" test`: 51 testes aprovados, zero falhas/erros/skips. Sleeper simulado confirma default e configuracao de 1700 ms antes da segunda request; teste HTTP de cadastro usa 1 ms. Cobertos interrupcao, configuracao invalida, nome ausente e 429/Note/Information sem retry.
- OpenSpec strict da change aprovado; validacao global strict: 36 itens aprovados. `git diff --check` aprovado.
- Nenhuma API real, Graphify, archive, staging, commit, push ou merge. Rebuild do backend necessario antes de uma tentativa manual NVDA.

## Evidências anteriores

- `AcaoEuaRegistrationRequestsTest`: 18 cenários com POST/controller/service/adapter reais, persistência mockada e HTTP simulado contado: sucesso NVDA/AAPL = 2; busca inválida/nome ausente = 1; cotação inválida/divergente = 2; duplicidade = 0; rate limit HTTP/Note/Information na busca = 1 e na cotação = 2, sem retry ou escrita.
- Comando: `.\mvnw.cmd "-Dtest=AcaoEuaRegistrationRequestsTest,AlphaVantageAdapterTest,AcaoServiceTest,AcaoResourceTest,AlphaVantageConsumptionTest" test`.
- Resultado: 79 testes, zero falhas/erros/skips, BUILD SUCCESS.
- `openspec validate otimizar-cadastro-acoes-eua --strict` aprovado; `openspec validate --all --strict`: 36 itens aprovados.
- `git diff --check`: sem erros (somente avisos de conversão LF/CRLF).
- Não houve chamada real ao provider, Docker, Graphify, archive, staging ou commit. Homologação NVDA/AAPL depende do backend atualizado e de cota disponível; 429 real permanece explícito.

## Auditoria de divergência em 2026-09-13

- POST /acoes chama AcaoResource.cadastrar -> AcaoService.cadastrar -> AlphaVantageAdapter.consultar, sem cache, cooldown ou retenção de falha. Uma nova tentativa após 429 volta a consultar imediatamente, com duas requests no sucesso.
- Cooldown de 15 minutos e reuso existem somente em atualizarCotacao (PATCH). O cooldown reapresenta status/código/mensagem da falha anterior, inclusive 429, mesmo se o provider recuperar antes. Isso corresponde à política de refresh vigente e não afeta POST. O mapa refreshAttempts é por instância e desaparece no reinício; cotações persistidas permanecem.
- O adapter gera 429 para HTTP 429, qualquer Note não vazio e Information contendo RATE LIMIT, CALL FREQUENCY, REQUESTS PER ou API CALL. A classificação textual é ampla; sem o payload da tentativa problemática não é possível determinar se houve falso positivo nem atribuir a divergência a ele.
- Inspeção somente leitura: nenhum container em execução; backend gestaoacoes-backend-1 parado. JAR copiado para pasta temporária; SHA-256 das classes AcaoResource, AcaoService e AlphaVantageAdapter coincide com target/classes atual. Adapter da imagem não contém OVERVIEW. Não houve start, rebuild ou alteração Docker.
- Sete novos cenários HTTP simulado: recuperação imediata após HTTP/Note/Information nas duas etapas e isolamento do POST mesmo com cooldown de refresh ativo no mesmo ticker. Suíte focada: 86 testes aprovados. Acrescentado depois cenário de recuperação do refresh exatamente aos 900 segundos; AlphaVantageConsumptionTest: 9 testes aprovados.
- Nenhuma alteração adicional de produção: cooldown e imagem desatualizada descartados como causa do POST no artefato inspecionado. Causa exata do incidente permanece não comprovada sem resposta/status externos daquela tentativa e confirmação de que a requisição chegou a este backend. Não foram feitas chamadas reais à Alpha Vantage.

## Verificacao final antes do archive

- 90 testes focados aprovados (AlphaVantageAdapterTest, AcaoEuaRegistrationRequestsTest, AlphaVantageConsumptionTest, AcaoServiceTest, AcaoResourceTest), zero falhas/erros/skips.
- OpenSpec strict change aprovado; global: 36 itens aprovados. git diff --check aprovado.
- Specs alpha-vantage-consumption e stock-registration sincronizadas e comparadas com os deltas; requisitos nao relacionados preservados.
- Sem chamadas reais nesta etapa, Graphify, staging, commit, push ou merge.
