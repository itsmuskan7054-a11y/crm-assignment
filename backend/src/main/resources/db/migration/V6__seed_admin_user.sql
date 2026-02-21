-- Default admin user: admin@palmonas.com / Admin@123
-- BCrypt hash for "Admin@123" with strength 12
INSERT INTO users (id, email, password_hash, full_name, role, is_active)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'admin@palmonas.com',
    '$2a$12$LJ3m4ys3LzZpXGMSB6cLGuSxNm7LVIBftXgjqLcXDMb1fMFqxNePe',
    'System Admin',
    'SUPER_ADMIN',
    true
)
ON CONFLICT (email) DO NOTHING;
