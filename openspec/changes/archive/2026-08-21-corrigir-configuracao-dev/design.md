## Context

Veja `proposal.md` para a motivação. A primeira linha de `src/main/resources/application-dev.properties` contém atualmente `spring.datasource.url=gi${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/gestaoacoesdb}`. O literal `gi` é concatenado tanto ao valor externo quanto ao fallback e impede que o datasource receba uma URL JDBC válida.

O Graphify relaciona essa configuração à baseline de runtime: o profile `dev` deve permanecer isolado, utilizar PostgreSQL configurado externamente e manter o Hibernate em `ddl-auto=validate`. A capability correspondente da change `estabilizar-infraestrutura-base` está concluída, mas ainda não foi promovida para `openspec/specs`; por isso esta change descreve a correção em uma capability nova e estritamente focada, sem alterar ou arquivar a change anterior.

## Goals / Non-Goals

**Goals:**

- Fazer o placeholder de URL ocupar todo o valor de `spring.datasource.url`.
- Preservar byte a byte as demais linhas de `application-dev.properties`.
- Validar a alteração com inspeção focada e com o Maven Wrapper.

**Non-Goals:**

- Mudar defaults, nomes de variáveis, credenciais ou seleção de profiles.
- Alterar estratégia de schema, Liquibase, Hibernate, código Java, testes, dependências ou integrações.
- Executar refatorações ou funcionalidades de negócio.

## Decisions

### 1. Realizar uma substituição de uma única linha

A implementação substituirá:

```properties
spring.datasource.url=gi${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/gestaoacoesdb}
```

por:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/gestaoacoesdb}
```

Essa alteração remove somente os dois caracteres indevidos e mantém o nome da variável, o fallback e a sintaxe do placeholder.

Alternativas consideradas: remover o fallback ou substituir a propriedade por uma URL fixa. Ambas foram rejeitadas porque mudariam decisões já estabelecidas para a configuração externa do ambiente `dev`.

### 2. Não criar código ou teste específico para uma correção declarativa

A verificação focada comparará o diff de `application-dev.properties` e confirmará a forma exata da propriedade, incluindo resolução por variável e fallback. Em seguida, os testes existentes e `clean verify` serão executados pelo Maven Wrapper para detectar regressões de compilação, contexto, Liquibase e Hibernate já cobertas pela suíte.

Alternativa considerada: adicionar um teste que replique o conteúdo esperado do arquivo de propriedades. Foi rejeitada porque duplicaria uma linha declarativa estável em código de teste e ampliaria a alteração além do necessário; a inspeção exata do diff cobre diretamente o defeito, enquanto a suíte existente cobre a baseline da aplicação.

### 3. Não iniciar nem modificar o banco durante a validação

A validação obrigatória não dependerá de um PostgreSQL de desenvolvimento disponível. Ela usará o profile `test` e o H2 conforme a estratégia existente da suíte, sem alterar `ddl-auto=validate` ou changeSets. Uma execução manual com `dev` poderá ser feita somente como confirmação adicional quando as variáveis e o PostgreSQL já estiverem disponíveis, mas não será necessária para concluir esta correção declarativa.

Alternativa considerada: exigir a inicialização contra PostgreSQL. Foi rejeitada como critério obrigatório porque adicionaria uma dependência externa que não é necessária para comprovar a remoção do prefixo e poderia bloquear a correção por condições do ambiente.

## Risks / Trade-offs

- [A suíte automatizada usa o profile `test` e não exercita diretamente `application-dev.properties`] → Confirmar de forma determinística o valor exato da propriedade e revisar o diff de escopo antes do build completo.
- [Alterações não relacionadas já presentes no worktree podem contaminar a revisão] → Restringir a implementação e a inspeção de diff ao arquivo `application-dev.properties` e aos artefatos desta change, preservando o trabalho existente.
- [Uma execução real com `dev` ainda depende de PostgreSQL e senha externos] → Manter essa validação como confirmação opcional, sem inventar credenciais nem alterar a configuração para contornar indisponibilidade ambiental.

## Migration Plan

1. Alterar exclusivamente a linha de `spring.datasource.url` conforme a decisão 1.
2. Inspecionar o diff focado para confirmar que nenhum outro conteúdo do profile foi modificado.
3. Executar os testes existentes e `clean verify` pelo Maven Wrapper.
4. Validar a change em modo strict e revisar o estado final do Git sem realizar commit.

Não há migração nem rollback de banco. Se for necessário desfazer a alteração por uma condição imprevista, a reversão se limita à mesma linha de configuração, embora restaure o defeito conhecido.
