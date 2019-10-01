CREATE TRIGGER trigger_name
    BEFORE INSERT
    ON product
    FOR EACH ROW
BEGIN
    if new.maker IS NULL then
        signal sqlstate '45000' set message_text = 'error';
    end if;
END;
