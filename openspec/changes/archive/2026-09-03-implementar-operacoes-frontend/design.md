## Context

Ver `proposal.md`. A feature parcial já possui rotas lazy, listagem, detalhe, formulário reutilizado como página/dialog, serviço HTTP, parser lossless e integração com Carteira. O POST discriminado permanece inalterado. O commit backend `de4a849` adiciona uma prévia informativa do fechamento de COMPRA e uma sugestão não vinculante para VENDA, exigindo novo estado consultivo no mesmo formulário.

O response continua contendo `precoUnitario`, `ordemNoDia` e `valorTotal`. Quantidade e preço aceitam no máximo 13 dígitos inteiros e 6 fracionários, total pode exigir precisão `(38,12)`, quantidade BRASIL é inteira, quantidade EUA admite até seis casas e `dataOperacao` continua sendo `YYYY-MM-DD` sem hora.

## Goals / Non-Goals

**Goals:**

- alinhar tipos, formulário, validação, payloads, mensagens e testes ao contrato discriminado;
- preservar precisão decimal lossless nas respostas e texto decimal na entrada de VENDA;
- manter fluxos global e contextual sem duplicação;
- preservar double-submit lock apenas durante o POST pendente.
- tornar visível o preço de COMPRA antes do registro e preencher inicialmente o preço de VENDA sem transferir autoridade financeira ao frontend;
- apresentar quantidade × preço como estimativa visual lossless no formulário, sem integrar o request ou substituir o total calculado pelo backend;
- impedir preço obsoleto ou resposta fora de ordem em toda mudança de contexto.

**Non-Goals:**

- alterar backend, providers, dependências, specs promovidas ou regras financeiras;
- buscar ou calcular fechamento histórico no frontend;
- oferecer preço manual de fallback para COMPRA;
- introduzir ordem/hora manual, idempotency key ou detecção de duplicidade por payload;
- calcular preço médio, posição, resultados ou patrimônio no frontend, ou tratar a estimativa visual de quantidade × preço como `valorTotal` autoritativo;
- editar/excluir Operações ou realizar redesign amplo.

## Decisions

### D1 — União discriminada no limite HTTP

Definir campos comuns e dois requests: `OperacaoCompraCreateRequest` com `tipo: 'COMPRA'` e sem `precoUnitario`; `OperacaoVendaCreateRequest` com `tipo: 'VENDA'` e `precoUnitario: string` obrigatório. `OperacaoCreateRequest` será a união desses tipos. Nenhum request possuirá `ordemNoDia`. `OperacaoResponse` será independente e continuará contendo preço, ordem e total.

Alternativa rejeitada: campos opcionais em um único tipo permitiriam serializar `precoUnitario: null` em COMPRA ou omitir preço em VENDA.

### D2 — Um campo visual com semântica discriminada

O formulário reutilizado continuará único e exibirá sempre um único `MatFormField` de preço quando o tipo estiver definido. Em COMPRA, o controle será `readonly`, sem validators de preço, receberá somente o valor da prévia e nunca participará da construção do request. `readonly` foi escolhido em vez de `disabled` para manter valor, foco e leitura assistiva previsvisíveis sem depender de `getRawValue`; o payload continuará explícito. Em VENDA, o mesmo controle ficará editável, obrigatório, positivo e limitado a 13 dígitos inteiros e 6 fracionários.

Trocas de tipo sempre limpam o valor antes da nova consulta. COMPRA nunca reutiliza valor digitado/sugerido da VENDA; VENDA nunca reutiliza a prévia da COMPRA. Ordem permanece integralmente ausente.

### D3 — Payload construído por discriminante

O submit construirá explicitamente um dos dois objetos, em vez de espalhar o valor bruto do FormGroup. COMPRA serializará somente campos comuns; VENDA acrescentará preço normalizado. Isso garante ausência de `precoUnitario`, inclusive `null`, e ausência de `ordemNoDia` em COMPRA, além de ausência de ordem em VENDA.

O service aceitará a união discriminada e enviará o objeto recebido sem retry automático ou lógica de provider.

### D4 — Validação estrutural e precisão

Quantidade continuará textual, positiva, com no máximo 13 dígitos inteiros e 6 fracionários; BRASIL continuará matematicamente inteiro e EUA poderá ser fracionário. A data continuará validada e enviada como texto `YYYY-MM-DD`, sem `Date`, hora ou ajuste de pregão. Preço só será validado em VENDA com os mesmos limites. O validador de inteiro positivo dedicado à ordem ficará obsoleto e deverá ser removido se não tiver outro consumidor.

### D5 — Responses consultivos e parser lossless

Adicionar `PreviaPrecoCompraResponse` com ticker, mercado, moeda, data e preço textual, e `SugestaoPrecoVendaResponse` com preço textual anulável. Os dois GETs serão lidos como texto pelo mesmo limite lossless, ampliado para `precoUnitarioSugerido`; nenhum decimal financeiro será convertido para `number`. O parser permanece somente de leitura e não influencia a serialização do POST.

### D6 — Pipeline consultivo cancelável e estados explícitos

Combinar tipo, Carteira efetiva, Ação canônica e `dataOperacao` em um pipeline RxJS. Cada mudança invalida o preço imediatamente; `switchMap` cancela logicamente a consulta anterior e impede resposta atrasada de preencher o novo contexto. Valores incompletos retornam estado ocioso sem HTTP. COMPRA mantém estados `idle/loading/ready/error`; VENDA diferencia loading, sugestão presente e ausência normal. O campo de VENDA permanece editável durante a consulta; se o usuário digitar antes da resposta, uma marca de edição manual impede que a sugestão tardia sobrescreva sua escolha.

Não haverá debounce obrigatório: selects e data já produzem mudanças discretas. `distinctUntilChanged` por chave canônica evita GET idêntico acidental sem impedir nova consulta após erro por uma ação explícita de retry, se exposta.

### D7 — Erros históricos sem fallback

O erro normalizado será preservado. A prévia mapeará `COTACAO_HISTORICA_INDISPONIVEL`, `HISTORICO_COTACAO_FORA_DO_ALCANCE`, `TICKER_INEXISTENTE` e `LIMITE_REQUISICOES_EXCEDIDO`; `502`, `503` e `504` permanecerão no tratamento técnico central. O preço ficará inválido e o submit de COMPRA bloqueado, sem fallback. Pelo menos um teste com `HttpTestingController` e interceptor real provará `HttpErrorResponse → normalização → service → mensagem`, evitando mocks já normalizados como única evidência.

### D8 — Semântica de submissão

O signal de submissão continuará bloqueando novo clique enquanto o POST estiver pendente. Além disso, COMPRA somente poderá ser submetida no estado `ready` da prévia correspondente à chave atual. O preço visível nunca será copiado ao request. Concluído o request, um novo envio deliberado continuará permitido.

### D9 — Listagem, detalhe e contexto de Carteira

Listagem e histórico contextual continuarão usando preço, ordem e total retornados, sem recalculá-los. O detalhe exibirá preço e total autoritativos formatados, mantendo `ordemNoDia` no response e na ordenação, mas sem expô-la ao usuário. O dialog contextual reutilizará o mesmo formulário discriminado com `carteiraId` pré-selecionado e fixo. Após sucesso, o DTO retornado será inserido no histórico e ordenado para apresentação por `dataOperacao`, `ordemNoDia`, `id`.

O formulário poderá apresentar um total estimado como multiplicação decimal textual de quantidade por preço. Essa informação será identificada como estimativa, não será enviada no POST e não participará de preço médio, posição, resultados ou patrimônio.

### D10 — Estratégia de testes

Atualizar testes de models, lossless, service e formulário para os dois GETs. Cobrir campo único, moedas, total estimado apenas visual, loading, erro, null, reconsulta, descarte imediato, respostas fora de ordem, alternância, payloads exatos, double-submit e fluxo contextual. Manter os testes compatíveis de validators, rotas, lista, detalhe e shell.

## Risks / Trade-offs

- [Valor residual de VENDA vaza para COMPRA] → construir payload por discriminante e testar a alternância nos dois sentidos.
- [Tipos permissivos mascaram contrato inválido] → usar união discriminada sem preço no membro COMPRA e com preço obrigatório no membro VENDA.
- [Parser lossless amplia o adaptador HTTP] → mantê-lo restrito aos campos decimais de responses de Operações e coberto por testes.
- [Mensagens locais divergem do backend] → mapear por código, acrescentando orientação curta sem substituir `message`/`details`.
- [Cadastro contextual diverge do global] → manter um único componente e um único construtor de payload.
- [Resposta antiga sobrescreve preço novo] → invalidar antes do GET e usar `switchMap` com chave completa do contexto.
- [Campo readonly vaza para o POST] → nunca serializar FormGroup; manter construtores discriminados explícitos e testes do request HTTP real.

## Migration Plan

1. Adicionar DTOs e GETs consultivos lossless ao service.
2. Introduzir pipeline cancelável e estados de prévia/sugestão no formulário único.
3. Tornar o campo readonly em COMPRA e editável em VENDA, mantendo payloads discriminados explícitos.
4. Integrar erros, ausência normal e bloqueio de submit da COMPRA.
5. Atualizar testes globais/contextuais, incluindo interceptor real e races.
6. Reexecutar validações automatizadas e a bateria manual reconciliada.

Rollback: restaurar somente os ajustes frontend desta reconciliação; nenhum dado, backend, migration ou dependência é alterado.
