## 1. Contrato e restrições

- [x] 1.1 Adicionar somente o código de erro necessário para ticker canônico divergente, preservando os demais status, códigos e o formato `StandardError` existentes.
- [x] 1.2 Confirmar por revisão que não serão alterados schema, changelogs Liquibase, dependências, configurações, enums ou contratos de `CotacaoProvider` e dos adapters.

## 2. Domínio e persistência final

- [x] 2.1 Adicionar a `Acao` um método de domínio restrito que altere somente `cotacaoAtual` e `dataHoraCotacao`, sem introduzir setters genéricos nem permitir valores nulos ou não positivos.
- [x] 2.2 Adicionar a `AcaoRepository` uma leitura por ID com lock pessimista de escrita dedicada ao fluxo de atualização, sem alterar o comportamento de `findById` usado pelos GETs.
- [x] 2.3 Criar `AcaoCotacaoPersistenceService` com transação curta para reler a Ação sob lock, tratar desaparecimento concorrente e comparar o timestamp candidato com o persistido.
- [x] 2.4 Persistir e devolver a entidade atualizada somente quando o timestamp candidato for estritamente posterior; para timestamp igual ou anterior, devolver a entidade atual sem executar update.
- [x] 2.5 Garantir que a persistência final preserve ID, ticker, nome da empresa, mercado e moeda e não crie registros ou estruturas de histórico.
- [x] 2.6 Manter `AcaoPersistenceService` e seus testes restritos ao cadastro existente.

## 3. Orquestração da atualização

- [x] 3.1 Adicionar `AcaoCotacaoPersistenceService` às dependências de `AcaoService` sem alterar o comportamento de cadastro, listagem ou consulta por ID.
- [x] 3.2 Implementar `AcaoService.atualizarCotacao(Long id)` sem transação abrangente, localizando primeiro a Ação e falhando com `ObjectNotFoundException` antes de qualquer provider quando o ID não existir.
- [x] 3.3 Selecionar o `CotacaoProvider` pelo mercado persistido e consultar exclusivamente com o ticker persistido: BRAPI para `BRASIL` e Alpha Vantage para `EUA`.
- [x] 3.4 Manter a chamada `CotacaoProvider.consultar` e todas as validações externas fora da transação de escrita e do lock pessimista.
- [x] 3.5 Reutilizar as regras existentes para validar ticker retornado, nome utilizável, moeda compatível, cotação positiva e representação exata em `NUMERIC(19,6)`, evitando duplicação desnecessária.
- [x] 3.6 Preservar `nomeEmpresa`, ticker, mercado e moeda mesmo quando os metadados retornados pelo provider forem diferentes.
- [x] 3.7 Tratar ticker retornado incompatível sem renomeação explícita como resposta externa inválida e ticker canônico divergente indicado pelo provider como `409/TICKER_CANONICO_DIVERGENTE`, com os dois tickers em `details`.
- [x] 3.8 Normalizar timestamp confiável do provider para UTC e usar como fallback o instante UTC do `Clock` em que a aplicação obteve a cotação.
- [x] 3.9 Delegar somente a aplicação final validada ao serviço transacional e mapear a entidade efetivamente persistida para `AcaoResponse`.
- [x] 3.10 Enriquecer falhas posteriores à leitura da Ação com `acaoId`, `cotacaoPreservada`, `ultimaCotacaoValida` e `dataHoraUltimaCotacao`, preservando o status, código e mensagem originais.

## 4. Endpoint REST

- [x] 4.1 Adicionar `PATCH /acoes/{id}/cotacao` ao `AcaoResource` sem aceitar DTO ou valor de cotação definido pelo cliente.
- [x] 4.2 Rejeitar qualquer corpo enviado ao endpoint com `400/REQUEST_INVALIDO` antes de chamar `AcaoService`.
- [x] 4.3 Responder `200 OK`, sem `Location`, com `AcaoResponse` completo tanto para atualização aplicada quanto para resultado temporalmente não mais novo que preserve o estado atual.
- [x] 4.4 Reutilizar `ResourceExceptionHandler` para Ação inexistente e falhas de provider, incluindo `code` e `details` quando aplicável.
- [x] 4.5 Confirmar que `POST /acoes`, `GET /acoes` e `GET /acoes/{id}` permanecem comportamentalmente inalterados e que nenhum outro endpoint foi adicionado.

## 5. Testes unitários de service e domínio

- [x] 5.1 Testar atualização BRASIL com seleção exclusiva da BRAPI, ticker persistido, moeda BRL, timestamp do provider em UTC e preservação dos demais campos.
- [x] 5.2 Testar atualização EUA com seleção exclusiva da Alpha Vantage, moeda USD, último preço disponível e fallback do `Clock` em UTC.
- [x] 5.3 Testar ID inexistente, confirmando `ObjectNotFoundException` e ausência de chamadas aos dois providers e à persistência final.
- [x] 5.4 Testar ticker não encontrado, provider ausente/não configurado, rate limit, indisponibilidade, timeout e resposta inválida ou incompleta, confirmando a preservação da última cotação nos `details` e ausência de escrita.
- [x] 5.5 Testar cotação ausente, zero, negativa e fora da precisão, sem truncamento, arredondamento ou persistência.
- [x] 5.6 Testar nome externo diferente e confirmar que `nomeEmpresa`, ticker, mercado e moeda persistidos não são alterados.
- [x] 5.7 Testar ticker canônico igual e divergente indicado pelo provider, incluindo status, código, `details` e ausência de mutação no conflito.
- [x] 5.8 Testar preço igual com timestamp posterior, timestamp igual e timestamp anterior, verificando atualização apenas da observação temporalmente nova.
- [x] 5.9 Testar o método de domínio para alteração restrita e rejeição de cotação nula, zero ou negativa.

## 6. Testes de persistência, concorrência e HTTP

- [x] 6.1 Testar `AcaoCotacaoPersistenceService` para atualização aplicada, candidata igual/antiga ignorada e Ação removida entre leitura externa e persistência final.
- [x] 6.2 Ampliar `AcaoRepositoryTest` sobre H2 para validar leitura com lock, alteração somente de cotação/timestamp e preservação dos demais campos com Liquibase e Hibernate existentes.
- [x] 6.3 Testar duas persistências concorrentes da mesma Ação com timestamps diferentes, confirmando que o estado final mantém a observação mais recente.
- [x] 6.4 Testar `PATCH /acoes/{id}/cotacao` sem corpo para BRASIL e EUA, verificando `200 OK`, ausência de `Location` e todos os campos do `AcaoResponse`.
- [x] 6.5 Testar corpo manual rejeitado com `400/REQUEST_INVALIDO` e confirmar que o service não é chamado.
- [x] 6.6 Testar pelo endpoint Ação inexistente, ticker inexistente, ticker canônico divergente e os erros externos padronizados com `details` de cotação preservada.
- [x] 6.7 Preservar e reexecutar todos os testes atuais de cadastro, listagem, consulta, repository e adapters, sem chamadas reais à BRAPI ou Alpha Vantage.
- [x] 6.8 Quando o ambiente PostgreSQL de desenvolvimento estiver disponível, validar o lock e a regra temporal concorrente; se estiver indisponível, registrar a pendência sem inventar configuração nem marcar esta tarefa como concluída.

## 7. Verificações finais

- [x] 7.1 Executar pelo Maven Wrapper os testes direcionados de `AcaoService`, `AcaoCotacaoPersistenceService`, `AcaoRepository` e `AcaoResource`.
- [x] 7.2 Executar a suíte completa pelo Maven Wrapper e confirmar o carregamento do contexto com Liquibase e Hibernate sobre H2.
- [x] 7.3 Executar `clean verify` pelo Maven Wrapper e registrar o resultado do build e dos testes.
- [x] 7.4 Validar a change `atualizacao-cotacao` com OpenSpec em modo strict e reconciliar qualquer divergência entre proposal, spec, design, tasks e implementação.
- [x] 7.5 Atualizar o Graphify após as alterações de código e consultar o grafo para confirmar as novas relações do endpoint, service, persistência, repository, entidade e providers.
- [x] 7.6 Executar `git diff --check`, revisar integralmente `git diff` e registrar `git status` sem realizar commit, push, merge, rebase ou alteração do histórico Git.
- [x] 7.7 Confirmar por revisão final que não foram alterados schema, Liquibase, dependências, configurações, cadastro, GETs ou adapters e que não foram implementados histórico, agendamento, lote, Carteira, Operação, cálculos financeiros ou frontend.
