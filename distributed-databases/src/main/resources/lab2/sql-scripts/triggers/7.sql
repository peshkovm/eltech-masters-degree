CREATE TRIGGER trigger_7
    BEFORE DELETE
    ON product
    FOR EACH ROW
BEGIN
    if ((select count(1)
         from product
         where product.maker = OLD.maker)) = 1 then
        signal sqlstate '45000' set message_text = 'error';
    end if;
END;
