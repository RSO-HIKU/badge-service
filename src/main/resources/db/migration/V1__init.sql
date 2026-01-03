CREATE SCHEMA IF NOT EXISTS badge_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA badge_service GRANT ALL PRIVILEGES ON TABLES TO hikuuser;

ALTER DEFAULT PRIVILEGES IN SCHEMA badge_service GRANT ALL PRIVILEGES ON SEQUENCES TO hikuuser;

CREATE TABLE badge_service.logbook (
    id SERIAL PRIMARY KEY,        
    user_id INT NOT NULL,        
    peak_id INT NOT NULL,         
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT 
);