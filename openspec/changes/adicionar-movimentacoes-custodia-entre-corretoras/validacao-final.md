# Procedimento e resultado da validação final — tarefa 6.10

Procedimento preparado em 29/10/2026 e posteriormente executado por autorização do usuário. **6.10 concluída e aprovada tecnicamente**, com marcação autorizada após a execução. Estado atual: 58/60 tarefas marcadas; 5.10 e 6.5 permanecem desmarcadas com as exceções aceitas. Fonte: `target/custodia-validacao-final-20261029-190655/relatorio.md` e `resultado-consolidado.json`. O procedimento abaixo foi o aprovado para essa rodada; não autoriza repetição automática.

## Escopo mínimo

1. Uma execução global backend Maven/H2 pelo procedimento Windows já aprovado, incluindo regressões financeiras, persistência, migration/upgrade/rollback H2, REST e o novo teste de invariância financeira.
2. Após aprovação do backend, uma execução global de cobertura Angular, abrangendo os novos fluxos de custódia, parsers, mensagens, recargas e regressões dos consumidores existentes.
3. Um build de produção Angular para conferir compilação, templates e budgets do conjunto final, sem substituir o teste de cobertura. As validações da Etapa 4 são anteriores aos novos componentes da Etapa 5.
4. Conferência documental e dos arquivos de migrations, revisão do Git/Graphify e uma validação estrita OpenSpec.

Não executar separadamente todas as seleções direcionadas já aprovadas: elas integram as suítes globais aplicáveis. Reutilizar as evidências PostgreSQL 1.9, 1.10 e 3.10, incluindo a reconciliação 6.3. Não iniciar Docker/PostgreSQL, repetir homologações ou executar as duas verificações manuais [dispensadas](excecoes-homologacao.md).

## Preparação e preservação

- Confirmar branch `dev`, HEAD, referência local remota, staging, arquivos modificados/não rastreados e hashes dos arquivos anteriores. Não descartar alterações nem executar Git de escrita.
- Conferir ausência de outro Maven/Surefire ou Angular **de testes** concorrente. Distinguir o frontend `ng serve` e o backend descartável dos processos de teste; não encerrar serviços ou processos de terceiros.
- Criar uma pasta inédita `target/custodia-validacao-final-<data-hora>/` para o consolidado, logs, comandos, ExitCodes, inventário Git e hashes; preservar evidências históricas sem sobrescrita.
- O script backend já cria sua própria pasta `target/validacao-etapa3-<id>/`, verifica/copía os relatórios anteriores e separa os dados ativos Surefire/JaCoCo. Registrar o caminho real no consolidado. O nome histórico “etapa3” não restringe a suíte ao código antigo nem ativa uma homologação PostgreSQL.
- Antes do frontend, copiar e verificar por hash qualquer cobertura existente em `frontend/coverage/` e saída anterior em `frontend/dist/`. Retirar somente a cobertura ativa já comprovadamente preservada, mantendo o destino de backup intacto, para que os artefatos finais pertençam à nova execução. Não usar `clean` ou exclusão ampla de diretórios.
- Conferir variáveis Spring/JDBC/datasource e opções Java/Maven antes do backend. O script aprovado rejeita configurações ambientais incompatíveis; não limpar variáveis silenciosamente, imprimir segredos ou improvisar conexão com banco persistente. Interromper e diagnosticar se a pré-verificação falhar.
- Validar o agente Mockito 5.23.0 pelo procedimento existente. A função `InvocarMaven` já trata `cmd.exe`, caminhos com espaços, logs e ExitCode do processo; não substituir pelo ExitCode de `Tee-Object`.

## Comandos do procedimento aprovado e executado

Na raiz `gestaoacoes`, PowerShell do ambiente que já aprovou os testes focados:

```powershell
& .\validar-etapa3.ps1
```

Esse comando executa a versão do Maven e **um único verify**, com logs e resultado JSON próprios. Ele já passa, entre outras proteções:

```text
-Dhomologacao.custodia.real=DESATIVADO
-Dtemporal.test.postgresql.enabled=false
-Dtemporal.test.postgresql.url=
-Dbroker.test.postgresql.enabled=false
-Dbroker.test.postgresql.url=
-Dtemporal.poc.enabled=false
-DskipTests=false
-Dmaven.test.skip=false
-Dmaven.test.failure.ignore=false
-DfailIfNoTests=true
-Djacoco.skip=false
-Djacoco.append=false
verify
```

Não executar um segundo `mvn verify` além do script. Conferir `ExitCodeMaven`, `Aprovado` e os demais campos do `resultado.json` gerado, junto dos logs e XMLs; não inferir aprovação apenas da conclusão do script na tela.

Somente após o backend aprovado, no diretório `frontend`:

```powershell
$env:VITEST_MAX_WORKERS = '1'
$env:NG_BUILD_MAX_WORKERS = '2'
npm run test:coverage
$codigoTestes = $LASTEXITCODE
if ($codigoTestes -ne 0) { throw "Cobertura Angular reprovada: ExitCode $codigoTestes" }
npm run build
$codigoBuild = $LASTEXITCODE
if ($codigoBuild -ne 0) { throw "Build Angular reprovado: ExitCode $codigoBuild" }
```

Preservar/restaurar os valores anteriores dessas variáveis no processo de execução. Não encadear o build automaticamente após cobertura reprovada. Se logs usarem pipeline, capturar o código do processo nativo de forma confiável; não substituir seu resultado por código de um comando posterior.

Conferência de migrations e Git, na raiz, sem aplicar schema:

```powershell
git diff --exit-code HEAD -- src/main/resources/db/changelog
git diff --check
git diff --cached --check
git status --short --untracked-files=all
```

Comparar hashes antes/depois dos arquivos 001–008 e master; conferir oito arquivos de migration e nove changeSets, sem alterações históricas. A global H2 cobre instalação/upgrade/Hibernate/integridade/rollback apropriados; a prova PostgreSQL anterior continua separada, não precisa ser repetida.

Validação documental final, ainda na raiz:

```powershell
openspec validate adicionar-movimentacoes-custodia-entre-corretoras --type change --strict --no-interactive --json
```

Revisar a correspondência do Graphify existente com o código final; atualizar apenas se necessário e dentro da autorização da rodada 6.10, preservando os artefatos anteriores. Documentação por si só não exige reextrair código. Não interpretar timestamp do grafo como prova única de completude.

## Critérios objetivos e evidências

| Área | Aprovação exigida | Artefatos a preservar |
| --- | --- | --- |
| Backend | BUILD SUCCESS, ExitCodeMaven 0, zero falhas/erros, testes efetivamente executados, resultado do script aprovado | Logs Maven/stdout/stderr, ExitCodes, resultado.json, Surefire XMLs, diário e backups/hash |
| Condicionais externos | Nenhum PostgreSQL real/protótipo executado; nenhum skip inesperado nem relatório condicional ausente | Classificador do script, justificativas e XMLs. Historicamente são 102 skips; não aceitar qualquer skip só para repetir a contagem |
| JaCoCo | report e check efetivamente executados; dados novos, BUNDLE LINE ≥80% e BRANCH ≥80% | jacoco.exec atual separado, XML/HTML, contadores cobertos/perdidos e log do check |
| Angular | ExitCode 0, zero falhas/erros/skips inesperados; linhas e ramos globais ≥80% sem novas exclusões | Log, ExitCode, HTML/LCOV/JSON-summary/JSON, totais e limites configurados |
| Build Angular | ExitCode 0, templates/compilação aprovados; identificar warnings e distinguir de erros/budgets reprovados | Log e ExitCode do build, inventário da saída atual |
| Migrations/documentação | Arquivos de migration preservados; contratos/links/contagens coerentes; sem execução de schema persistente | Hashes e diff; referências às homologações anteriores e à auditoria |
| OpenSpec | ExitCode 0, valid=true, issues=[] | Saída JSON e ExitCode reais da rodada final |
| Preservação | Nenhuma alteração inesperada em fontes, testes, configurações, docs ou staging; serviços não encerrados/reiniciados | Git antes/depois, hashes, recibos/estado dos serviços e consolidado final |

Registrar contagens e percentuais **da execução nova**, sem reaproveitar os números globais de 25/10 ou 26/10. Regressões direcionadas já aprovadas não substituem o portão global atual. Não diminuir limites, ignorar testes ou alterar asserts para aprovar.

## Condições de parada e encerramento

Se uma validação falhar, conservar todos os artefatos e distinguir falha funcional, ambiente, teste, cobertura ou documento. Não corrigir automaticamente nem repetir toda a suíte sem diagnóstico. Backend reprovado impede iniciar o frontend nesta sequência.

Com 6.10 aprovada, as exceções 5.10/6.5 permanecem explicitamente não homologadas nos pontos dispensados; a validação estrita não as converte em aprovação manual. 6.10 foi marcada somente após autorização posterior do usuário. Nenhuma nova marcação, execução, commit, push ou arquivamento decorre automaticamente deste registro.
