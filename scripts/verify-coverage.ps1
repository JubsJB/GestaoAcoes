#requires -Version 7.2
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = Split-Path $PSScriptRoot -Parent
$runId = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' + [guid]::NewGuid().ToString('N')
$run = Join-Path $root "target/coverage-verification/$runId"
$current = Join-Path $run 'current'
$result = [ordered]@{
    runId = $runId; startedUtc = [DateTime]::UtcNow.ToString('o')
    preflight = 'NAO_EXECUTADO'; backend = 'NAO_EXECUTADO'; frontend = 'NAO_EXECUTADO'
    exitCode = 1; reports = @(); error = $null
}
$previousWorkers = @{}
foreach ($name in @('VITEST_MAX_WORKERS', 'NG_BUILD_MAX_WORKERS')) {
    $previousWorkers[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Assert-LocalPath([string]$Path) {
    $absolute = [IO.Path]::GetFullPath($Path)
    if (!$absolute.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Caminho de relatorio fora do repositorio.'
    }
    $cursor = $absolute
    while ($cursor -and $cursor -ne $root) {
        if ((Test-Path -LiteralPath $cursor) -and
            ((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'Links de filesystem nao sao aceitos nos caminhos de relatorios.'
        }
        $cursor = Split-Path $cursor -Parent
    }
}

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
    & $Command @Arguments
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        $script:result.exitCode = $code
        throw "Comando reprovado (codigo $code): $([IO.Path]::GetFileName($Command))"
    }
}

# Each old output is moved intact; only newly generated outputs are copied to current/.
$outputs = [ordered]@{
    surefire = 'target/surefire-reports'
    jacoco = 'target/site/jacoco'
    execution = 'target/jacoco.exec'
    frontend = 'frontend/coverage'
}
try {
    if (!$IsWindows) { throw 'Esta verificacao requer Windows e PowerShell 7.2+.' }
    # Reject inherited injection rather than silently sanitizing a developer environment.
    foreach ($entry in Get-ChildItem Env:) {
        $key = $entry.Name -replace '[._-]', ''
        if ($entry.Value -and ($key -match '^(SPRING|HIBERNATECONNECTION|JAKARTAPERSISTENCEJDBC|TEMPORAL|BROKERTESTPOSTGRESQL|CUSTODIA|POSTGRES|PGHOST|PGPORT|PGDATABASE|PGUSER|PGPASSWORD)' -or
            $entry.Name -match '^(JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|JDK_JAVA_OPTIONS|MAVEN_OPTS|MAVEN_ARGS|MVNW_REPOURL|MVNW_USERNAME|MVNW_PASSWORD|NODE_OPTIONS|VITEST_COVERAGE.*|npm_config_argv)$')) {
            throw "Configuracao externa proibida: $($entry.Name). Valores nao sao exibidos."
        }
    }
    foreach ($config in @('.mvn/maven.config', '.mvn/jvm.config')) {
        if ((Test-Path (Join-Path $root $config)) -and
            (Get-Content (Join-Path $root $config) -Raw).Trim()) {
            throw "Argumentos Maven externos nao permitidos: $config"
        }
    }
    foreach ($tool in @('java.exe', 'javac.exe', 'node.exe', 'npm.cmd')) {
        $null = Get-Command $tool -CommandType Application -ErrorAction Stop
    }
    $java = (& java.exe --version | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $java -notmatch '(?m)^(openjdk|java) 17\.') { throw 'Java 17 obrigatorio.' }
    $javac = (& javac.exe -version | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $javac -notmatch '^javac 17\.') { throw 'JDK 17 obrigatorio.' }
    $node = (& node.exe --version | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $node -ne 'v24.19.0') { throw 'Node.js 24.19.0 obrigatorio.' }
    $npm = (& npm.cmd --version | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $npm -ne '11.17.0') { throw 'npm 11.17.0 obrigatorio.' }
    Write-Host "$java`n$javac`nNode $node; npm $npm"
    $result.versions = @{ java = $java; javac = $javac; node = $node; npm = $npm }
    $wrapper = Join-Path $root 'mvnw.cmd'
    if (!(Test-Path $wrapper)) { throw 'Maven Wrapper ausente.' }
    [xml]$pom = Get-Content (Join-Path $root 'pom.xml') -Raw
    $ns = [Xml.XmlNamespaceManager]::new($pom.NameTable)
    $ns.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
    $rule = $pom.SelectSingleNode('//m:execution[m:id="coverage-check" and m:phase="verify"]/m:configuration/m:rules/m:rule[m:element="BUNDLE"]', $ns)
    foreach ($metric in @('LINE', 'BRANCH')) {
        if (!$rule -or !$rule.SelectSingleNode("m:limits/m:limit[m:counter='$metric' and m:value='COVEREDRATIO' and m:minimum='0.80']", $ns)) {
            throw "Portao JaCoCo 80% ausente: $metric"
        }
    }
    $angular = Get-Content (Join-Path $root 'frontend/angular.json') -Raw | ConvertFrom-Json
    $test = $angular.projects.frontend.architect.test
    $threshold = $test.configurations.coverage.coverageThresholds
    if ($test.builder -ne '@angular/build:unit-test' -or $threshold.perFile -ne $false -or
        $threshold.lines -ne 80 -or $threshold.branches -ne 80) { throw 'Portoes Angular divergentes.' }
    $package = Get-Content (Join-Path $root 'frontend/package.json') -Raw | ConvertFrom-Json
    if ($package.scripts.'test:coverage' -ne 'ng test --configuration coverage --watch=false') {
        throw 'Script frontend de cobertura divergente.'
    }
    if (!(Test-Path (Join-Path $root 'frontend/node_modules/.bin/ng.cmd'))) {
        throw 'Dependencias frontend ausentes. Execute npm.cmd ci em frontend antes da verificacao.'
    }
    foreach ($relative in $outputs.Values) { Assert-LocalPath (Join-Path $root $relative) }
    Assert-LocalPath $run
    $null = New-Item -ItemType Directory -Path $current -Force
    foreach ($name in $outputs.Keys) {
        $source = Join-Path $root $outputs[$name]
        if (Test-Path -LiteralPath $source) {
            $null = New-Item -ItemType Directory -Path (Join-Path $run 'previous') -Force
            Move-Item -LiteralPath $source -Destination (Join-Path $run "previous/$name")
        }
    }
    # Avoid user/global Maven settings injecting profiles, repositories or JVM test properties.
    $settings = Join-Path $run 'settings.xml'
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" />' | Set-Content $settings -Encoding utf8
    if ($env:GITHUB_OUTPUT) { "evidence=$current" | Add-Content $env:GITHUB_OUTPUT -Encoding utf8 }
    Push-Location $root
    try {
        $mavenVersion = (& $wrapper -B -s $settings -gs $settings --version | Out-String).Trim()
        if ($LASTEXITCODE -ne 0) {
            $result.exitCode = $LASTEXITCODE
            throw 'Falha ao verificar a versao do Maven Wrapper.'
        }
        Write-Host $mavenVersion
        if ($mavenVersion -notmatch 'Apache Maven 3\.9\.16\b' -or $mavenVersion -notmatch 'Java version: 17\.') {
            throw 'Maven Wrapper 3.9.16 executado com Java 17 obrigatorio; confira JAVA_HOME.'
        }
        $result.versions.maven = $mavenVersion
        $result.preflight = 'APROVADO'
        $result.backend = 'FALHA'
        Invoke-Checked $wrapper @('-B', '-s', $settings, '-gs', $settings,
            '-Dtemporal.test.postgresql.url=', '-Dtemporal.test.postgresql.enabled=false',
            '-Dbroker.test.postgresql.enabled=false', '-Dtemporal.poc.enabled=false',
            '-Djacoco.append=false', 'verify')
        foreach ($file in @('target/site/jacoco/index.html', 'target/site/jacoco/jacoco.xml')) {
            if (!(Test-Path $file)) { throw "Relatorio backend ausente: $file" }
        }
        $result.backend = 'APROVADO'
        $result.frontend = 'FALHA'
        $env:VITEST_MAX_WORKERS = '1'
        $env:NG_BUILD_MAX_WORKERS = '2'
        Push-Location (Join-Path $root 'frontend')
        try { Invoke-Checked 'npm.cmd' @('run', 'test:coverage') } finally { Pop-Location }
        foreach ($file in @('index.html', 'lcov.info', 'coverage-summary.json')) {
            if (!(Test-Path (Join-Path $root "frontend/coverage/frontend/$file"))) {
                throw "Relatorio frontend ausente: $file"
            }
        }
        $result.frontend = 'APROVADO'
        $result.exitCode = 0
    } finally { Pop-Location }
} catch {
    if ($result.preflight -ne 'APROVADO') { $result.preflight = 'FALHA' }
    $result.error = $_.Exception.Message
    Write-Host "FALHA: $($result.error)" -ForegroundColor Red
} finally {
    foreach ($name in $previousWorkers.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousWorkers[$name], 'Process')
    }
    # If preflight failed, no existing reports are eligible as current evidence.
    if ($result.preflight -eq 'APROVADO') {
        foreach ($name in $outputs.Keys) {
            $source = Join-Path $root $outputs[$name]
            if (Test-Path -LiteralPath $source) {
                Copy-Item -LiteralPath $source -Destination (Join-Path $current $name) -Recurse
            }
        }
    }
    foreach ($entry in @('surefire', 'jacoco/index.html', 'jacoco/jacoco.xml',
                         'frontend/frontend/index.html', 'frontend/frontend/lcov.info',
                         'frontend/frontend/coverage-summary.json')) {
        $state = if (Test-Path (Join-Path $current $entry)) { 'DISPONIVEL' } else { 'AUSENTE' }
        $result.reports += "$entry : $state"
    }
    Assert-LocalPath $current
    $null = New-Item -ItemType Directory -Path $current -Force
    $result.finishedUtc = [DateTime]::UtcNow.ToString('o')
    $result | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $current 'result.json') -Encoding utf8
    $summary = @("# Verificacao $runId", "Preflight: $($result.preflight)",
        "Backend: $($result.backend)", "Frontend: $($result.frontend)", "Exit: $($result.exitCode)")
    $summary += $result.reports
    if ($result.error) { $summary += "Erro: $($result.error)" }
    $summary | Set-Content (Join-Path $current 'summary.md') -Encoding utf8
    $summary | ForEach-Object { Write-Host $_ }
    Write-Host "Evidencias atuais: $current"
    if ($env:GITHUB_OUTPUT) { "evidence=$current" | Add-Content $env:GITHUB_OUTPUT -Encoding utf8 }
    if ($env:GITHUB_STEP_SUMMARY) { $summary | Add-Content $env:GITHUB_STEP_SUMMARY -Encoding utf8 }
}
exit $result.exitCode
