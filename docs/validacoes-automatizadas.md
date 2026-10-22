# Testes e cobertura automatizados

## Execucao local

Requer Windows, PowerShell 7.2+, JDK 17 (java e javac no PATH), Node.js
24.19.0 e npm 11.17.0. O Maven Wrapper fornece Maven 3.9.16; nao e necessario
instalar Maven globalmente. O primeiro uso pode baixar dependencias.

Na raiz Git `gestaoacoes`, com as dependencias frontend previamente instaladas:

```powershell
pwsh -NoProfile -File ./scripts/verify-coverage.ps1
```

Se necessario, prepare o frontend separadamente em seu diretorio:

```powershell
npm.cmd ci
```

O script nao instala dependencias npm nem carrega `.env`. Executa o backend antes
do frontend, independentemente do diretorio de onde foi chamado. Usa `mvnw.cmd`
e `npm.cmd`, confere as versoes e os portoes configurados e restaura as variaveis
de trabalhadores do processo ao terminar.

## Comandos e isolamento

O backend executa `verify` com os argumentos existentes:

```text
-Dtemporal.test.postgresql.url=
-Dtemporal.test.postgresql.enabled=false
-Dbroker.test.postgresql.enabled=false
-Dtemporal.poc.enabled=false
-Djacoco.append=false
```

Utiliza settings Maven vazios exclusivos da execucao (`-s` e `-gs`) para evitar
perfis externos do usuario. Configuracoes nao vazias em `.mvn/maven.config` e
`.mvn/jvm.config` sao rejeitadas. O script tambem rejeita variaveis herdadas de
Spring, banco, perfis, homologacao PostgreSQL e injecao de argumentos JVM/Maven.
Os nomes conflitantes sao informados sem mostrar seus valores. Nao altera essas
configuracoes nem injeta um perfil Spring global: isso conflitaria com o
isolamento dos contextos H2 de custodia.

O JDK usado pelo Wrapper tambem e conferido, pois `JAVA_HOME` pode divergir do
Java encontrado no PATH. Sandboxes que bloqueiam o attach local da JVM podem
impedir Mockito; nesse caso, registrar a limitacao e usar somente um terminal
autorizado, sem desabilitar testes ou suas protecoes.

Os testes existentes usam H2 descartavel e mocks/stubs de integracoes.
Os cenarios PostgreSQL condicionais e o prototipo temporal nao sao habilitados;
devem ser homologados separadamente mediante autorizacao. Testes unitarios dos
adaptadores operacionais de custodia usam transportes simulados e continuam
incluidos, sem iniciar Docker ou conexoes PostgreSQL reais.

O frontend executa `npm.cmd run test:coverage`, equivalente a
`ng test --configuration coverage --watch=false`, com `VITEST_MAX_WORKERS=1` e
`NG_BUILD_MAX_WORKERS=2`. Nao inicia a aplicacao, Docker, PostgreSQL nem chama
Liquibase diretamente; migrations que fazem parte dos testes usam os contextos
descartaveis existentes. Nao sao fornecidas credenciais de APIs reais.

## Portoes e resultados

- Backend: JaCoCo, linhas >=80% e ramos >=80%, separadamente, no BUNDLE Maven.
- Frontend: Angular/Vitest V8, linhas >=80% e ramos >=80%, globalmente.
- Fontes nao exercitadas permanecem no denominador conforme as configuracoes
  existentes. Nao ha portao por arquivo ou cobertura por diff.

Qualquer comando nativo reprovado tem seu codigo propagado. Se o backend falhar,
o frontend fica `NAO_EXECUTADO`. Falha de preparacao nao executa nenhum projeto.
Ausencia de relatorio obrigatorio tambem reprova. `APROVADO` exige comando
concluido com sucesso e relatorios obrigatorios presentes. Contagens de testes,
falhas, ignorados e percentuais ficam no console e nos relatorios das ferramentas.

Cada chamada cria `target/coverage-verification/<id>/current/`, contendo
`result.json`, `summary.md` e os relatorios novos disponiveis:

| Destino em current | Conteudo |
| --- | --- |
| `surefire/` | Resultados XML/TXT dos testes backend |
| `jacoco/` | HTML e XML JaCoCo |
| `execution` | Dados JaCoCo da execucao |
| `frontend/frontend/` | HTML, LCOV e JSON da cobertura frontend |

Antes dos testes, os relatorios anteriores em `target/surefire-reports`,
`target/site/jacoco`, `target/jacoco.exec` e `frontend/coverage` sao movidos intactos
para `<id>/previous/`. Essa pasta nunca e publicada como evidencia atual. Caminhos
fora do repositorio ou com links de filesystem sao rejeitados. Cada relatorio
esperado e listado como `DISPONIVEL` ou `AUSENTE`; falhas precoces podem impedir
sua geracao. Nao execute duas verificacoes ao mesmo tempo no mesmo checkout.

## GitHub Actions

O workflow `coverage.yml` usa Windows hospedado (`windows-2025`), Temurin 17,
Node.js 24.19.0 e npm 11.17.0. Faz instalacao limpa com `npm ci`, usa caches de
dependencias Maven/npm e chama o mesmo script sequencial. Os gatilhos sao push
em dev/main, pull requests destinados a dev/main e execucao manual.

As evidencias atuais sao publicadas mesmo quando a verificacao falha, com
retencao de 14 dias. Se a preparacao impedir a chamada do script, o resumo informa
que os relatorios estao ausentes. O workflow tem apenas `contents: read`, nao
recebe credenciais de desenvolvimento, nao configura servicos e nao faz deploy.

## Limitacoes e proximos passos

O CI so verifica arquivos presentes no commit publicado, nao alteracoes locais ou
arquivos nao rastreados. A execucao real no Actions depende de commit/push
posteriormente autorizados. O runner, caches e upload precisam dessa validacao
remota. A versao corretiva de Java 17 e as imagens do runner podem evoluir;
Node/npm estao fixos. As actions usam tags de versao principal oficiais.

Downloads de ferramentas e dependencias exigem rede. Os testes usam simulacoes
existentes, mas este script nao e um firewall: novos testes precisam preservar
esse isolamento. Nao ha garantia de comportamento PostgreSQL a partir de H2.
Cancelamento forcado pode impedir a finalizacao e o upload de relatorios.

O workflow reprovado nao bloqueia merge sozinho: protecao de branches/checks
obrigatorios depende de configuracao remota separadamente autorizada.

## Pre-commit versionado (instalacao explicita)

Os arquivos `.githooks/pre-commit`, `scripts/pre-commit.ps1` e
`scripts/install-hooks.ps1` fornecem uma verificacao rapida. Apenas criar ou
clonar esses arquivos nao instala o hook. Nesta etapa, o hook nao foi instalado
no projeto real.

Requer Git for Windows e PowerShell 7.2+ (`pwsh.exe` ou `pwsh` no PATH). O hook
shell localiza PowerShell, usa caminhos relativos ao repositorio e propaga seu
codigo de saida. Falta de PowerShell retorna 127; erros de validacao retornam 1.
Nao ha instalacao automatica, alteracao de ExecutionPolicy, stash, correcao de
arquivos ou modificacao do indice.

Quando a instalacao for autorizada, executar explicitamente na copia desejada:

```powershell
pwsh -NoProfile -File ./scripts/install-hooks.ps1
```

O instalador inspeciona `core.hooksPath` de todos os escopos e o diretorio de hooks
padrao. Configuracoes preexistentes ou arquivos diferentes de `*.sample` impedem
a instalacao, sem sobrescrita ou encadeamento automatico. Uma instalacao local
ja apontando exclusivamente para `.githooks` e reconhecida sem nova escrita.
Somente a configuracao Git local e alterada; hooks Codex/Graphify e configuracoes
globais permanecem intactos. Reverter explicitamente com:

```powershell
pwsh -NoProfile -File ./scripts/install-hooks.ps1 -Uninstall
```

A reversao so remove a chave local se ela corresponder exclusivamente a
`.githooks`; preserva os arquivos e volta a usar a configuracao herdada/padrao.

O pre-commit executa `git diff --cached --check` e analisa os blobs do indice,
incluindo nomes com espacos, sem consultar o conteudo correspondente no working
tree. Verifica arquivos adicionados, modificados, copiados ou com tipo alterado;
renomes sao tratados como adicao/remocao, e arquivos removidos nao sao analisados.

- `.json`: JSON estrito; `tsconfig.json`, `tsconfig.*.json` e `.jsonc`: comentarios e virgulas finais
  permitidos, respeitando o formato JSONC ja utilizado pelo TypeScript;
- `.xml`: XML bem formado, com DTD proibido e resolucao externa desativada;
- `.ps1`: somente analise sintatica PowerShell, sem executar o conteudo.

Os validadores sao integrados ao PowerShell/.NET; nao precisam de npm, Java,
Python ou servicos externos. Mensagens identificam arquivo e motivo da falha.
YAML, schemas JSON/XML e semantica dos scripts nao sao validados por esse hook.
Arquivos nao alterados no indice nao sao revalidados; arquivos nao rastreados
precisam ser preparados explicitamente para participar da verificacao.

Testes e cobertura completos permanecem no script `verify-coverage.ps1` e no CI.
O hook pode ser contornado com `git commit --no-verify` e nao substitui checks
obrigatorios no servidor. Politicas locais podem impedir a execucao PowerShell;
isso deve bloquear com erro, sem desabilitar a politica automaticamente.
O fluxo foi validado para Git for Windows; permissoes de execucao do hook em
sistemas Unix exigem validacao separada.
