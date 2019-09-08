select *
from laptop
where not exists(select *
                 from pc
                 where laptop.speed > pc.speed)