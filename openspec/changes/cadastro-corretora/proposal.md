## Why

O MVP precisa permitir o primeiro cadastro persistente de uma corretora a partir de um CNPJ real, reduzindo preenchimento manual e impedindo que dados cadastrais ou de endereço não validados sejam salvos. Esta change entrega somente essa primeira fatia funcional, apoiada pela BrasilAPI e pela ViaCEP, sobre a baseline técnica já estabilizada.

## What Changes

- Criar o cadastro de corretora por meio de `POST /corretoras`, recebendo inicialmente somente o CNPJ como dado cadastral fornecido pelo cliente.
- Aceitar CNPJ com ou sem máscara, normalizar para 14 dígitos, validar localmente os dígitos verificadores, confirmar sua existência na BrasilAPI e persistir somente o valor normalizado.
- Impedir que o cliente forneça ou sobrescreva os dados cadastrais e de endereço obtidos das fontes externas nesta primeira fatia.
- Obter da BrasilAPI os dados cadastrais relacionados ao CNPJ e o CEP usado na etapa de endereço; consultar a ViaCEP para validar o CEP e obter os dados de endereço disponíveis.
- Exigir para persistência CNPJ, razão social, CEP, logradouro, bairro, cidade, UF, situação cadastral e data de cadastro; tratar nome fantasia, e-mail, telefone, número e complemento como opcionais, preenchidos somente quando disponíveis nas fontes externas.
- Quando a situação cadastral for `ATIVA`, prosseguir normalmente. Para qualquer outro valor, responder sem persistir que uma confirmação é necessária e permitir uma nova chamada explicitamente confirmada, preservando exatamente a situação retornada pela fonte.
- Manter `validadaMercadoFinanceiro=false` enquanto não houver fonte pública aprovada, documentando que `false` significa “ainda não validada no mercado financeiro”, e não uma conclusão negativa sobre a instituição.
- Gerar `dataCadastro` na aplicação como `OffsetDateTime` em UTC no instante da persistência; o campo não será aceito do cliente nem obtido das APIs externas.
- Garantir unicidade do CNPJ normalizado no banco e rejeitar cadastro duplicado.
- Responder ao cadastro concluído com `201 Created`, DTO completo e header `Location: /corretoras/{id}`.
- Adicionar `liquibase-core`, um changelog master e um primeiro changeSet contendo somente a tabela, as constraints e o índice necessários para Corretora.
- Executar o changelog do Liquibase nos ambientes aplicáveis, inclusive testes, manter `spring.jpa.hibernate.ddl-auto=validate` e não usar `create`, `update` ou `create-drop` como estratégia de schema desta funcionalidade.
- Tratar de forma padronizada CNPJ inválido ou inexistente, CEP inválido ou inexistente, dados externos obrigatórios ausentes, situação cadastral não ativa sem confirmação, duplicidade e falhas de integração, incluindo indisponibilidade, timeout e limite de requisições.
- Isolar BrasilAPI e ViaCEP atrás de abstrações de integração e criar os testes mínimos do cadastro, da persistência, do endpoint, das migrations, das integrações e dos cenários de falha.
- Manter fora desta fatia a listagem, a consulta de corretora persistida, a atualização e a exclusão, além de Ação, Carteira, Operação, preço médio, cálculos financeiros, BRAPI, Alpha Vantage e frontend.

## Capabilities

### New Capabilities

- `broker-registration`: Cadastro persistente de corretora por CNPJ, enriquecimento e validação cadastral pela BrasilAPI, validação de endereço pela ViaCEP, confirmação explícita de situação não ativa e tratamento das falhas correspondentes.

### Modified Capabilities

Nenhuma. A capability de baseline técnica permanece inalterada; esta change introduz a primeira capability funcional de corretoras e passa a usar Liquibase dentro das garantias de isolamento de profiles e validação não destrutiva já estabelecidas.

## Impact

- Nova API REST em `POST /corretoras`, com request inicial contendo somente `cnpj`, controle explícito de confirmação em uma nova requisição e resposta `201 Created` com DTO completo e `Location`.
- Novos componentes de entidade/modelo, DTO, service e repository sob a árvore `com.projeto`, além da ampliação do formato centralizado de erros para comunicar confirmação necessária de maneira estruturada.
- Novas abstrações e adapters HTTP dedicados à BrasilAPI e à ViaCEP, com configuração externa de endpoints e tempos limite, sem credenciais versionadas.
- Nova dependência `liquibase-core`, changelog master e changeSet inicial restrito à entidade Corretora.
- Alteração da configuração de schema de teste para executar Liquibase antes da validação do Hibernate, eliminando `ddl-auto=create-drop`; PostgreSQL e H2 usarão o mesmo changelog aplicável e Hibernate permanecerá somente com `validate`.
- Uso das dependências Spring Web, Spring Data JPA, Bean Validation e testes já presentes; nenhuma outra dependência ou abstração será adicionada sem necessidade demonstrada.
- Nenhuma alteração em listagem ou consulta de corretoras persistidas, atualização, exclusão, ações, carteira, operações, posições, cálculos financeiros, BRAPI, Alpha Vantage ou frontend.
