$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

foreach ($command in @('git', 'node', 'npm')) {
  if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
    throw "Missing required command: $command"
  }
}

Push-Location (Join-Path $root 'apps/web')
try {
  & npm ci
  if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
} finally {
  Pop-Location
}

Write-Host 'Setup complete. Run scripts/dev.ps1 to start the PWA.'
