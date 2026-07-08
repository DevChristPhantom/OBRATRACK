@echo off
REM ============================================================
REM  Genera el INSTALADOR de ObraTrack para Windows (.exe) con jpackage.
REM  Requisitos:
REM    - JDK 17 o superior en el PATH (jpackage viene incluido).
REM    - Para .exe/.msi: WiX Toolset 3.x instalado (candle.exe / light.exe en el PATH).
REM      Descarga: https://github.com/wixtoolset/wix3/releases
REM  Si no tienes WiX, usa build-portable.bat (no requiere WiX).
REM ============================================================
setlocal
cd /d "%~dp0\.."

echo == 1/3  Compilando el .jar con Maven...
call mvn -q clean package
if errorlevel 1 ( echo Fallo el build de Maven & exit /b 1 )

echo == 2/3  Preparando carpeta de entrada...
if exist packaging\app-input rmdir /s /q packaging\app-input
mkdir packaging\app-input
copy /y target\ObraTrack.jar packaging\app-input\ >nul

echo == 3/3  Generando instalador con jpackage...
jpackage ^
  --type exe ^
  --name ObraTrack ^
  --app-version 2.0.0 ^
  --vendor "Grupo Titan G&L S.A.C." ^
  --description "Sistema de gestion de obras" ^
  --input packaging\app-input ^
  --main-jar ObraTrack.jar ^
  --main-class com.obratrack.Main ^
  --icon packaging\obratrack.ico ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "ObraTrack" ^
  --win-dir-chooser ^
  --dest dist-installer

if errorlevel 1 (
  echo.
  echo No se pudo generar el .exe. Si el error menciona WiX, instala WiX Toolset 3.x,
  echo o genera una version portable con:  packaging\build-portable.bat
  exit /b 1
)
echo.
echo Listo. El instalador quedo en la carpeta  dist-installer\
endlocal
