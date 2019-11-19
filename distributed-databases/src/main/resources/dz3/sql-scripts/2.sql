select distinct product.maker
from product
where product.model in
      (select laptop.model
       from laptop
       where product.type != 'Pc');

## Класс 1 подкласс J

##   |
##   |
## Лемма 1
##   |
##   V

select distinct product.maker
from product,
     laptop
where product.model = laptop.model
  AND product.type != 'Pc';