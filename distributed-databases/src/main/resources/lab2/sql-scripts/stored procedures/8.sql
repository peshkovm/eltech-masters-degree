create view task8 as
    select *
    from pc
    union
    (select *
     from laptop);