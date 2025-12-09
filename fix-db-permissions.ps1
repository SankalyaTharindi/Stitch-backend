# PowerShell script to fix PostgreSQL database permissions
# Run this script as Administrator

Write-Host "=== PostgreSQL Database Permission Fix Script ===" -ForegroundColor Cyan
Write-Host ""

# Try to find PostgreSQL installation
$postgresqlPaths = @(
    "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files\PostgreSQL\15\bin\psql.exe",
    "C:\Program Files\PostgreSQL\14\bin\psql.exe",
    "C:\Program Files\PostgreSQL\13\bin\psql.exe",
    "C:\Program Files (x86)\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files (x86)\PostgreSQL\15\bin\psql.exe"
)

$psqlPath = $null
foreach ($path in $postgresqlPaths) {
    if (Test-Path $path) {
        $psqlPath = $path
        Write-Host "Found PostgreSQL at: $path" -ForegroundColor Green
        break
    }
}

if (-not $psqlPath) {
    Write-Host "ERROR: Could not find psql.exe" -ForegroundColor Red
    Write-Host "Please locate your PostgreSQL installation and run:" -ForegroundColor Yellow
    Write-Host '  & "C:\Path\To\PostgreSQL\bin\psql.exe" -U postgres -d stitch -f fix-db-permissions.sql' -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "This script will grant all necessary permissions to stitch_user1" -ForegroundColor Yellow
Write-Host "You will be prompted for the postgres user password" -ForegroundColor Yellow
Write-Host ""

# Run the SQL file
& $psqlPath -U postgres -d stitch -f "fix-db-permissions.sql"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=== Permissions have been successfully granted! ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "You can now run your Spring Boot application:" -ForegroundColor Cyan
    Write-Host "  mvn spring-boot:run" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "ERROR: Failed to grant permissions" -ForegroundColor Red
    Write-Host "Please check the error message above and try manually" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "See FIX_DATABASE_PERMISSIONS.md for manual instructions" -ForegroundColor Yellow
}

