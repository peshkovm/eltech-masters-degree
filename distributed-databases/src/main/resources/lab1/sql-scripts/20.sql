select avg(price) as avgPrice
from product
         join pc on product.model = pc.model
where product.maker = 'A'