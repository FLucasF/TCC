package com.loja.api;

import io.javalin.Javalin;
import com.loja.model.*;
import com.loja.repository.ProductRepository;

public class ProductEndpoint {
    private final ProductRepository productRepo;

    public ProductEndpoint(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public void register(Javalin app) {
        app.get("/products/{sku}", ctx -> {
            Product product = productRepo.findBySku(ctx.pathParam("sku"));
            if (product == null) {
                ctx.status(404).json(new ErrorResponse("PRODUCT_NOT_FOUND"));
                return;
            }
            ctx.status(200).json(product);
        });
    }
}
