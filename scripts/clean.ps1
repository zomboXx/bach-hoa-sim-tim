$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Split-Path $PSScriptRoot -Parent)).Path
$targets = @(
  'apps/web/dist',
  'apps/web/test-results',
  'apps/web/playwright-report',
  'apps/web/public/training',
  'services/api/target',
  'apps/training-godot/.godot',
  'rendered'
)

foreach ($relative in $targets) {
  $candidate = Join-Path $root $relative
  $full = [IO.Path]::GetFullPath($candidate)
  if (-not $full.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to clean path outside repository: $full"
  }
  if (Test-Path -LiteralPath $full) {
    Remove-Item -LiteralPath $full -Recurse -Force
    Write-Host "Removed $relative"
  }
}
