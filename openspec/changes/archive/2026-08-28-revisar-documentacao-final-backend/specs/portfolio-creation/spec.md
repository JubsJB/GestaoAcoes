## Purpose

Definir o contrato para criar e persistir uma Carteira mínima, independente de usuário e preparada para receber operações sem antecipar associações ou regras financeiras.

## ADDED Requirements

### Requirement: Contrato de entrada da criação de Carteira
O sistema SHALL expor `POST /carteiras` e SHALL aceitar como único dado de negócio fornecido pelo cliente o campo `nome`. O cliente MUST NOT fornecer `id`, `dataCriacao`, operações, ações, posições, usuário ou qualquer outro campo fora do contrato de criação.

#### Scenario: Solicitação contendo somente o nome admitido
- **WHEN** o cliente envia `POST /carteiras` com um `nome` que atende à política de validação aprovada
- **THEN** o sistema inicia a criação sem exigir ou aceitar outros dados da Carteira

#### Scenario: Tentativa de fornecer campo controlado pela aplicação
- **WHEN** o cliente inclui `id`, `dataCriacao` ou outra propriedade não admitida no request
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e não persiste a Carteira

### Requirement: Validação e representação do nome
O sistema SHALL exigir `nome`, SHALL remover somente os espaços das extremidades antes da validação e persistência e SHALL rejeitar valor nulo, vazio ou composto somente por espaços. O nome normalizado MUST possuir no máximo 255 caracteres. O sistema SHALL preservar espaços internos, acentos e caixa, MUST NOT aplicar qualquer outra transformação e SHALL devolver o mesmo valor efetivamente persistido. Nomes duplicados SHALL ser permitidos, MUST NOT provocar consulta ou rejeição por duplicidade e MUST NOT possuir constraint única; `id` SHALL ser a identidade estável da Carteira.

#### Scenario: Nome válido com espaços nas extremidades
- **WHEN** o cliente informa um nome válido com espaços antes ou depois do conteúdo
- **THEN** o sistema remove somente esses espaços externos e persiste o conteúdo interno, acentos e caixa sem outras transformações

#### Scenario: Nome ausente, vazio ou somente com espaços
- **WHEN** o cliente omite `nome` ou informa valor nulo, vazio ou composto somente por espaços
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO`, informa o campo inválido em `details` e não persiste a Carteira

#### Scenario: Nome acima do tamanho máximo
- **WHEN** o nome normalizado excede 255 caracteres
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e não persiste a Carteira

#### Scenario: Nomes iguais permitidos
- **WHEN** já existe uma Carteira com o mesmo nome normalizado e uma nova solicitação válida usa esse nome
- **THEN** o sistema cria outro recurso com identificador próprio, sem tratar o nome como chave única

### Requirement: Modelo persistido da Carteira
O sistema SHALL persistir `id`, `nome` e `dataCriacao` para cada Carteira. O identificador SHALL ser gerado pelo banco, e a criação da Carteira MUST NOT exigir associação com usuário, ação, corretora, operação, posição, snapshot ou histórico.

#### Scenario: Persistência da Carteira mínima
- **WHEN** uma solicitação válida é concluída
- **THEN** o banco contém uma nova Carteira com identificador gerado, nome validado e data de criação gerada pela aplicação

#### Scenario: Carteira independente de usuário no MVP
- **WHEN** uma Carteira é criada
- **THEN** sua criação não exige autenticação, usuário ou vínculo de propriedade

### Requirement: Geração da data de criação
O sistema SHALL representar `dataCriacao` como `OffsetDateTime`, gerar o valor pela aplicação no momento da persistência, normalizá-lo para UTC e serializá-lo em ISO-8601. `dataCriacao` MUST NOT ser aceita do cliente.

#### Scenario: Data gerada pela aplicação
- **WHEN** a Carteira está pronta para ser persistida
- **THEN** a aplicação usa seu relógio UTC para gerar `dataCriacao` e persiste o instante com offset UTC

#### Scenario: Data devolvida ao cliente
- **WHEN** a criação é concluída
- **THEN** a resposta apresenta `dataCriacao` em ISO-8601 com UTC representado por `Z` ou `+00:00`

### Requirement: Resposta da criação concluída
O sistema SHALL responder à criação concluída com `201 Created`, SHALL incluir `Location: /carteiras/{id}` e SHALL devolver `CarteiraResponse` completo com `id`, `nome` e `dataCriacao`, sem expor a entidade de persistência diretamente.

#### Scenario: Carteira criada com sucesso
- **WHEN** uma Carteira válida é persistida
- **THEN** o sistema responde `201 Created`, inclui `Location: /carteiras/{id}` e retorna corpo com `id`, `nome` e `dataCriacao`

### Requirement: Schema versionado e validado
O schema necessário à Carteira SHALL ser gerido pelo changelog Liquibase compartilhado entre PostgreSQL e H2. O Hibernate SHALL apenas validar o mapeamento com `ddl-auto=validate`, e a estrutura da Carteira MUST conter somente as colunas e constraints necessárias ao contrato persistido.

#### Scenario: Inicialização do banco
- **WHEN** a aplicação inicia sobre um banco compatível
- **THEN** o Liquibase garante a estrutura de Carteira antes de o Hibernate validar o mapeamento

#### Scenario: Inicialização dos testes
- **WHEN** os testes de integração iniciam com o H2 efêmero
- **THEN** o mesmo changelog master cria a tabela Carteira e o Hibernate valida a estrutura sem usar `create`, `update` ou `create-drop`

### Requirement: Tratamento padronizado e atomicidade
O sistema SHALL usar o formato de erro atual da API para rejeições do request e SHALL persistir a Carteira de forma atômica. Uma falha de validação ou persistência MUST NOT criar registro parcial.

#### Scenario: Falha de validação
- **WHEN** o request viola o contrato ou a política aprovada para o nome
- **THEN** o sistema responde no formato `StandardError`, com `code` e `details` quando aplicáveis, e não cria Carteira

#### Scenario: Falha durante a persistência
- **WHEN** a gravação da Carteira não pode ser concluída
- **THEN** a transação não deixa registro parcial nem cria objetos de outras funcionalidades
