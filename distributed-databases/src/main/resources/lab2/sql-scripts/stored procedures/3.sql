create procedure task3(IN T int, IN D int)
begin
    select *, (case when pc.price < D then pc.price + T ELSE price END)
    from pc;
end;