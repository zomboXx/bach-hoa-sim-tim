param(
  [ValidateSet('Fe02', 'Qa02Gap05', 'Qa02Gap06')]
  [string]$Suite = 'Fe02'
)

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
$liveArtifacts = Join-Path ([System.IO.Path]::GetTempPath()) "$containerName-artifacts"
$playwrightOutput = Join-Path $liveArtifacts 'playwright'
$webBuildOutput = Join-Path $liveArtifacts 'dist'
$webTestScript = switch ($Suite) {
  'Qa02Gap05' { 'test:e2e:live:qa02-gap05' }
  'Qa02Gap06' { 'test:e2e:live:qa02-gap06' }
  default { 'test:e2e:live' }
}

function Get-FreeTcpPort {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
  $listener.Start()
  try { return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port }
  finally { $listener.Stop() }
}

function Get-QaDatabaseSummary {
  param([string]$ContainerName)

  $summarySql = Get-Content -LiteralPath (Join-Path $root 'scripts/qa02-gap06-db-summary.sql') -Raw
  $output = @(& docker exec $ContainerName psql `
      --set ON_ERROR_STOP=1 --tuples-only --no-align --field-separator '|' `
      --username simtim --dbname simtim --command $summarySql)
  if ($LASTEXITCODE -ne 0) { throw 'Could not query the QA-02 GAP-06 database oracle.' }
  $line = @($output | Where-Object { $_ -match '^\d+\|\d+\|\d+\|\d+\|\d+\|\d+$' })[-1]
  if (-not $line) { throw 'QA-02 GAP-06 database oracle returned no summary row.' }
  $values = $line.Split('|')
  return [pscustomobject]@{
    Receipts = [int]$values[0]
    Invoices = [int]$values[1]
    Payments = [int]$values[2]
    SaleMovements = [int]$values[3]
    BalanceMismatches = [int]$values[4]
    Revenue = [long]$values[5]
  }
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

  $qaBaseline = $null
  if ($Suite -in @('Qa02Gap05', 'Qa02Gap06')) {
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
      $qaBaseline = Get-QaDatabaseSummary -ContainerName $containerName
      if ($qaBaseline.Receipts -ge 1) { break }
      Start-Sleep -Seconds 1
    }
    if ($qaBaseline.Receipts -lt 1) { throw 'Demo inventory seed did not become ready.' }
  }

  Push-Location $webDir
  try {
    $env:API_PROXY_TARGET = "http://127.0.0.1:$apiPort"
    $env:SIMTIM_LIVE_PASSWORD = $demoPassword
    $env:PLAYWRIGHT_OUTPUT_DIR = $playwrightOutput
    $env:SIMTIM_BUILD_OUT_DIR = $webBuildOutput
    & npm run $webTestScript
    if ($LASTEXITCODE -ne 0) { throw "$Suite live backend test failed." }
    if ($Suite -in @('Qa02Gap05', 'Qa02Gap06')) {
      $qaAfter = Get-QaDatabaseSummary -ContainerName $containerName
      $receiptDelta = $qaAfter.Receipts - $qaBaseline.Receipts
      $invoiceDelta = $qaAfter.Invoices - $qaBaseline.Invoices
      $paymentDelta = $qaAfter.Payments - $qaBaseline.Payments
      $saleMovementDelta = $qaAfter.SaleMovements - $qaBaseline.SaleMovements
      $revenueDelta = $qaAfter.Revenue - $qaBaseline.Revenue
      $expectedReceiptDelta = if ($Suite -eq 'Qa02Gap06') { 1 } else { 0 }
      $expectedSaleDelta = if ($Suite -eq 'Qa02Gap06') { 1 } else { 2 }
      if ($receiptDelta -ne $expectedReceiptDelta) {
        throw "Expected receipt delta $expectedReceiptDelta, found $receiptDelta."
      }
      if ($invoiceDelta -ne $expectedSaleDelta) {
        throw "Expected invoice delta $expectedSaleDelta, found $invoiceDelta."
      }
      if ($paymentDelta -ne $expectedSaleDelta) {
        throw "Expected payment delta $expectedSaleDelta, found $paymentDelta."
      }
      if ($saleMovementDelta -ne $expectedSaleDelta) {
        throw "Expected SALE movement delta $expectedSaleDelta, found $saleMovementDelta."
      }
      if ($revenueDelta -le 0) { throw "Expected positive revenue delta, found $revenueDelta." }
      if ($qaAfter.BalanceMismatches -ne 0) {
        throw "Inventory balance differs from movement ledger for $($qaAfter.BalanceMismatches) batch(es)."
      }
      Write-Output "QA-02 $Suite DB oracle: receipt_delta=$receiptDelta; invoice_delta=$invoiceDelta; payment_delta=$paymentDelta; sale_movement_delta=$saleMovementDelta; revenue_delta=$revenueDelta; balance_mismatches=$($qaAfter.BalanceMismatches)"
    }
  } finally {
    Remove-Item Env:API_PROXY_TARGET -ErrorAction SilentlyContinue
    Remove-Item Env:SIMTIM_LIVE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PLAYWRIGHT_OUTPUT_DIR -ErrorAction SilentlyContinue
    Remove-Item Env:SIMTIM_BUILD_OUT_DIR -ErrorAction SilentlyContinue
    Pop-Location
  }
} finally {
  if ($apiProcess -and -not $apiProcess.HasExited) {
    & taskkill /PID $apiProcess.Id /T /F *> $null
  }
  & docker stop $containerName *> $null
  Remove-Item -LiteralPath $apiOut, $apiErr -Force -ErrorAction SilentlyContinue
  $resolvedLiveArtifacts = [System.IO.Path]::GetFullPath($liveArtifacts)
  $resolvedTempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
  if ($resolvedLiveArtifacts.StartsWith($resolvedTempRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    Remove-Item -LiteralPath $resolvedLiveArtifacts -Recurse -Force -ErrorAction SilentlyContinue
  }
}
