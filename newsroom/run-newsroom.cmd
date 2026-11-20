@echo off
setlocal
cd /d "C:\Users\Administrator\Desktop\0721acgn\newsroom"
if not exist "data" mkdir "data"
set TEMP=C:\Users\Administrator\Desktop\0721acgn\newsroom\data\tmp
set TMP=%TEMP%
if not exist "%TEMP%" mkdir "%TEMP%"
echo [%date% %time%] start >> "data\run.out"
"C:\Users\Administrator\Desktop\0721acgn\newsroom\.venv\Scripts\python.exe" "C:\Users\Administrator\Desktop\0721acgn\newsroom\worker.py" run --config "C:\Users\Administrator\Desktop\0721acgn\newsroom\config.json" >> "data\run.out" 2>&1
echo [%date% %time%] exit %ERRORLEVEL% >> "data\run.out"
