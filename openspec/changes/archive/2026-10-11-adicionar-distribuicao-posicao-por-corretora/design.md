## Context

Operacao possui carteira_id/acao_id obrigatorios e corretora_id anulavel. Migration 004 ja possui FK e indices de cronologia e corretora. Carteira e Acao nao exigem associacao com Corretora. PosicaoService agrupa por Acao e CalculadoraPosicao reproduz quantidade/preco/custo por Carteira + Acao, ignorando corretora. OperacaoPersistenceService valida a candidata e todo o historico sob lock da Carteira, sem validacao por corretora.

O detalhe da Carteira ja consulta historico e posicoes; CorretoraNamesService resolve nomes no historico. Dashboard e formulas financeiras permanecem intactos. README ainda menciona seis migrations, embora 007 exista; essa defasagem nao exige editar migrations nem corrigir documentacao externa nesta etapa.

## Goals / Non-Goals

Goals: leitura independente, quantitativa, exata e consistente; avisos rastreaveis, incluindo categorias zeradas; frontend sem calculo de saldo.
Non-Goals: custodia comprovada, preco medio/realizado por corretora, transferencia, extratos, bloqueio de operacoes, reparo de legado, novos vinculos/tabelas e mudancas no Dashboard.

## Decisions

### Estrategia efetiva da camada de consulta

DistribuicaoPosicaoCorretoraService possui transacao propria REQUIRES_NEW/readOnly/SERIALIZABLE,acessada via proxy Spring. Nao herda isolamento mais fraco nem entidades gerenciadas do chamador. Carteira e validada por findById e historico completo daquela carteira e carregado por consulta dedicada com EntityGraph de Acao/Corretora. Nomes e operacoes integram o mesmo SELECT;consolidado e distribuicao derivam da mesma lista,sem chamadas a consultas independentes. Nao modifica queries existentes.

SERIALIZABLE foi necessario para a estrategia comum:teste concorrente H2 mostrou operacoes antigas/nome novo com REPEATABLE_READ e passou com SERIALIZABLE. Teste confirma isolamento JDBC e read-only efetivos,inclusive chamador READ_COMMITTED. PostgreSQL temporario permanece pendente da task4.6;nao inferir certificacao PostgreSQL a partir do H2. Transacao curta,possivel conexao adicional quando chamada dentro de outra transacao e eventual falha de serializacao seguem riscos tecnicos existentes,sem retry automatico. Evidencia detalhada em resultado-camada-consulta.md.

### Fronteira e algoritmo

Servico dedicado de leitura, usando a estrategia efetiva SERIALIZABLE/readOnly/REQUIRES_NEW descrita acima (a proposta inicial REPEATABLE_READ foi substituida apos evidencia H2). Ler operacoes uma vez, ordenadas por dataOperacao, ordemNoDia e id; usar validarQuantidade do replay oficial por Acao para obter quantidade consolidada cronologica, sem duplicar regras financeiras nem transformar corretora em dimensao de preco medio. O id e apenas desempate tecnico, nunca muda a ordem financeira.

Para cada Acao consolidada aberta, manter acumulador BigDecimal por corretoraId (null e um grupo proprio); COMPRA soma e VENDA subtrai. Registrar primeiro deficit por grupo e flag historica duravel durante TODO o replay do historico persistido, inclusive ciclos anteriores. Regularizacao ou zeramento nao apagam a evidencia historica. Nao truncar negativo nem interromper replay so por deficit do grupo, pois ele nao invalida a operacao consolidada atual.

Final negativo gera aviso atual, com evidencia historica; final zero remove SOMENTE a linha quantitativa, nao aviso historico. Final positivo com deficit anterior continua com aviso. Somar os finais com sinal, incluindo null e zeros, e comparar com consolidado por valor, nao escala. Nunca excluir negativos para fabricar soma correta. Posicao consolidada zero nao entra no endpoint, mesmo com deficit por corretora; nao ampliar esta change para auditoria de posicoes encerradas.

Alternativas rejeitadas: SQL SUM isolado perde deficits intermediarios; replay financeiro por corretora muda custo/realizado; calculo no navegador duplica autoridade; nova tabela materializada traz sincronizacao e migration desnecessarias.

### HTTP final

GET /carteiras/{carteiraId}/posicoes/distribuicao-corretoras, sem body funcional, independente de GET /posicoes. 200 para Carteira existente, inclusive colecao vazia, deficits por agrupamento e divergencia de reconciliacao explicitamente diagnosticada; 404 para Carteira inexistente. Historico consolidado invalido segue erro padronizado de replay, sem fabricar uma distribuicao. Erro inesperado segue tratamento seguro existente.

Schema FINAL em contrato-http.md: campos obrigatorios/tipos/nulabilidade e cinco exemplos completos. Posicao possui corretoras,diagnosticosCorretoras e diagnosticoReconciliacao separado. Cada agrupamento tem um diagnostico logico unico com quantidadeFinal,saldoNegativoAtual,possuiDeficitHistorico,primeiroDeficit;inclui grupos zerados/saudaveis sem gerar alerta quando flags falsas. Null de corretora identifica somente categoria "Sem corretora informada". Reconciliacao nunca tem corretoraId.

Primeiro deficit e obrigatorio quando historico:true e nulo quando false;identifica primeira operacao da ordem oficial cujo saldo apos aplicacao fica negativo,dataOperacao,ordemNoDia,operacaoId e saldoAposOperacao. Nunca pior ocorrencia ou reset. Reconciliada:true exige diagnosticoReconciliacao:null,mas nao ausencia de deficits. Aviso unico por grupo combina flags;divergencia da posicao e aviso separado. Status/codigos exatos em contrato-http.md,seguindo StandardError vigente sem alterar erros de outros endpoints.

Decimals continuam JSON numeric BigDecimal com parsing textual lossless no Angular;campos novos no parser/guard proprio,sem Number/parseFloat para quantidade. Operandos BRASIL inteiros/EUA ate6decimais;acumuladores derivados nao recebem limite NUMERIC(19,6) artificial capaz de arredondar somas de grupos com compensacoes negativas.

### Consistencia, nomes e compatibilidade

Consolidado e distribuicao derivam do mesmo historico/snapshot;nao reconciliar respostas de dois GETs tomados em instantes distintos. Carregar nomes por fetch/projecao ou consulta em lote dos IDs,no mesmo snapshot,nao uma consulta por linha. Retornar apenas identificacao necessaria,sem dados cadastrais sensiveis. Ordenacao FINAL em contrato-http.md:mercado BRASIL/EUA,ticker ordinalUTF-16 eacaoId;grupos/diagnosticos por null-last,nome ordinalUTF-16 eID numerico. Sem collationSQL,locale,case-folding,normalizacao de acentos ou prioridade de severidade interferindo no replay.

Endpoint separado evita alterar DTOs/guards existentes. Sem novos providers/relogios nem escrita. Guardas existentes de operacoes/financeiro permanecem iguais. Inconsistencia da distribuicao nao deve contaminar GET /posicoes/resumo/resultados nem o POST de operacoes.

### Frontend localizado

Painel informativo proprio em Detalhe da Carteira -> Posicoes abertas,sem alterar tabela consolidada compartilhada. Consultar para Carteira valida,exibir identificacao do ativo e mensagens "distribuicao das operacoes registradas, nao custodia comprovada". Reutilizar formatters lossless e estados/acessibilidade existentes. Nao inferir saldo do historico carregado no browser.

Loading/vazio/erro proprios,retry manual,cancelamento/controle de geracao ao trocar Carteira. Operacao registrada recarrega painel junto com posicoes;nao adicionar polling nem POST. Falha do painel nao oculta dados consolidados. Dashboard,listagem de Carteiras e formulario compra/venda nao exigem alteracoes nesta fatia. Os avisos de corretora zero aparecem no painel da posicao,fora da lista quantitativa.

## Risks / Trade-offs

- Historico pode produzir negativos de grupo apesar de consolidado valido -> preservar numeros e avisos,sem saneamento.
- Duas consultas independentes podem diferir legitimamente por concorrencia -> reconciliacao autoritativa dentro do novo response,nao pelo DTO anterior.
- Ocultar grupo zero pode ocultar deficit -> lista independente de avisos com nome/categoria.
- Nome de corretora pode mudar -> quantidade/agrupamento por ID,nome atual informativo.
- Somar precos medios por corretora alteraria contabilidade -> nao produzir metricas financeiras locais.
- Historico grande -> medir queries/tempo;sem N+1,novos indices somente com evidencia/autorizar migration.
- Fixtures temporais JDBC/BC fora do Git -> nao incorporar;testes novos independentes desses arquivos.
- Registro sem corretora nao revela local real -> categoria explicita,sem inferencias.

## Migration Plan

Nenhuma migration prevista:leitura derivada sobre colunas/indices existentes. Nao preencher nulos,editar operacoes,modificar 004/007 ou criar tabela de posicao. Se necessidade concreta aparecer,parar e solicitar autorizacao.

Futuro deploy apenas backend endpoint e frontend consumidor,com contrato anterior preservado. Rollback remove consumidor/endpoint novo sem tocar dados. PRD/README/documentacao de API atualizados somente na implementacao aprovada. Archive/sync/Git sao autorizacoes separadas.

## Review Gate

Contrato tecnico fechado em contrato-http.md e nos deltas,conforme ajustes autorizados. Nao ha ambiguidade funcional pendente. Implementacao NAO iniciada nesta etapa documental;aguarda autorizacao de execucao. Medicao de desempenho e escolha fetch/projecao podem ser resolvidas na implementacao sem mudar contrato;PostgreSQL temporario e H2 previstos explicitamente. Nao comparar snapshots frontend independentes nem introduzir percentuais/roscas/normalizacoes.
