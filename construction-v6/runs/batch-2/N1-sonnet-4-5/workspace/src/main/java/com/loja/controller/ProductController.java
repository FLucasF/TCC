package com.loja.controller;

import com.loja.domain.Product;
import com.loja.repository.ProductRepository;
import com.loja.util.ApiException;
import io.javalin.http.Context;

import java.util.Map;

public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void getProduct(Context ctx) {
        try {
            String sku = ctx.pathParam("sku");
            Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", 404));
            ctx.status(200).json(product);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }
}
