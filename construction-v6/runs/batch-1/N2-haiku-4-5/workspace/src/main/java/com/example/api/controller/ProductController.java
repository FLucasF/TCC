package com.example.api.controller;

import com.example.api.dto.ProductDto;
import com.example.application.service.ProductService;
import com.example.application.exception.ProductNotFoundException;
import com.example.domain.entity.Product;
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
    public ResponseEntity<ProductDto> getProduct(@PathVariable String sku) {
        Product product = productService.getProduct(sku);
        if (product == null) {
            throw new ProductNotFoundException();
        }
        ProductDto dto = new ProductDto(
                product.getSku(),
                product.getName(),
                product.getPrice(),
                product.getWeightKg(),
                product.getStock(),
                product.isDiscontinued()
        );
        return ResponseEntity.ok(dto);
    }
}
