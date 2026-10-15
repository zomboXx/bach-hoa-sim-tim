param(
  [string]$OutFile = ""
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$infraDir = Join-Path $root 'infra'
$envFile = Join-Path $infraDir '.env'

$db = "simtim"
$user = "simtim"

if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*POSTGRES_DB\s*=\s*(.+)$') { $db = $matches[1].Trim() }
    if ($_ -match '^\s*POSTGRES_USER\s*=\s*(.+)$') { $user = $matches[1].Trim() }
  }
}

$composeFile = Join-Path $infraDir 'compose.demo.yml'
if (-not (Test-Path $composeFile)) {
  throw "Compose file not found at $composeFile"
}

if ([string]::IsNullOrWhiteSpace($OutFile)) {
  $backupDir = Join-Path $infraDir 'backups'
  if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir | Out-Null
  }
  $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
  $OutFile = Join-Path $backupDir "backup_${db}_${timestamp}.sql"
} else {
  $parent = Split-Path $OutFile -Parent
  if ($parent -and -not (Test-Path $parent)) {
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
  }
}

Write-Host "Creating backup of database '$db' from Compose postgres service..."

$composeArgs = @('compose')
if (Test-Path $envFile) {
  $composeArgs += @('--env-file', $envFile)
}
$composeArgs += @('-f', $composeFile, 'exec', '-T', 'postgres', 'pg_dump', '-U', $user, '-d', $db, '--clean', '--if-exists')

& docker @composeArgs > $OutFile

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $OutFile) -or (Get-Item $OutFile).Length -eq 0) {
  throw "Database backup failed."
}

$size = (Get-Item $OutFile).Length
Write-Host "Backup completed successfully:`n  File: $OutFile`n  Size: $size bytes"
