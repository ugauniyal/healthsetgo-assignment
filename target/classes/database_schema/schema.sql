CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    age INT NOT NULL
);

CREATE TABLE health_records (
    record_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    health_info JSONB
);

CREATE TABLE episodes (
    episode_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complaint_type VARCHAR(255) NOT NULL,
    treatment VARCHAR(255)
);
