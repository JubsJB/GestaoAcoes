## Why

Hoje dataHoraCotacao mistura referência de mercado e instante de consulta. GLOBAL_QUOTE fornece data de pregão, mas o adapter atual a descarta; nova consulta pode aparentar referência nova sem informação de mercado nova. O Dashboard relê indicadores, mas não oferece atualização explícita das cotações das posições abertas. É necessário separar referência, obtenção válida e tentativa, mantendo cotação como último preço conhecido validamente obtido, sem promessa de tempo real.

## What Changes

- Modelo temporal aditivo INSTANTE/DATA/DESCONHECIDA em Ação, histórico e responses; campo legado preservado.
- Baseline inicial confiável sobre legado desconhecido; comparação tipada compartilhada por individual/lote; tipos diferentes falham sem conversão artificial.
- POST /carteiras/{carteiraId}/atualizacoes-cotacoes, seleção backend pelo replay oficial, lote síncrono sequencial e sucesso parcial por ativo.
- Nova consulta real válida com referência e preço iguais continua SEM_NOVA_COTACAO e avança somente cotacaoObtidaEm quando a obtenção é posterior; não muda preço/referência nem cria histórico. Reuso/cooldown não avançam obtenção.
- cotacaoObtidaEm pertence à observação atualmente armazenada: monotonicidade somente na mesma referência/preço. Referência posterior aceita substitui a observação inteira com a obtenção própria da candidata, mesmo que anterior à obtenção da observação substituída; obtenção nunca ordena referências de mercado.
- Controle EUA separa obtenção/reuso de tentativa/cooldown em memória local à instância, perdido em restart e sem persistência; cooldown ativo precede reuso, que precede nova consulta permitida. Parsing correto de latest trading day.
- Dashboard oferece Atualizar cotações separado de Atualizar dados, feedback acessível e recarga final. Referência discreta por ativo, sem tabela adicional.
- Planejar migration Liquibase, contratos OpenAPI e testes, sem implementar nesta criação.

- Decisão da correção da Etapa C: referenciaInstante e cotacaoObtidaEm exatamente persistíveis em microssegundos; rejeição temporal existente para nanos significativos excedentes, sem arredondamento/truncamento ou correção pelo banco; zeros excedentes sem perda permitidos. Mesma fronteira de candidata para cadastro/PATCH/lote, incluindo Clock; DATA permanece civil.

## Capabilities

Decisão aprovada em 2026-10-10 para a correção do domínio temporal/F4: novas candidatas SHALL aceitar DATA somente como calendário gregoriano real, de 0001 a 9999, com ano de quatro dígitos, sem ano zero, negativo ou expandido, sem Date/timezone, piso 1970 ou limite futuro relativo à obtenção. INSTANTE SHALL ter piso inclusivo 1970-01-01T00:00:00Z e ser menor ou igual à cotacaoObtidaEm da própria candidata, por instante real considerando offset, com tolerância ZERO. Calendário, hora, offset e representação exata em microssegundos permanecem obrigatórios; normalização UTC não altera o instante, sem arredondamento/truncamento. Cadastro, PATCH e lote compartilham a fronteira CotacaoCandidata.

cotacaoObtidaEm SHALL continuar sendo capturada pelo Clock backend para a própria aquisição, exatamente em microssegundos, sem nova validação pelo relógio do navegador ou reinterpretação em GET/mapping/cooldown/reuso. dataHoraCotacao permanece legado sem os novos limites financeiros e sem fallback de referência. Registros anteriormente persistidos SHALL ser tratados separadamente da validação de aquisição: não converter/apagar/reconstruir referência, fabricar obtenção ou rejeitar silenciosamente leitura por limites novos. Não se introduz política EUA → timezone de mercado. Migration 007 e regras de ordenação, lock, atomicidade, histórico, cooldown/reuso e sucesso parcial permanecem fora da correção salvo incompatibilidade concreta submetida previamente ao usuário.

### New Capabilities

- portfolio-quote-refresh: lote autoritativo, transações independentes e resultado por ativo.

### Modified Capabilities

- stock-registration: temporalidade, regra individual e compatibilidade aditiva.
- stock-quote-history: modelo, observações aceitas, atomicidade e unicidade real.
- alpha-vantage-consumption: DATA, reuso por obtenção e atribuição de resultado nesta execução.
- portfolio-position: novos campos do response e compatibilidade com migration de Ação, sem fórmula nova.
- frontend-dashboard-management: comando manual e seus estados.
- frontend-stock-management: referência real e preservação da ação individual.
- frontend-position-analysis: novo contrato observável de metadado discreto no desempenho por ativo.
- api-documentation: endpoint/schema temporal e documento derivado da API real.

## Impact

Implementação futura afetará entidades/providers/services/DTOs/resources/mappers, migration aditiva, frontend e testes direcionados. Cotação continua global por Ação. Avaliações e novos snapshots manuais podem refletir o preço novo; operações e snapshots anteriores permanecem intactos. PRD/README não são alterados nesta etapa. Os deltas são planejamento, não declaração de funcionalidade já implementada.

## Non-goals

Decisões humanas da preparação da Etapa B (2026-10-04): falhas globais inesperadas do lote usarão tratamento específico com 500 StandardError, código FALHA_ATUALIZACAO_COTACOES e mensagem segura que preserve a possibilidade de sucessos anteriores confirmados; erros globais conhecidos mantêm seus contratos. A Etapa B limita-se à exposição HTTP e às extensões temporais públicas. A correção da aquisição/persistência temporal do cadastro inicial será uma etapa separada desta change, obrigatória antes de sua conclusão integral; os mappers não reconstruirão metadados ausentes.

Sem scheduler, polling, coleta automática, jobs/filas, paralelização, retry automático, autenticação, LGPD, OHLC, backfill, FX, classes de ativos, fórmulas novas, classificação de defasagem por calendário, BRAPI 502 × 422, limpeza geral README/PRD, remoção de dataHoraCotacao ou archive. Sem staging/commit/push/merge nesta etapa.
