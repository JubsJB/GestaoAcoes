## 1. Preparação e proteção de escopo

- [x] 1.1 Confirmar branch `feature/frontend-angular`, estado Git esperado e ausência de outra change ativa antes de editar o frontend.
- [x] 1.2 Reler proposal, design, ambas as delta specs e o contrato backend vigente de Ações antes da implementação.
- [x] 1.3 Registrar o estado inicial de `package.json`, `package-lock.json`, `.npmrc`, `allowScripts` e `packageManager` para comprovar que não haverá dependência nova.
- [x] 1.4 Confirmar que `/acoes` ainda é um limite lazy com placeholder e que Corretoras está funcional enquanto Dashboard, Carteiras e Operações permanecem estruturais.
- [x] 1.5 Revisar o escopo negativo e confirmar que nenhuma alteração backend, provider direto, cálculo financeiro, paginação, edição ou exclusão será necessária.

## 2. Contratos tipados e formatação visual

- [x] 2.1 Criar os tipos estritos de mercado, moeda, `AcaoResponse` e request de criação com todos os campos e nullability alinhados ao backend.
- [x] 2.2 Garantir por tipagem que o request de cadastro contém somente ticker e mercado e que moeda, nome, cotação e data não podem ser enviados.
- [x] 2.3 Implementar normalização leve de entrada que aplique apenas trim/uppercase, preserve caracteres internos e não decida ticker canônico ou sufixos.
- [x] 2.4 Implementar formatadores puros para mercado, cotação por moeda e data/hora apenas na apresentação, sem conversão cambial ou cálculo financeiro.
- [x] 2.5 Testar os formatadores, os enums e a preservação dos valores recebidos, incluindo BRL, USD e timestamp ISO.

## 3. Service HTTP de Ações

- [x] 3.1 Implementar service stateless com `HttpClient` e `API_BASE_URL`, sem estado, cache ou regra de provider.
- [x] 3.2 Implementar `listar`, `buscarPorId` e `buscarPorTickerEMercado` com métodos, caminhos e query parameters exatos.
- [x] 3.3 Implementar cadastro com POST contendo exclusivamente ticker e mercado.
- [x] 3.4 Implementar atualização por PATCH em `/acoes/{id}/cotacao` sem payload funcional de negócio.
- [x] 3.5 Testar os cinco endpoints com `HttpTestingController`, verificando método, URL, path, query params, body e DTO completo.
- [x] 3.6 Confirmar nos testes que nenhum endpoint alternativo, URL absoluta, BRAPI ou Alpha Vantage é chamado pelo navegador.

## 4. Rotas lazy da feature

- [x] 4.1 Substituir somente o placeholder de Ações por rotas lazy de lista, cadastro e detalhe com páginas carregadas sob demanda.
- [x] 4.2 Declarar `/acoes/nova` antes de `/acoes/:id` e preservar `/acoes` como listagem.
- [x] 4.3 Atualizar os testes do router global para reconhecer Ações como funcional sem alterar Corretoras nem os placeholders restantes.
- [x] 4.4 Testar as três rotas, sua precedência, o limite lazy e a remoção efetiva do placeholder de Ações.

## 5. Listagem de Ações

- [x] 5.1 Implementar página standalone de listagem com signals locais para coleção, loading e erro, carregando `GET /acoes` ao entrar.
- [x] 5.2 Implementar estados visuais distintos para carregamento, cards com registros, coleção vazia e erro.
- [x] 5.3 Preservar a ordem recebida do backend e não introduzir paginação, ordenação configurável ou atualização de cotação nos cards.
- [x] 5.4 Implementar ações acessíveis para cadastro, detalhe e retry, garantindo que retry execute novo GET real.
- [x] 5.5 Testar loading, registros, vazio, erro, retry, ordem, cards e navegações sem acoplamento a classes internas do Material.

## 6. Busca exata por ticker e mercado

- [x] 6.1 Implementar formulário de busca com ticker e `MatSelect` de mercado, validação de UX e submit explícito.
- [x] 6.2 Manter a coleção completa separada do estado de busca e impedir qualquer request durante digitação ou alteração do select.
- [x] 6.3 Navegar ao detalhe com o DTO retornado como estado transitório quando a combinação for encontrada, sem GET imediato por ID.
- [x] 6.4 Implementar 404 local sem code como estado contextual de Ação não encontrada e diferenciá-lo de `TICKER_INEXISTENTE` externo.
- [x] 6.5 Implementar limpeza/cancelamento que restaure imediatamente a coleção preservada sem novo GET e testar todos esses comportamentos com contagem real de chamadas.

## 7. Cadastro de Ação

- [x] 7.1 Implementar página standalone com Reactive Form tipado contendo somente ticker e mercado, com mercado em `MatSelect`.
- [x] 7.2 Aplicar validação apenas de UX compatível com obrigatoriedade e limite contratual, mantendo o backend como autoridade.
- [x] 7.3 Serializar a submissão para impedir double-submit e comunicar loading sem criar retry automático.
- [x] 7.4 No 201, exibir snackbar, navegar para `/acoes/{id}` e transportar o DTO completo como estado transitório sem GET redundante.
- [x] 7.5 Tratar `ACAO_DUPLICADA`, `TICKER_INEXISTENTE` e falhas externas no formulário preservando `status`, `code`, `message` e `details`, sem diálogo nem segundo POST, e testar a página de cadastro quanto aos campos ticker e mercado, Reactive Form tipado, `MatSelect`, validação de UX, payload contendo exclusivamente ticker e mercado, bloqueio de double-submit, sucesso com snackbar, navegação para `/acoes/{id}`, DTO transitório, ausência de GET redundante e matriz parametrizável desses erros sem exigir um teste isolado por código.

## 8. Detalhe da Ação

- [x] 8.1 Implementar página standalone de detalhe com signals locais para DTO, loading, erro e estado não encontrado.
- [x] 8.2 Aceitar estado transitório somente quando o ID do DTO corresponder ao ID da rota; caso contrário executar `GET /acoes/{id}`.
- [x] 8.3 Apresentar ticker, empresa, mercado, moeda, última cotação e data/hora com os formatadores definidos e linguagem que não prometa tempo real.
- [x] 8.4 Implementar 404 próprio, erro não-404, retorno à listagem e retry que execute novo GET por ID.
- [x] 8.5 Testar DTO transitório sem GET, acesso direto/refresh por GET, campos completos, formatação, 404, erro e retry real.

## 9. Atualização manual de cotação

- [x] 9.1 Disponibilizar ação “Atualizar cotação” somente no detalhe, com nome acessível e ausência comprovada na listagem.
- [x] 9.2 Bloquear PATCH concorrente, anunciar semanticamente o estado ocupado e manter o bloqueio até a operação finalizar.
- [x] 9.3 No sucesso, substituir o DTO local pela resposta, atualizar cotação/data exibidas e mostrar snackbar sem executar GET adicional.
- [x] 9.4 Em qualquer falha, preservar integralmente o DTO anterior, liberar o botão e permitir somente nova tentativa manual posterior.
- [x] 9.5 Tratar contextualmente `LIMITE_REQUISICOES_EXCEDIDO`, indisponibilidade, timeout e erros de dados/cotação sem retry automático.
- [x] 9.6 Validar o tratamento de `409 / TICKER_CANONICO_DIVERGENTE`, garantindo a preservação do DTO anterior, sem alterar o ticker local, sem aceitar substituição ou canonicalização no frontend, sem abrir diálogo de confirmação e sem repetir automaticamente o PATCH.
- [x] 9.7 Testar método/body do PATCH, estado ocupado, double-submit, sucesso, DTO atualizado, snackbar, ausência de GET, matriz de erros, preservação anterior e liberação final.

## 10. Responsividade e acessibilidade

- [x] 10.1 Implementar grid fluido de cards no desktop e fluxo em coluna no viewport compacto, sem tabela rígida ou breakpoint frágil desnecessário.
- [x] 10.2 Garantir `h1`, labels, erros associados, nomes acessíveis, foco visível e ordem de teclado coerente em lista, busca, cadastro e detalhe.
- [x] 10.3 Anunciar loading inicial, busca, cadastro, detalhe, atualização e feedback dinâmico com regiões semânticas concisas.
- [x] 10.4 Garantir que mercado, moeda, estados e erros não dependam somente de cor e que as ações permaneçam operáveis em tela compacta.
- [x] 10.5 Testar a estrutura/semântica responsiva e acessível verificável em jsdom e registrar inspeção visual manual apenas para aspectos puramente visuais.

## 11. Cobertura integrada da feature

- [x] 11.1 Testar o fluxo completo de listagem para cadastro e detalhe, incluindo feedback e ausência de GET redundante pós-POST.
- [x] 11.2 Testar busca explícita encontrada e inexistente, ausência de request durante digitação e limpeza sem recarregar a coleção.
- [x] 11.3 Testar detalhe por navegação transitória e por acesso direto, incluindo retry e preservação do contrato completo.
- [x] 11.4 Testar atualização manual bem-sucedida e falhas 409, 422, 429, 502, 503 e 504 com asserts de status/code/message/details relevantes.
- [x] 11.5 Verificar em testes que não há polling, scheduler, retry temporizado, store global, provider externo ou funcionalidade das demais áreas.
- [x] 11.6 Revisar os testes quanto a asserts úteis, mocks proporcionais e ausência de dependência de DOM/classe interna do Material.

## 12. Validações finais e rastreabilidade

- [x] 12.1 Confirmar que `package.json`, `package-lock.json`, `.npmrc`, `allowScripts`, `packageManager` e dependências permaneceram inalterados.
- [x] 12.2 Executar `npm ci` sob `strict-allow-scripts=true` sem bypass e parar diante de qualquer novo `ESTRICTALLOWSCRIPTS` não analisado.
- [x] 12.3 Executar `npm test -- --watch=false` e registrar arquivos, total, aprovados, failures, errors e skipped.
- [x] 12.4 Executar `npm run build` em produção e registrar bundle, transfer size, budgets e warnings sem alterar budgets.
- [x] 12.5 Validar manualmente a rastreabilidade dos 10 requirements e seus scenarios contra implementação e testes, incluindo a delta do shell.
- [x] 12.6 Executar `openspec validate implementar-acoes-frontend --strict` e `openspec validate --all --strict`.
- [x] 12.7 Confirmar semanticamente foundation como infraestrutura, shell como composição/lazy boundaries, broker-management como Corretoras e stock-management como Ações, sem conflito normativo.
- [x] 12.8 Confirmar backend intacto, demais placeholders preservados, ausência do escopo negativo e nenhuma chamada direta aos providers.
- [x] 12.9 Registrar o Graphify desatualizado/WinError 5 como risco operacional conhecido sem atualizar, alterar permissões ou modificar código por esse motivo.
- [x] 12.10 Executar `git diff --check` e revisar todos os arquivos alterados/untracked quanto a secrets, temporários, escopo e mudanças fora da change.
