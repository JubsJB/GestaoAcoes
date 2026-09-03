## Context

O commit `bc6b743` consolidou `POST /operacoes` discriminado: COMPRA obtém fechamento histórico exato no backend e VENDA recebe preço do usuário. `CotacaoHistoricaProvider` já seleciona BRAPI ou Alpha Vantage por mercado; `Operacao` persiste `dataOperacao`, `ordemNoDia`, `id` e preço exato; e novas Operações do mesmo dia são anexadas por `MAX(ordemNoDia)+1`. Esta evolução acrescenta somente leituras para apoiar o formulário, preservando o POST e as regras financeiras vigentes.

## Goals / Non-Goals

**Goals:**

- Compartilhar integralmente a obtenção e validação do fechamento entre prévia e criação de COMPRA.
- Consultar a última COMPRA aplicável com uma query limitada e determinística.
- Oferecer contratos REST simples, lossless e explícitos para presença e ausência de sugestão.
- Manter endpoints consultivos sem transação externa prolongada, locks ou efeitos colaterais.

**Non-Goals:**

- Aceitar ou confiar em preço de COMPRA enviado pelo cliente.
- Alterar o fluxo transacional, a geração de `ordemNoDia` ou qualquer cálculo do POST.
- Calcular preço médio, posição, resultado, cotação atual ou recomendação financeira.
- Persistir prévias/sugestões, adicionar cache, migration, tabela, dependência ou provider.
- Implementar frontend ou executar Graphify nesta change.

## Decisions

### 1. Endpoints no domínio de Operações

Usar `GET /operacoes/previa-compra?ticker&mercado&dataOperacao` e `GET /carteiras/{carteiraId}/operacoes/sugestao-preco-venda?ticker&mercado&dataOperacao`.

A prévia não depende de Carteira, mas depende de Ação cadastrada, e representa um insumo da criação de Operação. A sugestão depende essencialmente da Carteira e fica aninhada nesse contexto. Query parameters representam os critérios da consulta e evitam confundir a prévia com cotação corrente ou histórico público de mercado. Alternativas sob `/acoes/{ticker}` foram rejeitadas porque escondem a finalidade operacional e poderiam sugerir uma API genérica de cotações.

### 2. DTOs mínimos e estáveis

`PreviaPrecoCompraResponse` será um record com `String ticker`, `Mercado mercado`, `Moeda moeda`, `LocalDate dataCotacao` e `BigDecimal precoUnitario`. Os metadados permitem ao frontend confirmar identidade, moeda e data exata sem expor payload ou nome do provider.

`SugestaoPrecoVendaResponse` conterá somente `BigDecimal precoUnitarioSugerido`, anulável. ID e data da COMPRA de origem não são necessários para preenchimento nem diagnóstico do usuário e ampliariam o acoplamento. `200` com campo nulo foi escolhido em vez de `204` porque mantém um schema JSON único, explícito e simples de consumir nos dois resultados normais.

### 3. Serviço compartilhado para fechamento histórico

Extrair da orquestração atual uma colaboração de aplicação que normalize/valide os dados do fechamento retornado por `CotacaoHistoricaProvider`, sem mover parsers dos adapters. A prévia e o ramo COMPRA do POST usarão essa mesma colaboração. O POST continuará executando sua própria consulta no momento do cadastro e continuará sendo a autoridade final.

A validação preliminar de ticker, mercado, data e existência da Ação ocorrerá antes da chamada externa. A chamada não ficará dentro de transaction ou lock. Nenhum resultado será salvo em `Acao`, `HistoricoCotacao` ou cache.

### 4. Corte temporal da sugestão de VENDA

A query filtrará `carteira_id`, `acao_id`, `tipo=COMPRA` e `data_operacao <= :dataOperacao`, ordenando por `data_operacao DESC, ordem_no_dia DESC, id DESC` e limitando a um resultado. Isso preserva coerência em venda retroativa e evita carregar/reproduzir todo o histórico.

Na mesma data, toda COMPRA já persistida é elegível. A razão é determinística: a nova VENDA ainda não possui ordem, mas o POST vigente sempre a anexará após todas as Operações já persistidas daquele dia. Assim, a COMPRA existente de maior ordem realmente precederá a nova VENDA. `id` é apenas desempate defensivo e não redefine a ordem financeira.

### 5. Validações e erros

Ambas as consultas reutilizarão normalização de ticker, enum de mercado, validação de `LocalDate` e existência das entidades. A prévia propagará os códigos históricos já vigentes: `COTACAO_HISTORICA_INDISPONIVEL`, `HISTORICO_COTACAO_FORA_DO_ALCANCE`, `TICKER_INEXISTENTE`, `LIMITE_REQUISICOES_EXCEDIDO`, `RESPOSTA_EXTERNA_INVALIDA`, `SERVICO_EXTERNO_INDISPONIVEL` e `SERVICO_EXTERNO_TIMEOUT`. Não serão criados códigos duplicados.

Na sugestão, ausência de COMPRA é `200` nulo. Carteira/Ação inexistente é `404`; entrada inválida é `400`. Ela nunca chama integração externa, portanto não declara erros de provider.

### 6. OpenAPI e compatibilidade

Os resources documentarão parâmetros, schemas, nulabilidade, respostas e semântica informativa. Testes de contrato protegerão simultaneamente os novos endpoints e o schema discriminado de `POST /operacoes`, incluindo rejeição de preço em COMPRA e de ordem em ambas as variantes.

## Risks / Trade-offs

- [A prévia e o POST fazem duas chamadas externas] → Aceitar nesta evolução; o fechamento passado é estável, mas o POST deve revalidar e não confiar no cliente. Não adicionar cache prematuro.
- [Provider pode ficar indisponível após a prévia] → O POST preserva seus erros e atomicidade; a UI trata a prévia como informativa.
- [Nova COMPRA pode surgir após uma sugestão de VENDA] → Não afeta integridade, pois a sugestão não é vinculante e o usuário escolhe o preço final.
- [Query derivada pode gerar assinatura extensa ou limite implícito inadequado] → Preferir query explícita/projeção ou método `findFirst...OrderBy...Desc`, com teste de repository que prove filtros, ordenação e limite.
- [Extração do fechamento compartilhado pode causar regressão no POST] → Cobrir o fluxo existente de COMPRA e todos os códigos externos antes e depois da extração; não alterar DTOs nem persistência do POST.

## Migration Plan

1. Adicionar serviços/DTOs/endpoints consultivos e a query de repository sem alterar schema.
2. Extrair e compartilhar somente a obtenção/validação histórica usada pelo POST.
3. Publicar os endpoints de forma aditiva; clientes existentes continuam compatíveis.
4. Em rollback, remover endpoints, DTOs, serviço consultivo e query; não há dados para migrar ou reverter.
