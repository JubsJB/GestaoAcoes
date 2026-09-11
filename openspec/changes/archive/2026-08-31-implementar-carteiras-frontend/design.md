## Context

O destino lazy de Carteiras existe como placeholder. Corretoras e Ações consolidaram uma arquitetura standalone com models locais, service HttpClient, páginas lazy, Typed Reactive Forms, dialogs contextuais, `NormalizedHttpError` e componentes visuais compartilhados. O backend já expõe CRUD básico de Carteiras e devolve `CarteiraResponse(id, nome, dataCriacao)`; ver `proposal.md` para motivação.

## Goals / Non-Goals

**Goals:**

- Implementar CRUD frontend básico no limite lazy existente.
- Reutilizar a linguagem de interação e feedback já aprovada.
- Evitar requests redundantes usando DTO transitório e respostas de POST/PATCH.
- Preservar acesso direto e refresh de cadastro, detalhe e edição.
- Manter contratos HTTP e `StandardError` como autoridades.

**Non-Goals:**

- Consumir endpoints de Operações, posição, resultados, resumo, valuation, snapshots ou evolução.
- Criar store global, cache persistente, paginação, filtros ou ordenação configurável.
- Alterar backend, dependências, budgets ou componentes compartilhados sem necessidade estrita.
- Alterar qualquer aspecto visual ou comportamental compartilhado além da duração aprovada do `SuccessToast`.

## Decisions

### D1. Feature local e contratos tipados

Criar model com `CarteiraResponse`, `CarteiraCreateRequest` e `CarteiraUpdateRequest`, além de service local com os cinco métodos REST. Isso segue Corretoras/Ações e mantém chamadas fora dos componentes. Não será criado client gerado ou camada de estado global.

### D2. Rotas diretas e formulário reutilizado

Preservar `/carteiras` e adicionar `nova`, `:id` e `:id/editar`, ordenando rotas estáticas antes de `:id`. Cadastro e edição reutilizam o mesmo componente de formulário em dialog ou página, como o cadastro atual de Corretoras/Ações. A alternativa de dialogs exclusivos reduziria arquivos, mas perderia deep link e refresh útil.

### D3. Cadastro em dialog a partir da lista

O CTA da listagem abre formulário vazio em dialog. Em sucesso, o DTO do POST é anexado à coleção na ordem devolvida/esperada por ID, sem novo GET, e o toast compartilhado comunica sucesso. A rota direta navega ao detalhe usando o DTO retornado.

### D4. Edição em dialog a partir do detalhe

Editar abre dialog preenchido com o DTO atual. A resposta do PATCH substitui atomicamente o estado local do detalhe. A rota direta carrega o DTO quando necessário e retorna ao detalhe com estado transitório. Não haverá edição inline, pois mistura leitura e mutação e cria uma terceira abordagem.

### D5. Exclusão conservadora

Excluir abre dialog dedicado e só chama DELETE após confirmação real. Não se aplica remoção otimista antes do `204`, porque conflitos `CARTEIRA_POSSUI_OPERACOES` e `CARTEIRA_POSSUI_SNAPSHOTS` são decisões do backend. Após sucesso no detalhe, navegar para a lista produz seu carregamento normal e garante visão autoritativa; isso não é um GET individual redundante.

### D6. Estado transitório validado

Detalhe e edição aceitam DTO transitório somente quando a estrutura é compatível e o ID coincide com a rota. Acesso direto, refresh ou estado inválido executa GET individual. Nenhum estado é persistido em query string ou storage.

### D7. Formatação e validação

`dataCriacao` usa o formatter `OffsetDateTime` compartilhado somente na apresentação. O nome usa `required`, rejeição de branco e `maxLength(255)` para UX; trim pode ser aplicado antes do envio, preservando espaços internos, acentos e caixa. O backend continua autoridade e nomes duplicados são aceitos.

### D8. Composição visual sem falsa antecipação

Listagem e detalhe usam `PageHeader`, cards/superfícies, `FeedbackAlert`, `SuccessToast`, `StickyBack`, sprite local e tokens existentes. O detalhe pode manter espaçamento extensível, mas não exibe cards vazios intitulados patrimônio, posições ou resultados, pois sugeririam funcionalidade inexistente.

### D9. Testes por risco

Testes de service fixam método, URL e payload. Testes de rotas protegem precedência. Testes de página cobrem loading/empty/error/404, DTO transitório, ausência de GET redundante e updates locais. Integrações com dialog real cobrem cancelar, confirmar, POST/PATCH/DELETE único, foco e resultado de `afterClosed`.

### D10. Duração global do SuccessToast

Reduzir a constante compartilhada de descarte automático de `10000 ms` para `8000 ms`, sem configuração específica de Carteiras. A posição superior responsiva, o fechamento manual, a semântica acessível e a aparência Financial Olive permanecem inalterados para Corretoras, Ações e Carteiras.

## Risks / Trade-offs

- [Componente de formulário em dois contextos aumenta ramificações] → manter apenas diferenças de fechamento/navegação e cobrir dialog e rota direta.
- [Estado transitório incompatível pode mostrar Carteira errada] → validar estrutura e igualdade do ID antes de evitar GET.
- [Exclusão pode falhar por dependências futuras] → nunca inferir elegibilidade; preservar `StandardError` e estado atual.
- [Lista após DELETE requer nova leitura ao navegar] → aceitar o GET normal da rota como fonte autoritativa, sem adicionar store global.
- [Nomes longos degradam cards/dialogs] → usar wrap, largura fluida e testes estruturais em viewport compacto.

## Migration Plan

1. Substituir somente o placeholder e ampliar as rotas lazy de Carteiras.
2. Introduzir model/service e fluxos por fatias protegidas por testes.
3. Validar testes focados, suíte completa, build, budgets, OpenSpec strict e inspeção manual.
4. Rollback consiste em restaurar o placeholder e remover somente os arquivos da feature; não há migration, dado ou contrato backend novo.
