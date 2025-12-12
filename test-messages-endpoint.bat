@echo off
echo ========================================
echo Testing Messages Endpoint
echo ========================================
echo.

REM Step 1: Login
echo [1/3] Logging in as admin...
curl -s -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"admin@stitch.com\",\"password\":\"admin123\"}" ^
  -o temp_auth.json

if errorlevel 1 (
    echo ERROR: Backend not responding or login failed
    echo Make sure backend is running: mvn spring-boot:run
    del temp_auth.json 2>nul
    pause
    exit /b 1
)

REM Extract token (this is a simple approach, may need PowerShell for complex JSON)
for /f "tokens=2 delims=:," %%a in ('type temp_auth.json ^| findstr /C:"token"') do (
    set TOKEN=%%a
)

REM Remove quotes and spaces
set TOKEN=%TOKEN:"=%
set TOKEN=%TOKEN: =%

if "%TOKEN%"=="" (
    echo ERROR: Could not extract token from response
    type temp_auth.json
    del temp_auth.json
    pause
    exit /b 1
)

echo SUCCESS: Logged in successfully
echo.

REM Step 2: Test /api/auth/me
echo [2/3] Testing authentication with /api/auth/me...
curl -s http://localhost:8080/api/auth/me ^
  -H "Authorization: Bearer %TOKEN%" ^
  -o temp_me.json

if errorlevel 1 (
    echo ERROR: Auth verification failed
    del temp_auth.json temp_me.json 2>nul
    pause
    exit /b 1
)

type temp_me.json
echo.
echo SUCCESS: Authentication verified
echo.

REM Step 3: Test messages endpoint
echo [3/3] Testing /api/messages/customers...
curl -v http://localhost:8080/api/messages/customers ^
  -H "Authorization: Bearer %TOKEN%"

echo.
echo.
echo ========================================
echo Test complete!
echo.
echo If you see "200 OK" above, the backend is working correctly.
echo If you see "401 Unauthorized", check the backend logs for details.
echo ========================================

REM Cleanup
del temp_auth.json temp_me.json 2>nul
pause

