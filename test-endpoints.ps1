# Test Stitch Backend API Endpoints
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Stitch Backend API Endpoints" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if server is running
Write-Host "[0] Checking if server is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/me" -Method GET -ErrorAction SilentlyContinue
    Write-Host "Server is running!" -ForegroundColor Green
} catch {
    Write-Host "Server is NOT running on port 8080. Please start it first." -ForegroundColor Red
    Write-Host "Run: mvn spring-boot:run" -ForegroundColor Yellow
    exit
}
Write-Host ""

# Test 1: Register Admin User
Write-Host "[1] Testing Registration - Admin User" -ForegroundColor Yellow
$adminRegisterBody = @{
    email = "admin@stitch.com"
    password = "admin123"
    fullName = "Admin User"
    phoneNumber = "1234567890"
    role = "ADMIN"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -Body $adminRegisterBody -ContentType "application/json"
    Write-Host "✓ Admin registered successfully" -ForegroundColor Green
    Write-Host "Token: $($response.token.Substring(0,20))..." -ForegroundColor Gray
    $adminToken = $response.token
} catch {
    Write-Host "✗ Admin registration failed (might already exist): $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Register Customer User
Write-Host "[2] Testing Registration - Customer User" -ForegroundColor Yellow
$customerRegisterBody = @{
    email = "customer@test.com"
    password = "customer123"
    fullName = "Test Customer"
    phoneNumber = "0987654321"
    role = "CUSTOMER"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -Body $customerRegisterBody -ContentType "application/json"
    Write-Host "✓ Customer registered successfully" -ForegroundColor Green
    Write-Host "Token: $($response.token.Substring(0,20))..." -ForegroundColor Gray
    $customerToken = $response.token
} catch {
    Write-Host "✗ Customer registration failed (might already exist): $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Login - Existing Customer
Write-Host "[3] Testing Login - Existing Customer (sankalyacus@gmail.com)" -ForegroundColor Yellow
$loginBody = @{
    email = "sankalyacus@gmail.com"
    password = "12345678"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "✓ Login successful" -ForegroundColor Green
    Write-Host "User: $($response.user.fullName) ($($response.user.role))" -ForegroundColor Gray
    Write-Host "Token: $($response.token.Substring(0,20))..." -ForegroundColor Gray
    $customerToken = $response.token
    $customerId = $response.user.id
} catch {
    Write-Host "✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Login - Admin (if exists)
Write-Host "[4] Testing Login - Admin" -ForegroundColor Yellow
$adminLoginBody = @{
    email = "admin@stitch.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $adminLoginBody -ContentType "application/json"
    Write-Host "✓ Admin login successful" -ForegroundColor Green
    Write-Host "User: $($response.user.fullName) ($($response.user.role))" -ForegroundColor Gray
    Write-Host "Token: $($response.token.Substring(0,20))..." -ForegroundColor Gray
    $adminToken = $response.token
} catch {
    Write-Host "✗ Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 5: Get current user info (Customer)
if ($customerToken) {
    Write-Host "[5] Testing GET /api/auth/me (Customer)" -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $customerToken"
    }
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method GET -Headers $headers
        Write-Host "✓ Auth check successful" -ForegroundColor Green
        Write-Host "User: $($response.fullName) - $($response.email) ($($response.role))" -ForegroundColor Gray
    } catch {
        Write-Host "✗ Auth check failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 6: Get all users (Admin)
if ($adminToken) {
    Write-Host "[6] Testing GET /api/admin/users (Admin)" -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $adminToken"
    }
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Method GET -Headers $headers
        Write-Host "✓ Get all users successful" -ForegroundColor Green
        Write-Host "Total users: $($response.Count)" -ForegroundColor Gray
        foreach ($user in $response) {
            Write-Host "  - $($user.fullName) ($($user.role)) - $($user.email)" -ForegroundColor Gray
        }
    } catch {
        Write-Host "✗ Get all users failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 7: Get all customers (Admin)
if ($adminToken) {
    Write-Host "[7] Testing GET /api/admin/customers (Admin)" -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $adminToken"
    }
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/customers" -Method GET -Headers $headers
        Write-Host "✓ Get all customers successful" -ForegroundColor Green
        Write-Host "Total customers: $($response.Count)" -ForegroundColor Gray
        foreach ($customer in $response) {
            Write-Host "  - $($customer.fullName) - $($customer.email)" -ForegroundColor Gray
        }
    } catch {
        Write-Host "✗ Get all customers failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 8: Get my appointments (Customer)
if ($customerToken) {
    Write-Host "[8] Testing GET /api/appointments/customer/my-appointments (Customer)" -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $customerToken"
    }
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments/customer/my-appointments" -Method GET -Headers $headers
        Write-Host "✓ Get my appointments successful" -ForegroundColor Green
        Write-Host "Total appointments: $($response.Count)" -ForegroundColor Gray
        if ($response.Count -gt 0) {
            foreach ($appt in $response) {
                Write-Host "  - ID: $($appt.id), Status: $($appt.status), Deadline: $($appt.deadline)" -ForegroundColor Gray
            }
        } else {
            Write-Host "  No appointments found" -ForegroundColor Gray
        }
    } catch {
        Write-Host "✗ Get my appointments failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 9: Get all appointments (Admin)
if ($adminToken) {
    Write-Host "[9] Testing GET /api/appointments/admin/all (Admin)" -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $adminToken"
    }
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments/admin/all" -Method GET -Headers $headers
        Write-Host "✓ Get all appointments successful" -ForegroundColor Green
        Write-Host "Total appointments: $($response.Count)" -ForegroundColor Gray
        if ($response.Count -gt 0) {
            foreach ($appt in $response) {
                Write-Host "  - ID: $($appt.id), Customer: $($appt.customerName), Status: $($appt.status)" -ForegroundColor Gray
            }
        } else {
            Write-Host "  No appointments found" -ForegroundColor Gray
        }
    } catch {
        Write-Host "✗ Get all appointments failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 10: Create appointment (Customer) - Simple JSON test
if ($customerToken) {
    Write-Host "[10] Testing POST /api/appointments/customer (Customer)" -ForegroundColor Yellow
    Write-Host "Note: This endpoint expects multipart/form-data. Testing with JSON body only..." -ForegroundColor Gray
    $headers = @{
        "Authorization" = "Bearer $customerToken"
    }
    $appointmentBody = @{
        customerName = "Test Customer"
        age = 25
        phoneNumber = "0987654321"
        deadline = "2025-12-15"
        notes = "Test appointment from PowerShell"
    } | ConvertTo-Json

    try {
        # Note: This will likely fail because the endpoint expects multipart/form-data
        # But we're testing if the endpoint is reachable
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments/customer" -Method POST -Headers $headers -Body $appointmentBody -ContentType "application/json"
        Write-Host "✓ Create appointment successful" -ForegroundColor Green
        Write-Host "Appointment ID: $($response.id)" -ForegroundColor Gray
    } catch {
        if ($_.Exception.Message -like "*415*" -or $_.Exception.Message -like "*Unsupported Media Type*") {
            Write-Host "⚠ Endpoint reachable but requires multipart/form-data (expected)" -ForegroundColor Yellow
        } else {
            Write-Host "✗ Create appointment failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Endpoint Testing Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Customer Token (for manual testing):" -ForegroundColor Yellow
Write-Host $customerToken -ForegroundColor Gray
Write-Host ""
Write-Host "Admin Token (for manual testing):" -ForegroundColor Yellow
Write-Host $adminToken -ForegroundColor Gray

