# Autenticação, acesso e ferramentas

Estado em 01/11/2026: backend, Angular e homologação PostgreSQL 10.4 aprovados tecnicamente. A configuração 9.4 foi implementada e homologada localmente em recursos descartáveis, conforme operacional-https.md da change, aguardando revisão. **O sistema não está declarado pronto para produção.** Este guia não autoriza deploy, bootstrap, migration ou uso de dados reais. Topologia, segredos externos, backup e recuperação: [OPERACIONAL-HTTPS.md](OPERACIONAL-HTTPS.md).

## Matriz de permissões e dados compartilhados

| Operação | Anônimo | USER | ADMIN |
| --- | --- | --- | --- |
| Ler Swagger/OpenAPI e obter CSRF | Sim, respeitando transporte seguro | Sim | Sim |
| Login e confirmação de senha com código | Público, com CSRF | Público, com CSRF | Público, com CSRF |
| Identidade, atividade e logout | Não | Própria sessão | Própria sessão |
| Consultar carteiras, ativos, corretoras, operações, indicadores e custódia | Não | Sim | Sim |
| Cadastrar/alterar/excluir carteira nas operações existentes | Não | Não | Sim, com CSRF e regras financeiras |
| Cadastrar ativo/corretora/operação, atualizar cotação individual/lote, criar snapshot, movimentação ou estorno | Não | Não | Sim, com CSRF e regras financeiras |
| Listar/consultar/cadastrar usuários, alterar perfil/situação e emitir código | Não | Não | Sim; mutações com CSRF |

Carteiras, operações, ativos e corretoras são compartilhados entre identidades autorizadas. Não há proprietário, filtro por usuário ou separação de patrimônio por login. BRL/USD e todas as regras de precisão/histórico continuam preservadas. Não há cadastro público, DELETE de usuário ou atualização de senha escolhida pelo administrador.

A allowlist/default deny HTTP é a primeira fronteira. @PreAuthorize protege adicionalmente os métodos de escrita dos Resources financeiros antes de efeitos e os serviços públicos administrativos/reset contra chamadas fora do controller; não substitui CSRF ou validação transacional. Administração revalida o ator autenticado depois de esperar pelos locks. Não recebe ator no corpo do comando. O Angular oculta ações/rotas, mas o backend é a autoridade e recusa requests forçados.

## Sessões, cookies e CSRF

- JWT somente em GESTAOACOES_AUTH: HttpOnly, SameSite=Lax, Path=/, sem Domain e Secure fora da exceção local explícita. Sem Bearer alternativo, refresh token, JWT no JSON ou em JavaScript/Web Storage.
- GET /auth/csrf retorna 204/no-store e disponibiliza XSRF-TOKEN, cookie separado legível, com Path=/, SameSite=Lax e Secure em produção. Enviar o valor em X-XSRF-TOKEN em todas as mutações, inclusive login, confirmação pública, atividade e logout.
- Login retorna 200 com identidade/perfil/prazos, nunca JWT/senha/hash; logout retorna 204. Ambos invalidam CSRF anterior: obter novamente antes de escrever. GET /auth/me retorna identidade válida sem registrar atividade.
- Dez minutos consecutivos sem atividade global válida encerram a validade; na igualdade já expirou. Sessão/JWT têm limite absoluto de quatro horas desde login. Atividade não renova JWT/cookie nem altera esse limite. Não há tolerância adicional ou heartbeat para manter sessão aberta.
- Somente eventos humanos confiáveis em aba visível comunicam atividade. /me, CSRF, polling, foco, timers e providers não renovam. Uma atividade em voo por aba e uma pendente; aba suspensa é mascarada e revalidada antes do acesso.
- Reload restaura sessão por cookie e /me antes de abrir o shell. Logout, expiração e troca de identidade limpam carteira, caches, formulários/diálogos e respostas antigas. Retorno só preserva rota/parâmetros permitidos e vínculo com a mesma identidade anterior, sem recuperar campos não salvos.

401 corrente exige nova autenticação. 403 ACESSO_NEGADO é papel insuficiente; 403 CSRF_INVALIDO exige token atual e nova submissão manual, sem retry de escrita. CSRF pode produzir 403 antes de 401 quando ambos faltam. 409 exige consultar estado/versão atual, incluindo ULTIMO_ADMIN_ATIVO, VERSAO_USUARIO_OBSOLETA ou CONFLITO_VERSAO conforme o comando. 429 inclui Retry-After; aguardar, sem repetição automática. 503 indica indisponibilidade/resultado incerto, não credencial incorreta; confirmar estado antes de repetir uma mutação.

Revogação é persistida por SID no logout e por versão de segurança em bloqueio, mudança efetiva de perfil e consumo de código. Novas decisões após commit negam sessões antigas em todos os navegadores, independentemente de BroadcastChannel. Habilitar não restaura sessões/códigos. A revogação não cancela automaticamente uma requisição financeira já autorizada antes do commit. Falha no banco nega acesso; limpeza física não é necessária para expirar/revogar.

## Bootstrap e onboarding

Bootstrap é comando one-shot local com profile bootstrap-admin, sem servidor HTTP. Exige datasource explicitamente destinado à inicialização e AUTH_BOOTSTRAP_USERNAME/AUTH_BOOTSTRAP_PASSWORD runtime, sem defaults. Serializa pelo singleton, cria somente o primeiro ADMIN com credencial e recusa banco já inicializado. Remover profile e segredos após execução; não há endpoint/usuário automático no modo normal. Execução em banco real requer autorização própria; este guia não a concede.

Após build e configuração segura do ambiente do processo, a forma de invocação é `java -jar target/gestaoacoes-0.0.1-SNAPSHOT.jar --spring.profiles.active=bootstrap-admin`. O profile impõe `spring.main.web-application-type=none` e fecha o contexto ao terminar. A inicialização ainda usa Liquibase: confirmar previamente o destino e a autorização para migrations; não executar esse exemplo sobre dados reais para validar a change. Fornecer a senha fora de argumentos/histórico e removê-la do ambiente depois, sem publicá-la junto do comando.

No Angular, ADMIN abre /usuarios/novo, informa username e perfil. POST /usuarios exige CSRF e retorna 201/Location e envelope usuario/autorizacao. Conta nasce sem senha e não é ADMIN operacional até definir credencial. ADMIN entrega o código por canal confiável e não escolhe/conhece a senha do titular. Código é exibido somente na resposta inicial; se perder a resposta, consultar estado e emitir novo código com versão atual, sem recuperar o anterior.

Usuário abre /definir-senha, informa username/código/nova senha/confirmacao. POST público /auth/redefinicoes/confirmar exige CSRF, retorna 204 e não inicia login nem emite JWT. Senha: mínimo oito caracteres, máximo 72 bytes UTF-8, sem trim/truncamento; confirmação deve coincidir exatamente. Depois é necessário login separado.

Para redefinição, ADMIN consulta versão e usa POST /usuarios/{id}/redefinicoes com versaoEsperada. Conta inicializada recebe REDEFINICAO; não inicializada recebe INICIAL. Emissão incrementa versão otimista, não versão de segurança; substitui autorização pendente atomicamente. Código Base32 sem padding, pelo menos 128 bits, aceita caixa/espaços/hífens; expira em quinze minutos, rejeitado na igualdade. Só HMAC-SHA-256 vinculado ao usuário/autorização é persistido. Cinco códigos incorretos invalidam autorização. Senha fora da política/divergente e CSRF rejeitado não consomem tentativas do código. Consumo válido é único/atômico, troca senha BCrypt, incrementa versão de segurança, invalida sessões anteriores e audita, sem login automático.

Limite de login: cinco falhas username+IP/cinquenta por IP em quinze minutos. Confirmação pública: dez requisições username+IP/cinquenta por IP em quinze minutos, inclusive payloads inválidos/identidades inexistentes conforme a camada. Falhas de identidade/código são genéricas; não consultar existência pública. IP é request.getRemoteAddr(): no perfil operacional homologado, Tomcat só aplica o IP encaminhado pelo Nginx exato, que sobrescreve os headers do cliente. Dois IPs reais distintos e tentativa de falsificação foram verificados em 9.4.

Último ADMIN operacional (ativo e com senha definida) não pode ser eliminado por bloqueio/rebaixamento, inclusive autoalteração e concorrência. PATCHs exigem versaoEsperada antes de reconhecer no-op e retornam 200/UsuarioResponse. No-op não incrementa versões, revoga sessão ou audita mudança efetiva. Recusas de negócio auditáveis são preservadas sem inventar sucesso de transação revertida.

## Variáveis runtime e pendências de ambiente

| Variável/propriedade | Requisito |
| --- | --- |
| AUTH_JWT_SECRET / auth.jwt.secret | Base64 de pelo menos 32 bytes aleatórios; somente runtime, sem valor padrão publicado |
| AUTH_RESET_SECRET / auth.reset.secret | Chave Base64 própria de pelo menos 32 bytes, diferente da JWT; obrigatória no contexto web |
| AUTH_ABSOLUTE_DURATION / auth.absolute-duration | Padrão PT4H, positiva e no máximo quatro horas; mudança não prolonga sessão emitida |
| auth.allow-local-http | Exceção false por padrão; true somente dev/test explícitos e conexão loopback; não funciona como liberação de HTTP entre containers |
| SPRING_PROFILES_ACTIVE | Profile deliberadamente escolhido; não usar dev/debug como evidência de produção |
| SPRING_DATASOURCE_URL/USERNAME/PASSWORD | Destino explicitamente aprovado; nunca apontar teste para banco persistente de desenvolvimento/produção |
| AUTH_BOOTSTRAP_USERNAME/PASSWORD | Somente comando one-shot autorizado; remover após inicialização |

Gerar valores com CSPRNG em ambiente seguro; não colar em histórico de shell, logs, README, coleção, screenshots ou Git. Manter JWT/HMAC estáveis entre reinícios/instâncias que compartilhem estado. Rotação JWT invalida tokens antigos; rotação HMAC torna códigos pendentes inutilizáveis e exige reemissão. Não há keyring/recuperação de código.

compose.yaml e frontend/nginx.conf continuam exclusivamente de desenvolvimento HTTP. A configuração autônoma compose.operacional.yaml, ops/nginx.conf e profile operacional foram homologados com HTTPS em gestaoacoes.localhost:8443, proxy exato /32, chaves via configtree e portas backend/DB não publicadas. Consultar OPERACIONAL-HTTPS.md; não combinar Composes, reutilizar dados/segredos de desenvolvimento ou remover Secure/CSRF. A homologação local não constitui publicação de produção.

Migration 009 integra o master e foi validada em H2 e PostgreSQL 17.11 exclusivamente descartáveis. 10.4 comprovou instalação/upgrade, integridade, concorrência de autenticação, atomicidade e reinícios, sem alterar a migration. O rollback destrutivo foi recusado por StopChange com preservação dos registros de autenticação/auditoria e histórico Liquibase. Isso não autoriza aplicar migrations em outro banco. Recuperação e compatibilidade de versão exigem plano autorizado; não fazer DROP/rollback automático nem usar Compose/banco persistente para homologar.

Para repetir a suíte específica, somente com autorização de provisionamento descartável: `.\mvnw.cmd -B "-Dtest=AutenticacaoPostgres*Test" "-Dauth.pg.real=EXECUTAR_009" "-Djunit.jupiter.execution.parallel.enabled=false" "-DforkCount=1" test`. O suporte fixa contexto desktop-linux, Docker local Windows e imagem 17.11 linux/amd64 por digest; recusa configuração externa de datasource e selecionamento sem opt-in. Não é um runner portátil de CI nem usa Testcontainers. A suíte padrão não habilita PostgreSQL real. Manifestos, logs/eventos sem segredos e inspeção de descarte ficam em target/auth-postgresql; consultar o relatório da change para resultados e limites.

## Postman/Newman

Coleção: docs/api/gestao-acoes.postman_collection.json. Não executar tudo como sequência: exemplos financeiros existentes são independentes e alguns consultam providers reais. Para o aceite isolado, selecionar somente **Autenticação e usuários — ambiente isolado**.

Usar banco descartável, jar novo e baseUrl da origem aprovada; HTTP só em localhost/127.0.0.1/::1 com exceção local do backend. O script exige ambienteDescartavel=CONFIRMO, recusa destino fora de baseUrl e desabilita redirects. No aplicativo Postman, permitir acesso do cookie jar somente ao domínio de teste utilizado. Não escrever header Cookie/Authorization manualmente nem copiar JWT para campos de autorização.

Fornecer usernameAdmin/senhaAdmin de fixture isolada e senhaInicial/senhaNova somente em variáveis locais/runtime. usernameNovo opcional; vazio gera nome de fixture. A exportação versionada mantém valores vazios. Não salvar/exportar ambiente preenchido, respostas, relatórios detalhados ou cookie jar. Código de emissão usa somente pm.variables temporário do Collection Runner, é consumido na próxima confirmação e removido; nunca collectionVariables/environment/globals. Não há captura automática de código para persistência ou reuso manual. Para Send manual, entregar/copiar o código explicitamente sem salvar resposta/valor.

Fluxo da pasta: 401 anônimo → CSRF → login ADMIN → CSRF atual/identidade → cadastro e definição inicial → consultas/perfil/bloqueio/habilitação → emissão e consumo de redefinição → 403 sem CSRF → logout/novo CSRF/401 → login USER → consulta permitida e 403 administrativo/financeiro → logout/401. O fluxo termina sem autenticação: exemplos financeiros posteriores precisam de novo login ADMIN. Antes de cada mutação permitida, o script obtém CSRF atual; falha interrompe o comando, sem retry. Nenhuma chamada de atividade automática é usada para manter sessão aberta.

Testes estáticos: node --test docs/api/validar-colecao.test.cjs (quatro aprovados). Em 31/10/2026 a coleção exportada foi executada com Newman 6.2.3 em H2 descartável: 28 passos, 41 requests incluindo handshakes internos, 44 asserções e nenhuma falha. A recusa sem confirmação de ambiente produziu zero requests. Registrar somente contagens/status/nomes de asserções, sem exportar valores. Newman é ferramenta de teste, não dependência da aplicação. Evidências detalhadas em openspec/changes/archive/2026-11-02-adicionar-autenticacao-e-gestao-de-usuarios/fechamento-documentacao.md.

## Swagger same-origin

Documentação pública não autoriza execução financeira anônima. Abrir /swagger-ui/index.html na mesma origem da API. Cookie HttpOnly é gerido pelo navegador; Swagger não pode escrever manualmente Cookie. Não tentar preencher JWT no Authorize nem usar Bearer como alternativa.

Na versão instalada, springdoc gera no swagger-initializer.js um interceptor CSRF same-origin que lê XSRF-TOKEN e envia X-XSRF-TOKEN, sem Web Storage nem persistência de autorização. Testes verificam esse JavaScript realmente servido. No navegador local foram comprovados GET /auth/csrf com 204 sem parâmetros fictícios e GET /carteiras anônimo com 401/AUTENTICACAO_NECESSARIA.

**Limitação verificada:** o formulário Try it out valida o header X-XSRF-TOKEN obrigatório antes de chamar o interceptor. No login com corpo válido, campo vazio provoca “Required field is not provided” no cliente. Não há aceite completo de mutações pelo Swagger. Manter o header obrigatório e os filtros; utilizar Postman para os fluxos integrados, sem copiar JWT, relaxar CSRF ou introduzir bypass. Não presumir que login no jar do Postman autentique o navegador. HTTPS/produção continua fora desse aceite local.
