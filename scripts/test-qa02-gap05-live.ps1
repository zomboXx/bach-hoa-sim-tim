$ErrorActionPreference = 'Stop'

$liveRunner = Join-Path $PSScriptRoot 'test-fe02-live.ps1'
& $liveRunner -Suite Qa02Gap05
