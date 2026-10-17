package com.loja.orderservice.controller;

import com.loja.orderservice.dto.ProductResponseDTO;
import com.loja.orderservice.entity.Product;
import com.loja.orderservice.exception.OrderException;
import com.loja.orderservice.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products/{sku}")
    public ResponseEntity<?> getProduct(@PathVariable String sku) {
        var product = productRepository.findById(sku);
        if (product.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new com.loja.orderservice.dto.ErrorResponseDTO("PRODUCT_NOT_FOUND"));
        }

        var p = product.get();
        ProductResponseDTO response = new ProductResponseDTO(
            p.getSku(),
            p.getName(),
            p.getPrice(),
            p.getWeightKg(),
            p.getStock(),
            p.isDiscontinued()
        );
        return ResponseEntity.ok(response);
    }
}
