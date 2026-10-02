## 1. Preparação da implementação autorizada

- [x] 1.1 Confirmar autorização de implementação; revisar PRD, deltas e design, usando a change de shell como predecessora sem editar seus artefatos ou antecipar sincronização.

## 2. Contexto e URL da listagem

- [x] 2.1 Integrar a rota exata /operacoes ao binding de CarteiraNavigationService, preservando precedência, inicialização compartilhada e isolamento das rotas de cadastro/detalhe.
- [x] 2.2 Normalizar URL sem ID com replaceUrl, preservar parâmetros não relacionados e distinguir normalização automática de seleção explícita para não alterar preferência indevidamente.
- [x] 2.3 Sincronizar troca pelo seletor e voltar/avançar com contexto/URL sem loops; não gerar entrada nem consulta ao repetir a mesma seleção.

## 3. Histórico contextual e estados

- [x] 3.1 Substituir apenas a consulta da página principal por listarPorCarteira(id); preservar GET global, listar(), DTO, parser decimal, ordem e apresentação semântica existentes.
- [x] 3.2 Implementar invalidação visual e cancelamento/ignorância de respostas, erros e finalizações obsoletas nas trocas A/B ou perda de contexto; retry manual para contexto atual.
- [x] 3.3 Apresentar nome da Carteira e todos os estados especificados, sem segundo seletor, opção agregada ou fallback global; bloquear cadastro sem Carteira válida.

## 4. Cadastro, detalhe e retorno

- [x] 4.1 Criar links de nova operação/detalhe com carteiraId e origem=operacoes, estender helper de retorno apenas com destino interno conhecido e validar origem contextual incompleta.
- [x] 4.2 Preservar captura fixa e POST de A durante troca global para B; garantir cancelamento/conclusão e reload retornando ao histórico de A com nova consulta na reentrada.
- [x] 4.3 Preservar identidade do detalhe por ID, DTO compatível/GET no reload, origem durável e proteção contra conflito com carteiraId; fallback seguro sem contexto recuperável.

## 5. Testes automatizados de aceitação

- [x] 5.1 Atualizar carteira-navigation.service.spec.ts e integrações de contexto: URL A versus memória/preferência B, ausência de ID, fallback existente, ID inválido, armazenamento indisponível, normalização sem GET duplicado e sem persistir fallback.
- [x] 5.2 Atualizar operacoes-list-page.component.spec.ts: contexto carregando, coleção vazia, seleção inválida, erro de Carteiras, operações carregando, vazio contextual, conteúdo e erro/retry incluindo 404; ausência de chamadas globais.
- [x] 5.3 Cobrir A → B, A → B → voltar/avançar, respostas/erros tardios e finalize obsoleto, mesma seleção, invalidação de contexto e retry na Carteira atual com HTTP/Subjects controlados.
- [x] 5.4 Atualizar app.routes.spec.ts: substituir a expectativa de histórico global da predecessora pelo novo contrato, verificar menu, acesso direto/reload, parâmetro normalizado, nome da Carteira e ausência de segundo seletor.
- [x] 5.5 Atualizar operacao-context.integration.spec.ts e operacao-form-page.component.spec.ts: origem operacoes, COMPRA/VENDA com carteiraId fixo, troca A/B, cancelar/concluir, URL reconstruída e origem inválida bloqueada; preservar fluxos dashboard/carteira e dialogs.
- [x] 5.6 Atualizar operacao-detail-page.component.spec.ts e testes do helper: links de origem, DTO versus GET no reload, troca de seletor sem substituir item, conflito, acesso sem contexto e destino arbitrário rejeitado.
- [x] 5.7 Preservar/ampliar operacoes.service.spec.ts para os três GETs e precisão do DTO contextual; manter regressões financeiras, de ordenação recebida, layout semântico e de retorno legado.

## 6. Validação final após implementação

- [x] 6.1 Validar manualmente navegação A/B/voltar/avançar, acesso direto/reload e cadastro/detalhe/cancelamento/conclusão; conferir nome/contexto, teclado/foco e estados em desktop/mobile com cards sem corte.
- [x] 6.2 Executar npm test -- --watch=false e npm run build em frontend; registrar contagens, tamanho antes/depois e warnings, sem alterar budgets ou introduzir dependências.
- [x] 6.3 Executar openspec validate contextualizar-historico-operacoes-por-carteira --strict e git diff --check; revisar escopo e coerência dos deltas, preservação da predecessora, backend, banco e contratos globais.
- [x] 6.4 Atualizar Graphify após alterações de código e apresentar resultado para revisão; não presumir autorização de staging, commit, push, merge, sincronização ou arquivamento.

## Estado da implementação

Implementação, testes automatizados e validações técnicas concluídos. O usuário realizou e aprovou a validação manual de `/operacoes` contextual, identificação e troca de Carteira, URL, voltar/avançar, reload, estado vazio, cadastro com origem `operacoes` e Carteira fixa, cancelamento, detalhe, mobile, teclado e foco. Estado final: 21 tasks concluídas, nenhuma pendente.
