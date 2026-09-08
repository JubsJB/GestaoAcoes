## MODIFIED Requirements

### Requirement: Apresentação responsiva sem cálculo financeiro
A aplicação SHALL apresentar ticker, empresa, mercado, moeda, `cotacaoAtual` e referência temporal fornecidos pelo backend de modo responsivo, usando cabeçalho e superfícies coerentes. `dataHoraCotacao` SHALL ser formatada somente na apresentação como `dd/MM/yyyy às HH:mm`, em `pt-BR` e timezone local do navegador, sem alterar o DTO, converter moeda ou calcular resultado financeiro. A cotação SHALL ser identificada como a última cotação registrada e MUST NOT ser apresentada como garantia de valor em tempo real, ao vivo ou equivalente. A apresentação MUST NOT introduzir polling ou atualização automática e SHALL preservar o fluxo de atualização manual existente. A listagem SHALL usar tabela semântica no desktop e cards completos no mobile conforme frontend-visual-experience, com ticker em destaque, empresa legível, moeda explícita e cotação alinhada à direita. O detalhe SHALL hierarquizar identificação, cotação registrada e sua referência temporal sem promover metadados a indicadores novos.

#### Scenario: Mercado e moeda apresentados
- **WHEN** uma Ação é exibida
- **THEN** mercado aparece amigavelmente e cotação é formatada conforme BRL ou USD recebido

#### Scenario: Data e cotação
- **WHEN** cotação e `dataHoraCotacao` são exibidas
- **THEN** a interface identifica a última cotação registrada, mostra a data no padrão aprovado e não promete valor em tempo real

#### Scenario: Nome empresarial longo
- **WHEN** o nome da empresa é extenso
- **THEN** ele quebra de forma legível sem truncamento obrigatório ou overflow horizontal

#### Scenario: Viewport compacto
- **WHEN** listagem ou detalhe é exibido em tela compacta
- **THEN** cards, feedbacks e ações permanecem legíveis e operáveis em coluna

#### Scenario: Listagem comparável
- **WHEN** Ações são exibidas no desktop
- **THEN** ticker, empresa, mercado, moeda, cotação e referência temporal permanecem comparáveis com as ações existentes

#### Scenario: Uma representação ativa
- **WHEN** a listagem é exibida em largura compacta
- **THEN** somente os cards participam da árvore acessível e do foco, preservando todos os campos e ações


