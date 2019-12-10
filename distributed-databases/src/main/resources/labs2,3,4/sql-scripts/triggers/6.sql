CREATE TRIGGER trigger_6
    BEFORE INSERT
    ON product
    FOR EACH ROW
BEGIN
    IF ((select count(1)
         from product
         where product.maker = NEW.maker)) = 6 THEN
        signal sqlstate '45000' set message_text = 'error';
    end if;
END;
