## Context

O PRD define `Histórico de Cotação` apenas como entidade candidata (`id`, Ação, cotação e data/hora) e deixa a estratégia definitiva para a modelagem técnica. A spec promovida de `stock-registration` atualmente proíbe histórico adicional no cadastro e no PATCH, portanto esta change modifica explicitamente esse baseline, sem alterar os demais contratos.

Hoje `AcaoService` consulta o provider fora de transação, valida identidade, moeda, cotação e timestamp, e delega a gravação final. No PATCH, `AcaoCotacaoPersistenceService` abre transação, adquire `PESSIMISTIC_WRITE`, reaplica a regra monotônica e atualiza somente candidatas posteriores. BRAPI atende `BRASIL`; Alpha Vantage atende `EUA`. O timestamp confiável do provider é preferido e o `Clock` UTC é fallback.

`Acao.cotacaoAtual` usa `BigDecimal`/`NUMERIC(19,6)` positivo e `dataHoraCotacao` usa `OffsetDateTime`/`TIMESTAMP WITH TIME ZONE`. Liquibase controla o schema por changeSets 001–004 e Hibernate usa `ddl-auto=validate` em PostgreSQL e H2.

## Goals / Non-Goals

**Goals:**

- Preservar uma série temporal mínima das cotações efetivamente aceitas como estado atual.
- Garantir atomicidade entre `Acao` e seu histórico.
- Preservar a monotonicidade e a serialização concorrente atuais.
- Manter o histórico apto a futuras consultas por Ação e intervalo temporal.
- Evitar qualquer impacto de leitura sobre posição, patrimônio, resumo e resultado realizado.

**Non-Goals:**

- Endpoint de consulta nesta primeira fatia.
- Backfill, scheduler, coleta automática ou integração histórica adicional com providers.
- Snapshot da Carteira, evolução ou valuation histórico.
- Histórico cambial, eventos corporativos ou alteração das Operações.

## Decisions

### Modelo mínimo aprovado

Criar futuramente `HistoricoCotacao` e tabela `historico_cotacao` com:

- `id BIGINT` identity como chave primária;
- `acao_id BIGINT NOT NULL`, FK para `acao(id)`, sem cascade delete;
- `cotacao NUMERIC(19,6) NOT NULL` com check positivo;
- `data_hora_cotacao TIMESTAMP WITH TIME ZONE NOT NULL`;
- unique constraint `(acao_id, data_hora_cotacao)`.

O relacionamento aprovado é unidirecional do histórico para `Acao`, `ManyToOne LAZY`, sem coleção obrigatória em `Acao`. A chave única composta também fornece o índice de acesso com `acao_id` como prefixo, suficiente inicialmente para intervalo e ordenação ascendente ou descendente; um segundo índice seria redundante nesta fatia.

Não armazenar moeda, mercado, ticker, nome ou provider: são dados duplicados e o provider vigente é determinado pelo mercado. A alternativa de guardar origem é útil somente se o mesmo mercado puder usar múltiplas fontes ou auditoria de proveniência passar a ser requisito.

### Captura aprovada

Registrar o estado inicial do `POST /acoes` e cada candidata do PATCH que efetivamente vencer a regra `timestamp > dataHoraCotacao` dentro da seção serializada. Não registrar tentativas, respostas inválidas, candidatas stale ou duplicadas.

Isso produz a invariável: toda cotação persistida como estado atual possui exatamente uma observação histórica. A alternativa de iniciar somente no PATCH deixaria Ações sem representação do estado inicial e tornaria snapshots futuros incompletos.

### Temporalidade e idempotência aprovadas

Preservar granularidade intraday por timestamp, não agregação diária. `dataHoraCotacao` conserva a semântica atual: instante confiável da cotação fornecido pelo provider ou instante UTC de obtenção como fallback.

- Mesmo preço em timestamps posteriores: nova observação.
- Mesmo timestamp: nenhuma nova observação e nenhuma atualização.
- Timestamp anterior: nenhuma nova observação e nenhuma regressão.

Uma política diária descartaria informação que já existe no contrato e exigiria escolher arbitrariamente abertura, fechamento ou última observação do dia.

### Atomicidade do cadastro

Evoluir a responsabilidade transacional atualmente usada por `AcaoPersistenceService.saveUnique` para persistir a Ação, obter seu ID e inserir a observação inicial na mesma transação. Qualquer falha de FK, precisão, unicidade ou flush reverte ambos. Provider e validação continuam fora da transação.

### Atomicidade e concorrência do PATCH

Evoluir `AcaoCotacaoPersistenceService.atualizarSePosterior` para, sob o mesmo `PESSIMISTIC_WRITE` atual:

1. reler a Ação;
2. rejeitar silenciosamente a candidata igual ou anterior, preservando o `200` atual;
3. atualizar `Acao` quando posterior;
4. inserir a observação correspondente;
5. efetuar flush e commit atômicos.

O lock existente serializa a decisão monotônica. Se duas candidatas forem `t1 < t2` e `t2` vencer primeiro, `t1` não entra no histórico porque nunca foi estado persistido. Isso privilegia consistência com o estado atual, e não auditoria de toda resposta recebida do provider.

### Ausência de consulta pública aprovada

Não criar endpoint nesta change. Snapshot e evolução ainda não possuem contrato temporal aprovado; expor agora paginação, filtros ou semântica de datas cristalizaria decisões prematuras. O repository interno futuro deve permitir busca por Ação e intervalo ordenada por `dataHoraCotacao`, mas nenhum consumidor será implementado nesta fatia.

Quando uma consulta pública for proposta, a recomendação inicial é paginação obrigatória e filtro temporal opcional, devido ao crescimento append-only; isso pertence a outra change.

### Evolução de schema

Criar novo changeSet `005` no momento da implementação e incluí-lo no master. Não alterar 001–004. O mesmo changelog deve funcionar em PostgreSQL e H2 antes do `ddl-auto=validate`.

## Approved Decisions

As fontes normativas não resolviam estas decisões; todas as escolhas abaixo foram aprovadas explicitamente e são definitivas para esta change:

1. **Cadastro inicial gera histórico:** sim, para manter correspondência completa entre estado atual e série temporal.
2. **Todo PATCH válido gera histórico:** somente quando a candidata posterior é efetivamente persistida; PATCH stale continua idempotente sem insert.
3. **Granularidade:** intraday por timestamp, preservando informação existente.
4. **Timestamp:** provider confiável; fallback para instante UTC de obtenção, exatamente como hoje.
5. **Unicidade:** `(acao_id, data_hora_cotacao)`.
6. **Mesmo preço, timestamps diferentes:** dois registros.
7. **Mesmo timestamp:** nenhum segundo registro; timestamp igual não é posterior.
8. **Endpoint nesta change:** não.
9. **Paginação:** não aplicável sem endpoint; futura consulta deve ser paginada.
10. **Filtro por período:** não nesta change; repository deve permitir evolução futura.
11. **Backfill:** não.
12. **Concorrência:** reutilizar lock pessimista e registrar somente candidatas aplicáveis sob lock.
13. **Atomicidade:** obrigatória entre estado atual e histórico no POST e PATCH.
14. **Precisão:** `BigDecimal`/`NUMERIC(19,6)`, positiva e exata, igual a `cotacaoAtual`.
15. **Provider/origem:** não armazenar nesta fatia.

Alternativas rejeitadas: registrar toda resposta válida mesmo stale criaria auditoria de observações, mas quebraria a invariável de estados efetivamente persistidos; consulta REST imediata exigiria decidir volume e filtros antes do consumidor; armazenar provider duplicaria informação hoje determinada por mercado.

## Risks / Trade-offs

- [Sem backfill, Ações existentes não terão histórico anterior ao deploy] → aceitar explicitamente o corte temporal; não fabricar observações retroativas.
- [O lock pessimista reduz concorrência por Ação] → preservar a coordenação já aprovada e manter chamadas externas fora da transação.
- [Tabela append-only cresce continuamente] → unique/index por Ação e timestamp; paginação obrigatória quando houver endpoint.
- [Uma candidata antiga que termina depois da nova não é auditada] → documentar que o histórico representa estados aceitos, não todas as respostas externas.
- [Exclusão futura de Ação referenciada] → FK sem cascade; definir política de exclusão em capability própria.

## Migration Plan

1. Adicionar novo changeSet criando tabela, FK, check e unicidade.
2. Adicionar mapeamento e repository sem coleção em `Acao`.
3. Evoluir as duas fronteiras transacionais para gravar histórico atomicamente.
4. Validar H2 e PostgreSQL disponível com Hibernate `validate`.
5. Rollback remove somente `historico_cotacao`; como a change não executa backfill, os dados atuais de `acao` permanecem no formato anterior.

Este planejamento não executa migration nem implementa código; a execução futura depende do workflow de aplicação da change.
