select avg(price), pc.speed
from pc
group by pc.speed