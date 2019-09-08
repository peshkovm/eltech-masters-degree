select avg(laptop.screen), product.maker
from laptop
         join product on laptop.model = product.model
group by product.maker