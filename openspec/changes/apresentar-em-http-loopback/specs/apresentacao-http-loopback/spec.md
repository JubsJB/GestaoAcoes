## ADDED Requirements

### Requirement: Fronteira HTTP exclusiva
O roteiro SHALL publicar somente o Nginx em 127.0.0.1:8081 em modo explícito. O Spring SHALL exigir proxy real identificado, Host canônico, Origin compatível, JWT HttpOnly e CSRF nas mutações.

#### Scenario: Entrada HTTP fora da fronteira
- **WHEN** uma requisição usa HTTP sem o perfil exclusivo ou apresenta proxy, Host ou Origin incompatível
- **THEN** o Spring a rejeita antes da autenticação

### Requirement: Alternância de transporte preserva dados
O roteiro SHALL usar um único modo web no mesmo projeto, verificar a integridade dos artefatos e preservar as opções de integração.

#### Scenario: Retorno a HTTPS
- **WHEN** HTTPS é selecionado após HTTP
- **THEN** o web retorna às imagens originais e usa o mesmo PostgreSQL sem novo bootstrap
