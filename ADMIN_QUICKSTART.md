# Quick Start - Admin & Customer Setup

## ✅ What's Been Configured

Your system now supports:
- **1 Admin User** (auto-created on first startup)
- **Multiple Customer Users** (self-registration via public endpoint)

## 🚀 Quick Start (3 Steps)

### Step 1: Start Your Application
```bash
mvn spring-boot:run
```

Look for this message in the console:
```
✓ Admin user created successfully!
  Email: admin@stitch.com
  Password: admin123
⚠ Please change the admin password after first login!
```

### Step 2: Login as Admin
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@stitch.com","password":"admin123"}'
```

You'll get a response with a token - **save this token!**

### Step 3: Change Admin Password (IMPORTANT!)
```bash
curl -X PUT http://localhost:8080/api/admin/change-password \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"admin123","newPassword":"YourNewSecurePassword123"}'
```

## 👥 User Types

| Role | Count | How Created | Access |
|------|-------|-------------|--------|
| **ADMIN** | 1 (only) | Auto-created on startup | All endpoints, manage users |
| **CUSTOMER** | Unlimited | Self-registration | Own data only |

## 📋 Common Operations

### Register New Customer (No Token Needed)
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

### Admin: View All Customers
```bash
curl -X GET http://localhost:8080/api/admin/customers \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### Admin: Deactivate/Activate Customer
```bash
curl -X PUT http://localhost:8080/api/admin/users/2/toggle-status \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

## 🔐 Default Admin Credentials

```
Email:    admin@stitch.com
Password: admin123
```

⚠️ **CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN!**

## 📁 New Files

- ✅ `DataInitializer.java` - Creates admin on startup
- ✅ `AdminController.java` - Admin user management endpoints
- ✅ `ChangePasswordRequest.java` - Password change DTO
- ✅ Updated `UserDTO.java` - Added `active` status field

## 📖 Full Documentation

See `ADMIN_CUSTOMER_SETUP.md` for:
- Complete API documentation
- Security best practices
- Troubleshooting guide
- Production deployment checklist

---

**Ready to test!** Start your app and login as admin.

