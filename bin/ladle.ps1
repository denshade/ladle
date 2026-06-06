$ErrorActionPreference = "Stop"

if ($env:LADLE_HOME) {
    $AppHome = $env:LADLE_HOME
} else {
    $AppHome = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

$JarPath = Join-Path $AppHome "lib\ladle.jar"
if (-not (Test-Path -LiteralPath $JarPath)) {
    Write-Error "Cannot find ladle.jar at $JarPath. Set LADLE_HOME or run install.ps1 to install Ladle."
    exit 1
}

if ($env:JAVA_HOME) {
    $Java = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $Java = "java"
}

& $Java -jar $JarPath @args
exit $LASTEXITCODE
