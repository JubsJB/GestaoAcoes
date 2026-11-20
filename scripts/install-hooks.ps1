#requires -Version 7.2
[CmdletBinding()]
param([switch]$Uninstall)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = Split-Path $PSScriptRoot -Parent

function Invoke-Git([string[]]$Arguments, [switch]$AllowMissing) {
    $output = @(& $script:git -C $root @Arguments)
    $code = $LASTEXITCODE
    if ($code -ne 0 -and !($AllowMissing -and $code -eq 1)) { throw "Git falhou (codigo $code)." }
    return $output
}

try {
    $script:git = (Get-Command git -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $top = (Invoke-Git @('rev-parse', '--show-toplevel')) -join ''
    if ([IO.Path]::GetFullPath($top) -ne [IO.Path]::GetFullPath($root)) { throw 'Execute a copia do instalador da raiz deste repositorio.' }
    $local = @(Invoke-Git @('config', '--local', '--get-all', 'core.hooksPath') -AllowMissing)
    $configured = @(Invoke-Git @('config', '--show-origin', '--show-scope', '--get-all', 'core.hooksPath') -AllowMissing)
    if ($Uninstall) {
        if ($local.Count -ne 1 -or $local[0] -ne '.githooks') {
            throw 'Reversao recusada: core.hooksPath local nao corresponde exclusivamente a .githooks.'
        }
        Invoke-Git @('config', '--local', '--unset-all', 'core.hooksPath') | Out-Null
        Write-Host 'core.hooksPath local removido. Arquivos e configuracoes globais preservados.'
        exit 0
    }
    if ($configured.Count) {
        $configured | ForEach-Object { Write-Host "Configuracao existente: $_" }
        if ($configured.Count -eq 1 -and $local.Count -eq 1 -and $local[0] -eq '.githooks') {
            Write-Host 'Ja instalado neste repositorio; nenhuma configuracao alterada.'
            exit 0
        }
        throw 'Instalacao recusada: core.hooksPath ja configurado. Nenhuma substituicao automatica.'
    }
    $defaultHooks = (Invoke-Git @('rev-parse', '--git-path', 'hooks')) -join ''
    if (![IO.Path]::IsPathRooted($defaultHooks)) { $defaultHooks = Join-Path $root $defaultHooks }
    $existing = @()
    if (Test-Path -LiteralPath $defaultHooks) {
        $existing = @(Get-ChildItem -LiteralPath $defaultHooks -Force -ErrorAction Stop |
            Where-Object { $_.Name -notlike '*.sample' })
    }
    if ($existing.Count) {
        $existing | ForEach-Object { Write-Host "Hook/arquivo existente: $($_.Name)" }
        throw 'Instalacao recusada: diretorio de hooks existente requer revisao manual.'
    }
    foreach ($relative in @('.githooks/pre-commit', 'scripts/pre-commit.ps1')) {
        if (!(Test-Path -LiteralPath (Join-Path $root $relative) -PathType Leaf)) { throw "Arquivo ausente: $relative" }
    }
    $hook = [IO.File]::ReadAllBytes((Join-Path $root '.githooks/pre-commit'))
    if ($hook -contains 13 -or $hook.Length -lt 2 -or $hook[0] -ne 35 -or $hook[1] -ne 33) {
        throw 'Hook deve iniciar com shebang, sem BOM, e usar LF.'
    }
    $null = Get-Command pwsh -CommandType Application -ErrorAction Stop
    Invoke-Git @('config', '--local', 'core.hooksPath', '.githooks') | Out-Null
    Write-Host 'Instalado apenas neste repositorio: core.hooksPath=.githooks'
    Write-Host 'Reverter: pwsh -NoProfile -File ./scripts/install-hooks.ps1 -Uninstall'
} catch {
    Write-Host "Instalacao/reversao reprovada: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
