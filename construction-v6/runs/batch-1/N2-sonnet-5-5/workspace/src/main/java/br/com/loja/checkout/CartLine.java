package br.com.loja.checkout;

import java.math.BigDecimal;

import br.com.loja.catalog.Product;

public record CartLine(Product product, int quantity) {

    public BigDecimal total() {
        return product.price().multiply(BigDecimal.valueOf(quantity));
    }
}
