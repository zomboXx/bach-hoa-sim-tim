$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path $PSScriptRoot -Parent)).Path
$targets = @(
  'apps/web/dist',
  'apps/web/test-results',
  'apps/web/playwright-report'
)

foreach ($relative in $targets) {
  $full = [IO.Path]::GetFullPath((Join-Path $root $relative))
  if (-not $full.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to clean path outside repository: $full"
  }
  if (Test-Path -LiteralPath $full) {
    Remove-Item -LiteralPath $full -Recurse -Force
    Write-Host "Removed $relative"
  }
}
