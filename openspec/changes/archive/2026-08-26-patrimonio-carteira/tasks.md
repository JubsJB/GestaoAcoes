## 1. Baseline e contrato

- [x] 1.1 Confirmar na baseline os contratos de Carteira, posição consolidada, resultado realizado e atualização de cotação afetados pela nova consulta.
- [x] 1.2 Confirmar o fetch plan e a quantidade de consultas executadas atualmente por `PosicaoService.listarPorCarteira`.
- [x] 1.3 Registrar testes de caracterização para a lista de posições abertas consumida pelo patrimônio, incluindo ordenação e omissão de posições zeradas.
- [x] 1.4 Confirmar que `CALCULO_POSICAO_FORA_DA_PRECISAO` e o tratamento centralizado produzem o contrato 422 exigido.

## 2. DTOs e projeção

- [x] 2.1 Criar `PatrimonioMoedaResponse` com somente `moeda` e `patrimonioAtual`.
- [x] 2.2 Criar `PatrimonioResponse` com somente `carteiraId` e `patrimonios`.
- [x] 2.3 Criar `PatrimonioMapper` para projetar acumulados prontos sem executar regra financeira.
- [x] 2.4 Testar DTOs, serialização em escala 12, lista vazia e ausência de campos fora do contrato.

## 3. Agregação financeira

- [x] 3.1 Implementar em `PatrimonioService` a soma exata de `valorAtualPosicao` agrupada por moeda com `BigDecimal`.
- [x] 3.2 Normalizar somente cada acumulado final para escala 12 com `RoundingMode.UNNECESSARY` e validar precisão máxima 38.
- [x] 3.3 Reutilizar `CALCULO_POSICAO_FORA_DA_PRECISAO` para escala ou precisão não representável, impedindo resposta parcial.
- [x] 3.4 Ordenar explicitamente os acumulados por `moeda ASC` e omitir moedas sem posições abertas.
- [x] 3.5 Testar uma posição, múltiplas posições da mesma moeda, soma exata e separação de BRL e USD.
- [x] 3.6 Testar escala 12, valores grandes, overflow da soma, `RoundingMode.UNNECESSARY` e falha 422 integral.

## 4. Service, transação e endpoint

- [x] 4.1 Fazer `PatrimonioService` reutilizar uma única chamada a `PosicaoService.listarPorCarteira`, sem consulta ou replay próprios.
- [x] 4.2 Configurar `PatrimonioService` como read-only com `Isolation.REPEATABLE_READ`, sem lock pessimista, escrita, `Clock` ou provider.
- [x] 4.3 Adicionar `GET /carteiras/{carteiraId}/patrimonio` em `CarteiraResource`, preservando os endpoints existentes.
- [x] 4.4 Testar service para Carteira com uma moeda, múltiplas moedas, Carteira vazia e Carteira inexistente.
- [x] 4.5 Testar o contrato HTTP: 200, lista vazia, 404, ausência de body, filtros, paginação e `Location`.

## 5. Regras de ciclo e separação financeira

- [x] 5.1 Testar venda parcial considerando somente o valor atual da posição remanescente.
- [x] 5.2 Testar venda total excluindo a posição e novo ciclo considerando somente a nova posição aberta.
- [x] 5.3 Testar que resultado realizado histórico não é tratado como caixa nem somado ao patrimônio.
- [x] 5.4 Testar que resultado não realizado não é somado novamente e que rentabilidade não participa da agregação.
- [x] 5.5 Testar que uma nova cotação persistida altera o patrimônio sem mudar preço médio, custo ou replay financeiro.

## 6. Performance, consistência e efeitos colaterais

- [x] 6.1 Verificar por teste que a consulta reutiliza o fetch plan vigente sem N+1 ou consulta individual por Ação.
- [x] 6.2 Verificar por teste que não há segundo replay, query adicional, escrita, `save`, `Clock`, BRAPI, Alpha Vantage ou `CotacaoProvider`.
- [x] 6.3 Testar visão consistente sob `REPEATABLE_READ` durante alterações concorrentes relevantes.
- [x] 6.4 Confirmar que nenhum timestamp agregado de cotação é criado ou retornado.

## 7. Regressões e infraestrutura

- [x] 7.1 Executar regressão de posição consolidada, valor atual, resultado não realizado e rentabilidade percentual.
- [x] 7.2 Executar regressão de resultado realizado, registro/consulta de Operações, VENDA e proteção de DELETE da Carteira.
- [x] 7.3 Executar regressão de `PATCH /acoes/{id}/cotacao` para BRASIL e EUA, incluindo regra temporal e ticker canônico divergente.
- [x] 7.4 Validar Liquibase/Hibernate com os changeSets 001–004 e `ddl-auto=validate`, confirmando ausência de migration e alteração de schema.
- [x] 7.5 Executar testes direcionados, suíte completa e `clean verify`; executar validação PostgreSQL se prevista e o ambiente estiver disponível.

## 8. Especificação, Graphify e auditoria

- [x] 8.1 Atualizar o Graphify após as alterações de código e conferir aderência do novo fluxo à arquitetura aprovada.
- [x] 8.2 Validar `patrimonio-carteira` e o conjunto global OpenSpec em modo strict.
- [x] 8.3 Executar `git diff --check`, revisar `git diff` e `git status`, confirmando somente alterações previstas e nenhuma migration, schema ou dependência nova.
