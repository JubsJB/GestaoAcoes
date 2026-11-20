# HTTPS operacional — homologação local isolada

Origem única: `https://gestaoacoes.localhost:8443`. Este procedimento não declara prontidão de produção nem autoriza publicação, banco existente ou dados reais. O Compose de desenvolvimento continua independente e não serve como configuração operacional aceita.

## Topologia e transporte

`compose.operacional.yaml` é autônomo: nunca usar `-f compose.yaml -f compose.operacional.yaml`. Somente Nginx publica `127.0.0.1:8443`. Backend e PostgreSQL não publicam portas. Redes separadas: ingresso (Nginx/clientes de homologação), API interna (Nginx/backend) e banco interna (backend/PostgreSQL/bootstrap). Não conectar serviços alheios a essas redes.

Nginx exige Host público com porta, termina TLS 1.2/1.3, mantém Origin original e sobrescreve X-Forwarded-For/Proto/Host/Port. Remove Forwarded, X-Real-IP e X-Forwarded-Prefix. Não usa proxy_add_x_forwarded_for nem real_ip_header recebido do cliente. Tomcat usa estratégia nativa e aceita encaminhamento somente do IPv4 exato do Nginx em /32. O perfil operacional recusa HTTP local, perfis dev/test misturados, proxy amplo, headers alternativos, origem diferente e redirecionamentos de datasource. A validação ocorre antes da criação dos beans de infraestrutura.

A proteção de transporte continua baseada em request.isSecure(). Uma conexão direta com headers forjados não se torna HTTPS. CSRF e CORS de mesma origem continuam ativos, sem confiança global em ForwardedHeaderFilter. Cookies mantêm Path=/, SameSite=Lax, Secure e escopo host-only; o JWT mantém HttpOnly. A porta pública participa da comparação de Origin. O proxy adapta somente Location ao prefixo público /api, sem alterar contratos financeiros. OpenAPI anuncia /api; Swagger permanece com a limitação Try it out já documentada, usando Postman como alternativa sem bypass.

## Runtime e materiais privados

Dependências locais: Docker Desktop no contexto desktop-linux e endpoint local esperado pelo executor, Compose v2, Node.js compatível com o projeto, Java 17/Maven wrapper, npm e OpenSSL do Git for Windows. O executor é deliberadamente específico deste ambiente Windows; não constitui provisionador portátil ou mecanismo de produção.

O executor `ops/homologacao.cjs` exige opt-in AUTH_HTTPS_REAL=EXECUTAR_094. Recusa configurações externas Spring/Java/Docker/Compose/OP capazes de alterar a execução. Não lê .env. Usa --env-file externo explícito, imagens fixadas por digest e manifesto sanitizado. O arquivo .env.example só documenta nomes/valores não secretos; não contém chaves utilizáveis.

Antes de executar, preparar artefatos atuais:

```powershell
.\mvnw.cmd -B '-Dtest=OperationalRuntimeConfigTest' package
# no diretório frontend, com NG_BUILD_MAX_WORKERS=2:
npm run build
# de volta à raiz:
node --test ops/homologacao.test.cjs
$env:AUTH_HTTPS_REAL='EXECUTAR_094'
node ops/homologacao.cjs
Remove-Item Env:AUTH_HTTPS_REAL
```

A obtenção das três imagens oficiais pelos digests constantes no executor deve ser explícita e ocorrer antes do opt-in. Imagem ausente, contexto divergente, plataforma diferente ou DNS sem loopback interrompem antes das migrations. O executor não substitui imagem ou contexto silenciosamente.

Os materiais reais ficam em diretório novo do TEMP, fora do repositório, com ACL apenas para usuário executor e SYSTEM. Não exportar/importar a CA no Windows. O certificado tem SAN gestaoacoes.localhost; clientes usam CA explícita. Não usar -k, insecure ou rejectUnauthorized=false para aceite. A CA e o certificado de homologação expiram em dois dias; não são adequados para exposição pública.

Arquivos externos necessários:

| Arquivo | Serviço que recebe |
|---|---|
| provision-password | PostgreSQL, provisionamento exclusivo |
| app-password | PostgreSQL, somente para criar o papel limitado |
| spring.datasource.password (montagem do app-password) | Backend e bootstrap |
| auth.jwt.secret | Somente backend normal |
| auth.reset.secret | Somente backend normal |
| AUTH_BOOTSTRAP_PASSWORD | Somente comando bootstrap one-shot |
| server.crt, server.key | Somente Nginx |
| ca.key | Nenhum container |

JWT e reset são Base64 de chaves CSPRNG independentes de 48 bytes, sem defaults. Configtree obrigatório /run/secrets/ fornece as propriedades; AUTH_JWT_SECRET/AUTH_RESET_SECRET são equivalentes de configuração existentes, mas o Compose operacional não usa variáveis com valores secretos. Os diretórios internos de segredos são privados (0700), bind mounts são somente leitura e o backend roda UID 10001. Assim, não se depende de mode de Compose secrets, que pode ser ignorado em bind Windows. O executor verifica leitura pelo processo e recusa por UID alheio.

APP_TRUSTED_PROXY_IP é único e deve corresponder ao endereço estático do Nginx na rede API, sem regex genérica. APP_PUBLIC_ORIGIN corresponde à origem homologada. OP_DB_NAME/OP_DB_APP_USER são identificadores fechados; datasource é exclusivamente postgres:5432 no banco explicitado. Provedores externos ficam desabilitados por destinos locais indisponíveis nesta homologação, sem novas funcionalidades financeiras.

## Banco e bootstrap

Um volume novo recebe banco, provisionador e papel de aplicação separados. O papel de aplicação não tem SUPERUSER, CREATEDB ou CREATEROLE; possui o schema necessário a Liquibase/JPA. Não significa separação completa entre papel de migration e papel DML, evolução não incluída neste recorte acadêmico.

Antes de iniciar Liquibase/Hibernate, verificar contexto, digests, labels/IDs exclusivos, redes e membros, portas, banco, papel e marcador ops_guard.execution. O marcador é criado pelo provisionamento e não integra o backup public. Não compartilhar o banco descartável nem conectar recursos existentes. Migrations 001–009 não são modificadas.

Bootstrap usa profiles operacional,bootstrap-admin, web-application-type=none e credencial exclusiva montada somente no comando. Após término, remover o container one-shot. Backend normal nunca recebe credencial/profile de bootstrap. O comando existente recusa reinicialização de banco já inicializado; não existe cadastro público de administrador nem senha padrão.

## Reinício, chaves e sessões

Em reinício normal, preservar volume e arquivos JWT/HMAC. Nunca gerar chaves novas automaticamente no start. Sessões continuam subordinadas ao estado persistido: dez minutos exatos desde última atividade válida e quatro horas absolutas, sem refresh, renovação por GET/polling ou tolerância adicional. Bloqueio, perfil e reset invalidam as sessões anteriores. Uma requisição já autorizada pode terminar; revogação impede autorizações seguintes.

Troca emergencial da chave JWT invalida os cookies antigos; troca da chave HMAC invalida códigos pendentes. Fazer sob manutenção, com registro operacional e invalidação persistida correspondente, exigindo novo login/reemissão administrativa. Não manter fallback de chaves antigas nesta versão. Não prometer rotação transparente ou prazo de rotação de produção não aprovado.

Recriar o container backend pode alterar seu IP interno: reiniciar/recarregar Nginx para resolver novamente o upstream, mantendo seu próprio IP confiável. Indisponibilidade durante manutenção deve negar acesso; não liberar APIs nem desabilitar transporte/CSRF.

## Backup e recuperação de homologação

Antes de mudanças planejadas: parar escritas, confirmar identidade/propriedade e schema compatível, gerar pg_dump -Fc do schema public e criptografar AES-256-GCM antes de gravar. O executor mantém dump aberto somente em memória; disco recebe backup.enc, sem texto puro. Chave de backup separada do arquivo, diretório externo privado. Para produção futura, custódia independente da chave, retenção, RPO/RTO e teste periódico precisam de planejamento próprio; não há job automático aqui.

Restaurar exclusivamente em outro banco novo descartável, nunca sobrepor o original. Confirmar marcador e ausência de tabelas public antes de preparar o schema vazio; remover apenas esse schema vazio sem CASCADE para acomodar CREATE SCHEMA do dump. pg_restore é transacional, sem --clean, com papel da aplicação e recusa imediata de erros. Comparar integralmente os registros de todas as tabelas public, inclusive financeiro, auditoria e databasechangelog, antes de qualquer acesso da aplicação restaurada.

Sob manutenção, antes de reabrir: transação na ordem singleton → usuários ordenados → autorizações → sessões; incrementar versões de segurança/otimistas, invalidar autorizações pendentes e revogar sessões restauradas. Preservar auditoria histórica. Rotacionar JWT/HMAC e iniciar aplicação compatível, com ddl-auto=validate/Liquibase e testes de novo login. Tokens/códigos anteriores devem ser negados. Reconciliar bloqueios, permissões e credenciais alterados após o backup antes de reabrir dados reais: restaurar dados antigos não é, sozinho, recuperar o estado de segurança atual.

Em falha: manter sistema fechado, preservar original e backup criptografado, registrar erro sanitizado e corrigir em novo destino. Não restaurar parcialmente, executar DROP CASCADE ou rollback destrutivo de 008/009. Recuperar binário anterior somente se comprovadamente compatível com o schema atual; caso contrário corrigir adiante ou restaurar conjunto aplicação/schema em destino novo sob autorização. Não alterar checksums/history do Liquibase para forçar compatibilidade.

## Evidências e descarte

Manifestos/checks sanitizados ficam em target/auth-https/UUID, sem senhas, hashes de senha, códigos, JWT ou dumps abertos. Recursos recebem label gestaoacoes.operacional.run=UUID; antes de remover cada ID, conferir propriedade. Remover containers, redes, volume e imagens exclusivas; não usar prune, compose down -v, prefixos ou volumes existentes. Propriedade incerta impede descarte automático e exige investigação. Materiais privados externos, inclusive backup criptografado e CA, são preservados para revisão; não adicionar ao Git ou contexts de build.

O relatório da change registra somente cenários realmente executados, falhas intermediárias e limites. Testes do protocolo usam clientes HTTPS com confiança isolada, sem modificar navegador/trust store global. Testes Angular e de limites temporais continuam cobertos pelas respectivas suítes; não confundir essas evidências com um ensaio manual de quatro horas ou navegação pública em produção.
