package com.clothing;

import com.clothing.model.ErrorResponse;
import com.clothing.service.ProductRepository;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductRoutes {
    private static final Logger logger = LoggerFactory.getLogger(ProductRoutes.class);
    private final Javalin app;
    private final ProductRepository productRepo = new ProductRepository();

    public ProductRoutes(Javalin app) {
        this.app = app;
    }

    public void register() {
        app.get("/products/{sku}", ctx -> {
            try {
                String sku = ctx.pathParam("sku");
                var product = productRepo.findBySku(sku);
                if (product == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("PRODUCT_NOT_FOUND"));
                    return;
                }
                ctx.status(200);
                ctx.json(product);
            } catch (Exception e) {
                logger.error("Error getting product", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });
    }
}
