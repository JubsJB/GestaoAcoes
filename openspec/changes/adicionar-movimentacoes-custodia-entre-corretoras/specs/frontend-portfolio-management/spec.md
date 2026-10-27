## MODIFIED Requirements

### Requirement: Contrato frontend mínimo de Carteiras
A área SHALL manter contratos básicos de carteira e consultas financeiras existentes. Detalhe SHALL consumir histórico financeiro, posições abertas e distribuição separada sem calcular quantidades no navegador; SHALL também consumir rotas de movimentações/estornos de custódia aprovadas. DTO cadastral SHALL permanecer id/nome/dataCriacao e requests de carteira somente nome. Evolução do schema de evidências da distribuição SHALL ser coordenada; demais schemas financeiros não recebem campos de custódia.

#### Scenario: Leitura do DTO básico
- **WHEN** o backend devolve uma Carteira
- **THEN** a aplicação preserva `id`, `nome` e `dataCriacao` sem acrescentar indicadores financeiros

#### Scenario: Requests mínimos
- **WHEN** o usuário cria ou edita uma Carteira
- **THEN** o frontend envia um corpo contendo exatamente `nome`

#### Scenario: Limite funcional
- **WHEN** a capability é apresentada
- **THEN** ela consulta e exibe os dados básicos, histórico de Operações e posições abertas aprovadas, com distribuicao informativa separada,sem acrescentar resumo, patrimônio agregado, evolução, snapshots, gráficos ou conversão cambial

#### Scenario: Limite de custódia
- **WHEN** detalhe consulta movimentações e estornos
- **THEN** histórico próprio é separado de operações e não adiciona gráficos, patrimônio, conversão ou redesign

### Requirement: Distribuicao informativa no detalhe da Carteira
Detalhe -> posições abertas SHALL apresentar distribuição autoritativa de operações e custódias efetivas, sem comprovar custódia externa nem calcular saldos/financeiro. SHALL manter decimal lossless, linhas não zero, negativos, avisos efetivos e reconciliação. SHALL interpretar identidade de evento discriminada e evidência genérica, inclusive desaparecimento de aviso após retroatividade válida. Dados consolidados SHALL permanecer independentes em falha. SHALL recarregar distribuição no contexto correto após operação, movimentação ou estorno; nenhuma atualização de leitura repetirá escrita.

#### Scenario: Dados e aviso de corretora zero
- **WHEN** resposta inclui varias corretoras,categoria nula e aviso historico de grupo zerado
- **THEN** exibe todas linhas nao zero com quantidades exatas e aviso identificavel separado,sem linha zero e sem ocultar negativos

#### Scenario: Loading vazio e erro independentes
- **WHEN** consulta esta carregando,retorna vazio ou falha
- **THEN** anuncia loading,explica ausencia de posicoes abertas ou oferece retry manual respectivamente,sem apagar posicoes consolidadas validas nem afirmar distribuicao sem problemas

#### Scenario: Troca de Carteira e nova operacao
- **WHEN** selecao muda durante consulta ou operacao e registrada no detalhe
- **THEN** nao mostra distribuicao antiga como atual,e recarrega o contexto correto apos operacao,sem polling ou mutacao adicional

#### Scenario: Divergencia de quantidades
- **WHEN** backend sinaliza reconciliacao divergente
- **THEN** mostra aviso e quantidades autoritativas sem recalcular,corrigir ou apresentar distribuicao como reconciliada

#### Scenario: Acessibilidade e responsividade
- **WHEN** usuario navega por teclado/leitor de tela ou viewport360px
- **THEN** controles tem nome/foco,loading e avisos sao anunciados sem concorrencia desnecessaria,quantidades e corretoras permanecem legiveis,e problemas nao dependem somente de cor

#### Scenario: Contratos e superficies anteriores
- **WHEN** nova consulta falha ou nao e utilizada
- **THEN** listagem de Carteiras,historico,formulario de operacoes e Dashboard preservam contratos e calculos existentes

#### Scenario: Diagnostico unico sem recalc
- **WHEN** mesmo agrupamento possui flags de saldo negativo atual e deficit historico ou finalzero
- **THEN** renderiza no maximo um aviso por grupo inconsistente,com nome e primeira evidencia recebidos,nao alerta grupos saudaveis e nao cria linha quantitativa zero;divergencia da posicao e aviso distinto sem corretora ficticia

#### Scenario: Snapshots independentes
- **WHEN** GET de distribuicao e GET de posicoes retornam quantidades diferentes por aquisicao em instantes distintos
- **THEN** apresenta dados da propria resposta identificada,nao compara/soma no navegador,nem cria falso diagnostico ou sobrescreve tabela consolidada;usa apenas reconciliada/diagnosticoReconciliacao autoritativos

#### Scenario: Sem graficos de participacao
- **WHEN** distribuicao apresenta positivos,negativos ou divergencia
- **THEN** nao apresenta percentuais,roscas,graficos de participacao,modulo de negativos ou normalizacao de subconjuntos;preserva valores textuais e todos os avisos pertinentes

#### Scenario: Atualização após estorno
- **WHEN** estorno conclui e distribuição é recarregada
- **THEN** mostra neutralização efetiva sem ocultar histórico original

## ADDED Requirements

### Requirement: Formulário contextual de transferência e atribuição
Detalhe SHALL oferecer formulário próprio, carteira fixa do contexto, ação existente, natureza, origem/destino, quantidade textual exata e data efetiva. TRANSFERENCIA exige duas corretoras distintas; ATRIBUICAO fixa origem Sem corretora informada sem entidade fictícia. Saldo exibido SHALL ser auxiliar e identificado como atual, não disponibilidade garantida na data retroativa. Backend SHALL escolher posição e validar saldo. Não haverá preço, valor total, controle manual de posição, cadastro de referência ou transferência entre carteiras.

#### Scenario: Transferência e atribuição no formulário
- **WHEN** usuário alterna natureza
- **THEN** origem é selecionável na transferência e categoria fixa na atribuição; destino sempre corretora cadastrada

#### Scenario: Saldo auxiliar e retroatividade
- **WHEN** painel mostra saldo atual e usuário informa data anterior
- **THEN** interface informa que backend validará posição/histórico, sem usar saldo atual como garantia

#### Scenario: Precisão e validação estrutural
- **WHEN** quantidade/data/direção são inválidas
- **THEN** identifica campos, conserva strings lossless e não envia request estruturalmente inválido

#### Scenario: Troca de contexto
- **WHEN** carteira muda durante consulta ou abertura de dialog
- **THEN** respostas antigas não atualizam nova carteira e escrita permanece vinculada ao contexto capturado

### Requirement: Histórico e confirmação de estorno acessíveis
Detalhe SHALL exibir histórico unificado de custódia separado do financeiro, natureza, situação, ativo, quantidade, origem/destino, datas e vínculos. SHALL ordenar por registro decrescente e aplicar filtros do contrato. Original SHALL ter ATIVA/ESTORNADA; estorno terá ESTORNO e situação null. Confirmação SHALL explicar neutralização retroativa, preservar original, oferecer motivo opcional e não efetuar POST por abertura/cancelamento. SHALL comunicar conflitos específicos e duplicidade, com acessibilidade, responsividade e estados loading/vazio/erro/retry próprios.

#### Scenario: Original e estorno vinculados
- **WHEN** histórico contém original estornada
- **THEN** mostra duas linhas auditáveis, vínculo visual e explicação de neutralização, sem transferência inversa

#### Scenario: Filtros no frontend
- **WHEN** usuário escolhe natureza/situação
- **THEN** envia filtros do contrato, exibe situações de originais e estornos somente conforme política especificada

#### Scenario: Confirmação ou cancelamento
- **WHEN** usuário abre confirmação e cancela
- **THEN** não há POST; confirmação explícita envia somente motivo opcional

#### Scenario: Conflito no estorno
- **WHEN** backend informa duplicidade, déficit ou custódia invalidada
- **THEN** explica conflito e mantém estado sem estorno fictício ou cascata

### Requirement: Sucesso de escrita separado da falha de recarga
Após movimentação/estorno confirmado a interface SHALL atualizar histórico e solicitar distribuição atualizada, com cancelamento/geração de respostas obsoletas. Se leitura falhar após sucesso, SHALL informar registro concluído e falha de atualização separadamente, oferecendo retry de GET. MUST NOT repetir POST, apagar dados financeiros válidos ou afirmar falha do cadastro confirmado.

#### Scenario: Falha de recarga após escrita concluída
- **WHEN** POST retorna201 e GET seguinte falha
- **THEN** mostra sucesso de cadastro/estorno e erro de leitura; retry repete apenas GET

#### Scenario: Respostas atrasadas
- **WHEN** consulta anterior retorna após troca de carteira ou nova recarga
- **THEN** não sobrescreve contexto ou geração atual
