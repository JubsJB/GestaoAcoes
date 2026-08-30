## 1. Pré-validação e contratos

- [x] 1.1 Confirmar a branch, o estado Git e que somente a change aprovada está ativa antes da implementação.
- [x] 1.2 Revalidar os quatro endpoints de Corretoras, os DTOs e a nullability contra OpenSpec, OpenAPI e código backend vigentes.
- [x] 1.3 Registrar como restrições de implementação a ausência de mudanças no backend, de integrações diretas com BrasilAPI/ViaCEP e de regras cadastrais duplicadas.
- [x] 1.4 Revisar a estrutura lazy atual de `features/corretoras` e identificar o menor conjunto de arquivos a substituir ou criar.
- [x] 1.5 Confirmar que Angular Material, Reactive Forms, HttpClient, signals e a infraestrutura de erros existentes atendem à feature sem nova dependência.

## 2. Modelos e utilitários contratuais

- [x] 2.1 Criar o tipo `Corretora` com todos os campos, tipos e valores nulos alinhados ao contrato real do backend.
- [x] 2.2 Criar o request de cadastro com `cnpj` e o controle técnico opcional de confirmação restrito ao segundo envio.
- [x] 2.3 Criar funções puras para normalização de entrada e formatação visual de CNPJ sem validar dígitos verificadores.
- [x] 2.4 Criar somente os formatadores mínimos necessários para apresentar CEP, data e valores opcionais sem alterar os dados contratuais.
- [x] 2.5 Testar modelos e utilitários quanto a máscara, valor enviado, valores nulos e preservação do timestamp recebido.

## 3. Serviço HTTP

- [x] 3.1 Implementar `CorretorasService` stateless com `HttpClient` e `API_BASE_URL`, sem URL absoluta ou estado de tela.
- [x] 3.2 Implementar a listagem por `GET /corretoras` com retorno tipado.
- [x] 3.3 Implementar a consulta por ID em `GET /corretoras/{id}` com codificação segura do parâmetro.
- [x] 3.4 Implementar a consulta exata em `GET /corretoras/por-cnpj` usando o query parameter `cnpj`.
- [x] 3.5 Implementar o cadastro em `POST /corretoras` sem transformar ou capturar os erros normalizados da infraestrutura.
- [x] 3.6 Testar método, URL, query parameter, resposta e payloads exatos `{ cnpj }` e `{ cnpj, confirmarSituacaoCadastralNaoAtiva: true }` com `HttpTestingController`.

## 4. Rotas e limite lazy

- [x] 4.1 Substituir somente o placeholder de Corretoras pelas rotas funcionais mantendo o limite lazy independente do shell.
- [x] 4.2 Configurar `/corretoras`, `/corretoras/nova` e `/corretoras/:id`, declarando `nova` antes do parâmetro dinâmico.
- [x] 4.3 Preservar shell, navegação, wildcard e placeholders das outras quatro áreas sem mudança funcional.
- [x] 4.4 Testar resolução lazy, precedência de `nova`, navegação para detalhe e integração das três rotas com o shell.

## 5. Listagem e busca por CNPJ

- [x] 5.1 Implementar a página de listagem com estado local explícito de carregamento, sucesso, vazio e erro.
- [x] 5.2 Apresentar cada Corretora com identificação suficiente e ações acessíveis para cadastro e detalhe, preservando a ordem recebida.
- [x] 5.3 Implementar estado vazio com ação de cadastro e erro recuperável com nova tentativa, sem confundir carregamento com vazio.
- [x] 5.4 Implementar a busca exata por CNPJ como ação explícita, aceitando entrada visual com ou sem máscara, sem request durante digitação e sem substituir a coleção completa preservada localmente.
- [x] 5.5 Navegar ao detalhe do resultado, tratar ausência ou falha sem apagar a coleção e restaurar imediatamente a listagem preservada ao limpar/cancelar a busca, sem novo GET automático.
- [x] 5.6 Testar registros, ordem, estados da listagem, retry explícito, validação e submissão da busca, ausência de request durante digitação, resultado, ausência de correspondência e limpeza sem recarregar a coleção.

## 6. Cadastro inicial por CNPJ

- [x] 6.1 Implementar `/corretoras/nova` com Reactive Forms e somente o controle permanente `cnpj`.
- [x] 6.2 Aplicar validação de UX para obrigatoriedade, caracteres/formato e 14 dígitos, sem reproduzir validação algorítmica do backend.
- [x] 6.3 Garantir que a primeira submissão válida envie exclusivamente `{ cnpj }` com o valor normalizado.
- [x] 6.4 Comunicar processamento e impedir submissões concorrentes enquanto o POST estiver em andamento.
- [x] 6.5 Após sucesso, apresentar feedback e navegar para o detalhe pelo ID devolvido, reutilizando o `CorretoraResponse` como estado transitório sem GET imediato ou store global.
- [x] 6.6 Testar formulário, mensagens acessíveis, ausência de request inválido, primeiro payload, bloqueio concorrente, sucesso, navegação com o DTO devolvido e ausência de GET redundante.

## 7. Confirmação contextual de situação não ativa

- [x] 7.1 Detectar o fluxo excepcional somente pela combinação `status=409` e `code=SITUACAO_CADASTRAL_NAO_ATIVA` do erro normalizado.
- [x] 7.2 Implementar confirmação acessível que informe que a situação devolvida não é ativa e apresente a situação/mensagem fornecida pelo backend.
- [x] 7.3 Manter `confirmarSituacaoCadastralNaoAtiva` fora do formulário e impedir qualquer reenvio automático ao abrir a confirmação.
- [x] 7.4 Reenviar uma única vez o mesmo CNPJ com `confirmarSituacaoCadastralNaoAtiva=true` somente após confirmação explícita.
- [x] 7.5 Garantir que cancelar, fechar ou abandonar a confirmação não realize nova requisição e descarte o estado técnico contextual.
- [x] 7.6 Encaminhar qualquer outro erro 409 ao tratamento normal sem abrir a confirmação nem inferir a situação cadastral.
- [x] 7.7 Testar gatilho exato, conteúdo, foco, confirmação, cancelamento/fechamento, payload do segundo POST, ausência de retry automático e outros conflitos.

## 8. Detalhe e apresentação contratual

- [x] 8.1 Implementar a página de detalhe para usar o DTO transitório quando disponível e consultar `GET /corretoras/{id}` em acesso direto, refresh ou navegação sem DTO, com estados de carregamento, sucesso, não encontrado e erro recuperável.
- [x] 8.2 Apresentar identificação, contato, endereço, situação cadastral, validação financeira e data de cadastro sem inferir dados.
- [x] 8.3 Representar campos nulos como não informados e `validadaMercadoFinanceiro=false` como validação ainda não realizada.
- [x] 8.4 Oferecer retorno acessível à listagem tanto no detalhe quanto no estado de registro inexistente.
- [x] 8.5 Testar DTO transitório sem GET, acesso direto/refresh sem DTO por GET, contrato completo, nulos, data, semântica da validação financeira, 404, erro e recuperação.

## 9. Responsividade e acessibilidade

- [x] 9.1 Ajustar lista, busca, formulário, confirmação e detalhe para desktop e viewport compacto sem rolagem horizontal obrigatória na tarefa principal.
- [x] 9.2 Garantir `h1`, labels, descrições, regiões de estado, nomes acessíveis e ordem de foco coerente em todas as páginas.
- [x] 9.3 Garantir operação por teclado, foco visível e retorno de foco após confirmação, sem comunicar estado apenas por cor.
- [x] 9.4 Adicionar testes de acessibilidade e responsividade para os fluxos principais sem acoplamento ao DOM interno do Material.

## 10. Testes integrados e escopo

- [x] 10.1 Testar que os placeholders e rotas das demais áreas continuam estruturais e que somente Corretoras ganhou comportamento funcional.
- [x] 10.2 Testar que os placeholders e componentes da feature não realizam requisições fora dos quatro contratos aprovados.
- [x] 10.3 Testar preservação de `status`, `code`, `message` e `details` nos erros usados pela feature.
- [x] 10.4 Revisar os testes para evitar mocks excessivos, asserts triviais e acoplamento desnecessário a classes internas do Material.
- [x] 10.5 Confirmar por inspeção a ausência de DTOs/services de outras features, dados fake, CRUD de edição/exclusão, paginação e regras cadastrais no frontend.
- [x] 10.6 Executar a suíte relevante em modo não interativo e confirmar todos os testes aprovados, sem failures, errors ou skipped inesperados.

## 11. Validação final

- [x] 11.1 Revisar `package.json`, `package-lock.json`, `.npmrc` e a política `allowScripts`, confirmando ausência de dependências ou drift não planejados.
- [x] 11.2 Executar `npm run build`, confirmar build de produção aprovado e verificar budgets e warnings.
- [x] 11.3 Revisar o bundle e confirmar que a feature permanece em limite lazy e não introduz carregamento eager acidental.
- [x] 11.4 Executar `openspec validate implementar-corretoras-frontend --strict` e corrigir somente inconsistências documentais da change.
- [x] 11.5 Executar `openspec validate --all --strict` e confirmar compatibilidade entre foundation, shell e broker management.
- [x] 11.6 Atualizar e consultar o Graphify conforme o workflow do projeto, verificando a arquitetura da feature e registrando limitações não bloqueantes.
- [x] 11.7 Executar `git diff --check` e revisar todos os arquivos alterados ou não rastreados quanto a secrets, temporários e escopo negativo.
- [x] 11.8 Confirmar por diff que `pom.xml`, `src/main/**`, `src/test/**` e demais arquivos backend não foram alterados.
