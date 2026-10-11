package br.com.loja.checkout.resumo;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    boolean valido() {
        return positivo(precoUnitario) && quantidade != null && quantidade > 0 && positivo(pesoKg);
    }

    public BigDecimal valorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
