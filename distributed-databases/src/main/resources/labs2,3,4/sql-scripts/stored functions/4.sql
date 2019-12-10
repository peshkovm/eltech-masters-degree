CREATE FUNCTION task4_func(userRAM int) RETURNS int DETERMINISTIC
BEGIN
    DECLARE cnt int;

    select count(1)
    into cnt
    from product
             join (select model, Ram
                   from pc
                   union
                   select model, Ram
                   from laptop) as t on product.model = t.model
    where t.Ram > userRAM;

    return cnt;
END;