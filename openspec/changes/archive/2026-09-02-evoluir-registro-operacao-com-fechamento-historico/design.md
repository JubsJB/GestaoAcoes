## Context

Ver [proposal.md](proposal.md) para a motivação. Hoje `OperacaoService.cadastrar` é um único método `@Transactional`: adquire `PESSIMISTIC_WRITE` na Carteira, valida um DTO uniforme, usa preço e ordem fornecidos, reproduz o histórico e persiste. `CotacaoProvider` e os adapters atuais atendem cadastro/atualização de cotação corrente; `HistoricoCotacao` registra essas observações e não representa candles OHLC.

O schema já contém `preco_unitario NUMERIC(19,6) NOT NULL`, `ordem_no_dia INTEGER NOT NULL`, `valor_total NUMERIC(38,12) NOT NULL`, índice cronológico e unique constraint `(carteira_id, acao_id, data_operacao, ordem_no_dia)`. A nova regra muda a origem dos valores de novos registros, não sua forma persistida.

## Goals / Non-Goals

**Goals:**

- representar e validar inequivocamente os requests distintos de COMPRA e VENDA;
- obter fechamento diário exato antes da seção transacional e persistir a COMPRA atomicamente;
- preservar lock, replay, constraints e cálculos existentes em uma transação curta;
- separar fechamento histórico de cotação corrente e mapear erros sem afirmar alcance que o provider não demonstrou;
- manter adapters testáveis sem rede real e OpenAPI fiel ao contrato.

**Non-Goals:**

- alterar frontend, expor endpoint público de candles ou persistir/cachear OHLC;
- recalcular Operações existentes, renumerar ordens ou criar migration;
- implementar idempotency key, retry automático, detecção de duplicidade por payload, reordenação manual ou `horaOperacao`;
- alterar o preço médio da posição atual ou introduzir preço médio acumulado;
- oferecer preço manual ou pregão anterior como fallback de COMPRA.

## Decisions

### 1. Request polimórfico discriminado por `tipo`

Usar uma classe base abstrata de request e duas classes concretas finais, `OperacaoCompraCreateRequest` e `OperacaoVendaCreateRequest`; não usar records. A classe base declara somente `carteiraId`, `ticker`, `mercado`, `corretoraId`, `tipo`, `quantidade` e `dataOperacao`. Apenas VENDA declara `precoUnitario`; nenhuma classe declara `ordemNoDia`.

Configurar a classe base com `@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tipo", visible = true)` e `@JsonSubTypes` para os nomes exatos e case-sensitive `COMPRA` e `VENDA`. O campo `tipo` permanece visível para binding/Bean Validation e cada classe concreta valida que seu valor corresponde à própria variante. Tipo ausente, nulo, desconhecido ou em caixa incorreta falha no binding/validação com `400 REQUEST_INVALIDO`.

Manter o padrão atual de rejeição explícita por `@JsonAnySetter` em um método herdado que sempre lança `IllegalArgumentException`. Como COMPRA não possui `precoUnitario`, inclusive `"precoUnitario": null` chega a esse rejeitador; `ordemNoDia` e qualquer outro campo desconhecido seguem a mesma regra. Não desabilitar `FAIL_ON_UNKNOWN_PROPERTIES` nem adicionar captura flexível de propriedades.

No springdoc, anotar a classe base com `@Schema(oneOf = {OperacaoCompraCreateRequest.class, OperacaoVendaCreateRequest.class}, discriminatorProperty = "tipo", additionalProperties = AdditionalPropertiesValue.FALSE)` e aplicar `additionalProperties = FALSE` também às classes concretas se a versão efetiva não herdar essa característica. O schema de COMPRA não declara `precoUnitario`; o de VENDA o declara em `required`; ambos omitem `ordemNoDia`. Testes separados de binding Jackson e do JSON efetivo de `/v3/api-docs` são a autoridade para impedir divergência entre runtime e documentação. Não haverá desserializador manual como fallback.

### 2. Orquestrador externo separado do serviço transacional

Manter o entry point de cadastro como orquestrador não transacional. O fluxo usa esta ordem fixa:

1. binding polimórfico e rejeição estrutural;
2. Bean Validation;
3. normalização e validação de ticker e mercado;
4. verificação preliminar da Carteira;
5. verificação preliminar da Ação por ticker normalizado e mercado;
6. verificação preliminar da Corretora opcional, aceitando ID omitido ou nulo;
7. validação de quantidade;
8. validação de `dataOperacao`;
9. validação de `precoUnitario` somente para VENDA;
10. seleção e consulta do provider histórico somente para COMPRA;
11. montagem do comando interno e chamada ao colaborador transacional.

As verificações preliminares de referências falham cedo e evitam consumo desnecessário do provider, mas não substituem as confirmações dentro da transação.

O colaborador terá método público `@Transactional` invocado através do proxy Spring. Dentro dele:

1. adquirir `PESSIMISTIC_WRITE` na Carteira pelo ID do comando;
2. confirmar/recarregar as referências persistentes necessárias;
3. confirmar que a Ação ainda existe e corresponde exatamente ao ticker normalizado e mercado usados pelo comando;
4. confirmar a Corretora quando o ID opcional foi informado, mantendo associação nula quando omitido ou nulo;
5. revalidar toda invariável mutável necessária à escrita, sem confiar nas leituras preliminares;
6. consultar `MAX(ordemNoDia)` para Carteira + Ação + data;
7. gerar 1 ou `MAX+1`, verificando o limite de `Integer` antes da soma para impedir overflow;
8. calcular `valorTotal` com `BigDecimal` e os limites vigentes;
9. carregar o histórico completo da Carteira+Ação;
10. inserir conceitualmente a candidata em `dataOperacao` e na ordem gerada;
11. executar replay integral na ordem cronológica;
12. rejeitar qualquer prefixo ou estado inválido, inclusive posição negativa na candidata ou em Operação posterior;
13. persistir com `saveAndFlush` e mapear violações conhecidas.

Nenhuma consulta externa ocorre nessa seção transacional.

Isso segue a separação já usada no projeto entre orquestração e persistence services e evita self-invocation: mover um método `@Transactional` para chamada privada/interna do mesmo `OperacaoService` não ativaria o proxy. Também evita `TransactionTemplate` no orquestrador e reduz acoplamento com infraestrutura transacional.

A confirmação anterior da Ação é read-only e não segura lock durante HTTP; o colaborador transacional deve recarregar as referências para fechar a janela TOCTOU antes da persistência. Nenhuma escrita ocorre antes da consulta externa.

### 3. Capability histórica própria

Introduzir conceitualmente:

```java
interface CotacaoHistoricaProvider {
    Mercado mercado();
    CotacaoHistoricaData consultarFechamento(String ticker, LocalDate data);
}

record CotacaoHistoricaData(String ticker, LocalDate dataPregao, BigDecimal close) {}
```

O orquestrador indexa providers históricos por `Mercado`, como já ocorre com providers correntes, mas sem alterar `CotacaoProvider`. A validação comum exige ticker correspondente, data exata e `close` positivo/representável. Não se adiciona repository de candles nem se usa `HistoricoCotacao`.

Preferir adapters históricos separados que reutilizem os `RestClient` qualificados, configuração, autenticação, timeouts e `ExternalApiErrorMapper`. Isso mantém cada adapter com uma responsabilidade explícita e evita tornar os adapters correntes uma interface de duas naturezas. Reuso interno de parsing/mapeamento pode ser extraído somente se reduzir duplicação sem mudar contratos existentes.

### 4. BRAPI usa janela de um único dia e somente `close`

O adapter brasileiro chama `GET /api/v2/stocks/historical` com `symbols`, `startDate` e `endDate` iguais a `dataOperacao` e `interval=1d`, usando autenticação Bearer já configurada. Seleciona inequivocamente o resultado do ticker esperado e aceita somente o candle cuja data corresponda exatamente à consulta. `adjustedClose` é ignorado mesmo se presente.

Ticker ausente/divergente, múltiplos resultados incompatíveis, `historicalDataPrice` ausente/nulo/vazio, candle sem data, data inválida, `close` ausente/não numérico/zero/negativo e estrutura malformada produzem `502 RESPOSTA_EXTERNA_INVALIDA`, salvo sinal externo inequívoco de ticker inexistente, limite, timeout ou indisponibilidade. Candle de outro dia nunca é usado como fallback.

Resposta vazia só vira `COTACAO_HISTORICA_INDISPONIVEL` quando a resposta permitir afirmar que a data está no alcance consultável. Caso o contrato/resposta do provider não permita distinguir data legítima sem pregão de limitação de alcance ou payload incompleto, o adapter não inventa um dos 422 e usa o erro existente tecnicamente justificável (`RESPOSTA_EXTERNA_INVALIDA`) com mensagem segura. Testes documentam essa limitação.

### 5. Alpha Vantage usa `TIME_SERIES_DAILY` compact

O adapter americano envia somente `function=TIME_SERIES_DAILY`, `symbol`, `outputsize=compact` e `apikey` pela configuração existente, e lê `Time Series (Daily) -> YYYY-MM-DD -> "4. close"`. Não usa `TIME_SERIES_DAILY_ADJUSTED` nem `GLOBAL_QUOTE`.

Um candle é válido para análise da janela somente quando a chave é parseável como `LocalDate`, sua data é distinta e `4. close` está presente, é numérico e maior que zero. Chave inválida, data duplicada detectável, close inválido ou estrutura necessária malformada tornam o payload `502 RESPOSTA_EXTERNA_INVALIDA`; candles inválidos não são descartados silenciosamente para alcançar a contagem.

A classificação é totalmente objetiva:

- chave exata presente com candle válido: retornar exatamente `4. close`;
- data entre a menor e a maior data válidas, mas sem chave exata: `422 COTACAO_HISTORICA_INDISPONIVEL`;
- data anterior à menor data válida e pelo menos 100 candles diários válidos com datas distintas: `422 HISTORICO_COTACAO_FORA_DO_ALCANCE`;
- data anterior à menor data válida e menos de 100 candles válidos distintos: `502 RESPOSTA_EXTERNA_INVALIDA`, pois a aplicação não distingue ativo recente, série parcial ou outra limitação.

O número mínimo de 100 candles válidos/distintos é a única evidência de janela compacta usada pelo código; “aproximadamente 100”, quantidade subjetivamente suficiente ou mera comparação com a menor data não são critérios de classificação.

Antes de interpretar a série, aplicar a política já usada pelo adapter corrente: `Note` ou `Information` inequivocamente de rate limit produz 429; `Error Message` inequivocamente de ticker/símbolo inválido produz 404; `Information` ou `Error Message` não classificável produz 502. `Time Series (Daily)` ausente/vazia sem outro sinal inequívoco e payload malformado também produzem 502.

### 6. Mapeamento de erros

Adicionar os códigos 422 históricos ao catálogo e ao handler/OpenAPI. Preservar o mapeamento comum de 404, 429, 502, 503 e 504. Payload vazio, estrutura ausente, datas inválidas ou `close` inválido são resposta externa inválida, salvo quando o provider fornece sinal inequívoco de ticker inexistente, limite ou indisponibilidade.

Mensagens e detalhes nunca incluem API key. Configuração ausente continua sendo indisponibilidade externa antes de qualquer chamada HTTP.

### 7. Ordem automática e concorrência

Adicionar query agregada de máximo filtrada por Carteira, Ação e data. Ela é executada somente após o lock pessimista da Carteira, que serializa registros concorrentes no mesmo agregado e preserva a proteção já usada para vendas concorrentes. `null` produz ordem 1; caso contrário, usa soma inteira exata com proteção contra overflow.

A unique constraint existente permanece a última defesa. Uma violação inesperada identificada pelo nome `uk_operacao_carteira_acao_data_ordem` será convertida em `409 INTEGRIDADE_DADOS_VIOLADA`, o fallback público vigente para conflito de integridade; não se reutilizará `ORDEM_OPERACAO_DUPLICADA`, pois o cliente deixou de escolher a ordem. A mensagem não sugere informar ou alterar `ordemNoDia`. Não se cria retry automático: um retry invisível poderia repetir trabalho externo e obscurecer falhas.

O teste concorrente de duas Operações financeiramente válidas da mesma Carteira+Ação+data usa provider stubado, libera duas threads em conjunto e prova: duas conclusões com sucesso, exatamente duas linhas, conjunto de ordens `{1, 2}`, ausência de duplicidade e replay final válido. A regressão de vendas concorrentes prova que somente a combinação compatível persiste e que nenhum prefixo do replay fica negativo. O cenário existente de criação versus exclusão da Carteira permanece protegido.

Inserções retroativas no mesmo dia recebem a próxima ordem e ficam após as existentes. O replay mantém `dataOperacao`, `ordemNoDia` e o desempate técnico vigente; uma inserção que invalide qualquer saldo posterior falha integralmente.

### 8. Sem idempotência por payload

Não haverá idempotency key nem comparação de payload. Duas submissões deliberadas, mesmo idênticas, são duas Operações e recebem ordens distintas quando ambas são válidas. A prevenção de múltiplo clique e a ausência de retry automático pertencem à futura reconciliação frontend, fora desta change.

### 9. Persistência e dados existentes sem migration

Nenhuma migration é necessária: preço e ordem continuam calculados antes do `INSERT` e persistidos NOT NULL nos mesmos tipos; valor total, índices, checks e unique constraint continuam compatíveis. O changeSet `004` não será modificado e nenhum novo changeSet será criado.

Operações existentes não passam pelo novo POST, logo não são recalculadas, consultadas externamente ou renumeradas. Rollback de aplicação restaura o contrato anterior para novos requests sem transformação de dados; registros criados pela versão nova continuam válidos no schema e nas consultas.

### 10. OpenAPI e compatibilidade de consultas

Anotar a hierarquia de request para gerar `oneOf`/discriminator e declarar respostas específicas da COMPRA. `OperacaoResponse` permanece único e completo. `operation-query` não recebe delta: ordenação, valores retornados e independência de providers não mudam. Testes sobre `/v3/api-docs` verificam discriminator, propriedades proibidas/obrigatórias e erros, sem exemplos que contenham credenciais.

## Risks / Trade-offs

- [A chamada HTTP ocorre antes do lock e o estado pode mudar até a transação] → recarregar referências, gerar ordem e repetir todas as validações dependentes de estado dentro do colaborador transacional.
- [Duas COMPRAS concorrentes podem fazer chamadas externas redundantes] → aceitar o custo no MVP para não manter lock durante rede; serializar somente a persistência.
- [Alpha Vantage compact limita retroatividade] → classificar fora do alcance somente com pelo menos 100 candles válidos/distintos e nunca aceitar preço manual.
- [Provider não distingue ausência de candle e alcance] → não inventar 422; usar apenas classificação sustentada e manter testes explícitos para respostas indeterminadas.
- [Polimorfismo Jackson/springdoc pode gerar schema divergente] → usar classes concretas, `EXISTING_PROPERTY`, `visible=true`, rejeição herdada por `@JsonAnySetter`, `additionalProperties=false` documental e testes separados do binding e do JSON OpenAPI efetivo.
- [`MAX+1` isolado é vulnerável a corrida] → executá-lo após o lock de Carteira na mesma transação e manter unique constraint como defesa.
- [Inserção retroativa no fim do dia pode divergir da ordem real] → comportamento deliberado do MVP; exigir cadastro na ordem real e rejeitar replay negativo.
- [Falha depois da consulta externa desperdiça quota] → trade-off necessário para manter transação curta; não repetir automaticamente.

## Migration Plan

1. Implementar e validar contrato, capability histórica e adapters com stubs, sem chamadas reais.
2. Introduzir o colaborador transacional e mover lock, `MAX+1`, replay e persistência para ele.
3. Executar testes focados, concorrência, regressões e suíte completa; validar Liquibase/Hibernate sem novo changeSet.
4. Publicar backend e somente depois reconciliar a change frontend bloqueada com o contrato aprovado.
5. Em rollback, reimplantar a versão anterior; nenhuma reversão de schema ou dados é necessária.

## Open Questions

Não há decisão funcional ou arquitetural pendente para iniciar a implementação. Respostas Alpha Vantage anteriores à menor data com menos de 100 candles válidos/distintos possuem classificação fechada em `502 RESPOSTA_EXTERNA_INVALIDA`; nenhuma heurística adicional será inventada durante a implementação.
