CREATE PROCEDURE task9(IN in_model int)
BEGIN
    delete product, p
    from product
             join pc p
                  on product.model = p.model
    where p.model = in_model;
END;