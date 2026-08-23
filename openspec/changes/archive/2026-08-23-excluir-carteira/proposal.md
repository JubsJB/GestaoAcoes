## Why

O sistema já permite criar, consultar e atualizar o nome de uma Carteira, mas ainda não oferece uma operação explícita para remover um registro criado indevidamente ou que não seja mais necessário. Esta change estabelece a menor fatia aprovada para exclusão de uma Carteira sem Operações, preservando os contratos existentes e preparando uma restrição segura para o futuro histórico financeiro.

## What Changes

- Adicionar `DELETE /carteiras/{id}` para excluir uma Carteira existente e elegível.
- Responder `204 No Content`, sem corpo, quando a exclusão for concluída.
- Responder `404 Not Found` no formato `StandardError` existente quando o ID não corresponder a uma Carteira, inclusive em uma segunda tentativa após exclusão bem-sucedida.
- Realizar exclusão física no estado atual, sem introduzir campos de exclusão lógica ou alteração de schema.
- Reutilizar `CarteiraResource`, `CarteiraService` e `CarteiraRepository`, com transação de escrita e sem nova camada de persistência.
- Preservar integralmente outras Carteiras e os contratos existentes de `POST`, `GET` e `PATCH`.
- Registrar como restrição normativa futura que uma Carteira com Operações não poderá ser excluída fisicamente nem provocar exclusão em cascata do histórico; a verificação e o erro de negócio, com `409 Conflict` como resposta recomendada, serão definidos e implementados somente quando a associação com Operação existir.
- Não implementar Operações, posições, cálculos financeiros, histórico, snapshot ou frontend.

## Capabilities

### New Capabilities

- `portfolio-deletion`: Define o contrato REST, a elegibilidade, a persistência, a atomicidade e a compatibilidade futura da exclusão de Carteira.

### Modified Capabilities

Nenhuma. Os requisitos de `portfolio-query` e `portfolio-update` permanecem inalterados, e o contrato de criação documentado no change arquivado continua preservado.

## Impact

- API REST: novo endpoint `DELETE /carteiras/{id}`.
- Backend: evolução localizada em `CarteiraResource` e `CarteiraService`, utilizando a operação padrão de exclusão de `CarteiraRepository`.
- Testes: cobertura de service, resource e repository/H2, além de regressão dos contratos de criação, consulta e atualização.
- Persistência: remoção física da linha existente; nenhuma migration Liquibase, alteração de entidade, dependência ou configuração.
- Evolução futura: a capability de Operações deverá introduzir a verificação de vínculo e um erro de conflito/negócio antes que a exclusão seja permitida em carteiras com histórico; nenhum componente artificial dessa proteção integra a implementação atual.
