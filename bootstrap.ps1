# Downloads Gradle and the Gradle wrapper for the repo.
$root = $PSScriptRoot
$tools = Join-Path $root 'tools'
New-Item -ItemType Directory -Force -Path $tools | Out-Null

$zip = Join-Path $tools 'gradle-9.9.2-bin.zip'
if (-not (Test-Path (Join-Path $tools 'gradle-9.9.2\bin\gradle.bat'))) {
    Write-Host 'Downloading Gradle 9.9.2...'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.9.2-bin.zip' -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $tools -Force
    Remove-Item $zip
    Write-Host 'Gradle installed.'
} else {
    Write-Host 'Gradle already installed.'
}
