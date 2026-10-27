& {
    Set-StrictMode -Version Latest
    $ErrorActionPreference = 'Stop'

    $raiz = 'C:\Users\julia\Projetos\GestaoAcoes\gestaoacoes'
    $opcoesJavaAnteriores = $env:JAVA_TOOL_OPTIONS
    $estado = @{
        Fase = 'INICIO'
        Processo = $null
        ExitCodeMaven = $null
    }
    $pasta = $null
    $localAlterado = $false

    function Exigir([bool]$condicao, [string]$mensagem) {
        if (-not $condicao) { throw $mensagem }
    }

    function Registrar([string]$evento, $detalhes) {
        if ($null -ne $pasta) {
            [pscustomobject]@{
                Instante = (Get-Date).ToString('o')
                Fase = $estado.Fase
                Evento = $evento
                ExitCodeMaven = $estado.ExitCodeMaven
                Detalhes = $detalhes
            } | ConvertTo-Json -Depth 6 -Compress |
                Add-Content -LiteralPath (Join-Path $pasta 'diario.ndjson') `
                    -Encoding UTF8
        }
    }

    function LerXml([string]$caminho) {
        $documento = New-Object System.Xml.XmlDocument
        $documento.XmlResolver = $null
        $documento.Load($caminho)
        return ,$documento
    }

    function BackupVerificado([string]$origem, [string]$destino) {
        if (-not (Test-Path -LiteralPath $origem)) { return }

        Exigir (-not (Test-Path -LiteralPath $destino)) `
            "Destino de backup já existe: $destino"

        Copy-Item -LiteralPath $origem -Destination $destino -Recurse

        if (Test-Path -LiteralPath $origem -PathType Container) {
            foreach ($arquivo in Get-ChildItem -LiteralPath $origem -Recurse -File) {
                $relativo = $arquivo.FullName.Substring($origem.Length).TrimStart('\')
                $copia = Join-Path $destino $relativo
                Exigir (Test-Path -LiteralPath $copia -PathType Leaf) `
                    "Arquivo ausente no backup: $relativo"
                Exigir (
                    (Get-FileHash -LiteralPath $arquivo.FullName).Hash -eq
                    (Get-FileHash -LiteralPath $copia).Hash
                ) "Hash divergente no backup: $relativo"
            }
        } else {
            Exigir (
                (Get-FileHash -LiteralPath $origem).Hash -eq
                (Get-FileHash -LiteralPath $destino).Hash
            ) "Hash divergente no backup: $origem"
        }
    }

    function InvocarMaven([string[]]$argumentos, [string]$nome) {
        $wrapper = Join-Path $raiz 'mvnw.cmd'
        $cmd = Join-Path $env:SystemRoot 'System32\cmd.exe'

        # Os argumentos são os definidos pelo script.
        # Caminhos com espaços, como o valor de -l, já chegam entre aspas.
        $argumentosTexto = $argumentos -join ' '

        # Resultado:
        # ""C:\caminho com espaços\mvnw.cmd" -l "C:\log com espaços.log" verify"
        $linha = '""{0}" {1}"' -f $wrapper, $argumentosTexto
        $argumentosCmd = '/d /s /c ' + $linha

        Registrar 'PROCESSO_SOLICITADO' @{
            Nome = $nome
            Argumentos = $argumentos
        }

        $processo = Start-Process -FilePath $cmd `
            -ArgumentList $argumentosCmd `
            -WorkingDirectory $raiz `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $pasta "$nome.stdout.log") `
            -RedirectStandardError (Join-Path $pasta "$nome.stderr.log") `
            -PassThru

        $estado.Processo = $processo
        Registrar 'PROCESSO_INICIADO' @{
            Nome = $nome
            PID = $processo.Id
        }

        # Reter o handle antes da espera permite consultar ExitCode no PS5.1.
        [void]$processo.Handle
        $processo.WaitForExit()
        $processo.Refresh()

        $codigoObtido = $processo.ExitCode
        Exigir ($null -ne $codigoObtido) 'ExitCode indisponível.'
        $codigo = [int]$codigoObtido

        if ($nome -eq 'verify') {
            $estado.ExitCodeMaven = $codigo
        }

        # Persistir antes de apresentar ou analisar os relatórios.
        $codigo | Set-Content -LiteralPath `
            (Join-Path $pasta "$nome.exitcode.txt") -Encoding UTF8

        Registrar 'PROCESSO_FINALIZADO' @{
            Nome = $nome
            PID = $processo.Id
            ExitCode = $codigo
        }

        $processo.Dispose()
        $estado.Processo = $null

        return $codigo
    }

    try {
        Exigir (Test-Path -LiteralPath $raiz -PathType Container) `
            'Diretório do projeto não encontrado.'
        Push-Location -LiteralPath $raiz
        $localAlterado = $true

        # Evita caracteres interpretáveis pelo cmd nos caminhos construídos.
        Exigir ($raiz -notmatch '[&|<>^%!"\r\n]') `
            'Caminho exige revisão de escaping.'
        Exigir (Test-Path -LiteralPath '.\mvnw.cmd' -PathType Leaf) `
            'Maven Wrapper ausente.'

        $id = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' +
            [guid]::NewGuid().ToString('N').Substring(0, 8)
        $pasta = Join-Path $raiz "target\validacao-etapa3-$id"
        Exigir (-not (Test-Path -LiteralPath $pasta)) 'Destino já existente.'
        New-Item -ItemType Directory -Path $pasta -Force | Out-Null
        Registrar 'PROCEDIMENTO_INICIADO' $null

        $estado.Fase = 'PRE_VERIFICACOES'
        $branch = (& git branch --show-current | Out-String).Trim()
        $codigoGit = $LASTEXITCODE
        Exigir ($codigoGit -eq 0 -and $branch -eq 'dev') `
            'A branch precisa ser dev.'

        $gitAntes = @(& git status --short --untracked-files=all)
        $codigoGit = $LASTEXITCODE
        Exigir ($codigoGit -eq 0) 'Não foi possível registrar o estado Git.'
        $gitAntes | Set-Content (Join-Path $pasta 'git-antes.txt')

        $bloqueadas = @(Get-ChildItem Env: | Where-Object {
            -not [string]::IsNullOrWhiteSpace($_.Value) -and (
                $_.Name -match '^(SPRING[._-]|JDBC[._-]|DATASOURCE[._-]|DB_|DATABASE_|PG|POSTGRES|HIBERNATE_|JPA_|TEMPORAL_|BROKER_|HOMOLOGACAO_|CUSTODIA_PG17)' -or
                $_.Name -match '^(BRAPI_|BRASIL_API_|VIA_CEP_|ALPHA_VANTAGE_)' -or
                $_.Name -match '^(JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS|MAVEN_OPTS|MAVEN_ARGS)$'
            )
        })
        Exigir ($bloqueadas.Count -eq 0) (
            'Revisar ambiente manualmente; variáveis presentes: ' +
            (($bloqueadas | Select-Object -ExpandProperty Name) -join ', ')
        )

        foreach ($arquivo in '.mvn\jvm.config', '.mvn\maven.config') {
            if (Test-Path -LiteralPath $arquivo) {
                Exigir ([string]::IsNullOrWhiteSpace(
                    (Get-Content -LiteralPath $arquivo -Raw)
                )) "Revisar manualmente $arquivo."
            }
        }

        $pom = LerXml (Join-Path $raiz 'pom.xml')
        $plugin = $pom.SelectSingleNode(
            "//*[local-name()='plugin'][*[local-name()='artifactId']='jacoco-maven-plugin']"
        )
        Exigir ($null -ne $plugin) 'Plugin JaCoCo ausente.'
        $limites = @($plugin.SelectNodes(".//*[local-name()='limit']"))
        Exigir ($limites.Count -eq 2) 'Revisar limites JaCoCo.'
        foreach ($tipo in 'LINE', 'BRANCH') {
            $limite = @($limites | Where-Object {
                $_.SelectSingleNode("*[local-name()='counter']").InnerText -eq $tipo
            })
            Exigir ($limite.Count -eq 1) "Limite $tipo ausente."
            Exigir (
                $limite[0].SelectSingleNode("*[local-name()='minimum']").InnerText -eq '0.80'
            ) "O limite $tipo deve permanecer0.80."
        }
        Exigir (
            $plugin.SelectSingleNode(
                ".//*[local-name()='rule']/*[local-name()='element']"
            ).InnerText -eq 'BUNDLE'
        ) 'Escopo JaCoCo deve permanecer BUNDLE.'

        $agente = Join-Path $env:USERPROFILE `
            '.m2\repository\org\mockito\mockito-core\5.23.0\mockito-core-5.23.0.jar'
        Exigir (Test-Path -LiteralPath $agente -PathType Leaf) `
            'Mockito5.23.0 ausente; não instalar automaticamente.'

        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [IO.Compression.ZipFile]::OpenRead($agente)
        try {
            $entrada = $zip.GetEntry('META-INF/MANIFEST.MF')
            Exigir ($null -ne $entrada) 'Manifesto do agente ausente.'
            $leitor = New-Object IO.StreamReader($entrada.Open())
            try { $manifesto = $leitor.ReadToEnd() }
            finally { $leitor.Dispose() }
            Exigir ($manifesto -match '(?m)^Premain-Class:\s*\S+') `
                'JAR não declara agente inicializável.'
        }
        finally { $zip.Dispose() }
        Get-FileHash -LiteralPath $agente -Algorithm SHA256 |
            Export-Csv (Join-Path $pasta 'agente-sha256.csv') `
                -NoTypeInformation -Encoding UTF8

        # Backup completo antes de retirar qualquer relatório ativo.
        $estado.Fase = 'BACKUP'
        $antes = Join-Path $pasta 'antes'
        $retirados = Join-Path $pasta 'retirados'
        New-Item -ItemType Directory -Path $antes, $retirados | Out-Null

        $ativos = @{
            'target\surefire-reports' = 'surefire-reports'
            'target\site\jacoco'      = 'jacoco'
            'target\jacoco.exec'      = 'jacoco-default.exec'
        }
        foreach ($relativo in $ativos.Keys) {
            $origem = [IO.Path]::GetFullPath((Join-Path $raiz $relativo))
            Exigir ($origem.StartsWith(
                $raiz + '\', [StringComparison]::OrdinalIgnoreCase
            )) 'Origem fora do workspace.'
            BackupVerificado $origem (Join-Path $antes $ativos[$relativo])
        }

        foreach ($arquivo in Get-ChildItem -LiteralPath 'target' -File) {
            if ($arquivo.Extension -in '.log', '.md', '.json', '.xml', '.exec' -and
                $arquivo.Name -ne 'jacoco.exec') {
                BackupVerificado $arquivo.FullName (Join-Path $antes $arquivo.Name)
            }
        }
        Get-ChildItem -LiteralPath $antes -Recurse -File |
            Get-FileHash -Algorithm SHA256 |
            Export-Csv (Join-Path $pasta 'backup-sha256.csv') `
                -NoTypeInformation -Encoding UTF8
        Registrar 'BACKUP_VERIFICADO' $antes

        foreach ($relativo in $ativos.Keys) {
            $origem = [IO.Path]::GetFullPath((Join-Path $raiz $relativo))
            if (Test-Path -LiteralPath $origem) {
                Move-Item -LiteralPath $origem `
                    -Destination (Join-Path $retirados $ativos[$relativo])
            }
        }

        # Não alterar argLine: ele continua pertencendo ao JaCoCo.
        $env:JAVA_TOOL_OPTIONS = '-javaagent:"{0}"' -f $agente
        $estado.Fase = 'VERSAO_E_AGENTE'
        $codigoVersao = InvocarMaven @('-v') 'versao'
        Exigir ($codigoVersao -eq 0) 'Maven/JVM/agente não inicializou.'
        $versao = Get-Content (Join-Path $pasta 'versao.stdout.log') -Raw
        $java = [regex]::Match($versao, 'Java version:\s*(\d+)')
        Exigir ($java.Success -and [int]$java.Groups[1].Value -ge 17) `
            'Java17 ou superior necessário.'

        # Exatamente uma execução de verify.
        $estado.Fase = 'VERIFY'
        $logMaven = Join-Path $pasta 'verify.maven.log'
        $argumentos = @(
            '-B'
            '-l', ('"{0}"' -f $logMaven)
            '-Dhomologacao.custodia.real=DESATIVADO'
            '-Dtemporal.test.postgresql.enabled=false'
            '-Dtemporal.test.postgresql.url='
            '-Dbroker.test.postgresql.enabled=false'
            '-Dbroker.test.postgresql.url='
            '-Dtemporal.poc.enabled=false'
            '-DskipTests=false'
            '-Dmaven.test.skip=false'
            '-Dmaven.test.failure.ignore=false'
            '-DfailIfNoTests=true'
            '-DforkCount=1'
            '-DreuseForks=true'
            '-Djacoco.skip=false'
            '-Djacoco.append=false'
            'verify'
        )
        $inicio = Get-Date
        $codigoMaven = InvocarMaven $argumentos 'verify'

        # Tee recebe apenas texto de arquivo, depois de salvar ExitCode.
        # stdout/stderr originais permanecem separados e intactos.
        $estado.Fase = 'APRESENTACAO'
        if (Test-Path -LiteralPath $logMaven) {
            Get-Content -LiteralPath $logMaven |
                Tee-Object -FilePath (Join-Path $pasta 'verify.exibido.log')
        }
        Write-Host "ExitCode Maven: $codigoMaven"

        $estado.Fase = 'CONFERENCIA'
        $condicionais = @(
            'com.projeto.custodia.postgresql.CustodiaPostgresCapacidadeRealTest'
            'com.projeto.custodia.postgresql.CustodiaPostgresHomologacao19RealTest'
            'com.projeto.custodia.postgresql.CustodiaPostgresConcorrenciaRealTest'
            'com.projeto.services.AplicacaoCotacaoPostgresqlTest'
            'com.projeto.resources.ReferenciaDataJpaPostgresqlTest'
            'com.projeto.services.AtualizacaoCotacoesCarteiraPostgresqlTest'
            'com.projeto.services.DistribuicaoPosicaoCorretoraPostgresqlTest'
            'com.projeto.temporalpoc.ReferenciaDataBcPrototypeTest'
        )
        $assumptionsPg = @(
            'com.projeto.repositories.ReferenciaTemporalMigrationTest'
            'com.projeto.repositories.ReferenciaDataJdbcDiagnosticoTest'
        )
        $testes = 0; $falhas = 0; $erros = 0
        $skips = @(); $externosExecutados = @(); $presentes = @()
        $xmls = @(Get-ChildItem 'target\surefire-reports' `
            -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue)

        foreach ($arquivo in $xmls) {
            $xml = LerXml $arquivo.FullName
            $suite = $xml.DocumentElement
            $nome = $suite.GetAttribute('name')
            $presentes += $nome
            $testes += [int]$suite.GetAttribute('tests')
            $falhas += [int]$suite.GetAttribute('failures')
            $erros += [int]$suite.GetAttribute('errors')

            if ($nome -in $condicionais -and
                [int]$suite.GetAttribute('tests') -ne
                [int]$suite.GetAttribute('skipped')) {
                $externosExecutados += $nome
            }
            foreach ($caso in $suite.SelectNodes('testcase')) {
                $skip = $caso.SelectSingleNode('skipped')
                if ($null -ne $skip) {
                    $classe = $caso.GetAttribute('classname')
                    if ([string]::IsNullOrWhiteSpace($classe)) { $classe = $nome }
                    $nomeCaso = $caso.GetAttribute('name')
                    $assumptionEsperada = $classe -in $assumptionsPg -and
                        ($nomeCaso + ' ' + $skip.GetAttribute('message') + ' ' + $skip.InnerText) -match '(?i)postgres'
                    $skips += [pscustomobject]@{
                        Classe = $classe
                        Caso = $nomeCaso
                        Previsto = ($classe -in $condicionais -or $assumptionEsperada)
                    }
                }
            }
        }
        $skips | Export-Csv (Join-Path $pasta 'skips.csv') `
            -NoTypeInformation -Encoding UTF8

        $ausentes = @($condicionais | Where-Object { $_ -notin $presentes })
        $naoPrevistos = @($skips | Where-Object { -not $_.Previsto })
        $cobertura = @()
        $xmlCobertura = Join-Path $raiz 'target\site\jacoco\jacoco.xml'
        if (Test-Path -LiteralPath $xmlCobertura) {
            $jacoco = LerXml $xmlCobertura
            foreach ($tipo in 'LINE', 'BRANCH') {
                $contador = $jacoco.SelectSingleNode("/report/counter[@type='$tipo']")
                if ($null -ne $contador) {
                    $cobertos = [double]$contador.GetAttribute('covered')
                    $total = $cobertos + [double]$contador.GetAttribute('missed')
                    $razao = 0
                    if ($total -gt 0) { $razao = $cobertos / $total }
                    $cobertura += [pscustomobject]@{
                        Tipo = $tipo
                        Percentual = 100 * $razao
                        Aprovado = ($total -gt 0 -and $razao -ge 0.80)
                    }
                }
            }
        }

        $checkExecutado = $false
        if (Test-Path -LiteralPath $logMaven) {
            $checkExecutado = $null -ne (
                Select-String -LiteralPath $logMaven `
                    -SimpleMatch 'jacoco:0.8.13:check' | Select-Object -First 1
            )
        }
        $dadosNovos = Test-Path -LiteralPath 'target\jacoco.exec'
        if ($dadosNovos) {
            $dados = Get-Item -LiteralPath 'target\jacoco.exec'
            $dadosNovos = $dados.Length -gt 0 -and $dados.LastWriteTime -ge $inicio
        }
        $coberturaOk = $cobertura.Count -eq 2 -and
            @($cobertura | Where-Object { -not $_.Aprovado }).Count -eq 0

        $gitDepois = @(& git status --short --untracked-files=all)
        $codigoGit = $LASTEXITCODE
        Exigir ($codigoGit -eq 0) 'Falha ao conferir estado Git final.'
        $gitDepois | Set-Content (Join-Path $pasta 'git-depois.txt')
        $gitIgual = ($gitAntes -join "`n") -eq ($gitDepois -join "`n")

        $aprovado = $codigoMaven -eq 0 -and $testes -gt 0 -and
            $falhas -eq 0 -and $erros -eq 0 -and
            $externosExecutados.Count -eq 0 -and $ausentes.Count -eq 0 -and
            $naoPrevistos.Count -eq 0 -and $checkExecutado -and
            $dadosNovos -and $coberturaOk -and $gitIgual

        $resumo = [pscustomobject]@{
            ExitCodeMaven = $codigoMaven
            Testes = $testes
            Falhas = $falhas
            Erros = $erros
            SkipsPrevistos = @($skips | Where-Object { $_.Previsto }).Count
            SkipsNaoPrevistos = $naoPrevistos
            CondicionaisExecutados = $externosExecutados
            RelatoriosCondicionaisAusentes = $ausentes
            JacocoCheckExecutado = $checkExecutado
            DadosJacocoNovos = $dadosNovos
            Cobertura = $cobertura
            EstadoGitPreservado = $gitIgual
            Aprovado = $aprovado
        }
        $resumo | ConvertTo-Json -Depth 6 |
            Tee-Object -FilePath (Join-Path $pasta 'resultado.json')
        Registrar 'CONFERENCIA_FINALIZADA' $resumo

        Exigir $aprovado `
            'Validação não aprovada. Diagnosticar antes de repetir; não alterar limites ou asserts.'
    }
    catch {
        $erroOriginal = $_
        try {
            Registrar 'FALHA_DO_PROCEDIMENTO' @{
                Tipo = $erroOriginal.Exception.GetType().FullName
                Mensagem = $erroOriginal.Exception.Message
            }
        }
        catch {
            Write-Warning 'Não foi possível acrescentar o diagnóstico ao diário.'
        }
        throw $erroOriginal
    }
    finally {
        if ($null -ne $estado.Processo) {
            try {
                $estado.Processo.Refresh()
                if (-not $estado.Processo.HasExited) {
                    Registrar 'PROCESSO_AINDA_ATIVO' @{ PID = $estado.Processo.Id }
                    Write-Warning (
                        'Processo ainda ativo, PID ' + $estado.Processo.Id +
                        '. Resultado inconclusivo; não iniciar outra execução.'
                    )
                }
            } catch {
                Write-Warning 'Estado final do processo inconclusivo.'
            }
        }

        if ($null -eq $opcoesJavaAnteriores) {
            Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
        } else {
            $env:JAVA_TOOL_OPTIONS = $opcoesJavaAnteriores
        }
        if ($localAlterado) { Pop-Location }
        if ($null -ne $pasta) { Write-Host "Evidências: $pasta" }
    }
}
