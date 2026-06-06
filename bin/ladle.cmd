@echo off
setlocal

if defined LADLE_HOME (
  set "APP_HOME=%LADLE_HOME%"
) else (
  set "APP_HOME=%~dp0.."
)

for %%I in ("%APP_HOME%") do set "APP_HOME=%%~fI"

set "JAR=%APP_HOME%\lib\ladle.jar"
if not exist "%JAR%" (
  echo Cannot find ladle.jar at %JAR% >&2
  echo Set LADLE_HOME or run install.ps1 to install Ladle. >&2
  exit /b 1
)

if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA=java"
)

"%JAVA%" -jar "%JAR%" %*
exit /b %ERRORLEVEL%
