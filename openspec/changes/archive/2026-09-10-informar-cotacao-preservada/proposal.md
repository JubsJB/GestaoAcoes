## Why

A RN21 do PRD exige informar o uso da última cotação válida quando a atualização falha. O detalhe preserva o DTO, mas hoje apresenta apenas o erro genérico, sem esclarecer o significado do valor mantido.

## What Changes

- Exibir aviso explícito junto à cotação preservada após falha de atualização manual.
- Remover o aviso quando uma nova atualização for concluída com sucesso.
- Preservar mensagem e detalhes do erro e a política de tentativas exclusivamente manuais.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `frontend-stock-management`: comunicação explícita da última cotação válida após falha.

## Impact

Somente detalhe Angular de Ações e seus testes; sem mudança de API, persistência ou cálculos.

Pendências reais identificadas, em prioridade: (1) completar RN21 nesta change; (2) entregar coleção Postman/Insomnia prevista nas seções 23/26; (3) completar README conforme seção 22. Os fluxos centrais de cadastro, operações, posições, resultados e evolução já possuem specs e implementação; esta inspeção não substitui homologação prática com provedores reais.
