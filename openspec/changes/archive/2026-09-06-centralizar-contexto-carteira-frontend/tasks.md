## 1. Contexto compartilhado

- [x] 1.1 Implementar store leve com coleção, ID ativo e estados de inicialização/vazio/erro, sem dados financeiros e compartilhando a carga inicial de shell e Dashboard.
- [x] 1.2 Implementar precedência URL/memória/preferência/fallback por id ASC, validação de IDs e recuperação explícita de URL inválida.
- [x] 1.3 Implementar persistência versionada somente da seleção explícita, tolerância a falhas e preferência inválida, sem sincronização live entre abas.
- [x] 1.4 Implementar coordenador de navegação sem loops, normalização por replaceUrl, preservação de parâmetros e suporte a voltar/avançar.
- [x] 1.5 Testar coleção vazia/única/múltipla, erro de carga, URL versus preferência, storage indisponível e respostas da coleção concorrentes com mutações.

## 2. Shell e Dashboard

- [x] 2.1 Integrar seletor global próximo à identificação, com estados, label e operação acessível nos layouts existentes.
- [x] 2.2 Remover Operações somente da sidebar/drawer, preservando três rotas e limite lazy; testar acessos diretos.
- [x] 2.3 Remover seletor e carga de Carteiras do Dashboard e consumir o contexto compartilhado com carteiraId na URL.
- [x] 2.4 Preservar consultas financeiras e evolução independentes, invalidando dados antigos sem snapshot automático nem alteração de parser/formatador.
- [x] 2.5 Testar ausência de GET duplicado de Carteiras entre shell e Dashboard, troca durante consultas, reload e navegação contextual.

## 3. Carteiras e posições

- [x] 3.1 Sincronizar POST/PATCH confirmados no contexto com os DTOs retornados, mantendo seleção de criação em dialog e seleção por abertura do detalhe direto.
- [x] 3.2 Sincronizar DELETE 204, remoção de preferência correspondente, fallback da ativa e vazio da última, preservando estado em cancelamento/409/erro.
- [x] 3.3 Tornar detalhe reativo a paramMap, validar DTO transitório e selecionar/persistir abertura válida; trocar no shell navega ao novo detalhe.
- [x] 3.4 Apresentar posições via serviço/contrato existente com campos autoritativos, estados independentes, moedas e formatação preservadas.
- [x] 3.5 Após operação contextual bem-sucedida, incorporar histórico conforme contrato vigente e recarregar posições da origem ainda ativa sem cálculos locais.
- [x] 3.6 Testar criação/edição/exclusão, detalhe com mudança de ID na mesma instância, 404, vazio/erro de posições e respostas tardias.

## 4. Operações e retorno

- [x] 4.1 Capturar Carteira fixa na abertura do formulário global/contextual, visível e não editável, e usar a captura na sugestão de VENDA e no POST.
- [x] 4.2 Transportar carteiraId e origem conhecida em URL e reconstruir retorno após reload, mantendo retorno global sem origem e rejeitando contexto inválido.
- [x] 4.3 Preservar origem nos links ao detalhe de Operação, usar DTO compatível ou GET existente e manter histórico global sem filtro implícito.
- [x] 4.4 Isolar troca global, mudança de rota e respostas pendentes da origem, fechando dialog da rota anterior sem reatribuir POST enviado.
- [x] 4.5 Testar abertura em A seguida de seleção B até o POST, reload global/contextual, cancelamento, retorno, referência indisponível e respostas tardias.
- [x] 4.6 Confirmar nos testes os payloads discriminados vigentes, prévia de COMPRA, sugestão editável de VENDA, Corretora opcional e bloqueio de double-submit, sem alterar cálculos ou parsing.

## 5. Validação da implementação futura

- [x] 5.1 Executar testes frontend focados e suíte existente, registrando resultados e regressões dos fluxos de contexto e contratos preservados.
- [x] 5.2 Executar build frontend e verificar manutenção dos limites lazy e budgets, sem nova dependência ou mudança de configuração para mascarar falhas.
- [x] 5.3 MANUAL: validar seletor global, teclado, foco, anúncios, drawer e responsividade em desktop/mobile e fronteiras 959/960/961px.
- [x] 5.4 MANUAL: validar URL versus preferência, refresh, voltar/avançar, storage bloqueado e criação/edição/exclusão com evidências por cenário.
- [x] 5.5 MANUAL: validar posições no detalhe, operação fixa e retorno em página/dialog, compatibilidade das três rotas e snapshot exclusivamente manual.
- [x] 5.6 Revisar diff de escopo, validar OpenSpec strict da change e global, git diff --check e git status --short; preservar change visual pausada, sem Graphify, commit, push, merge ou archive.

Total: 28 tasks; 28 concluídas e nenhuma pendente. Validações manuais 5.3–5.5 concluídas e aprovadas pelo usuário nesta conversa. Dependências: grupo 1 precede 2 e 3; contexto e detalhe precedem integração contextual do grupo 4; grupo 5 verifica os anteriores. Nenhuma dependência externa nova ou task para retomar a change visual.

Evidências automatizadas em 2026-09-06:
- Testes focados: contexto/coordenador 21 aprovados; shell/Dashboard 28; Carteiras/Dashboard/contexto 75; Operações e integração contextual/dialogs 65; rotas/shell/detalhe 23; contexto/seletor 23; rotas finais 5. Conjuntos sobrepostos, não somáveis.
- Suíte completa: 366 testes em 59 arquivos aprovados com `VITEST_MAX_WORKERS=2` somente no processo de execução e `npm test -- --watch=false`. Duas execuções anteriores com concorrência padrão tiveram 365 aprovados e timeout de 5s no teste existente de lazy loading de Ações; nenhum teste, timeout ou configuração foi alterado para contornar isso.
- Build production aprovado: initial 512,76 kB, transferência estimada 121,99 kB; +6,67 kB (aproximadamente 1,32%) sobre baseline 506,09 kB. Aviso do budget initial de 500 kB; limite de erro de 1 MB e budgets existentes preservados, assim como os limites lazy.
- OpenSpec strict da change aprovado; strict global: 33 itens aprovados, zero falhas. `git diff --check` sem erros de whitespace; Git informa conversão futura LF/CRLF em três arquivos frontend.
- Revisão Git somente leitura: alterações restritas ao frontend e tasks desta change, sem alterações staged; pasta da change já estava untracked no início. Backend, specs canônicas, change visual pausada, dependências, budgets e Graphify intactos. Sem commit, push, merge ou archive.

Evidência humana de conclusão das tasks 5.3–5.5: o usuário confirmou aprovação integral das três validações. Na 5.5, confirmou posições abertas no detalhe, vínculo da operação à carteira de origem, carteira contextual fixa/não editável, prevenção de alteração indevida do contexto, atualização do histórico e informações da origem após registro, cancelamento sem registro e adequação visual dos dialogs no desktop testado após padronização, incluindo Nova operação sem rolagem interna. Esta evidência foi fornecida pelo usuário, não inferida dos testes automatizados.

Revalidação final após aprovação humana e padronização dos dialogs: 28/28 tasks; suíte frontend completa com 366 testes em 59 arquivos aprovada usando `VITEST_MAX_WORKERS=2` somente no processo; build production aprovado com initial 515,58 kB e transferência estimada 122,33 kB. Mantido aviso de 500 kB, limite de erro de 1 MB e budgets de estilo de 4/8 kB sem alteração. OpenSpec strict da change aprovado e global com 33 itens aprovados; `git diff --check` sem erros. Nenhuma pendência identificada; change tecnicamente pronta para finalização, ainda sem archive, staging ou commit.
