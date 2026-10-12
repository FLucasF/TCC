package br.com.loja.checkout;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    boolean valido() {
        return precoUnitario != null && precoUnitario.signum() > 0
                && quantidade != null && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal valorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
