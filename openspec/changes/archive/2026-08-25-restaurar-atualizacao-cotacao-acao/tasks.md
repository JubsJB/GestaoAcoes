## 1. Baseline e referÃªncia histÃ³rica

- [x] 1.1 Confirmar na baseline as assinaturas, dependÃªncias e testes atuais de `Acao`, `AcaoResource`, `AcaoService`, `AcaoRepository`, mapper/DTO, providers, adapters e tratamento de erros.
- [x] 1.2 Comparar manualmente o diff do commit Ã³rfÃ£o `999fc4aba4a083a6d2d2f5488ca61a5d2c44764e` com a baseline e registrar quais trechos ainda sÃ£o compatÃ­veis.
- [x] 1.3 Identificar e proteger as evoluÃ§Ãµes posteriores de Carteira, OperaÃ§Ãµes e posiÃ§Ã£o consolidada que nÃ£o podem ser sobrescritas.
- [x] 1.4 Confirmar que `check`, a alteraÃ§Ã£o de `application-dev.properties` e outros trechos nÃ£o relacionados do commit Ã³rfÃ£o estÃ£o excluÃ­dos do plano de implementaÃ§Ã£o.

## 2. DomÃ­nio e persistÃªncia concorrente

- [x] 2.1 Implementar em `Acao` uma operaÃ§Ã£o de domÃ­nio restrita Ã  atualizaÃ§Ã£o de `cotacaoAtual` e `dataHoraCotacao`, sem setters genÃ©ricos e sem alterar campos cadastrais.
- [x] 2.2 Cobrir no domÃ­nio valor/timestamp vÃ¡lidos, preÃ§o igual com timestamp posterior e rejeiÃ§Ã£o das entradas invÃ¡lidas jÃ¡ definidas.
- [x] 2.3 Adicionar a `AcaoRepository` uma busca por ID com `PESSIMISTIC_WRITE`, preservando `findByTickerAndMercado(...)` e todos os mÃ©todos atuais.
- [x] 2.4 Criar ou adaptar `AcaoCotacaoPersistenceService` para reler sob lock e executar uma transaÃ§Ã£o curta sem chamada externa.
- [x] 2.5 Implementar na persistÃªncia a comparaÃ§Ã£o estrita de timestamps: atualizar o posterior e retornar sem update para igual ou anterior.
- [x] 2.6 Garantir que a persistÃªncia retorne o estado efetivamente persistido e trate ausÃªncia na releitura final sem estado parcial.

## 3. OrquestraÃ§Ã£o da atualizaÃ§Ã£o

- [x] 3.1 Implementar `AcaoService.atualizarCotacao(...)` localizando a AÃ§Ã£o antes de qualquer provider e mantendo o mÃ©todo fora da transaÃ§Ã£o de escrita.
- [x] 3.2 Selecionar exclusivamente BRAPI para mercado `BRASIL` e Alpha Vantage para mercado `EUA`, usando mercado e ticker persistidos.
- [x] 3.3 Reutilizar as validaÃ§Ãµes atuais de ticker, nome, mercado, moeda, preÃ§o positivo, precisÃ£o e payload externo sem duplicar regras.
- [x] 3.4 Aplicar o timestamp do provider e usar o `Clock` existente somente no fallback jÃ¡ aprovado pela spec.
- [x] 3.5 Delegar a persistÃªncia ao componente transacional e mapear o estado final retornado, inclusive para timestamp igual ou anterior.
- [x] 3.6 Verificar que somente `cotacaoAtual` e `dataHoraCotacao` mudam e que `id`, `ticker`, `nomeEmpresa`, `mercado` e `moeda` permanecem inalterados.

## 4. Contrato HTTP e erros

- [x] 4.1 Restaurar `PATCH /acoes/{id}/cotacao` em `AcaoResource`, sem DTO de entrada, retornando `200 OK` com `AcaoResponse` completo e sem `Location`.
- [x] 4.2 Rejeitar qualquer body nÃ£o vazio com `400 / REQUEST_INVALIDO`, preservando a aceitaÃ§Ã£o de request sem body ou com body vazio conforme o contrato.
- [x] 4.3 Adicionar/restaurar `TICKER_CANONICO_DIVERGENTE` no catÃ¡logo atual e mapear ticker canÃ´nico incompatÃ­vel para `409 Conflict`, sem migrar ticker ou persistir alteraÃ§Ãµes.
- [x] 4.4 Reutilizar os mapeamentos atuais para ticker inexistente, cotaÃ§Ã£o indisponÃ­vel, precisÃ£o invÃ¡lida, dados incompletos, rate limit, resposta invÃ¡lida, indisponibilidade e timeout.
- [x] 4.5 Enriquecer os erros previstos apÃ³s a localizaÃ§Ã£o com `acaoId`, `cotacaoPreservada=true`, `ultimaCotacaoValida` e `dataHoraUltimaCotacao`, preservando details e causa existentes.
- [x] 4.6 Confirmar que ID inexistente retorna `404 Not Found` e nÃ£o invoca nenhum provider.

## 5. Testes unitÃ¡rios e de serviÃ§o

- [x] 5.1 Testar a mutaÃ§Ã£o de domÃ­nio e a preservaÃ§Ã£o de todos os campos nÃ£o atualizÃ¡veis.
- [x] 5.2 Testar `AcaoService` para atualizaÃ§Ã£o BRASIL via BRAPI e EUA via Alpha Vantage.
- [x] 5.3 Testar no service ID inexistente sem chamada externa e seleÃ§Ã£o baseada apenas no mercado persistido.
- [x] 5.4 Testar atualizaÃ§Ã£o de preÃ§o e timestamp e preÃ§o igual com timestamp posterior.
- [x] 5.5 Testar timestamp igual e anterior com retorno do estado persistido sem update.
- [x] 5.6 Testar ticker canÃ´nico divergente com `409`, nenhuma mutaÃ§Ã£o e nenhuma migraÃ§Ã£o automÃ¡tica do ticker.
- [x] 5.7 Testar preservaÃ§Ã£o da Ãºltima cotaÃ§Ã£o e dos details exigidos em falhas posteriores Ã  localizaÃ§Ã£o.
- [x] 5.8 Testar timeout, rate limit, provider indisponÃ­vel, precisÃ£o invÃ¡lida, payload invÃ¡lido e demais erros externos normativos.

## 6. Testes HTTP

- [x] 6.1 Testar `PATCH` BRASIL e EUA com `200 OK`, `AcaoResponse` completo e ausÃªncia de `Location`.
- [x] 6.2 Testar request sem body e body vazio aceitos.
- [x] 6.3 Testar bodies nÃ£o vazios, inclusive JSON vÃ¡lido, JSON invÃ¡lido e conteÃºdo textual, com `400 / REQUEST_INVALIDO` e sem execuÃ§Ã£o da atualizaÃ§Ã£o.
- [x] 6.4 Testar `404` para AÃ§Ã£o inexistente e verificar ausÃªncia de interaÃ§Ã£o com providers.
- [x] 6.5 Testar respostas HTTP e payloads de erro para ticker canÃ´nico divergente e falhas externas com cotaÃ§Ã£o preservada.

## 7. Lock e concorrÃªncia

- [x] 7.1 Testar que `findByIdForUpdate(...)` usa lock `PESSIMISTIC_WRITE` e que os mÃ©todos existentes do repository continuam funcionais.
- [x] 7.2 Testar o componente de persistÃªncia para candidato posterior, igual e anterior, verificando quando hÃ¡ ou nÃ£o escrita.
- [x] 7.3 Criar teste concorrente coordenado para candidatos com timestamps diferentes e afirmar que o maior timestamp sempre prevalece.
- [x] 7.4 Verificar por teste/estrutura transacional que o lock nÃ£o Ã© mantido durante a chamada HTTP externa e que nÃ£o foi introduzido `@Version`.

## 8. RegressÃµes funcionais e estruturais

- [x] 8.1 Executar e corrigir somente regressÃµes relacionadas da suÃ­te completa de cadastro e consulta de AÃ§Ã£o.
- [x] 8.2 Executar a regressÃ£o completa de registro, consulta, replay cronolÃ³gico e validaÃ§Ãµes de OperaÃ§Ãµes.
- [x] 8.3 Executar a regressÃ£o completa de `CalculadoraPosicao`, `PosicaoService` e endpoint de posiÃ§Ã£o consolidada.
- [x] 8.4 Verificar que atualizaÃ§Ã£o de cotaÃ§Ã£o nÃ£o altera preÃ§os histÃ³ricos, preÃ§o mÃ©dio, custo da posiÃ§Ã£o nem comportamento de venda.
- [x] 8.5 Executar os testes de integraÃ§Ã£o com H2/Liquibase/Hibernate e confirmar `ddl-auto=validate` sem nova migration.
- [x] 8.6 Executar o build e a suÃ­te completa do projeto.

## 9. OpenSpec, Graphify e auditoria final

- [x] 9.1 Validar `restaurar-atualizacao-cotacao-acao` em modo strict e confirmar que `skip_specs: true` nÃ£o modifica a spec principal.
- [x] 9.2 Validar todo o conjunto OpenSpec em modo strict.
- [x] 9.3 Atualizar o Graphify apÃ³s a implementaÃ§Ã£o e consultar o grafo para confirmar os relacionamentos Resource â†’ Service â†’ Persistence/Repository e Service â†’ Provider.
- [x] 9.4 Auditar o diff final para confirmar ausÃªncia de alteraÃ§Ãµes em schema, Liquibase, dependÃªncias, OperaÃ§Ãµes, posiÃ§Ã£o consolidada, providers e arquivos acidentais do commit Ã³rfÃ£o.
- [x] 9.5 Auditar o histÃ³rico de comandos/diff para confirmar que nÃ£o houve cherry-pick, checkout/restauraÃ§Ã£o automÃ¡tica, reset, rebase, merge ou outra alteraÃ§Ã£o destrutiva do Git.
