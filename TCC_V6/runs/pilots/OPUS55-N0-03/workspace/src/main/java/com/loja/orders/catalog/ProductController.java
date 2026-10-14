package com.loja.orders.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;

@RestController
public class ProductController {

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/products/{sku}")
    public Product get(@PathVariable String sku) {
        return products.findBySku(sku).orElseThrow(() -> ApiException.notFound(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
