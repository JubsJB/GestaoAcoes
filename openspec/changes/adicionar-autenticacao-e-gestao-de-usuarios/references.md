# Evidências e rastreabilidade

Planejamento revisado em 30/10/2026 após aprovação documental de cookie JWT, CSRF, quatro horas e sessão entre abas; implementação não autorizada. Arquivos financeiros, outras changes e alterações locais preexistentes preservados.

## Materiais obrigatórios

- [Aula 13 — Login e proteção da API com JWT](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-13-LOGIN-COM-JWT.md), consultada via conector GitHub: Spring Security/Resource Server, JWT HS256, segredo runtime >=32 bytes, BCrypt, PostgreSQL/Liquibase, login e /me, testes JWT real e 401/403. Exemplo com sessão HTTP stateless, validade 60min e bootstrap por ambiente. A revogação não é entregue na introdução.
- [Aula 14 — Controle de acesso baseado em papéis](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-14-CONTROLE-DE-ACESSO.md), consultada via conector GitHub: atividade avaliativa §13 exige dois papéis, matriz prévia, escrita administrativa, usuários sem hashes, testes 200/401/403, explicação de autorização horizontal e justificativa de @PreAuthorize. §14 inclui @EnableMethodSecurity, login público e usuários desativados em vez de removidos. §12 reconhece limitações de tokens antigos e último ADMIN.

Não transplantar o endpoint didático que permite ADMIN escolher nova senha, o CRUD de produtos/grupos, changelog XML ou comandos de commit/tag/push das aulas. O pedido atual determina código temporário, carteiras compartilhadas e nenhuma implementação/commit nesta etapa. A segurança apenas de DELETE da aula 13 é superada pela política completa da aula 14.

## Professor, existente, aprovado e proposto

| Tema | Professor | Evidência atual | Regra aprovada pelo usuário | Decisão técnica proposta |
| --- | --- | --- | --- | --- |
| Identidade | JWT, BCrypt, Spring Security | pom sem dependências de segurança; rotas Angular sem auth | username/senha; sem cadastro público | Resource Server Nimbus, HS256 e cookie HttpOnly protegido; /me restaura sem atividade |
| Permissões | ADMIN/USER, matriz, @PreAuthorize | Resources financeiros hoje sem política de usuário | USER consulta; ADMIN escreve/administra | filtro restritivo + segurança de métodos, inventário D6 |
| Horizontal | explicar regra do domínio | Carteira sem proprietário | carteiras compartilhadas | proteger sessão própria, sem filtro fictício de proprietário |
| CSRF | reavaliar quando credencial mudar para cookie | ainda sem Spring Security na aplicação | proteção obrigatória com cookie automático | handshake independente, login/reset/atividade/logout e clientes sem bypass |
| Sessões | exemplo stateless e exp; limitação de revogação | não há sessão autenticada | 10min reais e tokens antigos sem acesso indevido | tabela de sessão compartilhada entre abas, versão para invalidação global, atividade significativa e JWT/registro com quatro horas configuráveis |
| Reset | exemplo ADMIN informa senha | inexistente no fluxo inspecionado | ADMIN entrega código 15min, usuário escolhe senha | HMAC, 128 bits, cinco tentativas e consumo transacional |
| Primeiro ADMIN | credencial runtime e bootstrap | migrations chegam à 008, sem usuário | procedimento seguro sem senha fixa | one-shot, lock singleton, sem endpoint |
| Navegação | aula centrada em API | CarteiraContextService, localStorage global e requests compartilhados | retorno autorizado e sem vazamento | cookie compartilhado sem JWT no JS; seleção apenas em memória, intenção relativa por identidade e geração de sessão |
| Qualidade | testes mock e JWT real | AGENTS exige TDD e 80% linhas/ramos globais | TDD, regressão financeira e pouca repetição manual | fronteiras D8 e testes determinísticos de tempo/corridas |

## Fontes locais consultadas

- `AGENTS.md`, sobretudo §§7.1, 22, 25–29; `docs/PRD.md`, incluindo exclusões iniciais e evolução §27.1; `README.md` (inclusive alterações locais existentes, não editadas).
- `openspec/config.yaml` (`spec-driven`), skills locais `openspec-propose` e `tdd`, OpenSpec instalado 1.9.0 e instruções CLI de cada artefato.
- Specs atuais de API/documentação/erros e inventário completo de capabilities; contratos financeiros/custódia e shell/contexto já registrados em AGENTS, README e Graphify.
- Consulta `graphify query "authentication authorization controllers write endpoints carteira context storage" --budget 1800`: apontou `CarteiraContextService` e relações do shell/features; resultado truncado, usado apenas como mapa e confirmado nos arquivos atuais. Não foi tratado como prova de ausência de segurança nem atualizado como se houvesse código novo.
- `pom.xml`, `src/main/resources/application.properties`, `db/changelog/db.changelog-master.yaml` e lista 001–008; mappings de todos os sete Resources atuais; `StandardError`.
- `frontend/src/app/app.routes.ts`, `app.config.ts`, `core/config/api.config.ts`, `core/carteira/carteira-context.service.ts`, `frontend/package.json`, `frontend/nginx.conf`.

O git inicial tinha README.md e offset-date-time.formatter.spec.ts modificados e revisao-movimentacoes-custodia.zip não rastreado. Nenhum desses arquivos foi alterado pelo planejamento.

## Mapa de aceitação

| Regras do pedido | Specs de destino | Etapas em tasks.md |
| --- | --- | --- |
| Carteiras compartilhadas e perfis | role-access-control | 3, 8 |
| Login, BCrypt e primeiro ADMIN | user-authentication | 1, 2 |
| 10min, revogação individual/global, quatro horas e abas | session-lifecycle | 2.4, 6.1–6.6, 7.7 |
| Cookie protegido e reload sem login quando válido | user-authentication, frontend-authentication, frontend-application-shell, frontend-portfolio-management | 2.3, 2.8, 7.1, 7.8 |
| CSRF independente, precedência de erros e clientes | user-authentication, role-access-control, api-error-handling, api-documentation, password-reset | 2.7, 3.4–3.5, 5.2, 7.2, 9.1–9.2 |
| Código 15min e senha privada | password-reset, user-administration | 4, 5 |
| Último ADMIN e concorrência | user-administration, session-lifecycle, password-reset | 4, 5, 6 |
| Angular, rota, isolamento e sincronização visual de abas | frontend-authentication | 7.1–7.8, 8 |
| Swagger e 401/403 | api-documentation, api-error-handling | 3, 9 |
| Cobertura/TDD/compatibilidade financeira | todas, fronteiras de design D8 | todas, 10 |

## Fontes técnicas da revisão

- BOM local: C:/Users/julia/.m2/repository/org/springframework/boot/spring-boot-dependencies/4.1.0/spring-boot-dependencies-4.1.0.pom declara spring-security.version=7.1.0; pom.xml da aplicação usa Boot 4.1.0 e ainda não adiciona Security. Não confundir documentação web corrente 7.1.2 com dependência gerenciada 7.1.0; reconfirmar APIs/compilação na implementação.
- [Spring Security — CSRF para SPA](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa): integração com token diferido e disponibilização após login/logout. Aplicar versão compatível, sem desabilitar proteção globalmente.
- [Angular — segurança XSRF](https://angular.dev/best-practices/security#httpclient-xsrfcsrf-security): cookie XSRF-TOKEN e header X-XSRF-TOKEN em mutações elegíveis; backend valida. JWT HttpOnly permanece independente.
- [Swagger — autenticação por cookie](https://swagger.io/docs/specification/v3_0/authentication/cookie-authentication/): OpenAPI apiKey in:cookie e limitações do Try it out; nunca prometer escrita manual de Cookie pelo JavaScript ou contornar CSRF.
- [Spring Security 7.1.0 — OAuth2ResourceServerConfigurer](https://github.com/spring-projects/spring-security/blob/7.1.0/config/src/main/java/org/springframework/security/config/annotation/web/configurers/oauth2/server/resource/OAuth2ResourceServerConfigurer.java): registerDefaultCsrfOverride adiciona matcher de exclusão CSRF. D3.1 evita essa DSL automática usando filtro cookie e JwtAuthenticationProvider/JwtDecoder, com teste de escrita negada mesmo com cookie e Authorization. Adaptação necessária para cumprir cookie + CSRF aprovados.

Aulas 13/14 foram novamente consultadas via GitHub nesta revisão. Os sessenta minutos são exemplo configurável da aula, substituído por quatro horas aprovadas. A própria aula exige reavaliar CSRF com cookie. Permanecem JWT/BCrypt/RBAC/segurança de métodos e testes 200/401/403; não copiar exceções de CSRF do exemplo Bearer ou endpoint administrativo que escolhe senha.

## Decisões aprovadas versus detalhes de implementação

Aprovados pelo pedido: cookie JWT HttpOnly/Secure em produção, CSRF independente, restauração por /me sem atividade, dez minutos exatos no servidor, quatro horas configuráveis sem renovação, mesma sessão nas abas e revogação efetiva. D3/D3.1 especificam SameSite=Lax, Path=/, sem Domain, handshake CSRF e clientes. D1/D7 atribuem revogação individual ao sid e invalidação global à versão, sem varredura redundante. D2 define eventos significativos, deduplicação sem atraso artificial e revalidação temporal após lock; nenhuma tolerância de segundos foi incorporada.

Sincronização visual: sinais entre abas, /me no foco e polling de trinta segundos em abas visíveis, sempre sem atividade. Esse limite de apresentação não amplia autorização backend nem promete push imediato. Seleção de carteira permanece local à aba em memória, embora a autenticação seja compartilhada. Intenção de retorno tem TTL próprio de sessenta minutos, independente do limite autenticado de quatro horas; não contém credenciais ou campos financeiros.

Ficam para verificação futura custo BCrypt, domínio/TLS e suporte real de Swagger UI à combinação cookie/CSRF. Se Try it out não comportar o fluxo seguro, usar Postman. Eventual mudança de tolerância ociosa precisa de aprovação; não é parte deste plano. Somente documentação foi revisada, sem código, configuração de aplicação, migrations ou tarefas de implementação executadas.
