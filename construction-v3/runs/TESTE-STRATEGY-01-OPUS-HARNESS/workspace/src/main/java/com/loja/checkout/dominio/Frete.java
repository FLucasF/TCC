package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Frete(BigDecimal valor, int prazoDias) {

    public Frete gratis() {
        return new Frete(Dinheiro.ZERO, prazoDias);
    }
}
