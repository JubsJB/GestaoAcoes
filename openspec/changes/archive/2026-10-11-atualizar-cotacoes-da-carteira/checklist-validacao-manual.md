# Checklist da sessao manual final - 12.1

Ratificacao posterior autorizada pelo usuario:12.1,15.1,15.2 concluidas;estado atual40/40,0pendentes. Resultados manuais abaixo preservados integralmente,inclusive loading NAO OBSERVADO e recarga NAO CONFIRMADA VISUALMENTE. Fechamento documental aprovado nao executa archive ou Git.

Preparado em2026-10-11. Sessao final concluida pelo usuario, com resultados registrados abaixo. A falha inicial da PETR4 permanece no historico.40tasks/37concluidas/3pendentes:12.1,15.1,15.2. Nenhum servidor iniciado, banco consultado ou dado alterado pelo agente nesta consolidacao. Nao foram criados registros/fixtures para a sessao.

## Ocorrencia manual PETR4 - FALHOU

Relato do usuario em2026-10-11:Acoes -> Detalhe -> PETR4, erro "Instante não exatamente representável em microssegundos". Estado visivel preservado:49.26,legado2026-09-25T00:27:52Z,acaoId1,cotacaoPreservada=true. Detalhes tecnicos expostos no alerta vermelho. Preservacao visual confirmada pelo usuario;atualizacao/feedback final FALHOU. Leitor de tela,mobile e demais passos NAO OBSERVADOS neste relato.12.1 permanece aberta. Diagnostico e decisoes propostas em diagnostico-precisao-petr4-manual.md;nenhuma correcao implementada nesta investigacao.

As atualizacoes clicadas por voce na futura sessao sao o fluxo normal da aplicacao e podem persistir cotacao/historico. Este roteiro nao as executa nem autoriza reset, SQL manual, migration, alteracao de provider/configuracao ou saneamento. Nao alterar dados para fabricar erro. Uma tentativa individual e um lote bastam; repetir somente quando houver uma observacao visual/assistiva ainda necessaria.

## Registro unico

Correcao localizada autorizada implementada em2026-10-11:Clock de aquisicao micros explicito e alerta unico amigavel;ver correcao-petr4-precisao-feedback.md. Revalidacao da PETR4 aprovada pelo relato humano abaixo;nao substitui nem apaga a falha inicial. Nenhuma task marcada.

### Resultado final informado pelo usuario

| Item | Resultado exato |
| --- | --- |
| Atualizacao individual PETR4 | PASSOU apos correcao do Clock |
| Atualizacao em lote | Feedback correto: 2 atualizadas, 1 sem nova cotacao e 0 falhas |
| Estado de carregamento do lote | NAO OBSERVADO |
| Recarga dos indicadores apos o lote | NAO CONFIRMADA VISUALMENTE |
| Dashboard em 360 px | PASSOU |
| Detalhe da Acao em 360 px | PASSOU |
| Navegacao por teclado | PASSOU |
| Leitor de tela | PASSOU |
| Observacoes adicionais | Nenhuma |

Relato humano, nao execucao do agente. Nao foram informados navegador/versao, fuso, leitor/versao, screenshot ou transcricao dos anuncios;nao fabricar essas evidencias. O PASSOU do leitor nao afirma que o loading nao observado tenha sido ouvido. Loading e recarga continuam com os resultados manuais acima;conferencia automatizada complementar e recomendacao para12.1 em revisao-final-pos-validacao-manual.md.

Navegador/versao:_____ Fuso:_____ Desktop:_____ Mobile:360px ou aparelho_____ Leitor de tela/versao:_____ Data/hora:_____

Marcar o resultado de cada passo na sessao:PASSOU/FALHOU/NAO OBSERVADO. Registrar uma frase do esperado/observado e screenshot apenas quando util. Para leitor de tela, anotar o anuncio ouvido ou sua ausencia; screenshot/DOM nao prova anuncio. Evitar gravar credenciais,console/payload integral ou dados nao necessarios. Estes checkboxes sao acompanhamento da sessao, nao tasks OpenSpec concluidas.

## Sequencia pratica

| Passo/pagina | Acao sua | Esperado | Evidencia minima | Indicio de problema |
| --- | --- | --- | --- | --- |
| 1. Acoes /acoes; detalhe /acoes/{id} | Antes de atualizar, escolher uma Acao existente e inspecionar referencias disponiveis na lista/detalhe. | DATA civil,INSTANTE local ou DESCONHECIDA explicita;obtencao separada, nula="Nao informada" (com acento na UI). | Nota dos tipos vistos + uma captura do detalhe; nao precisa capturar cada registro. | Referencia usa timestamp legado,DATA ganha hora/dia diferente,texto cortado ou obtencao misturada com referencia. |
| 2. Detalhe da mesma Acao | Com Tab acessar Atualizar cotacao;acionar com Enter/Space uma vez. | Foco visivel,loading/busy,botao protegido;finalizacao libera acao e mostra estado retornado/mensagem honesta. Falha natural preserva estado e permite tentativa manual. | Nota do feedback/anuncio ouvido;captura final apenas se util. | Sem nome/foco,botao ocupado preso,erro oculto,anuncio ausente,ou mensagem garante cotacao nova/provider. Nao exigir preco numerico diferente. |
| 3. Dashboard /dashboard | Escolher uma carteira existente com posicoes;acionar Atualizar cotacoes, nao Atualizar dados,uma vez. | Ocupado anunciado;contadores/feedback identificam carteira;indicadores,posicoes e desempenho reapresentados apos recarga;botao liberado. | Uma captura do resultado com nota sobre recarga/anuncio. | Carteira/feedback errados,contadores ilegíveis,loading preso,sucesso apagado por erro de recarga ou resultado parcial apresentado como sucesso integral. Nao recalcular patrimonio manualmente. |
| 4. Dashboard + detalhe no mobile | Reduzir viewport para360px ou usar aparelho;inspecionar os mesmos estados e navegar com teclado/leitor. | Reflow sem perder conteudo/controles;referencias completas;foco,nome de botao,estado ocupado e feedback compreensiveis;contraste suficiente. | Uma captura mobile e nota sobre operacao por teclado/anuncios (normalmente duas paginas, sem novos requests). | Sobreposicao,texto/controle inacessivel,foco invisivel,trava de navegacao ou mensagem nao anunciada. Se houver duvida real de contraste,medir so o par de cores afetado. |
| 5. Estado adicional somente se faltar aceite visual | Aproveitar falha/reuso/carteira vazia que tenha aparecido naturalmente. Se nao houver, consultar a tabela abaixo com apoio tecnico. | Visual e semantica adequados para variantes ainda nao inspecionadas. | Nota de estado real ou SIMULADO e observado;nenhuma escrita adicional para provocar falha. | Variante warning/error nao legivel/nao anunciada,motivo disabled nao acessivel ou simulado registrado como integracao real. |

No passo1,ver tipos desconhecidos antes das atualizacoes evita perder a oportunidade de inspecionar legado naturalmente. Posicoes e desempenho ficam no Dashboard; se um ativo/posicao nao existir,nao cadastrar para o teste. Carteira sem posicoes, se ja existir, pode confirmar botao visivel/disabled com explicacao,sem chamar lote.

## Simulacao: quando e realmente necessaria

| Estado indisponivel | Necessidade para aceite manual |
| --- | --- |
| Reuso/cooldown ou resposta igual | Nao obrigatoria se ja foi validado o anuncio/visual do feedback de processamento. Conteudo exato e ausencia de promessa de cotacao nova estao automatizados. PATCH nao expõe classificacao REUSO;nao precisa comprovar caminho interno. |
| Falha parcial/todas as falhas | Necessaria apenas para uma variante visual/assistiva warning/error ainda nao observada e que tenha papel/estilo diferente do estado ja inspecionado. Apoio tecnico pode mostrar fixtures ja testadas em sessao UI isolada,interceptando antes de qualquer request de escrita. Nao exigir todos os codigos HTTP/contadores/combinacoes. |
| Loading muito rapido | Se nao for possivel confirmar busy/anuncio,fazer apoio tecnico com atraso controlado somente na sessao de navegador. Nao disparar chamadas repetidas para procurar um estado lento. |
| DATA/INSTANTE/DESCONHECIDA ou obtencao nula ausentes | Nao obrigatoria so para reproduzir todos os tipos:formatter/fusos/precisao ja testados. Simular somente se falta conferir legibilidade/reflow/leitura de um texto ou superficie visual nao coberta pelos registros existentes. Nunca gravar DTOs no PostgreSQL. |
| Carteira vazia/disabled ausente | Inspecao tecnica somente se explicacao/navegacao assistiva do disabled ainda nao foi validada;nao vender/apagar operacoes para zerar carteira. |
| Erro global/resultado nao confirmado/erro de recarga | Nao obrigatoria como nova bateria. Usar um estado representativo apenas se seu anuncio ou layout distinto precisar de aceite;algoritmo/recuperacao ja cobertos. |

Nenhuma simulacao foi preparada/executada aqui. Exige apoio tecnico se necessaria;voce nao precisa provocar falhas externas,alterar configuracoes ou montar respostas. Uma simulacao UI nao comprova integracao real. Falha assistiva/visual observada deve ser registrada como falha,nao dispensada por testes automatizados.

## Checklist curto para acompanhar

- [ ] Paginas existentes e referencias inspecionadas antes das atualizacoes.
- [ ] Uma atualizacao individual:busy,retorno e feedback honesto conferidos.
- [ ] Um lote:feedback,contadores e Dashboard apos recarga conferidos.
- [ ] Mobile,reflow,foco e navegacao por teclado conferidos.
- [ ] Leitor de tela:nome/busy/feedback e anuncio efetivo registrados.
- [ ] Variantes visuais/assistivas faltantes avaliadas;simulacao somente quando necessaria,identificada.
- [ ] Resultado PASSOU/FALHOU/NAO OBSERVADO e evidencias registrados,sem ratificar tasks automaticamente.

Nao repetir manualmente:concorrencia/rollback,migration,BC/outubro1582/0001/9999,microssegundos/fusos,calculos financeiros/replay,ordem dos quatro GETs,duplo POST por troca de carteira e matriz de payloads/codigos. Ja certificados automaticamente. A sessao manual avalia experiencia real,no minimo de estados necessario.

## Encerramento posterior

Com resultado manual,consolidar15.1 e apresentar revisao humana15.2.12.1,15.1,15.2 permanecem pendentes ate evidencias/aprovacao;nao encerrar change,archive ou operar Git automaticamente.
