## Why

A decisão aprovada em `2026-09-10-evoluir-cadastro-operacao-dashboard` exige preço final informado em COMPRA e VENDA, com prévia histórica independente e editável. Código e testes já seguem essa regra, mas a consolidação conservou cláusulas e títulos do contrato intermediário que exigia fechamento no POST. O schema público da prévia ainda afirma reconsulta do provider; o cadastro contextual de Carteira ainda descreve preço somente leitura.

## What Changes

- Reconciliar exclusivamente os cinco deltas abaixo, preservando requisitos completos e todos os comportamentos válidos.
- Distinguir preço negociado, sugestão da prévia, fechamento bruto e cotação corrente; preservar o preço final enviado e `valorTotal = quantidade × precoUnitario`.
- Corrigir futuramente as descrições em `PreviaPrecoCompraResponse.java` e `OperacaoResource.java`, sem alteração funcional.
- Proteger a descrição efetivamente servida pelo OpenAPI e corrigir somente o título obsoleto de um teste frontend, preservando suas asserções.
- Preservar referências, quantidade, data civil, precisão, ordem backend, replay, retroatividade, posição disponível, atomicidade, concorrência, Carteira capturada, Corretora opcional, operações antigas e fórmulas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `operation-registration`: retirar dependência histórica do POST e reconciliar conteúdos, sem enfraquecer validações; renomear requisito pelo mecanismo oficial e preservar os títulos históricos dos cenários.
- `purchase-price-preview`: fonte histórica da prévia, sugestão editável e independência do preço final.
- `stock-quote-history`: reconhecer prévia independente sem OHLC persistido, backfill ou alteração de observações.
- `api-documentation`: coerência do endpoint e schema, sem promessa de reconsulta no POST.
- `frontend-portfolio-management`: corrigir somente o cenário de cadastro contextual dentro do requisito completo.

## Impact

Somente planejamento nesta etapa. Implementação futura limitada a duas annotations Java, proteção OpenAPI e título de teste frontend. Nenhuma mudança em DTOs de criação, services, entidades, repositories, providers, fórmulas ou componentes Angular. Sem alteração direta das specs consolidadas nesta etapa.

## Non-goals

BRAPI 502 × 422 e demais classificações históricas; atualização automática de cotação; AAPL/defasagem; precisão de Ativos; novas funcionalidades; fórmulas; schema/migrations; revisão geral README/PRD; contagem geral de endpoints; homologação integrada; refatoração geral de testes; changes arquivadas.

## Restrição de consolidação

OpenSpec 1.9.0 não suporta rename real de cenário e verifica sua preservação nominal em MODIFIED. Por decisão aprovada, os nove títulos históricos dos cenários serão mantidos com seus conteúdos reconciliados para a semântica vigente. Os títulos novos mapeados no design são propostas não aplicadas, sem promessa de segunda etapa automática. A limitação é exclusivamente nominal e está aceita nesta change. As três renomeações de requisitos permanecem pelo mecanismo oficial RENAMED Requirements. Não remover requisitos, duplicar cenários nem alterar a baseline para contornar a proteção.
