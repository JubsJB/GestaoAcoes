# Reorganização da implementação

Estado histórico deste plano: estratégia geral aprovada pelo solicitante com condições, incorporadas em revisão exclusivamente documental. A execução posterior de backend, administração e reset consta nos relatórios próprios; os recortes Angular A–H foram depois autorizados e verificados localmente em 31/10/2026, conforme frontend-integrado.md. Produção continua pendente por decisão expressa do solicitante. As tabelas abaixo preservam as fronteiras e pendências do ciclo original, não o estado atual das tarefas; consultar tasks.md e os relatórios para conclusão posterior. O bloco original inclui 2.1–2.8, 3.1–3.5 e partes indispensáveis de servidor da etapa 6, com regressão financeira e fixtures isoladas, mantendo administração, reset e Angular fora daquele ciclo.

## Problema de sequenciamento

A etapa 1 entregou persistência e continua concluída. As tarefas 2.1–2.8 tratam autenticação e integração com Spring Security, mas a política HTTP completa está em 3.1–3.5. Uma cadeia que apenas exija autenticação ainda permitiria escritas financeiras por USER; uma negação temporária de todas as APIs também bloquearia ADMIN e os contratos HTTP de sucesso existentes. Nenhuma dessas alternativas deve ser adotada como entrega final da etapa 2.

O predicado de dez minutos depende da última atividade persistida. Aplicar esse predicado sem oferecer o protocolo de atividade da etapa 6 produz, na prática, sessões de apenas dez minutos desde o login, mesmo para quem continua trabalhando. Desativar o predicado, renovar em qualquer request ou alterar o JWT para dez minutos também não atende ao design.

A escolha do solicitante foi reorganizar o planejamento, em vez de aprovar o bloqueio temporário das APIs e o corte sem renovação. A aprovação posterior da estratégia autoriza esta atualização documental, sem iniciar código ou etapas posteriores.

## Organização aprovada para planejamento

Manter os IDs e as responsabilidades existentes, mas organizar a próxima entrega como um **bloco integrado de segurança do backend**, abrangendo autenticação, autorização HTTP e protocolo de atividade do servidor. As etapas numéricas continuam sendo grupos de tarefas; não representam, isoladamente, versões publicáveis. Implementar em ciclos TDD pequenos dentro desse bloco, com revisão conjunta ao final.

O bloco somente poderá ser executado após nova aprovação explícita da implementação integrada. A estratégia e suas condições já foram aprovadas para planejamento e agora estão refletidas em proposal.md, design.md e tasks.md. As specs mantêm seus requisitos: a reorganização altera sequência e pontos de controle, não os contratos de segurança. IDs e checkboxes existentes são preservados, sem duplicação.

| Grupo / IDs preservados | Responsabilidade na próxima entrega planejada | Critério de revisão |
| --- | --- | --- |
| 2.1–2.8 | Dependências, configuração segura, BCrypt, login, JWT/cookie, CSRF, bootstrap, limites de login, sessão, identidade e logout | Contratos HTTP reais, segredos ausentes das respostas, validação criptográfica e persistida, revogação e falha fechada |
| 3.1–3.5 | Matriz financeira D6 completa, allowlist/default deny, proteção em métodos, erros e paths reais/proxy | Anônimo negado, USER consulta e não escreve, ADMIN executa contratos financeiros existentes; sem efeitos nos caminhos negados |
| 6.1 | POST /auth/atividade e atualização persistida com CSRF, Clock e revalidação após espera de lock | Atividade válida renova somente o prazo ocioso; igualdade de dez minutos ou quatro horas nega e não ressuscita sessão |
| 6.2, parte de servidor | GETs, /me, /csrf e polling não renovam; timestamp do cliente não determina o prazo | Testes HTTP com relógio controlado; deduplicação de eventos do cliente continua posterior |
| 6.3 | Coerência entre quatro horas absolutas, JWT, banco e cookie, sem refresh | Atividade não altera exp; mudar configuração não prolonga sessões existentes; exemplo 08h00/08h40/12h00 |
| 6.4, parte independente de administração/reset | Corridas de atividade, expiração e logout; versão persistida e limites de requests já autorizados | Transações reais e decisões após commit; cenários funcionais dependentes de 4.x/5.x continuam posteriores |
| 6.6, parte de servidor | Dois clientes com o mesmo cookie/SID e outro navegador com SID distinto | Atividade de B mantém prazo compartilhado de A; logout invalida o SID comum; outro SID não é revogado pelo logout individual |
| 10.1–10.2, verificações pertinentes ao backend | Adaptar fixtures financeiras para autenticação/CSRF reais e executar regressão e cobertura | Nenhum teste ignorado ou enfraquecido para acomodar a cadeia; sem desligar segurança; portões e cobertura existente preservados |
| 10.5–10.6, verificações pertinentes à entrega | Rastreabilidade, Graphify após código, OpenSpec strict, diff/status e relatório | Evidências verificáveis; nenhuma operação Git ou arquivamento automático |

Uma tarefa que contenha também cenários posteriores, como 6.2, 6.4, 6.6 ou 10.1, permanece desmarcada até cumprir sua redação integral. A entrega parcial deve ser registrada com cenários e evidências no relatório, sem duplicar checkboxes ou declarar todo o ID concluído. A introdução e as dependências de tasks.md refletem esta ordem de execução, mantendo os IDs; não renumerar tarefas para simular conclusão de uma etapa isolada.

### Rastreabilidade obrigatória das partes da etapa 6

| ID original | Parte indispensável deste ciclo de backend | Parte posterior / dependência | Regra para o checkbox |
| --- | --- | --- | --- |
| 6.1 | Endpoint de atividade, CSRF, Clock, lock e corte exato após espera | Nenhuma dependência de tela para validar o contrato HTTP | Somente concluir após todos os cenários do ID passarem |
| 6.2 | GET/polling/me/csrf/timers sem renovação no servidor, timestamp de cliente ignorado e atividade tardia negada | Deduplicação de eventos reais do cliente em 7.5 | Manter pendente até completar também a parte do cliente |
| 6.3 | Limite absoluto, exemplo de jornada, configuração capturada no login e ausência de refresh | Nenhuma dependência de Angular para testes HTTP com Clock | Somente concluir após todos os cenários do ID passarem |
| 6.4 | Corridas de atividade/expiração/logout/versão, request previamente autorizado e cookie compartilhado revogado mesmo sem sinal entre clientes | Corridas com comandos funcionais de bloqueio/reset dependem de 4.3–4.5 e 5.2/5.5 | Manter pendente; fixtures de versão/ativo verificam leitura e invalidação, sem provar comandos futuros |
| 6.5 | Não faz parte deste ciclo; a negação de acesso independe de limpeza | Planejamento de limpeza e medição de eventos/cliente continuam pendentes | Manter pendente |
| 6.6 | Dois clientes com SID compartilhado, atividade válida de B mantendo A e sessões independentes entre navegadores | Timer local/integração visual dependem de 7.5–7.7; invalidação por comando administrativo depende de 4.3–4.5 | Manter pendente até cumprir o ID integral |

Para cada parte entregue futuramente, o relatório registrará ID original, cenário OpenSpec, teste público, resultado/evidência, comportamento implementado e dependência ainda não resolvida. Nenhuma parte está entregue por esta revisão documental.

## Ordem técnica dentro do bloco

1. Reconfirmar Git e baseline; conferir BOM/APIs; preparar configuração, BCrypt e bootstrap por testes nas fronteiras aprovadas. Criar usuários necessários aos testes somente por fixtures isoladas, sem contas ou credenciais fixas na aplicação.
2. Implementar cookie JWT, sessões persistidas, CSRF e endpoints de autenticação, incluindo os handlers de erro necessários de 3.4. A matriz de autorização e as fixtures financeiras acompanham a integração da cadeia, sem aceitar uma versão aberta ou com bloqueio geral como entrega.
3. Implementar e verificar a matriz 3.1–3.5 sobre os mappings existentes, mantendo consultas compartilhadas e a segurança fora das transações financeiras.
4. Completar o protocolo de atividade do backend e seus cortes exatos, incluindo concorrência e leitura sem renovação. Esse protocolo deve estar presente antes de considerar a sessão funcionalmente pronta.
5. Executar regressão completa do backend em H2 descartável e cobertura, revisar a rastreabilidade, validar OpenSpec e apresentar o diff. Uma falha de regressão não vira exceção aceita por planejamento.

Estados intermediários de desenvolvimento não são candidatos a publicação. Nenhum deploy está autorizado. A matriz financeira não altera cálculos, locks, precisão ou regras de domínio. ADMIN deve continuar executando as APIs financeiras e USER deve poder somente consultar conforme D6; nenhum endpoint financeiro pode permanecer público ou ser bloqueado indiscriminadamente como solução definitiva. Os testes HTTP existentes serão preservados e adaptados para cookie/CSRF, sem ignorá-los, desligar segurança ou reduzir suas verificações para obter GREEN.

## Responsabilidades que continuam posteriores

- **4.1–4.5:** serviço funcional e endpoints administrativos de usuários, último ADMIN, mudança de perfil, bloqueio e habilitação. O bloco integrado apenas verifica o estado persistido atual e sua versão; não implementa esses comandos.
- **5.1–5.5:** emissão e consumo funcionais de códigos, definição/redefinição de senha e respectivas corridas. Não criar endpoints vazios nem marcar seus cenários de CSRF como testados antes de existir o fluxo real. A proteção geral deve abrangê-los quando forem adicionados.
- **6.2/6.6, partes do cliente:** deduplicação de eventos e timer local sem logout indevido ficam com a integração Angular. **6.4, partes dependentes de administração/reset:** cenários de integração somente após 4.x/5.x. **6.5:** limpeza e medição permanecem pendentes, sem condicionar a negação de sessões à limpeza.
- **7.1–7.8 e 8.1–8.4:** Angular, eventos reais, sincronização visual, guards, telas e restrições de ações. O endpoint do servidor não prova interação humana; o cliente oficial ainda precisa cumprir o protocolo. Não declarar a experiência completa de inatividade entregue apenas com testes do servidor.
- **9.1–9.4:** documentação pública e integração operacional completa. Testes de cookies/TLS e configuração segura necessários ao bloco não equivalem a concluir implantação ou integração completa do Swagger/Postman.
- **10.3:** testes/build/cobertura do frontend quando alterado. **10.4:** homologação PostgreSQL, incluindo a migration 009, continua pendente e depende de autorização para banco descartável dedicado. Não usar banco com dados reais.

## Requisitos que não mudam

JWT HS256 apenas no cookie HttpOnly; Secure em produção, SameSite=Lax, Path=/, sem Domain; CSRF separado, obrigatório nas mutações, sem exceção por Bearer ou ferramenta; sem JSESSIONID, refresh token ou credencial no JavaScript/Web Storage. Limite absoluto configurável de quatro horas e dez minutos consecutivos sem atividade válida persistida, sem tolerância positiva. Revogação individual por SID e global por versão, verificação no banco por request e 503 seguro quando não for possível verificar. Carteiras compartilhadas, perfis ADMIN/USER e contratos financeiros preservados.

## Alternativa considerada

Manter apenas a etapa 2 na próxima execução, como componentes ainda não integrados ao servidor, exigiria reformular seus critérios de aceite e não entregaria os contratos HTTP/JWT/CSRF já previstos. Pode servir como trabalho interno, mas não resolve a entrega solicitada nem permite marcar 2.3, 2.7 e 2.8 concluídas. A estratégia aprovada para planejamento é o bloco integrado acima, sem criar um modo temporário de autenticação.

## Pontos de controle e pendências

1. **Antes de código:** obter nova aprovação explícita para executar a implementação integrada conforme este planejamento. Aprovação da estratégia não equivale a autorização de execução nesta revisão.
2. **Na integração da cadeia:** demonstrar a matriz D6 com fixtures isoladas, cookies e CSRF; ADMIN mantém contratos existentes, USER somente consulta, anônimo é negado, caminhos negados não produzem efeitos. Adaptar testes HTTP financeiros junto à integração, sem exceção temporária aceita no relatório.
3. **Antes do aceite de sessão:** demonstrar atividade explícita e cortes exatos de dez minutos/quatro horas no servidor, incluindo concorrência e reinício; /me e leituras não renovam. Nenhuma tolerância temporal ou expiração fixa de dez minutos desde login é introduzida.
4. **Antes da entrega integrada:** regressão financeira e cobertura aprovadas, evidências TDD, rastreabilidade parcial por ID, Graphify após código, OpenSpec strict e Git diff/status. Não concluir IDs com cenários futuros pendentes.
5. **Dependências posteriores:** comandos administrativos (4.x), reset (5.x), cliente oficial (7.x/8.x), integração pública/operacional completa (9.x) e homologação PostgreSQL 009 (10.4). A experiência completa do cliente não será declarada entregue pelo backend. Calibração BCrypt, confirmação das APIs efetivas e configuração TLS/mesma origem serão verificadas durante as tarefas correspondentes, sem antecipar deploy.

Nesta revisão foram atualizados somente proposal.md, design.md, tasks.md e este documento. Specs e requisitos permanecem preservados. Nenhum código, migration, teste, frontend ou configuração de runtime foi alterado. A etapa 1 e seu relatório histórico permanecem preservados; o andamento continua 5/56 tarefas concluídas e 51 pendentes. Não houve testes de aplicação nem medição nova de cobertura. A validação OpenSpec e a conferência de Git são registradas na resposta da revisão, sem atribuir a elas prova de funcionamento do backend. Nenhum commit, push, merge ou arquivamento foi realizado; aguardar aprovação antes de iniciar a implementação integrada.
