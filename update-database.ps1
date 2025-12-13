# Update Notification Types in Database
# This script updates the database constraint to support new notification types

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Notification Types Database Update" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check if application.yml exists to get database connection info
$appYmlPath = "src\main\resources\application.yml"

if (-not (Test-Path $appYmlPath)) {
    Write-Host "Error: application.yml not found at $appYmlPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run this script from the project root directory." -ForegroundColor Yellow
    exit 1
}

Write-Host "Reading database configuration from application.yml..." -ForegroundColor Yellow

# Read application.yml to get database connection details
$appYml = Get-Content $appYmlPath -Raw

# Extract database connection details (basic parsing)
if ($appYml -match 'url:\s*jdbc:postgresql://([^:]+):(\d+)/(\w+)') {
    $dbHost = $matches[1]
    $dbPort = $matches[2]
    $dbName = $matches[3]
} else {
    Write-Host "Could not parse database URL from application.yml" -ForegroundColor Red
    Write-Host "Please update the database manually using update-notification-types.sql" -ForegroundColor Yellow
    exit 1
}

if ($appYml -match 'username:\s*(\w+)') {
    $dbUser = $matches[1]
} else {
    $dbUser = "postgres"
}

if ($appYml -match 'password:\s*(.+)') {
    $dbPassword = $matches[1].Trim()
} else {
    $dbPassword = ""
}

Write-Host "Database: $dbName" -ForegroundColor Gray
Write-Host "Host: $dbHost:$dbPort" -ForegroundColor Gray
Write-Host "User: $dbUser" -ForegroundColor Gray
Write-Host ""

# Check if psql is available
try {
    $psqlVersion = & psql --version 2>&1
    Write-Host "PostgreSQL client found: $psqlVersion" -ForegroundColor Green
} catch {
    Write-Host "Error: PostgreSQL client (psql) not found in PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install PostgreSQL client or run the SQL script manually:" -ForegroundColor Yellow
    Write-Host "  1. Open pgAdmin or your PostgreSQL client" -ForegroundColor White
    Write-Host "  2. Connect to database: $dbName" -ForegroundColor White
    Write-Host "  3. Run the SQL from: update-notification-types.sql" -ForegroundColor White
    Write-Host ""
    Write-Host "SQL Script Location: $(Join-Path $PWD 'update-notification-types.sql')" -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "Updating notification type constraint..." -ForegroundColor Yellow

# Set PostgreSQL password environment variable
$env:PGPASSWORD = $dbPassword

# Execute the SQL script
$sqlFile = "update-notification-types.sql"

try {
    $result = & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $sqlFile 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Database constraint updated successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "The following notification types are now supported:" -ForegroundColor Cyan
        Write-Host "  • APPOINTMENT_BOOKED" -ForegroundColor White
        Write-Host "  • APPOINTMENT_APPROVED" -ForegroundColor White
        Write-Host "  • APPOINTMENT_DECLINED" -ForegroundColor White
        Write-Host "  • MEASUREMENT_REMINDER" -ForegroundColor White
        Write-Host "  • JACKET_READY" -ForegroundColor White
        Write-Host "  • PAYMENT_REMINDER" -ForegroundColor White
        Write-Host "  • GALLERY_PHOTO_LIKED ⭐ NEW" -ForegroundColor Green
        Write-Host "  • GALLERY_PHOTO_UPLOADED ⭐ NEW" -ForegroundColor Green
        Write-Host "  • CUSTOMER_REGISTERED ⭐ NEW" -ForegroundColor Green
        Write-Host "  • CUSTOMER_MILESTONE ⭐ NEW" -ForegroundColor Green
        Write-Host "  • PROFILE_UPDATED ⭐ NEW" -ForegroundColor Green
        Write-Host "  • APPOINTMENT_STATUS_CHANGED ⭐ NEW" -ForegroundColor Green
        Write-Host ""
        Write-Host "You can now restart your Spring Boot application." -ForegroundColor Yellow
        Write-Host "The notification system will work without errors!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "❌ Failed to update database constraint" -ForegroundColor Red
        Write-Host ""
        Write-Host "Output:" -ForegroundColor Yellow
        Write-Host $result -ForegroundColor Gray
        Write-Host ""
        Write-Host "Please try running the SQL manually:" -ForegroundColor Yellow
        Write-Host "  psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $sqlFile" -ForegroundColor White
    }
} catch {
    Write-Host ""
    Write-Host "❌ Error executing SQL script: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run the SQL script manually:" -ForegroundColor Yellow
    Write-Host "  1. Open pgAdmin or your PostgreSQL client" -ForegroundColor White
    Write-Host "  2. Connect to database: $dbName" -ForegroundColor White
    Write-Host "  3. Run the SQL from: update-notification-types.sql" -ForegroundColor White
} finally {
    # Clear password from environment
    $env:PGPASSWORD = $null
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan

