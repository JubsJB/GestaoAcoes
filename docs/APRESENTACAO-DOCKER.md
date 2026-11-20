# Apresentação acadêmica — Docker persistente

Entrega: **24/11/2026**. Roteiro preparado em 16/11; **ainda não executado nem homologado como ambiente de apresentação**. Complementa `OPERACIONAL-HTTPS.md` e reutiliza `compose.operacional.yaml`, os três Dockerfiles de `ops/`, o proxy e o perfil operacional existentes. Somente o modo com integrações acrescenta `compose.apresentacao-integracoes.yaml`. Não combinar com `compose.yaml`.

## Versão aprovada e evidências de 20/11/2026

Para 24/11, use a instância já preparada em `http://localhost:8081`, no modo `sem-integracoes` com transporte `http-local`. As seções de provisionamento e transição servem para intervenções futuras autorizadas.

| Item | Revisão ou evidência |
| --- | --- |
| Frontend | `6ca837ef2b78c6d3052e3d3e6fc42525248acfa9` |
| Ambiente operacional | `2feed96afa24a04d04c531fec9322b69d4145026` |
| Imagem Angular aprovada e implantada | `sha256:df1fad36833c9e25df0c629e3ecea4a3726c7a754d71b70485fbd201dff123cd` |
| Integridade do artefato | Os hashes dos 68 arquivos compilados conferiram entre o build aprovado, o inventário de empacotamento e os arquivos implantados. Falta um inventário contemporâneo das fontes para comprovar a correspondência integral entre as fontes atuais e o build aprovado. |

A implantação substituiu somente o frontend e a imagem anterior ficou retida para rollback. A [auditoria financeira e operacional](../openspec/changes/preparar-dados-ficticios-offline/tasks.md#auditoria-financeira-e-operacional-de-20112026) e o [ensaio autenticado de sete etapas](../openspec/changes/apresentar-em-http-loopback/tasks.md#ensaio-autenticado-de-20112026) foram concluídos. O retorno a HTTPS e a carga específica do catálogo fictício no ambiente limpo permanecem pendentes.

## Escopo e autorizações

Nesta etapa foram preparados script, testes isolados e documentação. Nenhum container, imagem, rede, volume, certificado, segredo ou conta de demonstração foi criado. Os comandos abaixo são **para execução futura, após autorização**. Não executar `ops/homologacao.cjs` para disponibilizar a apresentação: esse executor realiza ensaios de segurança/recuperação e descarta seus recursos.

O script `ops/apresentacao.cjs` exige Windows, Docker Desktop Linux/amd64 e contexto local `desktop-linux`. Usa um único arquivo Compose, um env-file externo explícito e não imprime saídas brutas de subprocessos. Não oferece comando de exclusão. Uma falha preserva o que já foi criado; não há rollback, limpeza ou adoção automática de recursos desconhecidos.

**Pendência externa:** a revogação/rotação do acesso PostgreSQL exposto historicamente permanece sem comprovação. Não ler, tentar ou reutilizar aquela credencial. Gerar um novo ambiente não comprova revogação no destino anterior. Registrar confirmação sanitizada do responsável antes de reutilizar o acesso afetado, sem senha, token ou hash do segredo.

## 1. Pré-requisitos

- Docker Desktop com containers Linux/amd64, contexto `desktop-linux` e Compose v2; no Windows homologado, WSL2.
- No modo conectado, Compose deve reconhecer `gw_priority`, usado para priorizar a rota pela rede de saída. O script valida a configuração com o Compose instalado antes de criar recursos/iniciar serviços; suporte efetivo ainda não verificado nesta rodada. Não retirar o campo para contornar uma versão incompatível.
- Java 17, Maven wrapper e Node compatível com `frontend/package.json` (`^22.22.3`, `^24.15.0` ou `^26.0.0`), npm 11.17.0 para preparar os artefatos.
- OpenSSL do Git for Windows em `C:/Program Files/Git/usr/bin/openssl.exe`.
- Porta local 8443 livre; `gestaoacoes.localhost` resolvendo para loopback. Não substituir a origem por localhost, IP ou outra porta: Host, SAN, Origin e encaminhamento têm validação estrita.
- Diretório privado NOVO, permanente para os ensaios/apresentação, fora do repositório e de pastas sincronizadas/compartilhadas. Não usar TEMP para os materiais persistentes.
- Uma subrede `172.30.N.0/24` livre, inclusive em relação à rede local/VPN. O script detecta sobreposição com redes Docker; isso não comprova ausência de conflito com VPN/host.
- Confirmar contexto/capacidade de disco e memória antes de provisionar. Não confiar que imagens antigas representem o frontend atual.

Em uma sessão PowerShell dedicada, remover somente variáveis de runtime conflitantes previamente configuradas, após conferir sua finalidade. O script recusa `OP_*`, `APP_*`, `SPRING_*`, `AUTH_*`, opções Java e `DOCKER_*`/`COMPOSE_*` herdadas; não remover configurações globais automaticamente. Nunca imprimir o ambiente inteiro.

## 2. Preparação única de materiais privados e certificado

Na raiz do projeto, depois de escolher caminho e subrede:

```powershell
$demo = Read-Host 'Informe o caminho absoluto de um novo diretório privado fora do repositório'
# Exemplo de subrede: confirmar disponibilidade antes de executar.
node ops/apresentacao.cjs prepare "$demo" 172.30.180.0/24
if ($LASTEXITCODE -ne 0) { throw 'Preparação interrompida; revisar sem sobrescrever materiais.' }
```

Essa ação não chama Docker. Cria diretório privado com ACL para executor/SYSTEM, UUID/projeto `gademo…`, `runtime.env`, `manifesto.json`, senhas aleatórias novas, chaves JWT/reset independentes de 48 bytes e CA/certificado de 30 dias. Recusa diretório existente; não regenera segredos na retomada. Resolve junctions e recusa material dentro do repositório. Não copiar arquivos privados para Git, contextos da aplicação ou relatórios.

Arquivos secretos: `provision-password`, `app-password`, `AUTH_BOOTSTRAP_PASSWORD`, `auth.jwt.secret`, `auth.reset.secret`, `ca.key`, `server.key`. O backend recebe somente senha da aplicação e chaves JWT/reset; o bootstrap recebe somente senha da aplicação e a credencial inicial. A chave privada da CA nunca é montada em container.

`runtime.env` contém identificadores, caminhos e nomes de imagens, sem valores de senha/chave; mesmo assim permanece privado. `manifesto.json` registra identidade dos recursos e hashes dos arquivos operacionais, sem hashes de senhas. Não editar esses arquivos para contornar uma recusa. Preservar volume, manifesto, env-file e segredos como um conjunto: perder/trocar a senha em arquivo não muda o papel PostgreSQL já criado; perder/trocar JWT/reset altera validade de sessões/códigos.

O manifesto atual é versão 2 e inclui `OP_EGRESS_NETWORK` e o hash da extensão. Manifestos versão 1 são recusados sem alteração. Nenhum ambiente anterior foi executado nesta preparação; se houver material v1 criado fora desta rodada, interromper e planejar migração autorizada, sem editar recibos, recriar volume ou executar `prepare` sobre ele.

### Confiança TLS — passo manual pendente de autorização

O certificado cobre `gestaoacoes.localhost`. Se gerado em 16/11 com 30 dias, cobre os ensaios e 24/11; confirmar datas efetivas no momento da geração:

```powershell
& 'C:/Program Files/Git/usr/bin/openssl.exe' x509 -in "$demo/server.crt" -noout -dates -ext subjectAltName
& 'C:/Program Files/Git/usr/bin/openssl.exe' verify -CAfile "$demo/ca.crt" -verify_hostname gestaoacoes.localhost "$demo/server.crt"
```

**O script não instala confiança.** Para Edge/Chrome que utilizem a confiança local do Windows, a alternativa proposta, sujeita à sua autorização, é importar **somente `ca.crt` público** em `certmgr.msc` → Usuário Atual → Autoridades de Certificação Raiz Confiáveis → Certificados. Conferir identificação/impressão digital do certificado local antes de importar. Não importar chave privada, não usar o repositório da Máquina Local e não executar importação automática. Essa confiança alcança os aplicativos que consultam esse armazenamento do usuário: é uma decisão explícita, não uma preferência isolada de um perfil Chrome.

Se essa extensão de confiança não for aprovada, definir um cliente com armazenamento de confiança isolado antes do ensaio, sem presumir que criar um perfil Chrome isolado basta. Firefox, políticas corporativas e versões do navegador podem exigir procedimento próprio; a confiança efetiva ainda precisa ser conferida.

Verificação de aceite: abrir a origem HTTPS sem alerta e sem clicar em “continuar mesmo assim”. Nunca usar `-k`, `--ignore-certificate-errors`, `rejectUnauthorized=false`, remoção de Secure ou desativação de CSRF. A consulta de prontidão do script usa a CA explícita e valida hostname; ela não comprova confiança do navegador. A partida recusa certificado com menos de 24 horas restantes. Renovação posterior é manutenção própria, não reexecução de `prepare` sobre o ambiente existente.

## 3. Preparação das imagens operacionais

**Não executar nesta rodada.** Compilar a versão atual; não selecionar arquivos antigos em target/dist por conveniência. Os testes da aplicação são evidência separada: `-DskipTests` não é validação funcional.

```powershell
.\mvnw.cmd -B '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw 'Falha no artefato Java.' }
Push-Location frontend
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw 'Falha nas dependências frontend.' }
    npm run build -- --configuration production
    if ($LASTEXITCODE -ne 0) { throw 'Falha no build Angular.' }
} finally { Pop-Location }
```

Obter explicitamente as mesmas bases por digest da homologação, sem substituir tags ou arquitetura silenciosamente:

```powershell
docker --context desktop-linux pull --platform linux/amd64 postgres@sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb
docker --context desktop-linux pull --platform linux/amd64 nginx@sha256:0e52bd054126cdc040b1276c0af347aeb02a41b9cfe305d6094767ac30e97949
docker --context desktop-linux pull --platform linux/amd64 eclipse-temurin@sha256:c35794d1348777e6bc310b9b7f420098ef8f37e988199549b545fded79a895a2
# Só prosseguir se todos os pulls concluírem sem erro.
node ops/apresentacao.cjs build "$demo"
```

O comando copia apenas app.jar/browser/configuração pública e scripts necessários para contextos pequenos dentro do diretório privado; não envia o diretório privado inteiro ao Docker. Reutiliza os Dockerfiles existentes, identifica imagens por projeto/UUID e registra seus IDs. Não inicia containers. Recusa substituir um conjunto já preparado ou contexto parcial existente. Se falhar parcialmente, preservar materiais e diagnosticar antes de nova tentativa; não apagar/recriar recursos automaticamente.

## 4. Redes e volume exclusivos

```powershell
node ops/apresentacao.cjs resources "$demo" --modo sem-integracoes
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes
```

`resources` cria somente nomes exclusivos previstos no manifesto: ingresso comum, API interna com IP fixo do Nginx, banco interno e volume externo. Não conecta o backend ao ingresso; PostgreSQL participa somente da rede banco. Backend/banco não publicam portas. Somente frontend publica `127.0.0.1:8443`.

Com `resources --modo com-integracoes`, há uma quinta rede exclusiva, `OP_EGRESS_NETWORK`, não interna e utilizável apenas pelo backend. Rede criada não significa backend conectado: a associação é aplicada no `start` do modo escolhido. No modo sem integrações, a rede de saída não é criada em ambiente novo; se já existir por uso anterior, fica preservada e sem backend após a troca concluída. Nunca confundir existência da rede com conectividade do backend.

Repetir `resources` apenas verifica os recursos registrados. Recurso existente sem recibo, labels divergentes, membro estranho, rede/volume registrado que desapareceu ou isolamento divergente interrompem a ação. Não adotar volumes de desenvolvimento/homologação nem renomear recursos existentes. Volume externo permanece independente da parada dos containers; não executar comandos de remoção ou prune.

## 5. Banco e ADMIN — primeira inicialização, autorização de escrita própria

```powershell
node ops/apresentacao.cjs start-db "$demo"
# SOMENTE após autorização expressa para inicializar o ADMIN e migrations:
node ops/apresentacao.cjs bootstrap "$demo" --primeiro-admin
```

`start-db` inicia apenas PostgreSQL e espera seu healthcheck. Em volume novo, PostgreSQL cria banco/papel da aplicação conforme o script já homologado. O roteiro confere o marcador `ops_guard.execution` antes de acesso da aplicação. **Iniciar o banco já escreve no volume**, embora não crie usuários da aplicação.

Bootstrap executa o serviço existente, one-shot não web, em `operacional,bootstrap-admin`; também executa Liquibase. Exige serviços web parados, registra sucesso e recusa repetição pelo roteiro. A aplicação também recusa banco já inicializado. Não executar bootstrap em retomadas, não recriar ADMIN por SQL e não editar o recibo para forçar nova tentativa. Se o processo falhar depois de gravar no banco e antes de salvar o recibo, investigar o estado: não presumir rollback nem repetir automaticamente.

Usuário inicial: `admin_demo`. A senha fica no arquivo privado `AUTH_BOOTSTRAP_PASSWORD`; o script nunca a exibe. O responsável pode consultá-la localmente em editor sem compartilhamento/gravação e utilizá-la no navegador; não usar Get-Content/echo, parâmetros, relatórios ou capturas de credenciais. O container one-shot é removido pelo `--rm`; nenhum volume é removido. O arquivo privado permanece para custódia do operador e não é montado no backend normal.

## 6. Partida web e acesso

```powershell
node ops/apresentacao.cjs start "$demo" --modo sem-integracoes --autorizar-migrations
node ops/apresentacao.cjs status "$demo"
```

Requer bootstrap confirmado e modo explícito. Verifica arquivos/imagens/recursos, inicia banco, espera saúde/identidade, inicia backend e depois frontend; aguarda `/api/auth/csrf` com CA explícita e sem registrar cookies. Na primeira partida web ou troca de modo, recria somente backend e reinicia frontend para atualizar DNS do upstream. Autorizar migrations é necessário também na retomada: Spring usa Liquibase e Hibernate validate; não há rollback automático de schema. A prontidão HTTP não equivale a login aprovado.

Abrir `https://gestaoacoes.localhost:8443`. Conferir TLS confiável, login ADMIN e USER, seleção de carteira e consulta do Dashboard. O banco novo começa sem dados financeiros e sem USER: preparar os dados da seção 9 somente após autorização própria. Não prometer dashboard preenchido antes dessa preparação.

## 7. Parada e retomada — repetíveis, preservam dados

```powershell
node ops/apresentacao.cjs stop "$demo"
# Em outra sessão, redefinir $demo para o mesmo caminho privado e retomar:
node ops/apresentacao.cjs start "$demo" --modo sem-integracoes --autorizar-migrations
node ops/apresentacao.cjs status "$demo"
```

Parada em ordem frontend/backend/banco, sem exclusão. Retomada usa os mesmos IDs de imagem, volume e arquivos JWT/reset. Não chamar prepare/build/bootstrap novamente. Não usar `docker compose down -v`, `volume rm`, `system prune` ou o executor descartável. Não substituir chave/arquivo privado como “correção” de login. Respeitar os limites normais de inatividade/expiração; realizar novo login quando necessário.

Se um container backend for recriado por manutenção autorizada, seu IP pode mudar; o Nginx precisa ser reiniciado/recarregado para resolver o upstream novamente. Uma parada/retomada dos mesmos containers não exige recriação. Manutenção de versão/imagens e renovação de certificados devem ser ensaiadas separadamente, não durante a apresentação.

## 8. Integrações híbridas — dois modos explícitos

**Alternativa B aprovada exclusivamente para demonstração acadêmica local.** A ausência de allowlist foi aceita expressamente pelo usuário. A rede de saída não restringe destinos, portas, hosts da rede local ou outros endereços alcançáveis; não é política aprovada para produção. Não foi criado proxy de saída. `ExternalApiConfig.java`, TLS Java, timeouts, autenticação, Nginx e Dockerfiles permanecem inalterados.

| Modo | Arquivos Compose | Redes do backend | Providers |
|---|---|---|---|
| `sem-integracoes` | Somente operacional original | API interna + banco interno | Quatro destinos desabilitados; nenhuma chave adicional montada |
| `com-integracoes` | Operacional + extensão de apresentação | API interna + banco interno + saída dedicada | HTTPS real; chaves externas montadas somente no backend |

PostgreSQL e bootstrap permanecem somente no banco interno; frontend permanece em ingresso/API. Somente HTTPS loopback é publicado. Não basta omitir chaves ou desabilitar destinos para obter isolamento: o modo offline exige backend **sem associação** à rede de saída.

### Parâmetros privados para o modo conectado

Depois de autorizar a preparação efetiva, fornecer por editor local, dentro de `$demo` e sob a ACL privada já preparada:

| Arquivo privado | Propriedade configtree montada no backend |
|---|---|
| `brapi-api-key` | `integration.brapi.api-key` |
| `alpha-vantage-api-key` | `integration.alpha-vantage.api-key` |

Usar chaves próprias válidas dos provedores, uma linha por arquivo; não gerar strings aleatórias como se fossem chaves de API. Não usar argv, echo, capturas ou relatórios. Não editar `runtime.env`/manifesto: o nome da rede já é preparado automaticamente. Os endpoints públicos são fixados na extensão: `https://brapi.dev`, `https://www.alphavantage.co`, `https://brasilapi.com.br`, `https://viacep.com.br`. BrasilAPI e ViaCEP não recebem chaves adicionais nesta integração. Não há segredo em Compose ou no Angular.

O modo conectado exige ambos os arquivos não vazios; não se declara que a chave é válida apenas por existir. O modo offline não lê nem monta esses arquivos. O bootstrap nunca os recebe e sempre utiliza a configuração original isolada.

### Habilitar ou alternar — somente após autorização de execução

```powershell
# Habilitar: preparar os dois arquivos privados antes destes comandos.
node ops/apresentacao.cjs resources "$demo" --modo com-integracoes
node ops/apresentacao.cjs start "$demo" --modo com-integracoes --autorizar-migrations
node ops/apresentacao.cjs check "$demo" --modo com-integracoes

# Voltar ao modo principal sem saída:
node ops/apresentacao.cjs start "$demo" --modo sem-integracoes --autorizar-migrations
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes
```

Interromper se qualquer comando retornar código diferente de zero. Não executar bootstrap, prepare ou build na troca. O script verifica o volume/banco existente, para frontend/backend e recria **somente backend**, removendo/adicionando a rede e montagens de API conforme o modo. Reinicia o mesmo frontend para renovar a resolução do backend, preservando imagem, certificado, volume e chaves de sessão. Não recria PostgreSQL nem volume; o `up` de PostgreSQL é idempotente com a mesma configuração/imagem. Mudança inesperada de identidade de recursos/imagens interrompe o roteiro.

Ao iniciar a transição, o manifesto registra `pendingMode`; só confirma `activeMode` depois das verificações e prontidão. Falha não apaga recursos nem declara o modo novo concluído. `status` mostra modo confirmado e transição pendente. Se a mudança falhar antes de parar o frontend, o modo antigo pode continuar disponível; se falhar depois, o frontend permanece parado até correção. Não presumir desconexão offline antes de `check --modo sem-integracoes` passar. O check compara também as redes reais do backend, inclusive quando parado; não altera a topologia para fazê-la passar.

Parar com `stop` e retomar com `start --modo <modo-escolhido> --autorizar-migrations`. `stop`, `status`, `start-db` e `bootstrap` usam o Compose base e não exigem arquivos de API; param/consultam os mesmos serviços/projeto, sem reconciliação ou exclusão de volume. `resources`, `check` e `start` exigem `--modo`; não há escolha silenciosa para essas ações.

### Verificação da conectividade e fallback

O `check` valida a topologia e as redes reais; **não faz consulta aos provedores**. Em ensaio posterior autorizado, pelo frontend autenticado: consultar um CNPJ e CEP apropriados, cadastrar/consultar um ticker BR e um EUA e atualizar cotações de forma controlada. Conferir resposta real/erros e quotas, sem capturar chaves. Não usar URLs com `apikey` em comandos ou compartilhar logs que a contenham.

Para diagnóstico, separar resolução DNS no backend, rota pela rede de saída, handshake TLS padrão Java e resposta de provider (quota, chave, indisponibilidade). Resolução/rota/certificados reais dependem do Docker Desktop e da rede do computador, ainda não testados. A falta de resposta não autoriza ampliar confiança de proxy, desativar validação TLS ou mudar timeouts para esconder falhas. Não publicar logs brutos de URLs/headers.

A preparação inicial de ativos/corretoras pelos fluxos normais requer o modo conectado e consultas autorizadas. Depois, persistir os dados e usar o modo sem integrações na apresentação principal; consultas históricas usam os registros existentes. Não prometer cadastro/atualização offline, não inventar cotações e não substituir falha por resposta fictícia. O plano de dados da seção 9 continua sem execução.

## 9. Plano mínimo de dados — não executado

Para o cenário integralmente fictício e offline de 24/11, use o [procedimento de dados demonstrativos](DADOS-DEMONSTRACAO-OFFLINE.md), sujeito a autorização antes da ativação e da carga. O plano abaixo descreve a preparação com provedores reais.

Somente no banco exclusivo de apresentação, pelos fluxos existentes, após liberar as etapas necessárias:

1. ADMIN one-shot; criar `julia` com papel USER pela administração e concluir definição de senha pelo código protegido existente, sem capturar código/senha. Demonstrar consulta e ausência/negação dos comandos administrativos.
2. Cadastrar duas corretoras válidas por CNPJ através da consulta real BrasilAPI/ViaCEP. Escolher dados públicos apropriados antes do ensaio; não inventar CNPJ de empresa inexistente. Identificações públicas podem ser reais; usuários e negociações são exclusivamente de demonstração.
3. Cadastrar pelo menos dois ativos BR e um EUA por ticker/mercado, pelas integrações existentes. Conferir as moedas e cotações recebidas; não substituir provider por fixture nem confundir preços de operação com cotação.
4. Criar `Carteira — Demonstração acadêmica`. Exemplos de negociações sintéticas para revisão: BR A compra 100 a R$20,00 na corretora A; BR B compra 20 a R$30,00; EUA C compra 2,5 a US$100,00. Usar datas/mercados válidos e preços manuais efetivamente registrados, não recomendações ou dados reais do investidor.
5. Registrar venda BR A de 10 a R$22,00, em data posterior e na corretora com saldo. Manter quantidades/custos calculados somente pelo backend; não editar resultados financeiros para compor a tela.
6. Transferir 20 unidades remanescentes de BR A da corretora A para B, em data válida; conferir custódia e invariância consolidada. Avaliar atribuição/estorno apenas se fizerem parte da apresentação, sem encadear escritas desnecessárias.
7. Registrar patrimônio pelo comando existente com cotações válidas. Para mais de um ponto real, preparar em momentos distintos, sem retrodatá-los/inventar registros ou interpolar datas. Manter BRL/USD separados, gráficos V2 atuais e valores autoritativos.
8. Conferir lista/histórico/Dashboard e guardar uma relação sanitizada de IDs, quantidades e datas esperadas. Depois da preparação, a apresentação principal consulta os dados persistidos; atualização ao vivo é complemento sujeito à rede/provider.

As quantidades/preços acima são planejamento ilustrativo, não scripts de carga e não garantia de valores finais do Dashboard. Não criar permissões, dados em banco, usuário, operação, snapshot ou custódia nesta rodada. Falha/limite de provider deve ser apresentado como falha real contextualizada; nunca como consulta fictícia bem-sucedida.

## 10. Diagnóstico e validação mínima futura

`check` verifica configuração/identidade/imagens, não autenticação funcional. `status` mostra somente estado/saúde, sem ambientes ou segredos. Em falha, não exibir inspect completo, compose config expandido, arquivos privados ou logs brutos em gravações/relatórios. Conferir logs localmente e sanitizar somente o trecho necessário antes de compartilhar. O script suprime stdout/stderr de subprocessos para evitar publicação acidental.

| Sintoma | Conferência mínima, sem contornar segurança |
|---|---|
| Comando interrompido | Etapa, pré-requisitos, permissões do diretório, contexto local e saídas locais sanitizadas; nenhuma limpeza automática |
| Banco unhealthy | Nome/volume exclusivo, armazenamento, inicialização completa, marcador; não trocar senha esperando alterar papel existente |
| Backend não inicia | Leitura de secrets pelo UID, Base64/chaves distintas, perfis, IP confiável, Liquibase; não usar ddl-auto=create ou desligar segurança |
| 421/403/400 | Origem exata com porta, Host/Origin, IP real do Nginx e forwarding; não ampliar CORS/proxy |
| 502 após manutenção | Backend disponível e resolução do upstream Nginx após recriação |
| TLS recusado | SAN, datas, cadeia CA e confiança do navegador; não bypass |
| Providers indisponíveis | Esperado no modo sem integrações; no conectado separar DNS/rota/TLS/chave/quotas, sem publicar segredo |
| Login expira | Respeitar inatividade e duração absoluta; novo login, sem alterar regras |

Ensaio futuro mínimo: (1) preparar recursos e banco; (2) verificar identidade/saúde; (3) bootstrap autorizado; (4) HTTPS confiável no navegador; (5) ADMIN/USER; (6) dados sintéticos autorizados e consultas Carteiras/Dashboard; (7) uma operação sintética identificável; (8) parada/retomada controlada; (9) conferir persistência dessa operação e dos demais dados, com mesmos arquivos de chaves/volume; (10) testar uma consulta real por integração aprovada, respeitando quotas. Não repetir toda a homologação TLS/backup/restore nem suítes completas só por antiguidade das provas anteriores.

## Evidência da preparação de 16/11/2026

Testes direcionados: `node --test ops/apresentacao.test.cjs ops/homologacao.test.cjs`: **22 aprovados** (20 do roteiro e 2 existentes); sintaxe Node aprovada. Os testes usam funções puras/IO simulado, não Docker ou OpenSSL. A primeira preparação teve RED/GREEN registrado; na extensão de modos, cinco testes adicionais falharam antes de implementar seleção/topologia/transição e passaram depois, com mais duas verificações de redes reais/plano de recursos.

Os YAMLs reais foram carregados com biblioteca local já disponível, sem instalar dependências; foi conferido que o delta só altera backend, adiciona rede e declara dois segredos externos. A composição estática dos campos alterados, normalizada, passou no mesmo validador utilizado pelo roteiro; PostgreSQL/bootstrap/frontend permaneceram iguais. Isso **não é saída de `docker compose config`**. A renderização pelo Compose instalado, suporte a `gw_priority`, rotas efetivas, DNS, TLS e providers continuam pendentes para execução autorizada. Aceite integrado/TLS do navegador, criação dos materiais, bootstrap e dados continuam pendentes. Sem mudanças nos budgets/cobertura, Compose original, aplicação, autenticação ou checkboxes.

## 11. Sequência do primeiro provisionamento — não executar sem nova autorização

1. Confirmar pré-requisitos, subrede, espaço e diretório privado novo; revisar pendência de revogação antiga e autorização de confiança TLS.
2. Executar `prepare` da seção 2; conferir certificado e concluir confiança manual autorizada.
3. Executar builds/pulls exatos da seção 3, interrompendo em cada falha; eles não foram executados nesta etapa.
4. Depois dos artefatos e bases preparados, na raiz, com `$demo` definido como na seção 2:

```powershell
node ops/apresentacao.cjs build "$demo"
node ops/apresentacao.cjs resources "$demo" --modo sem-integracoes
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes
node ops/apresentacao.cjs start-db "$demo"
# Requer autorizacao especifica para migrations e criacao do primeiro ADMIN:
node ops/apresentacao.cjs bootstrap "$demo" --primeiro-admin
node ops/apresentacao.cjs start "$demo" --modo sem-integracoes --autorizar-migrations
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes
node ops/apresentacao.cjs status "$demo"
```

Executar cada linha somente se a anterior terminar com código zero. O primeiro ambiente está vazio: testar login/TLS antes de preparar dados. Para os cadastros/consultas externas autorizados, preparar os dois arquivos de API da seção 8, executar `resources/start/check --modo com-integracoes` como documentado, e depois autorizar a carga sintética pelos fluxos normais. Finalizar a carga e testar o retorno `start/check --modo sem-integracoes`. Demonstrar persistência com `stop` e retomada no mesmo modo, sem prepare/bootstrap/build. Nenhuma dessas ações foi autorizada para execução na rodada atual.

## HTTP local da apresentação de 24/11/2026

O modo `http-local` usa **http://localhost:8081**. O Compose publica somente o Nginx em `127.0.0.1:8081`. Use o hostname `localhost` e a porta indicada. O ingresso rejeita outro Host e o Spring exige o proxy identificado e a origem canônica. No navegador, a API continua em `/api`. JWT e CSRF continuam com `Secure` e `SameSite=Lax`; só o token CSRF é legível pelo JavaScript. HTTP local não cifra o tráfego. A compatibilidade dos cookies deve ser conferida no navegador e perfil usados na aula.

São requisitos adicionais Docker Engine 28+, Compose com suporte a `!override` e `!reset`, porta 8081 livre e apresentação com PostgreSQL, imagens originais e bootstrap registrados. Antes de autorizar a partida, conferir se o JAR HTTP leva o mesmo changelog do banco preparado. Qualquer alteração pendente no schema exige decisão própria.

Após autorização, execute o procedimento no projeto `gestaoacoes` com `$demo` apontando para o **diretório privado já existente**.

```powershell
mvn -o -DskipTests package
node ops/apresentacao.cjs build-http "$demo"
node ops/apresentacao.cjs start "$demo" --autorizar-migrations --modo sem-integracoes --transporte http-local
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
```

Execute cada linha só após sucesso da anterior. `build-http` é uma preparação única. Cria uma tag de backend separada e um recibo em `http-local/`, mantendo as imagens originais e os hashes do manifesto. O Nginx HTTP usa a imagem Angular já preparada e recebe a configuração verificada por montagem somente leitura, sem as montagens TLS. `start` usa o container PostgreSQL existente, verifica imagem e volume, confirma identidade e troca somente os serviços web. Se houver falha, consultar `status` e o estado pendente antes de repetir. Se sobrar `execucao.lock`, verificar se existe um operador/processo ativo antes de qualquer intervenção. Não remover recibos ou refazer `prepare`, `build` ou `bootstrap` para resolver falha de transição.

`--modo` continua selecionando integrações separadamente de `--transporte`. Use `sem-integracoes` no primeiro ensaio. O modo conectado requer a preparação e autorização próprias descritas acima. A sondagem da API é independente dos cookies no navegador. No primeiro ensaio, confirmar abertura pelo hostname canônico, indisponibilidade pela rede externa, login/logout, CSRF, papéis e consulta financeira com dados já preparados. O teste isolado de cookies usa um servidor de loopback descartável e deve ser executado com a porta 8081 livre. Para repetir no Chrome instalado no caminho padrão:

```powershell
$env:APRESENTACAO_BROWSER_EXE = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
node --test ops/apresentacao-http-browser.test.cjs
```

Para retornar ao HTTPS, confirmar a validade e o hostname do certificado existente e, após autorização, usar a origem HTTPS original. A confiança do navegador continua sendo requisito para o acesso HTTPS.

```powershell
node ops/apresentacao.cjs start "$demo" --autorizar-migrations --modo sem-integracoes --transporte https
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte https
```

A troca para HTTPS restaura a configuração e as imagens web originais no mesmo projeto e mantém o único PostgreSQL. Use o modo de integrações anteriormente autorizado, se for diferente de `sem-integracoes`.

## Janela temporária de integrações cadastrais no ambiente limpo

O modo `integracoes-cadastrais` é exclusivo do projeto `gademofc3279fb5761`, com volume `gademofc3279fb5761_data` e transporte `http-local`. Informe em `$demo` o diretório privado existente da execução aprovada. O roteiro verifica manifesto, configuração renderizada e recursos Docker. A partida também verifica o marcador do banco `ops_guard.execution` antes de trocar os serviços web.

O backend reutiliza a imagem HTTP aprovada. BrasilAPI e ViaCEP passam a usar os endereços públicos. BRAPI e Alpha Vantage continuam com os destinos locais desativados e sem suas chaves montadas. A rede externa é ligada somente ao backend novo e permite saída sem lista de destinos. Mantenha a janela aberta somente pelo tempo necessário para a validação cadastral autorizada.

Depois da autorização de ativação, confirme PostgreSQL saudável, volume e marcador corretos, histórico Liquibase compatível e lock livre. Execute em PowerShell, da raiz do projeto, interrompendo se qualquer comando falhar.

```powershell
$demo = Read-Host 'Informe o diretório privado existente da apresentação'
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
node ops/apresentacao.cjs resources "$demo" --modo integracoes-cadastrais
node ops/apresentacao.cjs start "$demo" --modo integracoes-cadastrais --transporte http-local --autorizar-migrations
node ops/apresentacao.cjs check "$demo" --modo integracoes-cadastrais --transporte http-local
```

O `start` exige `--autorizar-migrations`. Use-o somente após confirmar a compatibilidade do JAR com os dez changesets aplicados e a ausência de pendências. Durante a janela, confira o CNPJ e a situação retornados pela BrasilAPI e confira o CEP e a coerência do endereço via ViaCEP antes de registrar a corretora. Interrompa em caso de divergência cadastral ou falha de integração.

Para encerrar a janela, use o mesmo diretório privado. Confirme novamente o histórico Liquibase e o lock antes do `start`.

```powershell
node ops/apresentacao.cjs start "$demo" --modo sem-integracoes --transporte http-local --autorizar-migrations
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
```

O retorno recria o backend com a topologia offline e o `check` recusa uma associação residual à rede externa. A rede de saída permanece registrada, sem membros. PostgreSQL e volume são preservados. Em caso de transição incompleta, confira `status` e a topologia real antes de qualquer nova tentativa.

## Atualização e recuperação somente do frontend HTTP limpo

Este fluxo é exclusivo de `gademofc3279fb5761` em `sem-integracoes/http-local` e exige aprovação de implantação separada. Preserve o diretório privado existente. Use o build Angular visualmente aprovado e um contexto de build separado contendo apenas `browser/`, `ops/frontend.Dockerfile` como `Dockerfile` e `ops/nginx.conf` como `nginx.conf`. Use a base Nginx fixada em `PINS.frontend`, Linux/amd64, o rótulo `gestaoacoes.operacional.run` com o identificador da execução registrada no manifesto e uma tag candidata distinta. Confira a integridade dos arquivos estáticos e registre o ID completo `sha256:` da imagem candidata. A verificação de compatibilidade do roteiro cobre ID, rótulo, plataforma, prefixo de camadas da base e configuração de execução. A procedência do Angular precisa ser verificada no empacotamento.

Antes da troca, registre o ID dos containers e as contagens do banco por leitura. Execute da raiz do repositório, depois da autorização:

```powershell
$demo = Read-Host 'Informe o diretório privado existente da apresentação'
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
node ops/apresentacao.cjs update-frontend "$demo" $idCompletoDaImagem
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
```

Interrompa se qualquer comando falhar. A atualização usa o lock existente, verifica o marcador do banco em sessão somente de leitura e retém a imagem anterior sob tag local `rollback-<ID>`, conferida pelo ID. O manifesto recebe a intenção pendente antes da troca da tag canônica. O Compose reconcilia exclusivamente `frontend`, sem dependências, build ou pull. O recibo é confirmado por substituição atômica após validar a imagem e as redes, a estabilidade dos serviços protegidos e HTTP 200/CSRF 204. Durante a troca, o acesso local pode ficar brevemente indisponível. Depois do check, confira as duas rotas de histórico no navegador e compare as contagens com a linha de base.

Se a versão foi confirmada no manifesto mas falhou na validação visual, execute:

```powershell
node ops/apresentacao.cjs rollback-frontend "$demo"
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
```

Se a operação falhar ou for interrompida com `pendingFrontend` gravado, use:

```powershell
node ops/apresentacao.cjs recover-frontend "$demo"
node ops/apresentacao.cjs check "$demo" --modo sem-integracoes --transporte http-local
```

A recuperação restaura a imagem anterior de uma atualização pendente ou conclui um rollback pendente. Ela compara a tag e o container reais com os IDs do manifesto e só reconcilia o Nginx quando necessário. Um estado desconhecido ou a mudança de identidade, imagem ou horário de partida dos serviços protegidos exige inspeção do operador. Uma falha anterior ao registro da intenção pode deixar uma tag de retenção válida e o frontend original intacto. Confirme o `check` antes de decidir repetir a atualização.

Se um encerramento abrupto deixar `execucao.lock`, o roteiro recusa a execução. Siga a verificação de lock do runbook e confirme que o processo Node e qualquer subprocesso Docker/Compose da operação já terminaram antes de liberar exclusivamente esse lock órfão. Preserve o manifesto e as tags. Após liberar um lock comprovadamente órfão, use `recover-frontend` quando houver intenção pendente e verifique o resultado com `check`. Se a recuperação recusar o estado, interrompa para diagnóstico.

Preserve as imagens retidas e a base fixada durante a apresentação. A troca, a substituição atômica sob falhas de I/O do host e a recuperação real no projeto persistente exigem validação na janela autorizada. Os testes direcionados exercitam a lógica de estados e falhas com mocks.
