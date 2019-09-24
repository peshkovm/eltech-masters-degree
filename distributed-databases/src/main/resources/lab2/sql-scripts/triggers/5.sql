CREATE TRIGGER trigger_5
    BEFORE UPDATE
    ON pc
    FOR EACH ROW
BEGIN
    IF NEW.speed not in (32, 64, 128)
    THEN
        signal sqlstate '45000' set message_text = 'error';
    end if;
END;
