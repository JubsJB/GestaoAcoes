# Verificação curta da movimentação entre páginas

## Encerramento da investigação direcionada com instância real — 2026-11-14

Última rodada autorizada pelo usuário, sem novas séries semelhantes. Verificação HTTP somente de status, sem corpos, credenciais ou mutações: 127.0.0.1:4200/login recusou conexão; 127.0.0.1:8080/ respondeu 401; 127.0.0.1:8443/ recusou conexão. A restrição inicial EACCES foi distinguida da disponibilidade real repetindo apenas essas sondagens fora do sandbox. Backend real disponível, mas exige autenticação; frontend não estava disponível nas portas locais documentadas verificadas.

Inspeção de AuthConfig, SecurityWebConfig, configuração dev e docs/AUTENTICACAO.md: perfil dev permite HTTP local quando configurado, mas não dispensa autenticação. /auth/me exige sessão e consultas financeiras exigem ADMIN/USER; Usuários exige ADMIN. Não foi encontrado mecanismo já autorizado que autentique um perfil headless novo sem credenciais. A entrada frontend /__dev/amostra-a é amostra isolada sem shell/API/sessão real, inadequada para esta captura.

Não iniciado Chrome para esta rodada, pois o impedimento de autenticação já é suficiente e abrir o login não produziria medições das páginas protegidas. Não solicitado, copiado, lido ou exportado segredo/cookie/token; nenhum login, atividade, bootstrap, escrita no banco ou atalho de autenticação executado. Nenhuma configuração ou código de produção alterado. Não instaladas dependências. Não repetidos testes/build, pois somente este registro documental mudou; OpenSpec estrito validado.

Resultado final: sacudida relatada continua não reproduzida com dados reais e não declarada resolvida. Evidência anterior de fixtures permanece limitada conforme seção seguinte. Investigação encerrada conforme prioridade do usuário, preservando scrollbar-gutter e demais comportamentos. Sem nova correção proposta na ausência de causa concreta; sem concluir tarefas transversais ou realizar commit/push/merge/arquivamento.

## Estado atual: problema reaberto — captura transitória de 2026-11-14

O usuário confirmou que a aplicação real ainda apresenta sacudida lateral após scrollbar-gutter:stable. A medição anterior identificou e corrigiu somente a variação de scrollbar; NÃO demonstra resolução integral do problema. A conclusão anterior de estabilidade fica limitada ao ambiente de teste. Nenhuma nova correção de produção aplicada nesta investigação.

Reutilizado `target/navigation-diagnostic/measure.mjs`, ampliado com modo `--transient`: Chrome headless isolado, cliques por CDP Input.dispatchMouseEvent (eventos reais, em vez de HTMLElement.click), coleta iniciada antes do clique e requestAnimationFrame durante 1,6s. Capturados sidebar/workspace/main/container (x, largura, clientWidth, scrollLeft, scrollTop, marginLeft, transform, transition), até 35 elementos internos por identidade DOM persistente, MutationObserver de class/style e eventos de transição/animação. Fixtures de leitura com atraso de 180ms incluem estados assíncronos. A fixture temporal de sessão foi corrigida apenas no script para manter expiração constante entre /me e atividade; autenticação do produto intacta.

Evidência completa em `target/navigation-diagnostic/transient.json`: 24 navegações, 2356 frames, larguras desktop 1440/1920, sidebar expandida/recolhida, Dashboard/Carteiras/Ações/Usuários. Doze navegações começaram comprovadamente em scrollTop=480; demais em zero. Todas terminaram na rota solicitada. Variação transitória máxima de x/largura/clientWidth/scrollLeft dos quatro elementos do shell: **0px**. Nenhum transitionrun/start/end de margin-left ou margin-right. Mudanças de classes observadas no menu ativo e nos estados assíncronos não mudaram a geometria do shell. Eventos transform/opacity do ripple e animações de carregamento foram preservados.

A medição por identidade detectou um caso interno que a investigação anterior não acompanhava: h1 do Dashboard mudou 60px por um frame durante a inicialização da classe page-header--with-icon, sem mudança no container. Repetição focada em `target/navigation-diagnostic/transient-probe.json`: 8 navegações, 788 frames; em três ocorrências, h1 inicialmente em x=416,5 com textLength=0 e depois em x=476,5 com textLength=9. Portanto, o deslocamento capturado ocorre no heading ainda vazio; o texto Dashboard aparece já na posição final. Isso não confirma movimento visível do texto nem explica sacudida em múltiplos módulos. Não foi alterado PageHeader ou qualquer layout aprovado por essa observação.

Limitações: aplicação production real com dados/sessão de teste locais; não reproduz conteúdo e timing exatos do backend real, renderização gráfica do Chrome pessoal, extensões, zoom e configuração física do ambiente do vídeo. Amostragem DOM em rAF mede layout; não certifica ausência de artefato de composição/pintura na tela física. A correção atual de scrollbar permanece. Sem nova alteração, não há comparação antes/depois de uma segunda correção a apresentar. O problema permanece **aberto**, sem promessa de resolução a partir desses zeros.

Validação direcionada: 19 testes existentes de MainLayoutComponent aprovados; build production atualizado aprovado, mesmos chunks relevantes da aplicação medida e initial 503,01kB; budgets e configuração/limiares de cobertura intactos, avisos de tamanho preservados. OpenSpec estrito validado. Sem suíte/cobertura global, alterações de backend/autenticação/financeiro, novos testes artificiais de geometria em jsdom, conclusão transversal ou operações Git.

Próxima evidência necessária: executar esta mesma captura na instância real em navegador isolado, usando a URL e o login normais, sem exigir medições manuais de DevTools. A diferença de dados/renderização deve ser reproduzida antes de corrigir qualquer transição. O modo atual usa fixtures e não deve ser apresentado como captura do ambiente real do usuário.

## Resultado automatizado — 2026-11-14

Causa reproduzida e correção implementada após autorização do usuário: aparecimento/desaparecimento da scrollbar clássica, sem mudança de margem ou geometria da sidebar. Projeto sem Playwright/Cypress/Puppeteer instalados. Utilizado Chrome headless já instalado, perfil temporário exclusivo, Node 24/WebSocket/CDP nativos e servidor HTTP restrito a 127.0.0.1. Nenhuma dependência instalada; nenhum acesso ao perfil pessoal, backend real ou dados reais. Aplicação production real servida de `frontend/dist/frontend/browser`; fixtures descartáveis de sessão ADMIN, uma carteira e 30 ações/usuários são respostas apenas do servidor de diagnóstico, sem mudança na autenticação da aplicação.

Primeira tentativa restrita não disponibilizou CDP em 15s. Execução isolada fora da restrição permitiu a medição, sem controlar Chrome pessoal. Script temporário e evidências locais ignorados pelo Git:

- `target/navigation-diagnostic/measure.mjs`
- `target/navigation-diagnostic/before.json`
- `target/navigation-diagnostic/after.json`

Medidos frames via requestAnimationFrame por 1,2s a partir do clique no menu, incluindo amostra anterior ao clique. Viewport desktop 1440×900. Sequência Carteiras → Ações → Carteiras → Usuários → Dashboard → Carteiras; recolhimento manual e nova sequência Usuários → Carteiras. Valores mínimo–máximo:

| Medida | Antes expandida | Depois expandida | Antes recolhida | Depois recolhida |
| --- | --- | --- | --- | --- |
| sidebarX | 0 | 0 | 0 | 0 |
| sidebarWidth | 240 | 240 | 80 | 80 |
| marginLeft | 240 | 240 | 80 | 80 |
| mainClientWidth | 1185–1200 | 1185 | 1345–1360 | 1345 |
| scrollbarWidth/reserva | 0–15 | 15 | 0–15 | 15 |
| containerX | 240 | 240 | 144,5–152 | 144,5 |
| containerWidth | 1185–1200 | 1185 | 1216 | 1216 |

A variação de 15px da largura útil deslocava 7,5px o container centralizado quando atingia max-width. Com sidebar expandida, alterava a largura da composição, embora sua borda esquerda permanecesse fixa. Margens e sidebar constantes afastam autosize/transições como origem neste cenário.

Correção única em `main-layout.component.scss`: `.main-content { scrollbar-gutter: stable; }`. Preservados overflow:auto, rolagem vertical, autosize, transições do Material e lógica de recolhimento. Em scrollbar clássica, o espaço passa a ser reservado mesmo em páginas curtas; em scrollbar sobreposta, não é criada reserva desnecessária. Antes/depois sem overflow horizontal no container de rolagem.

Validação automática posterior em emulação mobile 360×800 e 320×800: drawer abriu e fechou após navegação, marginLeft=0, containerX=0, larguras úteis respectivamente 360/320, scrollbar sobreposta/reserva=0, rolagem vertical presente e overflow horizontal ausente. São medidas em Chrome headless, não certificação de todos os dispositivos físicos.

Regressão: 19 testes existentes de MainLayoutComponent aprovados; sem teste artificial de CSS em jsdom. Build production aprovado; initial 503,01kB. Budgets intactos, com avisos já existentes; aviso do estilo do shell aumentou de 88 para 126 bytes acima de 4kB, abaixo do limite de erro 8kB. Configuração e limiares de cobertura não alterados; suíte/cobertura global não repetidas. Conferência visual real do usuário continua pendente; nenhuma tarefa transversal encerrada. Sem commit, push, merge ou arquivamento.

Para repetir sem DevTools após build: `node target/navigation-diagnostic/measure.mjs after.json`, na raiz do repositório. O script encerra navegador/servidor ao terminar e reprova variações maiores que 0,5px nas medidas desktop, overflow horizontal e regressão de abertura/fechamento mobile. Não é nova dependência nem suíte permanente.

## Roteiro original — referência histórica

Preparada em 2026-11-14. Sem alteração do layout compartilhado. Causa ainda não confirmada em navegador real; o inventário conectado não ofereceu uma aba autenticada do sistema nesta execução.

Em desktop, manter zoom e largura da janela constantes. Usar uma página curta e uma longa já disponíveis, sem salvar ou excluir dados. No Console do DevTools, executar a função abaixo uma vez; chamar `medirLayout()` antes da navegação, durante o movimento observado e depois de estabilizar. Não coleta conteúdo, credenciais, cookies ou armazenamento.

```js
function medirLayout() {
  const sidebar = document.querySelector('.app-sidenav');
  const workspace = document.querySelector('.workspace');
  const main = document.querySelector('.main-content');
  const container = document.querySelector('.main-content__container');
  if (!sidebar || !workspace || !main || !container) return 'Shell indisponível';
  const side = sidebar.getBoundingClientRect();
  const content = workspace.getBoundingClientRect();
  const inner = container.getBoundingClientRect();
  const style = getComputedStyle(workspace);
  return {
    viewport: innerWidth,
    sidebarX: side.x, sidebarWidth: side.width,
    workspaceX: content.x, workspaceWidth: content.width,
    marginLeft: style.marginLeft, marginRight: style.marginRight,
    mainClientWidth: main.clientWidth,
    mainOffsetWidth: main.offsetWidth,
    scrollbarWidth: main.offsetWidth - main.clientWidth,
    hasVerticalScroll: main.scrollHeight > main.clientHeight,
    containerX: inner.x, containerWidth: inner.width
  };
}
```

Interpretação:

- Sidebar e margens constantes, mas scrollbar/clientWidth variáveis: candidata mínima `scrollbar-gutter: stable` apenas em `.main-content`. Scrollbar sobreposta pode medir zero; nesse caso não atribuir deslocamento horizontal à perda de largura sem outra evidência.
- Sidebar constante e margem variável: registrar valores intermediários/finais e conferir o recálculo do Material/autosize. A transição de margens de 400 ms pode tornar a variação visível, mas não comprova sua origem.
- sidebarX/sidebarWidth variáveis sem clique de recolhimento nem mudança de breakpoint: investigar a geometria real da sidebar antes de corrigir scrollbar ou margens.
- Tudo constante: não aplicar correção no shell; verificar se o deslocamento pertence apenas à composição da página.

Repetir somente a comparação com a sidebar manualmente recolhida. Depois conferir abertura/fechamento mobile, sem confundir sua animação esperada com navegação desktop. Preservar autosize até evidência de que precisa mudar. Apresentar os valores e a proposta ao usuário antes de implementar qualquer ajuste no shell. Geometria não é certificada pelos testes jsdom.
