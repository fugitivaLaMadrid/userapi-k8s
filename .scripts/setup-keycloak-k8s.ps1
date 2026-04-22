#################################################################################
#               KEYCLOAK SETUP SCRIPT FOR USERAPI APPLICATION                   #
#################################################################################

# This script sets up Keycloak with realm, client, and user for the userapi application

$ErrorActionPreference = "Continue"

# Display title in console
Write-Host ""
Write-Host "#################################################################################" -ForegroundColor Cyan
Write-Host "#               KEYCLOAK SETUP SCRIPT FOR USERAPI APPLICATION                   #" -ForegroundColor Cyan
Write-Host "#################################################################################" -ForegroundColor Cyan
Write-Host ""

# Function to write status messages with colors
function Write-Status {
    param(
        [string]$Message,
        [string]$Type = "Info"
    )

    switch ($Type) {
        "Success" { Write-Host $Message -ForegroundColor Green }
        "Error"   { Write-Host $Message -ForegroundColor Red }
        "Warning" { Write-Host $Message -ForegroundColor Yellow }
        "Wait"    { Write-Host $Message -ForegroundColor Cyan }
        default   { Write-Host $Message -ForegroundColor White }
    }
}

# Configuration
$KEYCLOAK_URL = "http://localhost:30180"
$REALM_NAME = "userapi-realm"
$CLIENT_ID = "userapi-client"
$CLIENT_SECRET = "US9FWPsmElIYJHwHSQOHh9GRQm1yqXAw"
$USERNAME = "admin"
$PASSWORD = "admin123"
$ADMIN_ROLE = "ADMIN"

# Step 1: Wait for Keycloak to be ready
Write-Status "STEP 1: Waiting for Keycloak to respond..." "Wait"
$maxRetries = 60
$retryCount = 0
$keycloakReady = $false

while ($retryCount -lt $maxRetries -and -not $keycloakReady) {
    try {
        # Check token endpoint instead of admin realms endpoint (which requires auth)
        $response = Invoke-WebRequest -Uri "$KEYCLOAK_URL/realms/master/.well-known/openid-configuration" -Method GET -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $keycloakReady = $true
            Write-Status "Keycloak is ready!" "Success"
        }
    } catch {
        $retryCount++
        if ($retryCount % 10 -eq 0) {
            Write-Status "Still waiting for Keycloak... ($retryCount/$maxRetries)" "Wait"
        }
        Start-Sleep -Seconds 1
    }
}

if (-not $keycloakReady) {
    Write-Status "Keycloak did not respond after $maxRetries seconds. Exiting." "Error"
    exit 1
}

# Step 2: Get Admin Token
try {
    Write-Status "STEP 2: Getting admin token from Keycloak..." "Wait"
    $tokenResponse = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" `
        -Method POST `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
        "grant_type" = "password"
        "client_id" = "admin-cli"
        "username" = "admin"
        "password" = "admin123"
    } `
        -ErrorAction Stop

    $adminToken = $tokenResponse.access_token
    Write-Status "Admin token obtained successfully" "Success"
} catch {
    Write-Status "Failed to obtain admin token: $_" "Error"
    exit 1
}

# Headers for authenticated requests
$headers = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

# Step 3: Create Realm
try {
    Write-Status "STEP 3: Creating realm '$REALM_NAME'..." "Wait"
    $realmPayload = @{
        realm = $REALM_NAME
        enabled = $true
        displayName = "UserAPI Realm"
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms" `
        -Method POST `
        -Headers $headers `
        -Body $realmPayload `
        -ErrorAction Stop | Out-Null

    Write-Status "Realm created successfully" "Success"
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Status "Realm already exists" "Success"
    }
    else {
        Write-Status "Error creating realm: $_" "Error"
        exit 1
    }
}

# Step 4: Create Client
try {
    Write-Status "STEP 4: Creating client '$CLIENT_ID'..." "Wait"
    $clientPayload = @{
        clientId = $CLIENT_ID
        secret = $CLIENT_SECRET
        enabled = $true
        clientAuthenticatorType = "client-secret"
        redirectUris = @("http://localhost:8080/*", "http://localhost:3000/*")
        webOrigins = @("+")
        standardFlowEnabled = $true
        directAccessGrantsEnabled = $true
        serviceAccountsEnabled = $true
        publicClient = $false
        protocolMappers = @()
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" `
        -Method POST `
        -Headers $headers `
        -Body $clientPayload `
        -ErrorAction Stop | Out-Null

    # Get the client UUID after creation (API doesn't return it in response)
    $createdClient = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients?clientId=$CLIENT_ID" `
        -Method GET `
        -Headers $headers `
        -ErrorAction Stop

    $CLIENT_UUID = $createdClient[0].id
    Write-Status "Client created successfully" "Success"
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Status "Client already exists, fetching UUID..." "Info"
        try {
            $existingClient = Invoke-RestMethod `
                -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients?clientId=$CLIENT_ID" `
                -Method GET `
                -Headers $headers `
                -ErrorAction Stop

            $CLIENT_UUID = $existingClient[0].id
            Write-Status "Client UUID retrieved" "Success"
        } catch {
            Write-Status "Failed to get existing client: $_" "Error"
            exit 1
        }
    }
    else {
        Write-Status "Error creating client: $_" "Error"
        exit 1
    }
}

try {
    Write-Status "STEP 4.5: Ensuring client is enabled..." "Wait"
    $enablePayload = @{
        enabled = $true
        standardFlowEnabled = $true
        directAccessGrantsEnabled = $true
        serviceAccountsEnabled = $true
    } | ConvertTo-Json


    $enableHeaders = @{
        "Authorization" = "Bearer $adminToken"
        "Content-Type" = "application/json"
    }


    Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients/$CLIENT_UUID" `
        -Method PUT `
        -Headers $enableHeaders `
        -Body $enablePayload `
        -ErrorAction Stop

    Write-Status "Client enabled successfully" "Success"
} catch {
    Write-Status "Error enabling client: $_" "Error"
    exit 1
}

# Step 5: Get Client Secret
try {
    Write-Status "STEP 5: Getting client secret..." "Wait"
    $secretResponse = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients/$CLIENT_UUID/client-secret" `
        -Method GET `
        -Headers $headers `
        -ErrorAction Stop

    $actualSecret = $secretResponse.value
    Write-Status "Client secret obtained" "Success"
} catch {
    Write-Status "Error getting client secret: $_" "Error"
    Write-Status "Using configured secret as fallback" "Info"
    $actualSecret = $CLIENT_SECRET
}

# Step 6: Create ADMIN Role
try {
    Write-Status "STEP 6: Creating ADMIN role..." "Wait"
    $rolePayload = @{
        name = $ADMIN_ROLE
        description = "Administrator role for userapi"
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" `
        -Method POST `
        -Headers $headers `
        -Body $rolePayload `
        -ErrorAction Stop | Out-Null

    Write-Status "ADMIN role created" "Success"
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Status "ADMIN role already exists" "Success"
    }
    else {
        Write-Status "Error creating role: $_" "Error"
        exit 1
    }
}

# Step 7: Create User
try {
    Write-Status "STEP 7: Creating user '$USERNAME'..." "Wait"
    $userPayload = @{
        username = $USERNAME
        email = "admin@userapi.local"
        firstName = "Admin"
        lastName = "User"
        enabled = $true
        emailVerified = $true
    } | ConvertTo-Json

    $userResponse = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" `
        -Method POST `
        -Headers $headers `
        -Body $userPayload `
        -ErrorAction Stop

    Write-Status "User created successfully" "Success"
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Status "User already exists" "Success"
    }
    else {
        Write-Status "Error creating user: $_" "Error"
        exit 1
    }
}

# Step 8: Find User ID
try {
    Write-Status "STEP 8: Finding user ID..." "Wait"
    $userList = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=$USERNAME" `
        -Method GET `
        -Headers $headers `
        -ErrorAction Stop

    $USER_ID = $userList[0].id
    Write-Status "User found: $USER_ID" "Success"
} catch {
    Write-Status "Error finding user: $_" "Error"
    exit 1
}

# Step 9: Set User Password
try {
    Write-Status "STEP 9: Setting user password..." "Wait"
    $passwordPayload = @{
        type = "password"
        value = $PASSWORD
        temporary = $false
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/reset-password" `
        -Method PUT `
        -Headers $headers `
        -Body $passwordPayload `
        -ErrorAction Stop | Out-Null

    Write-Status "Password set successfully" "Success"
} catch {
    Write-Status "Error setting password: $_" "Error"
    exit 1
}

# Step 10: Get ADMIN Role Details
try {
    Write-Status "STEP 10: Getting ADMIN role details..." "Wait"
    $roleList = Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles?search=$ADMIN_ROLE" `
        -Method GET `
        -Headers $headers `
        -ErrorAction Stop

    $ADMIN_ROLE_ID = $roleList[0].id
    Write-Status "ADMIN role found" "Success"
} catch {
    Write-Status "Error getting role details: $_" "Error"
    exit 1
}

# Step 11: Assign ADMIN Role to User
try {
    Write-Status "STEP 11: Assigning ADMIN role to user..." "Wait"
    $roleAssignPayload = "[{`"id`":`"$ADMIN_ROLE_ID`",`"name`":`"$ADMIN_ROLE`"}]"

    Invoke-RestMethod `
        -Uri "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" `
        -Method POST `
        -Headers $headers `
        -Body $roleAssignPayload `
        -ErrorAction Stop | Out-Null

    Write-Status "ADMIN role assigned successfully" "Success"
} catch {
    Write-Status "Error assigning role: $_" "Error"
    exit 1
}

# Success Summary
Write-Status "=== KEYCLOAK SETUP COMPLETE ===" "Success"
Write-Status "" "Info"
Write-Status "Configuration Details:" "Info"
Write-Status "  Keycloak URL: $KEYCLOAK_URL" "Info"
Write-Status "  Realm: $REALM_NAME" "Info"
Write-Status "  Client ID: $CLIENT_ID" "Info"
Write-Status "  Client Secret: $CLIENT_SECRET" "Info"
Write-Status "  Admin User: $USERNAME" "Info"
Write-Status "  Admin Password: $PASSWORD" "Info"
Write-Status "  Admin Role: $ADMIN_ROLE" "Info"
Write-Status "" "Info"
Write-Status "You can now use these credentials to configure your Spring Boot application!" "Success"
Write-Status "=== SETUP COMPLETE ===" "Success"
