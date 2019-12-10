create index ind1 on groceries_table (type);

explain
select distinct groceries_table.maker
from groceries_table
where groceries_table.type = 'колбаса';

show indexes from groceries_table;

drop index ind1 on groceries_table;
