# Builds every supported version and collects JARs into dist/<version>/.
# Launch detached; writes per-version status to build-results.txt.
$root = 'C:\Users\ndds4\Downloads\HugoSMP auction mod'
$versions = @('1.21','1.21.1','1.21.2','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7',
              '1.21.8','1.21.9','1.21.10','1.21.11','26.1','26.1.2','26.2','26.3')
$results = Join-Path $root 'build-results.txt'
Set-Content -Path $results -Value "started $(Get-Date -Format 'HH:mm:ss')"

foreach ($v in $versions) {
    $stamp = Get-Date -Format 'HH:mm:ss'
    Write-Output "[$stamp] building $v"
    powershell -ExecutionPolicy Bypass -File (Join-Path $root 'run-gradle.ps1') $v clean build --no-daemon --console=plain *> (Join-Path $root "build-$v.log")
    if ($LASTEXITCODE -eq 0) {
        $dist = Join-Path $root "dist\$v"
        New-Item -ItemType Directory -Force -Path $dist | Out-Null
        Get-ChildItem (Join-Path $root 'build\libs\*.jar') | Where-Object { $_.Name -notmatch 'sources' } | ForEach-Object {
            Copy-Item $_.FullName (Join-Path $dist ("hugosmp-auctionhouse-$v.jar")) -Force
        }
        Add-Content -Path $results -Value "$v OK"
    } else {
        Add-Content -Path $results -Value "$v FAILED (exit $LASTEXITCODE)"
    }
}
Add-Content -Path $results -Value "done $(Get-Date -Format 'HH:mm:ss')"
