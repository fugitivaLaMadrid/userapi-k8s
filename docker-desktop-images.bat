@echo off
echo ========================================
echo   Docker Desktop Image Pull
echo ========================================
echo.

echo Checking Docker Desktop status...
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker Desktop not running
    pause
    exit /b 1
)

echo Docker Desktop is running
echo.

echo Pre-pulling all required images...
echo.

echo Pulling PostgreSQL...
docker pull postgres:15

echo Pulling Keycloak...
docker pull quay.io/keycloak/keycloak:latest

echo Pulling SonarQube (with embedded Elasticsearch)...
docker pull sonarqube:9.9.3-community

echo Pulling Adminer...
docker pull adminer:latest

echo Pulling Kubernetes Dashboard...
docker pull kubernetesui/dashboard:v2.7.0

echo.
echo ========================================
echo   All Images Downloaded!
echo ========================================
echo.
echo Images ready:
echo   - PostgreSQL 15
echo   - Keycloak
echo   - SonarQube 9.9.3 Community (with embedded Elasticsearch)
echo   - Adminer
echo   - Kubernetes Dashboard
echo.
echo Next step: Run deploy.bat
echo   .\setup-persistence-services.bat
echo.

pause