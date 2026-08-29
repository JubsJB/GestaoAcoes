## 1. Confirmar o baseline documental

- [x] 1.1 Confirmar que as 16 specs principais auditadas permanecem inalteradas antes da consolidação
- [x] 1.2 Confirmar que `portfolio-creation` continua sendo a única capability arquivada sem spec principal correspondente
- [x] 1.3 Conferir a delta `portfolio-creation` desta change contra o archive `2026-08-21-criacao-carteira`
- [x] 1.4 Conferir a delta contra RF13, `POST /carteiras`, `CarteiraCreateRequest` e `CarteiraResponse`
- [x] 1.5 Confirmar que a change ativa `estabilizar-infraestrutura-base` permanece fora desta consolidação

## 2. Consolidar a capability faltante

- [x] 2.1 Validar que a delta `portfolio-creation` está pronta para sincronização pelo fluxo oficial de archive, sem promover ou arquivar nesta etapa
- [x] 2.2 Verificar que `openspec/specs/portfolio-creation/spec.md` ainda não existe e será criada exclusivamente pela promoção posterior
- [x] 2.3 Validar que o contrato consolidado preserva `POST /carteiras`, entrada apenas com `nome`, normalização por trim e limite de 255 caracteres
- [x] 2.4 Validar que nomes duplicados continuam permitidos e que `id` permanece a identidade da Carteira
- [x] 2.5 Validar `201 Created`, `Location`, `CarteiraResponse` e geração UTC de `dataCriacao`
- [x] 2.6 Validar que a spec consolidada representa o schema já existente sem planejar ou alterar migration
- [x] 2.7 Confirmar que nenhuma outra spec principal foi modificada pela sincronização

## 3. Verificar a matriz final do backend

- [x] 3.1 Revalidar o mapeamento RF01–RF26 para as capabilities indicadas no design
- [x] 3.2 Revalidar o inventário dos 24 endpoints contra os quatro Resources reais
- [x] 3.3 Revalidar métodos, paths, parâmetros, status de sucesso e DTOs do domínio Corretora
- [x] 3.4 Revalidar métodos, paths, parâmetros, status de sucesso e DTOs do domínio Ação
- [x] 3.5 Revalidar métodos, paths, parâmetros, status de sucesso e DTOs de Carteira e indicadores financeiros
- [x] 3.6 Revalidar métodos, paths, parâmetros, status de sucesso e DTOs de Operação
- [x] 3.7 Confirmar a cobertura documental de posição, preço médio, resultado realizado, resultado não realizado, rentabilidade, patrimônio, resumo, histórico de cotação, snapshots e evolução patrimonial
- [x] 3.8 Confirmar a cobertura transversal de `api-error-handling` e dos ErrorCodes materialmente públicos

## 4. Verificar persistência e preparação para OpenAPI

- [x] 4.1 Revalidar o vínculo das migrations 001–006 com as capabilities documentadas sem editar changelogs
- [x] 4.2 Confirmar que não há springdoc/OpenAPI/Swagger no `pom.xml`
- [x] 4.3 Confirmar que não há configuração, annotations ou Swagger UI já implementados
- [x] 4.4 Registrar `documentar-api-openapi` como próxima change independente, sem instalar dependência nesta change

## 5. Garantir ausência de alterações funcionais

- [x] 5.1 Confirmar que nenhum arquivo em `src/main/java` foi alterado
- [x] 5.2 Confirmar que nenhum arquivo em `src/test/java` foi alterado
- [x] 5.3 Confirmar que nenhuma migration, configuração ou dependência foi alterada
- [x] 5.4 Confirmar que nenhum endpoint, DTO, provider ou regra financeira foi alterado

## 6. Validar a documentação

- [x] 6.1 Validar a delta `portfolio-creation` por meio de `openspec validate revisar-documentacao-final-backend --strict`, pois a spec principal só existirá após o archive
- [x] 6.2 Executar `openspec validate revisar-documentacao-final-backend --strict`
- [x] 6.3 Executar `openspec validate --all --strict`
- [x] 6.4 Consultar o Graphify para confirmar a rastreabilidade final entre Resources, services e capabilities
- [x] 6.5 Executar `git diff --check`
- [x] 6.6 Inspecionar `git diff` e `git status`, confirmando que apenas documentação OpenSpec prevista foi alterada
