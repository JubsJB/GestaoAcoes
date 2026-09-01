## MODIFIED Requirements

### Requirement: Busca exata por CNPJ
A página de Corretoras SHALL permitir buscar uma Corretora por CNPJ usando `GET /corretoras/por-cnpj?cnpj=...` somente após ação explícita do usuário. A busca SHALL aceitar entrada com ou sem máscara, preservar o CNPJ pesquisado e a coleção completa, MUST NOT consultar durante digitação e SHALL classificar como ausência local somente `status=404` com `code=null`. Essa ausência SHALL abrir dialog informativo centralizado; estado vazio de coleção e erros técnicos ou de provider MUST NOT usar esse dialog.

#### Scenario: Busca com resultado
- **WHEN** o usuário informa um CNPJ com formato aceito e o backend devolve uma Corretora
- **THEN** a aplicação conduz o usuário ao detalhe do registro devolvido

#### Scenario: Busca sem correspondência
- **WHEN** a busca responde `404` com `code=null`
- **THEN** a página abre dialog “Corretora não cadastrada”, exibe o CNPJ pesquisado formatado, informa que nenhuma Corretora cadastrada foi encontrada, preserva a coleção original e oferece as ações “Cancelar” ou “Fechar” e “Cadastrar corretora”

#### Scenario: Fechamento do dialog de ausência local
- **WHEN** o usuário cancela, aciona o backdrop ou pressiona Escape no dialog
- **THEN** o dialog fecha, o foco retorna ao contexto da busca e nenhuma navegação ou requisição HTTP adicional é realizada

#### Scenario: Cadastro a partir do dialog
- **WHEN** o usuário aciona “Cadastrar corretora” no dialog
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

### Requirement: Cadastro inicial somente por CNPJ
A aplicação SHALL oferecer o formulário Typed Reactive Forms de cadastro com somente CNPJ como dado permanente, preferencialmente em `MatDialog` quando iniciado pela listagem ou por ausência local, e SHALL preservar `/corretoras/nova` para acesso direto. O fluxo contextual MAY preencher somente o controle de CNPJ pesquisado, mas MUST NOT submeter automaticamente, persistir o prefill, usar query parameter ou alterar o payload aprovado. O frontend MUST NOT solicitar ou preencher preventivamente razão social, nome fantasia, endereço, telefone, e-mail, situação cadastral ou outros dados cadastrais retornados pelo backend ou por providers, nem exibir `confirmarSituacaoCadastralNaoAtiva` como controle permanente ou preventivo. Esse controle SHALL permanecer exclusivamente contextual após resposta `409` com `code=SITUACAO_CADASTRAL_NAO_ATIVA`.

#### Scenario: Cadastro iniciado pela listagem
- **WHEN** o usuário aciona “Cadastrar corretora” na listagem
- **THEN** a aplicação abre dialog acessível com o formulário contendo somente CNPJ vazio e não realiza requisição até submissão explícita

#### Scenario: Primeira tentativa válida
- **WHEN** o usuário submete um CNPJ com formato básico aceito
- **THEN** a aplicação envia `POST /corretoras` com corpo contendo somente `cnpj`

#### Scenario: Prefill transitório válido
- **WHEN** o CTA de ausência local abre o cadastro contextual com CNPJ compatível
- **THEN** somente o campo CNPJ inicia preenchido e aguarda submissão explícita do usuário

#### Scenario: Prefill ausente ou inválido
- **WHEN** o cadastro é acessado diretamente, recarregado ou recebe estado incompatível
- **THEN** o formulário inicia vazio sem consultar ou submeter dados

#### Scenario: Rota direta preservada
- **WHEN** o usuário acessa `/corretoras/nova` diretamente
- **THEN** a página de cadastro permanece disponível com o mesmo formulário e contrato HTTP

#### Scenario: Cancelamento do cadastro contextual
- **WHEN** o usuário cancela o dialog de cadastro antes de submeter
- **THEN** o dialog fecha sem requisição HTTP e preserva a listagem e a busca atuais

#### Scenario: Validação de experiência
- **WHEN** o campo está vazio, incompleto ou contém formato não aceito
- **THEN** o formulário apresenta orientação acessível e não envia a requisição

#### Scenario: Autoridade do backend
- **WHEN** o CNPJ possui formato básico aceito pelo frontend
- **THEN** validade algorítmica, existência, unicidade, dados cadastrais, endereço e situação continuam sendo decididos pelo backend

#### Scenario: Submissão em andamento
- **WHEN** uma tentativa de cadastro está em andamento
- **THEN** o formulário evita submissões concorrentes e comunica o processamento

### Requirement: Detalhe completo da Corretora
A aplicação SHALL disponibilizar `/corretoras/:id`, apresentar o contrato completo e formatar `dataCadastro` somente na apresentação como `dd/MM/yyyy às HH:mm`, em `pt-BR` e timezone local do navegador. O DTO SHALL permanecer inalterado e o estado transitório compatível SHALL continuar evitando GET redundante.

#### Scenario: Detalhe existente
- **WHEN** o backend devolve a Corretora solicitada por ID
- **THEN** a página apresenta identificação, contatos, endereço, situação, validação financeira e data de cadastro no padrão aprovado

#### Scenario: Detalhe sem estado transitório
- **WHEN** `/corretoras/:id` é acessada diretamente, recarregada ou aberta sem DTO previamente disponível
- **THEN** a aplicação consulta `GET /corretoras/{id}` e apresenta o contrato devolvido

#### Scenario: Campos opcionais ausentes
- **WHEN** nome fantasia, e-mail, telefone, número ou complemento possuem valor nulo
- **THEN** a interface indica a ausência sem inventar, ocultar o registro ou exibir `null`

#### Scenario: Validação financeira pendente
- **WHEN** `validadaMercadoFinanceiro` é falso
- **THEN** a interface comunica que a validação ainda não foi realizada sem afirmar que a instituição não pertence ao mercado financeiro

#### Scenario: Detalhe inexistente
- **WHEN** a consulta por ID responde que a Corretora não existe
- **THEN** a página apresenta estado de não encontrado e forma de retornar à listagem

### Requirement: Resultado e erros das operações
A área SHALL comunicar sucesso, warning, ausência local e falha de forma visualmente distinta e contextual, preservando `status`, `code`, `message` e `details`. Feedback relevante de página SHALL aparecer após o cabeçalho quando apropriado; validações permanecem junto aos campos e toast superior pode continuar para sucesso transitório.

#### Scenario: Cadastro concluído
- **WHEN** uma tentativa no dialog devolve a Corretora criada
- **THEN** a aplicação fecha o dialog, mantém a listagem ativa, incorpora o DTO devolvido à coleção sem GET redundante e comunica sucesso por toast

#### Scenario: Cadastro concluído pela rota direta
- **WHEN** uma tentativa aprovada em `/corretoras/nova` devolve a Corretora criada
- **THEN** a aplicação preserva o fluxo de rota vigente, transporta o DTO completo e não executa GET redundante

#### Scenario: Erro padronizado
- **WHEN** uma operação falha com erro padronizado diferente do conflito contextual aprovado
- **THEN** a interface apresenta mensagem e detalhes coerentes sem perder seus dados técnicos nem inventar regra

#### Scenario: Falha técnica
- **WHEN** uma operação falha sem corpo padronizado
- **THEN** a interface apresenta falha técnica normalizada e recuperação quando aplicável

#### Scenario: Confirmação de situação não ativa preservada
- **WHEN** o cadastro responde `409` com `code=SITUACAO_CADASTRAL_NAO_ATIVA`
- **THEN** o fluxo existente de confirmação explícita permanece inalterado
