## MODIFIED Requirements

### Requirement: Contrato frontend mínimo de Carteiras
A área de Carteiras SHALL consumir `POST /carteiras`, `GET /carteiras`, `GET /carteiras/{id}`, `PATCH /carteiras/{id}` e `DELETE /carteiras/{id}` pela configuração central da API. Para integrar o histórico contextual aprovado, o detalhe SHALL também consumir `GET /carteiras/{carteiraId}/operacoes` por meio da capability `frontend-operation-management` e `GET /carteiras/{id}/posicoes` pelo contrato existente de posições. O frontend SHALL representar `id`, `nome` e `dataCriacao` do `CarteiraResponse`, SHALL enviar somente `nome` nos requests de criação e edição e MUST NOT introduzir campos, endpoints ou dados derivados fora da consulta separada de distribuicao informativa aprovada nesta capability. O detalhe SHALL consumir GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras sem alterar os contratos anteriores nem calcular quantidades no navegador.

#### Scenario: Leitura do DTO básico
- **WHEN** o backend devolve uma Carteira
- **THEN** a aplicação preserva `id`, `nome` e `dataCriacao` sem acrescentar indicadores financeiros

#### Scenario: Requests mínimos
- **WHEN** o usuário cria ou edita uma Carteira
- **THEN** o frontend envia um corpo contendo exatamente `nome`

#### Scenario: Limite funcional
- **WHEN** a capability é apresentada
- **THEN** ela consulta e exibe os dados básicos, histórico de Operações e posições abertas aprovadas, com distribuicao informativa separada,sem acrescentar resumo, patrimônio agregado, evolução, snapshots, gráficos ou conversão cambial

## ADDED Requirements

### Requirement: Distribuicao informativa no detalhe da Carteira
Detalhe da Carteira -> Posicoes abertas SHALL apresentar distribuicao por corretora autoritativa do endpoint separado,identificando ativo,nome,quantidade liquida e categoria "Sem corretora informada". SHALL comunicar que representa operacoes registradas,nao custodia comprovada. MUST NOT substituir dados consolidados,calcular saldo,preco medio ou resultados no navegador. SHALL preservar decimal lossless,negativos,avisos historicos e divergencias. Grupos zero MUST NOT ter linha quantitativa,mas seus avisos historicos SHALL identificar corretora/categoria na posicao.

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
