## MODIFIED Requirements

### Requirement: Representação contábil e de mercado da posição
Cada posição SHALL conter `acaoId`, `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `quantidadeAtual`, `precoMedio`, `custoPosicao`, `cotacaoAtual`, `dataHoraCotacao`, `valorAtualPosicao`, `resultadoNaoRealizado` e `rentabilidadePercentual`. Os identificadores, dados descritivos, moeda, `cotacaoAtual` e `dataHoraCotacao` da Ação SHALL refletir exatamente o relacionamento e o estado persistidos. `valorAtualPosicao`, `resultadoNaoRealizado` e `rentabilidadePercentual` SHALL ser derivados conforme as regras desta capability. A resposta MUST NOT incluir resultado realizado, patrimônio ou snapshot.
 Cada posição SHALL incluir também referenciaMercadoCotacao {tipo, instante, data} e cotacaoObtidaEm conforme Ação persistida, admitindo DESCONHECIDA e obtenção nula no legado; dataHoraCotacao SHALL permanecer legado obrigatório.

#### Scenario: Posição brasileira com cotação
- **WHEN** uma posição aberta referencia Ação de `mercado=BRASIL`, `moeda=BRL` e cotação persistida válida
- **THEN** a resposta identifica a Ação, preserva os valores contábeis em BRL e inclui cotação, data/hora, valor atual, resultado não realizado e rentabilidade percentual

#### Scenario: Posição americana com quantidade fracionária
- **WHEN** uma posição aberta referencia Ação de `mercado=EUA`, `moeda=USD`, possui quantidade fracionária e cotação persistida válida
- **THEN** a resposta preserva mercado, moeda, quantidade fracionária, cotação, valor atual, resultado não realizado e rentabilidade percentual, sem conversão cambial

#### Scenario: Indicadores fora do escopo ausentes
- **WHEN** qualquer posição é retornada
- **THEN** a resposta não contém resultado realizado, patrimônio ou snapshot

### Requirement: Cotação persistida e referência temporal preservada
Cotação, referência tipada, obtenção e dataHoraCotacao legado SHALL refletir exatamente a Ação persistida. GET MUST NOT consultar providers, usar Clock para substituir referências, classificar defasagem por calendário, atualizar a Ação ou escrever. Cotação positiva e campo legado SHALL permanecer obrigatórios; somente os novos campos seguem a nullability da transição. Nenhum fallback de referência SHALL ser fabricado.

#### Scenario: Data e hora persistidas
- **WHEN** a Ação associada possui `dataHoraCotacao` persistida
- **THEN** a posição devolve exatamente essa referência temporal, sem substituição pelo instante da consulta

#### Scenario: Consulta repetida
- **WHEN** a mesma posição é consultada repetidamente sem atualização da Ação
- **THEN** `cotacaoAtual` e `dataHoraCotacao` permanecem iguais aos valores persistidos em todas as respostas

#### Scenario: Estado suportado da Ação
- **WHEN** uma posição aberta é consultada sobre o schema Liquibase vigente
- **THEN** sua Ação possui cotação não nula e positiva e data/hora não nula, sem necessidade de tratamento artificial para ausência, zero ou valor negativo

### Requirement: Compatibilidade sem persistência adicional
A posição SHALL continuar derivada sem tabela, entidade persistida ou campo financeiro consolidado. A migration temporal pertence a Ação/histórico, não ao estado contábil de posição. SHALL preservar POST /operacoes, replay, consultas, DELETE de Carteira e fórmulas. PATCH individual e POST do lote SHALL ser responsáveis pela atualização; GET de posições MUST NOT consultar provider.

#### Scenario: Inicialização com schema vigente
- **WHEN** PostgreSQL ou H2 executa o changelog completo
- **THEN** valida o modelo temporal aditivo sem tabela ou migration de posição

#### Scenario: Regressão das Operações
- **WHEN** os dados de mercado são acrescentados à consulta de posição
- **THEN** registro, consulta, cronologia, concorrência e proteção do histórico de Operações preservam seus contratos atuais

#### Scenario: Atualização dedicada preservada
- **WHEN** PATCH individual ou POST de atualização da carteira é acionado
- **THEN** usa regra compartilhada e preserva GET de posições sem efeitos colaterais
