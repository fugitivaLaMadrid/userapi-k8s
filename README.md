# UserAPI Kubernetes Deployment

This guide explains how to deploy UserAPI application to Kubernetes on Docker Desktop.

## Prerequisites

### Required Tools
- **kubectl** - Kubernetes command-line tool
- **Docker Desktop** - With Kubernetes enabled and running
- **PowerShell** - For running deployment scripts
- **Git** - For version control

### Important: Kubernetes Setup
**Before starting the deployment, ensure:**
- Docker Desktop is installed and running
- Kubernetes is **enabled** in Docker Desktop settings
- Kubernetes cluster status shows "running" (green) in Docker Desktop
- `kubectl cluster-info` command returns successful response

### Optional Tools
- **Helm** - For package management (future)
- **Lens** - Kubernetes IDE for monitoring

## Complete Step-by-Step Deployment Guide

### 1. Navigate to Project Directory
```batch
cd C:\Apps\repos\intellij\userapi\userapi-k8s
```

### 2. Complete Cleanup (Remove all old resources)
```batch
.\cleanup.bat
kubectl delete pvc --all -n default --ignore-not-found=true
timeout /t 10 /nobreak
```

Verify cleanup:
```batch
kubectl get all
# Should only show: kubernetes service
```

### 3. Pull All Docker Images
```batch
.\docker-desktop-images-working.bat
```

This pre-pulls images so Kubernetes doesn't have to fetch them:
- PostgreSQL 15 Alpine
- Keycloak (latest)
- SonarQube 9.9.3 Community
- Adminer (latest)
- Kubernetes Dashboard v2.7.0

### 4. Deploy All Services
```batch
.\setup-persistent-services.bat
```

The script automatically:
- Applies ConfigMaps and Secrets
- Deploys PostgreSQL and waits for readiness
- Deploys all microservices
- Waits for Keycloak and SonarQube initialization
- Shows final pod status
- This takes ~3-5 minutes. Wait for completion before moving on.

### 5. Check Keycloak logs (Optional)
Wait until keycloak is running- check the logs
```batch
kubectl logs -l app=keycloak -n default –f
```


This opens port-forward windows for:
- **UserAPI**: http://localhost:30080
- **Keycloak**: http://localhost:30180
- **Adminer**: http://localhost:30880
- **SonarQube**: http://localhost:30900
- **Kubernetes Dashboard**: https://localhost:30443

Keep these windows open while developing.

### 6. Setup Keycloak Realm 
Open a new PowerShell window and run:
```powershell
powershell -ExecutionPolicy Bypass -File .\.scripts\setup-keycloak-k8s.ps1
```

This script:
- Creates the `userapi-realm` realm
- Configures OAuth2/OIDC client
- Sets up roles and permissions
- Enables client credentials flow

## Manual Setup Steps Keycloak (After deployment completes)

```powershell
# 3. Setup Keycloak realm (NEW PowerShell window)
# Go to http://localhost:30180/admin
# Login with admin/admin123
# Navigate to realm "userapi-realm"
# Click on "userapi-client" to activate the client
# Ensure client is enabled and configured

# 4. Alternative: Run realm setup script
powershell -ExecutionPolicy Bypass -File .\.scripts\setup-keycloak-k8s.ps1
```

### Keycloak Client Activation Details

After accessing Keycloak admin console:

1. After login on http://localhost:30180/admin
2. **Navigate to Realm**: Click on "userapi-realm" in the realms list
3. **Activate Client**: Click on "userapi-client" 
4. **Find Client**: Click on "Clients" in the left menu
4. **Verify Settings**:
    - **Enabled**: on Realm Manger the "userapi" show in grey
    - **Access Type**: confidential
    - **Valid Redirect URIs**: Include your UserAPI endpoints
    - **Service Accounts Enabled**: ON
5. **Get Credentials**: Click on "Credentials" tab to see client secret

Done! Services are now running with persistent storage.


**Note**: Kubernetes Dashboard setup and token generation are automatically included in setup-persistent-services.bat

### Dashboard Client Activation Details
**Access Dashboard**: https://localhost:30443
**Token**: Automatically saved in .env-kubernetes file. Follow up the browser and copy the token

## Service Access URLs

After deployment, access services at:

| Service | URL | Default Credentials |
|---------|-----|-------------------|
| **UserAPI** | http://localhost:30080 | None (needs token) |
| **Keycloak Admin** | http://localhost:30180/admin | admin / admin123 |
| **Adminer** | http://localhost:30880 | See below |
| **SonarQube** | http://localhost:30900 | admin / admin |
| **Kubernetes Dashboard** | https://localhost:30443 | Token (see below) |

### Adminer Database Access

Access Adminer at http://localhost:30880

**PostgreSQL Credentials:**
```
System: PostgreSQL
Server: postgres-service
Username: userapi_user
Password: userapi_pass
Database: userapi_db
```

### SonarQube Configuration

Access SonarQube at http://localhost:30900

**Initial Setup:**
1. Login with admin / admin
2. Go to Account > Security
3. Create new token for scanning

### Creating a SonarQube Project

1. **Access SonarQube**: Open http://localhost:30900
2. **Login**: Use admin / admin
3. **Create Project**:
   - Click "Manually" (or "From existing project")
   - **Project Key**: `userapi-k8s` (must match Maven project key)
   - **Display Name**: `UserAPI Kubernetes`
   - **Organization**: Select default or create new
4. **Configure Project**:
   - **Main Branch**: `main` (or your default branch)
   - **Build System**: Maven
   - **Project Language**: Java
5. **Get Analysis Token**:
   - Go to **My Account** (top right) > **Security**
   - Click **Generate Tokens**
   - **Token Name**: `userapi-analysis`
   - **Copy the generated token**

### Save SonarQube Token

Add the token to your `.env-kubernetes` file:

```bash
# Add this line to .env-kubernetes
SONAR_TOKEN=your_generated_token_here
```

### Run Code Analysis

Once you have the token saved, run the analysis:

```bash
# Run SonarQube analysis
.\.scripts\sonar.bat
```

**What the script does:**
1. Reads `SONAR_TOKEN` from `.env-kubernetes`
2. Runs Maven clean, verify, and SonarQube analysis
3. Uploads results to http://localhost:30900
4. Shows success/failure message

**Manual alternative:**
```bash
mvn clean verify sonar:sonar -Dsonar.projectKey=userapi-k8s -Dsonar.host.url=http://localhost:30900 -Dsonar.login=YOUR_TOKEN
```

### View Analysis Results

After successful analysis:
1. Open http://localhost:30900
2. Go to **Projects** > **userapi-k8s**
3. View:
   - **Overall Quality Gate** (Pass/Fail)
   - **Bugs**, **Vulnerabilities**, **Code Smells**
   - **Coverage** percentage
   - **Duplicated** code percentage
   - **Technical Debt** ratio

### Quality Gate Configuration

Configure quality rules in SonarQube:

1. **Go to**: http://localhost:30900/quality_gates
2. **Default Quality Gate**: **Sonar way**
3. **Customize thresholds**:
   - Coverage > 80%
   - New Bugs = 0
   - New Vulnerabilities = 0
   - New Code Smells < 5
   - New Duplicated Lines < 3%

### Continuous Integration

Add SonarQube analysis to your CI/CD pipeline:

```yaml
# GitHub Actions example
- name: SonarQube Analysis
  run: mvn clean verify sonar:sonar -Dsonar.projectKey=userapi-k8s -Dsonar.host.url=${{ secrets.SONAR_URL }} -Dsonar.login=${{ secrets.SONAR_TOKEN }}
```

### Getting Keycloak Token

To get a token for testing UserAPI:

```powershell
# Replace PASSWORD with your actual password
$TOKEN = (Invoke-RestMethod -Uri "http://localhost:30180/realms/userapi-realm/protocol/openid-connect/token" `
  -Method Post `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=userapi-client&client_secret=US9FWPsmElIYJHwHSQOHh9GRQm1yqXAw&username=admin&password=admin123").access_token

Write-Host "Token: $TOKEN"

# Use token in request
$headers = @{ Authorization = "Bearer $TOKEN" }
Invoke-RestMethod -Uri "http://localhost:30080/api/users" -Headers $headers
```

## Architecture

### Services Deployed

| Service | Type | Port | Purpose |
|---------|------|------|---------|
| **UserAPI** | Spring Boot | 8080 | REST API with JWT security |
| **PostgreSQL** | Database | 5432 | Persistent data storage |
| **Keycloak** | Auth Server | 8080 | OAuth2/OIDC authentication |
| **Adminer** | Web UI | 8080 | Database management tool |
| **SonarQube** | Code Quality | 9000 | Code analysis and metrics |

### Kubernetes Resources

**Deployments:**
- `userapi-deployment` - UserAPI application (1 replica)
- `userapi-keycloak` - Keycloak authentication (1 replica)
- `adminer` - Database admin UI (1 replica)
- `sonarqube` - Code quality analysis (1 replica)

**StatefulSets:**
- `userapi-postgres-0` - PostgreSQL database (1 replica, persistent storage)

**Services:**
- `userapi-service` & `userapi-service-nodeport` - UserAPI access
- `keycloak-service` & `keycloak-service-nodeport` - Keycloak access
- `postgres-service` - PostgreSQL internal access
- `adminer-service` & `adminer-service-nodeport` - Adminer access
- `sonarqube-service` & `sonarqube-service-nodeport` - SonarQube access

**Storage:**
- `userapi-postgres-pvc` - PostgreSQL data (20Gi)
- `sonarqube-data-pvc` - SonarQube data (5Gi)
- `sonarqube-logs-pvc` - SonarQube logs (2Gi)
- `sonarqube-extensions-pvc` - SonarQube extensions (1Gi)

## Configuration

### Environment Variables

**Database:**
```yaml
POSTGRES_HOST: postgres-service
POSTGRES_PORT: 5432
POSTGRES_DB: userapi_db
POSTGRES_USER: .env-kubernetes
POSTGRES_PASSWORD: .env-kubernetes
```

**Security:**
```yaml
JWT_ISSUER_URI: http://keycloak-service:8080/realms/userapi-realm
KEYCLOAK_URL: http://keycloak-service:8080
KEYCLOAK_CLIENT_ID: userapi-client
KEYCLOAK_CLIENT_SECRET: .env-kubernetes
```

## Monitoring and Debugging

### Check Pod Status
```powershell
kubectl get pods
kubectl get pods -l app=userapi
kubectl describe pod <pod-name>
```

### View Logs
```powershell
# All UserAPI logs
kubectl logs -l app=userapi -n default -f

# Keycloak logs
kubectl logs -l app=keycloak -n default -f

# Specific pod logs
kubectl logs <pod-name> -f
```

### Connect to Pod
```powershell
kubectl exec -it <pod-name> -- bash
kubectl exec -it postgres-0 -- psql -U userapi_user -d userapi_db
```

### Port Forward Manually
```powershell
kubectl port-forward svc/userapi-service-nodeport 30080:8080 --address 127.0.0.1
kubectl port-forward svc/postgres-service 5432:5432 --address 127.0.0.1
```

## Cleanup

To remove all services and resources:

```powershell
.\cleanup.bat
```

Or manually:
```powershell
kubectl delete -f k8s/
kubectl delete pvc --all
```

## Troubleshooting

### Pods Stuck in Pending
```powershell
# Check resource availability
kubectl describe nodes

# Check PVC status
kubectl get pvc
```

### Database Connection Failed
```powershell
# Check PostgreSQL is running
kubectl logs -l app=postgres -n default -f

# Test connection from UserAPI
kubectl exec <userapi-pod> -- nc -zv postgres-service 5432
```

### Port-Forward Not Working
```powershell
# Kill existing port-forwards
taskkill /F /FI "WINDOWTITLE eq UserAPI*"
taskkill /F /FI "WINDOWTITLE eq Keycloak*"

# Restart setup-complete.bat
.\setup-complete.bat
```

### Keycloak Token Issues
```powershell
# Check Keycloak is running
kubectl logs -l app=keycloak -n default -f

# Test token endpoint
curl http://localhost:30180/realms/userapi-realm/protocol/openid-connect/token
```

## Development Workflow

### Rebuild and Redeploy UserAPI
```powershell
# Build new image
docker build -t userapi:latest .

# Restart UserAPI pod
kubectl rollout restart deployment/userapi-deployment

# Watch rollout
kubectl rollout status deployment/userapi-deployment

# View new logs
kubectl logs -l app=userapi -n default -f
```

### Database Changes
```powershell
# Connect to database
kubectl exec -it postgres-0 -- psql -U userapi_user -d userapi_db

# Or use Adminer web UI
# http://localhost:30880
```

## Performance Tips

- **First Deploy**: 5-10 minutes (images pulled, databases initialized)
- **Subsequent Deploys**: 2-3 minutes (cached images)
- **Keycloak Startup**: 2-3 minutes (JVM warmup + DB migration)
- **SonarQube Startup**: 1-2 minutes (embedded Elasticsearch)

## Production Considerations

For production deployment, add:

1. **Resource Quotas** - Limit CPU/memory per namespace
2. **Network Policies** - Restrict pod-to-pod communication
3. **RBAC** - Fine-grained access control
4. **Monitoring** - Prometheus + Grafana stack
5. **Logging** - ELK or similar centralized logging
6. **SSL/TLS** - cert-manager for HTTPS
7. **Backup** - Automated PostgreSQL backups
8. **High Availability** - Multi-replica deployments
9. **Scaling** - HPA (Horizontal Pod Autoscaler)

## Next Steps

- [ ] Test UserAPI endpoints with Postman
- [ ] Configure SonarQube scanning
- [ ] Setup monitoring dashboard
- [ ] Add automated backups
- [ ] Implement CI/CD pipeline
- [ ] Scale to production cluster

## Support Scripts

Located in `.scripts/` directory:

- `setup-keycloak.ps1` - Create realm and configure authentication
- `setup-keycloak-k8s.ps1` - Alternative Keycloak setup
- `setup-keycloak-realm.ps1` - Realm-only configuration

Located in root directory:

- `deploy.bat` - Deploy all services
- `cleanup.bat` - Remove all resources
- `docker-desktop-images-working.bat` - Pre-pull Docker images
- `setup-complete.bat` - Start all port-forwards

## FAQ

**Q: Can I access services without port-forward?**
A: Yes, use NodePort directly: `kubectl port-forward svc/userapi-service-nodeport 30080:8080`

**Q: How do I scale services?**
A: `kubectl scale deployment userapi-deployment --replicas=3`

**Q: How do I backup the database?**
A: `kubectl exec postgres-0 -- pg_dump -U userapi_user userapi_db > backup.sql`

**Q: Can I use this setup for production?**
A: Not directly - add monitoring, logging, backups, and RBAC first.

**Q: What if I want to use a different Kubernetes cluster?**
A: The YAML files work with any Kubernetes cluster (1.24+), adjust StorageClass if needed.