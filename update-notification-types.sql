-- Update notification type constraint to include new notification types
-- Run this SQL script in your PostgreSQL database

-- Drop the existing check constraint
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

-- Add the updated check constraint with all notification types
ALTER TABLE notifications
ADD CONSTRAINT notifications_type_check
CHECK (type IN (
    'APPOINTMENT_BOOKED',
    'APPOINTMENT_APPROVED',
    'APPOINTMENT_DECLINED',
    'MEASUREMENT_REMINDER',
    'JACKET_READY',
    'PAYMENT_REMINDER',
    'GALLERY_PHOTO_LIKED',
    'GALLERY_PHOTO_UPLOADED',
    'CUSTOMER_REGISTERED',
    'CUSTOMER_MILESTONE',
    'PROFILE_UPDATED',
    'APPOINTMENT_STATUS_CHANGED'
));

-- Verify the constraint was updated
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
AND conname = 'notifications_type_check';

