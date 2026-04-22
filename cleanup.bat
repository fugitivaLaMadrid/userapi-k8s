@echo off
REM Kubernetes Cleanup Script for UserAPI - Windows Version

echo [CLEANUP] Cleaning up UserAPI Kubernetes resources...

REM Check if kubectl is available
kubectl version --client >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] kubectl is not installed or not in PATH
    pause
    exit /b 1
)

REM Delete all resources in reverse order of creation
echo [REMOVING] UserAPI deployment...
kubectl delete -f k8s/userapi-deployment.yaml --ignore-not-found=true

echo [REMOVING] UserAPI services...
kubectl delete -f k8s/userapi-service.yaml --ignore-not-found=true

echo [REMOVING] Keycloak...
kubectl delete -f k8s/keycloak-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/keycloak-service.yaml --ignore-not-found=true

echo [REMOVING] PostgreSQL...
kubectl delete -f k8s/postgres-statefulset.yaml --ignore-not-found=true
kubectl delete -f k8s/postgres-service.yaml --ignore-not-found=true

echo [REMOVING] Adminer...
kubectl delete -f k8s/adminer-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/adminer-service.yaml --ignore-not-found=true

echo [REMOVING] SonarQube...
kubectl delete -f k8s/sonarqube-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/sonarqube-service.yaml --ignore-not-found=true

echo [REMOVING] Kubernetes Dashboard...
kubectl delete -f k8s/kubernetes-dashboard.yaml --ignore-not-found=true

echo [REMOVING] Ingress...
kubectl delete -f k8s/ingress.yaml --ignore-not-found=true

echo [REMOVING] ConfigMaps and Secrets...
kubectl delete -f k8s/configmap.yaml --ignore-not-found=true
kubectl delete -f k8s/secret.yaml --ignore-not-found=true

REM Delete any remaining PVCs
echo [REMOVING] Persistent Volume Claims...
kubectl delete pvc --all -n default --ignore-not-found=true

REM Delete ConfigMap if it exists separately
echo [REMOVING] init scripts...
kubectl delete configmap postgres-init-script --ignore-not-found=true

REM Wait for pods to terminate
echo [WAITING] Waiting for pods to terminate...
timeout /t 10 /nobreak >nul

echo [SUCCESS] Cleanup completed!

REM Show remaining resources (if any)
echo.
echo [STATUS] Remaining resources:
kubectl get all -n default
echo.
kubectl get pvc -n default
echo.

echo [DONE] All UserAPI resources have been cleaned up!
echo.
echo To start fresh, run:
echo   1. docker-desktop-images-working.bat
echo   2. setup-persistent-services.bat
echo.

pause