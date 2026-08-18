## 1. Visão Geral do Projeto

Este repositório contém o sistema de **Gestão de Ações / Controle de Ações**.

O objetivo inicial do projeto é permitir o gerenciamento de uma carteira de ações brasileiras e americanas, incluindo cadastro de corretoras, registro de operações, acompanhamento das posições dos ativos, cálculo de preço médio, cálculo de lucro ou prejuízo realizado e consulta de informações de mercado.

A arquitetura deve permitir que, futuramente, o sistema seja expandido para outros tipos de investimentos sem exigir alterações desnecessárias nas funcionalidades já existentes.

O PRD oficial do projeto está localizado em `docs/PRD.md`.

O arquivo `docs/PRD.md` é a principal fonte de verdade para requisitos funcionais e regras de negócio do sistema.

Antes de implementar ou alterar uma funcionalidade relacionada a requisitos do produto, consultar `docs/PRD.md`.

Este `AGENTS.md` define como agentes de IA, incluindo o Codex, devem trabalhar neste repositório e registra regras críticas do projeto que não devem ser violadas durante a implementação.

---

## 2. Fontes de Verdade

Ao trabalhar neste projeto, considerar a seguinte ordem de prioridade:

1. Instruções explícitas fornecidas pelo usuário para a tarefa atual.
2. Especificações aprovadas da alteração atual, quando estiver sendo utilizado o OpenSpec.
3. PRD atual do projeto.
4. Este `AGENTS.md`.
5. Testes automatizados existentes.
6. Implementação atual.

Se a implementação existente entrar em conflito com uma regra de negócio explicitamente definida no PRD ou em uma especificação aprovada, não preservar silenciosamente o comportamento incorreto.

Identificar a divergência e seguir a especificação vigente.

Se uma regra de negócio estiver ambígua, solicitar esclarecimento em vez de inventar uma regra financeira.

---

## 3. Escopo Atual do Projeto

A versão atual do sistema é focada no controle e gestão de ações.

O sistema deve permitir:

- cadastro e gerenciamento de corretoras;
- gerenciamento de ações brasileiras;
- gerenciamento de ações americanas;
- cadastro e/ou consulta de ativos quando necessário;
- registro de operações de compra;
- registro de operações de venda;
- consulta do histórico de operações;
- cálculo automático da posição dos ativos;
- cálculo do preço médio ponderado;
- cálculo de lucro ou prejuízo realizado;
- consulta da cotação atual dos ativos;
- consulta de dados de ações brasileiras através da BRAPI;
- consulta de dados de ações americanas através da Alpha Vantage;
- consulta de CNPJ através da BrasilAPI;
- consulta e validação de CEP através da ViaCEP.

O sistema deve ser desenvolvido de maneira que novos tipos de investimentos possam ser adicionados futuramente sem criar acoplamento desnecessário exclusivamente ao domínio de ações.

---

## 4. Fora do Escopo Atual

A menos que seja explicitamente solicitado, não implementar nesta etapa:

- cadastro de investidor;
- cadastro de usuário;
- autenticação;
- login;
- autorização;
- perfis e permissões;
- múltiplos usuários;
- carteiras vinculadas a usuários;
- login social;
- recuperação de senha;
- autenticação multifator;
- outras classes de investimentos;
- análises avançadas de carteira não definidas no PRD;
- cálculo tributário não definido no PRD;
- negociação automática;
- envio de ordens para corretoras.

Essas funcionalidades representam possíveis evoluções futuras e não devem ser implementadas antecipadamente.

---

## 5. Evoluções Futuras

O projeto deverá evoluir além da versão inicial.

Entre as possíveis evoluções estão:

- cadastro de investidor/usuário;
- autenticação e login;
- carteiras vinculadas a investidores;
- múltiplas carteiras por investidor;
- autorização e controle de acesso;
- novos tipos de investimentos;
- novas instituições financeiras;
- novas integrações externas;
- análises avançadas de carteira;
- dashboards;
- relatórios.

Decisões arquiteturais atuais devem evitar tornar essas evoluções desnecessariamente difíceis.

Entretanto, não implementar essas funcionalidades antes que sejam explicitamente incluídas no escopo do projeto.

---

## 6. Regras de Negócio Fundamentais

As regras financeiras são críticas para o sistema.

Não alterar comportamentos relacionados aos cálculos financeiros sem verificar o PRD e, quando existir, a especificação correspondente no OpenSpec.

As regras relacionadas a:

- quantidade;
- custo da posição;
- preço médio;
- lucro realizado;
- prejuízo realizado;

devem permanecer centralizadas na camada de domínio ou serviço responsável pelas regras de negócio.

Não duplicar cálculos financeiros em Controllers, componentes Angular, integrações ou outros pontos da aplicação.

---

## 7. Operações

Toda movimentação de ações deve ser representada através de uma operação.

Os tipos inicialmente suportados são:

- `COMPRA`;
- `VENDA`.

Caso o código utilize enums em inglês, podem ser utilizados:

- `BUY`;
- `SELL`.

Uma operação deve possuir as informações exigidas pelo PRD, incluindo conceitos como:

- ativo;
- corretora;
- tipo da operação;
- quantidade;
- preço unitário;
- data da operação.

Campos adicionais podem existir conforme o modelo de domínio e o PRD.

Não assumir que a cotação atual do ativo corresponde ao preço da operação.

O preço da operação representa o preço efetivamente registrado no momento da compra ou venda.

---

## 8. Recalcular Posição

O sistema deve recalcular automaticamente a posição do ativo após cada operação.

Uma posição deve controlar conceitualmente pelo menos:

- quantidade atual;
- custo atual da posição;
- preço médio;
- lucro/prejuízo realizado, quando aplicável.

Os cálculos da posição devem ser determinísticos e baseados nas operações registradas e nas regras de negócio.

---

## 9. Regra de Compra

Uma operação de compra aumenta:

- a quantidade do ativo;
- o custo total da posição.

O preço médio deve ser recalculado utilizando média ponderada.

Conceitualmente:

```text
custoCompra =
    quantidadeComprada * precoUnitarioCompra

novoCustoTotal =
    custoTotalAtual + custoCompra

novaQuantidade =
    quantidadeAtual + quantidadeComprada

novoPrecoMedio =
    novoCustoTotal / novaQuantidade
```

Representação equivalente:

```text
novoPrecoMedio =
    ((quantidadeAtual * precoMedioAtual)
    + (quantidadeComprada * precoUnitarioCompra))
    / novaQuantidade
```

Não calcular o novo preço médio utilizando uma média aritmética simples entre o preço médio anterior e o preço da nova compra.

---

## 10. Regra de Venda

Uma operação de venda reduz:

- a quantidade do ativo;
- o custo da posição correspondente à quantidade vendida.

O preço de venda não deve ser utilizado para recalcular o preço médio da posição remanescente.

Antes da venda, utilizar o preço médio vigente.

Conceitualmente:

```text
custoBaixado =
    quantidadeVendida * precoMedioAtual

novaQuantidade =
    quantidadeAtual - quantidadeVendida

novoCustoTotal =
    custoTotalAtual - custoBaixado
```

Para uma venda parcial:

```text
novoPrecoMedio = precoMedioAtual
```

Portanto, o preço médio unitário da posição remanescente deve permanecer igual ao preço médio existente antes da venda.

---

## 11. Lucro ou Prejuízo Realizado

O resultado financeiro de uma venda deve ser calculado separadamente do preço médio da posição remanescente.

Conceitualmente:

```text
resultadoRealizado =
    (precoUnitarioVenda - precoMedioAtual)
    * quantidadeVendida
```

Se:

```text
precoUnitarioVenda > precoMedioAtual
```

a operação gera lucro realizado.

Se:

```text
precoUnitarioVenda < precoMedioAtual
```

a operação gera prejuízo realizado.

Se os valores forem iguais, o resultado realizado será zero.

O lucro ou prejuízo realizado não deve ser incorporado ao preço médio das ações remanescentes.

---

## 12. Venda Total da Posição

Se uma venda remover toda a quantidade restante da posição:

```text
novaQuantidade = 0
novoCustoTotal = 0
novoPrecoMedio = 0
```

O lucro ou prejuízo realizado na venda deve continuar registrado separadamente.

Uma nova compra realizada depois que a posição foi completamente encerrada inicia um novo cálculo de posição a partir de zero, salvo se o PRD definir explicitamente outro comportamento.

---

## 13. Validação de Venda

O sistema nunca deve permitir:

```text
quantidadeVendida > quantidadeDisponivel
```

A venda também deve respeitar todas as validações definidas pelo PRD.

Operações financeiras inválidas devem falhar explicitamente em vez de gerar dados inconsistentes na carteira.

Quantidades e valores monetários não devem ser zero ou negativos, salvo quando uma regra de negócio explicitamente permitir.

---

## 14. Ordem Cronológica das Operações

Os cálculos da posição dependem da ordem das operações.

Ao reconstruir ou recalcular uma posição a partir do histórico, as operações devem ser processadas cronologicamente conforme as regras definidas no PRD.

Não utilizar a ordem de inserção no banco de dados como substituto da ordem cronológica das operações.

Caso duas operações possam ocorrer na mesma data e a ordem entre elas possa alterar os cálculos, utilizar o mecanismo de ordenação definido pelo domínio.

Caso essa regra ainda não exista e se torne necessária, solicitar esclarecimento antes de inventar um comportamento.

---

## 15. Cálculos Monetários

Os cálculos financeiros devem evitar problemas de precisão de ponto flutuante.

No backend Java:

- utilizar preferencialmente `BigDecimal` para valores monetários;
- não utilizar `float` para cálculos monetários;
- evitar `double` para cálculos financeiros centrais;
- definir explicitamente regras de arredondamento quando necessário;
- evitar arredondamentos arbitrários durante cálculos intermediários.

Utilizar as regras de escala e arredondamento definidas pelo PRD quando existirem.

Caso não estejam definidas e a decisão possa alterar resultados financeiros, solicitar esclarecimento.

---

## 16. Cadastro e Gerenciamento de Corretoras

O cadastro de corretoras faz parte do escopo atual do sistema.

As informações das corretoras devem seguir os requisitos definidos no PRD.

Quando for necessário consultar ou validar informações relacionadas ao CNPJ, utilizar a BrasilAPI.

Quando for necessário consultar ou validar informações relacionadas ao CEP, utilizar a ViaCEP.

O cadastro de corretoras não deve depender da existência de uma entidade de investidor ou usuário nesta versão.

Não introduzir autenticação, usuário ou vínculo de propriedade apenas para implementar o cadastro de corretoras.

---

## 17. Integrações Externas

As APIs externas devem permanecer isoladas das regras de negócio centrais.

Preferir componentes dedicados, como:

- clients;
- gateways;
- adapters;
- services de integração.

Evitar chamadas diretas para APIs externas dentro de Controllers ou entidades de domínio.

Os cálculos históricos da carteira não devem depender da disponibilidade das APIs externas.

Falhas em APIs externas não devem corromper operações ou posições existentes.

### 17.1 BRAPI

Utilizar a BRAPI para dados e cotações de ações brasileiras.

Utilizar essa API quando uma funcionalidade exigir informações de ações brasileiras suportadas pelo serviço.

Não substituir a BRAPI por outro provedor sem solicitação explícita.

### 17.2 Alpha Vantage

Utilizar a Alpha Vantage para dados e cotações de ações americanas.

Não substituir a Alpha Vantage por outro provedor sem solicitação explícita.

Chaves de API não devem ser armazenadas diretamente no código-fonte.

Utilizar variáveis de ambiente, arquivos de configuração apropriados ou a estratégia de configuração adotada pelo projeto.

### 17.3 BrasilAPI

Utilizar a BrasilAPI para consultas relacionadas a CNPJ quando necessário no cadastro de corretoras ou em outras funcionalidades explicitamente definidas.

Não acoplar a lógica de consulta de CNPJ diretamente aos Controllers ou componentes da interface.

### 17.4 ViaCEP

Utilizar a ViaCEP para consulta e validação de CEP quando necessário.

Falhas na consulta de endereço devem ser tratadas explicitamente.

Falhas em APIs externas não devem deixar dados parcialmente inconsistentes no domínio.

---

## 18. Cotação do Ativo vs. Preço da Operação

A cotação de uma ação representa uma informação atual de mercado obtida através de um provedor externo.

O preço da operação representa o valor efetivamente registrado em uma compra ou venda.

Esses conceitos devem permanecer separados.

Exemplo:

```text
cotacao
    ↓
informacao atual de mercado

preco da operacao
    ↓
informacao historica da compra ou venda
```

Alterações na cotação atual nunca devem modificar os preços das operações históricas.

Os cálculos históricos da carteira não devem ser alterados retroativamente porque a cotação atual do ativo mudou.

---

## 19. Diretrizes de Arquitetura

Manter responsabilidades separadas.

Preferir uma estrutura conceitual semelhante a:

```text
Controller / API
        ↓
Application / Service
        ↓
Domain / Regras de Negocio
        ↓
Repository
```

Integrações externas devem permanecer separadas:

```text
Application / Service
        ↓
Abstracao da Integracao
        ↓
Client da API Externa
```

Não colocar cálculos financeiros importantes em:

- Controllers;
- componentes Angular;
- código de mapeamento de DTO;
- clients de APIs externas;
- implementações de Repository.

As regras de negócio devem ser testáveis independentemente da infraestrutura sempre que possível.

---

## 20. Diretrizes do Backend

Ao implementar funcionalidades no backend:

- seguir as convenções Java existentes no projeto;
- seguir a organização de packages existente;
- utilizar nomes claros e relacionados ao domínio;
- utilizar `BigDecimal` para valores monetários;
- validar entradas;
- evitar duplicação de regras de negócio;
- manter Controllers enxutos;
- separar persistência de cálculos financeiros;
- utilizar DTOs quando apropriado;
- evitar exposição desnecessária de entidades de persistência;
- tratar explicitamente falhas em integrações externas.

Não introduzir novos frameworks ou padrões arquiteturais sem uma necessidade concreta.

---

## 21. Diretrizes do Frontend

O frontend deve consumir as funcionalidades fornecidas pelo backend em vez de reproduzir as regras financeiras centrais.

O Angular pode apresentar informações calculadas, mas não deve se tornar a implementação oficial das regras de cálculo da carteira.

Evitar duplicar no frontend:

- cálculo de preço médio;
- cálculo da posição;
- cálculo de lucro/prejuízo realizado;
- regras financeiras de validação.

O backend deve permanecer como fonte autoritativa dessas regras.

Seguir a estrutura e as convenções Angular existentes no projeto.

---

## 22. Testes

As regras financeiras devem ser protegidas por testes automatizados.

Ao alterar cálculos relacionados às posições, criar ou atualizar testes que cubram cenários relevantes, incluindo:

- primeira compra;
- múltiplas compras com preços diferentes;
- venda parcial com lucro;
- venda parcial com prejuízo;
- venda pelo mesmo valor do preço médio;
- venda total da posição;
- tentativa de venda acima da quantidade disponível;
- nova compra após uma posição ter sido completamente encerrada.

Quando aplicável, testar também:

- falhas nas integrações externas;
- entradas inválidas;
- quantidade zero;
- quantidade negativa;
- valores monetários inválidos;
- cálculos dependentes da ordem cronológica.

Não modificar uma regra financeira sem verificar se os testes correspondentes também precisam ser alterados.

---

## 23. Trabalho com o PRD

O PRD descreve o que o sistema deve fazer.

Este AGENTS.md descreve como os agentes de IA devem trabalhar durante a implementação e registra restrições críticas do projeto.

O AGENTS.md não substitui o PRD.

Antes de implementar um requisito:

1. identificar o requisito correspondente no PRD;
2. identificar suas regras de negócio;
3. consultar a implementação atual;
4. consultar os testes relacionados;
5. identificar os componentes afetados;
6. implementar a menor alteração coerente possível;
7. validar o resultado contra o requisito.

Quando o PRD for alterado, verificar se alguma regra deste AGENTS.md também precisa ser atualizada.

Se o PRD entrar em conflito com este arquivo e a divergência puder afetar um comportamento financeiro, informar o conflito em vez de tomar uma decisão silenciosamente.

---

## 24. Trabalho com OpenSpec

Utilizar o OpenSpec para especificar alterações significativas antes da implementação quando apropriado.

O PRD representa os requisitos gerais do produto.

O OpenSpec representa uma alteração ou funcionalidade específica que está sendo desenvolvida.

Fluxo conceitual:

```text
PRD
 ↓
Requisito
 ↓
OpenSpec
 ↓
Implementacao
 ↓
Testes
```

Quando existir uma alteração aprovada no OpenSpec:

- consultá-la antes da implementação;
- respeitar o escopo definido;
- identificar os requisitos relacionados;
- evitar refatorações não relacionadas;
- criar ou atualizar testes conforme a especificação.

O OpenSpec não deve substituir o PRD.

---

## 25. Trabalho com Graphify

Este projeto utiliza o Graphify para manter um grafo de conhecimento do código.

Utilizar o Graphify para compreender:

- classes afetadas;
- dependências;
- relacionamentos entre componentes;
- services relevantes;
- arquitetura;
- impacto de alterações propostas.

Após alterações no código, atualizar o grafo conforme as instruções do Graphify.

## **graphify**

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

---

## 26. Fluxo Recomendado para os Agentes

Para tarefas significativas de implementação, seguir preferencialmente este fluxo:

```text
Solicitacao do Usuario
        ↓
Ler AGENTS.md
        ↓
Identificar requisito no PRD
        ↓
Consultar OpenSpec
(se existir especificacao para a alteracao)
        ↓
Consultar Graphify
        ↓
Identificar arquivos e componentes afetados
        ↓
Inspecionar somente o codigo necessario
        ↓
Implementar alteracao
        ↓
Criar/atualizar testes
        ↓
Executar testes relevantes
        ↓
Atualizar Graphify
        ↓
Informar alteracoes realizadas
```

Não iniciar uma exploração ampla e desnecessária do código quando o Graphify puder fornecer primeiro os relacionamentos relevantes.

---

## 27. Disciplina de Alterações

Ao modificar o projeto:

- realizar a menor alteração coerente que satisfaça o requisito;
- não implementar funcionalidades não relacionadas;
- evitar grandes refatorações sem justificativa;
- preservar comportamentos existentes fora do escopo solicitado;
- evitar lógica duplicada;
- preservar a consistência do domínio;
- atualizar testes quando o comportamento mudar;
- atualizar documentação quando necessário.

Nunca alterar silenciosamente uma regra financeira.

---

## 28. Proibições Importantes

Não:

- calcular preço médio utilizando a cotação atual do ativo;
- modificar preços históricos das operações quando a cotação mudar;
- recalcular o preço médio remanescente utilizando o preço de venda;
- permitir venda superior à quantidade disponível;
- utilizar `float` para cálculos monetários;
- utilizar `double` para cálculos financeiros centrais;
- armazenar chaves de APIs diretamente no código;
- depender de APIs externas para preservar a integridade do histórico de operações;
- duplicar cálculos financeiros entre backend e frontend;
- introduzir cadastro de investidor/usuário antes de entrar no escopo;
- introduzir autenticação ou login antes de entrar no escopo;
- substituir as APIs definidas pelo projeto sem autorização;
- inventar regras financeiras quando os requisitos forem ambíguos.

---

## 29. Definition of Done

Uma alteração deve ser considerada concluída somente quando, quando aplicável:

- o requisito solicitado estiver implementado;
- as regras de negócio relevantes forem respeitadas;
- as entradas forem devidamente validadas;
- existirem testes automatizados para o comportamento;
- os testes relevantes existentes estiverem passando;
- falhas de integrações externas estiverem tratadas;
- nenhuma credencial estiver hardcoded;
- documentação e especificação estiverem coerentes com a implementação;
- o Graphify tiver sido atualizado após alterações no código.

---

## 30. Princípio Central

O princípio mais importante deste projeto é:

> O estado da carteira deve ser derivado de forma consistente a partir de operações financeiras válidas, e alterações nas cotações de mercado nunca devem modificar o significado histórico das operações registradas.

Quando existir conflito entre conveniência de implementação e correção das regras financeiras, preservar a correção financeira e seguir o PRD.
