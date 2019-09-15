select product.model, a.price
from product
         join (select pc.model, pc.price
               from pc
               union
               select laptop.model, laptop.price
               from laptop
               union
               select printer.model, printer.price
               from printer) as a on product.model = a.model
where product.maker = 'C';