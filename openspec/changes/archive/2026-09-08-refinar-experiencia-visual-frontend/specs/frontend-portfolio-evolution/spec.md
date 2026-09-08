## MODIFIED Requirements

### Requirement: Garantir experiência acessível e responsiva
A seção SHALL possuir heading, descrição textual, identificação não cromática das moedas, foco visível e operação por mouse, teclado e toque. Loading e criação SHALL ser anunciados polidamente; erros SHALL ser anunciados adequadamente. Pontos interativos e tooltip, quando existentes, SHALL ser acessíveis por foco e a alternativa textual SHALL ser a fonte acessível principal, com o SVG tratado como visualização complementar. Desktop, tablet e mobile SHALL preservar dados essenciais sem overflow horizontal obrigatório da página. Dimensões, labels, contraste e tooltip SHALL permanecer legíveis no mobile e em zoom. Cada ponto interativo SHALL ter nome e papel coerentes: quando acionável como botão, Enter e Espaço SHALL executar a mesma interação do clique/toque. Foco e hover SHALL permitir consultar o tooltip; Escape SHALL dispensá-lo sem registrar snapshot nem alterar seleção. Conteúdo adicional em hover/foco SHALL ser dispensável, alcançável por ponteiro quando pertinente e persistente enquanto consultado, conforme WCAG 1.4.13. Alvos SHALL atender WCAG 2.5.8, e a ordem de Tab SHALL acompanhar a cronologia sem armadilha de foco. O refinamento MUST preservar dataset completo, estilos contínuo/tracejado, gaps, snapshots vazios, valores, timestamps e regras atuais de projeção geométrica; ampliar a área interativa MUST NOT deslocar o ponto financeiro.

#### Scenario: Operação sem percepção de cor
- **WHEN** os gráficos de BRL e USD são apresentados
- **THEN** títulos, símbolos e estilos distinguem as moedas independentemente de cor

#### Scenario: Interação assistiva
- **WHEN** o usuário utiliza teclado ou tecnologia assistiva
- **THEN** ações, estados, pontos interativos e histórico textual possuem nomes, foco e anúncios compreensíveis

#### Scenario: Viewport compacto
- **WHEN** a evolução é apresentada em tablet ou mobile
- **THEN** gráfico, legenda, labels, tooltip e histórico se adaptam sem remover observações ou informação financeira essencial

#### Scenario: Ativação de ponto por teclado
- **WHEN** um ponto com papel de botão recebe foco
- **THEN** Enter e Espaço oferecem a mesma consulta que clique, com nome acessível e foco visível

#### Scenario: Dispensa do tooltip
- **WHEN** um tooltip está visível e Escape é pressionado
- **THEN** somente o tooltip fecha, sem ação financeira ou perda indevida de foco

#### Scenario: Tooltip na borda
- **WHEN** um ponto próximo da borda é consultado em viewport compacto
- **THEN** texto e valores completos permanecem legíveis e alcançáveis sem recorte ou overflow da página

#### Scenario: Alvos próximos e histórico
- **WHEN** observações próximas são percorridas
- **THEN** a interação atende tamanho ou espaçamento/exceções normativas e o histórico completo continua disponível sem eliminar pontos


### Requirement: Visualizar moedas em gráficos SVG independentes
A evolução SHALL usar SVG próprio, responsivo e sem biblioteca gráfica, Canvas ou nova dependência. BRL e USD SHALL possuir gráficos e escalas monetárias independentes, identificados textualmente e formatados respectivamente com `R$` e `US$`. O frontend MUST NOT somar, converter, consolidar ou assumir equivalência entre moedas.

#### Scenario: Somente uma moeda
- **WHEN** o histórico contém observações somente em BRL ou somente em USD
- **THEN** apenas o gráfico da moeda existente é apresentado

#### Scenario: BRL e USD simultâneos
- **WHEN** o histórico contém observações nas duas moedas
- **THEN** são apresentados gráficos independentes “Histórico do patrimônio — BRL” e “Histórico do patrimônio — USD”, sem eixo monetário compartilhado


### Requirement: Criar snapshot somente por ação manual explícita
A seção SHALL oferecer ação acessível “Registrar patrimônio atual” que execute `POST /carteiras/{carteiraId}/snapshots` sem body somente após acionamento do usuário. Durante a criação, submissões duplicadas SHALL ser impedidas e o progresso SHALL ser anunciado sem bloquear desnecessariamente o restante do Dashboard. Sucesso SHALL produzir confirmação e recarregar somente a evolução da Carteira de origem.

#### Scenario: Registro manual concluído
- **WHEN** o usuário aciona “Registrar patrimônio atual” e o backend responde `201 Created`
- **THEN** a interface confirma a criação, mantém o contexto e solicita novamente a evolução dessa Carteira

#### Scenario: Double-submit
- **WHEN** uma criação de snapshot está pendente
- **THEN** novos acionamentos da mesma ação não disparam outro POST

#### Scenario: Ausência de automação
- **WHEN** o Dashboard abre, carrega, troca de Carteira, atualiza dados, registra Operação ou permanece aberto
- **THEN** nenhum snapshot é criado sem acionamento explícito de “Registrar patrimônio atual”


## ADDED Requirements

### Requirement: Apresentar snapshots como Histórico do patrimônio
A seção SHALL usar o título visível “Histórico do patrimônio” e explicar que apresenta registros manuais do valor da Carteira ao longo do tempo, separados por moeda. A interface SHALL preferir “registro” a “snapshot” em textos próprios, incluindo identificação por ID, estados e confirmação de sucesso, mantendo a indicação de horário local. A apresentação SHALL ser complementar aos indicadores principais, sem mudar a ordem funcional aprovada do Dashboard. Nomes técnicos internos, DTOs, métodos, endpoints, payloads, persistência, cálculos, erros recebidos do backend e comportamento manual MUST permanecer inalterados. Esta mudança MUST NOT acrescentar gráficos por ativo, alocação, cálculos, consultas ou captura automática.

#### Scenario: Finalidade compreensível
- **WHEN** o usuário consulta a seção
- **THEN** encontra “Histórico do patrimônio”, explicação de registro manual por moeda, indicação de horário local e acesso ao histórico textual completo

#### Scenario: Linguagem sem alteração contratual
- **WHEN** o usuário aciona “Registrar patrimônio atual”
- **THEN** somente o fluxo manual existente de snapshot é executado, com o mesmo endpoint, payload, bloqueio concorrente e tratamento de sucesso/erro

#### Scenario: Registros preservados
- **WHEN** o histórico contém registros vazios, uma observação, lacunas ou instantes próximos
- **THEN** IDs, timestamps, valores, moedas, precisão e representação completa permanecem preservados, sem tendência artificial

#### Scenario: Labels temporais sem quebra ou colisão
- **WHEN** instantes próximos, extremidades ou texto ampliado reduzem o espaço disponível
- **THEN** labels permanecem em uma linha, ancorados para dentro e medidos no espaço renderizado; primeiro e último são priorizados quando cabem, com representação temporal curta e supressão de intermediários conflitantes, sem mover pontos ou remover registros

#### Scenario: Gráficos compactos por largura disponível
- **WHEN** o Histórico do patrimônio contém BRL e USD e dispõe de largura suficiente para duas colunas legíveis, inclusive no desktop
- **THEN** os gráficos independentes ficam lado a lado conforme o espaço disponível, mantendo mini gráficos com títulos curtos na região compacta; quando a largura mínima funcional não cabe, os gráficos empilham automaticamente, preservando tooltip completo e um único histórico textual abaixo

#### Scenario: Horário local apresentado até minutos
- **WHEN** um timestamp do histórico patrimonial é apresentado no tooltip, registro, descrição ou nome acessível
- **THEN** a apresentação usa DD/MM/YYYY, HH:mm (horário local), sem segundos ou milissegundos; os timestamps originais permanecem integrais nos dados e atributos datetime, e o eixo preserva seu formato compacto
