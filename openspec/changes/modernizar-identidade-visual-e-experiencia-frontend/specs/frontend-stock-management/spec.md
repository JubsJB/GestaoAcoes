## MODIFIED Requirements

### Requirement: Tratamento contextual preservando StandardError
A feature SHALL reutilizar a normalização HTTP central e preservar internamente `status`, `code`, `message` e `details`. A apresentação SHALL manter mensagens contextualizadas, incluindo duplicidade de ticker e mercado, sem renderizar indiscriminadamente o objeto técnico de detalhes. Cadastro SHALL apresentar o alerta junto ao formulário e conservar os dados preenchidos e editáveis após falha. A feature MUST NOT criar outro interceptor, realizar retry automático ou classificar erro externo como ausência local. O comportamento de atualização manual e confirmação de cotação preservada SHALL permanecer inalterado.

#### Scenario: Ticker inexistente no provider
- **WHEN** cadastro ou atualização responde `404` com `code=TICKER_INEXISTENTE`
- **THEN** a aplicação apresenta erro de validação externa distinto da ausência de registro local

#### Scenario: Dados ou cotação inválidos
- **WHEN** o backend responde `DADOS_EXTERNOS_INCOMPLETOS`, `COTACAO_INDISPONIVEL` ou `COTACAO_FORA_DA_PRECISAO`
- **THEN** a aplicação apresenta a falha sem inventar cotação nem alterar o DTO atual

#### Scenario: Resposta externa inválida
- **WHEN** o backend responde `502` com `code=RESPOSTA_EXTERNA_INVALIDA`
- **THEN** a aplicação conserva mensagem e detalhes internamente e apresenta feedback contextual sem despejar campos técnicos

#### Scenario: Serviço externo indisponível
- **WHEN** o backend responde `503` com `code=SERVICO_EXTERNO_INDISPONIVEL`
- **THEN** a aplicação informa indisponibilidade sem provider direto nem retry automático

#### Scenario: Timeout externo
- **WHEN** o backend responde `504` com `code=SERVICO_EXTERNO_TIMEOUT`
- **THEN** a aplicação informa timeout e permite somente tentativa manual posterior

#### Scenario: Duplicidade de ticker e mercado
- **WHEN** o cadastro recebe `409` com `code=ACAO_DUPLICADA`
- **THEN** apresenta a mensagem de duplicidade junto ao formulário, mantém ticker e mercado preenchidos e editáveis, conserva o erro normalizado integral internamente e não expõe o objeto técnico, navega ou repete POST automaticamente

### Requirement: Busca exata e explícita por ticker e mercado
A listagem SHALL permitir consulta singular somente por ação explícita, exigindo ticker e mercado, preservando ambos os termos e a coleção completa. A busca MUST NOT realizar requests durante digitação e SHALL classificar como ausência local somente `status=404` com `code=null`, sem confundir o caso com `TICKER_INEXISTENTE` ou outra falha de provider. Essa ausência SHALL abrir dialog informativo centralizado; estado vazio de coleção e erros técnicos ou de provider MUST NOT usar esse dialog. O convite e a abertura de cadastro contextual SHALL respeitar o perfil ADMIN existente; USER SHALL receber somente a informação de ausência e a ação de retorno à listagem.

#### Scenario: Busca explícita encontrada
- **WHEN** o usuário informa ticker e mercado válidos e confirma a busca
- **THEN** a aplicação solicita `GET /acoes/por-ticker` com ambos os parâmetros e navega ao detalhe com DTO transitório

#### Scenario: Digitação sem consulta
- **WHEN** o usuário digita ou altera ticker ou mercado sem submeter
- **THEN** nenhuma requisição de busca é realizada

#### Scenario: Busca incompleta
- **WHEN** ticker ou mercado não foi informado
- **THEN** a aplicação comunica a validação de UX e não realiza a requisição

#### Scenario: Ação não cadastrada localmente
- **WHEN** uma ausência local já classificada precisa ser apresentada
- **THEN** a página abre dialog “Ação não cadastrada”, mostra ticker e mercado pesquisados e preserva a coleção; ADMIN recebe Cancelar e Cadastrar ação, enquanto USER recebe Voltar à listagem, sem convite ao cadastro

#### Scenario: Combinação local inexistente
- **WHEN** a busca responde `404` com `code=null`
- **THEN** a aplicação classifica o caso como ausência local, preserva ticker, mercado e coleção e não o interpreta como ticker inexistente no provider

#### Scenario: Fechamento do dialog de ausência local
- **WHEN** o usuário cancela, retorna à listagem, aciona o backdrop ou pressiona Escape no dialog
- **THEN** o dialog fecha, o foco retorna ao contexto da busca e nenhuma navegação ou requisição HTTP adicional é realizada

#### Scenario: Cadastro a partir do dialog
- **WHEN** ADMIN aciona “Cadastrar ação” no dialog
- **THEN** a aplicação fecha o dialog informativo e abre o dialog de cadastro com somente ticker e mercado pesquisados preenchidos, sem POST, GET ou provider call automático

#### Scenario: Ticker inexistente no provider
- **WHEN** um fluxo responde com `code=TICKER_INEXISTENTE`
- **THEN** a aplicação preserva o erro externo e não o apresenta como ausência local

#### Scenario: Limpeza da busca
- **WHEN** o usuário limpa ou cancela a busca
- **THEN** a coleção completa preservada volta imediatamente sem novo `GET /acoes`

### Requirement: Comunicação explícita da cotação preservada
Após falha da atualização manual, o detalhe SHALL informar preservação somente quando o backend confirmar `details.cotacaoPreservada === true` e houver uma ação carregada. O valor e a referência temporal exibidos SHALL permanecer os últimos disponíveis até uma resposta de sucesso. O aviso confirmado SHALL permanecer durante nova tentativa e SHALL desaparecer após sucesso, inclusive quando a resposta não alterar a cotação. Sem confirmação explícita, a aplicação MUST NOT afirmar preservação. Tentativas SHALL continuar exclusivamente manuais. O erro normalizado completo SHALL ser conservado internamente, mas sua apresentação SHALL usar mensagens contextualizadas sem expor indiscriminadamente mensagens ou detalhes técnicos internos.

#### Scenario: Atualização falha com cotação anterior
- **WHEN** uma atualização manual falha e o backend confirma cotacaoPreservada como booleano true
- **THEN** o detalhe informa preservação, mantém o DTO anterior, conserva o erro normalizado internamente e apresenta uma única mensagem contextualizada, sem repetir a chamada

#### Scenario: Recuperação manual
- **WHEN** o usuário tenta novamente após falha com preservação confirmada
- **THEN** o aviso permanece durante a chamada e desaparece após sucesso; valor e metadados passam a refletir a resposta, mesmo que não tenham mudado

#### Scenario: Ausência de falha de atualização
- **WHEN** o detalhe é carregado inicialmente ou falha sem obter uma ação
- **THEN** não afirma que uma cotação foi preservada após atualização

#### Scenario: Preservação não confirmada
- **WHEN** o erro não contém confirmação booleana true, inclusive falha de transporte com resultado incerto
- **THEN** a aplicação apresenta erro contextualizado sem afirmar que o backend preservou a cotação
