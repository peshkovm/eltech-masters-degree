select *
from product
         join printer on product.model = printer.model
where price = (select max(price) from printer);