# Runs bin/download.ps1 from the project root (Gradle-style project-local download).
& (Join-Path $PSScriptRoot "bin\download.ps1") @args
exit $LASTEXITCODE
