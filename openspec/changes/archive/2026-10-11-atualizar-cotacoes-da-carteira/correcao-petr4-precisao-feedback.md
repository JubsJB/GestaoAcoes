# Correcao localizada de precisao e feedback PETR4

Data:2026-10-11. Status: implementada/testada, pronta para revalidacao manual. Registro original PETR4 continua FALHOU;nenhum resultado manual novo inventado.40tasks/37concluidas/3pendentes(12.1,15.1,15.2). Nenhuma checkbox alterada.

## Origem confirmada e limite da atribuicao

Instrumentacao temporaria SOMENTE no teste do Clock:128 leituras do applicationClock real desta JVM Java17.0.14;128 com nano%1000 !=0;0 leituras incompatíveis no novo Clock de aquisicao. Evidencia:target/petr4-clock-diagnostic.log. Log continha apenas contadores,sem instantes,credentials,payload BRAPI ou dados financeiros. Sondagem removida do fonte depois de obter o resultado;nenhum logging de diagnostico em producao.

Confirmado:defeito de configuracao da origem de cotacaoObtidaEm,pois systemUTC nao garantia representabilidade exata exigida pela candidata. Testes de integracao com referencia exata e fonte bruta .123456789 confirmam que a fonte configurada agora permite cadastro/PATCH e persiste obtencao .123456Z em Acao/historico. Nao e falha JDBC/DATE/BC.

A resposta original da BRAPI da tentativa PETR4 nao estava disponivel e nao foi recapturada. Nao disparado PATCH ou consulta real ao provider para diagnostico. Portanto NAO afirmar que a tentativa original falhou exclusivamente na obtencao:referencia externa textual com nanos excedentes produz a mesma mensagem,e ambas as origens podem ser incompatíveis. Defeito do Clock foi confirmado independentemente;origem do provider na requisicao original continua indeterminada. Valores de ultimaCotacaoValida/dataHoraUltimaCotacao nao sao os instantes novos da tentativa.

## Producao alterada: tres arquivos

- TimeConfig.java:applicationClock original systemUTC preservado como @Primary;novo bean cotacaoAcquisitionClock derivado explicitamente com Clock.tick(baseClock,Duration.ofNanos(1000)). A quantizacao da resolucao na FONTE foi autorizada pelo usuario;nao e conversao lossless do Clock bruto,e nao foi ocultada como tal.
- AcaoService.java:qualificador no parametro Clock seleciona o bean de cotacoes. Assinatura Java/possibilidade de construir com Clock.fixed em testes preservadas;nenhuma mudanca nas regras. Aquisicao e controle local EUA desse servico observam o Clock declarado em micros;formulas/precedencia/cooldown/reuso nao mudam. Outros consumidores financeiros/calendario/snapshot continuam recebendo o applicationClock original primary.
- acao-detail-page.component.ts:erro de atualizacao agora renderiza UMA mensagem amigavel no alerta vermelho,sem raw details e sem aviso amarelo redundante. Preservacao afirmada apenas com details.cotacaoPreservada === true e DTO anterior presente. Erro de transporte status0 informa resultado nao confirmado,sem prometer ausencia de commits. Mensagens uteis de limite/ticker divergente selecionadas por codigo publico,sem exibir payload bruto.

Clock continua injetavel e UTC no ambiente configurado. Nenhum arredondamento/truncamento em CotacaoCandidata,providers,mapper,entidades ou JDBC. Constructor continua rejeitando qualquer INSTANTE/obtencao nao representavel;provider nanos significativos continuam422. Contratos DATA/INSTANTE/DESCONHECIDA,piso1970,referencia<=obtencao propria e toleranciaZERO mantidos. Nenhum registro existente reinterpretado/modificado.

Frontend conserva o mesmo NormalizedHttpError/code/message/details internamente;HTTP/DTOs nao alterados. FeedbackAlertComponent,normalizador e outras telas nao modificados;ajuste e local ao erro do PATCH. Erro de carregamento inicial e demais acoes continuam com seu comportamento. DOM de um unico role=alert/aria-live=assertive foi testado;anuncio por leitor real aguarda manual.

## Testes alterados/criados

- TimeConfigTest novo:7 casos. Clock real configurado alinhado a1micro/UTC;Clock original continua primary para demais servicos;Clocks fixos com nanos0,123456000,123456001,123456789,999999999 continuam injetaveis e nao sao mutados;tick da fonte produz resolucao declarada.
- AcaoCadastroTemporalIntegrationTest:separa rejeicao dos nanos externos sem escrita e sucesso com fonte bruta de nanos via configuracao;confere obtencao realmente armazenada e historico.
- AcaoCotacaoHttpIntegrationTest:PATCH com fonte bruta nao representavel+referencia exata agora sucesso;obtencao micros em SQL/entidade/historico;provider .123456789 continua erro temporal antes de escrita. Testes existentes de atomicidade/financeiro preservados.
- acao-detail-page.component.spec.ts:atualizados asserts que exigiam cotacaoPreservada na pagina;seis casos extras para payload PETR4 oculto/estado interno intacto,confirmacao ausente/falsa/string nao aceita,sem cotacao anterior e falha de transporte. Sucesso/retry/loading/reuso visual continuam cobertos. Nenhum teste de rejeicao estrita da candidata/provider removido.

## Execucoes e evidencias

1. mvn -o -Dtest=TimeConfigTest,CotacaoPrecisaoTemporalTest,CotacaoCandidataProvidersTest,AcaoCadastroTemporalIntegrationTest,AcaoCotacaoHttpIntegrationTest,AtualizacaoCotacoesCarteiraHttpIntegrationTest,AtualizacaoIndividualCotacaoTest test:106 casos,0falhas/erros/skips. target/petr4-backend-focused.log.
2. mvn -o test:1076reportados,985executados,91skips opt-in/diagnosticos ja auditados,0falhas/erros. target/petr4-backend-regression.log. Rodada completa justificada pela nova configuracao de beans Clock e garantia de regressao de injecao/calculos/servicos;nao reexecutou PG opt-in nem investigacoes historicas.
3. ng test --watch=false com detalhe/FeedbackAlert/normalizador:48 testes/3arquivos aprovados. target/petr4-frontend-focused.log.
4. npm test -- --watch=false:671testes/70arquivos aprovados. target/petr4-frontend-regression.log. Necessaria para verificar isolamento do ajuste da pagina e preservacao de outras telas.
5. npx tsc -p tsconfig.app.json --noEmit e tsconfig.spec.json --noEmit:exit0.
6. npm run build -- --configuration production --stats-json:exit0;target/petr4-build.log. Initial516734bytes,igual ao ultimo build aprovado;detalhe lazy7589bytes. Warnings initial/portfolio-positions/evolution conhecidos abaixo dos limites de erro;nenhum budget/dependencia alterado.
7. graphify update . AST-only:9728nodes/17634edges/790comunidades;target/petr4-graphify.log. Codigo da configuracao/servico/pagina e testes atualizado;nenhuma API/LLM/novo pacote.
8. OpenSpec strict:change validada e 35 specs consolidadas aprovadas. git diff --check e verificacao de whitespace nos arquivos desta correcao aprovados.

Ocorrencias intermediarias:Mockito attach bloqueado por sandbox (pipe JVM acesso negado),processos das tentativas interrompidos e execucao autorizada repetida em modo offline com cache. Um assert novo de cadastro comparava .123456 com a constante antiga .987654 do fixture;corrigida somente a expectativa. Rodadas finais acima aprovadas;nenhuma regra flexibilizada para passar testes.

Backend de testes usa H2/profile test;nenhum acesso/escrita ao PostgreSQL local,nenhuma migration local,nenhum provider real chamado. Nao repetidas DATA/BC,prototipos ou inventario. Nenhum staging/commit/push/merge/archive.

## Riscos remanescentes

- Provider ainda pode fornecer nanos incompatíveis ou referencia futura por divergencia de relogios;rejeicao conservadora e obrigatoria,sem tolerancia positiva.
- Origem exata da antiga resposta PETR4 nao comprovada;nao atribuir exclusivamente ao provider ou ao Clock nessa requisicao.
- Bean da versao antiga ainda rodando precisa ser substituido para testar a correcao. Mockito/DOM nao certificam anuncio assistivo real.
- Clock.tick explicita resolucao da fonte;nao ampliar essa politica para outros relogios/valores ou truncar campos ja adquiridos.

## Revalidacao minima PETR4

1. Carregar o backend recompilado no ambiente dev ja autorizado,sem migration nesta revalidacao:usar opcao de execucao --spring.liquibase.enabled=false e manter --spring.jpa.hibernate.ddl-auto=validate,sem editar arquivos/credenciais. O agente NAO iniciou esse backend. Frontend deve servir a versao nova/recarregada.
2. Abrir Acoes -> PETR4 -> Detalhe;acionar Atualizar cotacao UMA vez. O fluxo normal futuro pode persistir cotacao/historico aceitos;nao usar SQL/fixtures/reset para provocar resultado.
3. Sucesso:botao retorna disponivel,estado retornado exibido,mensagem honesta. Falha:um alerta legivel,sem keys/valores internos;preservacao apenas se confirmada. Com leitor de tela,registrar anuncio real unico;nao certificar por screenshot.
4. Se continuar falhando,registrar somente codigo/mensagem HTTP e observacao sanitizada do campo temporal por apoio tecnico;nenhuma resposta completa/token. Provider realmente incompativel deve permanecer rejeitado;nao repetir chamadas em loop nem relaxar contrato.

Anexar o resultado a checklist-validacao-manual.md;registro original FALHOU mantido.12.1,15.1,15.2 seguem abertas,sem ratificacao automatica.
