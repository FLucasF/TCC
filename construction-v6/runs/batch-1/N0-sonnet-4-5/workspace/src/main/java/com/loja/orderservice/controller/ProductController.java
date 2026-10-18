package com.loja.orderservice.controller;

import com.loja.orderservice.dto.ProductResponse;
import com.loja.orderservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String sku) {
        ProductResponse product = productService.getProduct(sku);
        return ResponseEntity.ok(product);
    }
}
