create index ind1 on groceries_table (maker);

explain
select distinct groceries_table.type
from groceries_table
where groceries_table.maker = 'Вкусняш';

show indexes from groceries_table;

drop index ind1 on groceries_table;
