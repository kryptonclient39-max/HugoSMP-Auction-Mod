$tools = 'C:\Users\ndds4\Downloads\HugoSMP auction mod\tools'
New-Item -ItemType Directory -Force -Path $tools | Out-Null
$zip = Join-Path $tools 'gradle-9.9.2-bin.zip'
& curl.exe -L --retry 3 -o $zip 'https://services.gradle.org/distributions/gradle-9.9.2-bin.zip'
if ($LASTEXITCODE -eq 0) {
    Expand-Archive -Path $zip -DestinationPath $tools -Force
    Remove-Item $zip
    Write-Output 'DONE'
}
