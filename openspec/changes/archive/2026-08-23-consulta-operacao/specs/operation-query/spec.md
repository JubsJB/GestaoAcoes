## Purpose

Definir consultas REST determinísticas e sem efeitos colaterais para recuperar as Operações persistidas e o histórico de uma Carteira, preservando exatamente o significado financeiro registrado.

## ADDED Requirements

### Requirement: Listagem geral das Operações persistidas
O sistema SHALL expor `GET /operacoes`, SHALL responder `200 OK` e SHALL devolver todas as Operações persistidas como uma lista de `OperacaoResponse` em ordem cronológica determinística. A consulta SHALL retornar o conjunto completo, sem filtros ou paginação nesta primeira fatia.

#### Scenario: Listagem com múltiplas Operações
- **WHEN** existem Operações persistidas e o cliente solicita `GET /operacoes`
- **THEN** o sistema responde `200 OK` com todas as Operações no contrato completo e na ordem definida pela capability

#### Scenario: Listagem sem Operações
- **WHEN** não existe Operação persistida e o cliente solicita `GET /operacoes`
- **THEN** o sistema responde `200 OK` com o array vazio `[]`

#### Scenario: Compras e vendas na mesma listagem
- **WHEN** existem Operações dos tipos `COMPRA` e `VENDA`
- **THEN** ambas integram a resposta com seus tipos e valores persistidos, sem transformação financeira

### Requirement: Consulta individual de Operação por identificador
O sistema SHALL expor `GET /operacoes/{id}` e SHALL responder `200 OK` com o `OperacaoResponse` completo quando o identificador corresponder a uma Operação persistida. Quando o identificador não existir, o sistema SHALL responder `404 Not Found` no formato `StandardError` vigente e MUST NOT criar ou alterar qualquer registro.

#### Scenario: Operação existente
- **WHEN** o cliente solicita `GET /operacoes/{id}` com o identificador de uma Operação persistida
- **THEN** o sistema responde `200 OK` com o `OperacaoResponse` correspondente

#### Scenario: Operação inexistente
- **WHEN** o cliente solicita `GET /operacoes/{id}` com um identificador sem Operação correspondente
- **THEN** o sistema responde `404 Not Found` no formato centralizado atual e não produz efeito colateral

### Requirement: Histórico de Operações de uma Carteira
O sistema SHALL expor `GET /carteiras/{carteiraId}/operacoes` em `CarteiraResource`, que SHALL delegar a execução a `OperacaoService`, sem duplicar a regra em `CarteiraService`. `OperacaoService` SHALL validar a existência da Carteira por meio de `CarteiraRepository`, consultar somente as Operações associadas ao identificador, aplicar a ordenação cronológica determinística e mapear cada item para `OperacaoResponse`. A existência da Carteira SHALL ser verificada independentemente da existência de Operações: Carteira existente sem histórico SHALL produzir `200 OK` com `[]`, enquanto Carteira inexistente SHALL produzir `404 Not Found` no formato `StandardError` vigente por meio de `ObjectNotFoundException` e do tratamento centralizado.

#### Scenario: Carteira existente com histórico
- **WHEN** a Carteira existe e possui múltiplas Operações
- **THEN** o sistema responde `200 OK` com todas e somente as Operações daquela Carteira na ordem definida

#### Scenario: Carteira existente sem histórico
- **WHEN** a Carteira existe e não possui Operações
- **THEN** o sistema responde `200 OK` com o array vazio `[]`

#### Scenario: Carteira inexistente
- **WHEN** `carteiraId` não identifica uma Carteira persistida
- **THEN** o sistema responde `404 Not Found` no formato centralizado e não consulta histórico de outra Carteira

#### Scenario: Isolamento entre Carteiras
- **WHEN** existem Operações associadas a Carteiras diferentes
- **THEN** o histórico solicitado contém exclusivamente as Operações cujo `carteiraId` corresponde ao identificador da URI

#### Scenario: Diferentes Ações na mesma Carteira
- **WHEN** a Carteira possui Operações de Ações distintas
- **THEN** todas essas Operações integram o mesmo histórico da Carteira, preservando em cada item seu ticker e mercado persistidos

### Requirement: Ordenação cronológica determinística
As listagens SHALL ordenar por `dataOperacao ASC`, `ordemNoDia ASC` e `id ASC`, nessa sequência. A ordem financeira SHALL continuar determinada exclusivamente por `dataOperacao` e `ordemNoDia`. O sistema SHALL usar `id` somente como terceiro desempate técnico para tornar a resposta determinística quando Operações independentes empatarem nas duas chaves financeiras. O identificador MUST NOT substituir `ordemNoDia`, redefinir a sequência financeira de uma mesma Carteira e Ação ou provocar recálculo da ordem persistida.

#### Scenario: Operações em datas diferentes
- **WHEN** a resposta contém Operações com datas distintas
- **THEN** a menor `dataOperacao` aparece primeiro, independentemente da ordem de inserção no banco

#### Scenario: Operações na mesma data
- **WHEN** a resposta contém Operações na mesma data com diferentes valores de `ordemNoDia`
- **THEN** a menor `ordemNoDia` aparece primeiro

#### Scenario: Desempate técnico entre Operações independentes
- **WHEN** duas Operações de grupos financeiros independentes possuem a mesma data e a mesma `ordemNoDia`
- **THEN** a de menor `id` aparece primeiro somente para estabilizar a representação da lista

### Requirement: Representação completa e fiel da Operação
Cada item SHALL reutilizar o contrato `OperacaoResponse` e SHALL conter `id`, `carteiraId`, `ticker`, `mercado`, `corretoraId`, `tipo`, `quantidade`, `precoUnitario`, `dataOperacao`, `ordemNoDia` e `valorTotal` conforme persistidos. `corretoraId` SHALL permanecer nulo quando a Operação não possuir Corretora. A resposta MUST NOT incluir cotação atual, cotação histórica, preço médio, posição, custo consolidado, resultado realizado, resultado não realizado, rentabilidade, patrimônio ou snapshot.

#### Scenario: Operação com Corretora
- **WHEN** a Operação persistida possui Corretora associada
- **THEN** o response apresenta o identificador persistido da Corretora e todos os demais campos do contrato

#### Scenario: Operação sem Corretora
- **WHEN** a Operação persistida não possui Corretora associada
- **THEN** o response apresenta `corretoraId=null` sem omitir nem rejeitar a Operação

#### Scenario: Preservação dos valores financeiros registrados
- **WHEN** uma Operação é retornada por qualquer consulta desta capability
- **THEN** `quantidade`, `precoUnitario` e `valorTotal` correspondem exatamente aos valores persistidos

### Requirement: Consultas sem recálculo ou efeitos colaterais
As consultas SHALL usar exclusivamente os dados e relacionamentos persistidos e MUST NOT recalcular `valorTotal`, posição, preço médio ou qualquer outro indicador financeiro. Elas MUST NOT normalizar novamente o ticker, modificar `ordemNoDia`, usar relógio, adquirir lock de escrita, persistir dados, atualizar cotação ou chamar BRAPI, Alpha Vantage, BrasilAPI, ViaCEP ou qualquer outro provider externo.

#### Scenario: Consulta preserva o estado
- **WHEN** qualquer endpoint desta capability é chamado uma ou mais vezes
- **THEN** Operações, Carteiras, Ações e Corretoras permanecem inalteradas e nenhuma escrita é executada

#### Scenario: Consulta independente de integrações
- **WHEN** os providers externos estão indisponíveis e o cliente consulta Operações existentes ou uma lista vazia
- **THEN** a resposta é determinada somente pelo banco de dados, sem chamada externa

#### Scenario: Consulta não executa replay
- **WHEN** o histórico de uma Carteira é consultado
- **THEN** o sistema devolve a sequência persistida sem derivar saldo, validar novamente VENDA ou executar replay de posição

### Requirement: Compatibilidade com os contratos e o schema existentes
A capability SHALL preservar integralmente `POST /operacoes`, a proteção de `DELETE /carteiras/{id}` quando houver Operações e os endpoints existentes de Carteira, Ação e Corretora. As consultas SHALL operar sobre o modelo vigente e MUST NOT exigir alteração de entidade, schema, Liquibase, dependências, configurações ou `spring.jpa.hibernate.ddl-auto=validate`.

#### Scenario: Cadastro de Operação preservado
- **WHEN** as consultas de Operação são disponibilizadas
- **THEN** `POST /operacoes` mantém seu request, validações, atomicidade, resposta `201 Created` e `Location` vigentes

#### Scenario: Exclusão protegida preservada
- **WHEN** uma Carteira possui ao menos uma Operação e sua exclusão é solicitada
- **THEN** o contrato vigente continua respondendo `409 Conflict` com `CARTEIRA_POSSUI_OPERACOES` sem remover o histórico

#### Scenario: Inicialização sem migration nova
- **WHEN** PostgreSQL ou H2 inicia com o changelog já promovido para Operações
- **THEN** Liquibase e Hibernate continuam validando o mesmo schema sem changeSet adicional para esta capability
