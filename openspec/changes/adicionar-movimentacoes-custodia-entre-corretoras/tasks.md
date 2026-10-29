Estado atual em **29/10/2026**: **58 das 60 tarefas concluídas**, incluindo 6.9 e 6.10. A validação final Maven/H2, Angular, cobertura, build, integridade das migrations e OpenSpec estrito foi executada e aprovada tecnicamente. Permanecem **duas tarefas desmarcadas**: 5.10 e 6.5, parcialmente comprovadas, com verificações manuais dispensadas para esta entrega e riscos residuais aceitos. Dispensa não equivale a verificação aprovada nem altera os critérios originais. Consulte [excecoes-homologacao.md](excecoes-homologacao.md), a rastreabilidade em [evidencias.md](evidencias.md) e o procedimento/registro de 6.10 em [validacao-final.md](validacao-final.md). Os resultados anteriores das Etapas 3 e 4 permanecem históricos. Esta atualização é documental e não executa nem autoriza novas operações, testes, migrations, commits ou arquivamento.

## 1. Modelo e persistência

Dependências: o planejamento incremental foi aprovado e as homologações 1.10 e 1.9 no PostgreSQL 17.11 foram concluídas, incluindo as evidências H2 aplicáveis, conforme [evidencias.md](evidencias.md). O bloqueio de homologação PostgreSQL dessas duas tarefas está atendido. A auditoria 6.9, aprovada em 29/10/2026, reconciliou também 1.1–1.8 e 2.1–2.10 com implementação, testes e homologações existentes; esses itens agora estão concluídos, sem repetir as execuções. Os critérios originais abaixo são preservados integralmente, inclusive as condições e restrições do planejamento inicial, cujo estado de execução é atualizado por este registro e pelas caixas de conclusão. Novas implementações, testes ou aplicação de migrations em bancos existentes continuam exigindo autorização específica. Nenhuma nova execução é realizada nesta atualização.

- [x] 1.1 [Implementação] Criar MovimentacaoCustodia e enum de natureza separado de TipoOperacao, verificando que COMPRA/VENDA permanecem únicos tipos financeiros. Domínio e testes unitários podem anteceder PostgreSQL 17; integração da posição usa 2.2, mapeamentos são verificados em 1.3/1.7/1.9.
- [x] 1.2 [Implementação] Criar EstornoMovimentacaoCustodia com FK única à original, dataRegistro e motivo opcional; derivar situação e dataEfetiva sem duplicação divergente. Depende de 1.1; domínio pode anteceder PostgreSQL 17, integridade do vínculo depende de 1.6/1.8/1.9 e não é comprovada por teste unitário.
- [x] 1.3 [Implementação] Mapear BigDecimal NUMERIC(19,6), LocalDate, instante servidor e âncora/fração textuais conforme design. Adotar entidades JPA diretas (alternativa B), com hidratação/identidade pelo ORM e imutabilidade pela API de negócio, sem setters públicos de auditoria. Separar criação de reconstrução histórica, preservando datas/instantes armazenados sem Clock na leitura. Manter PosicaoRacionalCustodia imutável e independente de JPA; mapear duas colunas VARCHAR(512), rejeitar fronteiras e frações históricas não canônicas como 2/4. Validar estrutura na hidratação sem consultas adicionais ou correções silenciosas; manter invariantes no domínio e constraints no banco. Mapear vínculo obrigatório/único do estorno sem duplicar posição, quantidade ou data efetiva; derivar situação do vínculo persistido carregado em contexto consistente. Depende de 1.1/1.2 e da representação pura de 2.2. Desenvolvimento e verificação H2 podem anteceder PostgreSQL 17; manter pendência de homologação integrada em 1.9 antes da conclusão desta tarefa.
- [x] 1.4 [Implementação] Criar repositórios para replay/histórico/detalhe/vínculos em lote, com isolamento por carteira/ação e sem N+1. Depende de 1.1–1.3 e do schema para testes de integração; SQL carrega candidatos, sem ordenar frações como autoridade cronológica. Desenvolvimento e verificação H2 podem anteceder PostgreSQL 17; manter pendência de homologação integrada em 1.9 antes da conclusão desta tarefa.
- [x] 1.5 [Implementação] Reconfirmar próximo prefixo Liquibase e criar migration aditiva das duas tabelas, sem editar001–007 ou backfill de operações. Depende do modelo/mapeamentos propostos em 1.1–1.3; criar schema provisório para verificação isolada, sem depender da conclusão dessas tarefas. Desenvolvimento e verificação H2 podem anteceder PostgreSQL 17; manter pendência de homologação integrada em 1.9 antes da conclusão desta tarefa. Não aplicar migrations em bancos existentes antes da homologação.
- [x] 1.6 [Implementação] Adicionar constraints de natureza/direção/quantidade/posição, FKs restritivas, unicidade de posição canônica e vínculo de estorno. Depende de 1.5 e das invariantes de 1.1/1.2/2.2; UNIQUE textual não substitui redução por gcd no domínio. Desenvolvimento e verificação H2 podem anteceder PostgreSQL 17; manter pendência de homologação integrada em 1.9 antes da conclusão desta tarefa.
- [x] 1.7 [Testes automatizados] Cobrir persistência exata BR/EUA, datas, original/vínculo e nulabilidade da atribuição em H2 isolado autorizado. Depende da implementação provisória de 1.1–1.6, não da sua homologação PostgreSQL 17. Conclusão atesta exclusivamente H2; roundtrip PostgreSQL 17 permanece em 1.9.
- [x] 1.8 [Testes automatizados] Cobrir unicidades, directions inválidas, limite/forma da chave, estorno duplicado e rejeição de exclusão em cascata. Depende da implementação provisória de 1.5/1.6 e do domínio canônico. Executar inicialmente em H2 isolado autorizado; repetir os casos de integridade aplicáveis no PostgreSQL 17 em 1.9. Conclusão desta tarefa atesta o escopo H2, não homologação PostgreSQL.
- [x] 1.9 [Testes automatizados] Preparar e executar instalação limpa/upgrade Liquibase e validação Hibernate em H2 isolado autorizado, com modelo/schema provisórios de 1.1–1.6. Após concluir a verificação preliminar de 1.10 no PostgreSQL 17, repetir a homologação integrada nessa versão descartável explicitamente autorizada, incluindo roundtrip, constraints, casos de integridade de 1.8 e tamanho real do índice integrado. Concluir somente com evidências H2 e PostgreSQL 17; não exige conclusão antecipada de 1.3–1.6, evitando dependência circular.
- [x] 1.10 [Verificação técnica preliminar] Verificar em estruturas/dados descartáveis, independentemente da aplicação implementada, armazenamento/roundtrip, comparação BigInteger, canonicidade/UNICIDADE, índice composto e crescimento adversarial; registrar versões/metodologia/evidências. Registrar resultados favoráveis existentes em H2 2.4.240 e PostgreSQL 16.8 como validação parcial. Repetir os experimentos aplicáveis em PostgreSQL 17 isolado autorizado antes da conclusão de 1.9 e da integração transacional da Etapa 3. A pendência PostgreSQL 17 permite desenvolvimento incremental nos limites aprovados, mas impede homologação da capacidade e conclusão da Etapa 1. Manter limite de 512 dígitos e representação racional textual; resultado ausente, inconclusivo ou inviável mantém bloqueada a homologação e exige revisão/aprovação antes de alterar representação/capacidade. Não executar nesta revisão documental.

## 2. Motor quantitativo e ordenação

Estado reconciliado em 29/10/2026: 2.1–2.10 concluídas conforme auditoria 6.9 e [evidencias.md](evidencias.md); matemática, replay, comparação diferencial, espaços e testes já comprovados. As homologações PostgreSQL 1.9/1.10 permanecem as anteriores, sem nova execução.

Dependências: design incremental aprovado e autorização de implementação. Componentes puros e testes unitários podem avançar sem banco; 2.2 pode anteceder os mapeamentos de 1.3 e permanece nesta etapa. Integração persistida usa 1.1–1.4. A passagem para integração transacional da Etapa 3 exige Etapa 2 validada e homologações PostgreSQL 17 de 1.10/1.9 concluídas.

- [x] 2.1 [Implementação] Criar eventos quantitativos discriminados FINANCEIRO/CUSTODIA, incluindo grupo nulo e checkpoints de originais neutralizadas.
- [x] 2.2 [Implementação] Implementar comparação exata de âncora/fração com BigInteger, medianta reduzida, formato canônico e rejeição explícita do limite sem renumeração. Pode ser antecipada como componente puro com testes unitários, sem banco e sem conclusão de 1.10; manter 512 dígitos após redução. Testes matemáticos não homologam armazenamento ou índice PostgreSQL 17; crescimento adversarial também permanece coberto em 1.10/2.10.
- [x] 2.3 [Implementação] Implementar replay puro de compra/venda e débito/crédito indivisível de custódia, com estorno sem efeito na dataRegistro.
- [x] 2.4 [Implementação] Enumerar espaços da data e selecionar primeiro válido, preservando chaves existentes e estabilidade em dia sem operações financeiras.
- [x] 2.5 [Implementação] Implementar comparação antes/depois por união de checkpoints/identidade, transporte de saldo e deficitDepois<=deficitAntes.
- [x] 2.6 [Implementação] Validar saldo local da candidata e origens das custódias ativas, sem revalidar vendas legadas como novas.
- [x] 2.7 [Implementação] Produzir evidências genéricas, primeira ocorrência efetiva, saldos/reconciliação e conflito determinístico conforme precedência.
- [x] 2.8 [Testes automatizados] Cobrir primeira posição/ausência, inserção entre custódias, empates, dia sem operações e operação adicionada depois sem reposicionamento.
- [x] 2.9 [Testes automatizados] Cobrir déficit novo/agravado/prolongado/inalterado/reduzido, cadeia e estorno com checkpoint preservado; reproduzir tabelas antes/depois do design, união por identidade/posição, transporte de saldo no ponto ausente e rejeição intermediária mesmo com saldos finais não negativos, sem comparar índices.
- [x] 2.10 [Testes automatizados] Cobrir precisão/limites, crescimento adversarial da chave, soma zero de custódia e replay de posições encerradas, verificando que financeiro não recebe novos tipos.

## 3. Serviços e transações

Dependências: Etapa 1 homologada, incluindo 1.10 e 1.9 no PostgreSQL 17, e Etapa 2 validada. Nenhuma integração de serviços/transações ou alteração das escritas financeiras existentes será liberada apenas por evidências H2/PostgreSQL 16.8. Bloqueia endpoints de escrita da Etapa 4.

Estado em 25/10/2026: critérios 3.1–3.10 concluídos conforme a matriz de rastreabilidade e as evidências em [evidencias.md](evidencias.md). A validação global aprovou 2.082 casos, com zero falhas/erros e 102 skips esperados, e cobertura BUNDLE LINE 96,16% / BRANCH 85,51%. A homologação 3.10 aprovou separadamente os 26 cenários PostgreSQL e a limpeza. O bloqueio técnico desta etapa para continuidade está atendido; nenhuma implementação da Etapa 4 é iniciada ou autorizada por esta marcação.

- [x] 3.1 [Implementação] Implementar registro de TRANSFERENCIA com referências reais, Clock por mercado e validação integral sob lock da carteira.
- [x] 3.2 [Implementação] Implementar ATRIBUICAO com grupo nulo como origem, destino obrigatório e operação financeira original inalterada.
- [x] 3.3 [Implementação] Implementar estorno único sob lock, motivo opcional, comparação histórica e nenhuma cascata/substituição automática.
- [x] 3.4 [Implementação] Integrar validação local/diferencial a OperacaoPersistenceService.persistir para novas vendas inclusive sem custódias, preservando validação consolidada/códigos.
- [x] 3.5 [Implementação] Integrar proteção de custódias/legado às compras e operações retroativas sem alterar MAX(ordemNoDia)+1 ou fórmulas.
- [x] 3.6 [Implementação] Garantir fronteira REQUIRED apropriada, carteira como primeiro lock, leitura na própria transação, save/flush e rollback; proibir consulta pública REQUIRES_NEW para candidata.
- [x] 3.7 [Implementação] Traduzir constraint de estorno e conflitos transitórios; proteger exclusão de carteira depois dos bloqueios atuais, sem cascade.
- [x] 3.8 [Testes automatizados] Cobrir serviços de registro/atribuição/estorno e vendas com grupo cadastrado/nulo, legado independente e custódia posterior invalidada.
- [x] 3.9 [Testes automatizados] Cobrir rollback de flush/commit, estorno duplicado, referências ausentes e ausência de providers/retries na escrita.
- [x] 3.10 [Testes automatizados] Cobrir venda versus transferência, dois estornos, registro versus exclusão e carteiras independentes em infraestrutura isolada, sem banco local.

## 4. APIs e distribuição

Dependências: etapas2/3; contratos revisados no design. Bloqueia integração Angular da etapa5.

Estado em 26/10/2026: critérios 4.1–4.10 concluídos conforme a matriz de rastreabilidade em [evidencias.md](evidencias.md). Validação global atual: backend BUILD SUCCESS/ExitCode 0, 2.374 casos contabilizados, 2.272 aprovados, 102 skips esperados e nenhum inesperado, zero falhas/erros; JaCoCo BUNDLE LINE 96,18% / BRANCH 85,28%. Frontend: 763 aprovados, zero falhas/erros/skips, ExitCode 0; linhas 98,62% / ramos 93,79%. As homologações PostgreSQL 1.9/1.10/3.10 permanecem válidas e não foram repetidas. O fechamento não marca tarefas da Etapa 5 nem autoriza seu início; a adaptação Angular já aprovada limitou-se à compatibilidade do contrato existente.

- [x] 4.1 [Implementação] Criar DTOs discriminados de cadastro e respostas, proibindo campos controlados/financeiros e preservando decimal exato.
- [x] 4.2 [Implementação] Expor POST de movimentação e estorno com201/Location, contexto de carteira e corpo opcional de motivo conforme contrato.
- [x] 4.3 [Implementação] Expor histórico unificado e detalhe original com vínculo, filtros/situações e desempate por registro, sem paginação/truncamento ocultos.
- [x] 4.4 [Implementação] Evoluir StandardError/códigos/details e classificação conservadora de integridade/transientes, preservando POSICAO_INSUFICIENTE.
- [x] 4.5 [Implementação] Carregar operações/custódias/estornos/nomes no snapshot da distribuição e obter consolidado exclusivamente das operações financeiras.
- [x] 4.6 [Implementação] Evoluir primeiroDeficit, DTOs/mapper/exemplos para identidade genérica e posição exata no endpoint atual, preservando abertas/arrays/ordens de apresentação.
- [x] 4.7 [Documentação] Atualizar OpenAPI e exemplos das rotas/erros/legado/diagnósticos, sem publicar endpoint inexistente ou custódia externa.
- [x] 4.8 [Testes automatizados] Cobrir REST de201/400/404/409, isolamento, campos desconhecidos, filtros, detalhe, Location e duplicidade; exercitar exemplos de erros simultâneos do design e a precedência entre estrutura, referências, estorno duplicado, consolidado, saldo local, custódia invalidada e déficit, distinguindo erro externo da busca e sua causa.
- [x] 4.9 [Testes automatizados] Cobrir schemas/evidências/lossless e OpenApiDocumentationTest, preservando contratos financeiros.
- [x] 4.10 [Testes automatizados] Cobrir snapshot concorrente, neutralização, diagnósticos efetivos, encerradas, consultas sem escrita/N+1 e exclusão protegida; usar históricos representativos completos com filtros/empates/estornos, verificar conjunto, contagem e ordem sem truncamento e registrar tempo, payload, memória quando mensurável e consultas, sem transformar volumes de teste em limites de produto.

## 5. Frontend Angular

Estado reconciliado em 29/10/2026: 5.1–5.9 concluídas pelos quatro incrementos, compatibilidade reutilizada, testes direcionados e correções aprovadas. 5.10 permanece parcialmente comprovada: teclado/foco e viewport real aprovados; avaliação sonora real não executada e dispensada com risco aceito, conforme [excecoes-homologacao.md](excecoes-homologacao.md). Não declarar toda a Etapa 5 integralmente homologada.

Dependências: etapa4/contratos estabilizados. Componentes podem ser preparados depois de revisão do contrato; integração exige backend correspondente.

- [x] 5.1 [Implementação] Criar modelos/serviço HTTP lossless de movimentações/estornos/detalhe, incluindo decimais/details e IDs discriminados seguros.
- [x] 5.2 [Implementação] Atualizar parser/guard/modelos/fixtures de distribuição para evidência genérica com chaves textuais sem Number/parseFloat.
- [x] 5.3 [Implementação] Criar dialog contextual de TRANSFERENCIA/ATRIBUICAO com carteira fixa, referências existentes, quantidade/data e origem por natureza.
- [x] 5.4 [Implementação] Exibir saldo atual auxiliar com aviso temporal, sem replay ou elegibilidade autoritativa no navegador.
- [x] 5.5 [Implementação] Criar histórico separado com linhas/vínculos/datas/situação, filtros e estados loading/vazio/erro/retry.
- [x] 5.6 [Implementação] Criar confirmação de estorno retroativo com motivo opcional, sem POST em cancelamento e sem substituição automática.
- [x] 5.7 [Implementação] Atualizar histórico/distribuição após sucesso com geração/contexto, distinguindo falha posterior de GET e retry apenas de leitura.
- [x] 5.8 [Implementação] Ampliar mensagens do formulário financeiro e de custódia para saldo local, conflitos históricos, futuro, duplicidade e falhas transitórias.
- [x] 5.9 [Testes automatizados] Cobrir contratos/parsers BR/EUA e variantes, filtros, dialogs, cancelamento, estorno, contexto/respostas atrasadas e POST concluído seguido de GET falho.
- [ ] 5.10 [Validação manual e testes automatizados] Verificar teclado/foco/leitor de tela, viewport360px, legibilidade de números/vínculos e estados independentes sem redesign.

## 6. Regressão e documentação

Estado atual em 29/10/2026: 6.1–6.4 e 6.6–6.10 concluídas, com auditoria 6.9 e validação final 6.10 aprovadas. A etapa avançou por autorizações incrementais expressas; 5.10 e 6.5 continuam parcialmente comprovadas nos pontos dispensados. As exceções não alteram os critérios originais nem certificam verificação manual ausente. Procedimento e resultado de 6.10 em [validacao-final.md](validacao-final.md); nenhuma nova execução nesta atualização documental.

Dependências: etapas1–5 completas na futura implementação autorizada. Não confundir validação documental desta entrega com execução destas tasks.

- [x] 6.1 [Testes automatizados] Executar regressão financeira de compra/venda/preço médio/custo/realizado/não realizado/rentabilidade/patrimônio/snapshots com invariância antes/depois de custódia e estorno.
- [x] 6.2 [Testes automatizados] Adaptar fixtures de legado antes produzidas pelo POST permissivo, mantendo consultas de negativos sem afrouxar nova regra.
- [x] 6.3 [Testes automatizados] Confirmar matriz concorrente e isolamento efetivo após espera pelo lock em PostgreSQL descartável autorizado; H2 não substitui essa evidência.
- [x] 6.4 [Testes automatizados] Confirmar precisão BR/EUA, roundtrip de instante persistido, limites cronológicos e rollback integral.
- [ ] 6.5 [Validação manual] Homologar transferência, atribuição, estorno, dependências/cadeia, retroatividade e erro de recarga após sucesso usando dados descartáveis.
- [x] 6.6 [Documentação] Atualizar PRD nas regras de custódia/vendas/diagnósticos e README nas funcionalidades, endpoints, limitações e quantidade real de migrations.
- [x] 6.7 [Documentação] Atualizar AGENTS.md para distinguir operação financeira/custódia, ordem estável e preservação de legado; não alterar changes arquivadas.
- [x] 6.8 [Documentação] Atualizar coleção Postman/contratos/examples e conferir plano de deploy coordenado/rollback sem perda de auditoria.
- [x] 6.9 [Revisão] Auditar cobertura dos32 critérios e cenários extras, deltas completos sem contradições, ausência de fórmulas novas e preservação de alterações preexistentes.
- [x] 6.10 [Validação final] Executar checks apropriados de backend/frontend e OpenSpec estrito após implementação autorizada, revisar diffs/estado Git e atualizar Graphify somente após código; archive/commit requerem autorização separada.
