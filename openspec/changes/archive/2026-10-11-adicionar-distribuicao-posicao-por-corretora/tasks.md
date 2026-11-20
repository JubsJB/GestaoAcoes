## 1. Ratificacao do plano

Estado final aprovado pelo usuario:29 tasks,29 concluidas,0 pendentes.7.5 ratificada apos revisao humana e correcao da dependencia dos testesHTTP. Ressalvas aceitas:integracao ponta a ponta real nao executada,SERIALIZABLE/conexao adicional ePostgreSQL opt-in exclusivamente descartavel. Archive eGit deescrita NAO executados nesta etapa;contagens abaixo sao historicas.

Estado apos conclusao documental:29 tasks,28 concluidas,1 pendente (7.5). Nesta etapa1.2,7.1,7.2,7.3 concluidas;matriz emrastreabilidade-rf.md e certificacao emresultado-documentacao-final.md. Contagens abaixo sao historicas. Integracao ponta a ponta real NAO EXECUTADA;aprovacao humana final pendente.

Estado apos consolidacao manual:29 tasks,24 concluidas,5 pendentes (1.2,7.1,7.2,7.3,7.5). Nesta etapa somente1.1,7.4 ratificadas. Evidencias e ressalva de integracao real em resultado-consolidacao-manual.md. Contagens posteriores neste arquivo sao historicas.

Estado apos certificacoes finais (2026-10-11):29 tasks,22 concluidas,7 pendentes. Somente4.5,4.6,6.4 concluidas nesta etapa; evidencias em resultado-certificacoes-finais.md. Demais contagens abaixo sao historicas. Validacao manual real,documentacao final e fechamento continuam pendentes.

Estado atual apos painel Angular (2026-10-11):29 tasks,19 concluidas,10 pendentes. Nesta etapa somente5.2,5.3,5.4,6.1,6.2 adicionadas as conclusoes anteriores;6.3 revalidada com249 testes/25arquivos eTypeScript app/spec aprovados. Evidencia em resultado-painel-angular.md. Zoom real/leitor de tela permanecem na validacao manual7.4;4.6 permanece pendente da integracao PostgreSQL temporaria. Contagens anteriores sao historicas.

- [x] 1.1 Ratificar contrato-http.md/proposal/design/deltas e obter autorizacao de implementacao antes de DTO/backend/consumidores;schema e HTTP nao ficam a criterio implicito do implementador.
- [x] 1.2 Confirmar limites de escopo e matriz RF01-RF12 sem alterar optionalidade ou venda consolidada.

## 2. Backend de leitura

- [x] 2.1 Criar fronteira consistente de consulta com carteira validada e historico ordenado,sem escrita.
- [x] 2.2 Reutilizar replay quantitativo oficial para selecionar posicoes abertas no mesmo snapshot.
- [x] 2.3 Implementar acumulacao decimal por corretora/null e diagnostico unico por grupo,com flags/nulabilidade e primeira ocorrencia do saldo apos operacao,de todo historico;desenvolver junto com4.1/4.2.
- [x] 2.4 Separar linhas nao zero de avisos,incluindo grupos zero e negativos atuais.
- [x] 2.5 Reconciliar total com sinal e consolidado,diagnosticando divergencia sem reparo.
- [x] 2.6 Carregar nomes/fallback em lote/fetch,ordenar UTF-16/null-last/desempatesID e medir ausencia de N+1,junto com4.2/4.3.

## 3. Contratos HTTP

- [x] 3.1 Implementar DTOs conforme contrato fechado em1.1 e GET separado com decimal exato,campos obrigatorios,200/400/404/409/500 e erros seguros,junto com4.4;consumidores5.x dependem desta fronteira.
- [x] 3.2 Documentar schemas/exemplos OpenAPI sem alterar OperacaoResponse/PosicaoResponse.

## 4. Testes backend e compatibilidade

- [x] 4.1 Cobrir uma/varias corretoras,null,zero,negativo,diagnostico unico e deficit regularizado/inclusive ciclos anteriores;primeira ocorrencia nao pior saldo/reset e nulabilidade condicional,junto com2.3/2.4.
- [x] 4.2 Cobrir retroativas,cronologia/primeira ocorrencia,isolamento,reconciliacao,precisao BRASIL/EUA e ordenacao ordinal completa(nomes empatados/acentos/caixa/null-last),junto com2.2/2.5/2.6.
- [x] 4.3 Verificar SQL/entidades imutaveis no GET,snapshot consistente e queries sem N+1.
- [x] 4.4 Cobrir schema/tipos/campos/nulabilidade/arrays eHTTP200/400/404/409/500;divergencia separada em teste isolado sem dados fabricados noPostgreSQLdev,junto com3.1/3.2.
- [x] 4.5 Certificar regressao operacoes/vendas/concorrencia,preco medio,custo,realizado,rentabilidade,snapshots e cotacoes.
- [x] 4.6 Executar regressaoH2 e integracao ISOLADA obrigatoria emPostgreSQLtemporario para snapshot consistente/consultas/N+1/precisao;sem acesso aoPostgreSQLdev ou nova migration presumida.

## 5. Frontend

- [x] 5.1 Criar consumidor/modelos/guards/parsing lossless conforme contrato3.1,nulabilidade/diagnosticos earrays;desenvolver junto com6.1.
- [x] 5.2 Integrar painel localizado no detalhe da Carteira mantendo tabela consolidada e demais telas.
- [x] 5.3 Implementar estados independentes,retry,contexto capturado e recarga apos operacao.
- [x] 5.4 Apresentar no maximo um aviso por grupo inconsistente,zero sem linha,divergencia separada e nenhum percentual/rosca/normalizacao;reflow/teclado/anuncios junto com6.1/6.2.

## 6. Testes frontend e build

- [x] 6.1 Cobrir payloads/tipos/nulabilidade/primeira evidencia,precision,negativos,null/zero,reconciliacao,avisos unicos e ausencia de graficos/recálculo;executar junto com5.1/5.2/5.4.
- [x] 6.2 Cobrir loading/vazio/erro/retry,troca de contexto,recarga e snapshots independentes que NAO geram diagnostico no browser,preservando posicoes;executar junto com5.3/5.4.
- [x] 6.3 Executar regressao pertinente e TypeScript app/spec.
- [x] 6.4 Executar build production/budgets sem elevar limites para passar.

## 7. Documentacao e validacao final

- [x] 7.1 Atualizar PRD/README/documentacao da API conforme funcionalidade aprovada e preservar evidencias.
- [x] 7.2 Executar strict change/specs,rebuild oficial em memoria e diff/whitespace incluindo novos arquivos.
- [x] 7.3 Atualizar/verificar Graphify apos codigo estabilizado,sem incluir experimentos JDBC/BC.
- [x] 7.4 Validar manualmente painel360px,teclado/leitor e estados representativos sem repetir algoritmos automatizados.
- [x] 7.5 Revisar diff integral/contratos e obter aprovacao final antes de fechar tasks/archive/Git.

Nenhuma implementacao autorizada ou iniciada nesta etapa. Todas as tasks pendentes. Banco dev nao deve ser usado para fixtures,inventarios ou testes sem autorizacao separada.

Atualizacao posterior:implementacao do NUCLEO PURO autorizada e realizada em2026-10-11. Somente2.3,2.4,2.5,4.1,4.2 concluidas apos20testes novos e89testes unitarios pertinentes aprovados. Estado29tasks/5concluidas/24pendentes. Registros acima sobre etapa documental sao historicos;nao representam o status atual. Evidencia em resultado-nucleo-backend.md.2.1/2.2/2.6 permanecem pendentes:selecao/replay/nome/ordem em memoria implementados,mas fronteira DB,snapshot transacional eN+1 nao certificados. Nenhuma taskHTTP/frontend/PostgreSQL/documentacao final/fechamento antecipada.

Dependencias de execucao:1.1/1.2 antes dequalquer implementacao;contrato3.1 definido pelo documento antes deconsumidores5.x. Backend2.x econtratos3.x sao construidos com testes4.x;frontend5.x com6.1/6.2. Integracao4.6 ecertificacao4.5 antecedem validacao final7.x. Numeracao agrupa responsabilidades,nao exige adiar todos os testes ate terminar funcionalidade. Manual7.4 limita-se a painel360px,teclado/leitor e avisos representativos,sem repetir algoritmo/matriz automatizada. Contagem29,todas pendentes.
