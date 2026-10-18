package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;

public record PricedItem(String sku, BigDecimal unitPrice, BigDecimal weightKg, int quantity) {

    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
