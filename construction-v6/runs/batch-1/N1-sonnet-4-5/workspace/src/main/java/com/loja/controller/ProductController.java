package com.loja.controller;

import com.loja.domain.Product;
import com.loja.dto.ProductResponse;
import com.loja.service.ProductService;
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
        Product product = productService.getProduct(sku);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
}
