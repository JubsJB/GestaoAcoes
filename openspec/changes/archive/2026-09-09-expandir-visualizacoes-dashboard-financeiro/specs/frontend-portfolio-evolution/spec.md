## ADDED Requirements

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
