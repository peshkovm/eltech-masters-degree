create procedure task4()
begin
    select model
    from pc
    where price < (select avg(price)
                   from pc);
end;