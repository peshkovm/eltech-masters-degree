CREATE TRIGGER trigger_4
    BEFORE DELETE
    ON pc
    FOR EACH ROW
BEGIN
    declare res char(1);
    select maker into res from product where model = old.model;
    if exists(select 1 from product where maker = res and type = 'printer') then
        signal sqlstate '45000' set message_text = 'error';
    end if;
END
