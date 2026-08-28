## Why

O backend do MVP possui 24 endpoints implementados e 16 capabilities principais consolidadas, mas a criação de Carteira (RF13 e `POST /carteiras`) permanece representada apenas no archive `2026-08-21-criacao-carteira`: a capability `portfolio-creation` nunca foi promovida para `openspec/specs/`. Antes da documentação OpenAPI e do frontend Angular, o catálogo normativo precisa refletir integralmente o contrato já implementado, sem alterar comportamento funcional.

## What Changes

- Promover documentalmente a capability `portfolio-creation` a partir das decisões consolidadas no archive de criação de Carteira.
- Registrar a auditoria final de rastreabilidade entre RF01–RF26, endpoints REST, DTOs públicos, migrations 001–006, archives e specs principais.
- Confirmar que as demais capabilities principais representam os contratos implementados e não exigem deltas corretivos nesta change.
- Registrar que os paths refinados por changes aprovadas prevalecem sobre os exemplos iniciais e refináveis do PRD, sem alterar o PRD nesta change.
- Registrar a ausência atual de springdoc/OpenAPI/Swagger e reservar sua instalação e configuração para uma change separada.
- Não alterar código Java, testes, schema, contratos REST, DTOs, providers, configurações, dependências ou regras financeiras.

## Capabilities

### New Capabilities

- `portfolio-creation`: consolida o contrato já implementado de criação de Carteira, incluindo entrada, normalização, resposta, persistência e erros.

### Modified Capabilities

- Nenhuma. A auditoria não identificou requisito principal obsoleto ou divergente que exija delta em capability existente.

## Impact

- OpenSpec: criação da spec principal `openspec/specs/portfolio-creation/spec.md` quando a change for aplicada/arquivada.
- Documentação: complementação do catálogo normativo e registro da matriz final de rastreabilidade do backend.
- API e implementação: nenhum impacto funcional; os 24 endpoints, DTOs, ErrorCodes, migrations 001–006 e dependências permanecem inalterados.
- Próxima etapa: uma change independente deverá tratar springdoc/OpenAPI, Swagger UI, configuração e annotations, caso aprovados.
