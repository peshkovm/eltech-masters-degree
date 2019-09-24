create procedure task1()
begin
    select model
    from pc
    where price = (select max(price)
                   from pc);
end;