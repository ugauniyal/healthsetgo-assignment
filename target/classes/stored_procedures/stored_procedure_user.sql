CREATE OR REPLACE PROCEDURE get_all_users()
AS $$
BEGIN
    SELECT * FROM users;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE create_user(
    IN p_name VARCHAR(255),
    IN p_age INTEGER
)
AS $$
BEGIN
    INSERT INTO users (name, age) VALUES (p_name, p_age);
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE PROCEDURE update_user(
    IN p_user_id INT,
    IN p_name VARCHAR(255),
    IN p_email VARCHAR(255)
)
AS $$
BEGIN
    UPDATE users SET name = p_name, email = p_email WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE delete_user(
    IN p_user_id INT
)
AS $$
BEGIN
    DELETE FROM users WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

