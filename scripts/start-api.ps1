param([string]$JavaHome = $env:JAVA_HOME, [string]$Maven = 'mvn')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $JavaHome) { throw 'Pass -JavaHome or set JAVA_HOME to a valid JDK 21–25 directory.' }
$javaExecutable = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) { throw 'Pass -JavaHome with a valid JDK 21–25 directory.' }
$env:JAVA_HOME = $JavaHome
& $Maven -B -ntp -f (Join-Path $projectRoot 'services/api/pom.xml') spring-boot:run
