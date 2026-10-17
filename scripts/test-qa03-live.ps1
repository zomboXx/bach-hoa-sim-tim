$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$apiDir = Join-Path $root 'services/api'
$webDir = Join-Path $root 'apps/web'
$containerName = "simtim-qa03-live-$PID"
$dbPassword = [Guid]::NewGuid().ToString('N')
$demoPassword = "Qa03-$([Guid]::NewGuid().ToString('N'))"
$apiProcess = $null
$webProcess = $null
$apiOut = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-api.out.log"
$apiErr = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-api.err.log"
$webOut = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-web.out.log"
$webErr = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-web.err.log"
$webBuildDir = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-web-dist"
$testOutputDir = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-test-results"

if ($IsWindows -and -not $env:DOCKER_HOST -and (Test-Path '\\.\pipe\dockerDesktopLinuxEngine')) {
  $env:DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'
}

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
  if ($LASTEXITCODE -ne 0) { throw 'Could not start the QA-03 PostgreSQL container.' }

  $databaseReady = $false
  for ($attempt = 0; $attempt -lt 30; $attempt++) {
    & docker exec $containerName pg_isready -U simtim -d simtim *> $null
    if ($LASTEXITCODE -eq 0) { $databaseReady = $true; break }
    Start-Sleep -Seconds 1
  }
  if (-not $databaseReady) { throw 'QA-03 PostgreSQL did not become ready.' }

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
    throw 'Spring API did not become ready for QA-03.'
  }

  Push-Location $webDir
  try {
    $env:API_PROXY_TARGET = "http://127.0.0.1:$apiPort"
    $env:SIMTIM_LIVE_PASSWORD = $demoPassword
    $env:QA03_BROWSER_CHANNEL = 'chrome'
    $env:VITE_USE_API = 'true'
    $env:VITE_INVENTORY_TIMEOUT_MS = '3000'
    $env:QA03_OUTPUT_DIR = $testOutputDir
    & npm run typecheck
    if ($LASTEXITCODE -ne 0) { throw 'QA-03 web typecheck failed.' }
    & npx vite build --outDir $webBuildDir
    if ($LASTEXITCODE -ne 0) { throw 'Could not build the QA-03 web client.' }
    $webProcess = Start-Process -FilePath $cmdExe `
      -ArgumentList @('/d', '/c', "npx vite preview --host 127.0.0.1 --outDir `"$webBuildDir`" --port 4177") `
      -WorkingDirectory $webDir `
      -WindowStyle Hidden `
      -Environment @{
        API_PROXY_TARGET = $env:API_PROXY_TARGET
        VITE_USE_API = $env:VITE_USE_API
        VITE_INVENTORY_TIMEOUT_MS = $env:VITE_INVENTORY_TIMEOUT_MS
      } `
      -RedirectStandardOutput $webOut `
      -RedirectStandardError $webErr `
      -PassThru
    $webReady = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
      try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:4177/' -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $webReady = $true; break }
      } catch { }
      if ($webProcess.HasExited) { break }
      Start-Sleep -Seconds 1
    }
    if (-not $webReady) {
      Get-Content -LiteralPath $webOut -Tail 40 -ErrorAction SilentlyContinue
      Get-Content -LiteralPath $webErr -Tail 40 -ErrorAction SilentlyContinue
      throw 'QA-03 web preview did not become ready.'
    }
    & npx playwright test --config=playwright.qa03.config.ts --reporter=line
    if ($LASTEXITCODE -ne 0) { throw 'QA-03 live browser test failed.' }
  } finally {
    if ($webProcess -and -not $webProcess.HasExited) {
      & taskkill /PID $webProcess.Id /T /F *> $null
    }
    Remove-Item Env:API_PROXY_TARGET -ErrorAction SilentlyContinue
    Remove-Item Env:SIMTIM_LIVE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:QA03_BROWSER_CHANNEL -ErrorAction SilentlyContinue
    Remove-Item Env:VITE_USE_API -ErrorAction SilentlyContinue
    Remove-Item Env:VITE_INVENTORY_TIMEOUT_MS -ErrorAction SilentlyContinue
    Remove-Item Env:QA03_OUTPUT_DIR -ErrorAction SilentlyContinue
    Pop-Location
  }
} finally {
  if ($apiProcess -and -not $apiProcess.HasExited) {
    & taskkill /PID $apiProcess.Id /T /F *> $null
  }
  & docker stop $containerName *> $null
  Remove-Item -LiteralPath $apiOut, $apiErr, $webOut, $webErr -Force -ErrorAction SilentlyContinue
  $resolvedTemp = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
  $resolvedBuild = [System.IO.Path]::GetFullPath($webBuildDir)
  if ($resolvedBuild.StartsWith($resolvedTemp) -and (Test-Path -LiteralPath $resolvedBuild)) {
    Remove-Item -LiteralPath $resolvedBuild -Recurse -Force
  }
  $resolvedResults = [System.IO.Path]::GetFullPath($testOutputDir)
  if ($resolvedResults.StartsWith($resolvedTemp) -and (Test-Path -LiteralPath $resolvedResults)) {
    Remove-Item -LiteralPath $resolvedResults -Recurse -Force
  }
}
