## Purpose

Permitir consulta informativa e consistente das quantidades de posicoes abertas atribuidas a corretoras pelas operacoes registradas, com reconciliacao e diagnosticos explicitos sem afirmar custodia externa.

## ADDED Requirements

### Requirement: Consulta independente de distribuicao de posicoes abertas
O sistema SHALL disponibilizar GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras somente para posicoes consolidadas abertas daquela Carteira. SHALL retornar 200 com identificacao da Carteira e colecao de posicoes com identificacao do ativo,mercado,moeda,quantidade consolidada,total atribuido,resultado de reconciliacao,grupos e avisos. Carteira inexistente SHALL retornar 404 padronizado. MUST NOT alterar contratos existentes de posicoes/operacoes.

#### Scenario: Posicao consolidada aberta
- **WHEN** uma Acao possui quantidade consolidada positiva na Carteira
- **THEN** sua distribuicao e retornada,identificando exclusivamente aquela Acao e Carteira

#### Scenario: Posicao consolidada zerada
- **WHEN** uma Acao tem quantidade consolidada zero,inclusive com deficits por corretora
- **THEN** ela nao integra esta consulta de posicoes abertas,sem apagar operacoes ou promover auditoria de posicoes encerradas

#### Scenario: Carteira vazia ou inexistente
- **WHEN** uma Carteira existente nao possui posicoes abertas ou o identificador nao existe
- **THEN** retorna respectivamente 200 com colecao vazia ou 404 padronizado,sem dados de outra Carteira

### Requirement: Atribuicao exata e cronologica por corretora
Para cada posicao aberta,o sistema SHALL reproduzir as operacoes persistidas na ordem deterministica vigente dataOperacao/ordemNoDia/id,agrupando por Carteira,Acao e corretoraId. SHALL somar COMPRA e subtrair VENDA com decimal exato,sem float,double,truncamento ou arredondamento. Null SHALL formar grupo "Sem corretora informada". SHALL apresentar nome e quantidade liquida de cada grupo de saldo final diferente de zero. MUST NOT inferir corretora pelo ativo,mercado ou outras operacoes.

#### Scenario: Uma corretora
- **WHEN** existem compra100 e venda40 da mesma Acao na mesma corretora
- **THEN** apresenta quantidade60 nessa corretora e consolidado60

#### Scenario: Varias corretoras
- **WHEN** uma Acao possui compras100 em A e50 em B,venda20 em A
- **THEN** apresenta A80 eB50,com consolidado130,sem misturar outra Acao

#### Scenario: Corretora nao informada
- **WHEN** compras e vendas novas ou antigas possuem corretora nula
- **THEN** suas quantidades com sinal integram um unico grupo identificado "Sem corretora informada",sem atribuir custodia conhecida

#### Scenario: Precisao BRASIL e EUA
- **WHEN** existem quantidades inteiras brasileiras e compras EUA0.123456 mais0.000001
- **THEN** conserva inteiros BRASIL e quantidade EUA0.123457 exatamente,sem conversao cambial nem perda textual no cliente

#### Scenario: Operacoes retroativas
- **WHEN** uma operacao retroativa foi aceita pelo contrato vigente
- **THEN** consulta refaz todo replay na cronologia persistida,sem alterar ordemNoDia e sem tratar id como ordem financeira

### Requirement: Diagnosticos historicos persistem na leitura derivada
O sistema SHALL fornecer um diagnostico logico unico por agrupamento observado,inclusive saudavel ou zerado,conforme contrato-http.md. SHALL identificar corretora/categoria,quantidadeFinal e flags saldoNegativoAtual/possuiDeficitHistorico;primeiroDeficit SHALL ser nao nulo se e somente se historico:true. A evidencia SHALL identificar primeira operacao da sequencia oficial cujo saldo apos aplicacao fica negativo,dataOperacao,ordemNoDia,operacaoId e saldoAposOperacao. SHALL distinguir saldo final negativo de deficit historico,manter evidencia historica mesmo apos regularizacao ou zeramento,e considerar todo historico registrado,sem reiniciar flags em ciclos posteriores. MUST NOT substituir primeira ocorrencia pelo minimo,zerar negativos,omitir problemas ou alterar registros.

#### Scenario: Saldo negativo atual
- **WHEN** compra100 em A e venda40 em B deixam consolidado60 eB negativo40
- **THEN** apresenta A100 eB negativo40 com aviso de saldo negativo emB,sem redistribuir a venda

#### Scenario: Deficit historico regularizado
- **WHEN** com consolidado sempre valido,B vende10 antes de comprar20
- **THEN** apresenta B10 com aviso historico e evidencia do deficit anterior,sem afirmar ausencia de inconsistencias

#### Scenario: Corretora zerada com inconsistencia historica
- **WHEN** B vende10 e depois compra10,outra corretora mantem a posicao consolidada aberta
- **THEN** nao apresenta linha B0,mas a posicao aberta apresenta aviso identificando B e seu deficit historico

#### Scenario: Grupo zerado sem deficit
- **WHEN** um grupo encerra com zero e nunca esteve negativo
- **THEN** omite a linha quantitativa e preserva o historico de operacoes sem fabricar aviso

#### Scenario: Primeira ocorrencia e nulabilidade
- **WHEN** grupo apresenta deficit de1,posteriormente deficit maior e depois regulariza
- **THEN** conserva primeira evidencia de negativo1 sem troca pelo pior saldo;grupo saudavel tem flagsfalse e primeiroDeficit:null,e grupo historicamente negativo possui evidencia obrigatoria

### Requirement: Schema publico completo e separacao de diagnosticos
A resposta SHALL seguir integralmente contrato-http.md,com campos obrigatorios,tipo/nulabilidade e colecoes sempre presentes como arrays. Quantidades SHALL ser numeros JSON decimais exatos,preservados lossless no cliente. Posicao SHALL identificar acaoId,ticker,mercado,moeda,quantidadeConsolidada,quantidadeAtribuidaTotal,reconciliada,diagnosticoReconciliacao,corretoras e diagnosticosCorretoras. Corretoras SHALL conter somente grupos finais nao zero;diagnosticosCorretoras SHALL conter um objeto unico por grupo,inclusive null/zero,sem duplicacao entre flags. Null SHALL identificar apenas corretora ausente. Divergencia SHALL ocupar diagnosticoReconciliacao da posicao sem corretoraId. Esse campo SHALL ser null se e somente se reconciliada:true. Deficits/zeros SHALL seguir exemplos do contrato,sem transformar reconciliacao em ausencia de deficits.

#### Scenario: Vazio e colecoes obrigatorias
- **WHEN** Carteira existente nao tem posicoes abertas ou nao existe diagnostico de divergencia
- **THEN** retorna posicoes:[] ou diagnosticoReconciliacao:null respectivamente,sem omitir campos nem retornar arraysnull

#### Scenario: Divergencia separada e HTTP
- **WHEN** total atribuido difere do consolidado ou ha apenas deficits de agrupamento
- **THEN** retorna200 com diagnosticos explicitos;divergencia tem codigoDIVERGENCIA_QUANTIDADES e nenhum corretoraId;ambas quantidades permanecem no pai,e negativos nao viram erro global

#### Scenario: Erros globais seguros
- **WHEN** Carteira nao existe,parametro nao converte paraLong,replay consolidado e inconsistente ou ha falha tecnica inesperada
- **THEN** responde404,400REQUEST_INVALIDO,409HISTORICO_OPERACOES_INCONSISTENTE ou500FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS respectivamente,conforme StandardError e nulabilidade do contrato,sem dados tecnicos sensiveis nem distribuicao fabricada

### Requirement: Ordenacao deterministica independente de collation
Posicoes SHALL ordenar por mercadoBRASIL antes deEUA,ticker ordinalUTF-16 case-sensitive eacaoId numericoASC. Grupos e diagnosticos SHALL ordenar por corretoraId nao nulo primeiro,nome ordinalUTF-16 ecorretoraId numericoASC,categoria null por ultimo. MUST NOT depender de locale,collationSQL,normalizacao de acentos ou prioridade de severidade. Diagnosticos SHALL ser unicos por grupo;diagnosticoReconciliacao e um objeto unico da posicao. Nomes SHALL seguir fallback e rotulo definidos em contrato-http.md.

#### Scenario: Empates nomes e categoria ausente
- **WHEN** corretoras compartilham nome,ha variacao de maiusculas/acentos e categoria nula
- **THEN** usa comparacao ordinal eID como desempate completo,null sempre ultimo,e diagnosticos seguem a mesma chave sem reordenar replay

### Requirement: Reconciliacao consistente sem efeitos colaterais
O sistema SHALL comparar por valor exato a soma de todos os saldos finais com sinal,incluindo null e zeros,com a quantidade consolidada do mesmo snapshot. SHALL fornecer ambas quantidades e resultado explicito;divergencia SHALL ser diagnosticada,sem correcao silenciosa ou exibicao de distribuicao falsamente reconciliada. SHALL respeitar isolamento por Carteira e leitura consistente,evitar N+1 de corretoras e MUST NOT escrever registros,consultar providers ou produzir snapshots no GET.

#### Scenario: Soma reconciliada inclusive negativos
- **WHEN** os grupos sao A100,B negativo40 eSem corretora5,e consolidado65
- **THEN** retorna total65 reconciliado,e os avisos negativos continuam presentes

#### Scenario: Divergencia identificada
- **WHEN** a verificacao encontra quantidade atribuida diferente do consolidado
- **THEN** retorna diagnostico explicito e ambas quantidades,sem ajustar grupos ou alegar reconciliacao

#### Scenario: Isolamento e leitura concorrente
- **WHEN** outra Carteira opera o mesmo ativo ou uma operacao concorre com a consulta
- **THEN** utiliza apenas o historico da Carteira solicitada em um snapshot consistente para total e distribuicao

#### Scenario: Leitura sem efeitos e sem N+1
- **WHEN** consulta retorna muitas corretoras ou e repetida
- **THEN** nao faz uma consulta por agrupamento,nem escrita,provider,snapshot ou alteracao dos dados existentes

### Requirement: Preservacao das regras consolidadas
A distribuicao SHALL ser exclusivamente informativa,derivada das operacoes,e MUST NOT representar custodia comprovada. MUST NOT introduzir bloqueio por corretora,obrigatoriedade de corretora,transferencia,saneamento,preco medio ou resultado por corretora. SHALL preservar compras/vendas retroativas,validacao de saldo consolidado,preco medio,custo,realizado,nao realizado,rentabilidade,patrimonio,snapshots e cotacoes.

#### Scenario: Venda em corretora diferente ou sem corretora
- **WHEN** uma venda e valida pelo saldo consolidado mas gera deficit num agrupamento
- **THEN** comportamento do POST permanece o vigente,e somente a leitura informativa sinaliza o deficit

#### Scenario: Regressao financeira
- **WHEN** distribuicao e consultada antes ou depois de atualizar cotacao ou registrar operacao valida
- **THEN** formulas/resultados oficiais permanecem determinados pelos contratos consolidados,e nenhum calculo financeiro por corretora e introduzido
