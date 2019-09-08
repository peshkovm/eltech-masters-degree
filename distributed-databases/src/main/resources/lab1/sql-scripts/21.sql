with pc_laptop as (select *
                   from pc
                   union
                   select *
                   from laptop)

select avg(price) as avgPrice
from product
         join pc_laptop on product.model = pc_laptop.model
where product.maker = 'D'