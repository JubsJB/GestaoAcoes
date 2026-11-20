# user-administration Specification

## Purpose

Permitir a administradores gerenciar contas sem conhecer senhas e sem eliminar o último administrador operacional do sistema.

## Requirements

### Requirement: Provisionamento e consulta privados
O sistema SHALL permitir ADMIN cadastrar username/perfil, listar e consultar contas sem retornar hash/senha. Cadastro SHALL criar conta sem credencial definida e emitir código inicial de quinze minutos para o próprio usuário escolher senha. Não haverá exclusão de usuário nem alteração de username nesta versão.

#### Scenario: Cadastro inicial
- **WHEN** ADMIN cadastra username disponível e perfil permitido
- **THEN** recebe conta pública e código uma única vez, sem senha; a conta só poderá autenticar após definir credencial pelo fluxo público de código

#### Scenario: Contrato integrado de cadastro
- **WHEN** ADMIN com CSRF envia username/perfil válidos em POST /usuarios
- **THEN** recebe 201, Location e envelope usuario/autorizacao com código INICIAL, motivo e expiraEm, no-store; conta ativa sem credencial, versão otimista incrementada pela emissão, sem senha/hash/versão de segurança e com auditoria atômica

#### Scenario: Username duplicado em corrida
- **WHEN** dois cadastros concorrem para o mesmo username normalizado
- **THEN** apenas um é criado e o outro recebe 409 sem duplicação
- **AND** a recusa usa USUARIO_DUPLICADO, sem duplicar autorização ou alterar o perfil da conta existente

### Requirement: Alterar perfil e situação com revogação
O sistema SHALL permitir somente ADMIN alterar perfil, bloquear e habilitar conta, com controle de versão e auditoria segura. Alteração de perfil e bloqueio SHALL incrementar versão de segurança e invalidar globalmente sessões anteriores na mesma transação, sem depender de atualizar cada linha de sessão; bloqueio SHALL invalidar códigos pendentes. Habilitar conta SHALL exigir login novo, sem recuperar códigos antigos. Toda mutação administrativa SHALL exigir CSRF válido.

#### Scenario: Mudança concorrente de cadastro
- **WHEN** dois administradores alteram a mesma versão de uma conta
- **THEN** uma alteração obsoleta recebe 409 e não sobrescreve a mudança confirmada

#### Scenario: Auditoria administrativa
- **WHEN** cadastro, perfil, bloqueio, habilitação ou autorização de reset é realizado
- **THEN** ator, alvo, instante e resultado são registrados sem senha, hash, token ou código

#### Scenario: Invalidação administrativa em todas as abas
- **WHEN** bloqueio ou alteração de perfil é confirmado com ADMIN autorizado e CSRF válido
- **THEN** cookies antigos do alvo em qualquer aba/navegador não autorizam novo acesso, e habilitação posterior não desfaz invalidação

#### Scenario: Contrato de versão e resposta administrativa
- **WHEN** ADMIN envia PATCH de perfil ou situação com CSRF válido e versaoEsperada atual
- **THEN** recebe 200 e UsuarioResponse atualizado com versao, sem versaoSeguranca ou segredos; ator é obtido exclusivamente do contexto autenticado, inclusive na proteção do serviço público

#### Scenario: Comando sem alteração e versão obsoleta
- **WHEN** o estado solicitado já corresponde ao estado persistido
- **THEN** a versão esperada é verificada primeiro; versão obsoleta recebe 409, e versão atual recebe 200 com o estado atual sem incrementar versões, invalidar sessões ou gerar auditoria de mudança

#### Scenario: Autorização expira durante espera por locks
- **WHEN** um comando administrativo espera pelo singleton ou pelo usuário alvo e a sessão do ator expira ou é invalidada
- **THEN** a autorização é revalidada após a espera e o comando é negado sem alterar o alvo

#### Scenario: Atomicidade e auditoria de recusa
- **WHEN** uma alteração efetiva falha na persistência da auditoria ou uma recusa auditável por versão obsoleta ou último ADMIN ocorre
- **THEN** a falha reverte integralmente a alteração sem auditoria SUCESSO; a recusa preserva o usuário e registra RECUSADO em transação independente após liberar os locks originais, inclusive se o chamador sofrer rollback

### Requirement: Último administrador operacional protegido
O sistema MUST NOT bloquear ou rebaixar o último ADMIN ativo com credencial definida, inclusive por ele próprio e sob concorrência. ADMIN sem credencial definida MUST NOT ser contado como substituto operacional.

#### Scenario: Último ADMIN tenta se rebaixar
- **WHEN** única conta ADMIN operacional tenta mudar seu perfil para USER ou se bloquear
- **THEN** recebe 409 ULTIMO_ADMIN_ATIVO e a conta/sessões permanecem inalteradas

#### Scenario: Dois administradores concorrem para remover acesso
- **WHEN** operações paralelas tentam bloquear/rebaixar os dois últimos administradores
- **THEN** pelo menos um ADMIN operacional permanece e a operação que violaria a regra é rejeitada atomicamente
