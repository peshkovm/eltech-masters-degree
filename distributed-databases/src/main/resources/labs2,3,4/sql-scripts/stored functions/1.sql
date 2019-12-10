CREATE FUNCTION func_1() RETURNS int deterministic
BEGIN
    declare res1, res2 int;
    select min(price) into res1 from pc;
    select min(price) into res2 from laptop;
    return res1 + res2;
END;