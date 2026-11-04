# ============================================================
# Script Build APK BluetoothRemote
# Otomatis install JDK 17 + Android Command Line Tools + Build APK
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
$AndroidSdkDir = "C:\Android\Sdk"
$JdkDir = "C:\Android\jdk17"
$CmdlineToolsDir = "$AndroidSdkDir\cmdline-tools\latest"
$BuildToolsVersion = "34.0.0"
$SdkApiLevel = "34"

function Log($msg) { Write-Host "[BUILD] $msg" -ForegroundColor Cyan }
function LogOK($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function LogErr($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }

# ===========================================================
# STEP 1: Install JDK 17 via winget jika belum ada
# ===========================================================
Log "Memeriksa JDK 17..."
$javaExe = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaExe) {
    Log "JDK tidak ditemukan. Menginstall JDK 17 via winget..."
    winget install --id Microsoft.OpenJDK.17 --accept-source-agreements --accept-package-agreements --silent
    # Refresh PATH
    $env:PATH = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
} else {
    LogOK "Java sudah terinstall: $($javaExe.Source)"
}

# Cari JAVA_HOME
$javaBin = Get-Command java -ErrorAction SilentlyContinue
if ($javaBin) {
    $env:JAVA_HOME = (Get-Item (Get-Command java).Source).DirectoryName | Split-Path
    LogOK "JAVA_HOME = $env:JAVA_HOME"
} else {
    # Cari manual
    $javaSearch = @(
        "C:\Program Files\Microsoft\jdk-17*",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-17*"
    ) | ForEach-Object { Get-Item $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
    if ($javaSearch) {
        $env:JAVA_HOME = $javaSearch.FullName
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
        LogOK "JAVA_HOME = $env:JAVA_HOME"
    } else {
        LogErr "JDK tidak ditemukan setelah instalasi. Install manual: https://adoptium.net"
        exit 1
    }
}

# Verifikasi java
$javaVersion = & java -version 2>&1
Log "Java version: $javaVersion"

# ===========================================================
# STEP 2: Download Android Command Line Tools
# ===========================================================
Log "Memeriksa Android SDK..."
if (-not (Test-Path $CmdlineToolsDir)) {
    New-Item -ItemType Directory -Force -Path $AndroidSdkDir | Out-Null
    New-Item -ItemType Directory -Force -Path "$AndroidSdkDir\cmdline-tools" | Out-Null

    Log "Mengunduh Android Command Line Tools..."
    $cmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    $zipPath = "$env:TEMP\cmdline-tools.zip"
    $extractPath = "$env:TEMP\cmdline-tools-extract"

    Invoke-WebRequest -Uri $cmdlineToolsUrl -OutFile $zipPath -UseBasicParsing
    LogOK "Download selesai"

    Log "Mengekstrak..."
    if (Test-Path $extractPath) { Remove-Item $extractPath -Recurse -Force }
    Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force

    # Pindahkan ke lokasi yang benar
    Move-Item "$extractPath\cmdline-tools" "$CmdlineToolsDir" -Force
    LogOK "Android Command Line Tools terinstall di $CmdlineToolsDir"
} else {
    LogOK "Android Command Line Tools sudah ada"
}

# ===========================================================
# STEP 3: Set environment variables
# ===========================================================
$env:ANDROID_HOME = $AndroidSdkDir
$env:ANDROID_SDK_ROOT = $AndroidSdkDir
$env:PATH = "$CmdlineToolsDir\bin;$AndroidSdkDir\platform-tools;$AndroidSdkDir\build-tools\$BuildToolsVersion;$env:PATH"

# ===========================================================
# STEP 4: Install SDK components via sdkmanager
# ===========================================================
Log "Menginstall Android SDK components..."
$sdkmanager = "$CmdlineToolsDir\bin\sdkmanager.bat"

if (-not (Test-Path $sdkmanager)) {
    LogErr "sdkmanager tidak ditemukan di $sdkmanager"
    exit 1
}

# Accept licenses
Log "Menyetujui lisensi Android SDK..."
"y`ny`ny`ny`ny`ny`ny`n" | & $sdkmanager --licenses 2>&1 | Out-Null

# Install platform dan build-tools
Log "Menginstall platform-tools, build-tools, android-$SdkApiLevel..."
& $sdkmanager "platform-tools" "platforms;android-$SdkApiLevel" "build-tools;$BuildToolsVersion" 2>&1
LogOK "SDK components terinstall"

# ===========================================================
# STEP 5: Buat local.properties di project
# ===========================================================
Log "Membuat local.properties..."
$localProps = "sdk.dir=$($AndroidSdkDir -replace '\\', '\\\\')"
$localProps | Set-Content -Path "$ProjectDir\local.properties" -Encoding UTF8
LogOK "local.properties: $localProps"

# ===========================================================
# STEP 6: Buat launcher icons placeholder (agar build tidak error)
# ===========================================================
Log "Membuat launcher icon placeholder..."
$mipmapDirs = @("mipmap-hdpi", "mipmap-mdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")
foreach ($dir in $mipmapDirs) {
    $dirPath = "$ProjectDir\app\src\main\res\$dir"
    New-Item -ItemType Directory -Force -Path $dirPath | Out-Null

    # Buat ic_launcher.xml (adaptive icon placeholder)
    $iconXml = @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/primary"/>
    <foreground android:drawable="@color/secondary"/>
</adaptive-icon>
'@
    $iconXml | Set-Content "$dirPath\ic_launcher.xml" -Encoding UTF8
    Copy-Item "$dirPath\ic_launcher.xml" "$dirPath\ic_launcher_round.xml" -Force
}
LogOK "Launcher icons dibuat"

# ===========================================================
# STEP 7: Jalankan Gradle Build
# ===========================================================
Log "Memulai Gradle build..."
Set-Location $ProjectDir

# Set JAVA_HOME untuk Gradle
$gradlePropsFile = "$ProjectDir\gradle.properties"
if (-not (Test-Path $gradlePropsFile)) {
    "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8" | Set-Content $gradlePropsFile
}

$gradlew = "$ProjectDir\gradlew.bat"
if (-not (Test-Path $gradlew)) {
    LogErr "gradlew.bat tidak ditemukan. Pastikan proyek Android lengkap."
    exit 1
}

Log "Running: gradlew assembleDebug"
$buildResult = & "$gradlew" assembleDebug 2>&1
$buildResult | Write-Host

if ($LASTEXITCODE -eq 0) {
    $apkPath = Get-ChildItem "$ProjectDir\app\build\outputs\apk\debug\*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($apkPath) {
        LogOK "=============================================="
        LogOK "APK BERHASIL DIBUAT!"
        LogOK "Lokasi APK: $($apkPath.FullName)"
        LogOK "Ukuran: $([math]::Round($apkPath.Length / 1MB, 2)) MB"
        LogOK "=============================================="
        # Buka folder output
        Start-Process explorer.exe "/select,`"$($apkPath.FullName)`""
    }
} else {
    LogErr "Build gagal! Periksa output di atas untuk detail error."
    exit 1
}
