@echo off
setlocal
set "GRADLE_VERSION=8.10.2"
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo Gradle %GRADLE_VERSION% is not installed. Please install Gradle or open the project in Android Studio.
exit /b 1
