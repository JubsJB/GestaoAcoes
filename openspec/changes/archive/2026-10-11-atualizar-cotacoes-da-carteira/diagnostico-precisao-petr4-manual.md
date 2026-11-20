# Diagnostico PETR4: precisao temporal e feedback

Registro em2026-10-11, a partir da observacao manual informada pelo usuario. Resultado: FALHOU.12.1 permanece aberta;40tasks/37concluidas/3pendentes(12.1,15.1,15.2). Nenhum codigo, task, banco, migration, contrato ou permissao modificado. Nenhum PATCH/provider/SQL disparado nesta investigacao; nenhuma suite completa reexecutada.

## Observacao manual

Pagina Acoes -> Detalhe -> PETR4. Mensagem: "Instante não exatamente representável em microssegundos". Details informados:acaoId1,ultimaCotacaoValida49.26,dataHoraUltimaCotacao2026-09-25T00:27:52Z,cotacaoPreservada=true. Preservacao do estado visivel:confirmada pelo usuario. Problema operacional/UX:atualizacao rejeitada e detalhes tecnicos renderizados no alerta vermelho. Sem evidencia fornecida de navegador/leitor de tela/anuncio;nao registrar esses itens como aprovados.

IMPORTANTE:dataHoraUltimaCotacao e o timestamp LEGADO da cotacao ja persistida, nao a referencia nova recebida nem o cotacaoObtidaEm da tentativa rejeitada. Nao permite determinar o campo causador.

## Rastreio comprovado pelo codigo

1. AcaoDetailPageComponent.updateQuote guarda busy e chama AcoesService.atualizarCotacao(id).
2. AcoesService envia PATCH /acoes/{id}/cotacao com null body;normalizador HTTP preserva StandardError.
3. AcaoResource -> AcaoService.atualizarCotacao -> atualizarCotacaoCompartilhada. Mercado BRASIL vai diretamente a atualizarCotacaoPersistida, sem atalho de reuso/cooldown EUA.
4. BrapiAdapter.consultar recebe regularMarketTime. Numero inteiro vira Instant.ofEpochSecond (nanos0);texto ISO e parseado preservando sua fracao. Numeros fracionarios/overflow sao marcados invalidos antes da candidata. Texto com nanos significativos pode chegar a validacao central.
5. CotacaoProvider.consultarCandidataAtualizacao -> CotacaoCandidata.obtidaDe, com referenciaInstante=data.dataHoraCotacao do provider e cotacaoObtidaEm=OffsetDateTime.now(clock).
6. Construtor CotacaoCandidata valida referenciaInstante PRIMEIRO e cotacaoObtidaEm DEPOIS em instantePersistivel. getNano()%1000 !=0 produz exatamente a mensagem observada, codigo REFERENCIA_TEMPORAL_INVALIDA e status422 no codigo. A mensagem nao identifica o campo. Nenhum outro campo chama essa mensagem;DATA/dataHoraCotacao legado nao sao validados como obtencao aqui.
7. AcaoService captura ApiException antes de chamar AplicacaoCotacaoService.aplicar. withPreservedQuote acrescenta os quatro details do estado previamente lido. ResultadoAtualizacaoCotacao.respostaIndividual relanca a falha;ResourceExceptionHandler devolve StandardError sem traduzir a mensagem.
8. Frontend normaliza/preserva message e details;AcaoDetailPageComponent passa ambos diretamente ao FeedbackAlertComponent. detailEntries enumera cada chave/valor bruto em dl, explicando os nomes tecnicos vistos. Callback de erro conserva DTO e define quotePreserved=true, ativando tambem o aviso amarelo.

O erro ocorre antes do nucleo transacional/historico, nao no binding JDBC A, SQL DATE, banco, mapper ou conversao de fuso. Com essa causa, o codigo nao escreve preco/referencia/obtencao/historico. Nao houve leitura do banco para afirmar estado persistido atual:constatacao e pelo fluxo e pelo relato de preservacao visivel.

## Campo exato: limite da evidencia

Dois caminhos produzem a MESMA mensagem:

- referenciaInstante nao representavel:fonte BRAPI, possivel quando regularMarketTime e texto ISO com nanos significativos alem de6casas. Rejeicao correta de dado externo realmente incompativel;nao arredondar.
- cotacaoObtidaEm nao representavel:fonte Clock backend, sem transformacao de precisao. TimeConfig.applicationClock retorna Clock.systemUTC, que nao garante microssegundos. Defeito de integracao/configuracao da origem com o contrato persistivel, nao permissao para relaxar a candidata.

Hipotese principal:cotacaoObtidaEm do Clock. Porem NAO confirmado como campo exato da requisicao PETR4, pois o erro e os details recebidos nao carregam referencia/obtencao novas. Nem o nano real do Clock nem regularMarketTime dessa tentativa foram capturados. Se referencia nova tiver nanos0, a mensagem implica obtencao;se ela tiver nanos excedentes, o construtor falha nela antes de verificar obtencao.

Defeito concreto comprovado independentemente da atribuicao desta tentativa:TimeConfig nao assegura que o Clock de aquisicao satisfaz nano%1000=0, embora a propria aplicacao rejeite sua saida quando nao satisfaz. A documentacao oficial Java17 confirma que systemUTC usa a melhor resolucao disponivel, podendo ser superior a milissegundos;nao promete alinhamento micro. [Clock Java17](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Clock.html).

Uma tentativa de amostragem isolada via JShell local, sem aplicacao/provider/banco, foi impedida antes de executar por SecurityException de preferencias Windows. Nenhuma amostra produzida;nao apresentar a hipotese como confirmada por execucao nem elevar permissao para essa sondagem acessoria.

Para fechar a atribuicao exata:minima observacao via debugger local no breakpoint instantePersistivel, registrando SOMENTE qual chamada (referencia/obtencao),os dois valores novos e nano%1000. Alternativamente,regularMarketTime sanitizado dessa mesma resposta pode excluir a referencia quando exato. Nao solicitar apiKey/URL autenticada,dump completo,credentials ou dados do banco;nao disparar request automaticamente nesta etapa.

## Contrato e testes

Aplicar exatidao em microssegundos aos DOIS campos e correto pelo contrato aprovado;UTC/withOffsetSameInstant nao elimina nem introduz nanos residuais. Piso1970 e referencia<=obtencao propria so sao avaliados depois;mensagem atual nao e de piso/futuro. Nao alterar tolerancia zero.

CotacaoPrecisaoTemporalTest.construtorProtegeReferenciaEObtencaoSemPerderInstante cobre ambas as rejeicoes;clockNaoRepresentavelEhRejeitadoInclusiveEmDataEDesconhecida intencionalmente prova rejeicao de Clock.fixed com nanos123456789. Ou seja,o validador foi testado corretamente, mas nao a adequacao do bean TimeConfig real.

CotacaoCandidataProvidersTest cobre BRAPI texto com123456000 aceito versus123456001/123456789 rejeitados, usando Clock.fixed exato. BrapiPrecisionTest usa Clock.fixed com nanos0;AcaoCotacaoHttpIntegrationTest substitui applicationClock por mock com .123456Z exato. Os fluxos verdes nao exercitaram a combinacao de provider valido com o Clock de producao nao alinhado. Nao encontrado teste que certifique a resolucao do bean TimeConfig.

Frontend:acao-detail-page.component.spec verifica preservar DTO, warning com role alert e detalhes;um teste chega a exigir texto cotacaoPreservada na pagina. Assim,a exposicao tecnica foi comportamento aprovado pelos asserts existentes,nao uma falta de parsing. Normalizador trata envelope/erro de transporte,nao transforma todos os erros backend em copy amigavel. Nao houve validacao real de anuncios concorrentes pelo leitor de tela;12.1 continua aberta.

## Preservacao e dois alertas

Preservacao da ultima cotacao e correta neste fluxo;nao trocar por zero/DESCONHECIDA ou reconstruir referencia. Timestamp dataHoraUltimaCotacao permanece diagnostico legado,nunca fallback visual de referencia.

Os dois alertas nao sao exigencia do HTTP:vermelho explica erro,amarelo explica preservacao,mas ambos usam role=alert/aria-live=assertive. Podem causar anuncios duplicados/competitivos;esse efeito assistivo nao foi observado pelo usuario e deve ser tratado como risco,nao fato medido. Uma unica mensagem pode explicar falha e preservacao sem perder informacao util. Callback atual tambem marca preservacao em todo erro;isso garante apenas manter o DTO exibido,não prova ausencia de commits em falha tecnica/conexao perdida.

## Menor correcao proposta, nao implementada

1. Confirmar o campo desta tentativa por observacao minima. Se for referencia BRAPI realmente nao representavel,manter422 e ultima cotacao;nao corrigir/tolerar/truncar timestamp externo.
2. Para o defeito do Clock,definir explicitamente uma fonte UTC de aquisicao com resolucao contratual micro,preferencialmente restrita a cotacoes,sem alterar Clock de regras financeiras/calendarios/cooldown por conveniencia. Manter rejeicao na candidata de QUALQUER valor nao representavel. Nao usar truncatedTo/rounding em obtidaDe,mapper,entidade ou JDBC para mascarar o dado.
3. Clock.tick(base,Duration.ofNanos(1000)) e uma opcao pequena de configuracao,mas a API TRUNCA o relogio base para ticks. Nao e conversao lossless:essa quantizacao na fonte precisa de aprovacao explicita/documentacao,especialmente diante da proibicao anterior de truncamento. Sem essa decisao,nao incorporar silenciosamente. Nao usar clockMillis para ocultar erro reduzindo arbitrariamente a resolucao;nao relaxar referencia<=obtencao nem nanos de provider.
4. No detalhe de Acao,traduzir erro temporal para mensagem segura e compreensivel;nao renderizar keys/valores brutos de details. Manter code/message/details originais no contrato/estado tecnico,sem alterar normalizador/FeedbackAlert global ou outras telas. Consolidar erro+preservacao em uma mensagem acessivel (ou um aviso complementar nao urgente se aprovado),mantendo preco/referencia exibidos. A politica de copy/apresentacao deve ser aprovada antes do ajuste,pois os testes atuais exigem detalhes.
5. Testes futuros focados:resolucao da fonte configurada com base controlada;provider exato+obtencao da fonte real de aquisicao;provider nanos excedentes continua422;nenhuma escrita apos rejeicao;DTO/HTTP existentes preservados. Frontend:nao expoe detalhe tecnico,explica preservacao,anuncio unico e retry apenas manual. Depois repetir apenas o passo manual PETR4 afetado/assistivo,nao toda a matriz temporal.

Decisoes necessarias:politica explicita de resolucao na origem do Clock (sem arredondamento silencioso de valores adquiridos) e apresentacao amigavel/local de erro/preservacao. Campo exato da requisicao ainda requer observacao minima;nao afirmar defeito do provider por falta de evidencia.

Bloqueante operacional:atualizacao individual observada falhou;configuracao da origem temporal pode rejeitar obtencoes validas de mercado. UX/assistiva:rawdetails e duplicacao urgente precisam de aceite/correcao. Dados e tarefas permanecem preservados;esta etapa e somente diagnostico/registro manual.
