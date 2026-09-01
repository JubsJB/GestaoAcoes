## MODIFIED Requirements

### Requirement: Busca exata e explícita por ticker e mercado
A listagem SHALL permitir consulta singular somente por ação explícita, exigindo ticker e mercado, preservando ambos os termos e a coleção completa. A busca MUST NOT realizar requests durante digitação e SHALL classificar como ausência local somente `status=404` com `code=null`, sem confundir o caso com `TICKER_INEXISTENTE` ou outra falha de provider. Essa ausência SHALL abrir dialog informativo centralizado; estado vazio de coleção e erros técnicos ou de provider MUST NOT usar esse dialog.

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
- **THEN** a página abre dialog “Ação não cadastrada”, mostra ticker e mercado pesquisados, informa que nenhuma Ação cadastrada foi encontrada, preserva a coleção e oferece as ações “Cancelar” ou “Fechar” e “Cadastrar ação”

#### Scenario: Combinação local inexistente
- **WHEN** a busca responde `404` com `code=null`
- **THEN** a aplicação classifica o caso como ausência local, preserva ticker, mercado e coleção e não o interpreta como ticker inexistente no provider

#### Scenario: Fechamento do dialog de ausência local
- **WHEN** o usuário cancela, aciona o backdrop ou pressiona Escape no dialog
- **THEN** o dialog fecha, o foco retorna ao contexto da busca e nenhuma navegação ou requisição HTTP adicional é realizada

#### Scenario: Cadastro a partir do dialog
- **WHEN** o usuário aciona “Cadastrar ação” no dialog
- **THEN** a aplicação fecha o dialog informativo e abre o dialog de cadastro com somente ticker e mercado pesquisados preenchidos, sem POST, GET ou provider call automático

#### Scenario: Ticker inexistente no provider
- **WHEN** um fluxo responde com `code=TICKER_INEXISTENTE`
- **THEN** a aplicação preserva o erro externo e não o apresenta como ausência local

#### Scenario: Limpeza da busca
- **WHEN** o usuário limpa ou cancela a busca
- **THEN** a coleção completa preservada volta imediatamente sem novo `GET /acoes`

### Requirement: Cadastro somente por ticker e mercado
A aplicação SHALL cadastrar enviando exclusivamente ticker e mercado por Typed Reactive Forms, preferencialmente em `MatDialog` quando iniciado pela listagem ou por ausência local, e SHALL preservar `/acoes/nova` para acesso direto. O fluxo contextual MAY preencher somente esses dois controles com ticker e mercado válidos, mas MUST NOT submeter automaticamente, chamar provider, persistir o prefill, usar query parameters ou alterar o contrato HTTP. O frontend MUST NOT solicitar moeda, nome da empresa, cotação, data/hora de cotação, ticker canônico ou qualquer outro dado derivado do backend ou de provider.

#### Scenario: Cadastro iniciado pela listagem
- **WHEN** o usuário aciona “Cadastrar ação” na listagem
- **THEN** a aplicação abre dialog acessível com ticker vazio e mercado sem seleção e não realiza requisição até submissão explícita

#### Scenario: Formulário inicial direto
- **WHEN** o cadastro é acessado diretamente ou por refresh
- **THEN** ticker inicia vazio e mercado sem seleção

#### Scenario: Formulário inicial
- **WHEN** o cadastro é apresentado
- **THEN** somente ticker e seleção obrigatória entre `BRASIL` e `EUA` são solicitados

#### Scenario: Prefill transitório válido
- **WHEN** o CTA de ausência local abre o cadastro contextual com ticker e mercado tipados
- **THEN** os controles iniciam preenchidos e aguardam submissão explícita

#### Scenario: Prefill inválido
- **WHEN** o estado transitório contém ticker ou mercado incompatível
- **THEN** o formulário ignora o prefill inválido sem request automático

#### Scenario: Rota direta preservada
- **WHEN** o usuário acessa `/acoes/nova` diretamente
- **THEN** a página de cadastro permanece disponível com o mesmo formulário e contrato HTTP

#### Scenario: Cancelamento do cadastro contextual
- **WHEN** o usuário cancela o dialog de cadastro antes de submeter
- **THEN** o dialog fecha sem requisição HTTP e preserva a listagem e a busca atuais

#### Scenario: Payload do cadastro
- **WHEN** o usuário submete formulário válido
- **THEN** a aplicação envia `POST /acoes` com exatamente ticker e mercado

#### Scenario: Normalização leve de entrada
- **WHEN** o ticker contém espaços nas extremidades ou letras minúsculas
- **THEN** a aplicação pode aplicar trim e uppercase sem alterar caracteres internos, sufixos ou decidir ticker canônico

#### Scenario: Submissão concorrente
- **WHEN** um cadastro está em andamento
- **THEN** nova submissão permanece bloqueada até o fluxo terminar

#### Scenario: Cadastro concluído
- **WHEN** o backend responde `201 Created` ao formulário em dialog
- **THEN** a aplicação fecha o dialog, mantém a listagem ativa, incorpora o DTO devolvido à coleção sem GET redundante e comunica sucesso por toast

#### Scenario: Cadastro concluído pela rota direta
- **WHEN** o backend responde `201 Created` ao formulário acessado em `/acoes/nova`
- **THEN** a aplicação preserva o fluxo de rota vigente e transporta o DTO completo sem GET redundante

#### Scenario: Cadastro duplicado
- **WHEN** o cadastro responde `409` com `code=ACAO_DUPLICADA`
- **THEN** a aplicação mantém conflito e detalhes sem diálogo, retry automático ou segundo POST

### Requirement: Tratamento contextual preservando StandardError
A feature SHALL reutilizar a normalização HTTP central, preservar `status`, `code`, `message` e `details` e apresentar feedback relevante em posição de destaque após o cabeçalho quando semanticamente apropriado. Ela MUST NOT criar outro interceptor, esconder detalhes, realizar retry automático ou classificar erro externo como ausência local.

#### Scenario: Ticker inexistente no provider
- **WHEN** cadastro ou atualização responde `404` com `code=TICKER_INEXISTENTE`
- **THEN** a aplicação apresenta erro de validação externa distinto da ausência de registro local

#### Scenario: Dados ou cotação inválidos
- **WHEN** o backend responde `DADOS_EXTERNOS_INCOMPLETOS`, `COTACAO_INDISPONIVEL` ou `COTACAO_FORA_DA_PRECISAO`
- **THEN** a aplicação apresenta a falha sem inventar cotação nem alterar o DTO atual

#### Scenario: Resposta externa inválida
- **WHEN** o backend responde `502` com `code=RESPOSTA_EXTERNA_INVALIDA`
- **THEN** a aplicação mantém mensagem e detalhes disponíveis

#### Scenario: Serviço externo indisponível
- **WHEN** o backend responde `503` com `code=SERVICO_EXTERNO_INDISPONIVEL`
- **THEN** a aplicação informa indisponibilidade sem provider direto nem retry automático

#### Scenario: Timeout externo
- **WHEN** o backend responde `504` com `code=SERVICO_EXTERNO_TIMEOUT`
- **THEN** a aplicação informa timeout e permite somente tentativa manual posterior

### Requirement: Apresentação responsiva sem cálculo financeiro
A aplicação SHALL apresentar ticker, empresa, mercado, moeda, `cotacaoAtual` e referência temporal fornecidos pelo backend de modo responsivo, usando cabeçalho e superfícies coerentes. `dataHoraCotacao` SHALL ser formatada somente na apresentação como `dd/MM/yyyy às HH:mm`, em `pt-BR` e timezone local do navegador, sem alterar o DTO, converter moeda ou calcular resultado financeiro. A cotação SHALL ser identificada como a última cotação persistida e MUST NOT ser apresentada como garantia de valor em tempo real, ao vivo ou equivalente. A apresentação MUST NOT introduzir polling ou atualização automática e SHALL preservar o fluxo de atualização manual existente.

#### Scenario: Mercado e moeda apresentados
- **WHEN** uma Ação é exibida
- **THEN** mercado aparece amigavelmente e cotação é formatada conforme BRL ou USD recebido

#### Scenario: Data e cotação
- **WHEN** cotação e `dataHoraCotacao` são exibidas
- **THEN** a interface identifica a última cotação persistida, mostra a data no padrão aprovado e não promete valor em tempo real

#### Scenario: Nome empresarial longo
- **WHEN** o nome da empresa é extenso
- **THEN** ele quebra de forma legível sem truncamento obrigatório ou overflow horizontal

#### Scenario: Viewport compacto
- **WHEN** listagem ou detalhe é exibido em tela compacta
- **THEN** cards, feedbacks e ações permanecem legíveis e operáveis em coluna
