## ADDED Requirements

### Requirement: Erros de segurança padronizados
O sistema SHALL produzir StandardError também para falhas anteriores ao controller: 401 AUTENTICACAO_NECESSARIA para cookie ausente/JWT inválido, expirado ou revogado, 401 SESSAO_EXPIRADA quando timeout de sessão autenticável for identificado e 403 ACESSO_NEGADO para perfil insuficiente. CSRF ausente/inválido SHALL responder 403 CSRF_INVALIDO, sem valores esperados; em mutações esse filtro pode preceder autenticação. Testes de escrita 401 ou perfil 403 SHALL fornecer CSRF válido. SHALL manter timeStamp, status, error, message, path, code e details seguros. Falha ao verificar estado persistido SHALL negar operação com indisponibilidade segura, sem masquerar falha de infraestrutura como login inválido. Segurança SHALL preceder execução financeira e não alterar códigos financeiros depois de autorizado.

#### Scenario: Filtro recusa cookie JWT
- **WHEN** token inválido é rejeitado antes do controller
- **THEN** resposta 401 usa o envelope existente sem stack trace ou JWT

#### Scenario: Perfil insuficiente
- **WHEN** USER autenticado tenta escrita
- **THEN** resposta 403 usa ACESSO_NEGADO sem executar negócio

#### Scenario: ADMIN recebe erro de domínio
- **WHEN** ADMIN autorizado tenta venda financeiramente inválida
- **THEN** o contrato financeiro vigente é preservado sem converter o conflito em erro de autenticação

#### Scenario: Dados de segurança indisponíveis
- **WHEN** banco não permite verificar sessão
- **THEN** retorna 503 seguro sem executar operação protegida nem expor causa interna

#### Scenario: Precedência CSRF
- **WHEN** escrita chega sem CSRF válido e sem cookie de autenticação válido
- **THEN** recebe 403 CSRF_INVALIDO antes de efeitos; com CSRF válido mas sem autenticação recebe 401, sem prometer precedência inversa

#### Scenario: CSRF não encerra sessão válida
- **WHEN** cookie autenticado é válido mas header CSRF não corresponde
- **THEN** retorna 403 CSRF_INVALIDO sem logout automático, atualização de atividade ou repetição da escrita
