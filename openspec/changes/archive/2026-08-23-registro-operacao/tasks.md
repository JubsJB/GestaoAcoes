## 1. Schema, domínio e repositories

- [x] 1.1 Criar `004-create-operacao.yaml` com somente a tabela `operacao` e os tipos aprovados, sem modificar os changeSets 001–003.
- [x] 1.2 Adicionar PK, FKs sem cascade, checks aprovados, unique de ordem cronológica, índices estritamente necessários e rollback explícito compatíveis com PostgreSQL e H2.
- [x] 1.3 Incluir o changeSet 004 ao final do changelog master, preservando dependências, configurações e `spring.jpa.hibernate.ddl-auto=validate`.
- [x] 1.4 Criar `TipoOperacao` e a entidade unidirecional `Operacao`, com Carteira e Ação obrigatórias, Corretora opcional e nenhum campo financeiro consolidado.
- [x] 1.5 Adicionar a `AcaoRepository` a leitura por ticker normalizado + `Mercado` e criar em `OperacaoRepository` as consultas cronológicas e de existência necessárias.
- [x] 1.6 Adicionar a leitura bloqueante da Carteira necessária para serializar criação de Operação e exclusão, sem `@Version`, tabela de lock ou abstração concorrente adicional.

## 2. Contrato HTTP e mapeamento

- [x] 2.1 Criar `OperacaoCreateRequest` e `OperacaoResponse` com apenas os campos definidos, mantendo `corretoraId` anulável e excluindo `acaoId`, `valorTotal` de entrada e cotações.
- [x] 2.2 Configurar Bean Validation e desserialização restrita para obrigatoriedade, enum, positividade, precisão, data, ordem e rejeição de campos desconhecidos/controlados.
- [x] 2.3 Criar `OperacaoMapper` somente para projetar a entidade persistida no response, sem cálculos financeiros nem consultas externas.
- [x] 2.4 Criar exclusivamente `POST /operacoes` em `OperacaoResource`, retornando `201 Created`, DTO completo e `Location: /operacoes/{id}`.

## 3. Serviço e regras de registro

- [x] 3.1 Validar `quantidade` com `BigDecimal`/`NUMERIC(19,6)`, exigindo valor matematicamente inteiro em `BRASIL` e permitindo até seis casas decimais em `EUA`, sempre sem arredondar ou truncar.
- [x] 3.2 Validar `precoUnitario` positivo e exato, calcular `valorTotal = quantidade × precoUnitario` em `NUMERIC(38,12)` e rejeitar qualquer resultado não representável antes da persistência.
- [x] 3.3 Validar `dataOperacao` com o `Clock` injetado e a zona do mercado — `America/Sao_Paulo` ou `America/New_York` — aceitando passado/dia civil atual e rejeitando futuro.
- [x] 3.4 No `OperacaoService`, localizar a Carteira bloqueada, a Ação por `TickerNormalizer` + mercado e a Corretora somente quando informada, reutilizando `ObjectNotFoundException`.
- [x] 3.5 Implementar o registro de COMPRA com valor total calculado e sem persistir posição, preço médio, custo, resultado, patrimônio ou snapshot.
- [x] 3.6 Implementar o replay ordenado de todas as Operações da mesma Carteira e Ação, incluindo a candidata, para validar VENDA atual ou retroativa em todos os prefixos.
- [x] 3.7 Rejeitar ordem cronológica duplicada e candidata que produza saldo negativo em qualquer ponto, preservando integralmente o histórico já persistido.
- [x] 3.8 Executar localização, replay, validação e `saveAndFlush` numa única transação curta de escrita protegida pelo lock pessimista da Carteira.
- [x] 3.9 Garantir que o registro não cadastre Ação, não altere Carteira/Ação/Corretora e não chame BRAPI, Alpha Vantage, BrasilAPI, ViaCEP ou provider histórico.

## 4. Erros e proteção do histórico

- [x] 4.1 Adicionar somente os códigos `ORDEM_OPERACAO_DUPLICADA`, `POSICAO_INSUFICIENTE` e `CARTEIRA_POSSUI_OPERACOES`, integrando-os a `ApiException`, `StandardError` e `ResourceExceptionHandler` existentes.
- [x] 4.2 Atualizar `CarteiraService.excluir` para adquirir o mesmo lock pessimista curto e consultar `OperacaoRepository.existsByCarteiraId` antes da exclusão física.
- [x] 4.3 Preservar `204` para Carteira sem Operações e `404` para ID inexistente, retornando `409/CARTEIRA_POSSUI_OPERACOES` sem alteração de estado quando houver histórico.
- [x] 4.4 Garantir que as FKs de Carteira, Ação e Corretora nunca apaguem Operações por cascade e que violações conhecidas sejam traduzidas deterministicamente.

## 5. Testes automatizados

- [x] 5.1 Testar DTOs, mapper, enum e contrato restrito, incluindo campos obrigatórios, desconhecidos, controlados e tipo inválido.
- [x] 5.2 Testar Carteira existente/inexistente, Ação BRASIL/EUA por ticker normalizado, ausência de cadastro automático e Corretora existente, ausente ou inexistente.
- [x] 5.3 Testar em `BRASIL` quantidade inteira, representação inteira com zeros decimais, fração rejeitada, zero, negativo e precisão excedida.
- [x] 5.4 Testar em `EUA` quantidade inteira, fração com até seis casas, escala excedida, zero, negativo e precisão excedida.
- [x] 5.5 Testar `precoUnitario` válido, zero, negativo ou fora dos limites e `valorTotal` calculado exatamente sem aceitar entrada do cliente.
- [x] 5.6 Testar datas passadas, data civil atual e futura nos limites de `America/Sao_Paulo` e `America/New_York`, usando `Clock` controlado sem fabricar horário persistido.
- [x] 5.7 Testar que `precoUnitario` é preservado, que `Acao.cotacaoAtual` ou referência histórica não o substituem, que o valor total usa apenas o preço real e que nenhum provider é chamado.
- [x] 5.8 Testar COMPRA inicial e múltiplas COMPRAS sem criação de posição ou alteração das entidades relacionadas.
- [x] 5.9 Testar VENDA válida, exatamente igual à posição, superior à posição e precedida por múltiplas Operações.
- [x] 5.10 Testar cronologia por data e `ordemNoDia`, ordem duplicada, operação retroativa válida e candidata retroativa que invalidaria venda posterior.
- [x] 5.11 Testar isolamento do saldo por Carteira e por Ação, sem aproveitar Operações de outro agrupamento.
- [x] 5.12 Testar `POST /operacoes` válido com Ação brasileira e americana, com e sem Corretora, verificando `201`, DTO, `Location` e ausência de campos proibidos.
- [x] 5.13 Testar respostas padronizadas `400`, `404` e `409`, detalhes relevantes e ausência de persistência parcial em cada falha.
- [x] 5.14 Testar `OperacaoRepository` no H2: relacionamentos, Corretora nullable, enum textual, escalas, FKs, checks, unique, ordenação e schema criado pelo Liquibase.
- [x] 5.15 Testar rollback/atomicidade e concorrência de duas VENDAS que excedem juntas a posição disponível.
- [x] 5.16 Testar DELETE de Carteira sem Operações, com Operações, ID inexistente, ausência de cascade e disputa concorrente entre criação e exclusão.
- [x] 5.17 Executar e preservar os testes existentes de Carteira, Ação e Corretora, inclusive POST/GET/PATCH/DELETE e inicialização Liquibase/Hibernate.

## 6. Verificação e encerramento técnico

- [x] 6.1 Executar os testes direcionados de `Operacao`, `Carteira`, `Acao` e `Corretora` e corrigir somente falhas dentro do escopo definido.
- [x] 6.2 Executar a suíte completa do projeto e confirmar ausência de regressões.
- [x] 6.3 Executar `mvnw.cmd clean verify` e registrar o resultado.
- [x] 6.4 Confirmar pelo pipeline de testes que Liquibase e Hibernate validam o novo schema no H2 e revisar a portabilidade declarada para PostgreSQL.
- [x] 6.5 Executar `openspec validate registro-operacao --strict` e a validação global em modo strict.
- [x] 6.6 Atualizar o Graphify após as alterações de código e consultar o grafo para conferir os relacionamentos implementados.
- [x] 6.7 Executar `git diff --check`, revisar todo o diff e o status, sem commit, push, pull, merge, rebase ou arquivamento.
- [x] 6.8 Fazer auditoria final de escopo, confirmando ausência de posição persistida, cálculos consolidados, consulta/persistência histórica, endpoints adicionais e alterações fora da change.
