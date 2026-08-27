ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN google_id VARCHAR(255);

ALTER TABLE users
    ADD CONSTRAINT users_google_id_unique UNIQUE (google_id);
