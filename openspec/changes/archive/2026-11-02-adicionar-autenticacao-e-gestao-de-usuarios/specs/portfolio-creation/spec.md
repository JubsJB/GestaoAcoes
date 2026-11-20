## MODIFIED Requirements

### Requirement: Modelo persistido da Carteira
O sistema SHALL persistir `id`, `nome` e `dataCriacao` para cada Carteira. O identificador SHALL ser gerado pelo banco, e a criação da Carteira MUST NOT exigir associação com usuário, ação, corretora, operação, posição, snapshot ou histórico. As Carteiras SHALL permanecer compartilhadas entre usuários, sem proprietário individual. A criação SHALL exigir autenticação, autorização ADMIN e proteção CSRF válida; USER MUST NOT criar Carteiras.

#### Scenario: Persistência da Carteira mínima
- **WHEN** uma solicitação válida é concluída
- **THEN** o banco contém uma nova Carteira com identificador gerado, nome validado e data de criação gerada pela aplicação

#### Scenario: Carteira independente de usuário no MVP
- **WHEN** uma Carteira é criada
- **THEN** sua criação exige ADMIN autenticado e CSRF válido, sem associação da Carteira a usuário ou vínculo de propriedade individual

#### Scenario: USER não cria Carteira
- **WHEN** USER autenticado envia `POST /carteiras` com CSRF válido
- **THEN** o sistema responde `403 ACESSO_NEGADO` sem criar Carteira

#### Scenario: Criação exige CSRF
- **WHEN** ADMIN autenticado envia `POST /carteiras` sem CSRF válido
- **THEN** o sistema responde `403 CSRF_INVALIDO` sem criar Carteira
