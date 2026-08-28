## 1. Consulta read-only no service

- [x] 1.1 Adicionar ao `AcaoService` um método `@Transactional(readOnly = true)` que receba ticker e mercado e reutilize `TickerNormalizer.normalizeAndValidate` antes de qualquer acesso ao repository.
- [x] 1.2 Rejeitar mercado ausente com `400 / REQUEST_INVALIDO`, sem criar ErrorCode novo e sem consultar o repository.
- [x] 1.3 Consultar `AcaoRepository.findByTickerAndMercado` exatamente uma vez com ticker normalizado e mercado explicitamente informado, sem `exists`, busca somente por ticker ou query nova.
- [x] 1.4 Lançar `ObjectNotFoundException` no padrão vigente quando a combinação válida não existir e mapear o registro encontrado com `AcaoMapper` para `AcaoResponse` completo.
- [x] 1.5 Confirmar que o novo método não escreve, não usa locks, `Clock`, serviços de persistência, providers, adapters nem atualiza campos da Ação.

## 2. Endpoint REST dedicado

- [x] 2.1 Adicionar exclusivamente `GET /acoes/por-ticker?ticker={ticker}&mercado={mercado}` ao `AcaoResource`, delegando ao novo método do service e retornando `200 OK` com `AcaoResponse`.
- [x] 2.2 Configurar o binding local de `ticker` e `mercado` para permitir que parâmetros ausentes sejam tratados pela política aprovada, embora ambos permaneçam obrigatórios no contrato público.
- [x] 2.3 Garantir `400 / TICKER_INVALIDO` para ticker ausente ou inválido e `400 / REQUEST_INVALIDO` para mercado ausente, sem handler MVC global novo.
- [x] 2.4 Preservar `400 / REQUEST_INVALIDO` para mercado textual desconhecido pelo tratamento centralizado de conversão do enum.
- [x] 2.5 Preservar sem alteração `POST /acoes`, `GET /acoes`, `GET /acoes/{id}` e `PATCH /acoes/{id}/cotacao`, sem expor os aliases rejeitados.

## 3. Testes de repository e identidade composta

- [x] 3.1 Ampliar `AcaoRepositoryTest` para confirmar `findByTickerAndMercado` encontrando combinações válidas em `BRASIL` e `EUA`.
- [x] 3.2 Cobrir retorno vazio para combinação válida inexistente.
- [x] 3.3 Persistir conceitualmente o mesmo ticker em mercados diferentes e confirmar que cada consulta retorna exclusivamente a Ação do mercado solicitado.
- [x] 3.4 Confirmar que `uk_acao_ticker_mercado` continua garantindo a unicidade composta e que nenhuma migration, constraint, índice ou query customizada foi adicionada.

## 4. Testes do service e da validação

- [x] 4.1 Testar ticker já normalizado em `BRASIL` e `EUA`, verificando `AcaoResponse` completo e uma única consulta ao repository.
- [x] 4.2 Testar lowercase e espaços nas extremidades, confirmando uppercase, trim e argumento normalizado enviado ao repository.
- [x] 4.3 Testar o mesmo ticker em mercados diferentes e confirmar seleção exclusiva pelo mercado informado, sem preferência implícita.
- [x] 4.4 Testar ticker nulo, vazio, branco e acima de 30 caracteres com `400 / TICKER_INVALIDO` antes do repository.
- [x] 4.5 Testar mercado nulo com `400 / REQUEST_INVALIDO` antes do repository.
- [x] 4.6 Testar combinação válida inexistente com `ObjectNotFoundException` e mensagem coerente com o tratamento centralizado atual.
- [x] 4.7 Verificar por interações e reflexão que o método é read-only, usa isolamento padrão, não possui lock e não aciona providers, `AcaoPersistenceService`, `AcaoCotacaoPersistenceService`, escrita ou `Clock`.

## 5. Testes do resource e arquitetura

- [x] 5.1 Ampliar `AcaoResourceTest` para cobrir `200 OK` em `BRASIL` e `EUA`, incluindo ticker lowercase/espaçado e o payload completo de `AcaoResponse`.
- [x] 5.2 Cobrir ticker ausente e inválido com `400 / TICKER_INVALIDO` no `StandardError` vigente.
- [x] 5.3 Cobrir mercado ausente e textual desconhecido com `400 / REQUEST_INVALIDO` no tratamento centralizado vigente.
- [x] 5.4 Cobrir `404 Not Found` para combinação válida inexistente e confirmar que ticker e mercado chegam corretamente ao service.
- [x] 5.5 Confirmar que `GET /acoes` permanece uma listagem, `GET /acoes/{id}` permanece funcional e os três aliases rejeitados não são contratos válidos.
- [x] 5.6 Adicionar inspeção arquitetural compatível com o padrão do projeto para confirmar o fluxo `AcaoResource → AcaoService → TickerNormalizer → AcaoRepository → AcaoMapper` e a ausência de providers, escritas e locks no novo caso de uso.

## 6. Regressão e validações finais

- [x] 6.1 Executar testes direcionados de `TickerNormalizer`, `AcaoRepository`, `AcaoService` e `AcaoResource`, registrando total, failures, errors e skipped.
- [x] 6.2 Executar regressões de cadastro, listagem, consulta por ID, atualização e histórico de cotação, além das funcionalidades relacionadas do backend.
- [x] 6.3 Executar `./mvnw.cmd -q test` e `./mvnw.cmd -q clean verify`, confirmando H2, Liquibase 001–006 e Hibernate `ddl-auto=validate` sem migration nova.
- [x] 6.4 Executar `openspec validate consulta-acao-por-ticker-mercado --strict`, `openspec validate --all --strict` e atualizar/consultar o Graphify para confirmar o fluxo local.
- [x] 6.5 Executar `git diff --check`, revisar `git diff` e registrar `git status`, confirmando ausência de alterações fora do escopo e sem operações de histórico Git.
