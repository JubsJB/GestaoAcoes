## Why

O frontend possui tokens e componentes reutilizáveis, mas autenticação, administração e páginas financeiras apresentam diferentes níveis de acabamento e organização. Esta change propõe uma identidade clara e minimalista, com azul como marca, e padronização completa da apresentação, preservando os comportamentos aprovados.

## What Changes

- Substituir a direção institucional verde/oliva pela direção azul em marca, navegação e ações; reservar verde/vermelho principalmente aos resultados e estados semânticos, com contraste validado.
- Consolidar tema Material, tipografia do sistema, superfícies claras, espaçamento, coleções, formulários, detalhes, modais e feedback.
- Apresentar login e definição/redefinição de senha com composição dividida no desktop e uma coluna no mobile, mantendo usuário/senha e código entregue pelo administrador.
- Organizar o dashboard em módulos inspirados em bento grid, destacando gráficos e indicadores existentes, com BRL/USD separados.
- Padronizar administração de usuários e todas as superfícies de carteiras, ativos, corretoras, operações e custódia.
- Prever baseline, aprovações visuais antes da propagação, acessibilidade e equivalência funcional como condições de aceite.
- Nesta execução, entregar somente planejamento; implementação depende de nova autorização após revisão.

## Capabilities

### New Capabilities

- `frontend-user-administration`: apresentação administrativa de lista, criação, detalhe, comandos existentes e códigos de uso único, sem regras de backend ou novas buscas/filtros.

### Modified Capabilities

- `frontend-visual-experience`: reutilizar a capability global existente; substituir os requisitos de identidade oliva por azul e acrescentar cobertura integral, composição de autenticação/dashboard e aprovação visual progressiva.
- `frontend-authentication`: reconciliar o requisito que limita a apresentação ao layout anterior, preservando integralmente a autorização e os fluxos de sessão.
- `frontend-stock-management`: refinamento de 6.3 autorizado em 2026-11-12; restringir o convite de cadastro contextual a ADMIN e manter o aviso de preservação confirmada durante nova tentativa, com mensagens contextualizadas e erro completo somente interno. Sem mudança dos contratos ou permissões do backend.
- `frontend-broker-management`: 6.4 autorizada em 2026-11-12; consulta integrada e identidade C2, convite ao cadastro contextual somente para ADMIN, mensagens preservadas com erro normalizado integral interno e sem renderização indiscriminada de detalhes técnicos. Cadastro por CNPJ, confirmação de situação não ativa e integrações backend preservados.

## Impact

Implementação futura restrita à apresentação em `frontend/src/styles*`, shell, componentes compartilhados e páginas Angular. Sem alteração de backend, endpoints, DTOs, métodos, payloads, gatilhos HTTP, JWT/cookies/CSRF, permissões, sessões, regras financeiras, custódia, precisão, datas, moedas, rotas ou fluxos. As demais capabilities funcionais permanecem vigentes e são referências de compatibilidade, sem deltas cosméticos redundantes.

Não adicionar cadastro público, login social, recuperação por e-mail, tema escuro, métricas não aprovadas, filtros, ordenação, paginação, chamadas de enriquecimento ou novas dependências. No refinamento administrativo posteriormente autorizado, o resumo total/ativos/bloqueados usa somente a resposta completa da listagem existente, conforme o delta frontend-user-administration. Não elevar budgets nem aceitar crescimento do initial bundle sobre baseline medido na mesma configuração. A paleta e a composição detalhada ainda dependem de contraste e aprovação visual.

Apenas os arquivos desta change serão criados nesta rodada. README, teste do formatter e ZIP preexistentes permanecem preservados, sem staging, commits ou operações de integração Git.
