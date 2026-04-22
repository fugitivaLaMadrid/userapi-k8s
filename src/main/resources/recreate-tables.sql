-- Script to drop and recreate the users table
-- Run this manually when you need to reset the database schema

-- Drop the table if it exists
DROP TABLE IF EXISTS users;

-- Note: The table will be automatically recreated by JPA/Hibernate
-- when the application starts with ddl-auto: update or create-drop

-- If you need to manually create the table (not recommended with JPA), uncomment below:
/*
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);
*/
