param(
    [Parameter(Position = 0)]
    [string]$IniFile = "build.ini"
)

$ErrorActionPreference = "Stop"

if ($args -contains "-?" -or $args -contains "--help" -or $args -contains "-h") {
    Write-Host @"
Usage: .\download.ps1 [<ini-file>]

Download and install JAR dependencies into dependencies/ for the current project.
Reads [dependencies] and [testdependencies] from build.ini (default: build.ini).

Uses a local ladle checkout when present, otherwise LADLE_HOME or ladle on PATH.
"@
    exit 0
}

if (-not (Test-Path -LiteralPath $IniFile)) {
    Write-Error "Cannot read $IniFile"
    exit 2
}

function Invoke-LadleDependency {
    param([Parameter(Mandatory = $true)][string[]]$Command)
    & $Command[0] $Command[1..($Command.Length - 1)] dependency $IniFile
    exit $LASTEXITCODE
}

$ScriptDir = $PSScriptRoot

$LocalJar = Join-Path (Join-Path $ScriptDir "..") "lib\ladle.jar"
if (Test-Path -LiteralPath $LocalJar) {
    if ($env:JAVA_HOME) {
        $Java = Join-Path $env:JAVA_HOME "bin\java.exe"
    } else {
        $Java = "java"
    }
    Invoke-LadleDependency @($Java, "-jar", $LocalJar)
}

$LocalLadle = Join-Path $ScriptDir "ladle.cmd"
if (Test-Path -LiteralPath $LocalLadle) {
    Invoke-LadleDependency @("cmd.exe", "/c", $LocalLadle)
}

if ($env:LADLE_HOME) {
    $InstalledLadle = Join-Path $env:LADLE_HOME "bin\ladle.cmd"
    if (Test-Path -LiteralPath $InstalledLadle) {
        Invoke-LadleDependency @("cmd.exe", "/c", $InstalledLadle)
    }
}

$LadleCmd = Get-Command ladle -ErrorAction SilentlyContinue
if ($LadleCmd) {
    Invoke-LadleDependency @($LadleCmd.Source)
}

Write-Error "Cannot find ladle. Install it or set LADLE_HOME."
exit 1
