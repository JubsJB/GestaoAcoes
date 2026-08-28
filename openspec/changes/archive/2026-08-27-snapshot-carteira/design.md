## Context

As consultas atuais seguem três camadas reutilizáveis: `PosicaoService` executa uma única leitura cronológica com fetch de `Acao` e o replay oficial; `AgregadorPosicoesPorMoeda` soma custo, patrimônio e resultado não realizado em memória; `PatrimonioService` apenas projeta o patrimônio atual. `portfolio-valuation` define `patrimonioAtual(moeda) = soma(valorAtualPosicao)` das posições abertas, escala 12 e precisão 38, sem resultado realizado, caixa ou conversão.

Operações de escrita são coordenadas por lock curto de Carteira. Atualizações de cotação usam lock curto de Ação. As consultas financeiras atuais usam `REPEATABLE_READ`, não usam lock e observam um estado integral anterior ou posterior a escritas concorrentes. `HistoricoCotacao` registra somente cotações aceitas, mas posição e patrimônio usam diretamente `Acao.cotacaoAtual`.

O PRD prevê conceitualmente Snapshot da Carteira, mas não define modelo, momento de geração, contrato REST, tratamento de Carteira vazia, unicidade ou concorrência. As decisões abaixo foram aprovadas como normativas para esta change.

## Goals / Non-Goals

**Goals:**

- Capturar e persistir uma visão consistente do patrimônio atual por moeda.
- Preservar snapshots imutáveis e independentes de alterações futuras.
- Reutilizar integralmente replay, agregação e política numérica existentes.
- Criar base normalizada e temporal para futura `evolucao-patrimonial`.

**Non-Goals:**

- Reconstrução para data passada, backfill ou histórico completo de mercado.
- Persistência de posições, custo, resultado, rentabilidade ou caixa.
- Consulta pública, evolução patrimonial, gráficos ou agrupamentos.
- Scheduler, cron, job, geração automática ou chamada a provider.

## Decisions

### 1. Modelar pai e componentes por moeda

Decisão: utilizar duas entidades/tabelas normalizadas.

```text
SnapshotCarteira
  id: Long
  carteira: Carteira (obrigatória, LAZY, unidirecional)
  dataHoraSnapshot: OffsetDateTime

SnapshotCarteiraMoeda
  id: Long
  snapshotCarteira: SnapshotCarteira (obrigatório, LAZY, unidirecional)
  moeda: Moeda
  patrimonioAtual: BigDecimal
```

Não adicionar coleções em `Carteira` ou `SnapshotCarteira`, cascade JPA ou `ON DELETE CASCADE`. O serviço persiste pai e componentes explicitamente na mesma transação; isso mantém a atomicidade sem criar mecanismo de exclusão incompatível com append-only.

Alternativa rejeitada: uma linha independente por Carteira+moeda+instante não representa naturalmente a captura vazia, repete Carteira/timestamp, enfraquece atomicidade da fotografia e não fornece identidade única ao evento de captura.

### 2. Persistir somente patrimônio atual por moeda

Decisão: o pai guarda identidade temporal e os filhos guardam somente `moeda` e `patrimonioAtual`. Não persistir posições individuais, custo, resultado não realizado, rentabilidade ou quantidade de posições. A futura evolução patrimonial necessita da série `Carteira + instante + moeda + patrimônio`, e métricas adicionais aumentariam acoplamento e redundância sem requisito atual.

### 3. Representar Carteira vazia por pai sem filhos

Decisão: persistir snapshot válido com `patrimonios=[]`. Isso preserva o ponto temporal de patrimônio vazio/zero sem fabricar componentes BRL/USD e permite que a evolução futura represente intervalos sem posições. Rejeitar ou não persistir eliminaria informação temporal relevante.

### 4. Criar somente sob comando REST explícito

Decisão:

```http
POST /carteiras/{carteiraId}/snapshots
Content-Length: 0

201 Created
Location: /carteiras/{carteiraId}/snapshots/{snapshotId}
```

Resposta aprovada:

```json
{
  "id": 10,
  "carteiraId": 1,
  "dataHoraSnapshot": "2026-08-27T15:00:00Z",
  "patrimonios": [
    { "moeda": "BRL", "patrimonioAtual": 12500.000000000000 }
  ]
}
```

Sem request body. Carteira inexistente retorna 404. Não há provider, refresh de cotação ou `Location` para outra entidade. Geração após Operação/PATCH e scheduler foram rejeitados no MVP por acoplarem use cases e tornarem frequência/volume implícitos.

### 5. Não criar endpoint público de leitura

Decisão: devolver o recurso recém-criado no POST, mas não implementar `GET`, `PATCH` ou `DELETE`. A URI em `Location` estabelece identidade canônica futura; não congela paginação, filtros ou contrato de evolução. A futura `evolucao-patrimonial` definirá a consulta temporal necessária.

### 6. Definir `dataHoraSnapshot` pelo relógio da aplicação

Decisão: capturar `OffsetDateTime.now(clock).withOffsetSameInstant(UTC)` exatamente uma vez dentro da transação e antes da primeira leitura financeira. O valor identifica o começo da fotografia, não o timestamp de qualquer cotação. As diferentes `Acao.dataHoraCotacao` permanecem independentes e não são persistidas no snapshot agregado.

Não usar `LocalDate`, timestamp do provider, maior/menor timestamp de cotação nem instante fornecido pelo cliente.

### 7. Permitir múltiplos snapshots e não deduplicar conteúdo

Decisão: múltiplos snapshots no mesmo dia e patrimônios idênticos em timestamps diferentes são válidos. A única unicidade temporal é `(carteira_id, data_hora_snapshot)`. Colisão no mesmo timestamp retorna `409 / SNAPSHOT_CARTEIRA_DUPLICADO`; não reutilizar erro de Operação ou cotação com semântica diferente.

### 8. Tornar a persistência estritamente append-only

Decisão: nenhuma API ou fluxo automático altera/recalcula snapshot existente. Não há update nem delete público. Operações e cotações posteriores afetam apenas futuras capturas.

### 9. Reutilizar componentes inferiores, não `PatrimonioService`

Fluxo recomendado:

```text
CarteiraResource
  → SnapshotCarteiraService
      → Clock (uma vez)
      → PosicaoService.listarPorCarteira (uma vez)
          → OperacaoRepository com fetch plan atual
          → CalculadoraPosicao / CalculadoraRentabilidade
      → AgregadorPosicoesPorMoeda.agregar (uma vez)
      → SnapshotCarteiraRepository + SnapshotCarteiraMoedaRepository
      → SnapshotCarteiraMapper
      → SnapshotCarteiraResponse
```

`PatrimonioService` é outro use case e não deve ser chamado nem modificado. O reuso de `PosicaoService + AgregadorPosicoesPorMoeda` garante a mesma fonte de cálculo sem acoplamento entre serviços de aplicação. O agregador continua puro e retorna também campos não persistidos; o snapshot seleciona apenas `patrimonioAtual`.

### 10. Usar uma transação de escrita `REPEATABLE_READ`, sem lock novo

Decisão: `SnapshotCarteiraService.criar` abre uma única transação com `Isolation.REPEATABLE_READ`. A chamada ao `PosicaoService` participa dessa transação, a agregação ocorre em memória e pai/filhos são gravados antes do commit.

No PostgreSQL, a visão MVCC permanece consistente diante de Operação ou PATCH de cotação concorrente: o snapshot reflete integralmente o estado comprometido visível à primeira leitura. Não é necessário adquirir simultaneamente locks de Carteira e todas as Ações, o que ampliaria contenção e risco de deadlock. A unique constraint resolve duas capturas no mesmo timestamp.

Alternativa rejeitada: usar `PatrimonioService` em uma transação separada perderia a unidade entre cálculo e gravação. Lock global/pessimista em todas as fontes seria desproporcional.

### 11. Reutilizar política numérica de `portfolio-valuation`

Decisão: `patrimonioAtual` vem diretamente de `TotaisPorMoeda.patrimonioAtual`, calculado com `BigDecimal.add`, sem `MathContext` ou arredondamento intermediário, normalizado ao final para escala 12 por `UNNECESSARY` e precisão 38. Persistência `NUMERIC(38,12)`. Reutilizar `422 / CALCULO_POSICAO_FORA_DA_PRECISAO`; histórico inconsistente continua `409 / HISTORICO_OPERACOES_INCONSISTENTE`.

### 12. Evoluir o schema pelo changeSet 006

Decisão: `006-create-snapshot-carteira.yaml`, incluído depois do 005, sem modificar 001–005.

`snapshot_carteira`:

- `id BIGINT` PK identity;
- `carteira_id BIGINT NOT NULL` FK sem cascade;
- `data_hora_snapshot TIMESTAMP WITH TIME ZONE NOT NULL`;
- `UNIQUE (carteira_id, data_hora_snapshot)`.

`snapshot_carteira_moeda`:

- `id BIGINT` PK identity;
- `snapshot_carteira_id BIGINT NOT NULL` FK sem cascade;
- `moeda VARCHAR(3) NOT NULL`, check em `BRL`, `USD`;
- `patrimonio_atual NUMERIC(38,12) NOT NULL`, check `> 0` porque moedas sem posições são omitidas;
- `UNIQUE (snapshot_carteira_id, moeda)`.

A unique do pai já funciona como índice para futura leitura `Carteira + tempo`; a unique do filho cobre acesso por snapshot+moeda. Não criar índices redundantes. Rollback explícito remove primeiro filhos e depois pais. Hibernate permanece em `ddl-auto=validate`.

### 13. Proteger Carteira com snapshots contra exclusão

Decisão: sem cascade delete, `DELETE /carteiras/{id}` verifica Operações primeiro e snapshots depois, sob o lock curto de Carteira já utilizado. Se ambas existirem, preserva-se o erro vigente `409 / CARTEIRA_POSSUI_OPERACOES`; se somente snapshot existir, responde `409 / CARTEIRA_POSSUI_SNAPSHOTS`. Isso preserva todo histórico e evita que a FK resulte no handler genérico incorreto. Carteira sem Operações ou snapshots mantém 204.

### 14. Preparar consumo futuro sem implementar consulta

A futura `evolucao-patrimonial` poderá ler pais por `carteira_id, data_hora_snapshot ASC` e associar os componentes por moeda com fetch planejado, produzindo séries independentes BRL/USD. Não criar método público ou query especulativa agora; repositories desta change contêm apenas gravação e leituras estritamente necessárias aos testes/proteção de exclusão.

## Risks / Trade-offs

- [O instante do snapshot não torna as cotações síncronas] → documentar que a captura usa o estado local conhecido e timestamps de cotação podem divergir.
- [Carteira vazia gera crescimento sem valor monetário] → manter criação explícita; cada pai ainda registra informação temporal relevante.
- [Unique temporal pode colidir com Clock fixo ou resolução do banco] → mapear deterministicamente para 409 e testar H2/PostgreSQL.
- [REPEATABLE_READ varia entre bancos] → validar concorrência em H2 e PostgreSQL quando disponível; não prometer serialização global.
- [FK sem cascade altera elegibilidade de DELETE] → proteção explícita e erro específico, nunca depender da exceção genérica de integridade.
- [Resposta do POST contém URI sem GET atual] → tratar como identidade canônica reservada; leitura ficará para capability futura.

## Migration Plan

1. Adicionar changeSet 006 e incluí-lo no master depois do 005.
2. Criar modelo e repositories sem backfill; a tabela começa vazia.
3. Implementar criação transacional e contrato POST.
4. Acrescentar proteção explícita ao DELETE de Carteira.
5. Validar Liquibase/Hibernate em H2 e PostgreSQL disponível.
6. Rollback técnico remove primeiro `snapshot_carteira_moeda` e depois `snapshot_carteira`; não há migração de dados anteriores.
