CREATE FUNCTION func_2() RETURNS int deterministic
BEGIN
    declare res int;
    select max(price) - min(price) into res from pc;
    return res;
END;