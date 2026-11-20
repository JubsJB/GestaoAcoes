> **Decisão vigente — priorização da entrega de24/11/2026:** manter a implementação V2 do Dashboard, com evolução patrimonial em gráficos separados BRL/USD. V3 analisada e adiada para depois da entrega; nenhum elemento de comparação percentual, seletor de modos ou unificação está autorizado agora. A documentação técnica e as prévias abaixo permanecem para retomada futura mediante nova autorização. O usuário confirmou posteriormente o aceite visual da composição nas capturas desktop/360/320; critérios funcionais e transversais são avaliados separadamente em diagnostico-pendencias-entrega-20261115.md, sem conclusão automática de tarefas.

# Vamos prosseguir com a implementação da modernização visual do Dashboard, dentro da change `modernizar-identidade-visual-e-experiencia-frontend`.

**Aprovo a composição geral do Dashboard e a proposta V2 dos gráficos**, com os refinamentos discutidos e a nova paleta de cores para a distribuição da carteira.

### 1. Identidade visual

- Seguir o padrão já aprovado nos módulos Usuários, Carteiras, Ações, Corretoras e Operações.
- Remover o ícone decorativo do cabeçalho.
- Padronizar tipografia, espaçamentos, cartões, bordas e botões.
- Utilizar azul como identidade principal, preservando verde e vermelho para resultados financeiros positivos e negativos.
- Evitar cartões excessivamente altos ou largos, principalmente no mobile.

### 2. Resumo financeiro

- Preservar a separação entre BRL e USD, sem conversões ou totalizações entre moedas.
- Destacar patrimônio e resultado financeiro.
- Apresentar custo e rentabilidade como informações complementares.
- Manter uma composição compacta e legível.

### 3. Evolução patrimonial — V2

- Preservar os gráficos reais e suas funcionalidades existentes.
- Valorizar as linhas, preenchimentos suaves, pontos históricos e seleção de registros.
- Manter BRL com linha contínua e USD tracejada, incluindo identificação textual.
- Utilizar grades e referências financeiras discretas, sem inventar valores intermediários ou alterar as escalas financeiras.
- Preservar tooltip, consulta por teclado, foco, Escape, histórico textual e lacunas entre registros.
- Evitar repetição visual excessiva entre último patrimônio, registro selecionado e histórico.

### 4. Distribuição da carteira — nova paleta aprovada

Substituir a paleta predominantemente azul pela seguinte combinação:

- Azul profundo: `#315EAB`
- Terracota suave: `#D39A64`
- Verde petróleo: `#478F89`
- Lavanda acinzentada: `#8875AD`
- Rosa antigo: `#C87986`
- Cinza azulado: `#8A98A7`

Aplicar as cores às fatias da rosca e aos marcadores correspondentes da legenda.

Preservar a estrutura V2: gráfico de rosca, identificação dos ativos, ticker, empresa, valores e recursos de acessibilidade.

Para carteiras com mais de seis ativos, avaliar uma extensão coerente da paleta, mantendo distinção suficiente entre categorias. Preservar a correspondência de cores entre gráfico e legenda.

Não apresentar percentuais de participação que não sejam fornecidos de forma autoritativa pelo sistema. Não alterar cálculos nem utilizar pesos gráficos como percentuais financeiros.

### 5. Desempenho por ativo — V2

- Preservar as barras horizontais divergentes com referência central em zero.
- Manter verde para ganhos e vermelho para perdas.
- Preservar escalas independentes por moeda.
- Destacar resultado monetário e apresentar rentabilidade como informação complementar.
- Melhorar espaçamentos entre ticker, valores, barras e percentuais.
- Garantir leitura adequada de nomes e valores extensos.

### 6. Responsividade

Validar desktop, 360 px e 320 px.

- Evitar overflow horizontal.
- Manter cartões compactos.
- Garantir legibilidade dos valores financeiros.
- Preservar os controles de interação e navegação por teclado.
- Evitar sobreposição entre gráficos, legendas, textos e botões.
- Não utilizar alturas fixas que cortem informações.

### 7. Preservação funcional

Não modificar serviços, DTOs, APIs, cálculos, regras financeiras, seleção de carteira ou contratos existentes.

Preservar todos os estados de carregamento, vazio e erro, ações de retry e funcionalidades de atualização de dados, atualização de cotações e registro patrimonial.

Não modificar o diálogo de Operações já aprovado.

Manter a modernização de custódia separada, conforme as tarefas 7.3 e 7.4.

### 8. Implementação e validação

Implemente de forma incremental, concentrando as alterações nos componentes e estilos locais do Dashboard.

Reutilize os testes existentes e aplique TDD aos comportamentos efetivamente modificados.

Execute testes direcionados, build production e validação OpenSpec estrita. Preserve os limites de cobertura e budgets configurados.

Atualize a rastreabilidade das tarefas 7.1 e 7.2, registrando o que foi implementado e o que ainda depende de conferência visual.

Não conclua verificações transversais ou aceite visual sem minha aprovação.

**Não realize commit, push, merge ou arquivamento.**

Ao finalizar, apresente os arquivos alterados, os resultados das verificações, eventuais avisos e as capturas da implementação para conferência.

A implementação está autorizada, mas o aceite visual definitivo dependerá da minha avaliação na aplicação real.

## Registro da implementação — 2026-11-15, conferência real pendente

Preservada a autorização acima. Entregues composição geral e V2 somente nos componentes/estilos locais do Dashboard: título sem ícone, identidade azul, contexto/comandos agrupados, quatro indicadores separados por moeda, resumo compacto, evolução/distribuição/desempenho/resultados harmonizados. Títulos internos redundantes continuam disponíveis de forma não visual, com IDs e semântica preservados. Mobile mantém patrimônio/resultado em linhas completas e custo/rentabilidade lado a lado, com altura natural e quebra de valores extensos.

Evolução mantém SVGs/geometrias reais, gaps, BRL contínuo/USD tracejado, áreas por segmento, controles e histórico integral. Guia de inspeção usa somente o ponto ativo existente. Nova diretiva local EvolutionValueLabelsDirective alinha até três referências de registros às coordenadas de tela e oculta somente labels largos/sobrepostos, reavaliando em render/resize e desconectando na destruição. Não interpreta texto monetário, gera ticks, altera escalas ou remove valores das alternativas. Tooltip/foco/hover/Enter/Espaço/Escape e fluxos de registro/retry intactos.

Distribuição usa exatamente as seis cores aprovadas, mais extensões #21437C, #986139, #27615D, #5E4B82, #914855 e #536475 para carteiras maiores. Acima12, repetição com números/texturas, sem limitar, ordenar ou agrupar ativos. Cores coincidem em fatias/legendas; números escuros sobre cores claras e brancos sobre escuras. Contraste calculado dos números sobre cor-base mínimo4,63:1; textura do marcador fica na borda e halo de cor-base protege o número da fatia. Sem percentuais de participação derivados dos pesos de desenho; rentabilidade recebida preservada. Não equivale a certificar contraste transversal.

Hashes confirmam serviços/modelos/geometrias/apresentação financeira/AxisLabelsDirective e formulário de Operações intactos. Corpos das classes DashboardPageComponent, PortfolioEvolutionComponent e PositionPerformanceChartComponent idênticos; evolução muda apenas metadata para importar a diretiva. Sem alteração de compartilhados/backend/seleção/POST/GET/custódia.

TDD: RED/GREEN de paleta/correspondência em oito ativos, referências existentes e colisão/resize/destruição. Resultado final221/221 testes direcionados em15 arquivos. Build production exit0, initial503,01kB; warnings locais Dashboard5,50kB (novo) e evolução6,20kB (antes5,68kB), abaixo do erro8kB. Nenhum budget/threshold/exclusão alterado; cobertura global não repetida nesta rodada e sem nova certificação do portão global. Evidências/comandos/arquivos em target/dashboard-implementation-20261115/relatorio.md.

Chrome isolado com build real e fixtures:21 cenários em1440/360/320 (normal, uma moeda, oito ativos, extensos512 dígitos, vazio, erro financeiro e erro de histórico), sem overflow horizontal. Consulta por teclado/Enter/Escape e diálogo de Operações768/312/272px conferidos. Capturas em after/ são da implementação compilada com dados simulados, não mockups nem prova do ambiente real. Em capturas integrais o script aumenta temporariamente a altura, mantendo a largura; geometria/interações verificadas em altura1000px.

7.1/7.2 continuam abertas para conferência real; posições detalhadas da Carteira não modernizadas e7.3/7.4/7.5–7.7/transversais não concluídas. Matriz completa, zoom/leitor de tela e verificação integral7.6 continuam pendentes. Movimentação horizontal não reinvestigada; traços Material permanecem pendentes e aceites anteriores de Operações preservados. Sem commit, push, merge ou arquivamento.

## Proposta de evolução unificada BRL/USD — 2026-11-15, aguardando aprovação

Esta seção é somente análise e proposta visual posterior à implementação V2. Não autoriza unificação nem modifica os gráficos atuais. Atualizado apenas este planejamento; tasks.md/checklists e código/configuração da aplicação não alterados. Prévias independentes em target/planejamento-evolucao-unificada-20261115/.

### Diagnóstico e viabilidade

PortfolioEvolutionComponent recebe a evolução completa por carteira, mas chama buildEvolutionGeometry separadamente para BRL e USD. Cada retorno possui pontos, segmentos, domínio monetário e seleção de labels temporais próprios. Hoje timeMin/timeMax são calculados a partir dos pontos desenháveis daquela moeda, não de um domínio temporal comum. Assim, sobrepor os SVGs sem adaptação pode colocar no mesmo x duas datas diferentes: se o último BRL for15/11 e o último USD14/11, ambos podem estar na extremidade direita de seus gráficos atuais. A unificação não é exclusivamente HTML/SCSS.

É viável com o mesmo GET e os mesmos snapshots/valores, sem alterar backend, serviços, DTOs, APIs ou cálculos financeiros. Requer adaptar a geometria de apresentação temporal, a composição SVG e a seleção local. O domínio vertical continua independente para cada moeda; a projeção geométrica não passa a ser fonte de valores financeiros.

### Organização proposta

1. Manter a seção Histórico do patrimônio na mesma posição e a ação Registrar patrimônio atual com permissões/busy/feedback atuais.
2. Um painel, uma área de desenho e duas linhas: BRL azul #315EAB contínua, USD terracota #D39A64 tracejada. Legenda textual “BRL · eixo esquerdo” e “USD · eixo direito”; títulos laterais “BRL · R$” e “USD · US$”. O tom terracota tem baixo contraste isoladamente sobre branco; propor contorno fino #986139 acompanhando os mesmos segmentos/tracejado, preservando #D39A64 como cor central. Texto das referências mantém contraste próprio, sem depender da cor da linha.
3. Aviso persistente e objetivo: “Escalas independentes. A altura das linhas não indica valores equivalentes nem rentabilidade comparável.” Não apresentar cruzamento, inclinação ou distância entre linhas como ganho relativo, conversão ou comparação de rentabilidade.
4. Dois últimos registros compactos, com data/hora própria de cada moeda, pois podem ser de instantes diferentes. Não chamá-los de patrimônio de uma mesma data nem somá-los.
5. Eixo BRL à esquerda e USD à direita. Usar poucas referências monetárias provenientes de observações existentes da respectiva moeda, não valores interpolados/nice ticks. Manter domínio vertical próprio atual, sem normalização percentual/base100, sem forçar zero novo ou domínio compartilhado. A mesma altura pode corresponder a R$24.200 e US$8.900: isso é efeito dos eixos independentes e não equivalência econômica.
6. Um eixo temporal com datas/horários de registros existentes, baseado nos instantes originais, com seleção de labels por espaço. Grade vertical discreta ligada aos registros; evitar grade horizontal com pares de números que sugira conversão entre eixos.
7. Recomendar linhas sem preenchimento de área neste modo comparativo para evitar sobreposição visual/ilusão de empilhamento. Não é remoção de dado ou controle; a V2 atual permanece intacta até aprovação.
8. Consulta no fluxo abaixo do desenho, sem sobrepor curvas ou eixos: ID, data/hora local completa e BRL/USD do mesmo registro. Um ponto pode indicar a moeda ativa, mas a consulta pode apresentar os dois valores somente quando pertencem ao mesmo snapshot. Valor ausente recebe “Sem observação neste registro”; nunca zero, carry-forward ou busca do valor mais próximo. Valores são lidos dos patrimonios originais, com formatadores atuais, nunca inferidos de x/y ou convertidos para número para exibição.
9. Controles individuais de registros e histórico integral permanecem. Propor um botão por snapshot no seletor unificado, com nome acessível completo; ações do histórico também selecionam o registro/moeda. Nenhuma paginação, filtro, agrupamento por dia/minuto ou eliminação de registros. Um snapshot sem moedas continua consultável com estado vazio próprio.

### Datas diferentes, lacunas e sobreposição

- Domínio temporal compartilhado derivado dos instantes válidos de todos os snapshots da carteira, inclusive registros com uma moeda ausente ou sem patrimônio por moeda. Apenas o x é compartilhado. Não ordenar/remover dados para unir moedas; preservar identidade/ordem e segmentação dos registros recebidos.
- Ausência de BRL interrompe somente BRL; ausência de USD interrompe somente USD. Não ligar pontos através desse registro, preencher valores, prolongar linha até o último instante da outra moeda ou desenhar ponto na lacuna. Uma série com uma única observação permanece um marcador, sem tendência ou curva.
- Duas moedas observadas em datas diferentes não formam artificialmente um par. A guia pertence ao instante do registro selecionado; uma interseção visual da outra linha nesse x não constitui observação nem gera tooltip interpolado. Não alinhar registros por data formatada/dia, ignorando hora, fuso ou ID.
- Instantes coincidentes de IDs diferentes permanecem registros distintos. Não aplicar jitter temporal ou espalhamento artificial para caber; manter x coincidente e acesso individual via seletor/histórico. Se todo o período tiver um único instante, mostrar os pontos nesse instante, sem inventar duração; a apresentação dessa condição exige teste específico em eventual implementação.
- Se duas curvas se sobrepõem, estilos distintos e controles por registro/moeda permitem acesso sem deslocar os pontos. Preserve tooltip por foco/hover, Enter/Espaço/click, Escape, prioridade de foco e consulta touch pelos botões. A adaptação da seleção deve manter proteção contra respostas/contextos obsoletos já existente.
- Valores que não possam ser projetados com segurança continuam consultáveis no histórico e no texto original; não fabricar ponto/escala. Qualquer diferença exposta na política atual de projeção de extremos deve ser tratada explicitamente, não como correção financeira silenciosa.

### Desktop e mobile

Desktop1440: largura estrutural do Dashboard preservada; painel único com duas últimas observações em linha, eixos laterais de aproximadamente100px e área central fluida. Traçados/guia e legenda claros; consulta pode ter valores lado a lado. Histórico abaixo com ID/data, BRL e USD.

Mobile360/320: margens existentes16px, painel com12px internos, eixos de aproximadamente60px por lado e área central restante, sem rolagem horizontal. O desenho pode precisar de aproximadamente180px de altura para não ficar achatado; essa dimensão se limita à área gráfica, nunca fixa a altura do cartão, consulta ou histórico. Referências financeiras12px, em quantidade reduzida; valores que não caibam não são truncados, mantendo consulta integral nas alternativas. Mostrar poucas datas com hora em segunda linha no eixo; todos os horários permanecem na seleção/histórico.

As duas últimas observações mantêm moeda e data individual. Legenda pode quebrar em linhas. Consulta dos valores empilha BRL/USD; botões44px com wrap; histórico em linhas móveis completas. Pontos próximos podem sobrepor alvos no gráfico estreito: não deslocá-los para contornar isso, preservar controles individuais de44px. A comparação visual tende a ser menos precisa em320px; aprovação deve considerar esse limite, sem prometer a mesma densidade do desktop.

### Estados e acessibilidade preservados

Loading, erro de leitura/retry, erro/progresso/sucesso do registro de patrimônio e independência em relação ao carregamento financeiro do Dashboard ficam intactos. Nenhum POST automático, retry de escrita ou nova consulta de enriquecimento.

Sem snapshots: mesmo estado vazio. Com snapshots sem moedas: histórico integral, mensagem explícita de ausência de pontos, sem eixos financeiros fictícios. Uma moeda apenas: desenhar somente a série/eixo correspondente e indicar ausência de observações da outra; não desenhar linha zero. Séries constantes: domínio/representação próprios existentes sem inventar marcas financeiras intermediárias. Leitor de tela recebe nome que menciona escalas independentes, descrição de linhas/ausências e acesso às observações originais. Não duplicar anúncios da consulta nem remover os controles textuais.

### Prévia e limites desta análise

Arquivo: target/planejamento-evolucao-unificada-20261115/proposta-evolucao-unificada.html. Prévias completas em previews/unificada-1440-completa.png, unificada-360-completa.png e unificada-320-completa.png; primeira área visível em unificada-1440/360/320.png. Recorte somente da evolução, sem propor mudanças no shell, resumo, distribuição ou desempenho.

Amostra explicitamente ilustrativa: seis registros, cinco observações BRL e quatro USD. USD ausente em#2/#6, BRL ausente em#4; seleção#4 demonstra ausência sem valor zero. Último BRL15/11, último USD14/11. Referências dos eixos são valores reais dessa amostra, não valores criados para divisões regulares. O HTML é protótipo independente; sua projeção ilustrativa não substitui a geometria dinâmica real na futura aplicação.

Chrome headless isolado renderizou1440/360/320 sem overflow horizontal no protótipo; seis registros e nove pontos preservados em cada captura. Verificada seleção ilustrativa#2 com USD ausente. Sem backend, dados reais, credenciais, novas dependências, build ou suítes. Não comprova integração/acessibilidade final, todos os extremos ou leitor de tela real. Somente este planejamento e artefatos descartáveis de prévia foram alterados nesta rodada; tasks.md e checkboxes intactos.

### Arquivos previstos após eventual aprovação

Todos sob frontend/src/app/features/dashboard/evolution/:

| Arquivo | Necessidade |
|---|---|
| evolution-geometry.ts e .spec.ts | Domínio temporal comum explícito para as duas séries; testes de datas diferentes, gaps, domínio vertical independente, instante único e coincidência. Alteração de projeção temporal, não cálculo financeiro. Não simplesmente sobrepor os resultados atuais. |
| portfolio-evolution.component.ts e .spec.ts | Compor as séries no mesmo domínio; adaptar seleção de registro/moeda e consulta das ausências a partir do snapshot original. Preservar efeitos de carregamento, cancelamento, POST/GET/retry e contexto. |
| portfolio-evolution.component.html | Um SVG, duas linhas/eixos, legenda, guia, consulta e seletor unificados; histórico integral preservado. |
| portfolio-evolution.component.scss | Reflow dos dois eixos, estilos BRL/USD, contraste/contorno, consulta e registro compacto. Consolidar regras antigas sem elevar budgets. |
| evolution-value-labels.directive.ts e .spec.ts | Medir referências dos dois lados separadamente, sem misturar moedas, colisão ou novos valores monetários. |
| axis-labels.directive.ts e .spec.ts | Reutilizar no único eixo X; adaptação só se comprovadamente necessária para labels do domínio comum. É diretiva local de Dashboard, não compartilhada global. |

dashboard-page.component.ts, DashboardService, EvolutionService, modelos/DTOs, APIs, formatadores compartilhados e demais gráficos não precisam mudar. Não adicionar biblioteca de gráficos. Reaproveitar testes de cancelamento/contexto, foco/Escape, duplicação, gaps/registros completos e casos extremos; TDD obrigatório para os novos comportamentos de apresentação/domínio/seleção depois de aprovados. A aprovação não pode ser tratada como mudança meramente cosmética de CSS.

7.2 é a referência principal;7.1,7.3/7.4 e demais aceites permanecem no estado atual.7.5/7.6/7.7 e transversais continuam pendentes; nenhum checkbox concluído. Movimentação horizontal e traços Material não investigados. Aguardar aprovação explícita da proposta visual e deste escopo de apresentação antes de implementar. Sem commit, push, merge ou arquivamento.

## Proposta V3 — comparar variação patrimonial e consultar valores, 2026-11-15

**Status:** somente análise/planejamento, aguardando aprovação explícita. A proposta anterior de duas linhas monetárias sobrepostas com dois eixos laterais não foi aprovada visualmente e não será implementada como padrão. V3 mantém a modernização já entregue e propõe apenas substituir a organização da evolução depois de autorizada. Prazo informado pelo usuário:24/11/2026; preferir escopo local, sem novas dependências ou persistência. tasks.md e seus checkboxes não alterados nesta rodada;7.1/7.2 não concluídas.

### 1. Confirmação técnica dos dados disponíveis

- SnapshotCarteira armazena ID, carteira e OffsetDateTime normalizado para UTC; restrição única por carteira/instante. SnapshotCarteiraMoeda armazena uma linha por snapshot/moeda, BigDecimal NUMERIC(38,12), com patrimônio estritamente positivo. Construtor e CHECK do schema rejeitam zero/negativo. Não confundir uma moeda ausente com patrimônio zero.
- SnapshotCarteiraService cria observações somente por comando explícito, a partir das posições do momento. Não reconstrói passado. EvolucaoPatrimonialService consulta via LEFT JOIN, preserva snapshots sem componentes e agrupa por ID com moedas presentes. Repository ordena por instante ASC, ID ASC, moeda ASC. Esses arquivos foram apenas lidos, sem alteração.
- O frontend recebe snapshotId, dataHoraSnapshot e patrimonios[{moeda,patrimonioAtual}], com valores preservados como strings pelo parser lossless. A validação estrutural frontend é mais permissiva que a persistência normal (aceita decimal zero/negativo), portanto a nova apresentação precisa tratar defensivamente esses casos, sem mudar DTO/serviço/validador.
- PortfolioEvolutionComponent já preserva histórico integral, seleção/foco/hover/Escape, registro explícito e estados/recargas independentes. Geometria atual é calculada separadamente por moeda e interrompe a linha se um snapshot não tiver o componente. Aproximação Number existente é exclusivamente de desenho; não é adequada para calcular percentuais financeiros de números longos.
- O modelo fornece valor, moeda, identidade e instante suficientes para uma **variação percentual patrimonial derivada**, sem novos endpoints. Não fornece ajustes de fluxos para medir rentabilidade: compras, vendas e cotações podem alterar o patrimônio das posições; não inferir retorno sobre capital, TWR, MWR ou taxa cambial.

**Confirmação limitada e objetiva:** a estrutura de dados permite o cálculo com segurança quando satisfaz os critérios abaixo. Não foi acessado banco/sessão autenticada ou amostra real nesta análise; não está confirmado que toda carteira existente tenha dois registros conjuntos utilizáveis. Fixtures e testes atuais contêm histórias com apenas uma observação USD, que não são elegíveis à comparação. A UI deverá verificar elegibilidade nos dados já recebidos; não fabricar histórico, criar snapshots ou afrouxar a regra para habilitar o gráfico.

### 2. Regra recomendada de base e período comum

**Modo principal:** Comparar evolução (%) — título/descrição do gráfico identificam “Variação percentual do patrimônio”, nunca “Rentabilidade”. Uma base monetária válida para cada moeda, **no mesmo snapshot e instante real**, evita comparar crescimentos iniciados em períodos diferentes.

1. Base comum = primeiro snapshot cronológico com BRL e USD presentes e estritamente positivos, representáveis pelo cálculo decimal local. Cada moeda usa seu próprio patrimônio desse registro como denominador. Não usar a primeira observação individual de cada moeda se as datas diferirem, nem escolher registro mais próximo/interpolar por data.
2. Fim comum = último snapshot posterior com observações numéricas utilizáveis das duas moedas, em instante estritamente posterior à base. Observações posteriores explicitamente iguais a zero podem ser tratadas como zero registrado, nunca criadas a partir de ausência; negativos inesperados são incompatíveis com o contrato normal e não entram como ponto calculado. O período é divulgado na UI, junto ao ID/data/hora da base.
3. Exigir ao menos esses dois registros conjuntos com resolução temporal suficiente para distinguir os instantes. Datas iguais após conversão de fuso não constituem um período. Se dois instantes diferem apenas abaixo da resolução de milissegundos suportada pelo desenho atual, não espalhar por índice para simular tempo; comparação fica insuficiente até haver período distinguível, preservando os registros originais.
4. Dentro da janela, manter todos os snapshots originais, inclusive os com apenas uma moeda. Uma ausência interrompe somente aquela série; não preencher com zero, último valor ou observação mais próxima, nem ligar através da lacuna. Snapshot vazio interrompe ambas. Linha reta entre observações consecutivas existentes é apenas conexão visual; não há pontos/tooltip calculados entre registros.
5. Registros antes da base/depois do fim comum permanecem no histórico e no modo Valores por moeda; não são descartados. No modo percentual, selecionar um deles mostra o valor original e “Fora da janela comparável”, sem percentual ou guia fictício fora do gráfico. Nenhuma alteração automática de modo/foco para esconder essa condição.
6. Sem base comum positiva, sem segundo registro conjunto posterior, apenas uma moeda, intervalos sem janela comum ou precisão não suportada: mensagem “Histórico insuficiente para comparar as duas moedas no mesmo período” com motivo específico e ação “Ver valores por moeda”. Ambos os modos permanecem acessíveis; não mostrar linha0% ou retornos de períodos diferentes. O modo principal abre como solicitado, mas não finge elegibilidade. Não gerar snapshot automaticamente.

Exemplo da prévia: BRL existe em01/11; USD começa em04/11. Base conjunta#2 em04/11, BRL10.000 e USD2.000. Último conjunto#6 em14/11. BRL posterior#7 em15/11 continua no histórico/monetário, sem extensão artificial de USD. Não é um filtro novo escolhido pelo usuário; é uma janela de comparabilidade explicitada, sem adicionar campos ou controles de período.

### 3. Fórmula e precisão propostas

Para moeda c e observação real t elegível:

    variação_c(t) = ((patrimônio_c(t) − base_c) / base_c) × 100

Exemplos independentes: BRL10.800/base10.000 → +8%; USD1.900/base2.000 → −5%. No instante-base,0% é resultado exato da fórmula sobre o registro existente, não ponto inventado. Nunca somar moedas nem formar um percentual combinado.

É um **novo cálculo derivado de apresentação**, sujeito à aprovação desta regra, não apenas uma mudança de cor e não um percentual autoritativo recebido da API. Não altera cálculos oficiais do backend ou grava percentuais nos snapshots. Usar strings decimais e aritmética racional/BigInt (coeficiente/escala, inclusive notação científica) em helper local, sem Number para a diferença/divisão. Number somente pode receber projeções limitadas para coordenadas SVG depois do cálculo; fonte e sinal continuam exatos. O projeto já utiliza BigInt, mas os formatadores existentes não calculam essa variação; não modificar helpers de Operações ou compartilhados para reaproveitar uma divisão inexistente.

Apresentação proposta: duas casas, sinal explícito e HALF_UP somente na exibição, sem arredondar patrimônios antes de calcular. Guardar numerador/denominador exatos na apresentação; quocientes periódicos não serão declarados “percentual completo exato”. Tooltip permite consultar valores-base e valores originais que explicam o cálculo. Variação não nula abaixo da resolução exibida recebe indicação “positiva/negativa inferior a0,01%”, não zero silencioso. Evitar sinal negativo em zero matemático.

Casos defensivos:

| Caso | Tratamento proposto |
|---|---|
| Base zero, negativa ou ausente | Não dividir; não usar valor absoluto ou base alternativa por proximidade. Procurar o primeiro registro conjunto positivo real, preservando os anteriores. Se não existir, comparação indisponível. |
| Valor atual positivo abaixo da base | Percentual negativo correto, com posição abaixo de0%. Não chamar de prejuízo/rentabilidade negativa. |
| Zero atual explicitamente recebido após base positiva | −100% matemático, identificado como zero observado; nunca substituir ausência por esse valor. Este caso não é produzido pela persistência normal atual. |
| Valor atual negativo inesperado | Manter valor original consultável; percentual indisponível por incompatibilidade com o contrato de snapshot positivo. Não normalizar/corrigir o dado silenciosamente. |
| Moeda ausente/snapshot vazio | Percentual ausente e lacuna; histórico mantém identidade/instante e ausência explícita. |
| Percentuais constantes em0% | Linha plana/referência0%, sem inventar ±1% ou crescimento; registros reais continuam distintos. |
| Precisão/exponente extremo não processável com segurança | Percentual indisponível com motivo, valor monetário intacto no modo alternativo/histórico; sem Infinity, NaN, clamps financeiros ou expansão ilimitada de strings. |

O frontend nunca fará ajuste por aportes, retiradas, compras/vendas ou cotações para chamar o resultado de retorno. A indicação “Não representa rentabilidade” permanece visível, e o aumento/redução recebe somente sinal/variação, sem rótulo de lucro/prejuízo.

### 4. Interface V3 recomendada

Um cartão de evolução com seletor nativo de dois botões: Comparar evolução (%) e Valores por moeda. Botões com aria-pressed, nome de grupo e Tab/Enter/Espaço; sem biblioteca nova ou foco automático ao alternar. Trocar modo é ação local, sem GET/POST, sem apagar seleção/histórico nem remontar carregamentos. Na troca de carteira, preservar preferência do modo em memória do componente, mas limpar seleção/base de origem e reavaliar dados; sem persistência/localStorage nova.

**Comparar evolução (%):** um gráfico com eixo percentual único e temporal compartilhado; azul#315EAB contínuo/círculo para BRL e terracota#D39A64 tracejado/losango para USD. Referência0% forte e grade discreta. Referências adicionais somente em percentuais derivados de pontos existentes (exemplo+12%/−5%), sem inventar divisões numéricas intermediárias. Mesmo domínio percentual para as duas moedas, incluindo0; séries não são normalizadas separadamente na altura. Sem preenchimento que sugira soma. Contorno fino escuro na terracota para contraste, mantendo a cor solicitada.

Faixa compacta identifica base e período. Consulta abaixo mostra ID, data/hora, valor monetário original e percentual de cada moeda. Não repetir dois cartões de “último patrimônio” acima: essa informação permanece consultável por registro e no histórico. Um registro ilustrativo selecionado nas capturas demonstra positivo e negativo; não implica seleção automática na aplicação.

**Valores por moeda:** duas faixas monetárias alinhadas verticalmente **dentro do mesmo cartão**, com um domínio temporal e eixo X comum, escalas Y próprias e títulos BRL/USD. Reutiliza segmentação/valores/seleção da análise anterior, mas não restaura o desenho rejeitado de dois eixos laterais sobrepostos. As faixas usam mais largura no mobile e evitam o cruzamento ambíguo das linhas monetárias. Aviso discreto: “Escalas monetárias independentes. As alturas das faixas não são diretamente comparáveis.” Histórico completo, inclusive fora da janela percentual; todos os valores em moedas originais. Repetições visuais de datas nos desenhos das faixas do mockup são apenas contexto; na implementação, preferir um único eixo X abaixo da dupla, alinhado ao mesmo domínio.

Histórico mantido em coleção compacta com ID/data/hora e valores originais por moeda. No mobile, moedas em duas colunas quando couberem e quebra natural para extensos; no320, cabeçalho do registro pode ocupar duas linhas. Botões de consulta44px, sem corte/ellipsis, sem paginação/recolhimento novo ou esconder registros antigos para reduzir altura. O seletor individual de registros continua atendendo pontos próximos/coincidentes e registros fora da janela.

### 5. Responsividade, estados e acessibilidade

- Desktop1440: cartão na largura estrutural atual, seletor compacto, dados-base/período discretos e área gráfica principal. Consulta em duas colunas; histórico íntegro abaixo.
- Mobile360/320: insets16px, padding12px, seletor em duas colunas com quebra de label; eixo percentual único libera largura central. Área gráfica ilustrativa180px, sem limitar altura de texto/consulta. No monetário, cada faixa tem altura gráfica aproximada130px e usa a mesma largura, sem dois eixos laterais espremendo o desenho. Poucas labels de data/hora por espaço, todas as datas completas na consulta/histórico.
- Não deslocar pontos para evitar coincidência, nem aumentar artificialmente diferenças. Círculo/losango, tracejado, texto e seletor de registros permitem distinguir observações. Alvos próximos no gráfico estreito podem coincidir; a alternativa44px é preservada. Referência0% e sinais são independentes de cor.
- Loading, vazio, erro de leitura/retry, criação pendente/erro/sucesso e independência da recarga financeira mantidos. Elegibilidade insuficiente é estado informativo, não erro HTTP e não retry automático. Snapshot não será criado para habilitar o gráfico.
- Foco/hover, consulta por teclado, Enter/Espaço/Escape, prioridade de foco e histórico permanecem. Mudança de modo mantém foco no botão acionado e seleção por ID; não reanuncia todo o histórico. Consulta de observação ausente não mostra percentuais do vizinho.

### 6. Prévias e limites da confirmação

Protótipo independente: target/planejamento-evolucao-v3-20261115/proposta-evolucao-v3.html. Dois modos interativos apenas no artefato, com sete registros ilustrativos, datas iniciais diferentes e lacunas em BRL/USD. Dados numéricos/percentuais da amostra são exemplos trabalhados, não implementação de aritmética financeira da aplicação. Seis capturas completas em previews/percent-1440/360/320-completa.png e money-1440/360/320-completa.png; primeira área visível sem sufixo-completa.

Chrome isolado renderizou os seis cenários sem overflow horizontal do protótipo e com os sete registros mantidos. Capturas inspecionadas em desktop/mobile. Sem backend, banco, dados autenticados, dependências novas, testes de aplicação ou builds nesta etapa. Isso não certifica elegibilidade de carteiras reais, contraste final, leitor de tela, zoom ou extremos do cálculo futuro. Somente este planejamento e artefatos descartáveis alterados; aplicação, tasks.md e verificações transversais preservadas.

### 7. Lista exata de arquivos previstos

Sob frontend/src/app/features/dashboard/evolution/:

| Arquivo | Alteração prevista |
|---|---|
| evolution-comparison.ts (novo) | Helper puro: elegibilidade, base/janela comum, estados por registro/moeda e cálculo racional decimal; sem HTTP ou persistência. |
| evolution-comparison.spec.ts (novo) | Casos independentes e valores esperados para regra temporal, sinais, divisão/precisão e ausência. |
| evolution-comparison-geometry.ts (novo) | Projeção percentual com domínio Y comum e eixo0, preservando fontes, ordem e gaps; somente coordenadas aproximadas. |
| evolution-comparison-geometry.spec.ts (novo) | Mesma escala para ambas, posição de0, positivos/negativos, constância e ausência de pontos inventados. |
| evolution-geometry.ts | Permitir domínio temporal explícito comum no modo monetário, mantendo domínio Y e segmentação próprios. Não usar cálculo em Number desse arquivo como percentual exibido. |
| evolution-geometry.spec.ts | Reaproveitar invariantes monetários/gaps e acrescentar alinhamento de instantes em domínio comum. |
| portfolio-evolution.component.ts | Modo local, derivação da comparação/seleção e composição dos gráficos; preservar efeitos de leitura, escrita, contexto e proteção de respostas tardias. |
| portfolio-evolution.component.spec.ts | Manter regressões existentes exercitando o modo monetário quando necessário; acrescentar modo/inelegibilidade/seleção/foco e ausência de novas requisições. |
| portfolio-evolution.component.html | Um cartão, seletor, gráfico percentual/duas faixas monetárias, referências da base/janela e consulta sem duplicação; histórico íntegro. |
| portfolio-evolution.component.scss | Reflow,0%, duas faixas, seletor/consulta/histórico, reaproveitando/consolidando regras. Nenhum aumento de budgets. |

Reutilizar sem alteração prevista evolution-value-labels.directive.ts/.spec.ts, axis-labels.directive.ts/.spec.ts e formatação existente; manter viewBox/contratos de coordenadas compatíveis com esses adaptadores. Não modificar serviços, modelos/DTOs, APIs, componentes compartilhados, outras features ou configurações. Se a implementação provar necessidade além desses dez arquivos, justificar antes de ampliar escopo. Não adicionar biblioteca decimal sem autorização; BigInt nativo já é suportado no projeto.

### 8. Testes realmente necessários após aprovação

Reaproveitar evolution.models.spec.ts/evolution.service.spec.ts para lossless/contratos e leitura/escrita; evolution-geometry.spec.ts para strings autoritativas, gaps e preservação dos pontos; portfolio-evolution.component.spec.ts para registro explícito, bloqueio de duplicação, respostas de outra carteira, loading/vazio/erro/retry, foco/hover/Escape e histórico integral; diretivas de labels para colisões/resize/destruição. Não executar backend ou suíte global por consequência automática desta proposta.

Novas fronteiras com TDD:

1. Helper público de comparação: bases em datas diferentes, primeiro conjunto positivo, fim comum, sem conjunto posterior, uma moeda/snapshot vazio,0/negativo/ausência e registros fora da janela; nenhuma entrada mutada. Exemplos fixos+8%/−5%, decimal longo/científico, arredondamento de divisão periódica e sinal não nulo pequeno.
2. Geometria percentual: domínio único/0, mesma variação na mesma altura para moedas diferentes, negativo abaixo de0, constância, gaps e sem jitter/percentuais intermediários. Separar prova do cálculo exato da aproximação do desenho.
3. Componente: alternância local sem GET/POST, manutenção do ID/foco, detalhe original e motivo da variação ausente, inelegibilidade com acesso monetário e histórico completo. Preservar regressões atuais; não apagar testes porque o modo padrão mudou.

Depois, apenas testes direcionados/build production/OpenSpec estrito e inspeção desktop/360/320 dos dois modos, conforme implementação autorizada; cobertura/budgets originais preservados, transversais e aceite real continuam abertos.

### 9. Riscos, decisões e esforço

**Esforço: médio.** UI e dados existentes são reaproveitáveis; o trabalho novo concentra-se na regra de comparação e precisão decimal, não em novos serviços. Não classificar como ajuste baixo de CSS nem expandir para biblioteca, fluxos de caixa, cálculo de rentabilidade, filtros de período ou backend antes da entrega.

Decisões recomendadas para aprovação: (a) base/fim em registros conjuntos e período divulgado; (b) fórmula de variação patrimonial, duas casas/HALF_UP e indicação de magnitude pequena; (c) faixas monetárias empilhadas no mesmo cartão, em vez de dois eixos laterais rejeitados; (d) comparação indisponível quando os dados não sustentarem a regra, sem fallback que compare períodos diferentes. A aprovação visual deve incluir essas regras, pois são novos comportamentos derivados de apresentação.

Riscos principais: interpretar compras/vendas como rendimento; comparar períodos diferentes; perder precisão com Number; ocultar a falta de uma moeda; custo/limites em expoentes extremos; colisão de pontos e labels móveis; mudança inadvertida do ciclo de recarga. As regras e fronteiras acima mitigam esses riscos sem alterar o domínio financeiro. Limitação real: sem dados conjuntos suficientes, não há comparação percentual segura a oferecer com os contratos atuais; alternativa mínima é Valores por moeda, sem novos registros ou contratos.

Nenhuma implementação autorizada nesta rodada. Proposta V3 e exemplos aguardam aprovação; proposta monetária sobreposta anterior continua não aprovada. Pendências conhecidas de movimentação horizontal/contornos Material, custódia e aceites transversais não reabertos nem concluídos. Sem commit, push, merge ou arquivamento.

## Adiamento aprovado da V3 e prioridades para24/11/2026

Decisão posterior explícita do usuário: interromper temporariamente a evolução do gráfico unificado. A V3 foi analisada, mas sua implementação fica **adiada para depois da entrega**, por prioridade de soluções operacionais, estabilidade e redução de retrabalho. As expressões anteriores “aguardando aprovação” são histórico da análise; o status atual é adiada, sem autorização de implementação. O gráfico monetário sobreposto anterior também não foi aprovado. Não implementar parte isolada da V3, inclusive cálculo percentual, base comum, domínio temporal unificado ou seletor de modos.

### Baseline a preservar

Manter integralmente a modernização V2 já implementada: dois gráficos patrimoniais separados por BRL/USD, consultas/controles/histórico, identidade/paleta e demais blocos do Dashboard. Nenhum rollback ou alteração de componente, cálculo, teste, estilo, serviço ou configuração nesta rodada. Não apagar as análises de elegibilidade/precisão da V3, a lista de arquivos proposta ou os artefatos visuais em target/planejamento-evolucao-v3-20261115/; estes são planejamento futuro, não código do produto. Retomada exige nova autorização e revisão das decisões à luz do estado do projeto depois da entrega.

### Estado de7.1/7.2 e critérios pendentes da implementação atual

Ambos os checkboxes continuam abertos em tasks.md. A implementação localizada e as evidências automatizadas existem:221 testes direcionados/15 arquivos, build production e OpenSpec estrito aprovados e21 cenários com fixtures em1440/360/320. Resultados históricos, sem repetição nesta rodada. A aprovação de manter a V2 não substitui o aceite visual real nem certifica leitor de tela, matriz completa ou o ambiente de entrega.

| Tarefa | Aceite ainda pendente, restrito à V2 atual |
|---|---|
|7.1|Conferência real e aprovação explícita do cabeçalho/contexto/comandos e resumo com uma/duas moedas, incluindo legibilidade emdesktop/360/320 e estados existentes. Confirmar apresentação e uso de Ver carteira, Registrar operação, Atualizar dados e Atualizar cotações conforme perfil, sem nova função ou composição.|
|7.2|Conferência real e aprovação explícita dos gráficos separados, referências/pontos/seleção/tooltip/teclado/Escape, gaps e registros completos; distribuição com paleta/legenda e desempenho/resultados com nomes/valores extensos. Confirmar estados/retry e uso atual de registrar patrimônio, sem POST duplicado ou confusão entre falha de escrita e recarga. Não exige V3 ou novos cálculos.|
|Limite de escopo|O Dashboard continua com análise resumida e acesso às posições detalhadas pela Carteira; seu interior financeiro não foi modernizado nesta entrega. Registrar esse limite ao avaliar o item amplo7.2, sem abrir implementação adicional apenas para satisfazer uma leitura estética da palavra “posições”. Custódia permanece em7.3/7.4.|

Verificações transversais relacionadas permanecem nas tarefas próprias:7.5/7.6,8.1/8.2 e8.4–8.7. Não duplicá-las como novas funcionalidades de7.1/7.2, nem encerrar esses itens por evidências parciais. Nenhuma comparação percentual, regra de base/janela, gráfico unificado ou teste da V3 é critério da entrega atual.

### Prioridades recomendadas das tarefas abertas

Classificação para planejamento, sem executar nem autorizar implementações adicionais. “Alta” significa priorizar confirmação de funcionamento e correção de falhas comprovadas; não afirma que já exista um bloqueador reproduzido nesses fluxos.

| Prioridade | Tarefas abertas relacionadas | Próximo resultado necessário |
|---|---|---|
|Alta — funcionamento e integridade|6.6,7.5,8.5; parte operacional de3.4/5.5|Consolidar/reutilizar regressões e conferir os fluxos existentes de sessão/perfis, seleção de carteira, operações, cotações, snapshots e custódia. Foco em gravação confirmada, ausência de POST duplicado, contexto correto e recuperação de falha de leitura. Investigar/corrigir somente defeitos concretos com autorização própria, sem redirecionar a rodada para modernização financeira.|
|Alta — acesso aos dados e controles|7.6,8.1,8.2 e verificações operacionais de3.3/5.5|Priorizar desvios que cortem valores, impeçam seleção ou escondam ações/foco no mobile/teclado. Preservar consulta integral de nomes/quantidades extensas e avançar nas pendências de acessibilidade real, inclusive leitor de tela. Seletor global com nomes longos é desvio já registrado; avaliar seu impacto antes de propor substituição. Não encerrar a matriz inteira com uma amostra.|
|Alta — critérios finais e evidências|1.6,8.4,8.5,8.6,8.7|Consolidar rastreabilidade, comparar o build final com a baseline, documentar warnings sem elevar budgets, revisar preservação de arquivos/escopo e obter o aceite final quando cabível. Build atual passou, mas CSS Dashboard5,50kB/evolução6,20kB têm warnings e a comparação final8.4 continua aberta. Não tratar warnings como falha operacional comprovada nem aumentá-los silenciosamente.|
|Média — conferir/formalizar o que já foi entregue|5.6,6.4,6.5,7.1,7.2,7.7|Resolver lacunas de conferência/registro de aceite sem reabrir composições aprovadas. Operações já tem aceite das superfícies explicitadas e pendência específica de contornos; não repetir esse aceite geral.7.7 abrange também custódia, portanto aceite do Dashboard sozinho não a conclui.|
|Menor — novas mudanças puramente estéticas|Acabamento/consolidação remanescentes de3.2/3.3; padronização visual de7.3/7.4|Manter tarefas abertas e reavaliar o escopo de implementação antes da entrega. A custódia já existe funcionalmente; estar pendente de modernização não significa que transferência/atribuição/estorno estejam ausentes. Verificar sua operação na prioridade alta; propor apenas acabamento localizado se autorizado, sem refatoração ampla ou novos recursos.|
|Depois da entrega|V3;8.8 apenas quando sincronização/arquivamento forem autorizados|Preservar planejamento futuro sem iniciar código ou tratar arquivamento como consequência automática da entrega. V3 está efetivamente adiada;8.8 continua condicionada à autorização própria.|

### Pendências conhecidas e cautelas

- Pequenos traços horizontais nos contornos Material em página/diálogo de Operações: pendência visual específica, causa não comprovada; sem CSS especulativo e sem novo bloqueio funcional presumido.
- Sacudida horizontal: continua registrada, investigação anterior encerrada; não reabrir por esta decisão. Só reavaliar prioridade se surgir evidência concreta de impedimento de uso, com novo direcionamento do usuário.
- Questões de Operações em planejamento-operacoes.md (falha de referências versus vazio, convite textual de cadastro a USER e feedback de navegação após POST) são constatações de código, não novos bugs reproduzidos nesta rodada. Se necessário, priorizar verificação pontual da recuperação após escrita por risco de repetição; não fazer correção financeira silenciosa nem iniciar bateria extensa.
- Não declarar novas verificações executadas. Nenhum teste, build, navegador ou mudança de código nesta atualização; somente o presente planejamento. tasks.md/checklists, propostas/arquivos visuais e implementação V2 preservados. Nenhuma tarefa concluída, commit, push, merge ou arquivamento.

**Próxima etapa recomendada:** organizar uma conferência operacional curta da versão atual e o aceite visual V2 com as evidências já existentes; registrar falhas reais e obter autorização para correções localizadas. Não iniciar novo refinamento de gráficos. Essa sequência não autoriza automaticamente escrita em dados reais ou alterações adicionais.
