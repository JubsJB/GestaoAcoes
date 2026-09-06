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


