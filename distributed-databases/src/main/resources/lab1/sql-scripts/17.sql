with smallestRamPc as (select model, speed
                       from pc
                       where Ram = (select min(Ram) from pc))

select maker
from product
         join smallestRamPc on product.model = smallestRamPc.model
where smallestRamPc.speed = (select max(speed) from smallestRamPc)
