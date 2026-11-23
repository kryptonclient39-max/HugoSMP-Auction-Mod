# Wrapper: sets JAVA_HOME to JDK 25 (required by fabric-loom 1.19.4), runs gradle.
# Usage: .\run-gradle.ps1 <mc-version> <gradle args...>
param(
    [Parameter(Mandatory=$true)][string]$McVersion,
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs
)
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$root = 'C:\Users\ndds4\Downloads\HugoSMP auction mod'
$props = Join-Path $root "versions\$McVersion.gradle.properties"
& (Join-Path $root 'tools\gradle-9.9.2\bin\gradle.bat') -p $root "-PversionProps=$props" @GradleArgs
exit $LASTEXITCODE
