@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\run-site.ps1" %*
exit /b %ERRORLEVEL%
