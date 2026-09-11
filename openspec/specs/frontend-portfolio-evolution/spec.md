# frontend-portfolio-evolution Specification

## Purpose

Permitir acompanhar e registrar observações históricas do patrimônio de uma Carteira no Dashboard, com precisão lossless, moedas independentes, amostragem temporal fiel e visualização acessível.

## Requirements

### Requirement: Incorporar evolução ao contexto atual do Dashboard
O Dashboard SHALL apresentar uma seção modular de evolução patrimonial para a Carteira selecionada em `/dashboard?carteiraId={id}` e SHALL consumir exclusivamente `GET /carteiras/{carteiraId}/evolucao-patrimonial`, sem criar rota, paginação, filtros ou parâmetros adicionais. A seção SHALL reutilizar as regras vigentes de seleção, URL e contexto e MUST NOT consultar evolução sem Carteira válida.

#### Scenario: Carteira válida selecionada
- **WHEN** o Dashboard possui uma Carteira válida como contexto
- **THEN** a evolução dessa mesma Carteira é consultada e exibida na página atual

#### Scenario: Contexto indisponível
- **WHEN** não há Carteira, a seleção está pendente ou o parâmetro é inválido
- **THEN** nenhuma consulta ou criação de evolução é iniciada e prevalece o estado contextual do Dashboard

### Requirement: Preservar contrato histórico e precisão lossless
O frontend SHALL representar `carteiraId` e `snapshotId` como IDs numéricos seguros, `dataHoraSnapshot` como string ISO-8601, `moeda` como `BRL | USD` e `patrimonioAtual` como string lossless obtida do token numérico `BigDecimal`. A string autoritativa MUST ser usada para formatação, labels, tooltip e alternativa textual e MUST NOT passar por `Number`, `parseFloat` ou aritmética binária financeira.

#### Scenario: Decimal longo e escala preservada
- **WHEN** a resposta contém `patrimonioAtual` além da precisão segura de JavaScript, com escala 12 ou notação científica válida
- **THEN** todos os dígitos do token são preservados na string do model e formatados sem coerção binária

#### Scenario: Payload inválido
- **WHEN** a resposta não corresponde à estrutura aninhada ou contém JSON ou campo decimal inválido
- **THEN** a seção apresenta erro técnico tratável sem exibir dados parciais como válidos

### Requirement: Restringir aproximação numérica à geometria SVG
O frontend MAY converter uma cópia da string financeira para representação numérica aproximada exclusivamente para calcular coordenadas e dimensões do SVG. Essa aproximação MUST NOT substituir o valor autoritativo, alimentar labels, tooltip ou alternativa textual, produzir indicador financeiro, ser persistida ou ser enviada ao backend.

#### Scenario: Coordenada gráfica aproximada
- **WHEN** um ponto monetário é posicionado no gráfico SVG
- **THEN** somente sua geometria pode usar aproximação numérica e toda informação financeira apresentada deriva da string lossless original

#### Scenario: Precisão não representável visualmente
- **WHEN** diferenças decimais excedem a resolução numérica ou de pixels do gráfico
- **THEN** a visualização pode aproximar posições, mas os valores textuais permanecem exatos e não são substituídos pela geometria

### Requirement: Visualizar moedas em gráficos SVG independentes
A evolução SHALL usar SVG próprio, responsivo e sem biblioteca gráfica, Canvas ou nova dependência. BRL e USD SHALL possuir gráficos e escalas monetárias independentes, identificados textualmente e formatados respectivamente com `R$` e `US$`. O frontend MUST NOT somar, converter, consolidar ou assumir equivalência entre moedas.

#### Scenario: Somente uma moeda
- **WHEN** o histórico contém observações somente em BRL ou somente em USD
- **THEN** apenas o gráfico da moeda existente é apresentado

#### Scenario: BRL e USD simultâneos
- **WHEN** o histórico contém observações nas duas moedas
- **THEN** são apresentados gráficos independentes “Histórico do patrimônio — BRL” e “Histórico do patrimônio — USD”, sem eixo monetário compartilhado

### Requirement: Preservar lacunas sem fabricar continuidade
A série de cada moeda SHALL conter somente componentes efetivamente retornados para ela. Ausência de moeda em um snapshot SHALL significar ausência de observação e MUST NOT ser convertida em zero, último valor conhecido, interpolação ou ligação visual através da lacuna. Todos os snapshots SHALL permanecer disponíveis na representação histórica, ainda que não originem ponto para determinada moeda.

#### Scenario: Moeda ausente entre observações
- **WHEN** uma moeda aparece antes e depois de um snapshot no qual está ausente
- **THEN** a série apresenta segmentos interrompidos e não conecta os pontos através da lacuna

#### Scenario: Única observação de moeda
- **WHEN** uma moeda ocorre em somente um snapshot
- **THEN** o gráfico apresenta marcador isolado sem fabricar pontos, linha ou tendência

### Requirement: Representar fielmente zero, um ou vários snapshots
Uma resposta com `pontos=[]` SHALL produzir empty state próprio e não erro. Um único snapshot SHALL ser comunicado como uma observação sem tendência. Múltiplos snapshots SHALL permanecer em ordem `dataHoraSnapshot ASC`, estabilizada por `snapshotId ASC`, sem agregação, média, downsampling financeiro, interpolação ou cálculo derivado. O frontend MAY reduzir somente labels do eixo, sem remover dados.

#### Scenario: Evolução vazia
- **WHEN** uma Carteira existente não possui snapshots
- **THEN** a seção informa a ausência de histórico e oferece a ação explícita para registrar o primeiro snapshot

#### Scenario: Um snapshot
- **WHEN** a resposta contém exatamente um snapshot
- **THEN** a interface informa que existe somente uma observação e não sugere crescimento ou queda

#### Scenario: Histórico completo
- **WHEN** a resposta contém múltiplos snapshots, inclusive no mesmo dia ou minuto
- **THEN** todos permanecem representados na ordem recebida e distinguíveis por data e hora

### Requirement: Preservar snapshots vazios na alternativa histórica
Um snapshot com `patrimonios=[]` SHALL permanecer identificável por `snapshotId` e data/hora na representação textual. O frontend MUST NOT criar componente BRL ou USD artificial para esse instante.

#### Scenario: Snapshot sem componente monetário
- **WHEN** um ponto retornado possui `patrimonios=[]`
- **THEN** a representação histórica informa a observação temporal sem patrimônio e os gráficos não fabricam marcadores

### Requirement: Oferecer alternativa textual completa e datas locais
O gráfico MUST NOT ser a única fonte de informação. A seção SHALL manter disponível uma lista ou estrutura responsiva que apresente cada snapshot, sua data e hora local e cada moeda/valor autoritativo, incluindo snapshots vazios. A string ISO-8601 original SHALL permanecer no model; a apresentação SHALL indicar que usa horário local e possuir precisão temporal suficiente para distinguir observações próximas.

#### Scenario: Consulta textual do histórico
- **WHEN** o usuário acessa a alternativa textual
- **THEN** cada snapshot e componente monetário pode ser compreendido sem depender do SVG

#### Scenario: Instantes próximos
- **WHEN** existem snapshots no mesmo dia, minuto ou segundo
- **THEN** tooltip e representação textual exibem data e hora suficientes para diferenciá-los em horário local

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

### Requirement: Tratar erros e recuperação sem retry automático
GET e POST SHALL reutilizar a normalização HTTP existente. O GET SHALL tratar 404 e erro técnico; o POST SHALL tratar 404, `SNAPSHOT_CARTEIRA_DUPLICADO`, `INTEGRIDADE_DADOS_VIOLADA`, `CALCULO_POSICAO_FORA_DA_PRECISAO`, demais erros de domínio recebidos e erro técnico. Falha SHALL preservar recuperação explícita e MUST NOT iniciar retry HTTP automático nem apresentar criação como concluída.

#### Scenario: Falha ao consultar evolução
- **WHEN** o GET falha
- **THEN** a seção apresenta mensagem normalizada e ação explícita de tentar novamente para o contexto ainda válido

#### Scenario: Falha ao registrar snapshot
- **WHEN** o POST falha com erro de domínio ou técnico
- **THEN** a seção anuncia a falha, reabilita a ação quando apropriado e não recarrega a evolução como se houvesse sucesso

### Requirement: Proteger evolução contra mudanças concorrentes de contexto
Ao trocar a Carteira, a evolução anterior SHALL ser invalidada imediatamente e a consulta anterior SHALL ser cancelada logicamente. Respostas atrasadas de GET, POST ou reload MUST NOT substituir, contaminar ou produzir confirmação no novo contexto. O reload posterior ao POST SHALL ocorrer somente se a Carteira que originou a criação ainda for a seleção atual.

#### Scenario: Troca durante GET
- **WHEN** o usuário troca de Carteira enquanto a evolução anterior está pendente
- **THEN** dados ou erros da resposta anterior não aparecem no novo contexto

#### Scenario: Troca durante criação ou reload posterior
- **WHEN** a seleção muda enquanto o POST ou seu reload está pendente
- **THEN** o resultado permanece associado à Carteira de origem e não altera a evolução da seleção nova

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

### Requirement: Reconhecer limite de crescimento do histórico
O frontend SHALL consumir todo o histórico devolvido pelo backend e MUST NOT criar paginação, filtros, agregação ou amostragem financeira fictícios. Otimizações de DOM, labels e detalhes decorativos MAY ocorrer somente quando não removem snapshots ou alteram valores representados.

#### Scenario: Histórico extenso
- **WHEN** o backend retorna grande quantidade de snapshots
- **THEN** todos continuam no dataset e qualquer redução visual limita-se a labels ou detalhes não financeiros

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

### Requirement: Linha financeira de observações manuais sem série inventada
O Histórico do patrimônio SHALL comunicar evolução dos valores registrados manualmente, com linha reta entre pontos observados de cada segmento, grid discreto, eixo temporal legível e tooltip com moeda, valor autoritativo e data local em hierarquia clara. SHALL preservar coordenadas, paths, dataset integral, IDs, timestamps, vazios, gaps, uma observação, história textual única e botão manual. MUST NOT adicionar suavização, pontos, interpolação de dados, coleta automática, nova série, request ou rentabilidade derivada. BRL/USD SHALL continuar em gráficos monetários independentes, lado a lado quando legíveis.

#### Scenario: Gaps e observação única
- **WHEN** existe moeda ausente entre registros ou apenas uma observação
- **THEN** os gaps permanecem interrompidos e observação única permanece um ponto sem tendência, com o mesmo histórico textual e sem preenchimento decorativo sugerindo continuidade

#### Scenario: Datas e consulta acessível
- **WHEN** um registro é consultado por teclado, toque ou ponteiro
- **THEN** tooltip/histórico apresentam o valor autoritativo e DD/MM/YYYY, HH:mm local; Enter/Espaço/Escape, foco, seleção e consulta de instantes próximos preservam o comportamento vigente

#### Scenario: Alteração somente visual
- **WHEN** grid, títulos, legenda e tooltip recebem refinamento
- **THEN** pontos e paths financeiros permanecem iguais e nenhuma chamada ou snapshot é criado pela renderização

#### Scenario: Linha ampla e área opcional
- **WHEN** o histórico é refinado no Bloco 2
- **THEN** a linha P0 usa o painel amplo, pontos discretos e identificação de registros manuais; área decorativa P1 pode existir somente sob segmentos existentes até a base visual existente, sem alterar domínio/coordenadas, preencher gaps/ponto isolado ou sugerir observações intermediárias; sua omissão não bloqueia a linha
