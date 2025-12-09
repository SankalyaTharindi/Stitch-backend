@echo off
echo === PostgreSQL Database Permission Fix ===
echo.
echo This script will fix database permissions for stitch_user1
echo You will need to enter the postgres user password when prompted
echo.

REM Try to find PostgreSQL installation
set PSQL_PATH=

if exist "C:\Program Files\PostgreSQL\18\bin\psql.exe" set PSQL_PATH=C:\Program Files\PostgreSQL\18\bin\psql.exe
if exist "C:\Program Files\PostgreSQL\16\bin\psql.exe" set PSQL_PATH=C:\Program Files\PostgreSQL\16\bin\psql.exe
if exist "C:\Program Files\PostgreSQL\15\bin\psql.exe" set PSQL_PATH=C:\Program Files\PostgreSQL\15\bin\psql.exe
if exist "C:\Program Files\PostgreSQL\14\bin\psql.exe" set PSQL_PATH=C:\Program Files\PostgreSQL\14\bin\psql.exe
if exist "C:\Program Files\PostgreSQL\13\bin\psql.exe" set PSQL_PATH=C:\Program Files\PostgreSQL\13\bin\psql.exe

if "%PSQL_PATH%"=="" (
    echo ERROR: Could not find psql.exe
    echo Please see FIX_DATABASE_PERMISSIONS.md for manual instructions
    pause
    exit /b 1
)

echo Found PostgreSQL at: %PSQL_PATH%
echo.

"%PSQL_PATH%" -U postgres -d stitch -c "GRANT ALL PRIVILEGES ON DATABASE stitch TO stitch_user1; GRANT USAGE ON SCHEMA public TO stitch_user1; GRANT CREATE ON SCHEMA public TO stitch_user1; GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO stitch_user1; GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO stitch_user1; ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO stitch_user1; ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO stitch_user1;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo === Permissions successfully granted! ===
    echo.
    echo You can now run: mvn spring-boot:run
    echo.
) else (
    echo.
    echo ERROR: Failed to grant permissions
    echo Please see FIX_DATABASE_PERMISSIONS.md for manual instructions
    echo.
)

pause

