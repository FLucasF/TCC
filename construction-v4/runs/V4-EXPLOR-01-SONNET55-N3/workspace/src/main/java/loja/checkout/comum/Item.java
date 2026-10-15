package loja.checkout.comum;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
