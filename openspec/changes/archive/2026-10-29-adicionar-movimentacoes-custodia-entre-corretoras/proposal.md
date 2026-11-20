## Why

A distribuição atual informa quantidades por corretora, mas não permite registrar mudanças de custódia ou atribuir o saldo sem corretora. O MVP permitirá controle interno auditável dessas movimentações, protegendo saldos históricos sem alterar a contabilidade consolidada.

## What Changes

- Registrar `TRANSFERENCIA` entre corretoras distintas e `ATRIBUICAO` do grupo "Sem corretora informada" para corretora cadastrada, dentro da mesma carteira.
- Registrar estorno próprio, vinculado e único, neutralizando a original desde sua posição efetiva; motivo opcional e nenhuma substituição automática.
- Buscar a primeira posição cronológica válida no dia, inclusive entre custódias, persistir chave estável e preservar ordens financeiras existentes.
- Comparar históricos antes/depois por identidade e posição: `max(0,-saldoDepois) <= max(0,-saldoAntes)`; proteger custódias ativas e permitir legado independente inalterado.
- **BREAKING:** novas vendas exigirão saldo consolidado e saldo do grupo da corretora, inclusive quando nulo e mesmo sem custódias registradas. Operações legadas não serão rejeitadas como novas vendas.
- **BREAKING:** evoluir os diagnósticos do endpoint atual de distribuição para identidade genérica de evento; atualizar backend e parser Angular de maneira coordenada, sem versão paralela.
- Histórico de custódia separado do financeiro, com filtros por natureza/situação, datas de efetividade/registro e vínculo de estorno.
- Persistência aditiva de movimentações e estornos, com lock por carteira e unicidade de estorno. Nenhum saldo materializado.

## Capabilities

### New Capabilities

- `custody-movement-registration`: cadastro, precisão, data, posição estável e proteção histórica de transferências/atribuições.
- `custody-movement-reversal`: estorno auditável, neutralização retroativa, integridade e atomicidade.
- `custody-movement-query`: histórico unificado, detalhe, filtros e ordenação de auditoria.

### Modified Capabilities

- `operation-registration`: saldo por grupo em novas vendas e validação diferencial para novas operações.
- `portfolio-broker-distribution`: custódias ativas no replay e evidências genéricas do histórico efetivo.
- `frontend-operation-management`: mensagens dos novos conflitos quantitativos, com regras autoritativas no backend.
- `frontend-portfolio-management`: formulário de custódia, histórico, estorno e consumo coordenado da distribuição.
- `api-error-handling`: códigos e detalhes estáveis de conflitos de custódia, vendas e concorrência.
- `api-documentation`: contratos e exemplos das novas rotas e dos diagnósticos evoluídos.
- `portfolio-deletion`: proteger auditoria de custódia contra exclusão física da carteira.

## Impact

Backend: OperacaoPersistenceService.persistir, distribuição/calculadora/mapper/DTOs, CarteiraService.excluir e novos componentes de custódia. CalculadoraPosicao e enum COMPRA/VENDA permanecem financeiros. Frontend: detalhe da carteira, dialogs, histórico próprio, parser lossless e mensagens no formulário financeiro. Schema: futura migration aditiva (prefixo 008 disponível na inspeção); não alterar 004 ou registros legados. Regressão: contratos REST/OpenAPI, precisão, regras financeiras, concorrência e integridade.

PRD RF16/RF17, README e AGENTS.md atualmente excluem transferência/bloqueio por corretora ou generalizam toda movimentação como operação; a etapa de implementação autorizada deverá harmonizá-los. Changes arquivadas e specs canônicas permanecem intactas nesta etapa.

Fora do escopo: custódia certificada, B3/corretoras, extratos, conciliação, transferência entre carteiras, edição/exclusão de custódia, saneamento automático, reposicionamento, gráficos por corretora, autenticação e redesign.

Esta entrega autoriza somente planejamento dentro desta change. Todas as tasks permanecem pendentes; execução, migrations, banco, Git e archive exigem autorização futura.
