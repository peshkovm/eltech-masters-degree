CREATE PROCEDURE task13(IN old_model int,
                        IN new_model int)
BEGIN
    update product,pc,laptop,printer
    set product.model=new_model,
        pc.model=new_model,
        laptop.model=new_model,
        printer.model=new_model
    where product.model = old_model
      and pc.model = old_model
      and laptop.model = old_model
      and printer.model = old_model;
END;