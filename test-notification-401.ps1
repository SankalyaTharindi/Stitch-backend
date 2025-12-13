# Quick Test Script for Notification 401 Issue
# Run this to diagnose the problem

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Notification Endpoint Test" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

# Test 1: Check if server is running
Write-Host "1. Testing if server is running..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method OPTIONS -UseBasicParsing -TimeoutSec 5
    Write-Host "   ✓ Server is running" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Server is NOT running or not accessible" -ForegroundColor Red
    Write-Host "   Please start the server with: mvn spring-boot:run" -ForegroundColor Yellow
    exit
}

Write-Host ""

# Test 2: Login as Admin
Write-Host "2. Logging in as Admin..." -ForegroundColor Yellow

$loginBody = @{
    email = "admin@stitch.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    $token = $loginResponse.token
    Write-Host "   ✓ Login successful" -ForegroundColor Green
    Write-Host "   User: $($loginResponse.user.email)" -ForegroundColor Gray
    Write-Host "   Role: $($loginResponse.user.role)" -ForegroundColor Gray
    Write-Host "   Token (first 50 chars): $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Login failed" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "   Make sure admin user exists. You may need to create it first." -ForegroundColor Yellow
    exit
}

Write-Host ""

# Test 3: Verify token with /api/auth/me
Write-Host "3. Verifying token with /api/auth/me..." -ForegroundColor Yellow

$headers = @{
    "Authorization" = "Bearer $token"
}

try {
    $meResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" `
        -Method GET `
        -Headers $headers

    Write-Host "   ✓ Token is valid" -ForegroundColor Green
    Write-Host "   Authenticated as: $($meResponse.email)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Token validation failed" -ForegroundColor Red
    Write-Host "   Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "   This suggests a problem with JWT token generation or validation." -ForegroundColor Yellow
    exit
}

Write-Host ""

# Test 4: Try to access notifications endpoint
Write-Host "4. Testing /api/notifications endpoint..." -ForegroundColor Yellow

try {
    $notificationsResponse = Invoke-RestMethod -Uri "$baseUrl/api/notifications" `
        -Method GET `
        -Headers $headers

    Write-Host "   ✓ Notifications endpoint accessible!" -ForegroundColor Green
    Write-Host "   Found $($notificationsResponse.Count) notification(s)" -ForegroundColor Green

    if ($notificationsResponse.Count -gt 0) {
        Write-Host ""
        Write-Host "   Latest notifications:" -ForegroundColor Cyan
        $notificationsResponse | Select-Object -First 3 | ForEach-Object {
            $readStatus = if ($_.isRead) { "Read" } else { "Unread" }
            Write-Host "      • [$($_.type)] $($_.title) - $readStatus" -ForegroundColor Gray
        }
    } else {
        Write-Host "   No notifications yet. Try triggering some actions:" -ForegroundColor Yellow
        Write-Host "      - Register a new customer" -ForegroundColor Gray
        Write-Host "      - Update a customer profile" -ForegroundColor Gray
        Write-Host "      - Create an appointment" -ForegroundColor Gray
    }
} catch {
    Write-Host "   ✗ Failed to access notifications endpoint" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""

    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "   DIAGNOSIS: 401 Unauthorized Error" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   Possible causes:" -ForegroundColor Yellow
        Write-Host "      1. The token might be malformed" -ForegroundColor White
        Write-Host "      2. Security filter might be rejecting the token" -ForegroundColor White
        Write-Host "      3. The endpoint might require additional permissions" -ForegroundColor White
        Write-Host ""
        Write-Host "   Debug steps:" -ForegroundColor Yellow
        Write-Host "      1. Check the Spring Boot console logs for JWT errors" -ForegroundColor White
        Write-Host "      2. Look for 'JWT authentication successful' or 'JWT token is NOT VALID'" -ForegroundColor White
        Write-Host "      3. Verify the Authorization header format" -ForegroundColor White
        Write-Host ""
        Write-Host "   Testing with curl:" -ForegroundColor Cyan
        Write-Host "      curl -v -H `"Authorization: Bearer $token`" http://localhost:8080/api/notifications" -ForegroundColor Gray
    }

    exit
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "✅ ALL TESTS PASSED!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Your notification system is working correctly." -ForegroundColor Green
Write-Host "You can now test triggering notifications by:" -ForegroundColor White
Write-Host "  • Registering new customers" -ForegroundColor Gray
Write-Host "  • Updating customer profiles" -ForegroundColor Gray
Write-Host "  • Creating/updating appointments" -ForegroundColor Gray
Write-Host "  • Liking gallery photos" -ForegroundColor Gray
Write-Host ""

