## Why

O shell existente já oferece a infraestrutura de navegação e acessibilidade necessária, mas não expõe Operações no menu e mantém a sidebar sempre expandida no desktop. Esta primeira etapa melhora a descoberta das áreas e permite aproveitar o espaço de trabalho, alinhada aos RNF02, RNF07, RNF08 e RNF10 e à seção 17 do PRD.

## What Changes

- Evoluir o MainLayoutComponent existente, sem segundo shell.
- Ordenar a navegação: Dashboard (`/dashboard`), Carteiras (`/carteiras`), Operações (`/operacoes`), Ativos (`/acoes`), Corretoras (`/corretoras`).
- Permitir recolher/expandir a sidebar desktop com identificação acessível e rota ativa em ambos os estados; estado local, sem persistência necessária.
- Preservar drawer mobile, header, marca, seletor global, skip link, região principal, router-outlet, URLs, limites lazy e contratos existentes.
- Manter histórico global de Operações sem filtro implícito pelo seletor; preservar formulário, captura de Carteira, `carteiraId`, `origem` e retornos.
- Representar as mudanças como deltas; as specs consolidadas não serão editadas nesta preparação.

## Capabilities

### New Capabilities

Nenhuma. Os novos comportamentos pertencem ao shell existente.

### Modified Capabilities

- `frontend-application-shell`: modificar Shell principal da aplicação, Navegação principal entre áreas e Navegação responsiva; adicionar requisitos de recolhimento acessível e preservação dos contratos nesta evolução.
- `frontend-operation-management`: modificar Listagem cronológica e somente leitura exclusivamente para permitir acesso pelo menu, preservando seu escopo global.

## Impact

Implementação em NAVIGATION_ITEMS, MainLayoutComponent e respectivos testes, reutilizando CarteiraSelectorComponent, AppIconComponent, ícones locais e Angular Material/CDK. A validação ampliou o escopo autorizado exclusivamente para ajustes responsivos do header/seletor e correção de corte das listagens: CSS de Ações, Carteiras, Corretoras, coleções compartilhadas e apresentação de posições. Sem mudança de API, backend, banco, regra financeira ou dependência.

Fora do escopo: conteúdo do Dashboard, posições, página conceitual Carteira, alocação, nova rentabilidade, proventos, transferência de custódia, posição por corretora, configurações, autenticação, usuários, LGPD, outras classes de ativos, novos endpoints/cálculos, refatoração geral, correções não relacionadas e novas bibliotecas visuais, de estado ou gráficos. Ativos é somente um rótulo; não renomear features, services, models ou endpoints. Carteiras mantém nome e função atuais. PRD e README permanecem intactos.

A implementação e as correções responsivas foram autorizadas e estão em revisão final. A validação manual informada cobre apresentação mobile, cards, zoom aumentado, ausência do corte observado e funcionamento visual desktop com sidebar expandida/recolhida; demais verificações constam como pendentes em tasks.md. Commit, push, merge e arquivamento continuam não autorizados.

No fechamento, o usuário aceitou o crescimento de initial de aproximadamente 2,21 kB (~0,43%) e o warning de portfolio-positions, sem alterar budgets ou adicionar dependências; decisão detalhada em design.md. A melhoria de contexto de Operações será tratada separadamente em `contextualizar-historico-operacoes-por-carteira`, ainda não criada e sem implementação antecipada nesta change.
