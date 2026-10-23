$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'test-fe02-live.ps1') -Suite Transfer
