## Context

Ver proposal.md. O componente limpa updateError no início de cada tentativa; derivar o aviso desse erro o ocultaria antes de obter uma cotação nova.

## Goals / Non-Goals

Manter o aviso durante a recuperação, sem alterar DTOs, contratos ou cálculos.

## Decisions

Usar um sinal booleano local, ativado na falha e limpo no sucesso. Reutilizar FeedbackAlertComponent com variante warning. Não derivar o aviso de updateError, pois esse estado também controla a mensagem da tentativa atual.

## Risks / Trade-offs

Estado adicional pode ficar incoerente → testar falha, nova tentativa pendente e sucesso, além do carregamento inicial sem aviso.
