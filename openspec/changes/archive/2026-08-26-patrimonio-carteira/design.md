## Context

O PRD define patrimônio atual como a soma do valor atual das posições abertas. A capability `portfolio-position` já concentra o replay cronológico e entrega, para cada posição aberta, `valorAtualPosicao` calculado com a cotação persistida, além de omitir posições zeradas e evitar N+1 pelo fetch plan de `Operacao.acao`.

A Carteira pode combinar posições BRL e USD. Como não há câmbio nem saldo de caixa no modelo, esta change precisa agregar somente valores compatíveis, preservar o resultado realizado como histórico separado e não duplicar resultado não realizado já incorporado ao valor atual.

## Goals / Non-Goals

**Goals:**

- Expor o patrimônio atual por moeda no endpoint dedicado da Carteira.
- Reutilizar integralmente a consolidação oficial de posições e seu `valorAtualPosicao`.
- Manter cálculo exato, transação read-only e ausência de consultas ou replay adicionais.
- Preservar separação entre patrimônio das posições, resultado realizado e atualização externa de cotação.

**Non-Goals:**

- Converter ou somar moedas distintas.
- Controlar caixa, aportes, resgates ou incorporar vendas encerradas ao patrimônio.
- Agregar custo, resultado não realizado, resultado realizado ou rentabilidade da Carteira.
- Criar patrimônio histórico, snapshots, persistência, cache ou novas integrações.

## Decisions

### 1. Contrato REST e DTOs mínimos

Será criado `GET /carteiras/{carteiraId}/patrimonio`. A resposta será:

```json
{
  "carteiraId": 1,
  "patrimonios": [
    {
      "moeda": "BRL",
      "patrimonioAtual": 12500.000000000000
    },
    {
      "moeda": "USD",
      "patrimonioAtual": 3200.000000000000
    }
  ]
}
```

`PatrimonioResponse` conterá somente `carteiraId` e a lista `patrimonios`. `PatrimonioMoedaResponse` conterá somente `moeda` e `patrimonioAtual`. O nome da Carteira não será duplicado. Carteira existente sem posição aberta retornará a mesma estrutura com lista vazia. Itens serão ordenados explicitamente por `moeda ASC`, sem depender de ordem de mapa ou banco.

### 2. Fonte oficial e granularidade

Para cada moeda:

```text
patrimonioAtual = soma(valorAtualPosicao das posições abertas da moeda)
```

O cálculo consumirá o `valorAtualPosicao` já consolidado. Não haverá nova implementação de `quantidadeAtual × cotacaoAtual`. Moedas sem posição aberta serão omitidas; BRL e USD nunca serão combinados.

Resultado realizado não será incluído porque o sistema não modela caixa. Resultado não realizado também não será somado, pois `valorAtualPosicao = custoPosicao + resultadoNaoRealizado`; adicioná-lo produziria dupla contagem. Percentuais não participam de soma monetária.

### 3. Reutilização de `PosicaoService` sem segundo replay

O fluxo será:

```text
CarteiraResource
  → PatrimonioService
    → PosicaoService
      → OperacaoRepository
      → CalculadoraPosicao
      → PosicaoMapper
    → agregação por moeda
    → PatrimonioMapper
    → PatrimonioResponse
```

`PatrimonioService` chamará uma única vez `PosicaoService.listarPorCarteira(carteiraId)`. Essa chamada já valida a Carteira, carrega o histórico com `Operacao.acao`, executa um único replay por Ação e retorna somente posições abertas. Assim, o patrimônio não repetirá validação, query, agrupamento ou replay.

Uma projeção interna compartilhada foi considerada, mas exigiria refatorar a boundary atual sem benefício funcional nesta primeira versão. Consultar `OperacaoRepository` diretamente no novo service foi rejeitado porque duplicaria a orquestração financeira. O acoplamento ao DTO de posição é aceito deliberadamente por ele já constituir o contrato consolidado contendo a fonte oficial necessária.

### 4. Local do cálculo e mapper

`PatrimonioService` será responsável pela regra de agregação monetária sobre posições prontas. `PatrimonioMapper` apenas projetará acumulados já calculados para os DTOs e não conterá soma, normalização ou tratamento de precisão. Não será criado um service financeiro adicional para uma única agregação.

### 5. Política numérica e overflow

Cada `valorAtualPosicao` de entrada já possui escala 12 e precisão máxima 38. A agregação usará `BigDecimal.add` sem `MathContext`, sem arredondamento e sem normalização por item. Cada moeda terá acumulador independente.

Depois de concluir a soma de uma moeda, o acumulado será normalizado com `setScale(12, RoundingMode.UNNECESSARY)` e validado com precisão máxima 38. A soma pode exigir um dígito inteiro adicional mesmo quando todos os itens são individualmente válidos; nesse caso, a consulta falhará integralmente com `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`. Não haverá truncamento, saturação ou resposta parcial.

### 6. Transação e consistência

`PatrimonioService` usará `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`. A chamada ao `PosicaoService` participará da mesma transação pela propagação padrão, preservando uma visão consistente de Operações e Ações. Não haverá lock pessimista.

Não serão usados `Clock`, providers ou escrita. `PATCH /acoes/{id}/cotacao` continuará sendo o fluxo independente de atualização externa; o patrimônio refletirá apenas o estado persistido observado pela consolidação.

### 7. Persistência e temporalidade

O patrimônio será calculado sob demanda. Não haverá entidade, repository, tabela, migration, snapshot ou cache. A resposta não terá `dataHoraCotacao` agregada, pois posições distintas podem ter referências temporais distintas e selecionar uma delas produziria semântica artificial.

## Risks / Trade-offs

- **Acoplamento ao contrato de posição:** `PatrimonioService` consumirá `PosicaoResponse`. A mitigação é limitar o consumo a `moeda` e `valorAtualPosicao` e proteger a integração com testes de regressão.
- **Custo do replay:** a consulta executa a consolidação completa das posições abertas. Isso preserva uma única interpretação financeira; materialização e cache permanecem fora do escopo até existir necessidade medida.
- **Overflow na soma:** múltiplos valores válidos podem gerar acumulado acima da precisão 38. A falha explícita 422 preserva a política financeira e evita resultado aproximado.
- **Novas moedas futuras:** a coleção por moeda é extensível, mas cada moeda continuará acumulada isoladamente e a ordenação deverá permanecer explícita.

## Migration Plan

Não há migração de dados ou schema. A entrega adicionará apenas o contrato HTTP e componentes de aplicação/projeção. O rollback consiste em remover o endpoint e esses componentes, sem transformação de dados.
