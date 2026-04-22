@echo off
REM Check Kubernetes Status and List Resources

echo 🔍 Checking Kubernetes status...

REM Check if kubectl can connect
kubectl cluster-info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Kubernetes is not running or not accessible
    echo.
    echo 💡 To fix:
    echo    1. Open Docker Desktop
    echo    2. Go to Settings → Kubernetes
    echo    3. Check "Enable Kubernetes"
    echo    4. Click "Apply & Restart"
    echo.
    pause
    exit /b 1
)

echo ✅ Kubernetes is running!
echo.

echo 📊 Listing ALL Kubernetes resources:
echo ==================================

echo.
echo 🔄 Pods:
kubectl get pods --all-namespaces -o wide

echo.
echo 🌐 Services:
kubectl get services --all-namespaces

echo.
echo 🚀 Deployments:
kubectl get deployments --all-namespaces

echo.
echo 📦 StatefulSets:
kubectl get statefulsets --all-namespaces

echo.
echo 💾 PersistentVolumes:
kubectl get pv

echo.
echo 📁 PersistentVolumeClaims:
kubectl get pvc

echo.
echo 🔧 ConfigMaps:
kubectl get configmaps --all-namespaces

echo.
echo 🔐 Secrets:
kubectl get secrets --all-namespaces

echo.
echo 🌍 Ingress:
kubectl get ingress --all-namespaces

echo.
echo 🏷️  Namespaces:
kubectl get namespaces

echo.
echo 📈 Cluster Info:
kubectl cluster-info

pause
