package com.example.application.service;

import com.example.domain.entity.Product;
import com.example.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product getProduct(String sku) {
        Optional<Product> product = productRepository.findById(sku);
        return product.orElse(null);
    }

    public void updateStock(String sku, Integer quantity) {
        Product product = productRepository.findById(sku).orElse(null);
        if (product != null) {
            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
        }
    }

    public void saveProduct(Product product) {
        productRepository.save(product);
    }
}
