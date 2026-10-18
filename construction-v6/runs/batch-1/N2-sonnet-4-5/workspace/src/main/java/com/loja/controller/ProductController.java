package com.loja.controller;

import com.loja.domain.Product;
import com.loja.dto.ErrorResponse;
import com.loja.dto.ProductResponse;
import com.loja.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<?> getProduct(@PathVariable String sku) {
        Product product = productRepository.findById(sku).orElse(null);

        if (product == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("PRODUCT_NOT_FOUND"));
        }

        ProductResponse response = new ProductResponse();
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setWeightKg(product.getWeightKg());
        response.setStock(product.getStock());
        response.setDiscontinued(product.getDiscontinued());

        return ResponseEntity.ok(response);
    }
}
