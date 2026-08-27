## 1. Baseline e caracterização

- [x] 1.1 Confirmar a baseline dos contratos de `GET /resumo`, `GET /patrimonio` e `GET /posicoes`, incluindo DTOs, ordenação, respostas vazias, `404`, transações e política de erros.
- [x] 1.2 Registrar ou revisar testes de caracterização da rentabilidade individual para assegurar os resultados atuais antes da extração, incluindo `350 / 3200 × 100 = 10.937500`.
- [x] 1.3 Confirmar por testes e inspeção que `PosicaoService.listarPorCarteira` mantém o fetch plan cronológico sem N+1 e rejeita posição aberta com custo não positivo.

## 2. Calculadora percentual compartilhada

- [x] 2.1 Criar `CalculadoraRentabilidade` como componente puro e sem estado mutável de domínio, repository, service, provider, `Clock`, transação, persistência, banco ou replay.
- [x] 2.2 Centralizar em `CalculadoraRentabilidade` a fórmula única `(resultado / custo) × 100` usando somente `BigDecimal` e exigindo custo estritamente positivo.
- [x] 2.3 Implementar exclusivamente na calculadora a divisão em escala 24 com `HALF_EVEN`, multiplicação por 100, escala final 6 com `HALF_EVEN` e precisão máxima 38.
- [x] 2.4 Fazer a calculadora sinalizar operandos inválidos e falhas de representação sem acoplamento HTTP e sem criar novo `ErrorCode`.

## 3. Refatoração da rentabilidade da posição

- [x] 3.1 Remover de `CalculadoraPosicao` a implementação e as constantes que pertençam exclusivamente à política percentual, preservando replay, quantidade, preço médio, custo, valor atual e resultado não realizado.
- [x] 3.2 Injetar `CalculadoraRentabilidade` em `PosicaoService` e delegar a ela a rentabilidade individual sobre `resultadoNaoRealizado` e `custoPosicao` já consolidados.
- [x] 3.3 Preservar em `PosicaoService` o tratamento `409 / HISTORICO_OPERACOES_INCONSISTENTE` para custo não positivo e `422 / CALCULO_POSICAO_FORA_DA_PRECISAO` para falha de representação.
- [x] 3.4 Comprovar que a refatoração não altera `PosicaoResponse`, ordenação, venda parcial, venda total, novo ciclo ou qualquer requisito promovido de `portfolio-position`.

## 4. Integração da rentabilidade no resumo

- [x] 4.1 Acrescentar `rentabilidadePercentual` ao final de `ResumoMoedaResponse`, preservando `ResumoCarteiraResponse` com somente `carteiraId` e `resumos`.
- [x] 4.2 Adaptar `ResumoCarteiraMapper` para projetar o percentual já calculado, sem fórmula, divisão, soma ou normalização financeira.
- [x] 4.3 Injetar `CalculadoraRentabilidade` em `ResumoCarteiraService` e calcular uma vez por moeda usando somente `resultadoNaoRealizadoTotal` e `custoTotalPosicoes` de `TotaisPorMoeda`.
- [x] 4.4 Tratar custo total não positivo no resumo como `409 / HISTORICO_OPERACOES_INCONSISTENTE`, sem fallback, `null`, infinito, zero artificial ou resposta parcial.
- [x] 4.5 Traduzir falha de precisão percentual para `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`, preservando a falha integral do resumo.
- [x] 4.6 Preservar `ResumoCarteiraService` com uma chamada a `PosicaoService.listarPorCarteira`, uma agregação, `readOnly=true`, `REPEATABLE_READ`, sem `PatrimonioService`, lock, query ou replay adicional.

## 5. Testes da CalculadoraRentabilidade e da posição

- [x] 5.1 Testar ganho positivo, perda negativa, zero `0.000000` e ganho acima de `100.000000` diretamente na `CalculadoraRentabilidade`.
- [x] 5.2 Testar divisão exata e periódica, escala intermediária 24, `HALF_EVEN`, multiplicação por 100 e escala final 6.
- [x] 5.3 Testar precisão máxima 38, falha de representação e rejeição de custo zero, negativo ou ausente sem valor sintético.
- [x] 5.4 Testar estruturalmente a pureza da calculadora e a ausência de infraestrutura, `Clock`, transação, persistência, replay e estado mutável de domínio.
- [x] 5.5 Executar regressões da rentabilidade individual cobrindo todos os valores aprovados e demonstrar equivalência exata após a delegação compartilhada.

## 6. Testes funcionais e financeiros do resumo

- [x] 6.1 Testar contrato JSON do novo campo para Carteira somente BRL, somente USD, multimoeda, Carteira vazia e Carteira inexistente.
- [x] 6.2 Testar rentabilidade consolidada positiva, negativa, nula, acima de 100% e divisão periódica com as escalas aprovadas.
- [x] 6.3 Testar múltiplas posições com `400 / 4000 × 100 = 10.000000` e demonstrar que o cálculo não usa média simples das rentabilidades individuais.
- [x] 6.4 Testar separação e ordenação BRL/USD, ausência de conversão, média entre moedas ou item artificial para moeda ausente.
- [x] 6.5 Testar venda parcial, venda total da última posição da moeda e novo ciclo, sem participação de resultado realizado ou ciclo encerrado.
- [x] 6.6 Testar custo total zero e negativo com `409`, overflow ou falha de normalização com `422` e ausência de resposta parcial em ambos os casos.

## 7. Arquitetura, performance e regressões

- [x] 7.1 Testar que posição e resumo usam a mesma `CalculadoraRentabilidade` e que não existe implementação percentual paralela em `CalculadoraPosicao`, services, agregador ou mappers.
- [x] 7.2 Testar que `AgregadorPosicoesPorMoeda` continua estritamente monetário e que `PatrimonioService` não depende de `CalculadoraRentabilidade` nem altera `GET /patrimonio`.
- [x] 7.3 Testar uma única chamada a `PosicaoService`, ausência de segundo replay, nova query, N+1, provider, `Clock`, lock, escrita ou persistência e custo O(1) adicional por moeda.
- [x] 7.4 Executar regressões de resumo, patrimônio, posições, resultado não realizado, resultado realizado, PATCH de cotação, Operações e DELETE protegido de Carteira.

## 8. Verificação e entrega

- [x] 8.1 Executar os testes direcionados da calculadora, posição, agregador, resumo, patrimônio e contratos HTTP afetados.
- [x] 8.2 Executar a suíte completa e `mvnw clean verify`, incluindo Liquibase/Hibernate nos ambientes de teste previstos e disponíveis.
- [x] 8.3 Validar `rentabilidade-carteira` e o conjunto global OpenSpec em modo strict e atualizar o Graphify após as alterações de código.
- [x] 8.4 Auditar ausência de migration, schema, dependência, provider, persistência e funcionalidades fora do escopo; executar `git diff --check` e revisar `git diff` e `git status` sem operações Git proibidas.
