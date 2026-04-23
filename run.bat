@echo off
echo Starting AI CV Optimizer...
echo.
echo Make sure you have set your CLAUDE_API_KEY environment variable.
echo OR edit application.properties and replace "your-api-key-here" with your actual key.
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

set CLAUDE_API_KEY=%CLAUDE_API_KEY%

if "%CLAUDE_API_KEY%"=="" (
    echo WARNING: CLAUDE_API_KEY is not set!
    echo Set it with:  set CLAUDE_API_KEY=your-key-here
    echo Or edit src\main\resources\application.properties
    echo.
)

echo Opening browser at http://localhost:8087 in 5 seconds...
timeout /t 5 /nobreak > nul
start "" "http://localhost:8087"

java -jar target\cv-optimizer-1.0.0.jar
pause
