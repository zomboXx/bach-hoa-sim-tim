$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

Push-Location (Join-Path $root 'apps/web')
try {
  & npm run dev
  if ($LASTEXITCODE -ne 0) { throw 'Web development server failed.' }
} finally {
  Pop-Location
}
