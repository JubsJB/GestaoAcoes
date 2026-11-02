# password-reset Specification

## Purpose

Permitir definição de senha pelo titular mediante autorização administrativa temporária, de uso único e resistente a abuso.

## Requirements

### Requirement: Autorizar sem escolher senha
O sistema SHALL permitir somente ADMIN emitir código aleatório de pelo menos 128 bits para conta ativa, com prazo de quinze minutos contado no servidor, retornado uma única vez para entrega por canal confiável. SHALL armazenar somente verificador seguro e nunca logar código/senha. Nova emissão SHALL invalidar imediatamente a autorização anterior.

#### Scenario: Emissão administrativa
- **WHEN** ADMIN autoriza redefinição de conta ativa
- **THEN** pode copiar o código novo uma vez, sem campo para definir a nova senha e sem recuperar o código depois

#### Scenario: Segunda autorização
- **WHEN** outro código é emitido antes de usar o anterior
- **THEN** somente o último pode ser consumido, mesmo em corrida com confirmação

#### Scenario: Contrato de autorização e versão
- **WHEN** ADMIN com CSRF solicita POST /usuarios/{id}/redefinicoes com versaoEsperada atual para conta ativa
- **THEN** recebe 200 no-store com usuario/autorizacao, contendo codigo/motivo/expiraEm; a emissão incrementa versão otimista sem invalidar sessões, infere INICIAL ou REDEFINICAO pelo estado da credencial e nunca recebe senha ou ator livre

#### Scenario: Recusa administrativa conhecida
- **WHEN** a versão esperada é obsoleta ou a conta alvo está bloqueada
- **THEN** retorna respectivamente 409 CONFLITO_VERSAO ou USUARIO_BLOQUEADO, sem substituir autorização, mantendo auditoria segura de recusa após liberar locks

#### Scenario: Configuração e representação seguras
- **WHEN** a aplicação web inicializa ou emite autorização
- **THEN** exige chave HMAC runtime própria distinta da JWT e retorna código Base32 RFC 4648 sem padding, aceitando caixa e separadores de espaços/hífens sem persistir ou logar seu texto puro

### Requirement: Consumir código e invalidar sessões atomicamente
O sistema SHALL receber username, código, nova senha e confirmação sem exigir sessão autenticada, mas com CSRF válido. Para conta ativa e código válido SHALL gravar BCrypt, consumir autorização e invalidar globalmente todas as sessões anteriores por incremento de versão de segurança em uma única transação. Código SHALL expirar na igualdade de quinze minutos, ser utilizável uma vez e ser invalidado após cinco erros de código; falhas de negócio não podem desfazer contador de tentativas. Redefinição MUST NOT habilitar conta bloqueada ou autenticar automaticamente.

#### Scenario: Redefinição válida
- **WHEN** usuário confirma senha válida com código vigente antes de quinze minutos
- **THEN** recebe 204, a senha antiga deixa de autenticar, a nova funciona em login separado e cookies JWT antigos de todas as abas/navegadores recebem 401 em novo acesso

#### Scenario: Código incorreto ou esgotado
- **WHEN** são realizadas cinco tentativas incorretas para uma autorização
- **THEN** ela é invalidada de forma persistente e nem o código correto pode ser usado depois

#### Scenario: Expirado reutilizado ou bloqueado
- **WHEN** código é usado após quinze minutos, já foi consumido ou a conta foi bloqueada
- **THEN** confirmação falha genericamente, sem mudar senha nem habilitar conta

#### Scenario: Dois consumos simultâneos
- **WHEN** dois requests confirmam o mesmo código ao mesmo tempo
- **THEN** somente um confirma, sem dupla troca de senha nem reutilização

#### Scenario: Falha de persistência
- **WHEN** ocorre falha ao persistir senha, consumo ou invalidação de sessões
- **THEN** não há sucesso parcial e a senha anterior permanece consistente com a autorização e sessões

#### Scenario: Reset com sessão antiga ainda no navegador
- **WHEN** titular conclui redefinição válida com CSRF e código corretos enquanto há abas da sessão antiga
- **THEN** essas sessões são invalidadas, nenhum cookie JWT novo é emitido automaticamente e novo login explícito é necessário

#### Scenario: Confirmação com cookie inválido e CSRF válido
- **WHEN** titular confirma código e senha válidos em POST /auth/redefinicoes/confirmar com cookie JWT antigo, inválido ou de sessão expirada
- **THEN** o cookie não bloqueia o fluxo público, CSRF permanece obrigatório e sucesso retorna 204 sem emitir JWT

#### Scenario: Senha inadequada ou CSRF rejeitado
- **WHEN** senha não atende oito caracteres/máximo 72 bytes UTF-8, diverge da confirmação ou CSRF é inválido
- **THEN** retorna validação 400 ou CSRF 403, sem consumir código ou suas tentativas; payloads inválidos que passam CSRF continuam sujeitos ao limite de requisições

#### Scenario: Decisão após espera e reinício
- **WHEN** consumo/emissão espera por locks ou a aplicação reinicia
- **THEN** revalida prazos/estado após espera, inclusive ADMIN na emissão; persistência preserva autorização, tentativas, consumo e revogação, sem ampliar os quinze minutos

### Requirement: Fluxo público sem enumeração
A interface SHALL oferecer o mesmo caminho de redefinição para qualquer username sem revelar existência ou autorização pendente. Falhas por identidade/código SHALL responder 400 REDEFINICAO_INVALIDA com mensagem equivalente, sem distinguir expirado, incorreto ou reutilizado. Confirmação SHALL limitar requests a dez por username+IP e cinquenta por IP a cada quinze minutos, inclusive identidades inexistentes.

#### Scenario: Identidade inexistente
- **WHEN** usuário informa username inexistente no fluxo público
- **THEN** vê o mesmo formulário e falha genérica de confirmação, sem confirmação pública de cadastro

#### Scenario: Limite público excedido
- **WHEN** confirmação excede limite de requests da janela
- **THEN** recebe 429 com Retry-After sem revelar existência da identidade

#### Scenario: Payload inválido também contabilizado
- **WHEN** requisições de confirmação com CSRF válido têm senha inválida, campos extras, JSON ilegível ou identidade inexistente
- **THEN** contam no limite persistido por IP e, quando a identidade é extraível, por username+IP antes do binding; 400 ou rollback de consumo não desfaz os contadores já confirmados
