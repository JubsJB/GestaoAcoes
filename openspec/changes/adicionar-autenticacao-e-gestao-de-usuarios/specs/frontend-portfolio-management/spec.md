## MODIFIED Requirements

### Requirement: Detalhe reativo e seleção global
Após autenticação e autorização, o detalhe SHALL reagir a mudanças do identificador na mesma rota, validar a Carteira, selecionar a abertura válida em memória da sessão autenticada, sem preferência global persistida entre identidades, e invalidar imediatamente dados e requests do contexto anterior. Estado transitório SHALL ser aceito somente quando compatível com o ID atual. Trocar a Carteira no shell estando no detalhe SHALL navegar ao novo detalhe.

#### Scenario: Troca sem recriar componente
- **WHEN** `/carteiras/{id}` muda para outro ID na mesma instância de página
- **THEN** dados básicos, posições e histórico acompanham o novo ID e respostas tardias não sobrescrevem a nova Carteira

#### Scenario: Detalhe indisponível
- **WHEN** o ID é malformado ou a Carteira não existe
- **THEN** o detalhe mostra erro ou não encontrado, não ativa silenciosamente outra Carteira e oferece retorno à listagem

#### Scenario: Reload do detalhe com cookie válido
- **WHEN** detalhe é recarregado com sessão válida e ID na URL
- **THEN** /me restaura identidade sem registrar atividade e o detalhe carrega carteira validada sem novo login ou restauração de dados financeiros não salvos
