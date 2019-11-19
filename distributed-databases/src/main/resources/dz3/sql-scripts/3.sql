select pc.model
from pc
where pc.Ram =
      (select MAX(laptop.Ram)
       from laptop
       where pc.speed = laptop.speed);

## Класс 1 подкласс JA

##   |
##   |
## Лемма 1
##   |
##   V

select pc.model
from pc
where pc.Ram = (
    select Rt.C2
    from ((select laptop.speed as C1, MAX(laptop.Ram) as C2
           from laptop
           group by laptop.speed)) as Rt
    where Rt.C1 = pc.speed
);
