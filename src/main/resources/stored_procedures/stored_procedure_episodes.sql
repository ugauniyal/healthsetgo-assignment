CREATE OR REPLACE PROCEDURE create_episode(
    IN p_user_id INT,
    IN p_complaint_type VARCHAR(255),
    IN p_treatment VARCHAR(255)
)
AS $$
BEGIN
    INSERT INTO episodes (user_id, complaint_type, treatment) VALUES (p_user_id, p_complaint_type, p_treatment);
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE PROCEDURE update_episode(
    IN p_episode_id INT,
    IN p_user_id INT,
    IN p_complaint_type VARCHAR(255),
    IN p_treatment VARCHAR(255)
)
AS $$
BEGIN
    UPDATE episodes
    SET user_id = p_user_id, complaint_type = p_complaint_type, treatment = p_treatment
    WHERE episode_id = p_episode_id;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE PROCEDURE delete_episode(
    IN p_episode_id INT
)
AS $$
BEGIN
    DELETE FROM episodes WHERE episode_id = p_episode_id;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE PROCEDURE get_all_episodes()
AS $$
BEGIN
    SELECT * FROM episodes;
END;
$$ LANGUAGE plpgsql;
