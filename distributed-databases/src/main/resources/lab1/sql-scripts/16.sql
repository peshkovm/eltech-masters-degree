select maker
from (select model
      from printer
      where color = 'C'
        and printer.price = (select min(price) from printer)
     ) as coloredPrinterModelWithMinPrice,
     product
where product.model = coloredPrinterModelWithMinPrice.model