@echo off
chcp 65001 > nul
echo ==============================================================
echo   MUMBAI BEST BUS TRANSIT MANAGEMENT SYSTEM (OOP PROJECT)
echo ==============================================================
echo.
echo Compiling source code...
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src/com/mumbai/best/exception/*.java src/com/mumbai/best/model/*.java src/com/mumbai/best/service/*.java src/com/mumbai/best/test/*.java src/com/mumbai/best/app/*.java src/com/mumbai/best/gui/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please make sure JDK 17+ is installed.
    pause
    exit /b %ERRORLEVEL%
)

echo [OK] Compilation successful!
echo.
echo Select an option:
echo   1. Launch Visual GUI Application (Swing Desktop App) [DEFAULT]
echo   2. Run Interactive Console CLI Menu
echo   3. Run Comprehensive Automated Demo (--demo)
echo   4. Run Unit Test Suite (35 Tests)
echo   5. Exit
echo.
set /p choice="Enter choice (1-5, default 1): "
if "%choice%"=="" set choice=1

if "%choice%"=="1" (
    echo Launching Visual GUI...
    start javaw "-Dfile.encoding=UTF-8" -cp bin com.mumbai.best.gui.BestTransitGuiApp
) else if "%choice%"=="2" (
    java "-Dfile.encoding=UTF-8" -cp bin com.mumbai.best.app.BestTransitApp
) else if "%choice%"=="3" (
    java "-Dfile.encoding=UTF-8" -cp bin com.mumbai.best.app.BestTransitApp --demo
) else if "%choice%"=="4" (
    java "-Dfile.encoding=UTF-8" -cp bin com.mumbai.best.test.TransitSystemTest
) else (
    echo Exiting...
)
pause
