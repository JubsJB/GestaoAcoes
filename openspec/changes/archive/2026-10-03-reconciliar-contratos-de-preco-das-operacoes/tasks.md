## 1. Reconciliação das specs

- [x] 1.1 Revisar os cinco deltas completos contra a baseline vigente e a regra canônica aprovada.
- [x] 1.2 Verificar a estratégia aprovada: preservar os nove títulos históricos dos cenários com os conteúdos reconciliados, mantendo as três renomeações oficiais de requisitos, sem duplicar comportamentos, remover requisitos ou editar baseline como atalho.
- [x] 1.3 Confirmar preservação dos contratos de historical-closing-price e de todas as invariantes financeiras/transacionais.

## 2. Annotations e documentação OpenAPI

- [x] 2.1 Corrigir somente a descrição de PreviaPrecoCompraResponse para retirar a reconsulta do provider pelo POST.
- [x] 2.2 Esclarecer em OperacaoResource a consulta sem efeitos colaterais e a sugestão inicial editável, sem mudar endpoint ou comportamento.

## 3. Título do teste frontend

- [x] 3.1 Corrigir somente o título obsoleto de COMPRA sem preço em operacoes.service.spec.ts, preservando asserções e fixtures.

## 4. Proteção automatizada OpenAPI

- [x] 4.1 Ampliar OpenApiDocumentationTest para verificar a semântica da prévia no schema efetivamente servido e sua coerência com o endpoint.

## 5. Testes backend diretamente afetados

- [x] 5.1 Executar OpenApiDocumentationTest e registrar resultado.
- [x] 5.2 Executar OperacaoContractTest, OperacaoResourceTest e PrecoOperacaoResourceTest, confirmando preço obrigatório e ausência de reconsulta no POST.

## 6. Testes frontend diretamente afetados

- [x] 6.1 Executar operacoes.service.spec.ts e confirmar payload com preço e sem ordem/total.
- [x] 6.2 Executar testes do formulário/contexto para preço editável, falha da prévia, respostas tardias e Carteira capturada.

## 7. Regressão necessária

- [x] 7.1 Executar regressão backend de OperacaoServiceTest e OperacaoConcurrencyTest, preservando total, replay, retroatividade, posição, ordem e atomicidade.
- [x] 7.2 Avaliar resultados e requisitos de entrega; ampliar suites somente se alterações, falhas ou riscos concretos justificarem. Registrar decisão e evidências, sem homologação integrada do MVP.

## 8. OpenSpec strict

- [x] 8.1 Obter aprovação de openspec validate reconciliar-contratos-de-preco-das-operacoes --strict --no-interactive após resolver a proteção nominal.
- [x] 8.2 Validar specs consolidadas com openspec validate --specs --strict --no-interactive, sem confundir validade estrutural com reconciliação semântica.

## 9. Consistência dos deltas

- [x] 9.1 Repetir combinação futura em memória com a ferramenta e comparar requisitos/cenários e Purposes; confirmar os nove títulos históricos de cenários preservados e as três renomeações oficiais RENAMED de requisitos, sem requisito órfão, nova duplicação ou perda semântica.
- [x] 9.2 Verificar que Cadastro contextual é o único cenário alterado em frontend-portfolio-management e que nenhum item fora do escopo foi incluído.

## 10. Validação final antes do archive

- [x] 10.1 Revisar git diff --check, status e diff completo, confirmando somente arquivos autorizados.
- [x] 10.2 Confirmar tasks executadas com evidências, ausência de bloqueios e revisão humana antes de qualquer archive; não arquivar nem realizar staging/commit/push nesta etapa.

As 19 tasks estão concluídas, sem pendências. A revisão humana final da task 10.2 foi aprovada; nenhum archive ou operação de escrita Git foi realizado.


## Evidências da implementação — 2026-10-03

- 1.1–1.3: relidos proposal, design, tasks, cinco deltas, specs consolidadas relacionadas e historical-closing-price. Revisão preserva preço final manual, precisão, total, replay, referências, ordem, atomicidade, concorrência e independência da prévia; capability histórica não recebe alterações.
- 2.1–4.1: modificadas somente duas descriptions OpenAPI, asserções de fragmentos contratuais no endpoint e schema efetivamente servidos e o título do teste frontend. DTO, endpoints, execução, fixtures e asserções frontend permanecem intactos.
- 5.1: mvn -Dtest=OpenApiDocumentationTest test — 9 testes, zero falhas/erros.
- 5.2: mvn "-Dtest=OperacaoContractTest,OperacaoResourceTest,PrecoOperacaoResourceTest" test — 28 testes (7 + 13 + 8), zero falhas/erros.
- 6.1: npm test -- --watch=false --include=src/app/features/operacoes/operacoes.service.spec.ts — 9 testes aprovados. Primeira tentativa bloqueada antes dos testes por spawn EPERM; repetição com execução ampliada aprovada.
- 6.2: npm test -- --watch=false --include=src/app/features/operacoes/pages/operacao-form-page.component.spec.ts --include=src/app/features/operacoes/pages/operacao-context.integration.spec.ts — 37 testes aprovados; cobrem edição, falha da prévia, respostas tardias e Carteira capturada.
- 7.1: mvn "-Dtest=OperacaoServiceTest,OperacaoConcurrencyTest" test — 23 testes (20 + 3), zero falhas/erros.
- 7.2: testes dirigidos e regressões passaram; nenhuma mudança funcional ou falha residual justifica ampliar suites ou homologar o MVP. Avisos preexistentes de APIs deprecated não foram corrigidos.
- 8.1: openspec validate reconciliar-contratos-de-preco-das-operacoes --strict --no-interactive — aprovado.
- 8.2: openspec validate --specs --strict --no-interactive — 35 aprovadas, zero falhas; validade estrutural não elimina as divergências semânticas corrigidas nos deltas.
- 9.1: buildUpdatedSpec oficial e validateSpecContent strict, somente em memória, aprovados nas cinco capabilities. Requisitos/cenários antes e depois: operation-registration 19/74; purchase-price-preview 3/14; stock-quote-history 9/22; api-documentation 8/22; frontend-portfolio-management 11/54; total 50/186. Zero perdas, novas duplicações ou requisitos órfãos; Purposes e requisitos fora dos deltas idênticos. Nove títulos históricos preservados; três RENAMED produzem exatamente um requisito novo e nenhum antigo por par.
- 9.2: comparação do requisito Detalhe básico da Carteira confirma que somente Cadastro contextual difere da baseline; delta não foi modificado durante a implementação.
- 10.1: git diff --check aprovado; diff completo e status revisados. Quatro arquivos rastreados alterados: DTO, resource, teste OpenAPI e título do teste frontend. A change inteira continua não rastreada, portanto seu tasks.md foi revisado diretamente. Nenhum arquivo funcional adicional, spec consolidada, provider, migration, README ou PRD alterado.
- 10.2: revisão humana final aprovada, sem achados bloqueantes ou correções recomendáveis; comportamento funcional preservado. Os 106 testes previamente executados foram aprovados, com zero falhas, e o strict da change foi aprovado. A revisão final não exigiu repetição dos testes, pois os arquivos de implementação e testes permaneceram inalterados após as execuções relevantes. Não houve staging, commit, push, merge ou archive.
