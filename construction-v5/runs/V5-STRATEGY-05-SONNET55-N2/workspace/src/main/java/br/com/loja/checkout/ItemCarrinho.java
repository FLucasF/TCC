package br.com.loja.checkout;

import java.math.BigDecimal;

public record ItemCarrinho(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return precoUnitario != null && precoUnitario.signum() > 0
                && quantidade != null && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal subtotal() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
