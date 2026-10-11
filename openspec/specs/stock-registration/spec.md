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
- **THEN** o sistema SHALL responder 422 com DADOS_EXTERNOS_INCOMPLETOS sem consultar OVERVIEW ou cotação e sem persistir a Ação

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
A Ação SHALL separar referenciaTipo (INSTANTE, DATA ou DESCONHECIDA), referenciaInstante, referenciaData e cotacaoObtidaEm. INSTANTE SHALL usar OffsetDateTime normalizado para UTC; DATA SHALL usar LocalDate sem inventar hora ou offset. DESCONHECIDA SHALL manter ambas as referências nulas. cotacaoObtidaEm SHALL representar o instante em que a observação de cotação atualmente armazenada foi obtida validamente pela aplicação e MAY ser nula no legado; não representa o maior instante histórico de consulta. dataHoraCotacao SHALL permanecer obrigatório como campo legado de compatibilidade, sem autoridade de comparação ou apresentação da referência real. No cadastro com preço válido e referência ausente, o estado inicial MAY ser DESCONHECIDA; isso MUST NOT fabricar referência de mercado.

A precisão temporal persistível de referenciaInstante e cotacaoObtidaEm SHALL ser de microssegundos: o instante deve ser exatamente representável com até seis casas decimais nos segundos. Casas excedentes exclusivamente zero SHALL ser aceitas sem perda; nanos significativos não representáveis SHALL ser rejeitados com 422 REFERENCIA_TEMPORAL_INVALIDA antes de disponibilizar CotacaoCandidata válida ou persistir estado/histórico. A mesma validação SHALL abranger referências externas e obtenção do Clock, em cadastro e atualização individual/lote. MUST NOT arredondar, truncar ou delegar ao banco a correção da precisão. UTC SHALL preservar o instante; LocalDate não é afetada.

#### Scenario: Precisão temporal exatamente persistível
- **WHEN** referência INSTANTE ou obtenção possui segundos inteiros, até seis casas ou somente zeros após a sexta casa
- **THEN** aceita o mesmo instante em UTC, sem perda ou arredondamento

#### Scenario: Domínio civil de nova candidata
- **WHEN** o provider fornece DATA gregoriana real com ano de exatamente quatro dígitos entre 0001 e 9999, inclusive 0001-01-01 e 9999-12-31
- **THEN** a fronteira CotacaoCandidata SHALL aceitar a referência sem piso 1970, limite futuro relativo à obtenção, Date, timezone ou calendário de mercado
- **AND** calendário impossível, ano zero, negativo ou expandido SHALL causar REFERENCIA_TEMPORAL_INVALIDA, nunca DESCONHECIDA

#### Scenario: Domínio financeiro de INSTANTE da própria candidata
- **WHEN** uma nova candidata fornece INSTANTE com calendário, hora e offset válidos e precisão exata em microssegundos
- **THEN** SHALL exigir instante real maior ou igual a 1970-01-01T00:00:00Z e menor ou igual à cotacaoObtidaEm dessa mesma candidata, com tolerância ZERO e normalização UTC sem perda
- **AND** um microssegundo abaixo do piso ou posterior à obtenção SHALL causar REFERENCIA_TEMPORAL_INVALIDA antes de escrita de Ação/histórico; igualdade e offsets equivalentes SHALL ser aceitos

#### Scenario: Obtenção e legado não herdam limites da referência
- **WHEN** cadastro, PATCH ou lote valida a candidata
- **THEN** cotacaoObtidaEm SHALL pertencer à aquisição capturada pelo Clock backend, exatamente em microssegundos, sem herdar automaticamente o piso financeiro da referência
- **AND** dataHoraCotacao SHALL permanecer legado sem novos limites financeiros ou fallback; GET/mapping/cooldown/reuso MUST NOT fabricar ou reinterpretar obtenção

#### Scenario: Registro aceito pelo contrato anterior
- **WHEN** um registro anteriormente persistido é retornado pela API
- **THEN** os novos limites de aquisição MUST NOT converter, apagar ou reconstruir sua referência, fabricar obtenção ou provocar rejeição silenciosa da leitura
- **AND** a validação estrutural de leitura SHALL ser tratada separadamente da validação financeira de nova candidata, sem saneamento automático

#### Scenario: Precisão temporal não persistível
- **WHEN** referência de mercado ou obtenção do Clock possui nanos significativos além de microssegundos, como .123456789
- **THEN** rejeita com REFERENCIA_TEMPORAL_INVALIDA antes da persistência, sem candidata válida, Ação ou histórico parcial

#### Scenario: Provider fornece timestamp confiável da cotação
- **WHEN** o cadastro recebe instante confiável do preço
- **THEN** persiste INSTANTE em UTC e obtenção separada; mantém dataHoraCotacao legado

#### Scenario: Provider não fornece timestamp utilizável
- **WHEN** o cadastro recebe somente data civil, ou nenhuma referência utilizável
- **THEN** preserva DATA sem hora quando fornecida, ou DESCONHECIDA quando ausente; o relógio preenche somente obtenção e compatibilidade legada

#### Scenario: Valor temporal na resposta
- **WHEN** o cadastro é concluído
- **THEN** o DTO inclui a referência tipada e obtenção além do campo legado ISO-8601 UTC

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
O sistema SHALL persistir `id`, ticker normalizado ou canônico confirmado, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação. A mesma operação transacional SHALL persistir exatamente uma observação histórica inicial com a Ação, a cotação e o timestamp aceitos, reutilizando exatamente `Acao.cotacaoAtual` e `Acao.dataHoraCotacao` sem uma segunda chamada ao provider. O cadastro concluído SHALL responder `201 Created`, retornar o DTO completo persistido e incluir `Location: /acoes/{id}`.
O sistema SHALL armazenar `ticker` em `VARCHAR(30)`, `nome_empresa` em `VARCHAR(255)` e `cotacao_atual` em `NUMERIC(19,6)`. O sistema SHALL NOT persistir `origemCotacao`, pois o provider é determinado pelo mercado.
 A persistência SHALL incluir os novos campos temporais na Ação e na observação inicial com valores correspondentes, preservando a referência real disponível ou DESCONHECIDA e o instante de obtenção separado; nenhuma chamada adicional SHALL ser feita para o histórico.

#### Scenario: Cadastro concluído
- **WHEN** todas as validações locais e externas passam e a persistência é concluída
- **THEN** o sistema responde `201 Created` com todos os campos da Ação e `Location` baseado no identificador gerado
- **AND** existe exatamente uma observação histórica correspondente ao estado inicial
- **AND** nenhuma segunda chamada ao provider é executada para criar essa observação

#### Scenario: Falha antes da gravação
- **WHEN** qualquer validação ou integração falha antes da seção de persistência
- **THEN** nenhuma Ação ou observação histórica parcial é gravada

#### Scenario: Falha na observação inicial
- **WHEN** a Ação seria persistida, mas sua observação histórica falha
- **THEN** toda a transação é revertida e nenhuma Ação parcial permanece

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
O schema SHALL evoluir por novos changeSets Liquibase compartilhados entre PostgreSQL e H2, com ddl-auto=validate. A transição temporal SHALL adicionar campos em Ação e histórico sem editar changeSets anteriores; MUST NOT criar tabelas de posição ou estruturas fora desta evolução. Preço e campo legado SHALL ser preservados.

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

### Requirement: Consulta singular de Ação persistida por ticker e mercado
O sistema SHALL expor exclusivamente `GET /acoes/por-ticker?ticker={ticker}&mercado={mercado}` para consultar uma única Ação persistida usando conjuntamente o ticker e o mercado. Ambos os query parameters SHALL ser obrigatórios no contrato. O ticker SHALL aceitar a representação normalizável vigente, o mercado SHALL ser limitado a `BRASIL` ou `EUA`, e a consulta MUST NOT escolher implicitamente um mercado quando o mesmo ticker existir em ambos. O sistema SHALL NOT expor aliases nem transformar `GET /acoes` em consulta singular.

#### Scenario: Ação brasileira encontrada
- **WHEN** o cliente solicita `GET /acoes/por-ticker` com ticker válido e `mercado=BRASIL` correspondentes a uma Ação persistida
- **THEN** o sistema responde `200 OK` com a Ação brasileira correspondente

#### Scenario: Ação americana encontrada
- **WHEN** o cliente solicita `GET /acoes/por-ticker` com ticker válido e `mercado=EUA` correspondentes a uma Ação persistida
- **THEN** o sistema responde `200 OK` com a Ação americana correspondente

#### Scenario: Mesmo ticker em mercados distintos
- **WHEN** existem Ações com o mesmo ticker normalizado em `BRASIL` e `EUA`
- **THEN** o mercado informado seleciona somente a Ação da combinação correspondente, sem preferência implícita ou retorno arbitrário

#### Scenario: Ticker normalizável
- **WHEN** o ticker possui espaços nas extremidades ou letras minúsculas, mas é válido segundo a política vigente
- **THEN** o sistema aplica `trim`, converte para maiúsculas de forma independente de locale e consulta o valor normalizado preservando os caracteres internos

#### Scenario: Ticker inválido
- **WHEN** o ticker é nulo, vazio, contém somente espaços ou excede o tamanho máximo vigente após normalização
- **THEN** o sistema responde `400 Bad Request` com código `TICKER_INVALIDO` sem consultar dados persistidos

#### Scenario: Mercado ausente
- **WHEN** o query parameter `mercado` está ausente
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e não consulta dados persistidos

#### Scenario: Mercado inválido
- **WHEN** o query parameter `mercado` não corresponde a `BRASIL` nem `EUA`
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` pelo tratamento centralizado vigente e não consulta dados persistidos

#### Scenario: Combinação inexistente
- **WHEN** ticker e mercado são válidos, mas nenhuma Ação possui a combinação normalizada
- **THEN** o sistema responde `404 Not Found` no formato centralizado vigente de Ação não encontrada

#### Scenario: Ausência de aliases
- **WHEN** o cliente tenta usar `/acoes/ticker/{ticker}`, `/acoes` com ticker e mercado como lookup singular ou `/acoes/por-ticker/{ticker}/mercados/{mercado}`
- **THEN** o sistema não trata essas formas como contratos da consulta singular por ticker e mercado

### Requirement: Resposta equivalente à consulta por ID
A consulta por ticker e mercado SHALL reutilizar integralmente o contrato completo de resposta usado por `GET /acoes/{id}` e MUST NOT criar representação reduzida nem expor a entidade de persistência.

#### Scenario: Representação da Ação encontrada
- **WHEN** uma Ação é encontrada pelo ticker normalizado e mercado
- **THEN** a resposta contém id, ticker, nome da empresa, mercado, moeda, cotação atual e data/hora da cotação conforme persistidos

### Requirement: Consulta local e read-only
A consulta por ticker e mercado SHALL usar exclusivamente dados persistidos, SHALL NOT modificar a Ação e MUST NOT consultar BRAPI, Alpha Vantage ou qualquer outro provider. A consulta MUST NOT revalidar ticker externamente nem atualizar nome, moeda, cotação ou data/hora da cotação.

#### Scenario: Consulta encontrada sem integração externa
- **WHEN** a combinação corresponde a uma Ação persistida
- **THEN** a resposta é produzida somente pelo banco de dados, sem chamada externa e sem escrita

#### Scenario: Consulta inexistente sem integração externa
- **WHEN** a combinação não corresponde a uma Ação persistida
- **THEN** o `404 Not Found` é determinado somente pelo banco de dados, sem chamada externa e sem escrita

### Requirement: Compatibilidade dos contratos existentes
A consulta por ticker e mercado SHALL preservar sem alteração comportamental `POST /acoes`, `GET /acoes`, `GET /acoes/{id}` e `PATCH /acoes/{id}/cotacao` e SHALL NOT introduzir filtros genéricos ou paginação na listagem.

#### Scenario: APIs existentes após a ampliação
- **WHEN** a nova consulta singular estiver disponível
- **THEN** cadastro, listagem, consulta por ID e atualização de cotação mantêm seus contratos, status e representações vigentes

### Requirement: Consultas independentes dos provedores de cotação
As operações `GET /acoes` e `GET /acoes/{id}` SHALL usar exclusivamente os dados persistidos e MUST NOT consultar BRAPI, Alpha Vantage ou qualquer outro serviço externo. As consultas MUST NOT atualizar `cotacaoAtual`, `dataHoraCotacao` ou qualquer outro dado da Ação.

#### Scenario: Listagem sem chamadas externas
- **WHEN** o cliente solicita a listagem de ações, existam ou não registros
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BRAPI, à Alpha Vantage ou a outro serviço externo e sem alteração dos registros

#### Scenario: Consulta individual sem chamadas externas
- **WHEN** o cliente solicita uma ação por ID, exista ela ou não
- **THEN** a resposta é determinada pelo banco de dados sem chamada à BRAPI, à Alpha Vantage ou a outro serviço externo e sem alteração dos registros

### Requirement: Contrato da atualização de cotação
O sistema SHALL expor `PATCH /acoes/{id}/cotacao` como operação dedicada para atualizar a última cotação conhecida de uma Ação persistida. A operação SHALL NOT aceitar cotação, timestamp, ticker, mercado, moeda, nome da empresa ou qualquer outro dado de negócio no corpo da requisição. Quando concluída, SHALL responder `200 OK` com o `AcaoResponse` completo que representa o estado persistido ao final da solicitação e SHALL NOT incluir o header `Location`.

#### Scenario: Atualização solicitada sem corpo
- **WHEN** o cliente solicita `PATCH /acoes/{id}/cotacao` sem corpo para uma Ação existente e uma nova cotação aplicável é obtida
- **THEN** o sistema responde `200 OK` com o DTO completo da Ação no estado persistido ao final da operação

#### Scenario: Tentativa de informar a cotação manualmente
- **WHEN** o cliente envia corpo na solicitação de atualização, inclusive com cotação ou timestamp
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO`, sem consultar provider e sem alterar a Ação

### Requirement: Seleção da Ação e do provider pelo estado persistido
O sistema SHALL localizar a Ação pelo ID antes de qualquer chamada externa e SHALL usar exclusivamente o ticker e o mercado persistidos para solicitar a nova cotação. O sistema SHALL consultar BRAPI quando o mercado persistido for `BRASIL` e Alpha Vantage quando for `EUA`, respeitando reutilizacao e cooldown de cotacao EUA definidos em alpha-vantage-consumption, sem permitir seleção do provider pelo cliente.

#### Scenario: Ação brasileira persistida
- **WHEN** a Ação encontrada possui `mercado=BRASIL`
- **THEN** o sistema consulta somente a BRAPI usando o ticker persistido

#### Scenario: Ação americana persistida
- **WHEN** a Ação encontrada possui `mercado=EUA`
- **THEN** o sistema reutiliza a cotacao persistida recente ou respeita cooldown ativo; quando uma consulta e necessaria, consulta somente a Alpha Vantage usando o ticker persistido e a identidade ja validada

#### Scenario: Ação inexistente
- **WHEN** o ID informado não corresponde a uma Ação persistida
- **THEN** o sistema responde `404 Not Found` no formato padronizado atual e não consulta provider

### Requirement: Validação da nova cotação e preservação da Ação
A atualização SHALL exigir identidade compatível, preço presente, numérico, positivo e exatamente representável em NUMERIC(19,6), e referência de mercado confiável. Uma candidata aplicável SHALL modificar somente preço e metadados temporais de cotação, preservando id, ticker, nomeEmpresa, mercado e moeda. Cotação ou identidade inválida SHALL preservar o estado. Nome externo diferente MUST NOT modificar a empresa persistida.

#### Scenario: Nova cotação válida
- **WHEN** a candidata possui identidade compatível, preço válido e referência inicial confiável ou posterior do mesmo tipo
- **THEN** persiste preço e metadados temporais sem modificar os campos cadastrais

#### Scenario: Nome externo diferente
- **WHEN** o provider devolve nome da empresa diferente do nome persistido durante uma atualização válida
- **THEN** o sistema preserva `nomeEmpresa` e usa os dados externos somente para validar e obter a cotação

#### Scenario: Cotação ausente, não numérica, zero ou negativa
- **WHEN** o provider não fornece uma cotação positiva utilizável
- **THEN** o sistema responde `422 Unprocessable Entity` com código `COTACAO_INDISPONIVEL` e preserva integralmente a Ação

#### Scenario: Cotação fora da precisão suportada
- **WHEN** a nova cotação não pode ser representada exatamente em `NUMERIC(19,6)`
- **THEN** o sistema responde `422 Unprocessable Entity` com código `COTACAO_FORA_DA_PRECISAO`, sem truncar, arredondar ou persistir o valor

### Requirement: Semântica temporal e cotação numericamente igual
Atualização individual e lote SHALL compartilhar uma regra. Baseline DESCONHECIDA e candidata confiável SHALL estabelecer baseline e classificar ATUALIZADA com motivo REFERENCIA_INICIAL_ESTABELECIDA, sem afirmar ordem contra o legado desconhecido. Referência posterior do mesmo tipo SHALL classificar ATUALIZADA mesmo com preço igual. Igual e mesmo preço retornados por NOVA CONSULTA REAL válida ao provider SHALL classificar SEM_NOVA_COTACAO sem alterar cotacaoAtual, referência ou dataHoraCotacao legado e sem histórico; cotacaoObtidaEm SHALL avançar para o instante dessa obtenção somente se posterior ao persistido, ou ser preenchida se ainda nula. Na mesma observação (referência e preço iguais), instante igual ou anterior MUST NOT regredir obtenção. Baseline inicial ou referência posterior aceita SHALL substituir a observação inteira com cotacaoAtual, tipo, referência e cotacaoObtidaEm própria da candidata, mesmo que esta obtenção seja anterior à da observação substituída. MUST NOT usar max entre obtenções de observações diferentes. cotacaoObtidaEm MUST NOT ordenar referências de mercado ou desempatar tipos incompatíveis. A comparação SHALL ocorrer com estado relido sob lock, inclusive para execução atrasada. Reuso e cooldown MUST NOT atualizar cotacaoObtidaEm, preço/referência ou criar histórico: não constituem nova obtenção válida. Anterior SHALL classificar SEM_NOVA_COTACAO preservando preço, referência e obtenção associada. Igual e preço diferente SHALL classificar FALHA por conflito temporal. DATA versus INSTANTE SHALL classificar FALHA com motivo TIPO_REFERENCIA_INCOMPATIVEL sem conversões artificiais e sem alterar cotacaoObtidaEm. Referência ausente ou inválida na atualização SHALL classificar FALHA de validação temporal, inclusive para baseline desconhecida. No PATCH individual, falhas temporais SHALL usar erro padronizado 422; no lote SHALL ser resultado por ativo. dataHoraCotacao MUST NOT decidir ordenação; nas escritas aceitas SHALL manter compatibilidade com instante de mercado quando INSTANTE, ou obtenção quando DATA.

#### Scenario: Timestamp confiável do provider
- **WHEN** o provider retorna instante confiável
- **THEN** compara INSTANTE em UTC com baseline do mesmo tipo

#### Scenario: Referência posterior obtida antes da observação substituída
- **WHEN** o estado possui DATA 2026-10-01, preço 100 e obtenção 15:05Z, e chega candidata DATA 2026-10-02, preço 102 e obtenção 15:04Z
- **THEN** aceita a observação integral da candidata, com DATA 2026-10-02, preço 102 e obtenção 15:04Z; MUST NOT aplicar max entre as obtenções de observações diferentes
- **AND** a mesma política vale para INSTANTE e baseline inicial; somente referência/preço iguais exigem monotonicidade local de obtenção

#### Scenario: Fallback temporal da aplicação
- **WHEN** a candidata de atualização não fornece referência temporal confiável
- **THEN** preserva a Ação e retorna FALHA temporal; nenhum relógio substitui a referência

#### Scenario: Mesmo valor com referência temporal posterior
- **WHEN** a candidata possui mesmo preço e referência posterior do mesmo tipo
- **THEN** persiste a referência posterior e obtenção, cria histórico e retorna sucesso individual ou ATUALIZADA no lote

#### Scenario: Resultado sem referência temporal posterior
- **WHEN** a candidata possui referência igual com mesmo preço ou referência anterior
- **THEN** preserva preço/referência e retorna sucesso individual ou SEM_NOVA_COTACAO no lote, sem histórico; somente nova consulta real válida de referência/preço iguais avança obtenção posterior, enquanto referência anterior preserva também a obtenção

### Requirement: Divergência de ticker canônico
O sistema MUST NOT alterar automaticamente o ticker persistido durante a atualização de cotação. Quando o provider indicar explicitamente renomeação e devolver ticker canônico diferente, o sistema SHALL responder `409 Conflict` com código `TICKER_CANONICO_DIVERGENTE`, informar os tickers persistido e retornado em `details` e preservar integralmente a Ação para que uma futura funcionalidade de migração de identidade trate o caso.

#### Scenario: BRAPI confirma o ticker persistido
- **WHEN** a BRAPI confirma o mesmo ticker da Ação brasileira
- **THEN** o fluxo normal de validação da cotação pode prosseguir

#### Scenario: BRAPI informa ticker canônico diferente
- **WHEN** a BRAPI indica explicitamente que o ticker persistido foi renomeado e devolve ticker canônico diferente
- **THEN** o sistema responde `409 Conflict` com os dois tickers em `details`, sem atualizar ticker, nome, cotação ou timestamp

#### Scenario: Provider devolve ticker incompatível sem renomeação explícita
- **WHEN** o provider devolve ticker diferente sem sinalização explícita de renomeação admitida
- **THEN** o sistema responde `502 Bad Gateway` com código `RESPOSTA_EXTERNA_INVALIDA` e preserva a Ação

### Requirement: Preservação da última cotação em falhas
Quando uma Ação existente não puder obter uma nova cotação aplicável, o sistema SHALL preservar a última cotação válida e SHALL informar no erro padronizado, quando aplicável, `acaoId`, `cotacaoPreservada=true`, `ultimaCotacaoValida` e `dataHoraUltimaCotacao`. O erro SHALL manter o status e o código correspondentes à causa original.

#### Scenario: Ticker deixou de ser encontrado
- **WHEN** o provider correspondente não encontra mais o ticker persistido
- **THEN** o sistema responde `404 Not Found` com código `TICKER_INEXISTENTE`, inclui a última cotação em `details` e não altera o registro

#### Scenario: Limite de requisições excedido
- **WHEN** o provider informa rate limit por status ou payload reconhecido
- **THEN** o sistema responde `429 Too Many Requests` com código `LIMITE_REQUISICOES_EXCEDIDO`, informa a preservação e não altera o registro

#### Scenario: Provider indisponível ou sem configuração
- **WHEN** o provider selecionado está indisponível, não configurado ou apresenta falha de conexão que não seja timeout
- **THEN** o sistema responde `503 Service Unavailable` com código `SERVICO_EXTERNO_INDISPONIVEL`, informa a preservação e não altera o registro

#### Scenario: Timeout do provider
- **WHEN** a consulta externa excede o timeout configurado
- **THEN** o sistema responde `504 Gateway Timeout` com código `SERVICO_EXTERNO_TIMEOUT`, informa a preservação e não altera o registro

#### Scenario: Resposta inválida ou dados externos incompletos
- **WHEN** o provider responde com payload incompatível, identidade ou moeda incompatível, ou omite dado obrigatório do contrato de cotação
- **THEN** o sistema responde com o status e código padronizados de resposta externa inválida ou dados externos incompletos, informa a preservação e não altera o registro

### Requirement: Concorrência e persistência final consistente
Providers SHALL ser chamados fora da transação de persistência. A seção final SHALL adquirir lock da Ação, reler o estado e aplicar a comparação compartilhada, inclusive lote versus lote e lote versus individual. Candidata inicial confiável ou posterior aceita SHALL persistir estado e exatamente uma observação atomicamente. Igual, anterior, falha ou tipo incompatível MUST NOT criar histórico nem substituir informação mais recente.

#### Scenario: Duas atualizações concorrentes com timestamps diferentes
- **WHEN** duas candidatas do mesmo tipo confiável concorrem e a antiga termina depois da nova
- **THEN** o estado e histórico aceito preservam a maior referência; a resposta antiga não regride o estado

#### Scenario: Falha antes da seção final
- **WHEN** a busca inicial, a chamada externa ou a validação falha
- **THEN** nenhuma transação de escrita altera a Ação ou seu histórico

#### Scenario: Persistência concluída com histórico atômico
- **WHEN** uma baseline inicial confiável ou referência posterior é aceita
- **THEN** preço, metadados temporais e observação correspondente são confirmados na mesma transação

### Requirement: Comparações temporais e primeira baseline verificáveis
O sistema SHALL decidir sobre o estado relido sob lock e preservar o estado em conflitos ou dados inválidos.

#### Scenario: Baseline legada
- **WHEN** o preço legado é 250 com referência DESCONHECIDA e chega preço 252 com DATA confiável
- **THEN** estabelece a primeira baseline, cria observação e classifica ATUALIZADA sem inferir data do legado

#### Scenario: Conflito na mesma referência
- **WHEN** a mesma DATA ou INSTANTE chega com preço diferente
- **THEN** classifica FALHA e preserva preço, referência e obtenção

#### Scenario: Tipos não comparáveis nos dois sentidos
- **WHEN** baseline DATA recebe INSTANTE ou baseline INSTANTE recebe DATA
- **THEN** classifica FALHA TIPO_REFERENCIA_INCOMPATIVEL; preserva preço, referência e obtenção; não converte nem reduz a precisão

#### Scenario: Ausência temporal
- **WHEN** uma atualização recebe preço válido sem referência confiável
- **THEN** classifica FALHA e preserva inclusive baseline desconhecida

#### Scenario: Nova obtenção da mesma observação
- **WHEN** nova consulta real válida retorna mesma DATA ou INSTANTE e mesmo preço, com obtenção posterior à persistida
- **THEN** classifica SEM_NOVA_COTACAO, avança somente cotacaoObtidaEm e preserva preço, referência, legado e histórico

#### Scenario: Obtenção igual ou anterior
- **WHEN** nova consulta real válida retorna mesma referência e preço com obtenção igual ou anterior à persistida
- **THEN** classifica SEM_NOVA_COTACAO e preserva inclusive cotacaoObtidaEm, sem histórico

#### Scenario: Obtenção concorrente atrasada
- **WHEN** duas consultas reais válidas da mesma referência/preço concorrem e a obtenção mais antiga entra na seção final depois da mais recente
- **THEN** compara sob lock e nunca regride cotacaoObtidaEm nem cria histórico ou conta ATUALIZADA

### Requirement: Transição temporal aditiva e contratos preservados
A migration SHALL adicionar campos sem modificar changeSets anteriores, preservar cotacaoAtual e dataHoraCotacao, e marcar legado sem evidência como DESCONHECIDA com referências e obtenção nulas. DTOs SHALL adicionar referenciaMercadoCotacao {tipo, instante, data} e cotacaoObtidaEm sem remover campos públicos. A cotação SHALL continuar global à Ação. Atualizar cotação MUST NOT alterar Operacao, precoUnitario, quantidade, preço médio, custo histórico, resultado realizado, replay ou histórico de operações; snapshots anteriores SHALL permanecer imutáveis. Nenhuma classificação automática de defasagem SHALL ser introduzida.

#### Scenario: Legado preservado
- **WHEN** a migration encontra timestamp de origem não comprovada
- **THEN** mantém preço e campo legado sem reinterpretar como mercado ou consulta comprovada

#### Scenario: Efeito em outra Carteira
- **WHEN** Carteira A atualiza AAPL também presente na Carteira B
- **THEN** a próxima leitura de B usa a cotação global nova com suas próprias operações

#### Scenario: Separação financeira
- **WHEN** a cotação é atualizada
- **THEN** somente avaliações dependentes da cotação e futuros snapshots manuais podem refletir a mudança
