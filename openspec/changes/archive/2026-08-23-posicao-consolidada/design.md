## Context

Veja `proposal.md` para a motivação e `specs/portfolio-position/spec.md` para o contrato proposto. O PRD, especialmente RF17–RF20, RN12–RN18 e as seções 12.1, 12.2 e 12.5, define quantidade atual, custo da posição, preço médio ponderado, venda sem alteração do preço médio remanescente e reinício após encerramento. O PRD também prevê resultado realizado, mas não exige que ele integre a mesma representação de posição aberta.

As capabilities promovidas já fixam `Operacao` como fonte histórica, com quantidade e preço `NUMERIC(19,6)`, valor total `NUMERIC(38,12)`, `dataOperacao`, `ordemNoDia` e relações obrigatórias com Carteira e Ação. A cronologia financeira é `dataOperacao ASC, ordemNoDia ASC`; `id` existe apenas como terceiro desempate técnico em listagens que misturam grupos independentes. O cadastro valida por replay que nenhum prefixo de Carteira+Ação fique negativo e usa lock curto de Carteira somente em escrita.

No código atual, `CarteiraResource` já hospeda a leitura aninhada do histórico e delega a `OperacaoService`. `OperacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc` já entrega todo o histórico de uma Carteira. O replay quantitativo está privado em `OperacaoService.validateReplay`, e não há abstração financeira, posição persistida ou migration posterior ao changeSet 004. `Acao` já fornece ID, ticker, nome, mercado e moeda persistidos; sua cotação não deve participar desta change.

O Graphify confirmou as relações `CarteiraResource → OperacaoService/CarteiraService`, `OperacaoService → OperacaoRepository/CarteiraRepository/OperacaoMapper` e o RF17 do PRD. Como o próprio Graphify sinalizou que o código mudou desde uma memória anterior, as decisões abaixo foram conferidas também diretamente nos arquivos atuais.

As decisões funcionais e técnicas desta change foram aprovadas e estão consolidadas abaixo. Não há decisão bloqueante remanescente para a implementação.

## Goals / Non-Goals

**Goals:**

- calcular uma visão contábil determinística por Carteira+Ação sem criar segunda fonte de verdade;
- centralizar quantidade, custo e preço médio em componente puro, isolado de HTTP e persistência;
- preservar a semântica de venda, zeramento e novo ciclo definida no PRD;
- explicitar precisão, escala e arredondamento, inclusive para decimais periódicos;
- produzir uma leitura consistente, sem lock de escrita, efeito colateral ou integração externa;
- permitir reutilização pequena do replay quantitativo pelo cadastro sem refatoração ampla.

**Non-Goals:**

- persistir posição, custo, preço médio ou snapshot;
- calcular resultado realizado nesta resposta, resultado não realizado, valor atual, rentabilidade, patrimônio ou câmbio;
- consultar cotação atual/histórica ou provider externo;
- criar endpoint individual de posição, filtros, paginação, cache, materialized view ou processamento assíncrono;
- alterar entidade, Liquibase, schema, dependências, configurações ou contratos de Operação existentes;
- modelar taxas, corretagem, emolumentos, impostos, dividendos ou eventos corporativos.

## Decisions

### 1. Expor somente `GET /carteiras/{carteiraId}/posicoes`

O endpoint definido é uma listagem aninhada em Carteira:

```text
GET /carteiras/{carteiraId}/posicoes
```

Carteira existente produz `200 OK`; se não houver Operações ou todas as posições estiverem encerradas, o corpo é `[]`. Carteira inexistente reutiliza `ObjectNotFoundException`, `ResourceExceptionHandler` e `StandardError` para `404 Not Found`. Não há `Location` nem corpo de entrada.

Não será criado `GET /carteiras/{carteiraId}/posicoes/{acaoId}` nesta primeira fatia: a lista já atende RF17 e evita contrato adicional. Filtros e paginação também ficam fora desta capability.

Alternativas consideradas: endpoint de nível superior `/posicoes`, que perderia o contexto obrigatório da Carteira; e consulta individual imediata, que amplia testes e política de identificação sem requisito atual.

### 2. Manter a rota no `CarteiraResource` e delegar a `PosicaoService`

`CarteiraResource` receberá `PosicaoService` e apenas devolverá `ResponseEntity.ok(posicaoService.listarPorCarteira(carteiraId))`. A validação da Carteira, leitura, agrupamento, cálculo e mapeamento não ficarão no resource nem em `CarteiraService`.

`PosicaoService` será uma responsabilidade específica de aplicação, porque o cálculo não é mera consulta de `OperacaoResponse` e não deve ampliar `OperacaoService` com uma segunda razão de mudança. Ele reutilizará `CarteiraRepository` e `OperacaoRepository` existentes; nenhum repository novo será criado.

Alternativas consideradas: colocar tudo em `OperacaoService`, que mistura registro/consulta de fatos com projeção financeira; e criar `PosicaoResource`, desnecessário para uma única rota já naturalmente aninhada em `/carteiras`.

### 3. Retornar um `PosicaoResponse` específico e autossuficiente

O contrato definido é:

```json
[
  {
    "acaoId": 1,
    "ticker": "PETR4",
    "nomeEmpresa": "Petróleo Brasileiro S.A.",
    "mercado": "BRASIL",
    "moeda": "BRL",
    "quantidadeAtual": 100.000000,
    "precoMedio": 14.000000000000,
    "custoPosicao": 1400.000000000000
  }
]
```

`acaoId` oferece vínculo estável com `GET /acoes/{id}` sem ser aceito como entrada; ticker+mercado preservam a identidade compreensível; `nomeEmpresa` evita que o consumidor faça consulta adicional apenas para exibição; `moeda` torna preço e custo semanticamente inequívocos entre BRL e USD. Todos vêm da Ação já persistida e não implicam provider.

Não serão incluídos cotação, timestamp de cotação, resultado, valor atual ou indicador de mercado. Um `PosicaoMapper` pequeno mapeará `Acao + PosicaoCalculada` para o record, sem executar regra financeira.

Alternativa considerada: DTO mínimo sem ID/nome/moeda. É menor, porém custo e preço ficariam sem moeda e o cliente precisaria resolver a Ação para dados básicos. Também foi considerada a omissão de `acaoId`, seguindo `OperacaoResponse`; ele foi mantido porque a posição é uma projeção explicitamente agrupada pela entidade Ação e pode referenciá-la diretamente.

### 4. Ordenar posições por mercado, ticker e ID técnico

A resposta será determinística por `mercado ASC`, `ticker ASC`, `acaoId ASC`. Essa ordem é somente de apresentação das posições; não participa do replay. Dentro de cada grupo, a ordem financeira permanece data e ordem no dia.

Alternativas consideradas: ordem de primeiro aparecimento no histórico, que varia conforme operações; e somente `acaoId`, estável mas pouco orientado ao usuário.

### 5. Reutilizar a consulta cronológica já existente

O service validará a Carteira com `CarteiraRepository.findById` e carregará o histórico por `OperacaoRepository.findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc`. Não haverá SQL agregado: `SUM` não preserva o custo médio vigente em cada venda nem os ciclos encerrados.

O resultado será agrupado em memória por `acao.id`, preservando em cada grupo a ordem relativa recebida. Como a constraint vigente torna `(carteira, acao, data, ordemNoDia)` único, `id` não decide a ordem financeira dentro de uma posição; ele estabiliza somente a leitura global entre Ações independentes.

Nenhum novo método de repository é necessário. O mapeamento e o acesso às relações `LAZY` ocorrerão dentro da transação read-only.

### 6. Introduzir `CalculadoraPosicao` como componente puro de replay

Será criada uma abstração pequena, sem repository, HTTP, relógio ou estado mutável compartilhado:

```text
CalculadoraPosicao
  reproduzir(operacoesOrdenadas) -> ResultadoReplay
```

`ResultadoReplay` será um tipo interno que contém o estado final e, quando houver, o ponto de inconsistência com quantidade disponível e solicitada. `PosicaoService` transforma resultado válido em `PosicaoResponse` e traduz inconsistência persistida para erro padronizado.

Para eliminar duplicação da regra quantitativa, `OperacaoService.validateReplay` delegará à mesma calculadora após inserir e ordenar a candidata, mas continuará traduzindo falha para o contrato vigente `409/POSICAO_INSUFICIENTE`. Isso é uma extração localizada: não muda locks, transação, queries, DTOs ou resposta do cadastro.

Alternativas consideradas:

- deixar o replay atual duplicado: menor diff, porém duas implementações poderiam divergir sobre saldo negativo e cronologia;
- mover tudo para `OperacaoService`: reduz classes, mas torna regra financeira difícil de testar isoladamente e acopla consulta de posição ao cadastro;
- criar entidade `Posicao`: introduz persistência e consistência fora do escopo.

### 7. Executar um único fold por Carteira+Ação

Para cada grupo, o estado começa em:

```text
quantidade = 0
custo = 0
precoMedio = 0
```

Para `COMPRA`:

```text
custoCompra = quantidadeCompra × precoUnitarioCompra
novoCusto = custo + custoCompra
novaQuantidade = quantidade + quantidadeCompra
novoPrecoMedio = novoCusto / novaQuantidade
```

Para `VENDA` parcial:

```text
novaQuantidade = quantidade - quantidadeVenda
novoCusto = custo × novaQuantidade / quantidade
novoPrecoMedio = precoMedio vigente
```

A forma proporcional do custo é matematicamente equivalente a subtrair `quantidadeVenda × precoMedioVigente`, mas executa uma única divisão controlada e deixa explícito que o preço da venda não participa. Se `novaQuantidade = 0`, os três estados são zerados exatamente.

Uma compra posterior soma apenas seu custo ao estado então vigente. Se ocorreu zeramento, o estado anterior já é zero e o novo preço médio começa no preço da nova compra. Não entram taxas porque o modelo de Operação não as possui.

### 8. Preservar quantidade exata e as regras de mercado

`quantidadeAtual` continuará `BigDecimal`, sem `float`/`double`. A soma e subtração serão exatas. Para `BRASIL`, o estado deverá permanecer matematicamente inteiro; para `EUA`, poderá manter até seis casas decimais. A calculadora não arredondará quantidade e tratará violação como histórico inconsistente, pois requests novos já são protegidos pelo cadastro e pelo schema.

### 9. Usar custo interno com escala 24 e arredondamento `HALF_EVEN`

Não será usado `MathContext` global, pois limitar algarismos significativos poderia descartar dígitos inteiros em valores grandes. Em vez disso:

- multiplicações, somas e subtrações permanecem exatas;
- o custo interno é mantido em escala 24;
- somente divisões inevitáveis usam `divide(..., 24, RoundingMode.HALF_EVEN)`;
- zeramento define zero exato e elimina qualquer resíduo decimal;
- a projeção final aplica escala 12 com `HALF_EVEN`.

A escala interna 24 fornece doze casas de guarda além da resposta e reduz propagação de erro em sequências de venda/compra. `HALF_EVEN` reduz viés cumulativo e é apropriado para cálculos financeiros repetidos. Toda ocorrência é explícita; não há arredondamento silencioso.

Alternativas consideradas: escala 6, que perde precisão em médias periódicas; escala 12 também internamente, que não oferece casas de guarda; `DECIMAL128`, que limita a precisão total a 34 dígitos; `HALF_UP`, comum em apresentação, mas com maior viés cumulativo; e aritmética racional ilimitada, desproporcional à primeira fatia.

### 10. Apresentar preço médio em escala 12 e custo em `38,12`

O response usa:

| Campo | Tipo Java | Limite lógico | Regra |
|---|---|---|---|
| `quantidadeAtual` | `BigDecimal` | precisão 19, escala até 6 | exato, sem arredondar |
| `precoMedio` | `BigDecimal` | precisão 25, escala 12 | `HALF_EVEN` a partir do estado interno |
| `custoPosicao` | `BigDecimal` | precisão 38, escala 12 | `HALF_EVEN` a partir do estado interno |

O preço médio precisa de até 13 dígitos inteiros porque não pode exceder o maior preço unitário válido (`NUMERIC(19,6)`), mais 12 casas para a média. O custo mantém o limite do produto já aprovado para `valorTotal`. Esses são limites contratuais em memória/JSON, não novas colunas.

Se o estado final exceder os limites, a resposta inteira falhará com `422 Unprocessable Entity` e `CALCULO_POSICAO_FORA_DA_PRECISAO`, sem truncamento nem lista parcial.

Alternativas consideradas: preço médio em escala 6, coerente com entrada mas insuficiente para divisões periódicas; custo calculado a partir do preço médio já arredondado para resposta, que introduziria perda prematura; e números sem limite contratual, que dificultam interoperabilidade futura.

### 11. Omitir posições zeradas e representar preço médio interno como zero

A listagem representa posições atualmente abertas; grupos com quantidade final zero serão omitidos. O histórico completo continua disponível pelos endpoints de Operação. Internamente, zeramento define `precoMedio=0` e `custo=0`, coerente com o PRD e necessário para que nova compra inicie ciclo independente.

Como a posição zerada não é devolvida, o cliente não observará um DTO com preço médio zero nesta versão. Internamente será usado zero, nunca `null`, para preservar aritmética total e a regra expressa no AGENTS.md.

Alternativas consideradas: devolver posições zeradas, que mistura posição atual com histórico; e `precoMedio=null`, que exige ramo adicional e diverge da regra explícita de zeramento do projeto.

### 12. Manter resultado realizado fora desta capability

Embora o mesmo replay possa calcular `(precoVenda - precoMedio) × quantidadeVendida`, ele não será incluído agora. Uma lista que omite posições zeradas perderia justamente resultados de ciclos totalmente encerrados; incluí-los exigiria definir se a resposta representa resultado histórico por Ação, por ciclo ou por Carteira e como tratar moedas distintas.

A calculadora deve conservar uma arquitetura capaz de acrescentar esse acumulador futuramente, mas não calcular nem expor o valor nesta change. Resultado não realizado, valor atual e rentabilidade dependem de cotação e permanecem igualmente fora.

Alternativa considerada: adicionar `resultadoRealizado` ao `PosicaoResponse`. Foi rejeitada porque amplia a semântica de posição aberta e cria lacuna para Ações encerradas.

### 13. Falhar integralmente diante de histórico inconsistente

Sob o schema e o cadastro atuais, saldo negativo, quantidade/preço não positivo, tipo desconhecido e ordem duplicada não deveriam existir. Se dados legados ou manipulação externa violarem invariantes, a consulta não deve mascarar o problema.

O contrato será `409 Conflict`, código `HISTORICO_OPERACOES_INCONSISTENTE`, com detalhes como `carteiraId`, `acaoId`, ticker, `operacaoId`, data, ordem, quantidade disponível e quantidade solicitada quando aplicável. Nenhuma Operação é alterada e nenhuma lista parcial é devolvida.

Impossibilidade defensiva de representar a saída usa `422/CALCULO_POSICAO_FORA_DA_PRECISAO`, separando corrupção/invariante de limite aritmético.

Alternativas consideradas: ignorar a Operação inválida, que falsifica a posição; retornar somente grupos válidos, que aparenta consolidação completa; e `500`, que oculta um conflito de estado diagnosticável.

### 14. Usar transação read-only com isolamento repetível, sem locks

`PosicaoService.listarPorCarteira` será `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`. Assim, a validação da Carteira e a leitura de Operações pertencem ao mesmo snapshot lógico em PostgreSQL e H2. Nenhum lock pessimista será adquirido; writers continuam usando o lock curto já aprovado.

Uma Operação concorrente aparece integralmente ou fica fora do snapshot da consulta. O cálculo ocorre depois de uma única leitura ordenada e dentro da transação, inclusive o acesso às relações `LAZY` e o mapeamento.

Alternativas consideradas: `READ_COMMITTED`, suficiente para cada SELECT mas sem garantir o mesmo snapshot entre a validação da Carteira e o histórico; lock pessimista, que bloquearia escrita numa consulta; e carregar Carteira/histórico em uma query especializada, que amplia repository antes de necessidade.

### 15. Calcular em memória com custo linear após a query ordenada

O banco executa filtro e ordenação usando o índice cronológico existente. No backend, agrupamento e replay percorrem cada Operação uma vez: tempo `O(n)` e memória `O(n)` para o histórico da Carteira na primeira versão. Não haverá cache, posição materializada, paginação ou processamento assíncrono.

O histórico completo por Carteira pode crescer; métricas e testes de carga deverão orientar eventual paginação interna, projeção otimizada ou materialização. Nenhuma dessas otimizações será antecipada sem evidência.

### 16. Não alterar Liquibase, entidades ou configurações

Os changeSets 001–004, o master, `Operacao`, `Carteira`, `Acao`, `Corretora`, dependências e `ddl-auto=validate` permanecerão inalterados. `PosicaoResponse`, estado interno e calculadora não são entidades. H2 e PostgreSQL continuarão usando o schema vigente.

## Risks / Trade-offs

- [Médias periódicas exigem aproximação decimal] → usar escala interna e resposta explícitas, `HALF_EVEN` e testes com valores de fronteira.
- [Arredondamento proporcional pode acumular em muitos eventos] → manter doze casas de guarda, zerar resíduos no encerramento e medir antes de aumentar a escala.
- [Extração do replay pode causar regressão no POST] → preservar o contrato `POSICAO_INSUFICIENTE`, o lock e a transação; cobrir a delegação com toda a regressão existente.
- [Histórico grande aumenta latência e memória] → usar o índice/query atuais, manter `O(n)` e instrumentar antes de cache ou materialização.
- [Relações LAZY podem produzir N+1] → mapear dentro da transação e medir; considerar `@EntityGraph` somente com evidência, sem mudar contrato.
- [Isolamento repetível pode manter versões de leitura por mais tempo] → transação curta, sem chamadas externas nem espera de usuário.
- [Omitir zerados exclui resultados de ciclos fechados] → preservar Operações e criar capability própria para resultado realizado/histórico.
- [Mistura de BRL e USD na mesma Carteira] → apresentar moeda por posição e não somar custos entre moedas.
- [Dados legados inconsistentes impedem toda a resposta] → fornecer código/detalhes diagnósticos e nunca corrigir histórico automaticamente.

## Migration Plan

1. Criar DTO, estado interno, calculadora e testes unitários de matemática antes da integração REST.
2. Integrar `PosicaoService` à query/read-only e ao `CarteiraResource`; extrair somente a delegação quantitativa necessária de `OperacaoService`.
3. Executar testes de service, resource, repository/H2, precisão, concorrência de leitura e regressão completa.
4. Confirmar que Liquibase/Hibernate validam 001–004 sem alteração e executar `clean verify`, OpenSpec strict e atualização do Graphify após código.

Rollback de aplicação remove o endpoint e os componentes de projeção e restaura a implementação privada anterior do replay, se necessário. Não há rollback de banco porque nenhum dado ou schema será criado ou alterado.
