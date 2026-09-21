$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$missing = [System.Collections.Generic.List[string]]::new()

Get-ChildItem -LiteralPath $root -Filter '*.md' -File -Recurse |
  Where-Object { $_.FullName -notmatch '[\\/](node_modules|dist|target|\.git|rendered)[\\/]' } |
  ForEach-Object {
    $document = $_
    $content = Get-Content -LiteralPath $document.FullName -Raw
    foreach ($match in [regex]::Matches($content, '!?(?<!\!)\[[^\]]*\]\(([^)]+)\)')) {
      $target = $match.Groups[1].Value.Trim().Trim('<', '>')
      if ($target -match '^(https?://|mailto:|#)' -or [string]::IsNullOrWhiteSpace($target)) { continue }
      $pathOnly = ($target -split '#', 2)[0]
      $pathOnly = [Uri]::UnescapeDataString($pathOnly)
      $resolved = Join-Path $document.DirectoryName $pathOnly
      if (-not (Test-Path -LiteralPath $resolved)) {
        $relativeDocument = [IO.Path]::GetRelativePath($root, $document.FullName)
        $missing.Add("${relativeDocument}: $target")
      }
    }
  }

if ($missing.Count -gt 0) {
  $missing | ForEach-Object { Write-Error "Broken local link: $_" }
  exit 1
}
Write-Host 'Markdown links: OK'
