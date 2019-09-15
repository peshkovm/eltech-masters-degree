select maker, pc.speed
from product
         join pc on product.model = pc.model
where pc.hd >= 30;