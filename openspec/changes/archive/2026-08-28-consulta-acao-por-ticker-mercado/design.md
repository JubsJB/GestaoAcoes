## Context

Veja `proposal.md` para a motivação e a delta spec preliminar para o comportamento já determinado. O RF10 prevê busca por ID ou ticker, mas o modelo e a spec principal definem a identidade como `(ticker normalizado, mercado)` e permitem o mesmo ticker em `BRASIL` e `EUA`. A lista inicial do PRD sugere `/acoes/ticker/{ticker}`, porém autoriza refinamento durante a modelagem.

Atualmente, `GET /acoes/{id}` delega a `AcaoService.buscarPorId`, que usa uma consulta ao repository, lança `ObjectNotFoundException` quando ausente e mapeia com `AcaoMapper`. `AcaoRepository.findByTickerAndMercado` já existe. `TickerNormalizer.normalizeAndValidate` aplica `trim`, `toUpperCase(Locale.ROOT)`, rejeita nulo/branco e limite superior a 30, preservando caracteres internos. A tabela possui `uk_acao_ticker_mercado`; portanto, a consulta não exige schema novo.

## Goals / Non-Goals

**Goals:**

- completar o RF10 sem ambiguidade usando ticker e mercado;
- reutilizar normalizador, repository derivado, mapper, DTO e tratamento de erros atuais;
- realizar uma única consulta local em transação read-only;
- preservar todos os contratos existentes de Ação.

**Non-Goals:**

- busca parcial, autocomplete, filtros da coleção, paginação ou pesquisa por nome;
- cadastro, edição, exclusão ou atualização de cotação;
- validação externa, chamada a providers ou atualização de dados persistidos;
- DTO, repository paralelo, query customizada, migration, índice, entidade ou dependência nova.

## Decisions

### 1. Identidade singular exige ticker e mercado

`mercado` deve ser obrigatório. `uk_acao_ticker_mercado`, RF12 e a spec vigente permitem que o mesmo ticker exista nos dois mercados. Aceitar apenas ticker exigiria retornar lista, escolher um mercado por padrão ou selecionar arbitrariamente um registro, todos incompatíveis com uma consulta singular.

Alternativa descartada: ticker isolado com preferência por `BRASIL` ou `EUA`. Ela modifica a identidade do domínio e produz resultado incorreto quando ambos existem.

### 2. Reutilização da normalização vigente

A consulta reutilizará `TickerNormalizer.normalizeAndValidate`, sem criar gramática adicional por mercado. Assim, aplica `trim`, uppercase com `Locale.ROOT`, limite de 30 e preservação de caracteres internos, inclusive `.` e `-` quando presentes. As fontes atuais não definem regex mais restritiva; inventá-la nesta change faria a consulta rejeitar uma identidade que o cadastro aceita.

### 3. Repository e resposta existentes

`findByTickerAndMercado` será usado diretamente e uma única vez por entrada válida. `Optional.empty()` será traduzido em `ObjectNotFoundException`, sem código novo. A resposta reutilizará `AcaoResponse`, pois representa o mesmo recurso da consulta por ID.

### 4. Leitura local e transação

O método do service será `@Transactional(readOnly = true)` com isolamento padrão. Ele normalizará o ticker, validará a presença do mercado, fará uma consulta e mapeará a entidade encontrada. Não haverá lock, escrita, `Clock`, `AcaoPersistenceService`, `AcaoCotacaoPersistenceService` ou acesso ao mapa de providers nesse método.

### 5. Contrato REST aprovado

O contrato definitivo é `GET /acoes/por-ticker?ticker={ticker}&mercado={mercado}`. Os dois query parameters são semanticamente obrigatórios e compõem a identidade consultada. A rota explicita um lookup singular, espelha `/corretoras/por-cnpj`, não conflita com `/{id}` e mantém `GET /acoes` exclusivamente como listagem.

As alternativas analisadas e rejeitadas foram:

#### Alternativa A — `GET /acoes/por-ticker?ticker={ticker}&mercado={mercado}` (aprovada)

- Vantagens: explicita consulta singular; espelha `/corretoras/por-cnpj`; não conflita com `/{id}`; transporta os dois componentes da identidade; mantém `GET /acoes` como lista; é simples para Angular e deixa filtros futuros na coleção.
- Desvantagem: usa segmento de lookup mais query parameters em vez de identidade no path.
- Impacto: acrescenta apenas um método dedicado no resource e não muda contratos existentes.

#### Alternativa B — `GET /acoes/ticker/{ticker}?mercado={mercado}`

- Vantagens: próxima da sugestão inicial do PRD e mantém resposta singular.
- Desvantagens: divide a identidade entre path e query; o segmento textual precisa coexistir com `/{id}`; caracteres internos aceitos pelo normalizador podem demandar encoding; é menos consistente com a consulta de Corretora recém-consolidada.
- Impacto: exige cuidado adicional de roteamento/encoding sem benefício funcional.

#### Alternativa C — `GET /acoes?ticker={ticker}&mercado={mercado}`

- Vantagem: forma comum de filtrar coleção.
- Desvantagens: ou muda `GET /acoes` de lista para objeto, ou devolve lista de zero/um item; ambas contrariam a resposta singular equivalente à consulta por ID e introduzem filtro genérico fora do escopo.
- Impacto: aumenta o risco de quebra no Angular e mistura listagem com lookup singular.

#### Alternativa D — `GET /acoes/por-ticker/{ticker}/mercados/{mercado}`

- Vantagem: ambos os componentes aparecem no path.
- Desvantagens: rota mais extensa, caracteres do ticker continuam sujeitos a encoding e não segue o padrão local de lookup dedicado por query.
- Impacto: nenhum ganho sobre a alternativa A para o MVP.

**Decisão:** utilizar exclusivamente a alternativa A, sem aliases. Não serão expostos `/acoes/ticker/{ticker}`, `/acoes?ticker=...&mercado=...` como lookup singular nem `/acoes/por-ticker/{ticker}/mercados/{mercado}`.

### 6. Binding e erros de parâmetros aprovados

Ticker e mercado são obrigatórios no contrato, mas o resource deverá permitir que a ausência chegue à política já aprovada em vez de deixar o binding padrão produzir um payload diferente. Para isso, os `@RequestParam` serão recebidos com `required = false`: ticker nulo seguirá para `TickerNormalizer`, que produz `400 / TICKER_INVALIDO`; mercado nulo será rejeitado pelo service com `400 / REQUEST_INVALIDO` antes do repository.

Mercado textual desconhecido falhará durante a conversão para `Mercado` e continuará sendo tratado pelo `ResourceExceptionHandler` via `MethodArgumentTypeMismatchException`, resultando em `400 / REQUEST_INVALIDO`. Não será criado handler global nem ErrorCode novo.

## Risks / Trade-offs

- [O mesmo ticker pode existir nos dois mercados] → exigir mercado e consultar o par completo.
- [Ticker aceita caracteres internos sem regex] → reutilizar estritamente a política do cadastro, sem rejeições novas.
- [O service compartilhado possui providers para cadastro e cotação] → testes de interação e arquitetura devem provar que o novo método não os utiliza.
- [Uma rota textual pode disputar matching com `/{id}`] → usar rota dedicada estática e testes HTTP de regressão.
- [Parâmetros ausentes podem escapar do formato padronizado] → permitir binding nulo apenas nesta rota e aplicar no service os códigos aprovados.

## Migration Plan

Nenhuma migration de banco. A futura implementação deverá adicionar somente o método read-only, a rota dedicada e os testes proporcionais, seguida das regressões e validações do projeto. Rollback futuro consistirá na remoção do endpoint e método de leitura, sem alteração de dados.
