package com.loja.orderservice.service;

import com.loja.orderservice.dto.ProductResponse;
import com.loja.orderservice.entity.Product;
import com.loja.orderservice.exception.BusinessException;
import com.loja.orderservice.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String sku) {
        Product product = productRepository.findById(sku)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND));

        ProductResponse response = new ProductResponse();
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setWeightKg(product.getWeightKg());
        response.setStock(product.getStock());
        response.setDiscontinued(product.getDiscontinued());
        return response;
    }
}
