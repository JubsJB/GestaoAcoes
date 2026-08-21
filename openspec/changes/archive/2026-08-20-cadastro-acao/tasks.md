## 1. Decisões e reconciliação do planejamento

- [x] 1.1 Registrar a confirmação das doze decisões sobre contrato HTTP, normalização, ticker canônico, mercado/moeda, cotação, frescor americano, tempo, fluxo Alpha Vantage, precisão, tamanhos, origem e configuração externa.
- [x] 1.2 Reconciliar `proposal.md`, `specs/stock-registration/spec.md`, `design.md` e este `tasks.md` com as decisões aprovadas antes de qualquer alteração de código.

## 2. Liquibase, modelo e persistência

- [x] 2.1 Criar `changes/002-create-acao.yaml` somente com a tabela `acao`, `ticker VARCHAR(30)`, `nome_empresa VARCHAR(255)`, `cotacao_atual NUMERIC(19,6)`, demais colunas aprovadas, checks, unicidade `(ticker, mercado)` e rollback explícito da tabela.
- [x] 2.2 Incluir `002-create-acao.yaml` depois de `001-create-corretora.yaml` no changelog master, sem alterar o changeSet já aplicado de Corretora.
- [x] 2.3 Criar os enums textuais `Mercado {BRASIL, EUA}` e criar a entidade `Acao` com `BigDecimal`, `OffsetDateTime`, tamanhos, nulabilidades e precisão definidos no design.
- [x] 2.4 Criar `AcaoRepository` com operações derivadas do Spring Data para verificar existência pelo par ticker/mercado, sem consulta customizada quando desnecessária.
- [x] 2.5 Criar `AcaoResponse` e `AcaoMapper`, mantendo DTO e entidade separados e expondo somente os campos da primeira fatia.
- [x] 2.6 Criar teste de integração que execute o changelog master no H2, confirme a tabela e constraints de Ação e valide o mapeamento Hibernate com `ddl-auto=validate`.

## 3. Entrada, normalização e contratos internos

- [x] 3.1 Criar `AcaoCreateRequest` aceitando somente ticker e mercado, com validação de obrigatoriedade e rejeição de propriedades não permitidas.
- [x] 3.2 Implementar normalização de ticker exclusivamente com `trim` e uppercase independente de locale, preservando pontuação, sufixos e demais caracteres, e garantir que entrada local inválida não acione provider nem gravação.
- [x] 3.3 Criar a abstração `CotacaoProvider` e seu modelo interno mínimo para ticker confirmado, nome da empresa, moeda externa, cotação e timestamp opcional da cotação, sem vazar DTOs dos provedores.
- [x] 3.4 Implementar seleção determinística de provider por `Mercado` sem criar framework genérico de ativos ou suporte a mercados fora do PRD.

## 4. Configuração segura das integrações

- [x] 4.1 Adicionar `BRAPI_API_KEY`, `ALPHA_VANTAGE_API_KEY` e propriedades externas correspondentes de URL base, connect timeout e read timeout, sem segredos ou chaves padrão válidas.
- [x] 4.2 Documentar em `.env.example` somente os nomes das novas variáveis e valores claramente substituíveis, sem alterar ou expor credenciais reais.
- [x] 4.3 Ampliar `ExternalApiConfig` com `RestClient` síncrono qualificado para cada novo provider, reutilizando a fábrica e sem adicionar WebFlux, OpenFeign ou SDK externo.
- [x] 4.4 Validar a configuração somente quando o provider for utilizado, garantindo que BRAPI ausente não bloqueie EUA, Alpha Vantage ausente não bloqueie BRASIL e nenhuma chave apareça em logs, exceções, `details` ou fixtures.

## 5. Adapter da BRAPI

- [x] 5.1 Implementar `BrapiAdapter` para consultar um ticker brasileiro, autenticar por Bearer e mapear símbolo, nome, moeda, `regularMarketPrice` e `regularMarketTime` utilizável para o modelo interno.
- [x] 5.2 Tratar resultado vazio como ticker inexistente e payload sem nome, moeda ou cotação utilizável como dados incompletos ou cotação indisponível, conforme a spec.
- [x] 5.3 Persistir o ticker canônico retornado para `changed=true`, revalidando duplicidade sem fabricar ou alterar sufixos localmente.
- [x] 5.4 Mapear status e payloads de limite, resposta inválida, indisponibilidade e timeout pelo padrão atual de erros externos.

## 6. Adapter da Alpha Vantage

- [x] 6.1 Implementar `AlphaVantageAdapter` com `SYMBOL_SEARCH`, exigindo correspondência exata do ticker normalizado, região dos Estados Unidos e moeda compatível, aproveitando o nome quando utilizável.
- [x] 6.2 Interromper o fluxo americano sem chamar `OVERVIEW` nem `GLOBAL_QUOTE` quando a busca não confirmar exatamente o ticker no mercado dos EUA.
- [x] 6.3 Chamar `OVERVIEW` somente quando `SYMBOL_SEARCH` não fornecer nome utilizável e depois consultar `GLOBAL_QUOTE`, consolidando nome, símbolo, última cotação disponível e timestamp opcional sem chamadas adicionais desnecessárias.
- [x] 6.4 Detectar payloads `Note`, `Information`, `Error Message`, objetos vazios e formatos incompatíveis, diferenciando limite, ticker inexistente e resposta inválida sem repassar conteúdo externo não confiável.
- [x] 6.5 Mapear indisponibilidade e timeout e garantir que a `apikey` seja enviada somente conforme o contrato do provider e nunca seja exposta pela aplicação.

## 7. Caso de uso, transação e endpoint

- [x] 7.1 Implementar em `AcaoService` a normalização, verificação antecipada de duplicidade, seleção exclusiva do provider, mapeamento mercado/moeda e validação dos dados externos.
- [x] 7.2 Validar ticker confirmado/canônico, nome e cotação presente e positiva, exigindo representação exata em `NUMERIC(19,6)` e retornando `COTACAO_FORA_DA_PRECISAO` sem truncamento ou arredondamento.
- [x] 7.3 Definir `dataHoraCotacao` pelo timestamp confiável do provider normalizado para UTC ou, quando indisponível, pelo instante UTC do relógio da aplicação, sem fabricar horário nem criar histórico.
- [x] 7.4 Implementar `AcaoPersistenceService` com transação restrita à verificação final do ticker canônico e à gravação, mantendo toda chamada HTTP fora da transação.
- [x] 7.5 Traduzir antecipadamente e sob concorrência a duplicidade `(ticker, mercado)` para `409/ACAO_DUPLICADA` sem alterar o erro de unicidade de Corretora.
- [x] 7.6 Ampliar `ErrorCodes`, `ExternalApiErrorMapper` e o tratamento centralizado apenas no necessário para ticker, Ação e payloads dos novos providers, preservando `code` e `details` opcionais.
- [x] 7.7 Implementar `POST /acoes` com `201 Created`, `AcaoResponse` completo e `Location: /acoes/{id}`, sem adicionar GET, PUT, PATCH ou DELETE.

## 8. Testes automatizados

- [x] 8.1 Criar testes unitários da normalização para espaços, caixa, entrada vazia, preservação de caracteres internos e ausência de chamadas externas em entrada inválida.
- [x] 8.2 Criar testes do service para BRASIL, moeda BRL, ticker canônico, timestamp confiável da BRAPI normalizado para UTC e fallback por relógio fixo quando o horário não for utilizável.
- [x] 8.3 Criar testes do service para EUA, moeda USD, correspondência exata, `OVERVIEW` condicional, último fechamento aceito sem garantia de tempo real e fallback UTC quando não houver timestamp confiável.
- [x] 8.4 Criar testes do service para ticker inexistente, resultado aproximado, moeda incompatível, nome ausente, cotação ausente/não numérica/não positiva, precisão não representável e configuração independente dos providers.
- [x] 8.5 Criar testes de duplicidade antecipada, duplicidade do ticker canônico e violação concorrente da constraint, confirmando que no máximo um registro é criado.
- [x] 8.6 Criar testes HTTP simulados do `BrapiAdapter` para sucesso com timestamp, timestamp inválido, renomeação, inexistência, dados incompletos, limite, resposta inválida, indisponibilidade e timeout.
- [x] 8.7 Criar testes HTTP simulados do `AlphaVantageAdapter` para busca exata/aproximada, mercado incorreto, nome presente, `OVERVIEW` necessário, ausência de chamadas extras, último fechamento, payloads de erro, indisponibilidade e timeout.
- [x] 8.8 Criar testes de repository no H2 para persistência completa, enums textuais, `NUMERIC(19,6)`, `OffsetDateTime` UTC, checks e unicidade composta, sem depender do banco para arredondar.
- [x] 8.9 Criar testes de `POST /acoes` para request válido de cada mercado, propriedades proibidas, erros padronizados e resposta de sucesso aprovada com DTO e `Location`.
- [x] 8.10 Executar e preservar todos os testes existentes de Corretora, integrações, Liquibase e contexto, confirmando que nenhum provider de mercado é chamado por funcionalidades anteriores.

## 9. Verificação e consistência final

- [x] 9.1 Executar os testes direcionados de Ação pelo Maven Wrapper e registrar o resultado.
- [x] 9.2 Executar a suíte completa e o build pelo Maven Wrapper, confirmando compilação e ausência de chamadas reais à BRAPI, Alpha Vantage, BrasilAPI, ViaCEP ou PostgreSQL nos testes.
- [x] 9.3 Quando PostgreSQL e credenciais externas estiverem disponíveis, iniciar o profile `dev`, aplicar `002-create-acao.yaml` e confirmar Liquibase seguido de Hibernate `validate`; deixar a tarefa pendente e documentar o motivo se o ambiente não estiver disponível.
- [x] 9.4 Revisar o changelog, dependências e configurações para confirmar ausência de tabelas fora do escopo, credenciais versionadas e `ddl-auto=create`, `update` ou `create-drop`.
- [x] 9.5 Validar `cadastro-acao` com OpenSpec em modo strict e reconciliar qualquer divergência entre proposal, spec, design, tasks e implementação.
- [x] 9.6 Atualizar o Graphify após as futuras alterações de código e confirmar que o grafo identifica Ação, enums, resource, services, repository, mapper, providers, adapters, Liquibase e testes.
- [x] 9.7 Executar `git diff --check`, revisar `git diff` e `git status` e confirmar que não houve commit, push, merge, rebase nem alteração do histórico.
- [x] 9.8 Confirmar por revisão final que não foram implementados listagem, consulta individual, atualização, histórico de cotação, Carteira, Operação, preço médio, rentabilidade, patrimônio ou frontend.
