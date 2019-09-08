with unionTable as (select model, price
                    from pc
                    union
                    select model, price
                    from laptop
                    union
                    select model, price
                    from printer)

select model
from unionTable
where unionTable.price = (select max(price) from unionTable)