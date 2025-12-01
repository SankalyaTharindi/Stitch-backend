# Admin & Customer User Management

## Overview
Your Stitch backend now supports **1 Admin user** and **multiple Customer users** with automatic admin initialization on first startup.

## User Roles

### ADMIN (Single User)
- Full system access
- Can view all users and customers
- Can activate/deactivate customer accounts
- Can change their own password
- Access to `/api/admin/**` endpoints

### CUSTOMER (Multiple Users)
- Limited access to their own data
- Can register via public endpoint
- Can book appointments and view their own data
- Access to `/api/customer/**` endpoints

## Automatic Admin Setup

When you start the application for the first time, an admin user is automatically created:

### Default Admin Credentials
```
Email:    admin@stitch.com
Password: admin123
```

⚠️ **IMPORTANT:** Change this password immediately after first login!

### How It Works
The `DataInitializer` class runs on application startup and:
1. Checks if admin user exists
2. If not, creates admin with default credentials
3. Logs the credentials to console
4. Only runs once (won't create duplicate admins)

## API Endpoints

### 1. Admin Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@stitch.com",
    "password": "admin123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "email": "admin@stitch.com",
    "fullName": "System Administrator",
    "phoneNumber": "0000000000",
    "role": "ADMIN",
    "active": true
  }
}
```

### 2. Customer Registration (Public)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123",
    "fullName": "John Doe",
    "phoneNumber": "1234567890"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 2,
    "email": "customer@example.com",
    "fullName": "John Doe",
    "phoneNumber": "1234567890",
    "role": "CUSTOMER",
    "active": true
  }
}
```

### 3. Get All Users (Admin Only)
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE"
```

### 4. Get All Customers (Admin Only)
```bash
curl -X GET http://localhost:8080/api/admin/customers \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE"
```

### 5. Get User by ID (Admin Only)
```bash
curl -X GET http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE"
```

### 6. Toggle User Active Status (Admin Only)
```bash
curl -X PUT http://localhost:8080/api/admin/users/2/toggle-status \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE"
```

This will activate/deactivate the customer account.

### 7. Change Password (Admin or Customer)
```bash
curl -X PUT http://localhost:8080/api/admin/change-password \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "admin123",
    "newPassword": "newSecurePassword123"
  }'
```

## Security Configuration

### Endpoint Access Rules

| Endpoint Pattern | Access Level | Required Role |
|-----------------|--------------|---------------|
| `/api/auth/**` | Public | None |
| `/api/admin/**` | Protected | ADMIN |
| `/api/customer/**` | Protected | CUSTOMER |
| `/api/notifications/**` | Protected | Any authenticated user |

### Role Hierarchy
- **ADMIN** - Has full access to all endpoints
- **CUSTOMER** - Can only access customer-specific endpoints and their own data

## First Time Setup Checklist

1. ✅ Start the application
   ```bash
   mvn spring-boot:run
   ```

2. ✅ Check console logs for admin creation message:
   ```
   ✓ Admin user created successfully!
     Email: admin@stitch.com
     Password: admin123
   ⚠ Please change the admin password after first login!
   ```

3. ✅ Login as admin
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@stitch.com","password":"admin123"}'
   ```

4. ✅ Change admin password immediately
   ```bash
   curl -X PUT http://localhost:8080/api/admin/change-password \
     -H "Authorization: Bearer ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"oldPassword":"admin123","newPassword":"YourSecurePassword123!"}'
   ```

5. ✅ Test customer registration (public endpoint - no token needed)
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"email":"customer1@test.com","password":"password123","fullName":"Customer One","phoneNumber":"9876543210"}'
   ```

## Database Schema

The `users` table now contains both admin and customer accounts:

```sql
SELECT id, email, full_name, role, is_active FROM users;
```

Expected output after setup:
```
 id |        email        |      full_name        | role     | is_active
----+---------------------+----------------------+----------+-----------
  1 | admin@stitch.com    | System Administrator | ADMIN    | t
  2 | customer1@test.com  | Customer One         | CUSTOMER | t
  3 | customer2@test.com  | Customer Two         | CUSTOMER | t
```

## Admin Operations

### View All Customers
```bash
# Get list of all customers (excluding admin)
curl -X GET http://localhost:8080/api/admin/customers \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### Deactivate a Customer
```bash
# Toggle customer status (active/inactive)
curl -X PUT http://localhost:8080/api/admin/users/2/toggle-status \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

When a customer is deactivated:
- They cannot login
- Their token becomes invalid
- They cannot access any protected endpoints

### Reactivate a Customer
```bash
# Toggle again to reactivate
curl -X PUT http://localhost:8080/api/admin/users/2/toggle-status \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

## Security Best Practices

### For Production

1. **Change Default Admin Password**
   - Never use `admin123` in production
   - Use a strong password (min 12 characters, mixed case, numbers, symbols)

2. **Environment Variables**
   Set admin credentials via environment variables:
   ```yaml
   # application.yml
   admin:
     email: ${ADMIN_EMAIL:admin@stitch.com}
     password: ${ADMIN_PASSWORD:admin123}
   ```

3. **Password Policy**
   Consider implementing:
   - Minimum password length (current: 6, recommend: 12+)
   - Password complexity requirements
   - Password expiration
   - Password history

4. **Account Lockout**
   - Implement failed login attempt tracking
   - Lock account after N failed attempts
   - Require admin intervention to unlock

5. **Audit Logging**
   - Log all admin actions (user activation/deactivation)
   - Log password changes
   - Log failed login attempts

## Troubleshooting

### Admin User Not Created
Check application logs on startup. If you see:
```
✓ Admin user already exists
```
The admin was already created in a previous run.

### Admin Login Fails
1. Verify credentials:
   ```bash
   # Check if admin exists in database
   psql -U stitch_user1 -d stitch -c "SELECT email, role, is_active FROM users WHERE role='ADMIN';"
   ```

2. If password was changed and forgotten:
   ```sql
   -- Reset admin password (requires database access)
   -- Run this in psql as stitch_user1
   UPDATE users 
   SET password = '$2a$10$...' -- Use BCrypt hash of new password
   WHERE email = 'admin@stitch.com';
   ```

3. Generate BCrypt hash for password reset:
   Use online tool or create a simple Java program to generate BCrypt hash.

### Cannot Access Admin Endpoints
Verify:
1. Token is valid (not expired)
2. User role is ADMIN (check JWT token payload)
3. Authorization header format: `Bearer TOKEN_HERE`
4. Endpoint URL is correct: `/api/admin/**`

## Files Created

1. **DataInitializer.java** - Automatically creates admin on startup
2. **AdminController.java** - Admin endpoints for user management
3. **ChangePasswordRequest.java** - DTO for password change
4. **UserDTO.java** - Updated with `active` field

## Testing

### Manual Testing Script
```bash
#!/bin/bash

# 1. Login as admin
ADMIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@stitch.com","password":"admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | jq -r '.token')
echo "Admin Token: $ADMIN_TOKEN"

# 2. Register a customer
CUSTOMER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","fullName":"Test User","phoneNumber":"1234567890"}')

echo "Customer Registered: $CUSTOMER_RESPONSE"

# 3. Admin views all customers
curl -X GET http://localhost:8080/api/admin/customers \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 4. Change admin password
curl -X PUT http://localhost:8080/api/admin/change-password \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"admin123","newPassword":"newPassword123"}'
```

---

**Status:** ✅ Admin and Customer user system fully configured
**Next Steps:** 
1. Start application
2. Login as admin
3. Change default password
4. Test customer registration

