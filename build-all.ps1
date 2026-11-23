# Builds the mod for every supported Minecraft version.
# Usage: .\build-all.ps1            (all versions)
#        .\build-all.ps1 1.21.1     (single version)
param([string[]]$Versions = @())

$all = @('1.21','1.21.1','1.21.2','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7',
         '1.21.8','1.21.9','1.21.10','1.21.11','26.1','26.1.2','26.2','26.3')
if ($Versions.Count -eq 0) { $Versions = $all }

$root = $PSScriptRoot
$gradle = Join-Path $root 'tools\gradle-9.9.2\bin\gradle.bat'
if (-not (Test-Path $gradle)) {
    Write-Error "Gradle not found at $gradle - run bootstrap.ps1 first"
    exit 1
}

$results = @()
foreach ($v in $Versions) {
    $props = Join-Path $root "versions\$v.gradle.properties"
    if (-not (Test-Path $props)) {
        Write-Warning "No version properties for $v, skipping"
        continue
    }
    Write-Host "=== Building $v ===" -ForegroundColor Cyan
    & $gradle -q -p $root "-PmcVersion=$v" "-PversionProps=$props" build
    if ($LASTEXITCODE -eq 0) {
        Write-Host "=== $v OK ===" -ForegroundColor Green
        $results += [pscustomobject]@{ Version = $v; Status = 'OK' }
    } else {
        Write-Host "=== $v FAILED ===" -ForegroundColor Red
        $results += [pscustomobject]@{ Version = $v; Status = 'FAILED' }
    }
}
$results | Format-Table -AutoSize
