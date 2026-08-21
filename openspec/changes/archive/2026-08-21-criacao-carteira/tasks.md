## 1. Schema e modelo de domínio

- [x] 1.1 Criar `003-create-carteira.yaml` somente com a tabela `carteira`, `id`, `nome`, `data_criacao`, `VARCHAR(255)` e as constraints `NOT NULL` definidas, sem unicidade de nome e com rollback explícito da nova tabela.
- [x] 1.2 Incluir `003-create-carteira.yaml` ao final de `db.changelog-master.yaml` sem modificar os changeSets 001 e 002.
- [x] 1.3 Criar a entidade `Carteira` com `Long id`, `String nome` e `OffsetDateTime dataCriacao`, mapeada exatamente para o novo schema.
- [x] 1.4 Confirmar que a entidade não contém associações com usuário, ação, corretora, operação, posição, snapshot ou histórico.

## 2. Contratos HTTP e mapeamento

- [x] 2.1 Criar `CarteiraCreateRequest` aceitando somente `nome`, obrigatório e limitado a 255 caracteres após o `trim`, com rejeição de propriedades desconhecidas.
- [x] 2.2 Criar `CarteiraResponse` com `id`, `nome` e `dataCriacao`, sem expor a entidade JPA.
- [x] 2.3 Criar `CarteiraMapper` para converter a entidade persistida no DTO completo de resposta.

## 3. Persistência e serviço

- [x] 3.1 Criar `CarteiraRepository` estendendo somente o repository Spring Data necessário, sem consulta de duplicidade por nome nem consultas customizadas não justificadas.
- [x] 3.2 Criar `CarteiraService` com transação curta para aplicar somente `trim` às extremidades do nome, preservar conteúdo interno, acentos e caixa, gerar `dataCriacao` pelo `Clock` existente em UTC, persistir e mapear a resposta.
- [x] 3.3 Confirmar que nomes duplicados são persistidos com IDs distintos e que não foi criado `CarteiraPersistenceService`, provider externo ou outra abstração sem responsabilidade necessária nesta fatia.
- [x] 3.4 Reutilizar `ApiException`, `ErrorCodes`, `StandardError` e `ResourceExceptionHandler`; ajustar o tratamento existente somente se necessário para impedir classificação incorreta de erro de Carteira como duplicidade de Corretora.

## 4. Endpoint de criação

- [x] 4.1 Criar `CarteiraResource` em `/carteiras` com somente `POST /carteiras` nesta change.
- [x] 4.2 Validar o request, delegar ao service e devolver `201 Created`, `CarteiraResponse` completo e `Location: /carteiras/{id}`.
- [x] 4.3 Confirmar que nenhum GET, PUT, PATCH, DELETE, operação, posição ou cálculo financeiro foi introduzido.

## 5. Testes unitários

- [x] 5.1 Testar no `CarteiraService` a persistência do nome após `trim` e a geração determinística de `dataCriacao` em UTC com `Clock` fixo.
- [x] 5.2 Testar nome ausente, nulo, vazio, somente espaços e acima de 255 caracteres após o `trim`.
- [x] 5.3 Testar a preservação de espaços internos, acentos e caixa no nome.
- [x] 5.4 Testar a criação de duas Carteiras com o mesmo nome e IDs independentes.
- [x] 5.5 Testar `CarteiraMapper` e a preservação exata de `id`, `nome` e `dataCriacao` no response.

## 6. Testes de persistência e API

- [x] 6.1 Criar teste de `CarteiraRepository` com H2 para executar o changelog real, validar o mapeamento Hibernate e persistir os três campos.
- [x] 6.2 Testar no H2 as constraints `NOT NULL` de `nome` e `data_criacao`, o limite `VARCHAR(255)` e a ausência de unicidade de nome.
- [x] 6.3 Testar `POST /carteiras` válido, `201 Created`, o DTO completo, a data UTC e `Location: /carteiras/{id}`.
- [x] 6.4 Testar a rejeição de `id`, `dataCriacao` e propriedades desconhecidas com `400/REQUEST_INVALIDO` e ausência de persistência.
- [x] 6.5 Testar nome ausente, nulo, vazio, somente espaços e acima do limite com `400/REQUEST_INVALIDO` e `details` por campo quando aplicáveis.
- [x] 6.6 Confirmar pelo teste de contexto que Liquibase cria `carteira` antes de o Hibernate validar o schema com `ddl-auto=validate`.
- [x] 6.7 Preservar e reexecutar todos os testes existentes de infraestrutura, Corretora e Ação.

## 7. Verificação final

- [x] 7.1 Executar os testes direcionados de Carteira pelo Maven Wrapper.
- [x] 7.2 Executar a suíte completa pelo Maven Wrapper.
- [x] 7.3 Executar `./mvnw clean verify` ou `./mvnw.cmd clean verify`, conforme o ambiente, e confirmar build bem-sucedido.
- [x] 7.4 Confirmar na suíte que o mesmo changelog Liquibase e o Hibernate com `ddl-auto=validate` funcionam no H2.
- [x] 7.5 Quando um PostgreSQL de desenvolvimento corretamente configurado estiver disponível, validar a aplicação do changeSet e o carregamento do contexto sem inventar credenciais; deixar esta tarefa pendente se o ambiente não estiver disponível.
- [x] 7.6 Validar a change `criacao-carteira` com OpenSpec em modo strict.
- [x] 7.7 Atualizar o Graphify após as alterações de código e confirmar que os novos componentes e relações de Carteira foram incorporados.
- [x] 7.8 Executar `git diff --check`, revisar `git diff` e `git status`, confirmando ausência de funcionalidades fora do escopo e preservação dos changeSets anteriores.
