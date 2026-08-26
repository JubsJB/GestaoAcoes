## Context

Ver `proposal.md` para motivação e `specs/portfolio-position/spec.md` para o contrato proposto. A baseline já calcula em memória, nesta ordem, posição contábil, `valorAtualPosicao` e `resultadoNaoRealizado`, e projeta doze campos em `PosicaoResponse`. `PosicaoService` consulta Operações com `Operacao.acao` no fetch plan, executa sob `readOnly=true` e `Isolation.REPEATABLE_READ`, filtra posições zeradas e não chama providers.

O PRD define `rentabilidade = (resultadoNaoRealizado / custoPosicao) × 100`. O replay aceita somente quantidade e preço unitário positivos, impede VENDA superior ao saldo e zera custo junto com quantidade na VENDA total. Portanto, toda posição efetivamente retornada possui quantidade e custo positivos; divisão por zero não é um estado funcional suportado.

## Goals / Non-Goals

**Goals:**

- Acrescentar a rentabilidade percentual individual da posição aberta usando valores consolidados.
- Centralizar a nova regra financeira na calculadora e preservar o mapper como projeção.
- Definir uma política decimal explícita para divisão periódica e falha de precisão.
- Manter o GET determinístico, sem escrita, provider ou nova query.

**Non-Goals:**

- Resultado realizado, patrimônio, agregação de posições, rentabilidade de Carteira, rentabilidade histórica, TWR, XIRR e conversão cambial.
- Persistência, snapshot, entidade, repository, tabela ou migration da rentabilidade.
- Alteração de Operações, cotação, timestamp, PATCH de cotação ou regras anteriores da posição.

## Decisions

### 1. Consumir somente resultado não realizado e custo consolidados

`CalculadoraPosicao` receberá `resultadoNaoRealizado` e `custoPosicao` e calculará exclusivamente:

```text
rentabilidadePercentual =
    (resultadoNaoRealizado / custoPosicao) × 100
```

Isso mantém a cadeia unidirecional:

```text
Operações
→ quantidadeAtual, precoMedio, custoPosicao
→ cotacaoAtual persistida
→ valorAtualPosicao
→ resultadoNaoRealizado
→ rentabilidadePercentual
```

Alternativa rejeitada: recalcular por `(cotacaoAtual / precoMedio - 1) × 100`. Embora matematicamente equivalente sob invariantes ideais, duplicaria fontes e poderia divergir pelas políticas numéricas dos valores consolidados.

### 2. Calcular em `CalculadoraPosicao`

A regra ficará ao lado de `calcularValorAtual` e `calcularResultadoNaoRealizado`. A calculadora continuará pura, sem repository, provider, `Clock`, escrita ou transação. `PosicaoService` apenas encadeará os cálculos e `PosicaoMapper` receberá o percentual pronto.

Alternativas rejeitadas: cálculo no mapper, por ocultar regra financeira em projeção; `RentabilidadeService`, por acrescentar uma camada sem responsabilidade autônoma.

### 3. Retornar percentual, não razão

O campo será `rentabilidadePercentual`; `10.937500` significará `10,9375%`. O fator 100 integra o cálculo oficial do backend.

Alternativa rejeitada: retornar `0.109375` e delegar a conversão ao frontend, pois contradiz o nome do campo e dispersa a interpretação do contrato.

### 4. Política numérica aprovada

A implementação usará:

- somente `BigDecimal`;
- divisão `resultadoNaoRealizado / custoPosicao` em escala intermediária 24;
- `RoundingMode.HALF_EVEN` na divisão inevitável;
- multiplicação do quociente por `100` com `BigDecimal`;
- normalização final em escala 6 com `RoundingMode.HALF_EVEN`;
- precisão total máxima 38 no percentual final;
- `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` para falhas de representação, sem resposta parcial.

A escala intermediária 24 e `HALF_EVEN` reutilizam a política das divisões financeiras atuais. A escala final 6 oferece granularidade percentual suficiente e contrato estável sem expor as 24 casas intermediárias. A precisão 38 acompanha os valores monetários consolidados e evita um novo limite conceitual.

Alternativas consideradas: escala final 12, que aumenta o payload sem requisito de negócio; divisão direta em escala 6, que reduz casas de guarda; `UNNECESSARY`, inviável para dízimas periódicas; `HALF_UP`, inconsistente com as divisões existentes.

### 5. Custo não positivo viola a consistência do replay

Não haverá `null`, zero sintético nem regra percentual alternativa para denominador zero. Para uma posição aberta válida, ao menos uma COMPRA positiva sustenta quantidade e custo positivos; VENDA total zera ambos e a posição é descartada antes dos indicadores de mercado.

A implementação deve verificar a invariável antes da divisão. Se quantidade positiva coexistir com `custoPosicao <= 0`, toda a consulta falhará pelo mecanismo existente `409 / HISTORICO_OPERACOES_INCONSISTENTE`, sem resposta parcial. Não será criado ErrorCode específico. Falhas de representação de uma divisão com operandos válidos permanecem distintas e usam `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`.

### 6. Sem limites percentuais artificiais

Não haverá validação de faixa como `[-100, 100]`. A fórmula e os operandos determinam o valor; ganhos não têm teto teórico. A cotação positiva vigente naturalmente impede valor atual negativo, mas essa consequência não será duplicada como validação do DTO.

### 7. Orquestração e contrato

O fluxo permanecerá:

```text
CarteiraResource
→ PosicaoService
→ CalculadoraPosicao
→ PosicaoMapper
→ PosicaoResponse
```

Depois de filtrar quantidade zero, `PosicaoService` calculará valor atual, resultado não realizado e rentabilidade, nessa ordem. `PosicaoResponse` receberá somente o novo campo ao final. O endpoint, ordenação, respostas `200`, `[]`, `404` e ausência de `Location` permanecem iguais.

### 8. Transação, performance e integrações preservadas

`PosicaoService` continuará `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`, sem lock pessimista. O cálculo será em memória sobre valores já disponíveis, sem query adicional. O `@EntityGraph` atual de `OperacaoRepository` continuará evitando N+1 de Ação.

BRAPI, Alpha Vantage, `CotacaoProvider`, `Clock` e `PATCH /acoes/{id}/cotacao` não serão chamados pelo GET. O PATCH permanece o fluxo dedicado de atualização externa.

## Risks / Trade-offs

- [Dízimas exigem arredondamento] → usar escala intermediária 24 e `HALF_EVEN`, com escala final explicitamente contratada.
- [Percentuais extremos podem exceder precisão 38] → rejeitar toda a consolidação com o erro 422 já existente.
- [Mudança de cotação altera indicadores de mercado] → aceitar somente a cadeia esperada valor atual → resultado não realizado → rentabilidade, protegendo preço médio, custo e replay por regressão.
- [Consumidores podem interpretar 10.937500 como razão] → nomear o campo `rentabilidadePercentual` e especificar que o valor já está multiplicado por 100.
- [Dados legados corrompidos poderiam violar custo positivo] → preservar validação segura do replay; não fabricar percentual para estado inconsistente.

## Migration Plan

1. Implementar a extensão aditiva do DTO e os cálculos sem alteração de schema.
2. Executar testes direcionados e regressões completas.
3. Publicar como evolução compatível do endpoint; rollback consiste em reverter somente código e testes da change, sem rollback de banco.
