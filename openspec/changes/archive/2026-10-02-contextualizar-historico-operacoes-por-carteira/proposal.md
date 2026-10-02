## Why

O seletor global identifica uma Carteira, mas a página principal de Operações apresenta dados de todas as Carteiras. Esta evolução alinha o histórico ao contexto percebido pelo usuário e torna seleção e retorno reconstruíveis pela URL, conforme os objetivos de usabilidade do PRD (RNF02, RNF07, RNF08 e seção 17).

## What Changes

- **BREAKING (comportamento da interface):** `/operacoes` passa de histórico agregado para histórico da Carteira selecionada; não há quebra de API nem remoção de rota.
- Representar a seleção em `/operacoes?carteiraId={id}`, preservando precedência e fallback existentes, sem segundo seletor.
- Usar `OperacoesService.listarPorCarteira(id)` e `GET /carteiras/{id}/operacoes`; preservar `listar()` e `GET /operacoes`, sem opção “Todas as carteiras”.
- Invalidar dados anteriores e cancelar/ignorar respostas obsoletas ao trocar Carteira; distinguir estados de contexto, vazio e erros.
- Introduzir `origem=operacoes` nos links de cadastro/detalhe com retorno à Carteira de origem; preservar captura fixa, identidade da operação e contratos de Dashboard/Carteira.
- Garantir acesso direto, reload e voltar/avançar sem loops ou consultas duplicadas pela normalização da URL.

## Capabilities

### New Capabilities

Nenhuma: os novos requisitos pertencem às capabilities existentes.

### Modified Capabilities

- `frontend-operation-management`: listagem contextual, estados e isolamento de requisições, origem/retorno e preservação da leitura global na API/service.
- `frontend-application-shell`: contexto compartilhado, precedência e sincronização da seleção de Operações com URL/histórico, preservando os demais consumidores.

## Impact

Implementação futura restrita ao frontend: OperacoesListPageComponent, CarteiraNavigationService, operation-origin.ts, ajustes necessários de links/retorno em OperacaoFormPageComponent e OperacaoDetailPageComponent e testes correspondentes. Reutilizar CarteiraContextService, seletor do header, serviços, DTO e parser decimal existentes. Nenhum endpoint, backend, banco, cálculo, biblioteca ou budget será alterado.

Esta change sucede `evoluir-shell-navegacao-principal`, que preservou deliberadamente o histórico global. Seus artefatos e implementação não são reescritos nesta preparação. A nova semântica só entra em vigor após implementação autorizada desta evolução; sincronização futura deve respeitar essa ordem para não restaurar o contrato anterior.

Fora do escopo: histórico consolidado/auditoria na interface, filtros adicionais, autenticação, novas classes de ativos, reorganização de Dashboard, refatoração geral, remoção de métodos globais e alterações financeiras. Nesta etapa, somente artefatos OpenSpec; nenhuma task implementada, staging, commit, push, merge ou arquivamento.
