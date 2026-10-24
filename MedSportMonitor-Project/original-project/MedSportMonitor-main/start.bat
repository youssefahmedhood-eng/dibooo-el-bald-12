@echo off
setlocal EnableDelayedExpansion

REM ============================================================
REM  MedSport Monitor — Windows Startup Script
REM  Usage: double-click start.bat  (run as normal user)
REM ============================================================

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

set "PYTHON_SIM=%ROOT%\python-simulator"
set "PYTHON_API=%ROOT%\python-analytics"
set "JAVA_APP=%ROOT%\java-app"
set "LOG_DIR=%ROOT%\logs"

REM ---- Java 21 auto-detection ----
if "%JAVA_HOME%"=="" (
    for /f "delims=" %%i in ('where java 2^>nul') do (
        set "JAVA_BIN=%%i"
        goto :java_found
    )
    REM Try common installation paths
    if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
        goto :java_found
    )
    if exist "C:\Program Files\Microsoft\jdk-21.0.0.35-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.0.35-hotspot"
        goto :java_found
    )
    echo ERROR: Java 21 not found. Install from https://adoptium.net and set JAVA_HOME.
    pause
    exit /b 1
)
:java_found

if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo  =============================================
echo   MedSport Monitor — Starting
echo  =============================================
echo.

REM ---- Create log directory ----
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%ROOT%\mosquitto_data" mkdir "%ROOT%\mosquitto_data"

REM ---- 1. Mosquitto MQTT broker ----
echo [1/5] Starting MQTT broker...
where mosquitto >nul 2>&1
if %ERRORLEVEL%==0 (
    start /b "Mosquitto" mosquitto -c "%ROOT%\mosquitto.conf"
    timeout /t 1 /nobreak >nul
    echo   Mosquitto running on port 1883
) else (
    echo   Mosquitto not found — app will run in demo mode ^(no live sensor data^)
    echo   To install: winget install EclipseFoundation.Mosquitto
)

REM ---- 2. Python virtual environment ----
echo.
echo [2/5] Setting up Python environment...
if not exist "%ROOT%\venv\Scripts\activate.bat" (
    python -m venv "%ROOT%\venv" 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo   Python not found — simulator and analytics will not start.
        echo   Install Python 3.11+ from https://www.python.org
        goto :skip_python
    )
    echo   Virtual environment created.
)
call "%ROOT%\venv\Scripts\activate.bat"
pip install -r "%PYTHON_SIM%\requirements.txt" -q --disable-pip-version-check
pip install -r "%PYTHON_API%\requirements.txt" -q --disable-pip-version-check
echo   Python dependencies ready.

REM ---- 3. FastAPI analytics service ----
echo.
echo [3/5] Starting analytics service...
start /b "FastAPI" cmd /c "cd /d "%PYTHON_API%" && uvicorn main:app --host 0.0.0.0 --port 8000 --log-level warning > "%LOG_DIR%\fastapi.log" 2>&1"
timeout /t 2 /nobreak >nul
echo   FastAPI running at http://localhost:8000

REM ---- 4. Sensor simulator ----
echo.
echo [4/5] Starting sensor simulator...
start /b "Simulator" cmd /c "cd /d "%PYTHON_SIM%" && python simulator.py > "%LOG_DIR%\simulator.log" 2>&1"
timeout /t 1 /nobreak >nul
echo   Simulator publishing HR, SpO2, GPS, accelerometer data via MQTT.

:skip_python

REM ---- 5. Launch Java application ----
echo.
echo [5/5] Launching MedSport Monitor...
echo.
echo  =============================================
echo   Demo credentials (password: Demo1234!)
echo    Coach:   coach@demo.com
echo    Medical: doctor@demo.com
echo    Athlete: athlete1@demo.com
echo  =============================================
echo.

cd /d "%JAVA_APP%"
set "MAVEN_OPTS=-Dprism.order=sw -Xmx512m"
call mvnw.cmd javafx:run

REM ---- Cleanup on exit ----
echo.
echo Shutting down background services...
taskkill /f /im mosquitto.exe >nul 2>&1
taskkill /f /t /fi "WINDOWTITLE eq FastAPI" >nul 2>&1
taskkill /f /t /fi "WINDOWTITLE eq Simulator" >nul 2>&1
echo Done. Goodbye!
endlocal
