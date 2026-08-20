# Gestão de Ações

Este repositório contém a aplicação de Gestão de Ações. Os requisitos do produto estão descritos em `docs/PRD.md`.

## Execução em desenvolvimento

O ambiente de desenvolvimento usa PostgreSQL e deve ser ativado explicitamente. Antes de iniciar a aplicação, defina as variáveis nativas do Spring para o datasource:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:SPRING_DATASOURCE_URL = "<URL_DO_POSTGRESQL>"
$env:SPRING_DATASOURCE_USERNAME = "<USUARIO_DO_POSTGRESQL>"
$env:SPRING_DATASOURCE_PASSWORD = "<SENHA_DO_POSTGRESQL>"
./mvnw.cmd spring-boot:run
```

Os valores reais devem ser fornecidos pelo ambiente e não devem ser adicionados a arquivos versionados. O schema PostgreSQL deve existir e ser compatível com a aplicação: no profile `dev`, o Hibernate somente o valida e não cria, atualiza ou remove estruturas automaticamente.

Execuções normais e com o profile `dev` não usam H2 como fallback. Se o profile ou alguma variável obrigatória do PostgreSQL estiver ausente, a inicialização pode falhar explicitamente.

## Testes

Os testes ativam o profile `test` explicitamente e usam um banco H2 efêmero, disponível somente no classpath de teste:

```powershell
./mvnw.cmd test
```

## Ação operacional de segurança pendente

A credencial PostgreSQL que já esteve exposta em arquivo versionado deve ser revogada ou rotacionada diretamente no ambiente PostgreSQL antes de voltar a ser utilizada. A remoção dos valores do estado atual do repositório não invalida a credencial antiga nem remove ocorrências do histórico Git. Esta mudança não executa nem confirma a rotação externa e não reescreve o histórico.
