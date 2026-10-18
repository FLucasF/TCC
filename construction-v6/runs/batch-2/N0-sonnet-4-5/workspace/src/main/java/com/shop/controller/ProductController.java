package com.shop.controller;

import com.shop.dto.ErrorResponse;
import com.shop.dto.ProductResponse;
import com.shop.exception.BusinessException;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;
import io.javalin.http.Context;

public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void getProduct(Context ctx) {
        try {
            String sku = ctx.pathParam("sku");
            Product product = productRepository.findBySku(sku);

            if (product == null) {
                throw new BusinessException("PRODUCT_NOT_FOUND", 404);
            }

            ProductResponse response = new ProductResponse();
            response.setSku(product.getSku());
            response.setName(product.getName());
            response.setPrice(product.getPrice());
            response.setWeightKg(product.getWeightKg());
            response.setStock(product.getStock());
            response.setDiscontinued(product.isDiscontinued());

            ctx.status(200).json(response);
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }
}
