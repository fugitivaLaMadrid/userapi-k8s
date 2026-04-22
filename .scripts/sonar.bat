@echo off
echo Starting SonarQube analysis...

for /f "tokens=2 delims==" %%a in ('findstr SONAR_TOKEN .env-kubernetes') do set SONAR_TOKEN=%%a

mvn clean verify sonar:sonar -Dsonar.projectKey=userapi-k8s -Dsonar.host.url=http://localhost:30900 -Dsonar.login=%SONAR_TOKEN%

echo.
if %ERRORLEVEL% == 0 (
    echo Analysis completed successfully!
    echo Open http://localhost:30900 to see results
) else (
    echo Analysis failed. Check the logs above.
)

pause