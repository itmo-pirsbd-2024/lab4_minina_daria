CREATE TABLE users (
    username VARCHAR(20) PRIMARY KEY,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    locked BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE login_attempts (
    id SERIAL PRIMARY KEY,
    username VARCHAR(20) REFERENCES users(username),
    success BOOLEAN NOT NULL,
    attempt_time TIMESTAMP NOT NULL
);