CREATE FUNCTION func_3(company char(1)) RETURNS int deterministic
BEGIN
    declare r1, r2 int;
    select count(*) into r1 from product where maker = company and type = 'printer';
    select count(*) into r2 from product where maker = company;
    return r1 / r2 * 100;
END;