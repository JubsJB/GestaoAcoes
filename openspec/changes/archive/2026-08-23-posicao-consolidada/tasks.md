## 1. Baseline e guardrails

- [x] 1.1 Registrar o baseline dos testes e revisar os componentes atuais de Operação, Carteira e Ação para limitar a implementação ao menor conjunto de arquivos coerente.
- [x] 1.2 Confirmar antes da implementação que a change não exige entidade/tabela de posição, migration, alteração dos changeSets 001–004, dependência, configuração, provider externo ou indicador financeiro fora do escopo.

## 2. Contratos e tipos de saída

- [x] 2.1 Criar `PosicaoResponse` com `acaoId`, ticker, `nomeEmpresa`, mercado, moeda, `quantidadeAtual`, `precoMedio` em escala 12 e `custoPosicao` em escala 12, sem aceitar request nem incluir cotação ou indicadores excluídos.
- [x] 2.2 Criar os tipos internos imutáveis necessários para representar estado calculado e eventual falha de replay, sem anotação JPA nem persistência.
- [x] 2.3 Criar `PosicaoMapper` somente para projetar Ação persistida e estado calculado no DTO, mantendo toda matemática fora do mapper.
- [x] 2.4 Adicionar `HISTORICO_OPERACOES_INCONSISTENTE` e `CALCULO_POSICAO_FORA_DA_PRECISAO` a `ErrorCodes` e reutilizar `ApiException`, `StandardError` e o handler centralizado.
- [x] 2.5 Testar o contrato de `PosicaoResponse`, incluindo os oito campos exatos, enums, `BigDecimal`, ausência de cotação/resultado e serialização das escalas 12.

## 3. Calculadora financeira pura

- [x] 3.1 Implementar `CalculadoraPosicao` sem repository, HTTP, `Clock` ou estado compartilhado, com escala intermediária 24, saída 12 e `RoundingMode.HALF_EVEN` explícitos.
- [x] 3.2 Validar que o replay recebe um único grupo Carteira+Ação na ordem `dataOperacao ASC, ordemNoDia ASC`, sem usar ID como ordem financeira.
- [x] 3.3 Implementar o fold exato de quantidade com `BigDecimal`, somando COMPRA e subtraindo VENDA sem `float`, `double`, arredondamento ou truncamento.
- [x] 3.4 Implementar COMPRA como soma exata do custo negociado e recálculo ponderado do preço médio, sem taxa ou cotação.
- [x] 3.5 Implementar VENDA parcial reduzindo custo proporcionalmente ao preço médio vigente e preservando o preço médio remanescente, sem usar o preço da VENDA no custo.
- [x] 3.6 Implementar zeramento exato de quantidade/custo/preço médio e início de novo ciclo independente na COMPRA posterior.
- [x] 3.7 Implementar nova COMPRA após VENDA parcial usando quantidade e custo remanescentes como base da reponderação.
- [x] 3.8 Aplicar divisões em escala 24, normalização final em escala 12, precisão 25 para preço médio e 38 para custo, rejeitando overflow sem resposta parcial.
- [x] 3.9 Detectar saldo negativo e demais invariantes legadas inválidas, devolver contexto diagnóstico sem alterar ou ignorar Operações e sem calcular indicadores fora do escopo.

## 4. Reutilização do replay do cadastro

- [x] 4.1 Substituir somente a duplicação quantitativa de `OperacaoService.validateReplay` pela delegação localizada à calculadora/resultado comum.
- [x] 4.2 Preservar a inclusão conceitual e a ordenação da candidata retroativa, traduzindo a mesma falha para `409/POSICAO_INSUFICIENTE` com os detalhes vigentes.
- [x] 4.3 Preservar integralmente lock pessimista curto, transação de escrita, unicidade de ordem, `saveAndFlush`, atomicidade e ausência de providers em `POST /operacoes`.
- [x] 4.4 Atualizar e executar os testes de cadastro, venda, retroatividade e concorrência para demonstrar que a extração não alterou nenhum comportamento promovido.

## 5. Consulta e consolidação por Carteira

- [x] 5.1 Criar `PosicaoService` com `CarteiraRepository`, `OperacaoRepository`, calculadora e mapper, sem criar repository ou persistence service adicional.
- [x] 5.2 Implementar `listarPorCarteira(Long carteiraId)` com `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`, sem lock pessimista.
- [x] 5.3 Validar a Carteira por `findById` e reutilizar `ObjectNotFoundException` para o contrato `404 Not Found`.
- [x] 5.4 Reutilizar `findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc`, agrupar por Ação e preservar a ordem financeira de cada grupo sem query SQL agregada.
- [x] 5.5 Calcular cada grupo, omitir posições com quantidade final zero, mapear DTOs e ordenar por `mercado ASC`, `ticker ASC`, `acaoId ASC`.
- [x] 5.6 Traduzir inconsistência para `409/HISTORICO_OPERACOES_INCONSISTENTE` e precisão para `422/CALCULO_POSICAO_FORA_DA_PRECISAO`, garantindo ausência de save, delete, `Clock`, lock de escrita, normalização, cache ou chamada externa.

## 6. Endpoint REST

- [x] 6.1 Reutilizar `CarteiraResource` e adicionar somente `GET /carteiras/{carteiraId}/posicoes`, delegando integralmente a `PosicaoService`.
- [x] 6.2 Implementar `200 OK` com lista ou `[]`, `404` centralizado para Carteira inexistente e ausência de `Location` e corpo de entrada.
- [x] 6.3 Confirmar ausência de rota individual, filtros, paginação ou novo resource e preservar POST/GET/PATCH/DELETE de Carteira e todos os endpoints de Operação.

## 7. Testes unitários da matemática

- [x] 7.1 Testar compra única e múltiplas compras no mesmo preço, verificando quantidade, custo e preço médio exatos.
- [x] 7.2 Testar múltiplas compras com preços diferentes, incluindo divisão exata e periódica com escala intermediária 24, `HALF_EVEN` e saída em escala 12.
- [x] 7.3 Testar VENDA parcial com preço acima, abaixo e igual ao preço médio, confirmando preço médio inalterado e custo proporcional.
- [x] 7.4 Testar VENDA total, estado zerado e nova COMPRA iniciando ciclo independente do histórico encerrado.
- [x] 7.5 Testar COMPRA após VENDA parcial, incluindo o exemplo de resultado final quantidade 100, preço médio 14 e custo 1400.
- [x] 7.6 Testar quantidade inteira no BRASIL, quantidade fracionária nos EUA e isolamento entre mercados, Carteiras e Ações.
- [x] 7.7 Testar cronologia por data/ordem, operação retroativa, valores grandes, limites de precisão e histórico inconsistente sem mutação.

## 8. Testes de integração e regressão

- [x] 8.1 Testar `PosicaoService` para Carteira inexistente, sem Operações e somente com posições encerradas.
- [x] 8.2 Testar Carteira com uma e múltiplas posições, múltiplas Ações, mesma Ação em Carteiras distintas e isolamento completo.
- [x] 8.3 Testar os oito campos do DTO e a ordenação `mercado/ticker/acaoId`, excluindo posições zeradas, cotação e indicadores fora do escopo.
- [x] 8.4 Testar HTTP do endpoint para `200`, `[]`, múltiplos DTOs, `404/StandardError`, ausência de `Location` e inexistência de rotas adicionais.
- [x] 8.5 Testar explicitamente ausência de escrita, replay de persistência, `Clock`, lock pessimista, atualização de cotação e chamadas a BRAPI, Alpha Vantage, BrasilAPI e ViaCEP.
- [x] 8.6 Testar snapshot consistente diante de registro concorrente e validar repository/H2, relacionamentos LAZY, Liquibase/Hibernate e PostgreSQL quando disponível, sem alteração de schema.

## 9. Verificação final

- [x] 9.1 Executar os testes direcionados de calculadora, posição, Operação e Carteira e registrar o total e o resultado.
- [x] 9.2 Executar a suíte completa do projeto e confirmar ausência de regressões nas capabilities promovidas.
- [x] 9.3 Executar `mvnw.cmd clean verify` e confirmar Liquibase/Hibernate com os changeSets 001–004 inalterados.
- [x] 9.4 Executar `openspec validate posicao-consolidada --strict` e a validação global em modo strict.
- [x] 9.5 Após futuras alterações de código, executar `graphify update .`, consultar o novo fluxo e finalizar com `git diff --check`, revisão do diff/status e auditoria de ausência de persistência de posição, providers e indicadores fora do escopo.
