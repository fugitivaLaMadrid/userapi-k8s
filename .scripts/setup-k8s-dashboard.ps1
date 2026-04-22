param(
    [string]$Namespace = "kubernetes-dashboard"
)

function Setup-KubernetesDashboard {
    Write-Host "Setting up Kubernetes Dashboard access..."
    Write-Host ""

    # Create service account for dashboard admin
    Write-Host "Creating dashboard admin service account..."
    kubectl create serviceaccount dashboard-admin -n $Namespace -o yaml 2>$null | kubectl apply -f - | Out-Null

    # Create cluster role binding
    kubectl create clusterrolebinding dashboard-admin-binding `
        --clusterrole=cluster-admin `
        --serviceaccount=$Namespace`:dashboard-admin `
        -o yaml 2>$null | kubectl apply -f - | Out-Null

    # Wait for dashboard to be ready
    Write-Host "Waiting for Kubernetes Dashboard to be ready..."
    kubectl wait --for=condition=ready pod -l k8s-app=kubernetes-dashboard -n $Namespace --timeout=300s 2>$null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "Kubernetes Dashboard is ready!"
    } else {
        Write-Host "Dashboard may not be fully ready, but continuing..."
    }

    Write-Host ""

    # Get the token
    Write-Host "Getting dashboard access token..."
    Write-Host ""

    $token = kubectl create token dashboard-admin -n $Namespace 2>$null

    if (-not $token) {
        Write-Host "ERROR: Could not retrieve dashboard token"
        return
    }

    # Copy token to clipboard
    $token | Set-Clipboard
    
    # Save token to .env-kubernetes file
    $envFilePath = ".\.env-kubernetes"
    if (Test-Path $envFilePath) {
        # Read the current file content
        $content = Get-Content $envFilePath -Raw
        
        # Replace the KUBERNETES_DASHBOARD_TOKEN line
        $pattern = '(?m)^(KUBERNETES_DASHBOARD_TOKEN=).*'
        $replacement = "KUBERNETES_DASHBOARD_TOKEN=$token"
        
        if ($content -match $pattern) {
            # Update existing token
            $content = $content -replace $pattern, $replacement
        } else {
            # Add token at the end if not found
            $content = $content.TrimEnd() + "`nKUBERNETES_DASHBOARD_TOKEN=$token`n"
        }
        
        # Write back to file
        $content | Out-File -FilePath $envFilePath -Encoding UTF8 -NoNewline
        Write-Host "Token saved to .env-kubernetes file" -ForegroundColor Green
    } else {
        Write-Host "WARNING: .env-kubernetes file not found" -ForegroundColor Yellow
    }

    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Kubernetes Dashboard Setup Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access Kubernetes Dashboard:" -ForegroundColor Cyan
    Write-Host "  URL: https://localhost:30443" -ForegroundColor White
    Write-Host ""
    Write-Host "Login with Token:" -ForegroundColor Cyan

    # Safely display token preview
    if ($token -and $token.Length -gt 50) {
        Write-Host "  Token: $($token.Substring(0, 50))..." -ForegroundColor White
    } elseif ($token) {
        Write-Host "  Token: $token" -ForegroundColor White
    } else {
        Write-Host "  Token: (empty or not retrieved)" -ForegroundColor Red
    }

    Write-Host "  (Full token copied to clipboard)" -ForegroundColor White
    Write-Host ""
    Write-Host "Instructions:" -ForegroundColor Yellow
    Write-Host "1. Open https://localhost:30443 in your browser" -ForegroundColor White
    Write-Host "2. Accept the security warning (self-signed certificate)" -ForegroundColor White
    Write-Host "3. Select 'Token' authentication method" -ForegroundColor White
    Write-Host "4. Paste the token (Ctrl+V)" -ForegroundColor White
    Write-Host "5. Click 'Sign In'" -ForegroundColor White
    Write-Host ""
    Write-Host "Token expires in 24 hours" -ForegroundColor Gray
    Write-Host ""
}

Setup-KubernetesDashboard