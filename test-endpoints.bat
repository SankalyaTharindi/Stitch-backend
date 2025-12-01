@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Testing Stitch Backend API Endpoints
echo ========================================
echo.
echo Make sure the server is running on port 8080
echo Run: mvn spring-boot:run
echo.
pause

echo.
echo [1] Testing Login - Customer (sankalyacus@gmail.com)
echo -----------------------------------------------
curl.exe -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"sankalyacus@gmail.com\",\"password\":\"12345678\"}"
echo.
echo.
echo Please copy the token from above and enter it below:
set /p CUSTOMER_TOKEN="Enter Customer Token: "
echo.

echo [2] Testing GET /api/auth/me (Customer)
echo -----------------------------------------------
curl.exe -X GET http://localhost:8080/api/auth/me -H "Authorization: Bearer %CUSTOMER_TOKEN%"
echo.
echo.

echo [3] Testing GET /api/appointments/customer/my-appointments (Customer)
echo -----------------------------------------------
curl.exe -X GET http://localhost:8080/api/appointments/customer/my-appointments -H "Authorization: Bearer %CUSTOMER_TOKEN%"
echo.
echo.

echo [4] Testing Registration - New Admin User
echo -----------------------------------------------
curl.exe -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"admin@stitch.com\",\"password\":\"admin123\",\"fullName\":\"Admin User\",\"phoneNumber\":\"1234567890\",\"role\":\"ADMIN\"}"
echo.
echo.

echo [5] Testing Login - Admin
echo -----------------------------------------------
curl.exe -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@stitch.com\",\"password\":\"admin123\"}"
echo.
echo.
echo Please copy the admin token from above and enter it below:
set /p ADMIN_TOKEN="Enter Admin Token: "
echo.

echo [6] Testing GET /api/admin/users (Admin)
echo -----------------------------------------------
curl.exe -X GET http://localhost:8080/api/admin/users -H "Authorization: Bearer %ADMIN_TOKEN%"
echo.
echo.

echo [7] Testing GET /api/admin/customers (Admin)
echo -----------------------------------------------
curl.exe -X GET http://localhost:8080/api/admin/customers -H "Authorization: Bearer %ADMIN_TOKEN%"
echo.
echo.

echo [8] Testing GET /api/appointments/admin/all (Admin)
echo -----------------------------------------------
curl.exe -X GET http://localhost:8080/api/appointments/admin/all -H "Authorization: Bearer %ADMIN_TOKEN%"
echo.
echo.

echo ========================================
echo Testing Complete!
echo ========================================
echo.
echo Customer Token: %CUSTOMER_TOKEN%
echo Admin Token: %ADMIN_TOKEN%
echo.
echo Use these tokens for manual testing with the commands in API_TESTING_GUIDE.md
echo.
pause

