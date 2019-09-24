create procedure task5(IN inPrice int)
begin
    select maker
    from product
             join (select price, model
                   from pc
                   union
                   select price, model
                   from laptop
                   union
                   select price, model
                   from printer) as allPrices
                  on product.model = allPrices.model
    group by maker
    having avg(allPrices.price) < inPrice;
end;