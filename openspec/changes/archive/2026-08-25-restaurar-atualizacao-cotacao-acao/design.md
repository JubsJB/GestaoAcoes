## Context

Ver `proposal.md` para a motivação. A baseline atual persiste `cotacaoAtual` e `dataHoraCotacao` em `Acao`, dispõe de `CotacaoProvider`, `BrapiAdapter`, `AlphaVantageAdapter`, mapeamento de erros externos e validações usadas no cadastro, mas não contém o endpoint nem o fluxo de atualização exigidos por `openspec/specs/stock-registration/spec.md`.

O commit órfão `999fc4aba4a083a6d2d2f5488ca61a5d2c44764e` demonstra uma implementação anterior do contrato. Ele será somente uma referência de comportamento e testes: cada alteração será reaplicada manualmente e adaptada à baseline atual. Não serão recuperados o arquivo acidental `check`, a alteração não relacionada em `application-dev.properties`, versões antigas de arquivos inteiros ou qualquer trecho que sobrescreva evoluções posteriores de Carteira, Operações e posição consolidada.

Como a spec principal já contém o comportamento definitivo, esta change usa `skip_specs: true`: não existe delta comportamental a promover.

## Goals / Non-Goals

**Goals:**

- Reintroduzir o fluxo de atualização sob demanda sem ampliar o contrato aprovado.
- Manter a chamada externa fora da transação de escrita e o lock restrito à persistência final.
- Garantir atualização temporal monotônica sob concorrência.
- Reutilizar as validações, providers, adapters, DTOs e tratamento de erros atuais.
- Preservar integralmente os fluxos atuais de cadastro/consulta de Ação, Operações e posição consolidada.

**Non-Goals:**

- Alterar specs principais, schema, Liquibase, dependências ou providers.
- Introduzir `@Version`, histórico de cotações, scheduler, atualização automática, cache ou novas APIs externas.
- Calcular valor atual da posição, resultado não realizado, rentabilidade ou patrimônio.
- Refatorar componentes não necessários ou restaurar mecanicamente o commit órfão.

## Decisions

### 1. A baseline atual é a base de implementação

O commit `999fc4a` será comparado por diff e usado para recuperar intenção, cenários e decisões aprovadas, nunca por `cherry-pick`, checkout de arquivos ou cópia integral. A implementação será feita sobre as assinaturas e dependências atuais, preservando, em especial, `AcaoRepository.findByTickerAndMercado(...)`, os códigos de erro posteriores e os serviços de Operações e posição.

**Alternativa considerada:** reaplicar o commit inteiro. Rejeitada porque ele não contém as evoluções posteriores e inclui arquivos acidentais/não relacionados.

### 2. O endpoint permanece fino e não aceita representação de entrada

`AcaoResource` exporá `PATCH /acoes/{id}/cotacao`. O resource verificará o conteúdo real da requisição antes de delegar; ausência de body é aceita, enquanto qualquer body não vazio produz `400 / REQUEST_INVALIDO`. Em sucesso, mapeará o estado efetivamente persistido para `AcaoResponse`, com `200 OK` e sem header `Location`.

A técnica de inspeção do body deverá ser compatível com o stack MVC atual e coberta por testes para body ausente, vazio e não vazio. Ela não deve criar DTO de entrada nem permitir que desserialização silenciosa transforme um body em comando válido.

**Alternativa considerada:** receber um DTO opcional. Rejeitada porque sugere um contrato de entrada inexistente e pode tratar payloads de maneira diferente do requisito “qualquer body não vazio”.

### 3. A orquestração externa permanece fora da transação de escrita

`AcaoService.atualizarCotacao(id)` executará o fluxo:

1. localizar a Ação por ID e capturar o estado necessário para preservação de erro;
2. selecionar `CotacaoProvider` pelo `mercado` persistido;
3. consultar o provider com o ticker persistido;
4. reutilizar as validações atuais de ticker, nome, mercado, moeda, cotação positiva, precisão e payload;
5. verificar a compatibilidade do ticker canônico;
6. obter o timestamp candidato do provider, usando o `Clock` existente apenas no fallback já aprovado quando o provider não informa horário;
7. delegar a escrita ao componente transacional e retornar a releitura persistida.

O método de orquestração não será transacional. Assim, latência, timeout e rate limit externos não prolongam transações nem locks de banco. A Ação é localizada antes da chamada externa; ID inexistente termina em `404` sem invocar provider.

**Alternativa considerada:** transacionar todo o service. Rejeitada porque manteria conexão/transação durante I/O externo e ampliaria a janela de contenção.

### 4. A persistência final será curta, transacional e pessimista

Será criado ou adaptado um componente equivalente a `AcaoCotacaoPersistenceService`. Seu método transacional relerá a Ação por `AcaoRepository.findByIdForUpdate(id)`, usando `PESSIMISTIC_WRITE`, comparará o timestamp candidato e persistirá somente se ele for estritamente posterior. O lock começa apenas nessa releitura final e termina com a transação; nenhuma chamada HTTP ocorre nesse intervalo.

O repository receberá somente o método de busca bloqueante necessário, sem remover ou alterar métodos existentes. Não será adicionado `@Version`.

**Alternativa considerada:** atualizar com o objeto lido antes da chamada externa. Rejeitada porque permitiria lost update/regressão temporal entre consultas concorrentes.

### 5. A regra monotônica terá defesa na persistência e mutação restrita no domínio

`Acao` receberá `atualizarCotacao(BigDecimal, OffsetDateTime)` ou equivalente, sem setters genéricos. O método só poderá alterar `cotacaoAtual` e `dataHoraCotacao`, mantendo as invariantes já aprovadas para valor positivo, precisão e timestamp. A persistência compara o candidato com o estado relido sob lock:

- posterior: aplica preço e timestamp;
- igual: não altera e retorna o estado persistido;
- anterior: não altera e retorna o estado persistido.

Se o preço for numericamente igual e o timestamp posterior, a mutação atualiza o timestamp; o valor numérico permanece equivalente. O `Clock` não substitui um timestamp fornecido pelo provider e não é usado para contornar a comparação temporal.

**Alternativa considerada:** comparar apenas no service antes do I/O de persistência. Rejeitada porque a leitura estaria obsoleta sob concorrência.

### 6. Provider e ticker canônico seguem os dados persistidos

O provider é escolhido exclusivamente por `Acao.mercado`: BRAPI para `BRASIL` e Alpha Vantage para `EUA`. Não haverá fallback cruzado nem provider novo.

Quando o provider sinalizar ticker canônico divergente e este for incompatível com o ticker persistido, o fluxo lança `409 / TICKER_CANONICO_DIVERGENTE`; não migra ticker e não altera campo algum. As regras atuais de normalização/comparação e de payload externo serão reutilizadas, evitando inventar equivalências novas.

### 7. Falhas preservam o último estado válido e reutilizam o sistema de erros

Depois que a Ação foi localizada, qualquer falha prevista mantém `cotacaoAtual` e `dataHoraCotacao`, pois nenhuma escrita começa antes da validação e a escrita final é atômica. Os erros existentes de ticker inexistente, cotação indisponível, precisão, dados incompletos, rate limit, resposta inválida, indisponibilidade e timeout continuam sendo produzidos/mapeados pela infraestrutura atual.

Quando a spec principal prevê enriquecimento, `details` incluirá `acaoId`, `cotacaoPreservada=true`, `ultimaCotacaoValida` e `dataHoraUltimaCotacao`. `TICKER_CANONICO_DIVERGENTE` será apenas acrescentado ao catálogo atual e tratado pelo `ResourceExceptionHandler` existente, sem criar um sistema paralelo.

### 8. Não há mudança de persistência estrutural nem de cálculos financeiros

As colunas necessárias já existem em `Acao`; portanto não haverá migration, changeSet, alteração de `ddl-auto=validate` ou dependência. A atualização de cotação não toca `Operacao.precoUnitario`, replay, venda, `CalculadoraPosicao`, `PosicaoService` ou posição consolidada. Testes de regressão comprovarão a separação entre cotação de mercado e histórico financeiro.

## Risks / Trade-offs

- **[Inspeção de body pode variar entre filtros/servidores]** → escolher mecanismo compatível com o stack atual e cobrir request sem body, body vazio e diferentes bodies não vazios em teste HTTP.
- **[Lock pessimista tem diferenças entre H2 e o banco de desenvolvimento]** → testar semântica do repository e concorrência na infraestrutura de testes disponível, mantendo SQL/JPA portável e validando Liquibase/Hibernate com `ddl-auto=validate`.
- **[Refatorar validações compartilhadas pode regredir o cadastro]** → reutilizar os helpers atuais com alteração mínima e executar regressão completa de cadastro/consulta.
- **[Estado pode ser removido entre leitura inicial e releitura final]** → tratar a ausência sob lock com o mesmo `404` do domínio, sem persistência parcial.
- **[Enriquecimento de erro pode perder causa/details existentes]** → compor os detalhes preservando código, status, mensagem, causa e metadados já mapeados.
- **[Testes de concorrência podem ser sensíveis a timing]** → coordenar threads por barreiras/latches e afirmar estado final determinístico pelo maior timestamp, sem sleeps arbitrários.

## Migration Plan

1. Implementar e validar as alterações sobre a baseline atual, sem reaplicar commits históricos.
2. Executar testes focados de domínio, repository/persistência, service, HTTP e concorrência.
3. Executar regressões de Ação, Operações e posição consolidada, validação Liquibase/Hibernate e suíte completa.
4. Atualizar o Graphify somente após a futura implementação e revisar o diff para garantir ausência de schema, dependências e arquivos do commit órfão.

Não há migração de dados ou schema. Em caso de rollback da futura implementação, os componentes de código e testes da restauração poderão ser revertidos como uma unidade; os dados persistidos continuam compatíveis porque o modelo não muda.

## Open Questions

Nenhuma decisão funcional ou técnica bloqueante foi identificada. Os detalhes de API já são normativos na spec principal, e a arquitetura atual comporta a restauração sem mudança de contrato.
