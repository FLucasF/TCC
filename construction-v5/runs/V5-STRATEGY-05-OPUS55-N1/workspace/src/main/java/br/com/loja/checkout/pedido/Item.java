package br.com.loja.checkout.pedido;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    boolean valido() {
        return positivo(precoUnitario) && quantidade != null && quantidade > 0 && positivo(pesoKg);
    }

    public BigDecimal total() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
