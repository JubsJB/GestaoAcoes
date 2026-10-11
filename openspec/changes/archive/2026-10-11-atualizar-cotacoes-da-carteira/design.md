## Context

Decisões aprovadas nesta conversa encerraram DATA × INSTANTE: tipos diferentes falham, preservando preço, referência e obtenção. Implementação atual consultada em dev: cadastro inicial externo; GETs locais; PATCH dedicado; replay oficial; cooldown EUA por instância; Dashboard com quatro GETs. Não há decisão financeira pendente.

## Goals / Non-Goals

Entregar atualização manual das posições abertas da Carteira pessoal, precisão temporal honesta e sucesso parcial. Preservar finanças, contexto/shell, layout simplificado, individual e cotação global. Exclusões são as da proposal; coleta automática, processamento institucional e jobs não fazem parte desta evolução.

## Decisions

### Modelo temporal e API

#### Domínio de novas aquisições aprovado em 2026-10-10

CotacaoCandidata SHALL centralizar a validação compartilhada por cadastro, PATCH e lote. DATA SHALL representar calendário gregoriano real, ano 0001–9999 exatamente com quatro dígitos; ano zero, negativo ou expandido é inválido. MUST NOT aplicar piso financeiro 1970, comparação futura com obtenção, Date, timezone ou calendário de mercado a DATA. DATA posterior à obtenção permanece válida.

INSTANTE SHALL validar calendário, hora e offset, normalizar para UTC preservando o instante real e exigir representação exata em microssegundos. Piso inclusivo: 1970-01-01T00:00:00Z. SHALL exigir referenciaInstante <= cotacaoObtidaEm da MESMA candidata, comparando instantes reais e offsets equivalentes, com tolerância ZERO. Divergência positiva de relógio do provider é rejeitada conservadoramente; nenhuma tolerância arbitrária, arredondamento ou truncamento é permitida. Referência fornecida inválida SHALL produzir erro temporal, nunca DESCONHECIDA; ausência legítima no cadastro mantém DESCONHECIDA.

cotacaoObtidaEm tem validação própria de precisão exata em microssegundos e origem no Clock backend da aquisição. O piso da referência não é automaticamente aplicado à obtenção. GET, mapping, cooldown, reuso e navegador MUST NOT recapturar, reinterpretar ou comparar obtenção com relógio atual. dataHoraCotacao permanece legado e MUST NOT receber automaticamente os novos limites financeiros ou decidir referência/fallback.

#### Fronteira de leitura e registros anteriores

Validação de nova aquisição e validação estrutural de DTO persistido são fronteiras distintas. Novos limites de aquisição MUST NOT converter referência antiga em DESCONHECIDA, apagá-la, reconstruí-la no mapper, fabricar obtenção ou rejeitar silenciosamente registros anteriormente aceitos. Guards de leitura não podem ser endurecidos indiscriminadamente com regras financeiras de novas candidatas. A definição exata da aceitação estrutural anterior deve ser investigada antes de alterar guards; qualquer decisão adicional necessária exige parada e apresentação ao usuário. Nenhum saneamento de dados ou acesso ao banco real está autorizado nesta etapa.

Apresentação SHALL preservar DTOs/textos originais lossless: DATA civil sem Date/deslocamento; INSTANTE local ao timezone do navegador, com offsets incluindo segundos e frações sem avanço indevido de minuto; DESCONHECIDA explícita sem fallback; obtenção separada e nula como "Não informada". Verificação automatizada deve abranger quatro superfícies, UTC, America/Los_Angeles e Pacific/Kiritimati, incluindo fronteiras de calendário, piso, igualdade, diferença de um microssegundo, offsets equivalentes e viradas locais de dia/ano. Validação visual/assistiva final continua reservada à task 12.1.

Ação e HistoricoCotacao recebem referenciaTipo (INSTANTE/DATA/DESCONHECIDA), referenciaInstante (OffsetDateTime UTC/timestamp com timezone nullable), referenciaData (LocalDate/DATE nullable) e cotacaoObtidaEm (OffsetDateTime UTC nullable no legado). Tipo deve ser obrigatório após backfill DESCONHECIDA. CHECK impõe INSTANTE somente com instante, DATA somente com data, DESCONHECIDA sem ambos. Provider retorna referência tipada; serviço captura relógio ao receber informação válida.

AcaoResponse e PosicaoResponse adicionam referenciaMercadoCotacao {tipo, instante, data} e cotacaoObtidaEm; preservam todos os campos antigos. Decimais preservam políticas atuais e lossless nas finanças frontend. dataHoraCotacao permanece obrigatório, documentado como legado e fora de ordenação/reuso/apresentação real; valores antigos não são reinterpretados. Escritas aceitas mantêm compatibilidade legada com instante de mercado em INSTANTE ou obtenção em DATA. Isso não inventa horário de mercado: o campo legado é explicitamente misto e descontinuável somente em evolução separada.

Cadastro com identidade e preço válidos preserva possibilidade de referência DESCONHECIDA quando o provider não fornece referência; obtenção inicial é conhecida e histórico inicial é atômico. Atualizações, tanto sobre desconhecido como conhecido, exigem candidata confiável. Sem referência não há substituição do preço armazenado.

### Legado e baseline inicial

cotacaoObtidaEm significa "Instante em que a observação de cotação atualmente armazenada foi obtida validamente pela aplicação", não o maior instante histórico de consulta da Ação. A monotonicidade é local à mesma observação (mesma referência e mesmo preço). Baseline inicial ou referência posterior aceita substitui a observação como unidade: preço, tipo, referência e obtenção própria da candidata. MUST NOT usar max(obtenção atual, candidata) entre observações diferentes, nem obtenção para ordenar mercado ou desempatar DATA × INSTANTE. Na mesma observação, preencher obtenção nula ou avançar somente se posterior; anterior/falha preservam o estado.

Exemplo de chegada fora de ordem: estado DATA 2026-10-01, preço 100, obtenção 15:05Z; candidata DATA 2026-10-02, preço 102, obtenção 15:04Z. A regra aceita a referência posterior com preço 102 e obtenção 15:04Z. 15:05Z pertence à observação substituída e não deve ser associado à nova. Essa decisão pura não confirma persistência: classificação ATUALIZADA indica aceitação pretendida, a ser confirmada somente pela futura transação; efeitos internos distinguem substituição integral, avanço isolado de obtenção e preservação, sem criar status públicos novos.

O resultado interno imutável Decisao valida sua matriz no construtor: ATUALIZADA/SUBSTITUIR_OBSERVACAO somente com REFERENCIA_INICIAL_ESTABELECIDA ou REFERENCIA_POSTERIOR; SEM_NOVA_COTACAO/PRESERVAR_OBSERVACAO somente com REFERENCIA_ANTERIOR ou MESMA_OBSERVACAO; SEM_NOVA_COTACAO/AVANCAR_SOMENTE_OBTENCAO somente com MESMA_OBSERVACAO; FALHA/PRESERVAR_OBSERVACAO somente com CONFLITO_NA_MESMA_REFERENCIA, TIPO_REFERENCIA_INCOMPATIVEL ou REFERENCIA_TEMPORAL_INVALIDA. Enums nulos e combinações incompatíveis são rejeitados. Avanço exige obtenção; preservação admite obtenção legada nula.

SUBSTITUIR_OBSERVACAO carrega a própria CotacaoCandidata decidida em candidataParaSubstituicao, exigindo referência confiável e obtenção resultante equivalente à sua obtenção. A candidata é um record composto exclusivamente de valores imutáveis já validados (incluindo BigDecimal, LocalDate e OffsetDateTime); reutilizá-la evita duplicar validações ou depender de outro argumento do chamador. Outros efeitos rejeitam candidata de substituição. A futura etapa 3B poderá consumir preço, tipo, referência e obtenção diretamente deste vínculo, sem recalcular a regra ou misturar candidatas; isso não autoriza modificar identidade/nome nem dispensa releitura protegida e nova decisão sobre o estado corrente previstas para concorrência. Decisao não carrega Acao mutável nem executa persistência; ATUALIZADA permanece intenção.

Migration aditiva mantém preço, campo legado e observações existentes. Sem evidência confiável, tipo DESCONHECIDA, ambos os campos de mercado e obtenção nulos. Não inferir timestamp pelo mercado. Primeira candidata válida confiável estabelece preço/referência/obtenção e histórico; ATUALIZADA com motivo REFERENCIA_INICIAL_ESTABELECIDA sem afirmar posterioridade ao legado. Após baseline, somente comparação de mesmo tipo.

### Algoritmo compartilhado

1. Localizar identidade persistida; para EUA, avaliar cooldown ativo (falha ou sucesso) antes de reuso válido e somente então permitir nova consulta, conforme precedência abaixo.
2. Consultar provider fora de transação e validar identidade/preço/precisão/referência.
3. Capturar instante de obtenção; adquirir lock da Ação em transação curta de bean separado e reler estado.
4. DESCONHECIDA + confiável: estabelecer baseline. Mesmo tipo posterior: aceitar mesmo com preço igual. Igual e mesmo preço em nova consulta real válida: SEM_NOVA_COTACAO, sem alterar cotacaoAtual, referência ou dataHoraCotacao legado e sem histórico; cotacaoObtidaEm SHALL avançar para o instante dessa obtenção somente se posterior ao persistido (ou preencher obtenção ainda nula). Comparar obtenção com o estado relido sob lock; instante igual ou anterior preserva o valor. Trata-se de nova obtenção da mesma observação, não nova cotação. Reuso e cooldown MUST NOT atualizar obtenção, preço/referência ou histórico. Anterior: SEM_NOVA_COTACAO, preserva preço/referência/obtenção. Igual com preço diferente: FALHA temporal.
5. Tipos diferentes: FALHA TIPO_REFERENCIA_INCOMPATIVEL, sem DATA para meia-noite nem INSTANTE para DATA, sem alterar obtenção. Falta temporal/preço/identidade/erro externo: FALHA; preservar estado.
6. Avanço isolado de obtenção na igualdade mantém SEM_NOVA_COTACAO e não cria observação nem conta ATUALIZADA. Em aceitação de baseline inicial ou referência posterior persistir estado e observação atomicamente; erro reverte somente esse ativo. Resultado interno inclui classificação, motivo, estado persistido e origem (HTTP/reuso/cooldown) suficiente para atribuir persistência à execução.

Individual mantém PATCH sem body funcional, AcaoResponse e erros externos vigentes. Novos erros de validação/conflito temporal usam 422 padronizado com código específico a definir tecnicamente. Lote converte erros por ativo em FALHA pública apropriada. Sem implementação temporal paralela.

### Núcleo de aplicação e falhas residuais de integridade (decisão aprovada em 2026-10-04)

AplicacaoCotacaoService recebe somente id e candidata previamente obtida/validada. Em bean transacional separado com REQUIRED e sem transação envolvente no futuro orquestrador, usa o lock pessimista já disponível por Ação, seguido de refresh protegido para evitar estado antigo no contexto JPA. Decide sobre esse estado e aplica exatamente o efeito. Substituição usa somente a candidata vinculada, sincroniza o legado pela política aprovada e grava a nova observação aceita; avanço altera só obtenção; preservação/falha semântica não escreve. O resultado é um snapshot imutável, entregue ao chamador sem transação envolvente somente após commit do proxy; não constitui novo DTO público. A leitura protegida deve preceder qualquer aplicação: resultado calculado antes da rede não é reutilizável.

Violação residual de uk_historico_cotacao_acao_instante ou uk_historico_cotacao_acao_data é falha de integridade da persistência, não resultado da RegraDecisaoCotacao. Qualquer falha de escrita/commit causa rollback integral de estado e histórico; a exceção de integridade propaga para fora da fronteira transacional. Não converter em SEM_NOVA_COTACAO, ATUALIZADA ou preservação sem erro; não executar retry/segunda consulta, remover/sobrescrever histórico nem fabricar referência. A futura integração classificará publicamente a falha após rollback; o atual handler 409 INTEGRIDADE_DADOS_VIOLADA não é fixado como contrato futuro nesta etapa. A regra pura permanece inalterada. Esta decisão explicita o tratamento técnico dentro dos requisitos existentes de integridade/atomicidade, sem novo status semântico.

### Providers e consumo

BRAPI atual /api/quote/{ticker}: regularMarketPrice e regularMarketTime, instante parseável normalizado UTC; número em segundos conforme contrato atual ou texto com offset, sem fallback de mercado para Clock. Alpha Vantage atual GLOBAL_QUOTE: 05. price e 07. latest trading day, LocalDate exata; não descartar por OffsetDateTime. Cadastro mantém SYMBOL_SEARCH + GLOBAL_QUOTE, espaçamento 1100 ms configurável e nenhuma OVERVIEW.

Reuso EUA usa cotacaoObtidaEm válida do estado preservado, não futura e idade menor que intervalo padrão 15m; baseline desconhecida não bloqueia a primeira recuperação confiável por reuso legado. Cooldown usa conclusão de tentativa, padrão 15m, compartilhado por ticker entre individual/lote na instância. Falha não altera obtenção válida. Resposta anterior pode alimentar tentativa/cooldown, não obtenção da cotação preservada. Reuso/cooldown de sucesso é SEM_NOVA_COTACAO com motivo próprio; erro reutilizado é FALHA. Atualizada por outra execução não é contada novamente. Timeout conexão/leitura preserva configuração existente. Sem novo sleep de atualização/retry/agendamento; BRAPI sem novo cache.

O controle operacional de tentativa/cooldown permanece em memória, local à instância, não é persistido no banco e pode ser perdido em restart. Não representa observação de mercado: registrar ou reutilizar esse controle MUST NOT alterar cotacaoObtidaEm, preço/referência ou criar HistoricoCotacao. Sem Redis, cache distribuído, tabela de tentativas ou nova infraestrutura.

Precedência EUA preservada de AcaoService.atualizarCotacao, sob coordenação local e releitura:
1. Cooldown ativo da tentativa concluída: se houve falha, devolver a falha vigente (FALHA no lote); se houve sucesso, compartilhar o snapshot imutável da tentativa concluída com SEM_NOVA_COTACAO e COOLDOWN_SUCESSO, conforme a confirmação humana abaixo. São ramos mutuamente exclusivos da mesma tentativa. Não executar HTTP nem gravar cotação/obtenção/histórico, mesmo havendo obtenção recente.
2. Sem cooldown ativo, se referência conhecida e obtenção válida, não futura e com idade menor que quoteReuseInterval, devolver SEM_NOVA_COTACAO com REUSO, sem HTTP ou escrita.
3. Sem cooldown ativo nem estado reutilizável, permitir nova consulta explícita ao provider. No limite exato de cada intervalo, ele está expirado; cooldown expirado não força HTTP se ainda houver reuso válido. Após restart, perder cooldown não elimina a obtenção persistida nem dispensa essa verificação de reuso.

COOLDOWN_SUCESSO compartilha, no individual e no lote, o snapshot imutável da tentativa concluída que originou o cooldown. Esse snapshot representa aquela tentativa, sem garantir ser o estado persistido mais recente se outra instância atualizar a Ação depois. Não há releitura adicional apenas para atualizar o estado apresentado. Apenas uma nova consulta real pode avançar obtenção pela regra de igualdade; a manutenção do controle de tentativa é independente disso.

### Lote, HTTP e transações

POST /carteiras/{carteiraId}/atualizacoes-cotacoes sem request body, conforme a decisão da preparação da Etapa B abaixo. Path orientado à Carteira segue as ações financeiras existentes e evita enviar IDs arbitrários. Backend valida Carteira e captura conjunto pelo replay oficial em leitura consistente, deduplica acaoId, exclui zero e cadastradas sem posição, ordena id e fecha captura antes da rede. Seleção reutiliza/extrai replay sem depender de calcular avaliação monetária; não duplica fórmula. Conjunto não muda se posição fecha depois.

Orquestrador sem transação única; processamento síncrono sequencial, cada Ação com transação independente em serviço separado. REQUIRED sem transação envolvente é suficiente; não capturar exceção dentro de contexto rollback-only para continuar lote. Rede não mantém lock de Carteira nem de Ação. Falha isolada é capturada após rollback e processamento segue. Unexpected error global pode interromper, mas não promete reversão de sucessos.

Response 200: carteiraId, resumo {atualizadas, semNovaCotacao, falhas}, resultados {acaoId,ticker,mercado,resultado,motivo,cotacao:estado temporal resultante,erro público quando aplicável}. Contadores somam itens e cada identidade aparece uma vez. Vazio é 200 sem providers/gravação; inexistente é 404; replay inconsistente falha global antes de ativos. Só há ATUALIZADA quando a execução aceitou/persistiu preço/referência. Estado de cotação é observado ao concluir o item, exceto COOLDOWN_SUCESSO, que apresenta o snapshot da tentativa concluída conforme a semântica acima; não há promessa de snapshot global de todo lote.

Confirmação humana da Etapa A: a semântica de COOLDOWN_SUCESSO acima vale igualmente para lote e individual, sem reabrir a task 10.1. Implementação autorizada nesta etapa limita-se à captura, orquestração compartilhada, serviço e resultados internos do lote e testes backend; endpoint, DTO HTTP, OpenAPI e frontend permanecem posteriores.

### Preparação da Etapa B — decisões humanas (2026-10-04)

Falhas globais inesperadas do fluxo de atualização de cotações da Carteira terão tratamento específico do lote: HTTP 500 com StandardError, código FALHA_ATUALIZACAO_COTACOES e mensagem pública "Não foi possível concluir o lote. Atualizações já confirmadas podem ter sido preservadas." Erros globais conhecidos mantêm seus contratos atuais. Não haverá handler genérico para toda a API, retry automático ou exposição de causa, stack trace, SQL, payload externo ou detalhes internos. A mensagem não promete rollback global.

A divergência temporal do cadastro inicial não será corrigida na Etapa B. Antes da conclusão integral desta change, uma etapa separada deverá integrar aquisição de referência confiável e cotacaoObtidaEm no cadastro, persistência correspondente em Acao e histórico inicial, e testes de cadastro/histórico. A Etapa B representará fielmente os metadados persistidos, inclusive DESCONHECIDA e obtenção nula; nenhum mapper fabricará, inferirá ou reconstruirá referência/obtenção a partir do legado. Não se reabre a task 10.1 nem o núcleo de refresh aprovado.

Decisão humana posterior: o POST do lote não aceita request body. Requisição sem body é válida; {}, arrays/listas de IDs, objetos com campos e demais conteúdos funcionais retornam 400 REQUEST_INVALIDO antes do lote, sem processar Ações, provider ou aplicação. Não há DTO de request vazio. Whitespace segue o comportamento normal da infraestrutura HTTP para ausência de conteúdo, sem regra de negócio por bytes. A seleção permanece exclusivamente backend pelo replay oficial.

### Concorrência e escopo global

Lock da Ação e reread protegem lote × lote, lote × individual, igualdade concorrente e resposta antiga tardia; histórico registra só estados aceitos. Para obtenções concorrentes da mesma referência/preço, a comparação sob lock impede uma execução atrasada de regredir cotacaoObtidaEm. Deduplicação EUA local não substitui lock banco. Repetição da mesma referência é praticamente idempotente no estado/histórico, mas HTTP externo pode ocorrer após cooldown. Acao é global: outras Carteiras usam preço novo na próxima leitura. Não modificar operações, quantidade, preço médio, custo, realizado, replay, prévia consultiva ou snapshots antigos. Novos snapshots continuam exclusivamente manuais; lote nunca cria snapshot.

### Migration planejada

Criar futuro changeSet adicional em acao/historico_cotacao e incluir master, sem editar antigos. Preservar NUMERIC(19,6), FK LAZY unidirecional sem cascade e data_hora_cotacao. Adicionar tipos e nullability citados; CHECKs coerentes. Substituir uk_historico_cotacao_acao_data_hora por unicidades (acao_id,referencia_instante) e (acao_id,referencia_data), validando múltiplos nulos em PostgreSQL/H2. DESCONHECIDA legado não é deduplicado artificialmente. Histórico só aceita observação inicial no cadastro ou baseline/posterior aceitos, sem observação de mesma referência, anterior, reuso ou falha. Sem índice redundante; índices únicos atendem acesso por Ação quando adequado. ddl-auto=validate mantido.

Rollback estrutural pode ser tecnicamente possível conservando legado, mas perde novas informações. Restaurar constraint antiga exige preflight de duplicidade; não prometer rollback automático universal. Rollout: migration antes do backend/DTO/frontend, publicar juntos consumidores que usam novos campos; manter legado para compatibilidade, sem backfill de mercado. Nenhuma migration é criada nesta fase.

### Frontend e acessibilidade

DashboardPageComponent adiciona Atualizar cotações ao bloco dashboard-actions sem remover Atualizar dados. DashboardService/modelos recebem POST e resultado. Usa CarteiraContextService e captura id. Enabled depende de posições abertas carregadas; ausência, carregamento sem conjunto ou lote em curso desabilitam sem esconder; texto associado explica ausência, aria-busy e região aria-live polite anunciam Atualizando cotações. Mesmo trocando seleção, bloqueia segundo disparo enquanto original corre.

Feedback discreto: sucesso total conta atualizadas; misto conta três categorias e identifica tickers com falha. Origem da Carteira permanece clara. Conclusão inclusive todas falhas restaura ação e recarrega resumo/posições/realizados/evolução da seleção corrente; resposta antiga não sobrescreve contexto novo. Falha de recarga não apaga resultado do lote. Conexão interrompida informa resultado não confirmado, não pressupõe cancelamento backend/rollback e não repete POST.

Referência agrega valor no desempenho por ativo, área de Ações e posições completas da Carteira; não precisa repetir na distribuição ou cards agregados. DATA usa formatador de data civil sem new Date deslocando dia; INSTANTE usa offset/timezone local; desconhecida texto explícito. Não classificar por calendário. Preservar responsividade, hierarquia, ordem, BRL/USD independentes e componentes financeiros lossless, sem tabela nova.

### Decisões rejeitadas

Rejeitados lote de PATCHs comandado pelo navegador, lista arbitrária do cliente, transação global, rede sob lock de Carteira, comparação por consulta, conversão artificial DATA/INSTANTE, remoção do legado, fallback de mercado ao relógio, histórico de tentativas, fila/polling/scheduler/paralelização/retry, cotação por Carteira e fórmulas novas.

## Risks / Trade-offs

Síncrono sequencial em Carteira pessoal pode atingir timeouts ou limites somados entre tickers; cooldown local não controla orçamento distribuído. Não ampliar para jobs ou rate limiter global nesta change. Requisição interrompida pode deixar sucessos confirmados sem response. Referências legadas são irrecuperáveis sem evidência; não inventar. Payload temporal pode faltar; atualização segura falha preservando última cotação. Tipos distintos falham até evolução específica de transição. GETs financeiros são independentes e podem observar instantes distintos; não inventar uma data agregada ou nova fórmula. Rollback estrutural perde metadados.

Títulos de cenários e requisitos existentes são preservados por proteção nominal OpenSpec 1.9.0, inclusive Fallback temporal e Preservar granularidade intraday; corpos passam a regra nova, sem duplicação nem promessa de rename futuro. Nenhum RENAMED/REMOVED é necessário.

## Compatibilidade e mapa normativo

Operação manual, prévia histórica, histórico desacoplado, Dashboard simplificado e shell/contexto permanecem. portfolio-position participa porque DTO e cláusula sem migration de cotação precisam ser reconciliados; frontend-position-analysis participa por novo contrato observável discreto (não apenas mudança de arquivo). frontend-portfolio-management e frontend-portfolio-composition não exigem delta: apresentação reutiliza contrato de posições; não altera distribuição. api-documentation remove somente contagem fixa no requisito pertinente e exige contagem real com endpoint novo, sem revisão geral de endpoints.

## Migration Plan

Após revisão e autorização: modelo/migration; providers; regra única/persistência; lote/resource; DTO/OpenAPI; frontend; testes direcionados e regressão; strict e simulação; build production/budgets e comparação de bundle/performance; atualização/verificação do Graphify após mudanças estruturais conforme AGENTS.md; revisão humana. Nenhuma task concluída antecipadamente. Archive e Git são etapas separadas.

## Inventário exato dos deltas

| Capability | Operação | Requisito | Motivo |
|---|---|---|---|
| alpha-vantage-consumption | MODIFIED | Reutilizacao temporal especifica EUA | Corrigir DATA e separar obtenção/reuso de tentativa/cooldown sem contar sucesso alheio. |
| alpha-vantage-consumption | MODIFIED | Consulta minima e deduplicacao | Corrigir DATA e separar obtenção/reuso de tentativa/cooldown sem contar sucesso alheio. |
| alpha-vantage-consumption | MODIFIED | Falha preserva estado e consumo | Corrigir DATA e separar obtenção/reuso de tentativa/cooldown sem contar sucesso alheio. |
| api-documentation | MODIFIED | Documento OpenAPI derivado da API real | Descrever API real com lote/schema aditivo e eliminar contagem histórica fixa incompatível. |
| api-documentation | ADDED | Documentação do lote e precisão temporal de cotação | Descrever API real com lote/schema aditivo e eliminar contagem histórica fixa incompatível. |
| frontend-dashboard-management | ADDED | Atualização explícita das cotações da Carteira | Novo comando manual e seus estados, feedback e recarga. |
| frontend-position-analysis | ADDED | Referência de mercado discreta no desempenho por ativo | Nova apresentação normativa observável discreta no desempenho, sem HTTP ou tabela. |
| frontend-stock-management | MODIFIED | Atualização manual de cotação somente no detalhe | Mostrar referência real e delimitar individual sem proibir lote manual do Dashboard. |
| frontend-stock-management | MODIFIED | Apresentação responsiva sem cálculo financeiro | Mostrar referência real e delimitar individual sem proibir lote manual do Dashboard. |
| portfolio-position | MODIFIED | Representação contábil e de mercado da posição | Adicionar contrato temporal ao response e reconciliar migration de cotação, sem alterar fórmulas. |
| portfolio-position | MODIFIED | Cotação persistida e referência temporal preservada | Adicionar contrato temporal ao response e reconciliar migration de cotação, sem alterar fórmulas. |
| portfolio-position | MODIFIED | Compatibilidade sem persistência adicional | Adicionar contrato temporal ao response e reconciliar migration de cotação, sem alterar fórmulas. |
| portfolio-quote-refresh | ADDED | Lote manual orientado à Carteira | Capability nova: seleção autoritativa, lote sequencial, parcial e efeitos preservados. |
| portfolio-quote-refresh | ADDED | Processamento sequencial com sucesso parcial | Capability nova: seleção autoritativa, lote sequencial, parcial e efeitos preservados. |
| portfolio-quote-refresh | ADDED | Resultado por ativo e HTTP proporcional | Capability nova: seleção autoritativa, lote sequencial, parcial e efeitos preservados. |
| portfolio-quote-refresh | ADDED | Preservar cotação global e finanças | Capability nova: seleção autoritativa, lote sequencial, parcial e efeitos preservados. |
| stock-quote-history | MODIFIED | Manter modelo histórico mínimo e unidirecional | Reconhecer DATA/INSTANTE e metadados aprovados; substituir unicidade legada sem histórico de tentativas. |
| stock-quote-history | MODIFIED | Preservar contrato físico do histórico no schema | Reconhecer DATA/INSTANTE e metadados aprovados; substituir unicidade legada sem histórico de tentativas. |
| stock-quote-history | MODIFIED | Registrar cada estado de cotação efetivamente persistido | Reconhecer DATA/INSTANTE e metadados aprovados; substituir unicidade legada sem histórico de tentativas. |
| stock-quote-history | MODIFIED | Preservar granularidade intraday e semântica temporal vigente | Reconhecer DATA/INSTANTE e metadados aprovados; substituir unicidade legada sem histórico de tentativas. |
| stock-quote-history | MODIFIED | Impedir duplicidade temporal por Ação | Reconhecer DATA/INSTANTE e metadados aprovados; substituir unicidade legada sem histórico de tentativas. |
| stock-quote-history | MODIFIED | Manter estado atual e histórico atomicamente consistentes | Reconhecer DATA/INSTANTE e metadados aprovados; substituir unicidade legada sem histórico de tentativas. |
| stock-registration | MODIFIED | Referência temporal da cotação | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | MODIFIED | Persistência e resposta do cadastro | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | MODIFIED | Validação da nova cotação e preservação da Ação | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | MODIFIED | Semântica temporal e cotação numericamente igual | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | MODIFIED | Concorrência e persistência final consistente | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | MODIFIED | Schema versionado e validado | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | ADDED | Comparações temporais e primeira baseline verificáveis | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |
| stock-registration | ADDED | Transição temporal aditiva e contratos preservados | Separar temporalidade, baseline e comparação compartilhada; preservar legado e contrato individual. |

REMOVED = 0; RENAMED = 0. Capabilities não listadas não requerem mudança normativa; regressão testa sua preservação.

### Integração individual e substituição do legado (2026-10-04)

Confirmação humana mantém os contratos existentes: cooldown ativo precede reuso por cotacaoObtidaEm, que precede consulta. Na integração individual, sucesso em cooldown compartilha o resultado imutável da tentativa concluída; não compara dataHoraCotacao com estado relido. DESCONHECIDA nunca se torna reutilizável pelo legado. Nova consulta usa candidata tipada e núcleo atômico existente; resposta individual usa snapshot após conclusão transacional. Falhas semânticas são expostas em 422 com códigos técnicos CONFLITO_NA_MESMA_REFERENCIA, TIPO_REFERENCIA_INCOMPATIVEL e REFERENCIA_TEMPORAL_INVALIDA, mantendo detalhes de cotação preservada. Falhas residuais de integridade propagam sem retry ou transformação em sucesso. Nenhuma nova política temporal ou funcional.

### Precisão temporal persistível — decisão da correção da Etapa C

A precisão temporal persistível de referenciaInstante e cotacaoObtidaEm SHALL ser de microssegundos: o instante deve ser exatamente representável com até seis casas decimais nos segundos. Casas excedentes exclusivamente zero SHALL ser aceitas sem perda; nanos significativos não representáveis SHALL ser rejeitados com 422 REFERENCIA_TEMPORAL_INVALIDA antes de disponibilizar CotacaoCandidata válida ou persistir estado/histórico. A mesma validação SHALL abranger referências externas e obtenção do Clock, em cadastro e atualização individual/lote. MUST NOT arredondar, truncar ou delegar ao banco a correção da precisão. UTC SHALL preservar o instante; LocalDate não é afetada.

A validação central fica na construção de CotacaoCandidata. A migration 007 permanece inalterada. Verificação direta em H2 2.4.240 confirmou DATETIME_PRECISION=6 para TIMESTAMP WITH TIME ZONE e arredondamento de entrada excedente; a aplicação impedirá esse arredondamento rejeitando entradas não representáveis. PostgreSQL não foi certificado nesta correção: a equivalência física permanece na task 11.2.

### Clock de aquisicao e feedback - correcao manual PETR4 aprovada em 2026-10-11

TimeConfig SHALL fornecer cotacaoAcquisitionClock com resolucao explicitamente configurada de1micro (Clock.tick(applicationClock,Duration.ofNanos(1000))),selecionado por qualificador no AcaoService. A quantizacao no relogio fonte foi aprovada nesta correcao;MUST NOT arredondar/truncar referencia de provider ou obtencao ja capturada em CotacaoCandidata/mapper/JDBC. applicationClock original SHALL continuar primary para consumidores financeiros/calendario/snapshot;Clock.fixed injetavel e validacao estrita da candidata permanecem. Contrato temporal e toleranciaZERO nao mudam.

Erro de atualizacao no detalhe SHALL conservar StandardError/NormalizedHttpError internos,mas nao renderizar raw details. SHALL explicar falha e preservacao confirmada em um unico alerta acessivel;MUST NOT afirmar preservacao no servidor sem confirmacao nem em resultado de transporte desconhecido. Ajuste SHALL ser local a pagina,sem alterar componente compartilhado ou contratos HTTP. Evidencias em correcao-petr4-precisao-feedback.md;tentativa original BRAPI nao recuperada,defeito do Clock comprovado separadamente.

### Binding civil pontual - alternativa A aprovada apos inventario

O inventario autorizado em localhost:5432/gestaoacoesdb, schema public, confirmou 5 Acoes (4 referencias DATA nulas, 1 preenchida) e 9 historicos (8 nulas, 1 preenchida). Minimo/maximo preenchidos: 2026-10-02 em ambas. Nenhum BC, outubro de 1582, ano fora de 0001-9999, infinito ou inconsistencia tipo/data. O usuario aprovou A; nao implementar versionamento C.

ReferenciaDataJdbcType SHALL ser aplicado por @JdbcType somente a Acao.referenciaData e HistoricoCotacao.referenciaData. Escrita usa JDBC 4.2 LocalDate/SQL DATE diretamente; leitura positiva e civil direta. Para BC PostgreSQL (ano Java <=0), leitura delega ao caminho java.sql.Date/LocalDateJavaType anterior, preservando valores e erros legados sem fallback. A deteccao e pelo dialect PostgreSQL, nao timezone/configuracao da maquina. H2 efemero usa leitura direta, sem migracao de bancos descartaveis. MUST NOT registrar conversor global, mudar SQL DATE, migration 007, outros DATE/timestamps ou o contrato temporal aprovado.

O inventario reduz a ambiguidade para o conjunto atual; nao prova origem ou compatibilidade universal de futuras importacoes/restauracoes. AD SQL 5-14/10/1582 de outro caminho continua exigindo decisao separada antes de importar. Os valores contemporaneos existentes permanecem identicos sem UPDATE/migration. Evidencias: implementacao-alternativa-a.md. A decisao abaixo registra historicamente o prototipo anterior retirado; seu bloqueio BC foi superado pela aprovacao de A, nao pela incorporacao daquele prototipo indiscriminado.

### Persistência civil e arquitetura H2 — decisão aprovada em 2026-10-10

PostgreSQL é o banco persistente oficial. H2 SHALL ser disponibilizado exclusivamente em testes automatizados efêmeros e recriáveis, sem política de migração para bancos H2 descartáveis. A inspeção confirmou dependência H2 em scope test, profile test em src/test/resources com generate-unique-name, somente URLs H2 mem nos testes, dev/Compose/configuração local PostgreSQL e ausência de configuração/arquivo de base H2 persistida no projeto. PRD foi alinhado à mesma decisão, removendo a permissão histórica de H2 em desenvolvimento.

O binding direto JdbcTypeCode(SqlTypes.LOCAL_DATE) foi avaliado somente para referenciaData de Acao e HistoricoCotacao, mantendo o tipo físico SQL DATE. JDBC 4.2 carrega LocalDate diretamente e evita a ponte java.sql.Date/calendário híbrido/timezone do sistema. O protótipo foi retirado após revelar incompatibilidade na leitura de datas BC do PostgreSQL aceitas pelo contrato anterior; nenhuma solução incompatível permanece nas entidades. Não alterar outras datas, schema, migration 007, precisão de INSTANTE/obtenção ou regras de aplicação/ordenação. Compatibilidade SHALL comparar registros PostgreSQL escritos pelo binding anterior com leitura futura, incluindo valores fora do domínio novo que podiam ser persistidos anteriormente. A estratégia para BC exige decisão humana antes da implementação.
