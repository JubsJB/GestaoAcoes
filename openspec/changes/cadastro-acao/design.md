## Context

Veja `proposal.md` para a motivação e `specs/stock-registration/spec.md` para o contrato aprovado. O PRD define Ação com `id`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `cotacaoAtual` e `dataHoraCotacao`; permite o cadastro informando ticker e mercado; exige validação no provedor correspondente; e proíbe duplicidade do mesmo ticker no mesmo mercado. As decisões complementares necessárias ao contrato e à persistência foram aprovadas e estão consolidadas neste documento.

O Graphify mostra que o projeto já possui a estrutura em camadas de Corretora (`Resource`, `Service`, `PersistenceService`, `Repository`, `Mapper` e DTOs), providers/adapters para integrações, `RestClient` síncrono com timeouts, relógio UTC, tratamento centralizado por `ApiException`/`ErrorCodes`/`ResourceExceptionHandler`, Liquibase e Hibernate em `ddl-auto=validate`. Esta change deve estender esses padrões sem alterar Corretora.

As documentações oficiais consultadas mostram uma assimetria entre os provedores. A BRAPI oferece cotação, nomes, moeda e horário de mercado em uma consulta e recomenda autenticação Bearer no backend. A Alpha Vantage oferece `SYMBOL_SEARCH` para símbolo/nome/mercado e `GLOBAL_QUOTE` para preço, exige `apikey`, pode comunicar limite ou erro em payload de sucesso HTTP e, no acesso padrão, pode fornecer cotação atualizada somente ao fim do pregão. O desenho não pode assumir que ambos os provedores têm o mesmo contrato.

## Goals / Non-Goals

**Goals:**

- Criar uma fatia vertical de cadastro que seja testável sem rede e sem PostgreSQL real.
- Manter a seleção de provedor e a consolidação de dados fora do resource e da entidade.
- Separar as chamadas externas da transação curta de verificação final e persistência.
- Representar dinheiro com `BigDecimal`, tempo com `OffsetDateTime` UTC e mercado/moeda com enums.
- Evoluir o schema pelo mesmo changelog Liquibase usado em desenvolvimento e teste.
- Preservar o formato de erros e as configurações externas já adotados no projeto.

**Non-Goals:**

- Generalizar o domínio para todos os tipos de ativos ou investimentos.
- Criar cache, retentativa automática, circuit breaker, processamento assíncrono ou cliente reativo.
- Criar histórico de cotações ou usar cotação futura para alterar o registro histórico de operações.
- Relacionar Ação a Corretora, Carteira, Operação, investidor ou usuário.
- Implementar qualquer GET, atualização de cotação, cálculo financeiro ou frontend.

## Decisions

### 1. Repetir a estrutura em camadas já estabelecida

A implementação proposta adicionará `AcaoResource`, DTOs de entrada e saída, `AcaoService`, `AcaoPersistenceService`, `AcaoRepository`, `AcaoMapper` e a entidade `Acao` sob os pacotes atuais de `com.projeto`. O resource validará o contrato HTTP e delegará; o service normalizará, selecionará provedor e consolidará os dados; o persistence service abrirá a transação curta; o repository cuidará somente da persistência.

Uma abstração equivalente a `CotacaoProvider` devolverá um modelo interno mínimo, sem expor DTOs da BRAPI ou Alpha Vantage. `BrapiAdapter` e `AlphaVantageAdapter` implementarão essa abstração e declararão o mercado suportado. O service escolherá exatamente um provider pelo enum `Mercado`.

Alternativa considerada: criar um serviço genérico de ativos ou um framework de providers. Foi rejeitada por antecipar outros investimentos e adicionar abstração além dos dois mercados definidos no PRD.

### 2. Restringir o request a ticker e mercado

O request será:

```json
{
  "ticker": "PETR4",
  "mercado": "BRASIL"
}
```

Jackson continuará configurado para rejeitar propriedades desconhecidas, de modo que nome, moeda, cotação e horário não possam ser enviados como substitutos da consulta externa. `mercado` será desserializado diretamente como enum e entradas não suportadas seguirão o erro `REQUEST_INVALIDO`.

O sucesso será `201 Created`, com `Location: /acoes/{id}` e corpo `AcaoResponse` completo, por consistência com Corretora. Não será implementado `GET /acoes/{id}` nesta fatia; a URI apenas estabelece o identificador estável futuro.

Alternativa considerada: `200 OK` ou `204 No Content`. Foi rejeitada porque o POST cria um recurso e o cliente precisa conhecer o valor consolidado pelos provedores.

### 3. Aplicar normalização mínima e preservar a identidade do símbolo

O ticker será submetido a `trim()` e `toUpperCase(Locale.ROOT)`. A aplicação não acrescentará `.SA`, não trocará ponto por hífen e não removerá caracteres internos: diferenças de notação entre provedores devem permanecer nos adapters. O provider será a autoridade para confirmar a existência do símbolo.

Antes da rede, o service verificará duplicidade com o ticker normalizado solicitado. Depois da consulta, repetirá a verificação com o ticker canônico devolvido pelo provider. Se a BRAPI indicar explicitamente renomeação, o sistema persistirá e responderá o símbolo canônico; a Alpha Vantage somente será aceita quando `SYMBOL_SEARCH` trouxer correspondência exata no mercado dos EUA.

Alternativas consideradas: impor uma regex única a mercados diferentes, que poderia rejeitar símbolos válidos; aceitar a melhor correspondência aproximada da Alpha Vantage, que poderia cadastrar outro ativo; e persistir ticker descontinuado apesar de a BRAPI fornecer o atual.

### 4. Derivar mercado, moeda e provedor de forma determinística

Os enums são:

```text
Mercado: BRASIL, EUA
Moeda:   BRL, USD
```

O mapeamento será:

| Mercado | Provider | Moeda persistida |
|---|---|---|
| `BRASIL` | BRAPI | `BRL` |
| `EUA` | Alpha Vantage | `USD` |

O cliente não fornecerá moeda. O adapter deverá validar moeda e região quando o provedor disponibilizar esses atributos; uma divergência será tratada como resposta externa inválida, sem conversão cambial.

Alternativa considerada: confiar na moeda retornada e persistir qualquer valor. Foi rejeitada porque o modelo desta change aceita somente BRL e USD e o mercado solicitado é parte da identidade.

### 5. Obter nome e cotação no próprio cadastro

O modelo interno retornado pelo provider conterá, no mínimo, ticker confirmado ou canônico, nome da empresa, moeda informada pelo provedor quando disponível, cotação e timestamp da cotação quando existir. O cadastro somente será persistido com nome utilizável e cotação presente e maior que zero.

Para `BRASIL`, `BrapiAdapter` fará uma consulta de cotação para um ticker e extrairá `symbol` ou o símbolo canônico, `longName` com fallback para `shortName`, `currency`, `regularMarketPrice` e `regularMarketTime` quando este for utilizável. Resultado vazio significa ticker inexistente. O horário de mercado válido terá prioridade para `dataHoraCotacao`; `requestedAt` não será tratado como timestamp da cotação.

Para `EUA`, `AlphaVantageAdapter` fará:

```text
SYMBOL_SEARCH(keywords=ticker)
        ↓ correspondência exata + região EUA + nome
nome ausente? OVERVIEW(symbol=ticker)
        ↓ somente para completar o nome obrigatório
GLOBAL_QUOTE(symbol=ticker)
        ↓ símbolo confirmado + preço mais recente disponível
modelo interno consolidado
```

Essa estratégia usa `SYMBOL_SEARCH` para existência, mercado e nome. `OVERVIEW` somente será chamado quando a correspondência exata não trouxer nome utilizável. Depois de confirmar o símbolo e obter o nome obrigatório, `GLOBAL_QUOTE` fornecerá a última cotação disponibilizada pelo plano configurado. Um resultado aproximado nunca substituirá o ticker solicitado, e nenhuma chamada adicional será feita sem necessidade. Se a busca não confirmar exatamente o ativo, nem `OVERVIEW` nem `GLOBAL_QUOTE` serão chamados.

Para a Alpha Vantage, o último preço disponível poderá corresponder ao fechamento do último pregão. `cotacaoAtual` representa o último valor devolvido pelo provider e não constitui garantia de tempo real. Se a resposta não trouxer timestamp completo e confiável associado à cotação, será utilizado o fallback temporal da aplicação; uma data de pregão sem horário e offset não será convertida para um instante arbitrário.

Alternativas consideradas: chamar sempre `OVERVIEW`, rejeitado por consumir cota e retornar dados além desta fatia; aceitar correspondência aproximada, rejeitado por risco de cadastrar outro ativo; e persistir a Ação sem cotação, rejeitado porque criaria estado incompleto e exigiria outra capability.

### 6. Priorizar o timestamp da cotação e usar fallback UTC controlado

`dataHoraCotacao` usará preferencialmente o timestamp da própria cotação retornado pelo provider. Um timestamp será considerado utilizável e confiável quando estiver associado à cotação aceita, puder ser convertido de forma inequívoca para um instante e contiver offset ou informação temporal suficiente para a conversão definida pelo contrato do provider. O valor será normalizado com o mesmo instante em `ZoneOffset.UTC` antes da persistência.

Quando o provider omitir o timestamp, devolver valor inválido ou fornecer somente uma data sem horário/offset confiável, o service usará `OffsetDateTime.now(clock)` imediatamente após obter a cotação válida, com o `Clock.systemUTC()` já configurado. Não será fabricado horário de abertura, fechamento ou meia-noite. Não será criada coluna adicional nem histórico. A resposta usará ISO-8601 com `Z` ou `+00:00`.

Alternativas consideradas: usar sempre o instante da consulta, rejeitado porque descartaria um timestamp real da cotação; e converter a data do pregão americano para horário arbitrário, rejeitado porque criaria informação inexistente.

### 7. Persistir um único agregado de Ação

O modelo aprovado é:

| Campo Java | Tipo Java | Coluna | Tipo de banco | Restrição |
|---|---|---|---|---|
| `id` | `Long` | `id` | `BIGINT` | chave primária, identidade, não nulo |
| `ticker` | `String` | `ticker` | `VARCHAR(30)` | não nulo |
| `nomeEmpresa` | `String` | `nome_empresa` | `VARCHAR(255)` | não nulo |
| `mercado` | `Mercado` | `mercado` | `VARCHAR(10)` | não nulo, enum como texto |
| `moeda` | `Moeda` | `moeda` | `VARCHAR(3)` | não nulo, enum como texto |
| `cotacaoAtual` | `BigDecimal` | `cotacao_atual` | `NUMERIC(19,6)` | não nulo, maior que zero |
| `dataHoraCotacao` | `OffsetDateTime` | `data_hora_cotacao` | `TIMESTAMP WITH TIME ZONE` | não nulo, UTC |

A tabela não terá relação com `corretora`. A constraint única será `(ticker, mercado)`. Checks portáveis limitarão `mercado`, `moeda`, a combinação mercado/moeda e a cotação positiva. DTOs separarão a API da entidade JPA.

Alternativas consideradas: entidade separada de Cotação, rejeitada porque histórico não está no escopo; `double`, rejeitado pelas regras de precisão; e persistir enums por ordinal, rejeitado por fragilidade na evolução.

Antes da persistência, a aplicação validará que a cotação cabe exatamente em precisão total 19 e escala máxima 6. A validação poderá remover apenas zeros decimais não significativos e usará uma operação equivalente a `setScale(6, RoundingMode.UNNECESSARY)`; qualquer necessidade de truncamento ou arredondamento produzirá `422/COTACAO_FORA_DA_PRECISAO`. A mesma falha ocorrerá quando a parte inteira exceder a capacidade de `NUMERIC(19,6)`, antes de alcançar o banco.

### 8. Não persistir origem separada nesta fatia

RF08 pede origem “quando necessário”. Como `mercado` determina univocamente o provider nesta change, não será adicionado `origemCotacao`: `BRASIL` implica BRAPI e `EUA` implica Alpha Vantage. A origem pode ser derivada sem duplicar estado.

Alternativa considerada: coluna e campo de resposta `origemCotacao`. Foi rejeitada porque não está no modelo de Ação do PRD e não há múltiplos providers por mercado nesta fatia.

### 9. Manter chamadas externas fora da transação

O fluxo do service será:

```text
validar request e normalizar
        ↓
verificar duplicidade antecipada
        ↓
consultar provider fora de transação
        ↓
validar e consolidar dados + gerar instante UTC
        ↓
transação curta: verificar ticker canônico + salvar
```

A constraint única encerra a condição de corrida. A violação específica de `(ticker, mercado)` será traduzida para `ACAO_DUPLICADA`, sem alterar o tratamento de unicidade de Corretora. Nenhuma chamada de rede ocorrerá em transação de banco.

Alternativa considerada: anotar todo o cadastro com `@Transactional`. Foi rejeitada porque manteria conexão e transação abertas durante até duas chamadas externas.

### 10. Reutilizar RestClient, timeouts e configuração externa

`ExternalApiConfig` ganhará dois clientes síncronos qualificados, seguindo o padrão atual. As propriedades serão:

```properties
integration.brapi.base-url=${BRAPI_BASE_URL:https://brapi.dev}
integration.brapi.api-key=${BRAPI_API_KEY:}
integration.brapi.connect-timeout=${BRAPI_CONNECT_TIMEOUT:2s}
integration.brapi.read-timeout=${BRAPI_READ_TIMEOUT:5s}

integration.alpha-vantage.base-url=${ALPHA_VANTAGE_BASE_URL:https://www.alphavantage.co}
integration.alpha-vantage.api-key=${ALPHA_VANTAGE_API_KEY:}
integration.alpha-vantage.connect-timeout=${ALPHA_VANTAGE_CONNECT_TIMEOUT:2s}
integration.alpha-vantage.read-timeout=${ALPHA_VANTAGE_READ_TIMEOUT:5s}
```

Os nomes serão documentados em `.env.example`, sem valores reais. A BRAPI receberá Bearer no header e a Alpha Vantage receberá `apikey` conforme seu contrato. Chaves nunca serão registradas em logs ou incluídas em erros. Cada adapter validará sua própria configuração somente quando for utilizado: chave BRAPI ausente não impedirá cadastros `EUA`, e chave Alpha Vantage ausente não impedirá cadastros `BRASIL` nem funcionalidades de Corretora. O uso do provider sem configuração falhará claramente como serviço externo indisponível.

Alternativas consideradas: adicionar WebFlux, OpenFeign ou SDKs, rejeitados por não serem necessários; e tornar as chaves obrigatórias no bootstrap, rejeitado para não impedir o outro mercado, Corretora e testes isolados quando um provider não for usado.

### 11. Ampliar o padrão atual de erros sem quebrar Corretora

Serão adicionados códigos específicos, preservando `StandardError`, `ApiException` e os códigos externos existentes:

| Situação | HTTP | Code |
|---|---:|---|
| ticker local vazio/inválido | 400 | `TICKER_INVALIDO` |
| mercado inválido | 400 | `REQUEST_INVALIDO` |
| ticker não encontrado no mercado | 404 | `TICKER_INEXISTENTE` |
| ticker e mercado duplicados | 409 | `ACAO_DUPLICADA` |
| cotação ausente ou inválida | 422 | `COTACAO_INDISPONIVEL` |
| cotação não representável exatamente em `NUMERIC(19,6)` | 422 | `COTACAO_FORA_DA_PRECISAO` |
| nome ou outro dado externo obrigatório ausente | 422 | `DADOS_EXTERNOS_INCOMPLETOS` |
| limite identificado por HTTP ou payload | 429 | `LIMITE_REQUISICOES_EXCEDIDO` |
| payload incompatível | 502 | `RESPOSTA_EXTERNA_INVALIDA` |
| indisponibilidade ou provider não configurado | 503 | `SERVICO_EXTERNO_INDISPONIVEL` |
| timeout | 504 | `SERVICO_EXTERNO_TIMEOUT` |

`ExternalApiErrorMapper` será reutilizado e ampliado para respostas de limite ou erro que chegam com HTTP 200, especialmente `Note`, `Information` ou `Error Message` da Alpha Vantage. Mensagens externas não confiáveis não serão repassadas integralmente ao cliente.

### 12. Evoluir Liquibase com um único changeSet de Ação

O master atual passará a incluir, depois de `001-create-corretora.yaml`:

```text
src/main/resources/db/changelog/
├── db.changelog-master.yaml
└── changes/
    ├── 001-create-corretora.yaml
    └── 002-create-acao.yaml
```

`002-create-acao.yaml` criará somente `acao`, chave primária, colunas, checks e unicidade definidos acima. PostgreSQL e H2 executarão o mesmo arquivo; Hibernate continuará com `ddl-auto=validate`. Não será adicionada dependência e não haverá tabela de cotação, carteira ou operação.

O rollback explícito removerá somente `acao`, mas deverá ser autorizado operacionalmente fora de bancos descartáveis porque destrói os registros cadastrados.

### 13. Testar providers sem rede real

Testes unitários do service usarão implementações substitutas de `CotacaoProvider` e relógio fixo. Testes dos adapters usarão servidor HTTP simulado ou o mecanismo já empregado pelos testes de BrasilAPI/ViaCEP. Testes de repository, Liquibase, endpoint e contexto usarão H2 e o changelog real.

Os cenários cobrirão ambos os mercados, seleção exclusiva do provider, normalização, correspondência americana exata, `OVERVIEW` somente quando faltar nome, ausência de chamadas desnecessárias, ticker inexistente, ticker canônico BRAPI, dados incompletos, último fechamento americano, preço inválido, precisão sem arredondamento, duplicidade antecipada e concorrente, timestamp confiável normalizado para UTC, fallback por relógio fixo, `201/Location`, configuração independente, timeouts, indisponibilidade, payload inválido e limites expressos por status ou corpo. Nenhum teste dependerá de chave válida ou chamada real.

## Risks / Trade-offs

- [Cada cadastro americano consome ao menos duas chamadas e a cota gratuita atual da Alpha Vantage é limitada] → Interromper após busca sem correspondência, verificar duplicidade antes da rede, mapear payload de limite para 429 e não adicionar retentativa automática.
- [A cotação padrão da Alpha Vantage pode ser somente de fechamento do último pregão, não em tempo real] → Documentar em contrato e resposta que a primeira fatia usa a última cotação disponibilizada pelo plano configurado e não garante tempo real.
- [A BRAPI exige token para cobertura de produção além de poucos símbolos de demonstração] → Configurar chave externamente e falhar de modo explícito quando a integração necessária não estiver configurada.
- [Ticker antigo pode ser resolvido para um símbolo novo pela BRAPI] → Persistir o canônico explicitamente informado e revalidar duplicidade depois da resolução.
- [Timestamp de provider pode estar ausente, incompleto ou sem offset] → Usá-lo somente quando puder ser associado e convertido inequivocamente; caso contrário, aplicar o fallback pelo relógio UTC sem fabricar horário.
- [Cotação externa pode exceder a precisão aprovada] → Validar representação exata antes da transação e responder `COTACAO_FORA_DA_PRECISAO`, sem deixar o banco arredondar.
- [H2 e PostgreSQL podem divergir em checks, identidade e timestamp com timezone] → Usar tipos portáveis, executar o mesmo changelog no H2 e prever validação no PostgreSQL de desenvolvimento quando configurado.
- [A constraint composta pode colidir sob concorrência] → Traduzir a violação específica para o mesmo erro `ACAO_DUPLICADA` e manter a transação curta.
- [Rollback da tabela remove dados] → Nunca executar automaticamente; exigir autorização operacional e preservar migrações aplicadas em ambientes com dados.
- [Chaves em query string podem aparecer em logs HTTP] → Não habilitar log de URI com query sensível e nunca incluir a chave em mensagens, `details` ou fixtures versionadas.

## Migration Plan

1. Adicionar configuração externa e clientes BRAPI/Alpha Vantage, sem credenciais reais e com ativação independente.
2. Incluir `002-create-acao.yaml` no master e validar Liquibase seguido de Hibernate no H2.
3. Implementar modelo, repository, DTOs, mapper, providers/adapters, service transacional curto, resource e erros.
4. Executar testes direcionados, suíte completa e build pelo Maven Wrapper.
5. Quando houver PostgreSQL e credenciais de desenvolvimento disponíveis, aplicar o changelog e confirmar `ddl-auto=validate` sem alteração automática do schema.
6. Validar OpenSpec, atualizar Graphify após o código e revisar diff/status durante a futura implementação.

Rollback: remover o endpoint e componentes por reversão de código. Não executar o rollback Liquibase de `002-create-acao.yaml` em ambiente com dados sem autorização explícita; nunca substituir a migração por `ddl-auto=create`, `update` ou `create-drop`.
