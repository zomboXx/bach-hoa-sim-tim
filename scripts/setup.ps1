param([switch]$SkipDatabase)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

foreach ($command in @('git', 'node', 'npm', 'java', 'mvn')) {
  if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
    throw "Missing required command: $command"
  }
}

if (-not $SkipDatabase) {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Missing required command: docker' }
  & docker compose -f (Join-Path $root 'infra/compose.yaml') up -d
  if ($LASTEXITCODE -ne 0) { throw 'Could not start PostgreSQL.' }
}

Push-Location (Join-Path $root 'apps/web')
try {
  & npm ci
  if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
} finally {
  Pop-Location
}

Write-Host 'Setup complete. Use scripts/dev.ps1 to start the API and web client.'
