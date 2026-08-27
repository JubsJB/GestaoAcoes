## Context

Ver `proposal.md` para a motivação e `specs/portfolio-summary/spec.md` para o contrato proposto. A implementação atual já executa uma única chamada a `PosicaoService.listarPorCarteira`, agrega em memória custo, patrimônio e resultado não realizado por moeda e projeta esses totais no resumo. `PatrimonioService` compartilha o mesmo agregador, mas consome somente `patrimonioAtual`.

`CalculadoraPosicao` contém hoje a política pura `calcularRentabilidadePercentual(resultadoNaoRealizado, custoPosicao)`, com escala intermediária 24, `HALF_EVEN`, fator 100, escala final 6 e precisão 38. A fórmula da Carteira é estruturalmente idêntica, trocando apenas operandos individuais pelos totais oficiais da moeda. A decisão aprovada move esse algoritmo para `CalculadoraRentabilidade` e redireciona o cálculo individual e o resumo para a mesma implementação.

## Goals / Non-Goals

**Goals:**

- Acrescentar rentabilidade percentual ao item por moeda do resumo sem endpoint, replay, agregação ou consulta adicional.
- Manter uma única implementação da política percentual para posição e Carteira.
- Preservar as fórmulas oficiais e as políticas numéricas dos três acumulados existentes.
- Falhar integralmente e com semântica consistente diante de custo inválido ou precisão excedida.
- Manter `PatrimonioService` independente do novo percentual.

**Non-Goals:**

- Rentabilidade realizada, histórica, total, TWR, XIRR ou baseada em fluxos.
- Resultado realizado, caixa, aportes, resgates, dividendos, impostos, câmbio ou evolução patrimonial.
- Novo endpoint, frontend, persistência, snapshot, migration, scheduler, cache ou integração externa.
- Alteração das regras de replay, custo, patrimônio, resultado não realizado ou contratos de `/patrimonio` e `/posicoes`.

## Decisions

### 1. Estender `GET /carteiras/{carteiraId}/resumo`

Cada `ResumoMoedaResponse` receberá `rentabilidadePercentual`; o envelope, a rota e os cenários `200`, lista vazia e `404` permanecem inalterados. Os valores-base já coexistem no mesmo item e a nova métrica tem exatamente a mesma granularidade Carteira + moeda.

Alternativa rejeitada: `GET /carteiras/{carteiraId}/rentabilidade`. Criaria contrato e orquestração redundantes, além de risco de segunda consolidação ou divergência temporal.

### 2. Fórmula oficial sobre os acumulados existentes

O cálculo será exclusivamente:

```text
rentabilidadePercentual =
    (resultadoNaoRealizadoTotal / custoTotalPosicoes) × 100
```

`((patrimonioAtual / custoTotalPosicoes) - 1) × 100` será somente identidade matemática. Não haverá média de rentabilidades individuais nem nova soma de posições.

### 3. Extrair uma `CalculadoraRentabilidade` pura compartilhada

Criar um componente puro responsável exclusivamente por `calcularPercentual(resultado, custo)`. `CalculadoraPosicao` deixará de possuir a implementação percentual, e `PosicaoService` e `ResumoCarteiraService` usarão diretamente o novo componente sobre seus operandos já consolidados.

`CalculadoraRentabilidade` será injetada em `PosicaoService` e `ResumoCarteiraService`, mantendo `CalculadoraPosicao` focada em replay, valor atual e resultado não realizado. Isso elimina implementações paralelas e torna explícito que a política percentual não depende da granularidade.

Alternativas rejeitadas:

- manter o método em `CalculadoraPosicao` e chamá-lo pelo resumo, pois acopla uma métrica agregada a uma calculadora de replay de posição;
- duplicar o algoritmo no agregador ou no mapper, pois cria política concorrente;
- calcular no `AgregadorPosicoesPorMoeda`, pois mistura soma exata/normalização monetária com divisão percentual e obrigaria `PatrimonioService` a depender de uma métrica que não utiliza.

A extração será limitada à propriedade do algoritmo percentual e protegida por testes de caracterização e regressão da rentabilidade individual. Quantidade, preço médio, custo, valor atual, resultado não realizado, replay e ciclos permanecem inalterados.

### 4. Manter o agregador estritamente monetário

`AgregadorPosicoesPorMoeda` continuará produzindo somente `moeda`, `custoTotalPosicoes`, `patrimonioAtual` e `resultadoNaoRealizadoTotal`. Ele permanecerá puro, sem repository, service, provider, `Clock`, transação, persistência ou replay.

Depois de `agregar(posicoes)`, `ResumoCarteiraService` aplicará a calculadora percentual uma vez por `TotaisPorMoeda` e entregará o valor pronto ao mapper. O trabalho adicional será O(1) por moeda.

### 5. Política numérica idêntica à posição

A calculadora compartilhada usará somente `BigDecimal`: divisão em escala 24 por `HALF_EVEN`, multiplicação por `new BigDecimal("100")`, normalização final em escala 6 por `HALF_EVEN` e precisão máxima 38. Não usará `MathContext`, `double`, `float` ou arredondamento adicional.

Falha de representação com operandos válidos continuará traduzida para `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`, sem resposta parcial.

### 6. Custo total não positivo é inconsistência

Uma posição aberta já é rejeitada por `PosicaoService` quando `custoPosicao <= 0`. Como o resumo agrupa somente posições abertas de custo positivo, seu total por moeda também deve ser positivo. A checagem defensiva será feita antes da divisão; violação produzirá `409 / HISTORICO_OPERACOES_INCONSISTENTE` para a consulta inteira.

Não haverá `null`, infinito, zero sintético ou denominador alternativo.

### 7. DTO e mapper permanecem projeções

`ResumoCarteiraResponse` continuará com `carteiraId` e `resumos`. `ResumoMoedaResponse` conterá `moeda`, os três totais monetários e `rentabilidadePercentual`. `ResumoCarteiraMapper` apenas projetará valores já calculados, sem fórmula, soma ou normalização.

### 8. Preservar orquestração e transação

O fluxo ficará:

```text
CarteiraResource
→ ResumoCarteiraService
→ PosicaoService (uma vez)
→ AgregadorPosicoesPorMoeda (uma vez)
→ CalculadoraRentabilidade (uma vez por moeda)
→ ResumoCarteiraMapper
→ ResumoCarteiraResponse
```

`ResumoCarteiraService` continuará `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`, sem lock pessimista. Não chamará `PatrimonioService`, providers ou `Clock`.

`PatrimonioService` continuará chamando `PosicaoService` e o agregador uma vez e consumindo somente `patrimonioAtual`; não receberá dependência da calculadora percentual e seu contrato não mudará.

## Risks / Trade-offs

- [Extração altera a propriedade de uma regra financeira existente] → preservar testes de caracterização e comprovar equivalência exata da rentabilidade individual antes e depois da delegação.
- [Dízimas periódicas] → reutilizar escala intermediária 24 e `HALF_EVEN`, com escala final 6.
- [Custo inválido indicaria violação de invariantes anteriores] → falhar integralmente com 409, sem mascarar o histórico.
- [Novo campo pode afetar consumidores com desserialização estrita] → tratar como evolução aditiva documentada do resumo e manter todos os campos e cenários anteriores.
- [Acoplamento acidental do patrimônio ao percentual] → manter o percentual fora de `TotaisPorMoeda` e fora de `PatrimonioService`.

## Migration Plan

Não há migração de dados ou schema. A implementação deverá preservar primeiro a política individual em testes, introduzir a calculadora compartilhada, integrar o resumo e executar regressões completas. Rollback consiste em reverter somente código e testes da change, sem operação de banco.
