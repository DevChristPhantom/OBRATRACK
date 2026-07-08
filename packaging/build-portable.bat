@echo off
REM ============================================================
REM  Genera una version PORTABLE de ObraTrack (carpeta con ObraTrack.exe).
REM  NO requiere WiX. Solo JDK 17+ en el PATH.
REM  Resultado: dist-portable\ObraTrack\ObraTrack.exe (se puede copiar en un USB).
REM ============================================================
setlocal
cd /d "%~dp0\.."

echo == 1/2  Compilando el .jar con Maven...
call mvn -q clean package
if errorlevel 1 ( echo Fallo el build de Maven & exit /b 1 )

echo == 2/2  Generando app portable con jpackage...
if exist packaging\app-input rmdir /s /q packaging\app-input
mkdir packaging\app-input
copy /y target\ObraTrack.jar packaging\app-input\ >nul
if exist dist-portable rmdir /s /q dist-portable

jpackage ^
  --type app-image ^
  --name ObraTrack ^
  --app-version 2.0.0 ^
  --vendor "Grupo Titan G&L S.A.C." ^
  --input packaging\app-input ^
  --main-jar ObraTrack.jar ^
  --main-class com.obratrack.Main ^
  --icon packaging\obratrack.ico ^
  --dest dist-portable

if errorlevel 1 ( echo Fallo jpackage & exit /b 1 )
echo.
echo Listo. Ejecutable en  dist-portable\ObraTrack\ObraTrack.exe
endlocal
