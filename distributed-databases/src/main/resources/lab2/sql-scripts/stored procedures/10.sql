CREATE PROCEDURE task10(IN old_name char,
                        IN new_name char)
BEGIN
    select * from product;
    update product
    set maker=new_name
    where maker = old_name;
END;