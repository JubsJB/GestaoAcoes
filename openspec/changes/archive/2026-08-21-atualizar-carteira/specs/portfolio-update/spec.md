## Purpose

Definir a atualização controlada do nome de uma Carteira persistida, preservando sua identidade, sua data de criação e os contratos já existentes do recurso.

## ADDED Requirements

### Requirement: Contrato de atualização parcial do nome
O sistema SHALL disponibilizar `PATCH /carteiras/{id}`, SHALL aceitar exclusivamente o campo obrigatório `nome` no request e MUST NOT aceitar `id`, `dataCriacao` ou propriedades desconhecidas.

#### Scenario: Request contendo somente o novo nome
- **WHEN** o cliente solicita a atualização de uma Carteira existente com um request contendo somente um `nome` válido
- **THEN** o sistema processa exclusivamente a alteração do nome da Carteira identificada na URI

#### Scenario: Tentativa de alterar campo controlado pela aplicação
- **WHEN** o request contém `id`, `dataCriacao` ou qualquer propriedade diferente de `nome`
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e preserva integralmente a Carteira

### Requirement: Validação e normalização do novo nome
O sistema SHALL exigir `nome`, SHALL remover somente os espaços das extremidades antes da validação e persistência e SHALL rejeitar valor nulo, vazio ou composto somente por espaços. O nome normalizado MUST possuir no máximo 255 caracteres. O sistema SHALL preservar espaços internos, acentos e caixa, MUST NOT aplicar outra transformação, SHALL permitir nomes duplicados e MUST NOT consultar duplicidade ou tratar o nome como identidade.

#### Scenario: Atualização com espaços nas extremidades
- **WHEN** o cliente informa um nome válido com espaços antes ou depois do conteúdo
- **THEN** o sistema remove somente esses espaços externos e persiste os espaços internos, acentos e caixa sem outras transformações

#### Scenario: Nome ausente, nulo, vazio ou somente com espaços
- **WHEN** o request omite `nome` ou contém valor nulo, vazio ou composto somente por espaços
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO`, informa o campo inválido em `details` quando aplicável e não altera a Carteira

#### Scenario: Nome acima do limite
- **WHEN** o nome possui mais de 255 caracteres após a remoção dos espaços das extremidades
- **THEN** o sistema responde `400 Bad Request` com código `REQUEST_INVALIDO` e não altera a Carteira

#### Scenario: Nome já utilizado por outra Carteira
- **WHEN** outra Carteira já possui o mesmo nome normalizado
- **THEN** a atualização pode ser concluída sem consulta ou rejeição por duplicidade

### Requirement: Preservação da Carteira atualizada
A atualização SHALL modificar exclusivamente `nome` e MUST preservar `id` e `dataCriacao` exatamente como persistidos. A operação MUST NOT criar ou modificar relacionamentos, operações, posições, histórico, snapshots ou qualquer outro estado fora da Carteira identificada.

#### Scenario: Alteração válida preserva campos imutáveis
- **WHEN** uma atualização válida é concluída
- **THEN** somente `nome` contém o novo valor normalizado, enquanto `id` e `dataCriacao` permanecem inalterados

#### Scenario: Novo nome igual ao nome persistido
- **WHEN** o novo nome normalizado é exatamente igual ao nome já persistido
- **THEN** o sistema responde `200 OK` com o estado atual da Carteira, sem erro ou código especial e sem alterar `id` ou `dataCriacao`

### Requirement: Carteira inexistente
Quando o identificador informado não corresponder a uma Carteira persistida, o sistema SHALL responder `404 Not Found` usando o formato centralizado de erros vigente e MUST NOT criar uma nova Carteira.

#### Scenario: Atualização de identificador inexistente
- **WHEN** o cliente solicita a atualização de uma Carteira cujo `id` não existe
- **THEN** o sistema responde `404 Not Found` no formato `StandardError` atual e nenhum registro é criado ou alterado

### Requirement: Resposta e atomicidade da atualização
Uma atualização concluída SHALL ser atômica e SHALL responder `200 OK` com o `CarteiraResponse` completo contendo `id`, `nome` e `dataCriacao` efetivamente persistidos e sem header `Location`, pois nenhum recurso novo é criado. Uma falha de validação ou persistência MUST preservar o estado anterior.

#### Scenario: Atualização concluída
- **WHEN** o novo nome é válido e a Carteira existe
- **THEN** o sistema responde `200 OK` com o estado completo efetivamente persistido e sem `Location`

#### Scenario: Falha antes da conclusão
- **WHEN** a validação ou persistência da atualização falha
- **THEN** a transação não deixa alteração parcial e os contratos existentes de criação e consulta permanecem disponíveis
