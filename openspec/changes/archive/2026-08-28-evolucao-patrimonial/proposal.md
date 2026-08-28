## Why

O PRD exige acompanhar a evolução do patrimônio da Carteira, e a capability `portfolio-snapshot` já fornece a fonte histórica local, imutável e separada por moeda necessária para isso. Falta especificar uma consulta pública que exponha essa série persistida sem reconstruir o passado a partir de Operações, cotações históricas ou providers.

## What Changes

- Introduzir uma consulta read-only de evolução patrimonial baseada exclusivamente em `SnapshotCarteira` e `SnapshotCarteiraMoeda` já persistidos.
- Representar cada snapshot como uma observação temporal intradiária, preservando componentes BRL e USD independentes e snapshots vazios.
- Definir uma recuperação determinística e eficiente da série, sem N+1, efeitos colaterais, locks ou integrações externas.
- Manter separadas a evolução histórica persistida, o patrimônio atual calculado, o resumo atual, o resultado realizado e o histórico de cotações.
- Expor `GET /carteiras/{carteiraId}/evolucao-patrimonial` com wrapper da Carteira e pontos contendo `snapshotId`, `dataHoraSnapshot` e patrimônios por moeda.
- Retornar a série completa em ordem cronológica crescente, sem filtros ou paginação nesta primeira versão.
- Não introduzir métricas de rentabilidade, variações, agrupamentos, conversão cambial, automação de snapshots, backfill, gráficos ou frontend.

## Capabilities

### New Capabilities

- `portfolio-evolution`: consulta da série temporal do patrimônio histórico já capturado nos snapshots de uma Carteira.

### Modified Capabilities

Nenhuma. A change consome o modelo e a semântica promovidos por `portfolio-snapshot` sem alterar seus requisitos, e mantém inalteradas as demais capabilities atuais.

## Impact

- API: novo endpoint `GET /carteiras/{carteiraId}/evolucao-patrimonial`, sem alias `/evolucao`, filtros ou paginação.
- Backend futuro: extensão de `SnapshotCarteiraRepository`, novo service read-only, DTOs/mappers próprios e método enxuto em `CarteiraResource` ou organização equivalente compatível com o padrão atual.
- Persistência: leitura das tabelas `snapshot_carteira` e `snapshot_carteira_moeda` criadas pela migration 006; nenhuma migration é prevista.
- Arquitetura: consulta exclusivamente local, independente de `PosicaoService`, `AgregadorPosicoesPorMoeda`, `PatrimonioService`, `ResumoCarteiraService`, `Operacao`, `HistoricoCotacao` e providers externos.
- Compatibilidade: nenhum contrato existente é alterado e nenhuma criação, atualização ou exclusão de snapshot é adicionada.
