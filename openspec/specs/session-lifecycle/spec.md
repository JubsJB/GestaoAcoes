# session-lifecycle Specification

## Purpose

Controlar inatividade e revogação de sessões no servidor, distinguindo seus limites da validade absoluta do JWT.

## Requirements

### Requirement: Inatividade autoritativa de dez minutos
O sistema SHALL exigir nova autenticação ao completar dez minutos desde a última atividade recebida, com relógio servidor, inclusive sem Angular conectado. Somente comunicação explícita de interação real pelo cliente oficial SHALL atualizar atividade de sessão ainda válida. Requisições automáticas, consultas, /me e timers MUST NOT renovar o período. Timestamp do cliente MUST NOT determinar o prazo no servidor.

#### Scenario: Borda exata do prazo
- **WHEN** não houve atividade explícita por exatamente dez minutos
- **THEN** mesmo JWT ainda não expirado recebe 401, inclusive em POST /auth/atividade com CSRF válido, e a sessão não pode ser reativada

#### Scenario: Consulta automática contínua
- **WHEN** consultas automáticas são realizadas durante dez minutos sem interação humana
- **THEN** a sessão expira pelo mesmo prazo e os GETs não o prolongam

#### Scenario: Interação antes da borda
- **WHEN** atividade explícita é validada no servidor antes de dez minutos, após eventual espera de lock, e a sessão não está revogada nem expirada
- **THEN** o prazo de inatividade passa a contar do instante servidor dessa comunicação, sem alterar a expiração absoluta

### Requirement: Limite absoluto independente
O sistema SHALL limitar o JWT a quatro horas configuráveis desde a autenticação inicial, mesmo com atividade contínua, sem refresh ou renovação implícita. JWT exp e expiração absoluta persistida SHALL representar o mesmo instante; atividade, reload e abas MUST NOT reemitir JWT ou prolongar esse teto. Reinício de backend MUST NOT perder revogações nem prazos persistidos.

#### Scenario: Usuário continuamente ativo
- **WHEN** passam quatro horas configuráveis desde a autenticação inicial com interações durante todo o período
- **THEN** o usuário precisa autenticar novamente e recebe novo JWT e nova sessão

#### Scenario: Reinício do servidor
- **WHEN** aplicação reinicia após revogação ou após transcorrer o prazo ocioso
- **THEN** token anterior permanece recusado a partir do estado persistido

#### Scenario: Exemplo de jornada
- **WHEN** login ocorre às 08h00 com limite configurado em quatro horas e última atividade válida às 08h30
- **THEN** sem nova interação a sessão não autoriza às 08h40; com atividade válida contínua autoriza somente antes de 12h00 e exige login na igualdade de 12h00

### Requirement: Revogação e identidade atual verificadas
O sistema SHALL verificar estado atual de usuário/sessão em cada requisição e invalidar globalmente todas as sessões na conclusão de bloqueio/desativação, alteração de perfil ou redefinição de senha. Logout SHALL revogar o sid compartilhado; inatividade e limite absoluto SHALL tornar esse sid inelegível sem depender de job ou marcação física. Invalidação global SHALL usar a versão de segurança da conta, sem também exigir atualização em lote das sessões. Habilitação posterior MUST NOT restaurar sessões revogadas. Revogação SHALL atingir toda nova decisão de autenticação após seu commit; request já autorizado pode concluir.

#### Scenario: ADMIN rebaixado
- **WHEN** alteração de ADMIN para USER foi confirmada e o cookie JWT antigo tenta uma escrita com CSRF válido
- **THEN** recebe 401 por revogação, e novo login permite consultas mas retorna 403 para escrita

#### Scenario: Bloqueio e habilitação
- **WHEN** conta é bloqueada e depois habilitada
- **THEN** nenhum token anterior ao bloqueio volta a funcionar, exigindo novo login

#### Scenario: Logout em corrida com atividade
- **WHEN** logout e atualização de atividade concorrem no mesmo sid
- **THEN** atividade não desfaz revogação e requisições posteriores são negadas

#### Scenario: Banco indisponível
- **WHEN** o estado atual da sessão não pode ser verificado
- **THEN** a operação não executa e retorna erro seguro de indisponibilidade sem reutilizar permissões antigas

#### Scenario: Acesso a sessão alheia
- **WHEN** cliente tenta indicar outro usuarioId ou sid no corpo da atividade ou logout
- **THEN** não altera sessão de terceiros, pois a identidade da sessão vem exclusivamente do JWT no cookie validado

### Requirement: Sessão compartilhada entre abas
As abas do mesmo navegador/perfil/origem SHALL compartilhar a mesma sessão de autenticação. Atividade significativa válida em qualquer aba SHALL atualizar o prazo ocioso comum sem alterar limite absoluto. Logout confirmado em uma aba SHALL revogar essa sessão no backend para todas as abas, sem depender de mensagem entre elas. Outros navegadores SHALL permanecer sessões distintas, atingidas pelos eventos de invalidação global da conta.

#### Scenario: Atividade em outra aba
- **WHEN** aba A não tem interação local e aba B registra atividade válida antes do prazo comum
- **THEN** A continua autorizada pela sessão compartilhada e seu timer local não provoca logout indevido

#### Scenario: Logout com sinal perdido
- **WHEN** A confirma logout e B não recebe notificação entre abas
- **THEN** request novo de B com cookie antigo é negado pelo backend; revalidação visual detecta encerramento sem restaurar a sessão

#### Scenario: Administração em outro contexto
- **WHEN** perfil, bloqueio ou senha é alterado com invalidação global confirmada
- **THEN** sessões anteriores em todas as abas e navegadores são recusadas na próxima decisão de autenticação, mesmo antes da atualização visual

### Requirement: Comunicação de atividade sem renovação automática
O cliente oficial SHALL reconhecer interações significativas em documento visível e reduzir notificações duplicadas sem debounce temporal, sem heartbeat por timer e sem tolerância positiva ao prazo de dez minutos. Eventos sintéticos, movimento de ponteiro, foco e notificações automáticas MUST NOT contar. Última interação pendente SHALL ser comunicada quando possível, mas uma comunicação que chega após expiração MUST NOT ressuscitar sessão. Servidor SHALL decidir usando sua hora e estado persistido revalidado após espera de lock.

#### Scenario: Sequência de eventos de uma interação
- **WHEN** clique e navegação ou teclado e edição representam a mesma interação
- **THEN** não geram fila de notificações duplicadas, e a última interação significativa pendente é escoada sem espera artificial de segundos

#### Scenario: Corrida na borda após espera
- **WHEN** atividade aguarda lock e somente pode ser decidida após completar dez minutos sem atividade válida persistida
- **THEN** é recusada sem gravar nova atividade, mesmo se o request começou antes da borda

#### Scenario: Sem eventos significativos
- **WHEN** há apenas movimento do ponteiro, foco, polling ou timers
- **THEN** não há notificação de atividade nem prolongamento da sessão
