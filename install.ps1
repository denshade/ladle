param(
    [string]$Prefix = "$env:LOCALAPPDATA\Programs"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host @"
Usage: .\install.ps1 [-Prefix DIR]

Install Ladle so the ladle command is available on your PATH.

Default install location: $env:LOCALAPPDATA\Programs\ladle

After installation, open a new terminal and run:

  ladle build build.ini
"@
}

if ($args -contains "-?" -or $args -contains "--help" -or $args -contains "-h") {
    Show-Usage
    exit 0
}

$Root = $PSScriptRoot
$InstallDir = Join-Path $Prefix "ladle"
$BinDir = Join-Path $InstallDir "bin"
$LibDir = Join-Path $InstallDir "lib"
$Jar = Join-Path $LibDir "ladle.jar"

if (-not (Test-Path -LiteralPath (Join-Path $Root "lib\ladle.jar"))) {
    Write-Host "Building ladle.jar..."
    & (Join-Path $Root "build.ps1")
}

New-Item -ItemType Directory -Force -Path $BinDir, $LibDir | Out-Null
Copy-Item -Force (Join-Path $Root "bin\ladle.cmd") (Join-Path $BinDir "ladle.cmd")
Copy-Item -Force (Join-Path $Root "bin\ladle.bat") (Join-Path $BinDir "ladle.bat")
Copy-Item -Force (Join-Path $Root "bin\ladle.ps1") (Join-Path $BinDir "ladle.ps1")
Copy-Item -Force (Join-Path $Root "lib\ladle.jar") $Jar

$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
$PathEntries = @()
if ($UserPath) {
    $PathEntries = $UserPath -split ';' | Where-Object { $_ -ne "" }
}
if ($PathEntries -notcontains $BinDir) {
    $PathEntries = @($BinDir) + $PathEntries
    [Environment]::SetEnvironmentVariable("Path", ($PathEntries -join ';'), "User")
}
[Environment]::SetEnvironmentVariable("LADLE_HOME", $InstallDir, "User")

$Env:Path = "$BinDir;$env:Path"
$env:LADLE_HOME = $InstallDir

Write-Host ""
Write-Host "Ladle installed to $InstallDir"
Write-Host ""
Write-Host "Open a new terminal, then use ladle from any directory:"
Write-Host ""
Write-Host "  ladle build build.ini"
Write-Host "  ladle dependency build.ini"
