## Context

Veja `proposal.md` para a motivação e as delta specs `operation-registration` e `portfolio-deletion` para o contrato proposto. O PRD define Operação com Carteira, Ação, Corretora opcional, tipo, quantidade, preço unitário, data e valor total; determina `COMPRA`/`VENDA`, valores positivos e proíbe venda superior à posição. Ele também exige ordem cronológica, mas não define quantidade fracionária, precisão, operação futura, ordenação intradiária nem política de inserção retroativa.

O código atual usa estrutura plana em camadas sob `com.projeto`: Resources delegam a Services; DTOs e Mappers separam API e JPA; repositories estendem `JpaRepository`; `ObjectNotFoundException` e `ApiException` chegam ao `ResourceExceptionHandler` e ao `StandardError`. Carteira usa transação direta no `CarteiraService`; Ação e Corretora usam persistence services porque suas chamadas externas precisam ficar fora da transação. Como o registro proposto não consultará rede, uma transação curta no `OperacaoService` é suficiente.

`Carteira`, `Acao` e `Corretora` não possuem coleções entre si. `AcaoRepository` já protege unicidade por `(ticker, mercado)`, mas ainda precisa de uma leitura pelo mesmo par. `TickerNormalizer` já implementa `trim` + maiúsculas com `Locale.ROOT`. `CarteiraService.excluir` atualmente usa `findById` e `delete`; a criação de Operações torna necessária a proteção anteriormente documentada.

O changelog master contém, em ordem, `001-create-corretora.yaml`, `002-create-acao.yaml` e `003-create-carteira.yaml`. O mesmo Liquibase roda em PostgreSQL e H2, seguido por Hibernate com `ddl-auto=validate`. Não há entidade, tabela, repository, provider histórico ou posição consolidada de Operação.

O Graphify confirma as relações `CarteiraResource → CarteiraService → CarteiraRepository`, `AcaoResource → AcaoService → AcaoPersistenceService/AcaoRepository/CotacaoProvider` e `CorretoraResource → CorretoraService → CorretoraPersistenceService/CorretoraRepository`. O desenho mantém esses limites e não acopla regra financeira ao resource, mapper ou adapter.

As decisões funcionais e técnicas necessárias para esta fatia foram aprovadas e estão consolidadas abaixo. Não há decisão bloqueante remanescente para a implementação.

## Goals / Non-Goals

**Goals:**

- criar uma fatia vertical de Operação que use somente dados persistidos e seja testável sem rede;
- preservar com exatidão quantidade, preço negociado, valor total e ordem cronológica;
- validar uma VENDA contra todo o histórico afetado, inclusive inserções retroativas;
- garantir atomicidade e impedir overselling sob concorrência;
- introduzir relacionamentos unidirecionais sem cascade delete e efetivar a proteção de Carteira;
- evoluir o schema pelo Liquibase sem alterar changeSets anteriores;
- manter o preço da Operação independente de cotações atuais ou históricas.

**Non-Goals:**

- persistir posição, preço médio, custo, resultados, rentabilidade, patrimônio ou snapshot;
- implementar endpoint de consulta, atualização ou exclusão de Operação;
- implementar nesta change consulta externa de cotação histórica ou modificar os adapters atuais;
- persistir histórico de cotações ou referência usada durante o cadastro;
- cadastrar Ação automaticamente, revalidar Corretora externamente ou alterar dados relacionados;
- suportar taxas, emolumentos, impostos, dividendos, eventos corporativos ou frontend.

## Decisions

### 1. Usar `POST /operacoes` como recurso de nível superior

O contrato aprovado é:

```json
{
  "carteiraId": 1,
  "ticker": "PETR4",
  "mercado": "BRASIL",
  "corretoraId": 3,
  "tipo": "COMPRA",
  "quantidade": 100,
  "precoUnitario": 32.47,
  "dataOperacao": "2026-08-10",
  "ordemNoDia": 1
}
```

`corretoraId` é o único campo opcional. `id`, `acaoId`, `valorTotal`, cotação e campos desconhecidos serão rejeitados pelo padrão atual de request restrito. O ticker e o mercado tornam a Ação compreensível ao cliente e evitam expor sua chave técnica como entrada.

O sucesso será `201 Created`, com `Location: /operacoes/{id}` e:

```json
{
  "id": 42,
  "carteiraId": 1,
  "ticker": "PETR4",
  "mercado": "BRASIL",
  "corretoraId": 3,
  "tipo": "COMPRA",
  "quantidade": 100.000000,
  "precoUnitario": 32.470000,
  "dataOperacao": "2026-08-10",
  "ordemNoDia": 1,
  "valorTotal": 3247.000000000000
}
```

`acaoId` também é omitido do response: ticker + mercado expressam a identidade pública já usada pelo cliente. `corretoraId` aparece como `null` quando ausente. A escala JSON poderá refletir a escala persistida, sem alterar o valor numérico.

Alternativa considerada: `POST /carteiras/{carteiraId}/operacoes`, sugerido inicialmente pelo PRD. Foi preterida porque o contrato aprovado é `/operacoes`, já contém a Carteira e mantém uma URI estável para futuras consultas da Operação. `ordemNoDia` integra obrigatoriamente o request, response, entidade e schema.

### 2. Repetir a arquitetura em camadas sem persistence service adicional

A implementação planejada adiciona `OperacaoResource`, `OperacaoCreateRequest`, `OperacaoResponse`, `OperacaoMapper`, `OperacaoService`, `OperacaoRepository`, `Operacao`, e `TipoOperacao`. O resource valida o contrato HTTP e monta `Location`; o service contém a transação e as invariantes; o mapper somente projeta a resposta; o repository oferece as leituras cronológicas e de integridade.

Não será criado `OperacaoPersistenceService`: não há chamada externa no fluxo e separar a mesma transação entre services adicionaria indireção sem benefício. Também não será criada posição consolidada para apoiar a validação.

Alternativa considerada: repetir `AcaoPersistenceService`. Foi rejeitada porque ele existe para separar rede e concorrência de cadastro, enquanto toda a criação de Operação é local e precisa de um único limite transacional.

### 3. Resolver relacionamentos somente a partir do banco

O fluxo usará:

1. `carteiraId` para carregar a Carteira;
2. `TickerNormalizer` e `AcaoRepository.findByTickerAndMercado` para carregar a Ação;
3. `corretoraId`, quando presente, para carregar a Corretora;
4. `ObjectNotFoundException` para qualquer associação inexistente, preservando `404/StandardError`.

Não haverá cadastro de Ação, chamada a `CotacaoProvider`, BrasilAPI, ViaCEP ou revalidação da Corretora. O modelo JPA será unidirecional a partir de `Operacao`, com `@ManyToOne(fetch = LAZY, optional = false)` para Carteira e Ação e associação opcional para Corretora, sempre sem `cascade`.

Alternativa considerada: adicionar `@OneToMany` a todas as entidades relacionadas. Foi rejeitada por ampliar agregados, carregamento e ciclo de vida sem necessidade para esta fatia.

### 4. Nomear o enum `TipoOperacao`

`TipoOperacao` seguirá a linguagem do domínio e terá somente `COMPRA` e `VENDA`, persistidos como texto. Não será reutilizado ou duplicado `Mercado`; a Operação reutiliza o enum existente `BRASIL`/`EUA` somente no request e response, enquanto sua relação persistida aponta para a Ação.

Alternativa considerada: `OperacaoTipo`, que não acompanha a ordem nominal de enums existentes como `Mercado` e `Moeda`; e `BUY`/`SELL`, permitidos pelo AGENTS.md, mas menos coerentes com o contrato português solicitado.

### 5. Usar `BigDecimal` e `NUMERIC(19,6)` para quantidade

A quantidade será representada como `BigDecimal`/`NUMERIC(19,6)`, obrigatória e maior que zero. O mesmo tipo físico atende os dois mercados, mas a validação de domínio será determinada pelo `Mercado` da Ação já localizada:

- `BRASIL`: aceitar somente valor matematicamente inteiro. Uma representação como `100.000000` continua sendo o inteiro 100, mas qualquer componente fracionário diferente de zero será rejeitado;
- `EUA`: aceitar valor inteiro ou fracionário com até seis casas decimais, desde que caiba exatamente na precisão 19 e escala 6.

Bean Validation antecipará nulo, positividade e limites decimais. Depois de localizar a Ação, o service aplicará a regra do mercado; para `BRASIL`, poderá verificar `quantidade.stripTrailingZeros().scale() <= 0`. Nenhum fluxo arredondará ou truncará. O banco aplicará `CHECK (quantidade > 0)`; a integralidade brasileira permanece no service porque a tabela referencia `mercado` indiretamente por `acao_id` e não duplicará esse dado somente para viabilizar um check.

Alternativas consideradas:

- `Long/BIGINT`, simples para ações inteiras, mas incapaz de representar frações americanas;
- `Long/BIGINT` somente para `BRASIL` e decimal para `EUA`, que complicaria um único campo e o replay com dois tipos;
- `NUMERIC(19,8)`, sem evidência de necessidade adicional e divergente do padrão decimal já adotado.

### 6. Usar `BigDecimal` e `NUMERIC(19,6)` para preço unitário real

`precoUnitario` será `BigDecimal`/`NUMERIC(19,6)`, obrigatório, positivo e exato, por coerência com `Acao.cotacaoAtual`. Essa escolha não torna os conceitos equivalentes: a coluna da Operação registra o valor efetivamente negociado; a cotação da Ação continua sendo a última observação de mercado persistida.

O service não lerá `cotacaoAtual` para validar, preencher ou comparar o preço, e não haverá tolerância percentual. Escala excedida, parte inteira excedida, zero e negativo serão request inválido, sem deixar o banco arredondar.

Alternativa considerada: duas casas decimais. Foi rejeitada porque o domínio atende BRL e USD e o projeto já aprovou seis casas para preço de mercado; restringir a duas casas poderia perder preços válidos.

### 7. Calcular e persistir valor total como `NUMERIC(38,12)`

O service normalizará os dois operandos de forma exata e calculará:

```text
valorTotal = quantidade.multiply(precoUnitario)
```

O produto de dois valores `NUMERIC(19,6)` requer até 38 dígitos e escala 12; por isso `valorTotal` será `BigDecimal`/`NUMERIC(38,12)`. Essa escolha preserva todo o produto sem arredondamento. A aplicação fará uma verificação defensiva de precisão antes de salvar; com operandos dentro dos limites aprovados, o produto máximo cabe matematicamente no tipo.

Alternativas consideradas:

- `NUMERIC(19,6)`, que obrigaria arredondar ou rejeitar produtos perfeitamente válidos;
- não persistir `valorTotal`, que evitaria redundância, mas diverge do modelo do PRD e dificulta a auditoria do valor consolidado no momento do registro;
- escala 2, inadequada para frações e preços com seis casas.

### 8. Representar `dataOperacao` como `LocalDate`/`DATE`

Como o usuário pode conhecer somente a data da negociação, `LocalDate` e SQL `DATE` preservam exatamente a informação existente, sem fabricar horário, offset ou timezone. `OffsetDateTime` seria apropriado somente se o horário real fosse obrigatório.

A validação aceitará passado e o dia civil corrente do mercado e rejeitará futuro. O service reutilizará o `Clock` injetado, preservando testabilidade, mas projetará o mesmo instante na zona definida pelo mercado da Ação:

| Mercado | `ZoneId` de referência | Justificativa |
|---|---|---|
| `BRASIL` | `America/Sao_Paulo` | data civil do principal mercado brasileiro |
| `EUA` | `America/New_York` | data civil comum às sessões regulares de ações nas bolsas NYSE e Nasdaq, incluindo horário de verão |

Conceitualmente, o limite será obtido por `LocalDate.now(clock.withZone(zoneIdDoMercado))`, depois de resolver a Ação por ticker + mercado. Não será usada apenas a data UTC global, que pode divergir da data do mercado perto da meia-noite. A data persistida continua sem horário ou fuso; a zona serve exclusivamente para decidir se a data informada é futura.

Alternativas consideradas:

- `OffsetDateTime`, que exigiria do cliente horário e offset possivelmente desconhecidos;
- aceitar datas futuras, que registraria como executada uma negociação ainda não realizada;
- usar somente UTC global, rejeitado porque pode classificar incorretamente a data civil atual do mercado;
- usar timezone do usuário ou da Corretora, rejeitado porque a regra aprovada é determinada pelo mercado da Ação e esses fusos não fazem parte do modelo atual.

### 9. Exigir `ordemNoDia` como chave cronológica de domínio

O PRD e o AGENTS.md proíbem usar ordem de inserção quando ela puder alterar cálculos. Como `LocalDate` não diferencia Operações da mesma Carteira e Ação no mesmo dia, `ordemNoDia: Integer` será obrigatório, positivo e único no grupo `(carteira_id, acao_id, data_operacao)`.

A ordem total relevante será:

```text
dataOperacao ASC, ordemNoDia ASC
```

Uma constraint única em `(carteira_id, acao_id, data_operacao, ordem_no_dia)` elimina empate; `id` não participa da regra financeira. Lacunas na numeração serão permitidas e não alteram a ordenação.

Uma sugestão automática da próxima ordem poderá ser fornecida futuramente pelo frontend ou por capability auxiliar, mas não integra esta change.

Alternativas consideradas:

- desempatar por ID, rejeitado por transformar inserção em cronologia;
- ordenar COMPRA antes de VENDA, que inventaria uma sequência potencialmente diferente da negociação real;
- exigir horário, que contradiz o requisito de aceitar somente a data conhecida;
- impedir múltiplas Operações no mesmo dia, restrição incompatível com uso real.

### 10. Reproduzir toda a sequência para validar posição e retroatividade

`OperacaoRepository` oferecerá uma leitura por Carteira e Ação ordenada por data e ordem. O service inserirá conceitualmente a candidata nessa sequência e fará um fold com `BigDecimal`:

```text
saldo = 0
COMPRA → saldo = saldo + quantidade
VENDA  → saldo = saldo - quantidade
qualquer saldo < 0 → rejeitar a candidata inteira
```

Isso atende tanto a venda atual quanto a retroativa. Não basta calcular apenas `SUM(COMPRA)-SUM(VENDA)` antes da candidata: uma VENDA retroativa pode deixar o saldo final não negativo e ainda assim tornar negativa uma posição intermediária ou uma venda posterior.

A posição será isolada por `(carteira, acao)`. VENDA igual ao saldo fecha a posição em zero. A implementação não calculará preço médio, custo ou resultado realizado.

Alternativas consideradas:

- query agregada única, insuficiente para validar prefixos cronológicos;
- tabela de posição, fora do escopo e fonte adicional de consistência;
- rejeitar toda operação retroativa, simples porém incompatível com o cadastro de histórico solicitado.

### 11. Serializar criação e exclusão pela linha da Carteira

O registro será uma única transação de escrita. Um método dedicado de `CarteiraRepository` carregará a Carteira com lock pessimista de escrita; depois o service localizará Ação/Corretora, lerá o histórico, validará e executará `saveAndFlush`. O lock é adquirido sem chamada externa e serializa Operações da mesma Carteira, impedindo que duas vendas passem simultaneamente pelo mesmo saldo.

O mesmo lock deverá ser usado no fluxo de `CarteiraService.excluir`. A exclusão então verificará `OperacaoRepository.existsByCarteiraId` antes de `delete`. Se a Operação vencer a disputa, DELETE retorna `409`; se DELETE vencer, a criação relê a Carteira ausente e retorna `404`. A FK permanece como última proteção contra órfãos.

O lock por Carteira é mais amplo que um lock por Carteira+Ação, mas funciona inclusive na primeira Operação, quando ainda não há linha de Operação para bloquear, e coordena criação com DELETE. Não será introduzido `@Version` nem tabela de lock.

Alternativas consideradas:

- nenhum lock, vulnerável a duas vendas concorrentes;
- lock em Operação, impossível para o primeiro registro;
- lock em Ação, que serializaria Carteiras independentes do mesmo ativo e não coordenaria DELETE da Carteira;
- isolamento serializable global, mais caro e abrangente.

### 12. Efetivar a proteção de DELETE de Carteira

Quando `existsByCarteiraId` for verdadeiro, `DELETE /carteiras/{id}` lançará `ApiException` com `409 Conflict`, código `CARTEIRA_POSSUI_OPERACOES` e detalhes mínimos como `carteiraId`. A Carteira e suas Operações permanecerão intactas. Carteira sem Operações mantém `find → delete → 204`, e ID inexistente mantém `404`.

Essa mudança cumpre a regra normativa já promovida em `portfolio-deletion`. Nenhuma FK terá `ON DELETE CASCADE`. O bloqueio de futuras exclusões de Ação ou Corretora ficará garantido pela FK `RESTRICT/NO ACTION`; esta change não cria esses endpoints.

Alternativa considerada: depender somente da violação de FK. Foi rejeitada porque o handler genérico atual classifica qualquer `DataIntegrityViolationException` como Corretora duplicada e não produz um erro de negócio determinístico.

### 13. Criar códigos de erro específicos somente para invariantes novas

O desenho reutiliza `ObjectNotFoundException` para Carteira, Ação e Corretora inexistentes e `REQUEST_INVALIDO` para JSON, enum, obrigatoriedade, positividade, precisão de entrada, data futura e ordem não positiva.

Códigos específicos definidos para as novas invariantes:

| Situação | HTTP | Code |
|---|---:|---|
| ordem repetida na mesma Carteira/Ação/data | 409 | `ORDEM_OPERACAO_DUPLICADA` |
| venda torna algum saldo cronológico negativo | 409 | `POSICAO_INSUFICIENTE` |
| Carteira possui Operações | 409 | `CARTEIRA_POSSUI_OPERACOES` |

`POSICAO_INSUFICIENTE` poderá informar `carteiraId`, ticker, mercado, `quantidadeDisponivel` no ponto da falha e `quantidadeSolicitada`. Erros de constraint conhecidos serão traduzidos dentro do fluxo de Operação; não se ampliará o fallback genérico incorreto para atribuir falhas novas a `CORRETORA_DUPLICADA`.

O produto fora de `NUMERIC(38,12)` é defensivamente impossível quando ambos os operandos satisfazem `19,6`. Entradas que excedem os operandos falham como `400/REQUEST_INVALIDO`, sem deixar o banco arredondar.

### 14. Criar `004-create-operacao.yaml` sem alterar migrações anteriores

O novo changeSet será incluído ao final do master e criará somente `operacao`:

| Campo | Tipo SQL | Restrição |
|---|---|---|
| `id` | `BIGINT` identity | PK, não nulo |
| `carteira_id` | `BIGINT` | FK não nula, sem cascade |
| `acao_id` | `BIGINT` | FK não nula, sem cascade |
| `corretora_id` | `BIGINT` | FK anulável, sem cascade |
| `tipo` | `VARCHAR(10)` | não nulo, check COMPRA/VENDA |
| `quantidade` | `NUMERIC(19,6)` | não nulo, check > 0 |
| `preco_unitario` | `NUMERIC(19,6)` | não nulo, check > 0 |
| `data_operacao` | `DATE` | não nulo |
| `ordem_no_dia` | `INTEGER` | não nulo, check > 0 |
| `valor_total` | `NUMERIC(38,12)` | não nulo, check > 0 |

Constraints e índices:

- PK `pk_operacao`;
- FKs `fk_operacao_carteira`, `fk_operacao_acao`, `fk_operacao_corretora`, com `RESTRICT` ou sem ação equivalente e nunca cascade;
- unique `uk_operacao_carteira_acao_data_ordem`;
- checks de `tipo`, positividade de quantidade, preço e valor total, positividade da ordem e igualdade exata `valor_total = quantidade * preco_unitario`;
- índice `idx_operacao_carteira_acao_cronologia` em `(carteira_id, acao_id, data_operacao, ordem_no_dia)` para replay e existência por Carteira;
- índices `idx_operacao_acao_id` e `idx_operacao_corretora_id` para integridade/consultas futuras pelas FKs que não são prefixo do índice composto.

O rollback removerá explicitamente a tabela e seus objetos somente sob autorização operacional ou em banco descartável. Não haverá alteração em 001, 002, 003, entidade existente, dependência ou `ddl-auto`.

### 15. Separar rigorosamente três conceitos de preço

| Conceito | Origem | Persistência atual | Uso no registro |
|---|---|---|---|
| `Acao.cotacaoAtual` | último valor do provider já consultado | tabela `acao` | nenhum |
| `Operacao.precoUnitario` | preço efetivamente informado pelo usuário | tabela `operacao` | valor total e futura base de custo, preço médio e resultado |
| cotação histórica de referência | provider para a data solicitada | não persistida | somente auxílio anterior ao POST, em capability futura |

Nenhuma atualização de cotação altera Operações históricas. Nenhuma cotação histórica substitui o preço real, participa do valor total, altera `Acao.cotacaoAtual` ou impõe tolerância.

### 16. Capacidade histórica atual da BRAPI

A documentação oficial atual da [BRAPI — Histórico de preços de ações](https://brapi.dev/docs/acoes/historico) oferece `GET /api/v2/stocks/historical`, aceita `symbols`, `startDate`/`endDate` em `YYYY-MM-DD` ou `range`/`interval`, e retorna por pregão `open`, `high`, `low`, `close`, `adjustedClose` e volume. Ela informa que pedidos acima da profundidade do plano são reduzidos à janela permitida. A [página oficial de planos](https://brapi.dev/pricing) informa atualmente histórico diário de até um ano no Startup e histórico mais longo no Pro, além de limites mensais distintos; o plano efetivamente configurado no ambiente não é conhecido pelo código.

Para uma futura referência da negociação, a decisão é usar `close` bruto do pregão exato, identificado por data, e rotulá-lo como **fechamento de referência**. `adjustedClose` é útil para retorno ajustado, mas pode representar retrospectivamente eventos corporativos e não equivale ao preço negociado naquela data. `open`, `high` e `low` poderão ser apresentados como contexto, sem selecionar automaticamente outro valor.

Não haverá fallback automático para pregão anterior ou seguinte. Ausência da data, janela reduzida pelo plano, 401/403, 429, timeout ou indisponibilidade significam apenas “referência indisponível”. O endpoint atual de cotação instantânea do projeto não será presumido como histórico nem alterado nesta change.

### 17. Capacidade histórica atual da Alpha Vantage

A documentação oficial de [`TIME_SERIES_DAILY`](https://www.alphavantage.co/documentation/#daily) retorna série diária bruta com data, abertura, máxima, mínima, fechamento e volume. `outputsize=compact` fornece as 100 observações mais recentes e está disponível para chaves gratuitas; `outputsize=full`, com mais de 20 anos, é premium. `TIME_SERIES_DAILY_ADJUSTED` também é premium e não é necessário para a referência bruta. `GLOBAL_QUOTE`, usado atualmente pelo adapter, não oferece o histórico solicitado.

A [página oficial de suporte](https://www.alphavantage.co/support/#api-key) informa atualmente limite padrão gratuito de 25 chamadas por dia, salvo condições especiais de projetos verificados, enquanto planos premium ampliam volume. O plano configurado não pode ser inferido da existência da chave.

Para EUA, a decisão também é usar o `close` bruto da data exata como fechamento de referência por `TIME_SERIES_DAILY` ou endpoint correspondente que seja aprovado na futura capability. Datas fora das 100 observações exigirão `outputsize=full` e plano compatível. Data sem pregão, ausência no payload, rate limit ou indisponibilidade não deve provocar substituição por outra data nem afetar o POST.

### 18. Separar a consulta histórica do registro

Foram comparadas duas opções:

**A — consultar dentro de `POST /operacoes`:**

- vantagem: uma única interação pode devolver preço real e referência;
- desvantagens: adiciona latência e rate limit ao caminho crítico, mistura informação opcional com persistência, exige semântica parcial complexa e pode induzir o cliente a tratar a referência como validação.

**B — consulta auxiliar separada:**

- permite ao frontend consultar antes do POST e exibir a referência sem participar da transação;
- mantém falha externa independente do registro essencial;
- isola cache, planos, rate limits e evolução dos providers;
- permite um contrato conceitual futuro como `GET /acoes/{ticker}/cotacao-historica?mercado=BRASIL&data=2026-08-10`.

A decisão aprovada é **B em uma change futura**. Ela deverá criar uma abstração própria, como `CotacaoHistoricaProvider`, em vez de ampliar `CotacaoProvider` com responsabilidades incompatíveis. Esta change apenas garante que `POST /operacoes` não chama providers, não persiste referência e não a inclui no response. Qualquer indisponibilidade histórica permanecerá fora do caminho crítico e nunca bloqueará uma Operação válida.

### 19. Planejar testes nos níveis já adotados

- `OperacaoServiceTest`: relacionamentos, normalização, valores, data, total, compra, venda, replay, retroatividade, isolamento e ausência de chamadas externas.
- `OperacaoResourceTest`: contrato restrito, enum, `201`, DTO, `Location`, `StandardError` e campos controlados.
- `OperacaoRepositoryTest` com Spring Boot/H2: schema Liquibase, enums textuais, escalas, Corretora nula, FKs, checks, unique de ordem, índices, replay ordenado e rollback.
- testes concorrentes/transacionais: duas vendas e disputa criar Operação × excluir Carteira.
- testes de `CarteiraService`/resource/repository: sem Operações continua `204`; com Operações retorna `409`; nenhuma cascade.
- regressão: toda a suíte existente, `clean verify`, Hibernate validate e OpenSpec strict.

Como a arquitetura aprovada exclui histórico desta change, não haverá novos testes de adapter histórico. Haverá testes explícitos de que BRAPI, Alpha Vantage, BrasilAPI e ViaCEP não são chamados. Cenários de disponibilidade, timeout, rate limit e data sem pregão pertencem à futura capability auxiliar.

## Risks / Trade-offs

- [A ordem intradiária exige um novo dado do usuário] → explicar que a data sozinha é insuficiente e rejeitar duplicidade, sem inventar sequência por ID.
- [A validação de quantidade depende do mercado referenciado pela Ação] → resolver a Ação antes da validação específica, centralizar a regra no service e cobrir `BRASIL` inteiro e `EUA` fracionário em testes.
- [Replay completo cresce com o histórico] → criar índice cronológico; medir antes de introduzir posição materializada ou cache.
- [Lock por Carteira serializa ativos independentes] → mantê-lo somente na transação curta; revisar granularidade quando volume justificar uma chave de lock própria.
- [Operação retroativa pode invalidar vendas posteriores] → validar todos os prefixos e rejeitar somente a candidata, nunca reescrever histórico silenciosamente.
- [O response decimal pode exibir zeros adicionais] → tratar JSON como número exato; não reduzir escala por arredondamento.
- [FK de Corretora opcional impede remoção futura de Corretora usada] → preservar histórico; qualquer política de anonimização ou desvinculação exige capability própria.
- [Planos de providers podem mudar] → registrar links e limitações como análise, não como dependência do POST; revisar documentação na futura change histórica.
- [Rollback da tabela destrói histórico financeiro] → nunca executar automaticamente em ambiente com dados; exigir autorização operacional.
- [Eventos corporativos podem alterar quantidade econômica sem Operação] → manter fora desta fatia e não mascarar o limite; capability futura deverá modelá-los explicitamente.

## Migration Plan

1. Adicionar `004-create-operacao.yaml` e incluí-lo ao final do master, sem modificar 001–003.
2. Implementar enum, entidade, repository, DTOs, mapper, service e resource de Operação, além da leitura de Ação por ticker + mercado.
3. Implementar validações por mercado e data civil, lock/replay transacional e proteção `409` no DELETE de Carteira.
4. Executar testes unitários, HTTP, H2/Liquibase, rollback, concorrência e regressão; depois a suíte completa e `clean verify`.
5. Validar OpenSpec strict e atualizar Graphify somente após a futura alteração de código.

Rollback de código remove o endpoint e os componentes novos. Rollback de banco remove a tabela `operacao` e todo o histórico nela contido, portanto só poderá ser executado com autorização explícita ou em banco descartável; jamais deve ser acionado automaticamente para desfazer deployment. A proteção de DELETE não deve ser retirada enquanto a tabela e seus registros existirem.
