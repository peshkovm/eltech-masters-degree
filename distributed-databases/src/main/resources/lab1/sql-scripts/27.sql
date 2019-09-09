select pr.maker, avg(pc.Hd)
from pc
         join product pr on pc.model = pr.model
         join product pr1 on pr.maker = pr1.maker and pr1.type = 'Printer'
group by pr.maker;