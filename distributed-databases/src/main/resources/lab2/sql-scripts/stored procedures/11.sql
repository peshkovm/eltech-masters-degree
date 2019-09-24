CREATE PROCEDURE task11(OUT type varchar(10), OUT max_cnt int)
BEGIN
    select type, max(cnt)
    into type, max_cnt
    from (select type, count(1) cnt
          from product
          group by type)
             as t1;
END;