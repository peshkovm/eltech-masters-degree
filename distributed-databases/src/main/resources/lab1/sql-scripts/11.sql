select maker
from product
         join (select model, speed from pc union select model, speed from laptop) as t1 on product.model = t1.model
where speed >= 1000
group by maker
having count(*) >= 2;