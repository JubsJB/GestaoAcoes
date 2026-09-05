## Context

Ver `proposal.md` para motivação e escopo. O frontend Angular já possui shell, rota lazy `/dashboard`, `CarteirasService`, interceptor de erros, componentes compartilhados de feedback e padrões de loading/empty state. O Dashboard ainda é placeholder. O backend expõe três visões financeiras por `carteiraId`; seus valores monetários são `BigDecimal`, e o parser JSON padrão do navegador não oferece preservação lossless suficiente.

O parser lossless existente pertence à feature Operações e reconhece apenas seus campos. A nova capability atravessa Dashboard, infraestrutura HTTP/decimal compartilhada e navegação existente, portanto requer uma fronteira reutilizável sem alterar contratos backend.

## Goals / Non-Goals

**Goals:**

- Implementar uma página global que sempre opere no contexto de uma única Carteira identificável.
- Preservar tokens financeiros integralmente e limitar formatação ao nível de apresentação.
- Manter seções por moeda e impedir totais ou conversões indevidos por construção.
- Tornar carregamento, seleção, erros e respostas vazias explícitos e testáveis.
- Reutilizar shell, rotas, serviços de Carteiras, feedback e fluxo contextual de Operações existentes.

**Non-Goals:**

- Criar ou alterar cálculos financeiros, endpoints, contratos ou persistência.
- Consumir `/patrimonio`, pois `/resumo` já fornece `patrimonioAtual`.
- Consumir `/evolucao-patrimonial`, criar snapshots, gráficos SVG ou instalar biblioteca de gráficos.
- Atualizar periodicamente, agregar Carteiras, somar BRL e USD ou converter moedas.
- Somar resultados realizados, criar detalhe de posição ou modificar funcionalidades de Carteiras, Operações, Ações e Corretoras.

## Decisions

### 1. Dashboard global com contexto explícito

`/dashboard` substituirá seu placeholder e carregará primeiro `CarteirasService.listar()`. Com zero Carteiras, exibirá CTA de cadastro. Com exatamente uma e sem seleção válida, selecionará essa Carteira automaticamente porque não há ambiguidade. Com duas ou mais, aguardará escolha explícita.

Alternativa rejeitada: incorporar o Dashboard em `/carteiras/:id`. Isso deixaria o destino principal como placeholder e criaria conflito desnecessário com o escopo atual do detalhe de Carteira. Também foi rejeitada a implementação simultânea global e contextual por ampliar a change sem benefício necessário.

### 2. `carteiraId` como query parameter canônico

A seleção será refletida em `/dashboard?carteiraId={id}`. A lista de Carteiras valida o parâmetro antes das consultas financeiras. Valor ausente segue a cardinalidade da lista; valor positivo e presente seleciona; valor malformado ou ausente da lista produz estado recuperável, preservando o seletor disponível.

Ao selecionar ou trocar Carteira, a navegação atualizará o query parameter e disparará um novo contexto. A URL suporta reload, link direto e histórico do navegador. A remoção concorrente de uma Carteira será tratada tanto pela validação da lista quanto por eventual 404 financeiro, sem tela quebrada.

Alternativa rejeitada: guardar seleção apenas em memória, porque reload perderia contexto. Também não será escolhida silenciosamente a primeira Carteira em coleções múltiplas.

### 3. Orquestração das consultas financeiras

Após contexto válido, serão iniciadas em paralelo:

- `GET /carteiras/{id}/resumo`;
- `GET /carteiras/{id}/posicoes`;
- `GET /carteiras/{id}/resultados-realizados`.

Uma composição RxJS cancelável por mudança de `carteiraId`, preferencialmente com `switchMap`, impedirá resposta antiga de contaminar o novo contexto. O reload emitirá nova solicitação para as três fontes. Não haverá retry automático.

O estado financeiro será tratado como uma carga coerente da página: conteúdo anterior é invalidado ao trocar contexto; loading e erro pertencem à seleção corrente. Se as respostas representarem instantes ligeiramente diferentes, o frontend as exibirá sem tentar reconciliar ou recalcular.

Alternativa rejeitada: chamar `/patrimonio` além de `/resumo`, pois duplicaria informação e request. Chamadas sequenciais também foram rejeitadas por aumentar latência sem dependência entre elas.

### 4. Service financeiro dedicado e models manuais

Um service do Dashboard montará URLs explícitas a partir da configuração central da API e retornará models discriminados por `moeda`. `CarteirasService` continuará responsável apenas por Carteiras. Os DTOs financeiros serão manuais, com IDs como `number`, datas como `string`, enums existentes para mercado/moeda e todos os `BigDecimal` como `string`.

Não será gerado client OpenAPI nem adicionada dependência.

### 5. Infraestrutura lossless compartilhada

As respostas financeiras serão solicitadas como texto. Um parser compartilhado converterá somente tokens JSON de chaves decimais aprovadas em strings antes de `JSON.parse`, preservando inclusive sinal, escala e notação válida recebida. A lista explícita incluirá os campos dos três DTOs e será coberta por testes de payloads aninhados e coleções.

O utilitário ficará fora da feature Operações. A implementação avaliará extrair a lógica genérica existente e manter adaptadores finos para Operações, evitando duplicação sem ampliar seu comportamento. Formatação monetária/percentual será textual, com arredondamento apenas visual quando requerido, e nunca alterará o valor armazenado.

Alternativas rejeitadas: `HttpClient` JSON padrão, `Number`, `parseFloat` e aritmética binária, devido a perda potencial de precisão; biblioteca decimal externa, porque não é necessária para exibição.

### 6. Representação por moeda

Cada `ResumoMoedaResponse` originará um grupo independente com patrimônio, custo, resultado não realizado e rentabilidade. BRL usará `R$`; USD usará `US$`. Resultado negativo preservará sinal e receberá texto/ícone semanticamente compreensível além de eventual cor.

Não existirá acumulador de moedas, total geral, taxa cambial nem preenchimento artificial de moeda ausente. A quantidade de posições abertas será apenas `posicoes.length`, rotulada explicitamente como contagem estrutural.

### 7. Posições e resultados realizados

Posições serão apresentadas em tabela responsiva ou cards equivalentes no compacto. Todos os campos essenciais definidos na spec permanecerão acessíveis; a adaptação poderá mudar composição, mas não omitir informação essencial. Não haverá link para detalhe de posição inexistente.

Resultados realizados serão listados por Ação e moeda. Não haverá redução/soma da coleção. Coleção vazia será um empty state próprio, não valor financeiro zero.

### 8. Estados e erros

Haverá duas camadas de estado:

1. descoberta de contexto: loading/erro da lista, zero Carteiras, seleção pendente e seleção inválida;
2. dados financeiros: loading, conteúdo, respostas vazias e erro.

O interceptor e `NormalizedHttpError` existentes continuarão sendo a fronteira central. 404 indicará contexto removido/inexistente e permitirá selecionar novamente; 409 comunicará histórico inconsistente; 422 comunicará cálculo fora da precisão; erro técnico usará a mensagem normalizada. Retry será sempre explícito e repetirá apenas a etapa falha apropriada.

### 9. Navegação

Com seleção válida, haverá links para `/carteiras/{id}` e para o fluxo existente de registro de Operação já contextualizado pela Carteira. A implementação deverá reutilizar a forma de contexto aceita por `frontend-operation-management`, sem criar outra lógica de formulário.

### 10. Acessibilidade e responsividade

O seletor terá label persistente; o título principal e as seções usarão hierarquia semântica. Loading usará estado ocupado e anúncio polido. Erros terão mensagem e ação de recuperação. Atualizações não moverão foco automaticamente, exceto quando uma ação produzir erro que exija anúncio/foco conforme o padrão existente. Tabelas terão cabeçalhos associados; no compacto, uma representação alternativa preservará nome e valor de cada campo.

Sinais `+`/`-`, texto acessível e labels impedirão dependência exclusiva de cor. Botões de atualizar, acessar Carteira e registrar Operação terão nomes inequívocos.

### 11. Evolução patrimonial separada

O endpoint de evolução possui dados adequados, mas gráficos exigem decisões adicionais de escala, lacunas, séries por moeda e acessibilidade. Ele ficará para `implementar-evolucao-patrimonial-frontend`, evitando coerção numérica gráfica e dependência prematura nesta entrega.

## Risks / Trade-offs

- [Os três endpoints podem refletir instantes próximos, mas diferentes] → carregá-los em paralelo, oferecer reload conjunto e não inventar reconciliação financeira.
- [Parser por nomes de campo pode proteger campo homônimo não financeiro] → manter allowlist pequena, models explícitos e testes de estrutura real dos DTOs.
- [Extração do parser pode causar regressão em Operações] → preservar API/adaptador existente e executar testes lossless e de Operações.
- [Carteira removida após a listagem pode gerar 404] → invalidar o conteúdo financeiro, apresentar erro tratável e permitir recarregar Carteiras.
- [Muitas colunas prejudicam viewport compacto] → usar composição responsiva acessível sem eliminar dados essenciais.
- [Contagem de posições pode parecer indicador calculado] → rotular como “posições abertas” e não associá-la a valor monetário.
- [Carga all-or-nothing reduz conteúdo parcial disponível] → priorizar coerência e recuperação simples nesta primeira versão; não combinar respostas parciais de contextos distintos.

## Migration Plan

1. Introduzir infraestrutura lossless e models sem alterar endpoints existentes.
2. Adicionar service e componente funcional sob o limite lazy já existente.
3. Substituir somente a resolução do placeholder do Dashboard.
4. Validar regressões do shell e das quatro features já funcionais.
5. Em rollback, restaurar o componente placeholder e sua rota; nenhuma migração de dados ou backend é necessária.
