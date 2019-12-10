create index ind1 on groceries_table (maker);

explain
select distinct groceries_table.name
from groceries_table
where groceries_table.maker = 'ВЛ'
  and groceries_table.type not in (select groceries_table.type
                                   from groceries_table
                                   where groceries_table.maker != 'ВЛ');

show indexes from groceries_table;

drop index ind1 on groceries_table;


select groceries_table.name
from groceries_table