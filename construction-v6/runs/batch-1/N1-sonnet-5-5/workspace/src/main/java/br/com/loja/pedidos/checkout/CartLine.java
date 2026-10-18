package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;

import br.com.loja.pedidos.catalog.Product;

public record CartLine(Product product, int quantity) {

    public BigDecimal total() {
        return product.price().multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal weightKg() {
        return product.weightKg().multiply(BigDecimal.valueOf(quantity));
    }
}
