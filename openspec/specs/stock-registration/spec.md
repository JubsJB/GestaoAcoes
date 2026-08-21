# Stock Registration Specification

## Purpose

Definir o contrato observável para cadastrar uma ação brasileira ou americana somente depois de validar seu ticker e obter os dados mínimos de mercado no provedor externo correspondente.

## Requirements

### Requirement: Contrato de entrada do cadastro
O sistema SHALL disponibilizar `POST /acoes` e SHALL aceitar como dados de negócio fornecidos pelo cliente somente `ticker` e `mercado`, sendo `mercado` limitado a `BRASIL` ou `EUA`. Nome da empresa, moeda, cotação e data/hora da cotação SHALL NOT ser aceitos como valores definidos ou sobrescritos pelo cliente.

#### Scenario: Solicitação de ação brasileira
- **WHEN** o cliente envia ticker não vazio e `mercado=BRASIL`
- **THEN** o sistema inicia o fluxo de cadastro brasileiro sem aceitar dados cadastrais ou de cotação fornecidos pelo cliente

#### Scenario: Solicitação de ação americana
- **WHEN** o cliente envia ticker não vazio e `mercado=EUA`
- **THEN** o sistema inicia o fluxo de cadastro americano sem aceitar dados cadastrais ou de cotação fornecidos pelo cliente

#### Scenario: Mercado ausente ou não suportado
- **WHEN** o cliente omite o mercado ou informa valor diferente de `BRASIL` e `EUA`
- **THEN** o sistema responde `400 Bad Request` com erro padronizado e não consulta provedor externo nem persiste Ação

#### Scenario: Tentativa de sobrescrever dados externos
- **WHEN** o cliente fornece nome da empresa, moeda, cotação, data/hora da cotação ou outra propriedade não admitida pelo contrato
- **THEN** o sistema responde `400 Bad Request` com erro padronizado e não persiste Ação

### Requirement: Normalização do ticker
O sistema SHALL remover espaços nas extremidades e converter o ticker para letras maiúsculas usando regra independente de locale antes da validação, consulta e persistência. Nesta primeira fatia, o sistema SHALL preservar os caracteres internos informados e SHALL NOT acrescentar ou remover sufixos específicos de bolsa.

#### Scenario: Ticker com espaços e letras minúsculas
- **WHEN** o cliente envia `" petr4 "` ou `" aapl "`
- **THEN** o sistema usa respectivamente `PETR4` ou `AAPL` em todas as verificações e na identidade persistida

#### Scenario: Ticker vazio depois da normalização
- **WHEN** o ticker é nulo, vazio ou contém somente espaços
- **THEN** o sistema responde `400 Bad Request` com erro padronizado, sem chamada externa e sem persistência

### Requirement: Seleção do provedor e da moeda pelo mercado
O sistema SHALL consultar exclusivamente a BRAPI para `BRASIL` e exclusivamente a Alpha Vantage para `EUA`. A moeda SHALL ser derivada do mercado como `BRASIL` para `BRL` e `EUA` para `USD`, e a resposta do provedor SHALL ser rejeitada se declarar moeda incompatível.

#### Scenario: Mercado brasileiro
- **WHEN** o cadastro válido informa `mercado=BRASIL`
- **THEN** o sistema consulta somente a BRAPI e prepara a Ação com `mercado=BRASIL` e `moeda=BRL`

#### Scenario: Mercado americano
- **WHEN** o cadastro válido informa `mercado=EUA`
- **THEN** o sistema consulta somente a Alpha Vantage e prepara a Ação com `mercado=EUA` e `moeda=USD`

#### Scenario: Moeda externa incompatível
- **WHEN** o provedor retorna moeda diferente da moeda esperada para o mercado
- **THEN** o sistema responde com erro padronizado de resposta externa inválida e não persiste Ação

### Requirement: Validação externa da existência do ticker
O sistema SHALL concluir o cadastro somente quando o provedor correspondente confirmar a existência do ticker normalizado no mercado solicitado. Resultado aproximado da busca SHALL NOT ser aceito como confirmação de um ticker diferente.

#### Scenario: Ticker brasileiro existente
- **WHEN** a BRAPI confirma o ticker solicitado e fornece seus dados obrigatórios
- **THEN** o sistema considera o ticker válido para `BRASIL`

#### Scenario: Ticker americano exato e pertencente ao mercado dos EUA
- **WHEN** a Alpha Vantage devolve correspondência exata do símbolo, identifica o mercado dos Estados Unidos e fornece os dados obrigatórios
- **THEN** o sistema considera o ticker válido para `EUA`

#### Scenario: Somente correspondência aproximada
- **WHEN** a Alpha Vantage devolve resultados de busca, mas nenhum possui símbolo normalizado exatamente igual ao solicitado e mercado dos Estados Unidos
- **THEN** o sistema responde `404 Not Found` com código de ticker inexistente e não consulta nem persiste dados de outro símbolo

#### Scenario: Ticker não encontrado
- **WHEN** o provedor correspondente não encontra o ticker no mercado solicitado
- **THEN** o sistema responde `404 Not Found` com erro padronizado e não persiste Ação

#### Scenario: BRAPI informa substituição por ticker canônico
- **WHEN** a BRAPI informa explicitamente que o ticker solicitado foi renomeado e devolve outro ticker canônico
- **THEN** o sistema usa o ticker canônico normalizado para a verificação final de duplicidade, persistência e resposta

#### Scenario: Busca americana exata com nome utilizável
- **WHEN** a Alpha Vantage confirma símbolo exato e mercado ou região compatível e a busca já fornece nome utilizável
- **THEN** o sistema consulta a última cotação sem executar consulta adicional de dados da empresa

#### Scenario: Busca americana exata sem nome utilizável
- **WHEN** a Alpha Vantage confirma símbolo exato e mercado ou região compatível, mas a busca não fornece nome utilizável
- **THEN** o sistema consulta os dados da empresa para obter o nome e somente prossegue para a cotação quando o nome obrigatório estiver disponível

### Requirement: Obtenção dos dados da Ação e da cotação no cadastro
O sistema SHALL obter do provedor o ticker confirmado, o nome da empresa e a última cotação disponibilizada pelo provider durante o cadastro. O cadastro SHALL falhar sem persistência quando o nome estiver ausente, a cotação estiver ausente, não numérica, igual a zero ou negativa. `cotacaoAtual` SHALL representar a última cotação disponibilizada pelo provider e SHALL NOT ser apresentada como garantia de preço em tempo real.

#### Scenario: Dados externos completos
- **WHEN** o provedor confirma o ticker e devolve nome da empresa e cotação válida
- **THEN** o sistema usa esses valores externos, sem permitir sobrescrita pelo cliente, para preparar a persistência

#### Scenario: Nome da empresa ausente
- **WHEN** o ticker existe, mas o provedor não fornece nome da empresa utilizável
- **THEN** o sistema responde `422 Unprocessable Entity` com código de dados externos incompletos e não persiste Ação

#### Scenario: Cotação indisponível ou inválida
- **WHEN** o ticker existe, mas a cotação está ausente, não é numérica ou é menor ou igual a zero
- **THEN** o sistema responde `422 Unprocessable Entity` com código de cotação indisponível e não persiste Ação

#### Scenario: Último fechamento disponível para ação americana
- **WHEN** o plano configurado da Alpha Vantage fornece como última cotação o fechamento do último pregão
- **THEN** o sistema aceita o valor positivo como `cotacaoAtual` e não afirma que a cotação é em tempo real

#### Scenario: Cotação não representável sem arredondamento
- **WHEN** a cotação positiva não pode ser representada exatamente em `NUMERIC(19,6)`
- **THEN** o sistema responde `422 Unprocessable Entity` com código específico de cotação fora da precisão suportada, sem truncar, arredondar ou persistir o valor

### Requirement: Referência temporal da cotação
O sistema SHALL representar `dataHoraCotacao` como `OffsetDateTime`, armazenado em UTC e serializado em ISO-8601. O sistema SHALL usar preferencialmente o timestamp da própria cotação quando o provider o fornecer de forma utilizável e confiável, normalizando-o para UTC. Quando o provider não fornecer timestamp utilizável, o sistema SHALL usar como fallback o instante UTC em que a aplicação obteve a cotação.

#### Scenario: Provider fornece timestamp confiável da cotação
- **WHEN** o provider retorna timestamp utilizável e associado à cotação aceita no cadastro
- **THEN** o sistema normaliza esse timestamp para UTC e o persiste como `dataHoraCotacao`

#### Scenario: Provider não fornece timestamp utilizável
- **WHEN** o provider omite o timestamp, retorna somente uma data sem horário confiável ou retorna valor inválido
- **THEN** o sistema usa como `dataHoraCotacao` o instante do relógio UTC da aplicação em que a cotação foi obtida

#### Scenario: Valor temporal na resposta
- **WHEN** o cadastro é concluído
- **THEN** a resposta apresenta `dataHoraCotacao` em ISO-8601 com offset UTC representado por `Z` ou `+00:00`

### Requirement: Unicidade por ticker e mercado
O sistema SHALL impedir mais de uma Ação com o mesmo ticker normalizado e mercado, inclusive sob solicitações concorrentes. O mesmo texto de ticker em mercados distintos SHALL constituir identidades distintas.

#### Scenario: Ação duplicada
- **WHEN** já existe Ação com o mesmo ticker normalizado e mercado
- **THEN** o sistema responde `409 Conflict` com código de Ação duplicada e não cria outro registro

#### Scenario: Concorrência no cadastro
- **WHEN** duas solicitações concorrentes tentam persistir o mesmo ticker normalizado e mercado
- **THEN** no máximo uma persistência é concluída e a outra solicitação recebe o mesmo erro padronizado de duplicidade

#### Scenario: Mesmo ticker em mercados diferentes
- **WHEN** não há duplicidade dentro de cada mercado e o mesmo texto de ticker é validado separadamente em `BRASIL` e `EUA`
- **THEN** a unicidade composta não trata os dois registros como duplicados

### Requirement: Persistência e resposta do cadastro
O sistema SHALL persistir `id`, ticker normalizado ou canônico confirmado, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação. O cadastro concluído SHALL responder `201 Created`, retornar o DTO completo persistido e incluir `Location: /acoes/{id}`.
O sistema SHALL armazenar `ticker` em `VARCHAR(30)`, `nome_empresa` em `VARCHAR(255)` e `cotacao_atual` em `NUMERIC(19,6)`. O sistema SHALL NOT persistir `origemCotacao`, pois o provider é determinado pelo mercado.

#### Scenario: Cadastro concluído
- **WHEN** todas as validações locais e externas passam e a persistência é concluída
- **THEN** o sistema responde `201 Created` com todos os campos da Ação e `Location` baseado no identificador gerado

#### Scenario: Falha antes da gravação
- **WHEN** qualquer validação ou integração falha antes da seção de persistência
- **THEN** nenhuma Ação parcial é gravada

### Requirement: Tratamento padronizado de falhas externas
O sistema SHALL aplicar o formato atual de erro com `code` e `details` quando aplicável. Limite de requisições SHALL resultar em `429 Too Many Requests`, resposta externa inválida em `502 Bad Gateway`, indisponibilidade em `503 Service Unavailable` e timeout em `504 Gateway Timeout`, sem persistência parcial.

#### Scenario: Limite informado por status ou payload
- **WHEN** BRAPI ou Alpha Vantage informa limite excedido por status HTTP ou por payload reconhecido
- **THEN** o sistema responde `429 Too Many Requests` e não persiste Ação

#### Scenario: Timeout do provedor
- **WHEN** a consulta ultrapassa o timeout configurado
- **THEN** o sistema responde `504 Gateway Timeout` e não persiste Ação

#### Scenario: Provedor indisponível
- **WHEN** há falha de conexão ou erro de indisponibilidade no provedor
- **THEN** o sistema responde `503 Service Unavailable` e não persiste Ação

#### Scenario: Somente BRAPI sem configuração
- **WHEN** a configuração da BRAPI está ausente e a Alpha Vantage está configurada
- **THEN** uma solicitação `BRASIL` falha claramente ao tentar usar a BRAPI, enquanto solicitações `EUA` continuam disponíveis

#### Scenario: Somente Alpha Vantage sem configuração
- **WHEN** a configuração da Alpha Vantage está ausente e a BRAPI está configurada
- **THEN** uma solicitação `EUA` falha claramente ao tentar usar a Alpha Vantage, enquanto solicitações `BRASIL` continuam disponíveis

#### Scenario: Payload incompatível
- **WHEN** o provedor responde, mas o payload não pode ser interpretado com segurança
- **THEN** o sistema responde `502 Bad Gateway` e não persiste Ação

### Requirement: Schema versionado e validado
O sistema SHALL criar e evoluir a estrutura de Ação pelo changelog Liquibase compartilhado entre PostgreSQL e H2, e o Hibernate SHALL apenas validar o mapeamento com `ddl-auto=validate`. A migração desta capability SHALL criar somente os objetos de Ação e SHALL NOT criar estruturas das funcionalidades fora de escopo.

#### Scenario: Banco sem a tabela de Ação
- **WHEN** a aplicação inicia sobre banco compatível que já recebeu os changeSets anteriores
- **THEN** o Liquibase cria a tabela de Ação e suas constraints antes de o Hibernate validar o mapeamento

#### Scenario: Inicialização dos testes
- **WHEN** os testes de integração iniciam com H2 efêmero
- **THEN** o mesmo changelog master é executado e o Hibernate valida a estrutura sem usar `create`, `update` ou `create-drop`

### Requirement: Listagem das ações persistidas
O sistema SHALL expor `GET /acoes` e SHALL responder `200 OK` com um array contendo todas as ações já persistidas no contrato completo de resposta de Ação. A listagem SHALL ser ordenada de forma determinística por `id` em ordem crescente e SHALL NOT exigir paginação nesta primeira fatia.

#### Scenario: Listagem com ações cadastradas
- **WHEN** existem ações persistidas e o cliente solicita `GET /acoes`
- **THEN** o sistema retorna `200 OK` com todas as ações como DTOs completos, ordenadas por `id` crescente

#### Scenario: Listagem sem ações cadastradas
- **WHEN** não existe nenhuma ação persistida e o cliente solicita `GET /acoes`
- **THEN** o sistema retorna `200 OK` com um array vazio

#### Scenario: Valores persistidos preservados na listagem
- **WHEN** uma ação integra a resposta de `GET /acoes`
- **THEN** o sistema retorna seu ticker, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação conforme persistidos, sem recalcular ou substituir esses valores

### Requirement: Consulta de ação persistida por ID
O sistema SHALL expor `GET /acoes/{id}` para recuperar uma ação já persistida e SHALL devolver seus dados completos no mesmo contrato de resposta utilizado pelo cadastro.

#### Scenario: Consulta por ID existente
- **WHEN** o cliente solicita `GET /acoes/{id}` com o identificador de uma ação persistida
- **THEN** o sistema retorna `200 OK` com o DTO completo da ação correspondente

#### Scenario: Consulta por ID inexistente
- **WHEN** o cliente solicita `GET /acoes/{id}` com um identificador que não corresponde a uma ação persistida
- **THEN** o sistema retorna `404 Not Found` no formato padronizado atual de erros da API

### Requirement: Consultas independentes dos provedores de cotação
As operações `GET /acoes` e `GET /acoes/{id}` SHALL usar exclusivamente os dados persistidos e MUST NOT consultar BRAPI, Alpha Vantage ou qualquer outro serviço externo. As consultas MUST NOT atualizar `cotacaoAtual`, `dataHoraCotacao` ou qualquer outro dado da Ação.

#### Scenario: Listagem sem chamadas externas
- **WHEN** o cliente solicita a listagem de ações, existam ou não registros
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BRAPI, à Alpha Vantage ou a outro serviço externo e sem alteração dos registros

#### Scenario: Consulta individual sem chamadas externas
- **WHEN** o cliente solicita uma ação por ID, exista ela ou não
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BRAPI, à Alpha Vantage ou a outro serviço externo e sem alteração dos registros
