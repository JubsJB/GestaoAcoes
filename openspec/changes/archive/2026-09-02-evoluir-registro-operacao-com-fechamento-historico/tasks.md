## 1. Contrato discriminado — testes de binding primeiro

- [x] 1.1 Criar testes diretos de binding Jackson para COMPRA sem preço e VENDA com preço válido antes de consolidar a hierarquia DTO.
- [x] 1.2 Testar `tipo` ausente, nulo, desconhecido e em caixa incorreta, todos como `400 REQUEST_INVALIDO` no contrato HTTP.
- [x] 1.3 Testar `precoUnitario` e `precoUnitario:null` em COMPRA, ambos rejeitados em vez de ignorados.
- [x] 1.4 Testar VENDA sem `precoUnitario`, `ordemNoDia` em COMPRA/VENDA e campo desconhecido em ambas as variantes.
- [x] 1.5 Implementar classe base abstrata e classes concretas finais de COMPRA/VENDA com `EXISTING_PROPERTY`, `visible=true`, nomes exatos case-sensitive e sem records.
- [x] 1.6 Manter rejeição explícita herdada por `@JsonAnySetter`, sem desabilitar desconhecidos, sem `precoUnitario` em COMPRA e sem `ordemNoDia` em qualquer request.
- [x] 1.7 Adaptar resource, mapper e validações para o contrato discriminado, preservando `OperacaoResponse` completo e sem validação condicional frágil no controller.
- [x] 1.8 Preservar testes de ticker/mercado, quantidade, data futura, precisão e campos controlados pela aplicação.
- [x] 1.9 Testar `corretoraId` omitido, nulo, existente e inexistente conforme o contrato vigente.

## 2. Capability histórica — contrato e erros

- [x] 2.1 Criar testes do contrato `CotacaoHistoricaProvider`/`CotacaoHistoricaData` para ticker, data exata e close positivo/representável.
- [x] 2.2 Criar `CotacaoHistoricaProvider` e `CotacaoHistoricaData` separados de `CotacaoProvider` e `HistoricoCotacao`.
- [x] 2.3 Implementar seleção única de provider histórico por `Mercado` e validação comum de ticker, data exata e close.
- [x] 2.4 Adicionar `COTACAO_HISTORICA_INDISPONIVEL` e `HISTORICO_COTACAO_FORA_DO_ALCANCE` ao tratamento padronizado sem expor credenciais.
- [x] 2.5 Testar ausência de provider/configuração, ticker inexistente, limite, timeout, indisponibilidade e resposta inválida somente com stubs/fixtures locais.

## 3. BRAPI histórica — fixtures antes do adapter

- [x] 3.1 Criar fixtures/testes para resultado do ticker esperado, ticker divergente, ticker ausente e múltiplos resultados incompatíveis.
- [x] 3.2 Criar fixtures/testes para `historicalDataPrice` ausente, nulo e vazio.
- [x] 3.3 Criar fixtures/testes para candle sem data, data inválida, candle de outra data e candle exatamente da data.
- [x] 3.4 Criar fixtures/testes para close ausente, não numérico, zero, negativo e válido, além de `adjustedClose` divergente ignorado.
- [x] 3.5 Testar error mapper/HTTP, 429, timeout, indisponibilidade e payload inválido sem rede real.
- [x] 3.6 Implementar adapter histórico BRAPI reutilizando `brapiRestClient`, Bearer, configuração, timeouts e mapeamento comum.
- [x] 3.7 Montar `GET /api/v2/stocks/historical` com `symbols`, `startDate=endDate=dataOperacao` e `interval=1d`.
- [x] 3.8 Selecionar inequivocamente ticker e candle exatos, usar somente `close` bruto e nunca aplicar fallback, `adjustedClose` ou outro pregão.

## 4. Alpha Vantage histórica — fixtures antes do adapter

- [x] 4.1 Criar fixture/teste de chave exata válida retornando exclusivamente `4. close` bruto.
- [x] 4.2 Criar fixture/teste de data ausente entre menor/maior válidas produzindo `422 COTACAO_HISTORICA_INDISPONIVEL`.
- [x] 4.3 Criar fixture/teste de data anterior à menor com pelo menos 100 candles válidos/distintos produzindo `422 HISTORICO_COTACAO_FORA_DO_ALCANCE`.
- [x] 4.4 Criar fixture/teste de data anterior à menor com menos de 100 candles válidos produzindo `502 RESPOSTA_EXTERNA_INVALIDA`.
- [x] 4.5 Testar chave/data inválida, data duplicada detectável, close ausente, não numérico, zero ou negativo e estrutura malformada como 502.
- [x] 4.6 Testar `Note` e `Information` inequivocamente de rate limit como 429.
- [x] 4.7 Testar `Error Message` inequivocamente de ticker/símbolo inválido como 404 e mensagens não classificáveis como 502.
- [x] 4.8 Testar `Time Series (Daily)` ausente/vazia, timeout e indisponibilidade sem rede real.
- [x] 4.9 Implementar adapter histórico Alpha Vantage reutilizando `alphaVantageRestClient`, API key configurada, timeouts e mapeamento comum sem registrar a chave.
- [x] 4.10 Consultar exclusivamente `TIME_SERIES_DAILY` com `symbol`, `outputsize=compact` e `apikey`, sem `TIME_SERIES_DAILY_ADJUSTED` ou `GLOBAL_QUOTE`.
- [x] 4.11 Implementar a contagem objetiva de candles válidos/distintos e exatamente as quatro classificações de data aprovadas, sem heurística adicional.

## 5. Orquestração e transação curta

- [x] 5.1 Implementar no orquestrador não transacional a ordem fixa: binding/Bean Validation, ticker+mercado, Carteira, Ação, Corretora, quantidade, data, preço de VENDA, histórico de COMPRA e colaborador transacional.
- [x] 5.2 Testar que Carteira/Ação/Corretora inválidas falham antes do provider e que somente COMPRA consulta provider.
- [x] 5.3 Criar colaborador Spring separado com método público `@Transactional`, evitando self-invocation e garantindo que HTTP termine antes da transação/lock.
- [x] 5.4 Dentro da transação, adquirir lock da Carteira e recarregar/confirmar Ação por ticker+mercado e Corretora opcional antes de cálculos mutáveis.
- [x] 5.5 Revalidar explicitamente referências e invariantes mutáveis sob lock, sem confiar nas leituras preliminares.
- [x] 5.6 Adicionar query `MAX(ordemNoDia)` por Carteira+Ação+data e gerar 1 ou `MAX+1` após o lock, protegendo `Integer.MAX_VALUE` antes da soma.
- [x] 5.7 Calcular `valorTotal`, carregar histórico completo, inserir a candidata conceitualmente e executar replay integral antes de persistir.
- [x] 5.8 Rejeitar qualquer prefixo/estado negativo na candidata ou em Operação posterior e persistir somente com `saveAndFlush` após replay válido.
- [x] 5.9 Mapear colisão inesperada de `uk_operacao_carteira_acao_data_ordem` para `409 INTEGRIDADE_DADOS_VIOLADA`, sem orientar alteração de `ordemNoDia`.
- [x] 5.10 Testar falha externa sem escrita nem lock durante rede, valor total exato, ordem 1, `MAX+1`, overflow, retroatividade e anexo ao fim do mesmo dia.

## 6. Concorrência e compatibilidade de dados

- [x] 6.1 Testar duas Operações concorrentes financeiramente válidas da mesma Carteira+Ação+data com provider stubado e nenhuma rede real.
- [x] 6.2 No teste anterior, provar duas conclusões com sucesso, exatamente duas linhas, conjunto de ordens `{1, 2}`, nenhuma duplicidade e replay final válido.
- [x] 6.3 Preservar a regressão de vendas concorrentes incompatíveis, provando que somente Operações compatíveis persistem e nenhum prefixo do replay fica negativo.
- [x] 6.4 Preservar o teste concorrente de criação de Operação versus exclusão da Carteira.
- [x] 6.5 Testar a unique constraint como última defesa em cenário controlado, sem tratá-la como mecanismo principal.
- [x] 6.6 Testar que submissões deliberadamente repetidas são novas Operações, sem idempotency key, igualdade de payload ou retry automático.
- [x] 6.7 Testar que Operações existentes não têm preço recalculado, ordem renumerada nem consulta histórica retroativa.

## 7. OpenAPI — documento efetivo separado do binding

- [x] 7.1 Anotar a hierarquia para `oneOf`/discriminator e `additionalProperties=false`, mantendo schemas concretos coerentes com o binding.
- [x] 7.2 Testar o JSON efetivo de `/v3/api-docs` para `oneOf` COMPRA/VENDA e discriminator `tipo`, sem confiar apenas nas annotations.
- [x] 7.3 Testar no JSON OpenAPI que COMPRA não expõe `precoUnitario`, VENDA o marca como required e ambos omitem `ordemNoDia` e proíbem propriedades adicionais.
- [x] 7.4 Testar que `OperacaoResponse` contém `precoUnitario`, `ordemNoDia` e `valorTotal`.
- [x] 7.5 Testar documentação dos dois 422 e de 404/429/502/503/504 relevantes, com dependência externa descrita somente para COMPRA.
- [x] 7.6 Confirmar que exemplos/schemas não expõem API keys e que consultas de Operação continuam independentes de providers.

## 8. Regressões e validação final

- [x] 8.1 Executar regressões de `CalculadoraPosicao`, posição atual, resultado realizado, consultas e histórico de Operações.
- [x] 8.2 Executar regressões de patrimônio, evolução e snapshots afetados indiretamente pelos valores persistidos.
- [x] 8.3 Confirmar por teste/inspeção que `HistoricoCotacao` não é usado como OHLC nem recebe candles de COMPRA.
- [x] 8.4 Executar testes focados de binding/resource, providers, adapters, service, repository e concorrência sem chamadas reais.
- [x] 8.5 Executar a suíte backend completa.
- [x] 8.6 Validar Liquibase/Hibernate, confirmando ausência de migration nova e integridade do changeSet `004`.
- [x] 8.7 Executar validação OpenSpec strict da change e validação global strict.
- [x] 8.8 Revisar que `frontend/**`, changes arquivadas, dados existentes e configurações de segredos permanecem inalterados.
