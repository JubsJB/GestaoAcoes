## MODIFIED Requirements

### Requirement: Resultado e erros das operações
A área SHALL comunicar sucesso, warning, ausência local e falha de forma visualmente distinta e contextual, preservando internamente o erro normalizado integral (`status`, `code`, `message` e `details`). Feedback relevante de página SHALL aparecer após o cabeçalho quando apropriado; validações permanecem junto aos campos e toast superior pode continuar para sucesso transitório. As mensagens contextuais e a situação necessária à confirmação SHALL continuar disponíveis, mas a interface MUST NOT renderizar indiscriminadamente as chaves e valores técnicos de `details`.

#### Scenario: Cadastro concluído
- **WHEN** uma tentativa no dialog devolve a Corretora criada
- **THEN** a aplicação fecha o dialog, mantém a listagem ativa, incorpora o DTO devolvido à coleção sem GET redundante e comunica sucesso por toast

#### Scenario: Cadastro concluído pela rota direta
- **WHEN** uma tentativa aprovada em `/corretoras/nova` devolve a Corretora criada
- **THEN** a aplicação preserva o fluxo de rota vigente, transporta o DTO completo e não executa GET redundante

#### Scenario: Erro padronizado
- **WHEN** uma operação falha com erro padronizado diferente do conflito contextual aprovado
- **THEN** a interface apresenta mensagem contextual, sem exibir automaticamente detalhes técnicos; o erro completo permanece disponível internamente, sem inventar regra ou perder sua classificação

#### Scenario: Falha técnica
- **WHEN** uma operação falha sem corpo padronizado
- **THEN** a interface apresenta falha técnica normalizada e recuperação quando aplicável

#### Scenario: Confirmação de situação não ativa preservada
- **WHEN** o cadastro responde `409` com `code=SITUACAO_CADASTRAL_NAO_ATIVA`
- **THEN** o fluxo existente de confirmação explícita permanece inalterado, informa inicialmente que a situação não é ATIVA, apresenta a situação quando disponível e pergunta “Deseja continuar com o cadastro desta instituição?”, sem repetir visualmente a mensagem inferior original nem alterar o objeto recebido

### Requirement: Busca exata por CNPJ
A página de Corretoras SHALL permitir buscar uma Corretora por CNPJ usando `GET /corretoras/por-cnpj?cnpj=...` somente após ação explícita do usuário. A busca SHALL aceitar entrada com ou sem máscara, preservar o CNPJ pesquisado e a coleção completa, MUST NOT consultar durante digitação e SHALL classificar como ausência local somente `status=404` com `code=null`. Essa ausência SHALL abrir dialog informativo centralizado, com convite ao cadastro e prefill somente para ADMIN; USER SHALL receber a informação de ausência e a opção de retorno à listagem, sem convite ao cadastro; estado vazio de coleção e erros técnicos ou de provider MUST NOT usar esse dialog.

#### Scenario: Busca com resultado
- **WHEN** o usuário informa um CNPJ com formato aceito e o backend devolve uma Corretora
- **THEN** a aplicação conduz o usuário ao detalhe do registro devolvido

#### Scenario: Busca sem correspondência
- **WHEN** a busca responde `404` com `code=null`
- **THEN** a página abre dialog “Corretora não cadastrada”, exibe o CNPJ pesquisado formatado, informa que nenhuma Corretora cadastrada foi encontrada, preserva a coleção original e oferece Cancelar e Cadastrar corretora somente para ADMIN; USER recebe somente Voltar à listagem

#### Scenario: Fechamento do dialog de ausência local
- **WHEN** o usuário cancela, retorna à listagem, aciona o backdrop ou pressiona Escape no dialog
- **THEN** o dialog fecha, o foco retorna ao contexto da busca e nenhuma navegação ou requisição HTTP adicional é realizada

#### Scenario: Cadastro a partir do dialog
- **WHEN** ADMIN aciona “Cadastrar corretora” no dialog
- **THEN** a aplicação fecha o dialog informativo e abre o dialog de cadastro com somente o CNPJ pesquisado preenchido, sem POST ou GET adicional automático

#### Scenario: Erro com código preenchido
- **WHEN** a busca responde `404` ou outro status com `code` preenchido
- **THEN** a página preserva o tratamento contextual do `StandardError` e não classifica a falha como ausência local

#### Scenario: Busca sem entrada utilizável
- **WHEN** o CNPJ está vazio ou não possui o formato básico aceito
- **THEN** a aplicação orienta a correção e não envia a consulta

#### Scenario: Digitação sem consulta
- **WHEN** o usuário altera o CNPJ sem submeter a busca
- **THEN** nenhuma requisição de busca é realizada

#### Scenario: Limpeza da busca
- **WHEN** o usuário limpa ou cancela a busca após a listagem completa ter sido carregada
- **THEN** a aplicação restaura imediatamente a coleção preservada sem novo `GET /corretoras`
