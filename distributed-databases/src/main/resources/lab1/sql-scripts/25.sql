select max(pc.price), maker
from pc
         join product on pc.model = product.model
group by product.maker