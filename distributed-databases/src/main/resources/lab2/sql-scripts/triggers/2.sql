CREATE TRIGGER trigger_2
    BEFORE INSERT
    ON printer
    FOR EACH ROW
BEGIN
    if new.color = 'N' then
        signal sqlstate '45000' set message_text = 'error';
    end if;
END;
