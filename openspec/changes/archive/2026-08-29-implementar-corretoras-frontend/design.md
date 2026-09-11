## Context

Conforme `proposal.md`, a foundation fornece `HttpClient`, `API_BASE_URL=/api`, interceptor de normalização de `StandardError`, strict mode e testes Vitest. O application shell fornece Material 3, layout responsivo, acessibilidade e um limite lazy `corretoras/` ainda ocupado por placeholder. A capability backend `broker-registration` é a fonte normativa para os quatro endpoints e inclui o fluxo excepcional de confirmação de situação cadastral não ativa.

O PRD confirma cadastro por CNPJ, listagem e consulta por ID/CNPJ. Onde o PRD ainda menciona um path antigo de CNPJ, prevalecem OpenSpec/OpenAPI e o resource vigente: `GET /corretoras/por-cnpj?cnpj=...`.

## Goals / Non-Goals

**Goals:**

- Implementar a primeira feature funcional dentro de um limite lazy existente, sem estado global.
- Manter os DTOs manuais alinhados ao contrato backend e concentrar HTTP em um serviço stateless.
- Tornar listagem, busca, cadastro, confirmação e detalhe responsivos, acessíveis e testáveis.
- Preservar integralmente a autoridade do backend sobre validação e enriquecimento cadastral.

**Non-Goals:**

- Editar ou excluir Corretoras, paginar, ordenar ou filtrar genericamente a listagem.
- Chamar BrasilAPI/ViaCEP, replicar dígitos verificadores de CNPJ, inferir situação cadastral ou validar mercado financeiro.
- Criar autenticação, guards, NgRx, estado global, cliente OpenAPI, nova biblioteca, tabela avançada ou design system adicional.
- Implementar outras features ou alterar backend, proxy, API base, política npm ou shell fora da reconciliação normativa necessária.

## Decisions

### A. Arquitetura interna da feature

`features/corretoras/` conterá rotas, models contratuais, serviço HTTP e páginas de lista, cadastro e detalhe. Componentes menores só serão extraídos quando houver responsabilidade concreta, como confirmação ou apresentação reutilizada. Isso mantém coesão sem criar camadas genéricas prematuras.

### B. DTOs e tipos

Serão escritos manualmente `Corretora`, `CorretoraCreateRequest` e tipos auxiliares estritamente necessários. `Corretora` refletirá `id: number`, strings obrigatórias, campos opcionais como `string | null`, `validadaMercadoFinanceiro: boolean` e `dataCadastro: string` ISO-8601. O request permitirá o controle opcional somente internamente ao segundo envio; ele nunca será campo persistente do formulário.

### C. Serviço HTTP

Um `CorretorasService` stateless injetará `HttpClient` e `API_BASE_URL`, expondo operações tipadas para listar, criar, consultar por ID e consultar por CNPJ. Ele não armazenará estado de tela, não capturará erros globalmente e não conhecerá Material; os erros normalizados continuarão fluindo para a página solicitante.

### D. Estado local com signals

Cada página manterá signals locais e estados discriminados ou equivalentes para `idle/loading/success/empty/error`. Operações RxJS usarão cleanup vinculado ao ciclo de vida. NgRx, stores compartilhados e cache global foram rejeitados porque não há coordenação transversal nesta fatia.

### E/F. Páginas e navegação

O limite lazy existente passará a oferecer:

```text
/corretoras       -> listagem e busca exata por CNPJ
/corretoras/nova  -> cadastro
/corretoras/:id   -> detalhe
```

`nova` será declarada antes de `:id`. O cadastro concluído navegará ao detalhe reutilizando o `CorretoraResponse` completo devolvido pelo POST como estado transitório da navegação. A transição imediata não repetirá `GET /corretoras/{id}`; acesso direto, refresh ou navegação sem esse DTO carregarão o detalhe pelo GET. Esse reaproveitamento é local e efêmero, não constitui cache ou sincronização global. A lista será recarregada em um acesso posterior somente por seu fluxo normal explícito.

### G. Busca por CNPJ

A busca ficará na página de listagem como ação exata, submetida explicitamente pelo usuário e separada da coleção. Não haverá request automático durante digitação. A coleção completa já carregada permanecerá preservada no estado local: limpar ou cancelar a busca restaura imediatamente sua apresentação, sem novo `GET /corretoras`; somente recarregamento ou retry explícito consulta novamente a listagem. Um resultado navega ao detalhe pelo `id` devolvido; 404 produz mensagem contextual sem apagar a coleção. Não haverá filtro client-side, substituição permanente da coleção, query especulativa em `GET /corretoras` ou rota por path não aprovada.

### H. Componentes Material

O desenho poderá usar Card, List, Button, Form Field, Input, Progress Spinner, Progress Bar, Snack Bar, Dialog, Divider e componentes de semântica equivalente já disponíveis. `MatTable` não é necessária: uma lista/cards responsiva representa melhor o conjunto pequeno sem paginação e evita tabela horizontal em mobile. Nenhuma dependência será adicionada.

### I/J/K. Formulário, formatação e validação de CNPJ

O cadastro usará Reactive Forms com um único campo `cnpj`. Uma função local pura poderá remover caracteres de apresentação e formatar visualmente 14 dígitos como `NN.NNN.NNN/NNNN-NN`; nenhuma biblioteca de máscara será incluída. A validação frontend se limitará a obrigatoriedade, caracteres/formato e quantidade de dígitos para UX. Dígitos verificadores, existência, duplicidade e dados externos permanecem no backend.

### L. Apresentação dos dados

Lista e detalhe formatarão CNPJ/CEP apenas visualmente. O detalhe agrupará identificação, contato, endereço e status. Valores nulos usarão texto neutro como “Não informado”. `dataCadastro` será formatada para leitura pelo locale sem alterar o valor contratual. Situação cadastral usará texto/badge neutro, sem inferência. `false` em validação financeira será exibido como “Validação ainda não realizada”.

### M/N. StandardError e estados

As páginas consumirão `NormalizedHttpError` já produzido pelo interceptor. Mensagens contextuais poderão orientar cadastro duplicado, CNPJ inválido ou registro não encontrado usando `status/code/message`, sem apagar `details` nem reconstruir regras. Loading, empty, success e error serão mutuamente claros; retries existirão para consultas recuperáveis.

### O/P. Feedback e destino após cadastro

Sucesso será anunciado por feedback Material não bloqueante e seguido de navegação para `/corretoras/{id}`. Como o POST devolve o `CorretoraResponse` completo persistido, a transição imediata reutilizará esse DTO e não fará um GET redundante. A página de detalhe continuará consultando `GET /corretoras/{id}` quando for acessada diretamente, recarregada ou aberta sem o estado transitório. Permanecer no formulário, voltar silenciosamente à lista e introduzir cache/store global foram rejeitados.

### Fluxo excepcional aprovado de confirmação

A primeira submissão envia `{ cnpj }`. Somente `status=409` e `code=SITUACAO_CADASTRAL_NAO_ATIVA` abrem uma confirmação acessível, apresentando `details.situacaoCadastral` quando for string e a mensagem do backend. O frontend não compara a situação com `ATIVA` nem decide sua validade. Confirmar produz uma única nova submissão `{ cnpj, confirmarSituacaoCadastralNaoAtiva: true }`; cancelar ou fechar não produz request. Outros 409 seguem erro normal. O controle técnico não integra o form e é descartado ao terminar o fluxo.

### Q/R. Estratégia de testes

Testes HTTP verificarão URL, método, query parameter, tipos e os dois payloads exatos do POST. Testes de componentes/rotas cobrirão estados da lista, busca explícita sem request durante digitação, limpeza sem recarregar a coleção, formulário, navegação, reaproveitamento transitório do DTO criado sem GET redundante, carregamento direto do detalhe por GET, nulos, data e semântica financeira. O fluxo 409 cobrirá confirmação, cancelamento, ausência de retry automático e tratamento normal de outros conflitos. Serão preferidos RouterTestingHarness, HttpTestingController e Material harnesses apenas onde reduzirem acoplamento.

### S/T. Responsividade e acessibilidade

A lista usará cards/linhas fluidas e o detalhe uma grade que colapsa para coluna única em viewport compacto. Formulários e ações ocuparão largura adequada sem scroll horizontal obrigatório. Cada página terá `h1`; inputs terão labels/descrições; erros e estados dinâmicos usarão regiões vivas adequadas; diálogo terá foco inicial, contenção e retorno de foco providos pelo Material.

### Reconciliação com o shell

O requirement atual do shell limita todas as áreas a placeholders. A delta `MODIFIED` esclarece que o placeholder permanece até existir capability funcional aprovada; esta change substitui somente Corretoras e preserva os outros quatro destinos, o shell e o lazy boundary.

## Risks / Trade-offs

- [Contrato manual divergir do backend] → Confrontar models e testes HTTP com `broker-registration`, resource e OpenAPI vigentes antes de concluir.
- [Fluxo 409 abrir confirmação indevida] → Exigir simultaneamente status e code exatos; testar outros conflitos e cancelamento sem request.
- [Dados externos demorarem no POST] → Comunicar processamento, bloquear submissão concorrente e preservar possibilidade de nova tentativa após falha.
- [Estado assíncrono obsoleto após navegação] → Vincular subscriptions ao ciclo de vida e centralizar transições locais.
- [Lista crescer sem paginação] → Manter o contrato atual sem paginação; evolução exige endpoint e change próprios.
- [Formatação visual alterar valor] → Separar função de display do valor enviado e cobrir ambos por teste.
- [Mensagens do backend exporem detalhe inadequado] → Usar somente o contrato público normalizado; nunca exibir stack trace ou corpo desconhecido.

## Migration Plan

1. Confirmar contratos backend/OpenSpec e baseline frontend sem alterar dependências.
2. Criar models e serviço HTTP com testes contratuais.
3. Evoluir as rotas lazy de Corretoras e implementar lista/busca, cadastro/confirmação e detalhe.
4. Aplicar estados Material, responsividade e acessibilidade.
5. Executar testes, build, budgets, OpenSpec strict, revisão de escopo e atualização Graphify.

Rollback: restaurar o placeholder e as rotas estruturais de Corretoras e remover somente os artefatos funcionais desta feature. Não há migração de dados, dependência nova ou alteração backend.
