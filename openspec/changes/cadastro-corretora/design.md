## Context

Veja `proposal.md` para a motivação e `specs/broker-registration/spec.md` para o contrato final. O Graphify mostra que a aplicação atual possui somente a baseline Spring Boot sob `com.projeto`, com bootstrap, configurações, `DBService`, tratamento centralizado por `ResourceExceptionHandler`/`StandardError` e testes de contexto. Ainda não existem componentes de corretora nem clients para BrasilAPI ou ViaCEP.

A change `estabilizar-infraestrutura-base` estabeleceu PostgreSQL no profile `dev`, H2 somente em testes e `spring.jpa.hibernate.ddl-auto=validate` no banco persistente. A configuração de teste ainda usa schema efêmero criado pelo Hibernate, mas esta change substituirá esse mecanismo pelo mesmo changelog Liquibase usado nos demais ambientes. O Hibernate permanecerá responsável somente pela validação do mapeamento.

As decisões de contrato, campos, CNPJ, situação cadastral, validação no mercado financeiro, tempo, resposta HTTP e schema foram aprovadas. Não restam decisões bloqueantes para a implementação desta fatia.

## Goals / Non-Goals

**Goals:**

- Introduzir uma fatia vertical testável para cadastrar uma corretora fornecendo somente o CNPJ.
- Obter todos os demais dados de fontes externas e impedir sua sobrescrita pelo cliente.
- Exigir uma confirmação REST explícita antes de persistir uma situação cadastral diferente de `ATIVA`.
- Persistir um modelo mínimo e completo de Corretora com schema versionado pelo Liquibase.
- Reutilizar a baseline e manter testes determinísticos sem chamadas reais aos serviços públicos.

**Non-Goals:**

- Criar listagem, consulta de corretora persistida, atualização, exclusão ou paginação.
- Criar cadastro pendente, tabela de confirmação ou sessão de confirmação no backend.
- Validar a atuação da instituição no mercado financeiro sem fonte pública aprovada.
- Criar relacionamentos com Ação, Carteira, Operação, investidor ou usuário.
- Implementar preço médio, cálculos financeiros, BRAPI, Alpha Vantage, frontend, OpenAPI, cache ou retentativas automáticas.

## Decisions

### 1. Implementar a fatia dentro da estrutura em camadas existente

Os componentes ficarão sob `com.projeto`: resource/controller REST, DTOs, service de aplicação, entidade, repository e pacote dedicado a integrações. O resource validará o contrato HTTP e delegará; o service coordenará regras e fontes; o repository cuidará somente da persistência. BrasilAPI e ViaCEP serão acessadas por abstrações equivalentes a `CnpjProvider` e `CepProvider`.

Alternativa considerada: introduzir módulos ou uma arquitetura genérica para instituições financeiras. Foi rejeitada por antecipar necessidades fora desta change.

### 2. Restringir a entrada e separar confirmação de dados cadastrais

O request inicial de `POST /corretoras` conterá somente:

```json
{
  "cnpj": "12.345.678/0001-90"
}
```

O DTO aceitará, além de `cnpj`, somente o controle opcional `confirmarSituacaoCadastralNaoAtiva`, usado exclusivamente na nova chamada de confirmação. Esse controle não integra a entidade. Propriedades que tentem fornecer razão social, endereço, situação, data ou qualquer outro dado de Corretora serão rejeitadas, em vez de ignoradas silenciosamente.

Alternativa considerada: aceitar dados manuais como fallback. Foi rejeitada porque a decisão aprovada atribui todos os dados, além do CNPJ, às fontes externas nesta primeira fatia.

### 3. Modelar a confirmação de situação não ativa como repetição explícita e stateless

O fluxo será:

```text
POST /corretoras { cnpj }
        ↓
BrasilAPI + ViaCEP
        ↓
situação ATIVA? ── sim ──> persistir ──> 201 Created
        │
        não
        ↓
409 Conflict + situação real + confirmacaoNecessaria=true
        ↓
POST /corretoras { cnpj, confirmarSituacaoCadastralNaoAtiva: true }
        ↓
repetir BrasilAPI + ViaCEP
        ↓
persistir situação vigente sem alteração ──> 201 Created
```

A resposta `409 Conflict` usará o formato centralizado de erro, ampliado com campos opcionais estruturados:

```json
{
  "timeStamp": 0,
  "status": 409,
  "error": "Conflict",
  "message": "A situação cadastral não está ativa e exige confirmação",
  "path": "/corretoras",
  "code": "SITUACAO_CADASTRAL_NAO_ATIVA",
  "details": {
    "situacaoCadastral": "<valor retornado pela BrasilAPI>",
    "confirmacaoNecessaria": true
  }
}
```

Não será persistido cadastro pendente nem criado token de confirmação. A nova chamada refará as consultas para não usar dados possivelmente desatualizados. Se a situação tiver se tornado `ATIVA`, o fluxo normal prossegue; se continuar não ativa, a confirmação explícita permite persistir o valor vigente exatamente como retornado.

Alternativas consideradas: manter estado temporário no banco ou em memória e criar token. Foram rejeitadas por adicionar entidade, expiração e coordenação distribuída desnecessárias; o controle explícito no novo POST é suficiente para esta API sem autenticação.

### 4. Definir a precedência e a matriz de dados externos

A BrasilAPI será a fonte do CNPJ existente, razão social, nome fantasia, e-mail, telefone, CEP, número, complemento e situação cadastral quando esses valores estiverem disponíveis. O CEP será normalizado para oito dígitos e enviado à ViaCEP, que validará sua existência e fornecerá logradouro, bairro, cidade e UF.

Campos obrigatórios para persistência:

- CNPJ normalizado;
- razão social;
- CEP validado;
- logradouro;
- bairro;
- cidade;
- UF;
- situação cadastral;
- data de cadastro gerada pela aplicação.

Campos opcionais, mantidos sem valor quando indisponíveis:

- nome fantasia;
- e-mail;
- telefone;
- número;
- complemento.

Se uma fonte não fornecer um campo obrigatório, o service interromperá o fluxo com erro de dados externos incompletos. Nenhum valor será fabricado nem solicitado ao cliente.

### 5. Normalizar o CNPJ e proteger sua identidade no banco

O CNPJ aceitará forma mascarada ou não mascarada. A aplicação removerá caracteres de formatação, exigirá 14 dígitos e validará localmente os dois dígitos verificadores antes de chamar a BrasilAPI. Providers, repository e entidade usarão somente a forma normalizada.

O service fará uma verificação antecipada por CNPJ para devolver erro de domínio amigável. A tabela também terá `UNIQUE (cnpj)` para proteger contra concorrência; uma violação dessa constraint será convertida para o mesmo erro padronizado de duplicidade.

### 6. Usar um único agregado persistente de Corretora

A entidade e a tabela `corretora` terão o seguinte modelo final:

| Campo Java | Tipo Java | Coluna | Tipo de banco | Restrição |
|---|---|---|---|---|
| `id` | `Long` | `id` | `BIGINT` | chave primária, auto incremento, não nulo |
| `cnpj` | `String` | `cnpj` | `VARCHAR(14)` | não nulo, único, somente dígitos |
| `razaoSocial` | `String` | `razao_social` | `VARCHAR(255)` | não nulo |
| `nomeFantasia` | `String` | `nome_fantasia` | `VARCHAR(255)` | opcional |
| `email` | `String` | `email` | `VARCHAR(255)` | opcional |
| `telefone` | `String` | `telefone` | `VARCHAR(30)` | opcional |
| `cep` | `String` | `cep` | `VARCHAR(8)` | não nulo, somente dígitos |
| `logradouro` | `String` | `logradouro` | `VARCHAR(255)` | não nulo |
| `numero` | `String` | `numero` | `VARCHAR(30)` | opcional |
| `complemento` | `String` | `complemento` | `VARCHAR(255)` | opcional |
| `bairro` | `String` | `bairro` | `VARCHAR(150)` | não nulo |
| `cidade` | `String` | `cidade` | `VARCHAR(150)` | não nulo |
| `uf` | `String` | `uf` | `VARCHAR(2)` | não nulo |
| `situacaoCadastral` | `String` | `situacao_cadastral` | `VARCHAR(100)` | não nulo, valor externo sem transformação |
| `validadaMercadoFinanceiro` | `boolean` | `validada_mercado_financeiro` | `BOOLEAN` | não nulo, default `false` |
| `dataCadastro` | `OffsetDateTime` | `data_cadastro` | `TIMESTAMP WITH TIME ZONE` | não nulo |

O endereço permanecerá incorporado ao mesmo agregado, sem entidade ou repository próprios. DTOs separarão a API da entidade JPA. No DTO completo de resposta, os campos opcionais serão apresentados com valor nulo quando a fonte não os disponibilizar.

### 7. Representar validação no mercado financeiro como ainda não realizada

`validadaMercadoFinanceiro` será sempre criado com `false` nesta fatia. O nome histórico do campo será preservado, mas sua documentação e o DTO definirão `false` como “ainda não validada no mercado financeiro”. Esse valor não constitui reprovação nem afirma que a instituição não atua no mercado financeiro.

Não haverá provider, chamada externa nem entrada do cliente para esse campo até uma change futura aprovar fonte e semântica adicionais.

### 8. Gerar `dataCadastro` com relógio UTC controlado pela aplicação

O service gerará `OffsetDateTime` no instante imediatamente anterior à persistência usando um relógio da aplicação configurado com `ZoneOffset.UTC`. O valor será armazenado com offset e serializado em ISO-8601, usando `Z` ou `+00:00` como representação equivalente de UTC.

O relógio será injetável/substituível em testes para permitir asserções determinísticas. Cliente, BrasilAPI e ViaCEP não poderão fornecer `dataCadastro`.

Alternativa considerada: `LocalDateTime`. Foi rejeitada porque não preserva offset e torna o instante ambíguo entre ambientes.

### 9. Executar Liquibase antes da validação do Hibernate

Será adicionada a dependência `org.liquibase:liquibase-core` sem versão explícita, usando a versão gerenciada pelo Spring Boot. A estrutura prevista é:

```text
src/main/resources/db/changelog/
├── db.changelog-master.yaml
└── changes/
    └── 001-create-corretora.yaml
```

O master incluirá o primeiro changeSet, que criará somente a tabela `corretora`, sua chave primária, as constraints `NOT NULL`, o default de `validada_mercado_financeiro` e a unicidade de `cnpj`. As tabelas de controle criadas automaticamente pelo Liquibase não representam domínio adicional.

Liquibase será habilitado nos ambientes aplicáveis e executará antes da criação do `EntityManagerFactory`. `spring.jpa.hibernate.ddl-auto=validate` será mantido em `dev` e passará a ser usado também em `test`; `create`, `update` e `create-drop` não serão utilizados. O H2 efêmero executará o mesmo changelog master e desaparecerá ao final do processo por ser um banco em memória, não por ação do Hibernate.

O changeSet terá rollback explícito para remover `corretora`, mas esse rollback somente poderá ser executado operacionalmente com confirmação de que nenhum dado precisa ser preservado.

### 10. Isolar integrações e usar o cliente HTTP existente

DTOs da BrasilAPI e ViaCEP serão internos aos adapters e convertidos para modelos mínimos dos providers. URLs base e tempos limite serão configuráveis externamente. O cliente HTTP síncrono fornecido pelo Spring Web será usado sem adicionar WebFlux ou Spring Cloud OpenFeign.

Chamadas externas ocorrerão antes da seção transacional mínima de persistência. A transação conterá a verificação final de unicidade e a gravação, sem permanecer aberta durante acessos de rede.

### 11. Padronizar respostas de sucesso e falha

O cadastro concluído responderá `201 Created`, corpo com DTO completo e `Location: /corretoras/{id}`. A URI antecipa o identificador estável do recurso, sem implementar o `GET` nesta change.

O tratamento centralizado distinguirá:

- `400` para request inválido, propriedades de corretora não permitidas, CNPJ/CEP de formato inválido;
- `404` para CNPJ ou CEP inexistente;
- `409` para CNPJ duplicado ou situação não ativa sem confirmação, diferenciados por `code`;
- `422` para dados obrigatórios ausentes nas respostas externas;
- `429` para limite de requisições identificado;
- `502`, `503` ou `504` para resposta inválida, indisponibilidade ou timeout do provedor.

Os campos atuais de `StandardError` serão preservados e `code`/`details` serão opcionais, evitando quebrar os tratamentos existentes.

### 12. Testar sem rede ou PostgreSQL real

Testes unitários do service usarão providers substitutos. Testes dos adapters simularão respostas HTTP. Testes de repository, Liquibase, endpoint e contexto usarão o profile `test`, H2 e o changelog real. A suíte cobrirá os dois passos de situação não ativa, repetição das consultas, preservação da situação vigente, campos opcionais ausentes, campos obrigatórios ausentes, concorrência/duplicidade e todas as falhas externas previstas.

## Risks / Trade-offs

- [A confirmação exige duas chamadas externas completas] → Repetir as consultas evita persistir dados ou situação que mudaram entre a informação e a confirmação.
- [O controle booleano pode ser enviado preventivamente por um cliente] → Tratar `true` como manifestação explícita da decisão; documentar o fluxo esperado em duas chamadas e nunca inferir confirmação quando o campo estiver ausente ou `false`.
- [A ViaCEP pode omitir endereço em CEPs válidos] → Como os campos aprovados são obrigatórios e não há entrada manual nesta fatia, rejeitar com erro de dados externos incompletos.
- [Duas solicitações simultâneas podem tentar cadastrar o mesmo CNPJ] → Combinar verificação no service, constraint única e tradução da violação de banco.
- [H2 e PostgreSQL podem interpretar tipos de forma diferente] → Usar tipos portáveis no changelog e executar Liquibase seguido de `validate` em testes.
- [`false` pode ser interpretado como reprovação no mercado financeiro] → Documentar a semântica no DTO, na spec e no código futuro; não criar comportamento condicionado a esse valor nesta fatia.
- [Rollback do primeiro changeSet remove dados] → Disponibilizar rollback explícito, mas exigir decisão operacional antes de executá-lo fora de banco descartável.

## Migration Plan

1. Adicionar `liquibase-core`, o changelog master e `001-create-corretora.yaml`.
2. Alterar o profile de teste para `ddl-auto=validate` e habilitar o mesmo changelog usado em `dev`.
3. Implementar entidade e repository coerentes com o changeSet e validar o bootstrap em H2.
4. Implementar providers/adapters, regras de CNPJ, service, DTOs, fluxo de confirmação, resource e erros.
5. Executar Liquibase no PostgreSQL de desenvolvimento antes do Hibernate validar o novo mapeamento.
6. Executar build, suíte completa, validação OpenSpec e atualização do Graphify.

Rollback: reverter endpoint e componentes se necessário. A tabela somente deverá ser removida por rollback Liquibase explicitamente autorizado e apenas quando não houver dados a preservar; nunca restaurar `ddl-auto=create`, `update` ou `create-drop`.
