@echo off
echo Stopping MedSport Monitor services...
taskkill /f /im mosquitto.exe >nul 2>&1
taskkill /f /t /fi "WINDOWTITLE eq FastAPI" >nul 2>&1
taskkill /f /t /fi "WINDOWTITLE eq Simulator" >nul 2>&1
for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr ":8000 " ^| findstr "LISTENING"') do (
    taskkill /f /pid %%p >nul 2>&1
)
echo All services stopped.
