# Etapa 4 — Administração de usuários

Execução autorizada em 31/10/2026, após aprovação técnica do backend integrado. Escopo: 4.1, 4.3, 4.4 e 4.5. A tarefa 4.2 permanece pendente do cadastro com código inicial da etapa 5; não foi criado POST de usuários, DELETE, alteração de username, senha administrativa, emissão/consumo de códigos ou implementação Angular.

## Contratos entregues

| Operação | Contrato |
| --- | --- |
| GET /usuarios | ADMIN; lista por ID; 200 com DTOs seguros; no-store |
| GET /usuarios/{id} | ADMIN; 200 com DTO seguro ou 404; no-store |
| PATCH /usuarios/{id}/perfil | ADMIN e CSRF; corpo papel/versaoEsperada; 200 com DTO atualizado |
| PATCH /usuarios/{id}/situacao | ADMIN e CSRF; corpo ativo/versaoEsperada; 200 com DTO atualizado |

O DTO público contém id, username, papel, ativo, credencialDefinida, criadaEm, atualizadaEm e versao. Não contém senha, hash, versaoSeguranca, autorização de senha ou identificador de sessão. Campos adicionais no comando são rejeitados. Não há identificador de ator aceito do cliente: o filtro coloca os metadados JWT já verificados no contexto autenticado, sem guardar o token bruto; o serviço revalida a sessão persistida. A autorização ADMIN existe nos Resources e no serviço público, inclusive para chamadas diretas pelo proxy Spring.

O comando adquire singleton de segurança, depois usuário alvo, e revalida o ator após ambas as esperas. Decide versão antes de reconhecer ausência de mudança. Versão obsoleta retorna 409 VERSAO_USUARIO_OBSOLETA. Comando sem alteração retorna 200, sem modificar versões, timestamps, sessões ou auditoria de mudança. A contagem de ADMIN operacional considera somente ADMIN ativo com credencial definida. Autoalterações e operações concorrentes obedecem ao mesmo protocolo; violação retorna 409 ULTIMO_ADMIN_ATIVO.

Alteração efetiva de perfil ou bloqueio incrementa versaoSeguranca na mesma transação da alteração; todos os cookies emitidos com versão anterior passam a ser rejeitados pelo mecanismo persistido existente. Não é necessário atualizar cada linha de sessão. Bloqueio invalida autorizações ainda não consumidas/não invalidadas, sob lock e em ordem de ID. Códigos consumidos ou já invalidados não são reescritos. Habilitação não recupera sessões nem códigos, preservando a versão de segurança do bloqueio; o usuário precisa de novo login. Não houve renovação de JWT nem mudança nos limites de dez minutos/quatro horas.

## Auditoria e atomicidade

SUCESSO é gravado na mesma transação da alteração, com ator, alvo, ação, instante e resultado. Falha de gravação provoca rollback de usuário, versões e autorizações, sem registro falso de sucesso; o cliente recebe 503 SEGURANCA_INDISPONIVEL, sem detalhes SQL. Uma trigger exclusivamente do H2 de teste injeta a falha real de persistência e é removida ao final do teste.

As recusas de negócio por versão obsoleta e último ADMIN registram RECUSADO em transação REQUIRES_NEW independente, iniciada depois de concluída a decisão sem mudança e liberados seus locks. Isso evita uma transação de auditoria aguardando FKs presas pela própria transação suspensa no PostgreSQL. O registro sobrevive ao rollback do chamador; não há alteração no usuário. Se a auditoria não puder persistir, o serviço falha com 503 e não confirma sucesso. Entradas inválidas, identidade não autorizada e alvo inexistente não fabricam usuários de auditoria. Ausência de mudança não gera registro. Não se registra senha, hash, código, JWT, SID ou corpo dos comandos.

## Evidências TDD e integração

Logs locais ficam em target/auth-etapa4, ignorado pelo Git. São evidências desta execução, não artefatos para versionar com dados de teste.

| Ciclo | Evidência real |
| --- | --- |
| Consultas/DTO | 01-consultas-red.log: consulta esperava 200 e recebeu 404; 01-consultas-green.log: 7 testes, incluindo persistência/DTO, sem falha |
| Perfil, versão e último ADMIN | 02-ultimo-admin-red.log e 02-perfil-red.log: endpoints ausentes; 02-perfil-green.log: 5 testes sem falha |
| Bloqueio/habilitação | 03-situacao-red.log: comandos ausentes; 03-situacao-green.log: 8 testes sem falha |
| Recusa e espera por lock | 04-auditoria-ator-red.log: 0 auditorias em vez de 2 e 200 em vez de 401 após expiração na espera; 04-auditoria-ator-green.log: 10 testes sem falha |
| Falha real de auditoria | 05-integracao.log: exceção de persistência escapava sem resposta controlada; 05-integracao-green.log: 59 testes sem falha, com rollback e resposta genérica |
| Grupo autenticação/sessões | 06-autenticacao-sessoes-green.log: 128 testes sem falha; cobre 22 verificações administrativas e todos os testes de autenticação/sessões existentes |
| Contratos diretos do serviço | 08-servico-publico.log: 25 verificações administrativas sem falha; inclui comandos inválidos sem validação HTTP e contexto sem metadados/authority contraditória |
| Aceite completo final | 09-aceite-final.log: 2.507 testes, 2.405 aprovados e 102 ignorados; nenhuma falha/erro; 131 testes do pacote de autenticação, incluindo 25 administrativos, aprovados |

As extensões de verificação de invariantes e regressão foram adicionadas sobre os comportamentos já verdes; não se atribui RED artificial a testes que passaram na primeira execução. Uma falha intermediária em 06-autenticacao-sessoes.log foi identificada na fixture nova: nanossegundos em memória contra arredondamento de microssegundos no banco. O teste passou a comparar o estado persistido antes/depois; a exigência de não reescrever autorizações foi preservada. Não houve mudança de prazo ou de asserções financeiras para obter aprovação.

## Rastreabilidade

| Tarefa/cenário | Verificação |
| --- | --- |
| 4.1 consultas privadas e DTO | adminConsultaEListaDtoSeguroComVersaoOtimista; endpointsExigemAdminECsrfSemAlterarEstado; servicoPublicoExigeContextoAutenticadoEAdmin; papelAdminSemIdentidadeVerificadaNaoDefineAtor; AutenticacaoPersistenceTest |
| Proteção pública sem depender do HTTP | servicoRejeitaComandosInvalidosSemDependerDaValidacaoHttp; servicoRejeitaContextoSemMetadadosEAuthorityContraditoria |
| 4.3 perfil, versão, no-op e revogação | alteracaoEfetivaDePerfilRetornaNovaVersaoEInvalidaSessaoAntiga; versaoObsoletaEhRejeitadaAntesDoComandoSemMudanca; perfilSemMudancaMantemVersaoSessaoEAuditoria; situacaoSemMudancaNaoAtualizaVersoesNemAuditoria; comandosConcorrentesNaoSobrescrevemVersaoNemConfundemNoOp |
| 4.3 auditoria | recusasAuditaveisPersistemSemAlterarUsuario; recusaAuditadaSobreviveAoRollbackDoChamador; falhaNaAuditoriaReverteBloqueioVersaoEInvalidacaoDoCodigo |
| 4.4 último ADMIN e ator | ultimoAdministradorNaoPodeSeRebaixar; ultimoAdminNaoPodeSeBloquearNemContarAdminSemCredencialComoSubstituto; autoAlteracoesConcorrentesPreservamUmAdminOperacional (bloqueio e perfil); autoRebaixamentoPermitidoComOutroAdminInvalidaCookieAntigo; revalidaAtorDepoisDeEsperarLockDoAlvo; revalidaAtorBloqueadoEnquantoEsperaSingleton |
| 4.5 bloqueio/habilitação/códigos/ausência de DELETE | bloqueioInvalidaSessoesECodigoEHabilitacaoNaoRestauraEstadoAntigo; bloqueioNaoReescreveAutorizacoesConsumidasOuJaInvalidadas; validaContratosENaoAceitaAtorOuSegredosNoCorpo |
| 6.4/6.6 parte administrativa | loginEsperandoBloqueioNaoEmiteSessaoAutorizadaAposCommit; sessões independentes anteriores negadas após bloqueio/perfil; concorrência administrativa acima |
| Contratos anteriores e finanças | AutenticacaoHttpTest, SessaoInatividadeHttpTest, ReinicioAutenticacaoTest e suíte financeira existente; inventário financeiro continua exigindo seus 32 mappings, enquanto o inventário OpenAPI inclui os quatro novos mappings administrativos |

Os testes administrativos usam HTTP real via MockMvc, contexto Spring, repositories e transações reais em H2 descartável, com conexões independentes para concorrência e Clock controlado na expiração durante espera. Fixtures geram usernames/senhas efêmeros aleatórios; nenhum usuário de produção foi semeado. Digest de autorização em teste é apenas fixture persistida: não há emissão nem consumo funcional.

## Arquivos desta etapa

- Criados em produção: src/main/java/com/projeto/resources/UsuarioResource.java; src/main/java/com/projeto/services/UsuarioAdministracaoService.java.
- Produção existente estendida: dto/UsuarioResponse.java; entities/autenticacao/Usuario.java e AutorizacaoRedefinicaoSenha.java; repositories/autenticacao/UsuarioRepository.java e AutorizacaoRedefinicaoSenhaRepository.java; security/CookieAuthenticationFilter.java, todos sob src/main/java/com/projeto.
- Teste criado: src/test/java/com/projeto/autenticacao/AdministracaoUsuariosHttpTest.java.
- Testes adaptados: AutenticacaoPersistenceTest (DTO com versao), AutenticacaoHttpTest (separar inventário administrativo do financeiro, preservando a matriz financeira), resources/OpenApiDocumentationTest (incluir os quatro mappings novos). Nenhuma asserção de negócio financeiro removida ou relaxada.
- Documentação: este relatório, tasks.md, design.md e specs/user-administration/spec.md; backend-integrado.md recebe referência histórica ao recorte posterior.
- Nenhuma migration, dependência, configuração de segurança financeira, cálculo financeiro, teste Angular ou frontend alterado nesta etapa. Reutilizadas as tabelas da migration 009: usuario, sessao_autenticacao, autorizacao_redefinicao_senha, controle_seguranca e auditoria_usuario. Não há estrutura de banco nova.

## Pendências e limites preservados

- 4.2: cadastro completo depende da emissão inicial da etapa 5 e continua desmarcado. Toda a etapa 5 e Angular 7/8 seguem fora deste ciclo.
- 2.2: pendência histórica de processo/TDD da integração anterior não é apagada pelo TDD desta etapa.
- 6.2: deduplicação/eventos do cliente continuam pendentes. 6.4: os comandos administrativos e login versus bloqueio agora têm testes reais, mas reset/cliente ainda não são entregues. 6.6: invalidação global administrativa de sessões independentes está coberta; temporizadores e sincronização visual de abas permanecem pendentes. Nenhuma dessas tarefas mistas deve ser marcada integralmente concluída.
- 10.1: regressão backend renovada, mas cenários Angular e integração futura continuam pendentes; documentação operacional/OpenAPI de segurança completa permanece em 9.x.
- 10.4: homologação da migration 009 e concorrência PostgreSQL NÃO executadas. H2 não comprova os detalhes de lock/FK do PostgreSQL. Não foi acessado banco real, Docker, PostgreSQL de desenvolvimento nem provider externo.
- Revogação é efetiva para novas decisões após commit; uma requisição financeira já autorizada pode concluir conforme D7. Nenhum protocolo/lock financeiro foi alterado.

## Aceite final

4.1, 4.3, 4.4 e 4.5 concluídas e verificadas; 4.2 permanece pendente. A change contém 26 tarefas concluídas e 30 desmarcadas. Nenhuma parcial anterior foi marcada como concluída por este recorte.

Aceite final executado sequencialmente, sem Maven concorrente:

```powershell
.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" verify
```

BUILD SUCCESS em 2min03s. **2.507 testes: 2.405 aprovados, 102 ignorados, zero falhas e zero erros.** Os 102 ignorados são os mesmos grupos opcionais PostgreSQL/diagnósticos/PoC da execução anterior, não desativações introduzidas nesta etapa; a relação por classe está em target/auth-etapa4/resultados.json. A quantidade total aumentou exatamente pelas 25 verificações administrativas. O pacote de autenticação totaliza 131 testes aprovados. A regressão financeira completa do backend foi executada dentro da suíte, incluindo Resources, serviços, repositórios, precisão, custódia, auditoria e migrations H2. No subconjunto de Resources financeiros são 246 testes registrados, 242 aprovados e quatro PostgreSQL opcionais ignorados. O inventário OpenAPI passou com os quatro novos mappings, sem relaxar contratos financeiros.

| JaCoCo global | Baseline anterior | Aceite final |
| --- | --- | --- |
| Linhas | 3.913/4.068 — 96,19% | 3.995/4.151 — **96,24%** |
| Ramos | 2.022/2.385 — 84,78% | 2.083/2.453 — **84,92%** |

Ambos os portões >=80% aprovados; ambos os percentuais globais superiores ao baseline anterior. Nenhum limite, exclusão ou configuração de cobertura alterado. Relatórios HTML/XML em target/site/jacoco, analisando 265 classes; a cobertura anterior está preservada localmente em target/auth-etapa4/jacoco-anterior.xml. A primeira suíte completa (07-verificacao-completa.log) já havia passado com 2.505 testes; os testes públicos adicionais foram verificados em 08 e incluídos no aceite completo 09, com relatório limpo por jacoco.append=false.

OpenSpec `validate adicionar-autenticacao-e-gestao-de-usuarios --strict`: válido. `git diff --check`: exit 0; há apenas avisos Git de normalização LF/CRLF em alterações preexistentes. Os arquivos novos e alterados também foram verificados quanto a espaços finais, sem ocorrência. Graphify atualizado por AST após as alterações finais de código/testes (graphify-aceite.log); os avisos conhecidos de 12 JSONs sem nós e ausência de tree_sitter_sql permanecem, sem instalação adicional, LLM ou alegação de validação SQL.

Comparação SHA-256 dos 1.152 arquivos iniciais: **1.139 idênticos, 13 alterados dentro do escopo, quatro novos e nenhum removido**. Inventário em target/auth-etapa4/conferencia-escopo.json. README, teste Angular, ZIP, migrations, dependências, produção financeira e demais alterações locais fora do recorte permanecem idênticos ao baseline. Nos oito arquivos de produção novos/alterados, a busca de JWT/hash BCrypt/chave privada literais não encontrou credencial fixa; revisão confirmou ator apenas no contexto e fixtures fora da aplicação publicada.

Git status final: 32 arquivos rastreados modificados (mesmas entradas que já estavam modificadas no início), 66 arquivos não rastreados no inventário completo e zero arquivos no stage. Esse estado inclui mudanças de ciclos anteriores e não representa autoria de todas as alterações nesta etapa. Status completo em target/auth-etapa4/git-status-final.txt; relatório lista separadamente os arquivos desta etapa. Nenhum staging, commit, push, merge, arquivamento ou deploy executado. Implementação interrompida para revisão; cadastro/etapa 5 e Angular aguardam autorização posterior.
