-- Change user_id column from INT to VARCHAR(255) to store Keycloak user IDs
ALTER TABLE badge_service.logbook 
ALTER COLUMN user_id TYPE VARCHAR(255);
