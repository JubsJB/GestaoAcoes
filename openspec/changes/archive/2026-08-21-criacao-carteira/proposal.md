## Why

O MVP precisa permitir a criação de uma carteira persistente antes que operações de compra e venda possam ser associadas e consolidadas. Esta change introduz somente essa primeira fatia do domínio Carteira, preservando a arquitetura em camadas, a gestão de schema e o padrão HTTP já estabelecidos no projeto.

## What Changes

- Criar a entidade persistente `Carteira` com `id`, `nome` e `dataCriacao`, sem associação com usuário, ações, operações ou posições nesta fatia.
- Expor `POST /carteiras` com DTO de entrada separado da entidade; o cliente fornecerá somente `nome`, que será obrigatório, normalizado apenas com `trim` e limitado a 255 caracteres.
- Gerar `dataCriacao` pela aplicação em UTC, usando `OffsetDateTime` e o relógio já configurado no projeto.
- Responder à criação concluída com `201 Created`, `CarteiraResponse` completo e `Location: /carteiras/{id}`.
- Adicionar resource, service, repository, mapper e DTOs específicos de Carteira seguindo os pacotes existentes, sem criar uma camada transacional adicional onde não há chamada externa a isolar.
- Evoluir o changelog master com um único próximo changeSet, destinado exclusivamente à tabela `carteira`, mantendo Liquibase responsável pelo schema e Hibernate com `ddl-auto=validate`.
- Reutilizar o tratamento centralizado de erros atual e criar testes unitários e de integração proporcionais à criação.
- Rejeitar nome nulo, vazio, composto somente por espaços ou superior a 255 caracteres após o `trim`, preservando espaços internos, acentos e caixa.
- Permitir nomes duplicados, sem consulta de duplicidade ou constraint única, mantendo `id` como identidade estável da Carteira.

## Capabilities

### New Capabilities

- `portfolio-creation`: define o contrato para criar e persistir uma Carteira mínima, gerar sua data de criação e devolver o recurso criado.

### Modified Capabilities

Nenhuma.

## Impact

- API: novo endpoint `POST /carteiras`.
- Backend: novos componentes de Carteira nos pacotes existentes de entidade, DTO, mapper, repository, service e resource.
- Persistência: novo changeSet `003-create-carteira.yaml`, incluído após os changeSets imutáveis de Corretora e Ação.
- Testes: cobertura de validação, service, persistência, endpoint e inicialização Liquibase/Hibernate no H2.
- Dependências e integrações externas: nenhuma alteração prevista.
- Fora de escopo: consultas de carteira, atualização, exclusão, ações associadas diretamente, operações, posições, cálculos financeiros, snapshots, histórico e frontend.
