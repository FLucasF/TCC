package com.loja.strategy;

import com.loja.model.Product;

public interface ProductProvider {
    Product getProduct(String sku);
}
