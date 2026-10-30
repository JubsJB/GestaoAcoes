# portfolio-broker-distribution Specification

## Purpose
Permitir consulta informativa e consistente das quantidades de posicoes abertas atribuidas a corretoras pelas operacoes registradas, com reconciliacao e diagnosticos explicitos sem afirmar custodia externa.

## Requirements

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
Para cada posição aberta o sistema SHALL reproduzir operações financeiras e custódias ativas na cronologia estável aprovada, agrupando por carteira/ação/corretoraId. Operações SHALL manter dataOperacao/ordemNoDia/id; custódias SHALL usar dataEfetiva/posição persistida sem ordenação por IDs de domínios diferentes. COMPRA soma e VENDA subtrai; TRANSFERENCIA/ATRIBUICAO debitam origem e creditam destino atomicamente. Original estornada SHALL ter efeito zero desde posição original, sem inversão na data de registro. Null SHALL formar grupo Sem corretora informada. Cálculo SHALL ser decimal exato, sem arredondamento e sem inferir corretora; grupos finais não zero SHALL ser exibidos.

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

#### Scenario: Custódia e estorno refletidos
- **WHEN** compra A100 e transferência A→B40 existem e depois a transferência é estornada validamente
- **THEN** distribuição passa de A60/B40 para A100, sem mudar consolidado100

#### Scenario: Atribuição sem mudar operação
- **WHEN** compra nula100 precede atribuição para A40
- **THEN** retorna grupo nulo60/A40 e compra permanece nula

### Requirement: Diagnosticos historicos persistem na leitura derivada
O sistema SHALL fornecer diagnóstico único por grupo observado no replay efetivo, incluindo saudável/zerado, com quantidadeFinal, saldoNegativoAtual, possuiDeficitHistorico e primeiroDeficit. Evidência SHALL identificar o primeiro evento efetivo negativo por dominio FINANCEIRO|CUSTODIA, id, natureza, dataEfetiva, posicaoCronologica e saldoAposEvento. Primeiro déficit SHALL ser não nulo se e somente se possuiDeficitHistorico=true. Regularização posterior no mesmo replay SHALL manter evidência; alteração retroativa válida que elimina ocorrência SHALL permitir aviso desaparecer. Original e auditoria MUST NOT ser apagadas; não haverá tabela de diagnósticos permanentes. MUST NOT substituir primeira ocorrência por pior saldo, ocultar negativos ou reiniciar flags em ciclos posteriores.

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

#### Scenario: Déficit eliminado por retroatividade válida
- **WHEN** compra/entrada válida anterior elimina todas as ocorrências negativas do grupo
- **THEN** novo diagnóstico tem possuiDeficitHistorico=false e primeiroDeficit=null, sem editar legado

#### Scenario: Identidade discriminada
- **WHEN** evidência recebida pertence ao domínio financeiro ou custódia
- **THEN** identifica domínio/natureza/ID sem preencher ID de operação com ID de custódia

### Requirement: Schema publico completo e separacao de diagnosticos
A resposta SHALL manter campos obrigatórios de carteira/posição/grupos/reconciliação e arrays presentes. Quantidades SHALL ser números JSON decimais exatos preservados lossless. PrimeiroDeficit SHALL substituir campos exclusivos operacaoId/dataOperacao/ordemNoDia/saldoAposOperacao por {dominio,id,natureza,dataEfetiva,posicaoCronologica,saldoAposEvento}; posição financeira SHALL ser {ordemNoDia}, e posição de custódia {ordemFinanceiraAncora,numerador,denominador}, com inteiros de chave textuais exatos. Diagnóstico de reconciliação SHALL ser separado, null iff reconciliada=true; grupos finais zero não terão linha, mas diagnósticos incluirão grupos efetivos zero/null. Atualização SHALL ocorrer no endpoint atual com DTOs, OpenAPI, exemplos e parser coordenados, sem versão paralela. Demais erros e contratos financeiros SHALL permanecer conforme vigentes.

#### Scenario: Vazio e colecoes obrigatorias
- **WHEN** Carteira existente nao tem posicoes abertas ou nao existe diagnostico de divergencia
- **THEN** retorna posicoes:[] ou diagnosticoReconciliacao:null respectivamente,sem omitir campos nem retornar arraysnull

#### Scenario: Divergencia separada e HTTP
- **WHEN** total atribuido difere do consolidado ou ha apenas deficits de agrupamento
- **THEN** retorna200 com diagnosticos explicitos;divergencia tem codigoDIVERGENCIA_QUANTIDADES e nenhum corretoraId;ambas quantidades permanecem no pai,e negativos nao viram erro global

#### Scenario: Erros globais seguros
- **WHEN** Carteira nao existe,parametro nao converte paraLong,replay consolidado e inconsistente ou ha falha tecnica inesperada
- **THEN** responde404,400REQUEST_INVALIDO,409HISTORICO_OPERACOES_INCONSISTENTE ou500FALHA_CONSULTA_DISTRIBUICAO_CORRETORAS respectivamente,conforme StandardError e nulabilidade do contrato,sem dados tecnicos sensiveis nem distribuicao fabricada

#### Scenario: Parser coordenado
- **WHEN** resposta contém evidência financeira ou de custódia atualizada
- **THEN** cliente valida o schema discriminado e conserva saldo decimal e chave exata sem aceitar estrutura ambígua

### Requirement: Ordenacao deterministica independente de collation
Posicoes SHALL ordenar por mercadoBRASIL antes deEUA,ticker ordinalUTF-16 case-sensitive eacaoId numericoASC. Grupos e diagnosticos SHALL ordenar por corretoraId nao nulo primeiro,nome ordinalUTF-16 ecorretoraId numericoASC,categoria null por ultimo. MUST NOT depender de locale,collationSQL,normalizacao de acentos ou prioridade de severidade. Diagnosticos SHALL ser unicos por grupo;diagnosticoReconciliacao e um objeto unico da posicao. Nomes SHALL seguir fallback e rotulo definidos em contrato-http.md.

#### Scenario: Empates nomes e categoria ausente
- **WHEN** corretoras compartilham nome,ha variacao de maiusculas/acentos e categoria nula
- **THEN** usa comparacao ordinal eID como desempate completo,null sempre ultimo,e diagnosticos seguem a mesma chave sem reordenar replay

### Requirement: Reconciliacao consistente sem efeitos colaterais
O sistema SHALL comparar exatamente a soma de todos os saldos com sinal, incluindo null/zeros, com quantidade consolidada obtida exclusivamente das operações do mesmo snapshot. Operações, custódias, estornos e nomes SHALL pertencer à mesma leitura consistente, sem N+1. Divergência SHALL permanecer explícita sem correção silenciosa. GET MUST NOT escrever, chamar providers, gerar snapshots ou realizar estorno. Estornos neutralizam pares e custódias conservam a soma. O motor interno SHALL considerar históricos encerrados, embora o endpoint público continue mostrando somente posições abertas.

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

#### Scenario: Estorno durante consulta
- **WHEN** neutralização confirma enquanto distribuição é lida
- **THEN** resposta usa estado único anterior ou posterior coerente para quantidades, situação e evidências

### Requirement: Preservacao das regras consolidadas
A distribuição SHALL representar controle interno, sem comprovar custódia externa. SHALL derivar operações e custódias ativas sem preço médio, custo ou resultado por corretora. Consulta MUST NOT bloquear ou modificar operações. Novos POSTs financeiros SHALL seguir validação de grupo/diferencial de operation-registration; legado negativo permanece consultável. Custódia/estorno SHALL preservar preço médio, custo, realizado, não realizado, rentabilidade, patrimônio, snapshots e cotações.

#### Scenario: Venda em corretora diferente ou sem corretora
- **WHEN** venda antiga válida consolidada produziu déficit no grupo
- **THEN** consulta preserva quantidade e aviso; nova venda é avaliada pela regra atual de saldo local

#### Scenario: Regressao financeira
- **WHEN** distribuição é consultada ou custódia válida é registrada com operações/cotações iguais
- **THEN** fórmulas e resultados consolidados permanecem iguais

#### Scenario: Sem custódia externa
- **WHEN** transferência interna é exibida
- **THEN** não afirma execução junto à B3/corretora nem dispara integração
