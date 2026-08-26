## Context

Veja `proposal.md` para a motivação e `specs/portfolio-position/spec.md` para o contrato. O PRD define `valorAtual = quantidadeAtual × cotacaoAtual` e distingue a última cotação de mercado do preço efetivamente negociado. A capability promovida `stock-registration` já exige `Acao.cotacaoAtual` como `NUMERIC(19,6)`, positiva e não nula, e `dataHoraCotacao` como `OffsetDateTime` não nulo; a migration `002-create-acao` confirma `NOT NULL` e `CHECK (cotacao_atual > 0)`.

A implementação atual consolida sob demanda em `PosicaoService`, com `@Transactional(readOnly = true, isolation = REPEATABLE_READ)`, agrupa as Operações por Ação, delega o replay a `CalculadoraPosicao` e projeta o resultado em `PosicaoMapper`. `Operacao.acao` é `LAZY` e o método atual do repository não possui fetch plan explícito, de modo que acessar a Ação de cada grupo pode produzir N+1.

O Graphify relaciona diretamente `CarteiraResource`, `PosicaoService`, `CalculadoraPosicao`, `PosicaoMapper`, `PosicaoResponse`, `OperacaoRepository`, `Operacao` e `Acao`, além de confirmar que os providers pertencem ao fluxo de `AcaoService`, não ao de posição.

A baseline atual está novamente conforme a spec principal promovida de Ação: `PATCH /acoes/{id}/cotacao` está implementado como fluxo dedicado por `AcaoResource → AcaoService → CotacaoProvider → AcaoCotacaoPersistenceService → AcaoRepository`. A chamada ao provider ocorre fora da transação de escrita; a persistência final é curta, relê a Ação com `PESSIMISTIC_WRITE` e aplica a regra temporal monotônica. Esta change apenas consumirá `cotacaoAtual` e `dataHoraCotacao` já persistidas, sem duplicar ou alterar esse fluxo.

## Goals / Non-Goals

**Goals:**

- enriquecer a projeção da posição sem alterar o replay contábil;
- centralizar o cálculo do valor atual fora do mapper;
- reutilizar a Ação já relacionada às Operações em um fetch plan sem N+1;
- manter a fronteira transacional read-only e a independência dos providers;
- preservar exatamente a política numérica já aprovada.

**Non-Goals:**

- implementar ou redesenhar a atualização externa de cotação;
- introduzir tratamento para estados de cotação proibidos pelo schema suportado;
- materializar posição, adicionar cache, snapshot, conversão cambial ou agregação patrimonial;
- calcular resultados realizado/não realizado ou rentabilidade;
- alterar Operação, Acao, schema, migrations, dependências ou frontend.

## Decisions

### 1. Estender o `PosicaoResponse` existente

Adicionar ao final do record os campos:

```text
BigDecimal cotacaoAtual
OffsetDateTime dataHoraCotacao
BigDecimal valorAtualPosicao
```

A rota e o DTO existentes permanecem a superfície pública. Um novo endpoint ou DTO paralelo duplicaria a consolidação e permitiria contratos divergentes.

Contrato conceitual:

```json
{
  "acaoId": 1,
  "ticker": "PETR4",
  "nomeEmpresa": "Petróleo Brasileiro S.A.",
  "mercado": "BRASIL",
  "moeda": "BRL",
  "quantidadeAtual": 100.000000,
  "precoMedio": 32.000000000000,
  "custoPosicao": 3200.000000000000,
  "cotacaoAtual": 35.500000,
  "dataHoraCotacao": "2026-08-25T18:00:00Z",
  "valorAtualPosicao": 3550.000000000000
}
```

Alternativa considerada: novo endpoint “posição com cotação”. Rejeitada por duplicar semântica, replay, ordenação e tratamento de erros.

### 2. Calcular o valor atual em `CalculadoraPosicao`, depois do replay

`CalculadoraPosicao` ganhará uma operação explícita para multiplicar a `quantidadeAtual` já consolidada pela `cotacaoAtual` persistida. `PosicaoService` orquestrará replay, omissão de quantidade zero, cálculo do valor atual e mapeamento. `PosicaoMapper` receberá os valores prontos e continuará apenas projetando dados.

A cotação não será adicionada às entradas nem ao estado interno do replay; assim, é estruturalmente impossível que altere preço médio ou custo.

Alternativas consideradas:

- calcular no mapper, rejeitado por colocar regra financeira na camada de projeção;
- calcular diretamente no service, rejeitado por dispersar a política numérica fora do componente de cálculo;
- incorporar cotação ao replay, rejeitado por misturar estado de mercado com histórico contábil.

### 3. Política numérica exata e compatível

As decisões existentes permanecem:

| Valor | Origem/política | Saída |
|---|---|---|
| `quantidadeAtual` | Operações `NUMERIC(19,6)`; soma/subtração exatas | exata, até escala 6 |
| `cotacaoAtual` | Ação `NUMERIC(19,6)` | escala 6, precisão 19 |
| `precoMedio` | divisão em escala 24 com `HALF_EVEN` | escala 12, precisão 25 |
| `custoPosicao` | replay contábil | escala 12, precisão 38 |
| `valorAtualPosicao` | quantidade × cotação | escala 12, precisão 38 |

O produto de dois operandos `NUMERIC(19,6)` exige no máximo precisão 38 e escala 12. A multiplicação será exata. A normalização para escala 12 usará `RoundingMode.UNNECESSARY`; qualquer perda inesperada ou excesso de precisão será convertido no erro integral já aprovado `422/CALCULO_POSICAO_FORA_DA_PRECISAO`. `HALF_EVEN` continua limitado às divisões inevitáveis do replay.

Alternativas consideradas: escala 6 ou 2, que truncaria produtos válidos; `MathContext` global, que poderia arredondar silenciosamente; `double`, proibido para o domínio financeiro.

### 4. Projetar a cotação exatamente da Ação persistida

Depois do replay válido e antes do mapper, o service lerá `acao.getCotacaoAtual()` e `acao.getDataHoraCotacao()`. Não haverá `Clock`, timestamp substituto, fallback ou normalização temporal no GET. A serialização de `OffsetDateTime` seguirá a configuração existente.

O modelo suportado torna estados ausentes ou inválidos impossíveis: entidade e migration exigem ambos os campos, e a migration também exige cotação positiva. Portanto, não será criado contrato anulável, valor zero sintético ou erro funcional novo. Dados introduzidos fora das constraints representam corrupção/ambiente não suportado e não justificam regra artificial nesta capability.

Alternativa considerada: devolver nulos para tolerar legado. Rejeitada porque contradiz a spec de Ação, o schema e mascararia violação de integridade inexistente nas migrations aprovadas.

### 5. Usar fetch plan explícito para `Operacao.acao`

O método de leitura do histórico por Carteira será ajustado com `@EntityGraph(attributePaths = "acao")` ou JPQL equivalente com `JOIN FETCH operacao.acao`, preservando a ordenação `dataOperacao`, `ordemNoDia`, `id`. Isso carrega as Ações necessárias no mesmo acesso ao histórico e evita uma consulta por Ação.

Não será buscada `corretora`, pois não participa da consolidação. Também não será criado `AcaoRepository` adicional no fluxo, que introduziria uma leitura separada e risco de inconsistência entre snapshots.

Alternativa considerada: batch fetch global. Rejeitada por alterar configuração ampla para resolver uma consulta específica.

### 6. Preservar `REPEATABLE_READ` e read-only

A política atual de `PosicaoService` permanece adequada: uma única transação `readOnly=true`, isolamento `REPEATABLE_READ`, sem lock pessimista. O histórico e as Ações associadas são lidos no mesmo snapshot transacional. O GET não chama `save`, não atualiza cotação e não executa HTTP.

Alternativas consideradas: lock pessimista, desnecessário para uma projeção read-only; reduzir o isolamento sem evidência, que alteraria uma decisão já aprovada; chamar provider antes ou dentro da transação, que criaria dependência externa, latência e efeitos colaterais.

### 7. Manter a atualização externa em fluxo separado

O fluxo arquitetural permanece:

```text
BRAPI / Alpha Vantage
        ↓
atualização dedicada de Acao
        ↓
cotacaoAtual e dataHoraCotacao persistidas
        ↓
GET /carteiras/{carteiraId}/posicoes
        ↓
quantidadeAtual × cotacaoAtual persistida
```

Não há impedimento arquitetural para essa separação. A capability restaurada segue `AcaoResource → AcaoService → CotacaoProvider → AcaoCotacaoPersistenceService → AcaoRepository`: BRAPI ou Alpha Vantage são acionados pelo fluxo dedicado conforme o mercado persistido, fora da transação de escrita, e o `PESSIMISTIC_WRITE` é adquirido somente na persistência final. A implementação desta change deve preservar esse contrato e cobri-lo por regressão, sem colocar providers no GET.

### 8. Testar comportamento, isolamento e schema

Os testes serão distribuídos nos níveis existentes:

- `CalculadoraPosicaoTest`: produto exato, quantidade fracionária, escala 12, limites e não interferência no replay;
- `PosicaoServiceTest`: BRASIL/EUA, múltiplas posições/moedas, cotação e data persistidas, zero omitido, vazia/404, ausência de escrita/Clock/providers e preservação contábil;
- `PosicaoContractTest`: os três campos novos e projeção sem regra no mapper;
- `PosicaoResourceTest`: status, JSON, ordenação e contratos de erro/vazio;
- teste de repository/integração: fetch da Ação sem N+1 e mesmo snapshot;
- regressão: posição, Operações, atualização dedicada de cotação, Liquibase/Hibernate e suíte completa.

Os testes não chamarão providers reais.

## Risks / Trade-offs

- [O contrato JSON é ampliado e consumidores estritos podem rejeitar campos novos] → documentar como evolução aditiva do endpoint existente e atualizar contract tests.
- [O fetch join repete colunas da Ação em cada linha de Operação] → aceitar o custo previsível para eliminar round trips N+1; medir antes de materializar ou cachear.
- [A cotação persistida pode estar defasada] → expor `dataHoraCotacao` exatamente como persistida e manter atualização explicitamente separada.
- [A ampliação da projeção pode causar regressão acidental no fluxo dedicado recém-restaurado] → executar regressão explícita do PATCH, dos dois mercados, da regra temporal, da concorrência e do ticker canônico divergente, mantendo os providers fora do GET.
- [Um ambiente adulterado pode conter cotação inválida] → considerar fora do schema suportado; Liquibase constraints e Hibernate `validate` permanecem a barreira oficial.

## Migration Plan

1. Confirmar e preservar na baseline a capability restaurada de atualização dedicada de cotação, sem alterar seu contrato nem incorporá-la ao GET de posições.
2. Estender cálculo, DTO, mapper, service e fetch plan sem alterar a rota.
3. Executar testes unitários, HTTP, repository/integração, concorrência e regressão.
4. Confirmar que Liquibase aplica apenas os changeSets 001–004 e que Hibernate permanece com `ddl-auto=validate`.
5. Executar validações OpenSpec strict e atualizar Graphify somente após futura alteração de código.

Rollback de código remove os três campos e o cálculo/fetch plan acrescentados. Não há rollback de banco ou dados, pois não existe migration nesta change.

## Open Questions

Nenhuma decisão bloqueante identificada.
