# api-documentation Specification

## Purpose

Disponibilizar uma descrição OpenAPI navegável, segura e fiel aos contratos REST existentes para orientar consumidores da API sem modificar seu comportamento funcional.

## Requirements

### Requirement: Documento OpenAPI derivado da API real
O sistema SHALL disponibilizar, por integração code-first com springdoc, uma descrição OpenAPI gerada a partir das 24 operações REST implementadas em 18 paths funcionais distintos. A descrição MUST refletir métodos, paths, parâmetros, request bodies, respostas, status de sucesso e schemas públicos reais, e MUST NOT declarar endpoints ou comportamentos inexistentes. Endpoints e assets técnicos do springdoc MUST NOT integrar essa contagem funcional.

#### Scenario: Consulta da descrição JSON
- **WHEN** um consumidor acessa `/v3/api-docs` em qualquer profile do MVP
- **THEN** o sistema responde com um documento OpenAPI válido que contém as 24 operações públicas nos 18 paths funcionais implementados

#### Scenario: Consulta da descrição YAML
- **WHEN** um consumidor acessa `/v3/api-docs.yaml` em qualquer profile do MVP
- **THEN** o sistema responde com a representação YAML da mesma descrição OpenAPI

#### Scenario: Ausência de contrato fictício
- **WHEN** o documento OpenAPI é inspecionado
- **THEN** ele não contém autenticação, versionamento `/v1`, filtros, paginação ou endpoints que não existam no backend

### Requirement: Swagger UI navegável
O sistema SHALL disponibilizar Swagger UI em todos os profiles do MVP por `/swagger-ui.html` e pelo recurso efetivo `/swagger-ui/index.html`, para permitir navegação pelos domínios, inspeção dos schemas e execução manual dos endpoints documentados.

#### Scenario: Acesso à interface
- **WHEN** um consumidor acessa a entrada padrão do Swagger UI em qualquer profile do MVP
- **THEN** a interface carrega a descrição OpenAPI da própria aplicação e apresenta as operações agrupadas por domínio

### Requirement: Metadados globais da API
O documento SHALL identificar a API como `Sistema de Gestão e Controle de Carteira de Investimentos API`, SHALL descrevê-la como API REST para gerenciamento de corretoras, ações, carteiras, operações e indicadores, e SHALL usar a versão documental do projeto sem introduzir versionamento no path dos endpoints.

#### Scenario: Inspeção dos metadados
- **WHEN** o consumidor consulta a descrição OpenAPI
- **THEN** título, descrição e versão documental estão presentes e nenhuma definição de segurança inexistente é declarada

### Requirement: Organização por domínio
As operações SHALL ser organizadas por tags sustentáveis correspondentes aos domínios públicos Corretoras, Ações, Carteiras, Operações e Indicadores da Carteira.

#### Scenario: Navegação por tag
- **WHEN** o consumidor abre a Swagger UI
- **THEN** cada operação aparece em uma tag coerente com seu caso de uso sem duplicação artificial de endpoints

### Requirement: Contratos de entrada e saída fiéis
O documento SHALL representar os DTOs públicos, validações Bean Validation, enums, formatos temporais e valores monetários conforme o código real. Annotations documentais MUST NOT alterar serialização, validação ou semântica funcional.

Annotations `@Schema` SHALL ser usadas seletivamente somente quando acrescentarem semântica, exemplo, formato, enum, unidade, moeda, timestamp ou restrição relevante, e MUST NOT ser aplicadas mecanicamente a campos triviais nem a entities, repositories, services ou providers.

#### Scenario: Inspeção de request e response
- **WHEN** o consumidor inspeciona uma operação com corpo JSON
- **THEN** os campos obrigatórios, opcionais, tipos, formatos e schemas de resposta correspondem aos DTOs efetivamente usados pelo endpoint

#### Scenario: Precisão e temporalidade
- **WHEN** o consumidor inspeciona valores `BigDecimal`, `LocalDate` ou `OffsetDateTime`
- **THEN** a documentação preserva os tipos e formatos públicos sem sugerir conversões, arredondamentos ou fusos inexistentes

### Requirement: Respostas de erro públicas e proporcionais
O documento SHALL reutilizar `StandardError` como schema de erro e SHALL declarar em cada operação somente os status e ErrorCodes publicamente plausíveis para aquele fluxo. O fallback `409/INTEGRIDADE_DADOS_VIOLADA` SHALL usar a mensagem pública aprovada.

#### Scenario: Erro plausível documentado
- **WHEN** uma operação pode produzir erro de validação, ausência, conflito ou provider externo
- **THEN** a documentação apresenta apenas os status aplicáveis entre `400`, `404`, `409`, `422`, `429`, `502`, `503` e `504`, com `StandardError` como corpo

#### Scenario: Fallback de integridade
- **WHEN** uma resposta `409` genérica de integridade é documentada
- **THEN** ela identifica `INTEGRIDADE_DADOS_VIOLADA` e a mensagem `A operação viola uma regra de integridade dos dados.` sem expor detalhes internos

#### Scenario: Segurança do schema de erro
- **WHEN** o consumidor inspeciona exemplos ou schemas de erro
- **THEN** SQL, SQLState, stack trace, constraint, tabela, coluna, índice, mensagem nativa, detalhes de driver e secrets não são expostos

### Requirement: Compatibilidade sem mudança funcional
A integração de documentação MUST preserve todos os 24 contratos REST, DTOs funcionais, regras financeiras, migrations e integrações existentes. Ela MUST NOT exigir autenticação nem expor credenciais internas de providers.

A integração SHALL preservar Java 17 e MUST NOT alterar `java.version`, compiler source/target, toolchains ou configuração de JDK.

#### Scenario: Backend após habilitar documentação
- **WHEN** a aplicação inicia com a documentação habilitada
- **THEN** os endpoints existentes mantêm os mesmos métodos, paths, validações, respostas e efeitos anteriores

#### Scenario: Credenciais internas
- **WHEN** a descrição OpenAPI e a Swagger UI são consultadas
- **THEN** chaves da BRAPI, Alpha Vantage ou qualquer configuração interna não aparecem no contrato público
