$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$apiDir = Join-Path $root 'services/api'
$webDir = Join-Path $root 'apps/web'
$containerName = "simtim-fe02-live-$PID"
$dbPassword = [Guid]::NewGuid().ToString('N')
$demoPassword = "Live-$([Guid]::NewGuid().ToString('N'))"
$apiProcess = $null
$apiOut = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-api.out.log"
$apiErr = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-api.err.log"

function Get-FreeTcpPort {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
  $listener.Start()
  try { return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port }
  finally { $listener.Stop() }
}

$dbPort = Get-FreeTcpPort
$apiPort = Get-FreeTcpPort
$cmdExe = (Get-Command cmd.exe -ErrorAction Stop).Source

try {
  & docker run --detach --rm --name $containerName `
    --env POSTGRES_DB=simtim `
    --env POSTGRES_USER=simtim `
    --env "POSTGRES_PASSWORD=$dbPassword" `
    --publish "127.0.0.1:${dbPort}:5432" `
    postgres:17-alpine | Out-Null
  if ($LASTEXITCODE -ne 0) { throw 'Could not start the disposable PostgreSQL container.' }

  $databaseReady = $false
  for ($attempt = 0; $attempt -lt 30; $attempt++) {
    & docker exec $containerName pg_isready -U simtim -d simtim *> $null
    if ($LASTEXITCODE -eq 0) { $databaseReady = $true; break }
    Start-Sleep -Seconds 1
  }
  if (-not $databaseReady) { throw 'Disposable PostgreSQL did not become ready.' }

  $apiEnvironment = @{
    DB_URL = "jdbc:postgresql://127.0.0.1:$dbPort/simtim"
    DB_USER = 'simtim'
    DB_PASSWORD = $dbPassword
    SPRING_PROFILES_ACTIVE = 'demo'
    SIMTIM_DEMO_PASSWORD = $demoPassword
    SERVER_PORT = "$apiPort"
  }
  $apiProcess = Start-Process -FilePath $cmdExe `
    -ArgumentList @('/d', '/c', 'mvnw.cmd spring-boot:run') `
    -WorkingDirectory $apiDir `
    -WindowStyle Hidden `
    -Environment $apiEnvironment `
    -RedirectStandardOutput $apiOut `
    -RedirectStandardError $apiErr `
    -PassThru

  $apiReady = $false
  for ($attempt = 0; $attempt -lt 120; $attempt++) {
    try {
      $health = Invoke-RestMethod -Uri "http://127.0.0.1:$apiPort/actuator/health" -TimeoutSec 2
      if ($health.status -eq 'UP') { $apiReady = $true; break }
    } catch { }
    if ($apiProcess.HasExited) { break }
    Start-Sleep -Seconds 1
  }
  if (-not $apiReady) {
    Get-Content -LiteralPath $apiOut -Tail 80 -ErrorAction SilentlyContinue
    Get-Content -LiteralPath $apiErr -Tail 80 -ErrorAction SilentlyContinue
    throw 'Spring API did not become ready.'
  }

  Push-Location $webDir
  try {
    $env:API_PROXY_TARGET = "http://127.0.0.1:$apiPort"
    $env:SIMTIM_LIVE_PASSWORD = $demoPassword
    & npm run test:e2e:live
    if ($LASTEXITCODE -ne 0) { throw 'FE-02 live backend test failed.' }
  } finally {
    Remove-Item Env:API_PROXY_TARGET -ErrorAction SilentlyContinue
    Remove-Item Env:SIMTIM_LIVE_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
  }
} finally {
  if ($apiProcess -and -not $apiProcess.HasExited) {
    & taskkill /PID $apiProcess.Id /T /F *> $null
  }
  & docker stop $containerName *> $null
  Remove-Item -LiteralPath $apiOut, $apiErr -Force -ErrorAction SilentlyContinue
}
