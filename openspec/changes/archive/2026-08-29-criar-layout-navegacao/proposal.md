## Why

A baseline Angular existente ainda apresenta apenas um estado técnico, sem um shell visual e destinos navegáveis que sustentem a evolução incremental das features. Esta change estabelece a estrutura visual, responsiva e acessível comum da aplicação antes que telas ou integrações de negócio sejam introduzidas.

## What Changes

- Introduzir Angular Material e CDK, alinhados exatamente à versão 22.1.4 da baseline, com tema Material 3 claro e identidade azul-petróleo/teal.
- Criar um shell principal coeso com toolbar “Gestão de Ações”, navegação lateral e área de conteúdo roteada.
- Disponibilizar navegação para Dashboard, Corretoras, Ações, Carteiras e Operações, com indicação perceptível da rota ativa.
- Adaptar a navegação para sidenav persistente em desktop e drawer overlay em viewports abaixo de 960px.
- Criar limites lazy independentes e placeholders estritamente estruturais para os cinco destinos, sem chamadas HTTP nem comportamento de negócio.
- Evoluir a restrição inicial de navegação da foundation para permitir rotas estruturais introduzidas por capabilities posteriores aprovadas, sem permitir antecipação de funcionalidade de negócio.
- Exibir uma página técnica de rota não encontrada dentro do shell, sem redirecionamento silencioso.
- Garantir requisitos mínimos de teclado, foco, contraste, ARIA, skip link e identificação do conteúdo principal.
- Preservar a política npm estrita, sem aprovações automáticas de lifecycle scripts nem dependências remotas de fontes ou ícones.

## Capabilities

### New Capabilities

- `frontend-application-shell`: Define a base visual Angular Material, o shell principal, a navegação responsiva e acessível, os destinos estruturais lazy-loaded e o tratamento de rotas desconhecidas.

### Modified Capabilities

- `frontend-application-foundation`: Esclarece que a proibição de antecipar rotas e features de negócio se aplica à baseline inicial, permitindo que capabilities posteriores aprovadas introduzam rotas estritamente estruturais sem transferir comportamento de negócio para a foundation.

## Impact

- **Frontend:** composição raiz, rotas, estilos globais, layout e novos destinos estruturais em `frontend/src/app/`.
- **Dependências:** inclusão exata de `@angular/material@22.1.4` e `@angular/cdk@22.1.4`, com atualização controlada do lockfile.
- **Testes:** novos testes do shell, navegação, responsividade, lazy loading, fallback e acessibilidade.
- **npm/CI:** manutenção de `npm@11.17.0`, `strict-allow-scripts` e da política `allowScripts`; qualquer novo lifecycle script deverá ser analisado antes de ser permitido.
- **Backend e contratos:** nenhum endpoint, DTO, service de domínio ou arquivo do backend será alterado ou consumido nesta change.
- **OpenSpec:** a restrição de navegação da `frontend-application-foundation` será refinada por delta `MODIFIED`, preservando a foundation como infraestrutura inicial e permitindo a evolução estrutural definida por `frontend-application-shell`.
