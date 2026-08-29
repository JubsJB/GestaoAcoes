## ADDED Requirements

### Requirement: Consulta de Corretora persistida por CNPJ
O sistema SHALL expor `GET /corretoras/por-cnpj?cnpj={cnpj}` para consultar uma única Corretora pelo CNPJ e SHALL NOT expor alias por path nem alterar `GET /corretoras` para produzir resposta singular. A consulta SHALL aceitar CNPJ válido com máscara ou somente 14 dígitos, SHALL aplicar a mesma normalização e validação algorítmica do cadastro e SHALL buscar exclusivamente o valor normalizado persistido.

#### Scenario: Consulta com CNPJ sem máscara
- **WHEN** o cliente solicita `GET /corretoras/por-cnpj` com o query parameter `cnpj` contendo um CNPJ válido com 14 dígitos e existe um registro com esse CNPJ normalizado
- **THEN** o sistema responde `200 OK` com o `CorretoraResponse` completo do registro correspondente

#### Scenario: Consulta com CNPJ mascarado
- **WHEN** o cliente solicita `GET /corretoras/por-cnpj` com o query parameter `cnpj` contendo um CNPJ válido no formato `NN.NNN.NNN/NNNN-NN` e existe um registro com esse CNPJ normalizado
- **THEN** o sistema normaliza a entrada para 14 dígitos e responde `200 OK` com o mesmo `CorretoraResponse` que seria obtido pelo valor sem máscara

#### Scenario: CNPJ inválido
- **WHEN** o CNPJ estiver ausente, vazio, possuir formato ou quantidade de dígitos inválidos, contiver caracteres não aceitos ou falhar na validação dos dígitos verificadores
- **THEN** o sistema responde `400 Bad Request` com código `CNPJ_INVALIDO` e não consulta nem altera dados persistidos

#### Scenario: Ausência de alias por path
- **WHEN** o cliente tenta consultar pela rota não aprovada `GET /corretoras/cnpj/{cnpj}`
- **THEN** o sistema não trata essa rota como contrato de consulta por CNPJ

#### Scenario: Listagem com query parameter especulativo
- **WHEN** o cliente adiciona `cnpj` a `GET /corretoras` em vez de usar `/corretoras/por-cnpj`
- **THEN** o sistema não transforma a listagem em uma resposta singular de consulta por CNPJ

#### Scenario: CNPJ válido sem Corretora correspondente
- **WHEN** o CNPJ informado é válido, mas nenhuma Corretora possui o valor normalizado
- **THEN** o sistema responde `404 Not Found` no formato centralizado vigente de Corretora não encontrada

### Requirement: Resposta equivalente à consulta por ID
A consulta por CNPJ SHALL reutilizar integralmente o contrato `CorretoraResponse` vigente e MUST NOT criar uma representação reduzida ou expor a entidade de persistência.

#### Scenario: Corretora encontrada por CNPJ
- **WHEN** uma Corretora é encontrada pelo CNPJ normalizado
- **THEN** a resposta contém os mesmos campos cadastrais, opcionais, estado de validação e data de cadastro expostos por `GET /corretoras/{id}`

### Requirement: Consulta local, read-only e sem revalidação
A consulta por CNPJ SHALL usar somente os dados persistidos, SHALL NOT modificar a Corretora e MUST NOT consultar BrasilAPI, ViaCEP ou qualquer outro provider. A consulta MUST NOT revalidar situação cadastral, CEP ou atuação no mercado financeiro.

#### Scenario: Consulta existente independente dos providers
- **WHEN** o cliente consulta um CNPJ válido que corresponde a uma Corretora persistida
- **THEN** a resposta é produzida somente pelo banco de dados, sem chamada externa e sem escrita

#### Scenario: Consulta inexistente independente dos providers
- **WHEN** o cliente consulta um CNPJ válido sem Corretora correspondente
- **THEN** o `404 Not Found` é determinado somente pelo banco de dados, sem chamada externa e sem escrita

### Requirement: Compatibilidade dos contratos existentes
A consulta por CNPJ SHALL preservar sem alteração comportamental `POST /corretoras`, `GET /corretoras` e `GET /corretoras/{id}` e SHALL NOT introduzir paginação ou filtros genéricos na listagem.

#### Scenario: APIs existentes após a ampliação
- **WHEN** a consulta por CNPJ estiver disponível
- **THEN** cadastro, listagem e consulta por ID mantêm seus contratos, status e representações vigentes
