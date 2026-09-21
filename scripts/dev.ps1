param(
  [switch]$SkipDatabase,
  [switch]$Demo,
  [string]$Maven = 'mvn'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

if (-not $SkipDatabase) {
  & docker compose -f (Join-Path $root 'infra/compose.yaml') up -d
  if ($LASTEXITCODE -ne 0) { throw 'Could not start PostgreSQL.' }
}

$api = $null
if (-not $Demo) {
  $api = Start-Job -Name 'simtim-api' -ScriptBlock {
    param($mavenCommand, $repositoryRoot)
    & $mavenCommand -B -ntp -f (Join-Path $repositoryRoot 'services/api/pom.xml') spring-boot:run
  } -ArgumentList $Maven, $root

  $ready = $false
  foreach ($attempt in 1..60) {
    try {
      Invoke-RestMethod 'http://127.0.0.1:8080/api/health' -TimeoutSec 1 | Out-Null
      $ready = $true
      break
    } catch {
      Start-Sleep -Seconds 1
    }
  }
  if (-not $ready) {
    Receive-Job $api
    throw 'API did not become ready within 60 seconds.'
  }
}

Push-Location (Join-Path $root 'apps/web')
try {
  if ($Demo) { $env:VITE_DATA_MODE = 'demo' }
  & npm run dev
  if ($LASTEXITCODE -ne 0) { throw 'Web development server failed.' }
} finally {
  Pop-Location
  if ($api) {
    Stop-Job $api -ErrorAction SilentlyContinue
    Remove-Job $api -Force -ErrorAction SilentlyContinue
  }
}
