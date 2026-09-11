## Context

Auditoria: OperacaoCompraCreateRequest proibe preco; OperacaoService consulta FechamentoHistoricoService no POST; formulario ja suporta MAT_DIALOG_DATA/MatDialogRef usado em Carteira, mas Dashboard navega para rota. Pipeline de COMPRA preenche readonly por previa-compra. VENDA tem preco editavel e sugestao existente. Total visual ja usa strings/BigInt; total autoritativo permanece backend. PRD RF07-26 e regra de preco de operacao: nova instrucao explicita substitui fechamento automatico de COMPRA.

## Goals / Non-Goals

Mesmo formulario atende rota, Carteira e Dashboard. Sem alterar replay/ordem no dia/venda excedente ou providers. Manter previa-compra e servico historico para uso independente; consultar no formulario como sugestao inicial editavel, nunca dentro do POST.

## Decisions

COMPRA ganha mesmo campo/validadores BigDecimal da VENDA. Service valida preco recebido e passa ao comando transacional existente. Payload lossless envia preco normalizado string nos dois tipos. Estimativa visual existente preservada. Sugestao editavel de VENDA preservada, jamais usada como substituicao backend.

Dashboard importa formulario dinamicamente e abre MatDialog com objeto carteira capturado antes do import, sem seletor. Sucesso fecha e recarrega apenas resumo/posicoes/resultados se a carteira visivel ainda for a origem; snapshots manuais nao mudam nem sao criados. Se houve troca global, nao aplicar dados da origem a outra carteira. Nova selecao futura ja recarrega via fluxo existente. Bloqueio de abertura duplicada e submissao em andamento; erros mantem formulario aberto.

## Risks / Trade-offs

Contrato COMPRA muda conscientemente: clientes precisam enviar preco. Dados persistidos antigos nao sao reescritos. Revisao visual humana do dialog permanece pendente apos validacoes automatizadas.

## Migration Plan

Sem migracao de banco. Atualizar frontend/backend em conjunto. Preservar contratos de consulta historica e rota /operacoes/nova.

## Ajuste aprovado na revisao humana
COMPRA consulta previa-compra ao escolher acao/data e preenche sugestao lossless editavel. Mudanca de acao/data cancela resposta antiga e atualiza sugestao; edicao durante consulta nao e sobrescrita. POST usa o campo final, sem reconsulta. Falha historica mantem feedback existente e permite preco manual positivo. VENDA permanece inalterada. Form fields usam subscriptSizing dynamic para reservar mensagens multilinha sem sobreposicao. Auditoria PETR4 somente leitura, sem alteracao de formulas ou dados.
