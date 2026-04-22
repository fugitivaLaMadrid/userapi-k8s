@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Persistent Services Setup
echo ========================================
echo.

REM Check if kubectl is available
kubectl version --client >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: kubectl not found
    pause
    exit /b 1
)

echo Building UserAPI Docker image...
docker build -t userapi:latest . >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Docker build failed, continuing with existing image
)

echo.
echo Applying persistent configurations...
echo.

echo 1. Setting up ConfigMaps and Secrets...
kubectl apply -f k8s\configmap.yaml
kubectl apply -f k8s\secret.yaml

echo 2. Setting up PostgreSQL with persistent storage...
kubectl apply -f k8s\postgres-service.yaml
kubectl apply -f k8s\postgres-statefulset.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to setup PostgreSQL
    pause
    exit /b 1
)

echo.
echo 3. Setting up Keycloak with persistent storage...
kubectl apply -f k8s\keycloak-service.yaml
kubectl apply -f k8s\keycloak-deployment.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to setup Keycloak
    pause
    exit /b 1
)

echo.
echo 4. Setting up SonarQube with persistent storage...
kubectl apply -f k8s\sonarqube-deployment.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to setup SonarQube
    pause
    exit /b 1
)

echo.
echo 5. Setting up UserAPI service...
kubectl apply -f k8s\userapi-service.yaml
kubectl apply -f k8s\userapi-deployment.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to setup UserAPI
    pause
    exit /b 1
)

echo.
echo 6. Setting up Adminer for database access...
kubectl apply -f k8s\adminer-deployment.yaml
kubectl apply -f k8s\adminer-service.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to setup Adminer
    pause
    exit /b 1
)

echo.
echo 7. Setting up Kubernetes Dashboard...
kubectl apply -f k8s\kubernetes-dashboard.yaml
if %errorlevel% neq 0 (
    echo ERROR: Failed to setup Dashboard
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Services Configuration Complete!
echo ========================================
echo.

echo.
echo ========================================
echo   Waiting for Core Services
echo ========================================
echo.

echo Waiting for PostgreSQL to be ready...
kubectl wait --for=condition=ready pod -l app=postgres --timeout=300s

echo Waiting for UserAPI to be ready...
kubectl wait --for=condition=ready pod -l app=userapi --timeout=300s

echo.
echo ========================================
echo   Waiting for Keycloak Bootstrap
echo ========================================
echo.

echo Waiting for Keycloak pod to start...
kubectl wait --for=condition=ready pod -l app=keycloak --timeout=60s

echo Keycloak pod is running, waiting for full bootstrap...
echo Checking logs for "started in" message...
echo.

REM Wait for Keycloak to fully initialize
set "keycloak_ready=false"
set "max_attempts=120"
set "attempt=0"

:check_keycloak
set /a attempt=%attempt%+1
if %attempt% gtr %max_attempts% goto keycloak_timeout

for /f "tokens=*" %%A in ('kubectl logs -l app=keycloak -n default 2^>nul ^| findstr /C:"started in"') do (
    set "keycloak_ready=true"
    goto keycloak_ready
)

timeout /t 1 /nobreak >nul
goto check_keycloak

:keycloak_ready
echo Keycloak is fully initialized and ready!
echo.
goto sonarqube_check

:keycloak_timeout
echo WARNING: Keycloak initialization timeout, it may still be starting...
echo.

:sonarqube_check
echo ========================================
echo   Waiting for SonarQube Bootstrap
echo ========================================
echo.

echo Waiting for SonarQube pod to start...
kubectl wait --for=condition=ready pod -l app=sonarqube --timeout=300s

echo SonarQube pod is running, waiting for full bootstrap...
echo Checking logs for "SonarQube is ready" message...
echo.

REM Wait for SonarQube to fully initialize
set "sonarqube_ready=false"
set "max_attempts=180"
set "attempt=0"

:check_sonarqube
set /a attempt=%attempt%+1
if %attempt% gtr %max_attempts% goto sonarqube_timeout

for /f "tokens=*" %%A in ('kubectl logs -l app=sonarqube -n default 2^>nul ^| findstr /C:"SonarQube is ready"') do (
    set "sonarqube_ready=true"
    goto sonarqube_ready
)

timeout /t 1 /nobreak >nul
goto check_sonarqube

:sonarqube_ready
echo SonarQube is fully initialized and ready!
echo.
goto start_port_forwards

:sonarqube_timeout
echo WARNING: SonarQube initialization timeout, it may still be starting...
echo.

:start_port_forwards
echo ========================================
echo   Starting Port Forwards
echo ========================================
echo.

echo NOTE: Port-forward windows will open. Keep them open while using services.
echo.

echo Starting UserAPI port-forward (localhost:30080)...
start "UserAPI" cmd /c "kubectl port-forward svc/userapi-service-nodeport 30080:8080 --address 127.0.0.1"

timeout /t 3 /nobreak >nul

echo Starting Keycloak port-forward (localhost:30180)...
start "Keycloak" cmd /c "kubectl port-forward svc/keycloak-service-nodeport 30180:8080 --address 127.0.0.1"

timeout /t 3 /nobreak >nul

echo Starting Adminer port-forward (localhost:30880)...
start "Adminer" cmd /c "kubectl port-forward svc/adminer-service-nodeport 30880:8080 --address 127.0.0.1"

timeout /t 2 /nobreak >nul

echo Starting SonarQube port-forward (localhost:30900)...
start "SonarQube" cmd /c "kubectl port-forward svc/sonarqube-service-nodeport 30900:9000 --address 127.0.0.1"

timeout /t 5 /nobreak >nul

echo Starting Dashboard port-forward (localhost:30443)...
start "Dashboard" cmd /k "kubectl port-forward -n kubernetes-dashboard svc/kubernetes-dashboard 30443:443 --address 127.0.0.1"

timeout /t 3 /nobreak >nul

echo.
echo ========================================
echo   Generating Dashboard Token
echo ========================================
echo.

powershell -ExecutionPolicy Bypass -File .\.scripts\setup-k8s-dashboard.ps1

echo.
echo ========================================
echo   Services Ready!
echo ========================================
echo.

echo Service URLs:
echo - UserAPI: http://localhost:30080
echo - Keycloak: http://localhost:30180
echo - SonarQube: http://localhost:30900
echo - Adminer: http://localhost:30880
echo - Dashboard: https://localhost:30443
echo.
echo Database Credentials (saved in .env-kubernetes):
echo - PostgreSQL: postgres-service:5432
echo - Keycloak DB: keycloak/keycloak_password
echo - SonarQube DB: sonarqube/sonarqube_password
echo - UserAPI DB: userapi_user/userapi_pass
echo.
echo Dashboard token has been saved to .env-kubernetes
echo.
echo All services are now configured with persistent storage!
echo Your data will survive Docker Desktop restarts.
echo.

pause
