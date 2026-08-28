## Why

O RF06 do PRD exige consultar uma Corretora por ID ou CNPJ, mas a API atual cobre apenas a consulta por ID. Esta change completa o requisito com uma leitura local pelo CNPJ persistido, preservando a normalização e as regras já consolidadas no cadastro.

## What Changes

- Adicionar `GET /corretoras/por-cnpj?cnpj={cnpj}` para recuperar uma única Corretora por CNPJ, sem alias alternativo.
- Aceitar CNPJ válido com máscara ou somente 14 dígitos, reutilizando a normalização e validação existentes e consultando o banco pelo valor normalizado.
- Responder `200 OK` com o `CorretoraResponse` já existente quando houver correspondência.
- Responder `400 Bad Request` com `CNPJ_INVALIDO` para entrada inválida e `404 Not Found` no padrão centralizado vigente para CNPJ válido sem Corretora correspondente.
- Reutilizar `CorretoraResource`, `CorretoraService`, `CorretoraRepository.findByCnpj`, `CorretoraMapper`, `CorretoraResponse` e `CnpjValidator`.
- Garantir que a consulta seja read-only, sem persistência, alteração da entidade, revalidação cadastral ou chamadas à BrasilAPI e ViaCEP.
- Preservar sem mudança os contratos atuais de cadastro, listagem e consulta por ID.
- Não alterar schema, migrations, entidades, configurações ou dependências.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `broker-registration`: completar a consulta de Corretora persistida com busca por CNPJ normalizado, conforme o RF06, sem revalidação externa.

## Impact

- Futuras alterações limitadas ao endpoint de leitura em `CorretoraResource`, ao método read-only em `CorretoraService` e aos testes atuais de Corretora.
- Reutilização do método já existente `CorretoraRepository.findByCnpj`, do mapper, DTO, validador e tratamento centralizado de erros.
- Nenhuma integração externa nova e nenhuma chamada aos providers existentes durante a consulta.
- Nenhuma migration ou mudança na unique constraint `uk_corretora_cnpj`, que já sustenta a unicidade e a busca pelo CNPJ persistido.
