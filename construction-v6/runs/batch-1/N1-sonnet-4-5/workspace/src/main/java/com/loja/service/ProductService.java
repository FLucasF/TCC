package com.loja.service;

import com.loja.domain.Product;
import com.loja.exception.ErrorCode;
import com.loja.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Product getProduct(String sku) {
        return productRepository.findById(sku)
            .orElseThrow(() -> ErrorCode.PRODUCT_NOT_FOUND.asException());
    }
}
