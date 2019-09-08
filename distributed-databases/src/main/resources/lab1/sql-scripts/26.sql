select avg(price), maker
from pc
         join product on pc.model = product.model
where pc.speed > 800
group by product.maker