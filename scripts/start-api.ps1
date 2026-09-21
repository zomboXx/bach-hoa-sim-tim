param([string]$JavaHome = $env:JAVA_HOME, [string]$Maven = 'mvn')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
if (-not (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) { throw 'Pass -JavaHome with a valid JDK 21–25 directory.' }
& $Maven -B -ntp -f (Join-Path $projectRoot 'backend/pom.xml') spring-boot:run
