## Why

A consulta de posições consolidadas hoje apresenta apenas o estado contábil derivado das Operações, embora o PRD também exija a cotação mais recente e o valor atual de cada posição aberta. A evolução deve reutilizar a cotação já persistida em `Acao`, sem transformar `GET /carteiras/{carteiraId}/posicoes` em um fluxo de atualização externa nem comprometer os cálculos históricos.

## What Changes

- Estender a resposta existente de `GET /carteiras/{carteiraId}/posicoes` com `cotacaoAtual`, `dataHoraCotacao` e `valorAtualPosicao`.
- Obter `cotacaoAtual` e `dataHoraCotacao` exatamente da `Acao` persistida associada às Operações consolidadas.
- Calcular `valorAtualPosicao = quantidadeAtual × cotacaoAtual` com `BigDecimal`, preservando multiplicação exata e apresentação em escala 12, sem arredondamento ou truncamento silencioso.
- Preservar `Operacao.precoUnitario` como única fonte de preço para `precoMedio` e `custoPosicao`; a cotação de mercado não participa do replay contábil.
- Manter a consulta read-only, sem `Clock`, escrita, lock pessimista, cache, materialização ou chamadas à BRAPI/Alpha Vantage.
- Preservar o endpoint existente, a omissão de posições zeradas, a ordenação e os contratos de Carteira inexistente ou sem posições.
- Ajustar o carregamento do histórico para obter a `Acao` associada no mesmo acesso, evitando N+1.
- Preservar `PATCH /acoes/{id}/cotacao`, capability restaurada na baseline atual, como fluxo dedicado para atualizar `Acao.cotacaoAtual` e `Acao.dataHoraCotacao`, separado da consulta de posições.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-position`: ampliar o contrato da posição consolidada com a última cotação persistida, sua referência temporal e o valor atual calculado, preservando o isolamento entre mercado e histórico de Operações.

## Impact

- API: o corpo de sucesso de `GET /carteiras/{carteiraId}/posicoes` ganha três campos, sem nova rota.
- Backend: `PosicaoResponse`, `PosicaoService`, `CalculadoraPosicao`, `PosicaoMapper` e a leitura de `OperacaoRepository`; `CarteiraResource` mantém a rota e a delegação atuais.
- Testes: contratos DTO/HTTP, cálculo, service, repository/carregamento, ausência de efeitos colaterais e regressões de Operações e atualização de cotação.
- Persistência: nenhuma nova entidade, tabela, coluna ou migration; os campos obrigatórios e positivos de `Acao` já existem no schema vigente.
- Integrações e dependências: nenhuma alteração de provider, adapter ou dependência; BRAPI e Alpha Vantage permanecem fora do GET.
