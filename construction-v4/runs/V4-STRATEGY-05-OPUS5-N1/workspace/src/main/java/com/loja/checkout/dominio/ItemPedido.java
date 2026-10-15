package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal total() {
        return Dinheiro.emCentavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso do item no pedido, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
