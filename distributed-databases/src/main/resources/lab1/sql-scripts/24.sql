select maker
from product
where type = 'Pc'
group by product.maker
having count(*) >= 3