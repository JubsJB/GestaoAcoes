## Context

Veja `proposal.md` para a motivação e `specs/portfolio-evolution/spec.md` para o contrato normativo. A migration 006 já fornece `snapshot_carteira(carteira_id, data_hora_snapshot)` e `snapshot_carteira_moeda(snapshot_carteira_id, moeda, patrimonio_atual)`, com unicidade por Carteira+timestamp e snapshot+moeda. As entidades são unidirecionais e lazy, sem coleção de filhos no pai.

O fluxo de escrita atual captura o timestamp em UTC, usa `PosicaoService` e `AgregadorPosicoesPorMoeda`, e persiste pai e filhos atomicamente. A consulta de evolução deve ler o resultado histórico dessa escrita, não repetir o fluxo financeiro. O projeto não possui contrato público de paginação: listagens atuais devolvem `List`, aplicam ordem determinística e, em suas primeiras fatias, explicitamente dispensam filtros e paginação.

O PRD lista o candidato inicial `GET /carteiras/{id}/evolucao` e permite refinamento durante a modelagem. A decisão aprovada adota exclusivamente `/carteiras/{carteiraId}/evolucao-patrimonial`, por descrever o recurso e evitar ambiguidade com futuras evoluções de rentabilidade ou resultados.

## Goals / Non-Goals

**Goals:**

- Expor snapshots já persistidos como série temporal intradiária, fiel e determinística.
- Preservar snapshots vazios e componentes monetários independentes.
- Definir uma leitura local eficiente, sem N+1 e sem acoplamento aos cálculos atuais.
- Manter um contrato de domínio/API neutro para futuro consumidor Angular.

**Non-Goals:**

- Recalcular ou reconstruir patrimônio passado.
- Criar snapshots, backfill, automação ou alterar o schema.
- Calcular rentabilidade, variação, caixa, resultado, câmbio, agrupamentos ou indicadores gráficos.
- Expor CRUD público de snapshots ou implementar frontend.

## Decisions

### D1 — Endpoint final

Adotar exclusivamente `GET /carteiras/{carteiraId}/evolucao-patrimonial`, sem alias `/evolucao`. O nome explícito evita ambiguidade com rentabilidade, resultados e outras evoluções futuras. A alternativa curta do PRD foi rejeitada no refinamento da API.

### D2 — Estrutura pública e `snapshotId`

Usar wrapper `{ carteiraId, pontos }`. Cada ponto contém somente `snapshotId`, `dataHoraSnapshot` e `patrimonios`; cada componente contém somente `moeda` e `patrimonioAtual`. `snapshotId` fornece identidade e rastreabilidade, mas não cria endpoint individual ou mutabilidade. Lista nua ou omissão do ID foram rejeitadas.

### D3 — Ordenação

Ordenar a projeção por `dataHoraSnapshot ASC`, `snapshotId ASC`, `moeda ASC`. A série pública vai do ponto mais antigo ao mais recente, e os componentes seguem moeda crescente. Ordem descendente, agrupamento e deduplicação foram rejeitados.

### D4 — Filtros temporais

Não aceitar filtros nesta primeira versão. Parâmetros de início, fim, ano, mês ou período não serão declarados. Filtros futuros deverão ser especificados separadamente; a preferência preliminar por UTC e limites inclusivos não integra o contrato atual.

### D5 — Paginação

Retornar todos os snapshots da Carteira sem `page`, `size`, offset, limit ou cursor. A simplicidade é compatível com snapshots manuais e com as primeiras listagens do projeto. Se o volume justificar evolução futura, preferir cursor por `(dataHoraSnapshot, snapshotId)` a offset.

### D6 — Estratégia de repository/fetch

Estender `SnapshotCarteiraRepository` com uma única query de projeção plana ancorada em `Carteira`, usando `LEFT JOIN` para snapshots e seus componentes. Projetar somente `snapshotId`, `dataHoraSnapshot`, `moeda` e `patrimonioAtual`. Resultado sem linha significa Carteira inexistente; linha com snapshot nulo significa Carteira existente sem snapshots; snapshot com moeda/valor nulos significa ponto vazio. Agrupar as linhas no service por identidade do snapshot, descartando somente o marcador de Carteira sem snapshots e mantendo a ordem da query. Não adicionar coleção às entidades, eager fetch, repository paralelo, query separada de existência ou queries subsequentes.

### D7 — Política transacional e concorrência

Executar o service com `@Transactional(readOnly = true)` e isolation padrão, sem configuração explícita de `REPEATABLE_READ`. Uma única statement observa visão consistente: criação não confirmada não aparece; criação já confirmada pode aparecer; pai e filhos nunca aparecem parcialmente porque a escrita é atômica. Não usar locks, `@Version` ou coordenação de Carteira. Abandono da query única exige revisão prévia desta decisão.

### D8 — Códigos de erro

Inferir a existência da Carteira pela própria projeção ancorada: resultado sem linhas devolve `404` centralizado; linha marcadora sem snapshot devolve sucesso com `pontos=[]`. Snapshot sem filhos também é sucesso com `patrimonios=[]`. Não adicionar query de existência ou códigos específicos; falhas inesperadas continuam no tratamento global.

### Fonte, precisão e independência

Ler somente os valores históricos de `SnapshotCarteira` e `SnapshotCarteiraMoeda`. Projetar `patrimonioAtual` como `BigDecimal` exatamente como persistido em `NUMERIC(38,12)`, sem cálculo, `MathContext`, arredondamento ou conversão para `double`. `Operacao`, `HistoricoCotacao`, `Acao.cotacaoAtual`, `PosicaoService`, `AgregadorPosicoesPorMoeda`, `PatrimonioService`, `ResumoCarteiraService`, resultado realizado e providers permanecem fora do fluxo.

### Amostragem manual e granularidade

Preservar todos os snapshots, inclusive vazios, iguais em instantes diferentes e múltiplos no mesmo dia. A série reflete somente capturas efetivamente realizadas e pode ser vazia, irregular ou esparsa. Não interpolar, preencher lacunas, criar pontos durante GET ou introduzir automação.

## Risks / Trade-offs

- **[Série sem paginação pode crescer indefinidamente]** → documentar a limitação e planejar cursor por `(dataHoraSnapshot,id)` quando houver requisito de volume.
- **[Uma projeção plana repete colunas do pai por componente]** → no máximo duas moedas atuais por snapshot; o custo é limitado e evita N+1.
- **[Snapshot manual produz série irregular e possivelmente esparsa]** → expor fielmente os pontos existentes; automação e backfill permanecem evoluções futuras explícitas.
- **[Consumidor pode confundir evolução patrimonial com rentabilidade]** → contrato contém somente valores patrimoniais persistidos e documentação explicita a distinção.
- **[Adicionar filtros ou paginação futuramente altera o contrato]** → especificar a evolução em change própria e preservar o endpoint atual conforme compatibilidade aprovada.

## Migration Plan

Não há migration de banco nem transformação de dados. A implementação adicionará somente leitura e contrato REST sobre o schema 006. Rollback consiste em remover o endpoint e componentes de leitura, preservando todos os snapshots.
