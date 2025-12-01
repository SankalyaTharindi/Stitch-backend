# Stitch Backend - Authentication Setup ✅

## Summary
Your authentication system is now **fully operational**! 

### What Was Fixed
1.  **JWT Service** - Implemented token generation and validation with JJWT library
2. **Database Connection** - Connected to PostgreSQL (`stitch` database)
3. **Database Permissions** - Granted proper privileges to `stitch_user1`
4.  **Users Table** - Created and configured properly
5.  **Mail Configuration** - Made optional (no-op sender when not configured)
6.  **Security Configuration** - Configured JWT authentication filter and role-based access
7.  **Unit Tests** - All JWT service tests passing (2/2 tests)

## Working Endpoints

### 1. Register New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "fullName": "John Doe",
    "phoneNumber": "1234567890"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzAwNjQwMDAwLCJleHAiOjE3MDA3MjY0MDB9.signature",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "phoneNumber": "1234567890",
    "role": "CUSTOMER",
    "active": true
  }
}
```

### 2. Login (Authenticate)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "phoneNumber": "1234567890",
    "role": "CUSTOMER",
    "active": true
  }
}
```

### 3. Accessing Protected Endpoints
Use the token from login/register response in the Authorization header:

```bash
curl -X GET http://localhost:8080/api/customer/appointments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## Configuration

### Database (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/stitch
    username: stitch_user1
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### JWT Settings
```yaml
jwt:
  secret:
    key: ${JWT_SECRET_KEY:dGhpc2lzYXZlcnlzZWN1cmVrZXlmb3Jqd3R0b2tlbmVuY3J5cHRpb25wdXJwb3Nlc2FuZGl0c2hvdWxkYmVsb25nZW5vdWdo}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 hours
  allowed:
    clock:
      skew: 0  # seconds
```

## Security Rules

### Public Endpoints (No Authentication Required)
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Authenticate user
- `/api/public/**` - Public resources

### Protected Endpoints (Authentication Required)
- `/api/admin/**` - Requires `ADMIN` role
- `/api/customer/**` - Requires `CUSTOMER` role
- All other endpoints - Requires authentication (any authenticated user)

## User Roles
- **CUSTOMER** - Default role for new registrations
- **ADMIN** - Administrative access (must be set manually in database)

## Token Information
- **Algorithm:** HS256 (HMAC with SHA-256)
- **Expiration:** 24 hours (configurable)
- **Format:** Bearer token in Authorization header
- **Refresh:** Not implemented (token expires after 24 hours)

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn -Dtest=JwtServiceTest test
mvn -Dtest=AuthenticationServiceTest test
```

### Test Results
-  JwtServiceTest: 2/2 tests passing
  - Token generation and validation
  - Expired token detection
-  AuthenticationServiceTest: 2/2 tests passing
  - Successful authentication
  - User not found error handling

## Common Issues & Solutions

### Port 8080 Already in Use
```bash
# Kill existing Java processes
taskkill /F /IM java.exe

# Then restart
mvn spring-boot:run
```

### Database Connection Issues
```bash
# Verify connection with psql
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -U stitch_user1 -d stitch -W

# Grant permissions if needed (as postgres superuser)
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -U postgres -d stitch -c "GRANT ALL PRIVILEGES ON SCHEMA public TO stitch_user1;"
```

### Missing Tables
The application will automatically create tables on startup with `spring.jpa.hibernate.ddl-auto: update`.

If tables are not created:
1. Ensure the database user has CREATE TABLE privileges
2. Check startup logs for Hibernate DDL statements
3. Manually create tables using the SQL in the database setup

## Next Steps

### For Production
1. **Change JWT Secret** - Generate a strong, random 256-bit Base64 key
   ```bash
   # Example generation (use a secure method)
   openssl rand -base64 32
   ```
2. **Environment Variables** - Use environment variables for sensitive config:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET_KEY`
3. **CORS Configuration** - Update allowed origins in `SecurityConfig.java`
4. **HTTPS** - Enable SSL/TLS for production
5. **Token Refresh** - Implement refresh token mechanism for better security
6. **Email Service** - Configure SMTP settings for real email notifications

### For Development
1. **API Documentation** - Add Swagger/OpenAPI documentation
2. **Integration Tests** - Add more end-to-end tests
3. **Logging** - Configure structured logging (e.g., JSON logs)
4. **Health Checks** - Add actuator endpoints for monitoring

## Files Created/Modified

### Created
- `src/main/java/com/stitch/app/config/MailConfig.java`
- `src/test/java/com/stitch/app/security/JwtServiceTest.java`
- `src/test/java/com/stitch/app/service/AuthenticationServiceTest.java`

### Modified
- `src/main/java/com/stitch/app/security/JwtService.java`
- `src/main/java/com/stitch/app/service/NotificationService.java`
- `src/main/resources/application.yml`
- `pom.xml` (added JJWT dependencies)

## Support

If you encounter any issues:
1. Check application startup logs
2. Verify database connectivity
3. Ensure JWT secret is properly configured
4. Review security filter chain logs (enable DEBUG logging if needed)

---

**Status:**  Authentication system fully operational
**Last Updated:** November 22, 2025

