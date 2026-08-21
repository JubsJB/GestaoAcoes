## Why

O cadastro de Ação armazena a última cotação disponível, mas o sistema ainda não oferece a atualização sob demanda prevista por `RF11`. Esta change permite renovar essa informação sem alterar a identidade da Ação, sem criar histórico e sem acoplar o fluxo aos adapters concretos.

## What Changes

- Adicionar `PATCH /acoes/{id}/cotacao`, sem corpo de requisição e sem `Location`, para solicitar a atualização da cotação de uma Ação persistida, respondendo `200 OK` com o `AcaoResponse` completo no estado efetivamente persistido.
- Localizar a Ação pelo ID e selecionar o `CotacaoProvider` exclusivamente a partir do ticker e do mercado persistidos: BRAPI para `BRASIL` e Alpha Vantage para `EUA`.
- Exigir uma nova cotação positiva e exatamente representável na precisão atual, aplicando a política existente de timestamp do provider normalizado em UTC e fallback para o instante UTC da consulta.
- Atualizar somente `cotacaoAtual` e `dataHoraCotacao`, preservando ticker, nome da empresa, mercado, moeda e identificador.
- Manter a chamada externa fora da transação e restringir a transação final à releitura protegida e persistência da Ação.
- Preservar a última cotação válida quando a atualização falhar e informar essa preservação no erro padronizado.
- Tratar Ação inexistente, ticker não mais encontrado, rate limit, timeout, indisponibilidade, configuração ausente, resposta externa inválida ou incompleta e cotação inválida, sem gravação parcial.
- Impedir que ticker canônico divergente altere silenciosamente a identidade persistida, rejeitando a atualização com `409/TICKER_CANONICO_DIVERGENTE` e preservando integralmente o registro.
- Tratar atualizações concorrentes sem manter a chamada externa sob lock e sem permitir regressão temporal da cotação persistida.
- Criar testes unitários, de persistência, de integração HTTP e de concorrência proporcionais ao fluxo, sem chamadas reais aos providers.
- Não alterar o cadastro, os GETs, schema, Liquibase, dependências ou configurações e não adicionar histórico, agendamento, lote ou funcionalidades financeiras.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `stock-registration`: acrescentar a atualização sob demanda da última cotação persistida, selecionando o provider pelo mercado da Ação, preservando sua identidade e mantendo a última cotação válida em falhas.

## Impact

- API: novo endpoint dedicado de atualização de cotação, sem alteração dos contratos atuais de `POST /acoes`, `GET /acoes` e `GET /acoes/{id}`.
- Aplicação: ampliação de `AcaoResource` e `AcaoService`, reutilização de `CotacaoProvider`, `BrapiAdapter`, `AlphaVantageAdapter`, `AcaoRepository`, `AcaoMapper`, `AcaoResponse`, `Clock` e tratamento centralizado de erros.
- Domínio/persistência: adicionar um método de domínio específico e `AcaoCotacaoPersistenceService` para alterar apenas cotação e timestamp em uma seção transacional final curta, sem setters genéricos, mudança de schema ou nova dependência; `AcaoPersistenceService` permanece restrito ao cadastro.
- Integrações: nenhum novo provider, endpoint externo, SDK ou configuração; os adapters existentes permanecem responsáveis por BRAPI e Alpha Vantage.
- Testes: ampliação dos testes de service, persistência/repository e resource, preservando toda a suíte existente.
