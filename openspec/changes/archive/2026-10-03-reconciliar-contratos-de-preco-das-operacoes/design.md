## Context

Em agosto, ambas as operações usavam preço informado. Em 02/09, COMPRA passou a obter fechamento no POST, e ordem passou a ser gerada pelo backend. A prévia posterior inicialmente acompanhava essa regra. Em 10/09, a decisão aprovada restaurou preço informado em COMPRA, preservou a ordem backend e tornou a prévia sugestão editável. A reconciliação incompleta deixou cláusulas antigas; não há nova decisão financeira nesta change.

## Goals / Non-Goals

Alinhar documentação à regra atual sem implementar mudança funcional. Não modificar Purposes, arquivos consolidados ou código/testes nesta etapa de planejamento. Os não objetivos são os enumerados na proposal, especialmente erros BRAPI, política de cotação e fórmulas.

## Decisions

### 1. Quatro conceitos separados

- Preço negociado: valor final enviado em `precoUnitario`, obrigatório, positivo e na precisão vigente; pode ser uma sugestão mantida pelo usuário. Backend valida, persiste e calcula o total.
- Prévia: GET independente, informativo, não vinculante e sem efeitos colaterais. A sugestão inicial é editável; usuário pode mantê-la ou substituí-la. Falha, ausência ou indisponibilidade não bloqueiam preço válido quando as demais validações são satisfeitas.
- Cotação corrente: `Acao.cotacaoAtual` e suas observações persistidas, sem alteração pelo cadastro da operação.
- Fechamento histórico: close bruto da data exata, obtido somente pelo fluxo consultivo. Preço manual, cotação atual, adjustedClose e outro pregão não são fallback de sucesso da prévia.

POST não consulta a prévia nem provider histórico. Não existe reserva de preço, token de prévia ou obrigação de comprovar sucesso do GET. Data civil e validações cronológicas permanecem, sem exigir candle para cadastrar.

### 2. Preservação de baseline

Cada MODIFIED transporta o requisito completo, incluindo cenários não alterados. Não há ADDED ou REMOVED Requirements. São 16 MODIFIED e três RENAMED Requirements; as renomeações referenciam títulos exatos existentes e MODIFIED usa o título de destino.

Requisitos renomeados:

| Capability | Origem | Destino |
|---|---|---|
| operation-registration | Cotação histórica integrada somente à COMPRA | Registro independente de fechamento histórico |
| purchase-price-preview | Mesma fonte histórica da criação de COMPRA | Fonte histórica da prévia de COMPRA |
| stock-quote-history | Não expor consulta pública nesta primeira fatia | Separação entre prévia pública e histórico de cotações |

Cenários com renomeações propostas NÃO aplicadas. Por decisão aprovada, OpenSpec 1.9.0 não suporta rename real de cenário: os nove títulos históricos da coluna Origem serão preservados por compatibilidade com a proteção nominal de MODIFIED. Somente os conteúdos funcionais são reconciliados para a regra vigente, sem duplicar cenários. A limitação aceita é exclusivamente nominal; não haverá segunda etapa automática prometida para renomeá-los. As três renomeações oficiais de requisitos acima continuam válidas.

| Capability | Origem | Destino |
|---|---|---|
| operation-registration | COMPRA com preço proibido | COMPRA com preço informado aceito |
| operation-registration | Primeira compra com fechamento exato | Primeira compra com preço informado |
| operation-registration | Falha externa antes da transação | Falha da prévia não impede compra válida |
| operation-registration | Preço de COMPRA obtido do fechamento exato | Preço informado prevalece sobre referência histórica |
| operation-registration | Provider chamado somente para COMPRA | Nenhum provider histórico chamado pelo POST |
| operation-registration | Falha histórica impede nova COMPRA | Indisponibilidade histórica não impede compra manual |
| purchase-price-preview | Cliente tenta impor preço da COMPRA | Cliente informa preço final da COMPRA |
| stock-quote-history | Compra com fechamento externo | Prévia e compra preservam histórico corrente |
| api-documentation | Dependência externa exclusiva da COMPRA | Independência externa de COMPRA e VENDA |

Nos cenários que antes exigiam provider, substituir apenas a condição obsoleta. Preservar persistência atômica, saldo derivado, ausência de consolidação financeira materializada, referências e isolamento. A condição de falha externa passa a demonstrar independência do POST; não eliminar sua cobertura.

Repetições preexistentes (por exemplo, os dois cenários de prévia sem mutação) são preservadas. Não criar novas duplicações ou requisitos equivalentes para contornar renomeações.

### 3. Capability histórica preservada

`historical-closing-price` não participa dos deltas. Endpoint, providers, close bruto, data exata, precisão e classificações de erro permanecem intactos. A divergência BRAPI 502 × 422 não bloqueia a definição da independência do POST e será tratada separadamente.

### 4. Implementação futura estritamente documental

- `src/main/java/com/projeto/dto/PreviaPrecoCompraResponse.java`: substituir a descrição de reconsulta por sugestão independente e não vinculante.
- `src/main/java/com/projeto/resources/OperacaoResource.java`: esclarecer que a consulta não altera estado e sua resposta pode preencher preço editável; retirar ambiguidade de exibição somente leitura.
- `src/test/java/com/projeto/resources/OpenApiDocumentationTest.java`: inspecionar schema e endpoint servidos, cobrindo ausência da promessa de reconsulta e presença da semântica correta.
- `frontend/src/app/features/operacoes/operacoes.service.spec.ts`: alterar somente o título que diz COMPRA sem preço; manter asserções e fixtures.

Não alterar DTOs de criação, OperacaoService, OperacaoPersistenceService, entidades, repositories, integrações, calculadoras ou componentes Angular. Não remover testes funcionais nem limpar mocks desconectados nesta change.

### 5. Validação futura

Executar teste OpenAPI direcionado e o teste de serviço frontend alterado. Confirmar regressão nos testes de contrato/recurso de Operações e sequência prévia seguida de POST; preservar testes de formulário/contexto, replay e concorrência. Registrar comandos e resultados antes de marcar tasks. Não executar suites de aplicação na criação dos artefatos.

Antes de archive, repetir strict, revisar deltas contra a baseline então vigente e construir resultado futuro somente em memória. Comparar todos os requisitos e cenários, inclusive os não abrangidos, e verificar Purposes inalterados. Não usar flags que pulem validação.

## Risks / Trade-offs

- OpenSpec 1.9.0 usa `findMissingCurrentScenarios` e exige preservação nominal dos cenários em MODIFIED. RENAMED Requirements não oferece renomeação de cenários. A decisão aprovada preserva os nove títulos históricos, embora possam sugerir regras obsoletas; os corpos reconciliados expressam a regra vigente. Não duplicar cenários, remover/recriar requisitos ou editar specs consolidadas como atalho. Não prometer renomeação automática posterior.
- Atualização concorrente da baseline exige repetir comparação antes da implementação/promoção.
- Cópia parcial de requisito perderia cenários: manter blocos completos e comparação independente em memória.
- Contradições gerais fora deste tema permanecem deliberadamente (Purpose de Carteiras e contagem de endpoints, por exemplo).

## Migration Plan

Sem migração funcional ou de dados. Implementar somente após autorização posterior; validar documentação e testes dirigidos; revisar consolidação; archive e operações Git são etapas futuras sujeitas a autorização. Nesta etapa, somente os artefatos de planejamento são criados.

## Open Questions

Resolvida por decisão aprovada em 2026-10-03: preservar os nove títulos históricos dos cenários e seus conteúdos funcionais reconciliados. OpenSpec 1.9.0 não oferece rename real de cenário. As três renomeações de requisitos permanecem pelo mecanismo oficial. Não há pendência de regra financeira nem promessa de renomeação automática posterior.

## Validação dos artefatos de planejamento — 2026-10-02

`openspec validate reconciliar-contratos-de-preco-das-operacoes --strict --no-interactive` executado: reprovado com sete erros de cenário omitido, correspondentes às nove renomeações mapeadas acima. Não foram identificados outros erros por esse comando. Nenhuma task de implementação/validação futura foi marcada como concluída.

A construção oficial em memória (`buildUpdatedSpec`, sem chamada de escrita ou archive) aceita frontend-portfolio-management e recusa as outras quatro capabilities pela proteção nominal. Uma comparação independente em memória, considerando explicitamente as renomeações, preserva:

| Capability | Requisitos antes/depois | Cenários antes/depois | Requisitos fora do delta idênticos |
|---|---|---|---|
| operation-registration | 19 / 19 | 74 / 74 | 10 |
| purchase-price-preview | 3 / 3 | 14 / 14 | 1 |
| stock-quote-history | 9 / 9 | 22 / 22 | 7 |
| api-documentation | 8 / 8 | 22 / 22 | 6 |
| frontend-portfolio-management | 11 / 11 | 54 / 54 | 10 |

Nenhum requisito órfão, nova duplicação de requisito ou redução de cenários foi encontrado nessa comparação. Isso não equivalia à aprovação da consolidação oficial: na validação inicial de 2026-10-02, a change permanecia bloqueada para promoção na ferramenta instalada. Specs consolidadas, código e testes não foram alterados; nenhuma suite foi executada.

## Correção documental aprovada e validação — 2026-10-03

Restaurados exclusivamente os nove cabeçalhos históricos nos quatro deltas afetados. Os demais bytes desses deltas, incluindo WHEN/THEN, textos de requisitos e pares RENAMED, foram preservados. Design, proposal e tasks registram a decisão aprovada; frontend-portfolio-management/spec.md e .openspec.yaml permanecem intactos.

`openspec validate reconciliar-contratos-de-preco-das-operacoes --strict --no-interactive`: aprovado. A construção oficial em memória por `buildUpdatedSpec`, sem escrita ou archive, aceitou as cinco capabilities; cada resultado reconstruído também passou em `validateSpecContent` com strict.

| Capability | Requisitos antes/depois | Cenários antes/depois | Títulos históricos preservados | Títulos novos propostos como cabeçalhos |
|---|---|---|---|---|
| operation-registration | 19 / 19 | 74 / 74 | 6 | 0 |
| purchase-price-preview | 3 / 3 | 14 / 14 | 1 | 0 |
| stock-quote-history | 9 / 9 | 22 / 22 | 1 | 0 |
| api-documentation | 8 / 8 | 22 / 22 | 1 | 0 |
| frontend-portfolio-management | 11 / 11 | 54 / 54 | 0 | 0 |
| Total | 50 / 50 | 186 / 186 | 9 | 0 |

Confirmados zero cenários perdidos, zero novas duplicações e zero requisitos órfãos. As três renomeações oficiais produzem zero ocorrências dos requisitos antigos e exatamente uma de cada destino. Todos os requisitos fora do delta e Purposes permanecem idênticos; repetições semânticas preexistentes são preservadas.

Continuam declarados 0 ADDED, 16 MODIFIED, 0 REMOVED e 3 RENAMED Requirements. O aplicador conta 12 modificações efetivas e 3 renomeações, pois quatro MODIFIED já coincidem com a baseline após a preservação dos títulos e normalização do conteúdo.

`git diff --check`: aprovado, sem saída. `git diff --stat`: sem saída, pois a change inteira está não rastreada; esses comandos não incluem seus arquivos. `git status --short`: `?? openspec/changes/reconciliar-contratos-de-preco-das-operacoes/`. A revisão dos deltas foi realizada diretamente, incluindo a verificação de que somente os nove cabeçalhos foram alterados.

O bloqueio estrutural desta etapa está resolvido. As 19 tasks permanecem pendentes para execução/revisão futura, sem antecipar implementação ou autorização de archive. Specs consolidadas, código, testes, README, PRD e changes arquivadas não foram alterados. Nenhum teste da aplicação, staging, commit, push, merge ou archive foi executado.
