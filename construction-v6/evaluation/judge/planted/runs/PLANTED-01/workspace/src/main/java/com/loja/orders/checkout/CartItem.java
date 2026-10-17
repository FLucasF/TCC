package com.loja.orders.checkout;

import java.math.BigDecimal;

import com.loja.orders.catalog.Product;
import com.loja.orders.common.Money;

/** Um item do carrinho já com o produto do catálogo. */
public record CartItem(Product product, int quantity) {

    public BigDecimal total() {
        return Money.round(product.price().multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal weightKg() {
        return product.weightKg().multiply(BigDecimal.valueOf(quantity));
    }
}
