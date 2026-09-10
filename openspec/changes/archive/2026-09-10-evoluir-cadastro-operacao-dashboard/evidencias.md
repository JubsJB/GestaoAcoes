# Evidencias - 2026-09-10

Registro inicial preservado abaixo. A secao Ajustes da revisao humana substitui exclusivamente a decisao inicial de nao consultar previa no formulario; o POST continua independente.

## Implementacao
- COMPRA e VENDA exigem precoUnitario manual positivo em BigDecimal, com validacao de escala existente. POST de COMPRA nao consulta fechamento historico.
- Total, replay, preco medio, venda superior a posicao, quantidade por mercado, corretora opcional e separacao BRL/USD preservados.
- Formulario existente reutilizado no Dashboard via import lazy e MatDialog. Carteira capturada na abertura, sem seletor local. Mudanca global nao reatribui a operacao.
- Sucesso fecha dialog e atualiza resumo/posicoes/resultados (3 GETs) apenas se a origem ainda estiver visivel. Nao cria snapshot nem recarrega historico manual.
- Erro preserva dialog e valores; submissao duplicada e fechamento durante envio bloqueados.
- /operacoes/nova preservada. Endpoint de previa historica preservado como consulta independente, nao utilizado na COMPRA. Sugestao editavel de VENDA preservada.
- Deltas reconciliam somente regras afetadas. Titulos antigos de cenarios foram mantidos para rastreabilidade exigida pelo strict; seu conteudo registra a substituicao aprovada da regra historica.

## Validacoes
- Backend focados finais: 63/63, zero falhas, BUILD SUCCESS.
- Maven verify final: 459/459, zero falhas/erros/skips, BUILD SUCCESS.
- Frontend focados finais: 90/90 (7 arquivos).
- Frontend completo: 485/485 (68 arquivos), VITEST_MAX_WORKERS=2.
- Frontend production: aprovado. Nenhuma alteracao funcional frontend depois dessa execucao.
- Testes antigos que exigiam consulta historica foram atualizados para preco manual. Concorrencia continua esperando o resultado contabil correto dos precos enviados (compra 10, venda 15, quantidade 40: resultado 200).
- Mocks/stubs usados; nenhuma API financeira real consumida pelos testes.

## Performance
- Initial: 514416 -> 514405 bytes.
- Dashboard lazy: 66669 -> 67377 bytes (+708), abertura contextual do dialog.
- CSS Dashboard e graficos nao alterados por esta change; baseline preservado: Dashboard 3091, evolucao 5685, container analitico 415, custo 2046, composicao 2168, performance 2500 bytes.
- Budgets e dependencias inalterados; form/dialog continuam lazy.
- Warnings conhecidos: initial 514.40 kB acima do warning de 500 kB; CSS evolucao 5.68 kB acima do warning de 4 kB. Sem aumento de budgets.
- Maven: avisos conhecidos de SpringDoc habilitado e JVM class sharing; sem falha de build.

## Encerramento desta rodada
- OpenSpec strict change e global aprovados; git diff --check aprovado.
- Working tree preservado com alteracoes desta e de changes anteriores, sem staging/commit.
- Pendente somente revisao humana do dialog desktop e do fluxo de cadastro. Nao declarada como executada.
- Nova change nao arquivada. Nenhum Graphify ou comando Git de mutacao executado.


## Ajustes da revisao humana
- COMPRA consulta o GET existente previa-compra por acao/mercado/data e oferece preco lossless sugerido, editavel. O campo final e enviado sem consulta no POST.
- switchMap cancela selecao antiga; contador de edicoes impede sugestao tardia de sobrescrever entrada manual. Troca de data/acao invalida sugestao antiga e consulta a nova. VENDA mantida.
- Erro historico preserva codigo/message/details e orientacao existente. Usuario pode informar preco positivo manualmente; nenhuma data e substituida.
- Form fields passaram a subscriptSizing=dynamic. Erros e helpers multilinha entram no fluxo, sem CSS absoluto, overflow oculto ou reducao de fonte.
- Edge real headless 1440x900, build production com dados simulados: sugestao 48,123456 preenchida e editavel; dois erros terminam em Y=528,19, Corretora inicia Y=540,08. Campos alinhados em Y=441,08, dialog 768px de largura; sem overflow global. Nenhum POST realizado. Screenshots/script/resultados locais ignorados em frontend/.cache/operation-review.
- Backend focados: 33/33. Maven verify: 459/459, BUILD SUCCESS. Testes existentes confirmam POST manual exato sem chamada historica; nenhuma implementacao backend alterada nesta correcao.
- Frontend focados: 66/66. Suite completa: 489/489, 68 arquivos, maximo 2 workers. Production aprovado.
- Warnings conhecidos preservados: initial 514,40kB (warning 500kB), CSS evolucao 5,68kB (warning 4kB); budgets/dependencias e CSS dos graficos inalterados.
- OpenSpec strict change/global e git diff --check aprovados. Working tree sem staging, preservando alteracoes anteriores.

## Auditoria PETR4 - somente leitura
Fontes: GET /operacoes, /acoes, /carteiras e /carteiras/5/posicoes no backend local. Nenhum POST/PATCH/DELETE nem atualizacao de cotacao externa nesta auditoria.
Carteira que corresponde aos valores relatados: id 5, Americana. As operacoes PETR4 da carteira 3 sao independentes e nao entram nesta conta.

| ID | Data | Ordem no dia | Tipo | Quantidade | Preco unitario | Total | Quantidade acumulada | Custo acumulado |
|---|---|---|---|---|---|---|---|---|
| 11 | 2026-09-09 | 1 | COMPRA | 10 | 48.420000 | 484.200000000000 | 10 | 484.200000000000 |
| 13 | 2026-09-10 | 1 | COMPRA | 13 | 80.000000 | 1040.000000000000 | 23 | 1524.200000000000 |

Nao ha vendas PETR4 na carteira 5.
- Preco medio inicial 48,42; apos compra id13: 1524,20 / 23 = 66,269565217391 (escala autoritativa).
- Cotacao persistida PETR4: 47.110000 BRL, timestamp 2026-09-07T16:27:30Z, equivalente a 07/09/2026 13:27:30 local. Auditoria nao consultou mercado atual nem atualizou esse valor.
- Valor atual: 23 x 47,11 = 1083,53.
- Resultado nao realizado: 1083,53 - 1524,20 = -440,67.
- Rentabilidade: -440,67 / 1524,20 x 100 = -28,911560%, exibida como -28,91%.
- Antes da compra id13, com a mesma cotacao: 10 x 47,11 - 484,20 = -13,10. Nova compra acrescenta 13 x 47,11 - 1040 = -427,57 ao resultado. Total: -13,10 -427,57 = -440,67.
- PosicaoService usa CalculadoraPosicao e CalculadoraRentabilidade existentes; os valores da conta coincidem com os campos autoritativos retornados.
- Classificacao A: diferenca explicada pelo preco de R$80 informado na operacao id13. Nao ha evidencia de erro de replay/preco medio ou divergencia entre cotacao persistida e utilizada. Nao e possivel afirmar que R$80 foi um erro de digitacao/teste sem confirmacao do usuario. Nenhum dado alterado.

## Ponto de parada
11/11 tasks tecnicas comprovadas. Change permanece ativa, sem encerramento/archive. Revisao humana dos ajustes do dialog continua pendente; confirmacao do preco da operacao 13 cabe ao usuario, sem alteracao automatica.

## Aprovacao humana final
Usuario aprovou explicitamente dialog Dashboard, carteira fixa, COMPRA sugerida/editavel, VENDA preservada, preco obrigatorio positivo, estimativa, mensagens sem sobreposicao e desktop. Esta aprovacao encerra a pendencia humana registrada anteriormente. 11/11 tasks concluidas, zero abertas, nenhuma pendencia bloqueante. Archive autorizado. Testes/build anteriores reutilizados sem repeticao por esta atualizacao documental.

Auditoria pos-archive: CLI promoveu apenas a ultima secao de cada tipo no delta frontend com secoes repetidas. Sincronizacao canonica completada com os blocos aprovados restantes, sem alterar requisitos ou artefatos originais. Strict global revalidado.
