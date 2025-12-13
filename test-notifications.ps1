# Notification System Testing Script
# PowerShell script to test all notification triggers

$baseUrl = "http://localhost:8080"

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Notification System Test Script" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Function to make HTTP requests
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Token = "",
        [object]$Body = $null,
        [string]$ContentType = "application/json"
    )

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    if ($ContentType) {
        $headers["Content-Type"] = $ContentType
    }

    $params = @{
        Uri = "$baseUrl$Endpoint"
        Method = $Method
        Headers = $headers
    }

    if ($Body) {
        if ($ContentType -eq "application/json") {
            $params["Body"] = ($Body | ConvertTo-Json)
        } else {
            $params["Body"] = $Body
        }
    }

    try {
        $response = Invoke-RestMethod @params
        return $response
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
        return $null
    }
}

# Test 1: Login as Admin
Write-Host "1. Logging in as Admin..." -ForegroundColor Yellow
$adminLogin = Invoke-ApiRequest -Method POST -Endpoint "/api/auth/login" -Body @{
    email = "admin@stitch.com"
    password = "admin123"
}

if ($adminLogin) {
    $adminToken = $adminLogin.token
    Write-Host "   ✓ Admin logged in successfully" -ForegroundColor Green
    Write-Host "   Token: $($adminToken.Substring(0, 20))..." -ForegroundColor Gray
} else {
    Write-Host "   ✗ Admin login failed" -ForegroundColor Red
    exit
}

Write-Host ""

# Test 2: Login as Customer (or create one if doesn't exist)
Write-Host "2. Logging in as Customer..." -ForegroundColor Yellow
$customerLogin = Invoke-ApiRequest -Method POST -Endpoint "/api/auth/login" -Body @{
    email = "customer@example.com"
    password = "password123"
}

if ($customerLogin) {
    $customerToken = $customerLogin.token
    Write-Host "   ✓ Customer logged in successfully" -ForegroundColor Green
    Write-Host "   Token: $($customerToken.Substring(0, 20))..." -ForegroundColor Gray
} else {
    Write-Host "   Customer doesn't exist, creating one..." -ForegroundColor Yellow
    $newCustomer = Invoke-ApiRequest -Method POST -Endpoint "/api/auth/register" -Body @{
        email = "customer@example.com"
        password = "password123"
        fullName = "Test Customer"
        phoneNumber = "1234567890"
    }

    if ($newCustomer) {
        $customerToken = $newCustomer.token
        Write-Host "   ✓ Customer created successfully" -ForegroundColor Green
        Write-Host "   ⚡ This should have triggered 'New Customer Registered' notification for admin!" -ForegroundColor Magenta
    } else {
        Write-Host "   ✗ Customer creation failed" -ForegroundColor Red
        exit
    }
}

Write-Host ""

# Test 3: Check Admin Notifications
Write-Host "3. Checking Admin Notifications..." -ForegroundColor Yellow
$adminNotifications = Invoke-ApiRequest -Method GET -Endpoint "/api/notifications" -Token $adminToken

if ($adminNotifications) {
    Write-Host "   ✓ Admin has $($adminNotifications.Count) notification(s)" -ForegroundColor Green
    $unreadCount = ($adminNotifications | Where-Object { -not $_.isRead }).Count
    Write-Host "   📬 Unread: $unreadCount" -ForegroundColor Cyan

    if ($adminNotifications.Count -gt 0) {
        Write-Host "   Latest notifications:" -ForegroundColor Gray
        $adminNotifications | Select-Object -First 3 | ForEach-Object {
            $readStatus = if ($_.isRead) { "Read" } else { "Unread" }
            Write-Host "      • [$($_.type)] $($_.title) - $readStatus" -ForegroundColor Gray
        }
    }
}

Write-Host ""

# Test 4: Customer Creates Appointment
Write-Host "4. Customer Creating Appointment..." -ForegroundColor Yellow
Write-Host "   ⚡ This should trigger 'New Appointment' notification for admin!" -ForegroundColor Magenta

$appointmentData = @{
    customerName = "Test Customer"
    age = 25
    phoneNumber = "1234567890"
    deadline = "2025-12-31"
    notes = "Test appointment for notification system"
}

# Note: For multipart/form-data with files, you'd need to use different approach
# For now, we'll just demonstrate the JSON part
Write-Host "   (In real scenario, you'd upload with images via multipart/form-data)" -ForegroundColor Gray

Write-Host ""

# Test 5: Customer Updates Profile
Write-Host "5. Customer Updating Profile..." -ForegroundColor Yellow
Write-Host "   ⚡ This should trigger 'Profile Updated' notification for admin!" -ForegroundColor Magenta

$profileUpdate = Invoke-ApiRequest -Method PUT -Endpoint "/api/auth/me" -Token $customerToken -Body @{
    fullName = "Updated Test Customer"
    phoneNumber = "9876543210"
}

if ($profileUpdate) {
    Write-Host "   ✓ Profile updated successfully" -ForegroundColor Green
}

Write-Host ""

# Test 6: Register Another Customer (to test notification)
Write-Host "6. Registering Another New Customer..." -ForegroundColor Yellow
Write-Host "   ⚡ This should trigger 'New Customer Registered' notification for admin!" -ForegroundColor Magenta

$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$newCustomer2 = Invoke-ApiRequest -Method POST -Endpoint "/api/auth/register" -Body @{
    email = "customer$timestamp@example.com"
    password = "password123"
    fullName = "New Customer $timestamp"
    phoneNumber = "5555555555"
}

if ($newCustomer2) {
    Write-Host "   ✓ New customer registered: customer$timestamp@example.com" -ForegroundColor Green
}

Write-Host ""

# Test 7: Check Admin Notifications Again
Write-Host "7. Checking Admin Notifications Again..." -ForegroundColor Yellow
$adminNotificationsNew = Invoke-ApiRequest -Method GET -Endpoint "/api/notifications/unread" -Token $adminToken

if ($adminNotificationsNew) {
    Write-Host "   ✓ Admin has $($adminNotificationsNew.Count) UNREAD notification(s)" -ForegroundColor Green

    if ($adminNotificationsNew.Count -gt 0) {
        Write-Host "   📬 Recent unread notifications:" -ForegroundColor Cyan
        $adminNotificationsNew | Select-Object -First 5 | ForEach-Object {
            Write-Host "      • [$($_.type)] $($_.title)" -ForegroundColor Yellow
            Write-Host "        $($_.message)" -ForegroundColor Gray
            Write-Host ""
        }
    }
}

Write-Host ""

# Test 8: Check Customer Notifications
Write-Host "8. Checking Customer Notifications..." -ForegroundColor Yellow
$customerNotifications = Invoke-ApiRequest -Method GET -Endpoint "/api/notifications" -Token $customerToken

if ($customerNotifications) {
    Write-Host "   ✓ Customer has $($customerNotifications.Count) notification(s)" -ForegroundColor Green

    if ($customerNotifications.Count -gt 0) {
        Write-Host "   📬 Customer notifications:" -ForegroundColor Cyan
        $customerNotifications | ForEach-Object {
            $readStatus = if ($_.isRead) { "Read" } else { "Unread" }
            Write-Host "      • [$($_.type)] $($_.title) - $readStatus" -ForegroundColor Gray
        }
    }
}

Write-Host ""

# Test 9: Mark Notifications as Read
if ($adminNotificationsNew -and $adminNotificationsNew.Count -gt 0) {
    Write-Host "9. Marking First Admin Notification as Read..." -ForegroundColor Yellow
    $firstNotificationId = $adminNotificationsNew[0].id
    $markRead = Invoke-ApiRequest -Method PUT -Endpoint "/api/notifications/$firstNotificationId/read" -Token $adminToken

    if ($markRead -ne $null) {
        Write-Host "   ✓ Notification marked as read" -ForegroundColor Green
    }
}

Write-Host ""

# Summary
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Actions that should have triggered notifications:" -ForegroundColor Green
Write-Host "   1. New customer registration → Admin notification" -ForegroundColor White
Write-Host "   2. Customer profile update → Admin notification" -ForegroundColor White
Write-Host "   3. Another customer registration → Admin notification" -ForegroundColor White
Write-Host ""
Write-Host "📝 To test more notifications:" -ForegroundColor Yellow
Write-Host "   • Create/update appointments (customer) → Admin gets notified" -ForegroundColor White
Write-Host "   • Upload gallery photos (admin) → All customers get notified" -ForegroundColor White
Write-Host "   • Like gallery photos (customer) → Admin gets notified" -ForegroundColor White
Write-Host "   • Approve/decline appointments (admin) → Customer gets notified" -ForegroundColor White
Write-Host "   • Change appointment status (admin) → Customer gets notified" -ForegroundColor White
Write-Host ""
Write-Host "📖 See NOTIFICATION_TESTING_GUIDE.md for complete API documentation" -ForegroundColor Cyan
Write-Host ""

