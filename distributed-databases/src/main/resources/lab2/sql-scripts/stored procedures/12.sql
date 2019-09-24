CREATE PROCEDURE task12()
BEGIN
    select maker
    from product
    where product.maker in (select maker
                            from product
                                     left join printer on product.model = printer.model
                            where color = 'C')
      and type != 'Printer'
    group by maker
    order by count(*) desc
    limit 1;
END;