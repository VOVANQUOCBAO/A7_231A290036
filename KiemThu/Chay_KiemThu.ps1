param([ValidateSet('full','denial','bad_registration')][string]$Mode='full')
$ErrorActionPreference='Stop'
$ProjectRoot=Split-Path $PSScriptRoot -Parent
$SdkRoot=if ($env:ANDROID_HOME) {$env:ANDROID_HOME} else {Join-Path $env:LOCALAPPDATA 'Android/Sdk'}
$Adb=Join-Path $SdkRoot 'platform-tools/adb.exe'
if (!(Test-Path -LiteralPath $Adb)) { throw 'Khong tim thay Android SDK platform-tools/adb.exe' }
if (!$env:JAVA_HOME) { $env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr' }
[System.IO.File]::WriteAllText((Join-Path $ProjectRoot 'local.properties'), ('sdk.dir='+$SdkRoot.Replace('\','/')), [System.Text.UTF8Encoding]::new($false))
Push-Location $ProjectRoot
try {
    & ./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
    & $Adb install -r -t './app/build/outputs/apk/debug/app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Cai APK that bai; bat may ao truoc' }
    & $Adb install -r -t './app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Cai APK kiem thu that bai' }
    if ($Mode -ne 'bad_registration') { & $Adb shell pm clear 'vn.edu.vhu.ltdd.a7permission' }
    & $Adb shell am instrument -w -r -e mode $Mode 'vn.edu.vhu.ltdd.a7permission.test/vn.edu.vhu.ltdd.a7permission.LabRunner'
} finally { Pop-Location }
