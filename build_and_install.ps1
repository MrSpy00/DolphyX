$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$SdkRoot = if ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else {
    $localProps = Join-Path $ProjectRoot "local.properties"
    if (Test-Path $localProps) {
        $sdkLine = Get-Content $localProps | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($sdkLine) {
            ($sdkLine -replace "^sdk\.dir=", "").Replace("\\", "\").Replace("\:", ":")
        }
    }
}

if (-not $SdkRoot -or -not (Test-Path $SdkRoot)) {
    throw "Android SDK not found. Set ANDROID_HOME, ANDROID_SDK_ROOT, or sdk.dir in local.properties."
}

$Adb = Join-Path $SdkRoot "platform-tools\adb.exe"
$BuildToolsDir = Get-ChildItem (Join-Path $SdkRoot "build-tools") -Directory | Sort-Object Name -Descending | Select-Object -First 1
$ApkSigner = Join-Path $BuildToolsDir.FullName "apksigner.bat"
$ApkPath = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"

Push-Location $ProjectRoot
try {
    Write-Host "[1/4] Running unit tests (testDebugUnitTest)..." -ForegroundColor Cyan
    & "$ProjectRoot\gradlew.bat" testDebugUnitTest --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Unit tests failed with exit code $LASTEXITCODE" }

    Write-Host "[2/4] Building release APK (assembleRelease)..." -ForegroundColor Cyan
    & "$ProjectRoot\gradlew.bat" assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed with exit code $LASTEXITCODE" }

    if (-not (Test-Path $ApkPath)) { throw "Release APK not found at $ApkPath" }

    Write-Host "[3/4] Verifying APK v2/v3 signature via apksigner..." -ForegroundColor Cyan
    & $ApkSigner verify --verbose --print-certs $ApkPath
    if ($LASTEXITCODE -ne 0) { throw "apksigner verification failed with exit code $LASTEXITCODE" }

    Write-Host "[4/4] Installing release APK on connected device..." -ForegroundColor Cyan
    & $Adb install -r $ApkPath
    if ($LASTEXITCODE -ne 0) { throw "adb install failed with exit code $LASTEXITCODE" }

    Write-Host "[DONE] DolphyX built, verified, and installed successfully." -ForegroundColor Green
}
finally {
    Pop-Location
}
