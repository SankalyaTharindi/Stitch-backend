-- Fix PostgreSQL database permissions for stitch_user1
-- Run this script as the postgres superuser or database owner

-- Connect to the stitch database first
\c stitch

-- Grant all privileges on the database
GRANT ALL PRIVILEGES ON DATABASE stitch TO stitch_user1;

-- Grant usage on the public schema
GRANT USAGE ON SCHEMA public TO stitch_user1;

-- Grant create permission on the public schema
GRANT CREATE ON SCHEMA public TO stitch_user1;

-- Grant all privileges on all tables in public schema (for existing tables)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO stitch_user1;

-- Grant all privileges on all sequences in public schema (for auto-increment IDs)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO stitch_user1;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO stitch_user1;

-- Set default privileges for future sequences
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO stitch_user1;

