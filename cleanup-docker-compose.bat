@echo off
REM Docker Compose Cleanup Script - Windows Version

echo 🧹 Cleaning up Docker Compose resources...

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

REM Store current directory
set CURRENT_DIR=%cd%

REM Navigate to original project directory
cd ..\userapi

if not exist "docker-compose.yml" (
    echo ❌ docker-compose.yml not found in %cd%
    cd %CURRENT_DIR%
    pause
    exit /b 1
)

echo 📍 Working in: %cd%

REM Stop and remove all containers and volumes
echo 🛑 Stopping Docker Compose services...
docker-compose down -v --remove-orphans
if %errorlevel% equ 0 (
    echo ✅ Docker Compose services stopped and volumes removed
) else (
    echo ⚠️  Error stopping Docker Compose services
)

REM Remove specific images
echo 🗑️  Removing Docker images...

REM Remove userapi application image
docker images --format "table {{.Repository}}:{{.Tag}}" | findstr "userapi_userapi" >nul
if %errorlevel% equ 0 (
    docker rmi userapi_userapi:latest 2>nul
    echo ✅ Removed userapi_userapi image
)

REM Clean up related images
echo 🧹 Cleaning up related images...
docker image prune -f >nul

REM Clean up system
echo 🧹 System cleanup...
docker system prune -f >nul

REM Show remaining resources
echo.
echo 📊 Remaining Docker resources:
echo Containers:
docker ps -a --filter "name=userapi" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" 2>nul || echo   No userapi containers found

echo.
echo Images:
docker images --filter "reference=*userapi*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" 2>nul || echo   No userapi images found

echo.
echo Volumes:
docker volume ls --filter "name=userapi" --format "table {{.Name}}\t{{.Driver}}" 2>nul || echo   No userapi volumes found

echo.
echo ✅ Docker Compose cleanup completed!

REM Return to original directory
cd %CURRENT_DIR%

echo.
echo 🚀 You can now deploy to Kubernetes:
echo    deploy.bat

pause
