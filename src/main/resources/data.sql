-- Sample users for development

-- Clear table and reset IDs for predictable testing
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- Insert sample users
INSERT INTO users (username, email, created_at)
VALUES
    ('alice', 'alice@example.com', CURRENT_TIMESTAMP),
    ('bob', 'bob@example.com', CURRENT_TIMESTAMP),
    ('carol', 'carol@example.com', CURRENT_TIMESTAMP);