param([switch]$SkipInstall, [switch]$SkipE2E)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

& git -C $root diff --check
if ($LASTEXITCODE -ne 0) { throw 'git diff --check failed.' }
& git -C $root diff --cached --check
if ($LASTEXITCODE -ne 0) { throw 'git diff --cached --check failed.' }
& (Join-Path $PSScriptRoot 'check-markdown-links.ps1')

Push-Location (Join-Path $root 'apps/web')
try {
  if (-not $SkipInstall) {
    & npm ci
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
  }
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
