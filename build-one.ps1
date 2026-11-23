param([string]$McVersion)
$root = 'C:\Users\ndds4\Downloads\HugoSMP auction mod'
$log = Join-Path $root ("build-" + $McVersion + ".log")
& (Join-Path $root 'run-gradle.ps1') $McVersion clean build --no-daemon --console=plain *> $log
exit $LASTEXITCODE