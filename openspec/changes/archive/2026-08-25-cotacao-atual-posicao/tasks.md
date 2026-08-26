## 1. Verificação de baseline e contratos

- [x] 1.1 Confirmar e preservar a capability restaurada `PATCH /acoes/{id}/cotacao` como fluxo dedicado de atualização de `Acao.cotacaoAtual` e `Acao.dataHoraCotacao`, sem reutilizar providers, escrita ou lock no GET de posições.
- [x] 1.2 Revisar os testes atuais de posição, Ação e Operações e fixar expectativas de regressão para `precoMedio`, `custoPosicao`, atualização dedicada de cotação, ausência de chamadas externas e ausência de escrita.
- [x] 1.3 Atualizar o contrato de `PosicaoResponse` com `cotacaoAtual`, `dataHoraCotacao` e `valorAtualPosicao`, preservando todos os campos e a ordem conceitual já existentes.

## 2. Cálculo e projeção da posição

- [x] 2.1 Adicionar à `CalculadoraPosicao` o cálculo isolado `quantidadeAtual × cotacaoAtual` com `BigDecimal`, produto exato, escala 12, precisão máxima 38 e `RoundingMode.UNNECESSARY` na normalização.
- [x] 2.2 Reutilizar o tratamento `422/CALCULO_POSICAO_FORA_DA_PRECISAO` quando o valor atual não puder ser representado, sem resposta parcial ou arredondamento silencioso.
- [x] 2.3 Ajustar `PosicaoService` para, somente após replay válido e omissão de quantidade zero, obter da Ação persistida `cotacaoAtual` e `dataHoraCotacao`, calcular `valorAtualPosicao` e entregar os valores prontos ao mapper.
- [x] 2.4 Ajustar `PosicaoMapper` apenas para projetar cotação, data/hora e valor atual já calculado, sem mover cálculo financeiro para o mapper.
- [x] 2.5 Preservar `@Transactional(readOnly = true, isolation = REPEATABLE_READ)`, sem `Clock`, lock pessimista, save, cache, materialização ou acesso a providers no fluxo de `GET /carteiras/{carteiraId}/posicoes`.

## 3. Carregamento e performance

- [x] 3.1 Definir fetch plan explícito no método de histórico usado pela consolidação para carregar `Operacao.acao` no mesmo acesso e preservar `dataOperacao ASC`, `ordemNoDia ASC`, `id ASC`.
- [x] 3.2 Verificar por teste de integração/estatística de consultas que múltiplas posições não executam uma consulta adicional por Ação e que nenhuma Corretora ou dado fora do necessário é carregado deliberadamente.
- [x] 3.3 Confirmar que Carteira e Ações são observadas no mesmo snapshot read-only durante a consolidação concorrente, mantendo o comportamento transacional vigente.

## 4. Testes unitários de cálculo e service

- [x] 4.1 Cobrir posição BRASIL com cotação BRL e posição EUA com cotação USD, incluindo quantidade fracionária americana.
- [x] 4.2 Cobrir `valorAtualPosicao` para produto inteiro e fracionário, escala 12, precisão `BigDecimal`, produto exato e falha integral fora dos limites.
- [x] 4.3 Cobrir múltiplas posições e moedas diferentes, confirmando ausência de soma, agregação patrimonial ou conversão cambial.
- [x] 4.4 Cobrir preservação de `precoMedio` e `custoPosicao` antes e depois de variar `Acao.cotacaoAtual`, demonstrando que a cotação altera somente `valorAtualPosicao`.
- [x] 4.5 Cobrir que `dataHoraCotacao` devolvida é exatamente a persistida e não é substituída por relógio ou instante do GET.
- [x] 4.6 Cobrir posição zerada omitida, Carteira sem Operações, Carteira somente com posições encerradas e Carteira inexistente.
- [x] 4.7 Cobrir ausência de escrita em Carteira, Operação e Ação e ausência de chamadas a BRAPI, Alpha Vantage ou qualquer `CotacaoProvider` durante a consulta.
- [x] 4.8 Preservar e ampliar os testes de histórico inconsistente, ordem cronológica, venda parcial/total, novo ciclo e cálculo periódico com `HALF_EVEN` para demonstrar regressão zero no replay.

## 5. Testes de contrato HTTP e integração

- [x] 5.1 Atualizar `PosicaoContractTest` para validar os onze campos de `PosicaoResponse` e que o mapper apenas projeta estado persistido e cálculo recebido.
- [x] 5.2 Atualizar `PosicaoResourceTest` para validar `200 OK`, JSON de BRASIL/EUA, múltiplas posições ordenadas, decimais exatos, `dataHoraCotacao`, lista vazia, `404/StandardError` e ausência de `Location`.
- [x] 5.3 Validar por teste HTTP/service que o GET não contém resultado realizado, resultado não realizado, rentabilidade, patrimônio ou outros indicadores fora do escopo.
- [x] 5.4 Preservar os testes concorrentes de posição e cobrir que a resposta representa um snapshot consistente anterior ou posterior a uma Operação concorrente.
- [x] 5.5 Executar testes de regressão de `POST /operacoes`, consultas de Operação, validação de VENDA, posição zerada e proteção de exclusão da Carteira.
- [x] 5.6 Executar os testes de regressão da atualização dedicada de cotação cobrindo `PATCH /acoes/{id}/cotacao`, BRASIL, EUA, regra monotônica de timestamp, concorrência com timestamps diferentes e `409/TICKER_CANONICO_DIVERGENTE`, além das falhas externas e da preservação da última cotação válida.

## 6. Schema, validação e entrega

- [x] 6.1 Confirmar em PostgreSQL/H2 que `acao.cotacao_atual` e `acao.data_hora_cotacao` continuam `NOT NULL`, a cotação continua positiva e nenhuma migration ou alteração dos changeSets 001–004 foi adicionada.
- [x] 6.2 Executar a inicialização Liquibase e Hibernate com `spring.jpa.hibernate.ddl-auto=validate` e confirmar que o schema vigente suporta a change sem alteração.
- [x] 6.3 Executar os testes relevantes e a suíte completa pelo Maven Wrapper, incluindo `clean verify`, sem chamadas a integrações reais.
- [x] 6.4 Validar `cotacao-atual-posicao` em modo strict e validar também todo o conjunto OpenSpec em modo strict.
- [x] 6.5 Atualizar o Graphify com `graphify update .` após as futuras alterações de código e revisar o subgrafo de posição/cotação para confirmar as dependências finais.
