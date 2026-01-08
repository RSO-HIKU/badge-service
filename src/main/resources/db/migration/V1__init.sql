CREATE TABLE badge_service.logbook (
    id SERIAL PRIMARY KEY,        
    user_id INT NOT NULL,        
    peak_id INT NOT NULL,         
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT 
);