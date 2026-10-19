param(
  [Parameter(Mandatory=$true)]
  [string]$BackupFile,
  [string]$ProjectName = "",
  [string]$EnvFile = ""
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$infraDir = Join-Path $root 'infra'
if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path $infraDir '.env' }
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "Compose env file not found at '$EnvFile'" }

if (-not (Test-Path -LiteralPath $BackupFile)) {
  throw "Backup file not found at '$BackupFile'"
}

$db = "simtim"
$user = "simtim"

if (Test-Path -LiteralPath $EnvFile) {
  Get-Content -LiteralPath $EnvFile | ForEach-Object {
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
if (-not [string]::IsNullOrWhiteSpace($ProjectName)) { $composeArgs += @('-p', $ProjectName) }
$composeArgs += @('--env-file', $EnvFile)
$composeArgs += @('-f', $composeFile, 'exec', '-T', 'postgres', 'psql', '-v', 'ON_ERROR_STOP=1', '--single-transaction', '-U', $user, '-d', $db)

Get-Content -LiteralPath $BackupFile -Raw | & docker @composeArgs

if ($LASTEXITCODE -ne 0) {
  throw "Database restore failed."
}

Write-Host "Restore completed successfully into '$db'."
