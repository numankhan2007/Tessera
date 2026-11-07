@echo off
set VERSION=8.14.3
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set HOME_DIR=%GRADLE_USER_HOME%\tessera-gradle\gradle-%VERSION%
if not exist "%HOME_DIR%\bin\gradle.bat" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%GRADLE_USER_HOME%\tessera-gradle\gradle.zip'; Expand-Archive -Force '%GRADLE_USER_HOME%\tessera-gradle\gradle.zip' '%GRADLE_USER_HOME%\tessera-gradle'; Remove-Item '%GRADLE_USER_HOME%\tessera-gradle\gradle.zip'"
)
call "%HOME_DIR%\bin\gradle.bat" %*
