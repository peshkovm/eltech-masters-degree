CREATE PROCEDURE task6(IN inPrice int)
BEGIN
    select maker
    from (select distinct maker, type
          from product
          where product.maker in (select maker
                                  from product
                                           join (select model, price
                                                 from pc
                                                 union
                                                 select model, price
                                                 from laptop
                                                 union
                                                 select model, price
                                                 from printer) as t1 on product.model = t1.model
                                  group by maker
                                  having sum(price) > inPrice)
            and type != 'Printer') as t2
    group by maker
    having count(*) = 1;
END;