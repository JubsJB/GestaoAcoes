## Why

O frontend já oferece fundação, shell, identidade visual e gerenciamento de Corretoras e Ações, mas o destino Carteiras permanece como placeholder. Esta change disponibiliza o gerenciamento básico de Carteiras sobre os contratos REST existentes, formando a base de navegação para futuras capabilities financeiras sem antecipá-las.

## What Changes

- Substituir o placeholder lazy de `/carteiras` por listagem responsiva com loading, estado vazio, erro recuperável, nome, data de criação e acesso ao detalhe.
- Criar Carteiras com formulário tipado contendo somente `nome`, preferencialmente em dialog a partir da listagem e com rota direta `/carteiras/nova` preservada.
- Exibir o detalhe básico em `/carteiras/{id}`, com `StickyBack` e ações explícitas para editar e excluir, sem seções financeiras funcionais.
- Editar somente o nome via `PATCH /carteiras/{id}`, preferencialmente em dialog no detalhe e com rota direta `/carteiras/{id}/editar` para acesso e refresh.
- Excluir via `DELETE /carteiras/{id}` somente após confirmação acessível, preservando conflitos e erros do backend e retornando à listagem após sucesso.
- Reutilizar `PageHeader`, `FeedbackAlert`, `SuccessToast`, `StickyBack`, dialogs Material, formatter temporal, tokens Financial Olive e os padrões atuais de estados e acessibilidade.
- Atualizar estado local com os DTOs retornados por POST/PATCH sem GET redundante; o carregamento da listagem após exclusão permanece o carregamento normal do destino.
- Ajustar globalmente o descarte automático do `SuccessToast` compartilhado de `10000 ms` para `8000 ms`, preservando posição superior, fechamento manual, acessibilidade, responsividade e aparência.
- Manter fora do escopo Operações, posições, resultados, patrimônio, resumo, snapshots, evolução, gráficos, moedas e qualquer cálculo financeiro.

## Capabilities

### New Capabilities

- `frontend-portfolio-management`: gerenciamento frontend de listagem, cadastro, detalhe, edição e exclusão de Carteiras sobre os endpoints existentes.

### Modified Capabilities

- `frontend-application-shell`: substituir somente o placeholder de Carteiras por sua capability funcional, preservando shell, navegação e limite lazy.
- `frontend-visual-experience`: reduzir somente a duração global do `SuccessToast` de `10000 ms` para `8000 ms`.

## Impact

- Afeta `frontend/src/app/features/carteiras`, seus testes e somente a constante/teste compartilhados do `SuccessToast` necessários ao ajuste global aprovado.
- Reutiliza infraestrutura compartilhada existente sem alterar seus contratos, salvo ajuste compatível caso um ícone local de Carteira ainda não esteja disponível.
- Consome sem alteração `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}`, `PATCH /carteiras/{id}` e `DELETE /carteiras/{id}`.
- Não altera backend, OpenAPI, DTOs backend, dependências, budgets, autenticação ou configuração da aplicação.
