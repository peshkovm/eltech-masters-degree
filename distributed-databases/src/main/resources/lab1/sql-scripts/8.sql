select maker
from product as a
where type = 'Pc'
  and not exists(select 1 from product where type = 'Laptop' and maker = a.maker);