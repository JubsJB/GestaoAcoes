## Context

Ver `proposal.md` para motivação e capabilities. A análise inicial foi de código, estilos, testes e especificações; não equivale a uma nova aprovação de renderização em navegador. Existem tema Material, tokens semânticos, padrões de superfície e componentes compartilhados. Autenticação usa cartão simples e cores locais; usuários usa SCSS próprio e a classe de coleção sem importar seus estilos. Ativos/corretoras/carteiras usam coleção refinada; operações/custódia usam a base. Há espaçamentos e regras tipográficas locais repetidos.

O PRD reconhece autenticação e administração aprovadas, apesar de trechos históricos de exclusão no AGENTS.md. Esta change preserva essa evolução e não altera domínio. As specs vigentes exigem precisão lossless, acessibilidade, lazy loading, ausência de assets remotos obrigatórios e initial bundle sem crescimento sobre baseline medido. Não usar o número histórico de bundle como nova medição.

Antes da criação: branch dev em 298d9b3, índice vazio, README e teste offset-date-time.formatter.spec.ts modificados e revisao-movimentacoes-custodia.zip não rastreado. Essas alterações não integram a change. Somente os artefatos neste diretório são autorizados nesta rodada.

## Goals / Non-Goals

**Goals:** centralizar decisões visuais, reduzir divergências sem substituir componentes funcionais, destacar informação financeira e preservar contratos verificáveis de acessibilidade e operação. Usar aprovação progressiva para evitar propagar uma direção ainda não validada.

**Non-Goals:** reescrever services, guards, interceptadores, contratos, cálculos ou geometria financeira; adicionar biblioteca, busca administrativa, filtros, métricas, tema escuro, funções de autenticação ou rotas. Não atualizar grafo, README, formatter, ZIP ou specs principais nesta rodada. Não produzir código ou previews implementados nesta etapa documental.

## Decisions

### Refinamento administrativo autorizado após revisão B

Login dividido, azul, degradê/painel institucional e formulário aprovados pelo usuário; direção geral da navegação aprovada. Administração permanece em revisão, sem autorização para páginas financeiras. O resumo administrativo total/ativos/bloqueados é exceção explicitamente autorizada à exclusão inicial de novas métricas: `UsuarioAdministracaoService.listar` retorna `findAll` sem paginação/filtro e `UsuariosApi.listar` valida o array completo. Derivar contagens desse mesmo array, sem endpoint ou chamada adicional, e omitir resumo durante carregamento/erro. Preservar uma tabela com reflow em cartões e ícones decorativos, além de separar dados, perfil e segurança no detalhe. Não alterar lógica de comandos ou permissões. Aprovação geral B continua pendente.

### 1. Reutilizar a capability global existente

`frontend-visual-experience` já cobre identidade, superfícies, hierarquia, acessibilidade, HTTP e bundle. Criar outra capability global duplicaria contratos. Será modificada a identidade e serão acrescentadas composição e cobertura transversal. `frontend-user-administration` separa apresentação administrativa de regras de backend. O delta de `frontend-authentication` remove somente o limite histórico ao layout anterior; os demais requisitos continuam vigentes.

O Purpose da spec global ainda menciona oliva. O mecanismo de delta não atualiza Purpose: na futura sincronização/arquivamento autorizado, revisar esse texto para azul. Não editar a spec principal agora nem criar Purpose ignorado no delta.

O título histórico do cenário "Identidade oliva aplicada" é mantido para rastreabilidade e compatibilidade com a validação estrita, que exige conservar os cenários do requisito substituído. Seu resultado normativo passa explicitamente a exigir azul; o título não mantém a antiga paleta vigente.

### 2. Azul para identidade, sem nova dependência

Reutilizar Angular Material/CDK, tema claro, fontes do sistema e ícones locais. Ajustar tokens semânticos e tokens públicos do tema em conjunto. Evitar seletores internos privados do Material. Alternativa de segunda biblioteca visual rejeitada por duplicar estilos e ampliar risco/bundle.

Paleta candidata, não aprovação final de contraste:

| Papel | Candidato |
| --- | --- |
| Marca/navegação | #16324F |
| Ação principal | #1D4ED8 |
| Hover primário | #1E40AF |
| Seleção | #EFF6FF |
| Fundo / superfície | #F5F7FA / #FFFFFF |
| Texto principal / secundário | #172B3A / #526273 |
| Divisória / contorno de controle | #D8E1EA / #64748B |
| Positivo / negativo | #166534 / #B42318 |
| Atenção / informação | #92400E / #1D4ED8 |
| Foco | #1D4ED8, com separação clara da superfície |

Verde petróleo não será a ação principal. Sucesso/lucro e erro/prejuízo mantêm tokens diferentes, mesmo quando próximos em cor. Validar cada combinação real, incluindo hover, seleção, foco, gráficos e ações desabilitadas aplicáveis.

### 3. Hierarquia e padrões por papel

Manter Segoe UI/Arial/sans-serif, corpo 16px, apoio preferencialmente 14px, títulos de página 28–32px, seções 20–24px, pesos 400/600/700 e números tabulares. Escala de espaço em múltiplos de 4px, com padding de superfície 16–24px. Raios candidatos: controles 8px, cartões 12–16px; sombras leves. Manter largura estrutural entre 72–80rem e formulário simples próximo de 42rem. Medidas finais dependem das amostras, sem reduzir dados para caber.

Botão preenchido azul para a ação principal, contornado para secundária, textual para apoio; exclusão usa papel destrutivo e confirmações existentes. Preferir altura de controle de 44px, preservando mínimo/exceções acessíveis aprovados. Campos mantêm label, ajuda, erro e autocomplete. Detalhes usam agrupamentos e listas descritivas. Modais mantêm título/descrição, foco contido/restaurado, Escape e regras atuais de bloqueio enquanto pendente.

Unificar os estilos base/refinados de coleção por adoção controlada, sem um componente universal que absorva lógica de domínio. Não duplicar tabela e cards ativos; preservar cabeçalhos, labels compactos e campos completos. Alternativa de cards exclusivos em todas as larguras rejeitada por prejudicar comparação financeira.

### 4. Composição por área

| Área | Composição e inventário |
| --- | --- |
| Shell | Marca, seletor de carteira e identidade/saída com espaço definido; manter menu recolhível, cinco destinos financeiros e Usuários para ADMIN; skip link e região principal única |
| Autenticação | Duas áreas no desktop, painel decorativo aproximadamente 45% e formulário 55%; gradientes azuis suaves/formas CSS estáticas; uma coluna no mobile; login, definir/redefinir senha, continuar sessão e estados existentes |
| Usuários | /usuarios, /usuarios/novo e /usuarios/:id; tabela administrativa e cards compactos, cadastro, detalhe, perfil/situação, confirmação e código único; sem campo de busca novo |
| Carteiras | /carteiras, /carteiras/nova, /carteiras/:id/editar e /carteiras/:id; lista, formulário, detalhe, exclusão, posições e histórico |
| Ativos | /acoes, /acoes/nova e /acoes/:id; lista/busca existente, cadastro em página/diálogo, ausência e detalhe/cotação |
| Corretoras | /corretoras, /corretoras/nova e /corretoras/:id; consulta existente, cadastro, confirmação/ausência e detalhe |
| Operações | /operacoes, /operacoes/nova e /operacoes/:id; histórico contextual, formulário em página/diálogo, preços e detalhe |
| Dashboard | /dashboard; bento grid por agrupamento semântico, contexto/ações, resumo BRL/USD, evolução/snapshots, composição, desempenho, posições e resultados existentes |
| Custódia | Dentro da carteira, distribuição, histórico/detalhe, filtros existentes, transferência, atribuição e estorno; sem nova rota |
| Transversal | Erro/vazio/loading/retry/sucesso, verificações de sessão, bloqueio/mascaramento e página não encontrada |

Não haverá novos textos que insinuem recuperação por e-mail, privacidade individual de carteiras compartilhadas, métrica inexistente ou automação de investimentos. A menção do usuário a busca administrativa é referência visual: a lista atual não oferece busca, portanto não será adicionada. Uma busca futura exige análise e aprovação próprias.

### 5. Responsividade e acessibilidade como invariantes

Conservar breakpoint estrutural 960px e navegação side/over, conteúdo próximo de 36rem e container queries quando apropriado. O bento grid deve seguir ordem lógica no DOM, sem reordenação visual que prejudique teclado/leitura. Não impor alturas fixas que recortem indicadores, gráficos ou formulários.

Aceite: WCAG 2.2 AA aplicável, contraste 4.5:1 texto normal, 3:1 texto grande e elementos gráficos/controles necessários; foco visível e não encoberto; teclado, labels/erros associados, headings, alert/status e aria-busy; zoom 200%, reflow 320 CSS px, baixa altura, reduced motion e ausência de double scroll. Preservar alvos 24×24 CSS px ou espaçamento/exceções documentadas; preferir 44px nos controles usuais. Validar leitor de tela real nos fluxos alterados; pendências históricas continuam pendentes até evidência humana.

### 6. Fronteiras de compatibilidade e testes

Congelar métodos/URLs/params/payloads/gatilhos de requisições; não enriquecer IDs. Preservar JWT HttpOnly, cookies, CSRF, guards, ADMIN/USER, retomada, inatividade, logout e mascaramento de overlays. Não alterar parsers, máscaras, formatadores, arredondamento, dates civis/instantes, precisão textual ou BRL/USD. Custódia conserva identidade (tipoRegistro,id), vínculos, ordem e frações integrais, sem editar operação ou duplicar POST em retry.

Reaproveitar os 98 arquivos de testes identificados, selecionando os afetados: shell/rotas/guards, autenticação/usuários, componentes compartilhados, páginas/diálogos/contexto, formatadores/lossless, gráficos e custodia-recarga.integration.spec.ts. Não reescrever testes em massa nem criar asserções de classes CSS como prova visual. Novos comportamentos seguem TDD e fronteiras públicas aprovadas; mudanças puramente visuais usam caracterização existente mais inspeção em navegador.

Baseline e medição final de produção usam mesma configuração/ambiente, registrando initial, delta e warnings. Conservar budgets (initial 500kB warning/1MB error; estilo por componente 4kB/8kB), lazy boundaries, manifestos/lockfile e nenhum crescimento do initial sobre baseline medido. Cobertura futura do frontend mantém portões globais de linhas/ramos >=80%, sem alegar cobertura por arquivo. Backend não é alterado: não executar suíte completa por rotina; qualquer necessidade adicional deve ter justificativa e autorização de escopo. Nenhum teste completo ou build será executado nesta rodada.

## Risks / Trade-offs

- [CSS global afeta gráficos e overlays] → adoção por amostras, inspeção dos estados e regressão focada antes da propagação.
- [Layout expõe dados durante verificação de sessão] → manter condicionais, hidden/inert e mascaramento dos overlays; validar sessão expirada/retomada.
- [Coleção compacta perde dados extensos] → usar fixtures exatas, nomes longos e frações com 512 dígitos por componente, sem truncamento obrigatório.
- [Bento grid desperdiça área ou muda leitura] → módulos fluidos com ordem semântica; sem cards vazios nem dados inventados.
- [Bundle cresce com decoração/CSS] → formas CSS simples, reutilização e medição reproduzível; não elevar budgets.
- [Aprovação visual confundida com aprovação funcional] → evidências separadas; não marcar leitor de tela ou cadeia manual como aprovados por testes simulados.

## Migration Plan

Após revisão documental e autorização explícita de implementação: capturar baseline; preparar amostra de fundação/componentes; obter aprovação A antes de propagar; revisar shell/login/usuários em aprovação B antes das demais páginas; aprovar página financeira representativa em C antes de expansão; revisar dashboard/custódia em D antes do aceite final E. Os grupos pendentes não avançam por aprovação implícita.

Implementar por etapas descritas em tasks.md, mantendo rotas e limites lazy. Não há migração de dados ou backend. Publicação não é autorizada por esta change documental. Se necessário em entrega futura, rollback será para build frontend compatível anterior, preservando backend/schema/dados; sem reset/clean ou restauração que apague alterações de terceiros.

## Decisões de Ativos autorizadas em 2026-11-12

6.3 usa Consulta integrada com acabamento C2: cabeçalho sem ornamento adicional, tabela semântica única de cinco colunas com reflow local em cartões compactos, ticker destacado e empresa complementar. A consulta explícita mantém a coleção completa, com o texto aprovado “Informe o ticker e o mercado para consultar os detalhes de uma ação cadastrada.” Detalhe organiza identificação e cotação em seções distintas, preservando referência de mercado e obtenção. Cadastro em página/diálogo e ausência usam quatro SCSS locais com opt-in azul; estilos compartilhados e outras features permanecem intactos. Ausência de cotação/referência é comunicada sem fabricar zero ou data; DTOs e contratos continuam inalterados.

O usuário autorizou duas alterações funcionais restritas: USER recebe ausência e retorno à listagem sem convite ao cadastro, enquanto ADMIN mantém prefill transitório; o erro com confirmação booleana cotacaoPreservada=true continua visível durante nova tentativa manual e é limpo após sucesso, inclusive com cotação inalterada. Sem confirmação não há afirmação de preservação. O erro normalizado integral permanece interno, sem exposição indiscriminada de detalhes técnicos. O delta frontend-stock-management reconcilia os cenários e preserva as specs principais até sincronização autorizada. Foco, Escape, cancelamento, guards, backend, CSRF, DTOs, serviços e integrações não são redesenhados. Aceite das prévias e autorização de implementação não conclui 6.3: avaliação humana da implementação real continua necessária.

## Padronização visual aprovada

### Composição de Corretoras aprovada em 2026-11-13

Após revisão da implementação, o usuário aprovou título funcional separado da razão social extensa e composição unificada dos diálogos. O detalhe usa título “Detalhe da corretora”; a razão social integral fica em bloco de identificação com CNPJ/situação/data. Contato e endereço ficam em cartões alinhados pelo topo, com altura natural e sem equalização artificial, labels explícitos e todos os dados preservados. Fontes e espaçamentos seguem Usuários, sem copiar a estrutura administrativa.

Cadastro contextual usa cabeçalho próprio de diálogo e uma única área de padding/rolagem, sem o preset global app-dialog-page que comprimia o formulário. Ausência ADMIN/USER e confirmação têm o mesmo alinhamento local de título/conteúdo/ações, com bloco de CNPJ/situação; não há override global de overlay nem alteração de páginas aprovadas. O texto inicial de confirmação continua informando que a situação consultada não é ATIVA; a mensagem inferior passa, por autorização expressa, a “Deseja continuar com o cadastro desta instituição?”. A mensagem original continua no objeto recebido, sem ser repetida no corpo visual. Foco inicial, Escape, decisão explícita, cancelamento e segundo POST continuam iguais. Erro permanece no contexto e sucesso no toast existente, sem novos modais ou classificações. 6.4 não é concluída por aprovação do protótipo: aguarda aceite da implementação real.

### Escala visual de referência — Usuários, aprovada em 2026-11-13

Usuários é a referência de tipografia, proporções e densidade para as próximas modernizações. Reutilizar a escala por papel, não sua estrutura administrativa, monogramas, resumo ou campos. Não transportar métricas para páginas financeiras nem reduzir globalmente fontes/controles. A adoção nesta rodada é somente em Corretoras; Ativos/Carteiras mantêm seus aceites e qualquer alinhamento futuro depende de escopo autorizado. Operações/Dashboard seguirão a referência quando suas etapas forem autorizadas, preservando escalas próprias de valores/gráficos e precisão. Tarefas 3.2/3.3 orientam consolidação por papel, sem propagação automática; inspeções transversais permanecem em 8.1/8.2.

#### Refinamento de Ações autorizado em 2026-11-13

Aceite real posterior confirmado pelo usuário em desktop/mobile, com ressalva pontual do espaçamento do detalhe. Ajuste restrito a acao-detail.scss: compensação local do padding superior do shell por margem de -16px desktop/-8px até 959,98px, resultando em inset de 16px; margem local no cabeçalho imediatamente após app-sticky-back reduz somente esse gap de 20px para 8px. Gap entre cabeçalho e cartão continua 20px. Não modifica shell, compartilhados, áreas de interação, foco/sticky, larguras ou outros módulos. A conferência deste ajuste permanece pendente; aceite anterior preservado.

Após aprovar a listagem integrada e as prévias separadas de detalhe, cadastro, duplicidade, ausência e estados, o usuário autorizou aplicar a escala de Usuários/Corretoras somente em Ações. Títulos 28px desktop/24px mobile, contexto 12px, descrição/dados de tabela 14px, ticker 16px/600 na coleção, empresa 13px e ações com área mínima de 44px. Consulta e coleção formam um conjunto, conservando a seção Ações cadastradas e cinco colunas. Ticker, mercado e ações ficam em linha quando a largura comporta; em telas compactas os campos empilham e os botões permitem quebra. Nenhum filtro ou gatilho novo.

Detalhe usa título funcional, identificação com ticker/empresa e seção própria de cotação, sem equalização de altura; valor permanece destacado e referência tipada continua separada da obtenção. Cadastro em página mantém campos/validações; diálogo recebe cabeçalho H2 associado ao nome acessível, padding/rolagem locais e Cancelar antes de Cadastrar. Ausência conserva ADMIN com Cancelar/Cadastrar e USER com retorno, sem modificar permissões. Erros normalizados completos permanecem internos: mensagens contextuais são mantidas, incluindo duplicidade, sem renderizar indiscriminadamente details. Não há mudança na atualização manual ou no aviso condicionado de preservação.

Aplicação por estilos locais e template de Ações, sem importar estilos de outra feature, modificar compartilhados ou reabrir o aceite anterior de 6.3. Refinamento rastreado em 3.2/3.3; testes direcionados em 3.4 e verificações humanas/transversais em 8.1/8.2. O aceite das prévias não representa aceite desta implementação real; nenhuma dessas tarefas é concluída automaticamente.


| Papel | Referência adotada em Corretoras |
| --- | --- |
| Título de página | 1,75rem desktop / 1,5rem até 48rem de viewport; peso 700; entrelinha 1,2; marca azul |
| Contexto e descrição | Contexto 0,75rem/peso 400; descrição 0,875rem/entrelinha 1,5 |
| Coleção | Texto 0,875rem; razão social peso 600/entrelinha 1,45; fantasia 0,8125rem/peso 400 |
| Cabeçalho da tabela | 0,75rem/peso 600; padding vertical 0,6875rem |
| Células desktop | Padding 0,875rem vertical e 1,125rem horizontal, sem altura fixa ou corte de conteúdo |
| Labels, situação e ação | Labels mobile e badges 0,75rem; ação da linha 0,8125rem, alvo mínimo de 2,75rem mantido |
| Detalhe | Labels 0,8125rem; valores/endereço 0,875rem/peso 400/entrelinha 1,5; títulos de seção 1,125rem/entrelinha 1,4 |
| Formulários | Título de seção 1,125rem; gap 1,25rem; padding 1,5rem desktop e 1,125rem mobile; sem reduzir campos editáveis |
| Diálogos | Título 1,375rem/peso 650; corpo 0,9375rem/entrelinha 1,5; ações, foco e cancelamento preservados |

Medidas em rem preservam ampliação do texto. Nomes, e-mails, CNPJ e endereços completos podem quebrar naturalmente; sem ellipsis, altura fixa ou duplicação de controles. Os cartões conservam duas colunas somente onde há espaço, identificação/CNPJ/ação em linhas integrais e alvos de 44px na escala padrão. O mixin local corretoras-typography define a escala somente nos hosts de Corretoras. PageHeader recebeu variáveis opcionais de cor/peso/contexto/descrição com os mesmos fallbacks anteriores: sem valores opt-in, outras páginas mantêm sua aparência. Nenhum ::ng-deep, seletor global de override ou cópia da estrutura de Usuários.

### Corretoras — implementação autorizada em 2026-11-12

A proposta C2 com consulta integrada, tabela desktop de cinco colunas e cartões mobile foi aprovada antes da implementação de 6.4. Cadastro continua exclusivamente por CNPJ em página/diálogo, com processamento BrasilAPI/ViaCEP e validação de endereço integralmente no backend. O fluxo de confirmação após 409/SITUACAO_CADASTRAL_NAO_ATIVA, segundo envio explícito e cancelamento sem POST permanecem inalterados. Não há campos manuais novos, edição ou exclusão. USER recebe ausência e retorno; ADMIN mantém cadastro com prefill. Erro completo permanece interno e mensagens atuais são preservadas, sem renderização automática de details. Quatro SCSS locais opt-in azuis evitam propagação para layout e outras features. Delta frontend-broker-management reconcilia os cenários; specs principais continuam intactas. 6.4 permanece aberta até aceite explícito da implementação real.

### Carteiras — refinamento localizado autorizado em 2026-11-13

Propostas desktop aprovadas pelo usuário, com implementação autorizada e adaptação 360/320. Adotar a escala por papel de Usuários/Corretoras/Ações somente em estilos locais: títulos 28/24px, descrição/dados 14px, contexto/badges/cabeçalhos de tabela 12px, nome de coleção peso 600 e ações com área mínima de 44px. Preservar quatro colunas, nome completo, indicação do contexto ativo e data real de criação formatada a partir do DTO; nenhum valor vazio ilustrativo substitui dados reais.

Detalhe: título funcional Detalhe da carteira e nome integral destacado na identificação, seguido de identificador/data. Opt-in de escala somente no cabeçalho e seletores da identificação, sem tokens no host que se propaguem aos filhos financeiros. Preservar posições, distribuição, histórico de operações e de custódia, ações financeiras, carregamentos/erros independentes, contratos, precisão e cálculos. Nenhuma modernização de componentes financeiros internos nesta entrega.

Cadastro/edição: mesmo formulário/campo Nome, validações e payloads; página mantém cabeçalho/identificação e diálogo usa H2 associado ao ariaLabelledBy existente, descrição acessível preservada, padding/rolagem locais, sem preset global app-dialog-page. Campo Material conserva dimensão editável; subscriptSizing dynamic acomoda ajuda/erro em telas estreitas. Cancelar antes de Cadastrar/Salvar no diálogo; ordem da página preservada. Foco, Escape, cancelamento e bloqueios continuam atuais. Exclusão conserva composição C2, ícones semânticos, mensagens, ações e estados integralmente; ajuste somente de escala de rótulo/valor/corpo. Não redesenhar alertas ou regras de recuperação.

Rastreabilidade em 3.2/3.3/3.4, sem reabrir 6.1/6.2. Aprovação das propostas não conclui aceite desta implementação real; verificações transversais de 8.1/8.2 continuam pendentes. Somente Carteiras, sem compartilhados, shell, seletor global ou outras features.

## Open Questions

- Hexadecimais finais, intensidades do degradê, raios e espaçamento fino serão aprovados nas amostras A/B, dentro da direção azul já autorizada.
- Composição final do bento grid e densidade administrativa serão aprovadas nas amostras, preservando inventário e comportamento.
- Referências visuais ainda não anexadas podem orientar esses ajustes; sua ausência não autoriza copiar exemplos nem expandir funcionalidades.

Essas decisões são de acabamento; qualquer mudança de escopo funcional requer revisão separada dos requisitos antes da implementação correspondente.
