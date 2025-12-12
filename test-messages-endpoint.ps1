# Test script for messages endpoint
# This script will help debug the 401 error

$baseUrl = "http://localhost:8080"

# Step 1: Login as admin to get token
Write-Host "Step 1: Logging in as admin..." -ForegroundColor Cyan
$loginBody = @{
    email = "admin@stitch.com"
    password = "admin123"  # Adjust if different
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.token
    Write-Host "✓ Login successful! Token received." -ForegroundColor Green
    Write-Host "Token (first 50 chars): $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ Login failed: $_" -ForegroundColor Red
    Write-Host "Please make sure:" -ForegroundColor Yellow
    Write-Host "  1. Backend is running (mvn spring-boot:run)" -ForegroundColor Yellow
    Write-Host "  2. Admin credentials are correct" -ForegroundColor Yellow
    exit 1
}

# Step 2: Test /api/auth/me endpoint to verify token works
Write-Host "`nStep 2: Testing authentication with /api/auth/me..." -ForegroundColor Cyan
$headers = @{
    Authorization = "Bearer $token"
}

try {
    $meResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method GET -Headers $headers
    Write-Host "✓ Auth verification successful! User: $($meResponse.email) Role: $($meResponse.role)" -ForegroundColor Green
} catch {
    Write-Host "✗ Auth verification failed: $_" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    exit 1
}

# Step 3: Test /api/messages/customers endpoint
Write-Host "`nStep 3: Testing /api/messages/customers endpoint..." -ForegroundColor Cyan
try {
    $customersResponse = Invoke-RestMethod -Uri "$baseUrl/api/messages/customers" -Method GET -Headers $headers
    Write-Host "✓ SUCCESS! Endpoint returned:" -ForegroundColor Green
    Write-Host ($customersResponse | ConvertTo-Json -Depth 3) -ForegroundColor Gray
} catch {
    Write-Host "✗ Request failed!" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    Write-Host "Error: $_" -ForegroundColor Red

    # Try to get more details
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody" -ForegroundColor Yellow
    }

    Write-Host "`nPlease check the backend logs for detailed error messages." -ForegroundColor Yellow
    exit 1
}

Write-Host "`n✓ All tests passed!" -ForegroundColor Green

