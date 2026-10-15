package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Item já validado do carrinho. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    private BigDecimal unidades() {
        return BigDecimal.valueOf(quantidade);
    }

    public BigDecimal total() {
        return Dinheiro.centavos(precoUnitario.multiply(unidades()));
    }

    /** Peso do item no pedido, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(unidades());
    }
}
