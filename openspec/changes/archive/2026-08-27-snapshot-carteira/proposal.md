## Why

O produto precisa preservar fotografias imutáveis do patrimônio conhecido de uma Carteira para fundamentar uma futura evolução patrimonial. As consultas atuais calculam apenas o estado presente e, por isso, não preservam pontos temporais depois que Operações ou cotações mudam.

## What Changes

- Introduzir criação explícita de snapshot atual por `POST /carteiras/{carteiraId}/snapshots`, sem body, filtros, chamadas externas ou reconstrução de data passada.
- Persistir um snapshot pai com instante UTC da captura e zero ou mais componentes de patrimônio por moeda, contendo somente `moeda` e `patrimonioAtual`.
- Reutilizar uma única consolidação de `PosicaoService` e uma única execução do `AgregadorPosicoesPorMoeda`, preservando a fórmula, a política numérica e a separação BRL/USD de `portfolio-valuation`.
- Tornar os snapshots append-only: sem atualização, recálculo posterior, exclusão pública ou deduplicação por conteúdo.
- Manter a criação e todos os componentes monetários em uma única transação `REPEATABLE_READ`, sem provider ou lock pessimista adicional.
- Preparar persistência temporal eficiente para consumo futuro por `evolucao-patrimonial`, sem expor consulta de snapshots nesta change.
- Proteger explicitamente a exclusão de Carteira que já possua snapshot, evitando cascade delete ou erro de integridade não padronizado.
- Evoluir o schema apenas por um novo changeSet 006, preservando os changeSets 001–005 e `hibernate.ddl-auto=validate`.

## Capabilities

### New Capabilities

- `portfolio-snapshot`: criação explícita, persistência imutável e consistência temporal de snapshots patrimoniais atuais por moeda.

### Modified Capabilities

- `portfolio-deletion`: tornar Carteira com snapshot inelegível para exclusão física, preservando o histórico patrimonial sem cascade delete.

## Impact

- API: novo `POST /carteiras/{carteiraId}/snapshots`; nenhum endpoint público de leitura, atualização ou exclusão de snapshots.
- Domínio/persistência planejados: `SnapshotCarteira`, `SnapshotCarteiraMoeda`, repositories correspondentes e changeSet 006.
- Aplicação planejada: `SnapshotCarteiraService`, mapper/DTOs de criação e integração mínima em `CarteiraResource`.
- Reuso: `PosicaoService` e `AgregadorPosicoesPorMoeda`; `PatrimonioService` permanece independente.
- Erros planejados: reutilização dos erros financeiros atuais e conflitos específicos `SNAPSHOT_CARTEIRA_DUPLICADO` e `CARTEIRA_POSSUI_SNAPSHOTS`.
- Não há mudança em Operações, histórico de cotação, posição, patrimônio atual, resumo ou resultado realizado.
