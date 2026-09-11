## 1. Tema e tokens

- [x] 1.1 Substituir as paletas cyan/azure do tema Material 3 pela identidade oliva aprovada, preservando tema claro, system fonts, density e strong focus indicators.
- [x] 1.2 Definir tokens globais para brand-primary, brand-secondary, accent, surfaces, textos e bordas com os valores e derivados documentados no design.
- [x] 1.3 Definir tokens distintos para success, financial-positive, negative, error e warning, sem atribuir significado financeiro à cor institucional.
- [x] 1.4 Verificar contraste das combinações de foreground/background e ajustar apenas os papéis de uso que não atendam à acessibilidade.

## 2. Shell

- [x] 2.1 Aplicar ao shell as superfícies, tipografia e hierarquia visual transversais sem alterar seus fluxos ou lazy loading.
- [x] 2.2 Preservar Corretoras e Ações funcionais, os placeholders futuros e o wildcard/NotFound durante o refinamento.

## 3. Toolbar

- [x] 3.1 Refinar a toolbar com verde oliva escuro, texto/ícones claros, sombra discreta e alinhamento ao container.
- [x] 3.2 Preservar o nome “Gestão de Ações”, o botão mobile e alturas coerentes de aproximadamente 64px no desktop e 56px no compacto.
- [x] 3.3 Validar contraste, foco e operação por teclado dos controles da toolbar.

## 4. Sidebar

- [x] 4.1 Aplicar superfície clara, espaçamento vertical refinado e hover discreto à navegação lateral.
- [x] 4.2 Implementar item ativo em sage claro com indicador adicional à cor, preservando aria-current.
- [x] 4.3 Preservar a largura estrutural de 16rem e o fechamento compacto já existente.

## 5. Container de conteúdo

- [x] 5.1 Implementar container centralizado com max-width de 76rem e padding responsivo de 2rem/1rem conforme o design.
- [x] 5.2 Verificar que o container limita dispersão em telas largas sem introduzir overflow ou quebrar páginas compactas.

## 6. PageHeader compartilhado

- [x] 6.1 Criar PageHeader compartilhado sem lógica de negócio, com h1, descrição opcional e área projetável de ação.
- [x] 6.2 Implementar layout lado a lado no desktop e empilhamento compacto, permitindo ação full-width quando apropriado.
- [x] 6.3 Garantir wrap, line-height e escala responsiva para títulos e nomes longos sem truncamento obrigatório.
- [x] 6.4 Adotar PageHeader nas páginas de Corretoras e Ações sem alterar navegação ou comportamento de negócio.

## 7. FeedbackAlert compartilhado

- [x] 7.1 Criar FeedbackAlert com variantes success, info, warning e error e conteúdo textual contextual.
- [x] 7.2 Mapear role status/alert e aria-live conforme propósito, incluindo indicador não dependente apenas de cor.
- [x] 7.3 Suportar message e details do StandardError sem criar um segundo formato de erro.
- [x] 7.4 Posicionar feedback relevante após PageHeader e preservar validações locais de campo e feedback transitório complementar.

## 8. Padrões estruturais

- [x] 8.1 Criar somente os partials globais mínimos de tema, tokens e padrões estruturais definidos no design.
- [x] 8.2 Eliminar duplicações equivalentes entre Corretoras e Ações sem mover estilos de domínio para o escopo global.
- [x] 8.3 Evitar ::ng-deep, seletores internos frágeis do Material e !important; documentar qualquer exceção inevitável.
- [x] 8.4 Padronizar estrutura visual dos estados loading, empty, search-not-found, technical error e external error sem componente prematuro.

## 9. Corretoras — lista e busca

- [x] 9.1 Refinar lista, busca e estados de resultado de Corretoras segundo os padrões de página, cards e ações.
- [x] 9.2 Classificar como ausência local somente StandardError com status 404 e code null, preservando a coleção e o CNPJ pesquisado.
- [x] 9.3 Exibir “Corretora não cadastrada”, o CNPJ formatado e o CTA “Cadastrar corretora” no resultado local ausente.
- [x] 9.4 Manter erros 404 com code preenchido no tratamento contextual do provider/StandardError.
- [x] 9.5 Preservar ausência de request durante digitação e restauração da coleção sem GET ao limpar a busca.

## 10. Corretoras — cadastro e prefill

- [x] 10.1 Navegar pelo CTA com NavigationExtras.info contendo somente o CNPJ pesquisado.
- [x] 10.2 Validar o info transitório no cadastro e preencher o controle apenas quando o CNPJ tiver tipo e formato compatíveis.
- [x] 10.3 Garantir formulário vazio em acesso direto, refresh ou info inválido, sem query params, storage ou store global.
- [x] 10.4 Preservar submissão manual, validações, contratos HTTP e confirmação de situação cadastral não ativa.

## 11. Corretoras — detalhe e datas

- [x] 11.1 Refinar a hierarquia do detalhe de Corretoras, mantendo dados completos e nomes empresariais longos legíveis.
- [x] 11.2 Aplicar o formatter compartilhado de OffsetDateTime no detalhe sem alterar o DTO recebido.
- [x] 11.3 Confirmar apresentação pt-BR em timezone local no padrão dd/MM/yyyy às HH:mm.

## 12. Ações — lista e busca

- [x] 12.1 Refinar lista, busca e estados de resultado de Ações segundo os padrões de página, cards e ações.
- [x] 12.2 Preservar ticker e mercado e classificar como ausência local somente status 404 com code null.
- [x] 12.3 Exibir “Ação não cadastrada”, ticker, mercado e CTA “Cadastrar ação” no resultado local ausente.
- [x] 12.4 Manter TICKER_INEXISTENTE e demais erros externos distintos da ausência local e preservar StandardError.

## 13. Ações — cadastro e prefill

- [x] 13.1 Navegar pelo CTA com NavigationExtras.info contendo somente ticker e mercado pesquisados.
- [x] 13.2 Validar o info transitório no cadastro e preencher os controles apenas com tipos e valores compatíveis.
- [x] 13.3 Garantir formulário vazio em acesso direto, refresh ou info inválido, sem query params, storage ou store global.
- [x] 13.4 Preservar submissão manual, ausência de provider call automático, validações e contratos HTTP.

## 14. Ações — detalhe e datas

- [x] 14.1 Refinar a hierarquia do detalhe de Ações sem alterar DTO transitório, PATCH ou atualização manual de cotação.
- [x] 14.2 Aplicar o formatter compartilhado de OffsetDateTime às datas exibidas sem mutar o estado ou DTO.
- [x] 14.3 Confirmar apresentação pt-BR em timezone local e preservar futura dataOperacao civil YYYY-MM-DD fora do formatter.

## 15. Botões, formulários e cards

- [x] 15.1 Aplicar hierarquia filled/outlined/text às ações primárias, busca/atualização/retry e cancelar/limpar/voltar.
- [x] 15.2 Refinar formulários locais com superfície, max-width de 42rem, campos fluidos e agrupamento responsivo de ações, preservando Typed Reactive Forms.
- [x] 15.3 Aplicar às superfícies locais borda sutil, radius de 0,875rem, elevação baixa e spacing consistente.
- [x] 15.4 Manter entity cards, forms e detail views locais e limitar hover a elementos realmente interativos.

## 16. Acessibilidade

- [x] 16.1 Auditar hierarquia h1/h2, labels, keyboard, foco visível e active navigation nas páginas afetadas.
- [x] 16.2 Auditar role, aria-live, busy/disabled e anúncio dos novos feedbacks e estados de resultado.
- [x] 16.3 Confirmar que marca, lucro, prejuízo, warning e erro não dependem exclusivamente de cor.

## 17. Responsividade

- [x] 17.1 Preservar o breakpoint estrutural de 960px e os modos side/open e over/closed do sidenav.
- [x] 17.2 Refinar perto de 36rem o empilhamento de headers, alerts, forms, cards e grupos de ações.
- [x] 17.3 Verificar ausência de overflow e legibilidade de títulos, nomes longos, buscas e detalhes em desktop, tablet e mobile.

## 18. Testes automatizados

- [x] 18.1 Criar testes de PageHeader para h1, descrição, área de ação e estrutura compacta verificável.
- [x] 18.2 Criar testes de FeedbackAlert para variantes, role, aria-live, message e details.
- [x] 18.3 Atualizar testes de Corretoras para 404/code null, provider com code, preservação do CNPJ/coleção, CTA, `NavigationExtras.info` válido e inválido, confirmação não ativa e acesso direto/refresh vazio; comprovar que prefill válido preenche somente CNPJ, exige submissão explícita e não dispara automaticamente POST, BrasilAPI, ViaCEP ou qualquer outra chamada HTTP.
- [x] 18.4 Atualizar testes de Ações para 404/code null, distinção de TICKER_INEXISTENTE, preservação de ticker/mercado, CTA, info, prefill e refresh.
- [x] 18.5 Criar testes do formatter pt-BR/timezone local que comprovem apresentação sem mutação ou parsing na camada de dados.
- [x] 18.6 Atualizar testes estruturais de shell, acessibilidade e responsividade evitando asserções cosméticas frágeis por hexadecimal ou pixel.
- [x] 18.7 Executar os testes focados afetados e registrar arquivos, passed, failed, errors e skipped.
- [x] 18.8 Executar a suíte frontend completa e registrar arquivos, total, passed, failed, errors e skipped.

## 19. Build e bundle

- [x] 19.1 Executar o build de produção e registrar sucesso, initial bundle, transfer size e budgets.
- [x] 19.2 Comparar initial bundle e lazy chunks com o baseline de ~511,09 kB e warning conhecido de ~11,09 kB sem elevar budgets.
- [x] 19.3 Revisar CSS por componente e confirmar que nenhuma dependência foi adicionada para estilização.

## 20. Validação OpenSpec

- [x] 20.1 Validar a change com openspec validate refinar-identidade-visual-e-ux-frontend --strict.
- [x] 20.2 Validar todas as capabilities e changes com openspec validate --all --strict.
- [x] 20.3 Confirmar que requisitos, implementação e evidências de teste permanecem coerentes antes do archive.

## 21. Graphify

- [x] 21.1 Consultar o grafo durante a implementação para confirmar relações dos componentes afetados.
- [x] 21.2 Executar graphify update . pelo workflow normal após as alterações.
- [x] 21.3 Se ocorrer [WinError 5] Acesso negado, registrar o risco operacional sem elevar privilégios ou alterar ACL.

## 22. Inspeção visual manual

- [x] 22.1 Inspecionar toolbar, sidebar, container, páginas e feedbacks em viewports desktop, tablet e mobile.
- [x] 22.2 Inspecionar contraste, foco, nomes longos, estados de busca, alerts, forms e datas com dados representativos.
- [x] 22.3 Confirmar visualmente que Corretoras e Ações são consistentes sem antecipar UI funcional de Dashboard, Carteiras ou Operações.

## 23. Auditoria de escopo

- [x] 23.1 Auditar o diff para confirmar ausência de backend, endpoints, contratos, dependências, autenticação, gráficos, polling e cálculos financeiros.
- [x] 23.2 Confirmar ausência de mudanças funcionais não especificadas em Dashboard, Carteiras, Operações, Corretoras e Ações.
- [x] 23.3 Executar git diff --check e procurar logs, temporários, secrets, TODO/FIXME e código de debug.
- [x] 23.4 Revisar tasks e evidências, mantendo todos os artefatos OpenSpec coerentes antes da revisão independente.

## 24. Ajustes pós-inspeção humana

- [x] 24.1 Reestruturar o shell pela composição de toolbar, `MatSidenavContainer`, sidenav e content para manter toolbar/sidebar estáveis e uma única região de trabalho rolável, sem `position: fixed` isolado.
- [x] 24.2 Separar cabeçalho, controles e coleção nas listagens longas, tornando resultados a região principal de rolagem quando aplicável sem double scroll, conteúdo oculto, overflow horizontal, virtual scroll ou paginação.
- [x] 24.3 Padronizar no desktop e no drawer compacto cada item da sidebar como ícone local antes do label, com coluna consistente, `aria-current` preservado e SVG decorativo fora do nome acessível.
- [x] 24.4 Substituir o estado inline de ausência local de Corretora por `MatDialog` informativo com CNPJ, ações de fechar/cancelar e CTA “Cadastrar corretora”.
- [x] 24.5 Preservar no dialog de Corretora a coleção, a regra `404`/`code=null`, erros de provider, fechamento sem HTTP/navegação e CTA com `NavigationExtras.info` sem submissão automática.
- [x] 24.6 Substituir o estado inline de ausência local de Ação por `MatDialog` informativo com ticker, mercado, ações de fechar/cancelar e CTA “Cadastrar ação”.
- [x] 24.7 Preservar no dialog de Ação a coleção, a regra `404`/`code=null`, distinção de `TICKER_INEXISTENTE`, fechamento sem HTTP/navegação e CTA com `NavigationExtras.info` sem operação automática.
- [x] 24.8 Tornar os itens de Corretoras mais compactos usando somente identificação e metadados existentes, CTA de detalhe, ícone local, wrap e uma coluna legível no mobile.
- [x] 24.9 Aplicar às Ações a mesma densidade responsiva com os dados existentes, sem indicadores, cálculos ou campos derivados novos.
- [x] 24.10 Atualizar testes do shell para estabilidade estrutural no desktop/mobile, região correta de scroll, ausência de double scroll, conteúdo oculto e overflow horizontal, preservando skip link, foco e drawer.
- [x] 24.11 Atualizar testes da sidebar para ordem ícone→label, alinhamento estrutural, selected state, `aria-current` e ausência de duplicação do nome acessível.
- [x] 24.12 Atualizar testes de Corretoras para submit real, navegação existente, abertura seletiva do dialog, CNPJ, cancelamento/Escape/backdrop sem HTTP, coleção preservada, CTA/prefill e provider error sem dialog.
- [x] 24.13 Atualizar testes de Ações para abertura seletiva do dialog, ticker/mercado, cancelamento/Escape/backdrop sem HTTP, coleção preservada, CTA/prefill e `TICKER_INEXISTENTE` sem dialog.
- [x] 24.14 Verificar nos dialogs foco inicial, focus trap, restauração de foco, título/descrição associados, keyboard e responsividade sem fullscreen desnecessário.
- [x] 24.15 Executar testes focados e suíte completa após os ajustes, registrando totais, passed, failed, errors e skipped.
- [x] 24.16 Executar build, comparar initial/lazy chunks com 505,07 kB e warning residual de 5,07 kB e confirmar budgets e dependências intactos.
- [x] 24.17 Revalidar strict da change e global, tentar Graphify pelo fluxo normal e auditar diff/escopo antes de retomar as inspeções manuais 22.1–22.3.

## 25. Ajustes finais da inspeção visual

- [x] 25.1 Manter as ações textuais de retorno sticky dentro do workspace nas páginas longas, sem `position: fixed` global, sobreposição, cobertura de conteúdo ou mudança de navegação.
- [x] 25.2 Apresentar a situação cadastral na listagem de Corretoras como badge/chip compacto, mapeando somente o valor recebido e sem inferir status novo.
- [x] 25.3 Posicionar sucessos transitórios como toast Material acessível no topo responsivo, com semântica de sucesso e fechamento opcional, preservando erros no `FeedbackAlert`.
- [x] 25.4 Alinhar as ações dos dialogs lado a lado à direita no desktop e empilhá-las com dimensões equivalentes, gap e ordem Cancelar→principal no mobile estreito.
- [x] 25.5 Atualizar testes focados de sticky return, badge semântico, toast acessível e footer responsivo dos dialogs sem asserções cosméticas frágeis.
- [x] 25.6 Executar a suíte frontend completa e registrar total, passed, failed, errors e skipped.
- [x] 25.7 Executar o build, registrar bundle e budgets e confirmar ausência de dependências ou imports eager inesperados.
- [x] 25.8 Revalidar a change e o conjunto global em modo strict.
- [x] 25.9 Auditar escopo, arquivos protegidos, archives históricos, debug/temporários e `git diff --check`, mantendo 22.1–22.3 pendentes para inspeção humana.

## 26. Cadastro contextual e refinamentos pós-inspeção

- [x] 26.1 Reproduzir e corrigir a regressão do CTA “Cadastrar ação” com teste integrado que acione o botão real e comprove o resultado do dialog, sem tratar o CTA como cancelamento.
- [x] 26.2 Extrair/reutilizar o formulário de cadastro de Corretora em `MatDialog`, preservando somente CNPJ, Typed Reactive Forms, validações, payload e confirmação contextual de situação não ativa.
- [x] 26.3 Integrar o botão de cadastro da listagem de Corretoras ao dialog com formulário vazio e o CTA de ausência local ao mesmo dialog com somente CNPJ preenchido, sem HTTP automático.
- [x] 26.4 Fechar o cadastro contextual de Corretora após sucesso, atualizar a coleção pela resposta do POST sem GET redundante, manter a listagem ativa e apresentar toast; cancelar deve preservar busca/coleção sem HTTP.
- [x] 26.5 Extrair/reutilizar o formulário de cadastro de Ação em `MatDialog`, preservando somente ticker e mercado, Typed Reactive Forms, validações, payload e erros existentes.
- [x] 26.6 Integrar o botão de cadastro da listagem de Ações ao dialog com formulário vazio e o CTA de ausência local ao mesmo dialog com ticker e mercado preenchidos, sem HTTP ou provider call automático.
- [x] 26.7 Fechar o cadastro contextual de Ação após sucesso, atualizar a coleção pela resposta do POST sem GET redundante, manter a listagem ativa e apresentar toast; cancelar deve preservar busca/coleção sem HTTP.
- [x] 26.8 Preservar `/corretoras/nova` e `/acoes/nova` para acesso direto, reutilizando os formulários e mantendo seus contratos e fluxos de rota sem duplicar lógica de negócio.
- [x] 26.9 Preservar a ação de retorno disponível, com destino, nome acessível completo, foco e operação por teclado em desktop/mobile; o estado compacto baseado em rolagem foi deferido para change futura.
- [x] 26.10 Alterar o descarte automático do toast de sucesso para aproximadamente `10000 ms`, preservando posição superior, fechamento manual, `role=status` e `aria-live=polite`.
- [x] 26.11 Atualizar testes focados de dialogs informativo/cadastro, CTAs reais, prefill, cancelamento, ausência de HTTP automático, POST único, atualização local sem GET, rotas diretas, ação de retorno acessível e duração do toast.
- [x] 26.12 Executar a suíte frontend completa e registrar arquivos, total, passed, failed, errors e skipped.
- [x] 26.13 Executar o build, registrar bundle/lazy chunks e budgets sem elevar limites ou alterar dependências.
- [x] 26.14 Revalidar strict da change e global e auditar escopo, arquivos protegidos, archives, debug/temporários e `git diff --check`, mantendo 22.1–22.3 pendentes.
