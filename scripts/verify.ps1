param(
  [switch]$SkipApi,
  [switch]$SkipGodot,
  [switch]$SkipE2E,
  [string]$Godot = $env:GODOT_PATH,
  [string]$Maven = 'mvn'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

& git -C $root diff --check
if ($LASTEXITCODE -ne 0) { throw 'git diff --check failed.' }
& git -C $root diff --cached --check
if ($LASTEXITCODE -ne 0) { throw 'git diff --cached --check failed.' }
& (Join-Path $PSScriptRoot 'check-markdown-links.ps1')

if (-not $SkipApi) {
  & docker compose -f (Join-Path $root 'infra/compose.yaml') up -d postgres
  if ($LASTEXITCODE -ne 0) { throw 'Could not start PostgreSQL.' }
  $databaseExists = & docker compose -f (Join-Path $root 'infra/compose.yaml') exec -T postgres psql -U simtim -tAc "SELECT 1 FROM pg_database WHERE datname='simtim_test'"
  if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the isolated test database.' }
  if (($databaseExists -join '').Trim() -ne '1') {
    & docker compose -f (Join-Path $root 'infra/compose.yaml') exec -T postgres createdb -U simtim simtim_test
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the isolated test database.' }
  }
  & $Maven -B -ntp -f (Join-Path $root 'services/api/pom.xml') verify
  if ($LASTEXITCODE -ne 0) { throw 'API verification failed.' }
}

if (-not $SkipGodot) {
  if (-not $Godot) {
    $godotCommand = Get-Command godot -ErrorAction SilentlyContinue
    if (-not $godotCommand) { throw 'Godot was not found. Set GODOT_PATH or use -SkipGodot and report the omission.' }
    $Godot = $godotCommand.Source
  }
  & $Godot --headless --path (Join-Path $root 'apps/training-godot') --editor --import
  if ($LASTEXITCODE -ne 0) { throw 'Godot import failed.' }
  & $Godot --headless --path (Join-Path $root 'apps/training-godot') --script res://tests/tour_test.gd
  if ($LASTEXITCODE -ne 0) { throw 'Godot tour test failed.' }
  & $Godot --headless --path (Join-Path $root 'apps/training-godot') --export-release Web
  if ($LASTEXITCODE -ne 0) { throw 'Godot Web export failed.' }
}

Push-Location (Join-Path $root 'apps/web')
try {
  & npm ci
  if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
  & npm run lint
  if ($LASTEXITCODE -ne 0) { throw 'Web lint failed.' }
  & npm run format:check
  if ($LASTEXITCODE -ne 0) { throw 'Web format check failed.' }
  & npm run build
  if ($LASTEXITCODE -ne 0) { throw 'Web build failed.' }
  if (-not $SkipE2E) {
    & npm run test:e2e
    if ($LASTEXITCODE -ne 0) { throw 'Web end-to-end tests failed.' }
  }
} finally {
  Pop-Location
}
