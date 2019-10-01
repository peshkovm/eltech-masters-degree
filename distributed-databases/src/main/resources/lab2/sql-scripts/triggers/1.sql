CREATE TRIGGER trigger_1
    BEFORE INSERT
    ON pc
    FOR EACH ROW
BEGIN
    if new.price < 3000 then
        signal sqlstate '45000' set message_text = 'error';
    end if;
END;
