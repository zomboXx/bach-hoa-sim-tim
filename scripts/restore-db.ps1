param(
  [Parameter(Mandatory=$true)]
  [string]$BackupFile
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$infraDir = Join-Path $root 'infra'
$envFile = Join-Path $infraDir '.env'

if (-not (Test-Path $BackupFile)) {
  throw "Backup file not found at '$BackupFile'"
}

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

Write-Host "Restoring database '$db' from '$BackupFile' into Compose postgres service..."

$composeArgs = @('compose')
if (Test-Path $envFile) {
  $composeArgs += @('--env-file', $envFile)
}
$composeArgs += @('-f', $composeFile, 'exec', '-T', 'postgres', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $user, '-d', $db)

Get-Content $BackupFile -Raw | & docker @composeArgs

if ($LASTEXITCODE -ne 0) {
  throw "Database restore failed."
}

Write-Host "Restore completed successfully into '$db'."
