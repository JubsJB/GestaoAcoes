## 1. Liquibase e configuração de schema

- [x] 1.1 Adicionar `org.liquibase:liquibase-core` sem versão explícita, usando o gerenciamento de dependências do Spring Boot.
- [x] 1.2 Criar `db/changelog/db.changelog-master.yaml` e incluir `changes/001-create-corretora.yaml`.
- [x] 1.3 Criar o changeSet inicial somente com a tabela `corretora`, chave primária, colunas do modelo aprovado, constraints obrigatórias, default `false` para `validada_mercado_financeiro` e unicidade de `cnpj`, incluindo rollback explícito da tabela.
- [x] 1.4 Configurar Liquibase nos ambientes aplicáveis e alterar o profile `test` para `spring.jpa.hibernate.ddl-auto=validate`, removendo `create-drop` e qualquer uso de `create` ou `update` como estratégia de schema.
- [x] 1.5 Criar teste de integração que execute o changelog master no H2 efêmero e confirme que o Hibernate valida a estrutura resultante.

## 2. Modelo e persistência de Corretora

- [x] 2.1 Criar a entidade `Corretora` com o modelo e as nulabilidades aprovadas, usando `OffsetDateTime` para `dataCadastro`, `boolean` com default `false` para `validadaMercadoFinanceiro` e campos de endereço no mesmo agregado.
- [x] 2.2 Criar o repository com busca técnica por CNPJ normalizado e operação de verificação de existência para o fluxo de cadastro.
- [x] 2.3 Garantir no mapeamento JPA a correspondência exata com nomes, tipos, tamanhos, nulabilidades e unicidade definidos pelo changeSet.
- [x] 2.4 Criar DTO completo de saída e mapeamento entre entidade e DTO, mantendo campos opcionais com valor nulo quando indisponíveis e documentando a semântica de `validadaMercadoFinanceiro=false`.

## 3. Validação local e integrações externas

- [x] 3.1 Implementar normalização de CNPJ com ou sem máscara, exigência de 14 dígitos e validação local dos dois dígitos verificadores antes de qualquer chamada externa.
- [x] 3.2 Implementar normalização e validação de formato do CEP obtido da BrasilAPI para oito dígitos antes da consulta à ViaCEP.
- [x] 3.3 Criar os contratos internos `CnpjProvider` e `CepProvider` e seus modelos mínimos, sem vazar DTOs dos serviços externos para o caso de uso.
- [x] 3.4 Configurar URLs base e tempos limite da BrasilAPI e ViaCEP por propriedades externas, sem credenciais ou segredos versionados.
- [x] 3.5 Implementar o adapter da BrasilAPI para existência do CNPJ, razão social, nome fantasia, e-mail, telefone, CEP, número, complemento e situação cadastral, mapeando inexistência e falhas externas.
- [x] 3.6 Implementar o adapter da ViaCEP para existência do CEP, logradouro, bairro, cidade e UF, mapeando CEP inexistente e falhas externas.

## 4. Caso de uso e fluxo REST

- [x] 4.1 Criar o DTO de entrada aceitando somente `cnpj` e o controle opcional `confirmarSituacaoCadastralNaoAtiva`, rejeitando propriedades que tentem fornecer ou sobrescrever dados da Corretora.
- [x] 4.2 Implementar a verificação antecipada e o tratamento concorrente de CNPJ duplicado, traduzindo também a violação da constraint única para erro padronizado.
- [x] 4.3 Implementar a consolidação dos dados da BrasilAPI e ViaCEP, rejeitando ausência de campo obrigatório e permitindo ausência de nome fantasia, e-mail, telefone, número e complemento.
- [x] 4.4 Implementar o fluxo de situação `ATIVA`, prosseguindo para persistência sem confirmação adicional após o sucesso das demais validações.
- [x] 4.5 Implementar a primeira resposta para situação diferente de `ATIVA` como `409 Conflict`, sem persistência, com `code=SITUACAO_CADASTRAL_NAO_ATIVA`, situação real e `confirmacaoNecessaria=true`.
- [x] 4.6 Implementar a nova requisição com `confirmarSituacaoCadastralNaoAtiva=true`, repetindo BrasilAPI e ViaCEP e persistindo exatamente a situação cadastral vigente quando as validações tiverem sucesso.
- [x] 4.7 Gerar `dataCadastro` com relógio da aplicação em UTC imediatamente antes da persistência e inicializar `validadaMercadoFinanceiro=false` com significado de validação ainda não realizada.
- [x] 4.8 Restringir a transação à verificação final de unicidade e à persistência única, sem manter transação de banco aberta durante chamadas externas.
- [x] 4.9 Implementar `POST /corretoras` retornando `201 Created`, DTO completo e `Location: /corretoras/{id}` tanto no fluxo ativo quanto no fluxo confirmado.
- [x] 4.10 Ampliar `StandardError` e o tratamento centralizado com `code` e `details` opcionais para distinguir request inválido, CNPJ/CEP inexistente, duplicidade, confirmação necessária, dados externos incompletos, limite, indisponibilidade e timeout.

## 5. Testes automatizados

- [x] 5.1 Criar testes unitários para CNPJ mascarado e não mascarado, normalização, dígitos verificadores inválidos e garantia de que CNPJ inválido não aciona a BrasilAPI.
- [x] 5.2 Criar testes unitários para normalização e formato do CEP obtido externamente.
- [x] 5.3 Criar testes dos adapters com respostas HTTP simuladas de sucesso, inexistência, payload incompleto, limite de requisições, indisponibilidade e timeout da BrasilAPI e ViaCEP.
- [x] 5.4 Criar testes do service para cadastro `ATIVA`, precedência exclusiva das fontes externas, campos opcionais ausentes e rejeição de campos obrigatórios externos ausentes.
- [x] 5.5 Criar testes do service para primeira requisição não ativa sem persistência, resposta de confirmação necessária e preservação exata da situação retornada.
- [x] 5.6 Criar testes do service para nova requisição confirmada, repetição das duas consultas externas, situação ainda não ativa, situação que se tornou ativa e falha ocorrida durante a repetição.
- [x] 5.7 Criar testes para `validadaMercadoFinanceiro=false`, sua semântica de “ainda não validada” e `dataCadastro` determinística em UTC com relógio controlado.
- [x] 5.8 Criar testes de repository para persistência completa, campos opcionais nulos e unicidade concorrente do CNPJ normalizado.
- [x] 5.9 Criar testes do endpoint para rejeição de dados não permitidos, erros padronizados, `409` de confirmação e sucesso `201` com DTO completo e `Location`.
- [x] 5.10 Executar novamente os testes de contexto da baseline e confirmar descoberta dos novos componentes, Liquibase seguido de Hibernate `validate` e H2 restrito ao profile `test`.

## 6. Verificação e consistência

- [x] 6.1 Executar o build completo pelo Maven Wrapper e confirmar a compilação do código principal e de teste.
- [x] 6.2 Executar toda a suíte automatizada e confirmar que nenhum teste realiza chamadas reais à BrasilAPI, ViaCEP ou PostgreSQL.
- [x] 6.3 Iniciar o profile `dev` contra um PostgreSQL configurado, aplicar o changelog pelo Liquibase e confirmar que o Hibernate valida a estrutura sem tentar criá-la ou alterá-la.
- [x] 6.4 Revisar o changelog para confirmar que somente a estrutura de Corretora foi criada e que nenhuma tabela de Ação, Carteira, Operação ou outra funcionalidade entrou no changeSet.
- [x] 6.5 Revisar configurações e arquivos versionados para confirmar ausência de credenciais e de `ddl-auto=create`, `update` ou `create-drop` como estratégia de schema da funcionalidade.
- [x] 6.6 Validar a change `cadastro-corretora` com OpenSpec em modo estrito e reconciliar qualquer divergência entre implementação, proposal, spec, design e tasks.
- [x] 6.7 Atualizar o Graphify após as alterações de código e verificar que o grafo represente Corretora, resource, service, repository, providers, adapters, Liquibase e testes.
- [x] 6.8 Revisar `git diff` e `git status`, confirmando o conjunto final de arquivos sem realizar commit, push, merge ou alteração do histórico Git.
