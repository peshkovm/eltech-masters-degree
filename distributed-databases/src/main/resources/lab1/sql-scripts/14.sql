select *
from laptop
where price < (select min(price)
               from pc)