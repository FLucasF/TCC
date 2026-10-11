package br.com.loja.checkout;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    boolean valido() {
        return precoUnitario != null && precoUnitario.signum() > 0
                && quantidade != null && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    BigDecimal total() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
