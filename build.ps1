$ErrorActionPreference = "Stop"

$Root = $PSScriptRoot
$Classes = Join-Path $Root "build\classes"
$Lib = Join-Path $Root "lib"
$Jar = Join-Path $Lib "ladle.jar"
$Manifest = Join-Path $Root "manifest\MANIFEST.MF"

function Get-JavaHome {
    if ($env:JAVA_HOME) {
        $Candidate = $env:JAVA_HOME
        if (Test-Path -LiteralPath (Join-Path $Candidate "bin\jar.exe")) {
            return (Resolve-Path $Candidate).Path
        }
    }

    $PreviousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $Settings = (java -XshowSettings:properties -version 2>&1 | Out-String)
    } finally {
        $ErrorActionPreference = $PreviousPreference
    }
    if ($Settings -match 'java\.home = (.+)') {
        return $Matches[1].Trim()
    }

    throw "Cannot find a JDK. Install Java or set JAVA_HOME."
}

$JavaHome = Get-JavaHome
$Javac = Join-Path $JavaHome "bin\javac.exe"
$JarTool = Join-Path $JavaHome "bin\jar.exe"

New-Item -ItemType Directory -Force -Path $Classes, $Lib | Out-Null

$Sources = Get-ChildItem -Path (Join-Path $Root "src") -Filter *.java -Recurse | ForEach-Object { $_.FullName }
& $Javac -d $Classes @Sources
& $JarTool cfm $Jar $Manifest -C $Classes .

Write-Host "Built $Jar"
