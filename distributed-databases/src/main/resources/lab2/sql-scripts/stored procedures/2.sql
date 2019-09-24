create procedure task2()
begin
    select avg(pc.price) as pcAvgPrice, avg(laptop.price) as laptopAvgPrice
    from pc,
         laptop;
end;