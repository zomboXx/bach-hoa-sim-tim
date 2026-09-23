$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

$tracked = @(& git -C $root ls-files)
if ($LASTEXITCODE -ne 0) { throw 'Unable to list tracked files.' }

# Add an exact repository-relative path here only when the Project Owner approves
# a final binary deliverable for version control.
$approvedWordDeliverables = @()
$violations = [System.Collections.Generic.List[string]]::new()

foreach ($path in $tracked) {
  $normalized = $path.Replace('\', '/')

  if ($normalized -match '^(\.agents|\.codex)/') {
    $violations.Add("local tool configuration: $normalized")
  }
  if ($normalized -match '\.docx$' -and $normalized -notin $approvedWordDeliverables) {
    $violations.Add("unapproved Word deliverable: $normalized")
  }
  if ($normalized -match '(^|/)(node_modules|dist|target|test-results|playwright-report)/') {
    $violations.Add("dependency/build/test output: $normalized")
  }
  if ($normalized -match '(^|/)\.env($|\.)' -and $normalized -notmatch '(^|/)\.env\.example$') {
    $violations.Add("local environment file: $normalized")
  }
}

if ($violations.Count -gt 0) {
  $details = $violations | Sort-Object -Unique | ForEach-Object { "- $_" }
  throw "Repository policy violations:`n$($details -join "`n")"
}

Write-Host 'Repository policy: OK'
