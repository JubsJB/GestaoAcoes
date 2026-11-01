# Etapa 5 — Definição inicial e redefinição de senha

Escopo autorizado: 5.1–5.5 e conclusão de 4.2, nos recortes A–E aprovados pelo solicitante. Preservados os contratos anteriores de autenticação, sessão e administração. Nenhum Angular, dashboard, funcionalidade financeira, migration, staging, commit, push, merge ou arquivamento integra esta entrega.

## Contratos e mecanismos

| Endpoint | Entrada e proteção | Sucesso |
| --- | --- | --- |
| POST /usuarios | username/papel; ADMIN no endpoint e serviço; CSRF; sem senha/ator livres | 201, Location /usuarios/{id}, no-store, envelope usuario/autorizacao |
| POST /usuarios/{id}/redefinicoes | versaoEsperada obrigatória; ADMIN no endpoint e serviço; CSRF | 200, no-store, mesmo envelope |
| POST /auth/redefinicoes/confirmar | username/codigo/senha/confirmacao; público com CSRF obrigatório | 204, no-store, sem autenticação automática ou emissão de JWT |

Envelope: `{usuario: UsuarioResponse, autorizacao: {codigo, motivo, expiraEm}}`. Versão otimista pública; versão de segurança interna. Emissão incrementa somente a versão otimista, inclusive no cadastro inicial (versao=1), sem invalidar sessões. Consumo incrementa a versão de segurança e a versão otimista com a alteração persistida, negando cookies anteriores em todos os contextos a partir do commit. Motivo é inferido no servidor: INICIAL sem credencial, REDEFINICAO com credencial. O titular escolhe sua senha no endpoint público; o administrador não a recebe nem a define por comando administrativo.

Os novos POSTs retornam 409 USUARIO_DUPLICADO, CONFLITO_VERSAO ou USUARIO_BLOQUEADO conforme o caso, 404 para alvo inexistente e validação 400 para corpo inadequado. O contrato de conflito dos PATCHs anteriores foi preservado. Falhas públicas por identidade/código usam sempre 400 REDEFINICAO_INVALIDA e mensagem equivalente, sem distinguir inexistência, bloqueio, erro, expiração, revogação ou reuso. Senha inválida/divergente usa 400 SENHA_INVALIDA antes de consultar estado de conta. Mantidos 403 CSRF, 429 com Retry-After e 503 SEGURANCA_INDISPONIVEL sem SQL/segredos.

Código: 16 bytes de SecureRandom (128 bits), Base32 RFC 4648 sem padding, 26 caracteres. Entrada normaliza caixa e remove espaços ASCII/hífens; não altera a senha. Verificador HMAC-SHA-256 em hexadecimal sobre prefixo binário de 24 bytes (ID long do usuário e UUID da autorização) seguido do código normalizado ASCII. Somente o verificador é persistido; comparação por MessageDigest.isEqual. Para identidade inelegível há verificação dummy, sem conta fictícia nem promessa de latência de rede idêntica. A resposta administrativa é a única exibição do código; DTO de autorização e request de confirmação possuem toString protegido. Nenhum endpoint recupera código, senha ou hash.

Runtime requer auth.reset.secret ou AUTH_RESET_SECRET em Base64, com pelo menos 32 bytes, distinta em bytes da chave JWT. Ausência/valor inseguro falha fechado no runtime web, sem expor valores ou causas de binding. Instâncias/reinícios devem compartilhar a chave para preservar autorizações vigentes. Fixtures geram chave independente e credenciais efêmeras; não há segredo padrão ou usuário semeado na aplicação publicada. Bootstrap sem servidor web continua separado.

## Transações, locks e auditoria

Emissão/cadastro: singleton → usuário → autorizações pendentes em ordem de ID; revalidar ADMIN após esperas, inclusive no lock de autorizações, e aferir horário depois delas. Invalidar autorizações anteriores, gravar a nova e auditoria SUCESSO na mesma transação. Cadastros normalizados concorrentes são serializados e protegidos também pela constraint única. A entidade recém-inserida é recarregada sob lock antes do FORCE_INCREMENT: Hibernate mantém lock WRITE na inserção e ignorava o incremento, defeito detectado pelo teste de contrato, sem enfraquecer a expectativa de versão.

Consumo: mesmo singleton, usuário e autorizações; validar estado/código e prazo com Clock depois das esperas. BCrypt usa a política existente: mínimo de oito caracteres e máximo de 72 bytes UTF-8, sem trim/truncamento, com confirmação igual. Reavaliar o prazo após BCrypt antes de gravar. Na igualdade de quinze minutos, negar. Gravar hash, credencial definida, consumo, incremento de segurança e auditoria na mesma transação. Conta bloqueada não é habilitada. Emissões não derrubam sessões; bloqueio/habilitação e reemissão não restauram códigos antigos. Não houve refresh, reemissão automática de JWT ou alteração de dez minutos/quatro horas.

Erros de código contra autorização vigente decrementam tentativas na transação de decisão; o quinto grava invalidacao e zero restantes. O erro HTTP é produzido depois do commit, evitando rollback acidental do contador. Senha inválida/divergente e recusa por CSRF não decrementam tentativas. Erro de persistência impede confirmação e retorna 503, em vez de alegar que uma tentativa não gravada foi confirmada.

Auditoria de cadastro e autorização identifica ADMIN exclusivamente pelo contexto autenticado. Consumo bem-sucedido registra REDEFINICAO_SENHA com ator/alvo titular comprovado pelo código; o emissor permanece na autorização. Código inválido não comprova autoria e não atribui evento falso ao titular; seus contadores são persistidos. Recusas administrativas de negócio conhecidas preservam RECUSADO em transação independente, somente depois de liberar os locks originais, seguindo o mecanismo da etapa 4. Falhas de banco não geram auditoria falsa de sucesso.

O rate limiting é uma fase anterior ao consumo: depois de TLS/CSRF e antes do binding MVC. Registra primeiro IP em transação concluída, depois username+IP quando extraível do JSON; cada fase usa singleton e não mantém locks para a transação de consumo. Assim, contas inexistentes, senha inválida, campos extras, JSON ilegível ou corpo ausente/excessivo são contabilizados na camada apropriada. JSON ilegível/sem identidade conta por IP; JSON com identidade conta também por username canônico+IP. Rejeições CSRF encerram antes dessa fase, sem consumir código. O corpo deste endpoint é limitado a 16KiB; acima disso responde 400 genérico após contabilizar IP. Não são registrados corpo, código ou senha.

Revisão adicional detectou que encaminhar JSON malformado ao MVC permitia ao tratamento de exceção registrar o token bruto do parser em DEBUG. O teste reproduziu o vazamento; a falha de parsing agora é rejeitada no próprio filtro com 400 genérico, preservando a contagem por IP e sem consumir tentativas do código. Testes explícitos capturam logs MVC DEBUG nos caminhos de sucesso, parsing e desserialização inválida e verificam ausência de código/senha, inclusive em listagens e persistência.

Limites persistidos: dez requisições por username+IP e cinquenta por IP em quinze minutos; não apenas falhas. Identidade/IP usam tipos de contador já existentes, distintos dos limites de login. Janela expira na igualdade; Retry-After é arredondado para cima, sem bloqueio permanente. Usa endereço da conexão, sem confiar em X-Forwarded-For arbitrário. Counters já confirmados sobrevivem a 400, erro de binding e rollback de consumo; indisponibilidade do limitador nega antes da troca de senha.

O filtro JWT ignora somente o POST público exato de confirmação, como faz para login; a allowlist também é exata e CSRF continua obrigatório. Cookie JWT antigo/adulterado/expirado não impede usar código válido. A confirmação não emite nem apaga cookie de autenticação; a sessão antiga é negada no backend e o usuário precisa de login separado.

## Evidências por recorte

Logs locais ignorados em target/auth-etapa5. A primeira falha de compilação da fixture por API List.getFirst incompatível com Java 17 e a primeira comparação com versão de entidade detached não contam como RED funcional; foram corrigidas antes das respectivas evidências HTTP abaixo.

| Recorte | RED observado | GREEN / revisão |
| --- | --- | --- |
| A | a-runtime-red.log: contexto web aceitava ausência da chave HMAC | a-runtime-green.log: 18 testes, configuração HMAC e JWT |
| B | b-emissao-http-red.log: 200 esperado/404; b-recusa-http-red.log: duas auditorias esperadas/zero | b-aceite-green.log: 28 testes, incluindo os 25 administrativos anteriores; reemissão no mesmo instante incrementa versão e invalida anterior |
| C | c-consumo-red.log: 204 esperado/401 pelo cookie antigo; c-tentativas-red.log: zero tentativas restantes esperado/cinco; c-rate-red.log: 429 esperado/400 na 11ª requisição | respectivos GREENs; c-aceite.log: 42 testes de reset, autenticação e sessões; cortes exatos, política, CSRF, limites e payloads inválidos |
| D | d-cadastro-red.log: 201 esperado/405; d-cadastro-green.log identificou versão zero em vez de um no registro recém-inserido | d-aceite-green.log: 43 testes; cadastro → definição → login separado e regressões |
| E | e-espera-ator-red.log: 401 esperado/200 após espera por autorização | e-espera-ator-green.log: correção confirmada; e-integracao.log: 37 testes de cadastro/reset, concorrência e rollback; e-autenticacao-reinicio.log: 194 testes incluindo segurança anterior, três contextos reais de reinício e OpenAPI |
| E — sigilo de logs | e-parser-log-red.log: código aparecia em log MVC DEBUG na falha de parsing | e-logs-seguros.log: sigilo em emissão/consumo/listagens; e-parser-log-green.log: 20 testes HTTP de redefinição aprovados; e-parser-binding-log.log: parsing e tipos inválidos sem código/senha em logs e sem consumo de tentativas |

Os testes adicionais de invariantes sobre comportamentos já verdes são verificações de integração/regressão; não se atribui RED retroativo artificial a eles. Na revisão foi mantida injeção por construtor no Resource de autenticação e acrescentadas verificações diretas do serviço e equivalência das recusas públicas. A suíte completa final inclui essas extensões posteriores ao grupo de 194 testes.

## Rastreabilidade

| IDs | Evidência principal |
| --- | --- |
| 5.1 | RedefinicaoRuntimeTest; emissaoAdministrativaRetornaCodigoUmaVezSemRevogarSessao; segundaEmissaoSubstituiAnteriorMesmoComClockIgual; emissaoExigeAdminEProtecaoCsrf; recusasAdministrativasSaoAuditadasSemMudarContaOuCodigos |
| 5.1 sigilo | codigoESenhaNaoAparecemEmListagensOuLogsMesmoComMvcDebug; codigoNaoVazaNosLogsQuandoJsonEhMalformado (inclui desserialização de tipos inválidos) |
| 5.2 | consumoPublicoTrocaSenhaERevogaTodasAsSessoesSemEmitirJwt; cadastroCriaContaSemSenhaECodigoInicialAteLoginSeparado; adminSemCredencialSoSeTornaOperacionalAposDefinicaoInicial; reinicioPreservaCodigoTentativasConsumoERevogacao |
| 5.3 | cincoErrosPersistemEImpedemUsoPosteriorDoCodigoCorreto; expiracaoTemBordaExataDeQuinzeMinutos; senhaInvalidaOuDivergenteNaoConsomeTentativasDoCodigo; csrfRejeitadoNaoConsomeCodigoECodigoAceitaCaixaEspacosEHifens; codigoSubstituidoOuConsumidoNaoPodeSerReutilizado; bloqueioEHabilitacaoNaoRestauramCodigoNemTrocamSenha; codigoNaoPodeSerTransferidoParaOutraIdentidade |
| 5.4 | limitaDezRequisicoesMesmoComSenhaInvalidaEIdentidadeInexistente; cinquentaJsonsMalformadosContamPorIpERetentativaLiberaNaBorda; camposDesconhecidosTambemContamNoLimiteDaIdentidade; corpoExcessivoContaPorIpSemSerReproduzidoNoErro; limiteDeIdentidadeTambemEhAtomicoEmRequestsConcorrentes; falhasPublicasNaoRevelamExistenciaEstadoOuMotivoDaRecusa |
| 5.5 | RedefinicaoConcorrenciaTest (consumo duplo, emissão/consumo, login/reset, bloqueio/reset, quinto erro, cadastros e expiração/ator após espera); RedefinicaoAtomicidadeTest (falhas em usuário, autorização, auditoria, emissão, cadastro e limitador) |
| 4.2 | CadastroUsuarioHttpTest: normalização, duplicidade e concorrência, conta sem senha, código INICIAL, ADMIN não operacional antes da definição, rejeição de senha/ator/campos extras e proteção do serviço público |

Fixtures isoladas, Clock controlado, transações reais, conexões independentes e locks observados em H2; sem mocks de serviços/repositories internos. Triggers de falha pertencem somente aos testes e são removidas em finally. Reinício fecha/reabre três contextos sobre H2 em memória mantido por keeper, chave HMAC estável e porta efêmera de loopback. Nenhum banco persistente ou provider real usado.

## Arquivos do recorte

- Produção criada: dto/EmissaoAutorizacaoResponse; security/PasswordResetConfig, CodigosRedefinicao, RedefinicaoSenhaService, RedefinicaoRateLimiter e RedefinicaoRateLimitFilter, sob src/main/java/com/projeto.
- Produção estendida: entities/autenticacao/Usuario e AutorizacaoRedefinicaoSenha; security/AuthConfig, SecurityWebConfig e CookieAuthenticationFilter; resources/AutenticacaoResource e UsuarioResource; services/UsuarioAdministracaoService.
- Testes criados: RedefinicaoRuntimeTest, RedefinicaoHttpSupport, RedefinicaoSenhaHttpTest, CadastroUsuarioHttpTest, RedefinicaoConcorrenciaTest e RedefinicaoAtomicidadeTest.
- Testes/configuração adaptados: application-test.properties (chave efêmera); ReinicioAutenticacaoTest; AdministracaoUsuariosHttpTest (POST /usuarios agora existe: corpo ausente é 400, DELETE continua negado); OpenApiDocumentationTest (três operações HTTP aprovadas). Nenhuma asserção financeira enfraquecida.
- Documentação: este relatório e os artefatos de planejamento/rastreabilidade da change. Nenhuma dependência ou migration nova; todas as tabelas reutilizam 009. Listagem final discriminada pelo baseline em target/auth-etapa5/conferencia-escopo.json.

## Pendências e riscos

Homologação da migration 009 e concorrência PostgreSQL permanecem em 10.4: H2 não prova o comportamento do PostgreSQL. 2.2 conserva a lacuna histórica de processo/TDD. 6.2 continua dependente de eventos/deduplicação do cliente; 6.4/6.6 ampliam agora sua parte backend de reset, mas não concluem responsabilidades de cliente/abas. 10.1 mantém a integração futura; Angular 7/8 e documentação operacional completa 9.x continuam pendentes.

Configuração publicada exige TLS/mesma origem, ambas as chaves runtime e endereço de proxy confiável conforme 9.4. Rotação da chave HMAC torna códigos pendentes inutilizáveis e exige reemissão; não há keyring novo. ADMIN entrega o código por canal confiável. Perda da resposta após commit não permite recuperar o código: solicitar nova emissão explícita com a versão atual, invalidando a anterior. Não existe retry automático nem idempotência que recupere código secreto.

O singleton serializa também reset/rate limiting e mantém BCrypt dentro da decisão atômica: solução proporcional à arquitetura acadêmica, com contenção a medir se a carga crescer. Revogação vale para novas decisões após commit; não cancela requisição financeira já autorizada. Locks e regras financeiras permanecem preservados.

## Aceite final

**4.2 e 5.1–5.5 concluídas e verificadas; 32/56 tarefas concluídas e 24 desmarcadas.** As tarefas parciais anteriores permanecem desmarcadas e não houve implementação Angular ou avanço operacional.

Comando de aceite sequencial (e-aceite-definitivo.log), somente H2 descartável e integrações reais opcionais desabilitadas:

```powershell
.\mvnw.cmd -B "-Dtemporal.test.postgresql.url=" "-Dtemporal.test.postgresql.enabled=false" "-Dbroker.test.postgresql.enabled=false" "-Dtemporal.poc.enabled=false" "-Djacoco.append=false" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" verify
```

**BUILD SUCCESS em 2min19s: 2.555 testes, 2.453 aprovados, 102 ignorados, zero falhas e zero erros.** São 48 testes a mais que o baseline da etapa 4, incluindo a extensão de reinício e os testes de sigilo de logs. O pacote de autenticação totaliza 179 testes aprovados; todos os 25 administrativos anteriores permanecem aprovados. Regressão financeira completa executada dentro da suíte: Resources, serviços, repositórios, custódia, preços, precisão, auditoria e migrations H2. No subconjunto de Resources financeiros, 246 testes registrados, 242 aprovados e quatro PostgreSQL opcionais ignorados. Os 102 ignorados totais são os mesmos grupos condicionais PostgreSQL/diagnósticos/PoC do baseline, sem novos skips de segurança. Relação por classe em resultados.json. As suítes completas anteriores e-verify.log (2.553) e e-verify-final.log (2.554) também passaram; o aceite definitivo incorpora a correção posterior da falha de parsing detectada pelo teste de sigilo, sem removê-lo ou enfraquecê-lo.

| JaCoCo global | Baseline etapa 4 | Aceite etapa 5 |
| --- | --- | --- |
| Linhas | 3.995/4.151 — 96,24% | 4.175/4.347 — **96,04%** |
| Ramos | 2.083/2.453 — 84,92% | 2.174/2.577 — **84,36%** |

Portões globais >=80% de linhas e ramos aprovados, sem diminuir limites/exclusões; 278 classes analisadas. A variação das razões globais acompanha a inclusão do código de redefinição. Comparação por classe confirmou **nenhuma perda de linhas/ramos cobertos nas 251 classes preexistentes de produção cujo código permaneceu inalterado**, incluindo o domínio financeiro; detalhes em cobertura-preservada.json. HTML/XML em target/site/jacoco; XML anterior preservado em target/auth-etapa5/jacoco-anterior.xml.

OpenSpec `validate adicionar-autenticacao-e-gestao-de-usuarios --strict`: válido. `git diff --check`: exit 0; somente avisos Git de normalização LF/CRLF em arquivos preexistentes. Verificação adicional dos arquivos novos/alterados sem espaços finais. Graphify atualizado após as alterações finais de código/testes (graphify-aceite.log): 14.057 nós, 30.008 arestas, 952 comunidades. Permanecem os avisos conhecidos de JSONs sem nós e ausência de tree_sitter_sql; nenhuma instalação, extração LLM ou alegação de homologação SQL.

Snapshot SHA-256 anterior à primeira edição: **1.156 arquivos; 1.138 preservados byte a byte, 18 alterados no escopo e 13 criados, nenhum removido**. Inventário completo em conferencia-escopo.json. README, teste Angular, ZIP, todas as migrations, dependências e produção financeira preservados. Busca de JWT/hash BCrypt/chave privada literais nos 14 arquivos de produção novos/alterados não encontrou segredo fixo; revisão confirmou geração apenas de runtime/teste. Logs novos do ciclo foram sanitizados para remover segredos efêmeros de fixtures eventualmente impressos pelo harness antes de desabilitar a impressão MockMvc, preservando diagnósticos/status/contagens; não são logs de produção nem artefatos para versionar.

Git status final inclui **32 arquivos rastreados modificados, 79 não rastreados e zero arquivos no stage**. As 32 entradas rastreadas já estavam modificadas no início; o estado total inclui ciclos anteriores e não é atribuição de todas as alterações à etapa 5. Conferência em status-final.txt/git-resumo.json. Não houve git add, commit, push, merge, arquivamento, deploy, migration em banco real ou uso de PostgreSQL. Implementação encerrada; aguardar revisão antes de qualquer etapa posterior.
