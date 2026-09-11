## 1. Contratos e service HTTP

- [x] 1.1 Criar interfaces tipadas para `CarteiraResponse`, cadastro e edição contendo somente os campos aprovados.
- [x] 1.2 Criar service de Carteiras sobre `API_BASE_URL` com POST, listagem, consulta individual, PATCH e DELETE.
- [x] 1.3 Testar métodos, URLs, encoding do ID, payloads exatos e resposta sem corpo do DELETE.

## 2. Routing lazy

- [x] 2.1 Substituir o placeholder por rotas lazy de listagem, cadastro, detalhe e edição, com precedência das rotas estáticas.
- [x] 2.2 Testar acesso direto e refresh de `/carteiras`, `/carteiras/nova`, `/carteiras/:id` e `/carteiras/:id/editar`.
- [x] 2.3 Confirmar que Dashboard e Operações permanecem placeholders e que o shell não recebe lógica de Carteiras.

## 3. Listagem

- [x] 3.1 Implementar a página de listagem com `PageHeader`, CTA, GET único e cards com nome e data de criação.
- [x] 3.2 Implementar loading anunciado, estado vazio com CTA, erro por `FeedbackAlert` e retry manual.
- [x] 3.3 Implementar navegação ao detalhe com DTO transitório compatível e nome acessível por Carteira.
- [x] 3.4 Testar coleção, ordem recebida, estados, retry e abertura do detalhe sem request indevido.

## 4. Cadastro

- [x] 4.1 Implementar formulário tipado reutilizável em dialog e página com somente `nome`.
- [x] 4.2 Aplicar validação estrutural de obrigatoriedade, branco e 255 caracteres sem rejeitar duplicidade.
- [x] 4.3 Integrar cadastro contextual à listagem e incorporar o DTO do POST sem GET redundante.
- [x] 4.4 Preservar a rota direta, navegação ao detalhe com DTO retornado, cancelamento sem HTTP e bloqueio concorrente.
- [x] 4.5 Testar dialog e rota direta, payload exato, cancelamento, sucesso, erro e ausência de POST automático.

## 5. Detalhe

- [x] 5.1 Implementar detalhe básico com `StickyBack`, nome, ID, data de criação e ações Editar/Excluir.
- [x] 5.2 Validar DTO transitório pelo ID e executar GET somente em acesso direto, refresh ou estado incompatível.
- [x] 5.3 Implementar loading, 404 dedicado, erro recuperável e retry sem qualquer seção financeira simulada.
- [x] 5.4 Testar DTO transitório, GET necessário, 404, retry, data formatada e ausência de conteúdo financeiro.

## 6. Edição

- [x] 6.1 Reutilizar o formulário tipado preenchido em dialog no detalhe e em `/carteiras/:id/editar`.
- [x] 6.2 Enviar PATCH único com somente `nome` e preservar estado anterior durante erro ou cancelamento.
- [x] 6.3 Atualizar o detalhe com o DTO do PATCH sem GET redundante e apresentar toast de sucesso.
- [x] 6.4 Testar prefill, validação, cancelamento, rota direta, payload, concorrência, sucesso e `StandardError`.

## 7. Exclusão

- [x] 7.1 Criar dialog acessível de confirmação identificando a Carteira e diferenciando Cancelar de Excluir.
- [x] 7.2 Garantir zero DELETE em abertura, Escape, backdrop ou cancelamento e um único DELETE após confirmação.
- [x] 7.3 Em `204`, navegar à listagem e apresentar sucesso; em erro, preservar detalhe e `StandardError` sem inferir elegibilidade.
- [x] 7.4 Testar confirmação real, cancelamentos, conflito por dependências, falha genérica e fluxo pós-exclusão.

## 8. Visual, responsividade e acessibilidade

- [x] 8.1 Reutilizar tokens Financial Olive, padrões de superfície, ícone local e formatter temporal sem nova biblioteca.
- [x] 8.2 Ajustar cards, formulários e dialogs para desktop/mobile, nomes longos e ausência de overflow horizontal.
- [x] 8.3 Verificar h1 único, labels, foco, teclado, focus trap/restore, `aria-live`, busy/disabled e ações não dependentes somente de cor.

## 9. Validação automatizada

- [x] 9.1 Executar testes focados de models/service, rotas, listagem, formulários, detalhe e dialogs, registrando resultados.
- [x] 9.2 Executar integrações com dialogs reais para POST/PATCH/DELETE, `afterClosed`, cancelamento e atualização local.
- [x] 9.3 Executar a suíte frontend completa e confirmar ausência de regressão em Corretoras, Ações e shell.
- [x] 9.4 Executar build de produção, registrar initial/lazy bundles e confirmar budgets e dependências intactos.

## 10. Especificação e escopo

- [x] 10.1 Validar a change e o conjunto global com OpenSpec strict.
- [x] 10.2 Auditar diff para backend, contratos, dependências, budgets, auth, estado global e funcionalidades financeiras fora do escopo.
- [x] 10.3 Executar `git diff --check` e procurar secrets, TODO/FIXME, debug, temporários e outputs de build.
- [x] 10.4 Atualizar o Graphify pelo workflow normal após a implementação e registrar eventual risco operacional conhecido.

## 11. Revisão humana

- [x] 11.1 Inspecionar listagem, cadastro, detalhe, edição, confirmação e feedbacks em desktop, tablet e mobile.
- [x] 11.2 Confirmar foco, teclado, contraste, nomes longos, loading, empty, 404, erros, sucesso e rotas diretas.
- [x] 11.3 Confirmar visualmente que nenhuma informação financeira, Operação ou funcionalidade futura foi antecipada.

## 12. Ajuste global do SuccessToast

- [x] 12.1 Sincronizar o delta de `frontend-visual-experience` para duração global de `8000 ms`, preservando as demais regras do toast.
- [x] 12.2 Alterar somente a constante compartilhada de duração e atualizar os testes diretamente afetados.
- [x] 12.3 Executar testes focados, OpenSpec strict da change e global, `git diff --check` e auditoria de integridade.
