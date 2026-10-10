package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho. */
public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    /** Valor da linha do carrinho, arredondado para centavos. */
    public BigDecimal total() {
        return Dinheiro.emCentavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso da linha do carrinho, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    boolean preenchido() {
        return precoUnitario != null && quantidade != null && pesoKg != null
                && precoUnitario.signum() > 0 && quantidade > 0 && pesoKg.signum() > 0;
    }
}
