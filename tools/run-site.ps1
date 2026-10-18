[CmdletBinding()]
param([switch]$CompileOnly, [switch]$Offline)

$ErrorActionPreference = 'Stop'
$moduleDirectory = Split-Path -Parent $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Install JDK 21 or newer and set JAVA_HOME, then reopen VS Code.'
}

Push-Location -LiteralPath $moduleDirectory
try {
    $mavenArguments = @()
    if ($Offline) { $mavenArguments += '-o' }
    if ($CompileOnly) {
        $mavenArguments += 'compile'
    } else {
        $mavenArguments += @(
            '-Dspring-boot.run.arguments=--server.port=8080',
            '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false',
            'spring-boot:run'
        )
        Write-Host 'Starting PawCare at http://localhost:8080. Start MySQL first. Press Ctrl+C to stop.'
    }
    & (Join-Path $moduleDirectory 'mvnw.cmd') @mavenArguments
    $runExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $runExitCode
