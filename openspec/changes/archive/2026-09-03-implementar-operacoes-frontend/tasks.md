## 1. Contratos e precisão

- [x] 1.1 Substituir o request único por `OperacaoCompraCreateRequest`, `OperacaoVendaCreateRequest` e união discriminada, mantendo `OperacaoResponse` independente.
- [x] 1.2 Garantir tipagem que torne `precoUnitario` impossível em COMPRA, obrigatório em VENDA e `ordemNoDia` impossível em qualquer create request.
- [x] 1.3 Preservar quantidade, preço, preço sugerido e total dos responses como strings decimais no limite HTTP/apresentação, sem nova biblioteca decimal.
- [x] 1.4 Corrigir e testar quantidade para no máximo 13 dígitos inteiros e 6 fracionários, inteira para BRASIL e fracionária para EUA, com normalização lexical sem `number`.
- [x] 1.5 Preservar data civil por mercado e envio literal de `YYYY-MM-DD`, sem hora nem conversão UTC.
- [x] 1.6 Ampliar e revalidar o adaptador JSON lossless para `precoUnitarioSugerido`, preservando os campos atuais, nulos, escapes e contrato malformado.

## 2. Serviço HTTP e payloads

- [x] 2.1 Manter `cadastrar` discriminado e adicionar ao `OperacoesService` os dois GETs consultivos tipados, sem lógica de provider ou retry automático.
- [x] 2.2 Testar payload exato de COMPRA sem `precoUnitario`, inclusive nulo, e sem `ordemNoDia`, `id`, `acaoId` ou `valorTotal`.
- [x] 2.3 Testar payload exato de VENDA com `precoUnitario` obrigatório e sem `ordemNoDia`, `id`, `acaoId` ou `valorTotal`.
- [x] 2.4 Testar `corretoraId` nulo e omitido nos dois membros da união.
- [x] 2.5 Preservar os GETs atuais e integrar prévia/sugestão com leitura lossless e erros normalizados.
- [x] 2.6 Preservar rotas `/operacoes`, `/operacoes/nova` e `/operacoes/:id` no limite lazy, com `nova` antes de `:id`.

## 3. Formulário discriminado

- [x] 3.1 Remover `ordemNoDia` do FormGroup, template, validação, mensagens e construção de payload.
- [x] 3.2 Em COMPRA, manter preço visível e readonly, sem validator, preenchido somente pela prévia histórica exata.
- [x] 3.3 Em VENDA, preservar preço editável/validado e preenchê-lo inicialmente pela sugestão quando presente.
- [x] 3.4 Construir payload por discriminante, sem espalhar o valor bruto do formulário.
- [x] 3.5 Ao alternar VENDA para COMPRA, limpar preço, remover validators, tornar readonly e consultar nova prévia sem vazamento no payload.
- [x] 3.6 Ao alternar COMPRA para VENDA, descartar prévia, habilitar edição e consultar sugestão sem reutilizar o preço de COMPRA.
- [x] 3.7 Remover `positiveIntegerValidator` se ficar sem consumidor e atualizar seus testes.
- [x] 3.8 Integrar mudanças de Carteira, Ação/mercado e data à invalidação e reconsulta do preço, preservando referências persistidas.
- [x] 3.9 Garantir que a data escolhida em COMPRA seja enviada literalmente, inclusive quando não houver pregão, sem substituição local.

## 4. Erros e submissão

- [x] 4.1 Mapear `COTACAO_HISTORICA_INDISPONIVEL` também no GET de prévia e manter a COMPRA bloqueada.
- [x] 4.2 Mapear `HISTORICO_COTACAO_FORA_DO_ALCANCE` também no GET de prévia e manter a COMPRA bloqueada.
- [x] 4.3 Mapear `TICKER_INEXISTENTE` e `LIMITE_REQUISICOES_EXCEDIDO` da prévia sem fallback ou retry automático.
- [x] 4.4 Revalidar `502`, `503` e `504` dos GETs pelo tratamento técnico central, preservando formulário e estado inválido da prévia.
- [x] 4.5 Preservar tratamento contextual de `POSICAO_INSUFICIENTE`, referências inexistentes, request inválido e integridade.
- [x] 4.6 Remover tratamento de `ORDEM_OPERACAO_DUPLICADA` orientado a alterar ordem manual na criação.
- [x] 4.7 Preservar double-submit do POST e bloquear COMPRA durante loading/ausência/erro da prévia correspondente.
- [x] 4.8 Testar que, após conclusão do POST, novo envio deliberado é aceito mesmo com payload igual, sem idempotency key ou deduplicação local.

## 5. Listagem e detalhe

- [x] 5.1 Preservar listagem global com loading, vazio, conteúdo, erro e retry manual, na ordem recebida do backend.
- [x] 5.2 Preservar `precoUnitario`, `ordemNoDia` e `valorTotal` nos responses e na ordenação, exibindo preço e total formatados no detalhe sem expor `ordemNoDia` nessa tela.
- [x] 5.3 Preservar detalhe somente leitura, estado transitório compatível, fallback GET, 404 e retorno à origem.
- [x] 5.4 Acrescentar teste explícito de que o preço retornado para COMPRA é exibido sem cálculo ou substituição local.
- [x] 5.5 Preservar ausência de edição, exclusão, cotação, preço médio, posição e resultados.

## 6. Integração contextual em Carteira

- [x] 6.1 Preservar histórico independente no detalhe da Carteira e consulta a `GET /carteiras/{id}/operacoes`.
- [x] 6.2 Confirmar que o dialog contextual reutiliza também o pipeline de prévia/sugestão com Carteira fixa.
- [x] 6.3 Testar COMPRA contextual com prévia visual mas sem preço/ordem no POST e VENDA com sugestão editável/sem ordem.
- [x] 6.4 Preservar fechamento do dialog, toast e incorporação do DTO retornado no histórico por `dataOperacao`, `ordemNoDia` e `id`.
- [x] 6.5 Atualizar o teste de integração real do dialog para cobrir os GETs consultivos, sucesso e payload discriminado.
- [x] 6.6 Preservar Editar/Excluir Carteira quando somente o histórico falhar e não consultar histórico quando a Carteira não existir.

## 7. UX, acessibilidade e regressão

- [x] 7.1 Preservar PageHeader, StickyBack, FeedbackAlert, SuccessToast, Material e tokens locais sem dependências ou assets remotos.
- [x] 7.2 Testar acessibilidade do campo único, readonly/editável, moedas, loading, mensagens e estados do submit.
- [x] 7.3 Preservar responsividade de lista, formulário, detalhe e histórico contextual sem redesign amplo.
- [x] 7.4 Preservar shell, navegação global, placeholder do Dashboard e limites lazy das demais features.

## 8. Verificação automatizada

- [x] 8.1 Executar testes focados reconciliados de models, lossless JSON, validators, service, formulário, lista, detalhe e contexto de Carteira.
- [x] 8.2 Reexecutar a suíte completa do frontend após a implementação reconciliada.
- [x] 8.3 Reexecutar type-check/lint disponível e build de produção após a implementação reconciliada.
- [x] 8.4 Confirmar ausência de provider direto, retry do POST, ordem/hora manual, preço no payload de COMPRA, fallback, idempotency key e cálculos financeiros.

## 9. Bateria manual

- [x] 9.1 Validar navegação, deep links, reload, loading, vazio, erro/retry de GET, lista e detalhe em desktop/mobile.
- [x] 9.2 Registrar COMPRA brasileira e americana, com e sem Corretora, confirmando preço readonly em BRL/USD e ausência de preço/ordem no POST.
- [x] 9.3 Registrar VENDA com sugestão presente e ausente, confirmando edição livre, preço final no POST e ausência de ordem.
- [x] 9.4 Alternar COMPRA/VENDA nos dois sentidos e mudar Carteira/Ação/data, confirmando limpeza imediata e ausência de resposta antiga.
- [x] 9.5 Validar `TICKER_INEXISTENTE`, os dois 422, 429, 502, 503 e 504 na prévia, sem fallback e com submit bloqueado.
- [x] 9.6 Validar double-submit pendente e novo envio deliberado após conclusão.
- [x] 9.7 Validar fluxo contextual com Carteira fixa, os dois GETs consultivos, sucesso atualizando histórico e ausência de GET redundante obrigatório.
- [x] 9.8 Confirmar pela rede ausência de providers diretos, preço/ordem proibidos no POST de COMPRA e cálculos financeiros locais.

## 10. Encerramento

- [x] 10.1 Reexecutar strict da change e global após implementar todas as tasks reconciliadas.
- [x] 10.2 Reexecutar `git diff --check` e revisar o escopo após a implementação reconciliada.
- [x] 10.3 Registrar Graphify como dispensado nesta change por decisão explícita, sem torná-lo requisito de conclusão.
- [x] 10.4 Atualizar o handoff com resultados reconciliados e confirmar backend, dependências, archives e specs promovidas intactos.

## 11. Reconciliação com consultas de preço do backend `de4a849`

- [x] 11.1 Adicionar `PreviaPrecoCompraResponse` com ticker, mercado, moeda, dataCotacao e precoUnitario lossless.
- [x] 11.2 Adicionar `SugestaoPrecoVendaResponse` com `precoUnitarioSugerido` lossless e anulável.
- [x] 11.3 Testar URL, query params e response HTTP real de `GET /operacoes/previa-compra` para BRL e USD.
- [x] 11.4 Testar URL, query params e response presente/nulo de `GET /carteiras/{id}/operacoes/sugestao-preco-venda`.
- [x] 11.5 Implementar estado único do contexto de preço usando tipo, Carteira efetiva, Ação canônica e data.
- [x] 11.6 Invalidar imediatamente preço/prévia/sugestão quando qualquer parte do contexto mudar ou ficar incompleta.
- [x] 11.7 Usar cancelamento lógico com `switchMap` ou equivalente para impedir resposta fora de ordem.
- [x] 11.8 Exibir loading acessível da prévia de COMPRA sem manter preço anterior.
- [x] 11.9 Exibir preço de COMPRA readonly com moeda retornada, sem permitir digitação nem aplicar validator.
- [x] 11.10 Bloquear submit de COMPRA até existir prévia válida para a chave atual.
- [x] 11.11 Permitir recuperação após erro por nova mudança válida ou retry explícito, sem retry automático.
- [x] 11.12 Preencher sugestão de VENDA sem desabilitar, travar ou alterar os validators do campo.
- [x] 11.13 Tratar sugestão nula como estado normal, mantendo preço vazio, editável e obrigatório.
- [x] 11.14 Provar que preço maior ou menor que a sugestão é aceito e que o POST usa o valor final editado.
- [x] 11.15 Testar limpeza em mudanças de Carteira, Ação e data nos fluxos COMPRA e VENDA.
- [x] 11.16 Testar COMPRA→VENDA sem reutilizar prévia e VENDA→COMPRA sem reutilizar valor manual/sugerido.
- [x] 11.17 Testar que resposta atrasada de prévia não sobrescreve nova COMPRA.
- [x] 11.18 Testar que resposta atrasada de sugestão não sobrescreve nova VENDA.
- [x] 11.19 Integrar `HttpErrorResponse`, interceptor real, parsing textual lossless, service e mensagem do formulário em pelo menos um erro histórico.
- [x] 11.20 Cobrir todos os códigos da prévia: 400, 404/TICKER_INEXISTENTE, 422, 429, 502, 503 e 504.
- [x] 11.21 Revalidar página global e dialog contextual com exatamente a mesma implementação e sem lógica duplicada.
- [x] 11.22 Executar bateria manual reconciliada com preço visível em COMPRA, sugestão editável em VENDA e inspeção de rede dos payloads.
- [x] 11.23 Impedir que sugestão de VENDA pendente sobrescreva preço digitado manualmente e cobrir o caso por teste.
