select model, speed, hd
from pc
where price <= 2000
  and (rd regexp '12.*' or rd regexp '16.*');