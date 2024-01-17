CREATE OR REPLACE PROCEDURE create_health_record(
    IN p_user_id INT,
    IN p_health_info VARCHAR(255)
)
AS $$
BEGIN
    INSERT INTO health_records (user_id, health_info) VALUES (p_user_id, p_health_info);
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE update_health_record(
    IN p_record_id INT,
    IN p_user_id INT,
    IN p_health_info VARCHAR(255)
)
AS $$
BEGIN
    UPDATE health_records SET user_id = p_user_id, health_info = p_health_info WHERE record_id = p_record_id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE delete_health_record(
    IN p_record_id INT
)
AS $$
BEGIN
    DELETE FROM health_records WHERE record_id = p_record_id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE get_all_health_records()
AS $$
BEGIN
    SELECT * FROM health_records;
END;
$$ LANGUAGE plpgsql;
