param([string]$Godot = $env:GODOT_PATH)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $Godot) { $Godot = (Get-Command godot -ErrorAction Stop).Source }
New-Item -ItemType Directory -Force (Join-Path $projectRoot 'apps/web/public/training') | Out-Null
& $Godot --headless --path (Join-Path $projectRoot 'apps/training-godot') --editor --import
if ($LASTEXITCODE -ne 0) { throw 'Godot import failed.' }
& $Godot --headless --path (Join-Path $projectRoot 'apps/training-godot') --export-release Web
if ($LASTEXITCODE -ne 0) { throw 'Godot export failed. Install the matching 4.6.3 Web export templates.' }
Write-Host 'Chapter 0 exported into apps/web/public/training. Run npm run build in apps/web next.'
