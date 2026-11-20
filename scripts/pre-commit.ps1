#requires -Version 7.2
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

function Read-Git([string[]]$Arguments) {
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $script:git
    $start.WorkingDirectory = $root
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.ArgumentList.Add('--no-optional-locks')
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $start
    $bytes = [IO.MemoryStream]::new()
    try {
        $null = $process.Start()
        $copy = $process.StandardOutput.BaseStream.CopyToAsync($bytes)
        $errorText = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $null = $copy.GetAwaiter().GetResult()
        return @{ Code = $process.ExitCode; Bytes = $bytes.ToArray(); Error = $errorText.GetAwaiter().GetResult() }
    } finally { $bytes.Dispose(); $process.Dispose() }
}

try {
    $script:git = (Get-Command git -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $check = Read-Git @('diff', '--cached', '--check')
    if ($check.Code -ne 0) {
        Write-Host ([Text.Encoding]::UTF8.GetString($check.Bytes))
        throw "Whitespace/conflito staged reprovado. $($check.Error)"
    }
    $listing = Read-Git @('diff', '--cached', '--name-only', '--no-renames', '--diff-filter=ACMT', '-z')
    if ($listing.Code -ne 0) { throw "Nao foi possivel listar o indice: $($listing.Error)" }
    $paths = [Text.Encoding]::UTF8.GetString($listing.Bytes).Split([char]0, [StringSplitOptions]::RemoveEmptyEntries)
    $count = 0
    $failures = 0
    foreach ($path in $paths) {
        $extension = [IO.Path]::GetExtension($path).ToLowerInvariant()
        if ($extension -notin @('.json', '.jsonc', '.xml', '.ps1')) { continue }
        try {
            $blob = Read-Git @('cat-file', 'blob', ":$path")
            if ($blob.Code -ne 0) { throw "Nao foi possivel ler o blob staged: $($blob.Error)" }
            if ($extension -eq '.xml') {
                $settings = [Xml.XmlReaderSettings]::new()
                $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
                $settings.XmlResolver = $null
                $stream = [IO.MemoryStream]::new([byte[]]$blob.Bytes)
                $reader = $null
                try {
                    $reader = [Xml.XmlReader]::Create($stream, $settings)
                    while ($reader.Read()) { }
                } finally { if ($reader) { $reader.Dispose() }; $stream.Dispose() }
            } else {
                $stream = [IO.MemoryStream]::new([byte[]]$blob.Bytes)
                $reader = [IO.StreamReader]::new($stream, [Text.UTF8Encoding]::new($false, $true), $true)
                try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
                if ($extension -in @('.json', '.jsonc')) {
                    $options = [Text.Json.JsonDocumentOptions]::new()
                    if ($extension -eq '.jsonc' -or [IO.Path]::GetFileName($path) -match '^tsconfig(?:\..+)?\.json$') {
                        $options.CommentHandling = [Text.Json.JsonCommentHandling]::Skip
                        $options.AllowTrailingCommas = $true
                    }
                    $document = [Text.Json.JsonDocument]::Parse($content, $options)
                    $document.Dispose()
                } else {
                    $tokens = $null
                    $syntaxErrors = $null
                    $null = [Management.Automation.Language.Parser]::ParseInput($content, $path, [ref]$tokens, [ref]$syntaxErrors)
                    if ($syntaxErrors.Count) {
                        throw (($syntaxErrors | ForEach-Object { "linha $($_.Extent.StartLineNumber): $($_.Message)" }) -join '; ')
                    }
                }
            }
            $count++
            Write-Host "OK staged: $path"
        } catch {
            $failures++
            Write-Host "ERRO staged: $path : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    if ($failures) { throw "$failures arquivo(s) staged com erro de sintaxe." }
    Write-Host "Pre-commit aprovado: whitespace e $count arquivo(s) de sintaxe. Testes/cobertura nao executados."
    exit 0
} catch {
    Write-Host "Pre-commit reprovado: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
