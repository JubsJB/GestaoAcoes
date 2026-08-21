## Context

Veja `proposal.md` para a motivação e `specs/stock-registration/spec.md` para o contrato observável aprovado. O PRD define em `RF08` e `RF11` que uma Ação cadastrada deve consultar e atualizar a última cotação pelo provider correspondente ao mercado. A lista inicial de endpoints sugere `PUT /acoes/{id}/atualizar-cotacao`, mas autoriza refinamento durante a modelagem da API.

A implementação atual expõe `POST /acoes`, `GET /acoes` e `GET /acoes/{id}` em `AcaoResource`. `AcaoService` já indexa `CotacaoProvider` por `Mercado`, reutiliza `Clock`, valida ticker, moeda, nome, cotação e timestamp no cadastro e mantém a chamada externa fora de `AcaoPersistenceService`. BRAPI e Alpha Vantage já são isoladas por `BrapiAdapter` e `AlphaVantageAdapter`, e `ExternalApiErrorMapper` já padroniza rate limit, indisponibilidade, timeout e payload inválido.

`Acao` não possui setters nem método de atualização, e `AcaoRepository` não oferece leitura com lock. A tabela já contém `cotacao_atual NUMERIC(19,6)` e `data_hora_cotacao TIMESTAMP WITH TIME ZONE`; portanto, a funcionalidade não exige mudança de schema, Liquibase, configuração, dependência ou contrato dos providers.

O Graphify atualizado confirma as relações diretas `AcaoResource → AcaoService`, `AcaoService → CotacaoProvider` e `AcaoService → AcaoPersistenceService`, além do uso de `AcaoRepository`, `AcaoMapper` e `AcaoResponse` no mesmo agregado.

## Goals / Non-Goals

**Goals:**

- manter no service a orquestração entre leitura, provider, validação e persistência;
- selecionar ticker e provider somente pelo estado persistido;
- reutilizar integralmente `CotacaoProvider` e os adapters atuais;
- preservar as validações de moeda, identidade, cotação e precisão existentes;
- atualizar somente cotação e timestamp em uma transação final curta;
- impedir regressão temporal sob concorrência sem introduzir coluna de versão;
- preservar a última cotação válida e informar isso em falhas;
- manter os contratos existentes de cadastro e consulta.

**Non-Goals:**

- alterar ticker, mercado, moeda ou nome da empresa;
- criar histórico, tabela, auditoria ou origem da cotação;
- implementar atualização automática, agendada ou em lote;
- adicionar consulta por ticker, novos GETs ou qualquer endpoint mutável além da cotação;
- modificar os fluxos HTTP externos atualmente executados pelos adapters;
- alterar Carteira, Operação, posição ou cálculos financeiros.

## Decisions

### 1. Contrato aprovado: `PATCH /acoes/{id}/cotacao`

Usar `PATCH` porque o recurso existente terá apenas dois campos atualizados e os novos valores serão determinados pelo servidor. A URI usa o substantivo `cotacao` em vez do verbo `atualizar-cotacao`, seguindo um contrato orientado a recurso. A requisição não terá DTO de negócio; qualquer corpo será rejeitado como `REQUEST_INVALIDO` antes de acessar o service.

O sucesso retornará `200 OK` com `AcaoResponse` completo e sem `Location`. `204 No Content` foi descartado porque o cliente precisa distinguir o valor efetivamente persistido, inclusive quando a cotação é igual ou quando uma candidata temporalmente antiga é descartada. `PUT /acoes/{id}/atualizar-cotacao`, sugerido inicialmente pelo PRD, e `POST /acoes/{id}/atualizacoes-cotacao` foram preteridos porque o primeiro combina verbo na URI com uma representação que o cliente não fornece e o segundo sugere a criação de um recurso de histórico fora do escopo.

### 2. Orquestração em duas fases

`AcaoService.atualizarCotacao(Long id)` não será transacional e executará:

1. leitura da Ação e captura de ID, ticker, mercado, moeda, cotação e timestamp atuais;
2. seleção do provider pelo `Map<Mercado, CotacaoProvider>` já existente;
3. chamada `provider.consultar(tickerPersistido)` fora de transação;
4. validação da resposta e determinação do timestamp UTC fora da transação;
5. delegação da alteração para uma seção transacional final;
6. mapeamento da entidade efetivamente persistida para `AcaoResponse`.

A Ação inexistente falhará antes do provider. Se ela deixar de existir entre as fases, a seção final também responderá 404. Nenhuma chamada HTTP ficará sob transação ou lock de banco.

### 3. Persistência dedicada e mutação de domínio restrita

Criar `AcaoCotacaoPersistenceService` para manter `AcaoPersistenceService` restrito ao cadastro e delimitar uma transação curta de atualização. O novo serviço carregará a linha com lock pessimista de escrita por um método dedicado do repository, comparará a referência temporal e persistirá somente quando a candidata for posterior.

Adicionar a `Acao` o método aprovado `atualizarCotacao(BigDecimal, OffsetDateTime)`, sem setters genéricos. Esta alteração mínima da entidade mantém a operação explícita no domínio. Uma query JPQL de update evitaria editar a classe, mas espalharia a regra de mutação pelo repository, contornaria o estado gerenciado e exigiria releitura adicional para produzir a resposta.

O repository usará uma consulta JPA dedicada com `PESSIMISTIC_WRITE` somente na seção final. Não serão adicionados `@Version`, coluna, changeSet ou dependência.

### 4. Fluxo BRASIL

Para `mercado=BRASIL`, usar o `BrapiAdapter` existente com o ticker persistido. A resposta deverá manter moeda `BRL`, nome utilizável, cotação válida e ticker compatível. Timestamp utilizável será normalizado para UTC; valor ausente ou inválido usará o `Clock` da aplicação.

Se `tickerAlteradoExplicitamente=true` e o ticker retornado for diferente, não atualizar ticker nem usar silenciosamente a cotação do novo símbolo. Retornar `409 Conflict`, código `TICKER_CANONICO_DIVERGENTE` e `details` com `tickerPersistido` e `tickerCanonicoRetornado`. A migração de identidade ficará para change futura.

### 5. Fluxo EUA

Para `mercado=EUA`, reutilizar o `AlphaVantageAdapter` existente. Ele continuará executando `SYMBOL_SEARCH`, exigirá correspondência exata nos EUA, usará `OVERVIEW` apenas quando a busca não tiver nome utilizável e consultará `GLOBAL_QUOTE` para a última cotação. A moeda deverá permanecer `USD` e o último preço disponível continuará sem garantia de tempo real.

O valor `latest trading day`, quando for apenas data sem offset/horário confiável, continuará não sendo tratado como timestamp utilizável; nesse caso, o service usará o instante UTC em que recebeu a cotação.

### 6. Validação, campos preservados e igualdade de preço

Reutilizar as regras atuais do cadastro para validar ticker retornado, nome utilizável, moeda coerente, cotação positiva e representação exata em escala 6 e precisão 19. A validação poderá ser reorganizada dentro de `AcaoService` para evitar duplicação, sem criar uma nova abstração pública.

`nomeEmpresa`, ticker, mercado e moeda serão sempre preservados. Se o preço for igual mas o timestamp for posterior, atualizar somente `dataHoraCotacao`. Se a candidata possuir timestamp igual ou anterior ao persistido, retornar o estado atual com `200 OK` sem executar update. Essa política torna a operação monotônica e idempotente para a mesma observação do provider.

### 7. Concorrência sem mudança de schema

O lock pessimista será adquirido somente depois da chamada externa. Sob o lock, o serviço relerá o registro e comparará `dataHoraCotacao` com a candidata normalizada. Apenas timestamp estritamente posterior poderá alterar a linha. Assim, duas atualizações concorrentes podem consultar os providers em paralelo, mas a finalização serializada não permite que uma observação antiga sobrescreva a mais nova.

O snapshot inicial não será usado para gravar a entidade. A persistência sempre aplicará a mudança sobre a releitura protegida, evitando sobrescrever outros campos quando futuras funcionalidades puderem alterá-los.

### 8. Falhas preservam e informam a última cotação

Reutilizar os status e códigos atuais: `404/TICKER_INEXISTENTE`, `429/LIMITE_REQUISICOES_EXCEDIDO`, `503/SERVICO_EXTERNO_INDISPONIVEL`, `504/SERVICO_EXTERNO_TIMEOUT`, `502/RESPOSTA_EXTERNA_INVALIDA`, `422/DADOS_EXTERNOS_INCOMPLETOS`, `422/COTACAO_INDISPONIVEL` e `422/COTACAO_FORA_DA_PRECISAO`.

Quando a Ação existe e a falha ocorre depois da leitura inicial, o service preservará status, código e mensagem da `ApiException` e complementará `details` com:

- `acaoId`;
- `cotacaoPreservada=true`;
- `ultimaCotacaoValida`;
- `dataHoraUltimaCotacao`.

Nenhum desses erros executará a seção transacional de escrita. Ação inexistente permanece no tratamento atual de `ObjectNotFoundException`, sem dados de última cotação.

### 9. Testes sem integrações reais

Testes de service usarão mocks de `CotacaoProvider`, repository e persistência para cobrir seleção por mercado, validação, timestamp, identidade, igualdade, erros e ausência de escrita. Testes do serviço transacional e repository usarão H2 para lock, alteração restrita e não regressão temporal. Testes HTTP validarão rota, corpo proibido, resposta completa e erros padronizados. Os testes existentes dos adapters continuam simulando HTTP, e nenhuma suíte dependerá de BRAPI ou Alpha Vantage reais.

## Risks / Trade-offs

- [Lock pessimista pode reduzir throughput para muitas atualizações simultâneas da mesma Ação] → manter o lock apenas na persistência final, nunca durante a chamada externa.
- [Timestamp do provider pode ser antigo ou pouco confiável] → usar somente valores parseáveis/confiáveis, normalizar em UTC e impedir regressão temporal; usar o `Clock` quando não houver timestamp utilizável.
- [Retorno de ticker canônico pode impedir atualizações de um ativo renomeado] → preservar identidade e última cotação com erro explícito até existir fluxo de migração aprovado.
- [O provider retorna nome, mas esta change não o atualiza] → manter escopo restrito e evitar alteração cadastral indireta; futura revalidação de metadados deverá ser separada.
- [Detalhes com a última cotação ampliam o contrato de erro] → adicionar somente campos não sensíveis e preservar status, código e estrutura atuais.
- [H2 e PostgreSQL podem tratar lock de forma diferente] → testar a regra temporal no H2 e validar o comportamento concorrente no PostgreSQL de desenvolvimento quando disponível.

## Migration Plan

1. Adicionar o contrato HTTP e os códigos estritamente necessários.
2. Adicionar a mutação restrita de cotação e a leitura protegida no repository.
3. Implementar o serviço transacional final e a orquestração sem transação externa.
4. Criar testes unitários, H2, HTTP e de concorrência; executar a suíte completa e o build.
5. Validar OpenSpec e atualizar Graphify após a futura implementação.

Não existe migração de schema ou dados. O rollback consiste em remover endpoint, métodos e testes acrescentados; as últimas cotações persistidas continuam valores válidos e não exigem rollback de banco.
