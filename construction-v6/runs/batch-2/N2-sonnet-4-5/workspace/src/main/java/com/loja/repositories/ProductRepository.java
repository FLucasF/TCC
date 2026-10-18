package com.loja.repositories;

import com.loja.domain.entities.Product;

public interface ProductRepository {
    Product findBySku(String sku);

    void update(Product product);

    void initializeCatalog();
}
