## MODIFIED Requirements

### Requirement: Documento OpenAPI derivado da API real
O sistema SHALL disponibilizar documento code-first springdoc refletindo exatamente métodos, paths, parâmetros, bodies, respostas e schemas públicos implementados. SHALL incluir o POST de atualização das cotações da Carteira, extensões temporais e autenticação/administração somente quando implementados. Contagem SHALL corresponder à API real, sem perpetuar contagem histórica fixa nem contabilizar assets técnicos. MUST NOT declarar comportamentos inexistentes. Após implementação desta change SHALL documentar JWT em cookie e seus prazos de dez minutos ociosos/quatro horas absolutas configuráveis, handshake CSRF e sessão compartilhada, matriz de papéis e exceções públicas, sem alterar paths financeiros ou introduzir /v1.

#### Scenario: Consulta da descrição JSON
- **WHEN** consumidor acessa /v3/api-docs em qualquer profile
- **THEN** recebe documento válido com operações e paths realmente implementados, incluindo segurança e lote quando entregues

#### Scenario: Consulta da descrição YAML
- **WHEN** consumidor acessa /v3/api-docs.yaml em qualquer profile do MVP
- **THEN** recebe representação YAML da mesma descrição OpenAPI

#### Scenario: Ausência de contrato fictício
- **WHEN** documento é inspecionado
- **THEN** não contém /v1, filtros, paginação, endpoints ou mecanismos de autenticação não implementados

#### Scenario: Cookie e exceções explícitas
- **WHEN** cliente inspeciona contrato depois de implementar autenticação
- **THEN** operações financeiras e administrativas exigem cookieAuth; /auth/csrf, login e confirmação de redefinição não exigem autenticação, mas todos os POSTs exigem CSRF; /me, atividade e logout são autenticados

## ADDED Requirements

### Requirement: Contratos de segurança documentados e sem segredos
O OpenAPI SHALL descrever DTOs seguros de usuário, login, sessão e código temporário, respostas 200/201/204, validação, 401, 403, 409, 429 e indisponibilidade onde aplicáveis, com matriz ADMIN/USER. MUST NOT expor hashes, senhas reais, códigos utilizáveis ou bearer real em exemplos. Swagger SHALL permanecer acessível publicamente nos profiles atuais, mas suas chamadas protegidas SHALL exigir cookie JWT válido e CSRF nas mutações, sem fallback de autenticação por header.

#### Scenario: Inspeção de usuários e reset
- **WHEN** documento efetivamente servido é consultado
- **THEN** respostas comuns de usuário omitem senha/hash/código e somente emissão administrativa declara o código de exibição única, com senha nova recebida apenas no fluxo público de confirmação

#### Scenario: Try it out sem token
- **WHEN** visitante executa consulta financeira pela Swagger UI sem cookie de autenticação válido
- **THEN** recebe 401 apesar de poder ler a documentação

### Requirement: Ferramentas seguem cookie e CSRF
O documento SHALL declarar autenticação cookieAuth apiKey in:cookie GESTAOACOES_AUTH, sem esquema Bearer alternativo, e X-XSRF-TOKEN nas mutações, inclusive públicas. SHALL documentar Set-Cookie seguro, /auth/csrf sem atividade, identidade sem JWT e limites iguais de JWT/sessão. Postman SHALL usar cookie jar e handshake CSRF; Swagger same-origin SHALL usar cookies do navegador e integração CSRF suportada, sem tentar escrever manualmente header Cookie. Limitação de Try it out SHALL ser informada com alternativa Postman, nunca por bypass da segurança.

#### Scenario: Contrato e cliente seguros
- **WHEN** consumidor segue documentação para login e escrita
- **THEN** obtém CSRF, recebe cookie JWT no login e obtém CSRF atual antes de escrever; nenhum JWT é devolvido em JSON ou exigido em JavaScript

#### Scenario: UI sem suporte ao fluxo completo
- **WHEN** versão real de Swagger UI não executa cookie/CSRF com segurança
- **THEN** documentação permanece navegável e execução é orientada ao Postman com mesma proteção, sem liberar endpoint alternativo
