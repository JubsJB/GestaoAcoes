## 1. Consulta read-only no service

- [x] 1.1 Adicionar ao `CorretoraService` um método `@Transactional(readOnly = true)` que receba o CNPJ e reutilize `CnpjValidator.normalizeAndValidate` antes de qualquer acesso ao repository.
- [x] 1.2 Consultar `CorretoraRepository.findByCnpj` exatamente uma vez com o valor normalizado e mapear o registro encontrado com o `CorretoraMapper` existente.
- [x] 1.3 Lançar `ObjectNotFoundException` no padrão vigente quando o CNPJ válido normalizado não corresponder a uma Corretora persistida, sem criar código de erro novo.
- [x] 1.4 Confirmar que o novo fluxo não persiste, modifica ou revalida a Corretora e não interage com `CnpjProvider`, `CepProvider`, BrasilAPI, ViaCEP ou o serviço de persistência do cadastro.

## 2. Endpoint REST

- [x] 2.1 Adicionar exclusivamente `GET /corretoras/por-cnpj?cnpj={cnpj}` ao `CorretoraResource`, delegando ao novo método do service e retornando `200 OK` com `CorretoraResponse`.
- [x] 2.2 Garantir que o query parameter `cnpj` ausente ou vazio e os demais valores inválidos sejam encaminhados à validação aprovada e produzam `400 / CNPJ_INVALIDO`.
- [x] 2.3 Preservar sem alteração comportamental `POST /corretoras`, `GET /corretoras` e `GET /corretoras/{id}`.
- [x] 2.4 Confirmar que não foi criado alias em `/corretoras/cnpj/{cnpj}` e que `GET /corretoras?cnpj=...` não foi transformado em consulta singular.

## 3. Testes de repository

- [x] 3.1 Ampliar `CorretoraRepositoryTest` para confirmar que `findByCnpj` recupera a Corretora pelo CNPJ normalizado persistido.
- [x] 3.2 Cobrir o retorno vazio de `findByCnpj` para CNPJ válido inexistente.
- [x] 3.3 Confirmar nos testes de persistência que a unicidade `uk_corretora_cnpj` continua suficiente, sem query customizada, índice ou migration adicional.

## 4. Testes de service e validação

- [x] 4.1 Testar consulta encontrada com CNPJ sem máscara, verificando normalização, argumento enviado ao repository e `CorretoraResponse` completo.
- [x] 4.2 Testar consulta encontrada com CNPJ mascarado e confirmar que ela produz a mesma busca normalizada e o mesmo contrato da entrada sem máscara.
- [x] 4.3 Testar CNPJ nulo, vazio, caracteres/formato inválidos, quantidade de dígitos inválida, dígitos repetidos e dígitos verificadores inválidos com `400 / CNPJ_INVALIDO` antes do repository.
- [x] 4.4 Testar CNPJ válido inexistente com `ObjectNotFoundException` e mensagem coerente com o tratamento centralizado atual.
- [x] 4.5 Verificar que consultas existentes, inexistentes e inválidas não acionam providers, adapters, persistência, `Clock` ou validações externas.
- [x] 4.6 Verificar por reflexão que o novo método mantém `@Transactional(readOnly = true)` com isolamento padrão e sem locks.

## 5. Testes do resource e arquitetura

- [x] 5.1 Ampliar `CorretoraResourceTest` para cobrir `200 OK` com CNPJ sem máscara e o payload completo de `CorretoraResponse`.
- [x] 5.2 Cobrir `200 OK` com CNPJ mascarado enviado como query parameter e confirmar equivalência com a consulta sem máscara.
- [x] 5.3 Cobrir `400 / CNPJ_INVALIDO` para parâmetro ausente, vazio e valores inválidos no formato `StandardError` vigente.
- [x] 5.4 Cobrir `404 Not Found` para CNPJ válido inexistente no tratamento centralizado atual.
- [x] 5.5 Cobrir a ausência do alias `/corretoras/cnpj/{cnpj}`, a preservação da listagem em `GET /corretoras` e a ausência de conflito com `GET /corretoras/{id}`.
- [x] 5.6 Confirmar nos testes HTTP que a consulta por CNPJ não chama BrasilAPI, ViaCEP ou qualquer provider externo.
- [x] 5.7 Adicionar inspeção arquitetural, se compatível com o padrão atual, confirmando o fluxo `CorretoraResource → CorretoraService → CnpjValidator → CorretoraRepository → CorretoraMapper` e a ausência de novas dependências, escritas ou locks.

## 6. Regressão e validações finais

- [x] 6.1 Executar os testes direcionados de `CnpjValidator`, repository, service e resource de Corretora.
- [x] 6.2 Executar a suíte completa com `.\mvnw.cmd -q test` e registrar total, failures, errors e skipped.
- [x] 6.3 Executar `.\mvnw.cmd -q clean verify` e confirmar build, H2, Liquibase 001–006 e Hibernate `ddl-auto=validate` sem migration nova.
- [x] 6.4 Validar PostgreSQL somente se datasource e credenciais estiverem disponíveis, registrando explicitamente a não execução caso não estejam.
- [x] 6.5 Executar `openspec validate consulta-corretora-por-cnpj --strict` e `openspec validate --all --strict`.
- [x] 6.6 Atualizar o Graphify e consultar o grafo para confirmar o novo fluxo local e a ausência de dependências externas.
- [x] 6.7 Executar `git diff --check`, revisar `git diff` e registrar `git status` sem realizar operações de histórico Git.
- [x] 6.8 Revisar o resultado final para confirmar ausência de alterações em entidade, migrations, configurações, dependências, DTOs e contratos REST fora do escopo.
