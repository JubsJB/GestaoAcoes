## Why

O backend do MVP possui 24 endpoints e contratos OpenSpec consolidados, mas não oferece uma descrição OpenAPI nem uma interface Swagger UI para orientar o frontend Angular e testes manuais. A documentação deve ser derivada do código real sem alterar endpoints, DTOs funcionais, regras financeiras ou segurança.

## What Changes

- Adicionar integração code-first com springdoc para gerar OpenAPI JSON/YAML e Swagger UI a partir dos Resources e DTOs existentes.
- Documentar os 24 endpoints reais, incluindo parâmetros, request bodies, respostas de sucesso, `Location` quando aplicável e erros públicos plausíveis.
- Adicionar metadados globais da API e tags por domínio: Corretoras, Ações, Carteiras, Operações e Indicadores da Carteira.
- Representar `StandardError` como schema reutilizável e documentar ErrorCodes públicos somente nos endpoints em que podem ocorrer.
- Preservar os paths padrão `/v3/api-docs`, `/v3/api-docs.yaml` e `/swagger-ui.html`, sem versionar os endpoints com `/v1`.
- Disponibilizar OpenAPI e Swagger UI em todos os profiles durante o MVP, sem configuração específica por ambiente e sem exposição de secrets.
- Preservar o baseline Java 17 efetivamente configurado no repositório.
- Não adicionar autenticação fictícia, secrets, endpoints, migrations ou mudança funcional.

## Capabilities

### New Capabilities

- `api-documentation`: disponibiliza documentação OpenAPI navegável e fiel aos contratos REST públicos existentes.

### Modified Capabilities

- Nenhuma. A documentação descreve as capabilities funcionais existentes sem modificar seus requisitos.

## Impact

- Dependência: adição proposta de `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0` ao `pom.xml`.
- Backend documental: nova configuração global OpenAPI e annotations concentradas nos Resources, com uso seletivo de `@Schema` em DTOs públicos e `StandardError`.
- Testes: cobertura da geração do documento, presença dos 24 paths, metadados, schemas e disponibilidade da UI sem inspecionar seu HTML interno.
- Persistência, endpoints e regras de negócio: nenhum impacto.
- Baseline e profiles: Java 17 e disponibilidade da documentação em todos os profiles foram aprovados para esta change; eventual migração de Java ou restrição futura de exposição pertencem a changes independentes.
