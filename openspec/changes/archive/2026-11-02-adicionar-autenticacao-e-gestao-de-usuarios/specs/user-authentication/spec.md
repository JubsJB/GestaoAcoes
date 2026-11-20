## Purpose

Identificar usuários por credenciais seguras e emitir JWTs verificáveis sem cadastro público ou exposição de segredos.

## ADDED Requirements

### Requirement: Login por credenciais privadas
O sistema SHALL autenticar username canônico e senha de conta ativa com credencial definida, verificar BCrypt e emitir JWT assinado com identidade, sessão, perfil e prazo absoluto. SHALL aceitar apenas o algoritmo, emissor e audiência configurados. MUST NOT expor senha, hash ou segredo nas respostas ou logs. Username SHALL aceitar 3–100 caracteres ASCII a-z, 0-9, ponto, hífen e underscore após trim/lowercase; senha SHALL ter no mínimo oito caracteres e no máximo 72 bytes UTF-8, sem truncamento ou trim.

#### Scenario: Credencial válida
- **WHEN** uma conta ativa informa username e senha corretos em POST /auth/login
- **THEN** recebe 200 com identidade e prazos públicos, e JWT somente em Set-Cookie HttpOnly; GET /auth/me restaura identidade/perfil sem registrar atividade ou devolver JWT

#### Scenario: Credenciais indistinguíveis
- **WHEN** username não existe, senha está errada, conta está bloqueada ou não tem credencial definida
- **THEN** login responde 401 com mesma mensagem genérica e não revela qual condição ocorreu

#### Scenario: Token adulterado ou inadequado
- **WHEN** assinatura, algoritmo, emissor, audiência ou claims temporais são inválidos
- **THEN** a API nega acesso com 401 antes de executar o negócio

#### Scenario: Limite BCrypt e normalização
- **WHEN** o usuário define senha acima de 72 bytes UTF-8 ou username fora da política
- **THEN** recebe validação 400 sem truncamento silencioso, e diferenças de caixa no username válido não criam identidades duplicadas

### Requirement: Primeiro administrador seguro
O sistema SHALL permitir criação única do primeiro ADMIN por procedimento operacional explicitamente habilitado com segredo runtime, sem senha fixa e sem endpoint público. MUST NOT sobrescrever credenciais existentes ou criar contas automaticamente no modo normal.

#### Scenario: Bootstrap concorrente
- **WHEN** duas instâncias tentam inicializar banco sem usuários simultaneamente
- **THEN** somente uma conta ADMIN inicial é criada e nenhuma senha aparece em logs

#### Scenario: Reexecução ou segredo ausente
- **WHEN** bootstrap recebe segredo inválido/ausente ou é executado com banco já inicializado
- **THEN** recusa a criação sem modificar conta existente

### Requirement: Proteção de credenciais e abuso
O sistema SHALL servir autenticação publicada por HTTPS, enviar respostas sensíveis com no-store e limitar login em janela de 15min a cinco falhas por username+IP e cinquenta por IP, inclusive usernames inexistentes. MUST NOT oferecer cadastro público, refresh token, MFA ou recuperação automática por e-mail nesta versão.

#### Scenario: Excesso de login
- **WHEN** tentativas excedem a janela configurada
- **THEN** responde 429 com Retry-After genérico e permite nova tentativa após a janela sem bloquear permanentemente a conta

#### Scenario: Tentativa de cadastro anônimo
- **WHEN** um visitante tenta cadastrar conta
- **THEN** não existe fluxo público que permita fazê-lo e a administração exige ADMIN

### Requirement: Cookie de autenticação protegido e restaurável
O sistema SHALL transportar JWT exclusivamente em cookie GESTAOACOES_AUTH HttpOnly, Secure em produção, SameSite=Lax explícito, Path=/ e sem Domain. JWT MUST NOT aparecer no JSON, JavaScript, Web Storage, URL ou logs. Reload SHALL manter autenticação apenas enquanto cookie e sessão persistida forem válidos, sem renovar prazo por /me. A duração do cookie SHALL ser limitada ao prazo absoluto de quatro horas configuráveis e não ser renovada por atividade. Header Authorization ou parâmetro alternativo MUST NOT fornecer autenticação nesta entrega.

#### Scenario: Cookie válido no reload
- **WHEN** página é recarregada com cookie JWT e sessão válidos
- **THEN** /auth/me retorna 200 com identidade, perfil e prazos atuais sem novo login, sem JWT no JSON e sem alterar última atividade ou expiração

#### Scenario: Cookie ausente inválido ou expirado
- **WHEN** /auth/me recebe cookie ausente, adulterado, expirado ou de sessão revogada
- **THEN** responde 401 sem dados protegidos e o frontend exige login; cookie inválido remanescente não impede o fluxo público de login com CSRF

#### Scenario: Atributos e escopo
- **WHEN** login publicado em HTTPS é confirmado
- **THEN** Set-Cookie tem HttpOnly, Secure, SameSite=Lax, Path=/, nenhum Domain e duração não superior ao limite absoluto, sem exposição de JWT em body

#### Scenario: Credencial alternativa não admitida
- **WHEN** cliente envia somente Authorization Bearer ou JWT em parâmetro sem cookie válido
- **THEN** não autentica e não obtém rota alternativa para contornar CSRF

### Requirement: CSRF independente em todos os fluxos de escrita
O sistema SHALL disponibilizar token CSRF independente do JWT por GET /auth/csrf, com no-store, sem autenticar ou registrar atividade. SHALL exigir X-XSRF-TOKEN válido nas mutações, inclusive login, confirmação de redefinição, atividade, logout e escritas administrativas/financeiras. Cookie XSRF-TOKEN SHALL ser legível pelo cliente, separado da credencial HttpOnly, com Path=/, SameSite=Lax, sem Domain e Secure em produção. Login/logout SHALL invalidar CSRF anterior e disponibilizar novo token antes de outra submissão. MUST NOT desabilitar globalmente CSRF ou criar exceção para Postman/Swagger.

#### Scenario: Login com handshake CSRF
- **WHEN** cliente obtém CSRF e envia credenciais corretas com cookie/header anti-CSRF correspondentes
- **THEN** autentica, recebe cookie JWT protegido e consegue obter novo CSRF antes da próxima escrita

#### Scenario: Login ou reset sem CSRF
- **WHEN** POST /auth/login ou /auth/redefinicoes/confirmar não tem token anti-CSRF válido
- **THEN** recebe 403 CSRF_INVALIDO sem autenticar nem mudar senha, mesmo sendo fluxo público

#### Scenario: Logout e atividade protegidos
- **WHEN** sessão válida tenta POST /auth/logout ou /auth/atividade sem CSRF válido
- **THEN** recebe 403 CSRF_INVALIDO sem revogar sessão por esse comando nem atualizar atividade

#### Scenario: Token depois de logout
- **WHEN** logout com CSRF válido é confirmado
- **THEN** cookie de autenticação é removido, sessão é revogada e novo CSRF pode ser obtido para próximo login sem restaurar autenticação

#### Scenario: Cookie JWT não isenta CSRF
- **WHEN** escrita recebe cookie ADMIN válido e nenhum CSRF válido, com ou sem header Authorization adicional
- **THEN** responde 403 CSRF_INVALIDO, sem exclusão implícita por presença de JWT e sem efeito de negócio
