## Context

O shell Angular já fornece `/acoes` como limite lazy, mas seu conteúdo ainda é um placeholder. Corretoras estabeleceu padrões locais de services stateless, signals por página, estados assíncronos, cards responsivos, navegação com DTO transitório e testes com `HttpTestingController`/router. A nova feature deve reutilizar apenas os padrões gerais e consumir os contratos reais de Ações; BRAPI, Alpha Vantage, normalização canônica, moeda e precisão permanecem no backend.

Não haverá dependência nova, alteração de backend, store global ou atualização do Graphify. O WinError 5 conhecido permanece risco operacional não bloqueante.

## Goals / Non-Goals

**Goals:**

- Evoluir o limite lazy de Ações para listagem, cadastro e detalhe standalone.
- Representar manualmente os contratos HTTP e DTOs aprovados com tipagem estrita.
- Oferecer busca exata e atualização manual sem chamadas redundantes ou automáticas.
- Manter estados locais previsíveis, responsividade, acessibilidade e erros contextualizados.

**Non-Goals:**

- Editar ou excluir Ações, paginar, expor histórico, gráficos ou atualização periódica.
- Implementar operações, posições, carteira, patrimônio ou cálculos financeiros.
- Consultar providers no navegador, converter moedas ou decidir ticker canônico.
- Criar store/cache global, adicionar dependências ou gerar cliente OpenAPI.

## Decisions

### D1. Atualização manual somente no detalhe

O botão de atualização ficará somente em `/acoes/:id`. Isso concentra cotação, referência temporal, loading e erros do provider no contexto do ativo e reduz acionamentos acidentais/rate limit. Colocar a ação em cards da lista foi rejeitado por ampliar concorrência visual, estados independentes e custo de teste.

### D2. Busca explícita por ticker e mercado

A listagem terá ticker e `MatSelect` de mercado, ambos obrigatórios, e só consultará no submit. A coleção completa carregada permanecerá separada do estado da busca; limpar apenas restaura essa coleção, sem GET. Busca por tecla foi rejeitada por chamadas desnecessárias e porque o contrato é singular/exato.

### D3. Mercado selecionado com MatSelect

O formulário e a busca usarão os enums `BRASIL` e `EUA`, exibindo Brasil e EUA. `MatSelect` mantém a interface compacta e consistente. Radio buttons continuam tecnicamente possíveis, mas não oferecem vantagem suficiente nesta feature.

### D4. Cadastro navega ao detalhe com DTO transitório

Após 201, a aplicação exibirá snackbar, navegará para `/acoes/{id}` e levará o `AcaoResponse` em `NavigationExtras.info`. O detalhe o aceitará somente se o ID coincidir; acesso direto, refresh ou estado incompatível fará GET por ID. Isso elimina GET imediato redundante sem criar cache/store.

### D5. Listagem por cards fluidos

Cards apresentarão ticker, empresa, mercado, última cotação e atualização. Grid fluido no desktop e coluna no mobile evitam uma tabela rígida e mantêm ações acessíveis. Valores exatos de largura permanecem orientação de estilo, não contrato.

### D6. Busca encontrada navega ao detalhe

O response singular será usado como estado transitório para navegar ao detalhe. Não haverá modo permanente de “lista filtrada por um item”, simplificando estados e deixando claro que a busca é exata.

### D7. PATCH reaproveita a resposta completa

O service fará PATCH sem objeto funcional de negócio; se a assinatura Angular exigir body, será usado `null`. No sucesso, o detalhe substituirá seu signal local pelo DTO retornado e exibirá snackbar, sem GET adicional. O botão ficará bloqueado durante a operação.

### D8. Sem sincronização global da listagem

A listagem reflete seu último GET. Cadastro, busca e PATCH não criarão cache compartilhado; uma nova listagem ocorrerá ao reentrar na página ou por retry/recarregamento explicitamente implementado. Esse trade-off evita estado global prematuro.

### D9. Erros contextuais por status e code

O interceptor existente continuará responsável pela normalização. As páginas reconhecerão códigos relevantes somente para contextualizar cadastro, busca e PATCH, sempre preservando `message` e `details`. Não haverá retry automático. `404` sem code em buscas locais/detalhe será diferenciado de `TICKER_INEXISTENTE` oriundo de provider.

### D10. Apresentação amigável sem alterar DTO

DTOs mantêm `BRASIL|EUA`, `BRL|USD`, cotação numérica e data ISO string. Funções puras formatarão mercado e apresentação; `Intl.NumberFormat`/pipe poderá formatar a cotação pela moeda recebida e a data será convertida apenas na renderização. Não haverá conversão cambial, soma monetária ou biblioteca decimal.

### Organização e estado

`features/acoes/` conterá models, service, formatadores, rotas e páginas. Cada página standalone manterá signals locais claros para data/loading/error e usará cleanup idiomático (`takeUntilDestroyed` quando necessário). O service apenas compõe URLs a partir de `API_BASE_URL` e retorna Observables tipados.

### Fluxos de erro da atualização

O detalhe mantém o DTO atual até um PATCH bem-sucedido. `TICKER_CANONICO_DIVERGENTE`, rate limit, indisponibilidade, timeout e falhas de cotação preservam cotação/data anteriores e liberam o controle ao terminar. `details` como `cotacaoPreservada`, `ultimaCotacaoValida` e `dataHoraUltimaCotacao` poderão complementar a explicação, sem substituir o DTO nem disparar nova chamada.

### Acessibilidade e responsividade

Cada página terá `h1`; formulários terão labels e erros associados. Loadings e feedbacks assíncronos usarão regiões de status adequadas sem excesso de anúncios. A atualização comunicará estado ocupado e ficará desabilitada. Cards e ações usarão layout fluido; testes automatizarão estrutura/semântica verificável e a inspeção visual real cobrirá adaptação fina de viewport e foco visual.

### Estratégia de testes

- `HttpTestingController` verificará os cinco contratos, inclusive query params, payload mínimo e ausência de dados de cotação no PATCH.
- Testes de rotas verificarão precedência, lazy boundary e remoção do placeholder.
- Testes de páginas cobrirão estados, retry real, busca explícita/limpeza, DTO transitório, ausência de GET redundante, cadastro, detalhe e PATCH.
- Casos de provider poderão usar matriz parametrizada por status/code quando preservarem asserts semânticos.
- Testes evitarão classes internas do Material e pixels exatos; harnesses serão usados somente onde reduzirem fragilidade.

## Risks / Trade-offs

- **Rate limit da Alpha Vantage** → atualização somente manual no detalhe, proteção contra concorrência e zero retry automático.
- **BigDecimal chega como número JSON** → usar apenas para apresentação; nenhum cálculo ou conversão no frontend.
- **Timestamp pode variar na origem** → conservar string no DTO e formatar somente na camada visual.
- **Estado transitório se perde em refresh** → detalhe sempre possui fallback por GET do ID.
- **Erro do provider pode conter contexto útil** → preservar `message/details` e o DTO previamente exibido.
- **Graphify desatualizado** → revisar arquitetura diretamente pelo código e não alterar artefatos/permissões nesta change.

## Migration Plan

1. Substituir somente o placeholder de Ações mantendo seu arquivo de rotas lazy.
2. Adicionar models, service, formatadores, páginas e testes sem dependências novas.
3. Validar contratos HTTP, rotas, acessibilidade, testes, build, budgets e OpenSpec strict.
4. Em rollback, restaurar o placeholder e remover somente os artefatos funcionais da feature, sem alteração backend ou de dados.
