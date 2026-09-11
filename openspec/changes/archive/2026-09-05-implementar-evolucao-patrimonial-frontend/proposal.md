## Why

O backend já preserva snapshots patrimoniais manuais e expõe sua série histórica, mas o frontend ainda não permite consultá-la nem registrar novas observações. O Dashboard precisa incorporar essa evolução sem perder precisão decimal, fabricar continuidade temporal ou misturar BRL e USD.

## What Changes

- Adicionar ao Dashboard contextual uma seção modular de evolução patrimonial, mantendo `/dashboard?carteiraId={id}` e sem criar rota nova.
- Consumir exclusivamente `GET /carteiras/{carteiraId}/evolucao-patrimonial`, preservando todos os snapshots, sua ordem e `patrimonioAtual` como texto lossless.
- Exibir séries BRL e USD em gráficos SVG próprios e independentes, acompanhados de representação textual acessível.
- Tratar lacunas como ausência de observação, snapshots vazios como observações temporais válidas e séries com zero, um ou vários pontos sem inventar tendência, zero ou interpolação.
- Permitir criação exclusivamente manual por `POST /carteiras/{carteiraId}/snapshots`, sem body, com proteção contra double-submit, feedback acessível e reload contextual da evolução após sucesso.
- Fazer “Atualizar dados” recarregar também a evolução da seleção atual, sem criar snapshot; manter “Registrar snapshot” como única origem do POST.
- Reutilizar parser lossless, formatadores, erros normalizados, contexto de Carteira e padrões visuais/acessíveis existentes.
- Autorizar conversão de uma cópia decimal para `number` somente na geometria aproximada do SVG, preservando a string original como única fonte de labels, tooltip e alternativa textual.
- Manter fora do escopo backend, filtros, paginação, agregação, interpolação, métricas derivadas, automação de snapshots e novas dependências.

## Capabilities

### New Capabilities

- `frontend-portfolio-evolution`: consulta, visualização SVG acessível e responsiva da série patrimonial por moeda, além da criação manual de snapshots no contexto atual.

### Modified Capabilities

- `frontend-dashboard-management`: incorporar a seção modular de evolução ao contexto existente, incluir sua consulta no reload explícito e estender proteção contra respostas stale sem alterar rota ou demais funcionalidades.

## Impact

- Frontend Angular: Dashboard, models e service de evolução/snapshot, componente SVG, alternativa textual, estados e testes relacionados.
- APIs backend existentes e inalteradas: `GET /carteiras/{carteiraId}/evolucao-patrimonial` e `POST /carteiras/{carteiraId}/snapshots`.
- Reutilização de `parseLosslessJson`, formatadores financeiros, formatação temporal local, normalização de erros, feedback, PageHeader e tokens responsivos atuais.
- Nenhuma alteração em backend, migrations, contratos HTTP, shell/rotas, bibliotecas ou dependências.
