select distinct maker
from product
where product.model in
      (select pc.model
       from pc
       where speed >= 1200);

## Класс 1 подкласс N

##   |
##   |
## Лемма 1
##   |
##   V

select distinct product.maker
from product,
     pc
where product.model = pc.model
  AND pc.speed >= 1200;