package com.loja.orderservice.service;

import com.loja.orderservice.domain.entity.Product;
import com.loja.orderservice.dto.ProductResponse;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String sku) {
        Optional<Product> productOpt = productRepository.findById(sku);

        if (productOpt.isEmpty()) {
            throw new OrderServiceException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND);
        }

        Product product = productOpt.get();

        return ProductResponse.builder()
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .weightKg(product.getWeightKg())
                .stock(product.getStock())
                .discontinued(product.getDiscontinued())
                .build();
    }
}
